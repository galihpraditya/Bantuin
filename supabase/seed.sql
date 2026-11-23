-- ============================================================================
-- BANTUIN DATABASE SEED DATA (Fase 2)
-- Sesuai Dokumen Spesifikasi PRD.md Bagian 9 & 13
-- 6 Kategori, 2 Pelanggan Demo, 15 Mitra Demo di Malang (−7.9797, 112.6304),
-- Katalog Layanan, Jadwal Ketersediaan, dan Ulasan Historis.
-- Password demo untuk semua akun: Demo12345!
-- ============================================================================

-- ===== 1. Kategori Layanan =====
insert into public.service_categories (id, name, icon, default_duration_min)
values
  (1, 'Teknisi Listrik', 'bolt', 60),
  (2, 'Teknisi AC & Elektronik', 'ac_unit', 90),
  (3, 'Tukang Bangunan', 'construction', 180),
  (4, 'Jasa Kebersihan', 'cleaning_services', 120),
  (5, 'Angkut Barang', 'local_shipping', 120),
  (6, 'Pengajar Privat', 'school', 90)
on conflict (id) do update set
  name = excluded.name,
  icon = excluded.icon,
  default_duration_min = excluded.default_duration_min;

-- Reset sequence service_categories_id_seq
select setval('public.service_categories_id_seq', (select max(id) from public.service_categories));

-- ===== 2. Akun Auth Demo (auth.users) =====
-- Catatan: Password terenkripsi bcrypt untuk 'Demo12345!'
insert into auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, created_at, updated_at)
values
  -- Pelanggan Demo
  ('c0000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'user@bantuin.test', crypt('Demo123!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Rani Wijaya","phone":"081234567890","roles":["customer"]}', now(), now()),
  ('c0000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'pelanggan1@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Rani Wijaya","phone":"081234567890","roles":["customer"]}', now(), now()),
  ('c0000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'pelanggan2@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Dimas Arya","phone":"081234567891","roles":["customer"]}', now(), now()),
  -- Mitra Demo
  ('00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra@bantuin.test', crypt('Demo123!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Budi Santoso, S.T.","phone":"081200000001","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra01@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Budi Santoso, S.T.","phone":"081200000001","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra02@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Ahmad Fauzi","phone":"081200000002","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra03@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Surya Citra Teknik","phone":"081200000003","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra04@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Pak Sugeng W.","phone":"081200000004","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra05@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Joko Hendro","phone":"081200000005","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra06@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Rudi Hartono","phone":"081200000006","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra07@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Rian Pratama","phone":"081200000007","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra08@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Pak Slamet Renovasi","phone":"081200000008","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'mitra09@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Tukang Siaga 24 Jam","phone":"081200000009","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000010', 'authenticated', 'authenticated', 'mitra10@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Bu Siti Deep Clean","phone":"081200000010","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000011', 'authenticated', 'authenticated', 'mitra11@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Klin Kos Ceria","phone":"081200000011","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000012', 'authenticated', 'authenticated', 'mitra12@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Dimas Pick-up Angkut","phone":"081200000012","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000013', 'authenticated', 'authenticated', 'mitra13@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Anisa Rahmawati, S.Si.","phone":"081200000013","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000014', 'authenticated', 'authenticated', 'mitra14@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Teknisi Kepanjen Mandiri","phone":"081200000014","roles":["provider"]}', now(), now()),
  ('00000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000015', 'authenticated', 'authenticated', 'mitra15@bantuin.test', crypt('Demo12345!', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}', '{"full_name":"Bambang Libur Service","phone":"081200000015","roles":["provider"]}', now(), now())
on conflict (id) do nothing;

-- GoTrue membaca kolom token sebagai string (bukan nullable). Baris auth.users yang
-- disisipkan manual tanpa nilai token menyebabkan login gagal dengan
-- "Database error querying schema". Normalkan menjadi string kosong.
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

-- Identitas email diperlukan agar GoTrue dapat mencocokkan akun saat login.
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

-- ===== 3. Data Profil Pengguna (public.profiles) =====
insert into public.profiles (id, full_name, phone, roles)
values
  ('c0000000-0000-0000-0000-000000000001', 'Rani Wijaya', '081234567890', array['customer']),
  ('c0000000-0000-0000-0000-000000000002', 'Dimas Arya', '081234567891', array['customer']),
  ('00000000-0000-0000-0000-000000000001', 'Budi Santoso, S.T.', '081200000001', array['provider']),
  ('00000000-0000-0000-0000-000000000002', 'Ahmad Fauzi', '081200000002', array['provider']),
  ('00000000-0000-0000-0000-000000000003', 'Surya Citra Teknik', '081200000003', array['provider']),
  ('00000000-0000-0000-0000-000000000004', 'Pak Sugeng W.', '081200000004', array['provider']),
  ('00000000-0000-0000-0000-000000000005', 'Joko Hendro', '081200000005', array['provider']),
  ('00000000-0000-0000-0000-000000000006', 'Rudi Hartono', '081200000006', array['provider']),
  ('00000000-0000-0000-0000-000000000007', 'Rian Pratama', '081200000007', array['provider']),
  ('00000000-0000-0000-0000-000000000008', 'Pak Slamet Renovasi', '081200000008', array['provider']),
  ('00000000-0000-0000-0000-000000000009', 'Tukang Siaga 24 Jam', '081200000009', array['provider']),
  ('00000000-0000-0000-0000-000000000010', 'Bu Siti Deep Clean', '081200000010', array['provider']),
  ('00000000-0000-0000-0000-000000000011', 'Klin Kos Ceria', '081200000011', array['provider']),
  ('00000000-0000-0000-0000-000000000012', 'Dimas Pick-up Angkut', '081200000012', array['provider']),
  ('00000000-0000-0000-0000-000000000013', 'Anisa Rahmawati, S.Si.', '081200000013', array['provider']),
  ('00000000-0000-0000-0000-000000000014', 'Teknisi Kepanjen Mandiri', '081200000014', array['provider']),
  ('00000000-0000-0000-0000-000000000015', 'Bambang Libur Service', '081200000015', array['provider'])
on conflict (id) do update set
  full_name = excluded.full_name,
  phone = excluded.phone,
  roles = excluded.roles;

-- ===== 4. Profil Spesifik 15 Mitra (public.provider_profiles) =====
insert into public.provider_profiles (
  user_id, bio, address, lat, lng, is_suspended
)
values
  -- 1. Pak Budi Santoso (AC - Terpercaya, rujukan F1-F4)
  ('00000000-0000-0000-0000-000000000001', 'Teknisi AC Berlisensi BNSP dengan 7+ tahun pengalaman.', 'Jl. Kaliurang No. 14, Lowokwaru, Malang', -7.9650, 112.6320, false),
  -- 2. Ahmad Fauzi (AC - Terpercaya)
  ('00000000-0000-0000-0000-000000000002', 'Spesialis pendingin ruangan dan elektronik rumah tangga.', 'Jl. Soekarno Hatta No. 88, Malang', -7.9520, 112.6250, false),
  -- 3. Surya Citra Teknik (AC - Terpercaya)
  ('00000000-0000-0000-0000-000000000003', 'Solusi Cepat AC Split, Multi-split & Inverter.', 'Jl. Danau Toba G9/E12, Sawojajar, Malang', -7.9300, 112.6400, false),
  -- 4. Pak Sugeng W. (Listrik - Terpercaya)
  ('00000000-0000-0000-0000-000000000004', 'Spesialis Listrik Arus Kuat & Lemah, instalasi MCB rumah/kos.', 'Jl. Ijen No. 25, Klojen, Malang', -7.9750, 112.6310, false),
  -- 5. Joko Hendro (Listrik - Anomali Cancel > 20%)
  ('00000000-0000-0000-0000-000000000005', 'Perbaikan lampu dan kelistrikan darurat.', 'Jl. Bondowoso No. 4, Malang', -7.9700, 112.6200, false),
  -- 6. Rudi Hartono (Listrik - Anomali Trust Score < 60)
  ('00000000-0000-0000-0000-000000000006', 'Layanan instalasi listrik kos.', 'Jl. Sulfat No. 12, Blimbing, Malang', -7.9600, 112.6450, false),
  -- 7. Rian Pratama (Listrik - Mitra Baru)
  ('00000000-0000-0000-0000-000000000007', 'Teknisi muda bersertifikasi vokasi.', 'Jl. Kawi No. 10, Klojen, Malang', -7.9850, 112.6350, false),
  -- 8. Pak Slamet Renovasi (Bangunan - Terpercaya)
  ('00000000-0000-0000-0000-000000000008', 'Ahli perbaikan kebocoran atap dan instalasi kran sanitasi.', 'Jl. MT Haryono No. 50, Dinoyo, Malang', -7.9550, 112.6100, false),
  -- 9. Tukang Siaga 24 Jam (Bangunan - ALWAYS_ON)
  ('00000000-0000-0000-0000-000000000009', 'Siap meluncur 24 jam untuk kondisi kebocoran pipa darurat.', 'Jl. Veteran No. 1, Klojen, Malang', -7.9700, 112.6350, false),
  -- 10. Bu Siti Deep Clean (Kebersihan - Terpercaya)
  ('00000000-0000-0000-0000-000000000010', 'Pembersihan total kamar mandi berkerak dan kamar kos.', 'Jl. Gajayana No. 22, Lowokwaru, Malang', -7.9620, 112.6150, false),
  -- 11. Klin Kos Ceria (Kebersihan - Baru < 5 tx)
  ('00000000-0000-0000-0000-000000000011', 'Jasa kebersihan higienis untuk kos mahasiswa.', 'Jl. Semeru No. 3, Klojen, Malang', -7.9780, 112.6280, false),
  -- 12. Dimas Pick-up Angkut (Angkut - Terpercaya)
  ('00000000-0000-0000-0000-000000000012', 'Armada bersih, terpal aman, bantu angkat barang sampai kamar.', 'Jl. Tlogomas No. 15, Malang', -7.9400, 112.6200, false),
  -- 13. Anisa Rahmawati, S.Si. (Privat - Terpercaya)
  ('00000000-0000-0000-0000-000000000013', 'Tutor Sains & Matematika, alumni UB berprestasi.', 'Jl. Bandung No. 8, Klojen, Malang', -7.9720, 112.6350, false),
  -- 14. Teknisi Kepanjen Mandiri (Luar Radius 17 km)
  ('00000000-0000-0000-0000-000000000014', 'Layanan spesialis wilayah Malang Selatan.', 'Jl. Panji No. 80, Kepanjen, Malang', -8.1300, 112.5700, false),
  -- 15. Bambang Libur Service (Semua Hari OFF)
  ('00000000-0000-0000-0000-000000000015', 'Sedang cuti operasional sementara.', 'Jl. Merdeka Barat, Malang', -7.9780, 112.6300, false)
on conflict (user_id) do update set
  bio = excluded.bio,
  address = excluded.address,
  lat = excluded.lat,
  lng = excluded.lng,
  is_suspended = excluded.is_suspended;

-- ===== 5. Katalog Layanan Mitra (public.services) =====
insert into public.services (id, provider_id, category_id, name, description, base_fare, duration_min, is_active)
values
  -- Pak Budi (p01) - AC
  ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 2, 'Cuci AC Standar (0.5 - 1 PK)', 'Pembersihan filter, evaporator & blower indoor/outdoor', 65000, 90, true),
  ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 2, 'Cuci & Tambah Freon R32/R410', 'Cuci total beserta pengecekan tekanan dan isi freon', 120000, 120, true),
  ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 2, 'Bongkar Pasang Unit AC', 'Pemindahan unit AC indoor dan outdoor', 250000, 180, true),
  -- Ahmad Fauzi (p02) - AC
  ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000002', 2, 'Servis AC Sedang (1/2 - 1 PK)', 'Cuci total, perbaikan saluran bocor air & suara berisik', 60000, 90, true),
  -- Surya Citra Teknik (p03) - AC
  ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003', 2, 'Servis Rutin AC Inverter', 'Pembersihan mendalam kompresor inverter', 65000, 90, true),
  -- Pak Sugeng (p04) - Listrik
  ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000004', 1, 'Pemeriksaan Korsleting Listrik', 'Pelacakan jalur putus dan perbaikan MCB trip', 45000, 60, true),
  ('10000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000004', 1, 'Pasang Stop Kontak & Saklar Baru', 'Penambahan titik kabel dan saklar', 50000, 60, true),
  -- Joko Hendro (p05) - Listrik
  ('10000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000005', 1, 'Perbaikan Kelistrikan Rumah', 'Perbaikan saklar rusak dan pengecekan jalur', 40000, 60, true),
  -- Rudi Hartono (p06) - Listrik
  ('10000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000006', 1, 'Instalasi Listrik Kos', 'Pemasangan jalur lampu dan fiting', 45000, 60, true),
  -- Rian Pratama (p07) - Listrik
  ('10000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000007', 1, 'Perbaikan Lampu & Fitting', 'Ganti instalasi fiting dan lampu kos', 40000, 45, true),
  -- Pak Slamet (p08) - Bangunan
  ('10000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000008', 3, 'Perbaikan Pipa Bocor & Kran Air', 'Penggantian kran rusak & penambalan pipa saluran air', 80000, 120, true),
  -- Tukang Siaga (p09) - Bangunan
  ('10000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000009', 3, 'Darurat Bocor Atap Kos', 'Penambalan genteng dan talang darurat', 90000, 180, true),
  -- Bu Siti (p10) - Kebersihan
  ('10000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000010', 4, 'Deep Clean Kamar Mandi Kos', 'Pembersihan kerak keramik, kloset & dinding kamar mandi', 50000, 120, true),
  -- Klin Kos (p11) - Kebersihan
  ('10000000-0000-0000-0000-000000000014', '00000000-0000-0000-0000-000000000011', 4, 'Pembersihan Kamar Kos', 'Sapu, pel, lap perabot kamar kos', 45000, 90, true),
  -- Dimas (p12) - Angkut
  ('10000000-0000-0000-0000-000000000015', '00000000-0000-0000-0000-000000000012', 5, 'Angkut Pindahan Kos', 'Pick-up dan bantuan angkat barang kos hingga sampai tujuan', 100000, 120, true),
  -- Anisa (p13) - Privat
  ('10000000-0000-0000-0000-000000000016', '00000000-0000-0000-0000-000000000013', 6, 'Bimbingan Belajar Privat (90 mnt)', 'Bimbingan satu lawan satu mata kuliah / pelajaran sekolah', 60000, 90, true),
  -- Teknisi Kepanjen (p14)
  ('10000000-0000-0000-0000-000000000017', '00000000-0000-0000-0000-000000000014', 1, 'Servis Listrik Wilayah Selatan', 'Perbaikan kelistrikan Kepanjen dan sekitarnya', 70000, 90, true),
  -- Bambang (p15)
  ('10000000-0000-0000-0000-000000000018', '00000000-0000-0000-0000-000000000015', 2, 'Servis Kulkas & Freon', 'Pengecekan mesin kompresor kulkas', 50000, 60, true)
on conflict (id) do update set
  name = excluded.name,
  description = excluded.description,
  base_fare = excluded.base_fare,
  duration_min = excluded.duration_min,
  is_active = excluded.is_active;

-- ===== 6. Ketersediaan Jadwal 15 Mitra (public.availability) =====
-- Standar: Mitra 1-8, 10-14 buka Senin-Sabtu 07:00 - 21:00, Minggu OFF
insert into public.availability (provider_id, day_of_week, mode, open_time, close_time)
select
  p.user_id,
  d.day,
  case when d.day = 7 then 'OFF' else 'ON' end,
  case when d.day = 7 then null else '07:00:00'::time end,
  case when d.day = 7 then null else '21:00:00'::time end
from public.provider_profiles p
cross join (values (1),(2),(3),(4),(5),(6),(7)) as d(day)
where p.user_id not in ('00000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000015')
on conflict (provider_id, day_of_week) do update set
  mode = excluded.mode,
  open_time = excluded.open_time,
  close_time = excluded.close_time;

-- Mitra 9 (Tukang Siaga): ALWAYS_ON setiap hari (1-7)
insert into public.availability (provider_id, day_of_week, mode, open_time, close_time)
select
  '00000000-0000-0000-0000-000000000009',
  d.day,
  'ALWAYS_ON',
  null,
  null
from (values (1),(2),(3),(4),(5),(6),(7)) as d(day)
on conflict (provider_id, day_of_week) do update set
  mode = excluded.mode,
  open_time = excluded.open_time,
  close_time = excluded.close_time;

-- Mitra 15 (Bambang Libur): Semua hari OFF (1-7)
insert into public.availability (provider_id, day_of_week, mode, open_time, close_time)
select
  '00000000-0000-0000-0000-000000000015',
  d.day,
  'OFF',
  null,
  null
from (values (1),(2),(3),(4),(5),(6),(7)) as d(day)
on conflict (provider_id, day_of_week) do update set
  mode = excluded.mode,
  open_time = excluded.open_time,
  close_time = excluded.close_time;

-- ===== 7. Contoh Pesanan Historis (SELESAI) & Ulasan (public.bookings & reviews) =====
-- Pesanan 1 (Pak Budi - Rani Wijaya)
insert into public.bookings (
  id, customer_id, provider_id, service_id,
  start_at, end_at, complexity, is_urgent, distance_km,
  base_fare, base_cost, surge_applied, total_cost,
  customer_lat, customer_lng, notes, status,
  expires_at, started_at, finished_at, created_at
)
values (
  '20000000-0000-0000-0000-000000000001',
  'c0000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000001',
  now() - interval '2 days',
  now() - interval '2 days' + interval '2 hours',
  'SEDANG', false, 1.8,
  65000, 68600, false, 75500,
  -7.9797, 112.6304, 'AC kamar kos meneteskan air', 'SELESAI',
  now() - interval '2 days',
  now() - interval '2 days' + interval '5 minutes',
  now() - interval '2 days' + interval '110 minutes',
  now() - interval '2 days' - interval '1 hour'
)
on conflict (id) do nothing;

insert into public.reviews (id, booking_id, customer_id, provider_id, rating, comment, created_at)
values (
  '30000000-0000-0000-0000-000000000001',
  '20000000-0000-0000-0000-000000000001',
  'c0000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000001',
  5,
  'Pak Budi datang cepat sekali, AC langsung dingin dan kamar tetap bersih!',
  now() - interval '1 day'
)
on conflict (id) do nothing;

-- Pesanan 2 (Pak Budi - Dimas Arya)
insert into public.bookings (
  id, customer_id, provider_id, service_id,
  start_at, end_at, complexity, is_urgent, distance_km,
  base_fare, base_cost, surge_applied, total_cost,
  customer_lat, customer_lng, notes, status,
  expires_at, started_at, finished_at, created_at
)
values (
  '20000000-0000-0000-0000-000000000002',
  'c0000000-0000-0000-0000-000000000002',
  '00000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000001',
  now() - interval '4 days',
  now() - interval '4 days' + interval '2 hours',
  'RINGAN', false, 2.1,
  65000, 69200, false, 69500,
  -7.9750, 112.6350, 'Cuci berkala AC kamar kos', 'SELESAI',
  now() - interval '4 days',
  now() - interval '4 days' + interval '5 minutes',
  now() - interval '4 days' + interval '90 minutes',
  now() - interval '4 days' - interval '1 hour'
)
on conflict (id) do nothing;

insert into public.reviews (id, booking_id, customer_id, provider_id, rating, comment, created_at)
values (
  '30000000-0000-0000-0000-000000000002',
  '20000000-0000-0000-0000-000000000002',
  'c0000000-0000-0000-0000-000000000002',
  '00000000-0000-0000-0000-000000000001',
  5,
  'Teknisi sangat sopan, dijelaskan kenapa air AC menetes dan langsung tuntas.',
  now() - interval '3 days'
)
on conflict (id) do nothing;

-- ===== 8. Skenario F4 dari pesanan dan ulasan aktual =====
-- Sugeng (AMAN), Joko (rasio batal tinggi), Rudi (skor rendah dan rasio batal tinggi).
with demo(job, provider, service, days_ago, booking_status, delay_minutes) as (
  values
    (10, 4, 6, 1, 'SELESAI', 5),
    (11, 4, 6, 4, 'SELESAI', 5),
    (12, 5, 8, 1, 'SELESAI', 5),
    (13, 5, 8, 3, 'SELESAI', 5),
    (14, 5, 8, 5, 'SELESAI', 5),
    (15, 5, 8, 8, 'DIBATALKAN_MITRA', null),
    (16, 6, 9, 2, 'SELESAI', 25),
    (17, 6, 9, 4, 'SELESAI', 30),
    (18, 6, 9, 7, 'DIBATALKAN_PELANGGAN', null),
    (19, 6, 9, 9, 'DIBATALKAN_MITRA', null),
    (20, 1, 1, 0, 'DALAM_PENGERJAAN', 5)
), schedule as (
  select demo.*, now() - days_ago * interval '1 day' - interval '1 hour' as starts
  from demo
)
insert into public.bookings (
  id, customer_id, provider_id, service_id, start_at, end_at, complexity, is_urgent,
  distance_km, base_fare, base_cost, surge_applied, total_cost,
  customer_lat, customer_lng, status, expires_at, started_at, finished_at, created_at
)
select
  ('20000000-0000-0000-0000-' || lpad(job::text, 12, '0'))::uuid,
  'c0000000-0000-0000-0000-000000000001',
  ('00000000-0000-0000-0000-' || lpad(provider::text, 12, '0'))::uuid,
  s.id, starts, starts + interval '90 minutes', 'RINGAN', false,
  1.5, s.base_fare, s.base_fare + 3000, false, s.base_fare + 3000,
  -7.9797, 112.6304, booking_status, starts, starts + delay_minutes * interval '1 minute',
  case when booking_status <> 'DALAM_PENGERJAAN' then starts + interval '45 minutes' end,
  starts - interval '2 hours'
from schedule
join public.services s on s.id = ('10000000-0000-0000-0000-' || lpad(service::text, 12, '0'))::uuid
on conflict (id) do nothing;

with demo(job, rating, comment) as (
  values
    (10, 5, 'Datang sesuai jadwal dan instalasi rapi.'),
    (11, 5, 'Perbaikan selesai tepat waktu.'),
    (12, 5, 'Perbaikan tuntas dan rapi.'),
    (13, 5, 'Pelayanan baik dan cepat.'),
    (14, 5, 'Datang sesuai jadwal.'),
    (16, 2, 'Tiba jauh melebihi jadwal.'),
    (17, 2, 'Pengerjaan dimulai terlambat.')
)
insert into public.reviews (id, booking_id, customer_id, provider_id, rating, comment, created_at)
select
  ('30000000-0000-0000-0000-' || lpad(job::text, 12, '0'))::uuid,
  b.id, b.customer_id, b.provider_id, demo.rating, demo.comment, b.finished_at
from demo
join public.bookings b on b.id = ('20000000-0000-0000-0000-' || lpad(job::text, 12, '0'))::uuid
on conflict do nothing;
