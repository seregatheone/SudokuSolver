package pet.project.sudokusolver.recognition

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Color
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import org.json.JSONException
import org.json.JSONObject
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickFailure
import pet.project.sudokusolver.domain.SudokuCell
import pet.project.sudokusolver.domain.SudokuCellSource
import pet.project.sudokusolver.domain.SudokuGrid

internal class AndroidSudokuOcrException(
    val failure: SudokuPhotoPickFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

internal data class AndroidSudokuRawInference(
    val digit: Int,
    val confidence: Float,
    val logits: FloatArray,
)

internal data class AndroidSudokuOcrContract(
    val modelFile: String,
    val modelSha256: String,
    val modelSizeBytes: Int,
    val referenceVectorsFile: String,
    val referenceVectorsSha256: String,
    val referenceVectorsSizeBytes: Int,
    val referenceAbsoluteTolerance: Double,
    val inputWidth: Int,
    val inputHeight: Int,
    val outputDigits: IntArray,
    val minimumConfidence: Float,
    val backgroundBorderRatio: Double,
    val minimumForegroundThreshold: Double,
    val clearedBorderPixels: Int,
    val minimumComponentAreaRatio: Double,
    val minimumComponentAreaPixels: Int,
    val digitScoreThreshold: Double,
) {
    companion object {
        const val AssetRoot = "sudoku_ocr"

        fun load(assetManager: AssetManager): AndroidSudokuOcrContract {
            val manifestJson = try {
                assetManager.open("$AssetRoot/manifest.json").bufferedReader().use { reader ->
                    reader.readText()
                }
            } catch (error: IOException) {
                throw AndroidSudokuOcrException(
                    SudokuPhotoPickFailure.ModelUnavailable,
                    "Sudoku OCR manifest is unavailable.",
                    error,
                )
            } catch (error: Exception) {
                throw AndroidSudokuOcrException(
                    SudokuPhotoPickFailure.ModelIncompatible,
                    "Sudoku OCR manifest cannot be read.",
                    error,
                )
            }

            return parse(manifestJson)
        }

        internal fun parse(manifestJson: String): AndroidSudokuOcrContract = try {
            JSONObject(manifestJson).parseAndValidate()
        } catch (error: AndroidSudokuOcrException) {
            throw error
        } catch (error: JSONException) {
            throw AndroidSudokuOcrException(
                SudokuPhotoPickFailure.ModelIncompatible,
                "Sudoku OCR manifest has an invalid schema.",
                error,
            )
        } catch (error: IllegalArgumentException) {
            throw AndroidSudokuOcrException(
                SudokuPhotoPickFailure.ModelIncompatible,
                error.message ?: "Sudoku OCR manifest is incompatible.",
                error,
            )
        }

        private fun JSONObject.parseAndValidate(): AndroidSudokuOcrContract {
            require(getString("bundle_id") == "sudoku-cell-digit-classifier") {
                "Unsupported Sudoku OCR bundle."
            }
            require(getString("schema_version") == SupportedContractVersion) {
                "Unsupported Sudoku OCR contract version."
            }
            require(getString("model_version") == SupportedModelVersion) {
                "Unsupported Sudoku OCR model version."
            }

            val model = getJSONObject("model")
            require(model.getString("format") == "tflite") { "Unsupported OCR model format." }
            require(model.getString("operator_set") == "TFLITE_BUILTINS") {
                "Unsupported OCR operator set."
            }

            val input = getJSONObject("input")
            require(input.getString("dtype") == "float32") { "Unsupported OCR input type." }
            require(input.getString("layout") == "NHWC") { "Unsupported OCR input layout." }
            require(input.getString("foreground") == "white_on_black") {
                "Unsupported OCR foreground convention."
            }
            val inputShape = input.getJSONArray("shape").toIntArray()
            require(inputShape.contentEquals(intArrayOf(1, 28, 28, 1))) {
                "Unsupported OCR input shape."
            }
            val resize = input.getJSONObject("resize")
            require(resize.getString("interpolation") == "area") {
                "Unsupported OCR resize interpolation."
            }
            require(resize.getInt("width") == 28 && resize.getInt("height") == 28) {
                "Unsupported OCR resize dimensions."
            }
            val producer = input.getJSONObject("producer")
            require(producer.getString("contract") == "opencv_cell_foreground_v1") {
                "Unsupported OCR preprocessing contract."
            }
            require(producer.getString("background_statistic") == "per_channel_median_rgb") {
                "Unsupported OCR background statistic."
            }
            require(
                producer.getString("distance_metric") ==
                    "max_absolute_rgb_channel_difference",
            ) { "Unsupported OCR foreground distance metric." }
            require(producer.getString("threshold_method") == "otsu_with_minimum") {
                "Unsupported OCR threshold method."
            }
            require(producer.getDouble("cell_border_inset_ratio") == 0.1) {
                "Unsupported OCR cell inset."
            }
            require(producer.getString("threshold_comparison") == "distance >= max(otsu, minimum)") {
                "Unsupported OCR threshold comparison."
            }

            val output = getJSONObject("output")
            require(output.getString("dtype") == "float32") { "Unsupported OCR output type." }
            require(output.getString("semantics") == "logits") { "Unsupported OCR output." }
            require(output.getJSONArray("shape").toIntArray().contentEquals(intArrayOf(1, 9))) {
                "Unsupported OCR output shape."
            }
            val digits = output.getJSONArray("class_mapping").toIntArray()
            require(digits.contentEquals((1..9).toList().toIntArray())) {
                "Unsupported OCR class mapping."
            }
            val confidence = output.getJSONObject("confidence")
            require(confidence.getString("transform") == "softmax") {
                "Unsupported OCR confidence transform."
            }

            val emptyPolicy = getJSONObject("empty_policy")
            require(!emptyPolicy.getBoolean("classifier_includes_empty")) {
                "OCR classifier must not include an empty class."
            }
            require(emptyPolicy.getInt("component_connectivity") == 8) {
                "Unsupported OCR component connectivity."
            }
            require(emptyPolicy.getString("detector") == "opencv_connected_components_v1") {
                "Unsupported OCR empty-cell detector."
            }
            require(emptyPolicy.getString("comparison") == "digit_score < threshold => empty") {
                "Unsupported OCR empty-cell comparison."
            }
            require(
                emptyPolicy.getString("minimum_component_area_formula") ==
                    "max(3, round_ties_to_even(content_area * 0.0015))",
            ) { "Unsupported OCR component-area formula." }
            require(
                emptyPolicy.getString("digit_score_formula") ==
                    "max_components(min(width_ratio / 0.08, height_ratio / 0.34, " +
                    "area_ratio / 0.012, 1.0))",
            ) { "Unsupported OCR digit-score formula." }

            val referenceVectors = getJSONObject("reference_vectors")
            require(referenceVectors.getInt("count") == 9) {
                "OCR reference vectors must cover all digit classes."
            }
            require(referenceVectors.getString("input_stage") == "normalized_foreground_mask") {
                "Unsupported OCR reference-vector input stage."
            }
            val contract = AndroidSudokuOcrContract(
                modelFile = model.getSafeAssetFile("file"),
                modelSha256 = model.getSha256("sha256"),
                modelSizeBytes = model.getPositiveInt("size_bytes"),
                referenceVectorsFile = referenceVectors.getSafeAssetFile("file"),
                referenceVectorsSha256 = referenceVectors.getSha256("sha256"),
                referenceVectorsSizeBytes = referenceVectors.getPositiveInt("size_bytes"),
                referenceAbsoluteTolerance = referenceVectors.getDouble("absolute_tolerance"),
                inputWidth = inputShape[2],
                inputHeight = inputShape[1],
                outputDigits = digits,
                minimumConfidence = confidence.getDouble("minimum").toFloat(),
                backgroundBorderRatio = producer.getDouble("background_border_ratio"),
                minimumForegroundThreshold = producer.getDouble("minimum_threshold"),
                clearedBorderPixels = producer.getInt("border_clear_pixels"),
                minimumComponentAreaRatio = 0.0015,
                minimumComponentAreaPixels = 3,
                digitScoreThreshold = emptyPolicy.getDouble("digit_score_threshold"),
            )
            require(contract.referenceAbsoluteTolerance > 0.0) {
                "OCR reference tolerance must be positive."
            }
            require(contract.minimumConfidence in 0f..1f) {
                "OCR confidence threshold must be between 0 and 1."
            }
            require(contract.backgroundBorderRatio in 0.0..0.25) {
                "OCR background border ratio is invalid."
            }
            require(contract.minimumForegroundThreshold in 0.0..255.0) {
                "OCR foreground threshold is invalid."
            }
            require(contract.clearedBorderPixels in 0..4) { "OCR cleared border is invalid." }
            require(contract.digitScoreThreshold in 0.0..1.0) {
                "OCR empty-cell threshold is invalid."
            }
            return contract
        }

        private fun org.json.JSONArray.toIntArray(): IntArray =
            IntArray(length()) { index -> getInt(index) }

        private fun JSONObject.getSafeAssetFile(name: String): String =
            getString(name).also { file ->
                require(file.isNotBlank() && file == file.substringAfterLast('/')) {
                    "OCR asset filename is invalid."
                }
            }

        private fun JSONObject.getSha256(name: String): String = getString(name).also { value ->
            require(Sha256Regex.matches(value)) { "OCR asset checksum is invalid." }
        }

        private fun JSONObject.getPositiveInt(name: String): Int = getInt(name).also { value ->
            require(value > 0) { "OCR asset size must be positive." }
        }

        private const val SupportedContractVersion = "1.0.0"
        private const val SupportedModelVersion = "1.1.0"
        private val Sha256Regex = Regex("[0-9a-f]{64}")
    }
}

internal class AndroidSudokuDigitRecognizer private constructor(
    private val interpreter: Interpreter,
    internal val contract: AndroidSudokuOcrContract,
    private val preprocessor: AndroidSudokuCellPreprocessor = AndroidSudokuCellPreprocessor(contract),
) : AutoCloseable {
    fun recognize(cells: List<AndroidSudokuCellImage>): SudokuGrid {
        if (cells.size != SudokuGrid.CellCount) {
            throw AndroidSudokuOcrException(
                SudokuPhotoPickFailure.InferenceFailed,
                "Sudoku OCR requires exactly 81 cells.",
            )
        }

        val recognizedCells = cells.mapIndexed { index, cell ->
            if (cell.row != index / SudokuGrid.Size || cell.column != index % SudokuGrid.Size) {
                throw AndroidSudokuOcrException(
                    SudokuPhotoPickFailure.InferenceFailed,
                    "Sudoku OCR cells are not in row-major order.",
                )
            }
            val preprocessed = preprocessor.preprocess(cell.bitmap)
            if (preprocessed.isEmpty) {
                SudokuCell()
            } else {
                val inference = runNormalizedInput(preprocessed.normalizedForeground)
                if (inference.confidence < contract.minimumConfidence) {
                    throw AndroidSudokuOcrException(
                        SudokuPhotoPickFailure.InferenceFailed,
                        "Sudoku OCR confidence at r${cell.row + 1}c${cell.column + 1} " +
                            "is ${inference.confidence} for digit ${inference.digit}, below " +
                            "${contract.minimumConfidence}; digit score is " +
                            "${preprocessed.digitScore}.",
                    )
                }
                SudokuCell(
                    value = inference.digit,
                    isGiven = true,
                    confidence = inference.confidence,
                    source = SudokuCellSource.Ocr,
                )
            }
        }

        val grid = SudokuGrid(recognizedCells)
        grid.cells.forEachIndexed { index, cell ->
            val value = cell.value ?: return@forEachIndexed
            if (grid.conflictFor(index / SudokuGrid.Size, index % SudokuGrid.Size, value) != null) {
                throw AndroidSudokuOcrException(
                    SudokuPhotoPickFailure.InferenceFailed,
                    "Sudoku OCR produced conflicting digit $value at " +
                        "r${index / SudokuGrid.Size + 1}c${index % SudokuGrid.Size + 1}.",
                )
            }
        }
        return grid
    }

    internal fun runNormalizedInput(normalizedForeground: ByteArray): AndroidSudokuRawInference {
        val expectedPixels = contract.inputWidth * contract.inputHeight
        if (normalizedForeground.size != expectedPixels) {
            throw AndroidSudokuOcrException(
                SudokuPhotoPickFailure.InferenceFailed,
                "Sudoku OCR normalized input has an invalid size.",
            )
        }
        val input = ByteBuffer.allocateDirect(expectedPixels * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        normalizedForeground.forEach { value ->
            input.putFloat((value.toInt() and 0xff) / 255f)
        }
        input.rewind()
        val output = Array(1) { FloatArray(contract.outputDigits.size) }
        try {
            interpreter.run(input, output)
        } catch (error: Exception) {
            throw AndroidSudokuOcrException(
                SudokuPhotoPickFailure.InferenceFailed,
                "Sudoku OCR inference failed.",
                error,
            )
        }
        val logits = output.single()
        if (logits.any { !it.isFinite() }) {
            throw AndroidSudokuOcrException(
                SudokuPhotoPickFailure.InferenceFailed,
                "Sudoku OCR returned non-finite logits.",
            )
        }
        val probabilities = logits.softmax()
        val classIndex = probabilities.indices.maxBy { probabilities[it] }
        return AndroidSudokuRawInference(
            digit = contract.outputDigits[classIndex],
            confidence = probabilities[classIndex],
            logits = logits.copyOf(),
        )
    }

    override fun close() {
        interpreter.close()
    }

    companion object {
        fun create(assetManager: AssetManager): AndroidSudokuDigitRecognizer {
            val contract = AndroidSudokuOcrContract.load(assetManager)
            val modelBytes = assetManager.readVerifiedAsset(
                file = contract.modelFile,
                expectedSize = contract.modelSizeBytes,
                expectedSha256 = contract.modelSha256,
            )
            val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
                .order(ByteOrder.nativeOrder())
                .put(modelBytes)
                .apply { rewind() }
            val interpreter = try {
                Interpreter(
                    modelBuffer,
                    Interpreter.Options().setNumThreads(
                        Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
                    ),
                )
            } catch (error: Exception) {
                throw AndroidSudokuOcrException(
                    SudokuPhotoPickFailure.ModelIncompatible,
                    "Sudoku OCR model cannot be loaded by LiteRT.",
                    error,
                )
            } catch (error: UnsatisfiedLinkError) {
                throw AndroidSudokuOcrException(
                    SudokuPhotoPickFailure.ModelUnavailable,
                    "LiteRT native runtime is unavailable.",
                    error,
                )
            }

            try {
                require(interpreter.inputTensorCount == 1 && interpreter.outputTensorCount == 1) {
                    "Sudoku OCR model must contain one input and one output."
                }
                val input = interpreter.getInputTensor(0)
                require(input.dataType() == DataType.FLOAT32) { "OCR input tensor type is invalid." }
                require(
                    input.shape().contentEquals(
                        intArrayOf(1, contract.inputHeight, contract.inputWidth, 1),
                    ),
                ) { "OCR input tensor shape is invalid." }
                val output = interpreter.getOutputTensor(0)
                require(output.dataType() == DataType.FLOAT32) { "OCR output tensor type is invalid." }
                require(output.shape().contentEquals(intArrayOf(1, contract.outputDigits.size))) {
                    "OCR output tensor shape is invalid."
                }
                return AndroidSudokuDigitRecognizer(interpreter, contract)
            } catch (error: Exception) {
                interpreter.close()
                throw AndroidSudokuOcrException(
                    SudokuPhotoPickFailure.ModelIncompatible,
                    error.message ?: "Sudoku OCR tensors are incompatible.",
                    error,
                )
            }
        }
    }
}

internal data class AndroidSudokuPreprocessedCell(
    val normalizedForeground: ByteArray,
    val digitScore: Double,
    val isEmpty: Boolean,
)

internal class AndroidSudokuCellPreprocessor(
    private val contract: AndroidSudokuOcrContract,
) {
    fun preprocess(bitmap: Bitmap): AndroidSudokuPreprocessedCell {
        if (bitmap.isRecycled || bitmap.width < 3 || bitmap.height < 3) {
            throw AndroidSudokuOcrException(
                SudokuPhotoPickFailure.InferenceFailed,
                "Sudoku OCR cell bitmap is invalid.",
            )
        }
        if (!OpenCvRuntime.ensureLoaded()) {
            throw AndroidSudokuOcrException(
                SudokuPhotoPickFailure.NativeProcessingUnavailable,
                "OpenCV is unavailable for Sudoku OCR preprocessing.",
            )
        }

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val border = max(
            1,
            round(min(width, height) * contract.backgroundBorderRatio).toInt(),
        ).coerceAtMost((min(width, height) - 1) / 2)
        val background = borderMedianRgb(pixels, width, height, border)
        val distances = FloatArray(pixels.size)
        val distanceBytes = ByteArray(pixels.size)
        pixels.forEachIndexed { index, color ->
            val distance = max(
                abs(Color.red(color) - background[0]),
                max(
                    abs(Color.green(color) - background[1]),
                    abs(Color.blue(color) - background[2]),
                ),
            )
            distances[index] = distance.toFloat()
            distanceBytes[index] = distance.toInt().coerceIn(0, 255).toByte()
        }
        val otsuThreshold = otsuThreshold(distanceBytes, width, height)
        val threshold = max(contract.minimumForegroundThreshold, otsuThreshold)
        val mask = ByteArray(pixels.size) { index ->
            if (distances[index] >= threshold) 0xff.toByte() else 0
        }
        clearBorder(mask, width, height, contract.clearedBorderPixels)
        val digitScore = digitScore(mask, width, height)
        val normalized = resizeMask(mask, width, height)
        return AndroidSudokuPreprocessedCell(
            normalizedForeground = normalized,
            digitScore = digitScore,
            isEmpty = digitScore < contract.digitScoreThreshold,
        )
    }

    private fun borderMedianRgb(
        pixels: IntArray,
        width: Int,
        height: Int,
        border: Int,
    ): DoubleArray {
        val borderPixelCount = 2 * border * width + 2 * border * (height - 2 * border)
        val red = IntArray(borderPixelCount)
        val green = IntArray(borderPixelCount)
        val blue = IntArray(borderPixelCount)
        var outputIndex = 0

        fun appendPixel(color: Int) {
            red[outputIndex] = Color.red(color)
            green[outputIndex] = Color.green(color)
            blue[outputIndex] = Color.blue(color)
            outputIndex++
        }

        for (y in 0 until border) for (x in 0 until width) appendPixel(pixels[y * width + x])
        for (y in height - border until height) {
            for (x in 0 until width) appendPixel(pixels[y * width + x])
        }
        for (y in border until height - border) {
            for (x in 0 until border) appendPixel(pixels[y * width + x])
            for (x in width - border until width) appendPixel(pixels[y * width + x])
        }
        check(outputIndex == borderPixelCount)
        return doubleArrayOf(red.median(), green.median(), blue.median())
    }

    private fun IntArray.median(): Double {
        sort()
        val middle = size / 2
        return if (size % 2 == 0) {
            (this[middle - 1] + this[middle]) / 2.0
        } else {
            this[middle].toDouble()
        }
    }

    private fun otsuThreshold(values: ByteArray, width: Int, height: Int): Double {
        val source = Mat(height, width, CvType.CV_8UC1)
        val destination = Mat()
        return try {
            source.put(0, 0, values)
            Imgproc.threshold(
                source,
                destination,
                0.0,
                255.0,
                Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU,
            )
        } finally {
            destination.release()
            source.release()
        }
    }

    private fun clearBorder(mask: ByteArray, width: Int, height: Int, border: Int) {
        repeat(border.coerceAtMost(min(width, height) / 2)) { offset ->
            for (x in 0 until width) {
                mask[offset * width + x] = 0
                mask[(height - 1 - offset) * width + x] = 0
            }
            for (y in 0 until height) {
                mask[y * width + offset] = 0
                mask[y * width + width - 1 - offset] = 0
            }
        }
    }

    private fun digitScore(mask: ByteArray, width: Int, height: Int): Double {
        val visited = BooleanArray(mask.size)
        val minimumArea = max(
            contract.minimumComponentAreaPixels,
            round(width * height * contract.minimumComponentAreaRatio).toInt(),
        )
        var bestScore = 0.0
        val queue = IntArray(mask.size)

        for (start in mask.indices) {
            if (visited[start] || mask[start].toInt() == 0) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true
            var area = 0
            var minX = width
            var maxX = -1
            var minY = height
            var maxY = -1

            while (head < tail) {
                val index = queue[head++]
                val x = index % width
                val y = index / width
                area++
                minX = min(minX, x)
                maxX = max(maxX, x)
                minY = min(minY, y)
                maxY = max(maxY, y)

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nextX = x + dx
                        val nextY = y + dy
                        if (nextX !in 0 until width || nextY !in 0 until height) continue
                        val next = nextY * width + nextX
                        if (!visited[next] && mask[next].toInt() != 0) {
                            visited[next] = true
                            queue[tail++] = next
                        }
                    }
                }
            }

            if (area >= minimumArea) {
                val componentWidth = maxX - minX + 1
                val componentHeight = maxY - minY + 1
                val score = min(
                    componentWidth.toDouble() / width / 0.08,
                    min(
                        componentHeight.toDouble() / height / 0.34,
                        min(area.toDouble() / (width * height) / 0.012, 1.0),
                    ),
                )
                bestScore = max(bestScore, score)
            }
        }
        return bestScore
    }

    private fun resizeMask(mask: ByteArray, width: Int, height: Int): ByteArray {
        val source = Mat(height, width, CvType.CV_8UC1)
        val destination = Mat()
        return try {
            source.put(0, 0, mask)
            Imgproc.resize(
                source,
                destination,
                Size(contract.inputWidth.toDouble(), contract.inputHeight.toDouble()),
                0.0,
                0.0,
                Imgproc.INTER_AREA,
            )
            ByteArray(contract.inputWidth * contract.inputHeight).also { normalized ->
                destination.get(0, 0, normalized)
            }
        } finally {
            destination.release()
            source.release()
        }
    }
}

private fun FloatArray.softmax(): FloatArray {
    val maximum = maxOrNull() ?: error("Softmax requires logits.")
    val exponentials = DoubleArray(size) { index -> exp((this[index] - maximum).toDouble()) }
    val sum = exponentials.sum()
    return FloatArray(size) { index -> (exponentials[index] / sum).toFloat() }
}

private fun AssetManager.readVerifiedAsset(
    file: String,
    expectedSize: Int,
    expectedSha256: String,
): ByteArray {
    val bytes = try {
        open("${AndroidSudokuOcrContract.AssetRoot}/$file").use { stream -> stream.readBytes() }
    } catch (error: Exception) {
        throw AndroidSudokuOcrException(
            SudokuPhotoPickFailure.ModelUnavailable,
            "Sudoku OCR model asset is missing.",
            error,
        )
    }
    return verifySudokuOcrAsset(bytes, expectedSize, expectedSha256)
}

internal fun verifySudokuOcrAsset(
    bytes: ByteArray,
    expectedSize: Int,
    expectedSha256: String,
): ByteArray {
    if (bytes.size != expectedSize) {
        throw AndroidSudokuOcrException(
            SudokuPhotoPickFailure.ModelIntegrityFailed,
            "Sudoku OCR model size does not match its manifest.",
        )
    }
    val actualSha256 = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { value -> "%02x".format(value.toInt() and 0xff) }
    if (actualSha256 != expectedSha256) {
        throw AndroidSudokuOcrException(
            SudokuPhotoPickFailure.ModelIntegrityFailed,
            "Sudoku OCR model checksum does not match its manifest.",
        )
    }
    return bytes
}
