# Panduan Skenario Demo Bantuin

Panduan ini disusun mengikuti skenario demo dan kriteria keberhasilan pada [PRD.md](../PRD.md) Bagian 14 agar seluruh alur F1 hingga F4, Realtime, dan pengaturan tema berjalan mulus saat presentasi maupun pengujian dua perangkat.

## 1. Persiapan Lingkungan

### Opsi A: Mode Offline Mandiri (Tanpa Supabase)
1. Biarkan `local.properties` hanya berisi `sdk.dir=...` tanpa `SUPABASE_URL` dan `SUPABASE_ANON_KEY`.
2. Aplikasi otomatis memakai `FakeBantuinRepository` dengan 15 profil mitra demo di Malang dan riwayat pesanan awal.

### Opsi B: Mode Cloud Supabase (Dua Perangkat / Realtime)
1. Jalankan skrip SQL berikut secara berurutan di Supabase SQL Editor:
   - `supabase/migrations/001_init.sql`
   - `supabase/migrations/002_fix_auth_and_grants.sql`
   - `supabase/migrations/003_security_hardening.sql`
   - `supabase/seed.sql` (opsional, untuk data 15 mitra dan riwayat demo)
2. Salin `SUPABASE_URL` dan `SUPABASE_ANON_KEY` (publishable/anon key saja, jangan `service_role`) ke `local.properties`.
3. Bangun dan pasang APK ke perangkat uji via `./gradlew installDebug`.

### Akun Demo Standar
- **Pelanggan 1**: `pelanggan1@bantuin.test` (kata sandi: `Demo12345!`)
- **Pelanggan 2**: `pelanggan2@bantuin.test` (kata sandi: `Demo12345!`)
- **Mitra 1 (Terpercaya, jadwal padat)**: `mitra01@bantuin.test` (kata sandi: `Demo12345!`)
- **Mitra lain**: `mitra02@bantuin.test` sampai `mitra15@bantuin.test` (kata sandi: `Demo12345!`)

---

## 2. Langkah Skenario Demo Inti

### Skenario 1 (F1): Smart Service Matchmaking & Filter
1. Masuk dengan akun `pelanggan1@bantuin.test`.
2. Di layar **Beranda**, ketuk **Ubah** pada kartu lokasi untuk mencoba **Gunakan Lokasi GPS Saya**, mengetuk titik peta OSMDroid, atau memilih preset area Malang (Alun-alun, UB/Suhat, Sawojajar, Dinoyo).
3. Ketuk kategori **AC & Elektronik** (kategori 2) atau cari layanan di bilah pencarian.
4. Di layar **Rekomendasi Mitra**, tunjukkan:
   - Daftar mitra diurutkan otomatis berdasarkan skor gabungan (`50% Jarak + 35% Rating + 15% Penyelesaian`).
   - Mitra berstatus **Anomali Akun** memiliki faktor pengali `0.7x` sehingga turun peringkat.
   - Mitra di luar radius `10 km` tidak ikut daftar dan diringkas di kartu informasi bagian bawah.
   - Coba aktifkan filter `Jarak <= 5km`, `Rating 4.8+`, dan `Mitra Terpercaya`.

### Skenario 2 (F3): Deteksi Konflik Jadwal & Kunci Slot 10 Menit
1. Pilih mitra dengan jadwal terisi (misalnya `Budi Santoso, S.T.`) lalu pilih salah satu layanan (misalnya `Cuci & Servis AC Split`).
2. Pada layar **Pilih Jadwal Servis**:
   - Pilih slot jam yang bertabrakan dengan pesanan/jeda perjalanan 30 menit (misalnya `10:00 WIB`).
   - Aplikasi menampilkan kartu **Slot Tidak Tersedia** beserta hingga 3 tombol rekomendasi jam bebas konflik.
3. Ketuk salah satu slot alternatif yang valid, lalu tekan **Lanjut ke Estimasi Biaya**.
4. Sistem memanggil `acquireLock(...)` dan mengunci slot selama `10 menit`. Kartu hitung mundur `10:00` tampil di bagian atas layar estimasi. Jika pengguna kedua memilih slot yang sama pada jendela kunci ini, server menolak dengan pesan `"Slot baru saja diambil, pilih jam lain"`.
5. Catatan pengujian lepas kunci: tekan tombol kembali di layar estimasi untuk memverifikasi `releaseLock` melepaskan slot kembali.

### Skenario 3 (F2): Estimasi Tarif Dinamis & Penambahan Surge
1. Pada layar **Estimasi Tarif & Order**:
   - Bandingkan pilihan tingkat kompleksitas (`Ringan 1.00x`, `Sedang 1.25x`, `Berat 1.50x`). Perhatikan rincian tarif dasar, biaya jarak (`jarakKm x Rp2.000`), selisih kompleksitas, dan total pembayaran berubah seketika.
   - Aktifkan sakelar **Layanan Cepat Tanggap** (atau pilih jadwal pada jam sibuk `07:00-08:59` / `16:00-18:59` / malam `>= 20:00`) untuk memperlihatkan penerapan tunggal `Surge +20%` yang dibulatkan ke atas kelipatan `Rp 500`.
2. Isi catatan opsional untuk teknisi lalu ketuk **Konfirmasi Order**.

### Skenario 4: Siklus Pesanan Real-Time & Kedaluwarsa 15 Menit
1. Setelah pesanan dibuat oleh Pelanggan 1, layar berpindah ke **Detail & Status Pesanan** (`PENDING`) dengan hitung mundur SLA konfirmasi `15 menit`.
2. Di perangkat kedua (atau setelah beralih akun ke mitra terkait `mitra01@bantuin.test`), buka tab **Pesanan** (`ProviderOrdersScreen`):
   - Ketuk **Terima Pesanan** (`DIKONFIRMASI`), kemudian **Mulai Pengerjaan** (`DALAM_PENGERJAAN`), dan terakhir **Selesaikan Pengerjaan** (`SELESAI`).
3. Perhatikan di perangkat pelanggan: status banner dan riwayat waktu (`booking_events`) berpindah otomatis melalui Supabase Realtime tanpa muat ulang manual. Pelanggan hanya diizinkan membatalkan pada status `PENDING` atau `DIKONFIRMASI` dan tidak memiliki tombol menyelesaikan pekerjaan sendiri.
4. Setelah berstatus `SELESAI`, kirim ulasan bintang 1-5 beserta tag dan komentar (maksimal 300 karakter) pada bagian ulasan pelanggan.

### Skenario 5 (F4): Umpan Balik Trust Score -> Anomali -> Peringkat F1
1. Buka tab **Dashboard** di akun mitra (`ProviderDashboardScreen`).
2. Amati **Provider Trust Score** (gauge 0-100), rata-rata rating, ketepatan waktu SLA (toleransi 15 menit), rasio pembatalan 30 hari, serta grafik snapshot.
3. Ketuk **Simulasikan Kondisi Anomali** untuk memperlihatkan bagaimana penurunan rating atau kenaikan pembatalan > 20% memicu status **Anomali** (`Trust Score < 60` atau `Cancel Rate > 20%`), kemudian ketuk **Kembali ke Data Aktual**.
4. Ketuk **Lihat Rincian Bobot Algoritma & Evaluasi** untuk membuka layar evaluasi mitra dan jalankan **Hitung Ulang** (`RecalculateProviderMetricsUseCase`) untuk memperbarui cache `provider_profiles`.
5. Saat skenario nyata di cloud: apabila mitra mendapat ulasan rating rendah berturut-turut atau sering membatalkan pesanan hingga melewati ambang anomali (minimal 5 transaksi), peringkat mitra tersebut di layar **Rekomendasi Mitra (F1)** otomatis terdampak faktor pengali `0.7x`.

### Skenario 6: Pengaturan Mode Terang, Gelap & Sistem
1. Di layar **Profil Pengguna** (Pelanggan) atau **Dashboard** (Mitra), cari kartu **Tampilan Aplikasi**.
2. Pilih antara **Ikuti Sistem**, **Mode Terang**, atau **Mode Gelap**. Seluruh komponen, peta, grafik canvas, badge status, dan teks langsung menyesuaikan palet dengan kontras yang terjaga.
