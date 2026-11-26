-- ============================================================================
-- 006 - Hapus kewajiban penguncian slot pada alur pemesanan
-- ============================================================================
-- Sebelumnya klien wajib memanggil acquire_slot_lock dan mengirim p_lock_id
-- ke create_booking. Kunci yang kedaluwarsa atau hilang membuat order gagal.
-- Mulai sekarang jadwal dikirim langsung oleh klien (p_start_at / p_end_at);
-- p_lock_id bersifat opsional untuk kompatibilitas dengan klien lama.
-- Konflik jadwal tetap dicegah oleh constraint bookings_no_overlap.
begin;

drop function if exists public.create_booking(
    uuid, uuid, text, boolean, numeric, int, int, boolean, int,
    double precision, double precision, text, text
);

create or replace function public.create_booking(
    p_service_id uuid,
    p_complexity text,
    p_is_urgent boolean,
    p_distance_km numeric,
    p_base_fare int,
    p_base_cost int,
    p_surge_applied boolean,
    p_total_cost int,
    p_cust_lat double precision,
    p_cust_lng double precision,
    p_notes text,
    p_payment_method text default 'CASH',
    p_start_at timestamptz default null,
    p_end_at timestamptz default null,
    p_lock_id uuid default null
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
    if p_start_at is null or p_end_at is null or p_end_at <= p_start_at then
        raise exception 'INVALID_SCHEDULE';
    end if;

    select provider_id into v_provider from public.services
    where id = p_service_id and is_active;
    if v_provider is null then
        raise exception 'INVALID_SERVICE';
    end if;
    if v_user = v_provider then
        raise exception 'SELF_BOOKING';
    end if;

    -- Klien lama masih boleh mengirim lock; klien baru tidak mengirimnya.
    if p_lock_id is not null then
        select * into l from public.slot_locks
         where id = p_lock_id and locked_by = v_user and expires_at > now();
        if not found then
            raise exception 'LOCK_EXPIRED';
        end if;
        if v_provider <> l.provider_id then
            raise exception 'INVALID_SERVICE';
        end if;
    end if;

    v_pay_status := case when v_method = 'QRIS' then 'WAITING_PAYMENT' else 'UNPAID' end;

    insert into public.bookings (
        customer_id, provider_id, service_id, start_at, end_at, complexity,
        is_urgent, distance_km, base_fare, base_cost, surge_applied, total_cost,
        customer_lat, customer_lng, notes, expires_at, payment_method, payment_status
    )
    values (
        v_user, v_provider, p_service_id, p_start_at, p_end_at, p_complexity,
        p_is_urgent, p_distance_km, p_base_fare, p_base_cost, p_surge_applied, p_total_cost,
        p_cust_lat, p_cust_lng, p_notes, now() + interval '15 minutes', v_method, v_pay_status
    )
    returning id into v_id;

    if p_lock_id is not null then
        delete from public.slot_locks where id = p_lock_id;
    end if;
    return v_id;
end $$;

revoke execute on function public.create_booking(
    uuid, text, boolean, numeric, int, int, boolean, int,
    double precision, double precision, text, text, timestamptz, timestamptz, uuid
) from public, anon;
grant execute on function public.create_booking(
    uuid, text, boolean, numeric, int, int, boolean, int,
    double precision, double precision, text, text, timestamptz, timestamptz, uuid
) to authenticated;

commit;
