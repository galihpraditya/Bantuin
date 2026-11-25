package id.bantuin.app.core.config

/**
 * Konfigurasi global aplikasi Bantuin.
 * Sumber kebenaran tunggal untuk konstanta numerik, bobot algoritma, dan batas waktu sistem.
 */
object BantuinConfig {
    const val TIMEZONE_ID = "Asia/Jakarta"

    // F1 - Smart Service Matchmaking
    const val MAX_RADIUS_KM = 10.0
    const val W_DISTANCE = 0.50
    const val W_RATING = 0.35
    const val W_COMPLETION = 0.15
    const val DEFAULT_RATING_SCORE = 0.7
    const val DEFAULT_COMPLETION_SCORE = 0.7
    const val ANOMALY_PENALTY_FACTOR = 0.7

    // F2 - Dynamic Cost Estimator
    const val RATE_PER_KM = 2000
    const val SURGE_MULTIPLIER = "1.20"
    const val ROUNDING_STEP = 500
    // Jam sibuk: 07:00-08:59 (420..539) & 16:00-18:59 (960..1139) menit sejak 00:00 WIB
    val PEAK_RANGES = listOf(420..539, 960..1139)
    // Jam malam: >= 20:00 (1200) atau < 06:00 (360)
    const val NIGHT_START_MIN = 1200
    const val NIGHT_END_MIN = 360

    // F3 - Scheduling Conflict Detector
    const val TRAVEL_BUFFER_MIN = 30
    const val SLOT_LOCK_MINUTES = 10
    const val SLOT_GRID_MIN = 30
    const val MAX_ALTERNATIVE_SLOTS = 3
    const val PENDING_EXPIRY_MINUTES = 15

    // F4 - Provider Trust Score
    const val TRUST_WINDOW_DAYS = 30
    const val MIN_TX_FOR_EVALUATION = 5
    const val NEW_PROVIDER_TRUST = 70.0
    const val W_TRUST_RATING = 0.45
    const val W_TRUST_ONTIME = 0.35
    const val W_TRUST_RELIABILITY = 0.20
    const val TRUST_MIN_THRESHOLD = 60.0
    const val CANCEL_RATE_MAX_PCT = 20.0
    const val LATE_TOLERANCE_MIN = 15
    val MALANG_CENTER = id.bantuin.app.domain.model.GeoPoint(-7.9797, 112.6304)

    // Pembayaran, Chat, & Pencairan Dana Mitra
    const val MIN_PAYOUT_AMOUNT = 20_000
    const val PAYOUT_ADMIN_FEE = 2_500
    const val MAX_CHAT_MESSAGE_LENGTH = 1_000
}
