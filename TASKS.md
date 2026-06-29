# TASKS.md — Quest DDay

## Cara Penggunaan

Setiap task dikerjakan dalam satu sesi Antigravity terpisah.
Prompt pembuka setiap sesi:

```
Kerjakan TASKS.md > [Epic X] > [Task X.Y]: [judul task].
Sebelum mulai baca AGENTS.md dan load skill: [nama skill].
Hasilkan Implementation Plan sebagai artifact sebelum menulis kode apapun.
```

Status: TODO | IN_PROGRESS | DONE
Format commit: feat/fix/chore: [Epic X][Task X.Y] judul task

---

## Epic 1 — Project Setup & Fondasi

### Task 1.1 — Inisialisasi Project Android
**Status:** TODO
**Skill:** android-lifecycle
**Dependensi:** -

**Scope:**
Buat project Android baru dengan konfigurasi berikut:
- Bahasa: Kotlin
- UI: Jetpack Compose (Material3)
- Minimum SDK: 26 (Android 8.0)
- Package name: com.questdday
- Build system: Gradle (Kotlin DSL / build.gradle.kts)

Tambahkan semua dependency berikut ke build.gradle.kts:
```
// Room
androidx.room:room-runtime
androidx.room:room-ktx
androidx.room:room-compiler (kapt/ksp)

// Navigation
androidx.navigation:navigation-compose

// Lifecycle
androidx.lifecycle:lifecycle-viewmodel-compose
androidx.lifecycle:lifecycle-runtime-compose

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android

// Testing
junit:junit
io.mockk:mockk
org.jetbrains.kotlinx:kotlinx-coroutines-test
app.cash.turbine:turbine
androidx.test.ext:junit
androidx.room:room-testing
```

Buat struktur package lengkap sesuai AGENTS.md:
```
com.questdday/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── AppDatabase.kt (kosong dulu)
│   └── repository/
├── domain/
│   └── model/
├── ui/
│   ├── screen/
│   │   ├── today/
│   │   ├── masterplan/
│   │   ├── create/
│   │   └── settings/
│   ├── component/
│   └── theme/
│       └── Spacing.kt
├── service/
├── receiver/
└── util/
```

Buat file Spacing.kt di ui/theme/:
```kotlin
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
```

**Acceptance Criteria:**
- [ ] Project bisa build tanpa error
- [ ] Semua dependency terinstall
- [ ] Struktur package lengkap sesuai AGENTS.md
- [ ] Spacing.kt tersedia di ui/theme/
- [ ] MainActivity menampilkan Composable kosong (placeholder "Quest DDay")

**Unit Test:**
Tidak ada unit test untuk task ini — task ini murni setup infrastruktur.

---

### Task 1.2 — Setup Navigation & Screen Placeholder
**Status:** TODO
**Skill:** android-lifecycle, ui-design-qa
**Dependensi:** Task 1.1

**Scope:**
Implementasi Navigation Compose dengan 4 screen utama sebagai placeholder kosong.

Buat NavGraph di ui/navigation/QuestNavGraph.kt:
```kotlin
sealed class Screen(val route: String) {
    object Today : Screen("today")
    object MasterPlan : Screen("master_plan")
    object CreateQuest : Screen("create_quest")
    object Settings : Screen("settings")
}
```

Buat Bottom Navigation Bar dengan 3 tab:
- Today's Quests (icon: today/checklist)
- Master Plan (icon: map/flag)
- Settings (icon: settings/gear)

Screen CreateQuest tidak masuk Bottom Nav — diakses via tombol FAB (+) di Today's Quests.

Buat placeholder Composable untuk setiap screen (cukup Text dengan nama screen).

**Acceptance Criteria:**
- [ ] Navigasi antar 3 tab berfungsi
- [ ] FAB (+) di Today's Quests navigasi ke CreateQuest
- [ ] Back dari CreateQuest kembali ke Today's Quests
- [ ] Bottom Nav highlight tab yang aktif
- [ ] Tidak ada crash saat navigasi ke semua screen

**Unit Test:**
Tidak ada unit test untuk task ini — navigasi diverifikasi manual di emulator.

---

## Epic 2 — Database Layer

### Task 2.1 — Entity & DAO: users, attributes, user_attribute_stats
**Status:** DONE
**Skill:** database-conventions, testing-standards
**Dependensi:** Task 1.1

**Scope:**
Buat Room Entity dan DAO untuk 3 tabel pertama.

**Entity yang dibuat:**

UserEntity (tabel: users):
```
id: Long (PK autoGenerate)
username: String (default: "Adventurer")
lastActiveAt: String (datetime now)
lastEvaluatedDate: String? (nullable)
consecutiveInactiveScheduledDays: Int (default: 0)
totalExpEarnedLifetime: Double (default: 0.0)
hasSeenWelcome: Int (default: 0)
createdAt: String (datetime now)
```

AttributeEntity (tabel: attributes):
```
id: Long (PK autoGenerate)
code: String (UNIQUE) → 'STR','INT','WIS','DEX','VIT'
displayName: String
icon: String?
isDefault: Int (default: 0)
sortOrder: Int (default: 0)
```

UserAttributeStatEntity (tabel: user_attribute_stats):
```
id: Long (PK autoGenerate)
userId: Long (FK → users.id CASCADE)
attributeId: Long (FK → attributes.id RESTRICT)
currentLevel: Int (default: 1)
currentExp: Double (default: 0.0)
lastGainedAt: String?
updatedAt: String (datetime now)
UNIQUE(userId, attributeId)
```

**DAO yang dibuat:**

UserDao:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertUser(user: UserEntity): Long

@Query("SELECT * FROM users WHERE id = :userId")
fun getUserById(userId: Long): Flow<UserEntity?>

@Query("UPDATE users SET last_active_at = :now, last_evaluated_date = :date WHERE id = :userId")
suspend fun updateEvaluationDate(userId: Long, date: String, now: String)

@Query("UPDATE users SET consecutive_inactive_scheduled_days = :days WHERE id = :userId")
suspend fun updateInactiveDays(userId: Long, days: Int)

@Query("UPDATE users SET total_exp_earned_lifetime = total_exp_earned_lifetime + :amount WHERE id = :userId")
suspend fun addLifetimeExp(userId: Long, amount: Double)

@Query("UPDATE users SET has_seen_welcome = 1 WHERE id = :userId")
suspend fun markWelcomeSeen(userId: Long)
```

AttributeDao:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertAttribute(attribute: AttributeEntity): Long

@Query("SELECT * FROM attributes ORDER BY sort_order ASC")
fun getAllAttributes(): Flow<List<AttributeEntity>>

@Query("SELECT * FROM attributes WHERE id = :id")
suspend fun getAttributeById(id: Long): AttributeEntity?

@Query("UPDATE attributes SET display_name = :name, icon = :icon WHERE id = :id")
suspend fun updateAttribute(id: Long, name: String, icon: String?)

@Query("DELETE FROM attributes WHERE id = :id AND is_default = 0")
suspend fun deleteCustomAttribute(id: Long)
```

UserAttributeStatDao:
```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertStat(stat: UserAttributeStatEntity): Long

@Query("SELECT * FROM user_attribute_stats WHERE user_id = :userId")
fun getAllStatsByUser(userId: Long): Flow<List<UserAttributeStatEntity>>

@Query("""
    UPDATE user_attribute_stats 
    SET current_exp = :exp, current_level = :level, updated_at = :now
    WHERE user_id = :userId AND attribute_id = :attributeId
""")
suspend fun updateExpAndLevel(userId: Long, attributeId: Long, exp: Double, level: Int, now: String)

@Query("""
    UPDATE user_attribute_stats 
    SET current_exp = MAX(current_exp - :amount, 0), updated_at = :now
    WHERE user_id = :userId
""")
suspend fun applyDecayToAllStats(userId: Long, amount: Double, now: String)
```

**Domain Model yang dibuat:**
User.kt, Attribute.kt, UserAttributeStat.kt di domain/model/
Mapper extension function di data/local/entity/ untuk setiap entity.

**Acceptance Criteria:**
- [x] Semua Entity sesuai schema PRD.md (nama kolom, tipe, nullable, constraint)
- [x] Semua FK dan index terdefinisi di Entity annotation
- [x] Semua DAO menggunakan raw @Query dengan named parameter
- [x] Domain model dan mapper tersedia untuk ketiga entity
- [x] Tidak ada auto-generated method selain @Insert yang diizinkan

**Unit Test (src/androidTest — pakai Room in-memory DB):**
```
UserDaoTest:
- insertUser and getUserById returns correct data
- updateEvaluationDate updates correct fields only
- markWelcomeSeen sets has_seen_welcome to 1

AttributeDaoTest:
- getAllAttributes returns sorted by sort_order
- deleteCustomAttribute succeeds for non-default attribute
- deleteCustomAttribute does nothing for default attribute (is_default = 1)

UserAttributeStatDaoTest:
- insertStat with duplicate userId+attributeId is ignored (IGNORE strategy)
- applyDecayToAllStats does not produce negative exp
- applyDecayToAllStats updates all stats for user in one query
```

---

### Task 2.2 — Entity & DAO: quests, quest_logs, quest_history
**Status:** DONE
**Skill:** database-conventions, testing-standards
**Dependensi:** Task 2.1

**Scope:**
Buat Room Entity dan DAO untuk 3 tabel quest.

**Entity yang dibuat:**

QuestEntity (tabel: quests) — semua kolom sesuai PRD.md section 2:
```
id: Long (PK autoGenerate)
userId: Long (FK → users.id CASCADE)
parentQuestId: Long? (FK → quests.id CASCADE, nullable)
attributeId: Long? (FK → attributes.id SET NULL, nullable)
title: String
description: String?
isContainer: Int (default: 0)
completionMode: String? ('instant' | 'timer', nullable)
targetDurationSeconds: Int?
durationType: String ('endless' | 'time_bound')
durationInputType: String? ('date' | 'days', nullable)
targetDays: Int?
startDate: String (date now)
endDate: String?
absenceMode: String? ('shift' | 'stack', nullable)
stackedDurationSeconds: Int (default: 0)
scheduleType: String ('daily' | 'custom_days', default: 'daily')
scheduleDays: String?
status: String ('active' | 'completed' | 'failed', default: 'active')
consecutiveMissedSessions: Int (default: 0)
lastCompletedAt: String?
createdAt: String (datetime now)
updatedAt: String (datetime now)
```

Index wajib:
- idx_quests_user_status ON quests(user_id, status)
- idx_quests_parent ON quests(parent_quest_id)

QuestLogEntity (tabel: quest_logs):
```
id: Long (PK autoGenerate)
questId: Long (FK → quests.id CASCADE)
userId: Long (FK → users.id CASCADE)
logDate: String (format: YYYY-MM-DD)
actualDurationSeconds: Int?
expAwarded: Double (default: 0.0)
isEpicFinaleBonus: Int (default: 0)
completedAt: String (datetime now)
```

Index: idx_quest_logs_quest_date ON quest_logs(quest_id, log_date)
Index: idx_quest_logs_user_date ON quest_logs(user_id, log_date)

QuestHistoryEntity (tabel: quest_history):
```
id: Long (PK autoGenerate)
originalQuestId: Long (bukan FK aktif — quest asli bisa sudah dihapus)
userId: Long (FK → users.id CASCADE)
title: String
finalStatus: String ('completed' | 'failed_abandoned' | 'failed_via_parent')
totalDaysCompleted: Int (default: 0)
totalExpEarned: Double (default: 0.0)
startedAt: String
endedAt: String (datetime now)
```

**DAO yang dibuat:**

QuestDao:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertQuest(quest: QuestEntity): Long

@Query("SELECT * FROM quests WHERE user_id = :userId AND status = 'active' AND is_container = 0")
fun getActiveExecutableQuests(userId: Long): Flow<List<QuestEntity>>

@Query("SELECT * FROM quests WHERE user_id = :userId AND is_container = 1 AND status = 'active'")
fun getActiveEpicContainers(userId: Long): Flow<List<QuestEntity>>

@Query("SELECT * FROM quests WHERE parent_quest_id = :parentId AND status = 'active'")
fun getActiveSubQuests(parentId: Long): Flow<List<QuestEntity>>

@Query("SELECT * FROM quests WHERE id = :id")
suspend fun getQuestById(id: Long): QuestEntity?

@Query("""
    SELECT * FROM quests 
    WHERE user_id = :userId 
    AND status = 'active' 
    AND is_container = 0
    AND schedule_type = 'daily'
    AND date(:today) >= date(start_date)
    AND (end_date IS NULL OR date(:today) <= date(end_date))
""")
fun getDailyQuestsForToday(userId: Long, today: String): Flow<List<QuestEntity>>

@Query("""
    SELECT * FROM quests 
    WHERE user_id = :userId 
    AND status = 'active' 
    AND is_container = 0
    AND schedule_type = 'custom_days'
    AND date(:today) >= date(start_date)
    AND (end_date IS NULL OR date(:today) <= date(end_date))
""")
fun getCustomScheduleQuests(userId: Long, today: String): Flow<List<QuestEntity>>

@Query("UPDATE quests SET status = :status, updated_at = :now WHERE id = :id")
suspend fun updateQuestStatus(id: Long, status: String, now: String)

@Query("""
    UPDATE quests 
    SET consecutive_missed_sessions = :sessions, updated_at = :now 
    WHERE id = :id
""")
suspend fun updateMissedSessions(id: Long, sessions: Int, now: String)

@Query("UPDATE quests SET end_date = :newEndDate, updated_at = :now WHERE id = :id")
suspend fun updateEndDate(id: Long, newEndDate: String, now: String)

@Query("""
    UPDATE quests 
    SET stacked_duration_seconds = stacked_duration_seconds + :amount, updated_at = :now 
    WHERE id = :id
""")
suspend fun addStackedDuration(id: Long, amount: Int, now: String)

@Query("UPDATE quests SET stacked_duration_seconds = 0, updated_at = :now WHERE id = :id")
suspend fun resetStackedDuration(id: Long, now: String)

@Query("UPDATE quests SET last_completed_at = :now, updated_at = :now WHERE id = :id")
suspend fun updateLastCompleted(id: Long, now: String)

@Query("DELETE FROM quests WHERE id = :id")
suspend fun deleteQuest(id: Long)
```

QuestLogDao:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertLog(log: QuestLogEntity): Long

@Query("SELECT * FROM quest_logs WHERE quest_id = :questId AND log_date = :date")
suspend fun getLogForDate(questId: Long, date: String): QuestLogEntity?

@Query("SELECT * FROM quest_logs WHERE user_id = :userId AND log_date = :date")
fun getLogsForUserOnDate(userId: Long, date: String): Flow<List<QuestLogEntity>>

@Query("SELECT COUNT(*) FROM quest_logs WHERE quest_id = :questId")
suspend fun getTotalCompletionsForQuest(questId: Long): Int

@Query("SELECT COALESCE(SUM(exp_awarded), 0) FROM quest_logs WHERE quest_id = :questId")
suspend fun getTotalExpForQuest(questId: Long): Double
```

QuestHistoryDao:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertHistory(history: QuestHistoryEntity): Long

@Query("SELECT * FROM quest_history WHERE user_id = :userId ORDER BY ended_at DESC")
fun getHistoryByUser(userId: Long): Flow<List<QuestHistoryEntity>>
```

**Domain Model yang dibuat:**
Quest.kt, QuestLog.kt, QuestHistory.kt di domain/model/
Mapper untuk ketiga entity.

**Acceptance Criteria:**
- [x] Semua kolom QuestEntity sesuai PRD.md (termasuk semua yang nullable)
- [x] Semua index terdefinisi
- [x] FK dengan ON DELETE behavior yang benar (CASCADE, SET NULL)
- [x] Semua query menggunakan raw @Query dengan named parameter
- [x] originalQuestId di QuestHistoryEntity bukan FK aktif (hanya Long biasa)

**Unit Test (src/androidTest — Room in-memory DB):**
```
QuestDaoTest:
- insertQuest and getQuestById returns correct data
- getActiveExecutableQuests excludes is_container = 1
- getActiveExecutableQuests excludes status != 'active'
- getDailyQuestsForToday excludes quests before start_date
- getDailyQuestsForToday excludes quests after end_date
- getDailyQuestsForToday includes quests with null end_date
- deleteQuest cascades to sub-quests (parentQuestId FK)
- addStackedDuration accumulates correctly across multiple calls
- resetStackedDuration sets stacked_duration_seconds to 0

QuestLogDaoTest:
- insertLog and getLogForDate returns correct data for same date
- getLogForDate returns null for date with no log
- getTotalExpForQuest returns 0 when no logs exist
- getTotalExpForQuest sums correctly across multiple logs

QuestHistoryDaoTest:
- getHistoryByUser returns ordered by ended_at DESC
- insertHistory with all three finalStatus values succeeds
```

---

### Task 2.3 — Entity & DAO: active_timers, exp_decay_log, app_settings
**Status:** TODO
**Skill:** database-conventions, testing-standards
**Dependensi:** Task 2.1

**Scope:**
Buat Room Entity dan DAO untuk 3 tabel terakhir.

**Entity yang dibuat:**

ActiveTimerEntity (tabel: active_timers):
```
id: Long (PK autoGenerate)
questId: Long (FK → quests.id CASCADE, UNIQUE)
startedAt: String (datetime now)
targetDurationSeconds: Int
alarmFiredAt: String? (nullable — NULL = masih berjalan)
```

ExpDecayLogEntity (tabel: exp_decay_log):
```
id: Long (PK autoGenerate)
userAttributeStatId: Long (FK → user_attribute_stats.id CASCADE)
inactiveDays: Int
expBefore: Double
expAfter: Double
decayRateUsed: Double
processedAt: String (datetime now)
```

AppSettingEntity (tabel: app_settings):
```
key: String (PK — bukan autoGenerate)
value: String
updatedAt: String (datetime now)
```

**DAO yang dibuat:**

ActiveTimerDao:
```kotlin
@Insert(onConflict = OnConflictStrategy.ABORT)
suspend fun insertTimer(timer: ActiveTimerEntity): Long

@Query("SELECT * FROM active_timers WHERE quest_id = :questId")
suspend fun getTimerByQuestId(questId: Long): ActiveTimerEntity?

@Query("SELECT * FROM active_timers WHERE alarm_fired_at IS NOT NULL")
fun getPendingConfirmationTimers(): Flow<List<ActiveTimerEntity>>

@Query("SELECT * FROM active_timers WHERE alarm_fired_at IS NULL")
fun getRunningTimers(): Flow<List<ActiveTimerEntity>>

@Query("UPDATE active_timers SET alarm_fired_at = :firedAt WHERE quest_id = :questId")
suspend fun markAlarmFired(questId: Long, firedAt: String)

@Query("DELETE FROM active_timers WHERE quest_id = :questId")
suspend fun deleteTimer(questId: Long)

@Query("SELECT COUNT(*) FROM active_timers WHERE quest_id = :questId")
suspend fun timerExistsForQuest(questId: Long): Int
```

ExpDecayLogDao:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertDecayLog(log: ExpDecayLogEntity): Long

@Query("SELECT * FROM exp_decay_log ORDER BY processed_at DESC LIMIT :limit")
fun getRecentDecayLogs(limit: Int): Flow<List<ExpDecayLogEntity>>
```

AppSettingDao:
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertOrUpdate(setting: AppSettingEntity)

@Query("SELECT value FROM app_settings WHERE key = :key")
suspend fun getValue(key: String): String?

@Query("SELECT * FROM app_settings")
fun getAllSettings(): Flow<List<AppSettingEntity>>
```

**Acceptance Criteria:**
- [ ] ActiveTimerEntity punya UNIQUE constraint pada questId
- [ ] Insert timer dengan questId yang sama throw exception (OnConflictStrategy.ABORT)
- [ ] alarmFiredAt nullable dan default NULL
- [ ] AppSettingEntity menggunakan String sebagai PK (bukan autoGenerate)
- [ ] Semua query menggunakan raw @Query

**Unit Test (src/androidTest — Room in-memory DB):**
```
ActiveTimerDaoTest:
- insertTimer succeeds for new questId
- insertTimer throws for duplicate questId (ABORT strategy)
- getTimerByQuestId returns null if no timer exists
- getPendingConfirmationTimers returns only timers with alarm_fired_at NOT NULL
- getRunningTimers returns only timers with alarm_fired_at IS NULL
- markAlarmFired sets alarm_fired_at correctly
- deleteTimer removes timer from table
- timerExistsForQuest returns 0 when no timer, 1 when exists

AppSettingDaoTest:
- getValue returns null for non-existent key
- insertOrUpdate overwrites existing value for same key
- getAllSettings returns all seeded settings
```

---

### Task 2.4 — AppDatabase, Seed Data & Repository Interface
**Status:** TODO
**Skill:** database-conventions, testing-standards
**Dependensi:** Task 2.1, Task 2.2, Task 2.3

**Scope:**
Buat AppDatabase yang menggabungkan semua Entity dan DAO, jalankan seed data,
serta buat interface Repository untuk semua domain.

**AppDatabase:**
```kotlin
@Database(
    entities = [
        UserEntity::class,
        AttributeEntity::class,
        UserAttributeStatEntity::class,
        QuestEntity::class,
        QuestLogEntity::class,
        QuestHistoryEntity::class,
        ActiveTimerEntity::class,
        ExpDecayLogEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun attributeDao(): AttributeDao
    abstract fun userAttributeStatDao(): UserAttributeStatDao
    abstract fun questDao(): QuestDao
    abstract fun questLogDao(): QuestLogDao
    abstract fun questHistoryDao(): QuestHistoryDao
    abstract fun activeTimerDao(): ActiveTimerDao
    abstract fun expDecayLogDao(): ExpDecayLogDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        // singleton pattern
    }
}
```

**Seed data via RoomDatabase.Callback onCreate():**

Attributes default:
```
(code='STR', display_name='Strength',     icon='💪', is_default=1, sort_order=1)
(code='INT', display_name='Intelligence', icon='🧠', is_default=1, sort_order=2)
(code='WIS', display_name='Wisdom',       icon='🧘', is_default=1, sort_order=3)
(code='DEX', display_name='Dexterity',    icon='⚡', is_default=1, sort_order=4)
(code='VIT', display_name='Vitality',     icon='❤️', is_default=1, sort_order=5)
```

App settings default:
```
(key='epic_finale_bonus_exp',      value='1000')
(key='decay_grace_period_days',    value='')
(key='decay_rate_R',               value='')
(key='failure_threshold_sessions', value='7')
```

User default (satu user, single-user app):
```
(id=1, username='Adventurer')
```

Setelah insert user, insert user_attribute_stats untuk semua 5 atribut default
dengan current_level=1 dan current_exp=0.

**Repository Interface yang dibuat:**
```
data/repository/
├── UserRepository.kt (interface)
├── QuestRepository.kt (interface)
├── QuestLogRepository.kt (interface)
├── TimerRepository.kt (interface)
├── AttributeRepository.kt (interface)
└── SettingsRepository.kt (interface)
```

Isi interface cukup method signatures saja — implementasi di task berikutnya.

**Acceptance Criteria:**
- [ ] AppDatabase compile tanpa error dengan semua 9 entity terdaftar
- [ ] Seed data terpanggil onCreate() — bukan onOpen()
- [ ] Seed attributes menghasilkan 5 baris di tabel attributes
- [ ] Seed app_settings menghasilkan 4 baris
- [ ] User default dengan id=1 tersedia setelah pertama kali DB dibuat
- [ ] user_attribute_stats ter-seed untuk semua 5 atribut user id=1
- [ ] Semua Repository interface tersedia

**Unit Test (src/androidTest):**
```
AppDatabaseSeedTest:
- onCreate seeds 5 default attributes
- onCreate seeds user with id 1
- onCreate seeds 5 user_attribute_stats for user 1
- onCreate seeds 4 app_settings entries
- epic_finale_bonus_exp seeded with value '1000'
- failure_threshold_sessions seeded with value '7'
- decay_rate_R seeded with empty string
```

---

## Epic 3 — Repository Implementation

### Task 3.1 — UserRepository & AttributeRepository Implementation
**Status:** TODO
**Skill:** database-conventions, testing-standards, validation-rules
**Dependensi:** Task 2.4

**Scope:**
Implementasi UserRepositoryImpl dan AttributeRepositoryImpl.

**UserRepositoryImpl — method yang diimplementasikan:**
```kotlin
fun getUser(): Flow<User?>                          // dari userDao.getUserById(userId = 1)
suspend fun markWelcomeSeen()
suspend fun updateEvaluationDate(date: String)
suspend fun updateInactiveDays(days: Int)
suspend fun addLifetimeExp(amount: Double)
```

**AttributeRepositoryImpl — method:**
```kotlin
fun getAllAttributes(): Flow<List<Attribute>>
suspend fun getAttributeById(id: Long): Attribute?
suspend fun addCustomAttribute(code: String, name: String, icon: String?): Long
    // Validasi: code tidak boleh duplikat, name tidak boleh kosong
suspend fun updateAttribute(id: Long, name: String, icon: String?)
suspend fun deleteCustomAttribute(id: Long)
    // Validasi: tidak boleh delete attribute yang is_default = 1
    // Validasi: tidak boleh delete attribute yang sedang dipakai oleh quest aktif
```

**Acceptance Criteria:**
- [ ] Semua method Repository memetakan Entity ke Domain Model sebelum return
- [ ] deleteCustomAttribute throw exception jika attribute masih dipakai quest aktif
- [ ] deleteCustomAttribute throw exception jika attribute is_default = 1
- [ ] addCustomAttribute throw exception jika code sudah ada

**Unit Test (src/test — mock DAO dengan MockK):**
```
UserRepositoryTest:
- getUser maps entity to domain model correctly
- markWelcomeSeen calls dao with correct userId

AttributeRepositoryTest:
- getAllAttributes maps list of entities to domain models
- deleteCustomAttribute throws when attribute is default
- deleteCustomAttribute throws when attribute used by active quest
- addCustomAttribute throws when code already exists
- updateAttribute calls dao with correct parameters
```

---

### Task 3.2 — QuestRepository Implementation
**Status:** TODO
**Skill:** database-conventions, testing-standards, validation-rules
**Dependensi:** Task 2.4

**Scope:**
Implementasi QuestRepositoryImpl — repository paling kompleks karena
menangani hierarki Epic + sub-quest dan semua validasi business rules.

**Method yang diimplementasikan:**

```kotlin
fun getTodayQuests(userId: Long, today: String): Flow<List<Quest>>
    // Gabungkan getDailyQuestsForToday + getCustomScheduleQuests
    // Filter custom_days: parse schedule_days CSV, cek apakah today (dayOfWeek) termasuk

fun getActiveEpicContainers(userId: Long): Flow<List<Quest>>

fun getSubQuests(parentId: Long): Flow<List<Quest>>

suspend fun getQuestById(id: Long): Quest?

suspend fun insertStandaloneQuest(quest: Quest): Long
    // Validasi: semua field wajib (lihat SKILL validation-rules section 2)
    // Validasi: absence_mode hanya jika time_bound
    // Validasi: schedule_days tidak kosong jika custom_days

suspend fun insertEpicWithFirstSubQuest(epic: Quest, subQuest: Quest): Long
    // Dalam satu @Transaction
    // Validasi epic: lihat SKILL validation-rules section 3
    // Validasi subQuest: lihat SKILL validation-rules section 2
    // Validasi: subQuest.endDate <= epic.endDate jika epic time_bound

suspend fun insertSubQuest(subQuest: Quest, parentId: Long): Long
    // Validasi subQuest sesuai section 2
    // Validasi endDate terhadap parent

suspend fun updateQuestStatus(id: Long, status: String)

suspend fun updateMissedSessions(id: Long, sessions: Int)

suspend fun updateEndDateShift(id: Long, newEndDate: String)

suspend fun addStackedDuration(id: Long, amount: Int)

suspend fun resetStackedDuration(id: Long)

suspend fun deleteQuest(id: Long)
    // CASCADE di DB menangani sub-quest otomatis

suspend fun archiveQuestToHistory(
    quest: Quest,
    finalStatus: String,
    totalDaysCompleted: Int,
    totalExpEarned: Double
)
```

**Acceptance Criteria:**
- [ ] getTodayQuests hanya return quest yang schedule_days-nya mengandung hari ini
- [ ] insertEpicWithFirstSubQuest dalam satu transaksi — rollback jika salah satu gagal
- [ ] insertStandaloneQuest throw QuestValidationException jika validasi gagal
- [ ] Semua validasi dari SKILL validation-rules section 2 dan 3 terimplementasi
- [ ] archiveQuestToHistory insert ke quest_history lalu delete dari quests

**Unit Test (src/test — mock DAO dengan MockK):**
```
QuestRepositoryTest:
- getTodayQuests filters custom_days correctly for day of week
- getTodayQuests includes daily quests regardless of schedule_days
- insertStandaloneQuest throws when title is empty
- insertStandaloneQuest throws when absence_mode set for endless quest
- insertStandaloneQuest throws when custom_days selected but schedule_days empty
- insertStandaloneQuest throws when time_bound but no end_date or target_days
- insertEpicWithFirstSubQuest throws when sub-quest end_date exceeds epic end_date
- insertEpicWithFirstSubQuest is atomic (both inserted or neither)
- archiveQuestToHistory inserts to history then deletes from quests
```

---

### Task 3.3 — QuestLogRepository & TimerRepository Implementation
**Status:** TODO
**Skill:** database-conventions, testing-standards, validation-rules
**Dependensi:** Task 2.4

**Scope:**
Implementasi QuestLogRepositoryImpl dan TimerRepositoryImpl.

**QuestLogRepositoryImpl:**
```kotlin
suspend fun insertLog(
    questId: Long,
    userId: Long,
    logDate: String,
    actualDurationSeconds: Int?,
    expAwarded: Double,
    isEpicFinaleBonus: Boolean
): Long

fun getLogsForDate(userId: Long, date: String): Flow<List<QuestLog>>

suspend fun hasLogForDate(questId: Long, date: String): Boolean
    // return questLogDao.getLogForDate(questId, date) != null

suspend fun getTotalCompletions(questId: Long): Int

suspend fun getTotalExpForQuest(questId: Long): Double
```

**TimerRepositoryImpl:**
```kotlin
suspend fun startTimer(questId: Long, targetDurationSeconds: Int): Boolean
    // Validasi: cek timerExistsForQuest — jika sudah ada return false
    // Insert ke active_timers jika belum ada
    // Return true jika berhasil

suspend fun cancelTimer(questId: Long)
    // Delete dari active_timers
    // Tidak insert ke quest_logs (tidak ada EXP)

suspend fun markAlarmFired(questId: Long)
    // Update alarm_fired_at = now

suspend fun getTimerByQuestId(questId: Long): ActiveTimer?

fun getPendingConfirmationTimers(): Flow<List<ActiveTimer>>

fun getRunningTimers(): Flow<List<ActiveTimer>>

suspend fun confirmTimerComplete(
    questId: Long,
    userId: Long,
    logDate: String,
    actualDurationSeconds: Int,
    expAwarded: Double
)
    // Dalam satu @Transaction:
    // 1. Delete dari active_timers
    // 2. Insert ke quest_logs
```

**Acceptance Criteria:**
- [ ] startTimer return false (tidak throw) jika timer sudah ada untuk quest tersebut
- [ ] confirmTimerComplete dalam satu transaksi — delete timer + insert log atomik
- [ ] cancelTimer tidak insert ke quest_logs dalam kondisi apapun
- [ ] hasLogForDate return Boolean (bukan nullable)

**Unit Test (src/test — mock DAO dengan MockK):**
```
QuestLogRepositoryTest:
- insertLog calls dao with correct parameters
- hasLogForDate returns true when log exists for date
- hasLogForDate returns false when no log for date

TimerRepositoryTest:
- startTimer returns false when timer already exists for quest
- startTimer returns true and inserts when no existing timer
- cancelTimer deletes timer without inserting quest_log
- confirmTimerComplete deletes timer and inserts log in transaction
- markAlarmFired calls dao with correct questId and timestamp
```

---

### Task 3.4 — SettingsRepository & ExpRepository Implementation
**Status:** TODO
**Skill:** database-conventions, testing-standards, business-logic
**Dependensi:** Task 2.4

**Scope:**
Implementasi SettingsRepositoryImpl dan ExpRepositoryImpl (baru, tidak ada di interface awal — tambahkan interface-nya).

**SettingsRepositoryImpl:**
```kotlin
fun getAllSettings(): Flow<List<AppSetting>>

suspend fun getValue(key: String): String?

suspend fun setValue(key: String, value: String)

suspend fun getDecayRateR(): Double?
    // return getValue('decay_rate_R')?.toDoubleOrNull()

suspend fun getDecayGracePeriodDays(): Int?
    // return getValue('decay_grace_period_days')?.toIntOrNull()

suspend fun getFailureThresholdSessions(): Int
    // return getValue('failure_threshold_sessions')?.toIntOrNull() ?: 7

suspend fun getEpicFinaleBonus(): Double
    // return getValue('epic_finale_bonus_exp')?.toDoubleOrNull() ?: 1000.0
```

**ExpRepositoryImpl (tambahkan interface ExpRepository):**
```kotlin
suspend fun awardExp(
    userId: Long,
    attributeId: Long,
    amount: Double,
    now: String
)
    // Dalam satu @Transaction:
    // 1. Get current stat untuk userId + attributeId
    // 2. Hitung new_exp = current_exp + amount
    // 3. Loop level up: while new_exp >= targetExp(level) → level++, new_exp -= targetExp(level-1)
    // 4. Update user_attribute_stats
    // 5. Update users.total_exp_earned_lifetime += amount

suspend fun applyDecay(
    userId: Long,
    inactiveDays: Int,
    decayRateR: Double,
    now: String
)
    // Dalam satu @Transaction:
    // 1. Hitung exp_to_deduct = decayRateR * (inactiveDays - 1)
    // 2. Get semua stats untuk userId
    // 3. Untuk setiap stat: new_exp = MAX(current_exp - exp_to_deduct, 0.0)
    // 4. Update stats
    // 5. Insert ke exp_decay_log untuk setiap stat yang berubah
    // TIDAK BOLEH menurunkan current_level dalam kondisi apapun

fun calculateTargetExp(level: Int): Double
    // 100.0 * level.toDouble().pow(1.5)
    // Pure function — letakkan di util/ExpCalculator.kt, panggil dari sini
```

**Acceptance Criteria:**
- [ ] getDecayRateR return null jika value kosong atau tidak parseable sebagai Double
- [ ] getFailureThresholdSessions return 7 sebagai fallback jika setting tidak ada
- [ ] applyDecay tidak pernah menghasilkan current_exp negatif
- [ ] applyDecay tidak pernah mengubah current_level
- [ ] awardExp menangani multiple level up dalam satu call
- [ ] calculateTargetExp letaknya di util/ExpCalculator.kt sebagai pure function

**Unit Test (src/test):**
```
SettingsRepositoryTest:
- getDecayRateR returns null when value is empty string
- getDecayRateR returns null when value is non-numeric
- getFailureThresholdSessions returns 7 when setting not found
- getEpicFinaleBonus returns 1000.0 as default

ExpCalculatorTest (src/test/util/):
- calculateTargetExp returns 100.0 for level 1
- calculateTargetExp returns correct value for level 5 (1118.03...)
- calculateTargetExp returns correct value for level 10

ExpRepositoryTest:
- awardExp increments exp correctly without level up
- awardExp triggers level up when exp reaches threshold
- awardExp handles multiple level ups in single call
- awardExp does not produce negative exp
- applyDecay reduces exp by correct amount
- applyDecay floors exp at 0, never negative
- applyDecay never reduces current_level
- applyDecay inserts entry to exp_decay_log for each stat
- applyDecay skips execution when inactiveDays < 2
```

---

## Epic 4 — Lazy Evaluation Engine

### Task 4.1 — ScheduleCalculator & MissedSessionCalculator
**Status:** TODO
**Skill:** business-logic, testing-standards
**Dependensi:** Task 3.2

**Scope:**
Implementasi pure utility functions untuk kalkulasi jadwal dan sesi terlewat.
Semua fungsi di util/ tanpa Android dependency — bisa ditest murni sebagai unit test JVM.

**util/ScheduleCalculator.kt:**
```kotlin
object ScheduleCalculator {

    // Parse CSV schedule_days ke List<Int>
    // Input: "1,3,5" → Output: [1, 3, 5] (1=Senin..7=Minggu)
    fun parseScheduleDays(scheduleDays: String?): List<Int>

    // Cek apakah date termasuk dalam schedule
    // Input: date (LocalDate), scheduleDays (List<Int>)
    // Output: true jika hari itu terjadwal
    fun isScheduledOnDate(date: LocalDate, scheduleDays: List<Int>): Boolean

    // Ambil semua tanggal terjadwal dalam rentang (inklusif)
    // Input: fromDate, toDate, scheduleType, scheduleDays
    // Output: List<LocalDate> hari-hari terjadwal dalam rentang
    fun getScheduledDatesInRange(
        fromDate: LocalDate,
        toDate: LocalDate,
        scheduleType: String,  // 'daily' atau 'custom_days'
        scheduleDays: List<Int>
    ): List<LocalDate>

    // Cari slot jadwal berikutnya setelah fromDate (untuk mode Shift)
    // Input: fromDate, scheduleDays, count (berapa slot berikutnya)
    // Output: LocalDate slot ke-N berikutnya
    fun findNextScheduledDay(
        fromDate: LocalDate,
        scheduleDays: List<Int>,
        count: Int = 1
    ): LocalDate
}
```

**util/MissedSessionCalculator.kt:**
```kotlin
object MissedSessionCalculator {

    // Hitung consecutive missed sessions untuk satu quest
    // Input: daftar tanggal terjadwal (dari ScheduleCalculator),
    //        daftar tanggal yang punya log (dari quest_logs),
    //        lastCompletedAt (String? dari quest)
    // Output: jumlah sesi terjadwal berturut-turut dari belakang (terbaru) yang tidak punya log
    fun calculateConsecutiveMissedSessions(
        scheduledDates: List<LocalDate>,
        completedDates: Set<LocalDate>
    ): Int

    // Hitung apakah suatu hari adalah "scheduled day with no completion"
    // untuk keperluan update consecutive_inactive_scheduled_days di users
    // Input: date, semua quest aktif user (untuk cek apakah ada yang terjadwal),
    //        daftar log di hari itu
    // Output: true jika ada quest terjadwal di hari itu tapi tidak ada satu pun yang selesai
    fun isDayInactiveScheduled(
        date: LocalDate,
        questsScheduledOnDate: List<Quest>,
        completionsOnDate: List<QuestLog>
    ): Boolean
}
```

**Acceptance Criteria:**
- [ ] parseScheduleDays return empty list untuk input null atau kosong
- [ ] getScheduledDatesInRange untuk 'daily' return semua hari dalam rentang
- [ ] getScheduledDatesInRange untuk 'custom_days' hanya return hari yang terjadwal
- [ ] findNextScheduledDay tidak pernah return fromDate itu sendiri (selalu setelahnya)
- [ ] calculateConsecutiveMissedSessions return 0 jika semua sesi terjadwal sudah selesai
- [ ] calculateConsecutiveMissedSessions return 0 jika tidak ada sesi terjadwal dalam rentang
- [ ] isDayInactiveScheduled return false jika tidak ada quest terjadwal di hari itu

**Unit Test (src/test/util/):**
```
ScheduleCalculatorTest:
- parseScheduleDays returns empty list for null input
- parseScheduleDays returns empty list for empty string
- parseScheduleDays parses "1,3,5" correctly to [1,3,5]
- isScheduledOnDate returns true for daily quest on any day
- isScheduledOnDate returns true for custom_days quest on scheduled day
- isScheduledOnDate returns false for custom_days quest on non-scheduled day
- getScheduledDatesInRange daily returns all days inclusive
- getScheduledDatesInRange custom returns only scheduled days
- findNextScheduledDay returns next scheduled day after fromDate
- findNextScheduledDay skips non-scheduled days correctly
- findNextScheduledDay with count=3 returns third next scheduled day

MissedSessionCalculatorTest:
- calculateConsecutiveMissedSessions returns 0 when all sessions completed
- calculateConsecutiveMissedSessions returns correct count for trailing misses
- calculateConsecutiveMissedSessions returns 0 when no scheduled dates
- calculateConsecutiveMissedSessions resets count when completion found in sequence
- isDayInactiveScheduled returns false when no quests scheduled on date
- isDayInactiveScheduled returns false when at least one completion exists
- isDayInactiveScheduled returns true when quests scheduled but none completed
```

---

### Task 4.2 — LazyEvaluationRepository Implementation
**Status:** TODO
**Skill:** business-logic, testing-standards, database-conventions
**Dependensi:** Task 3.2, Task 3.3, Task 3.4, Task 4.1

**Scope:**
Implementasi engine lazy evaluation — ini adalah task paling kompleks di seluruh project.
Buat LazyEvaluationRepository (interface + impl) di data/repository/.

**Method utama:**

```kotlin
suspend fun runLayerB(userId: Long): LayerBResult
    // Tidak ada guard — selalu jalan
    // 1. Get pending confirmation timers (alarm_fired_at IS NOT NULL)
    // 2. Get running timers (alarm_fired_at IS NULL)
    // Return: data untuk di-render UI

suspend fun runLayerA(userId: Long, today: LocalDate): LayerAResult
    // Guard: cek last_evaluated_date vs today
    // Jika last_evaluated_date == today: return LayerAResult.AlreadyEvaluated
    // Jika last_evaluated_date IS NULL: set today, return LayerAResult.FirstTime

    // Step 1: hitung rentang evaluasi (last_evaluated_date+1 s/d yesterday)
    // Step 2: per quest aktif — update consecutive_missed_sessions
    //         via MissedSessionCalculator + ScheduleCalculator
    //         paralel: update users.consecutive_inactive_scheduled_days
    // Step 3: proses cascade failure (urutan wajib sesuai SKILL business-logic section 3)
    // Step 4: proses absence_mode (Shift/Stack) untuk quest time_bound yang masih hidup
    // Step 5: proses EXP decay via ExpRepository.applyDecay()
    //         (skip jika decay_rate_R atau grace_period_days belum dikonfigurasi)
    // Step 6: UPDATE last_evaluated_date = today, last_active_at = now

    // Seluruh Step 1-6 dalam SATU @Transaction
```

**Data class hasil:**
```kotlin
data class LayerBResult(
    val pendingConfirmationTimers: List<ActiveTimer>,
    val runningTimers: List<ActiveTimer>
)

sealed class LayerAResult {
    object AlreadyEvaluated : LayerAResult()
    object FirstTime : LayerAResult()
    data class Evaluated(
        val failedQuestIds: List<Long>,
        val decayApplied: Boolean
    ) : LayerAResult()
}
```

**Acceptance Criteria:**
- [ ] runLayerA return AlreadyEvaluated jika last_evaluated_date == today
- [ ] runLayerA return FirstTime jika last_evaluated_date IS NULL (tidak proses apapun)
- [ ] Urutan Step 1-6 diikuti tepat sesuai SKILL business-logic section 6
- [ ] Step 3 cascade: sub-quest gagal memicu kegagalan Epic induk
- [ ] Step 3 cascade: saudara sub-quest yang 'completed' diarsipkan sebagai 'completed'
- [ ] Step 3 cascade: saudara sub-quest yang 'active' diarsipkan sebagai 'failed_via_parent'
- [ ] Step 4 Shift: end_date digeser via findNextScheduledDay(), bukan hari kalender
- [ ] Step 5 decay: di-skip jika decay_rate_R atau grace_period kosong
- [ ] Seluruh Lapis A dalam satu transaksi DB

**Unit Test (src/test — mock semua Repository dependencies dengan MockK):**
```
LazyEvaluationRepositoryTest:
- runLayerA returns AlreadyEvaluated when last_evaluated_date equals today
- runLayerA returns FirstTime when last_evaluated_date is null
- runLayerA skips decay when decay_rate_R is not configured
- runLayerA skips decay when grace_period_days is not configured
- runLayerA processes cascade failure before absence_mode
- runLayerA archives sibling as completed when sibling status is completed
- runLayerA archives sibling as failed_via_parent when sibling status is active
- runLayerA applies Shift by calling findNextScheduledDay not calendar day increment
- runLayerA applies Stack by adding missed_sessions * target_duration_seconds
- runLayerA updates last_evaluated_date after successful evaluation
- runLayerB always runs regardless of last_evaluated_date
- runLayerB returns correct pending and running timer lists
```

---

## Epic 5 — Today's Quests Screen

### Task 5.1 — TodayQuestsViewModel
**Status:** TODO
**Skill:** testing-standards, business-logic, android-lifecycle
**Dependensi:** Task 3.2, Task 3.3, Task 4.2

**Scope:**
Implementasi TodayQuestsViewModel yang mengorkestrasi lazy evaluation dan
data Today's Quests.

**UiState:**
```kotlin
sealed class TodayQuestsUiState {
    object Loading : TodayQuestsUiState()
    data class Success(
        val quests: List<Quest>,
        val pendingConfirmationTimers: List<ActiveTimer>,
        val runningTimers: List<ActiveTimer>
    ) : TodayQuestsUiState()
    data class Error(val message: String) : TodayQuestsUiState()
}
```

**Method ViewModel:**
```kotlin
fun initialize()
    // 1. runLayerB → update state dengan timer data
    // 2. runLayerA → jalankan evaluasi harian jika perlu
    // 3. collect getTodayQuests → update state dengan quest list

fun completeInstantQuest(questId: Long, attributeId: Long)
    // 1. Insert quest_log untuk hari ini
    // 2. Award EXP via ExpRepository
    // 3. Cek apakah quest time_bound dan hari ini adalah end_date → award epic finale bonus
    // 4. Emit level up event jika naik level

fun startTimer(questId: Long, targetDurationSeconds: Int)
    // Panggil TimerRepository.startTimer()
    // Jika return false: emit error "Timer sudah berjalan untuk quest ini"

fun cancelTimer(questId: Long)
    // Panggil TimerRepository.cancelTimer()
    // Cancel AlarmManager via utility function

fun confirmTimerComplete(questId: Long, attributeId: Long, actualDurationSeconds: Int)
    // 1. TimerRepository.confirmTimerComplete()
    // 2. Award EXP
    // 3. Cek epic finale bonus
    // 4. Emit level up event jika naik level
```

**Acceptance Criteria:**
- [ ] initialize() memanggil runLayerB sebelum runLayerA
- [ ] completeInstantQuest menambah entry ke quest_logs
- [ ] completeInstantQuest memberikan EXP ke atribut yang benar
- [ ] startTimer emit error jika timer sudah ada (return false dari repository)
- [ ] confirmTimerComplete tidak bisa dipanggil untuk quest tanpa active_timer

**Unit Test (src/test — mock semua Repository):**
```
TodayQuestsViewModelTest:
- initial state is Loading
- initialize calls layerB before layerA
- initialize emits Success state after data loaded
- completeInstantQuest inserts log and awards exp
- completeInstantQuest awards epic finale bonus on quest end_date
- startTimer emits error when timer already exists
- cancelTimer calls repository cancel without awarding exp
- confirmTimerComplete awards exp and removes timer
```

---

### Task 5.2 — Today's Quests UI
**Status:** TODO
**Skill:** ui-design-qa, android-lifecycle
**Dependensi:** Task 5.1

**Scope:**
Implementasi screen Today's Quests lengkap dengan semua state dan interaksi.

**Layout:**
- TopAppBar: judul "Today's Quests" + tanggal hari ini
- FAB (+) di bottom right: navigasi ke CreateQuest
- Body: LazyColumn berisi QuestCard per quest

**QuestCard — komponen reusable di ui/component/QuestCard.kt:**
```
Tampilkan:
- Judul quest (maxLines=1, overflow=Ellipsis)
- Nama atribut + icon
- Badge status: "Selesai ✓" (jika sudah ada log hari ini) atau kosong
- Untuk quest selesai: seluruh card di-strikethrough atau opacity 50%

Untuk quest instant (completion_mode = 'instant'):
- Tombol centang (✓) di kanan — disabled jika sudah ada log hari ini

Untuk quest timer (completion_mode = 'timer'):
- Jika tidak ada active_timer: tombol "Mulai" di kanan
- Jika ada active_timer (alarm_fired_at IS NULL): progress timer real-time +
  tombol "Batal" (merah kecil)
- Jika ada active_timer (alarm_fired_at IS NOT NULL): badge "Selesai — Konfirmasi" +
  tombol "Konfirmasi" (hijau) — TANPA bunyi atau notifikasi baru
```

**Empty state:**
Jika quests kosong: tampilkan ilustrasi sederhana + teks "Tidak ada quest hari ini"
+ tombol "Buat Quest" yang navigasi ke CreateQuest.

**Loading state:**
Tampilkan shimmer placeholder atau CircularProgressIndicator di tengah layar
selama initialize() belum selesai.

**Error state:**
Tampilkan pesan error + tombol "Coba Lagi" yang memanggil initialize() ulang.

**Acceptance Criteria sesuai SKILL ui-design-qa:**
- [ ] Semua Text punya maxLines + overflow
- [ ] Semua padding menggunakan Spacing token
- [ ] LazyColumn dengan key = { it.id }
- [ ] Empty state informatif dengan CTA
- [ ] Loading dan Error state tersedia
- [ ] Quest selesai tetap tampil (tidak hilang dari list)
- [ ] Timer pending confirmation tidak membunyikan alarm baru
- [ ] Behavioral checklist PRD dilampirkan di artifact hasil

**Unit Test:**
Tidak ada unit test untuk Composable — verifikasi manual di emulator.
ViewModel sudah ditest di Task 5.1.

---

## Epic 6 — Foreground Service & AlarmManager

### Task 6.1 — QuestTimerService & AlarmManager Integration
**Status:** TODO
**Skill:** android-lifecycle, testing-standards
**Dependensi:** Task 3.3, Task 5.1

**Scope:**
Implementasi Foreground Service untuk timer quest dan integrasi AlarmManager.

**QuestTimerService (service/QuestTimerService.kt):**
- Return START_STICKY di onStartCommand
- Baca state dari active_timers (source of truth) saat onStartCommand dipanggil
- Hitung remaining = targetDuration - (now - startedAt)
- Jika remaining <= 0: langsung panggil markAlarmFired + fire notifikasi
- Jika remaining > 0: jadwalkan AlarmManager untuk sisa waktu
- Tampilkan Foreground notification dengan nama quest dan timer countdown
- stopSelf() setelah alarm di-fire

**util/AlarmScheduler.kt — pure utility:**
```kotlin
object AlarmScheduler {
    fun scheduleQuestAlarm(context: Context, questId: Long, triggerAtMillis: Long)
    fun cancelQuestAlarm(context: Context, questId: Long)
}
```

**QuestAlarmReceiver (receiver/QuestAlarmReceiver.kt):**
- Terima broadcast dari AlarmManager
- Panggil TimerRepository.markAlarmFired(questId)
- Tampilkan notifikasi sistem Android (bunyi + getar)
- Isi notifikasi: "Quest selesai! Tap untuk konfirmasi"

**BootReceiver (receiver/BootReceiver.kt):**
- Listen ACTION_BOOT_COMPLETED
- Query semua active_timers WHERE alarm_fired_at IS NULL
- Untuk setiap timer: reschedule AlarmManager jika triggerAt > now
- Untuk setiap timer: markAlarmFired jika triggerAt <= now (sudah lewat saat reboot)

**Registrasi di AndroidManifest.xml:**
Tambahkan Service, kedua Receiver, dan permission yang dibutuhkan:
- FOREGROUND_SERVICE
- RECEIVE_BOOT_COMPLETED
- SCHEDULE_EXACT_ALARM (Android 12+)
- POST_NOTIFICATIONS (Android 13+)

**Acceptance Criteria:**
- [ ] Service return START_STICKY
- [ ] Service baca state dari DB saat start, bukan dari parameter Intent saja
- [ ] AlarmManager menggunakan setExactAndAllowWhileIdle()
- [ ] PendingIntent menggunakan FLAG_IMMUTABLE
- [ ] BootReceiver me-reschedule timer yang masih aktif setelah reboot
- [ ] QuestAlarmReceiver tidak commit EXP — hanya markAlarmFired + notifikasi
- [ ] Semua permission terdaftar di AndroidManifest

**Unit Test:**
```
AlarmSchedulerTest (src/test/util/):
- scheduleQuestAlarm creates PendingIntent with correct questId as request code
- cancelQuestAlarm cancels existing PendingIntent

BootReceiverTest (src/test):
- onReceive reschedules timers where alarm not yet fired and trigger in future
- onReceive marks alarm fired for timers where trigger already passed
- onReceive ignores non-BOOT_COMPLETED intents
```

---

## Epic 7 — Quest Creation Flow

### Task 7.1 — CreateQuestViewModel
**Status:** TODO
**Skill:** validation-rules, testing-standards, business-logic
**Dependensi:** Task 3.1, Task 3.2

**Scope:**
Implementasi ViewModel untuk form pembuatan quest — menangani semua cabang
(standalone, Epic container, sub-quest) dan validasi real-time.

**FormState:**
```kotlin
data class CreateQuestFormState(
    val title: String = "",
    val description: String = "",
    val selectedAttributeId: Long? = null,
    val isContainer: Boolean = false,
    val completionMode: String? = null,        // 'instant' | 'timer'
    val targetDurationSeconds: Int? = null,
    val durationType: String = "endless",      // 'endless' | 'time_bound'
    val durationInputType: String = "days",    // 'date' | 'days'
    val targetDays: Int? = null,
    val endDate: String? = null,
    val absenceMode: String? = null,           // 'shift' | 'stack'
    val scheduleType: String = "daily",        // 'daily' | 'custom_days'
    val scheduleDays: List<Int> = emptyList(),
    val parentQuestId: Long? = null,
    val parentEndDate: String? = null,         // untuk validasi sub-quest
    val errors: Map<String, String> = emptyMap()
)
```

**Method ViewModel:**
```kotlin
fun updateTitle(title: String)
fun updateDescription(desc: String)
fun selectAttribute(attributeId: Long)
fun setIsContainer(isContainer: Boolean)
fun setCompletionMode(mode: String)
fun setTargetDuration(seconds: Int)
fun setDurationType(type: String)
fun setDurationInputType(type: String)
fun setTargetDays(days: Int)
fun setEndDate(date: String)
fun setAbsenceMode(mode: String)
fun setScheduleType(type: String)
fun toggleScheduleDay(dayOfWeek: Int)
fun setParentQuestId(parentId: Long, parentEndDate: String?)

fun validateAndSave(): Boolean
    // Validasi semua field sesuai SKILL validation-rules
    // Jika ada error: update formState.errors, return false
    // Jika valid:
    //   isContainer = true: panggil QuestRepository.insertEpicWithFirstSubQuest()
    //   isContainer = false dan parentQuestId != null: insertSubQuest()
    //   isContainer = false dan parentQuestId == null: insertStandaloneQuest()
    //   return true jika berhasil
```

**Acceptance Criteria:**
- [ ] formState.errors di-update per-field saat validasi gagal
- [ ] absence_mode field hanya relevan (dan visible di UI) jika duration_type = time_bound
- [ ] completion_mode field tidak ada di formState jika isContainer = true
- [ ] schedule_days hanya relevan jika schedule_type = custom_days
- [ ] validateAndSave memanggil method Repository yang tepat sesuai jenis quest
- [ ] parentEndDate dipakai untuk validasi sub-quest end_date

**Unit Test (src/test):**
```
CreateQuestViewModelTest:
- validateAndSave sets error when title is empty
- validateAndSave sets error when attribute not selected
- validateAndSave sets error when timer mode but no duration
- validateAndSave sets error when time_bound but no end_date or target_days
- validateAndSave sets error when custom_days but no days selected
- validateAndSave sets error when sub-quest end_date exceeds parent end_date
- validateAndSave clears errors on successful validation
- validateAndSave calls insertStandaloneQuest for non-container without parent
- validateAndSave calls insertSubQuest when parentQuestId is set
- validateAndSave calls insertEpicWithFirstSubQuest when isContainer is true
- setDurationType to endless clears absenceMode from formState
```

---

### Task 7.2 — Create Quest UI
**Status:** TODO
**Skill:** ui-design-qa, validation-rules
**Dependensi:** Task 7.1, Task 3.1

**Scope:**
Implementasi form CreateQuest yang menampilkan field secara kondisional
berdasarkan pilihan user.

**Layout form (scroll vertical):**

Section 1 — Dasar:
- Field teks: Judul (wajib, maxChar=100)
- Field teks: Deskripsi (opsional)
- Dropdown/selector: Atribut (load dari AttributeRepository)
- Toggle: "Epic Quest dengan sub-quest?" (switch/checkbox)

Section 2 — Mode penyelesaian (HIDDEN jika isContainer = true):
- Radio button: Instant Complete | Timer
- Jika Timer dipilih: input durasi (jam:menit atau menit saja)

Section 3 — Jadwal (HIDDEN jika isContainer = true):
- Radio button: Setiap Hari | Hari Tertentu
- Jika Hari Tertentu: 7 toggle button (Sen/Sel/Rab/Kam/Jum/Sab/Min)

Section 4 — Masa aktif:
- Radio button: Tanpa Batas | Berjangka
- Jika Berjangka: toggle "Input tanggal" | "Jumlah hari"
  - Input tanggal: DatePicker
  - Jumlah hari: NumberInput
  - Jika sub-quest: tampilkan info "Batas maksimal: [parentEndDate]"

Section 5 — Mode bolos (HIDDEN jika durationType = endless atau isContainer = true):
- Radio button: Geser Jadwal | Tumpuk Tugas
- Deskripsi singkat untuk masing-masing pilihan

Tombol Simpan (full width, disabled jika ada error):
- Label: "Simpan Quest" (standalone/sub-quest) | "Lanjut Tambah Sub-Quest" (Epic container)

**Sub-quest flow untuk Epic container:**
Jika isContainer = true, setelah Simpan berhasil → screen tidak ditutup,
tapi form direset untuk input sub-quest pertama dengan parentQuestId sudah terisi.
Tampilkan banner "Menambah sub-quest untuk: [judul Epic]".
Setelah sub-quest pertama disimpan (via insertEpicWithFirstSubQuest), navigasi kembali.

**Acceptance Criteria sesuai SKILL ui-design-qa:**
- [ ] Field kondisional muncul/hilang dengan animasi
- [ ] Error per-field tampil di bawah field yang bersangkutan (bukan toast)
- [ ] Tombol Simpan disabled selama ada error yang belum diperbaiki
- [ ] Sub-quest DatePicker tidak bisa pilih tanggal setelah parentEndDate
- [ ] Semua Text punya maxLines + overflow
- [ ] Semua padding menggunakan Spacing token
- [ ] Behavioral checklist PRD dilampirkan di artifact hasil

---

## Epic 8 — Master Plan / Log Screen

### Task 8.1 — MasterPlanViewModel
**Status:** TODO
**Skill:** testing-standards, business-logic
**Dependensi:** Task 3.1, Task 3.2, Task 3.4

**Scope:**
Implementasi ViewModel untuk Master Plan screen dengan 3 tab.

```kotlin
data class MasterPlanUiState(
    val characterTab: CharacterTabState = CharacterTabState.Loading,
    val activeQuestsTab: ActiveQuestsTabState = ActiveQuestsTabState.Loading,
    val historyTab: HistoryTabState = HistoryTabState.Loading
)

data class CharacterTabState(
    val username: String,
    val totalExpLifetime: Double,
    val consecutiveInactiveDays: Int,
    val attributeStats: List<AttributeStatDisplay>
)

data class AttributeStatDisplay(
    val attribute: Attribute,
    val level: Int,
    val currentExp: Double,
    val targetExp: Double,      // 100 * level^1.5
    val progressFraction: Float // currentExp / targetExp
)

data class ActiveQuestsTabState(
    val epicContainers: List<EpicQuestDisplay>,
    val standaloneQuests: List<Quest>
)

data class EpicQuestDisplay(
    val epic: Quest,
    val subQuests: List<Quest>,
    val daysElapsed: Int,
    val totalDays: Int?    // null jika endless
)

data class HistoryTabState(
    val entries: List<QuestHistory>
)
```

**Acceptance Criteria:**
- [ ] progressFraction dihitung dengan formula 100 * level^1.5
- [ ] daysElapsed dihitung dari quest.startDate ke hari ini
- [ ] totalDays null untuk endless Epic
- [ ] historyTab diurutkan ended_at DESC

**Unit Test (src/test):**
```
MasterPlanViewModelTest:
- characterTab calculates progressFraction correctly for each attribute
- characterTab progressFraction never exceeds 1.0
- activeQuestsTab groups sub-quests under correct epic container
- activeQuestsTab separates standalone quests from epic containers
- historyTab entries ordered by ended_at descending
```

---

### Task 8.2 — Master Plan UI
**Status:** TODO
**Skill:** ui-design-qa
**Dependensi:** Task 8.1

**Scope:**
Implementasi screen Master Plan dengan 3 tab.

**Tab 1 — Karakter:**
- Header: nama user
- Grid atau list atribut stats:
  - Nama atribut + icon
  - "Level [N]"
  - Progress bar EXP (currentExp / targetExp)
  - Label "[currentExp] / [targetExp] EXP"
- Footer stats: Total EXP lifetime, Hari nonaktif saat ini

**Tab 2 — Quest Aktif:**
- Section "Epic Quests": ExpansionPanel per Epic container
  - Header panel: judul Epic + progres hari (misal "Hari 23 / 100")
  - Header panel: progres bar (daysElapsed / totalDays, null = tidak ada bar)
  - Expanded: list sub-quest di dalamnya (judul, atribut, jadwal)
- Section "Quest Standalone": list biasa
- Empty state per section jika kosong

**Tab 3 — Riwayat:**
- LazyColumn riwayat quest
- Per item: judul, badge finalStatus (warna: hijau=completed, merah=failed_abandoned,
  oranye=failed_via_parent), total hari dikerjakan, total EXP earned
- Empty state jika belum ada riwayat

**Acceptance Criteria sesuai SKILL ui-design-qa:**
- [ ] Progress bar EXP akurat (tidak melebihi 100%)
- [ ] Badge warna berbeda untuk setiap finalStatus
- [ ] ExpansionPanel collapse/expand dengan animasi
- [ ] LazyColumn dengan key untuk riwayat
- [ ] Empty state per tab
- [ ] Behavioral checklist PRD dilampirkan di artifact hasil

---

## Epic 9 — Settings Screen

### Task 9.1 — Settings UI & ViewModel
**Status:** TODO
**Skill:** ui-design-qa, validation-rules, testing-standards
**Dependensi:** Task 3.1, Task 3.4

**Scope:**
Implementasi Settings screen + ViewModel dalam satu task karena scope-nya relatif kecil.

**Section di Settings:**

Section 1 — Konfigurasi Decay:
- Input: "Grace Period (hari)" — hanya angka positif
- Input: "Laju Decay (R)" — hanya angka positif, boleh desimal
- Info teks: "Kosongkan untuk menonaktifkan decay"

Section 2 — Atribut:
- List semua atribut (default + custom)
- Atribut default: tampilkan icon 🔒, tidak bisa dihapus, bisa edit nama/icon
- Atribut custom: bisa edit dan hapus
- Tombol "+ Tambah Atribut" di bawah list

Section 3 — Informasi:
- Versi aplikasi
- Label "Quest DDay"

**ViewModel method:**
```kotlin
fun loadSettings()
fun updateDecayR(value: String)          // validasi: angka positif atau kosong
fun updateGracePeriod(value: String)     // validasi: integer positif atau kosong
fun saveDecaySettings()
fun loadAttributes()
fun addAttribute(code: String, name: String, icon: String?)
fun updateAttribute(id: Long, name: String, icon: String?)
fun deleteAttribute(id: Long)            // show confirmation dialog sebelum delete
```

**Acceptance Criteria:**
- [ ] Input R dan grace period hanya menerima angka positif atau kosong
- [ ] Perubahan tersimpan ke app_settings di DB saat saveDecaySettings() dipanggil
- [ ] Atribut default tampilkan badge 🔒 dan tombol hapus di-disable
- [ ] Delete atribut custom tampilkan dialog konfirmasi sebelum eksekusi
- [ ] Jika atribut custom masih dipakai quest aktif: tampilkan error, jangan hapus

**Unit Test (src/test):**
```
SettingsViewModelTest:
- updateDecayR rejects negative number
- updateDecayR rejects non-numeric string
- updateDecayR accepts empty string (to disable decay)
- saveDecaySettings calls repository with correct values
- deleteAttribute shows confirmation before deleting
- deleteAttribute does not delete when attribute used by active quest
```

---

## Epic 10 — First Launch & Welcome Screen

### Task 10.1 — Welcome Screen & First Launch Flow
**Status:** TODO
**Skill:** ui-design-qa, android-lifecycle
**Dependensi:** Task 2.4, Task 5.2, Task 7.2

**Scope:**
Implementasi welcome screen untuk first launch dan logika routing awal app.

**Logika routing di MainActivity/NavGraph:**
```
Saat app pertama kali dibuka:
  Cek users.has_seen_welcome
  Jika 0: tampilkan WelcomeScreen
  Jika 1: langsung ke TodayQuestsScreen
```

**WelcomeScreen:**
- Judul: "Selamat Datang di Quest DDay"
- Subjudul: singkat tentang filosofi app (1-2 kalimat)
- Tombol utama: "Buat Quest Pertamamu" → navigasi ke CreateQuest,
  lalu setelah kembali → navigasi ke TodayQuests
- Tombol sekunder: "Lewati" → langsung ke TodayQuests
- Keduanya memanggil userRepository.markWelcomeSeen() sebelum navigasi

**Acceptance Criteria:**
- [ ] WelcomeScreen hanya muncul sekali (has_seen_welcome = 0)
- [ ] Setelah markWelcomeSeen(), buka app lagi langsung ke TodayQuests
- [ ] "Buat Quest Pertamamu" navigasi ke CreateQuest lalu kembali ke Today
- [ ] "Lewati" langsung ke TodayQuests
- [ ] Tidak ada flash/flicker saat routing (handle loading state routing dengan baik)

**Unit Test (src/test):**
```
WelcomeViewModelTest (jika ada):
- markWelcomeSeen called when either button tapped
- routing to Today after welcome dismissed
```

