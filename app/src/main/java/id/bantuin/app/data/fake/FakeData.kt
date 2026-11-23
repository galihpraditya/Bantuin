package id.bantuin.app.data.fake

import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ReviewItem
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.repository.UserProfile

object FakeData {
    val MALANG_CENTER = GeoPoint(-7.9797, 112.6304)

    val categories = listOf(
        ServiceCategory(1, "Teknisi Listrik", "bolt", 60, "Korslet, saklar, panel & MCB"),
        ServiceCategory(2, "Teknisi AC & Elektronik", "ac_unit", 90, "Cuci AC, perbaikan kulkas & TV"),
        ServiceCategory(3, "Tukang Bangunan", "construction", 180, "Atap bocor, kran air, cat dinding"),
        ServiceCategory(4, "Jasa Kebersihan", "cleaning_services", 120, "Deep clean kamar mandi & kos"),
        ServiceCategory(5, "Angkut Barang", "local_shipping", 120, "Pindahan kos, perabot & lemari"),
        ServiceCategory(6, "Pengajar Privat", "school", 90, "Kalkulus, Fisika Dasar, TOEFL & Ngaji")
    )

    // 15 Mitra Demo dengan variasi lengkap sesuai PRD Bagian 13
    val providers = listOf(
        // 1. Pak Budi Santoso (AC - Terpercaya, rujukan Screen 2-6)
        ProviderCandidate(
            providerId = "p01",
            name = "Budi Santoso, S.T.",
            avatarUrl = null,
            location = GeoPoint(-7.9650, 112.6320), // ~1.8 km
            address = "Jl. Kaliurang No. 14, Lowokwaru, Malang",
            bio = "Teknisi AC Berlisensi BNSP dengan 7+ tahun pengalaman.",
            ratingAvg = 4.9,
            reviewCount = 184,
            completionRate = 0.99,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 65000,
            responseTimeText = "< 5 mnt"
        ),
        // 2. Ahmad Fauzi (AC - Terpercaya)
        ProviderCandidate(
            providerId = "p02",
            name = "Ahmad Fauzi",
            avatarUrl = null,
            location = GeoPoint(-7.9520, 112.6250), // ~3.2 km
            address = "Jl. Soekarno Hatta No. 88, Malang",
            bio = "Spesialis pendingin ruangan dan elektronik rumah tangga.",
            ratingAvg = 4.8,
            reviewCount = 92,
            completionRate = 0.97,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 60000,
            responseTimeText = "~8 mnt"
        ),
        // 3. Surya Citra Teknik (AC - Terpercaya)
        ProviderCandidate(
            providerId = "p03",
            name = "Surya Citra Teknik",
            avatarUrl = null,
            location = GeoPoint(-7.9300, 112.6400), // ~6.5 km
            address = "Jl. Danau Toba G9/E12, Sawojajar, Malang",
            bio = "Solusi Cepat AC Split, Multi-split & Inverter.",
            ratingAvg = 4.7,
            reviewCount = 64,
            completionRate = 0.95,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 65000,
            responseTimeText = "~15 mnt"
        ),
        // 4. Pak Sugeng W. (Listrik - Terpercaya, rujukan Beranda)
        ProviderCandidate(
            providerId = "p04",
            name = "Pak Sugeng W.",
            avatarUrl = null,
            location = GeoPoint(-7.9750, 112.6310), // ~0.6 km
            address = "Jl. Ijen No. 25, Klojen, Malang",
            bio = "Spesialis Listrik Arus Kuat & Lemah, instalasi MCB rumah/kos.",
            ratingAvg = 4.9,
            reviewCount = 128,
            completionRate = 0.98,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 45000,
            responseTimeText = "< 5 mnt"
        ),
        // 5. Joko Hendro (Listrik - Anomali Cancel > 20%)
        ProviderCandidate(
            providerId = "p05",
            name = "Joko Hendro",
            avatarUrl = null,
            location = GeoPoint(-7.9700, 112.6200), // ~1.5 km
            address = "Jl. Bondowoso No. 4, Malang",
            bio = "Perbaikan lampu dan kelistrikan darurat.",
            ratingAvg = 4.1,
            reviewCount = 20,
            completionRate = 0.75, // cancel rate 25% > 20%
            flagAnomaly = true,
            badge = ProviderBadge.NONE,
            minBaseFare = 40000,
            responseTimeText = "~20 mnt"
        ),
        // 6. Rudi Instalasi (Listrik - Anomali Trust Score < 60)
        ProviderCandidate(
            providerId = "p06",
            name = "Rudi Hartono",
            avatarUrl = null,
            location = GeoPoint(-7.9600, 112.6450), // ~2.7 km
            address = "Jl. Sulfat No. 12, Blimbing, Malang",
            bio = "Layanan instalasi listrik kos.",
            ratingAvg = 2.4, // rating rendah
            reviewCount = 15,
            completionRate = 0.85,
            flagAnomaly = true,
            badge = ProviderBadge.NONE,
            minBaseFare = 45000,
            responseTimeText = "~30 mnt"
        ),
        // 7. Mitra Baru Listrik (tx < 5)
        ProviderCandidate(
            providerId = "p07",
            name = "Rian Pratama (Baru)",
            avatarUrl = null,
            location = GeoPoint(-7.9850, 112.6350), // ~0.8 km
            address = "Jl. Kawi No. 10, Klojen, Malang",
            bio = "Teknisi muda bersertifikasi vokasi.",
            ratingAvg = null,
            reviewCount = 2,
            completionRate = null,
            flagAnomaly = false,
            badge = ProviderBadge.NEW,
            minBaseFare = 40000,
            responseTimeText = "~10 mnt"
        ),
        // 8. Bangunan: Pak Slamet (Tukang Bangunan Terpercaya)
        ProviderCandidate(
            providerId = "p08",
            name = "Pak Slamet Renovasi",
            avatarUrl = null,
            location = GeoPoint(-7.9550, 112.6100), // ~3.5 km
            address = "Jl. MT Haryono No. 50, Dinoyo, Malang",
            bio = "Ahli perbaikan kebocoran atap dan instalasi kran sanitasi.",
            ratingAvg = 4.8,
            reviewCount = 55,
            completionRate = 0.96,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 80000,
            responseTimeText = "< 20 mnt"
        ),
        // 9. Bangunan: Tukang Kilat (ALWAYS_ON 24 Jam)
        ProviderCandidate(
            providerId = "p09",
            name = "Tukang Siaga 24 Jam",
            avatarUrl = null,
            location = GeoPoint(-7.9700, 112.6350), // ~1.2 km
            address = "Jl. Veteran No. 1, Klojen, Malang",
            bio = "Siap meluncur 24 jam untuk kondisi kebocoran pipa darurat.",
            ratingAvg = 4.6,
            reviewCount = 78,
            completionRate = 0.94,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 90000,
            responseTimeText = "< 10 mnt"
        ),
        // 10. Kebersihan: Bu Siti Kos Bersih (Terpercaya)
        ProviderCandidate(
            providerId = "p10",
            name = "Bu Siti Deep Clean",
            avatarUrl = null,
            location = GeoPoint(-7.9620, 112.6150), // ~2.5 km
            address = "Jl. Gajayana No. 22, Lowokwaru, Malang",
            bio = "Pembersihan total kamar mandi berkerak dan kamar kos.",
            ratingAvg = 4.9,
            reviewCount = 110,
            completionRate = 0.98,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 50000,
            responseTimeText = "< 15 mnt"
        ),
        // 11. Kebersihan: Mitra Baru Kebersihan
        ProviderCandidate(
            providerId = "p11",
            name = "Klin Kos Ceria (Baru)",
            avatarUrl = null,
            location = GeoPoint(-7.9780, 112.6280), // ~0.3 km
            address = "Jl. Semeru No. 3, Klojen, Malang",
            bio = "Jasa kebersihan higienis untuk kos mahasiswa.",
            ratingAvg = null,
            reviewCount = 1,
            completionRate = null,
            flagAnomaly = false,
            badge = ProviderBadge.NEW,
            minBaseFare = 45000,
            responseTimeText = "~10 mnt"
        ),
        // 12. Angkut: Mas Dimas Pick-up (Terpercaya)
        ProviderCandidate(
            providerId = "p12",
            name = "Dimas Pick-up Angkut",
            avatarUrl = null,
            location = GeoPoint(-7.9400, 112.6200), // ~4.6 km
            address = "Jl. Tlogomas No. 15, Malang",
            bio = "Armada bersih, terpal aman, bantu angkat barang sampai kamar.",
            ratingAvg = 4.8,
            reviewCount = 42,
            completionRate = 0.95,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 100000,
            responseTimeText = "~15 mnt"
        ),
        // 13. Privat: Anisa Les Privat (Terpercaya)
        ProviderCandidate(
            providerId = "p13",
            name = "Anisa Rahmawati, S.Si.",
            avatarUrl = null,
            location = GeoPoint(-7.9720, 112.6350), // ~1.1 km
            address = "Jl. Bandung No. 8, Klojen, Malang",
            bio = "Tutor Sains & Matematika, alumni UB berprestasi.",
            ratingAvg = 5.0,
            reviewCount = 35,
            completionRate = 1.0,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 60000,
            responseTimeText = "< 10 mnt"
        ),
        // 14. Mitra Luar Radius (11 km - out of range)
        ProviderCandidate(
            providerId = "p14",
            name = "Teknisi Kepanjen Mandiri",
            avatarUrl = null,
            location = GeoPoint(-8.1300, 112.5700), // ~17 km (di luar radius)
            address = "Jl. Panji No. 80, Kepanjen, Malang",
            bio = "Layanan spesialis wilayah Malang Selatan.",
            ratingAvg = 4.7,
            reviewCount = 18,
            completionRate = 0.95,
            flagAnomaly = false,
            badge = ProviderBadge.TRUSTED,
            minBaseFare = 70000,
            responseTimeText = "~45 mnt"
        ),
        // 15. Mitra Libur / Semua Hari OFF (tidak boleh muncul di F1)
        ProviderCandidate(
            providerId = "p15",
            name = "Bambang Libur Service",
            avatarUrl = null,
            location = GeoPoint(-7.9780, 112.6300), // ~0.2 km
            address = "Jl. Merdeka Barat, Malang",
            bio = "Sedang cuti operasional sementara.",
            ratingAvg = 4.5,
            reviewCount = 10,
            completionRate = 0.90,
            flagAnomaly = false,
            badge = ProviderBadge.NONE,
            minBaseFare = 50000,
            responseTimeText = "-"
        )
    )

    /**
     * Akun demo untuk mode offline (tanpa Supabase).
     * Kredensial ini hanya dipakai oleh [id.bantuin.app.data.repository.FakeBantuinRepository]
     * dan tidak pernah dipakai untuk mode cloud.
     */
    data class DemoAccount(
        val email: String,
        val password: String,
        val user: UserProfile
    )

    val demoAccounts: List<DemoAccount> = buildList {
        add(
            DemoAccount(
                email = "pelanggan1@bantuin.test",
                password = "Demo12345!",
                user = UserProfile(
                    id = "user_demo_rani",
                    fullName = "Rani Wijaya",
                    email = "pelanggan1@bantuin.test",
                    phone = "081234567890",
                    roles = setOf("customer", "provider")
                )
            )
        )
        // Alias lama yang masih dipakai oleh test dan dokumentasi demo.
        add(
            DemoAccount(
                email = "rani@bantuin.test",
                password = "Demo12345!",
                user = UserProfile(
                    id = "user_demo_rani",
                    fullName = "Rani Wijaya",
                    email = "rani@bantuin.test",
                    phone = "081234567890",
                    roles = setOf("customer", "provider")
                )
            )
        )
        add(
            DemoAccount(
                email = "pelanggan2@bantuin.test",
                password = "Demo12345!",
                user = UserProfile(
                    id = "user_demo_dimas",
                    fullName = "Dimas Arya",
                    email = "pelanggan2@bantuin.test",
                    phone = "081234567891",
                    roles = setOf("customer", "provider")
                )
            )
        )
        providers.forEachIndexed { index, provider ->
            val number = (index + 1).toString().padStart(2, '0')
            add(
                DemoAccount(
                    email = "mitra$number@bantuin.test",
                    password = "Demo12345!",
                    user = UserProfile(
                        id = provider.providerId,
                        fullName = provider.name,
                        email = "mitra$number@bantuin.test",
                        phone = "0812000000$number",
                        roles = setOf("provider")
                    )
                )
            )
        }
    }

    val services = listOf(
        // Layanan Pak Budi (p01)
        ServiceItem("s01", "p01", 2, "Cuci AC Standar (0.5 - 1 PK)", "Pembersihan filter, evaporator & blower indoor/outdoor", 65000, 90),
        ServiceItem("s02", "p01", 2, "Cuci & Tambah Freon R32/R410", "Cuci total beserta pengecekan tekanan dan isi freon", 120000, 120),
        ServiceItem("s03", "p01", 2, "Bongkar Pasang Unit AC", "Pemindahan unit AC indoor dan outdoor", 250000, 180),
        // Layanan Ahmad Fauzi (p02)
        ServiceItem("s04", "p02", 2, "Servis AC Sedang (1/2 - 1 PK)", "Cuci total, perbaikan saluran bocor air & suara berisik", 60000, 90),
        // Layanan Surya Citra (p03)
        ServiceItem("s05", "p03", 2, "Servis Rutin AC Inverter", "Pembersihan mendalam kompresor inverter", 65000, 90),
        // Layanan Pak Sugeng (p04)
        ServiceItem("s06", "p04", 1, "Pemeriksaan Korsleting Listrik", "Pelacakan jalur putus dan perbaikan MCB trip", 45000, 60),
        ServiceItem("s07", "p04", 1, "Pasang Stop Kontak & Saklar Baru", "Penambahan titik kabel dan saklar", 50000, 60),
        // Layanan Lainnya
        ServiceItem("s08", "p08", 3, "Perbaikan Pipa Bocor & Kran Air", "Penggantian kran rusak & penambalan pipa saluran air", 80000, 120),
        ServiceItem("s09", "p09", 3, "Darurat Bocor Atap Kos", "Penambalan genteng dan talang darurat", 90000, 180),
        ServiceItem("s10", "p10", 4, "Deep Clean Kamar Mandi Kos", "Pembersihan kerak keramik, kloset & dinding kamar mandi", 50000, 120),
        ServiceItem("s11", "p12", 5, "Angkut Pindahan Kos", "Pick-up dan bantuan angkat barang kos hingga sampai tujuan", 100000, 120),
        ServiceItem("s12", "p13", 6, "Bimbingan Belajar Privat (90 mnt)", "Bimbingan satu lawan satu mata kuliah / pelajaran sekolah", 60000, 90)
    )

    // Jadwal ketersediaan mitra
    fun getAvailability(providerId: String): List<DayAvailability> {
        if (providerId == "p15") {
            // Semua hari OFF
            return (1..7).map { DayAvailability(it, AvailabilityMode.OFF) }
        }
        if (providerId == "p09") {
            // ALWAYS_ON
            return (1..7).map { DayAvailability(it, AvailabilityMode.ALWAYS_ON) }
        }
        // Standar: Senin-Sabtu ON 07:00-21:00 (420..1260), Minggu OFF
        return (1..6).map {
            DayAvailability(it, AvailabilityMode.ON, openMin = 420, closeMin = 1260)
        } + DayAvailability(7, AvailabilityMode.OFF)
    }

    // Ulasan contoh untuk Pak Budi
    val sampleReviews = listOf(
        ReviewItem("r01", "b01", "Rani Wijaya", 5, "Pak Budi datang cepat sekali, AC langsung dingin dan kamar tetap bersih!", listOf("Tepat Waktu", "Pekerjaan Rapi"), "1 hari lalu"),
        ReviewItem("r02", "b02", "Dimas Arya", 5, "Teknisi sangat sopan, dijelaskan kenapa air AC menetes dan langsung tuntas.", listOf("Sopan & Ramah", "Harga Sesuai"), "3 hari lalu"),
        ReviewItem("r03", "b03", "Eka Putri", 5, "Sangat terbantu di kos, tarif jelas transparan tanpa biaya tersembunyi.", listOf("Pekerjaan Rapi"), "1 minggu lalu")
    )
}
