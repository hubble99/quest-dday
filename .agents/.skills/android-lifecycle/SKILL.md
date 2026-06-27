# SKILL.md — android-lifecycle

## Kapan Skill Ini Di-load

Load skill ini setiap kali task menyentuh:
- Foreground Service (timer quest)
- AlarmManager (alarm timer, notifikasi terjadwal)
- BroadcastReceiver
- Compose lifecycle (LaunchedEffect, DisposableEffect, SideEffect)
- ViewModel lifecycle
- App startup (lazy evaluation on open)

---

## 1. Foreground Service — Timer Quest

### Kapan digunakan

Foreground Service WAJIB digunakan untuk menjalankan timer quest agar tetap
berjalan akurat saat layar dikunci atau app di-background/ditutup total.

### Implementasi wajib

```kotlin
class QuestTimerService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Selalu baca state dari active_timers di DB — jangan andalkan in-memory state
        // karena proses bisa di-kill OS kapan saja
        val questId = intent?.getLongExtra("quest_id", -1L) ?: return START_NOT_STICKY

        startForeground(NOTIFICATION_ID, buildNotification())
        restoreOrStartTimer(questId)

        return START_STICKY  // WAJIB — OS akan restart service jika di-kill
    }

    private fun restoreOrStartTimer(questId: Long) {
        // Baca started_at dan target_duration_seconds dari active_timers
        // Hitung sisa waktu: remaining = target - (now - started_at)
        // Jika remaining <= 0: timer sudah selesai saat service restart → fire alarm segera
        // Jika remaining > 0: jadwalkan alarm untuk sisa waktu
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

### Aturan Foreground Service

1. Service WAJIB return START_STICKY — jangan gunakan START_NOT_STICKY untuk timer.
2. State timer WAJIB disimpan di tabel active_timers (source of truth), bukan hanya DataStore atau in-memory.
3. Saat onStartCommand dipanggil ulang (service restart oleh OS), WAJIB baca ulang state dari DB.
4. Jika saat restart ditemukan remaining <= 0: fire alarm segera, jangan tunggu.
5. Foreground notification WAJIB ditampilkan segera di onStartCommand sebelum operasi lain.
6. Service WAJIB dihentikan (stopSelf()) setelah timer selesai dan alarm di-fire, atau setelah user cancel.

### Stop service

```kotlin
// Saat user konfirmasi selesai atau cancel:
val intent = Intent(context, QuestTimerService::class.java)
context.stopService(intent)
// Lalu hapus baris dari active_timers
```

---

## 2. AlarmManager — Alarm Timer Quest

### Kapan digunakan

AlarmManager dijadwalkan SEKALI saat user menekan Start pada timer quest.
AlarmManager BUKAN dicek ulang saat app dibuka — alarm sudah independen dari lifecycle app.

### Implementasi wajib

```kotlin
fun scheduleQuestAlarm(context: Context, questId: Long, triggerAtMillis: Long) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val intent = PendingIntent.getBroadcast(
        context,
        questId.toInt(),  // request code unik per quest
        Intent(context, QuestAlarmReceiver::class.java).apply {
            putExtra("quest_id", questId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // WAJIB gunakan setExactAndAllowWhileIdle untuk akurasi saat device idle/Doze
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        intent
    )
}
```

### Aturan AlarmManager

1. WAJIB gunakan setExactAndAllowWhileIdle() — jangan gunakan set() atau setExact() saja.
2. WAJIB gunakan FLAG_IMMUTABLE untuk PendingIntent (required Android 12+).
3. Request code PendingIntent WAJIB unik per quest (gunakan questId.toInt()).
4. Alarm WAJIB di-cancel saat user cancel timer:

```kotlin
fun cancelQuestAlarm(context: Context, questId: Long) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val intent = PendingIntent.getBroadcast(
        context,
        questId.toInt(),
        Intent(context, QuestAlarmReceiver::class.java),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    intent?.let { alarmManager.cancel(it) }
}
```

5. Saat device reboot, AlarmManager terhapus — WAJIB reschedule via BOOT_COMPLETED receiver
   jika ada active_timers yang masih berjalan.

---

## 3. BroadcastReceiver

### QuestAlarmReceiver — saat alarm timer bunyi

```kotlin
class QuestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val questId = intent.getLongExtra("quest_id", -1L)
        if (questId == -1L) return

        // 1. Update active_timers SET alarm_fired_at = now WHERE quest_id = questId
        // 2. Tampilkan notifikasi sistem Android (bunyi + getar)
        // 3. Jangan commit EXP di sini — tunggu konfirmasi user lewat UI
    }
}
```

### BootReceiver — reschedule setelah device reboot

```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Query active_timers WHERE alarm_fired_at IS NULL
        // Untuk setiap baris:
        //   hitung triggerAtMillis = started_at + target_duration_seconds
        //   jika triggerAtMillis > now: reschedule alarm
        //   jika triggerAtMillis <= now: langsung update alarm_fired_at = now + trigger notifikasi
    }
}
```

### Registrasi di AndroidManifest.xml

```xml
<receiver android:name=".receiver.QuestAlarmReceiver" android:exported="false"/>
<receiver android:name=".receiver.BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
    </intent-filter>
</receiver>
<service
    android:name=".service.QuestTimerService"
    android:foregroundServiceType="specialUse"
    android:exported="false"/>
```

---

## 4. Compose Lifecycle

### LaunchedEffect vs DisposableEffect

```kotlin
// LaunchedEffect — untuk side effect yang hanya perlu START (coroutine)
// Contoh: load data saat screen pertama kali muncul
LaunchedEffect(Unit) {
    viewModel.loadTodayQuests()
}

// LaunchedEffect dengan key — re-launch saat key berubah
LaunchedEffect(questId) {
    viewModel.loadQuestDetail(questId)
}

// DisposableEffect — untuk side effect yang butuh CLEANUP saat composable keluar
// Contoh: register/unregister listener
DisposableEffect(Unit) {
    val listener = ...
    onDispose {
        listener.unregister()
    }
}
```

### Aturan Compose

1. JANGAN panggil suspend function atau blocking call langsung di Composable body.
2. JANGAN gunakan rememberCoroutineScope() untuk operasi yang seharusnya di ViewModel.
3. State yang berasal dari ViewModel WAJIB di-collect via collectAsStateWithLifecycle() — bukan collectAsState().
4. JANGAN buat State yang duplikat antara ViewModel dan Composable untuk data yang sama.

```kotlin
// BENAR
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// SALAH — collectAsState tidak lifecycle-aware
val uiState by viewModel.uiState.collectAsState()
```

---

## 5. ViewModel Lifecycle

### Struktur ViewModel wajib

```kotlin
class TodayQuestsViewModel(
    private val questRepository: QuestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TodayQuestsUiState>(TodayQuestsUiState.Loading)
    val uiState: StateFlow<TodayQuestsUiState> = _uiState.asStateFlow()

    init {
        loadTodayQuests()
    }

    private fun loadTodayQuests() {
        viewModelScope.launch {
            questRepository.getTodayQuests()
                .catch { e -> _uiState.value = TodayQuestsUiState.Error(e.message) }
                .collect { quests -> _uiState.value = TodayQuestsUiState.Success(quests) }
        }
    }
}
```

### Aturan ViewModel

1. WAJIB gunakan viewModelScope untuk semua coroutine di ViewModel.
2. WAJIB expose StateFlow, bukan MutableStateFlow, ke UI.
3. WAJIB definisikan sealed class UiState per screen untuk merepresentasikan Loading/Success/Error.
4. JANGAN simpan Android Context di ViewModel — gunakan Application context via AndroidViewModel jika benar-benar dibutuhkan.

---

## 6. App Startup — Lazy Evaluation

Lazy evaluation (Lapis A + Lapis B) WAJIB dijalankan di ViewModel atau Repository,
BUKAN di Activity/Composable onCreate langsung.

```kotlin
// Di MainActivity atau NavHost entry point
LaunchedEffect(Unit) {
    appViewModel.runLazyEvaluation()  // trigger dari UI, eksekusi di ViewModel/Repository
}
```

Pastikan UI menampilkan loading state selama lazy evaluation berjalan
agar user tidak melihat data stale sebelum evaluasi selesai.

---

## 7. Checklist Sebelum Task Android Lifecycle Selesai

- [ ] Foreground Service return START_STICKY
- [ ] Timer state di-restore dari DB saat Service restart, bukan dari in-memory
- [ ] AlarmManager menggunakan setExactAndAllowWhileIdle()
- [ ] PendingIntent menggunakan FLAG_IMMUTABLE
- [ ] Alarm di-cancel saat timer di-cancel
- [ ] BootReceiver me-reschedule alarm yang aktif setelah device reboot
- [ ] Compose menggunakan collectAsStateWithLifecycle()
- [ ] Tidak ada blocking call di Composable body
- [ ] ViewModel menggunakan viewModelScope untuk semua coroutine
- [ ] Lazy evaluation dipanggil dari ViewModel, bukan dari Activity/Composable langsung

