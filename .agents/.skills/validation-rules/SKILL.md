# SKILL.md — validation-rules

## Kapan Skill Ini Di-load

Load skill ini setiap kali task menyentuh:
- Form pembuatan atau edit quest (UI layer)
- Repository function yang menerima input dari user
- Operasi insert atau update ke tabel quests

---

## 1. Prinsip Dasar

Validasi dilakukan di DUA layer secara bersamaan — bukan pilih salah satu:

- **UI layer (ViewModel)**: validasi real-time saat user mengisi form, untuk feedback instan.
- **Repository layer**: validasi final sebelum data masuk ke DB, sebagai safety net.

Jika validasi gagal di Repository, lempar exception dengan pesan yang jelas —
jangan silent fail atau return null tanpa penjelasan.

---

## 2. Validasi Quest Standalone & Sub-quest

### Field wajib diisi (tidak boleh kosong atau null)

```
title           → tidak boleh kosong, maksimal 100 karakter
attribute_id    → wajib dipilih, tidak boleh null
completion_mode → wajib dipilih ('instant' atau 'timer')
duration_type   → wajib dipilih ('endless' atau 'time_bound')
schedule_type   → wajib dipilih ('daily' atau 'custom_days')
```

### Validasi kondisional

```
IF completion_mode = 'timer':
    target_duration_seconds WAJIB diisi dan > 0

IF duration_type = 'time_bound':
    end_date atau target_days WAJIB diisi
    absence_mode WAJIB dipilih ('shift' atau 'stack')
    IF duration_input_type = 'days':
        target_days WAJIB > 0
        end_date dihitung otomatis: start_date + target_days
    IF duration_input_type = 'date':
        end_date WAJIB > start_date (tidak boleh sama atau di masa lalu)

IF duration_type = 'endless':
    absence_mode WAJIB NULL — jangan isi dengan nilai apapun

IF schedule_type = 'custom_days':
    schedule_days WAJIB diisi dan minimal 1 hari dipilih
    format: CSV integer '1,3,5' (1=Senin, 2=Selasa, ..., 7=Minggu)

IF schedule_type = 'daily':
    schedule_days WAJIB NULL
```

### Validasi sub-quest terhadap parent (Epic container)

```
IF parent_quest_id IS NOT NULL:
    IF parent duration_type = 'time_bound':
        sub-quest end_date WAJIB <= parent end_date
        Jika sub-quest end_date > parent end_date → tolak dengan pesan:
        "Batas waktu sub-quest tidak boleh melebihi batas waktu Epic induk"
    IF parent duration_type = 'endless':
        sub-quest boleh time_bound atau endless — tidak ada constraint dari parent
```

---

## 3. Validasi Epic Container (is_container = 1)

### Field wajib diisi

```
title           → tidak boleh kosong, maksimal 100 karakter
attribute_id    → wajib dipilih (untuk bonus EXP finale)
duration_type   → wajib dipilih ('endless' atau 'time_bound')
```

### Field yang WAJIB NULL untuk Epic container

```
completion_mode          → WAJIB NULL
target_duration_seconds  → WAJIB NULL
schedule_type            → WAJIB NULL
schedule_days            → WAJIB NULL
stacked_duration_seconds → selalu 0, tidak relevan untuk container
```

### Validasi kondisional

```
IF duration_type = 'time_bound':
    end_date atau target_days WAJIB diisi
    absence_mode WAJIB dipilih

IF duration_type = 'endless':
    absence_mode WAJIB NULL
```

### Validasi minimal sub-quest

Epic container TIDAK BOLEH disimpan tanpa minimal 1 sub-quest.
Pembuatan Epic container + sub-quest pertama WAJIB dalam satu transaksi atomik:

```kotlin
// Di Repository
suspend fun insertEpicWithFirstSubQuest(
    epic: Quest,
    firstSubQuest: Quest
): Long {
    return db.withTransaction {
        val epicId = questDao.insertQuest(epic.toEntity())
        val subQuestWithParent = firstSubQuest.copy(parentQuestId = epicId)
        questDao.insertQuest(subQuestWithParent.toEntity())
        epicId
    }
}
```

Jika transaksi gagal di tengah jalan (misal sub-quest tidak valid),
seluruh operasi di-rollback — tidak boleh ada Epic tanpa sub-quest tersimpan di DB.

---

## 4. Validasi active_timers

```
Satu quest hanya boleh punya SATU timer aktif (UNIQUE constraint di DB sudah ada).
Sebelum insert ke active_timers, cek dulu:
    IF EXISTS (SELECT 1 FROM active_timers WHERE quest_id = :questId):
        jangan insert ulang — tampilkan error "Timer untuk quest ini sudah berjalan"

target_duration_seconds WAJIB > 0
quest yang di-timer WAJIB completion_mode = 'timer'
quest yang di-timer WAJIB status = 'active'
```

---

## 5. Validasi app_settings

Sebelum menjalankan Lapis A (lazy evaluation), cek apakah nilai konfigurasi sudah diisi:

```
IF app_settings['decay_rate_R'] IS NULL OR kosong:
    skip proses EXP decay — jangan jalankan dengan nilai default asumsi
    tampilkan reminder di UI Settings bahwa nilai R belum dikonfigurasi

IF app_settings['decay_grace_period_days'] IS NULL OR kosong:
    skip proses EXP decay — alasan sama
```

Failure threshold boleh punya fallback ke 7 jika kosong karena ini nilai yang sudah
ditetapkan di PRD dan tidak berubah-ubah seperti nilai R.

---

## 6. Error Handling Pattern

### Di ViewModel (feedback ke UI)

```kotlin
sealed class QuestFormError {
    object TitleEmpty : QuestFormError()
    object AttributeNotSelected : QuestFormError()
    object TimerDurationRequired : QuestFormError()
    object EndDateRequired : QuestFormError()
    object EndDateBeforeToday : QuestFormError()
    data class SubQuestExceedsParent(val parentEndDate: String) : QuestFormError()
    object CustomDaysEmpty : QuestFormError()
    object AbsenceModeRequired : QuestFormError()
}
```

Validasi di ViewModel WAJIB menghasilkan list error yang bisa ditampilkan
per-field di form — bukan satu error message generik untuk seluruh form.

### Di Repository (safety net)

```kotlin
// Lempar exception yang spesifik, bukan IllegalArgumentException generik
class QuestValidationException(message: String) : Exception(message)

// Contoh
if (quest.endDate != null && quest.parentEndDate != null) {
    if (quest.endDate > quest.parentEndDate) {
        throw QuestValidationException(
            "Sub-quest end_date (${quest.endDate}) exceeds parent end_date (${quest.parentEndDate})"
        )
    }
}
```

---

## 7. Checklist Sebelum Task Validasi Selesai

- [ ] Semua field wajib divalidasi di ViewModel sebelum submit
- [ ] Validasi kondisional sesuai tabel di section 2 dan 3 sudah diimplementasikan
- [ ] Sub-quest end_date divalidasi terhadap parent end_date
- [ ] Epic container tidak bisa disimpan tanpa minimal 1 sub-quest
- [ ] Insert Epic + sub-quest pertama dalam satu transaksi atomik
- [ ] active_timers di-cek sebelum insert timer baru (tidak boleh dobel)
- [ ] app_settings di-cek sebelum menjalankan decay — skip jika nilai belum dikonfigurasi
- [ ] Error di ViewModel per-field, bukan generik satu pesan
- [ ] Repository melempar QuestValidationException untuk pelanggaran constraint
- [ ] Unit test mencakup setiap kondisi validasi (valid input + setiap jenis invalid input)

