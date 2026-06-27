# SKILL.md — database-conventions

## Kapan Skill Ini Di-load

Load skill ini setiap kali task menyentuh:
- Pembuatan atau modifikasi Room Entity
- Pembuatan atau modifikasi Room DAO
- Pembuatan atau modifikasi Repository
- Database migration
- Operasi multi-tabel apapun

---

## 1. Aturan Room Entity

### Penamaan

- Class entity: PascalCase + suffix Entity → contoh: QuestEntity, UserEntity
- Nama tabel: snake_case, sesuai nama tabel di PRD.md → contoh: "quests", "users"
- Nama kolom: snake_case, sesuai nama kolom di PRD.md → jangan ubah nama kolom tanpa alasan eksplisit

### Struktur wajib

```kotlin
@Entity(
    tableName = "nama_tabel",
    foreignKeys = [...],   // wajib jika ada relasi FK
    indices = [...]        // wajib untuk kolom yang sering di-query atau di-join
)
data class NamaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // kolom lain...
)
```

### Tipe data

- ID: Long (bukan Int)
- Timestamp: String dalam format ISO-8601 "YYYY-MM-DD HH:MM:SS"
- Boolean flag (misal is_container, is_default): Int (0 atau 1) — SQLite tidak punya tipe Boolean
- EXP dan nilai pecahan: Double
- Enum-like (misal status, completion_mode): String dengan nilai yang sudah ditetapkan di PRD

### Nullable

Kolom yang boleh NULL di PRD wajib dideklarasikan sebagai nullable (String?, Long?, Int?) di Entity.
Kolom yang NOT NULL di PRD wajib non-nullable dengan default value yang eksplisit.

---

## 2. Aturan DAO

### Penamaan

- Interface DAO: PascalCase + suffix Dao → contoh: QuestDao, UserDao
- Fungsi query: camelCase, deskriptif → contoh: getActiveQuestsByUserId, insertQuestLog

### Query wajib raw SQL

SEMUA query selain @Insert single-row WAJIB menggunakan @Query dengan raw SQL eksplisit.

```kotlin
// BENAR
@Query("SELECT * FROM quests WHERE user_id = :userId AND status = 'active'")
fun getActiveQuests(userId: Long): Flow<List<QuestEntity>>

// SALAH — auto-generated, dilarang
@Query("SELECT * FROM quests")
fun getAllQuests(): Flow<List<QuestEntity>>
// (ini masih @Query tapi tanpa filter — hampir selalu salah untuk production use)
```

### @Insert yang diizinkan

@Insert hanya diizinkan untuk single-row insert sederhana tanpa kondisi kompleks:

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertUser(user: UserEntity): Long
```

OnConflictStrategy WAJIB disebutkan eksplisit — tidak boleh dibiarkan default.

### Parameter query

WAJIB menggunakan named parameter dengan prefix titik dua:

```kotlin
// BENAR
@Query("SELECT * FROM quests WHERE user_id = :userId AND status = :status")
fun getQuestsByStatus(userId: Long, status: String): Flow<List<QuestEntity>>

// SALAH — string concatenation, dilarang keras (SQL injection risk)
```

### Return type

- Query yang hasilnya berubah seiring waktu (list quest, stats): Flow<T>
- Query one-shot (insert, update, delete, single fetch untuk operasi): suspend fun
- Query yang mungkin tidak menemukan hasil: T? (nullable)

### @Transaction

WAJIB digunakan untuk semua operasi yang melibatkan lebih dari satu tabel:

```kotlin
@Transaction
@Query("SELECT * FROM quests WHERE parent_quest_id = :parentId")
fun getQuestWithSubQuests(parentId: Long): Flow<List<QuestWithSubQuestsRelation>>
```

Semua fungsi Repository yang memanggil lebih dari satu DAO function dalam satu operasi
WAJIB dibungkus dalam withTransaction { } di sisi Repository.

---

## 3. Aturan Repository

### Struktur

Setiap Repository terdiri dari dua file:
- Interface: NamaRepository.kt di data/repository/
- Implementasi: NamaRepositoryImpl.kt di data/repository/

```kotlin
interface QuestRepository {
    fun getActiveQuests(userId: Long): Flow<List<Quest>>
    suspend fun insertQuest(quest: Quest): Long
    // ...
}

class QuestRepositoryImpl(
    private val questDao: QuestDao,
    private val db: AppDatabase
) : QuestRepository {
    // ...
}
```

### Mapping Entity ↔ Domain Model

Repository WAJIB melakukan mapping antara Entity (data layer) dan Domain Model (domain layer).
ViewModel tidak boleh menerima atau mengirim Entity langsung — selalu Domain Model.

```kotlin
// Di Repository
override fun getActiveQuests(userId: Long): Flow<List<Quest>> {
    return questDao.getActiveQuests(userId).map { entities ->
        entities.map { it.toDomainModel() }
    }
}
```

Fungsi mapping toDomainModel() dan toEntity() letakkan sebagai extension function
di file terpisah: data/local/entity/NamaEntityMapper.kt

### Transaksi multi-DAO

Untuk operasi yang butuh lebih dari satu DAO (misal cascade failure yang menyentuh
quests, quest_history, dan user_attribute_stats sekaligus):

```kotlin
suspend fun processCascadeFailure(epicId: Long) {
    db.withTransaction {
        // panggil beberapa DAO dalam satu transaksi atomik
        questDao.getSubQuests(epicId)
        questHistoryDao.insertAll(...)
        questDao.deleteEpic(epicId)
    }
}
```

---

## 4. Aturan Migration

Setiap perubahan schema WAJIB menggunakan Migration eksplisit — tidak boleh
menggunakan fallbackToDestructiveMigration() kecuali di environment development/testing.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE quests ADD COLUMN stacked_duration_seconds INTEGER NOT NULL DEFAULT 0")
    }
}
```

Setiap migration WAJIB didokumentasikan dengan komentar: alasan perubahan dan task mana yang membutuhkannya.

---

## 5. Seed Data Wajib

Saat database pertama kali dibuat (versi 1), wajib di-seed:

### Tabel attributes (default)
```
STR  | Strength    | 💪 | is_default=1
INT  | Intelligence| 🧠 | is_default=1
WIS  | Wisdom      | 🧘 | is_default=1
DEX  | Dexterity   | ⚡ | is_default=1
VIT  | Vitality    | ❤️ | is_default=1
```

### Tabel app_settings
```
epic_finale_bonus_exp    | 1000
decay_grace_period_days  | (kosong, diisi user)
decay_rate_R             | (kosong, diisi user)
failure_threshold_sessions | 7
```

Seed dilakukan via RoomDatabase.Callback pada onCreate(), bukan via Migration.

---

## 6. Checklist Sebelum Task DB Selesai

Sebelum melaporkan task database sebagai selesai, verifikasi:

- [ ] Semua Entity sesuai schema di PRD.md (nama tabel, kolom, tipe data, nullable)
- [ ] Semua query menggunakan raw @Query dengan named parameter
- [ ] Semua operasi multi-tabel dibungkus @Transaction
- [ ] Tidak ada auto-generated method selain @Insert yang diizinkan
- [ ] Migration tersedia jika ada perubahan schema
- [ ] Seed data lengkap untuk attributes dan app_settings
- [ ] Semua DAO function punya unit test minimal happy path + edge case NULL

