# Galih: F2 Cost Estimator, Katalog Layanan, dan Skema DB & RPC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Menyelesaikan dan mematangkan seluruh tanggung jawab anggota Galih sesuai PRD Bantuin: Logika & pengujian F2 Cost Estimator, Screen 4 Estimasi & Order dengan hitung mundur kunci 10 menit, Katalog Layanan Mitra (CRUD-2) berbasis ViewModel & StateFlow, serta verifikasi skema database dan fungsi RPC Supabase (`create_booking`, RLS, snapshot biaya).

**Architecture:** Pure Kotlin domain use case di `domain/usecase/pricing` dengan formula BigDecimal dan `BantuinConfig`. ViewModel-driven Compose UI dengan StateFlow di `ui/customer/estimate` dan `ui/provider/services` tanpa logic atau side-effect langsung di Composable. Persistensi dan RPC transactions di Supabase PostgreSQL dengan RLS ketat dan exclusion constraints.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose, Material 3, AndroidX Lifecycle ViewModel & StateFlow, JUnit 4, Kotlinx Coroutines Test, Supabase PostgREST & RPC.

**Spec:** [PRD.md](file:///c:/Users/Galih/Coding/Project/Bantuin/PRD.md) Bagian 0.2, 5, 7.2, 8.2, 9, 11, 12 (P1, P3, P7).

## Global Constraints

- Platform: Android native (`minSdk` 26, `compileSdk` 37).
- Bahasa UI: Seluruh teks berbahasa Indonesia, konsisten dan alami tanpa AI slop.
- Tidak ada logika bisnis di Composable; semua mutasi state melalui ViewModel dan StateFlow.
- Tidak ada operasi jaringan / perhitungan berat di Main thread (`Dispatchers.Default` untuk F2, `Dispatchers.IO` untuk remote).
- Uang menggunakan `BigDecimal` dan pembulatan `ROUNDING_STEP = 500` (CEILING).
- Anti-Slop UI: Touch target minimal 48dp, kontras Material 3 (oranye `#F57C00`, latar `#F7F6F4`), bebas gradien dekoratif ungu/biru atau em dash.
- Zero-leak security: Tidak ada `service_role` key di kode Android; klien dilarang insert/update tabel `bookings` langsung (harus melalui RPC `create_booking`).

## Review Focus

1. **Lock expiry pada Screen 4:** Ketika hitung mundur kunci 10 menit habis sebelum pengguna menekan "Kirim Pesanan", UI harus menonaktifkan tombol kirim dan menampilkan pesan bahwa slot telah kedaluwarsa beserta navigasi kembali.
2. **Reaktivitas Estimasi F2 di UI:** Mengubah kompleksitas (RINGAN/SEDANG/BERAT) atau sakelar darurat harus langsung memperbarui rincian tarif di layar dan di state ViewModel tanpa memicu panggilan jaringan.
3. **Pemisahan State di Kelola Layanan Mitra:** Composable `ProviderServicesScreen` tidak boleh mengelola state lokal `services` secara terpisah atau memanggil repository langsung lewat coroutine scope Composable; seluruh aksi (simpan, ubah, hapus, filter) harus didelegasikan ke `ProviderServicesViewModel`.
4. **Validasi Form Layanan Mitra:** Form tambah/edit layanan harus memvalidasi nama (tidak kosong), tarif dasar (>= 0 dan disarankan kelipatan 500), serta durasi (15–720 menit).
5. **Integritas RPC `create_booking`:** RPC harus memastikan snapshot biaya lengkap tersimpan di `bookings`, pelanggan dilarang memesan jasanya sendiri (`customer_id <> provider_id`), event `MENUNGGU_KONFIRMASI` tercatat di `booking_events`, dan slot lock terhapus.

---

### Task 1: Penguatan Pengujian Unit & Edge Cases F2 Cost Estimator

**Files:**
- Modify: `app/src/test/java/id/bantuin/app/usecase/CostEstimatorUseCaseTest.kt`
- Target Code: `app/src/main/java/id/bantuin/app/domain/usecase/pricing/CostEstimatorUseCase.kt`

**Interfaces:**
- Consumes: `CostEstimatorUseCase.invoke(baseFare: Int, distanceKm: Double, complexity: Complexity, startMinOfDay: Int, isUrgent: Boolean): CostEstimate`
- Produces: `CostEstimate` (baseFare, distanceKm, distanceCost, complexity, baseCost, surgeApplied, surgeAmount, surgeReason, total)

- [ ] **Step 1: Tulis unit test untuk kasus batas (boundary) dan edge cases F2**

Tambahkan pengujian:
- Batas jarak 0.0 km (jarak 0 km tetap menghasilkan distanceCost 0 dan baseCost = baseFare * weight).
- Batas pembulatan 500 (misalnya nilai raw tidak bulat harus dibulatkan ke atas ke kelipatan 500 terdekat).
- Batas jam sibuk pagi (07:00 / 420 menit s.d. 09:00 / 540 menit) dan sore (16:00 / 960 menit s.d. 19:00 / 1140 menit).
- Batas malam (22:00 / 1320 menit s.d. 06:00 / 360 menit) di mana 05:59 adalah malam dan 06:00 adalah normal.
- Kombinasi jam sibuk + urgent dan malam + urgent (surge tetap 1.20x tunggal, tidak menumpuk).

- [ ] **Step 2: Jalankan test untuk memverifikasi fungsionalitas use case**

Run: `.\gradlew testDebugUnitTest --tests id.bantuin.app.usecase.CostEstimatorUseCaseTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/id/bantuin/app/usecase/CostEstimatorUseCaseTest.kt
git commit -m "test(pricing): add comprehensive edge-case tests for F2 CostEstimatorUseCase"
```

---

### Task 2: Refactoring & Penguatan Screen 4 Estimasi & Order (Reactive UI & Lock Countdown)

**Files:**
- Modify: `app/src/main/java/id/bantuin/app/ui/customer/estimate/EstimateViewModel.kt`
- Modify: `app/src/main/java/id/bantuin/app/ui/customer/estimate/EstimateScreen.kt`
- Modify/Create: `app/src/test/java/id/bantuin/app/viewmodel/EstimateViewModelTest.kt`

**Interfaces:**
- Consumes: `BookingRepository.createBooking(draft: OrderDraft): AppResult<String>`, `CostEstimatorUseCase`, `OrderFlowManager`
- Produces: `EstimateViewModel.setComplexity(complexity: Complexity)`, `EstimateViewModel.setUrgent(isUrgent: Boolean)`, `EstimateViewModel.setNotes(notes: String)`, `EstimateViewModel.tickLockRemaining(seconds: Long)`, `EstimateViewModel.confirmOrder(onSuccess: (String) -> Unit)`

- [ ] **Step 1: Tulis unit test untuk `EstimateViewModel`**

Uji fungsionalitas:
- Perhitungan ulang seketika saat `setComplexity` dan `setUrgent`.
- Pengelolaan state `notes`.
- Penanganan hitung mundur kunci dan deteksi kedaluwarsa slot lock (`isLockExpired`).
- Penanganan loading dan error saat pemesanan gagal.

- [ ] **Step 2: Jalankan test untuk melihat status**

Run: `.\gradlew testDebugUnitTest --tests id.bantuin.app.viewmodel.EstimateViewModelTest`
Expected: FAIL / UNRESOLVED jika fungsi/state baru belum ada.

- [ ] **Step 3: Perbarui `EstimateViewModel.kt`**

- Tambahkan `remainingLockSeconds: Long` dan `isLockExpired: Boolean` ke `EstimateUiState`.
- Tambahkan fungsi `setNotes(notes: String)`.
- Pastikan perubahan `complexity`, `urgent`, dan `notes` langsung mengupdate state dan mengkalkulasi ulang biaya via `CostEstimatorUseCase`.
- Tangani error pesan dengan jelas jika lock telah kedaluwarsa atau server menolak pesanan.

- [ ] **Step 4: Refactor `EstimateScreen.kt`**

- Hubungkan interaksi pemilihan kompleksitas, sakelar darurat, dan input catatan langsung ke `viewModel.setComplexity()`, `viewModel.setUrgent()`, dan `viewModel.setNotes()`.
- Tambahkan Kartu Hitung Mundur Kunci Slot (10 menit) yang menampilkan waktu tersisa (menit : detik).
- Jika waktu habis, tampilkan peringatan "Waktu reservasi slot telah habis" dan ubah tombol menjadi "Pilih Ulang Jadwal" yang menavigasi kembali ke Screen 3.
- Tampilkan indikator loading dan banner error jika terjadi kesalahan saat `Kirim Pesanan`.
- Pastikan teks tombol utama adalah "Kirim Pesanan" (sesuai spesifikasi PRD).

- [ ] **Step 5: Jalankan test untuk memverifikasi**

Run: `.\gradlew testDebugUnitTest --tests id.bantuin.app.viewmodel.EstimateViewModelTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/id/bantuin/app/ui/customer/estimate/EstimateViewModel.kt app/src/main/java/id/bantuin/app/ui/customer/estimate/EstimateScreen.kt app/src/test/java/id/bantuin/app/viewmodel/EstimateViewModelTest.kt
git commit -m "feat(estimate): add lock countdown, reactive viewModel binding, and error handling to Screen 4"
```

---

### Task 3: Refactoring & Validasi Form Kelola Layanan Mitra (CRUD-2)

**Files:**
- Modify: `app/src/main/java/id/bantuin/app/ui/provider/services/ProviderServicesViewModel.kt`
- Modify: `app/src/main/java/id/bantuin/app/ui/provider/services/ProviderServicesScreen.kt`
- Modify/Create: `app/src/test/java/id/bantuin/app/viewmodel/ProviderServicesViewModelTest.kt`

**Interfaces:**
- Consumes: `CatalogRepository.servicesOf`, `CatalogRepository.categories`, `CatalogRepository.upsertService`, `CatalogRepository.deleteService`
- Produces: `ProviderServicesUiState` (services, categories, isUpsertDialogOpen, editingService, isSaving, isLoading, errorMessage), `ProviderServicesViewModel.saveService()`, `deleteService()`, `toggleServiceActive()`, `openAddDialog()`, `openEditDialog()`

- [ ] **Step 1: Tulis unit test untuk `ProviderServicesViewModel`**

Uji fungsionalitas:
- Memuat layanan mitra dan kategori.
- Validasi form layanan: nama kosong ditolak, tarif < 0 atau bukan kelipatan 500 diberi peringatan/ditolak, durasi di luar 15–720 menit ditolak.
- Toggle status aktif/non-aktif layanan.
- Menghapus layanan dan memuat ulang data.

- [ ] **Step 2: Jalankan test untuk melihat kegagalan**

Run: `.\gradlew testDebugUnitTest --tests id.bantuin.app.viewmodel.ProviderServicesViewModelTest`
Expected: FAIL jika validasi atau method toggle belum tersedia.

- [ ] **Step 3: Perbarui `ProviderServicesViewModel.kt`**

- Tambahkan fungsi `toggleServiceActive(service: ServiceItem)`.
- Tambahkan validasi data pada fungsi `saveService`:
  - Nama tidak boleh kosong.
  - Tarif dasar >= 0 dan kelipatan 500.
  - Durasi antara 15 dan 720 menit.
- Pastikan loading dan error state ditangani dengan rapi.

- [ ] **Step 4: Refactor `ProviderServicesScreen.kt`**

- Bersihkan local remember state yang redundant (`var services by remember`, direct `scope.launch { repository.upsertService }`).
- Gunakan `uiState.services` dan delegasikan seluruh interaksi ke `viewModel`:
  - Tambah -> `viewModel.openAddDialog()`
  - Ubah -> `viewModel.openEditDialog(service)`
  - Hapus -> `viewModel.deleteService(service.id)`
  - Toggle aktif -> `viewModel.toggleServiceActive(service)`
- Pastikan dialog `UpsertServiceDialog` menggunakan state dari `viewModel` dan menampilkan pesan validasi jika ada input yang salah.

- [ ] **Step 5: Jalankan test untuk memverifikasi**

Run: `.\gradlew testDebugUnitTest --tests id.bantuin.app.viewmodel.ProviderServicesViewModelTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/id/bantuin/app/ui/provider/services/ProviderServicesViewModel.kt app/src/main/java/id/bantuin/app/ui/provider/services/ProviderServicesScreen.kt app/src/test/java/id/bantuin/app/viewmodel/ProviderServicesViewModelTest.kt
git commit -m "feat(services): refactor ProviderServicesScreen to strict ViewModel StateFlow and add robust validation"
```

---

### Task 4: Verifikasi & Audit Skema Database Supabase & RPC `create_booking`

**Files:**
- Verify/Modify: `supabase/migrations/001_init.sql`
- Verify/Modify: `app/src/main/java/id/bantuin/app/data/remote/dto/RemoteDtoModels.kt`
- Verify/Modify: `app/src/main/java/id/bantuin/app/data/repository/SupabaseBantuinRepository.kt`
- Test: `app/src/test/java/id/bantuin/app/remote/RemoteDtoModelsTest.kt`

**Interfaces:**
- Consumes: `CreateBookingRpcParams`, `ServiceDto`, `BookingDto`
- Produces: RPC `create_booking`, RLS policies pada `services` dan `bookings`

- [ ] **Step 1: Audit DDL tabel `services` dan `bookings` pada `001_init.sql`**

Pastikan:
- Tabel `services` memiliki kolom: `id`, `provider_id`, `category_id`, `name`, `description`, `base_fare`, `duration_min`, `is_active`, `created_at`, `updated_at`.
- RLS pada `services`: `services_select` mengizinkan `is_active or provider_id = auth.uid()`. `services_write_own` membatasi modifikasi hanya oleh pemilik mitra.
- Tabel `bookings` memuat seluruh snapshot tarif F2: `complexity`, `is_urgent`, `distance_km`, `base_fare`, `base_cost`, `surge_applied`, `total_cost`.
- RLS pada `bookings` melarang insert/update langsung oleh klien (`bookings_select` hanya untuk pelanggan atau mitra yang bersangkutan).

- [ ] **Step 2: Audit implementasi RPC `create_booking`**

Pastikan:
- Memeriksa validitas `slot_locks` (`locked_by = auth.uid()` dan `expires_at > now()`).
- Mencegah self-booking (`customer_id <> provider_id` jika user yang login sama dengan mitra).
- Mencatat event awal `MENUNGGU_KONFIRMASI` ke tabel `booking_events`.
- Menghapus kunci dari `slot_locks`.
- Mengembalikan UUID pesanan yang berhasil dibuat.

- [ ] **Step 3: Verifikasi serialisasi DTO RPC di Kotlin**

Pastikan parameter pada `CreateBookingRpcParams` cocok 1:1 dengan signature PostgreSQL `create_booking`.
Jalankan unit test:
`.\gradlew testDebugUnitTest --tests id.bantuin.app.remote.RemoteDtoModelsTest`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add supabase/migrations/001_init.sql app/src/main/java/id/bantuin/app/data/remote/dto/RemoteDtoModels.kt app/src/test/java/id/bantuin/app/remote/RemoteDtoModelsTest.kt
git commit -m "fix(db): verify and audit schema, RLS, and create_booking RPC transaction"
```

---

### Task 5: Verifikasi Menyeluruh (Full Build & Test Suite)

- [ ] **Step 1: Jalankan seluruh suite test unit di repository**

Run: `.\gradlew testDebugUnitTest --no-daemon`
Expected: Seluruh unit test (usecase, viewmodel, remote, repository, session) LULUS 100%.

- [ ] **Step 2: Verifikasi status git dan working tree**

Pastikan branch `galih/f2-cost-estimator-katalog-layanan` berada dalam kondisi bersih tanpa file tak terkelola.

- [ ] **Step 3: Dokumentasikan hasil verifikasi dan panduan pengujian**
