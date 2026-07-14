package pet.project.sudokusolver.recognition

import android.app.Activity
import android.app.Instrumentation
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickResult
import pet.project.sudokusolver.domain.SudokuGrid

class SudokuImageDecoderInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        try {
            verifyPngIsDownsampledAndPassedToRecognizer()
            verifyJpegExifRotation()
            verifyJpegExifMirror()
            verifyCorruptImageFailure()
            result.putString("summary", "JPEG, PNG, EXIF, bounded decode and recognizer handoff: PASS")
            finish(Activity.RESULT_OK, result)
        } catch (error: Throwable) {
            result.putString("failure", error.stackTraceToString())
            finish(Activity.RESULT_CANCELED, result)
        }
    }

    private fun verifyPngIsDownsampledAndPassedToRecognizer() {
        val imageUri = createMediaStoreImage(
            name = "bounded.png",
            width = 400,
            height = 300,
            format = Bitmap.CompressFormat.PNG,
        )
        var recognizedImage: AndroidSudokuImage? = null
        val useCase = AndroidSudokuImageRecognitionUseCase(
            decoder = AndroidSudokuImageDecoder(
                contentResolver = targetContext.contentResolver,
                maxDecodedDimension = 100,
                maxDecodedPixels = 5_000,
            ),
            recognizer = AndroidSudokuImageRecognizer { image ->
                check(!image.bitmap.isRecycled)
                check(image.bitmap.width <= 100 && image.bitmap.height <= 100)
                check(image.bitmap.width.toLong() * image.bitmap.height <= 5_000)
                recognizedImage = image
                SudokuPhotoPickResult.Recognized(SudokuGrid.Empty)
            },
        )

        try {
            val directDecode = AndroidSudokuImageDecoder(
                contentResolver = targetContext.contentResolver,
                maxDecodedDimension = 100,
                maxDecodedPixels = 5_000,
            ).decode(imageUri)
            check(directDecode is AndroidSudokuImageDecodeResult.Decoded) {
                "Direct PNG decode failed: $directDecode"
            }
            directDecode.image.close()

            check(useCase.recognize(imageUri) is SudokuPhotoPickResult.Recognized)
            checkNotNull(recognizedImage)
            check(recognizedImage.bitmap.isRecycled) { "Recognizer bitmap was not released." }
        } finally {
            targetContext.contentResolver.delete(imageUri, null, null)
        }
    }

    private fun verifyJpegExifRotation() {
        val imageFile = createImageFile(
            name = "rotated.jpg",
            width = 80,
            height = 40,
            format = Bitmap.CompressFormat.JPEG,
        )
        ExifInterface(imageFile.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val result = decoder().decode(Uri.fromFile(imageFile))
        check(result is AndroidSudokuImageDecodeResult.Decoded)
        result.image.use { image ->
            check(image.bitmap.width == 40 && image.bitmap.height == 80) {
                "EXIF rotation was not normalized."
            }
        }
    }

    private fun verifyJpegExifMirror() {
        val imageFile = createImageFile(
            name = "mirrored.jpg",
            width = 80,
            height = 40,
            format = Bitmap.CompressFormat.JPEG,
            splitColors = true,
        )
        ExifInterface(imageFile.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_FLIP_HORIZONTAL.toString())
            saveAttributes()
        }

        val result = decoder().decode(Uri.fromFile(imageFile))
        check(result is AndroidSudokuImageDecodeResult.Decoded)
        result.image.use { image ->
            val left = image.bitmap.getPixel(8, image.bitmap.height / 2)
            val right = image.bitmap.getPixel(image.bitmap.width - 9, image.bitmap.height / 2)
            check(Color.blue(left) > Color.red(left) && Color.red(right) > Color.blue(right)) {
                "EXIF mirror was not normalized."
            }
        }
    }

    private fun verifyCorruptImageFailure() {
        val file = File(targetContext.cacheDir, "corrupt-image.png")
        file.writeText("not an image")
        check(decoder().decode(Uri.fromFile(file)) is AndroidSudokuImageDecodeResult.Failed)
    }

    private fun decoder(): AndroidSudokuImageDecoder =
        AndroidSudokuImageDecoder(targetContext.contentResolver)

    private fun createImageFile(
        name: String,
        width: Int,
        height: Int,
        format: Bitmap.CompressFormat,
        splitColors: Boolean = false,
    ): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (splitColors) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    bitmap.setPixel(x, y, if (x < width / 2) Color.RED else Color.BLUE)
                }
            }
        } else {
            bitmap.eraseColor(Color.WHITE)
        }
        val file = File(targetContext.cacheDir, name)
        FileOutputStream(file).use { stream ->
            check(bitmap.compress(format, 95, stream))
        }
        bitmap.recycle()
        return file
    }

    private fun createMediaStoreImage(
        name: String,
        width: Int,
        height: Int,
        format: Bitmap.CompressFormat,
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SudokuSolverTests")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = targetContext.contentResolver
        val uri = checkNotNull(
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
        )
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                check(bitmap.compress(format, 100, stream))
            } ?: error("MediaStore returned no output stream.")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            check(resolver.update(uri, values, null, null) == 1)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        } finally {
            bitmap.recycle()
        }
    }
}
