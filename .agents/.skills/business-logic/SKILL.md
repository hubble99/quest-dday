# SKILL.md — business-logic

## Kapan Skill Ini Di-load

Load skill ini setiap kali task menyentuh:
- Kalkulasi EXP dan level up
- Logika decay
- Logika cascade failure
- Mode Shift dan Stack
- Lazy evaluation engine (Lapis A dan Lapis B)
- Apapun yang berhubungan dengan user_attribute_stats, exp_decay_log, quest_logs

---

## 1. Formula EXP & Level Up

### Threshold EXP per level

```
targetEXP(L) = 100 * L^1.5
```

Contoh:
- Level 1 → butuh 100 EXP untuk naik ke level 2
- Level 2 → butuh 283 EXP
- Level 5 → butuh 1118 EXP
- Level 10 → butuh 3162 EXP

Formula ini WAJIB diimplementasikan di application layer (Kotlin), BUKAN di SQL.
Letakkan di util/ExpCalculator.kt sebagai pure function tanpa Android dependency.

### Proses level up

```
if (current_exp >= targetEXP(current_level)):
    current_level += 1
    current_exp -= targetEXP(current_level - 1)
    cek lagi apakah current_exp >= targetEXP(current_level) (bisa naik lebih dari 1 level sekaligus)
```

Level up tidak ada batas maksimal. Loop sampai current_exp < targetEXP(current_level).

### Sumber EXP

EXP diberikan setiap kali quest_logs di-insert (quest selesai dikerjakan).
Bonus EXP finale diberikan saat Epic Quest container mencapai end_date dengan sukses
— bonus masuk ke attribute_id milik Epic induk, dicatat dengan is_epic_finale_bonus = 1.
Besaran EXP dasar harian belum ditetapkan — baca dari app_settings.
Bonus EXP finale = 1000 EXP (flat), dibaca dari app_settings["epic_finale_bonus_exp"].
Nilai default di-seed ke 1000 saat database pertama dibuat.
Dirancang untuk bisa dikembangkan ke sistem difficulty (easy/medium/hard) di masa depan
— masing-masing difficulty bisa punya nilai bonus yang berbeda di app_settings.

---

## 2. EXP Decay

### Trigger decay

Decay BUKAN berbasis per-atribut dan BUKAN berbasis kapan terakhir buka app.
Decay dipicu oleh: tidak menyelesaikan satu pun quest apapun selama N hari terjadwal berturut-turut.

"Hari terjadwal" = hari kalender di mana minimal ada satu quest aktif yang terjadwal (berdasarkan schedule_days).
Hari tanpa quest terjadwal sama sekali TIDAK dihitung sebagai hari nonaktif.

### Grace period

Hari nonaktif ke-1 = grace period, belum kena decay.
Decay mulai dihitung dari hari nonaktif ke-2.

```
if (consecutive_inactive_scheduled_days >= 2):
    hari_decay = consecutive_inactive_scheduled_days - 1
    R = baca dari app_settings['decay_rate_R']
    exp_to_deduct = R * hari_decay
```

### Eksekusi decay

Decay dikenakan ke SEMUA user_attribute_stats secara bersamaan (bukan per-atribut).

```
new_exp = MAX(current_exp - exp_to_deduct, 0.0)
```

Proteksi de-level: current_exp TIDAK BOLEH turun ke bawah 0.0 pada level yang sedang dipijak.
current_level TIDAK BOLEH berkurang akibat decay dalam kondisi apapun.

Setiap eksekusi decay WAJIB dicatat ke exp_decay_log:
- exp_before
- exp_after
- decay_rate_used (nilai R yang dipakai saat itu, bukan nilai default)
- inactive_days (nilai consecutive_inactive_scheduled_days saat decay dijalankan)

### Update consecutive_inactive_scheduled_days

Tracking dilakukan di kolom users.consecutive_inactive_scheduled_days.
Reset ke 0 setiap kali ada minimal 1 quest_logs di-insert pada hari terjadwal manapun.
Increment hanya pada hari terjadwal yang tidak punya satu pun quest_logs.

---

## 3. Cascade Failure

### Trigger

Quest dinyatakan gagal permanen jika consecutive_missed_sessions >= 7.
"Sesi" = hari terjadwal sesuai schedule_days, BUKAN hari kalender mentah.
Hari ini (today) DIKECUALIKAN dari perhitungan missed — evaluasi hanya untuk hari sebelum today.

### Urutan eksekusi cascade (WAJIB diikuti, tidak boleh dibalik)

```
Step 1: Identifikasi sub-quest yang gagal (consecutive_missed_sessions >= 7, punya parent)
Step 2: Untuk setiap sub-quest gagal yang punya parent:
    a. Arsipkan sub-quest ke quest_history (final_status = 'failed_abandoned')
    b. Arsipkan Epic induk ke quest_history (final_status = 'failed_abandoned')
    c. Untuk setiap saudara (sub-quest lain di bawah Epic yang sama):
       - Jika status = 'completed' → arsipkan dengan final_status = 'completed'
       - Jika status = 'active'    → arsipkan dengan final_status = 'failed_via_parent'
    d. DELETE Epic induk (CASCADE otomatis hapus semua sub-quest via FK)
Step 3: Identifikasi standalone quest yang gagal (consecutive_missed_sessions >= 7, parent IS NULL)
Step 4: Arsipkan ke quest_history (final_status = 'failed_abandoned'), DELETE dari quests
```

Step 1 dan 2 WAJIB selesai sebelum Step 3 dimulai.
Semua operasi di atas WAJIB dalam satu @Transaction.

### final_status yang valid

- 'completed' — quest selesai dengan sukses (mencapai goal/end_date)
- 'failed_abandoned' — gagal karena bolos 7 sesi berturut-turut
- 'failed_via_parent' — masih aktif tapi terseret gagal karena Epic induknya gagal

---

## 4. Mode Shift

Hanya berlaku untuk quest dengan duration_type = 'time_bound'.
Quest dengan duration_type = 'endless' TIDAK punya absence_mode.

### Logika

Saat ada sesi terjadwal yang terlewat, end_date digeser sebanyak jumlah sesi terlewat
dalam unit "slot jadwal berikutnya" — BUKAN hari kalender mentah.

```
Untuk setiap sesi terlewat:
    cari hari berikutnya setelah end_date yang termasuk dalam schedule_days
    geser end_date ke hari tersebut
```

Implementasi via fungsi: findNextScheduledDay(scheduleDays: List<Int>, fromDate: LocalDate): LocalDate
Letakkan di util/ScheduleCalculator.kt sebagai pure function.

Fungsi ini juga dipakai untuk render kalender di UI — implementasi sekali, pakai di banyak tempat.

---

## 5. Mode Stack

Hanya berlaku untuk quest dengan duration_type = 'time_bound'.

### Logika

Setiap sesi terlewat menambah beban durasi ke stacked_duration_seconds:

```
stacked_duration_seconds += missed_sessions * target_duration_seconds
```

Saat quest dikerjakan, durasi efektif yang harus diselesaikan:

```
effective_target = target_duration_seconds + stacked_duration_seconds
```

Setelah user konfirmasi selesai, reset:

```
stacked_duration_seconds = 0
```

Tidak ada batas maksimal tumpukan. Maksimal alami adalah 6x (bolos 6 sesi,
sesi ke-7 quest langsung failed dan dihapus sebelum sempat dikerjakan).

---

## 6. Lazy Evaluation — Urutan Wajib

Urutan ini TIDAK BOLEH dibalik. Setiap step bergantung pada hasil step sebelumnya.

### Lapis B (jalan setiap app dibuka, tanpa syarat)

```
1. Query active_timers WHERE alarm_fired_at IS NOT NULL
   → render sebagai "pending confirmation" di UI, TANPA bunyi/notifikasi baru
2. Query active_timers WHERE alarm_fired_at IS NULL
   → render progress timer real-time (hitung sisa waktu dari started_at)
3. Render Today's Quests
```

### Lapis A (jalan hanya jika last_evaluated_date < today)

```
Guard: IF users.last_evaluated_date == today → skip seluruh Lapis A

1. Hitung rentang: last_evaluated_date+1 s/d yesterday
   Gunakan julianday() untuk hitung selisih, TANPA loop eksplisit per hari kalender
   IF last_evaluated_date IS NULL → set last_evaluated_date = today, skip step berikutnya

2. Per quest aktif (is_container = 0):
   - Hitung sesi terjadwal di rentang yang tidak punya quest_logs
   - Update consecutive_missed_sessions
   - Hari today DIKECUALIKAN dari perhitungan
   - Paralel: tracking hari terjadwal tanpa satu pun quest_logs
     → increment users.consecutive_inactive_scheduled_days
     → reset ke 0 jika ada minimal 1 quest_logs di hari itu

3. Cascade failure (lihat section 3 di atas, urutan wajib diikuti)

4. Proses absence_mode untuk quest time_bound yang masih hidup:
   - Shift → geser end_date via findNextScheduledDay()
   - Stack → update stacked_duration_seconds

5. EXP Decay (lihat section 2 di atas)

6. UPDATE users.last_evaluated_date = today
   UPDATE users.last_active_at = now
```

