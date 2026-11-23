# Hafizh: F1 Smart Service Matchmaking, Profil Mitra & Kategori Implementation Plan

> **Goal:** Menyelesaikan, memperkuat, dan mematangkan seluruh tanggung jawab anggota Hafizh sesuai PRD Bantuin: Logika & pengujian unit komprehensif F1 Smart Service Matchmaking (`MatchmakingUseCase`), Layar Beranda Pelanggan (Screen 1), Rekomendasi Mitra (Screen 2) dengan filter interaktif & penanganan anomali (P5 & P10), Detail Mitra & Katalog Layanan (Screen 2b), serta Edit Profil Mitra & Pangkalan (Screen 9) dengan arsitektur murni StateFlow & ViewModel.

**Architecture:** Pure Kotlin domain use case di `domain/usecase/matchmaking` memakai formula Haversine & parameter bobot `BantuinConfig`. ViewModel-driven Compose UI dengan `StateFlow` di `ui/customer/home`, `ui/customer/recommendation`, `ui/customer/providerdetail`, dan `ui/provider/profile` tanpa logic atau side-effect langsung di Composable. Komunikasi data melalui interface `CatalogRepository` dan `AuthRepository`.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose, Material 3, AndroidX Lifecycle ViewModel & StateFlow, JUnit 4, Kotlinx Coroutines Test, OSMDroid.

**Spec:** [PRD.md](file:///D:/Antigravity/Bantuin/PRD.md) Bagian 0.2, 5, 6.2 (CRUD-1), 7.1, 8.1, 10.5, 11 (Screen 1, 2, 2b, 9), 12 (P3, P5, P10).

## Global Constraints

- **Platform:** Android native (`minSdk` 26, `compileSdk` 37).
- **Kepemilikan Paket:** Hanya memodifikasi berkas dalam ruang lingkup Hafizh:
  - `domain/usecase/matchmaking`
  - `ui/customer/home`
  - `ui/customer/recommendation`
  - `ui/customer/providerdetail`
  - `ui/provider/profile`
  - Unit tests terkait di `app/src/test/java/id/bantuin/app/`
- **Pure Kotlin Logic:** `MatchmakingUseCase` murni kelas Kotlin tanpa dependensi framework Android/Compose/Supabase.
- **StateFlow & ViewModel:** Seluruh state form dan UI dikelola reaktif di ViewModel menggunakan `StateFlow`. Composable dilarang mengelola state lokal terpisah atau memanggil repository langsung via coroutine Composable.
- **Bahasa UI:** Seluruh teks berbahasa Indonesia, konsisten, natural, dan sesuai pedoman M3 (warna oranye `#F57C00`, latar `#F7F6F4`, touch target minimal 48dp).

## Review Focus & Task Breakdown

1. **Task 1: Penguatan Pengujian Unit & Edge Cases F1 MatchmakingUseCase**
   - Verifikasi Test Vector 8.1 (kandidat A, B, C, D, E) dan akurasi Haversine.
   - Tambahkan pengujian edge cases: daftar kosong, kandidat di luar radius 10 km tepat, tie-breaker dengan skor identik, kandidat tanpa rating / completion rate (fallback 0.7), penalti anomali x0.7.

2. **Task 2: Refactoring ProviderProfileEditScreen & ViewModel (Strict StateFlow Architecture)**
   - Hilangkan antipattern mutable state lokal (`var name`, `var bio`, `var address`, dsb.) dan panggilan langsung repository di Composable.
   - Sambungkan interaksi input field secara reaktif ke `viewModel.onNameChange()`, `onBioChange()`, `onPhoneChange()`, `onAddressChange()`, `onLocationChange()`.
   - Validasi form di ViewModel: nama wajib diisi, format nomor telepon, indikator penyimpanan (`isSaving`), notifikasi keberhasilan (`isSaved`), dan error state.

3. **Task 3: Penyempurnaan RecommendationScreen & ViewModel (Screen 2 & Feedback Loop P10)**
   - Pastikan kartu rekomendasi mitra menampilkan Match Score %, jarak 1 desimal, rating & ulasan, badge kepercayaan (TRUSTED / NEW / NONE), dan label peringatan merah jika terdeteksi anomali.
   - Pastikan filter jarak (slider/chip), filter rating minimum (0..5), dan filter hanya mitra terpercaya bekerja reaktif.
   - Tampilkan notice card informatif ketika terdapat mitra di luar radius 10 km dan empty state yang jelas ketika filter menghasilkan 0 mitra.

4. **Task 4: Penyempurnaan ProviderDetailScreen & ViewModel (Screen 2b)**
   - Tampilkan informasi lengkap profil mitra, kartu peringatan anomali jika `flagAnomaly = true`, daftar layanan aktif dengan durasi dan tarif terformat Rupiah, serta ulasan pelanggan terbaru.
   - Penanganan loading, error dengan tombol coba lagi, dan empty state.

5. **Task 5: Pengujian Komprehensif ViewModel Hafizh**
   - Tambahkan unit test mandiri untuk `RecommendationViewModelTest`, `ProviderDetailViewModelTest`, dan `ProviderProfileEditViewModelTest`.
   - Verifikasi isolasi data, manipulasi state filter, dan penyimpanan profil.

6. **Task 6: Verifikasi Akhir Build & Test Suite**
   - Pastikan `./gradlew testDebugUnitTest` lulus 100% tanpa regresi.
