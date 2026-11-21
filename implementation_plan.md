Berdasarkan audit mendalam terhadap kode sumber, arsitektur, dan dokumen spesifikasi PRD.md,
  berikut adalah analisis progres terkini dan Implementation Plan menyeluruh dari titik ini hingga
  aplikasi Bantuin berstatus 100% selesai (Production/Demo-Ready).
  ──────
  ### 1. Audit Status & Progres Terkini
  Aplikasi Bantuin saat ini telah memiliki fondasi logika bisnis dan antarmuka yang sangat solid,
  namun masih beroperasi menggunakan repositori in-memory
  (app/src/main/java/id/bantuin/app/data/repository/FakeBantuinRepository.kt):

   Komponen                            │ Status              │ Catatan Evaluasi
  ─────────────────────────────────────┼─────────────────────┼──────────────────────────────────────
   Domain Use Cases (F1–F4)            │ 100% Selesai        │ MatchmakingUseCase,
                                       │                     │ CostEstimatorUseCase,
                                       │                     │ SchedulingConflictDetector,
                                       │                     │ TrustScoreUseCase, dan
                                       │                     │ RecalculateProviderMetricsUseCase
                                       │                     │ lulus seluruh test vector
                                       │                     │ PRD.md:450-520.
   Antarmuka Jetpack Compose           │ 90% Selesai         │ 16 layar utama pelanggan dan mitra
                                       │                     │ sudah terhubung ke alur navigasi di
                                       │                     │ app/src/main/java/id/bantuin/app/ui/
                                       │                     │ navigation/NavGraph.kt, menerapkan
                                       │                     │ Material 3 dan standar Anti-Slop
                                       │                     │ (target sentuh 48dp, kontras WCAG
                                       │                     │ AAA).
   Interaktivitas Lokal & Feedback     │ 100% Selesai (Mock) │ Jarak Haversine dinamis, kalender
   Loop                                │                     │ dengan jam operasional, slot locking
                                       │                     │ 10 menit, CRUD layanan, edit profil
                                       │                     │ mitra, dan ulasan memicu rekalkulasi
                                       │                     │ metrik mitra sudah berjalan di
                                       │                     │ memori lokal.
   Persistensi Data Cloud & RPC        │ Belum Dimulai (0%)  │ DDL/RLS/RPC masih berada dalam
   Supabase                            │                     │ bentuk teks di PRD.md:525-875. Belum
                                       │                     │ ada berkas migrasi .sql mandiri dan
                                       │                     │ belum ada integrasi klien supabase-
                                       │                     │ kt.
   Sinkronisasi Realtime Lintas        │ Belum Dimulai (0%)  │ Pemantauan pesanan baru bisa terjadi
   Perangkat                           │                     │ dalam satu instance aplikasi lokal
                                       │                     │ via StateFlow, belum sinkron antar-2
                                       │                     │ perangkat fisik (HP Pelanggan & HP
                                       │                     │ Mitra).
   Arsitektur State & Resilience       │ 65% Selesai         │ Sebagian besar layar masih mengelola
                                       │                     │ state lokal di dalam Composable via
                                       │                     │ remember. Belum tahan rotasi layar
                                       │                     │ (configuration change) dengan
                                       │                     │ ViewModel resmi dan belum ada DI
                                       │                     │ (Hilt).
   Integrasi Hardware (GPS)            │ 30% Selesai         │ Sudah ada peta OSMDroid di
                                       │                     │ app/src/main/java/id/bantuin/app/ui/
                                       │                     │ common/OsmMapView.kt dengan preset
                                       │                     │ lokasi, namun belum mengintegrasikan
                                       │                     │ sensor GPS riil
                                       │                     │ (FusedLocationProviderClient).
  ──────
  ### 2. Gap Analysis: Apa yang Dibutuhkan Menuju Selesai?

  Untuk menyelesaikan aplikasi sesuai seluruh kriteria keberhasilan pada PRD Bagian 12, 14, dan 15,
  terdapat 4 pilar kesenjangan (gaps) yang wajib ditutup:
  1. Backend & Persistensi Cloud: Mengaktifkan database PostgreSQL di Supabase dengan RLS ketat,
  memicu RPC untuk transaksi booking, dan migrasi dari in-memory fake ke klien data nyata.
  2. Sinkronisasi Realtime Lintas Perangkat: Menghubungkan Supabase Realtime channel agar perubahan
  status pesanan (Kirim → Terima → Mulai → Selesai) otomatis termutakhirkan di layar perangkat kedua
  secara instan.
  3. Ketahanan State & Sesi Pengguna: Pemisahan logic layar ke ViewModel + StateFlow (tahan rotasi
  layar), penyimpanan sesi login dan peran aktif (customer vs provider) menggunakan Jetpack
  DataStore.
  4. Otomasi Batas Waktu & GPS: Background timer untuk membatalkan pesanan PENDING yang lewat 15
  menit (KEDALUWARSA) serta pengambilan koordinat GPS pengguna.
  ──────
  ### 3. Master Implementation Plan (Fase 1 s.d. Fase 6)
  Berikut adalah urutan tahapan kerja bertahap (phased roadmap) hingga aplikasi selesai secara utuh:
    ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
    │ Fase 1:         │     │ Fase 2:         │     │ Fase 3:         │
    │ ViewModel,      │────▶│ Supabase Cloud  │────▶│ Realtime Sync & │
    │ DataStore & DI  │     │ Backend & RPC   │     │ Siklus Pesanan  │
    └─────────────────┘     └─────────────────┘     └─────────────────┘
                                                             │
    ┌─────────────────┐     ┌─────────────────┐              │
    │ Fase 6:         │     │ Fase 5:         │     ┌────────▼────────┐
    │ Polishing, Demo │◀────│ Integrasi GPS   │◀────│ Fase 4:         │
    │ & Laporan Akhir │     │ & Hardening     │     │ Otomasi Expiry  │
    └─────────────────┘     └─────────────────┘     │ & Skenario Demo │
                                                    └─────────────────┘
  ──────
  #### Fase 1: Pemisahan ViewModel, State Management, & Persistensi Sesi
  │ Tujuan: Membuat seluruh layar tahan rotasi (configuration change sesuai PRD 0.1 Aturan 5) dan
  │ memastikan peran aktif tersimpan saat aplikasi ditutup/dibuka kembali.

  • Fitur & Pekerjaan:
      1. Migrasi State Composable ke ViewModel:
          • Buat CustomerHomeViewModel, RecommendationViewModel, ScheduleViewModel,
          EstimateViewModel, OrderDetailViewModel.
	  • Buat ProviderOrdersViewModel, ProviderCalendarViewModel, ProviderDashboardViewModel.
	  • Pindahkan pemanggilan use case dan repository dari Composable rememberCoroutineScope ke
	  viewModelScope + StateFlow.
      2. Persistensi Sesi Multi-Peran (Jetpack DataStore):
          • Simpan userId, accessToken, dan activeRole ("customer" / "provider") ke DataStore
          Preferences.
	  • Tambahkan interceptor agar aplikasi langsung membuka layar beranda sesuai peran terakhir
	  tanpa harus memilih peran ulang saat cold restart.
      3. Abstraksi Repository:
          • Rapikan antarmuka di app/src/main/java/id/bantuin/app/domain/repository/Repositories.kt.
	  • Siapkan arsitektur dual-mode (bisa beralih antara FakeBantuinRepository untuk offline
	  testing dan RemoteBantuinRepository untuk cloud backend via konfigurasi).
  • Berkas yang Dibuat / Dimodifikasi:
      • Modifikasi: app/src/main/java/id/bantuin/app/domain/repository/Repositories.kt,
      app/src/main/java/id/bantuin/app/MainActivity.kt
      • Buat: app/src/main/java/id/bantuin/app/core/datastore/UserSessionDataStore.kt
      • Buat: Seluruh file ViewModel di paket id.bantuin.app.ui.*
  • Definition of Done (DoD):
      • Layar dirotasi (portrait ↔ landscape) pada saat proses input jadwal atau estimasi biaya
      tanpa kehilangan state draft atau re-fetching data.
      • Berganti peran menjadi Mitra, menutup aplikasi, lalu membuka kembali: aplikasi langsung
      masuk ke dashboard Mitra.

  ──────
  #### Fase 2: Backend Supabase, Migrasi Database, & Remote Data Layer
  │ Tujuan: Mengaktifkan cloud database PostgreSQL, seluruh tabel, trigger keamanan, RLS, serta
  │ fungsi RPC transaksi di Supabase sesuai PRD Bagian 9.
  • Fitur & Pekerjaan:
      1. Persiapan Berkas Migrasi SQL:
          • Ekstrak seluruh skrip DDL dari PRD Bagian 9 ke file:
              • supabase/migrations/001_init.sql (Tabel, RLS, Trigger, dan 8 RPC).
	      • supabase/seed.sql (Data demo 6 kategori, 15 mitra, layanan, ulasan historis, dan
              koordinat Malang).
      2. Integrasi SDK Supabase di Android:
          • Tambahkan dependensi supabase-kt (postgrest-kt, auth-kt, realtime-kt) dan ktor-client-
          android pada gradle/libs.versions.toml dan app/build.gradle.kts.
          • Muat SUPABASE_URL dan SUPABASE_ANON_KEY secara aman dari local.properties.
      3. Implementasi Remote Data Layer:
	  • Buat DTO serializable (@Serializable) untuk seluruh entity database di id.bantuin.app.
	  data.remote.dto.
	  • Implementasikan SupabaseBantuinRepository yang memanggil RPC:
              • get_busy_blocks & acquire_slot_lock untuk F3.
              • create_booking untuk F2.
              • update_booking_status untuk alur status pesanan.
              • submit_review & recalculate_provider_metrics untuk F4 → F1 feedback loop.
  • Berkas yang Dibuat / Dimodifikasi:
      • Buat: supabase/migrations/001_init.sql & supabase/seed.sql
      • Buat: app/src/main/java/id/bantuin/app/data/remote/SupabaseClientProvider.kt
      • Buat: app/src/main/java/id/bantuin/app/data/remote/dto/RemoteDtoModels.kt
      • Buat: app/src/main/java/id/bantuin/app/data/repository/SupabaseBantuinRepository.kt
  • Definition of Done (DoD):
      • Database Supabase terisi tabel dan data seed.
      • Eksekusi booking dari aplikasi Android berhasil membuat baris baru di tabel bookings dan
      baris audit di booking_events melalui RPC tanpa error RLS.

  ──────
  #### Fase 3: Multi-Perangkat Realtime Sync & Enforcing Siklus Pesanan
  │ Tujuan: Menghubungkan WebSocket Supabase Realtime agar transaksi pemesanan dapat dipantau
  │ langsung di dua perangkat berbeda secara bersamaan.
  • Fitur & Pekerjaan:
      1. Supabase Realtime Channel Subscription:
          • Buka channel postgres_changes pada tabel bookings di mana filter provider_id = user.id
          (untuk Mitra) dan customer_id = user.id (untuk Pelanggan).
          • Pantau tabel booking_events untuk pembaruan timeline pengerjaan di
          app/src/main/java/id/bantuin/app/ui/customer/orderdetail/OrderDetailScreen.kt.
      2. Penegakan Hak Akses Peran pada Status Pesanan (PRD 6.1):
	  • Sisi Mitra: hanya mitra yang dapat menekan tombol Terima, Tolak, Mulai Pengerjaan,
	  Selesai, dan Batalkan (Mitra).
	  • Sisi Pelanggan: hanya pelanggan yang dapat menekan tombol Batalkan (Pelanggan) (selama
	  status PENDING atau DIKONFIRMASI) dan memberi Ulasan Bintang setelah status SELESAI.
      3. Penanganan Slot Lock Concurrency (F3 Realtime):
	  • Saat satu akun melakukan acquire_slot_lock, akun lain yang mencoba memilih slot jam yang
	  sama pada hari yang sama langsung menerima notifikasi SLOT_LOCKED_BY_OTHER disertai
	  rekomendasi 3 slot alternatif terdekat.
  • Berkas yang Dimodifikasi:
      • app/src/main/java/id/bantuin/app/ui/customer/orderdetail/OrderDetailScreen.kt
      • app/src/main/java/id/bantuin/app/ui/provider/orders/ProviderOrdersScreen.kt
      • app/src/main/java/id/bantuin/app/ui/customer/schedule/ScheduleScreen.kt
  • Definition of Done (DoD):
      • Uji coba 2 perangkat: HP 1 (Pelanggan) menekan "Kirim Pesanan" → Seketika kartu pesanan
      muncul di tab "Menunggu" pada HP 2 (Mitra) tanpa perlu pull-to-refresh.
      • Mitra menekan "Mulai Pengerjaan" di HP 2 → Timeline status di HP 1 seketika berubah menjadi
      DALAM_PENGERJAAN.

  ──────
  #### Fase 4: Otomasi Expiry 15 Menit & Verifikasi Skenario Demo
  │ Tujuan: Mengotomatiskan transisi batas waktu serta memverifikasi 5 skenario demonstrasi wajib
  │ PRD.
  • Fitur & Pekerjaan:
      1. Otomasi Batas Waktu 15 Menit (PENDING → KEDALUWARSA):
          • Buat countdown aktif di kartu tab Menunggu Mitra dan Layar Tracking Pelanggan.
          • Jika waktu tersisa menyentuh 00:00 dan belum ada respons mitra, picu RPC
	  update_booking_status(..., 'KEDALUWARSA') dan lepaskan slot kalender.
      2. Verifikasi 5 Skenario Demo PRD Bagian 14:
          • Skenario 1 (F1): Verifikasi filter radius & rating, mitra status OFF tidak muncul di
          list, mitra anomali turun peringkat dengan faktor penalti ×0.7.
	  • Skenario 2 (F3): Pilih slot bentrok → sistem menampilkan 3 slot alternatif; kunci slot
	  10 menit mencegah perebutan jadwal.
	  • Skenario 3 (F2): Verifikasi transparansi rincian biaya: tarif dasar + jarak Haversine
	  (Rp 2.000/km) + pengali kesulitan + surge 20% (jam sibuk/malam) dibulatkan ke kelipatan Rp
	  500 terdekat.
	  • Skenario 4 (Tracking): Demonstrasi end-to-end pemesanan hingga selesai di 2 perangkat
	  fisik.
	  • Skenario 5 (F4 Feedback Loop): Pelanggan memberi ulasan bintang 1 → Rekalkulasi metrik
	  mitra → Skor Trust turun di bawah 60 → Muncul Alert Card merah di Dashboard Mitra → Urutan
          mitra langsung merosot di hasil pencarian Pelanggan.

  • Berkas yang Dibuat / Dimodifikasi:
      • Buat: app/src/main/java/id/bantuin/app/core/util/OrderExpiryTracker.kt
      • Buat: docs/DEMO_SCENARIO_GUIDE.md (panduan alur langkah demi langkah pengujian demo).
  • Definition of Done (DoD):
      • Pesanan yang diabaikan selama 15 menit otomatis berubah menjadi KEDALUWARSA dan slot terbuka
      kembali.
      • Kelima skenario demo PRD dapat dieksekusi mulus tanpa crash atau bug sinkronisasi.

  ──────
  #### Fase 5: Integrasi GPS Hardware & Resilience Jaringan
  │ Tujuan: Menghubungkan sensor lokasi perangkat Android asli dengan fallback peta offline serta
  │ penanganan error jaringan.
  • Fitur & Pekerjaan:
      1. GPS Provider via Play Services Location:
          • Tambahkan dependensi play-services-location.
          • Buat LocationService dengan runtime permission ACCESS_FINE_LOCATION &
          ACCESS_COARSE_LOCATION.
	  • Saat beranda dibuka, ambil lokasi riil pengguna. Jika GPS mati / izin ditolak, gunakan
          fallback dialog pemilih peta OSMDroid yang sudah ada di
          app/src/main/java/id/bantuin/app/ui/customer/home/CustomerHomeScreen.kt.
      2. Penanganan Kondisi Error & Koneksi (Offline Resilience):
	  • Tampilkan StateViews.kt (komponen ErrorState dan EmptyState Material 3) saat jaringan
	  terputus.
          • Tambahkan mekanisme auto-reconnect pada WebSocket Realtime saat perangkat kembali online.
  • Berkas yang Dibuat / Dimodifikasi:
      • Buat: app/src/main/java/id/bantuin/app/core/location/LocationHelper.kt
      • Modifikasi: app/src/main/AndroidManifest.xml (menambahkan permission lokasi)
      • Modifikasi: app/src/main/java/id/bantuin/app/ui/customer/home/CustomerHomeScreen.kt
  • Definition of Done (DoD):
      • Membuka aplikasi di perangkat fisik langsung mendeteksi alamat dan koordinat pengguna secara
      akurat.
      • Saat koneksi internet dimatikan di tengah jalan, aplikasi menampilkan pesan error yang ramah
      dan tombol "Coba Lagi" tanpa mengalami force close.
  ──────
  #### Fase 6: Dokumentasi Lengkap, Material Laporan Skripsi, & Rilis APK
  │ Tujuan: Menyelesaikan seluruh kewajiban administrasi dokumen arsitektur, panduan deployment, dan
  │ dukungan Bab III–V laporan.
  • Fitur & Pekerjaan:
      1. Arsitektur & Keputusan Sistem (docs/DECISIONS.md):
          • Catat seluruh Architecture Decision Records (ADR): pemilihan formula Haversine, struktur
          in-memory fallback, alasan pembatasan slot lock 10 menit, dan mekanisme feedback loop F4 →
          F1.
      2. Pembaruan Panduan Setup README.md:
	  • Tambahkan langkah inisialisasi Supabase CLI, eksekusi migrasi SQL, pembuatan akun
	  pengujian demo, dan pengaturan konfigurasi local.properties.
      3. Penyusunan Material Laporan Skripsi (PRD Bagian 15):
	  • Bab III: Diagram arsitektur Clean Architecture, flowchart 4 use case F1–F4, dan ERD
	  database Supabase.
	  • Bab IV: Tabel perbandingan test vector teori vs hasil eksekusi unit test, matriks
	  skenario pengujian fungsionalitas.
	  • Bab V: Analisis performa responsivitas algoritma F1-F4 dan batasan sistem.
      4. Build Release APK:
	  • Konfigurasi Proguard/R8 rules di proguard-rules.pro.
	  • Build release APK yang ditandatangani untuk keperluan demonstrasi penguji.
  • Berkas yang Dibuat / Dimodifikasi:
      • Buat: docs/DECISIONS.md
      • Buat: docs/LAPORAN_SKRIPSI_SUPPORT.md
      • Modifikasi: README.md
  • Definition of Done (DoD):
      • Seluruh file dokumen arsitektur dan panduan lengkap.
      • Perintah ./gradlew assembleRelease menghasilkan APK bersih tanpa warning lint kritis.
  ──────
  ### 4. Matriks Rincian Fitur & Modul
   ID Modul │ Nama Modul / Fitur      │ Status Sekarang │ Target Pekerjaan Penyelesaian │ Fase
  ──────────┼─────────────────────────┼─────────────────┼───────────────────────────────┼───────────
   CRUD-0   │ Multi-Role              │ UI + In-Memory  │ Supabase Auth (Email/Pass) +  │ Fase 1 &
            │ Authentication          │                 │ Session DataStore             │ 2
   CRUD-1   │ Profil Mitra & Kategori │ UI + In-Memory  │ Simpan ke tabel               │ Fase 2
            │                         │                 │ provider_profiles via         │
            │                         │                 │ Supabase                      │
   CRUD-2   │ Kelola Layanan Mitra    │ UI + In-Memory  │ Persistensi katalog ke tabel  │ Fase 2
            │                         │                 │ provider_services             │
   CRUD-3   │ Ketersediaan Jadwal &   │ UI + In-Memory  │ Sinkronisasi tabel            │ Fase 2 &
            │ Antrian                 │                 │ provider_availabilities &     │ 3
            │                         │                 │ bookings                      │
   CRUD-4   │ Rating & Ulasan         │ UI + In-Memory  │ Pemanggilan RPC submit_review │ Fase 2
            │                         │                 │ ke Supabase                   │
   F1       │ Smart Matchmaking       │ Logika + UI OK  │ Sambungkan data kandidat riil │ Fase 2
            │                         │                 │ dari Supabase                 │
   F2       │ Dynamic Cost Estimator  │ Logika + UI OK  │ Buat pesanan dengan snapshot  │ Fase 2
            │                         │                 │ biaya via RPC create_booking  │
   F3       │ SLA & Conflict Detector │ Logika + UI OK  │ Cegah race condition via RPC  │ Fase 2 &
            │                         │                 │ acquire_slot_lock             │ 3
   F4       │ Trust Score & Anomaly   │ Logika + UI OK  │ Rekalkulasi metrik otomatis   │ Fase 2
            │                         │                 │ pasca ulasan via RPC          │
   RT-1     │ Realtime Multi-Device   │ Belum Ada       │ Supabase Realtime channel     │ Fase 3
            │                         │                 │ subscription                  │
   EXP-1    │ Expiry Timer 15 Menit   │ Belum Ada       │ Background / In-App Countdown │ Fase 4
            │                         │                 │ + RPC status change           │
   GPS-1    │ GPS Hardware            │ Peta Mock OSM   │ Integrasi                     │ Fase 5
            │ Integration             │                 │ FusedLocationProviderClient   │
  ──────
  ### 5. Rekomendasi Langkah Pertama

  Untuk memulai rencana ini dengan mulus tanpa merusak fungsionalitas yang sudah ada, Langkah Awal
  yang Direkomendasikan adalah:

  1. Memulai Fase 1 (ViewModel & State Management): Membungkus state Composable ke dalam kelas
  ViewModel dengan StateFlow dan memasang Jetpack DataStore untuk peran aktif. Hal ini akan
  memperkokoh arsitektur UI sebelum disambungkan ke koneksi Supabase.
  2. Membuat berkas migrasi SQL ([Fase 2]): Mengekstrak skrip database dari PRD.md menjadi berkas
  supabase/migrations/001_init.sql dan supabase/seed.sql agar skema siap dijalankan kapan pun Anda
  mengaktifkan proyek Supabase.
