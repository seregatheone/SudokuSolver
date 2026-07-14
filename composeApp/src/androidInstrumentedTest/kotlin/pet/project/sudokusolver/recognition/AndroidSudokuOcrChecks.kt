package pet.project.sudokusolver.recognition

import android.app.Instrumentation
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import kotlin.math.abs
import org.json.JSONObject
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickFailure
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickResult
import pet.project.sudokusolver.domain.SudokuCellSource

internal class AndroidSudokuOcrChecks(
    private val instrumentation: Instrumentation,
) {
    fun runAll(): String {
        verifyUnsupportedContractIsExplicit()
        verifyChecksumFailureIsExplicit()
        verifyReferenceVectors()
        verifyTop1465Fixture()
        return "OCR contract, checksum, 9 reference vectors and top1465 #0001"
    }

    private fun verifyUnsupportedContractIsExplicit() {
        val manifest = targetAssets.open("sudoku_ocr/manifest.json")
            .bufferedReader()
            .use { it.readText() }
        val unsupported = JSONObject(manifest).apply {
            put("schema_version", "2.0.0")
        }.toString()

        val error = runCatching { AndroidSudokuOcrContract.parse(unsupported) }.exceptionOrNull()
        check(error is AndroidSudokuOcrException)
        check(error.failure == SudokuPhotoPickFailure.ModelIncompatible)
    }

    private fun verifyChecksumFailureIsExplicit() {
        val error = runCatching {
            verifySudokuOcrAsset(
                bytes = byteArrayOf(1, 2, 3),
                expectedSize = 3,
                expectedSha256 = "0".repeat(64),
            )
        }.exceptionOrNull()
        check(error is AndroidSudokuOcrException)
        check(error.failure == SudokuPhotoPickFailure.ModelIntegrityFailed)
    }

    private fun verifyReferenceVectors() {
        val contract = AndroidSudokuOcrContract.load(targetAssets)
        val vectorBytes = targetAssets.open(
            "${AndroidSudokuOcrContract.AssetRoot}/${contract.referenceVectorsFile}",
        ).use { it.readBytes() }
        check(vectorBytes.size == contract.referenceVectorsSizeBytes)
        check(vectorBytes.sha256() == contract.referenceVectorsSha256)

        val vectors = JSONObject(vectorBytes.decodeToString()).getJSONArray("vectors")
        check(vectors.length() == 9)
        AndroidSudokuDigitRecognizer.create(targetAssets).use { recognizer ->
            repeat(vectors.length()) { index ->
                val vector = vectors.getJSONObject(index)
                val input = Base64.decode(vector.getString("input_base64"), Base64.DEFAULT)
                check(input.sha256() == vector.getString("input_sha256"))
                val inference = recognizer.runNormalizedInput(input)
                check(inference.digit == vector.getInt("expected_digit")) {
                    "${vector.getString("id")}: expected ${vector.getInt("expected_digit")}, " +
                        "got ${inference.digit}."
                }
                val expectedLogits = vector.getJSONArray("expected_logits")
                check(expectedLogits.length() == inference.logits.size)
                repeat(expectedLogits.length()) { logitIndex ->
                    val difference = abs(
                        inference.logits[logitIndex] - expectedLogits.getDouble(logitIndex),
                    )
                    check(difference <= contract.referenceAbsoluteTolerance) {
                        "${vector.getString("id")}: logit $logitIndex differs by $difference."
                    }
                }
            }
        }
    }

    private fun verifyTop1465Fixture() {
        val bitmap = instrumentation.context.assets.open("top1465-0001.png").use { stream ->
            checkNotNull(BitmapFactory.decodeStream(stream))
        }
        val image = AndroidSudokuImage(
            bitmap = bitmap,
            sourceUri = Uri.EMPTY,
            originalWidth = bitmap.width,
            originalHeight = bitmap.height,
            sampleSize = 1,
        )
        val result = image.use {
            AndroidSudokuRecognitionRepository(targetAssets).recognize(it)
        }
        check(result is SudokuPhotoPickResult.Recognized) { "OCR failed for top1465 #0001: $result" }
        val actual = result.grid.values().joinToString("") { value -> value?.toString() ?: "0" }
        check(actual == ExpectedTop1465Grid) {
            "top1465 #0001 mismatch. Expected $ExpectedTop1465Grid, got $actual."
        }
        check(result.grid.cells.count { it.value != null } == 18)
        result.grid.cells.forEach { cell ->
            if (cell.value == null) {
                check(!cell.isGiven && cell.confidence == null)
            } else {
                check(cell.isGiven)
                check(cell.source == SudokuCellSource.Ocr)
                val confidence = checkNotNull(cell.confidence)
                check(confidence >= 0.75f)
            }
        }
    }

    private val targetAssets get() = instrumentation.targetContext.assets

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { value -> "%02x".format(value.toInt() and 0xff) }

    private companion object {
        const val ExpectedTop1465Grid =
            "400030000000600800000000001000050090080000600070200000000102700503000040900000000"
    }
}
