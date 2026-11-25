# Bantuin

Bantuin adalah aplikasi Android native untuk menghubungkan pelanggan dengan penyedia jasa harian seperti kelistrikan, AC dan elektronik, pipa dan sanitasi, kebersihan kos, angkut barang, serta les privat. Proyek ini dibangun menggunakan Kotlin, Jetpack Compose, dan Material 3 dengan dukungan Mode Terang, Mode Gelap, serta backend Supabase (PostgREST, Auth, Realtime, dan RPC PostgreSQL).

## Fitur dan algoritma inti

Aplikasi mengimplementasikan empat logika perhitungan di layer domain:

1. **Matchmaking mitra (F1)**: Menyaring mitra aktif dalam radius `10 km` dari lokasi pelanggan menggunakan jarak Haversine (`BantuinConfig.MAX_RADIUS_KM = 10.0`). Skor akhir memadukan bobot jarak `50%`, rating rata-rata `35%`, dan rasio penyelesaian `15%`, dengan faktor pengali penalti `0.7` bila mitra berstatus anomali.
2. **Estimasi tarif dinamis (F2)**: Menghitung perkiraan ongkos dari tarif dasar layanan ditambah biaya jarak (`Rp 2.000` per km). Total disesuaikan dengan pengali kompleksitas (`1.00x` ringan, `1.25x` sedang, `1.50x` berat) serta tambahan tunggal `20%` pada jam sibuk, jam malam, atau layanan darurat, lalu dibulatkan ke atas ke kelipatan `Rp 500`.
3. **Deteksi konflik jadwal (F3)**: Memvalidasi ketersediaan mitra (`OFF`, `ON`, `ALWAYS_ON`), jeda perjalanan `30 menit` antar-pesanan, dan waktu yang sudah lewat. Sistem menawarkan hingga `3` opsi waktu alternatif dan mengunci slot pilihan selama `10 menit`.
4. **Skor kepercayaan mitra (F4)**: Menghitung performa mitra pada jendela `30 hari` terakhir dengan bobot rating `45%`, ketepatan waktu SLA `35%` (toleransi `15 menit`), dan reliabilitas bebas pembatalan `20%`. Mitra baru dengan kurang dari `5` transaksi mendapat skor awal `70`. Status anomali aktif bila skor `< 60` atau rasio pembatalan `> 20%`.

## Dukungan lokasi dan tema visual

- **Lokasi & Peta**: Mendukung deteksi GPS perangkat melalui `FusedLocationProviderClient` dan `Geocoder`, pemilihan titik interaktif langsung pada peta OSMDroid (tanpa API key Google Maps), serta area cepat di Malang.
- **Mode Terang & Mode Gelap**: Tersedia pengaturan tema (**Ikuti Sistem**, **Mode Terang**, **Mode Gelap**) pada layar Profil Pelanggan maupun Dashboard Mitra dengan palet kontras terverifikasi WCAG AA.

## Struktur direktori

- `app/src/main/java/id/bantuin/app/core`: Konfigurasi konstanta bisnis (`BantuinConfig`), manajemen sesi (`SharedPreferencesSessionStore`), tema (`ThemePreferenceStore`), pembantu GPS (`LocationHelper`), format Rupiah, utilitas waktu WIB, dan `AppResult`.
- `app/src/main/java/id/bantuin/app/domain`: Model data domain, kontrak antarmuka repository, serta use case F1 hingga F4.
- `app/src/main/java/id/bantuin/app/data`: Implementasi cloud Supabase (`SupabaseBantuinRepository`, DTO, Ktor OkHttp WebSocket Realtime) serta mode mandiri offline (`FakeBantuinRepository` & `FakeData`).
- `app/src/main/java/id/bantuin/app/ui`: Layar Jetpack Compose untuk autentikasi, alur pemesanan pelanggan, portal mitra, navigasi terintegrasi, komponen status Loading/Empty/Error, serta tema Material 3.
- `supabase/`: Skema tabel, fungsi RPC keamanan, trigger akun baru, kebijakan RLS, dan data awal uji coba (`001_init.sql`, `002_fix_auth_and_grants.sql`, `003_security_hardening.sql`, `seed.sql`).
- `docs/`: Dokumentasi arsitektur ([DECISIONS.md](docs/DECISIONS.md)), skenario demo ([DEMO_SCENARIO_GUIDE.md](docs/DEMO_SCENARIO_GUIDE.md)), dukungan laporan ([LAPORAN_SKRIPSI_SUPPORT.md](docs/LAPORAN_SKRIPSI_SUPPORT.md)), dan acuan desain UI di `docs/design/`.

## Kebutuhan sistem

- Android Studio versi Ladybug (`2024.2.1`) atau yang lebih baru
- JDK 17 (atau JDK 21)
- Android SDK `minSdk 26`, `targetSdk 35`, `compileSdk 37`
- Gradle `9.5.0` melalui Gradle Wrapper

## Pengaturan Supabase dan build APK

1. Untuk mode **Supabase Cloud**, jalankan skrip SQL di SQL Editor proyek Supabase Anda secara berurutan:
   - `supabase/migrations/001_init.sql`
   - `supabase/migrations/002_fix_auth_and_grants.sql`
   - `supabase/migrations/003_security_hardening.sql`
   - `supabase/seed.sql` (opsional, untuk menanamkan akun & riwayat demo)
2. Tambahkan kredensial pada `local.properties` (jangan commit ke Git):
   ```properties
   SUPABASE_URL=https://<project-id>.supabase.co
   SUPABASE_ANON_KEY=<your-anon-key>
   ```
   Jika `SUPABASE_URL` dan `SUPABASE_ANON_KEY` tidak diatur, aplikasi berjalan dalam mode offline mandiri memakai `FakeBantuinRepository`.
3. Kompilasi APK debug atau release:
   ```bash
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```
   Di Windows PowerShell, gunakan `.\gradlew.bat assembleDebug`. Hasil APK debug tersimpan di `app/build/outputs/apk/debug/app-debug.apk`.

## Pengujian dan pemeriksaan kualitas

Jalankan seluruh unit test, lint, dan kompilasi dengan:

```bash
./gradlew testDebugUnitTest lintDebug --console=plain
```

Laporan unit test tersimpan di `app/build/reports/tests/testDebugUnitTest/index.html`.

## Akun demo bawaan

Kata sandi demo untuk seluruh akun berikut adalah `Demo12345!`:
- Pelanggan: `pelanggan1@bantuin.test`, `pelanggan2@bantuin.test`
- Mitra: `mitra01@bantuin.test` sampai `mitra15@bantuin.test`
