begin;

-- Perbaiki default provider_profiles agar mitra baru tidak langsung terkena flag Anomali Akun
alter table public.provider_profiles
    alter column trust_score set default 70,
    alter column flag_anomaly set default false,
    alter column badge set default 'NEW';

create or replace function public.refresh_provider_trust(p_provider uuid)
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
    next_badge text;
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

    if total = 0 then
        avg_rating := null;
        rating_points := 0;
        sla := 0;
        cancellation := 0;
        score := 70;
        flagged := false;
        next_badge := 'NEW';
    elsif review_count = 0 then
        -- Mitra baru punya transaksi tetapi belum pernah dirating user:
        -- hanya tandai anomali bila rasio pembatalan > 20%.
        avg_rating := null;
        rating_points := 0;
        sla := coalesce(punctual * 100.0 / nullif(finished, 0), 0);
        cancellation := coalesce(cancelled * 100.0 / nullif(total, 0), 0);
        score := 70;
        flagged := 5::bigint * cancelled > total;
        next_badge := case when flagged then 'NONE' else 'NEW' end;
    else
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
        next_badge := case when flagged then 'NONE' else 'TRUSTED' end;
    end if;

    update public.provider_profiles set rating_avg = avg_rating,
        completion_rate = finished::numeric / nullif(finished + cancelled, 0), on_time_rate = sla, cancel_rate = cancellation,
        trust_score = score, tx_count_30d = total, flag_anomaly = flagged,
        badge = next_badge, metrics_updated_at = as_of
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

-- Backfill mitra baru / belum memiliki ulasan yang sebelumnya salah ter-flag anomali
update public.provider_profiles
set trust_score = 70,
    flag_anomaly = false,
    badge = 'NEW'
where rating_avg is null
  and coalesce(cancel_rate, 0) <= 20
  and flag_anomaly = true;

commit;
