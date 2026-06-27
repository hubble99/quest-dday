# SKILL.md — testing-standards

## Kapan Skill Ini Di-load

Load skill ini untuk setiap task tanpa pengecualian.
Testing bukan opsional — setiap task wajib menghasilkan unit test sebelum dianggap selesai.

---

## 1. Library & Setup

### Dependencies wajib (test)

```kotlin
// build.gradle.kts (module app)
testImplementation("junit:junit:4.13.2")
testImplementation("io.mockk:mockk:1.13.x")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:x.x.x")
testImplementation("app.cash.turbine:turbine:x.x.x")  // untuk test Flow
androidTestImplementation("androidx.test.ext:junit:1.x.x")
androidTestImplementation("androidx.room:room-testing:x.x.x")  // untuk test DAO
```

### Lokasi file test

- Unit test (tanpa Android dependency): src/test/java/com/questdday/
- Instrument test (butuh device/emulator, khusus DAO): src/androidTest/java/com/questdday/

Struktur folder test WAJIB mencerminkan struktur package production:
```
src/test/java/com/questdday/
├── util/
│   ├── ExpCalculatorTest.kt
│   └── ScheduleCalculatorTest.kt
├── domain/
│   └── repository/
│       ├── QuestRepositoryTest.kt
│       └── UserRepositoryTest.kt
└── ui/
    └── viewmodel/
        ├── TodayQuestsViewModelTest.kt
        └── ...
```

---

## 2. Struktur Wajib Setiap Test

### Format Arrange-Act-Assert

SETIAP test function WAJIB menggunakan struktur AAA dengan komentar eksplisit:

```kotlin
@Test
fun `nama test yang deskriptif dalam bahasa Inggris`() {
    // Arrange
    val input = ...
    val expected = ...

    // Act
    val result = functionUnderTest(input)

    // Assert
    assertEquals(expected, result)
}
```

### Penamaan test function

Gunakan backtick notation dengan kalimat deskriptif:
```kotlin
// BENAR — jelas apa yang ditest dan apa hasilnya
fun `calculateTargetExp returns 100 for level 1`()
fun `processDecay does not reduce level when exp reaches zero`()
fun `getActiveQuests returns empty list when no quests exist`()

// SALAH — tidak deskriptif
fun testCalculateExp()
fun test1()
```

---

## 3. Coverage Wajib Per Layer

### util/ (pure functions — ExpCalculator, ScheduleCalculator, dll)

Wajib test:
- Happy path untuk setiap fungsi
- Boundary value: nilai tepat di batas (misal level 1, EXP tepat di threshold)
- Input ekstrem: level sangat tinggi, 0 hari, daftar hari kosong
- Input tidak valid: nilai negatif jika relevan

```kotlin
// Contoh untuk ExpCalculator
fun `calculateTargetExp returns 100 for level 1`()
fun `calculateTargetExp returns correct value for level 5`()
fun `levelUp increments level when exp exceeds threshold`()
fun `levelUp handles multiple level ups in one call`()
fun `levelUp does not change level when exp below threshold`()
```

### data/repository/

Wajib test menggunakan MockK untuk mock DAO:
- Happy path: data tersedia, operasi berhasil
- Empty result: DAO return list kosong atau null
- Exception handling: DAO throw exception
- Untuk fungsi yang melibatkan Flow: gunakan Turbine

```kotlin
// Contoh untuk QuestRepository
fun `getActiveQuests returns mapped domain models from dao`()
fun `getActiveQuests returns empty list when dao returns empty`()
fun `insertQuest calls dao with correct entity`()
fun `processCascadeFailure archives all sub quests in single transaction`()
```

### ui/viewmodel/

Wajib test menggunakan MockK untuk mock Repository:
- State awal ViewModel setelah init
- Setiap user action yang mengubah state
- Error state jika Repository gagal
- Gunakan TestCoroutineDispatcher untuk Coroutines

```kotlin
// Contoh untuk TodayQuestsViewModel
fun `initial state is loading`()
fun `quests loaded successfully updates state to success`()
fun `completeInstantQuest calls repository and updates ui state`()
fun `completeInstantQuest shows error when repository throws`()
```

---

## 4. Edge Case Wajib per Kategori Business Logic

Agent WAJIB menulis test untuk edge case berikut, bukan hanya happy path.

### EXP & Level Up

```
- EXP tepat di threshold → harus naik level
- EXP satu kurang dari threshold → tidak naik level
- EXP jauh melebihi threshold satu level → bisa naik beberapa level sekaligus
- Level up berulang: pastikan current_exp dikurangi dengan benar di setiap level
```

### EXP Decay

```
- consecutive_inactive_scheduled_days = 0 → tidak ada decay
- consecutive_inactive_scheduled_days = 1 → tidak ada decay (grace period)
- consecutive_inactive_scheduled_days = 2 → decay mulai (hari_decay = 1)
- Decay lebih besar dari current_exp → current_exp = 0, bukan negatif
- Decay tidak menurunkan current_level dalam kondisi apapun
- Nilai R dibaca dari app_settings, bukan hardcode
```

### Cascade Failure

```
- Sub-quest gagal, saudaranya sudah 'completed' → saudara diarsipkan sebagai 'completed'
- Sub-quest gagal, saudaranya masih 'active' → saudara diarsipkan sebagai 'failed_via_parent'
- Standalone quest gagal → tidak ada cascade ke parent
- Epic dengan satu sub-quest, sub-quest gagal → Epic ikut gagal
- Semua operasi dalam satu transaksi: jika satu gagal, semua rollback
```

### Mode Shift

```
- Quest jadwal harian, bolos 1 hari → end_date geser 1 hari
- Quest jadwal Senin/Rabu/Jumat, bolos 1 sesi → end_date geser ke slot jadwal berikutnya
- end_date jatuh di hari yang bukan jadwal → geser ke slot jadwal berikutnya yang valid
- Bolos 0 sesi → end_date tidak berubah
```

### Mode Stack

```
- Bolos 0 sesi → stacked_duration_seconds tidak berubah
- Bolos 1 sesi → stacked_duration_seconds += target_duration_seconds
- Bolos 6 sesi → stacked_duration_seconds = 6 * target_duration_seconds
- Setelah konfirmasi selesai → stacked_duration_seconds = 0
- effective_target = target_duration_seconds + stacked_duration_seconds
```

### Lazy Evaluation

```
- last_evaluated_date == today → Lapis A tidak jalan
- last_evaluated_date == yesterday → Lapis A jalan
- last_evaluated_date IS NULL → skip evaluasi, set today, tidak error
- Hari ini dikecualikan dari perhitungan missed
- Hari tanpa quest terjadwal tidak increment consecutive_inactive_scheduled_days
```

### Timer & active_timers

```
- alarm_fired_at IS NULL → render sebagai "sedang berjalan"
- alarm_fired_at IS NOT NULL → render sebagai "pending confirmation", tanpa bunyi baru
- Cancel timer → active_timers dihapus, tidak ada entry di quest_logs
- Konfirmasi selesai → active_timers dihapus, quest_logs di-insert
```

---

## 5. Test untuk DAO (Instrument Test)

DAO test menggunakan Room in-memory database — bukan mock.

```kotlin
@RunWith(AndroidJUnit4::class)
class QuestDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var questDao: QuestDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        questDao = db.questDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insert and retrieve quest returns correct data`() = runTest {
        // Arrange
        val quest = QuestEntity(title = "Test Quest", ...)

        // Act
        questDao.insertQuest(quest)
        val result = questDao.getActiveQuests(userId = 1L).first()

        // Assert
        assertEquals(1, result.size)
        assertEquals("Test Quest", result[0].title)
    }
}
```

---

## 6. Checklist Sebelum Task Testing Selesai

Sebelum melaporkan task sebagai selesai, verifikasi:

- [ ] Setiap public function di Repository punya minimal 1 unit test
- [ ] Setiap public function di ViewModel punya minimal 1 unit test
- [ ] Setiap pure function di util/ punya test untuk happy path + boundary + edge case
- [ ] Semua test menggunakan struktur Arrange-Act-Assert dengan komentar
- [ ] Semua test function menggunakan nama deskriptif dengan backtick notation
- [ ] Edge case wajib per kategori business logic sudah tercakup
- [ ] Semua test PASS sebelum artifact hasil dikirim
- [ ] Tidak ada test yang di-skip (@Ignore) tanpa alasan eksplisit di komentar

