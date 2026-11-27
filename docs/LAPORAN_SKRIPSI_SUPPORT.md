# Dokumen Pendukung Penyusunan Laporan (Bab III dan Bab IV)

Dokumen ini memetakan implementasi kode aktual aplikasi **Bantuin** ke struktur laporan pada [PRD.md](../PRD.md) Bagian 15.

---

## 1. Pemetaan Fungsi F1 - F4 ke Kode Sumber

### F1: Smart Service Matchmaking
- **A. Logika Pemrosesan Data**:
  - Kelas: `HaversineCalculator` & `MatchmakingUseCase` di [MatchmakingUseCase.kt](../app/src/main/java/id/bantuin/app/domain/usecase/matchmaking/MatchmakingUseCase.kt)
  - Konstanta: `BantuinConfig` (`MAX_RADIUS_KM = 10.0`, `W_DISTANCE = 0.50`, `W_RATING = 0.35`, `W_COMPLETION = 0.15`, `DEFAULT_RATING_SCORE = 0.7`, `DEFAULT_COMPLETION_SCORE = 0.7`, `ANOMALY_PENALTY_FACTOR = 0.7`)
  - Aturan pengurutan:
    $$S_{akhir} = (0.50 \cdot S_{jarak} + 0.35 \cdot S_{rating} + 0.15 \cdot S_{selesai}) \times P_{anomali}$$
    dengan $S_{jarak} = 1 - \frac{d}{10}$, $S_{rating} = \frac{\bar{r}}{5}$ (default `0.7`), dan $P_{anomali} = 0.7$ jika `flagAnomaly == true`. Kandidat dengan jarak $> 10$ km dipisahkan ke `outOfRange`.
- **B. Layout & UI**:
  - `RecommendationScreen` ([RecommendationScreen.kt](../app/src/main/java/id/bantuin/app/ui/customer/recommendation/RecommendationScreen.kt)) menampilkan chip filter (`Semua Mitra`, `Jarak <= 5km`, `Rating 4.8+`, `Mitra Terpercaya`), kartu skor & badge kepercayaan (`ProviderBadgeChip`), serta kartu catatan mitra di luar radius 10 km.
- **C. Event Handling & State**:
  - `RecommendationViewModel` & `RecommendationUiState` ([RecommendationViewModel.kt](../app/src/main/java/id/bantuin/app/ui/customer/recommendation/RecommendationViewModel.kt)) menggunakan `StateFlow` dan menjaga state filter terhadap perubahan konfigurasi/rotasi layar.
- **D. Komunikasi Client-Server & Database**:
  - `SupabaseBantuinRepository.candidates(categoryId)` ([SupabaseBantuinRepository.kt](../app/src/main/java/id/bantuin/app/data/repository/SupabaseBantuinRepository.kt)) membaca profil mitra aktif, layanan, jadwal ketersediaan (`availability`), serta mengecualikan akun sendiri (`customer_id != provider_id`).
- **Verifikasi**: [MatchmakingUseCaseTest.kt](../app/src/test/java/id/bantuin/app/usecase/MatchmakingUseCaseTest.kt).

---

### F2: Dynamic Cost & Surge Estimator
- **A. Logika Pemrosesan Data**:
  - Kelas: `CostEstimatorUseCase` di [CostEstimatorUseCase.kt](../app/src/main/java/id/bantuin/app/domain/usecase/pricing/CostEstimatorUseCase.kt)
  - Konstanta: `RATE_PER_KM = 2000`, `SURGE_MULTIPLIER = 1.20`, `ROUNDING_STEP = 500`, `PEAK_RANGES = [420..539, 960..1139]`, `NIGHT_START_MIN = 1200`, `NIGHT_END_MIN = 360`.
  - Rumus:
    $$\text{raw} = (\text{baseFare} + \text{round}_1(\text{jarak}) \times 2000) \times w_{\text{kompleksitas}}$$
    $$\text{baseCost} = \lceil \text{raw} / 500 \rceil \times 500$$
    $$\text{total} = \begin{cases} \lceil (\text{raw} \times 1.20) / 500 \rceil \times 500 & \text{jika jam sibuk / malam / urgent} \\ \text{baseCost} & \text{selainnya} \end{cases}$$
- **B. Layout & UI**:
  - `EstimateScreen` ([EstimateScreen.kt](../app/src/main/java/id/bantuin/app/ui/customer/estimate/EstimateScreen.kt)) menampilkan pemilih tingkat kompleksitas (`Ringan 1.00x`, `Sedang 1.25x`, `Berat 1.50x`), sakelar `Layanan Cepat Tanggap`, rincian biaya transparan (tarif dasar, biaya jarak, penyesuaian kompleksitas, surge +20%, total), dan hitung mundur kunci 10 menit.
- **C. Event Handling & State**:
  - `EstimateViewModel` ([EstimateViewModel.kt](../app/src/main/java/id/bantuin/app/ui/customer/estimate/EstimateViewModel.kt)) menghitung ulang estimasi di tiap perubahan kompleksitas/urgensi dan melepas lock (`releaseLock`) saat pengguna membatalkan langkah estimasi.
- **D. Komunikasi Client-Server & Database**:
  - `SupabaseBantuinRepository.createBooking(draft)` memanggil fungsi PostgreSQL `create_booking(...)` di [001_init.sql](../supabase/migrations/001_init.sql) dengan validasi lock aktif dan exclusion constraint `bookings_no_overlap`.
- **Verifikasi**: [CostEstimatorUseCaseTest.kt](../app/src/test/java/id/bantuin/app/usecase/CostEstimatorUseCaseTest.kt).

---

### F3: Scheduling Conflict Detector & Slot Locking
- **A. Logika Pemrosesan Data**:
  - Kelas: `SchedulingConflictDetector` di [SchedulingConflictDetector.kt](../app/src/main/java/id/bantuin/app/domain/usecase/scheduling/SchedulingConflictDetector.kt)
  - Aturan: memeriksa `AvailabilityMode` (`OFF`, `ON`, `ALWAYS_ON`), batas waktu lewat (`nowMin`), batas jam operasional, dan tumpang tindih dengan blok pesanan aktif + `TRAVEL_BUFFER_MIN = 30`. Apabila tidak valid, menghasilkan hingga `MAX_ALTERNATIVE_SLOTS = 3` alternatif dengan langkah grid 30 menit.
- **B. Layout & UI**:
  - `ScheduleScreen` ([ScheduleScreen.kt](../app/src/main/java/id/bantuin/app/ui/customer/schedule/ScheduleScreen.kt)) menampilkan kalender dinamis 7 hari ke depan, kartu slot jam dengan status `Tersedia` / `Bentrok`, kartu peringatan `ConflictAlertCard` beserta chip alternatif bebas konflik.
- **C. Event Handling & State**:
  - `ScheduleViewModel` ([ScheduleViewModel.kt](../app/src/main/java/id/bantuin/app/ui/customer/schedule/ScheduleViewModel.kt)) menyimpan tanggal terpilih, ketersediaan mitra, daftar blok sibuk, validasi slot, serta hasil `acquireLock` dalam `StateFlow<ScheduleUiState>`.
- **D. Komunikasi Client-Server & Database**:
  - `get_busy_blocks(uuid, timestamptz, timestamptz)` membaca pesanan aktif + `slot_locks` aktif pada jendela hari WIB, sementara `acquire_slot_lock(...)` mengunci slot `10 menit` menggunakan `pg_advisory_xact_lock` dan `btree_gist`.
- **Verifikasi**: [SchedulingConflictDetectorTest.kt](../app/src/test/java/id/bantuin/app/usecase/SchedulingConflictDetectorTest.kt).

---

### F4: Provider Trust Score & Feedback Loop Evaluasi
- **A. Logika Pemrosesan Data**:
  - Kelas: `TrustScoreUseCase` ([TrustScoreUseCase.kt](../app/src/main/java/id/bantuin/app/domain/usecase/trust/TrustScoreUseCase.kt)) dan `RecalculateProviderMetricsUseCase` ([RecalculateProviderMetricsUseCase.kt](../app/src/main/java/id/bantuin/app/domain/usecase/trust/RecalculateProviderMetricsUseCase.kt))
  - Rumus:
    $$\text{Trust} = 0.45 \cdot \text{RatingScore} + 0.35 \cdot \text{OnTimeRate} + 0.20 \cdot (100 - \text{CancelRate})$$
    Dengan `MIN_TX_FOR_EVALUATION = 5` (dibawah 5 transaksi memakai skor awal `70.0` dengan badge `Mitra Baru`). Anomali aktif bila transaksi $\ge 5$ dan (`TrustScore < 60.0` atau `CancelRate > 20.0`).
- **B. Layout & UI**:
  - `ProviderDashboardScreen` ([ProviderDashboardScreen.kt](../app/src/main/java/id/bantuin/app/ui/provider/dashboard/ProviderDashboardScreen.kt)) menampilkan gauge circular Canvas Trust Score, 3 metrik 30 hari (rating, ketepatan SLA, rasio batal), bar chart riwayat snapshot, kartu peringatan anomali + simulasi what-if, serta kartu pesanan `PENDING` terbaru.
  - `ProviderEvaluationScreen` ([ProviderEvaluationScreen.kt](../app/src/main/java/id/bantuin/app/ui/provider/evaluation/ProviderEvaluationScreen.kt)) menampilkan tier prioritas, faktor pengali matchmaking (`1.0x` vs `0.7x`), dan tombol **Hitung Ulang**.
- **C. Event Handling & State**:
  - `ProviderDashboardViewModel` ([ProviderDashboardViewModel.kt](../app/src/main/java/id/bantuin/app/ui/provider/dashboard/ProviderDashboardViewModel.kt)) & `ProviderEvaluationViewModel` ([ProviderEvaluationViewModel.kt](../app/src/main/java/id/bantuin/app/ui/provider/evaluation/ProviderEvaluationViewModel.kt)).
- **D. Komunikasi Client-Server & Database**:
  - `SupabaseBantuinRepository.trustInput(providerId, 30)` mengambil transaksi 30 hari terakhir; `apply(providerId, result)` memanggil RPC `apply_provider_metrics(...)` untuk memperbarui cache di `provider_profiles` dan menyimpan baris `provider_metric_snapshots`.
- **Verifikasi**: [TrustScoreUseCaseTest.kt](../app/src/test/java/id/bantuin/app/usecase/TrustScoreUseCaseTest.kt).

---

## 2. Tabel Kendala & Solusi (Materi Bab IV)

| No | Kendala Teknis | Solusi yang Diimplementasikan | Berkas Terkait |
|---|---|---|---|
| 1 | Potensi double booking saat dua pelanggan memesan jam yang sama secara bersamaan | Mengunci slot sementara 10 menit melalui RPC `acquire_slot_lock` berbasis `pg_advisory_xact_lock`, ditambah constraint eksklusi `bookings_no_overlap` (`tstzrange` + `btree_gist`) di PostgreSQL | [001_init.sql](../supabase/migrations/001_init.sql), [SupabaseBantuinRepository.kt](../app/src/main/java/id/bantuin/app/data/repository/SupabaseBantuinRepository.kt) |
| 2 | Modul Supabase Realtime (`realtime-kt`) gagal membuka koneksi WebSocket dengan `ktor-client-android` | Mengganti HTTP engine Ktor ke `ktor-client-okhttp` (`3.0.3`) dan mengaktifkan `REPLICA IDENTITY FULL` pada tabel `bookings` serta `booking_events` | [libs.versions.toml](../gradle/libs.versions.toml), [SupabaseClientProvider.kt](../app/src/main/java/id/bantuin/app/data/remote/SupabaseClientProvider.kt), [003_security_hardening.sql](../supabase/migrations/003_security_hardening.sql) |
| 3 | Pesanan `PENDING` dapat menggantung apabila mitra tidak merespons melewati batas 15 menit | Menambahkan hitung mundur klien (`CountdownTimerView`) dipadukan dengan pemanggilan `expire_pending_bookings()` saat daftar/detail pesanan dimuat | [CountdownTimerView.kt](../app/src/main/java/id/bantuin/app/ui/common/CountdownTimerView.kt), [OrderDetailViewModel.kt](../app/src/main/java/id/bantuin/app/ui/customer/orderdetail/OrderDetailViewModel.kt) |
| 4 | State draft pemesanan pelanggan berisiko hilang saat layar dirotasi | Menyimpan `OrderFlowManager` di dalam `OrderFlowViewModel` dan memindahkan seluruh state jadwal, estimasi, dan formulir mitra ke masing-masing `ViewModel` (`StateFlow`) | [OrderFlowViewModel.kt](../app/src/main/java/id/bantuin/app/ui/customer/OrderFlowViewModel.kt), [NavGraph.kt](../app/src/main/java/id/bantuin/app/ui/navigation/NavGraph.kt) |
| 5 | Kontras teks abu-abu dan aksen oranye pada mode terang serta dukungan mode gelap | Menyelaraskan palet `LightBantuinColors` dan `DarkBantuinColors` menggunakan pengujian kontras WCAG AA serta menyediakan pengubah tema di aplikasi | [Color.kt](../app/src/main/java/id/bantuin/app/ui/theme/Color.kt), [Theme.kt](../app/src/main/java/id/bantuin/app/ui/theme/Theme.kt), [ThemePreferenceStore.kt](../app/src/main/java/id/bantuin/app/core/theme/ThemePreferenceStore.kt) |
| 6 | Perangkat uji di dalam ruangan atau pengguna yang menolak izin GPS tetap perlu mengatur titik lokasi | Menggunakan `FusedLocationProviderClient` (`LocationHelper`) dengan fallback mengetuk peta interaktif OSMDroid (`MapEventsOverlay`) atau memilih titik cepat area Malang | [LocationHelper.kt](../app/src/main/java/id/bantuin/app/core/location/LocationHelper.kt), [OsmMapView.kt](../app/src/main/java/id/bantuin/app/ui/common/OsmMapView.kt) |
