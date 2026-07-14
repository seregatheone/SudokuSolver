package pet.project.sudokusolver.recognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import java.io.File
import kotlin.math.hypot
import kotlin.random.Random

internal class SudokuBoardGeometryChecks {
    fun runAll(externalFixtureRoot: String?): String {
        verifyGeneratedFixtures()
        verifyTypedFailures()
        if (externalFixtureRoot != null) verifyExternalBundle(File(externalFixtureRoot))
        return if (externalFixtureRoot == null) {
            "generated geometry fixtures"
        } else {
            "generated geometry fixtures and bundle 1.0.0"
        }
    }

    private fun verifyGeneratedFixtures() {
        FixtureProfiles.forEach { profile ->
            val bitmap = createGeneratedFixture(profile)
            try {
                val result = AndroidSudokuBoardExtractor().extract(bitmap)
                check(result is AndroidSudokuBoardExtractionResult.Extracted) {
                    "${profile.id} was not detected: $result"
                }
                result.board.use { board ->
                    verifyGeometry(profile, board)
                    verifyRowMajorMarkers(board)
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun verifyTypedFailures() {
        val tiny = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            check(AndroidSudokuBoardExtractor().extract(tiny) ==
                AndroidSudokuBoardExtractionResult.Failed(AndroidSudokuBoardFailure.ImageTooSmall))
        } finally {
            tiny.recycle()
        }

        val blank = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        try {
            check(AndroidSudokuBoardExtractor().extract(blank) ==
                AndroidSudokuBoardExtractionResult.Failed(AndroidSudokuBoardFailure.BoardNotFound))
        } finally {
            blank.recycle()
        }
    }

    private fun verifyExternalBundle(root: File) {
        FixtureProfiles.forEach { profile ->
            val imageFile = File(root, "${profile.id}/image.png")
            check(imageFile.isFile) { "Missing external fixture: $imageFile" }
            val bitmap = checkNotNull(BitmapFactory.decodeFile(imageFile.absolutePath)) {
                "Could not decode external fixture: $imageFile"
            }
            try {
                val result = AndroidSudokuBoardExtractor().extract(bitmap)
                check(result is AndroidSudokuBoardExtractionResult.Extracted) {
                    "External ${profile.id} was not detected: $result"
                }
                result.board.use { board -> verifyGeometry(profile, board) }
            } finally {
                bitmap.recycle()
            }
        }

        val failureImage = File(root, "failure-image-too-small/image.png")
        check(failureImage.isFile) { "Missing external fixture: $failureImage" }
        val bitmap = checkNotNull(BitmapFactory.decodeFile(failureImage.absolutePath))
        try {
            check(AndroidSudokuBoardExtractor().extract(bitmap) ==
                AndroidSudokuBoardExtractionResult.Failed(AndroidSudokuBoardFailure.ImageTooSmall))
        } finally {
            bitmap.recycle()
        }
    }

    private fun verifyGeometry(
        profile: GeometryFixtureProfile,
        board: AndroidSudokuBoardExtraction,
    ) {
        check(board.cells.size == CellCount)
        board.cells.forEachIndexed { index, cell ->
            check(cell.row == index / GridSize && cell.column == index % GridSize) {
                "${profile.id} cell $index is not row-major."
            }
        }

        val errors = board.diagnostics.sourceCorners.zip(profile.corners) { actual, expected ->
            hypot(actual.x - expected.x, actual.y - expected.y)
        }
        val meanError = errors.average()
        val maximumError = errors.maxOrNull() ?: Double.POSITIVE_INFINITY
        check(meanError <= MeanCornerErrorThreshold) {
            "${profile.id} mean corner error $meanError exceeds $MeanCornerErrorThreshold: " +
                board.diagnostics.sourceCorners
        }
        check(maximumError <= MaximumCornerErrorThreshold) {
            "${profile.id} max corner error $maximumError exceeds $MaximumCornerErrorThreshold: " +
                board.diagnostics.sourceCorners
        }
        check(board.diagnostics.confidence > 0.0)
    }

    private fun verifyRowMajorMarkers(board: AndroidSudokuBoardExtraction) {
        board.cells.forEachIndexed { index, cell ->
            val actual = cell.bitmap.getPixel(cell.bitmap.width / 2, cell.bitmap.height / 2)
            val expected = markerColor(index)
            check(colorDistance(actual, expected) <= MarkerColorTolerance) {
                "Cell $index does not contain its row-major marker."
            }
        }
    }

    private fun createGeneratedFixture(profile: GeometryFixtureProfile): Bitmap {
        val result = Bitmap.createBitmap(profile.width, profile.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(profile.backgroundColor)

        val board = createBoard(profile.darkBoard)
        try {
            val source = floatArrayOf(
                0f,
                0f,
                (BoardSize - 1).toFloat(),
                0f,
                (BoardSize - 1).toFloat(),
                (BoardSize - 1).toFloat(),
                0f,
                (BoardSize - 1).toFloat(),
            )
            val destination = profile.corners.flatMap { point ->
                listOf(point.x.toFloat(), point.y.toFloat())
            }.toFloatArray()
            val transform = Matrix()
            check(transform.setPolyToPoly(source, 0, destination, 0, 4))
            canvas.drawBitmap(board, transform, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } finally {
            board.recycle()
        }

        if (profile.noisy) addNoise(result, profile.id.hashCode())
        return result
    }

    private fun createBoard(dark: Boolean): Bitmap {
        val board = Bitmap.createBitmap(BoardSize, BoardSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(board)
        canvas.drawColor(if (dark) DarkBoardColor else LightBoardColor)

        val markerPaint = Paint().apply { style = Paint.Style.FILL }
        repeat(CellCount) { index ->
            val row = index / GridSize
            val column = index % GridSize
            val centerX = column * CellSize + CellSize / 2f
            val centerY = row * CellSize + CellSize / 2f
            markerPaint.color = markerColor(index)
            canvas.drawRect(
                centerX - MarkerRadius,
                centerY - MarkerRadius,
                centerX + MarkerRadius,
                centerY + MarkerRadius,
                markerPaint,
            )
        }

        val gridPaint = Paint().apply {
            color = if (dark) Color.WHITE else Color.BLACK
            style = Paint.Style.STROKE
            isAntiAlias = false
        }
        for (line in 0..GridSize) {
            val coordinate = when (line) {
                0 -> 1f
                GridSize -> (BoardSize - 2).toFloat()
                else -> line * CellSize.toFloat()
            }
            gridPaint.strokeWidth = if (line % 3 == 0) 10f else 4f
            canvas.drawLine(coordinate, 0f, coordinate, BoardSize.toFloat(), gridPaint)
            canvas.drawLine(0f, coordinate, BoardSize.toFloat(), coordinate, gridPaint)
        }
        return board
    }

    private fun addNoise(bitmap: Bitmap, seed: Int) {
        val random = Random(seed)
        repeat(bitmap.width * bitmap.height / 45) {
            val x = random.nextInt(bitmap.width)
            val y = random.nextInt(bitmap.height)
            val original = bitmap.getPixel(x, y)
            val delta = random.nextInt(-45, 46)
            bitmap.setPixel(
                x,
                y,
                Color.rgb(
                    (Color.red(original) + delta).coerceIn(0, 255),
                    (Color.green(original) + delta).coerceIn(0, 255),
                    (Color.blue(original) + delta).coerceIn(0, 255),
                ),
            )
        }
    }

    private fun markerColor(index: Int): Int = Color.rgb(
        32 + (index * 73) % 192,
        32 + (index * 109) % 192,
        32 + (index * 151) % 192,
    )

    private fun colorDistance(first: Int, second: Int): Int =
        kotlin.math.abs(Color.red(first) - Color.red(second)) +
            kotlin.math.abs(Color.green(first) - Color.green(second)) +
            kotlin.math.abs(Color.blue(first) - Color.blue(second))

    private data class GeometryFixtureProfile(
        val id: String,
        val width: Int,
        val height: Int,
        val corners: List<AndroidImagePoint>,
        val backgroundColor: Int,
        val darkBoard: Boolean = false,
        val noisy: Boolean = false,
    )

    private companion object {
        const val GridSize = 9
        const val CellSize = 100
        const val BoardSize = GridSize * CellSize
        const val CellCount = GridSize * GridSize
        const val MarkerRadius = 18f
        const val MarkerColorTolerance = 100
        const val MeanCornerErrorThreshold = 3.0
        const val MaximumCornerErrorThreshold = 6.0
        val LightBoardColor = Color.WHITE
        val DarkBoardColor = 0xFF20252D.toInt()

        val FixtureProfiles = listOf(
            GeometryFixtureProfile(
                id = "plain-light",
                width = 570,
                height = 1280,
                corners = points(7, 317, 562, 317, 562, 872, 7, 872),
                backgroundColor = 0xFFDDE3E8.toInt(),
            ),
            GeometryFixtureProfile(
                id = "noise-light",
                width = 570,
                height = 1280,
                corners = points(7, 317, 562, 317, 562, 872, 7, 872),
                backgroundColor = 0xFFDDE3E8.toInt(),
                noisy = true,
            ),
            GeometryFixtureProfile(
                id = "color-dark",
                width = 570,
                height = 1280,
                corners = points(8, 285, 560, 285, 560, 836, 8, 836),
                backgroundColor = 0xFF173B46.toInt(),
                darkBoard = true,
            ),
            GeometryFixtureProfile(
                id = "perspective-light",
                width = 553,
                height = 1140,
                corners = points(85, 398, 530, 440, 470, 941, 51, 871),
                backgroundColor = 0xFFE2DFD7.toInt(),
            ),
            GeometryFixtureProfile(
                id = "combined-dark",
                width = 560,
                height = 1125,
                corners = points(11, 417, 412, 422, 433, 963, 21, 1012),
                backgroundColor = 0xFF18242D.toInt(),
                darkBoard = true,
                noisy = true,
            ),
        )

        private fun points(vararg coordinates: Int): List<AndroidImagePoint> =
            coordinates.toList().chunked(2).map { pair ->
                AndroidImagePoint(pair[0].toDouble(), pair[1].toDouble())
            }
    }
}
