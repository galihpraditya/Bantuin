# Bantuin

Bantuin adalah aplikasi Android native untuk menghubungkan pelanggan dengan penyedia jasa harian rumah tangga dan kos di sekitar Malang, meliputi **Teknisi Listrik**, **Teknisi AC & Elektronik**, **Tukang Bangunan**, **Jasa Kebersihan**, **Angkut Barang**, serta **Pipa & Pompa Air**. Aplikasi dibangun dengan Kotlin, Jetpack Compose, dan Material 3 serta mendukung Mode Terang, Mode Gelap, mode offline mandiri, dan backend cloud Supabase (Auth, PostgREST, Realtime, dan RPC PostgreSQL).

## Fitur & Algoritma Inti

Di layer domain, Bantuin menerapkan empat mesin logika utama beserta fitur pendukung:

1. **Matchmaking Mitra & Ekspansi Radius Dinamis (F1)**: Menyaring mitra aktif dalam radius default `10 km` menggunakan jarak Haversine (`BantuinConfig.MAX_RADIUS_KM = 10.0`), dengan opsi perluasan radius hingga `20 km` (`DynamicRadiusExpansionUseCase`) bila kandidat di radius awal masih minim. Pemeringkatan memadukan bobot jarak `50%`, rating rata-rata `35%`, dan rasio penyelesaian `15%`, dengan pengali penalti `0.7` jika mitra berstatus anomali. Pelanggan juga dapat mem-bookmark mitra favorit.
2. **Estimasi Tarif Dinamis (F2)**: Menghitung perkiraan biaya dari tarif dasar layanan ditambah ongkos jarak (`Rp 2.000/km`), pengali tingkat kesulitan (`1.00x` ringan, `1.25x` sedang, `1.50x` berat), serta tambahan tunggal `20%` untuk jam sibuk/malam (`20:00-06:00` WIB) atau layanan darurat, lalu dibulatkan ke atas ke kelipatan `Rp 500`.
3. **Deteksi Konflik Jadwal (F3)**: Memvalidasi ketersediaan hari mitra (`OFF`, `ON`, `ALWAYS_ON`), jeda perjalanan `30 menit` antar-pesanan, serta slot waktu yang sudah lewat. Sistem menawarkan hingga `3` opsi waktu alternatif jika bentrok. Pada sisi cloud (`006_remove_slot_lock.sql`), booking langsung memvalidasi jadwal melalui constraint `bookings_no_overlap` tanpa kewajiban kunci slot sementara, dan pesanan `PENDING` kedaluwarsa otomatis setelah `15 menit`.
4. **Skor Kepercayaan & Deteksi Anomali (F4)**: Mengevaluasi performa mitra dalam jendela `30 hari` terakhir dengan bobot rating `45%`, ketepatan waktu SLA `35%` (toleransi `15 menit`), dan reliabilitas bebas pembatalan `20%`. Mitra baru memperoleh skor awal `70` dengan badge `NEW` tanpa langsung ditandai anomali (`006_fix_new_provider_anomaly_default.sql`). Status anomali aktif bila skor `< 60` atau rasio pembatalan `> 20%`.
5. **Pengalaman Transaksi Lengkap**:
   - **Multi-Peran Tanpa Logout**: Beralih antara mode **Pelanggan** dan **Mitra** atau aktivasi akun mitra langsung dari menu Pengaturan.
   - **Pembayaran `CASH` & `QRIS` serta Dompet Mitra**: Pelacakan omzet tunai/non-tunai, riwayat transaksi, dan pengajuan pencairan saldo QRIS (`MIN_PAYOUT_AMOUNT = Rp 20.000`, `PAYOUT_ADMIN_FEE = Rp 2.500`) ke bank/e-wallet (BCA, BRI, Mandiri, DANA, GoPay, OVO).
   - **Chat Realtime & Konfirmasi Geser**: Percakapan terintegrasi per pesanan (`booking_messages`), notifikasi pelanggan, serta `SlideToConfirmBar` untuk penyelesaian pesanan.

## Lokasi, Ikonografi & Tema Visual

- **Lokasi GPS & Peta OSMDroid**: Mendukung deteksi GPS perangkat melalui `FusedLocationProviderClient` dan `Geocoder`, pemilihan titik interaktif langsung pada peta OSMDroid (tanpa API key Google Maps), serta preset area cepat di Malang.
- **Ikonografi & Tema Material 3**: Memakai set ikon kustom Compose (`BantuinServiceIcons`, `BantuinUiIcons`, `BantuinIllustrations`) bergaya *soft duotone* di atas wadah palet hangat seragam, pilihan tema (**Ikuti Sistem**, **Mode Terang**, **Mode Gelap**) dengan kontras terverifikasi WCAG AA, dan dukungan aksesibilitas *reduced motion*.

## Teknologi & Dependensi Utama

- Kotlin `2.2.10`, Android Gradle Plugin `9.3.2`, Gradle `9.5.0`
- Jetpack Compose (BOM `2026.02.01`), Material 3, Navigation Compose `2.8.4`
- Supabase-kt `3.1.1` (`postgrest-kt`, `auth-kt`, `realtime-kt`) + Ktor OkHttp `3.0.3`
- Play Services Location `21.3.0`, OSMDroid `6.1.18`, Coil Compose `2.7.0`
- Android SDK: `minSdk 26`, `targetSdk 35`, `compileSdk 37`, JDK `17`

## Struktur Direktori

- `app/src/main/java/id/bantuin/app/core`: Konstanta bisnis (`BantuinConfig`), penyimpanan sesi & preferensi (`SharedPreferencesSessionStore`, `ThemePreferenceStore`, `SettingsPreferenceStore`, `ChatReadStore`), `LocationHelper`, utilitas Rupiah/jarak/waktu WIB, dan `AppResult`.
- `app/src/main/java/id/bantuin/app/domain`: Model domain, kontrak repository, dan use case (`matchmaking`, `pricing`, `scheduling`, `trust`, `bookmark`, `wallet`).
- `app/src/main/java/id/bantuin/app/data`: Implementasi cloud (`SupabaseBantuinRepository`, DTO, `SupabaseClientProvider`) dan mode offline mandiri (`FakeBantuinRepository` & `FakeData`).
- `app/src/main/java/id/bantuin/app/ui`: Layar Jetpack Compose + ViewModel (`auth`, `customer`, `provider`, `chat`, `settings`), komponen bersama (`common/`), serta tema (`theme/`).
- `supabase/`: Skema tabel, fungsi RPC, trigger, kebijakan RLS (`migrations/`), dan data demo (`seed.sql`).

## Pengaturan Supabase & Build APK

1. Untuk mode **Supabase Cloud**, jalankan skrip SQL pada SQL Editor proyek Supabase secara berurutan dari folder `supabase/migrations/`:
   - `001_init.sql`
   - `002_fix_auth_and_grants.sql`
   - `003_provider_trust_and_completion.sql`
   - `003_security_hardening.sql`
   - `004_ux_chat_payment_wallet.sql`
   - `005_review_outcomes_and_trust.sql`
   - `006_fix_new_provider_anomaly_default.sql`
   - `006_remove_slot_lock.sql`
   - `007_chat_permissions.sql`
   - `supabase/seed.sql` (opsional, untuk data & akun demo)
2. Tambahkan kredensial pada `local.properties` (lihat contoh di [.env.example](.env.example), jangan commit `local.properties` ke Git):
   ```properties
   SUPABASE_URL=https://<project-id>.supabase.co
   SUPABASE_ANON_KEY=<your-anon-key>
   ```
   Jika `SUPABASE_URL` dan `SUPABASE_ANON_KEY` tidak diatur, aplikasi otomatis berjalan dalam mode offline mandiri memakai `FakeBantuinRepository`.
3. Bangun APK debug atau release:
   ```bash
   # Linux / macOS
   ./gradlew assembleDebug
   ./gradlew assembleRelease

   # Windows PowerShell
   .\gradlew.bat assembleDebug
   ```
   Hasil APK debug tersimpan di `app/build/outputs/apk/debug/app-debug.apk`.

## Pengujian & Pemeriksaan Kualitas

Jalankan seluruh unit test dan lint dengan:

```bash
# Linux / macOS
./gradlew testDebugUnitTest lintDebug --console=plain

# Windows PowerShell
.\gradlew.bat testDebugUnitTest lintDebug --console=plain
```

Laporan hasil unit test tersedia di `app/build/reports/tests/testDebugUnitTest/index.html`.

## Akun Demo Bawaan

Kata sandi demo untuk seluruh akun berikut adalah `Demo12345!`:
- **Pelanggan**: `pelanggan1@bantuin.test`, `pelanggan2@bantuin.test`
- **Mitra**: `mitra01@bantuin.test` sampai `mitra15@bantuin.test`
