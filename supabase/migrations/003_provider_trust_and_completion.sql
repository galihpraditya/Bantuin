begin;

alter table public.provider_profiles
  alter column trust_score set default 0,
  alter column flag_anomaly set default true,
  alter column badge set default 'NONE';

alter table public.reviews add column tags text[] not null default '{}';
-- The old score used a different formula. Keep its snapshots for historical records.
alter table public.provider_metric_snapshots add column formula_version int not null default 1;

create table public.booking_outcomes (
  booking_id uuid primary key references public.bookings(id) on delete cascade,
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  status text not null check (status in ('SELESAI_TEPAT_WAKTU', 'TERLAMBAT', 'DIBATALKAN')),
  resolved_at timestamptz not null,
  customer_confirmed boolean not null default false
);

create index bookings_provider_created_time_idx on public.bookings (provider_id, created_at desc);
create index booking_outcomes_provider_time_idx on public.booking_outcomes (provider_id, resolved_at desc);
create index reviews_provider_time_idx on public.reviews (provider_id, created_at desc);
create index booking_events_booking_time_idx on public.booking_events (booking_id, created_at, id);
create index provider_metric_snapshots_recent_idx
  on public.provider_metric_snapshots (provider_id, formula_version, computed_at desc);

alter table public.booking_outcomes enable row level security;
revoke all on public.booking_outcomes from public, anon, authenticated;
grant select on public.booking_outcomes to authenticated;
create policy booking_outcomes_participants on public.booking_outcomes for select to authenticated
  using (exists (
    select 1 from public.bookings b where b.id = booking_id
      and auth.uid() in (b.customer_id, b.provider_id)
  ));

-- Existing terminal bookings use the old 15-minute arrival tolerance when no
-- customer confirmation exists. Existing booking status and audit events stay intact.
insert into public.booking_outcomes (booking_id, provider_id, status, resolved_at)
select b.id, b.provider_id,
  case when b.status <> 'SELESAI' then 'DIBATALKAN'
       when b.started_at <= b.start_at + interval '15 minutes' then 'SELESAI_TEPAT_WAKTU'
       else 'TERLAMBAT' end,
  coalesce(b.finished_at, terminal.created_at, b.created_at)
from public.bookings b
left join lateral (
  select e.created_at from public.booking_events e
  where e.booking_id = b.id and e.status = b.status
  order by e.created_at desc, e.id desc limit 1
) terminal on true
where b.status in ('SELESAI', 'DIBATALKAN_PELANGGAN', 'DIBATALKAN_MITRA');

-- Public aggregates let recommendations see all relevant transactions without
-- making any customer's bookings or transaction history public.
create function public.provider_trust_inputs(p_providers uuid[], p_days int default 30)
returns table (
  provider_id uuid, on_time_count int, late_count int, cancelled_count int,
  rating_count int, rating_total bigint, other_count int
)
language sql volatile security definer set search_path = ''
as $$
  with time_window as materialized (
    select clock_timestamp() as upper_bound
  )
  select p.user_id, o.on_time_count, o.late_count, o.cancelled_count, r.rating_count, r.rating_total, b.other_count
  from public.provider_profiles p
  cross join time_window w
  cross join lateral (
    select
      count(*) filter (where status = 'SELESAI_TEPAT_WAKTU')::int as on_time_count,
      count(*) filter (where status = 'TERLAMBAT')::int as late_count,
      count(*) filter (where status = 'DIBATALKAN')::int as cancelled_count
    from public.booking_outcomes outcome
    where outcome.provider_id = p.user_id and outcome.resolved_at between
      w.upper_bound - (greatest(1, least(p_days, 30)) * interval '24 hours') and w.upper_bound
  ) o
  cross join lateral (
    select count(*)::int as rating_count, coalesce(sum(rating), 0) as rating_total
    from public.reviews review
    where review.provider_id = p.user_id and review.created_at between
      w.upper_bound - (greatest(1, least(p_days, 30)) * interval '24 hours') and w.upper_bound
  ) r
  cross join lateral (
    select count(*)::int as other_count
    from public.bookings booking
    where booking.provider_id = p.user_id and booking.created_at between
      w.upper_bound - (greatest(1, least(p_days, 30)) * interval '24 hours') and w.upper_bound
      and not exists (select 1 from public.booking_outcomes outcome where outcome.booking_id = booking.id)
  ) b
  where p.user_id = any(p_providers)
$$;

create function public.refresh_provider_trust_internal(p_provider uuid)
returns void language plpgsql security definer set search_path = ''
as $$
declare
  inputs record;
  completed int;
  total int;
  avg_rating numeric;
  rating_points numeric;
  sla numeric;
  cancel_ratio numeric;
  score numeric;
  rating_den numeric;
  sla_den numeric;
  cancel_den numeric;
  numerator numeric;
  denominator numeric;
  anomaly boolean;
begin
  -- Serialize cache updates and read the window after acquiring this lock.
  perform 1 from public.provider_profiles where user_id = p_provider for no key update;
  if not found then return; end if;

  select * into inputs from public.provider_trust_inputs(array[p_provider], 30);
  completed := inputs.on_time_count + inputs.late_count;
  total := completed + inputs.cancelled_count + inputs.other_count;
  avg_rating := inputs.rating_total::numeric / nullif(inputs.rating_count, 0);
  rating_points := coalesce(avg_rating / 5.0 * 100, 0);
  sla := coalesce(inputs.on_time_count * 100.0 / nullif(completed, 0), 0);
  cancel_ratio := coalesce(inputs.cancelled_count * 100.0 / nullif(total, 0), 0);
  -- Compare the weighted fractions before division, matching TrustScoreUseCase.
  rating_den := greatest(inputs.rating_count, 1);
  sla_den := greatest(completed, 1);
  cancel_den := greatest(total, 1);
  denominator := rating_den * sla_den * cancel_den;
  numerator := 9.0 * inputs.rating_total * sla_den * cancel_den
    + 35.0 * inputs.on_time_count * rating_den * cancel_den
    - 20.0 * inputs.cancelled_count * rating_den * sla_den;
  score := greatest(0, least(100, numerator / denominator));
  anomaly := numerator < 60 * denominator or 100.0 * inputs.cancelled_count > 20.0 * total;

  update public.provider_profiles set
    rating_avg = avg_rating,
    completion_rate = completed::numeric / nullif(total, 0),
    on_time_rate = sla,
    cancel_rate = cancel_ratio,
    trust_score = score,
    tx_count_30d = total,
    flag_anomaly = anomaly,
    badge = case when anomaly then 'NONE' else 'TRUSTED' end,
    metrics_updated_at = clock_timestamp()
  where user_id = p_provider;

  -- A booking and its review can affect the same provider in a single transaction.
  update public.provider_metric_snapshots set
    trust_score = score, rating_score = rating_points,
    on_time_rate = sla, cancel_rate = cancel_ratio, tx_count = total, flag_anomaly = anomaly
  where provider_id = p_provider and computed_at = now() and formula_version = 2;
  if not found then
    insert into public.provider_metric_snapshots
      (provider_id, computed_at, trust_score, rating_score, on_time_rate, cancel_rate, tx_count, flag_anomaly, formula_version)
    values (p_provider, now(), score, rating_points, sla, cancel_ratio, total, anomaly, 2);
  end if;
end
$$;

create function public.sync_booking_outcome()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
  if new.status in ('SELESAI', 'DIBATALKAN_PELANGGAN', 'DIBATALKAN_MITRA') then
    insert into public.booking_outcomes (booking_id, provider_id, status, resolved_at)
    values (new.id, new.provider_id,
      case when new.status <> 'SELESAI' then 'DIBATALKAN'
           when new.started_at <= new.start_at + interval '15 minutes' then 'SELESAI_TEPAT_WAKTU'
           else 'TERLAMBAT' end,
      coalesce(new.finished_at, now()))
    on conflict (booking_id) do nothing;
  elsif tg_op = 'INSERT' then
    perform public.refresh_provider_trust_internal(new.provider_id);
  end if;
  return new;
end
$$;

create trigger sync_booking_outcome
  after insert or update of status on public.bookings for each row execute function public.sync_booking_outcome();

create function public.refresh_trust_after_change()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
  if tg_op = 'DELETE' then
    perform public.refresh_provider_trust_internal(old.provider_id);
  else
    perform public.refresh_provider_trust_internal(new.provider_id);
  end if;
  return null;
end
$$;

create trigger refresh_trust_on_outcome
  after insert or update or delete on public.booking_outcomes
  for each row execute function public.refresh_trust_after_change();
create trigger refresh_trust_on_review
  after insert or update or delete on public.reviews
  for each row execute function public.refresh_trust_after_change();

create function public.save_booking_review(
  p_booking uuid, p_rating int, p_comment text, p_tags text[], p_outcome text default null
)
returns void language plpgsql security definer set search_path = ''
as $$
declare
  b public.bookings;
begin
  select * into b from public.bookings where id = p_booking and customer_id = auth.uid() for update;
  if not found then raise exception 'Pesanan tidak ditemukan atau bukan milik Anda' using errcode = '42501'; end if;
  if p_rating is null or p_rating not between 1 and 5 or length(coalesce(p_comment, '')) > 300
      or coalesce(cardinality(p_tags), 0) > 4
      or exists (select 1 from unnest(p_tags) t where t is null or length(t) > 40) then
    raise exception 'Rating atau ulasan tidak valid' using errcode = '22023';
  end if;

  if p_outcome is not null and p_outcome not in ('SELESAI_TEPAT_WAKTU', 'TERLAMBAT', 'DIBATALKAN') then
    raise exception 'Status penyelesaian tidak valid' using errcode = '22023';
  end if;
  if p_outcome = 'DIBATALKAN' and b.status in ('PENDING', 'DIKONFIRMASI', 'DALAM_PENGERJAAN') then
    if b.status = 'PENDING' and b.expires_at <= now() then
      raise exception 'Pesanan telah kedaluwarsa' using errcode = '22023';
    end if;
    update public.bookings set status = 'DIBATALKAN_PELANGGAN', finished_at = now() where id = b.id;
  elsif p_outcome in ('SELESAI_TEPAT_WAKTU', 'TERLAMBAT') and b.status = 'DALAM_PENGERJAAN' then
    update public.bookings set status = 'SELESAI', finished_at = now() where id = b.id;
  elsif (p_outcome is null and b.status in ('SELESAI', 'DIBATALKAN_PELANGGAN', 'DIBATALKAN_MITRA'))
    or (p_outcome = 'DIBATALKAN' and b.status in ('DIBATALKAN_PELANGGAN', 'DIBATALKAN_MITRA'))
    or (p_outcome in ('SELESAI_TEPAT_WAKTU', 'TERLAMBAT') and b.status = 'SELESAI') then
    null;
  else
    raise exception 'Status pesanan tidak mendukung konfirmasi ini' using errcode = '22023';
  end if;

  if p_outcome is not null then
    insert into public.booking_outcomes (booking_id, provider_id, status, resolved_at, customer_confirmed)
    values (b.id, b.provider_id, p_outcome, coalesce(b.finished_at, now()), true)
    on conflict (booking_id) do update
      set status = excluded.status, customer_confirmed = true
      where (booking_outcomes.status, booking_outcomes.customer_confirmed)
        is distinct from (excluded.status, true);
  end if;

  insert into public.reviews (booking_id, customer_id, provider_id, rating, comment, tags)
  values (b.id, b.customer_id, b.provider_id, p_rating, nullif(btrim(p_comment), ''), coalesce(p_tags, '{}'))
  on conflict (booking_id) do update
    set rating = excluded.rating, comment = excluded.comment, tags = excluded.tags
    where (reviews.rating, reviews.comment, reviews.tags)
      is distinct from (excluded.rating, excluded.comment, excluded.tags);
end
$$;

create function public.delete_booking_review(p_booking uuid)
returns void language plpgsql security definer set search_path = ''
as $$
begin
  perform 1 from public.bookings where id = p_booking and customer_id = auth.uid() for update;
  if not found then raise exception 'Pesanan tidak ditemukan atau bukan milik Anda' using errcode = '42501'; end if;
  delete from public.reviews where booking_id = p_booking and customer_id = auth.uid();
end
$$;

create function public.delete_booking_outcome(p_booking uuid)
returns void language plpgsql security definer set search_path = ''
as $$
declare
  b public.bookings;
begin
  select * into b from public.bookings where id = p_booking and customer_id = auth.uid() for update;
  if not found then raise exception 'Pesanan tidak ditemukan atau bukan milik Anda' using errcode = '42501'; end if;
  -- Remove the customer's classification while keeping the actual booking outcome.
  update public.booking_outcomes set
    status = case when b.status <> 'SELESAI' then 'DIBATALKAN'
      when b.started_at <= b.start_at + interval '15 minutes' then 'SELESAI_TEPAT_WAKTU'
      else 'TERLAMBAT' end,
    customer_confirmed = false
  where booking_id = p_booking and customer_confirmed;
end
$$;

create function public.refresh_my_provider_trust()
returns void language plpgsql security definer set search_path = ''
as $$
begin
  if auth.uid() is null then raise exception 'Sesi diperlukan' using errcode = '42501'; end if;
  perform public.refresh_provider_trust_internal(auth.uid());
end
$$;

revoke execute on function public.refresh_provider_trust_internal(uuid),
  public.sync_booking_outcome(), public.refresh_trust_after_change()
  from public, anon, authenticated;
revoke execute on function public.provider_trust_inputs(uuid[], int),
  public.save_booking_review(uuid, int, text, text[], text), public.delete_booking_review(uuid),
  public.delete_booking_outcome(uuid), public.refresh_my_provider_trust() from public, anon, authenticated;
grant execute on function public.provider_trust_inputs(uuid[], int) to anon, authenticated;
grant execute on function public.save_booking_review(uuid, int, text, text[], text),
  public.delete_booking_review(uuid), public.delete_booking_outcome(uuid),
  public.refresh_my_provider_trust() to authenticated;

-- Keep trust data server-owned while preserving existing profile editing.
revoke insert, update, delete on public.reviews from anon, authenticated;
revoke insert, update on public.provider_profiles from anon, authenticated;
grant insert (user_id, bio, address, lat, lng), update (bio, address, lat, lng)
  on public.provider_profiles to authenticated;
revoke execute on function public.apply_provider_metrics(uuid, numeric, numeric, numeric, numeric, numeric, int, boolean, text, numeric)
  from public, anon, authenticated;

do $$
declare provider uuid;
begin
  for provider in select user_id from public.provider_profiles loop
    perform public.refresh_provider_trust_internal(provider);
  end loop;
end
$$;

commit;
