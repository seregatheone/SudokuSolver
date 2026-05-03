package pet.project.sudokusolver.feature.input

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickFailure
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickResult
import pet.project.sudokusolver.data.recognition.SudokuPhotoPicker
import pet.project.sudokusolver.domain.SudokuGrid

data class InputChoiceState(
    val isPhotoLoading: Boolean = false,
    val photoFailure: SudokuPhotoPickFailure? = null,
)

sealed interface InputChoiceIntent {
    data object ManualInputClicked : InputChoiceIntent
    data object PhotoInputClicked : InputChoiceIntent
}

sealed interface InputChoiceEffect {
    data class NavigateToBoard(val grid: SudokuGrid) : InputChoiceEffect
}

class InputChoiceViewModel(
    private val photoPicker: SudokuPhotoPicker,
) : ViewModel() {
    var state by mutableStateOf(InputChoiceState())
        private set

    fun onIntent(
        intent: InputChoiceIntent,
        onEffect: (InputChoiceEffect) -> Unit,
    ) {
        when (intent) {
            InputChoiceIntent.ManualInputClicked -> {
                state = state.copy(photoFailure = null)
                onEffect(InputChoiceEffect.NavigateToBoard(SudokuGrid.Empty))
            }

            InputChoiceIntent.PhotoInputClicked -> {
                state = state.copy(isPhotoLoading = true, photoFailure = null)
                photoPicker.pickSudokuPhoto { result ->
                    state = state.copy(isPhotoLoading = false)
                    when (result) {
                        SudokuPhotoPickResult.Cancelled -> Unit
                        is SudokuPhotoPickResult.Failed -> state = state.copy(photoFailure = result.failure)
                        is SudokuPhotoPickResult.Recognized -> onEffect(InputChoiceEffect.NavigateToBoard(result.grid))
                    }
                }
            }
        }
    }
}
