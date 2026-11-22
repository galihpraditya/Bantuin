-- ============================================================================
-- BANTUIN DATABASE INITIALIZATION (Fase 2)
-- Sesuai Dokumen Spesifikasi PRD.md Bagian 9
-- ============================================================================

-- ===== Ekstensi PostgreSQL =====
create extension if not exists btree_gist;
create extension if not exists pgcrypto;

-- ===== 1. Tabel-Tabel Utama =====

-- Profil pengguna (Customer & Provider)
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  phone text,
  roles text[] not null default array['customer']
    check (roles <@ array['customer','provider']::text[] and cardinality(roles) >= 1),
  avatar_url text,
  created_at timestamptz not null default now()
);

-- Kategori Layanan
create table if not exists public.service_categories (
  id serial primary key,
  name text not null unique,
  icon text not null default 'build',
  default_duration_min int not null default 60
);

-- Profil Spesifik Mitra
create table if not exists public.provider_profiles (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  bio text,
  address text,
  lat double precision not null,
  lng double precision not null,
  is_suspended boolean not null default false,
  -- metrik cache (dihitung F4 di Kotlin, jendela 30 hari)
  rating_avg numeric(3,2),
  completion_rate numeric(5,4),
  on_time_rate numeric(5,2),
  cancel_rate numeric(5,2) not null default 0,
  trust_score numeric(5,2) not null default 70,
  tx_count_30d int not null default 0,
  flag_anomaly boolean not null default false,
  badge text not null default 'NEW' check (badge in ('NEW','TRUSTED','NONE')),
  metrics_updated_at timestamptz
);

-- Katalog Layanan yang Ditawarkan Mitra
create table if not exists public.services (
  id uuid primary key default gen_random_uuid(),
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  category_id int not null references public.service_categories(id),
  name text not null,
  description text,
  base_fare int not null check (base_fare >= 0),
  duration_min int not null check (duration_min between 15 and 720),
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

-- Ketersediaan Jadwal Mingguan Mitra (1=Senin..7=Minggu)
create table if not exists public.availability (
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  day_of_week smallint not null check (day_of_week between 1 and 7),
  mode text not null check (mode in ('OFF','ON','ALWAYS_ON')),
  open_time time,
  close_time time,
  primary key (provider_id, day_of_week),
  check (mode <> 'ON' or (open_time is not null and close_time is not null and open_time < close_time))
);

-- Pesanan / Transaksi Booking
create table if not exists public.bookings (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.profiles(id),
  provider_id uuid not null references public.provider_profiles(user_id),
  service_id uuid not null references public.services(id),
  start_at timestamptz not null,
  end_at timestamptz not null,                 -- start + durasi + 30 menit buffer
  complexity text not null check (complexity in ('RINGAN','SEDANG','BERAT')),
  is_urgent boolean not null default false,
  distance_km numeric(5,1) not null,
  base_fare int not null,
  base_cost int not null,
  surge_applied boolean not null default false,
  total_cost int not null,
  customer_lat double precision not null,
  customer_lng double precision not null,
  notes text,
  status text not null default 'PENDING' check (status in
    ('PENDING','DIKONFIRMASI','DALAM_PENGERJAAN','SELESAI','DITOLAK','KEDALUWARSA',
     'DIBATALKAN_PELANGGAN','DIBATALKAN_MITRA')),
  expires_at timestamptz not null,
  started_at timestamptz,
  finished_at timestamptz,
  created_at timestamptz not null default now(),
  check (customer_id <> provider_id),
  check (end_at > start_at),
  constraint bookings_no_overlap exclude using gist (
    provider_id with =,
    tstzrange(start_at, end_at, '[)') with &&
  ) where (status in ('PENDING','DIKONFIRMASI','DALAM_PENGERJAAN'))
);

create index if not exists idx_bookings_provider_status on public.bookings (provider_id, status);
create index if not exists idx_bookings_customer_status on public.bookings (customer_id, status);

-- Kunci Slot Sementara (F3 - 10 Menit Anti-Race Condition)
create table if not exists public.slot_locks (
  id uuid primary key default gen_random_uuid(),
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  locked_by uuid not null references public.profiles(id) on delete cascade,
  start_at timestamptz not null,
  end_at timestamptz not null,
  expires_at timestamptz not null
);

-- Ulasan & Rating Pesanan
create table if not exists public.reviews (
  id uuid primary key default gen_random_uuid(),
  booking_id uuid not null unique references public.bookings(id) on delete cascade,
  customer_id uuid not null references public.profiles(id),
  provider_id uuid not null references public.provider_profiles(user_id),
  rating smallint not null check (rating between 1 and 5),
  comment text,
  created_at timestamptz not null default now()
);

-- Jejak Audit Status Pemesanan
create table if not exists public.booking_events (
  id bigint generated always as identity primary key,
  booking_id uuid not null references public.bookings(id) on delete cascade,
  status text not null,
  actor_id uuid,
  created_at timestamptz not null default now()
);

-- Snapshot Historis Metrik Mitra (F4)
create table if not exists public.provider_metric_snapshots (
  id bigint generated always as identity primary key,
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  computed_at timestamptz not null default now(),
  trust_score numeric(5,2) not null,
  rating_score numeric(5,2),
  on_time_rate numeric(5,2),
  cancel_rate numeric(5,2),
  tx_count int not null,
  flag_anomaly boolean not null
);

-- ===== 2. Trigger Otomatis =====

-- Trigger: Pembuatan profil otomatis saat sign up di auth.users
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, full_name, phone, roles)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', 'Pengguna'),
    new.raw_user_meta_data->>'phone',
    coalesce(
      array(
        select jsonb_array_elements_text(
          new.raw_user_meta_data->'roles'
        )
      ),
      array['customer']::text[]
    )
  )
  on conflict (id) do update set
    full_name = coalesce(excluded.full_name, public.profiles.full_name),
    phone = coalesce(excluded.phone, public.profiles.phone);

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- Trigger: Riwayat audit status otomatis ke booking_events
create or replace function public.log_booking_status() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if tg_op = 'INSERT' or new.status is distinct from old.status then
    insert into public.booking_events (booking_id, status, actor_id)
    values (new.id, new.status, auth.uid());
  end if;
  return new;
end $$;

drop trigger if exists trg_booking_status on public.bookings;
create trigger trg_booking_status after insert or update of status on public.bookings
  for each row execute function public.log_booking_status();

-- ===== 3. Prosedur Tersimpan (RPC Functions) =====

-- RPC 1: Dapatkan blok waktu sibuk mitra (booking aktif + slot lock pengguna lain)
create or replace function public.get_busy_blocks(p_provider uuid, p_from timestamptz, p_to timestamptz)
returns table (start_at timestamptz, end_at timestamptz)
language sql stable security definer set search_path = public as $$
  select b.start_at, b.end_at from public.bookings b
   where b.provider_id = p_provider
     and b.status in ('PENDING','DIKONFIRMASI','DALAM_PENGERJAAN')
     and b.start_at < p_to and b.end_at > p_from
  union all
  select l.start_at, l.end_at from public.slot_locks l
   where l.provider_id = p_provider and l.expires_at > now()
     and (auth.uid() is null or l.locked_by <> auth.uid())
     and l.start_at < p_to and l.end_at > p_from;
$$;

-- RPC 2: Kunci slot kalender selama 10 menit (F3)
create or replace function public.acquire_slot_lock(p_provider uuid, p_start timestamptz, p_end timestamptz)
returns uuid language plpgsql security definer set search_path = public as $$
declare v_id uuid; v_uid uuid := auth.uid();
begin
  delete from public.slot_locks where expires_at < now();
  if v_uid is not null then
    delete from public.slot_locks where locked_by = v_uid;
    if v_uid = p_provider then raise exception 'SELF_BOOKING'; end if;
  end if;

  if exists (select 1 from public.bookings b
              where b.provider_id = p_provider
                and b.status in ('PENDING','DIKONFIRMASI','DALAM_PENGERJAAN')
                and tstzrange(b.start_at, b.end_at, '[)') && tstzrange(p_start, p_end, '[)')) then
    raise exception 'SLOT_CONFLICT';
  end if;

  if exists (select 1 from public.slot_locks l
              where l.provider_id = p_provider and (v_uid is null or l.locked_by <> v_uid)
                and tstzrange(l.start_at, l.end_at, '[)') && tstzrange(p_start, p_end, '[)')) then
    raise exception 'SLOT_LOCKED';
  end if;

  insert into public.slot_locks (provider_id, locked_by, start_at, end_at, expires_at)
  values (p_provider, coalesce(v_uid, 'c0000000-0000-0000-0000-000000000001'::uuid), p_start, p_end, now() + interval '10 minutes')
  returning id into v_id;
  return v_id;
end $$;

-- RPC 3: Lepaskan kunci slot milik pengguna aktif
create or replace function public.release_slot_lock() returns void
language sql security definer set search_path = public as $$
  delete from public.slot_locks where locked_by = auth.uid();
$$;

-- RPC 4: Buat pesanan baru dengan validasi slot_lock (F2 & F3)
create or replace function public.create_booking(
  p_lock_id uuid, p_service_id uuid, p_complexity text, p_is_urgent boolean,
  p_distance_km numeric, p_base_fare int, p_base_cost int, p_surge_applied boolean, p_total_cost int,
  p_cust_lat double precision, p_cust_lng double precision, p_notes text
) returns uuid language plpgsql security definer set search_path = public as $$
declare
  l public.slot_locks;
  v_provider uuid;
  v_id uuid;
  v_user uuid := coalesce(auth.uid(), 'c0000000-0000-0000-0000-000000000001'::uuid);
begin
  select * into l from public.slot_locks
   where id = p_lock_id and (auth.uid() is null or locked_by = auth.uid()) and expires_at > now();
  if not found then raise exception 'LOCK_EXPIRED'; end if;

  select provider_id into v_provider from public.services where id = p_service_id and is_active;
  if v_provider is null or v_provider <> l.provider_id then raise exception 'INVALID_SERVICE'; end if;

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

-- RPC 5: Kedaluwarsakan pesanan PENDING yang lewat 15 menit
create or replace function public.expire_pending_bookings() returns void
language sql security definer set search_path = public as $$
  update public.bookings set status = 'KEDALUWARSA'
   where status = 'PENDING' and expires_at < now();
$$;

-- RPC 6: Update status pesanan dengan kontrol peran ketat
create or replace function public.update_booking_status(p_booking uuid, p_new text)
returns void language plpgsql security definer set search_path = public as $$
declare
  b public.bookings;
  uid uuid := auth.uid();
begin
  select * into b from public.bookings where id = p_booking for update;
  if not found then raise exception 'NOT_FOUND'; end if;

  -- Fallback untuk simulasi dev / service_role tanpa JWT
  if uid is null then
    uid := b.provider_id;
  end if;

  if uid = b.provider_id then
    if b.status = 'PENDING' and p_new in ('DIKONFIRMASI','DITOLAK') then
      if b.expires_at <= now() then raise exception 'EXPIRED'; end if;
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

-- RPC 7: Terapkan rekalkulasi metrik mitra (F4)
create or replace function public.apply_provider_metrics(
  p_provider uuid, p_rating_avg numeric, p_completion_rate numeric, p_on_time_rate numeric,
  p_cancel_rate numeric, p_trust_score numeric, p_tx_count int, p_flag boolean, p_badge text,
  p_rating_score numeric
) returns void language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is not null and auth.uid() <> p_provider and not exists (
      select 1 from public.bookings where provider_id = p_provider and customer_id = auth.uid()) then
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

-- Hak Akses Eksekusi RPC
grant execute on function public.get_busy_blocks(uuid, timestamptz, timestamptz) to anon, authenticated;
grant execute on function public.acquire_slot_lock(uuid, timestamptz, timestamptz) to anon, authenticated;
grant execute on function public.release_slot_lock() to anon, authenticated;
grant execute on function public.expire_pending_bookings() to anon, authenticated;
grant execute on function public.update_booking_status(uuid, text) to anon, authenticated;
grant execute on function public.create_booking(uuid, uuid, text, boolean, numeric, int, int, boolean, int, double precision, double precision, text) to anon, authenticated;
grant execute on function public.apply_provider_metrics(uuid, numeric, numeric, numeric, numeric, numeric, int, boolean, text, numeric) to anon, authenticated;

-- ===== 4. Row Level Security (RLS) =====
alter table public.profiles enable row level security;
alter table public.service_categories enable row level security;
alter table public.provider_profiles enable row level security;
alter table public.services enable row level security;
alter table public.availability enable row level security;
alter table public.bookings enable row level security;
alter table public.slot_locks enable row level security;
alter table public.reviews enable row level security;
alter table public.booking_events enable row level security;
alter table public.provider_metric_snapshots enable row level security;

-- Policies Profiles
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles for select to anon, authenticated using (true);

drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles for update to authenticated
  using (id = auth.uid()) with check (id = auth.uid());

-- Policies Categories
drop policy if exists categories_select on public.service_categories;
create policy categories_select on public.service_categories for select to anon, authenticated using (true);

-- Policies Provider Profiles
drop policy if exists pp_select on public.provider_profiles;
create policy pp_select on public.provider_profiles for select to anon, authenticated using (true);

drop policy if exists pp_insert_own on public.provider_profiles;
create policy pp_insert_own on public.provider_profiles for insert to authenticated with check (user_id = auth.uid());

drop policy if exists pp_update_own on public.provider_profiles;
create policy pp_update_own on public.provider_profiles for update to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Policies Services
drop policy if exists services_select on public.services;
create policy services_select on public.services for select to anon, authenticated
  using (is_active or (auth.uid() is not null and provider_id = auth.uid()));

drop policy if exists services_write_own on public.services;
create policy services_write_own on public.services for all to authenticated
  using (provider_id = auth.uid()) with check (provider_id = auth.uid());

-- Policies Availability
drop policy if exists availability_select on public.availability;
create policy availability_select on public.availability for select to anon, authenticated using (true);

drop policy if exists availability_write_own on public.availability;
create policy availability_write_own on public.availability for all to authenticated
  using (provider_id = auth.uid()) with check (provider_id = auth.uid());

-- Policies Bookings (Hanya baca, manipulasi lewat RPC)
drop policy if exists bookings_select on public.bookings;
create policy bookings_select on public.bookings for select to authenticated
  using (customer_id = auth.uid() or provider_id = auth.uid());

-- Policies Slot Locks
drop policy if exists locks_select_own on public.slot_locks;
create policy locks_select_own on public.slot_locks for select to authenticated using (locked_by = auth.uid());

-- Policies Reviews
drop policy if exists reviews_select on public.reviews;
create policy reviews_select on public.reviews for select to anon, authenticated using (true);

drop policy if exists reviews_insert on public.reviews;
create policy reviews_insert on public.reviews for insert to authenticated with check (
  customer_id = auth.uid() and exists (
    select 1 from public.bookings b
     where b.id = booking_id and b.customer_id = auth.uid()
       and b.provider_id = reviews.provider_id and b.status = 'SELESAI'));

-- Policies Booking Events
drop policy if exists events_select on public.booking_events;
create policy events_select on public.booking_events for select to authenticated using (
  exists (select 1 from public.bookings b where b.id = booking_id
          and (b.customer_id = auth.uid() or b.provider_id = auth.uid())));

-- Policies Metric Snapshots
drop policy if exists snapshots_select_own on public.provider_metric_snapshots;
create policy snapshots_select_own on public.provider_metric_snapshots for select to authenticated
  using (provider_id = auth.uid());

-- ===== 5. Publikasi Realtime =====
do $$
begin
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'bookings') then
    alter publication supabase_realtime add table public.bookings;
  end if;
  if not exists (select 1 from pg_publication_tables where pubname = 'supabase_realtime' and tablename = 'booking_events') then
    alter publication supabase_realtime add table public.booking_events;
  end if;
end $$;
