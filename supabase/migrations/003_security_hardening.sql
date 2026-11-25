-- ============================================================================
-- BANTUIN - SECURITY HARDENING, ENABLE PROVIDER ROLE & REALTIME REPLICA (Fase 3)
-- ============================================================================
-- Idempotent migration: memperketat autentikasi RPC mutasi, mencabut akses anon
-- pada prosedur mutasi, menambahkan enable_provider_role(), dan mengaktifkan
-- REPLICA IDENTITY FULL untuk sinkronisasi Realtime.

-- 0. Sinkronkan trigger handle_new_user agar pendaftar provider langsung mendapat baris provider_profiles + availability default
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_roles text[];
begin
  v_roles := coalesce(
    array(
      select jsonb_array_elements_text(
        new.raw_user_meta_data->'roles'
      )
    ),
    array['customer']::text[]
  );

  insert into public.profiles (id, full_name, phone, roles)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', 'Pengguna'),
    new.raw_user_meta_data->>'phone',
    v_roles
  )
  on conflict (id) do update set
    full_name = coalesce(excluded.full_name, public.profiles.full_name),
    phone = coalesce(excluded.phone, public.profiles.phone);

  if 'provider' = any(v_roles) then
    insert into public.provider_profiles (user_id, bio, address, lat, lng)
    values (new.id, 'Mitra jasa Bantuin', 'Malang, Jawa Timur', -7.9797, 112.6304)
    on conflict (user_id) do nothing;

    insert into public.availability (provider_id, day_of_week, mode, open_time, close_time)
    select
      new.id,
      d.day,
      case when d.day = 7 then 'OFF' else 'ON' end,
      case when d.day = 7 then null else '08:00:00'::time end,
      case when d.day = 7 then null else '17:00:00'::time end
    from (values (1),(2),(3),(4),(5),(6),(7)) as d(day)
    on conflict (provider_id, day_of_week) do nothing;
  end if;

  return new;
end;
$$;

-- 1. Kunci slot hanya untuk pengguna terautentikasi
create or replace function public.acquire_slot_lock(p_provider uuid, p_start timestamptz, p_end timestamptz)
returns uuid language plpgsql security definer set search_path = public as $$
declare
  v_id uuid;
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'UNAUTHENTICATED';
  end if;

  delete from public.slot_locks where expires_at <= now();
  delete from public.slot_locks where locked_by = v_uid;

  if v_uid = p_provider then
    raise exception 'SELF_BOOKING';
  end if;

  if exists (
    select 1 from public.bookings b
     where b.provider_id = p_provider
       and b.status in ('PENDING','DIKONFIRMASI','DALAM_PENGERJAAN')
       and tstzrange(b.start_at, b.end_at, '[)') && tstzrange(p_start, p_end, '[)')
  ) then
    raise exception 'SLOT_CONFLICT';
  end if;

  if exists (
    select 1 from public.slot_locks l
     where l.provider_id = p_provider
       and l.locked_by <> v_uid
       and l.expires_at > now()
       and tstzrange(l.start_at, l.end_at, '[)') && tstzrange(p_start, p_end, '[)')
  ) then
    raise exception 'SLOT_LOCKED';
  end if;

  insert into public.slot_locks (provider_id, locked_by, start_at, end_at, expires_at)
  values (p_provider, v_uid, p_start, p_end, now() + interval '10 minutes')
  returning id into v_id;

  return v_id;
end $$;

create or replace function public.release_slot_lock() returns void
language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then
    raise exception 'UNAUTHENTICATED';
  end if;
  delete from public.slot_locks where locked_by = auth.uid();
end $$;

create or replace function public.create_booking(
  p_lock_id uuid, p_service_id uuid, p_complexity text, p_is_urgent boolean,
  p_distance_km numeric, p_base_fare int, p_base_cost int, p_surge_applied boolean, p_total_cost int,
  p_cust_lat double precision, p_cust_lng double precision, p_notes text
) returns uuid language plpgsql security definer set search_path = public as $$
declare
  l public.slot_locks;
  v_provider uuid;
  v_id uuid;
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception 'UNAUTHENTICATED';
  end if;

  select * into l from public.slot_locks
   where id = p_lock_id and locked_by = v_user and expires_at > now();
  if not found then
    raise exception 'LOCK_EXPIRED';
  end if;

  select provider_id into v_provider from public.services where id = p_service_id and is_active;
  if v_provider is null or v_provider <> l.provider_id then
    raise exception 'INVALID_SERVICE';
  end if;

  if v_user = l.provider_id then
    raise exception 'SELF_BOOKING';
  end if;

  insert into public.bookings (
    customer_id, provider_id, service_id, start_at, end_at, complexity,
    is_urgent, distance_km, base_fare, base_cost, surge_applied, total_cost,
    customer_lat, customer_lng, notes, expires_at
  )
  values (
    v_user, l.provider_id, p_service_id, l.start_at, l.end_at, p_complexity,
    p_is_urgent, p_distance_km, p_base_fare, p_base_cost, p_surge_applied, p_total_cost,
    p_cust_lat, p_cust_lng, p_notes, now() + interval '15 minutes'
  )
  returning id into v_id;

  delete from public.slot_locks where id = p_lock_id;
  return v_id;
end $$;

create or replace function public.update_booking_status(p_booking uuid, p_new text)
returns void language plpgsql security definer set search_path = public as $$
declare
  b public.bookings;
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'UNAUTHENTICATED';
  end if;

  select * into b from public.bookings where id = p_booking for update;
  if not found then
    raise exception 'NOT_FOUND';
  end if;

  if uid = b.provider_id then
    if b.status = 'PENDING' and p_new in ('DIKONFIRMASI','DITOLAK') then
      if b.expires_at <= now() then
        raise exception 'EXPIRED';
      end if;
      update public.bookings set status = p_new where id = p_booking;
    elsif b.status = 'DIKONFIRMASI' and p_new = 'DALAM_PENGERJAAN' then
      update public.bookings set status = p_new, started_at = now() where id = p_booking;
    elsif b.status = 'DALAM_PENGERJAAN' and p_new = 'SELESAI' then
      update public.bookings set status = p_new, finished_at = now() where id = p_booking;
    elsif b.status = 'DIKONFIRMASI' and p_new = 'DIBATALKAN_MITRA' then
      update public.bookings set status = p_new, finished_at = now() where id = p_booking;
    else
      raise exception 'INVALID_TRANSITION';
    end if;
  elsif uid = b.customer_id then
    if b.status in ('PENDING','DIKONFIRMASI') and p_new = 'DIBATALKAN_PELANGGAN' then
      update public.bookings set status = p_new, finished_at = now() where id = p_booking;
    else
      raise exception 'INVALID_TRANSITION';
    end if;
  else
    raise exception 'FORBIDDEN';
  end if;
end $$;

create or replace function public.apply_provider_metrics(
  p_provider uuid, p_rating_avg numeric, p_completion_rate numeric, p_on_time_rate numeric,
  p_cancel_rate numeric, p_trust_score numeric, p_tx_count int, p_flag boolean, p_badge text,
  p_rating_score numeric
) returns void language plpgsql security definer set search_path = public as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'UNAUTHENTICATED';
  end if;

  if uid <> p_provider and not exists (
      select 1 from public.bookings where provider_id = p_provider and customer_id = uid
  ) then
    raise exception 'FORBIDDEN';
  end if;

  update public.provider_profiles set
    rating_avg = p_rating_avg,
    completion_rate = p_completion_rate,
    on_time_rate = p_on_time_rate,
    cancel_rate = p_cancel_rate,
    trust_score = p_trust_score,
    tx_count_30d = p_tx_count,
    flag_anomaly = p_flag,
    badge = p_badge,
    metrics_updated_at = now()
  where user_id = p_provider;

  insert into public.provider_metric_snapshots
    (provider_id, trust_score, rating_score, on_time_rate, cancel_rate, tx_count, flag_anomaly)
  values
    (p_provider, p_trust_score, p_rating_score, p_on_time_rate, p_cancel_rate, p_tx_count, p_flag);
end $$;

-- Aktifkan peran Mitra untuk akun Pelanggan (PRD Bagian 10.3 & 11)
create or replace function public.enable_provider_role(
  p_bio text default '',
  p_address text default '',
  p_lat double precision default -7.9797,
  p_lng double precision default 112.6304
) returns void language plpgsql security definer set search_path = public as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'UNAUTHENTICATED';
  end if;

  update public.profiles
     set roles = array(
       select distinct r
       from unnest(coalesce(roles, array['customer']::text[]) || array['provider']::text[]) as r
     )
   where id = uid;

  insert into public.provider_profiles (user_id, bio, address, lat, lng)
  values (
    uid,
    coalesce(nullif(trim(p_bio), ''), 'Mitra jasa Bantuin'),
    coalesce(nullif(trim(p_address), ''), 'Malang, Jawa Timur'),
    p_lat,
    p_lng
  )
  on conflict (user_id) do nothing;

  insert into public.availability (provider_id, day_of_week, mode, open_time, close_time)
  select
    uid,
    d.day,
    case when d.day = 7 then 'OFF' else 'ON' end,
    case when d.day = 7 then null else '08:00:00'::time end,
    case when d.day = 7 then null else '17:00:00'::time end
  from (values (1),(2),(3),(4),(5),(6),(7)) as d(day)
  on conflict (provider_id, day_of_week) do nothing;
end $$;

-- 2. Cabut akses anon pada seluruh RPC mutasi dan batasi ke authenticated
revoke execute on function public.acquire_slot_lock(uuid, timestamptz, timestamptz) from public, anon;
revoke execute on function public.release_slot_lock() from public, anon;
revoke execute on function public.expire_pending_bookings() from public, anon;
revoke execute on function public.update_booking_status(uuid, text) from public, anon;
revoke execute on function public.create_booking(uuid, uuid, text, boolean, numeric, int, int, boolean, int, double precision, double precision, text) from public, anon;
revoke execute on function public.apply_provider_metrics(uuid, numeric, numeric, numeric, numeric, numeric, int, boolean, text, numeric) from public, anon;
revoke execute on function public.enable_provider_role(text, text, double precision, double precision) from public, anon;

grant execute on function public.get_busy_blocks(uuid, timestamptz, timestamptz) to authenticated;
grant execute on function public.acquire_slot_lock(uuid, timestamptz, timestamptz) to authenticated;
grant execute on function public.release_slot_lock() to authenticated;
grant execute on function public.expire_pending_bookings() to authenticated;
grant execute on function public.update_booking_status(uuid, text) to authenticated;
grant execute on function public.create_booking(uuid, uuid, text, boolean, numeric, int, int, boolean, int, double precision, double precision, text) to authenticated;
grant execute on function public.apply_provider_metrics(uuid, numeric, numeric, numeric, numeric, numeric, int, boolean, text, numeric) to authenticated;
grant execute on function public.enable_provider_role(text, text, double precision, double precision) to authenticated;

-- 3. Pastikan payload Realtime membawa data lengkap saat UPDATE/DELETE
alter table public.bookings replica identity full;
alter table public.booking_events replica identity full;
