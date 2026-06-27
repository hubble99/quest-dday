# FLOW.md — Panduan Membangun Quest DDay

Dokumen ini adalah panduan operasional pengembangan aplikasi Quest DDay
menggunakan Antigravity IDE sebagai eksekutor dan Claude sebagai arsitek.
Baca dokumen ini sebelum memulai sesi pengembangan apapun.

---

## Prinsip Dasar

- Satu task = satu sesi Antigravity = satu commit Git
- Anda sebagai arsitek: menentukan scope, mereview, menyetujui
- Agent sebagai eksekutor: menulis kode, menulis test, melaporkan hasil
- Tidak ada task yang dimulai sebelum task sebelumnya selesai, diverifikasi, dan di-commit

---

## Struktur Folder Dokumen di Root Project

```
quest-dday/
├── AGENTS.md                          ← dibaca agent otomatis setiap sesi
├── PRD.md                             ← referensi business logic & schema
├── TASKS.md                           ← task board, sumber kebenaran pengerjaan
├── FLOW.md                            ← dokumen ini
└── .agents/
    └── skills/
        ├── business-logic/
        │   └── SKILL.md
        ├── database-conventions/
        │   └── SKILL.md
        ├── testing-standards/
        │   └── SKILL.md
        ├── android-lifecycle/
        │   └── SKILL.md
        ├── validation-rules/
        │   └── SKILL.md
        └── ui-design-qa/
            └── SKILL.md
```

---

## Fase 0 — Persiapan Awal (Sekali Saja, Manual)

Dikerjakan Anda sendiri tanpa agent sebelum sesi pertama.

1. Buat project Android baru di Android Studio:
   Kotlin + Jetpack Compose + Room, minimum SDK 26, package name com.questdday

2. Buka folder project di Antigravity

3. Buat semua file dokumen di atas — copy paste dari file yang sudah disiapkan:
   - AGENTS.md → root project
   - PRD.md → root project
   - TASKS.md → root project
   - FLOW.md → root project
   - Semua SKILL.md → masing-masing ke folder .agents/skills/[nama-skill]/

4. Inisialisasi Git dan buat initial commit dengan semua file dokumen
   sebelum agent menyentuh apapun:
   ```
   git init
   git add .
   git commit -m "chore: initial project setup & documentation"
   ```

5. Verifikasi project bisa build tanpa error sebelum lanjut ke Fase 1

---

## Fase 1 — Iterasi Per Task

Diulang untuk setiap task di TASKS.md.

### Step 1 — Persiapan Sebelum Buka Antigravity

Buka TASKS.md, identifikasi task berikutnya yang:
- Statusnya TODO
- Semua task di kolom Dependensi sudah berstatus DONE

Baca scope, acceptance criteria, dan unit test task itu.
Pastikan Anda sendiri paham apa yang harus dihasilkan sebelum menyerahkan ke agent.
Kalau scope-nya masih ambigu, diskusikan dengan Claude dulu — bukan dengan agent.

### Step 2 — Buka Sesi Baru di Manager View

Buka Antigravity, pilih Manager View, spawn agent baru.
Jangan gunakan sesi lama yang masih punya history task sebelumnya
kecuali task ini adalah revisi langsung dari task sebelumnya.

### Step 3 — Tulis Prompt Pembuka

Format prompt yang konsisten untuk setiap task:

```
Kerjakan TASKS.md > [Epic X] > [Task X.Y]: [judul task].

Sebelum mulai:
- Baca AGENTS.md
- Load skill: [nama skill yang relevan]
- Baca bagian PRD.md section [sebutkan section spesifik]

Hasilkan Implementation Plan sebagai artifact terlebih dahulu.
Jangan tulis kode apapun sebelum saya approve plan-nya.
```

Contoh nyata untuk Task 2.1:

```
Kerjakan TASKS.md > Epic 2 > Task 2.1: Entity & DAO untuk tabel users,
attributes, dan user_attribute_stats.

Sebelum mulai:
- Baca AGENTS.md
- Load skill: database-conventions, testing-standards
- Baca PRD.md section 2 untuk tabel users, attributes, user_attribute_stats

Hasilkan Implementation Plan sebagai artifact terlebih dahulu.
Jangan tulis kode apapun sebelum saya approve plan-nya.
```

### Step 4 — Review Implementation Plan Artifact

Agent menghasilkan Implementation Plan berisi:
- File apa saja yang akan dibuat atau diubah
- Struktur kode yang direncanakan
- Pendekatan testing

Baca dengan teliti. Bandingkan dengan:
- Acceptance criteria di TASKS.md
- Schema dan business logic di PRD.md
- Aturan di AGENTS.md dan skill yang relevan

Kalau ada yang tidak sesuai: komentar langsung di artifact (bukan ketik ulang
di chat), spesifik dan actionable.

Contoh komentar yang baik:
- "Section 3: query ini harus pakai @Query raw SQL, bukan auto-generated method"
- "Section 5: test case untuk NULL parent_quest_id belum ada, tambahkan"
- "Section 2: transaksi belum dibungkus @Transaction, padahal menyentuh 2 tabel"

Kalau plan sudah sesuai: klik Proceed.

### Step 5 — Agent Eksekusi

Agent menulis kode, menjalankan unit test via Android CLI, mengambil hasil test.
Tidak perlu intervensi kecuali agent stuck atau minta konfirmasi eksplisit.
Biarkan agent selesai sepenuhnya sebelum review.

### Step 6 — Review Artifact Hasil

Agent menghasilkan artifact berisi:
- Diff kode yang ditulis
- Hasil unit test (pass/fail)
- Checklist acceptance criteria dari TASKS.md
- Untuk task UI: checklist ui-design-qa

Review tiga hal ini secara berurutan:

**Pertama — hasil unit test:**
Kalau ada test yang fail, jangan lanjut.
Beri komentar di artifact: "test X fail, perbaiki sebelum lanjut."
Agent perbaiki, jalankan ulang test, hasilkan artifact baru.

**Kedua — checklist acceptance criteria:**
Centang satu per satu dari TASKS.md.
Kalau ada yang tidak terceklis, beri komentar spesifik di artifact.

**Ketiga — untuk task UI:**
Baca checklist ui-design-qa.
Kalau ada item visual yang tidak bisa diverifikasi dari artifact,
jalankan app di emulator dan verifikasi manual.

### Step 7 — Approve atau Reject

**Kalau semua item sudah oke:**
Approve task. Update status task di TASKS.md dari TODO ke DONE.
Bisa dilakukan manual atau minta agent update sekalian sebagai bagian task.

**Kalau masih ada masalah setelah dua kali revisi:**
Reject. Tutup sesi. Evaluasi apakah scope task terlalu besar dan perlu dipecah.
Buat sesi baru dengan scope yang sudah diperbaiki.

### Step 8 — Commit

```
git add .
git commit -m "feat: [Epic X][Task X.Y] judul task"
```

Satu task satu commit. Jangan gabung dua task dalam satu commit
meski task-nya kecil — memudahkan rollback kalau task berikutnya bermasalah.

---

## Fase 2 — Checkpoint Per Epic

Setelah semua task dalam satu Epic selesai dan di-commit,
lakukan checkpoint sebelum masuk Epic berikutnya.

1. Jalankan full build — pastikan tidak ada compile error
2. Jalankan semua unit test sekaligus — pastikan tidak ada regresi
3. Untuk Epic yang menghasilkan UI: jalankan app di emulator,
   verifikasi semua screen yang dihasilkan Epic ini secara end-to-end manual
4. Kalau ada yang fail: buat sesi baru khusus perbaikan,
   jangan campur dengan Epic berikutnya
5. Kalau semua oke:
   ```
   git commit -m "chore: [Epic X] checkpoint — all tests passing"
   ```

---

## Fase 3 — Aturan Situasional

### Kalau agent salah arah di tengah eksekusi

Jangan biarkan agent terus. Stop eksekusi, beri komentar di artifact
yang sedang berjalan, jelaskan apa yang salah secara spesifik.

Kalau agent sudah terlanjur mengubah banyak file ke arah yang salah:
```
git revert HEAD
```
Tutup sesi, buat sesi baru dengan prompt yang lebih spesifik
di bagian yang salah tadi.

### Kalau task ternyata terlalu besar

Tandanya:
- Implementation Plan artifact terlalu panjang (lebih dari 5-6 file sekaligus)
- Agent menyentuh layer yang tidak seharusnya di task ini

Tutup sesi sebelum agent eksekusi. Hubungi Claude untuk diskusi
pemecahan task menjadi dua task yang lebih kecil. Claude yang tulis
spesifikasi task barunya, lalu Anda minta agent untuk insert ke TASKS.md.

### Kalau ada bug ditemukan di task yang sudah DONE

Jangan perbaiki di sesi task yang sedang berjalan.
Catat dulu ke Claude untuk dibuatkan spesifikasi task bugfix baru.
Format commit untuk bugfix: `fix: [Epic X][Task X.Y-bugfix] deskripsi bug`
Selesaikan task yang sedang berjalan dulu, commit, baru kerjakan bugfix.

### Kalau rate limit Antigravity habis di tengah task

Jangan lanjutkan di sesi yang terputus — context-nya sudah tidak bersih.
Simpan artifact yang sudah dihasilkan agent (copy ke file lokal).
Jalankan:
```
git stash
```
Buat sesi baru saat limit sudah refresh. Prompt pembuka:
```
Kerjakan TASKS.md > [Epic X] > [Task X.Y]: [judul task].
Task ini sudah dimulai sebelumnya. File yang sudah dihasilkan: [sebutkan].
Lanjutkan dari bagian [sebutkan]. Baca AGENTS.md dan load skill [nama skill] sebelum mulai.
Hasilkan Implementation Plan artifact dulu sebelum menulis kode.
```

### Kalau perlu menambah task baru di tengah project

1. Diskusikan scope dan detail task baru dengan Claude (bukan dengan agent)
2. Claude tulis spesifikasi task dalam format TASKS.md
3. Anda ke Antigravity dengan prompt:
```
Tambahkan task berikut ke TASKS.md di antara Task [X.Y] dan [X.Z].
Geser nomor task yang terdampak dan update semua referensi dependensi yang berubah.
Spesifikasi task baru:
[paste spesifikasi dari Claude]
```

Catatan: task yang sudah DONE jangan digeser nomornya.
Kalau perlu insert di antara task DONE, gunakan suffix — misal Task 5.1a.

---

## Ringkasan Satu Halaman

```
FASE 0 (sekali):
  Buat project Android → copy semua dokumen → initial commit

PER TASK:
  1. Baca TASKS.md → pilih task berikutnya (TODO + dependensi DONE)
  2. Buka sesi baru di Manager View
  3. Tulis prompt → sebutkan task, skill, section PRD yang relevan
  4. Review Implementation Plan artifact → komentar jika ada yang salah
  5. Proceed → agent eksekusi + test
  6. Review artifact hasil:
     → test pass? acceptance criteria terpenuhi? UI ok?
  7. Approve → update TASKS.md → commit Git
  8. Ulang untuk task berikutnya

PER EPIC SELESAI:
  → Full build + full test + verifikasi manual emulator → checkpoint commit

TAMBAH TASK BARU:
  → Diskusi scope dengan Claude → Claude tulis spesifikasi
  → Agent insert ke TASKS.md + geser nomor
```

---

## Kolaborasi Claude vs Antigravity

| Keputusan / Pekerjaan | Siapa |
|---|---|
| Diskusi scope task baru | Claude |
| Tulis spesifikasi task baru | Claude |
| Review & koreksi AGENTS.md / SKILL.md | Claude |
| Tulis kode per task | Antigravity Agent |
| Tulis unit test per task | Antigravity Agent |
| Insert task baru ke TASKS.md | Antigravity Agent |
| Update status TASKS.md (TODO→DONE) | Antigravity Agent / Anda |
| Review Implementation Plan | Anda |
| Review artifact hasil | Anda |
| Commit Git | Anda |
| Verifikasi manual di emulator | Anda |

