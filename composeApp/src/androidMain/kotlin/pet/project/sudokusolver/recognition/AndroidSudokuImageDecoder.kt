package pet.project.sudokusolver.recognition

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.max

sealed interface AndroidSudokuImageDecodeResult {
    data class Decoded(val image: AndroidSudokuImage) : AndroidSudokuImageDecodeResult
    data class Failed(
        val failure: AndroidSudokuImageDecodeFailure,
        val detail: String?,
    ) : AndroidSudokuImageDecodeResult
}

enum class AndroidSudokuImageDecodeFailure {
    Unreadable,
    OutOfMemory,
}

class AndroidSudokuImageDecoder(
    private val contentResolver: ContentResolver,
    private val maxDecodedDimension: Int = DefaultMaxDecodedDimension,
    private val maxDecodedPixels: Long = DefaultMaxDecodedPixels,
) {
    init {
        require(maxDecodedDimension > 0) { "Maximum decoded dimension must be positive." }
        require(maxDecodedPixels > 0) { "Maximum decoded pixel count must be positive." }
    }

    fun decode(uri: Uri): AndroidSudokuImageDecodeResult {
        return try {
            decodeOrThrow(uri)
        } catch (error: Exception) {
            AndroidSudokuImageDecodeResult.Failed(
                failure = AndroidSudokuImageDecodeFailure.Unreadable,
                detail = error.message,
            )
        } catch (error: OutOfMemoryError) {
            AndroidSudokuImageDecodeResult.Failed(
                failure = AndroidSudokuImageDecodeFailure.OutOfMemory,
                detail = error.message,
            )
        }
    }

    private fun decodeOrThrow(uri: Uri): AndroidSudokuImageDecodeResult.Decoded {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = openInputStream(uri) ?: error("Content resolver returned no stream.")
        boundsStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        val originalWidth = bounds.outWidth
        val originalHeight = bounds.outHeight
        require(originalWidth > 0 && originalHeight > 0) { "Image bounds are invalid." }

        val sampleSize = calculateImageSampleSize(
            width = originalWidth,
            height = originalHeight,
            maxDimension = maxDecodedDimension,
            maxPixels = maxDecodedPixels,
        )
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }
        val decoded = openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("Image pixels could not be decoded.")

        val decodedPixels = decoded.width.toLong() * decoded.height.toLong()
        if (
            max(decoded.width, decoded.height) > maxDecodedDimension ||
            decodedPixels > maxDecodedPixels
        ) {
            decoded.recycle()
            error("Decoder exceeded the bounded allocation contract.")
        }

        val orientation = readExifOrientation(uri)
        val normalized = try {
            normalizeExifOrientation(decoded, orientation)
        } catch (error: Exception) {
            decoded.recycle()
            throw error
        } catch (error: OutOfMemoryError) {
            decoded.recycle()
            throw error
        }
        return AndroidSudokuImageDecodeResult.Decoded(
            AndroidSudokuImage(
                bitmap = normalized,
                sourceUri = uri,
                originalWidth = originalWidth,
                originalHeight = originalHeight,
                sampleSize = sampleSize,
            ),
        )
    }

    private fun readExifOrientation(uri: Uri): Int = runCatching {
        openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun openInputStream(uri: Uri): InputStream? = if (uri.scheme == ContentResolver.SCHEME_FILE) {
        uri.path?.let { path -> FileInputStream(File(path)) }
    } else {
        contentResolver.openInputStream(uri)
    }

    private fun normalizeExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = exifOrientationMatrix(orientation)
        if (matrix.isIdentity) return bitmap

        val normalized = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
        if (normalized !== bitmap) bitmap.recycle()
        return normalized
    }

    private fun exifOrientationMatrix(orientation: Int): Matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                setRotate(90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                setRotate(-90f)
                postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
        }
    }

    companion object {
        const val DefaultMaxDecodedDimension = 4096
        const val DefaultMaxDecodedPixels = 8_388_608L
    }
}

internal fun calculateImageSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int,
    maxPixels: Long,
): Int {
    require(width > 0 && height > 0) { "Image dimensions must be positive." }
    require(maxDimension > 0 && maxPixels > 0) { "Decode bounds must be positive." }

    var sampleSize = 1
    while (true) {
        val sampledWidth = ceilDiv(width, sampleSize)
        val sampledHeight = ceilDiv(height, sampleSize)
        if (
            max(sampledWidth, sampledHeight) <= maxDimension &&
            sampledWidth.toLong() * sampledHeight.toLong() <= maxPixels
        ) {
            return sampleSize
        }
        check(sampleSize <= Int.MAX_VALUE / 2) { "Image dimensions cannot be bounded." }
        sampleSize *= 2
    }
}

private fun ceilDiv(value: Int, divisor: Int): Int =
    ((value.toLong() + divisor.toLong() - 1L) / divisor.toLong()).toInt()
