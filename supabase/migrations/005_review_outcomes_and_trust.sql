begin;

alter table public.provider_profiles
    alter column trust_score set default 0,
    alter column flag_anomaly set default true,
    alter column badge set default 'NONE';
revoke insert, update on public.provider_profiles from anon, authenticated;
grant insert (user_id, bio, address, lat, lng), update (bio, address, lat, lng, is_accepting_orders)
    on public.provider_profiles to authenticated;

alter table public.bookings add column completion_outcome text check (
    completion_outcome is null
    or (completion_outcome in ('SELESAI_TEPAT_WAKTU', 'TERLAMBAT') and status = 'SELESAI')
    or (completion_outcome = 'DIBATALKAN' and status in ('DIBATALKAN_MITRA', 'DIBATALKAN_PELANGGAN'))
);
alter table public.reviews add column tags text[] not null default '{}';
alter table public.provider_metric_snapshots add column formula_version int not null default 1;

create index if not exists bookings_trust_window on public.bookings (provider_id, (coalesce(finished_at, created_at)));
create index if not exists reviews_trust_window on public.reviews (provider_id, created_at);

drop function public.get_provider_trust_bookings(uuid, timestamptz);
create function public.get_provider_trust_bookings(p_provider uuid, p_cutoff timestamptz)
returns table(status text, start_at timestamptz, started_at timestamptz, finished_at timestamptz,
              created_at timestamptz, completion_outcome text)
language sql stable security definer set search_path = '' as $$
    select b.status, b.start_at, b.started_at, b.finished_at, b.created_at, b.completion_outcome
    from public.bookings b
    where b.provider_id = p_provider and coalesce(b.finished_at, b.created_at) between p_cutoff and statement_timestamp()
$$;
revoke execute on function public.get_provider_trust_bookings(uuid, timestamptz) from public, anon;
grant execute on function public.get_provider_trust_bookings(uuid, timestamptz) to authenticated;

create function public.refresh_provider_trust(p_provider uuid)
returns void language plpgsql security definer set search_path = '' as $$
declare
    as_of timestamptz;
    cutoff timestamptz;
    total int;
    finished int;
    punctual int;
    cancelled int;
    stars bigint;
    review_count int;
    avg_rating numeric;
    rating_points numeric;
    sla numeric;
    cancellation numeric;
    score numeric;
    flagged boolean;
    rating_den numeric;
    sla_den numeric;
    cancel_den numeric;
    numerator numeric;
    denominator numeric;
begin
    perform 1 from public.provider_profiles where user_id = p_provider for no key update;
    if not found then return; end if;
    as_of := clock_timestamp();
    cutoff := as_of - interval '720 hours';

    select count(*), count(*) filter (where status = 'SELESAI'),
        count(*) filter (where status = 'SELESAI' and
            coalesce(completion_outcome, case when coalesce(started_at, start_at) <= start_at + interval '15 minutes'
                then 'SELESAI_TEPAT_WAKTU' else 'TERLAMBAT' end) = 'SELESAI_TEPAT_WAKTU'),
        count(*) filter (where status in ('DIBATALKAN_MITRA', 'DIBATALKAN_PELANGGAN'))
    into total, finished, punctual, cancelled
    from public.bookings
    where provider_id = p_provider and coalesce(finished_at, created_at) between cutoff and as_of;

    select coalesce(sum(rating), 0), count(*) into stars, review_count from public.reviews
    where provider_id = p_provider and created_at between cutoff and as_of;

    avg_rating := stars::numeric / nullif(review_count, 0);
    rating_points := coalesce(avg_rating * 20, 0);
    sla := coalesce(punctual * 100.0 / nullif(finished, 0), 0);
    cancellation := coalesce(cancelled * 100.0 / nullif(total, 0), 0);
    rating_den := greatest(review_count, 1);
    sla_den := greatest(finished, 1);
    cancel_den := greatest(total, 1);
    denominator := rating_den * sla_den * cancel_den;
    numerator := 9.0 * stars * sla_den * cancel_den + 35.0 * punctual * rating_den * cancel_den
        - 20.0 * cancelled * rating_den * sla_den;
    score := greatest(0, least(100, numerator / denominator));
    flagged := numerator < 60 * denominator or 5::bigint * cancelled > total;

    update public.provider_profiles set rating_avg = avg_rating,
        completion_rate = finished::numeric / nullif(finished + cancelled, 0), on_time_rate = sla, cancel_rate = cancellation,
        trust_score = score, tx_count_30d = total, flag_anomaly = flagged,
        badge = case when flagged then 'NONE' else 'TRUSTED' end, metrics_updated_at = as_of
    where user_id = p_provider;

    update public.provider_metric_snapshots set trust_score = score, rating_score = rating_points,
        on_time_rate = sla, cancel_rate = cancellation, tx_count = total, flag_anomaly = flagged
    where provider_id = p_provider and computed_at = transaction_timestamp() and formula_version = 2;
    if not found then
        insert into public.provider_metric_snapshots
            (provider_id, computed_at, trust_score, rating_score, on_time_rate, cancel_rate, tx_count, flag_anomaly, formula_version)
        values (p_provider, transaction_timestamp(), score, rating_points, sla, cancellation, total, flagged, 2);
    end if;
end $$;

-- Preserve the API used by the manual refresh. The database owns its inputs and the score.
create or replace function public.apply_provider_metrics(
    p_provider uuid, p_rating_avg numeric, p_completion_rate numeric, p_on_time_rate numeric,
    p_cancel_rate numeric, p_trust_score numeric, p_tx_count int, p_flag boolean, p_badge text, p_rating_score numeric
) returns void language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is distinct from p_provider or auth.uid() is null then
        raise exception 'FORBIDDEN' using errcode = '42501';
    end if;
    perform public.refresh_provider_trust(p_provider);
end $$;

create function public.sync_trust_after_write() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    if tg_op = 'DELETE' then
        perform public.refresh_provider_trust(old.provider_id);
    else
        perform public.refresh_provider_trust(new.provider_id);
    end if;
    return null;
end $$;

create trigger bookings_sync_provider_trust
after insert or update of status, completion_outcome or delete on public.bookings
for each row execute function public.sync_trust_after_write();
create trigger reviews_sync_provider_trust
after insert or update or delete on public.reviews
for each row execute function public.sync_trust_after_write();

create function public.save_booking_review(p_booking uuid, p_rating int, p_comment text,
    p_tags text[], p_outcome text default null)
returns void language plpgsql security definer set search_path = '' as $$
declare
    b public.bookings;
begin
    select * into b from public.bookings where id = p_booking and customer_id = auth.uid() for update;
    if not found then raise exception 'Pesanan bukan milik Anda' using errcode = '42501'; end if;
    if p_rating is null or p_rating not between 1 and 5 or length(coalesce(p_comment, '')) > 300
      or coalesce(cardinality(p_tags), 0) > 4
      or exists (select 1 from unnest(p_tags) tag where tag is null or length(tag) > 40) then
        raise exception 'Rating atau ulasan tidak valid' using errcode = '22023';
    end if;
    if p_outcome = 'DIBATALKAN' and b.status in ('PENDING', 'DIKONFIRMASI') then
        if b.status = 'PENDING' and b.expires_at <= now() then
            raise exception 'Pesanan sudah kedaluwarsa' using errcode = '22023';
        end if;
        perform public.update_booking_status(b.id, 'DIBATALKAN_PELANGGAN');
        select * into b from public.bookings where id = p_booking;
    end if;
    if not coalesce(
       (b.status = 'SELESAI' and (p_outcome is null or p_outcome in ('SELESAI_TEPAT_WAKTU', 'TERLAMBAT')))
       or (b.status in ('DIBATALKAN_MITRA', 'DIBATALKAN_PELANGGAN') and (p_outcome is null or p_outcome = 'DIBATALKAN')),
       false
    ) then
        raise exception 'Hasil penyelesaian tidak sesuai dengan status pesanan' using errcode = '22023';
    end if;
    if p_outcome is not null and b.completion_outcome is distinct from p_outcome then
        update public.bookings set completion_outcome = p_outcome where id = b.id;
    end if;
    insert into public.reviews (booking_id, customer_id, provider_id, rating, comment, tags)
    values (b.id, b.customer_id, b.provider_id, p_rating, nullif(btrim(p_comment), ''), coalesce(p_tags, '{}'))
    on conflict (booking_id) do update set rating = excluded.rating, comment = excluded.comment, tags = excluded.tags
    where (reviews.rating, reviews.comment, reviews.tags) is distinct from (excluded.rating, excluded.comment, excluded.tags);
end $$;

create function public.delete_booking_review(p_booking uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
    perform 1 from public.bookings where id = p_booking and customer_id = auth.uid() for update;
    if not found then raise exception 'Pesanan bukan milik Anda' using errcode = '42501'; end if;
    delete from public.reviews where booking_id = p_booking and customer_id = auth.uid();
end $$;

revoke execute on function public.refresh_provider_trust(uuid), public.sync_trust_after_write() from public, anon, authenticated;
revoke execute on function public.save_booking_review(uuid, int, text, text[], text),
    public.delete_booking_review(uuid) from public, anon;
grant execute on function public.save_booking_review(uuid, int, text, text[], text),
    public.delete_booking_review(uuid) to authenticated;
revoke insert, update, delete on public.reviews from anon, authenticated;

do $$
declare provider record;
begin
    for provider in select user_id from public.provider_profiles order by user_id loop
        perform public.refresh_provider_trust(provider.user_id);
    end loop;
end $$;

do $$
begin
    if exists (select 1 from pg_publication where pubname = 'supabase_realtime')
       and not exists (select 1 from pg_publication_tables
           where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'provider_profiles') then
        alter publication supabase_realtime add table public.provider_profiles;
    end if;
end $$;

commit;
