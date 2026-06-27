# AGENTS.md — Quest DDay

Baca file ini sepenuhnya sebelum mengerjakan task apapun.
Semua aturan di sini bersifat WAJIB dan tidak boleh dilanggar tanpa persetujuan eksplisit dari pemilik project.

---

## Identitas Project

- Nama: Quest DDay
- Platform: Android only
- Bahasa: Kotlin
- UI: Jetpack Compose
- Database: Room (raw SQL via @Query)
- Navigasi: Navigation Compose
- Concurrency: Kotlin Coroutines + Flow
- State management: ViewModel + StateFlow
- Timer: Foreground Service (native Android)
- Notifikasi terjadwal: AlarmManager + BroadcastReceiver
- Referensi lengkap: PRD.md

---

## Arsitektur — MVVM + Repository

Struktur package wajib diikuti tanpa pengecualian:

```
com.questdday/
├── data/
│   ├── local/
│   │   ├── dao/          ← semua Room DAO
│   │   ├── entity/       ← semua Room Entity
│   │   └── AppDatabase.kt
│   └── repository/       ← semua Repository (interface + impl)
├── domain/
│   └── model/            ← data class murni (bukan Room entity)
├── ui/
│   ├── screen/           ← satu folder per screen
│   │   ├── today/
│   │   ├── masterplan/
│   │   └── ...
│   ├── component/        ← Composable reusable
│   └── theme/
├── service/              ← Foreground Service
├── receiver/             ← BroadcastReceiver
└── util/                 ← helper function murni (tidak ada Android dependency)
```

Alur data wajib satu arah:
UI → ViewModel → Repository → DAO → Database
Database → DAO → Repository → ViewModel (via Flow) → UI

ViewModel tidak boleh mengakses DAO langsung. Repository tidak boleh mengakses ViewModel. UI tidak boleh mengakses Repository atau DAO langsung.

---

## Aturan Database — WAJIB

1. SEMUA query database wajib ditulis sebagai raw SQL menggunakan @Query annotation.
2. DILARANG menggunakan auto-generated Room method (@Insert tanpa @Query adalah pengecualian yang diizinkan hanya untuk single-row insert sederhana, itupun harus eksplisit dengan OnConflictStrategy).
3. DILARANG menggunakan ORM abstraction apapun selain Room.
4. Semua operasi yang melibatkan lebih dari satu tabel WAJIB dibungkus dalam @Transaction.
5. Semua nilai parameter query WAJIB menggunakan named parameter (:param), tidak boleh string concatenation.
6. Nilai R decay, grace period, dan failure threshold WAJIB dibaca dari tabel app_settings, tidak boleh hardcode di kode.

---

## Aturan Testing — WAJIB

1. Setiap public function di Repository wajib punya minimal satu unit test.
2. Setiap public function di ViewModel yang mengandung business logic wajib punya minimal satu unit test.
3. Setiap unit test wajib menggunakan struktur Arrange-Act-Assert dengan komentar eksplisit.
4. Test untuk business logic wajib mencakup: happy path, edge case boundary value, dan input tidak valid.
5. Hasil test wajib dilaporkan dalam artifact sebelum task dianggap selesai.
6. Task tidak boleh di-approve kalau ada test yang fail.

---

## Aturan Umum

1. Jangan ubah file di luar scope task yang sedang dikerjakan.
2. Jangan tambah dependency baru tanpa menyebutkannya di Implementation Plan artifact dan mendapat persetujuan.
3. Jangan buat abstraction layer tambahan yang tidak ada di struktur package di atas.
4. Setiap task wajib menghasilkan Implementation Plan artifact sebelum menulis kode apapun.
5. Setiap task wajib menghasilkan artifact hasil berisi: diff, hasil test, dan checklist acceptance criteria dari TASKS.md.
6. Kalau ada ambiguitas antara AGENTS.md dan TASKS.md, tanyakan sebelum eksekusi — jangan asumsikan sendiri.
7. Commit message mengikuti format: feat/fix/chore: [Epic X][Task X.Y] judul task

---

## Larangan Eksplisit

- DILARANG menggunakan Prisma, Hibernate, Room auto-generate, atau ORM apapun selain @Query raw SQL
- DILARANG hardcode nilai konfigurasi (R decay, grace period, failure threshold)
- DILARANG mengakses database langsung dari ViewModel atau UI
- DILARANG membuat file di luar struktur package yang sudah ditetapkan tanpa persetujuan
- DILARANG mengerjakan lebih dari satu task dalam satu sesi
- DILARANG skip penulisan unit test dengan alasan apapun
