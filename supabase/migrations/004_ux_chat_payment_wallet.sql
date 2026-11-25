-- ============================================================================
-- BANTUIN - UX REVAMP: PAYMENT (QRIS/CASH), BUILT-IN CHAT, CUSTOMER PROFILE,
-- PROVIDER AVAILABILITY TOGGLE, & PROVIDER WALLET/PAYOUT (Fase 4)
-- ============================================================================

-- 1. Ekstensi kolom profil pelanggan & status operasional mitra
alter table public.profiles
  add column if not exists default_address text,
  add column if not exists default_lat double precision,
  add column if not exists default_lng double precision;

alter table public.provider_profiles
  add column if not exists is_accepting_orders boolean not null default true;

-- 2. Ekstensi kolom metode bayar, status bayar, dan catatan penyelesaian pada bookings
alter table public.bookings
  add column if not exists payment_method text not null default 'CASH'
    check (payment_method in ('QRIS', 'CASH')),
  add column if not exists payment_status text not null default 'UNPAID'
    check (payment_status in ('UNPAID', 'WAITING_PAYMENT', 'PAID', 'REFUNDED')),
  add column if not exists completion_note text;

-- 3. Tabel percakapan per pesanan
create table if not exists public.booking_messages (
  id uuid primary key default gen_random_uuid(),
  booking_id uuid not null references public.bookings(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  sender_role text not null check (sender_role in ('customer', 'provider')),
  message_text text not null check (char_length(trim(message_text)) between 1 and 1000),
  created_at timestamptz not null default now()
);

create index if not exists idx_booking_messages_booking_created
  on public.booking_messages (booking_id, created_at asc);

alter table public.booking_messages enable row level security;

drop policy if exists "booking_messages_select_party" on public.booking_messages;
create policy "booking_messages_select_party" on public.booking_messages
  for select to authenticated
  using (
    exists (
      select 1 from public.bookings b
       where b.id = booking_messages.booking_id
         and (b.customer_id = auth.uid() or b.provider_id = auth.uid())
    )
  );

drop policy if exists "booking_messages_insert_party" on public.booking_messages;
create policy "booking_messages_insert_party" on public.booking_messages
  for insert to authenticated
  with check (
    sender_id = auth.uid()
    and exists (
      select 1 from public.bookings b
       where b.id = booking_messages.booking_id
         and (b.customer_id = auth.uid() or b.provider_id = auth.uid())
    )
  );

-- 4. Tabel pencairan saldo Mitra (hanya dari pesanan QRIS selesai lunas)
create table if not exists public.payout_requests (
  id uuid primary key default gen_random_uuid(),
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  amount int not null check (amount >= 20000),
  admin_fee int not null default 2500 check (admin_fee >= 0),
  net_amount int not null check (net_amount > 0),
  channel_type text not null check (channel_type in ('BANK', 'EWALLET')),
  channel_name text not null,
  account_number text not null,
  account_holder text not null,
  status text not null default 'COMPLETED' check (status in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
  created_at timestamptz not null default now()
);

create index if not exists idx_payout_requests_provider_created
  on public.payout_requests (provider_id, created_at desc);

alter table public.payout_requests enable row level security;

drop policy if exists "payout_requests_select_owner" on public.payout_requests;
create policy "payout_requests_select_owner" on public.payout_requests
  for select to authenticated
  using (provider_id = auth.uid());

-- 5. Perbarui create_booking agar mendukung p_payment_method (default 'CASH')
drop function if exists public.create_booking(uuid, uuid, text, boolean, numeric, int, int, boolean, int, double precision, double precision, text);

create or replace function public.create_booking(
  p_lock_id uuid, p_service_id uuid, p_complexity text, p_is_urgent boolean,
  p_distance_km numeric, p_base_fare int, p_base_cost int, p_surge_applied boolean, p_total_cost int,
  p_cust_lat double precision, p_cust_lng double precision, p_notes text,
  p_payment_method text default 'CASH'
) returns uuid language plpgsql security definer set search_path = public as $$
declare
  l public.slot_locks;
  v_provider uuid;
  v_id uuid;
  v_user uuid := auth.uid();
  v_method text := coalesce(nullif(upper(trim(p_payment_method)), ''), 'CASH');
  v_pay_status text;
begin
  if v_user is null then
    raise exception 'UNAUTHENTICATED';
  end if;
  if v_method not in ('QRIS', 'CASH') then
    raise exception 'INVALID_PAYMENT_METHOD';
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

  v_pay_status := case when v_method = 'QRIS' then 'WAITING_PAYMENT' else 'UNPAID' end;

  insert into public.bookings (
    customer_id, provider_id, service_id, start_at, end_at, complexity,
    is_urgent, distance_km, base_fare, base_cost, surge_applied, total_cost,
    customer_lat, customer_lng, notes, expires_at, payment_method, payment_status
  )
  values (
    v_user, l.provider_id, p_service_id, l.start_at, l.end_at, p_complexity,
    p_is_urgent, p_distance_km, p_base_fare, p_base_cost, p_surge_applied, p_total_cost,
    p_cust_lat, p_cust_lng, p_notes, now() + interval '15 minutes', v_method, v_pay_status
  )
  returning id into v_id;

  delete from public.slot_locks where id = p_lock_id;
  return v_id;
end $$;

-- 6. RPC konfirmasi pembayaran QRIS oleh pelanggan
create or replace function public.confirm_qris_payment(p_booking uuid)
returns void language plpgsql security definer set search_path = public as $$
declare
  b public.bookings;
  uid uuid := auth.uid();
begin
  if uid is null then raise exception 'UNAUTHENTICATED'; end if;

  select * into b from public.bookings where id = p_booking for update;
  if not found then raise exception 'NOT_FOUND'; end if;
  if b.customer_id <> uid then raise exception 'FORBIDDEN'; end if;
  if b.payment_method <> 'QRIS' then raise exception 'INVALID_PAYMENT_METHOD'; end if;
  if b.status in ('DITOLAK', 'KEDALUWARSA', 'DIBATALKAN_PELANGGAN', 'DIBATALKAN_MITRA') then
    raise exception 'INVALID_TRANSITION';
  end if;

  update public.bookings
     set payment_status = 'PAID'
   where id = p_booking;
end $$;

-- 7. RPC penyelesaian pekerjaan oleh mitra (validasi QRIS lunas / konfirmasi uang tunai)
create or replace function public.complete_booking(
  p_booking uuid,
  p_completion_note text default null,
  p_cash_collected boolean default false
) returns void language plpgsql security definer set search_path = public as $$
declare
  b public.bookings;
  uid uuid := auth.uid();
begin
  if uid is null then raise exception 'UNAUTHENTICATED'; end if;

  select * into b from public.bookings where id = p_booking for update;
  if not found then raise exception 'NOT_FOUND'; end if;
  if b.provider_id <> uid then raise exception 'FORBIDDEN'; end if;
  if b.status <> 'DALAM_PENGERJAAN' then raise exception 'INVALID_TRANSITION'; end if;

  if b.payment_method = 'CASH' then
    if not coalesce(p_cash_collected, false) then
      raise exception 'CASH_CONFIRMATION_REQUIRED';
    end if;
    update public.bookings
       set status = 'SELESAI',
           payment_status = 'PAID',
           completion_note = nullif(trim(p_completion_note), ''),
           finished_at = now()
     where id = p_booking;
  else
    if b.payment_status <> 'PAID' then
      raise exception 'QRIS_UNPAID';
    end if;
    update public.bookings
       set status = 'SELESAI',
           completion_note = nullif(trim(p_completion_note), ''),
           finished_at = now()
     where id = p_booking;
  end if;
end $$;

-- 8. RPC pengajuan pencairan dana Mitra (hanya saldo QRIS tersedia)
create or replace function public.request_provider_payout(
  p_amount int,
  p_channel_type text,
  p_channel_name text,
  p_account_number text,
  p_account_holder text
) returns uuid language plpgsql security definer set search_path = public as $$
declare
  uid uuid := auth.uid();
  v_qris_gross int;
  v_withdrawn_or_pending int;
  v_available int;
  v_admin_fee int := 2500;
  v_net int;
  v_id uuid;
begin
  if uid is null then raise exception 'UNAUTHENTICATED'; end if;
  if p_amount < 20000 then raise exception 'MIN_PAYOUT_NOT_MET'; end if;
  if p_channel_type not in ('BANK', 'EWALLET') then raise exception 'INVALID_CHANNEL'; end if;
  if nullif(trim(p_channel_name), '') is null
     or nullif(trim(p_account_number), '') is null
     or nullif(trim(p_account_holder), '') is null then
    raise exception 'INVALID_ACCOUNT_DETAILS';
  end if;

  perform 1 from public.provider_profiles where user_id = uid for update;
  if not found then raise exception 'FORBIDDEN'; end if;

  select coalesce(sum(total_cost), 0) into v_qris_gross
    from public.bookings
   where provider_id = uid
     and status = 'SELESAI'
     and payment_method = 'QRIS'
     and payment_status = 'PAID';

  select coalesce(sum(amount), 0) into v_withdrawn_or_pending
    from public.payout_requests
   where provider_id = uid
     and status in ('PENDING', 'PROCESSING', 'COMPLETED');

  v_available := greatest(v_qris_gross - v_withdrawn_or_pending, 0);
  if p_amount > v_available then
    raise exception 'INSUFFICIENT_BALANCE';
  end if;

  v_net := p_amount - v_admin_fee;
  if v_net <= 0 then
    raise exception 'MIN_PAYOUT_NOT_MET';
  end if;

  insert into public.payout_requests (
    provider_id, amount, admin_fee, net_amount,
    channel_type, channel_name, account_number, account_holder, status
  )
  values (
    uid, p_amount, v_admin_fee, v_net,
    p_channel_type, trim(p_channel_name), trim(p_account_number), trim(p_account_holder), 'COMPLETED'
  )
  returning id into v_id;

  return v_id;
end $$;

-- 9. Permissions RPC baru & publikasi Realtime untuk booking_messages
revoke execute on function public.create_booking(uuid, uuid, text, boolean, numeric, int, int, boolean, int, double precision, double precision, text, text) from public, anon;
revoke execute on function public.confirm_qris_payment(uuid) from public, anon;
revoke execute on function public.complete_booking(uuid, text, boolean) from public, anon;
revoke execute on function public.request_provider_payout(int, text, text, text, text) from public, anon;

grant execute on function public.create_booking(uuid, uuid, text, boolean, numeric, int, int, boolean, int, double precision, double precision, text, text) to authenticated;
grant execute on function public.confirm_qris_payment(uuid) to authenticated;
grant execute on function public.complete_booking(uuid, text, boolean) to authenticated;
grant execute on function public.request_provider_payout(int, text, text, text, text) to authenticated;

alter table public.booking_messages replica identity full;
do $$
begin
  if not exists (
    select 1
      from pg_publication_tables
     where pubname = 'supabase_realtime'
       and schemaname = 'public'
       and tablename = 'booking_messages'
  ) then
    alter publication supabase_realtime add table public.booking_messages;
  end if;
end $$;
