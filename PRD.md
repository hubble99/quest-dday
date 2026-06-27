# PRD — Quest DDay (MVP)

## 1. Visi & Filosofi

Aplikasi produktivitas dan pelacak kebiasaan yang berfokus pada **durasi penyelesaian tugas**
dengan fleksibilitas waktu tinggi. Elemen RPG digunakan sebagai alat visualisasi data
untuk membangun motivasi intrinsik — bukan sebagai gimmick.

---

## 2. Tech Stack

| Layer | Pilihan |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose (Material3) |
| Database | Room (raw `@Query` SQL) |
| Navigasi | Navigation Compose |
| Concurrency | Coroutines + Flow |
| Timer aktif | Foreground Service (native Android) |
| Timer state persistence | DataStore / active_timers DB |
| Notifikasi terjadwal | AlarmManager + BroadcastReceiver |
| Background decay/failure | Tidak ada — lazy evaluation saat app dibuka |
| Platform | Android only, minimum SDK 26 |

---

## 3. Schema Database

### `users`
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL DEFAULT 'Adventurer',
    last_active_at TEXT NOT NULL DEFAULT (datetime('now')),
    last_evaluated_date TEXT,
    consecutive_inactive_scheduled_days INTEGER NOT NULL DEFAULT 0,
    total_exp_earned_lifetime REAL NOT NULL DEFAULT 0,
    has_seen_welcome INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
```

### `attributes`
```sql
CREATE TABLE attributes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    icon TEXT,
    is_default INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER DEFAULT 0
);
```

### `user_attribute_stats`
```sql
CREATE TABLE user_attribute_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    attribute_id INTEGER NOT NULL,
    current_level INTEGER NOT NULL DEFAULT 1,
    current_exp REAL NOT NULL DEFAULT 0,
    last_gained_at TEXT,
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_id) REFERENCES attributes(id) ON DELETE RESTRICT,
    UNIQUE (user_id, attribute_id)
);
```

### `quests`
```sql
CREATE TABLE quests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    parent_quest_id INTEGER,
    attribute_id INTEGER,

    title TEXT NOT NULL,
    description TEXT,

    -- Apakah ini Epic container (punya sub-quest) atau quest yang dieksekusi langsung
    is_container INTEGER NOT NULL DEFAULT 0,

    -- Completion (NULL jika is_container = 1)
    completion_mode TEXT CHECK (completion_mode IN ('instant', 'timer')),
    target_duration_seconds INTEGER,

    -- Masa aktif
    duration_type TEXT NOT NULL CHECK (duration_type IN ('endless', 'time_bound')),
    duration_input_type TEXT CHECK (duration_input_type IN ('date', 'days')),
    target_days INTEGER,
    start_date TEXT NOT NULL DEFAULT (date('now')),
    end_date TEXT,

    -- Absence mode (NULL jika duration_type = 'endless')
    absence_mode TEXT CHECK (absence_mode IN ('shift', 'stack')),
    stacked_duration_seconds INTEGER NOT NULL DEFAULT 0,

    -- Jadwal (NULL jika is_container = 1)
    schedule_type TEXT DEFAULT 'daily' CHECK (schedule_type IN ('daily', 'custom_days')),
    schedule_days TEXT, -- CSV: '1,3,5' = Senin,Rabu,Jumat (1=Senin..7=Minggu)

    -- Status & tracking
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'completed', 'failed')),
    consecutive_missed_sessions INTEGER NOT NULL DEFAULT 0,
    last_completed_at TEXT,

    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_quest_id) REFERENCES quests(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_id) REFERENCES attributes(id) ON DELETE SET NULL
);

CREATE INDEX idx_quests_user_status ON quests(user_id, status);
CREATE INDEX idx_quests_parent ON quests(parent_quest_id);
```

### `quest_logs`
```sql
CREATE TABLE quest_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    quest_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    log_date TEXT NOT NULL,
    actual_duration_seconds INTEGER,
    exp_awarded REAL NOT NULL DEFAULT 0,
    is_epic_finale_bonus INTEGER NOT NULL DEFAULT 0,
    completed_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_quest_logs_quest_date ON quest_logs(quest_id, log_date);
CREATE INDEX idx_quest_logs_user_date ON quest_logs(user_id, log_date);
```

### `quest_history`
```sql
CREATE TABLE quest_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    original_quest_id INTEGER NOT NULL,  -- bukan FK aktif, quest asli bisa sudah dihapus
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    final_status TEXT NOT NULL CHECK (
        final_status IN ('completed', 'failed_abandoned', 'failed_via_parent')
    ),
    total_days_completed INTEGER NOT NULL DEFAULT 0,
    total_exp_earned REAL NOT NULL DEFAULT 0,
    started_at TEXT NOT NULL,
    ended_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### `active_timers`
```sql
CREATE TABLE active_timers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    quest_id INTEGER NOT NULL UNIQUE,
    started_at TEXT NOT NULL DEFAULT (datetime('now')),
    target_duration_seconds INTEGER NOT NULL,
    alarm_fired_at TEXT,  -- NULL = masih berjalan, terisi = alarm sudah bunyi
    FOREIGN KEY (quest_id) REFERENCES quests(id) ON DELETE CASCADE
);
```

### `exp_decay_log`
```sql
CREATE TABLE exp_decay_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_attribute_stat_id INTEGER NOT NULL,
    inactive_days INTEGER NOT NULL,
    exp_before REAL NOT NULL,
    exp_after REAL NOT NULL,
    decay_rate_used REAL NOT NULL,
    processed_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_attribute_stat_id) REFERENCES user_attribute_stats(id) ON DELETE CASCADE
);
```

### `app_settings`
```sql
CREATE TABLE app_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Seed default
INSERT INTO app_settings (key, value) VALUES
    ('epic_finale_bonus_exp',       '1000'),
    ('decay_grace_period_days',     ''),
    ('decay_rate_R',                ''),
    ('failure_threshold_sessions',  '7');
```

---

## 4. Flow Aplikasi

### 4.1 Flowchart Utama — Navigasi Antar Screen

```mermaid
flowchart TD
    START([App Dibuka]) --> EVAL[Lazy Evaluation\nLapis B + Lapis A]
    EVAL --> WELCOME{has_seen_welcome\n= 0?}
    WELCOME -->|Ya| WS[Welcome Screen]
    WELCOME -->|Tidak| TQ[Today's Quests Screen]
    WS -->|Buat Quest Pertama| CQ[Create Quest Screen]
    WS -->|Lewati| TQ
    CQ -->|Selesai| TQ
    TQ -->|FAB +| CQ
    TQ -->|Tab Master Plan| MP[Master Plan Screen]
    TQ -->|Tab Settings| ST[Settings Screen]
    MP -->|Tab Today| TQ
    MP -->|Tab Settings| ST
    ST -->|Tab Today| TQ
    ST -->|Tab Master Plan| MP
```

### 4.2 Flowchart — First Launch

```mermaid
flowchart TD
    A([App pertama dibuka]) --> B[Seed DB:\nattributes, app_settings, user default]
    B --> C[Cek has_seen_welcome]
    C -->|0 - belum pernah| D[Tampilkan Welcome Screen]
    D --> E{User pilih}
    E -->|Buat Quest Pertama| F[Navigasi ke Create Quest]
    E -->|Lewati| G[Navigasi ke Today's Quests]
    F --> H[markWelcomeSeen = 1]
    G --> H
    H --> I[Today's Quests — state kosong]
```

### 4.3 Flowchart — Pembuatan Quest

```mermaid
flowchart TD
    A([User tap FAB +]) --> B{Jenis Quest}
    B -->|Standalone| C[Input: judul, deskripsi, atribut]
    B -->|Epic + Sub-quest| D[Input: judul, deskripsi,\natribut untuk bonus finale]

    C --> E{Completion Mode}
    E -->|Instant| F[Lanjut ke jadwal]
    E -->|Timer| G[Input target durasi]
    G --> F

    F --> H{Schedule Type}
    H -->|Harian| I[Lanjut ke masa aktif]
    H -->|Hari Tertentu| J[Pilih hari dalam seminggu]
    J --> I

    I --> K{Duration Type}
    K -->|Endless| L[Simpan Quest]
    K -->|Time-Bound| M[Input tanggal akhir\natau jumlah hari]
    M --> N{Absence Mode}
    N -->|Shift| L
    N -->|Stack| L

    D --> O{Duration Type Epic}
    O -->|Endless| P[Lanjut buat sub-quest pertama]
    O -->|Time-Bound| Q[Input tanggal akhir\natau jumlah hari]
    Q --> R{Absence Mode Epic}
    R --> P

    P --> S[Input sub-quest:\njudul, atribut, completion mode,\njadwal, masa aktif]
    S --> T{Validasi:\nsub-quest end_date\n<= epic end_date?}
    T -->|Valid| U[insertEpicWithFirstSubQuest\ndalam 1 transaksi]
    T -->|Tidak Valid| V[Tampilkan error per field]
    V --> S
    U --> W([Navigasi ke Today's Quests])
    L --> W
```

### 4.4 Flowchart — Eksekusi Harian (Today's Quests)

```mermaid
flowchart TD
    A([User buka Today's Quests]) --> B[Lazy Evaluation selesai]
    B --> C[Render daftar quest terjadwal hari ini]
    C --> D{User pilih quest}

    D -->|Quest Instant| E[Tap tombol centang ✓]
    E --> F[Insert quest_logs]
    F --> G[Award EXP ke atribut]
    G --> H{Hari ini = end_date\nquest?}
    H -->|Ya| I[Award Epic Finale Bonus\n1000 EXP ke atribut Epic]
    H -->|Tidak| J{Level up?}
    I --> J
    J -->|Ya| K[Tampilkan notifikasi level up]
    J -->|Tidak| L[Quest ditandai selesai ✓\ntetap tampil di list]
    K --> L

    D -->|Quest Timer| M{Ada active_timer?}
    M -->|Tidak ada| N[Tap tombol Mulai]
    M -->|Berjalan - alarm IS NULL| O[Tampilkan progress timer\n+ tombol Batal]
    M -->|Selesai - alarm IS NOT NULL| P[Tampilkan badge\nSelesai — Konfirmasi]

    N --> Q[Insert active_timers]
    Q --> R[Jadwalkan AlarmManager\npada started_at + target_duration]
    R --> S[Start Foreground Service]
    S --> T[Timer berjalan di background]
    T --> U{Alarm bunyi\ntepat waktu}
    U --> V[Update alarm_fired_at\nTampilkan notifikasi sistem]
    V --> P

    P --> W[Tap Konfirmasi]
    W --> F

    O --> X[Tap Batal]
    X --> Y[Delete active_timers\nCancel AlarmManager\nStop Foreground Service]
    Y --> Z[Tidak ada EXP]
```

### 4.5 Flowchart — Lazy Evaluation (ON_APP_OPEN)

```mermaid
flowchart TD
    A([App Dibuka]) --> B

    subgraph LAPIS_B [Lapis B — Selalu Jalan]
        B[Query active_timers\nalarm_fired_at IS NOT NULL] --> C[Render pending confirmation\ntanpa bunyi baru]
        C --> D[Query active_timers\nalarm_fired_at IS NULL]
        D --> E[Render progress timer real-time]
        E --> F[Render Today's Quests]
    end

    F --> G{last_evaluated_date\n< today?}
    G -->|Tidak| Z([Selesai])
    G -->|Ya| H

    subgraph LAPIS_A [Lapis A — Sekali Per Hari]
        H{last_evaluated_date\nIS NULL?} -->|Ya - first time| I[Set last_evaluated_date = today\nSkip evaluasi]
        H -->|Tidak| J[Hitung rentang:\nlast_eval+1 s/d yesterday\npakai julianday]

        J --> K[Per quest aktif:\nHitung consecutive_missed_sessions\npakai schedule_days vs quest_logs\nHari ini DIKECUALIKAN]

        K --> L[Update\nconsecutive_inactive_scheduled_days\ndi tabel users]

        L --> M[Step 3: Cek Cascade Failure\nconsecutive_missed_sessions >= 7]

        M --> N{Ada sub-quest\ngagal?}
        N -->|Ya| O[Arsipkan sub-quest:\nfailed_abandoned]
        O --> P[Arsipkan Epic induk:\nfailed_abandoned]
        P --> Q{Saudara sub-quest\nlainnya?}
        Q -->|status = completed| R[Arsipkan: completed]
        Q -->|status = active| S[Arsipkan: failed_via_parent]
        R --> T[DELETE Epic induk\nCASCADE hapus semua sub-quest]
        S --> T

        N -->|Tidak| U[Cek standalone quest gagal]
        T --> U
        U --> V[Arsipkan + DELETE\nstandalone yang gagal]

        V --> W[Step 4: Proses Absence Mode\nuntuk quest time_bound yang hidup]
        W --> X{absence_mode?}
        X -->|shift| Y[Geser end_date via\nfindNextScheduledDay]
        X -->|stack| AA[Tambah stacked_duration_seconds\nmissed_sessions x target_duration]

        Y --> AB
        AA --> AB

        AB[Step 5: EXP Decay] --> AC{decay_rate_R &\ngrace_period dikonfigurasi?}
        AC -->|Tidak| AD[Skip decay]
        AC -->|Ya| AE{consecutive_inactive\n>= 2?}
        AE -->|Tidak| AD
        AE -->|Ya| AF[exp_to_deduct = R x\ninactive_days - 1\nApply ke semua stats\nFloor di 0, tidak de-level\nInsert exp_decay_log]

        AD --> AG[UPDATE last_evaluated_date = today\nUPDATE last_active_at = now]
        AF --> AG
        I --> AG
    end

    AG --> Z
```

### 4.6 Flowchart — EXP & Level Up

```mermaid
flowchart TD
    A([Quest selesai dikerjakan]) --> B[Hitung EXP yang diberikan]
    B --> C{is_epic_finale_bonus?}
    C -->|Ya| D[EXP = epic_finale_bonus_exp\ndari app_settings = 1000]
    C -->|Tidak| E[EXP = nilai dasar harian\ndari app_settings]
    D --> F[current_exp += EXP]
    E --> F
    F --> G[total_exp_earned_lifetime += EXP]
    G --> H{current_exp >=\n100 x level^1.5?}
    H -->|Tidak| I([Selesai])
    H -->|Ya| J[current_level += 1\ncurrent_exp -= threshold level sebelumnya]
    J --> H
```

### 4.7 Flowchart — EXP Decay

```mermaid
flowchart TD
    A([Lapis A Step 5 dijalankan]) --> B{decay_rate_R\n& grace_period\ndikonfigurasi?}
    B -->|Tidak| Z([Skip decay])
    B -->|Ya| C{consecutive_inactive\n_scheduled_days >= 2?}
    C -->|Tidak| Z
    C -->|Ya| D[hari_decay =\nconsecutive_inactive - 1]
    D --> E[exp_to_deduct = R x hari_decay]
    E --> F[Untuk setiap user_attribute_stats]
    F --> G[new_exp = MAX current_exp - exp_to_deduct, 0.0]
    G --> H{new_exp < 0?}
    H -->|Ya| I[new_exp = 0\ncurrent_level TIDAK BERUBAH]
    H -->|Tidak| J[Update current_exp = new_exp]
    I --> J
    J --> K[Insert exp_decay_log:\nexp_before, exp_after,\ndecay_rate_used, inactive_days]
    K --> L{Masih ada stat\nlainnya?}
    L -->|Ya| F
    L -->|Tidak| Z
```

### 4.8 Flowchart — Cascade Failure

```mermaid
flowchart TD
    A([Step 3 Lapis A]) --> B[Query sub-quest dengan\nconsecutive_missed_sessions >= 7\nyang punya parent]

    B --> C{Ada sub-quest\nyang gagal?}
    C -->|Tidak| G
    C -->|Ya| D[Arsipkan sub-quest ke quest_history\nfinal_status = failed_abandoned]
    D --> E[Arsipkan Epic induk ke quest_history\nfinal_status = failed_abandoned]
    E --> F[Untuk setiap saudara sub-quest lain\ndi bawah Epic yang sama]
    F --> FA{Status\nsaudara?}
    FA -->|completed| FB[Arsipkan: completed]
    FA -->|active| FC[Arsipkan: failed_via_parent]
    FB --> FD[DELETE Epic induk\nCASCADE hapus semua sub-quest]
    FC --> FD

    FD --> G[Query standalone quest dengan\nconsecutive_missed_sessions >= 7]
    G --> H{Ada standalone\nyang gagal?}
    H -->|Tidak| Z([Selesai Step 3])
    H -->|Ya| I[Arsipkan ke quest_history\nfinal_status = failed_abandoned]
    I --> J[DELETE dari quests]
    J --> H
```

### 4.9 Flowchart — Timer Lifecycle

```mermaid
flowchart TD
    A([User tap Mulai]) --> B[Validasi:\ntimer belum ada untuk quest ini]
    B --> C{Timer sudah ada?}
    C -->|Ya| D[Tampilkan error:\nTimer sudah berjalan]
    C -->|Tidak| E[Insert active_timers\nalarm_fired_at = NULL]
    E --> F[Jadwalkan AlarmManager:\nsetExactAndAllowWhileIdle\npada started_at + target_duration]
    F --> G[Start Foreground Service\nSTART_STICKY]
    G --> H[Timer berjalan\nForeground Service aktif]

    H --> I{App ditutup?}
    I -->|Ya| J[Foreground Service tetap jalan\nAlarmManager tetap terjadwal]
    I -->|Tidak| H

    J --> K{Device reboot?}
    K -->|Ya| L[BootReceiver:\nreschedule AlarmManager\ndari active_timers]
    K -->|Tidak| H

    H --> M{Alarm waktu tercapai}
    L --> M
    M --> N[QuestAlarmReceiver:\nUpdate alarm_fired_at = now\nTampilkan notifikasi sistem]

    N --> O{User aksi}
    O -->|Konfirmasi| P[Delete active_timers\nInsert quest_logs\nAward EXP\nStop Service]
    O -->|Batal| Q[Delete active_timers\nCancel AlarmManager\nStop Service\nTidak ada EXP]

    P --> R([Quest selesai])
    Q --> S([Quest dibatalkan])
```

---

## 5. Business Logic

### 5.1 Formula EXP & Level Up

```
targetEXP(L) = 100 × L^1.5

Level up:
  while current_exp >= targetEXP(current_level):
      current_level += 1
      current_exp -= targetEXP(current_level - 1)
```

Implementasi di util/ExpCalculator.kt — pure function, bukan di SQL.
Level tidak ada batas maksimal.

### 5.2 EXP Decay

Trigger: tidak menyelesaikan satu pun quest selama N hari terjadwal berturut-turut.
Hari tanpa quest terjadwal tidak dihitung. Grace period = 1 hari pertama.

```
hari_decay = consecutive_inactive_scheduled_days - 1
exp_to_deduct = R × hari_decay
new_exp = MAX(current_exp - exp_to_deduct, 0.0)
```

Nilai R dibaca dari app_settings['decay_rate_R'].
Decay dikenakan ke semua atribut sekaligus — bukan per atribut.
current_level tidak pernah turun akibat decay.

### 5.3 Bonus EXP Finale

Diberikan saat Epic Quest mencapai end_date dengan sukses.
Besaran: 1000 EXP (dibaca dari app_settings['epic_finale_bonus_exp']).
Masuk ke attribute_id milik Epic induk.
Dicatat di quest_logs dengan is_epic_finale_bonus = 1.

### 5.4 Aturan Gagal Permanen

Quest gagal jika consecutive_missed_sessions >= 7.
"Sesi" = hari terjadwal sesuai schedule_days, bukan hari kalender.
Hari ini selalu dikecualikan dari perhitungan.

Sub-quest gagal → Epic induk ikut gagal otomatis.
Saudara sub-quest yang sudah completed → diarsipkan sebagai completed.
Saudara sub-quest yang masih active → diarsipkan sebagai failed_via_parent.

### 5.5 Mode Shift

end_date digeser sebanyak sesi terlewat dalam unit slot jadwal berikutnya.
Implementasi via util/ScheduleCalculator.findNextScheduledDay().
Bukan geser hari kalender mentah.

### 5.6 Mode Stack

```
stacked_duration_seconds += missed_sessions × target_duration_seconds
effective_target = target_duration_seconds + stacked_duration_seconds
```

Reset ke 0 setelah user konfirmasi selesai.
Maksimal alami 6x (quest gagal di sesi bolos ke-7).

---

## 6. Aturan Validasi

### Quest yang dieksekusi langsung (standalone & sub-quest)

| Field | Aturan |
|---|---|
| title | Wajib, maksimal 100 karakter |
| attribute_id | Wajib dipilih |
| completion_mode | Wajib ('instant' atau 'timer') |
| target_duration_seconds | Wajib > 0 jika completion_mode = 'timer' |
| duration_type | Wajib ('endless' atau 'time_bound') |
| end_date atau target_days | Wajib jika duration_type = 'time_bound' |
| absence_mode | Wajib jika time_bound, NULL jika endless |
| schedule_days | Wajib minimal 1 hari jika schedule_type = 'custom_days' |
| sub-quest end_date | Tidak boleh melebihi parent end_date jika parent time_bound |

### Epic container (is_container = 1)

| Field | Aturan |
|---|---|
| completion_mode | WAJIB NULL |
| schedule_type | WAJIB NULL |
| schedule_days | WAJIB NULL |
| attribute_id | Wajib (untuk bonus EXP finale) |
| Minimal sub-quest | Wajib 1 sub-quest — insert dalam 1 transaksi atomik |

---

## 7. Seed Data

### attributes (default, is_default = 1)

| code | display_name | icon | sort_order |
|---|---|---|---|
| STR | Strength | 💪 | 1 |
| INT | Intelligence | 🧠 | 2 |
| WIS | Wisdom | 🧘 | 3 |
| DEX | Dexterity | ⚡ | 4 |
| VIT | Vitality | ❤️ | 5 |

### app_settings

| key | value |
|---|---|
| epic_finale_bonus_exp | 1000 |
| decay_grace_period_days | (kosong, diisi user) |
| decay_rate_R | (kosong, diisi user) |
| failure_threshold_sessions | 7 |

---

## 8. Screen & Navigasi

### Struktur Navigasi

```
Bottom Navigation:
├── Today's Quests (default)
├── Master Plan
└── Settings

FAB (+) di Today's Quests → Create Quest Screen
```

### Today's Quests Screen

Menampilkan semua quest yang terjadwal hari ini (sesuai schedule_days), status active.
Quest selesai tetap tampil dengan tanda selesai — tidak hilang sampai hari berganti.
Loading state, error state, dan empty state wajib ada.

### Create Quest Screen

Form kondisional — field muncul/hilang sesuai pilihan user.
Untuk Epic container: form dilanjutkan untuk input sub-quest pertama
sebelum bisa disimpan (dalam 1 transaksi).

### Master Plan Screen — 3 Tab

**Tab Karakter:**
Semua atribut dengan level, EXP saat ini, progress bar ke level berikutnya.
Total EXP lifetime, streak nonaktif saat ini.

**Tab Quest Aktif:**
Epic container dengan expansion panel untuk lihat sub-quest.
Standalone quest dalam list terpisah.

**Tab Riwayat:**
List dari quest_history ORDER BY ended_at DESC.
Badge warna berbeda: completed (hijau), failed_abandoned (merah), failed_via_parent (oranye).

### Settings Screen

Konfigurasi decay (R dan grace period).
Manajemen atribut (tambah, edit, hapus custom — default tidak bisa dihapus).

