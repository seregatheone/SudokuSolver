package pet.project.sudokusolver.recognition

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgproc.Imgproc

data class AndroidImagePoint(
    val x: Double,
    val y: Double,
)

data class AndroidSudokuGeometryDiagnostics(
    val sourceCorners: List<AndroidImagePoint>,
    val boardAreaRatio: Double,
    val confidence: Double,
)

data class AndroidSudokuCellImage(
    val row: Int,
    val column: Int,
    val bitmap: Bitmap,
) {
    init {
        require(row in 0..8 && column in 0..8) { "Sudoku cell coordinates must be in 0..8." }
    }
}

class AndroidSudokuBoardExtraction(
    val normalizedBoard: Bitmap,
    val cells: List<AndroidSudokuCellImage>,
    val diagnostics: AndroidSudokuGeometryDiagnostics,
) : AutoCloseable {
    init {
        require(cells.size == CellCount) { "A normalized Sudoku board must contain 81 cells." }
        require(cells.indices.all { index ->
            cells[index].row == index / GridSize && cells[index].column == index % GridSize
        }) { "Sudoku cells must be stored in row-major order." }
    }

    private var closed = false

    override fun close() {
        if (closed) return
        closed = true
        cells.forEach { cell ->
            if (!cell.bitmap.isRecycled) cell.bitmap.recycle()
        }
        if (!normalizedBoard.isRecycled) normalizedBoard.recycle()
    }

    private companion object {
        const val GridSize = 9
        const val CellCount = GridSize * GridSize
    }
}

sealed interface AndroidSudokuBoardExtractionResult {
    data class Extracted(val board: AndroidSudokuBoardExtraction) : AndroidSudokuBoardExtractionResult
    data class Failed(val failure: AndroidSudokuBoardFailure) : AndroidSudokuBoardExtractionResult
}

enum class AndroidSudokuBoardFailure {
    NativeRuntimeUnavailable,
    ImageTooSmall,
    BoardNotFound,
    InvalidGeometry,
    ProcessingFailed,
}

/**
 * Accepts only images already decoded by Android into [Bitmap]. Keep encoded-image decoding outside
 * OpenCV while 4.11.0 is pinned; its JP2/J2K decoder is affected by CVE-2025-53644.
 */
class AndroidSudokuBoardExtractor internal constructor(
    private val detector: SudokuBoardDetector,
    private val perspectiveCorrector: PerspectiveCorrector,
    private val cellExtractor: SudokuCellExtractor,
) {
    constructor() : this(
        detector = SudokuBoardDetector(),
        perspectiveCorrector = PerspectiveCorrector(),
        cellExtractor = SudokuCellExtractor(),
    )

    fun extract(bitmap: Bitmap): AndroidSudokuBoardExtractionResult {
        if (!OpenCvRuntime.ensureLoaded()) {
            return AndroidSudokuBoardExtractionResult.Failed(
                AndroidSudokuBoardFailure.NativeRuntimeUnavailable,
            )
        }
        if (
            bitmap.isRecycled ||
            bitmap.width < MinimumImageDimension ||
            bitmap.height < MinimumImageDimension
        ) {
            return AndroidSudokuBoardExtractionResult.Failed(AndroidSudokuBoardFailure.ImageTooSmall)
        }

        var source: Mat? = null
        var normalizedBoard: Mat? = null
        var cellMats: List<Mat> = emptyList()

        return try {
            val sourcePixels = bitmap.toRgbaMat()
            source = sourcePixels
            val contour = detector.detect(sourcePixels)
                ?: return AndroidSudokuBoardExtractionResult.Failed(
                    AndroidSudokuBoardFailure.BoardNotFound,
                )
            val correctedBoard = perspectiveCorrector.correct(sourcePixels, contour)
            normalizedBoard = correctedBoard
            cellMats = cellExtractor.extract(correctedBoard)
                ?: return AndroidSudokuBoardExtractionResult.Failed(
                    AndroidSudokuBoardFailure.InvalidGeometry,
                )

            val boardBitmap = correctedBoard.toBitmap()
            val cellBitmaps = mutableListOf<AndroidSudokuCellImage>()
            try {
                cellMats.forEachIndexed { index, cell ->
                    cellBitmaps += AndroidSudokuCellImage(
                        row = index / GridSize,
                        column = index % GridSize,
                        bitmap = cell.toBitmap(),
                    )
                }
                AndroidSudokuBoardExtractionResult.Extracted(
                    AndroidSudokuBoardExtraction(
                        normalizedBoard = boardBitmap,
                        cells = cellBitmaps,
                        diagnostics = AndroidSudokuGeometryDiagnostics(
                            sourceCorners = contour.corners.map { point ->
                                AndroidImagePoint(point.x, point.y)
                            },
                            boardAreaRatio = contour.areaRatio,
                            confidence = contour.confidence,
                        ),
                    ),
                )
            } catch (error: Throwable) {
                cellBitmaps.forEach { cell -> cell.bitmap.recycle() }
                boardBitmap.recycle()
                throw error
            }
        } catch (_: Exception) {
            AndroidSudokuBoardExtractionResult.Failed(AndroidSudokuBoardFailure.ProcessingFailed)
        } catch (_: UnsatisfiedLinkError) {
            AndroidSudokuBoardExtractionResult.Failed(
                AndroidSudokuBoardFailure.NativeRuntimeUnavailable,
            )
        } catch (_: OutOfMemoryError) {
            AndroidSudokuBoardExtractionResult.Failed(AndroidSudokuBoardFailure.ProcessingFailed)
        } finally {
            cellMats.forEach(Mat::release)
            normalizedBoard?.release()
            source?.release()
        }
    }

    private companion object {
        const val GridSize = 9
        const val MinimumImageDimension = 90
    }
}

internal object OpenCvRuntime {
    @Volatile
    private var loaded: Boolean? = null

    @Synchronized
    fun ensureLoaded(): Boolean {
        loaded?.let { return it }
        return runCatching(OpenCVLoader::initLocal)
            .getOrDefault(false)
            .also { loaded = it }
    }
}

internal data class BoardContour(
    val corners: List<Point>,
    val areaRatio: Double,
    val confidence: Double,
)

internal class SudokuBoardDetector {
    fun detect(source: Mat): BoardContour? {
        if (source.empty()) return null

        val gray = Mat()
        val blurred = Mat()
        val threshold = Mat()
        val edges = Mat()
        val candidates = mutableListOf<Quadrilateral>()

        return try {
            when (source.channels()) {
                4 -> Imgproc.cvtColor(source, gray, Imgproc.COLOR_RGBA2GRAY)
                3 -> Imgproc.cvtColor(source, gray, Imgproc.COLOR_BGR2GRAY)
                1 -> source.copyTo(gray)
                else -> return null
            }
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

            listOf(Imgproc.THRESH_BINARY_INV, Imgproc.THRESH_BINARY).forEach { thresholdType ->
                Imgproc.adaptiveThreshold(
                    blurred,
                    threshold,
                    255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    thresholdType,
                    11,
                    2.0,
                )
                candidates += findQuadrilaterals(threshold, source.width(), source.height())
            }

            Imgproc.Canny(blurred, edges, 50.0, 150.0)
            candidates += findQuadrilaterals(edges, source.width(), source.height())

            val distinctCandidates = candidates.deduplicated()
            distinctCandidates.maxByOrNull { candidate ->
                candidate.score * consensusWeight(candidate, distinctCandidates)
            }?.let { best ->
                val refinedCorners = consensusCorners(gray, best, distinctCandidates)
                BoardContour(
                    corners = refinedCorners,
                    areaRatio = best.areaRatio,
                    confidence = best.confidence,
                )
            }
        } finally {
            gray.release()
            blurred.release()
            threshold.release()
            edges.release()
        }
    }

    private fun findQuadrilaterals(mask: Mat, width: Int, height: Int): List<Quadrilateral> {
        val contourInput = mask.clone()
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        val imageArea = width.toDouble() * height.toDouble()

        return try {
            Imgproc.findContours(
                contourInput,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE,
            )
            contours.flatMap { contour -> contour.toQuadrilaterals(imageArea) }
        } finally {
            contourInput.release()
            hierarchy.release()
            contours.forEach(Mat::release)
        }
    }

    private fun MatOfPoint.toQuadrilaterals(imageArea: Double): List<Quadrilateral> {
        val contourPoints = toArray()
        if (contourPoints.size < CornerCount) return emptyList()

        val contour2f = MatOfPoint2f(*contourPoints)
        val hullIndices = MatOfInt()
        val hull2f = MatOfPoint2f()

        return try {
            Imgproc.convexHull(this, hullIndices)
            hull2f.fromArray(
                *hullIndices.toArray().map { index -> contourPoints[index] }.toTypedArray(),
            )
            buildList {
                contour2f.toQuadrilateral(imageArea, ApproximationRatio)?.let(::add)
                hull2f.toQuadrilateral(imageArea, HullApproximationRatio)?.let { hullCandidate ->
                    if (none { candidate ->
                            meanCornerDistance(candidate.points, hullCandidate.points) <=
                                DuplicateCornerDistance
                        }
                    ) {
                        add(hullCandidate)
                    }
                }
            }
        } finally {
            contour2f.release()
            hullIndices.release()
            hull2f.release()
        }
    }

    private fun MatOfPoint2f.toQuadrilateral(
        imageArea: Double,
        approximationRatio: Double,
    ): Quadrilateral? {
        val approximation = MatOfPoint2f()

        return try {
            val perimeter = Imgproc.arcLength(this, true)
            if (perimeter <= 0.0) return null
            Imgproc.approxPolyDP(this, approximation, perimeter * approximationRatio, true)
            val points = approximation.toArray()
            if (points.size != CornerCount) return null

            val convexContour = MatOfPoint(*points)
            val convex = try {
                Imgproc.isContourConvex(convexContour)
            } finally {
                convexContour.release()
            }
            if (!convex) return null

            val ordered = orderCorners(points.toList())
            val area = abs(Imgproc.contourArea(approximation))
            val areaRatio = area / imageArea
            if (areaRatio !in MinimumBoardAreaRatio..MaximumBoardAreaRatio) return null

            val edgeLengths = ordered.indices.map { index ->
                distance(ordered[index], ordered[(index + 1) % CornerCount])
            }
            val shortestEdge = edgeLengths.minOrNull() ?: return null
            val longestEdge = edgeLengths.maxOrNull() ?: return null
            if (shortestEdge < MinimumEdgeLength || longestEdge <= 0.0) return null

            val shapeScore = shortestEdge / longestEdge
            val confidence = (areaRatio / PreferredBoardAreaRatio)
                .coerceIn(0.0, 1.0) * shapeScore
            Quadrilateral(
                points = ordered,
                areaRatio = areaRatio,
                confidence = confidence,
                score = area * (0.5 + confidence / 2.0),
            )
        } finally {
            approximation.release()
        }
    }

    private fun List<Quadrilateral>.deduplicated(): List<Quadrilateral> =
        fold(mutableListOf()) { distinct, candidate ->
            val duplicateIndex = distinct.indexOfFirst { existing ->
                meanCornerDistance(existing.points, candidate.points) <= DuplicateCornerDistance
            }
            when {
                duplicateIndex < 0 -> distinct += candidate
                candidate.score > distinct[duplicateIndex].score -> distinct[duplicateIndex] = candidate
            }
            distinct
        }

    private fun consensusWeight(
        candidate: Quadrilateral,
        candidates: List<Quadrilateral>,
    ): Double {
        val support = candidates.count { neighbor ->
            abs(neighbor.areaRatio - candidate.areaRatio) <= ConsensusAreaRatioTolerance &&
                meanCornerDistance(neighbor.points, candidate.points) <= ConsensusCornerDistance
        }
        return 1.0 + (support - 1).coerceAtLeast(0) * ConsensusSupportBonus
    }

    private fun orderCorners(points: List<Point>): List<Point> {
        val byVerticalPosition = points.sortedWith(compareBy<Point> { it.y }.thenBy { it.x })
        val top = byVerticalPosition.take(2).sortedBy { it.x }
        val bottom = byVerticalPosition.takeLast(2).sortedBy { it.x }
        return listOf(top[0], top[1], bottom[1], bottom[0])
    }

    private fun refineCorners(gray: Mat, points: List<Point>): List<Point> {
        val corners = MatOfPoint2f(*points.toTypedArray())
        return try {
            Imgproc.cornerSubPix(
                gray,
                corners,
                Size(CornerRefinementRadius, CornerRefinementRadius),
                Size(-1.0, -1.0),
                TermCriteria(
                    TermCriteria.COUNT + TermCriteria.EPS,
                    CornerRefinementIterations,
                    CornerRefinementEpsilon,
                ),
            )
            orderCorners(corners.toArray().toList())
        } catch (_: Exception) {
            points
        } finally {
            corners.release()
        }
    }

    private fun consensusCorners(
        gray: Mat,
        best: Quadrilateral,
        candidates: List<Quadrilateral>,
    ): List<Point> {
        val neighbors = candidates.filter { candidate ->
            abs(candidate.areaRatio - best.areaRatio) <= ConsensusAreaRatioTolerance &&
                meanCornerDistance(candidate.points, best.points) <= ConsensusCornerDistance
        }
        val refined = neighbors.map { candidate -> refineCorners(gray, candidate.points) }
        if (refined.size < 2) return refineCorners(gray, best.points)

        return best.points.indices.map { cornerIndex ->
            Point(
                median(refined.map { corners -> corners[cornerIndex].x }),
                median(refined.map { corners -> corners[cornerIndex].y }),
            )
        }
    }

    private fun meanCornerDistance(first: List<Point>, second: List<Point>): Double =
        first.indices.map { index -> distance(first[index], second[index]) }.average()

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun distance(first: Point, second: Point): Double =
        hypot(first.x - second.x, first.y - second.y)

    private data class Quadrilateral(
        val points: List<Point>,
        val areaRatio: Double,
        val confidence: Double,
        val score: Double,
    )

    private companion object {
        const val CornerCount = 4
        const val ApproximationRatio = 0.02
        const val HullApproximationRatio = 0.01
        const val MinimumBoardAreaRatio = 0.05
        const val MaximumBoardAreaRatio = 0.90
        const val PreferredBoardAreaRatio = 0.25
        const val MinimumEdgeLength = 48.0
        const val CornerRefinementRadius = 4.0
        const val CornerRefinementIterations = 30
        const val CornerRefinementEpsilon = 0.01
        const val ConsensusAreaRatioTolerance = 0.025
        const val ConsensusCornerDistance = 16.0
        const val DuplicateCornerDistance = 0.5
        const val ConsensusSupportBonus = 0.25
    }
}

internal class PerspectiveCorrector(
    private val outputSize: Int = 900,
) {
    init {
        require(outputSize >= 9) { "Normalized board size must fit the Sudoku grid." }
    }

    fun correct(source: Mat, contour: BoardContour): Mat {
        val destination = Mat(outputSize, outputSize, source.type())
        val sourceCorners = MatOfPoint2f(*contour.corners.toTypedArray())
        val lastPixel = (outputSize - 1).toDouble()
        val targetCorners = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(lastPixel, 0.0),
            Point(lastPixel, lastPixel),
            Point(0.0, lastPixel),
        )
        val transform = Imgproc.getPerspectiveTransform(sourceCorners, targetCorners)

        return try {
            Imgproc.warpPerspective(
                source,
                destination,
                transform,
                Size(outputSize.toDouble(), outputSize.toDouble()),
            )
            destination
        } catch (error: Throwable) {
            destination.release()
            throw error
        } finally {
            sourceCorners.release()
            targetCorners.release()
            transform.release()
        }
    }
}

internal class SudokuCellExtractor(
    private val marginRatio: Double = 0.10,
) {
    init {
        require(marginRatio in 0.0..<0.45) { "Cell margin must preserve the cell center." }
    }

    fun extract(board: Mat): List<Mat>? {
        if (board.empty() || board.width() < GridSize || board.height() < GridSize) return null

        val cellWidth = board.width() / GridSize.toDouble()
        val cellHeight = board.height() / GridSize.toDouble()
        val marginX = (cellWidth * marginRatio).roundToInt()
        val marginY = (cellHeight * marginRatio).roundToInt()
        val cells = mutableListOf<Mat>()

        try {
            for (row in 0 until GridSize) {
                for (column in 0 until GridSize) {
                    val left = (column * cellWidth).roundToInt() + marginX
                    val top = (row * cellHeight).roundToInt() + marginY
                    val right = min(
                        ((column + 1) * cellWidth).roundToInt() - marginX,
                        board.width(),
                    )
                    val bottom = min(
                        ((row + 1) * cellHeight).roundToInt() - marginY,
                        board.height(),
                    )
                    if (right <= left || bottom <= top) return null
                    cells += board.submat(Rect(left, top, right - left, bottom - top)).clone()
                }
            }
            return cells
        } finally {
            if (cells.size != CellCount) cells.forEach(Mat::release)
        }
    }

    private companion object {
        const val GridSize = 9
        const val CellCount = GridSize * GridSize
    }
}

private fun Mat.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888)
    return try {
        Utils.matToBitmap(this, bitmap)
        bitmap
    } catch (error: Throwable) {
        bitmap.recycle()
        throw error
    }
}

private fun Bitmap.toRgbaMat(): Mat =
    Mat().also { matrix ->
        Utils.bitmapToMat(this, matrix)
    }
