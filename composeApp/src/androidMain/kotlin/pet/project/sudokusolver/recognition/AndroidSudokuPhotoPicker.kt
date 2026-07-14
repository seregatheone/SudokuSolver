package pet.project.sudokusolver.recognition

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickFailure
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickResult
import pet.project.sudokusolver.data.recognition.SudokuPhotoPicker

class AndroidSudokuPhotoPicker(
    private val activity: ComponentActivity,
    private val recognitionUseCase: AndroidSudokuImageRecognitionUseCase = AndroidSudokuImageRecognitionUseCase(
        decoder = AndroidSudokuImageDecoder(activity.contentResolver),
        recognizer = AndroidSudokuRecognitionRepository(activity.assets),
    ),
    private val worker: ExecutorService = Executors.newSingleThreadExecutor(),
) : SudokuPhotoPicker, AutoCloseable {
    private var activeResult: ((SudokuPhotoPickResult) -> Unit)? = null
    private val closed = AtomicBoolean(false)

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val callback = activeResult

        if (callback == null || closed.get()) return@registerForActivityResult
        if (uri == null) {
            complete(callback, SudokuPhotoPickResult.Cancelled)
            return@registerForActivityResult
        }

        try {
            worker.execute {
                val result = recognitionUseCase.recognize(uri)
                activity.runOnUiThread {
                    if (!closed.get()) complete(callback, result)
                }
            }
        } catch (_: RejectedExecutionException) {
            complete(callback, SudokuPhotoPickResult.Failed(SudokuPhotoPickFailure.Unavailable))
        }
    }

    override fun pickSudokuPhoto(onResult: (SudokuPhotoPickResult) -> Unit) {
        if (closed.get()) {
            onResult(SudokuPhotoPickResult.Failed(SudokuPhotoPickFailure.Unavailable))
            return
        }
        if (activeResult != null) {
            return
        }

        activeResult = onResult
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun complete(
        callback: (SudokuPhotoPickResult) -> Unit,
        result: SudokuPhotoPickResult,
    ) {
        if (activeResult !== callback) return
        activeResult = null
        callback(result)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            val callback = activeResult
            activeResult = null
            callback?.invoke(SudokuPhotoPickResult.Cancelled)
            worker.shutdownNow()
        }
    }
}

class AndroidSudokuImageRecognitionUseCase(
    private val decoder: AndroidSudokuImageDecoder,
    private val recognizer: AndroidSudokuImageRecognizer,
) {
    fun recognize(uri: Uri): SudokuPhotoPickResult = when (val decoded = decoder.decode(uri)) {
        is AndroidSudokuImageDecodeResult.Failed -> {
            SudokuPhotoPickResult.Failed(SudokuPhotoPickFailure.DecodeFailed)
        }

        is AndroidSudokuImageDecodeResult.Decoded -> {
            try {
                recognizer.recognize(decoded.image)
            } catch (_: Exception) {
                SudokuPhotoPickResult.Failed(SudokuPhotoPickFailure.RecognitionFailed)
            } catch (_: OutOfMemoryError) {
                SudokuPhotoPickResult.Failed(SudokuPhotoPickFailure.RecognitionFailed)
            } finally {
                decoded.image.close()
            }
        }
    }
}

class AndroidSudokuImage(
    val bitmap: Bitmap,
    val sourceUri: Uri,
    val originalWidth: Int,
    val originalHeight: Int,
    val sampleSize: Int,
) : AutoCloseable {
    override fun close() {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

fun interface AndroidSudokuImageRecognizer {
    fun recognize(image: AndroidSudokuImage): SudokuPhotoPickResult
}

class AndroidSudokuRecognitionRepository(
    private val assetManager: AssetManager,
    private val boardExtractor: AndroidSudokuBoardExtractor = AndroidSudokuBoardExtractor(),
) : AndroidSudokuImageRecognizer {
    override fun recognize(image: AndroidSudokuImage): SudokuPhotoPickResult {
        check(!image.bitmap.isRecycled) { "Recognition requires live decoded pixels." }

        return when (val result = boardExtractor.extract(image.bitmap)) {
            is AndroidSudokuBoardExtractionResult.Extracted -> result.board.use { board ->
                try {
                    AndroidSudokuDigitRecognizer.create(assetManager).use { recognizer ->
                        SudokuPhotoPickResult.Recognized(recognizer.recognize(board.cells))
                    }
                } catch (error: AndroidSudokuOcrException) {
                    SudokuPhotoPickResult.Failed(error.failure)
                }
            }

            is AndroidSudokuBoardExtractionResult.Failed -> SudokuPhotoPickResult.Failed(
                result.failure.toPhotoPickFailure(),
            )
        }
    }
}

private fun AndroidSudokuBoardFailure.toPhotoPickFailure(): SudokuPhotoPickFailure = when (this) {
    AndroidSudokuBoardFailure.NativeRuntimeUnavailable ->
        SudokuPhotoPickFailure.NativeProcessingUnavailable
    AndroidSudokuBoardFailure.ImageTooSmall -> SudokuPhotoPickFailure.ImageTooSmall
    AndroidSudokuBoardFailure.BoardNotFound -> SudokuPhotoPickFailure.BoardNotFound
    AndroidSudokuBoardFailure.InvalidGeometry -> SudokuPhotoPickFailure.InvalidBoardGeometry
    AndroidSudokuBoardFailure.ProcessingFailed -> SudokuPhotoPickFailure.RecognitionFailed
}
