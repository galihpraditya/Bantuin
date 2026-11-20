# Frontend Completion (Interactivity, Feedback Loop, and Anti-Slop) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete all interactive gaps in the Bantuin frontend: dynamic Haversine distance in F2, dynamic calendar scheduling with 10-minute lock in F3, reactive review feedback loop from F4 to F1, full provider services CRUD, provider calendar hours configuration, provider profile editing, and interactive location selection, while strictly adhering to Anti-Slop UI and Mobile Layout standards.

**Architecture:** Extend `FakeBantuinRepository` with automated metric recalculation and provider profile persistence. Replace hardcoded mock values in Compose screens with reactive repository state and calculations from domain use cases. Refactor input forms into accessible Material 3 dialogs and screens with minimum 48dp touch targets and WCAG 4.5:1 contrast.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose, Material 3, OSMDroid 6.1.18, JUnit 4, Kotlin Coroutines & StateFlow.

**Spec:** [2026-11-20-frontend-completion-design.md](file:///c:/Users/Galih/Coding/Project/Bantuin/docs/superpowers/specs/2026-11-20-frontend-completion-design.md)

## Global Constraints

- Platform: Android native with `minSdk` 26 and `compileSdk` 37.
- Language: Kotlin, with all UI copy in natural, grounded Indonesian.
- Zero AI-Slop: No generic purple-blue gradients, no decorative glowing dots, no em dashes (`—` or `–`), no marketing fluff.
- Mobile Layout: All interactive elements must meet the 48dp minimum touch target; no horizontal scroll leaks; proper scrolling containers for all forms and lists.
- Core Algorithms: F1, F2, F3, and F4 domain use cases must retain identical behavior and pass all PRD Section 8 test vectors.

## Review Focus

1. **Review submission without prior orders:** Submitting a review when no previous ratings exist must calculate initial composite score correctly without division by zero.
2. **Conflicting time slot edge case:** A requested slot that begins exactly at the end of an existing block minus 30-min buffer must be detected as a conflict.
3. **Invalid operational hours:** Provider setting `closeMin <= openMin` must be blocked by validation with a clear error message.
4. **Service fare validation:** Creating a service with negative fare or non-multiple of 500 must show validation guidance.
5. **Customer location change:** Updating customer location to an out-of-range coordinate (> 10 km) must move providers to the out-of-range section in F1 without crashing.

---

### Task 1: Repository Enhancements for Feedback Loop & Provider Profile Persistence

**Files:**
- Modify: `app/src/main/java/id/bantuin/app/domain/repository/Repositories.kt`
- Modify: `app/src/main/java/id/bantuin/app/data/repository/FakeBantuinRepository.kt`
- Test: `app/src/test/java/id/bantuin/app/repository/FakeBantuinRepositoryTest.kt`

**Interfaces:**
- Consumes: `RecalculateProviderMetricsUseCase`, `TrustInput`, `CompletedJob`, `GeoPoint`
- Produces: `FakeBantuinRepository.submitReview` (recalculating metrics), `FakeBantuinRepository.updateProviderProfile(providerId: String, name: String, bio: String, location: GeoPoint, address: String, phone: String): AppResult<Unit>`, `CatalogRepository.updateProviderProfile`

- [ ] **Step 1: Write the failing repository unit test**

```kotlin
// app/src/test/java/id/bantuin/app/repository/FakeBantuinRepositoryTest.kt
package id.bantuin.app.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.GeoPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeBantuinRepositoryTest {
    private lateinit var repo: FakeBantuinRepository

    @Before
    fun setUp() {
        repo = FakeBantuinRepository()
    }

    @Test
    fun submitReview_triggersRecalculateMetrics_andUpdatesProviderList() = runBlocking {
        val initialCandidate = repo.candidates(1).let { (it as AppResult.Success).data.first { p -> p.providerId == "p01" } }
        val initialRating = initialCandidate.ratingAvg ?: 0.0

        repo.submitReview(
            bookingId = "b01",
            providerId = "p01",
            rating = 5,
            comment = "Layanan istimewa dan cepat",
            tags = listOf("Cepat", "Rapi")
        )

        val updatedCandidate = repo.candidates(1).let { (it as AppResult.Success).data.first { p -> p.providerId == "p01" } }
        assertNotNull(updatedCandidate.ratingAvg)
        assertTrue(updatedCandidate.ratingAvg!! >= initialRating)
    }

    @Test
    fun updateProviderProfile_updatesLocationAndBio() = runBlocking {
        val newLoc = GeoPoint(-7.9500, 112.6200)
        val res = repo.updateProviderProfile(
            providerId = "p01",
            name = "Budi Santoso Baru",
            bio = "Teknisi AC bersertifikat 10 tahun",
            location = newLoc,
            address = "Jl. Sukarno Hatta No. 45, Malang",
            phone = "081299998888"
        )
        assertTrue(res is AppResult.Success)

        val candidate = repo.candidates(1).let { (it as AppResult.Success).data.first { p -> p.providerId == "p01" } }
        assertEquals("Budi Santoso Baru", candidate.name)
        assertEquals(newLoc.lat, candidate.location.lat, 0.0001)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests id.bantuin.app.repository.FakeBantuinRepositoryTest`  
Expected: FAIL with compilation error (unresolved `updateProviderProfile` or missing test file)

- [ ] **Step 3: Implement repository methods in `Repositories.kt` and `FakeBantuinRepository.kt`**

1. In `domain/repository/Repositories.kt`, add `updateProviderProfile` to `CatalogRepository`:
```kotlin
suspend fun updateProviderProfile(
    providerId: String,
    name: String,
    bio: String,
    location: GeoPoint,
    address: String,
    phone: String
): AppResult<Unit>
```

2. In `data/repository/FakeBantuinRepository.kt`:
- Wire `RecalculateProviderMetricsUseCase` into `submitReview`:
  - Calculate `ratings` list from `reviewsList.filter { it.providerId == providerId }.map { it.rating }`.
  - Build `TrustInput` and execute `recalculateUseCase(input)`.
  - Update `providersList` with new `ratingAvg`, `badge`, and `flagAnomaly`.
- Implement `updateProviderProfile`:
  - Update matching provider in `providersList` with new name, location, and update `currentUserFlow` with new name and phone.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests id.bantuin.app.repository.FakeBantuinRepositoryTest`  
Expected: PASS (2 tests passed)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/id/bantuin/app/domain/repository/Repositories.kt app/src/main/java/id/bantuin/app/data/repository/FakeBantuinRepository.kt app/src/test/java/id/bantuin/app/repository/FakeBantuinRepositoryTest.kt
git commit -m "feat(data): implement metric recalculation feedback loop and provider profile update"
```

---

### Task 2: Dynamic Haversine Distance in F2 (`EstimateScreen.kt`)

**Files:**
- Modify: `app/src/main/java/id/bantuin/app/ui/customer/estimate/EstimateScreen.kt`

**Interfaces:**
- Consumes: `HaversineCalculator.distanceKm(customer: GeoPoint, provider: GeoPoint): Double`, `orderFlow.customerLocation`, `provider.location`
- Produces: Dynamic `distanceKm` passed to `CostEstimatorUseCase`

- [ ] **Step 1: Update distance calculation and UI in `EstimateScreen.kt`**

1. Replace static `distanceKm = 1.8` with:
```kotlin
val distanceKm = remember(orderFlow.customerLocation, provider.location) {
    val d = id.bantuin.app.domain.usecase.matchmaking.HaversineCalculator.distanceKm(
        orderFlow.customerLocation,
        provider.location
    )
    // Bulatkan ke 1 desimal untuk konsistensi UI
    Math.round(d * 10.0) / 10.0
}
```
2. Pass `distanceKm` to `costEstimator`:
```kotlin
val estimate = remember(
    service.baseFare,
    orderFlow.selectedComplexity,
    orderFlow.selectedStartMin,
    orderFlow.isUrgent,
    distanceKm
) {
    val est = costEstimator(
        baseFare = service.baseFare,
        distanceKm = distanceKm,
        complexity = orderFlow.selectedComplexity,
        startMinOfDay = orderFlow.selectedStartMin,
        isUrgent = orderFlow.isUrgent
    )
    orderFlow.currentEstimate = est
    est
}
```
3. In the breakdown card, render dynamic distance text:
`"${String.format(java.util.Locale.US, "%.1f", distanceKm)} km x Rp 2.000"`
4. Verify all buttons and radio rows have at least 48dp height.

- [ ] **Step 2: Run unit tests to ensure no regressions**

Run: `.\gradlew.bat testDebugUnitTest --tests id.bantuin.app.usecase.CostEstimatorUseCaseTest`  
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/id/bantuin/app/ui/customer/estimate/EstimateScreen.kt
git commit -m "feat(ui): calculate real haversine distance dynamically in EstimateScreen"
```

---

### Task 3: Dynamic Scheduling, Operational Hours & Slot Lock in F3 (`ScheduleScreen.kt`)

**Files:**
- Modify: `app/src/main/java/id/bantuin/app/ui/customer/schedule/ScheduleScreen.kt`

**Interfaces:**
- Consumes: `repository.availability(providerId)`, `repository.busyBlocks(providerId, dateIso)`, `repository.acquireLock(providerId, startMin, endMin)`
- Produces: `orderFlow.selectedDateIso`, `orderFlow.selectedStartMin`, `orderFlow.lockInfo`

- [ ] **Step 1: Implement dynamic dates, slots, conflict checking, and slot locking**

1. Generate dynamic 7 calendar dates starting from today (`LocalDate.now()`).
2. Fetch `availability` and `busyBlocks` from `repository` via `LaunchedEffect(selectedDateIso, provider.providerId)`.
3. Compute provider's operational window for the selected day of week:
   - If `ALWAYS_ON`, window is 00:00 to 24:00 (0..1440).
   - If `ON`, window is `openMin` to `closeMin` (e.g. 480..1020).
   - If `OFF`, mark day as "Libur" and disable slot selection.
4. Generate slots in 30-minute intervals within the operational window.
5. In `detector.check`:
   - Pass `nowMin = if (isToday) (currentHour * 60 + currentMinute) else null`.
   - Past slots on today correctly show conflict `ConflictReason.PAST_TIME`.
6. On button "Lanjut ke Estimasi Biaya":
   - Call `val lockRes = repository.acquireLock(provider.providerId, selectedStartMin, selectedStartMin + service.durationMin)`.
   - Store `lockInfo` in `orderFlow.lockInfo`.
   - Call `onContinueToEstimate()`.
7. Ensure mobile layout has minimum 48dp height for date pills and slot chips.

- [ ] **Step 2: Verify unit tests**

Run: `.\gradlew.bat testDebugUnitTest --tests id.bantuin.app.usecase.SchedulingConflictDetectorTest`  
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/id/bantuin/app/ui/customer/schedule/ScheduleScreen.kt
git commit -m "feat(ui): implement dynamic calendar dates, slot generation and slot locking in ScheduleScreen"
```

---

### Task 4: Interactive Location Selection in Customer Home (`CustomerHomeScreen.kt`)

**Files:**
- Modify: `app/src/main/java/id/bantuin/app/ui/customer/home/CustomerHomeScreen.kt`

**Interfaces:**
- Consumes: `orderFlow.customerLocation`, `orderFlow.customerAddress`
- Produces: Updated `orderFlow.customerLocation` and `orderFlow.customerAddress`

- [ ] **Step 1: Enhance LocationPickerDialog with presets and manual address editing**

1. Define preset landmarks in Malang:
```kotlin
val presets = listOf(
    Triple("Alun-alun Malang", GeoPoint(-7.9797, 112.6304), "Jl. Merdeka Barat, Alun-alun Malang"),
    Triple("UB / Jl. Soekarno-Hatta", GeoPoint(-7.9467, 112.6156), "Jl. Soekarno-Hatta No. 9, Lowokwaru, Malang"),
    Triple("Sawojajar", GeoPoint(-7.9772, 112.6562), "Jl. Danau Toba, Sawojajar, Kedungkandang, Malang"),
    Triple("Dinoyo", GeoPoint(-7.9405, 112.6074), "Jl. MT Haryono No. 193, Dinoyo, Lowokwaru, Malang")
)
```
2. Display preset selection chips inside `LocationPickerDialog`. Tapping a preset updates `textInput` and `selectedLocation`.
3. In `onLocationConfirmed`, pass the updated `textInput` and `selectedLocation`.
4. Ensure target touch size is at least 48dp for all chips and buttons.

- [ ] **Step 2: Run build to verify compilation**

Run: `.\gradlew.bat compileDebugKotlin`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/id/bantuin/app/ui/customer/home/CustomerHomeScreen.kt
git commit -m "feat(ui): add quick location presets and coordinate selection in CustomerHomeScreen"
```

---

### Task 5: Reactive Feedback Loop in Recommendation Screen (`RecommendationScreen.kt`)

**Files:**
- Modify: `app/src/main/java/id/bantuin/app/ui/customer/recommendation/RecommendationScreen.kt`

**Interfaces:**
- Consumes: `repository.candidates(categoryId)`
- Produces: Dynamically sorted ranked providers reflecting latest ratings and anomaly penalty

- [ ] **Step 1: Connect RecommendationScreen to repository candidates**

1. Add `repository: FakeBantuinRepository` parameter to `RecommendationScreen` and update call site in `NavGraph.kt`.
2. Replace static `FakeData.providers` with state loaded from `repository.candidates(categoryId)` via `LaunchedEffect(categoryId, orderFlow.customerLocation)`.
3. Re-evaluate `matchmakingUseCase` with dynamic candidates so that if a provider was penalized or received new reviews, their ranking updates automatically.
4. Verify anti-slop rules: no generic purple glow, clear badges (Terpercaya, Baru, Anomali), min 48dp touch targets on provider cards.

- [ ] **Step 2: Run test suite**

Run: `.\gradlew.bat testDebugUnitTest --tests id.bantuin.app.usecase.MatchmakingUseCaseTest`  
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/id/bantuin/app/ui/customer/recommendation/RecommendationScreen.kt app/src/main/java/id/bantuin/app/ui/navigation/NavGraph.kt
git commit -m "feat(ui): connect RecommendationScreen to repository candidates for dynamic ranking"
```

---

### Task 6: Provider Services CRUD Dialogs (`ProviderServicesScreen.kt`)

**Files:**
- Modify: `app/src/main/java/id/bantuin/app/ui/provider/services/ProviderServicesScreen.kt`

**Interfaces:**
- Consumes: `repository.servicesOf("p01")`, `repository.upsertService(service)`, `repository.deleteService(serviceId)`
- Produces: Created, edited, or deleted `ServiceItem`

- [ ] **Step 1: Implement UpsertServiceDialog and Delete Confirmation**

1. Create state `showUpsertDialog` holding `editingService: ServiceItem?`.
2. Build `UpsertServiceDialog`:
   - OutlinedTextField for service name (non-empty validation).
   - Category selection filter chips (1 to 6).
   - OutlinedTextField for base fare with validation (`fare >= 0` and `fare % 500 == 0`).
   - OutlinedTextField for duration in minutes (`duration in 15..720`).
   - Save button with at least 48dp height.
3. On save, call `repository.upsertService(...)` and reload services from repository.
4. Add Edit (pencil) and Delete (trash) icon buttons on each service card with 48dp touch target.
5. On delete, show confirmation dialog and call `repository.deleteService(service.id)`.

- [ ] **Step 2: Run build to verify compilation**

Run: `.\gradlew.bat compileDebugKotlin`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/id/bantuin/app/ui/provider/services/ProviderServicesScreen.kt
git commit -m "feat(ui): implement complete service upsert and delete dialogs in ProviderServicesScreen"
```

---

### Task 7: Provider Calendar Operating Hours Configuration (`ProviderCalendarScreen.kt`)

**Files:**
- Modify: `app/src/main/java/id/bantuin/app/ui/provider/calendar/ProviderCalendarScreen.kt`

**Interfaces:**
- Consumes: `repository.availability("p01")`, `repository.saveAvailability("p01", days)`
- Produces: Updated `DayAvailability` with custom `openMin` and `closeMin` for mode `ON`

- [ ] **Step 1: Implement operating hours editor for mode ON**

1. Load availability from `repository.availability("p01")` in `LaunchedEffect(Unit)`.
2. When a day is set to `AvailabilityMode.ON`:
   - Display two input fields: "Jam Buka" (e.g. "08:00") and "Jam Tutup" (e.g. "17:00").
   - Parse input into `openMin` and `closeMin`.
   - Validate that `openMin < closeMin`. If invalid, show error message: "Jam buka harus lebih awal dari jam tutup".
3. When set to `AvailabilityMode.OFF` or `ALWAYS_ON`, hide the time inputs cleanly per PRD Section 11.2.
4. On clicking "Simpan Perubahan", call `repository.saveAvailability("p01", days)`.
5. Ensure touch targets for radio buttons, time inputs, and save button are at least 48dp.

- [ ] **Step 2: Run build to verify compilation**

Run: `.\gradlew.bat compileDebugKotlin`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/id/bantuin/app/ui/provider/calendar/ProviderCalendarScreen.kt
git commit -m "feat(ui): add operating hours editor and time validation in ProviderCalendarScreen"
```

---

### Task 8: Provider Profile Edit Screen & Navigation Integration

**Files:**
- Create: `app/src/main/java/id/bantuin/app/ui/provider/profile/ProviderProfileEditScreen.kt`
- Modify: `app/src/main/java/id/bantuin/app/ui/provider/home/ProviderHomeScreen.kt`
- Modify: `app/src/main/java/id/bantuin/app/ui/navigation/Routes.kt`
- Modify: `app/src/main/java/id/bantuin/app/ui/navigation/NavGraph.kt`

**Interfaces:**
- Consumes: `repository.candidates(categoryId)` to read `p01` profile, `repository.currentUser()`
- Produces: `repository.updateProviderProfile(providerId, name, bio, location, address, phone)`

- [ ] **Step 1: Create `ProviderProfileEditScreen.kt`**

1. Create Composable `ProviderProfileEditScreen`:
   - TopAppBar with back arrow and title "Edit Profil & Pangkalan".
   - Form fields: Nama Mitra, Bio Singkat, Alamat Pangkalan, Nomor Kontak WhatsApp.
   - Pangkalan coordinates picker with Malang presets and OSM map preview.
   - Button "Simpan Profil Mitra" (height 48dp, OrangePrimary, on-click validates and calls `repository.updateProviderProfile(...)`).
   - Snackbar or state feedback "Profil berhasil diperbarui".

- [ ] **Step 2: Wire navigation in `Routes.kt`, `NavGraph.kt`, and `ProviderHomeScreen.kt`**

1. In `Routes.kt`, add `const val PROVIDER_PROFILE_EDIT = "provider/profile/edit"`.
2. In `NavGraph.kt`, add `composable(Routes.PROVIDER_PROFILE_EDIT)`.
3. In `ProviderHomeScreen.kt`, add an action tile under "Kelola Layanan & Jadwal":
   - Icon: `Icons.Default.Person`
   - Title: "Edit Profil & Pangkalan"
   - Description: "Ubah biodata, kontak, dan titik lokasi pangkalan mitra"
   - onClick: navigate to `Routes.PROVIDER_PROFILE_EDIT`.
   - Also wire the edit button on the header card to this route.

- [ ] **Step 3: Run build to verify compilation**

Run: `.\gradlew.bat compileDebugKotlin`  
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/id/bantuin/app/ui/provider/profile/ProviderProfileEditScreen.kt app/src/main/java/id/bantuin/app/ui/provider/home/ProviderHomeScreen.kt app/src/main/java/id/bantuin/app/ui/navigation/Routes.kt app/src/main/java/id/bantuin/app/ui/navigation/NavGraph.kt
git commit -m "feat(ui): implement ProviderProfileEditScreen and integrate navigation from ProviderHomeScreen"
```

---

### Task 9: Full Build Verification, Unit Tests & Anti-Slop Audit

**Files:**
- Audit: All modified files in `ui/`

- [ ] **Step 1: Execute all unit tests and assemble debug APK**

Run: `.\gradlew.bat testDebugUnitTest assembleDebug`  
Expected: BUILD SUCCESSFUL with all unit tests passing.

- [ ] **Step 2: Run Anti-Slop UI & Mobile Layout Verification Checklist**

- [ ] No generic blue-purple or neon gradients.
- [ ] No decorative emojis in text, buttons, or headers.
- [ ] No em dashes (`—` or `–`) in UI text.
- [ ] All interactive buttons, chips, radio buttons, and inputs have at least 48dp touch targets.
- [ ] Text contrast meets WCAG 4.5:1 (Dark Charcoal on Warm Neutral `#F7F6F4` / White `#FFFFFF`).
- [ ] All lists and forms are inside scrollable containers (`LazyColumn` or `verticalScroll`) with zero horizontal overflow leak.

- [ ] **Step 3: Commit final verification**

```bash
git add -A
git commit -m "chore: verify frontend completion and anti-slop compliance"
```
