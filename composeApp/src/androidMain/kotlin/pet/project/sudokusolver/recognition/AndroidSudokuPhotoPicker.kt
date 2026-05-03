package pet.project.sudokusolver.recognition

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickResult
import pet.project.sudokusolver.data.recognition.SudokuPhotoPicker
import pet.project.sudokusolver.domain.SudokuGrid

class AndroidSudokuPhotoPicker(
    activity: ComponentActivity,
    private val recognitionRepository: AndroidSudokuRecognitionRepository = AndroidSudokuRecognitionRepository(),
) : SudokuPhotoPicker {
    private var pendingResult: ((SudokuPhotoPickResult) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val callback = pendingResult
        pendingResult = null

        if (callback == null) return@registerForActivityResult
        if (uri == null) {
            callback(SudokuPhotoPickResult.Cancelled)
        } else {
            callback(recognitionRepository.recognize(uri))
        }
    }

    override fun pickSudokuPhoto(onResult: (SudokuPhotoPickResult) -> Unit) {
        pendingResult = onResult
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

class AndroidSudokuRecognitionRepository {
    fun recognize(uri: Uri): SudokuPhotoPickResult {
        // TODO: Replace the sample grid with a real CV pipeline:
        // 1. Decode and normalize the image.
        // 2. Detect the outer sudoku contour and rectify perspective.
        // 3. Split the board into 81 cells.
        // 4. Run OCR/digit classification for each cell.
        // 5. Return a grid with confidence metadata for user correction.
        return SudokuPhotoPickResult.Recognized(SampleRecognizedGrid)
    }
}

private val SampleRecognizedGrid = SudokuGrid.fromRows(
    listOf(
        listOf(5, 3, null, null, 7, null, null, null, null),
        listOf(6, null, null, 1, 9, 5, null, null, null),
        listOf(null, 9, 8, null, null, null, null, 6, null),
        listOf(8, null, null, null, 6, null, null, null, 3),
        listOf(4, null, null, 8, null, 3, null, null, 1),
        listOf(7, null, null, null, 2, null, null, null, 6),
        listOf(null, 6, null, null, null, null, 2, 8, null),
        listOf(null, null, null, 4, 1, 9, null, null, 5),
        listOf(null, null, null, null, 8, null, null, 7, 9),
    ),
)
