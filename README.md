# Bantuin

Bantuin adalah aplikasi Android native untuk menghubungkan pelanggan dengan penyedia jasa harian seperti perbaikan AC, pipa, kelistrikan, elektronik, pertukangan, dan pembersihan rumah. Proyek ini dibangun menggunakan Kotlin, Jetpack Compose, dan Material 3 dengan pendekatan Clean Architecture.

## Algoritma inti

Aplikasi ini mengimplementasikan empat logika perhitungan utama di layer domain:

1. Matchmaking mitra (F1)
   Menyaring mitra aktif dalam radius 15 km dari lokasi pelanggan menggunakan rumus jarak Haversine. Skor akhir dihitung dari kombinasi bobot jarak (50%), rating rata-rata (35%), dan rasio penyelesaian pesanan (15%). Mitra yang terdeteksi anomali mendapat pengali penalti 0.7.

2. Estimasi tarif dinamis (F2)
   Menghitung perkiraan ongkos dari tarif dasar layanan ditambah biaya jarak (Rp 2.000 per km). Total disesuaikan dengan pengali tingkat kesulitan kerja (1.0x untuk ringan, 1.25x untuk standar, dan 1.5x untuk berat) serta tambahan 20% saat jam sibuk atau malam hari. Hasil akhir dibulatkan ke atas ke kelipatan Rp 500 terdekat.

3. Deteksi konflik jadwal (F3)
   Mengecek ketersediaan mitra pada tanggal dan jam yang dipilih pelanggan. Sistem memperhitungkan jam operasional mitra, durasi pengerjaan, dan jeda perjalanan 30 menit antar-pesanan. Jika terjadi bentrok, sistem menyediakan hingga 3 rekomendasi waktu alternatif dan mengunci slot pilihan selama 10 menit saat pemesanan berlangsung.

4. Skor kepercayaan mitra (F4)
   Menilai performa mitra berdasarkan data 30 hari terakhir dengan bobot rating (45%), ketepatan waktu SLA (35%), dan tingkat penyelesaian (20%). Mitra baru dengan riwayat kurang dari 5 pesanan otomatis mendapat skor awal 70. Status anomali ditandai jika skor berada di bawah 60 atau rasio pembatalan melebihi 20%.

## Struktur direktori

Kode sumber dibagi ke dalam empat layer:

- `app/src/main/java/id/bantuin/app/core`: Konstanta aturan bisnis, penanganan format mata uang Rupiah, manipulasi waktu WIB, dan coroutine dispatchers.
- `app/src/main/java/id/bantuin/app/domain`: Model data murni, antarmuka repository, dan implementasi logika bisnis tanpa ketergantungan pada Android framework.
- `app/src/main/java/id/bantuin/app/data`: Penyedia data lokal dan implementasi in-memory repository untuk menjalankan aplikasi secara mandiri tanpa backend luar.
- `app/src/main/java/id/bantuin/app/ui`: Komponen antarmuka pengguna berbasis Jetpack Compose, terbagi untuk alur pelanggan, portal mitra, navigasi, dan tema visual.

## Kebutuhan sistem

- Android Studio Ladybug (2024.2.1) atau versi lebih baru
- JDK 17 atau JDK 21
- Android SDK dengan `minSdk` 26 (Android 8.0) dan `compileSdk` 37
- Gradle 9.3.2 (sudah disertakan lewat Gradle Wrapper)

## Menjalankan aplikasi

Untuk mengompilasi dan menghasilkan berkas APK debug, jalankan perintah berikut di terminal:

```bash
./gradlew assembleDebug
```

Di lingkungan Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Berkas APK akan tersimpan di:
`app/build/outputs/apk/debug/app-debug.apk`

Untuk langsung memasang aplikasi ke perangkat fisik atau emulator yang aktif:

```bash
./gradlew installDebug
```

## Pengujian unit

Seluruh uji logika bisnis F1 hingga F4 mencakup skenario perhitungan dari spesifikasi teknis. Jalankan pengujian dengan:

```bash
./gradlew testDebugUnitTest
```

Laporan hasil pengujian dalam format HTML dapat dilihat di:
`app/build/reports/tests/testDebugUnitTest/index.html`

## Autentikasi dan akun demo

Aplikasi membuka layar login saat belum ada sesi. Kredensial yang salah akan ditolak, dan pengguna akan masuk ke profil sesuai akun yang digunakan (bukan akun demo bawaan).

Akun demo (kata sandi `Demo12345!`):

- Pelanggan: `pelanggan1@bantuin.test`, `pelanggan2@bantuin.test`
- Mitra: `mitra01@bantuin.test` … `mitra15@bantuin.test`

Saat Supabase dikonfigurasi, login memakai Supabase Auth. Jalankan `supabase/migrations/001_init.sql`, lalu `supabase/seed.sql`. Untuk proyek yang sudah menjalankan seed versi lama, jalankan juga `supabase/migrations/002_fix_auth_and_grants.sql` agar akun demo dapat login dan hak akses tabel sesuai RLS. Tanpa Supabase, aplikasi memakai repository in-memory dengan akun demo yang sama.

## Catatan penggunaan demo

Aplikasi ini sudah dilengkapi data awal 15 profil mitra di wilayah Malang (berpusat di sekitar Alun-alun Kota Malang) yang mencakup berbagai kategori layanan, kondisi jadwal sibuk, dan variasi skor metrik. Pengguna dapat berganti peran antara Pelanggan dan Mitra melalui profil/beranda selama akun memiliki peran tersebut.
