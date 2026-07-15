package pet.project.sudokusolver.data.recognition

import pet.project.sudokusolver.domain.SudokuGrid

interface SudokuPhotoPicker {
    fun pickSudokuPhoto(onResult: (SudokuPhotoPickResult) -> Unit)
}

sealed interface SudokuPhotoPickResult {
    data class Recognized(val grid: SudokuGrid) : SudokuPhotoPickResult
    data class Failed(val failure: SudokuPhotoPickFailure) : SudokuPhotoPickResult
    data object Cancelled : SudokuPhotoPickResult
}

enum class SudokuPhotoPickFailure {
    Unavailable,
    DecodeFailed,
    NativeProcessingUnavailable,
    ImageTooSmall,
    BoardNotFound,
    InvalidBoardGeometry,
    ModelUnavailable,
    ModelIncompatible,
    ModelIntegrityFailed,
    InferenceFailed,
    RecognitionFailed,
}

object UnavailableSudokuPhotoPicker : SudokuPhotoPicker {
    override fun pickSudokuPhoto(onResult: (SudokuPhotoPickResult) -> Unit) {
        onResult(SudokuPhotoPickResult.Failed(SudokuPhotoPickFailure.Unavailable))
    }
}
