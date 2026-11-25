# Catatan Keputusan Teknis (Architecture & Implementation Decisions)

Dokumen ini mencatat keputusan arsitektur dan implementasi proyek **Bantuin** yang melengkapi atau menyesuaikan rincian teknis pada [PRD.md](../PRD.md).

## 1. Dependency Injection Manual via Factory (`BantuinViewModelFactory`)

- **Konteks**: `PRD.md` Bagian 10.1 menyebut opsi Hilt (+ `hilt-navigation-compose`). Proyek saat ini memakai Android Gradle Plugin `9.3.2` dan Kotlin `2.2.10`.
- **Keputusan**: Menggunakan service locator sederhana di `BantuinApp` bersama `BantuinViewModelFactory` dan `OrderFlowViewModel` tanpa plugin Hilt/KSP.
- **Alasan**: Menghindari kompleksitas anotasi KSP lintas versi AGP 9.3.2 / Kotlin 2.2.10, mempercepat waktu kompilasi, dan menjaga seluruh ViewModel dapat diuji langsung lewat JUnit standar dengan konstruktor eksplisit.
- **Konsekuensi**: Setiap penambahan ViewModel baru didaftarkan secara eksplisit di `ui/common/ViewModelFactory.kt`.

## 2. Persistensi Sesi & Preferensi Tema Memakai `SharedPreferences`

- **Konteks**: `UserSessionStore` dan `ThemePreferenceStore` perlu menyimpan peran aktif (`customer` / `provider`), profil sesi offline/cloud, serta pilihan tema (`SYSTEM`, `LIGHT`, `DARK`) lintas restart aplikasi.
- **Keputusan**: Mengimplementasikan interface `UserSessionStore` dan `ThemePreferenceStore` memakai `SharedPreferencesSessionStore` dan `SharedPreferencesThemeStore` untuk runtime Android serta `InMemorySessionStore` dan `InMemoryThemeStore` untuk unit test.
- **Alasan**: Menghindari penambahan dependensi DataStore dan memisahkan kontrak abstrak agar mudah diganti di masa depan tanpa mengubah ViewModel atau test suite.

## 3. Repository Dual Mode Tanpa Silent Fallback ke Data Demo

- **Konteks**: Pengembangan dan demonstrasi berjalan baik dengan proyek Supabase live maupun mode mandiri tanpa kredensial cloud.
- **Keputusan**: `BantuinApp` memilih `SupabaseBantuinRepository` ketika `BuildConfig.SUPABASE_URL` dan `SUPABASE_ANON_KEY` (dari `local.properties` atau environment variable) terisi nilai non-placeholder, dan memilih `FakeBantuinRepository` bila tidak dikonfigurasi. Kegagalan jaringan atau RPC pada mode cloud tidak pernah dialihkan diam-diam ke `FakeBantuinRepository`.
- **Alasan**: Silent fallback menyamarkan bug otorisasi/RLS atau data demo yang tertukar saat demo cloud dua perangkat. Saat offline, seluruh 15 profil mitra demo di Malang dan test vector tetap dapat diuji deterministik.

## 4. Ktor `ktor-client-okhttp` untuk Supabase Realtime

- **Konteks**: Modul `supabase-kt` (`realtime-kt` 3.1.1) membutuhkan engine Ktor yang mendukung WebSocket untuk berlangganan `postgresChangeFlow<PostgresAction>` pada tabel `bookings`.
- **Keputusan**: Mengganti `ktor-client-android` dengan `ktor-client-okhttp` (`3.0.3`) pada `gradle/libs.versions.toml` dan mengonfigurasi `httpEngine = OkHttp.create()` di `SupabaseClientProvider.kt`.
- **Alasan**: `ktor-client-android` berbasis `HttpURLConnection` tidak mendukung WebSocket sehingga langganan Realtime tidak dapat terhubung ke server Supabase.

## 5. Penguatan Keamanan SQL (`003_security_hardening.sql`)

- **Konteks**: Audit awal migrasi SQL menemukan grant `anon` pada fungsi mutasi RPC, fallback `auth.uid()` saat bernilai `null`, belum adanya seeding otomatis `provider_profiles`/`availability` saat pendaftaran multi-peran `provider`, dan belum aktifnya `REPLICA IDENTITY FULL` pada tabel Realtime.
- **Keputusan**: Menambahkan `supabase/migrations/003_security_hardening.sql` (dan menyinkronkan `001_init.sql`) untuk mencabut izin eksekusi `anon` pada RPC mutasi, mewajibkan `auth.uid()` terautentikasi, membuat RPC `enable_provider_role(...)`, menanamkan baris awal `provider_profiles` dan `availability` (Senin sampai Sabtu `ON` 08:00-17:00 WIB, Minggu `OFF`) di trigger `handle_new_user()`, serta mengaktifkan `REPLICA IDENTITY FULL` untuk `bookings` dan `booking_events`.
- **Alasan**: Mencegah spoofing identitas atau modifikasi booking sebelum login serta memastikan perubahan baris Realtime mengirim payload kolom lengkap.

## 6. Dukungan Mode Terang & Mode Gelap dengan Verifikasi Kontras WCAG AA

- **Konteks**: `PRD.md` Bagian 11.1 awalnya menyarankan satu tema terang, namun pengguna menginstruksikan penyediaan Mode Gelap beserta pilihan tema yang dapat diatur.
- **Keputusan**: Menyediakan `LightBantuinColors` dan `DarkBantuinColors` di `ui/theme/Color.kt` dan `Theme.kt` dengan pilihan `SYSTEM`, `LIGHT`, dan `DARK` pada layar profil pelanggan dan dashboard mitra. Nilai warna disetel dan diverifikasi agar teks normal memenuhi rasio kontras minimal `4.5:1` serta batas input interaktif minimal `3.0:1`.
- **Konsekuensi**: Pengaturan bar sistem pada `MainActivity` dan `Theme.kt` mengikuti `isDarkTheme` secara reaktif menggunakan `enableEdgeToEdge` tanpa API usang.

## 7. Deteksi Lokasi GPS & Fallback Peta OSMDroid Interaktif

- **Konteks**: `PRD.md` Bagian 10.5 mensyaratkan lokasi pelanggan diambil via Fused Location dengan fallback pemilihan titik peta dan default Alun-alun Malang (`-7.9797, 112.6304`).
- **Keputusan**: Menambahkan `LocationHelper` berbasis `play-services-location` (`21.3.0`) dan `Geocoder`, serta `MapEventsOverlay` pada `OsmMapView.kt` agar pengguna dapat mengetuk peta untuk memindahkan pin atau memilih 4 area cepat di Malang ketika izin GPS ditolak atau perangkat dalam ruang.

## 8. Penataan Ulang Bottom Navigation Bar 4 Tab per Peran

- **Konteks**: Versi awal menaruh tab `Notifikasi` (`CUSTOMER_NOTIFICATIONS`) di navbar pelanggan sehingga bar bawah berjumlah 5 tab dan duplikat dengan pintasan cepat, sedangkan navbar mitra memiliki 5 tab.
- **Keputusan**: Merampingkan bottom navigation bar menjadi 4 tab inti per peran (`Beranda`, `Cari`, `Pesanan`, `Profil` untuk pelanggan; `Beranda`, `Pesanan`, `Dashboard`, `Profil` untuk mitra) di `BantuinBottomBar.kt`. Layar `CUSTOMER_NOTIFICATIONS` dan `PROVIDER_CALENDAR` tetap aktif sebagai rute penuh yang diakses lewat ikon lonceng notifikasi di beranda pelanggan maupun pintasan operasional di beranda mitra.
- **Alasan**: Mengurangi kepadatan visual navbar pada layar sempit, memperbesar area sentuh setiap tab, dan memisahkan alur operasional utama dari halaman utilitas sekunder.

## 9. Pemisahan Layar Profil Spesifik Peran dan Pengaturan Universal

- **Konteks**: Sebelumnya pengaturan tema, ganti peran, dan keluar akun bercampur di halaman Profil Pelanggan serta Dashboard Mitra.
- **Keputusan**: Memisahkan `CustomerProfileScreen` (edit identitas & alamat layanan utama pelanggan dengan `LocationPickerDialog`), `ProviderProfileScreen` (identitas usaha, jam kerja, status terima order, daftar layanan/tarif), dan `SettingsScreen` universal (ganti tema `SYSTEM`/`LIGHT`/`DARK`, switch peran, preferensi notifikasi, serta tombol keluar akun dengan dialog konfirmasi).
- **Alasan**: Menjaga kohesi tanggung jawab tiap layar agar pengguna tidak perlu berpindah halaman hanya untuk menemukan pengaturan akun atau tema.

## 10. Pergantian Peran Pelanggan dan Mitra Tanpa `signOut()`

- **Konteks**: Sebelumnya berpindah peran atau mengaktifkan akun mitra memaksa pengguna keluar dan login ulang.
- **Keputusan**: Menambahkan `UserSessionStore.setActiveRole(...)` serta metode `AuthRepository.switchActiveRole(...)` dan `AuthRepository.activateProviderRole(...)` yang memperbarui peran aktif langsung di memori, `SharedPreferencesSessionStore`, dan metadata Supabase tanpa memanggil `signOut()`. Akun khusus pelanggan yang ingin menjadi mitra diarahkan melalui dialog aktivasi di `SettingsScreen` lengkap dengan pemilih titik koordinat usaha (`LocationPickerDialog`).
- **Alasan**: Menghilangkan friksi saat pengguna multi-peran beralih antara memesan layanan dan mengelola pekerjaan sebagai mitra.

## 11. Model Pembayaran QRIS/Tunai dan Aturan Saldo Pencairan Mitra

- **Konteks**: Pelanggan membutuhkan fleksibilitas bayar di tempat (`CASH`) maupun non-tunai (`QRIS`), sedangkan mitra memerlukan transparansi mana dana yang bisa dicairkan dan mana yang sudah diterima tunai di lokasi.
- **Keputusan**: Menambahkan `PaymentMethod` (`CASH`, `QRIS`), `PaymentStatus`, `PayoutRequest`, dan `ProviderRevenueAnalytics` bersama migrasi `supabase/migrations/004_ux_chat_payment_wallet.sql` serta `ProviderRevenueUseCase`. Hanya booking berstatus `SELESAI` dengan `paymentMethod = QRIS` dan `paymentStatus = PAID` yang masuk ke `availableBalance` untuk pencairan (`MIN_PAYOUT_AMOUNT = 20_000`, `PAYOUT_ADMIN_FEE = 2_500`). Booking `SELESAI` dengan `paymentMethod = CASH` dicatat ke `cashRevenue` dan `totalCombinedRevenue` pada analisis pendapatan di `ProviderDashboardScreen`, tetapi tidak menambah `availableBalance`.
- **Alasan**: Mencegah mitra mencairkan uang tunai yang sudah diterima langsung di lokasi sekaligus tetap menampilkan performa omzet keseluruhan secara akurat.

## 12. Chat Pesanan Terintegrasi dan Konfirmasi Selesai Model Geser

- **Konteks**: Koordinasi pelanggan dan mitra sebelumnya hanya mengandalkan panggilan telepon (`ACTION_DIAL`), dan penyelesaian pesanan oleh mitra rawan tertekan tidak sengaja.
- **Keputusan**: Menyediakan `ChatListScreen` dan `ChatRoomScreen` berbasis `ChatRepository` (dengan tabel `booking_messages` + Realtime pada cloud dan state flow lokal pada mode demo), serta layar khusus `CompleteOrderConfirmScreen` yang mewajibkan verifikasi status lunas (untuk `QRIS`) atau centang penerimaan uang tunai (untuk `CASH`) sebelum `SlideToConfirmBar` (threshold geser `>= 0.85f` plus tombol aksesibilitas non-drag) dapat menyelesaikan pesanan ke `SELESAI`.
- **Alasan**: Memudahkan koordinasi dan riwayat komunikasi per pesanan langsung di dalam aplikasi sekaligus mencegah perubahan status selesai maupun pencatatan saldo secara tidak sengaja.
