# SKILL.md — ui-design-qa

## Kapan Skill Ini Di-load

Load skill ini setiap kali task menyentuh:
- Pembuatan atau modifikasi Composable screen manapun
- Pembuatan atau modifikasi reusable component
- Task yang menghasilkan UI yang bisa dilihat user

---

## 1. Aturan Text & Typography

### Text overflow — WAJIB eksplisit

Setiap Text Composable yang menampilkan dynamic content (dari DB, input user, atau variabel)
WAJIB memiliki salah satu dari:

```kotlin
// Opsi 1: potong dengan ellipsis
Text(
    text = quest.title,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
)

// Opsi 2: wrap ke baris berikutnya dengan batas maksimal
Text(
    text = quest.description ?: "",
    maxLines = 3,
    overflow = TextOverflow.Ellipsis
)

// SALAH — tidak ada constraint, bisa overflow keluar container
Text(text = quest.title)
```

Text yang BOLEH tanpa maxLines: label statis yang panjangnya sudah pasti
(misal label button "Simpan", "Batal", header tab yang fixed).

### Ukuran font

WAJIB menggunakan TextStyle dari Material3 Typography — jangan hardcode sp:

```kotlin
// BENAR
Text(text = "Judul", style = MaterialTheme.typography.titleMedium)
Text(text = "Deskripsi", style = MaterialTheme.typography.bodyMedium)

// SALAH
Text(text = "Judul", fontSize = 18.sp)
```

---

## 2. Aturan Layout & Container

### Padding — konsisten dan simetris

WAJIB menggunakan design token dari Theme — jangan hardcode angka dp:

```kotlin
// Buat di ui/theme/Spacing.kt
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

// Penggunaan
Box(modifier = Modifier.padding(Spacing.md))
```

Padding WAJIB simetris kecuali ada alasan desain eksplisit yang disebutkan di task:
```kotlin
// BENAR — simetris
Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)

// PERLU ALASAN — asimetris
Modifier.padding(start = Spacing.md, end = Spacing.sm, top = Spacing.lg, bottom = Spacing.xs)
```

### Constraint — WAJIB eksplisit untuk container dynamic content

Setiap container yang menampung konten dinamis WAJIB punya constraint width yang jelas:

```kotlin
// BENAR
Column(modifier = Modifier.fillMaxWidth()) { ... }
Card(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) { ... }

// SALAH — lebar tidak terdefinisi, bisa collapse atau overflow
Column { ... }
Card { ... }
```

### Alignment & Arrangement — WAJIB eksplisit

```kotlin
// BENAR — tidak mengandalkan default
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) { ... }

// SALAH — default Compose tidak selalu predictable di berbagai ukuran layar
Row { ... }
```

---

## 3. Aturan List & Scroll

### LazyColumn untuk list yang bisa panjang

Gunakan LazyColumn (bukan Column dengan forEach) untuk list yang itemnya
bisa lebih dari 5 — misal daftar quest, riwayat quest, daftar atribut:

```kotlin
// BENAR untuk list panjang
LazyColumn {
    items(quests, key = { it.id }) { quest ->
        QuestCard(quest = quest)
    }
}

// SALAH untuk list panjang — render semua sekaligus, bisa lag
Column {
    quests.forEach { quest ->
        QuestCard(quest = quest)
    }
}
```

WAJIB sertakan key = { it.id } di setiap items() untuk optimasi recomposition.

### Empty state — WAJIB ada

Setiap screen yang menampilkan list WAJIB punya empty state yang informatif:

```kotlin
if (quests.isEmpty()) {
    EmptyStateComponent(
        message = "Belum ada quest hari ini",
        actionLabel = "Buat Quest",
        onAction = { onNavigateToCreateQuest() }
    )
} else {
    LazyColumn { ... }
}
```

Tidak boleh menampilkan layar kosong polos tanpa pesan apapun.

---

## 4. Aturan State UI

### Loading state — WAJIB ada untuk setiap screen yang fetch data

```kotlin
when (val state = uiState) {
    is UiState.Loading -> LoadingComponent()
    is UiState.Success -> ContentComponent(data = state.data)
    is UiState.Error -> ErrorComponent(message = state.message)
}
```

Tidak boleh langsung render content tanpa handle loading dan error state.

### Disabled state untuk tombol

Tombol yang actionnya tidak valid dalam kondisi tertentu WAJIB di-disable,
bukan disembunyikan:

```kotlin
// Contoh: tombol Start timer disabled jika timer lain sudah aktif
Button(
    onClick = { viewModel.startTimer(questId) },
    enabled = uiState.canStartTimer
) {
    Text("Mulai Timer")
}
```

---

## 5. Behavioral Checklist vs PRD

Setiap task UI yang selesai WAJIB menyertakan checklist verifikasi behavior
berdasarkan PRD sebagai bagian dari artifact hasil. Agent mengisi checklist ini
sendiri berdasarkan implementasi yang dilakukan.

### Today's Quests screen

```
- [ ] Hanya menampilkan quest yang terjadwal hari ini (sesuai schedule_days)
- [ ] Quest selesai tetap terlihat dengan tanda selesai (strikethrough atau badge ✓)
- [ ] Quest selesai tidak hilang dari list sampai hari berganti
- [ ] Empty state muncul jika tidak ada quest terjadwal hari ini
- [ ] Loading state muncul saat data sedang di-fetch
- [ ] Quest dengan timer menampilkan tombol Start, bukan centang langsung
- [ ] Quest instant menampilkan tombol centang langsung
- [ ] Timer yang sedang berjalan menampilkan progress real-time
- [ ] Timer yang alarm_fired_at IS NOT NULL menampilkan badge "Selesai — Konfirmasi"
      tanpa bunyi atau notifikasi baru
```

### Form pembuatan quest

```
- [ ] Semua field validasi menampilkan error per-field (bukan satu toast generik)
- [ ] Field kondisional muncul/hilang sesuai pilihan user
      (misal: input durasi muncul hanya jika Timer dipilih)
- [ ] Field absence_mode hanya muncul jika Time-Bound dipilih
- [ ] Custom days picker hanya muncul jika custom_days dipilih
- [ ] Tombol Simpan disabled jika ada field wajib yang belum diisi
- [ ] Sub-quest end_date tidak bisa dipilih melebihi parent end_date
```

### Master Plan / Log screen

```
- [ ] Tab Karakter menampilkan semua atribut dengan level dan progress bar EXP
- [ ] Progress bar EXP akurat sesuai formula 100 * L^1.5
- [ ] Tab Quest Aktif menampilkan Epic container dengan sub-quest bisa di-expand
- [ ] Tab Riwayat menampilkan badge status dengan warna berbeda per final_status
- [ ] Semua tab punya empty state jika data kosong
```

### Settings screen

```
- [ ] Nilai R dan grace period menampilkan placeholder jika belum diisi
- [ ] Input nilai R dan grace period hanya menerima angka positif
- [ ] Daftar atribut menampilkan atribut default dan custom
- [ ] Atribut bisa ditambah dan diedit
- [ ] Perubahan settings tersimpan ke app_settings di DB, bukan hanya in-memory
```

---

## 6. Checklist Visual Sebelum Task UI Selesai

Agent WAJIB memverifikasi semua poin ini sebelum melaporkan task selesai:

- [ ] Tidak ada Text tanpa maxLines/overflow untuk dynamic content
- [ ] Semua font menggunakan MaterialTheme.typography, tidak ada hardcode sp
- [ ] Semua padding menggunakan Spacing token, tidak ada hardcode dp
- [ ] Semua container dynamic content punya fillMaxWidth atau constraint eksplisit
- [ ] Semua Row/Column punya Arrangement dan Alignment yang eksplisit
- [ ] List panjang menggunakan LazyColumn dengan key
- [ ] Setiap list punya empty state yang informatif
- [ ] Setiap screen punya Loading dan Error state
- [ ] Tombol yang tidak valid di-disable, bukan disembunyikan
- [ ] Behavioral checklist PRD sudah diisi dan dilampirkan di artifact hasil

