package pet.project.sudokusolver.feature.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickFailure
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickResult
import pet.project.sudokusolver.data.recognition.SudokuPhotoPicker
import pet.project.sudokusolver.domain.SudokuGrid

class InputChoiceViewModelTest {
    @Test
    fun exposesLoadingAndIgnoresDuplicatePhotoIntent() {
        val picker = ControlledPhotoPicker()
        val viewModel = InputChoiceViewModel()

        viewModel.onIntent(InputChoiceIntent.PhotoInputClicked, picker) {}
        viewModel.onIntent(InputChoiceIntent.PhotoInputClicked, picker) {}

        assertTrue(viewModel.state.isPhotoLoading)
        assertEquals(1, picker.requestCount)
    }

    @Test
    fun cancellationIsExplicitAndClearsLoading() {
        val picker = ControlledPhotoPicker()
        val viewModel = InputChoiceViewModel()
        viewModel.onIntent(InputChoiceIntent.PhotoInputClicked, picker) {}

        picker.complete(SudokuPhotoPickResult.Cancelled)

        assertFalse(viewModel.state.isPhotoLoading)
        assertTrue(viewModel.state.wasPhotoCancelled)
        assertNull(viewModel.state.photoFailure)
    }

    @Test
    fun decodeFailureIsExposedWithoutNavigation() {
        val picker = ControlledPhotoPicker()
        val viewModel = InputChoiceViewModel()
        var effect: InputChoiceEffect? = null
        viewModel.onIntent(InputChoiceIntent.PhotoInputClicked, picker) { effect = it }

        picker.complete(SudokuPhotoPickResult.Failed(SudokuPhotoPickFailure.DecodeFailed))

        assertFalse(viewModel.state.isPhotoLoading)
        assertEquals(SudokuPhotoPickFailure.DecodeFailed, viewModel.state.photoFailure)
        assertNull(effect)
    }

    @Test
    fun recognizedPixelsResultIsForwardedToBoardFlow() {
        val picker = ControlledPhotoPicker()
        val viewModel = InputChoiceViewModel()
        var effect: InputChoiceEffect? = null
        viewModel.onIntent(InputChoiceIntent.PhotoInputClicked, picker) { effect = it }

        picker.complete(SudokuPhotoPickResult.Recognized(SudokuGrid.Empty))

        val navigation = assertIs<InputChoiceEffect.NavigateToBoard>(effect)
        assertEquals(SudokuGrid.Empty, navigation.grid)
        assertFalse(viewModel.state.isPhotoLoading)
    }

    @Test
    fun manualInputCannotRaceAnActivePhotoRequest() {
        val picker = ControlledPhotoPicker()
        val viewModel = InputChoiceViewModel()
        val effects = mutableListOf<InputChoiceEffect>()
        viewModel.onIntent(InputChoiceIntent.PhotoInputClicked, picker, effects::add)

        viewModel.onIntent(InputChoiceIntent.ManualInputClicked, picker, effects::add)

        assertTrue(viewModel.state.isPhotoLoading)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun retainedStateCanUsePickerFromRecreatedActivity() {
        val oldPicker = ControlledPhotoPicker()
        val newPicker = ControlledPhotoPicker()
        val viewModel = InputChoiceViewModel()
        viewModel.onIntent(InputChoiceIntent.PhotoInputClicked, oldPicker) {}
        oldPicker.complete(SudokuPhotoPickResult.Cancelled)

        viewModel.onIntent(InputChoiceIntent.PhotoInputClicked, newPicker) {}

        assertEquals(1, oldPicker.requestCount)
        assertEquals(1, newPicker.requestCount)
        assertTrue(viewModel.state.isPhotoLoading)
        assertFalse(viewModel.state.wasPhotoCancelled)
    }

    private class ControlledPhotoPicker : SudokuPhotoPicker {
        var requestCount = 0
            private set
        private var callback: ((SudokuPhotoPickResult) -> Unit)? = null

        override fun pickSudokuPhoto(onResult: (SudokuPhotoPickResult) -> Unit) {
            requestCount += 1
            callback = onResult
        }

        fun complete(result: SudokuPhotoPickResult) {
            checkNotNull(callback).invoke(result)
            callback = null
        }
    }
}
