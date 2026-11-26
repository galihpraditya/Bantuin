# PRD FINAL — Bantuin: Smart Service Marketplace

> **Dokumen ini adalah sumber kebenaran (source of truth) untuk membangun aplikasi.** Ditulis agar dapat dieksekusi oleh AI coding agent maupun developer manusia. Semua angka, rumus, status, dan aturan di sini **final**. Jika ada konflik dengan dokumen lain, dokumen ini yang menang.

| Item | Nilai |
|---|---|
| Produk | Bantuin, on-demand multi-service marketplace |
| Platform | Android native (minSdk 26) |
| Bahasa & UI | Kotlin, Jetpack Compose, Material Design 3 |
| Backend | Supabase (Auth, Postgres, Realtime, RLS, RPC) |
| Peta | OSMDroid (OpenStreetMap), tanpa API key |
| Bahasa antarmuka | Indonesia |
| Zona waktu | `Asia/Jakarta` (WIB) |
| Konteks | Project Akhir Praktikum PAPB TIF-A, Universitas Brawijaya, Kelompok 5 |
| Versi | 1.0 (final) |

---

## 0. Petunjuk untuk AI Agent (WAJIB DIBACA)

### 0.1 Aturan kerja
1. **Ikuti dokumen ini secara harfiah.** Jangan menambah fitur di luar Bagian 3.2 (Out of Scope). Jangan mengubah rumus, konstanta, status, atau nama tabel.
2. Bila ada hal yang tidak ditentukan, pilih opsi **paling sederhana** yang konsisten dengan dokumen, lalu catat di `docs/DECISIONS.md` (tanggal, keputusan, alasan). Jangan berhenti untuk bertanya kecuali hal tersebut memblokir total.
3. **Logika problem solving F1–F4 wajib berupa kelas/fungsi Kotlin murni** (tanpa Android/Compose/Supabase di dalamnya) di `domain/usecase/`, memakai konstanta dari `BantuinConfig`, dan **wajib punya unit test** memakai test vector di Bagian 8. Bagian 7 memuat implementasi acuan; boleh disesuaikan sintaksnya selama perilaku dan hasil test vector identik.
4. Semua perhitungan berat memakai `Dispatchers.Default`; akses jaringan/DB memakai `Dispatchers.IO`. Tidak ada operasi jaringan di main thread.
5. State UI memakai `StateFlow` di `ViewModel` (tahan rotasi). Tidak ada logika bisnis di Composable.
6. **Jangan commit rahasia.** `SUPABASE_URL` dan `SUPABASE_ANON_KEY` dibaca dari `local.properties` → `BuildConfig`. `service_role` key tidak boleh ada di kode aplikasi maupun repository.
7. Semua teks UI berbahasa Indonesia, disimpan di `strings.xml`.
8. Versi library: gunakan **versi stabil terbaru yang saling kompatibel** (verifikasi di dokumentasi resmi masing-masing; jangan menebak nama API `supabase-kt` dari ingatan, cek dokumentasinya). Gunakan Version Catalog (`libs.versions.toml`).
9. Kerjakan **per fase** (Bagian 11). Setiap fase harus bisa dibuild dan dijalankan sebelum lanjut. Commit kecil dan sering dengan pesan jelas.
10. Kode harus mudah dikutip ke laporan: beri KDoc singkat pada fungsi F1–F4 dan pisahkan jelas: logika (A), UI (B), event/state (C), komunikasi data (D).

### 0.2 Struktur kepemilikan (agar tidak konflik merge)
Setiap anggota memiliki paket sendiri. Perubahan lintas paket dikomunikasikan lewat interface di `domain/`.

| Anggota | Fungsi utama | Paket yang dimiliki |
|---|---|---|
| Hafizh | F1 Matchmaking + CRUD Profil Mitra & Kategori | `domain/usecase/matchmaking`, `ui/customer/home`, `ui/customer/recommendation`, `ui/customer/providerdetail`, `ui/provider/profile` |
| Galih | F2 Cost Estimator + CRUD Pemesanan/Katalog Layanan | `domain/usecase/pricing`, `ui/customer/estimate`, `ui/provider/services`, pemilik **skema DB & RPC** |
| Adam | F3 Scheduling + CRUD Kalender & Antrian | `domain/usecase/scheduling`, `ui/customer/schedule`, `ui/provider/calendar`, `ui/provider/orders` |
| Ezekiel | F4 Trust Score + CRUD Ulasan & Riwayat | `domain/usecase/trust`, `ui/customer/orderdetail`, `ui/provider/dashboard`, `ui/provider/evaluation` |

Bersama: setup proyek, tema, navigasi, auth, Hilt, `data/remote`.

---

## 1. Ringkasan Produk

Bantuin mempertemukan **Pelanggan** (mahasiswa, penghuni kos, keluarga) dengan **Mitra** (teknisi listrik, teknisi AC/elektronik, tukang bangunan, petugas kebersihan, kurir angkut barang, pengajar privat) lewat pemesanan ala ride-hailing.

Nilai tambah (bukan sekadar CRUD) ada pada 4 fungsi utama:

| ID | Fungsi | Hasil bagi pengguna |
|---|---|---|
| F1 | Smart Service Matchmaking | Mitra terdekat & terbaik diurutkan otomatis (Haversine + weighted scoring) |
| F2 | Dynamic Cost & Surge Estimator | Estimasi biaya transparan + surge jam sibuk/darurat sebelum memesan |
| F3 | Automated SLA & Scheduling Conflict Detector | Tidak ada double booking + saran slot alternatif |
| F4 | Provider Trust Score & Anomaly Analyzer | Skor kepercayaan, badge, dan Alert Card anomali; memengaruhi urutan F1 |

**Feedback loop wajib berfungsi**: ulasan/pembatalan → F4 menghitung ulang → memengaruhi urutan F1.

## 2. Tujuan
1. Marketplace jasa serba ada yang cepat, aman, dan terukur.
2. Rekomendasi mitra optimal. 3. Transparansi biaya. 4. Cegah double booking. 5. Jaga mutu lewat Trust Score.
6. Akademik: tiap anggota punya CRUD pendukung + 1 fungsi utama yang nilai tambahnya terlihat di UI dan dapat didemokan.

## 3. Ruang Lingkup

### 3.1 In Scope (MVP)
- Auth email+password, profil, **akun multi-peran** (satu akun bisa Pelanggan dan Mitra, ganti peran aktif).
- Kategori jasa, lokasi pelanggan (GPS + pilih titik di peta OSMDroid + fallback manual).
- F1, F2, F3, F4 beserta UI.
- Mitra: katalog layanan & tarif, ketersediaan (OFF/ON/ALWAYS_ON per hari), terima/tolak pesanan, ubah status pengerjaan.
- Pelanggan: pesan, batalkan, pantau status real-time, beri rating & ulasan.
- Pembaruan real-time status pesanan (Supabase Realtime) saat aplikasi terbuka.

### 3.2 Out of Scope (JANGAN dibuat)
Payment gateway/dompet digital (pembayaran **tunai di tempat**), chat, push notification (FCM), verifikasi KYC mitra, login Google/OTP, aplikasi iOS/web, admin panel, multi-layanan dalam satu pesanan (satu pesanan = satu layanan), pembayaran/komisi platform.

## 4. Pengguna & Peran

| Peran | Kemampuan |
|---|---|
| Pelanggan | Cari mitra, lihat rekomendasi, pilih layanan & jadwal, lihat estimasi, pesan, batalkan, pantau, beri ulasan |
| Mitra | Kelola profil/lokasi, katalog layanan, ketersediaan, terima/tolak/mulai/selesaikan/batalkan pesanan, lihat Trust Score & Alert Card |

`profiles.roles` berisi `customer` dan/atau `provider`. Peran aktif disimpan lokal (DataStore/ViewModel) dan menentukan Bottom Navigation. Pengguna tidak boleh memesan layanan miliknya sendiri (`customer_id <> provider_id`; mitra yang juga pelanggan tidak muncul di rekomendasi miliknya sendiri).

## 5. Konfigurasi Global (Konstanta Final)

Seluruh angka bisnis hanya boleh didefinisikan di sini (satu objek) dan dipakai ulang.

```kotlin
// core/config/BantuinConfig.kt
object BantuinConfig {
    const val TIMEZONE_ID = "Asia/Jakarta"

    // F1
    const val MAX_RADIUS_KM = 10.0
    const val W_DISTANCE = 0.50
    const val W_RATING = 0.35
    const val W_COMPLETION = 0.15
    const val DEFAULT_RATING_SCORE = 0.7        // mitra tanpa data rating
    const val DEFAULT_COMPLETION_SCORE = 0.7    // mitra tanpa riwayat
    const val ANOMALY_PENALTY_FACTOR = 0.7

    // F2
    const val RATE_PER_KM = 2000                // Rp per km
    const val SURGE_MULTIPLIER = "1.20"         // +20% tunggal, tidak menumpuk
    const val ROUNDING_STEP = 500               // dibulatkan ke atas, kelipatan Rp 500
    // Jam sibuk [mulai, selesai) dalam menit sejak 00:00
    val PEAK_RANGES = listOf(420..539, 960..1139)   // 07:00-08:59, 16:00-18:59
    // Malam: >= 20:00 (1200) atau < 06:00 (360)
    const val NIGHT_START_MIN = 1200
    const val NIGHT_END_MIN = 360

    // F3
    const val TRAVEL_BUFFER_MIN = 30
    const val SLOT_LOCK_MINUTES = 10
    const val SLOT_GRID_MIN = 30
    const val MAX_ALTERNATIVE_SLOTS = 3
    const val PENDING_EXPIRY_MINUTES = 15

    // F4
    const val TRUST_WINDOW_DAYS = 30
    const val MIN_TX_FOR_EVALUATION = 5
    const val NEW_PROVIDER_TRUST = 70.0
    const val W_TRUST_RATING = 0.45
    const val W_TRUST_ONTIME = 0.35
    const val W_TRUST_RELIABILITY = 0.20
    const val TRUST_MIN_THRESHOLD = 60.0
    const val CANCEL_RATE_MAX_PCT = 20.0
    const val LATE_TOLERANCE_MIN = 15
}
```

Catatan jam sibuk: batas **mulai inklusif, selesai eksklusif**. 06:00–06:59, 09:00–15:59, 19:00–19:59 = tarif normal.

## 6. Kebutuhan Fungsional

### 6.1 Siklus Status Pesanan (final)

```
PENDING ─mitra terima──▶ DIKONFIRMASI ─mitra mulai──▶ DALAM_PENGERJAAN ─mitra selesai──▶ SELESAI
   │                          │
   ├─ mitra tolak ──▶ DITOLAK
   ├─ lewat 15 menit tanpa respons ──▶ KEDALUWARSA
   ├─ pelanggan batal ──▶ DIBATALKAN_PELANGGAN     (juga dari DIKONFIRMASI)
                              └─ mitra batal ──▶ DIBATALKAN_MITRA   (hanya dari DIKONFIRMASI)
```

- **Status aktif** (menahan slot kalender): `PENDING`, `DIKONFIRMASI`, `DALAM_PENGERJAAN`.
- Pesanan `PENDING` langsung menahan slot mitra. Slot lepas saat `DITOLAK`, `KEDALUWARSA`, atau dibatalkan.
- `DALAM_PENGERJAAN` mencatat `started_at = now()` (dasar ketepatan waktu F4). `SELESAI` dan `DIBATALKAN_MITRA` mencatat `finished_at`.
- Hanya **`DIBATALKAN_MITRA`** dihitung sebagai pembatalan mitra di F4. `DITOLAK`, `KEDALUWARSA`, `DIBATALKAN_PELANGGAN` tidak dihitung.
- Setelah `SELESAI`, pelanggan dapat memberi **satu** ulasan (rating 1–5 + komentar opsional) per pesanan.
- Transisi **hanya** lewat RPC `update_booking_status` (Bagian 9).

### 6.2 Modul CRUD

| ID | Modul | Layar | Tabel |
|---|---|---|---|
| CRUD-0 | Auth & profil multi-peran | Login, Register, Profil | `profiles` |
| CRUD-1 | Profil mitra (lokasi, bio) & eksplorasi kategori | Beranda, Detail Mitra, Edit Profil Mitra | `provider_profiles`, `service_categories` |
| CRUD-2 | Katalog layanan & tarif mitra; form pemesanan (draft) | Kelola Layanan, Estimasi & Order | `services`, `bookings` |
| CRUD-3 | Ketersediaan & kalender mitra; antrian pesanan | Jadwal Mitra, Pesanan Mitra | `availability`, `bookings`, `slot_locks` |
| CRUD-4 | Ulasan & riwayat transaksi | Detail Pesanan, Riwayat | `reviews`, `booking_events` |

"Keranjang layanan" = **draft pesanan** in-memory (`OrderDraft`) untuk satu mitra dan satu layanan; belum ada baris DB sampai dikonfirmasi.

---

## 7. Fungsi Utama (Spesifikasi + Implementasi Acuan)

Lokasi kode: `domain/usecase/...` Semua tipe di bawah berada di `domain/model`.

### 7.0 Model domain bersama

```kotlin
data class GeoPoint(val lat: Double, val lng: Double)

enum class Complexity(val weight: java.math.BigDecimal) {
    RINGAN(java.math.BigDecimal("1.00")),
    SEDANG(java.math.BigDecimal("1.25")),
    BERAT(java.math.BigDecimal("1.50"))
}

enum class BookingStatus {
    PENDING, DIKONFIRMASI, DALAM_PENGERJAAN, SELESAI,
    DITOLAK, KEDALUWARSA, DIBATALKAN_PELANGGAN, DIBATALKAN_MITRA;
    val isActive get() = this in setOf(PENDING, DIKONFIRMASI, DALAM_PENGERJAAN)
}

enum class AvailabilityMode { OFF, ON, ALWAYS_ON }

/** dayOfWeek: 1 = Senin ... 7 = Minggu (ISO). openMin/closeMin = menit sejak 00:00, hanya untuk mode ON. */
data class DayAvailability(
    val dayOfWeek: Int,
    val mode: AvailabilityMode,
    val openMin: Int? = null,
    val closeMin: Int? = null
)

enum class ProviderBadge { NEW, TRUSTED, NONE }
```

### 7.1 F1 — Smart Service Matchmaking (`MatchmakingUseCase`)

**Aturan**
1. Kandidat = mitra **aktif**: tidak `is_suspended`, punya ≥ 1 layanan aktif di kategori terpilih, dan punya ≥ 1 hari dengan mode ≠ `OFF`; bukan pengguna itu sendiri.
2. Jarak Haversine (R = 6371 km). Jarak **≤ 10 km** (inklusif) masuk rekomendasi; selainnya masuk daftar `outOfRange`.
3. `Match Score = 0.50 × Skor Jarak + 0.35 × Skor Rating + 0.15 × Skor Penyelesaian`
   - Skor Jarak = `1 − jarak/10`
   - Skor Rating = `rating_avg / 5` (null → 0.7)
   - Skor Penyelesaian = `completion_rate` 0..1 (null → 0.7)
4. Mitra `flag_anomaly = true` → skor akhir dikali **0.7** (tetap tampil dengan label peringatan).
5. Urut menurun skor akhir; tie-break: rating lebih tinggi, lalu jarak lebih dekat.
6. Tampilan: `matchScore = round(skorAkhir × 100)` (0–100).
7. UI menyediakan filter: jarak maks (slider 1–10 km) dan rating minimum (0–5) — filter di sisi klien setelah ranking.

```kotlin
data class ProviderCandidate(
    val providerId: String, val name: String, val avatarUrl: String?,
    val location: GeoPoint,
    val ratingAvg: Double?,          // 0..5, null jika belum ada
    val completionRate: Double?,     // 0..1, null jika belum ada
    val flagAnomaly: Boolean, val badge: ProviderBadge,
    val minBaseFare: Int?            // untuk tampilan "mulai dari"
)
data class RankedProvider(
    val candidate: ProviderCandidate, val distanceKm: Double,
    val distanceScore: Double, val ratingScore: Double, val completionScore: Double,
    val finalScore: Double, val matchScore: Int
)
data class MatchResult(val ranked: List<RankedProvider>, val outOfRange: List<ProviderCandidate>)

object HaversineCalculator {
    private const val EARTH_RADIUS_KM = 6371.0
    fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(a.lat)) * Math.cos(Math.toRadians(b.lat)) *
            Math.sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(h)))
    }
}

class MatchmakingUseCase {
    operator fun invoke(customer: GeoPoint, candidates: List<ProviderCandidate>): MatchResult {
        val withDist = candidates.map { it to HaversineCalculator.distanceKm(customer, it.location) }
        val (inRange, out) = withDist.partition { it.second <= BantuInConfig.MAX_RADIUS_KM }
        val ranked = inRange.map { (c, d) ->
            val ds = 1.0 - d / BantuInConfig.MAX_RADIUS_KM
            val rs = c.ratingAvg?.div(5.0) ?: BantuInConfig.DEFAULT_RATING_SCORE
            val cs = c.completionRate ?: BantuInConfig.DEFAULT_COMPLETION_SCORE
            var s = BantuInConfig.W_DISTANCE * ds + BantuInConfig.W_RATING * rs +
                    BantuInConfig.W_COMPLETION * cs
            if (c.flagAnomaly) s *= BantuInConfig.ANOMALY_PENALTY_FACTOR
            RankedProvider(c, d, ds, rs, cs, s, Math.round(s * 100).toInt())
        }.sortedWith(
            compareByDescending<RankedProvider> { it.finalScore }
                .thenByDescending { it.candidate.ratingAvg ?: 0.0 }
                .thenBy { it.distanceKm }
        )
        return MatchResult(ranked, out.map { it.first })
    }
}
```

*(Catatan: pastikan pemanggilan konstanta di kode menggunakan `BantuinConfig`)*

**UI (Screen 2)**: kartu mitra menampilkan nama, jarak (1 desimal), rating, Match Score, badge (Terpercaya hijau / Mitra Baru netral), label peringatan kecil bila anomali, "mulai dari Rp …". Bila `ranked` kosong: tampilkan pesan "Tidak ada mitra dalam radius 10 km" (+ jumlah `outOfRange` bila ada).

### 7.2 F2 — Dynamic Cost & Surge Estimator (`CostEstimatorUseCase`)

**Rumus**
```
raw       = (baseFare + jarakKm × 2000) × bobotKompleksitas      // jarakKm dibulatkan 1 desimal (HALF_UP)
baseCost  = roundUp500(raw)
isSurge   = jadwalDiJamSibukAtauMalam || isUrgent
total     = isSurge ? roundUp500(raw × 1.20) : baseCost
surgeAmt  = total − baseCost
```
- `baseFare` dari `services.base_fare` (diisi mitra). Bobot: RINGAN 1.00 / SEDANG 1.25 / BERAT 1.50 (dipilih **pelanggan**).
- Surge **tunggal +20%**: jam sibuk + urgent = tetap ×1.20.
- Waktu yang dinilai = **jam mulai jadwal layanan** yang dipilih (zona WIB), bukan waktu menekan tombol.
- Gunakan `BigDecimal` (bukan `Double`) untuk uang.
- Hasil disimpan sebagai **snapshot** di `bookings`.

```kotlin
enum class SurgeReason { NONE, PEAK_HOUR, NIGHT, URGENT, PEAK_AND_URGENT, NIGHT_AND_URGENT }

data class CostEstimate(
    val baseFare: Int, val distanceKm: Double, val distanceCost: Int,
    val complexity: Complexity, val baseCost: Int,
    val surgeApplied: Boolean, val surgeAmount: Int, val surgeReason: SurgeReason,
    val total: Int
)

class CostEstimatorUseCase {
    operator fun invoke(
        baseFare: Int, distanceKm: Double, complexity: Complexity,
        startMinOfDay: Int, isUrgent: Boolean
    ): CostEstimate {
        val dist = java.math.BigDecimal.valueOf(distanceKm).setScale(1, java.math.RoundingMode.HALF_UP)
        val distCost = dist.multiply(java.math.BigDecimal(BantuinConfig.RATE_PER_KM))
        val raw = (java.math.BigDecimal(baseFare) + distCost).multiply(complexity.weight)
        val baseCost = roundUp(raw)
        val peak = BantuinConfig.PEAK_RANGES.any { startMinOfDay in it }
        val night = startMinOfDay >= BantuinConfig.NIGHT_START_MIN || startMinOfDay < BantuinConfig.NIGHT_END_MIN
        val surge = peak || night || isUrgent
        val total = if (surge) roundUp(raw.multiply(java.math.BigDecimal(BantuinConfig.SURGE_MULTIPLIER))) else baseCost
        val reason = when {
            !surge -> SurgeReason.NONE
            peak && isUrgent -> SurgeReason.PEAK_AND_URGENT
            night && isUrgent -> SurgeReason.NIGHT_AND_URGENT
            peak -> SurgeReason.PEAK_HOUR
            night -> SurgeReason.NIGHT
            else -> SurgeReason.URGENT
        }
        return CostEstimate(baseFare, dist.toDouble(), distCost.setScale(0, java.math.RoundingMode.HALF_UP).toInt(),
            complexity, baseCost, surge, total - baseCost, reason, total)
    }
    private fun roundUp(x: java.math.BigDecimal): Int =
        x.divide(java.math.BigDecimal(BantuinConfig.ROUNDING_STEP), 0, java.math.RoundingMode.CEILING)
            .multiply(java.math.BigDecimal(BantuinConfig.ROUNDING_STEP)).toInt()
}
```

**UI (Screen 4)**: rincian baris demi baris (tarif dasar, biaya jarak, pengali kompleksitas, subtotal, surge + label "Jam Sibuk"/"Malam"/"Darurat", total tebal), pemilih kompleksitas (3 chip), switch "Layanan darurat", catatan, tombol **Kirim Pesanan**. Mengubah kompleksitas/urgent menghitung ulang seketika tanpa memanggil jaringan.

### 7.3 F3 — Automated SLA & Scheduling Conflict Detector (`SchedulingConflictDetector`)

**Ketersediaan mitra** (per hari, diatur mitra): `OFF` = tidak menerima; `ON` = hanya dalam `[openMin, closeMin)`; `ALWAYS_ON` = 24 jam `[0, 1440)`. Pesanan **tidak melewati tengah malam**.

**Blok waktu** = `[mulai, mulai + durasi + 30)` (half-open, menit sejak 00:00 WIB). Blok pesanan existing memakai definisi sama (kolom `start_at`/`end_at` di DB sudah termasuk buffer).

**Validasi urut**: (1) hari `OFF` → `DAY_OFF`; (2) tanggal hari ini & mulai < sekarang → `PAST_TIME`; (3) blok di luar jendela operasional → `OUTSIDE_HOURS`; (4) overlap `a.start < b.end && b.start < a.end` dengan blok existing → `OVERLAP`. Blok bersebelahan tepat (end == start) **bukan** overlap.

**Rekomendasi slot**: bila tidak valid (kecuali `DAY_OFF`), kandidat = akhir tiap blok existing ∪ grid kelipatan 30 menit; ambil kandidat **> jam yang diminta** yang lolos validasi, urut waktu, **maks 3**. Bila kosong, UI menyarankan memilih tanggal lain.

```kotlin
data class TimeBlock(val startMin: Int, val endMin: Int)
enum class ConflictReason { DAY_OFF, PAST_TIME, OUTSIDE_HOURS, OVERLAP }
data class SlotCheckResult(
    val valid: Boolean, val reason: ConflictReason?, val block: TimeBlock,
    val alternativeStartsMin: List<Int>
)

class SchedulingConflictDetector {
    fun blockFor(startMin: Int, durationMin: Int) =
        TimeBlock(startMin, startMin + durationMin + BantuinConfig.TRAVEL_BUFFER_MIN)

    fun overlaps(a: TimeBlock, b: TimeBlock) = a.startMin < b.endMin && b.startMin < a.endMin

    private fun window(day: DayAvailability): TimeBlock? = when (day.mode) {
        AvailabilityMode.OFF -> null
        AvailabilityMode.ALWAYS_ON -> TimeBlock(0, 1440)
        AvailabilityMode.ON -> TimeBlock(day.openMin!!, day.closeMin!!)
    }

    private fun violation(block: TimeBlock, win: TimeBlock?, existing: List<TimeBlock>, nowMin: Int?): ConflictReason? {
        if (win == null) return ConflictReason.DAY_OFF
        if (nowMin != null && block.startMin < nowMin) return ConflictReason.PAST_TIME
        if (block.startMin < win.startMin || block.endMin > win.endMin) return ConflictReason.OUTSIDE_HOURS
        if (existing.any { overlaps(block, it) }) return ConflictReason.OVERLAP
        return null
    }

    /** nowMin: isi menit sekarang (WIB) jika tanggal yang dipilih = hari ini, selain itu null. */
    fun check(requestedStartMin: Int, durationMin: Int, day: DayAvailability,
              existing: List<TimeBlock>, nowMin: Int?): SlotCheckResult {
        val block = blockFor(requestedStartMin, durationMin)
        val win = window(day)
        val reason = violation(block, win, existing, nowMin)
        if (reason == null) return SlotCheckResult(true, null, block, emptyList())
        if (reason == ConflictReason.DAY_OFF) return SlotCheckResult(false, reason, block, emptyList())
        val grid = (0 until 1440 step BantuinConfig.SLOT_GRID_MIN).toList()
        val candidates = (existing.map { it.endMin } + grid + listOfNotNull(win?.startMin))
            .distinct().sorted().filter { it > requestedStartMin }
        val alts = candidates.filter { violation(blockFor(it, durationMin), win, existing, nowMin) == null }
            .take(BantuinConfig.MAX_ALTERNATIVE_SLOTS)
        return SlotCheckResult(false, reason, block, alts)
    }
}
```

**Alur UI (Screen 3 → 4) dan penguncian**
1. Pelanggan memilih tanggal & jam. Aplikasi memanggil RPC `get_busy_blocks` (blok aktif + kunci milik pelanggan lain), mengonversi ke menit-of-day WIB, lalu `check(...)`.
2. Tidak valid → tampilkan alasan (teks Indonesia per `ConflictReason`) + chip slot alternatif yang bisa dipilih; tombol Lanjut nonaktif.
3. Valid → panggil RPC `acquire_slot_lock` (kunci **10 menit**) lalu navigasi ke Screen 4 dengan hitung mundur kunci. Kunci habis → kembali ke Screen 3 dengan pesan.
4. Kirim pesanan → RPC `create_booking` (mengonsumsi kunci). Bila server menolak (`SLOT_CONFLICT`/`LOCK_EXPIRED`), kembali ke Screen 3.
5. Meninggalkan alur → RPC `release_slot_lock`.

### 7.4 F4 — Provider Trust Score & Anomaly Analyzer

Gunakan jendela 30 hari. Waktu referensi pesanan adalah `finished_at` jika tersedia, atau `created_at` untuk pesanan lainnya. Ulasan memakai waktu dibuat.

- Skor Rating = rata-rata rating / 5 × 100. Tanpa ulasan, nilainya 0.
- On-Time SLA = jumlah `SELESAI_TEPAT_WAKTU` / semua pesanan selesai × 100. Pesanan lama tanpa konfirmasi memakai toleransi kedatangan 15 menit.
- Rasio Pembatalan = jumlah pembatalan pelanggan dan mitra / semua pesanan dalam jendela tersebut × 100.
- Trust Score = 0,45 × Skor Rating + 0,35 × On-Time SLA − 0,20 × Rasio Pembatalan, dibatasi ke 0–100.
- Anomali berlaku pada Trust Score < 60 atau Rasio Pembatalan > 20%. Ambang dibandingkan sebelum pembulatan tampilan. Penyebut kosong menghasilkan 0. Mitra aman menerima badge Terpercaya dan mitra anomali menerima penalti ×0,7 di F1.

Screen 5 menyimpan status penyelesaian dan ulasan, memulihkan data saat pesanan dibuka kembali, serta menyediakan perubahan dan penghapusan ulasan. Penyelesaian dan pembayaran aktual mengikuti alur pesanan yang sudah ada. Screen 6 menampilkan metrik, tren, dan penyebab anomali. Screen 7 menampilkan evaluasi dan dampak pada prioritas rekomendasi.

Migrasi 005 menghitung metrik di server setelah mutasi pesanan atau ulasan, sebelum transaksi penyimpanan selesai.

---

## 8. Test Vector (Wajib menjadi Unit Test)

### 8.1 F1 (customer di titik acuan; gunakan jarak yang diberikan langsung lewat fungsi skor atau titik koordinat yang menghasilkan jarak tsb)
| Mitra | Jarak | Rating | Completion | Anomali | Hasil |
|---|---|---|---|---|---|
| A | 2 km | 4.5 | 0.9 | tidak | skor 0.85 → **85** |
| B | 1 km | 4.0 | 1.0 | tidak | skor 0.88 → **88** |
| C | 11 km | 5.0 | 1.0 | tidak | **di luar radius** (outOfRange) |
| D | 0.5 km | 5.0 | 1.0 | **ya** | 0.975 × 0.7 = 0.6825 → **68** |
| E | 10 km | null | null | tidak | 0.5×0 + 0.35×0.7 + 0.15×0.7 = 0.35 → **35** (batas 10 km inklusif, termasuk) |

Urutan hasil: B (88), A (85), D (68), E (35). `HaversineCalculator`: jarak (−7.9797, 112.6304) ke dirinya sendiri = 0; antar dua titik yang berselisih 0.01° lintang ≈ 1.11 km (toleransi ±0.02).

### 8.2 F2
| baseFare | jarak | kompleksitas | jam mulai | urgent | baseCost | total |
|---|---|---|---|---|---|---|
| 100000 | 3.0 | SEDANG | 10:00 | tidak | 132500 | **132500** (surge tidak) |
| 100000 | 3.0 | SEDANG | 17:00 | tidak | 132500 | **159000** (surge 26500, PEAK_HOUR) |
| 50000 | 2.5 | RINGAN | 10:00 | ya | 55000 | **66000** (URGENT) |
| 75000 | 1.3 | BERAT | 10:00 | tidak | 116500 | **116500** |
| 75000 | 1.3 | BERAT | 21:00 | tidak | 116500 | **140000** (surge 23500, NIGHT) |
| 100000 | 3.0 | SEDANG | 17:00 | ya | 132500 | **159000** (PEAK_AND_URGENT, tidak menumpuk) |
| 100000 | 0.0 | RINGAN | 06:00 | tidak | 100000 | **100000** (batas 06:00 = normal) |
| 100000 | 0.0 | RINGAN | 05:59 (359 menit) | tidak | 100000 | **120000** (NIGHT) |

### 8.3 F3 (mitra mode ON 08:00–17:00 = 480..1020; existing: blok 09:00–11:30 = 540..690)
| Kasus | Input | Hasil |
|---|---|---|
| Overlap | mulai 10:00, durasi 60 | tidak valid `OVERLAP`; alternatif **11:30, 12:00, 12:30** |
| Bersebelahan | mulai 11:30, durasi 60 | **valid** (end 11:30 == start) |
| Di luar jam | mulai 16:00, durasi 60 (blok sampai 17:30) | `OUTSIDE_HOURS`; alternatif kosong |
| ALWAYS_ON batas | mulai 23:00, durasi 30 (blok sampai 24:00) | **valid** |
| ALWAYS_ON lewat | mulai 23:30, durasi 30 | `OUTSIDE_HOURS` |
| Hari OFF | mode OFF | `DAY_OFF`, alternatif kosong |
| Masa lalu | hari ini, nowMin = 600 (10:00), mulai 09:00 | `PAST_TIME` |

### 8.4 F4

| Kasus | Data | Hasil |
|---|---|---|
| Sehat | 6 tepat waktu, 2 terlambat, 2 batal, rating 4,5 | Skor **62,75**, pembatalan **20%**, badge Terpercaya |
| Pembatalan tinggi | 6 tepat waktu, 4 batal, rating 4 | Skor **63**, pembatalan **40%**, anomali pembatalan |
| Skor rendah | 1 tepat waktu, 4 terlambat, rating 2 | Skor **25**, anomali skor |
| Satu transaksi selesai | 1 tepat waktu, rating 5 | Skor **80**, badge Terpercaya |
| Tanpa ulasan | Ulasan kosong | Skor Rating **0** |

---

## 9. Database Supabase (`supabase/migrations/001_init.sql`)

Seluruh DDL, RLS, dan RPC di bawah adalah acuan final. Prinsip: **klien tidak boleh insert/update `bookings` langsung**; semua lewat RPC.

```sql
create extension if not exists btree_gist;
create extension if not exists pgcrypto;

-- ===== Tabel =====
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null,
  phone text,
  roles text[] not null default array['customer']
    check (roles <@ array['customer','provider']::text[] and cardinality(roles) >= 1),
  avatar_url text,
  created_at timestamptz not null default now()
);

create table public.service_categories (
  id serial primary key,
  name text not null unique,
  icon text not null default 'build',
  default_duration_min int not null default 60
);

create table public.provider_profiles (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  bio text,
  address text,
  lat double precision not null,
  lng double precision not null,
  is_suspended boolean not null default false,
  -- metrik cache (dihitung F4 di Kotlin, jendela 30 hari)
  rating_avg numeric(3,2),
  completion_rate numeric(5,4),
  on_time_rate numeric(5,2),
  cancel_rate numeric(5,2) not null default 0,
  trust_score numeric(5,2) not null default 70,
  tx_count_30d int not null default 0,
  flag_anomaly boolean not null default false,
  badge text not null default 'NEW' check (badge in ('NEW','TRUSTED','NONE')),
  metrics_updated_at timestamptz
);

create table public.services (
  id uuid primary key default gen_random_uuid(),
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  category_id int not null references public.service_categories(id),
  name text not null,
  description text,
  base_fare int not null check (base_fare >= 0),
  duration_min int not null check (duration_min between 15 and 720),
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

create table public.availability (
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  day_of_week smallint not null check (day_of_week between 1 and 7),  -- 1=Senin..7=Minggu
  mode text not null check (mode in ('OFF','ON','ALWAYS_ON')),
  open_time time,
  close_time time,
  primary key (provider_id, day_of_week),
  check (mode <> 'ON' or (open_time is not null and close_time is not null and open_time < close_time))
);

create table public.bookings (
  id uuid primary key default gen_random_uuid(),
  customer_id uuid not null references public.profiles(id),
  provider_id uuid not null references public.provider_profiles(user_id),
  service_id uuid not null references public.services(id),
  start_at timestamptz not null,
  end_at timestamptz not null,                 -- start + durasi + 30 menit buffer
  complexity text not null check (complexity in ('RINGAN','SEDANG','BERAT')),
  is_urgent boolean not null default false,
  distance_km numeric(5,1) not null,
  base_fare int not null,
  base_cost int not null,
  surge_applied boolean not null default false,
  total_cost int not null,
  customer_lat double precision not null,
  customer_lng double precision not null,
  notes text,
  status text not null default 'PENDING' check (status in
    ('PENDING','DIKONFIRMASI','DALAM_PENGERJAAN','SELESAI','DITOLAK','KEDALUWARSA',
     'DIBATALKAN_PELANGGAN','DIBATALKAN_MITRA')),
  expires_at timestamptz not null,
  started_at timestamptz,
  finished_at timestamptz,
  created_at timestamptz not null default now(),
  check (customer_id <> provider_id),
  check (end_at > start_at),
  constraint bookings_no_overlap exclude using gist (
    provider_id with =,
    tstzrange(start_at, end_at, '[)') with &&
  ) where (status in ('PENDING','DIKONFIRMASI','DALAM_PENGERJAAN'))
);
create index on public.bookings (provider_id, status);
create index on public.bookings (customer_id, status);

create table public.slot_locks (
  id uuid primary key default gen_random_uuid(),
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  locked_by uuid not null references public.profiles(id) on delete cascade,
  start_at timestamptz not null,
  end_at timestamptz not null,
  expires_at timestamptz not null
);

create table public.reviews (
  id uuid primary key default gen_random_uuid(),
  booking_id uuid not null unique references public.bookings(id) on delete cascade,
  customer_id uuid not null references public.profiles(id),
  provider_id uuid not null references public.provider_profiles(user_id),
  rating smallint not null check (rating between 1 and 5),
  comment text,
  created_at timestamptz not null default now()
);

create table public.booking_events (
  id bigint generated always as identity primary key,
  booking_id uuid not null references public.bookings(id) on delete cascade,
  status text not null,
  actor_id uuid,
  created_at timestamptz not null default now()
);

create table public.provider_metric_snapshots (
  id bigint generated always as identity primary key,
  provider_id uuid not null references public.provider_profiles(user_id) on delete cascade,
  computed_at timestamptz not null default now(),
  trust_score numeric(5,2) not null,
  rating_score numeric(5,2),
  on_time_rate numeric(5,2),
  cancel_rate numeric(5,2),
  tx_count int not null,
  flag_anomaly boolean not null
);

-- ===== Trigger: profil otomatis saat sign up =====
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, full_name, phone, roles)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', 'Pengguna'),
    new.raw_user_meta_data->>'phone',
    coalesce(string_to_array(new.raw_user_meta_data->>'roles', ','), array['customer'])
  );
  return new;
end $$;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- ===== Trigger: riwayat status =====
create or replace function public.log_booking_status() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if tg_op = 'INSERT' or new.status is distinct from old.status then
    insert into public.booking_events (booking_id, status, actor_id)
    values (new.id, new.status, auth.uid());
  end if;
  return new;
end $$;
create trigger trg_booking_status after insert or update of status on public.bookings
  for each row execute function public.log_booking_status();

-- ===== RPC =====
create or replace function public.get_busy_blocks(p_provider uuid, p_from timestamptz, p_to timestamptz)
returns table (start_at timestamptz, end_at timestamptz)
language sql stable security definer set search_path = public as $$
  select b.start_at, b.end_at from public.bookings b
   where b.provider_id = p_provider
     and b.status in ('PENDING','DIKONFIRMASI','DALAM_PENGERJAAN')
     and b.start_at < p_to and b.end_at > p_from
  union all
  select l.start_at, l.end_at from public.slot_locks l
   where l.provider_id = p_provider and l.expires_at > now()
     and l.locked_by <> auth.uid()
     and l.start_at < p_to and l.end_at > p_from;
$$;

create or replace function public.acquire_slot_lock(p_provider uuid, p_start timestamptz, p_end timestamptz)
returns uuid language plpgsql security definer set search_path = public as $$
declare v_id uuid;
begin
  delete from public.slot_locks where expires_at < now();
  delete from public.slot_locks where locked_by = auth.uid();          -- 1 kunci per pengguna
  if auth.uid() = p_provider then raise exception 'SELF_BOOKING'; end if;
  if exists (select 1 from public.bookings b
              where b.provider_id = p_provider
                and b.status in ('PENDING','DIKONFIRMASI','DALAM_PENGERJAAN')
                and tstzrange(b.start_at, b.end_at, '[)') && tstzrange(p_start, p_end, '[)')) then
    raise exception 'SLOT_CONFLICT';
  end if;
  if exists (select 1 from public.slot_locks l
              where l.provider_id = p_provider and l.locked_by <> auth.uid()
                and tstzrange(l.start_at, l.end_at, '[)') && tstzrange(p_start, p_end, '[)')) then
    raise exception 'SLOT_LOCKED';
  end if;
  insert into public.slot_locks (provider_id, locked_by, start_at, end_at, expires_at)
  values (p_provider, auth.uid(), p_start, p_end, now() + interval '10 minutes')
  returning id into v_id;
  return v_id;
end $$;

create or replace function public.release_slot_lock() returns void
language sql security definer set search_path = public as $$
  delete from public.slot_locks where locked_by = auth.uid();
$$;

create or replace function public.create_booking(
  p_lock_id uuid, p_service_id uuid, p_complexity text, p_is_urgent boolean,
  p_distance_km numeric, p_base_fare int, p_base_cost int, p_surge_applied boolean, p_total_cost int,
  p_cust_lat double precision, p_cust_lng double precision, p_notes text
) returns uuid language plpgsql security definer set search_path = public as $$
declare l public.slot_locks; v_provider uuid; v_id uuid;
begin
  select * into l from public.slot_locks
   where id = p_lock_id and locked_by = auth.uid() and expires_at > now();
  if not found then raise exception 'LOCK_EXPIRED'; end if;
  select provider_id into v_provider from public.services where id = p_service_id and is_active;
  if v_provider is null or v_provider <> l.provider_id then raise exception 'INVALID_SERVICE'; end if;
  insert into public.bookings (customer_id, provider_id, service_id, start_at, end_at, complexity,
      is_urgent, distance_km, base_fare, base_cost, surge_applied, total_cost,
      customer_lat, customer_lng, notes, expires_at)
  values (auth.uid(), l.provider_id, p_service_id, l.start_at, l.end_at, p_complexity,
      p_is_urgent, p_distance_km, p_base_fare, p_base_cost, p_surge_applied, p_total_cost,
      p_cust_lat, p_cust_lng, p_notes, now() + interval '15 minutes')
  returning id into v_id;                -- exclusion constraint = pengaman terakhir (error 23P01)
  delete from public.slot_locks where id = p_lock_id;
  return v_id;
end $$;

create or replace function public.expire_pending_bookings() returns void
language sql security definer set search_path = public as $$
  update public.bookings set status = 'KEDALUWARSA'
   where status = 'PENDING' and expires_at < now();
$$;

create or replace function public.update_booking_status(p_booking uuid, p_new text)
returns void language plpgsql security definer set search_path = public as $$
declare b public.bookings; uid uuid := auth.uid();
begin
  select * into b from public.bookings where id = p_booking for update;
  if not found then raise exception 'NOT_FOUND'; end if;
  if uid = b.provider_id then
    if b.status = 'PENDING' and p_new in ('DIKONFIRMASI','DITOLAK') then
      if b.expires_at <= now() then raise exception 'EXPIRED'; end if;
      update public.bookings set status = p_new where id = p_booking;
    elsif b.status = 'DIKONFIRMASI' and p_new = 'DALAM_PENGERJAAN' then
      update public.bookings set status = p_new, started_at = now() where id = p_booking;
    elsif b.status = 'DALAM_PENGERJAAN' and p_new = 'SELESAI' then
      update public.bookings set status = p_new, finished_at = now() where id = p_booking;
    elsif b.status = 'DIKONFIRMASI' and p_new = 'DIBATALKAN_MITRA' then
      update public.bookings set status = p_new, finished_at = now() where id = p_booking;
    else raise exception 'INVALID_TRANSITION'; end if;
  elsif uid = b.customer_id then
    if b.status in ('PENDING','DIKONFIRMASI') and p_new = 'DIBATALKAN_PELANGGAN' then
      update public.bookings set status = p_new, finished_at = now() where id = p_booking;
    else raise exception 'INVALID_TRANSITION'; end if;
  else raise exception 'FORBIDDEN'; end if;
end $$;

create or replace function public.apply_provider_metrics(
  p_provider uuid, p_rating_avg numeric, p_completion_rate numeric, p_on_time_rate numeric,
  p_cancel_rate numeric, p_trust_score numeric, p_tx_count int, p_flag boolean, p_badge text,
  p_rating_score numeric
) returns void language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() <> p_provider and not exists (
      select 1 from public.bookings where provider_id = p_provider and customer_id = auth.uid()) then
    raise exception 'FORBIDDEN';
  end if;
  update public.provider_profiles set
    rating_avg = p_rating_avg, completion_rate = p_completion_rate, on_time_rate = p_on_time_rate,
    cancel_rate = p_cancel_rate, trust_score = p_trust_score, tx_count_30d = p_tx_count,
    flag_anomaly = p_flag, badge = p_badge, metrics_updated_at = now()
  where user_id = p_provider;
  insert into public.provider_metric_snapshots
    (provider_id, trust_score, rating_score, on_time_rate, cancel_rate, tx_count, flag_anomaly)
  values (p_provider, p_trust_score, p_rating_score, p_on_time_rate, p_cancel_rate, p_tx_count, p_flag);
end $$;

grant execute on function
  public.get_busy_blocks(uuid, timestamptz, timestamptz), public.acquire_slot_lock(uuid, timestamptz, timestamptz),
  public.release_slot_lock(), public.expire_pending_bookings(), public.update_booking_status(uuid, text)
  to authenticated;
-- (grant juga create_booking dan apply_provider_metrics dengan signature lengkapnya)

-- ===== RLS =====
alter table public.profiles enable row level security;
alter table public.service_categories enable row level security;
alter table public.provider_profiles enable row level security;
alter table public.services enable row level security;
alter table public.availability enable row level security;
alter table public.bookings enable row level security;
alter table public.slot_locks enable row level security;
alter table public.reviews enable row level security;
alter table public.booking_events enable row level security;
alter table public.provider_metric_snapshots enable row level security;

create policy profiles_select on public.profiles for select to authenticated using (true);
create policy profiles_update_own on public.profiles for update to authenticated
  using (id = auth.uid()) with check (id = auth.uid());
create policy categories_select on public.service_categories for select to authenticated using (true);
create policy pp_select on public.provider_profiles for select to authenticated using (true);
create policy pp_insert_own on public.provider_profiles for insert to authenticated with check (user_id = auth.uid());
create policy pp_update_own on public.provider_profiles for update to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy services_select on public.services for select to authenticated
  using (is_active or provider_id = auth.uid());
create policy services_write_own on public.services for all to authenticated
  using (provider_id = auth.uid()) with check (provider_id = auth.uid());
create policy availability_select on public.availability for select to authenticated using (true);
create policy availability_write_own on public.availability for all to authenticated
  using (provider_id = auth.uid()) with check (provider_id = auth.uid());
create policy bookings_select on public.bookings for select to authenticated
  using (customer_id = auth.uid() or provider_id = auth.uid());          -- tidak ada policy insert/update (RPC saja)
create policy locks_select_own on public.slot_locks for select to authenticated using (locked_by = auth.uid());
create policy reviews_select on public.reviews for select to authenticated using (true);
create policy reviews_insert on public.reviews for insert to authenticated with check (
  customer_id = auth.uid() and exists (
    select 1 from public.bookings b
     where b.id = booking_id and b.customer_id = auth.uid()
       and b.provider_id = reviews.provider_id and b.status = 'SELESAI'));
create policy events_select on public.booking_events for select to authenticated using (
  exists (select 1 from public.bookings b where b.id = booking_id
          and (b.customer_id = auth.uid() or b.provider_id = auth.uid())));
create policy snapshots_select_own on public.provider_metric_snapshots for select to authenticated
  using (provider_id = auth.uid());

alter publication supabase_realtime add table public.bookings;

-- ===== Seed kategori =====
insert into public.service_categories (name, icon, default_duration_min) values
  ('Teknisi Listrik','bolt',60), ('Teknisi AC & Elektronik','ac_unit',90),
  ('Tukang Bangunan','construction',180), ('Jasa Kebersihan','cleaning_services',120),
  ('Angkut Barang','local_shipping',120), ('Pengajar Privat','school',90);
```

**Catatan keamanan (cakupan tugas kuliah)**: biaya dan metrik dihitung di klien sesuai tujuan akademik (logika terlihat di kode Kotlin). Di produksi nyata, perhitungan harus dipindah ke server. Jangan pernah menaruh `service_role` key di aplikasi.

**Pemetaan error RPC → pesan UI**: `SLOT_CONFLICT`/`SLOT_LOCKED`/error `23P01` → "Slot baru saja diambil, pilih jam lain"; `LOCK_EXPIRED` → "Waktu penahanan slot habis, pilih jadwal lagi"; `EXPIRED` → "Pesanan sudah kedaluwarsa"; `INVALID_TRANSITION`/`FORBIDDEN` → "Aksi tidak diizinkan"; `SELF_BOOKING` → "Tidak bisa memesan layanan sendiri".

**Konversi waktu**: simpan `timestamptz` (UTC). Konversi ke WIB hanya di lapisan presentasi/domain. Menit-of-day dihitung di WIB. Jendela hari untuk `get_busy_blocks` = `[00:00, 24:00)` WIB pada tanggal terpilih.

---

## 10. Arsitektur Aplikasi

### 10.1 Stack & dependensi
Kotlin, Compose + Material 3, Navigation Compose, Hilt (+ hilt-navigation-compose), `supabase-kt` (modul `postgrest-kt`, `auth-kt`, `realtime-kt`) + Ktor client engine Android, kotlinx-serialization, kotlinx-datetime, Coroutines/Flow, OSMDroid, Play Services Location, Coil (avatar), DataStore (peran aktif), JUnit + Turbine + MockK (opsional) untuk test.

### 10.2 Struktur paket
```
app/src/main/java/id/bantuin/app/
├── BantuinApp.kt, MainActivity.kt
├── core/            config/BantuinConfig.kt, util/ (TimeUtils WIB, Rupiah formatter), result/AppResult.kt, dispatcher/
├── domain/
│   ├── model/       (Bagian 7.0)
│   ├── repository/  interface: Auth, Provider, Catalog, Booking, Review, Metrics
│   └── usecase/     matchmaking/, pricing/, scheduling/, trust/, RecalculateProviderMetricsUseCase
├── data/
│   ├── remote/      SupabaseModule.kt (Hilt), dto/ (@Serializable), mapper/
│   └── repository/  implementasi repository
└── ui/
    ├── theme/       Color.kt, Theme.kt, Type.kt
    ├── navigation/  NavGraph.kt, Routes.kt, BottomBar
    ├── common/      komponen: LoadingState, ErrorState, EmptyState, RupiahText, BadgeChip, RatingBar, SurgeLabel
    ├── auth/        LoginScreen, RegisterScreen, AuthViewModel
    ├── customer/    home, recommendation, providerdetail, schedule, estimate, orders, orderdetail, profile
    └── provider/    home, orders, calendar, services, profile, dashboard, evaluation
```
Pola per layar: `XScreen` (stateless Composable) + `XViewModel` (Hilt, `StateFlow<XUiState>` + `Channel/SharedFlow` untuk event sekali pakai) + `XUiState` (data class/sealed: Loading/Success/Error).

### 10.3 Interface repository (kontrak minimum)
```kotlin
interface AuthRepository {
    suspend fun signUp(email: String, password: String, fullName: String, phone: String?, roles: Set<String>): AppResult<Unit>
    suspend fun signIn(email: String, password: String): AppResult<Unit>
    suspend fun signOut()
    fun sessionUser(): Flow<UserSession?>                 // id, profil, roles
    suspend fun enableProviderRole(): AppResult<Unit>
}
interface CatalogRepository {
    suspend fun categories(): AppResult<List<ServiceCategory>>
    suspend fun candidates(categoryId: Int): AppResult<List<ProviderCandidate>>   // join provider_profiles+profiles+services+availability
    suspend fun servicesOf(providerId: String): AppResult<List<ServiceItem>>
    suspend fun upsertService(s: ServiceItem): AppResult<Unit>; suspend fun deleteService(id: String): AppResult<Unit>
    suspend fun availability(providerId: String): AppResult<List<DayAvailability>>
    suspend fun saveAvailability(providerId: String, days: List<DayAvailability>): AppResult<Unit>
    suspend fun upsertProviderProfile(p: ProviderProfileInput): AppResult<Unit>
}
interface BookingRepository {
    suspend fun busyBlocks(providerId: String, date: LocalDate): AppResult<List<TimeBlock>>
    suspend fun acquireLock(providerId: String, startAt: Instant, endAt: Instant): AppResult<LockInfo>
    suspend fun releaseLock()
    suspend fun createBooking(req: CreateBookingRequest): AppResult<String>
    suspend fun updateStatus(bookingId: String, newStatus: BookingStatus): AppResult<Unit>
    suspend fun myBookings(asRole: Role): AppResult<List<BookingItem>>
    fun observeBooking(id: String): Flow<BookingItem>         // Realtime
    suspend fun events(bookingId: String): AppResult<List<BookingEvent>>
    suspend fun expirePending()
}
interface ReviewRepository {
    suspend fun submit(bookingId: String, providerId: String, rating: Int, comment: String?): AppResult<Unit>
    suspend fun reviewsOf(providerId: String): AppResult<List<ReviewItem>>
}
interface MetricsRepository {
    suspend fun trustInput(providerId: String, sinceDays: Int): AppResult<TrustInput>
    suspend fun apply(providerId: String, result: TrustResult): AppResult<Unit>
    suspend fun history(providerId: String): AppResult<List<MetricSnapshot>>
}
```
`candidates(categoryId)` memuat jumlah hari tersedia agar filter "aktif" (punya ≥ 1 hari ≠ OFF) bisa diterapkan; mitra tanpa baris `availability` dianggap tidak aktif.

### 10.4 Navigasi
- **Auth graph**: `login`, `register`. Setelah login, jika `roles` memuat keduanya → tampilkan pemilih peran (atau tombol ganti peran di Profil); peran aktif disimpan di DataStore.
- **Bottom Navigation (4 menu, sesuai desain)**:

| Pelanggan | Mitra |
|---|---|
| Beranda (`customer_home`) | Beranda (`provider_home`, ringkasan: pesanan menunggu, jadwal hari ini, Trust Score mini, toggle cepat) |
| Pesanan (`customer_orders`, aktif & riwayat) | Pesanan (`provider_orders`, antrian: terima/tolak/mulai/selesai/batal) |
| Jadwal (`customer_schedule`, pesanan mendatang) | Jadwal (`provider_calendar`, kalender + pengaturan OFF/ON/ALWAYS_ON per hari) |
| Profil (`customer_profile`) | **Dashboard** (`provider_dashboard`) → ke `provider_evaluation`, `provider_services`, `provider_profile_edit` |

- **Alur order** (nested graph `order_flow`, berbagi satu `OrderFlowViewModel` untuk `OrderDraft`): `recommendations/{categoryId}` → `provider/{providerId}` (pilih layanan) → `schedule` (F3) → `estimate` (F2) → `order/{bookingId}` (tracking & ulasan).
- `OrderDraft`: kategori, mitra, layanan, tanggal, jam mulai, hasil `SlotCheckResult`, `lockInfo`, kompleksitas, isUrgent, catatan, lokasi pelanggan, jarak.

### 10.5 Peta & lokasi
- GPS lewat Fused Location (izin runtime dengan penjelasan). Ditolak → fallback pilih titik di peta OSMDroid atau input manual; titik default Malang (−7.9797, 112.6304).
- OSMDroid dibungkus `AndroidView`; set `userAgentValue` ke package name dan tampilkan atribusi "© OpenStreetMap contributors". Peta dipakai untuk memilih lokasi pelanggan (Screen 1) dan lokasi mitra (Edit Profil Mitra).

### 10.6 Realtime
`observeBooking` dan daftar pesanan berlangganan perubahan tabel `bookings` (RLS berlaku). Pada Screen 5 dan antrian mitra, status berubah tanpa refresh manual. Panggil `expirePending()` saat membuka daftar pesanan dan memakai timer lokal untuk hitung mundur 15 menit pada pesanan `PENDING`.

---

## 11. Layar & Perilaku UI

| # | Layar | Peran | Isi & aksi | Sumber |
|---|---|---|---|---|
| 1 | Login & Home | Semua | Login/Register; Beranda: sapaan, lokasi saat ini (ubah via peta), grid 6 kategori jasa | CRUD-0/1 |
| 2 | Rekomendasi Mitra | Pelanggan | Daftar terurut Match Score, filter jarak & rating, state kosong | **F1** |
| 2b | Detail Mitra | Pelanggan | Profil, badge, rating, ulasan terbaru, daftar layanan (tarif dasar & durasi); pilih layanan | CRUD-1/2 |
| 3 | Pilih Jam & SLA | Pelanggan | Date picker (hari ini ke depan), time picker, status validasi, chip slot alternatif | **F3** |
| 4 | Estimasi & Order | Pelanggan | Rincian tarif, kompleksitas, urgent, catatan, hitung mundur kunci, Kirim Pesanan | **F2** |
| 5 | Tracking & Ulasan | Pelanggan | Timeline status real-time (dari `booking_events`), batalkan, ulasan (bintang + komentar) saat `SELESAI` | CRUD-4 |
| 6 | Dashboard Mitra | Mitra | Trust Score, metrik, grafik, Alert Card/Badge | **F4** |
| 7 | Evaluasi & Prioritas | Mitra | Rincian skor, pengali prioritas, sinkronisasi | **F4** → F1 |
| – | Pesanan Mitra | Mitra | Tab: Menunggu (terima/tolak + sisa waktu), Terkonfirmasi (mulai/batal), Berjalan (selesai), Riwayat | CRUD-3 |
| – | Jadwal Mitra | Mitra | Daftar hari Sen–Min dengan pemilih mode OFF/ON/ALWAYS_ON + jam buka/tutup (mode ON), tampilan pesanan per tanggal | CRUD-3 |
| – | Kelola Layanan | Mitra | CRUD layanan: kategori, nama, tarif dasar, durasi (default dari kategori), aktif/nonaktif | CRUD-2 |
| – | Edit Profil Mitra | Mitra | Bio, alamat, titik lokasi di peta. Onboarding mitra baru: profil → layanan pertama → ketersediaan (default Sen–Sab ON 08:00–17:00, Minggu OFF) | CRUD-1 |

### 11.1 Desain visual (Material 3)
- Warna: `primary #F57C00` (oranye), `onPrimary #FFFFFF`, `primaryContainer #FFE0B2`, `background #F7F6F4` (putih keabuan), `surface #FFFFFF`, `error #D32F2F` (Alert Card anomali), `success #2E7D32` (badge Terpercaya), badge Mitra Baru netral abu-biru.
- Komponen standar M3: `Scaffold`, `NavigationBar`, `Card`, `FilterChip`, `ModalBottomSheet`, `DatePicker/TimePicker`. Satu tema, tanpa dark mode kustom (boleh mengikuti sistem selama kontras terjaga).
- Setiap layar memiliki state **loading**, **empty**, **error (dengan tombol coba lagi)**.
- Referensi desain UI (hasil ekspor layar Figma): folder `docs/design/` (gunakan seluruh file gambar pada direktori ini sebagai acuan tata letak, hierarki komponen, dan gaya visual setiap layar).

### 11.2 Validasi form
- Email valid; password ≥ 8 karakter; nama wajib; no. HP opsional (format Indonesia bila diisi).
- Layanan: nama wajib, `base_fare` ≥ 0 dan kelipatan 500 (disarankan), durasi 15–720 menit.
- Mode ON: jam buka < jam tutup; mode ALWAYS_ON/OFF menyembunyikan input jam.
- Ulasan: rating wajib 1–5; komentar maks 300 karakter.
- Pesanan: lokasi pelanggan wajib ada (GPS atau peta).

---

## 12. Fase Pengembangan (Urutan Kerja Agent)

Setiap fase diakhiri build sukses + **Definition of Done (DoD)** terpenuhi.

| Fase | Pekerjaan | DoD |
|---|---|---|
| **P0 Setup** | Proyek Android (Compose, M3, Hilt, Nav, Version Catalog), `BantuinConfig`, `BuildConfig` dari `local.properties`, tema oranye, `AppResult`, util WIB & Rupiah, `docs/DECISIONS.md`, `README.md` (cara setup Supabase & build) | Build jalan, layar placeholder 4 tab tampil |
| **P1 Database** | Jalankan `001_init.sql` di Supabase; verifikasi RLS dengan akun uji | Semua tabel/RPC ada; insert langsung ke `bookings` oleh klien **ditolak** |
| **P2 Auth** | Register/Login/Logout, profil, peran ganda, ganti peran, bottom nav per peran | Login lintas 2 akun; roles tersimpan |
| **P3 Profil Mitra & Katalog** (CRUD-1/2) | Onboarding mitra, edit profil + peta, kelola layanan, kategori di Beranda, Detail Mitra | Mitra bisa membuat layanan; muncul di Detail Mitra |
| **P4 Ketersediaan** (CRUD-3 bagian 1) | Jadwal Mitra OFF/ON/ALWAYS_ON per hari | Tersimpan & terbaca ulang |
| **P5 F1** | `MatchmakingUseCase` + unit test 8.1; Screen 2 dengan filter; lokasi GPS/peta | Test lulus; urutan di UI sesuai Match Score |
| **P6 F3** | `SchedulingConflictDetector` + test 8.3; Screen 3; `get_busy_blocks`, `acquire_slot_lock`, hitung mundur | Test lulus; dua akun tidak bisa mengunci slot yang sama |
| **P7 F2** | `CostEstimatorUseCase` + test 8.2; Screen 4; `create_booking` | Test lulus; booking tersimpan dengan snapshot biaya |
| **P8 Antrian & Status** (CRUD-3 bagian 2) | Pesanan Mitra (terima/tolak/mulai/selesai/batal), Screen 5 timeline real-time, batal pelanggan, kedaluwarsa 15 menit | Status berubah real-time di 2 perangkat |
| **P9 Ulasan & F4** (CRUD-4) | Ulasan, `TrustScoreUseCase` + test 8.4, `RecalculateProviderMetricsUseCase`, Screen 6 & 7, snapshot grafik | Test lulus; Alert Card muncul saat anomali |
| **P10 Feedback loop** | Pastikan anomali menurunkan peringkat di Screen 2 (×0.7) dan badge tampil di kartu | Demo: ubah data → urutan F1 berubah |
| **P11 Seed & Polish** | Data demo (Bagian 13), empty/error state, rotasi layar, README, tangkapan layar | Semua skenario demo (Bagian 14) berjalan |

## 13. Data Demo (Seed)

- **Akun**: `pelanggan1@bantuin.test`, `pelanggan2@bantuin.test`, `mitra01@bantuin.test` … `mitra15@bantuin.test` (password demo yang sama, mis. `Demo12345!`, hanya untuk demo).
- Buat akun lewat Supabase Auth (Dashboard atau skrip lokal memakai `service_role` **yang tidak di-commit**), lalu isi data lewat `supabase/seed_demo.sql` (mencari user berdasarkan email).
- 15 mitra tersebar 0.5–14 km dari titik acuan Alun-alun Malang (−7.9797, 112.6304), mencakup semua 6 kategori, tarif dasar bervariasi (Rp 40.000–250.000).
- Variasi wajib: ≥ 2 mitra **anomali** (satu cancel > 20%, satu Trust Score < 60), ≥ 2 mitra **baru** (< 5 transaksi), ≥ 3 mitra **Terpercaya**, 1 mitra **ALWAYS_ON**, 1 mitra semua hari **OFF** (tidak boleh muncul di F1), ≥ 2 mitra di luar radius 10 km, 1 mitra dengan jadwal padat (untuk demo F3).
- Riwayat `bookings` + `reviews` historis (selesai/batal/telat) untuk setiap mitra agar F4 menghasilkan angka yang konsisten dengan metrik cache.

## 14. Skenario Demo & Kriteria Keberhasilan

Video demo 1080p wajib memperlihatkan nilai tambah tiap fungsi:

1. **F1**: dari Beranda pilih kategori → daftar terurut Match Score; ubah filter jarak/rating; tunjukkan mitra anomali turun peringkat & mitra OFF tidak muncul.
2. **F3**: pilih jam yang bentrok → ditolak + 3 slot alternatif; pilih alternatif → valid; tunjukkan kunci 10 menit (akun kedua tidak bisa mengambil slot yang sama).
3. **F2**: rincian biaya jam normal vs jam sibuk vs urgent; total berubah sesuai rumus.
4. **Order**: kirim → mitra menerima → mulai → selesai, status berubah real-time di dua perangkat.
5. **F4**: pelanggan memberi rating rendah / mitra membatalkan beberapa pesanan → Trust Score turun → Alert Card merah → mitra turun peringkat di F1 (feedback loop).

**Kriteria keberhasilan global (Definition of Done proyek)**
- [ ] Semua test vector Bagian 8 lulus sebagai unit test otomatis.
- [ ] Alur Screen 1–7 berjalan end-to-end dengan data cloud Supabase pada dua akun/perangkat.
- [ ] Tidak ada operasi jaringan/perhitungan berat di main thread; layar tahan rotasi.
- [ ] RLS aktif; klien tidak dapat mengubah `bookings` di luar RPC; tidak ada rahasia di repository.
- [ ] Semua teks UI berbahasa Indonesia; setiap layar punya loading/empty/error state.
- [ ] README menjelaskan setup Supabase (jalankan migrasi, buat akun demo, isi `local.properties`).
- [ ] `docs/DECISIONS.md` mencatat setiap keputusan di luar dokumen ini.

## 15. Dukungan Penyusunan Laporan (Bab III–V)

Struktur kode sengaja memudahkan pengisian template laporan per fungsi:
- **A. Logika pemrosesan data** → kutip use case di `domain/usecase/*` (KDoc + rumus).
- **B. Layout & UI** → kutip Composable output fungsi (kartu rekomendasi, rincian biaya, chip slot, Alert Card).
- **C. Event handling & state** → `ViewModel` + `StateFlow` (`XUiState`) dan penanganan rotasi.
- **D. Komunikasi client-server/database** → repository Supabase, pemanggilan RPC, `Dispatchers.IO/Default`, Realtime.
- **Bab IV**: tangkapan layar tiap skenario Bagian 14, link GitHub (commit kolaboratif per anggota), link video demo; tabel kendala & solusi (mis. race condition slot → `slot_locks` + exclusion constraint).
- **Bab II (laporan)**: sesuaikan rumus Trust Score ke versi final di Bagian 7.4 agar laporan konsisten dengan aplikasi.

## 16. Risiko & Mitigasi

| Risiko | Mitigasi |
|---|---|
| Double booking saat pemesanan bersamaan | `slot_locks` 10 menit + exclusion constraint `bookings_no_overlap` + RPC transaksional |
| Satu pembatalan membuat mitra langsung anomali | Evaluasi hanya jika tx ≥ 5; mitra baru netral (70) |
| Mitra baru tertinggal di ranking | Nilai default rating/completion 0.7 |
| Pesanan `PENDING` menggantung | Kedaluwarsa 15 menit (`expire_pending_bookings` + timer klien) |
| Jaringan tidak stabil saat demo | Data seed lengkap, state error + coba lagi, rekam video cadangan |
| Kebocoran kunci API | `local.properties` + `.gitignore`; hanya `anon` key di aplikasi |
| Agent menyimpang dari spesifikasi | Test vector Bagian 8 sebagai gerbang wajib; keputusan di luar dokumen dicatat di `DECISIONS.md` |