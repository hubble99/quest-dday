package com.questdday.ui.screen.today

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.questdday.data.repository.*
import com.questdday.domain.model.ActiveTimer
import com.questdday.domain.model.Quest
import com.questdday.domain.model.UserAttributeStat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayQuestsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val application = mockk<Application>(relaxed = true)
    private val questRepository = mockk<QuestRepository>(relaxed = true)
    private val timerRepository = mockk<TimerRepository>(relaxed = true)
    private val expRepository = mockk<ExpRepository>(relaxed = true)
    private val lazyEvaluationRepository = mockk<LazyEvaluationRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val questLogRepository = mockk<QuestLogRepository>(relaxed = true)
    private val attributeRepository = mockk<AttributeRepository>(relaxed = true)

    private lateinit var viewModel: TodayQuestsViewModel

    @Before
    fun setup() {
        // Default mocks
        coEvery { lazyEvaluationRepository.runLayerB(any()) } returns LayerBResult(emptyList(), emptyList())
        coEvery { lazyEvaluationRepository.runLayerA(any(), any()) } returns LayerAResult.AlreadyEvaluated
        every { questRepository.getTodayQuests(any(), any()) } returns flowOf(emptyList())
        every { timerRepository.getPendingConfirmationTimers() } returns flowOf(emptyList())
        every { timerRepository.getRunningTimers() } returns flowOf(emptyList())
        every { questLogRepository.getLogsForDate(any(), any()) } returns flowOf(emptyList())
        every { attributeRepository.getAllAttributes() } returns flowOf(emptyList())
    }

    @After
    fun teardown() {
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun createViewModel(): TodayQuestsViewModel {
        viewModel = TodayQuestsViewModel(
            application = application,
            questRepository = questRepository,
            timerRepository = timerRepository,
            expRepository = expRepository,
            lazyEvaluationRepository = lazyEvaluationRepository,
            settingsRepository = settingsRepository,
            questLogRepository = questLogRepository,
            attributeRepository = attributeRepository
        )
        return viewModel
    }

    @Test
    fun `initial state is Loading`() {
        // Arrange
        // (Initialization blocks in ViewModel setup flow, but dispatcher is paused)

        // Act
        val viewModel = createViewModel()

        // Assert
        assertEquals(TodayQuestsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `initialize calls layerB before layerA`() = runTest {
        // Arrange
        val layerBResult = LayerBResult(emptyList(), emptyList())
        coEvery { lazyEvaluationRepository.runLayerB(1L) } returns layerBResult
        coEvery { lazyEvaluationRepository.runLayerA(1L, any()) } returns LayerAResult.AlreadyEvaluated

        // Act
        val viewModel = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerifyOrder {
            lazyEvaluationRepository.runLayerB(1L)
            lazyEvaluationRepository.runLayerA(1L, any())
        }

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `initialize emits Success state after data loaded`() = runTest {
        // Arrange
        val expectedQuests = listOf(
            Quest(
                id = 10L,
                userId = 1L,
                parentQuestId = null,
                attributeId = 2L,
                title = "Study Math",
                description = "Solve 5 problems",
                isContainer = false,
                completionMode = "instant",
                targetDurationSeconds = null,
                durationType = "endless",
                durationInputType = null,
                targetDays = null,
                startDate = "2026-06-20",
                endDate = null,
                absenceMode = null,
                stackedDurationSeconds = 0,
                scheduleType = "daily",
                scheduleDays = null,
                status = "active",
                consecutiveMissedSessions = 0,
                lastCompletedAt = null,
                createdAt = "2026-06-20T10:00:00Z",
                updatedAt = "2026-06-20T10:00:00Z"
            )
        )
        val expectedPending = listOf(
            ActiveTimer(id = 1L, questId = 20L, startedAt = "2026-06-29T18:00:00Z", targetDurationSeconds = 1200, alarmFiredAt = "2026-06-29T18:20:00Z")
        )
        val expectedRunning = listOf(
            ActiveTimer(id = 2L, questId = 30L, startedAt = "2026-06-29T19:00:00Z", targetDurationSeconds = 1800, alarmFiredAt = null)
        )

        coEvery { lazyEvaluationRepository.runLayerB(1L) } returns LayerBResult(expectedPending, expectedRunning)
        every { questRepository.getTodayQuests(1L, any()) } returns flowOf(expectedQuests)
        every { timerRepository.getPendingConfirmationTimers() } returns flowOf(expectedPending)
        every { timerRepository.getRunningTimers() } returns flowOf(expectedRunning)

        // Act
        val viewModel = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val currentState = viewModel.uiState.value
        assertTrue(currentState is TodayQuestsUiState.Success)
        val successState = currentState as TodayQuestsUiState.Success
        assertEquals(expectedQuests, successState.quests)
        assertEquals(expectedPending, successState.pendingConfirmationTimers)
        assertEquals(expectedRunning, successState.runningTimers)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `completeInstantQuest inserts log and awards exp`() = runTest {
        // Arrange
        val quest = Quest(
            id = 10L,
            userId = 1L,
            parentQuestId = null,
            attributeId = 2L,
            title = "Study Math",
            description = "Solve 5 problems",
            isContainer = false,
            completionMode = "instant",
            targetDurationSeconds = null,
            durationType = "endless",
            durationInputType = null,
            targetDays = null,
            startDate = "2026-06-20",
            endDate = null,
            absenceMode = null,
            stackedDurationSeconds = 0,
            scheduleType = "daily",
            scheduleDays = null,
            status = "active",
            consecutiveMissedSessions = 0,
            lastCompletedAt = null,
            createdAt = "2026-06-20T10:00:00Z",
            updatedAt = "2026-06-20T10:00:00Z"
        )
        coEvery { questRepository.getQuestById(10L) } returns quest
        coEvery { settingsRepository.getValue("daily_exp_base") } returns "15.0"
        coEvery { expRepository.getStat(1L, 2L) } returns UserAttributeStat(userId = 1L, attributeId = 2L, currentLevel = 1, currentExp = 0.0, lastGainedAt = null, updatedAt = "")

        val viewModel = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.completeInstantQuest(10L, 2L)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            questLogRepository.insertLog(
                questId = 10L,
                userId = 1L,
                logDate = any(),
                actualDurationSeconds = null,
                expAwarded = 15.0,
                isEpicFinaleBonus = false
            )
            expRepository.awardExp(1L, 2L, 15.0, any())
        }

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `completeInstantQuest awards epic finale bonus on quest end_date`() = runTest {
        // Arrange
        val todayStr = LocalDate.now().toString()
        val quest = Quest(
            id = 10L,
            userId = 1L,
            parentQuestId = null,
            attributeId = 2L,
            title = "Study Math",
            description = "Solve 5 problems",
            isContainer = false,
            completionMode = "instant",
            targetDurationSeconds = null,
            durationType = "time_bound",
            durationInputType = "date",
            targetDays = null,
            startDate = "2026-06-20",
            endDate = todayStr,
            absenceMode = "shift",
            stackedDurationSeconds = 0,
            scheduleType = "daily",
            scheduleDays = null,
            status = "active",
            consecutiveMissedSessions = 0,
            lastCompletedAt = null,
            createdAt = "2026-06-20T10:00:00Z",
            updatedAt = "2026-06-20T10:00:00Z"
        )
        coEvery { questRepository.getQuestById(10L) } returns quest
        coEvery { settingsRepository.getValue("daily_exp_base") } returns "15.0"
        coEvery { settingsRepository.getEpicFinaleBonus() } returns 1000.0
        coEvery { expRepository.getStat(1L, 2L) } returnsMany listOf(
            UserAttributeStat(userId = 1L, attributeId = 2L, currentLevel = 1, currentExp = 0.0, lastGainedAt = null, updatedAt = ""), // stat before
            UserAttributeStat(userId = 1L, attributeId = 2L, currentLevel = 5, currentExp = 20.0, lastGainedAt = null, updatedAt = "")  // stat after (level up!)
        )

        val viewModel = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Collect events in backgroundScope
        val events = mutableListOf<LevelUpEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.levelUpEvent.collect { events.add(it) }
        }

        // Act
        viewModel.completeInstantQuest(10L, 2L)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            questLogRepository.insertLog(10L, 1L, todayStr, null, 15.0, false)
            expRepository.awardExp(1L, 2L, 15.0, any())
            questLogRepository.insertLog(10L, 1L, todayStr, null, 1000.0, true)
            expRepository.awardExp(1L, 2L, 1000.0, any())
        }

        // Verify level up event emitted
        assertEquals(1, events.size)
        val event = events.first()
        assertEquals(2L, event.attributeId)
        assertEquals(1, event.previousLevel)
        assertEquals(5, event.newLevel)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `startTimer emits error when timer already exists`() = runTest {
        // Arrange
        coEvery { timerRepository.startTimer(10L, 300) } returns false

        val viewModel = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Collect errors in backgroundScope
        val errors = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.errorEvent.collect { errors.add(it) }
        }

        // Act
        viewModel.startTimer(10L, 300)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(1, errors.size)
        assertEquals("Timer sudah berjalan untuk quest ini", errors.first())

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `cancelTimer calls repository cancel without awarding exp`() = runTest {
        // Arrange
        coEvery { timerRepository.cancelTimer(10L) } just Runs

        val viewModel = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.cancelTimer(10L)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { timerRepository.cancelTimer(10L) }
        coVerify(exactly = 0) { expRepository.awardExp(any(), any(), any(), any()) }

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `confirmTimerComplete awards exp and removes timer`() = runTest {
        // Arrange
        val activeTimer = ActiveTimer(id = 1L, questId = 10L, startedAt = "2026-06-29T19:00:00Z", targetDurationSeconds = 300, alarmFiredAt = null)
        coEvery { timerRepository.getTimerByQuestId(10L) } returns activeTimer
        coEvery { settingsRepository.getValue("daily_exp_base") } returns "15.0"
        coEvery { expRepository.getStat(1L, 2L) } returns UserAttributeStat(userId = 1L, attributeId = 2L, currentLevel = 1, currentExp = 0.0, lastGainedAt = null, updatedAt = "")
        coEvery { questRepository.getQuestById(10L) } returns null // non-end_date check

        val viewModel = createViewModel()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Act
        viewModel.confirmTimerComplete(10L, 2L, 300)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            timerRepository.confirmTimerComplete(10L, 1L, any(), 300, 15.0)
            expRepository.awardExp(1L, 2L, 15.0, any())
        }

        viewModel.viewModelScope.cancel()
    }
}
