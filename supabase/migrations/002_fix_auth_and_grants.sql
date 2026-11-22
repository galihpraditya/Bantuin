-- ============================================================================
-- BANTUIN - PERBAIKAN LOGIN & HAK AKSES (Fase 2 hotfix)
-- ============================================================================
-- Jalankan file ini di SQL Editor Supabase bila migrasi/seed sebelumnya sudah
-- pernah dijalankan. File ini aman dijalankan berulang kali (idempotent).

-- 1. Perbaiki baris auth.users yang disisipkan manual oleh seed.sql.
--    GoTrue membaca kolom-kolom token sebagai string sehingga NULL menyebabkan
--    error "Database error querying schema" saat login.
update auth.users
set
  instance_id = '00000000-0000-0000-0000-000000000000',
  confirmation_token = '',
  recovery_token = '',
  email_change_token_new = '',
  email_change = '',
  email_change_token_current = '',
  phone_change = '',
  phone_change_token = '',
  reauthentication_token = ''
where email like '%@bantuin.test';

-- 2. Pastikan setiap akun demo memiliki identitas email.
insert into auth.identities (
  id, provider_id, user_id, identity_data, provider,
  last_sign_in_at, created_at, updated_at
)
select
  gen_random_uuid(),
  u.id::text,
  u.id,
  jsonb_build_object(
    'sub', u.id::text,
    'email', u.email,
    'email_verified', true
  ),
  'email',
  now(), now(), now()
from auth.users u
where u.email like '%@bantuin.test'
  and not exists (
    select 1 from auth.identities i
    where i.user_id = u.id and i.provider = 'email'
  );

-- 3. Memberikan hak akses tabel. RLS tetap berlaku, tetapi privileges dasar
--    harus diberikan agar PostgREST tidak mengembalikan "permission denied".
grant usage on schema public to anon, authenticated;

grant select on public.service_categories, public.profiles,
  public.provider_profiles, public.services, public.availability,
  public.reviews to anon, authenticated;

grant select on public.bookings, public.slot_locks,
  public.booking_events, public.provider_metric_snapshots to authenticated;

grant update on public.profiles to authenticated;
grant insert, update, delete on public.services to authenticated;
grant insert, update, delete on public.provider_profiles to authenticated;
grant insert, update, delete on public.availability to authenticated;
grant insert on public.reviews to authenticated;

grant usage, select on all sequences in schema public to anon, authenticated;
