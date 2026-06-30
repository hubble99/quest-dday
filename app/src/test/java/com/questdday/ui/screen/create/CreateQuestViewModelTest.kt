package com.questdday.ui.screen.create

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.questdday.data.repository.AttributeRepository
import com.questdday.data.repository.QuestRepository
import com.questdday.ui.screen.today.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CreateQuestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val application = mockk<Application>(relaxed = true)
    private val questRepository = mockk<QuestRepository>(relaxed = true)
    private val attributeRepository = mockk<AttributeRepository>(relaxed = true)

    private lateinit var viewModel: CreateQuestViewModel

    @Before
    fun setup() {
        every { attributeRepository.getAllAttributes() } returns flowOf(emptyList())
        coEvery { questRepository.insertStandaloneQuest(any()) } returns 1L
        coEvery { questRepository.insertSubQuest(any(), any()) } returns 2L
        coEvery { questRepository.insertEpicWithFirstSubQuest(any(), any()) } returns 3L
    }

    @After
    fun teardown() {
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun createViewModel(): CreateQuestViewModel {
        viewModel = CreateQuestViewModel(
            application = application,
            questRepository = questRepository,
            attributeRepository = attributeRepository
        )
        return viewModel
    }

    @Test
    fun `validateAndSave sets error when title is empty`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        viewModel.updateTitle("")
        viewModel.selectAttribute(1L)
        viewModel.setCompletionMode("instant")

        // Act
        val result = viewModel.validateAndSave()

        // Assert
        assertFalse(result)
        assertTrue(viewModel.formState.value.errors.containsKey("title"))
        assertEquals("Quest title cannot be empty", viewModel.formState.value.errors["title"])
    }

    @Test
    fun `validateAndSave sets error when attribute not selected`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        viewModel.updateTitle("Quest Title")
        viewModel.setCompletionMode("instant")

        // Act
        val result = viewModel.validateAndSave()

        // Assert
        assertFalse(result)
        assertTrue(viewModel.formState.value.errors.containsKey("selectedAttributeId"))
        assertEquals("Quest must have an attribute selected", viewModel.formState.value.errors["selectedAttributeId"])
    }

    @Test
    fun `validateAndSave sets error when timer mode but no duration`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        viewModel.updateTitle("Quest Title")
        viewModel.selectAttribute(1L)
        viewModel.setCompletionMode("timer")
        viewModel.setTargetDuration(0)

        // Act
        val result = viewModel.validateAndSave()

        // Assert
        assertFalse(result)
        assertTrue(viewModel.formState.value.errors.containsKey("targetDurationSeconds"))
        assertEquals("Timer duration must be greater than 0", viewModel.formState.value.errors["targetDurationSeconds"])
    }

    @Test
    fun `validateAndSave sets error when time_bound but no end_date or target_days`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        viewModel.updateTitle("Quest Title")
        viewModel.selectAttribute(1L)
        viewModel.setCompletionMode("instant")
        viewModel.setDurationType("time_bound")
        viewModel.setAbsenceMode("shift")
        // Both targetDays and endDate are null or invalid

        // Act
        val result = viewModel.validateAndSave()

        // Assert
        assertFalse(result)
        assertTrue(viewModel.formState.value.errors.containsKey("targetDays") || viewModel.formState.value.errors.containsKey("endDate"))
    }

    @Test
    fun `validateAndSave sets error when custom_days but no days selected`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        viewModel.updateTitle("Quest Title")
        viewModel.selectAttribute(1L)
        viewModel.setCompletionMode("instant")
        viewModel.setScheduleType("custom_days")
        // scheduleDays is empty

        // Act
        val result = viewModel.validateAndSave()

        // Assert
        assertFalse(result)
        assertTrue(viewModel.formState.value.errors.containsKey("scheduleDays"))
        assertEquals("At least one day must be selected", viewModel.formState.value.errors["scheduleDays"])
    }

    @Test
    fun `validateAndSave sets error when sub-quest end_date exceeds parent end_date`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        viewModel.updateTitle("Sub Quest")
        viewModel.selectAttribute(1L)
        viewModel.setCompletionMode("instant")
        viewModel.setDurationType("time_bound")
        viewModel.setDurationInputType("date")
        viewModel.setEndDate("2026-07-15")
        viewModel.setAbsenceMode("shift")
        viewModel.setParentQuestId(10L, "2026-07-10")

        // Act
        val result = viewModel.validateAndSave()

        // Assert
        assertFalse(result)
        assertTrue(viewModel.formState.value.errors.containsKey("endDate"))
        assertEquals("Batas waktu sub-quest tidak boleh melebihi batas waktu Epic induk", viewModel.formState.value.errors["endDate"])
    }

    @Test
    fun `validateAndSave clears errors on successful validation`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        // First set invalid state to trigger errors
        viewModel.updateTitle("")
        viewModel.validateAndSave()
        assertFalse(viewModel.formState.value.errors.isEmpty())

        // Correct the state
        viewModel.updateTitle("Valid Quest")
        viewModel.selectAttribute(1L)
        viewModel.setCompletionMode("instant")

        // Act
        val result = viewModel.validateAndSave()

        // Assert
        assertTrue(result)
        assertTrue(viewModel.formState.value.errors.isEmpty())
    }

    @Test
    fun `validateAndSave calls insertStandaloneQuest for non-container without parent`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        viewModel.updateTitle("Standalone Quest")
        viewModel.selectAttribute(1L)
        viewModel.setCompletionMode("instant")
        viewModel.setDurationType("endless")

        // Act
        val result = viewModel.validateAndSave()

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) { questRepository.insertStandaloneQuest(any()) }
        coVerify(exactly = 0) { questRepository.insertSubQuest(any(), any()) }
        coVerify(exactly = 0) { questRepository.insertEpicWithFirstSubQuest(any(), any()) }
    }

    @Test
    fun `validateAndSave calls insertSubQuest when parentQuestId is set`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        viewModel.updateTitle("Sub Quest")
        viewModel.selectAttribute(1L)
        viewModel.setCompletionMode("instant")
        viewModel.setDurationType("endless")
        viewModel.setParentQuestId(5L, null)

        // Act
        val result = viewModel.validateAndSave()

        // Assert
        assertTrue(result)
        coVerify(exactly = 1) { questRepository.insertSubQuest(any(), 5L) }
        coVerify(exactly = 0) { questRepository.insertStandaloneQuest(any()) }
        coVerify(exactly = 0) { questRepository.insertEpicWithFirstSubQuest(any(), any()) }
    }

    @Test
    fun `validateAndSave calls insertEpicWithFirstSubQuest when isContainer is true`() = runTest {
        // Arrange
        val viewModel = createViewModel()
        
        // 1. Enter Epic details
        viewModel.setIsContainer(true)
        viewModel.updateTitle("Epic Quest")
        viewModel.selectAttribute(1L)
        viewModel.setDurationType("endless")

        // Act 1: click save/next on epic
        val epicResult = viewModel.validateAndSave()

        // Assert 1: should return false (transition to subquest mode), parentQuestId should be null and isAwaitingFirstSubQuest should be true
        assertFalse(epicResult)
        assertNull(viewModel.formState.value.parentQuestId)
        assertTrue(viewModel.formState.value.isAwaitingFirstSubQuest)

        // 2. Enter Sub-quest details
        viewModel.updateTitle("Sub Quest 1")
        viewModel.setCompletionMode("instant")
        viewModel.setDurationType("endless")

        // Act 2: click save on sub-quest
        val subQuestResult = viewModel.validateAndSave()

        // Assert 2: should return true and invoke repository
        assertTrue(subQuestResult)
        coVerify(exactly = 1) { questRepository.insertEpicWithFirstSubQuest(any(), any()) }
    }

    @Test
    fun `setDurationType to endless clears absenceMode from formState`() {
        // Arrange
        val viewModel = createViewModel()
        viewModel.setDurationType("time_bound")
        viewModel.setAbsenceMode("shift")
        assertEquals("shift", viewModel.formState.value.absenceMode)

        // Act
        viewModel.setDurationType("endless")

        // Assert
        assertEquals(null, viewModel.formState.value.absenceMode)
    }
}
