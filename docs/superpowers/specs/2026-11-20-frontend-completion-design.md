# Desain Penyempurnaan Frontend Bantuin (Fungsionalitas, Feedback Loop, dan Anti-Slop)

**Tanggal:** 2026-11-20  
**Status:** Siap Disetujui  
**Pendekatan Terpilih:** Opsi 1 (Fokus Fungsionalitas dan Interaktivitas Data + Anti-Slop UI & Layout Mobile)

---

## 1. Latar Belakang & Tujuan

Aplikasi Android native Bantuin telah memiliki 7 layar utama serta use case domain untuk 4 fungsi inti (F1 Matchmaking, F2 Estimasi Tarif, F3 Deteksi Konflik Jadwal, dan F4 Skor Kepercayaan). Namun, hasil analisis menunjukkan beberapa celah interaktivitas di sisi UI:
1. Jarak pada estimasi tarif F2 masih berupa nilai statis (1.8 km), belum menghitung jarak Haversine dari titik pelanggan ke mitra terpilih.
2. Pemilihan jadwal F3 masih menggunakan 5 tanggal contoh dan slot statis, belum membaca jam operasional mitra dan `busyBlocks` riil dari repositori, serta belum mengeksekusi `acquireLock` (kunci slot 10 menit).
3. Feedback loop antara ulasan pelanggan (F4) dan rekomendasi mitra (F1) terputus: ulasan yang disimpan belum memicu penghitungan ulang metrik mitra via `RecalculateProviderMetricsUseCase`.
4. Manajemen katalog layanan mitra (CRUD-2) dan jam kerja kalender (CRUD-3) belum memiliki form input data lengkap (tambah/edit layanan, atur jam buka/tutup mode ON).
5. Layar edit profil mitra (CRUD-1) belum tersedia.
6. Dialog pemilih titik lokasi pelanggan di beranda belum memperbarui koordinat `GeoPoint`.

Tujuan dari pekerjaan ini adalah menyelesaikan seluruh celah fungsional di atas sehingga skenario interaksi end-to-end dapat berjalan realistis, sembari memastikan seluruh antarmuka mematuhi standar `/antislop-ui` dan `/antislop-layoutmobile`.

---

## 2. Sasaran (Goals, Non-Goals, Anti-Goals)

### Goals
- Mengintegrasikan perhitungan jarak Haversine riil antara `customerLocation` dan `provider.location` pada `EstimateScreen` (F2).
- Membuat pemilihan tanggal kalender dinamis (mulai hari ini ke depan) dan membangkitkan slot waktu berdasarkan jam operasional mitra pada `ScheduleScreen` (F3).
- Mengintegrasikan pembacaan blok sibuk (`busyBlocks`) dan eksekusi penguncian slot (`acquireLock`) selama 10 menit di `ScheduleScreen`.
- Menyambungkan feedback loop F4 → F1: pengiriman ulasan pada `OrderDetailScreen` otomatis memicu `RecalculateProviderMetricsUseCase` di repositori, memperbarui `ratingAvg`, `completionRate`, `badge`, dan `flagAnomaly`, sehingga urutan mitra di `RecommendationScreen` langsung berubah.
- Melengkapi form modal untuk CRUD Layanan Mitra pada `ProviderServicesScreen` (tambah layanan baru, ubah nama, tarif dasar kelipatan 500, dan durasi pengerjaan).
- Melengkapi pengaturan jam buka dan tutup pada `ProviderCalendarScreen` untuk mode ON dengan validasi jam buka < jam tutup.
- Membuat fitur edit profil mitra (CRUD-1) untuk mengubah nama, bio, nomor telepon, alamat, dan titik koordinat pangkalan.
- Memperbarui dialog peta di `CustomerHomeScreen` agar dapat memperbarui koordinat `GeoPoint` dan alamat pelanggan.
- Memastikan kepatuhan antislop: target sentuh minimal 48dp, kontras teks WCAG 4.5:1, tanpa gradien generik, tanpa emoji dekoratif di teks UI, dan tanpa kebocoran scroll horizontal pada layar kecil.

### Non-Goals
- Migrasi backend ke Supabase Cloud (tetap menggunakan `FakeBantuinRepository` in-memory yang offline-ready).
- Refaktor arsitektur ke ViewModel per layar (disepakati menggunakan state Compose yang ringan dan reaktif untuk Opsi 1).
- Fitur di luar ruang lingkup PRD (payment gateway, chat antarpengguna, push notification).

### Anti-Goals
- Menambahkan pola visual AI-slop: dilarang menggunakan gradien ungu-biru, kartu bento yang dipaksakan, titik status berkedip tanpa arti, atau teks pemasaran hiperbolis.

---

## 3. Pendekatan yang Dipertimbangkan

- **Pendekatan A (Terpilih): Fokus Fungsionalitas dan Interaktivitas Data**  
  Menyelesaikan seluruh integrasi algoritma, form CRUD, dan feedback loop data dengan arsitektur state yang ada.  
  *Kelebihan*: Cepat diselesaikan, seluruh skenario demo interaktif langsung berfungsi, risiko regresi minimal.  
  *Kekurangan*: State masih berada di layer UI Compose, belum dipisah ke kelas ViewModel independen.
- **Pendekatan B: Fungsionalitas Lengkap + Migrasi ViewModel**  
  Menyelesaikan fungsionalitas di atas sekaligus membungkus seluruh state ke ViewModel dan StateFlow.  
  *Kelebihan*: Tahan rotasi layar secara penuh sesuai PRD 0.1 Aturan 5.  
  *Kekurangan*: Penulisan file kode jauh lebih banyak dan memperpanjang cakupan kerja di luar kebutuhan interaktivitas utama.

---

## 4. Rincian Desain & Perubahan Antarmuka

### 4.1 F2: Perhitungan Jarak Riil (`EstimateScreen.kt`)
- Jarak dihitung secara dinamis:
  ```kotlin
  val distanceKm = remember(orderFlow.customerLocation, provider.location) {
      HaversineCalculator.distanceKm(orderFlow.customerLocation, provider.location)
  }
  ```
- Nilai `distanceKm` ini diteruskan ke `costEstimator(baseFare, distanceKm, complexity, startMin, isUrgent)`.
- Rincian tarif pada UI menampilkan jarak riil dengan 1 desimal (contoh: "1.4 km x Rp 2.000").

### 4.2 F3: Pemilihan Tanggal Dinamis & Slot Lock (`ScheduleScreen.kt`)
- Menghasilkan daftar 7 hari kalender mulai dari hari ini menggunakan kalender lokal (WIB).
- Saat tanggal dipilih, layar membaca jam operasional mitra untuk hari tersebut via `repository.availability(provider.providerId)` dan memuat blok sibuk via `repository.busyBlocks(provider.providerId, selectedDateIso)`.
- Membangkitkan slot waktu per 30 menit dalam rentang jam buka-tutup mitra.
- *Keputusan Builder*: Untuk hari ini ("Hari Ini"), slot yang berada di waktu lampau akan divalidasi dengan `nowMin` sehingga memicu `ConflictReason.PAST_TIME` ("Waktu telah lampau") sebagai demonstrasi fitur F3 PRD. Slot di masa mendatang dan hari esoknya tetap valid dan dapat dipilih normal.
- Deteksi konflik dijalankan secara reaktif. Jika slot bentrok, tampilkan 3 alternatif terdekat.
- Saat tombol "Lanjut ke Estimasi Biaya" ditekan, panggil `repository.acquireLock(...)` dan simpan objek `LockInfo` ke dalam `orderFlow.lockInfo` untuk mengunci slot selama 10 menit.

### 4.3 F4 → F1 Feedback Loop (`FakeBantuinRepository.kt` & `OrderDetailScreen.kt`)
- Pada `FakeBantuinRepository.submitReview`:
  1. Simpan ulasan baru ke riwayat pesanan.
  2. Ambil seluruh ulasan untuk mitra terkait dan panggil `RecalculateProviderMetricsUseCase`.
  3. Perbarui `providersList` dengan hasil kalkulasi baru (`ratingAvg`, `completionRate`, `badge`, `flagAnomaly`).
- Pada `RecommendationScreen`:
  1. Jangan lagi membaca `FakeData.providers` statis. Panggil `repository.candidates(categoryId)` secara reaktif.
  2. Saat ulasan baru dibuat, kandidat yang diambil dari repositori otomatis mencerminkan rating dan anomali terbaru, sehingga urutan ranking F1 langsung berubah.

### 4.4 CRUD-2: Form Kelola Layanan (`ProviderServicesScreen.kt`)
- Tambahkan tombol aksi "Tambah Layanan" di bagian atas halaman.
- Tambahkan dialog modal `UpsertServiceDialog`:
  - Input nama layanan (teks).
  - Pilihan kategori (dropdown / filter chip 6 kategori).
  - Input tarif dasar (angka, minimal 0, kelipatan 500).
  - Input estimasi durasi pengerjaan (angka dalam menit, 15 - 720 menit).
- Setiap kartu layanan memiliki tombol edit untuk membuka kembali dialog dengan data awal.
- Kartu memiliki tombol hapus dengan konfirmasi.

### 4.5 CRUD-3: Atur Jam Kerja Kalender (`ProviderCalendarScreen.kt`)
- Ketika mode `AvailabilityMode.ON` dipilih pada suatu hari, tampilkan dua field pemilih jam: Jam Buka dan Jam Tutup (default 08:00 dan 17:00).
- Konversi input jam ke menit sejak 00:00 (`openMin`, `closeMin`).
- Validasi: jam buka harus lebih kecil dari jam tutup.
- Tombol "Simpan Perubahan" memanggil `repository.saveAvailability(...)`.

### 4.6 CRUD-1: Edit Profil Mitra (`ProviderProfileEditScreen.kt` & Navigasi)
- *Keputusan Builder*: Tombol navigasi ke `PROVIDER_PROFILE_EDIT` diletakkan sebagai kartu aksi (`ActionTile`) di `ProviderHomeScreen` di bawah bagian "Kelola Layanan & Jadwal", dan juga melalui tombol edit pada header kartu profil `ProviderHomeScreen`. Hal ini menjaga tata letak 4-tab bottom navigation tetap utuh.
- *Keputusan Builder*: Entitas profil mitra yang diedit adalah mitra aktif demo `"p01"` (`Pak Budi Santoso, S.T.`). Repositori menyediakan fungsi `updateProviderProfile(providerId, name, bio, location, address, phone)` yang memperbarui `providersList` sekaligus data `currentUser`.
- Layar memuat form:
  - Nama lengkap dan Bio singkat mitra.
  - Alamat pangkalan layanan.
  - Titik koordinat pangkalan (`GeoPoint`) yang dapat dipilih/disimulasikan pada peta OSM.
  - Nomor telepon / kontak WhatsApp.

### 4.7 Dialog Peta Lokasi Pelanggan (`CustomerHomeScreen.kt`)
- Sempurnakan dialog pemilih lokasi:
  - Menyediakan pilihan cepat beberapa lokasi populer di Malang untuk kemudahan pengujian jarak F1 & F2 (misalnya: Alun-alun Kota Malang, Kampus UB / Jl. Suhat, Sawojajar, Dinoyo).
  - Pilihan manual alamat dan titik koordinat.
  - Memperbarui `orderFlow.customerLocation` dan `orderFlow.customerAddress`.

### 4.8 Kepatuhan Anti-Slop (UI & Mobile Layout)
- **Target Sentuh**: Semua tombol, chip, radio button, dan icon button memiliki area sentuh minimal 48dp (melebihi batas minimal 44dp).
- **Rhythm & Spacing**: Padding layar konsisten (16dp horizontal), jarak antarelemen bernafas (8–16dp), tanpa margin raksasa desktop.
- **Bebas Teks AI-Slop**: Bahasa Indonesia lugas, tanpa istilah klise, tanpa em dash (`—` atau `–`), tanpa dekorasi emoji dalam teks.
- **Bebas Gradien & Glow**: Menggunakan warna solid Material 3 dengan palet Oranye Hangat (`#E65100`), kontras teks gelap di atas latar terang `#F7F6F4` (WCAG AAA).
- **Penanganan Overflow**: Semua container formulir dan daftar dibungkus dalam `LazyColumn` atau `verticalScroll`, mencegah teks terpotong atau kebocoran scroll horizontal pada layar sempit.

---

## 5. Ringkasan Keputusan Builder (Telah Diselesaikan)

1. **Target Profil Mitra (CRUD-1)**: Mengedit mitra aktif demo `"p01"` (`Pak Budi Santoso`) dan menyinkronkan data kontak ke `currentUser`.
2. **Titik Akses Navigasi Profil Mitra**: Ditambahkan sebagai `ActionTile` di `ProviderHomeScreen` ("Edit Profil & Pangkalan") dan tombol edit pada kartu profil atas, sehingga tidak mengubah struktur 4 tab bottom navigation.
3. **Validasi Jam Lampau Hari Ini (F3)**: Menampilkan status `ConflictReason.PAST_TIME` pada slot sebelum jam saat ini di "Hari Ini" untuk mendemonstrasikan validasi F3 PRD, sementara slot sore/malam atau hari esoknya tetap terbuka untuk dipesan.
