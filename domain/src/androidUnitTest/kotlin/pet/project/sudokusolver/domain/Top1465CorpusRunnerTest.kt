package pet.project.sudokusolver.domain

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class Top1465CorpusRunnerTest {
    @Test
    fun corpusContractIsPinned() {
        assertEquals(1_465, Top1465CorpusContract.PuzzleCount)
        assertEquals(64, Top1465CorpusContract.Sha256.length)
        assertTrue(Top1465CorpusContract.SourceUrl.startsWith("http://"))
    }

    @Test
    fun runTop1465CorpusWhenEnabled() {
        if (System.getenv(Top1465CorpusContract.RunEnvironmentVariable) != "1") return

        val outputDirectory = System.getenv(Top1465CorpusContract.OutputEnvironmentVariable)
            ?.let(::File)
            ?: File("build/reports/top1465")
        val source = System.getenv(Top1465CorpusContract.SourceEnvironmentVariable)
            ?: Top1465CorpusContract.SourceUrl

        val report = Top1465CorpusRunner().run(
            source = source,
            outputDirectory = outputDirectory,
        )

        assertEquals(Top1465CorpusContract.PuzzleCount, report.puzzles.size)
        assertEquals(Top1465CorpusContract.Sha256, report.actualSha256)
        assertTrue(report.elapsedMilliseconds <= Top1465CorpusContract.MaxRuntime.inWholeMilliseconds)
        println(report.summary())
    }
}

private object Top1465CorpusContract {
    const val SourceUrl = "http://magictour.free.fr/top1465"
    const val Sha256 = "32837f38ece94e75678deadbe256aeafda704c8d4c2f8b5095a630ce2d0114d3"
    const val PuzzleCount = 1_465
    const val RunEnvironmentVariable = "TOP1465_RUN"
    const val SourceEnvironmentVariable = "TOP1465_SOURCE"
    const val OutputEnvironmentVariable = "TOP1465_OUTPUT_DIR"
    val MaxRuntime = 10.minutes
}

private class Top1465CorpusRunner {
    fun run(source: String, outputDirectory: File): Top1465Report {
        val startedAt = System.nanoTime()
        val deadline = startedAt + Top1465CorpusContract.MaxRuntime.inWholeNanoseconds
        val sourceBytes = readSource(source)
        val actualSha256 = sourceBytes.sha256()
        require(actualSha256 == Top1465CorpusContract.Sha256) {
            "top1465 checksum mismatch: expected ${Top1465CorpusContract.Sha256}, got $actualSha256"
        }
        val lines = sourceBytes
            .toString(Charsets.UTF_8)
            .lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .toList()
        require(lines.size == Top1465CorpusContract.PuzzleCount) {
            "Expected ${Top1465CorpusContract.PuzzleCount} puzzles, got ${lines.size}."
        }
        lines.forEachIndexed { index, line ->
            require(line.length == SudokuGrid.CellCount && line.all { it == '.' || it in '1'..'9' }) {
                "Malformed puzzle at line ${index + 1}."
            }
        }

        outputDirectory.deleteRecursively()
        val failureDirectory = outputDirectory.resolve("failures").apply { mkdirs() }
        val puzzles = lines.mapIndexed { index, givens ->
            checkDeadline(deadline, index + 1)
            analyzePuzzle(
                lineNumber = index + 1,
                givens = givens,
                failureDirectory = failureDirectory,
            )
        }
        val elapsedMilliseconds = (System.nanoTime() - startedAt) / 1_000_000
        val report = Top1465Report(
            source = Top1465CorpusContract.SourceUrl,
            expectedSha256 = Top1465CorpusContract.Sha256,
            actualSha256 = actualSha256,
            elapsedMilliseconds = elapsedMilliseconds,
            puzzles = puzzles,
        )
        report.writeTo(outputDirectory)
        return report
    }

    private fun analyzePuzzle(
        lineNumber: Int,
        givens: String,
        failureDirectory: File,
    ): Top1465PuzzleResult {
        val grid = givens.toSudokuGrid()
        val initialBoard = checkNotNull(SudokuBoardState.from(grid, useCellNotes = false)).board
        val baselineSolutions = SudokuBacktrackingSolver.findSolutions(initialBoard, limit = 2)
        val baselineClassification = baselineSolutions.classification()
        val logicalState = checkNotNull(SudokuBoardState.from(grid, useCellNotes = false))
        val steps = buildList {
            while (true) {
                val step = SudokuStrategyRegistry.findNextStep(logicalState) ?: break
                logicalState.apply(step)
                add(step)
            }
        }
        val solverSolutions = SudokuBacktrackingSolver.findSolutions(logicalState.board, limit = 2)
        val replayed = grid.replay(steps)
        val remainingCells = replayed.values().count { it == null }
        val classification = solverSolutions.classification()
        val firstIncorrectStep = baselineSolutions.singleOrNull()?.let { solution ->
            steps.withIndex()
                .firstOrNull { (_, step) ->
                    step.isPlacement && solution[step.row * SudokuGrid.Size + step.column] != step.value
                }
                ?.let { (index, step) ->
                    Top1465IncorrectStep(
                        stepNumber = index + 1,
                        pattern = step.pattern,
                        row = step.row,
                        column = step.column,
                        expectedValue = solution[step.row * SudokuGrid.Size + step.column],
                        actualValue = step.value,
                    )
                }
        }
        val firstIncorrectElimination = baselineSolutions.singleOrNull()?.let { solution ->
            findFirstIncorrectElimination(
                grid = grid,
                solution = solution,
                steps = steps,
            )
        }
        val firstIncorrectCandidateLoss = baselineSolutions.singleOrNull()?.let { solution ->
            findFirstIncorrectCandidateLoss(
                grid = grid,
                solution = solution,
                steps = steps,
            )
        }
        val logicallySolved = classification == Top1465Classification.Unique && remainingCells == 0
        val image = if (logicallySolved) {
            null
        } else {
            val fileName = "top1465-${lineNumber.toString().padStart(4, '0')}.png"
            Top1465BoardImage.write(
                lineNumber = lineNumber,
                givens = givens,
                output = failureDirectory.resolve(fileName),
            )
            "failures/$fileName"
        }
        return Top1465PuzzleResult(
            lineNumber = lineNumber,
            givens = givens,
            baselineClassification = baselineClassification,
            classification = classification,
            logicallySolved = logicallySolved,
            remainingCells = remainingCells,
            stepCount = steps.size,
            patterns = steps.map { it.pattern }.distinct(),
            stalledGrid = replayed.toPuzzleLine(),
            image = image,
            firstIncorrectStep = firstIncorrectStep,
            firstIncorrectElimination = firstIncorrectElimination,
            firstIncorrectCandidateLoss = firstIncorrectCandidateLoss,
        )
    }

    private fun findFirstIncorrectElimination(
        grid: SudokuGrid,
        solution: IntArray,
        steps: List<SudokuSolutionStep>,
    ): Top1465IncorrectElimination? {
        val auditState = checkNotNull(SudokuBoardState.from(grid, useCellNotes = false))
        steps.forEachIndexed { stepIndex, step ->
            step.eliminations.forEach { elimination ->
                val cellIndex = elimination.row * SudokuGrid.Size + elimination.column
                val expectedValue = solution[cellIndex]
                if (
                    expectedValue in elimination.values &&
                    expectedValue in auditState.candidates[cellIndex]
                ) {
                    return Top1465IncorrectElimination(
                        stepNumber = stepIndex + 1,
                        pattern = step.pattern,
                        row = elimination.row,
                        column = elimination.column,
                        removedValue = expectedValue,
                    )
                }
            }
            auditState.apply(step)
        }
        return null
    }

    private fun findFirstIncorrectCandidateLoss(
        grid: SudokuGrid,
        solution: IntArray,
        steps: List<SudokuSolutionStep>,
    ): Top1465IncorrectCandidateLoss? {
        val auditState = checkNotNull(SudokuBoardState.from(grid, useCellNotes = false))
        steps.forEachIndexed { stepIndex, step ->
            auditState.apply(step)
            val affectedIndex = auditState.board.indices.firstOrNull { index ->
                auditState.board[index] == 0 && solution[index] !in auditState.candidates[index]
            }
            if (affectedIndex != null) {
                return Top1465IncorrectCandidateLoss(
                    stepNumber = stepIndex + 1,
                    triggerPattern = step.pattern,
                    triggerWasPlacement = step.isPlacement,
                    row = affectedIndex / SudokuGrid.Size,
                    column = affectedIndex % SudokuGrid.Size,
                    lostValue = solution[affectedIndex],
                )
            }
        }
        return null
    }

    private fun checkDeadline(deadline: Long, nextLineNumber: Int) {
        check(System.nanoTime() <= deadline) {
            "top1465 exceeded ${Top1465CorpusContract.MaxRuntime} before line $nextLineNumber."
        }
    }

    private fun readSource(source: String): ByteArray {
        if (!source.startsWith("http://") && !source.startsWith("https://")) {
            return File(source).readBytes()
        }
        val connection = URI(source).toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            check(connection.responseCode in 200..299) {
                "Could not download top1465: HTTP ${connection.responseCode}."
            }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}

private enum class Top1465Classification {
    Unique,
    Multiple,
    Invalid,
    Unsolvable,
}

private data class Top1465PuzzleResult(
    val lineNumber: Int,
    val givens: String,
    val baselineClassification: Top1465Classification,
    val classification: Top1465Classification,
    val logicallySolved: Boolean,
    val remainingCells: Int,
    val stepCount: Int,
    val patterns: List<SudokuSolvingPattern>,
    val stalledGrid: String,
    val image: String?,
    val firstIncorrectStep: Top1465IncorrectStep?,
    val firstIncorrectElimination: Top1465IncorrectElimination?,
    val firstIncorrectCandidateLoss: Top1465IncorrectCandidateLoss?,
)

private data class Top1465IncorrectStep(
    val stepNumber: Int,
    val pattern: SudokuSolvingPattern,
    val row: Int,
    val column: Int,
    val expectedValue: Int,
    val actualValue: Int,
)

private data class Top1465IncorrectElimination(
    val stepNumber: Int,
    val pattern: SudokuSolvingPattern,
    val row: Int,
    val column: Int,
    val removedValue: Int,
)

private data class Top1465IncorrectCandidateLoss(
    val stepNumber: Int,
    val triggerPattern: SudokuSolvingPattern,
    val triggerWasPlacement: Boolean,
    val row: Int,
    val column: Int,
    val lostValue: Int,
)

private data class Top1465Report(
    val source: String,
    val expectedSha256: String,
    val actualSha256: String,
    val elapsedMilliseconds: Long,
    val puzzles: List<Top1465PuzzleResult>,
) {
    fun writeTo(outputDirectory: File) {
        outputDirectory.mkdirs()
        outputDirectory.resolve("report.json").writeText(toJson())
        outputDirectory.resolve("puzzles.tsv").writeText(toTsv())
        outputDirectory.resolve("issue-drafts.jsonl").writeText(toIssueDraftsJsonLines())
        outputDirectory.resolve("summary.txt").writeText(summary() + "\n")
    }

    fun summary(): String = buildString {
        appendLine("top1465 corpus report")
        appendLine("source=$source")
        appendLine("sha256=$actualSha256")
        appendLine("processed=${puzzles.size}")
        appendLine("unique=${puzzles.count { it.classification == Top1465Classification.Unique }}")
        appendLine("multiple=${puzzles.count { it.classification == Top1465Classification.Multiple }}")
        appendLine("invalid=${puzzles.count { it.classification == Top1465Classification.Invalid }}")
        appendLine("unsolvable=${puzzles.count { it.classification == Top1465Classification.Unsolvable }}")
        appendLine(
            "baselineUnique=${puzzles.count { it.baselineClassification == Top1465Classification.Unique }}",
        )
        appendLine(
            "baselineMultiple=${puzzles.count { it.baselineClassification == Top1465Classification.Multiple }}",
        )
        appendLine(
            "baselineUnsolvable=${puzzles.count { it.baselineClassification == Top1465Classification.Unsolvable }}",
        )
        appendLine("incorrectLogicalPlacements=${puzzles.count { it.firstIncorrectStep != null }}")
        appendLine(
            "incorrectCandidateEliminations=" +
                puzzles.count { it.firstIncorrectElimination != null },
        )
        appendLine(
            "incorrectCandidateLosses=" +
                puzzles.count { it.firstIncorrectCandidateLoss != null },
        )
        appendLine("logicallySolved=${puzzles.count { it.logicallySolved }}")
        appendLine("logicallyStalled=${puzzles.count { !it.logicallySolved }}")
        append("elapsedMs=$elapsedMilliseconds")
    }

    private fun toJson(): String = buildString {
        appendLine("{")
        appendLine("  \"schemaVersion\": 2,")
        appendLine("  \"source\": ${source.jsonString()},")
        appendLine("  \"expectedSha256\": ${expectedSha256.jsonString()},")
        appendLine("  \"actualSha256\": ${actualSha256.jsonString()},")
        appendLine("  \"processed\": ${puzzles.size},")
        appendLine("  \"elapsedMilliseconds\": $elapsedMilliseconds,")
        appendLine("  \"summary\": {")
        Top1465Classification.entries.forEachIndexed { index, classification ->
            val comma = if (index == Top1465Classification.entries.lastIndex) "" else ","
            appendLine(
                "    ${classification.name.lowercase(Locale.ROOT).jsonString()}: " +
                    "${puzzles.count { it.classification == classification }}$comma",
            )
        }
        appendLine("  },")
        appendLine("  \"baselineSummary\": {")
        Top1465Classification.entries.forEachIndexed { index, classification ->
            val comma = if (index == Top1465Classification.entries.lastIndex) "" else ","
            appendLine(
                "    ${classification.name.lowercase(Locale.ROOT).jsonString()}: " +
                    "${puzzles.count { it.baselineClassification == classification }}$comma",
            )
        }
        appendLine("  },")
        appendLine("  \"logicallySolved\": ${puzzles.count { it.logicallySolved }},")
        appendLine("  \"logicallyStalled\": ${puzzles.count { !it.logicallySolved }},")
        appendLine("  \"patternUsage\": {")
        val patternUsage = puzzles
            .flatMap { it.patterns }
            .groupingBy { it }
            .eachCount()
            .toSortedMap(compareBy { it.ordinal })
        patternUsage.entries.forEachIndexed { index, entry ->
            val comma = if (index == patternUsage.size - 1) "" else ","
            appendLine("    ${entry.key.name.jsonString()}: ${entry.value}$comma")
        }
        appendLine("  },")
        appendLine("  \"puzzles\": [")
        puzzles.forEachIndexed { index, puzzle ->
            append(puzzle.toJson(indent = "    "))
            appendLine(if (index == puzzles.lastIndex) "" else ",")
        }
        appendLine("  ]")
        appendLine("}")
    }

    private fun toTsv(): String = buildString {
        appendLine(
            "line\tbaselineClassification\tclassification\tlogical\tremaining\tsteps\tpatterns\t" +
                "givens\tstalledGrid\timage\tincorrectStep\tincorrectElimination\tincorrectCandidateLoss",
        )
        puzzles.forEach { puzzle ->
            appendLine(
                listOf(
                    puzzle.lineNumber,
                    puzzle.baselineClassification.name,
                    puzzle.classification.name,
                    puzzle.logicallySolved,
                    puzzle.remainingCells,
                    puzzle.stepCount,
                    puzzle.patterns.joinToString(",") { it.name },
                    puzzle.givens,
                    puzzle.stalledGrid,
                    puzzle.image ?: "-",
                    puzzle.firstIncorrectStep?.toCompactText() ?: "-",
                    puzzle.firstIncorrectElimination?.toCompactText() ?: "-",
                    puzzle.firstIncorrectCandidateLoss?.toCompactText() ?: "-",
                ).joinToString("\t"),
            )
        }
    }

    private fun toIssueDraftsJsonLines(): String = puzzles
        .asSequence()
        .filterNot { it.logicallySolved }
        .joinToString(separator = "\n", postfix = "\n") { puzzle ->
            puzzle.toJson().replace("\n", "")
        }
}

private fun Top1465PuzzleResult.toJson(indent: String = ""): String = buildString {
    appendLine("${indent}{")
    appendLine("$indent  \"lineNumber\": $lineNumber,")
    appendLine("$indent  \"givens\": ${givens.jsonString()},")
    appendLine("$indent  \"baselineClassification\": ${baselineClassification.name.jsonString()},")
    appendLine("$indent  \"classification\": ${classification.name.jsonString()},")
    appendLine("$indent  \"logicallySolved\": $logicallySolved,")
    appendLine("$indent  \"remainingCells\": $remainingCells,")
    appendLine("$indent  \"stepCount\": $stepCount,")
    appendLine("$indent  \"patterns\": [${patterns.joinToString { it.name.jsonString() }}],")
    appendLine("$indent  \"stalledGrid\": ${stalledGrid.jsonString()},")
    appendLine("$indent  \"image\": ${image?.jsonString() ?: "null"},")
    appendLine("$indent  \"firstIncorrectStep\": ${firstIncorrectStep?.toJson() ?: "null"},")
    appendLine(
        "$indent  \"firstIncorrectElimination\": " +
            "${firstIncorrectElimination?.toJson() ?: "null"},",
    )
    append(
        "$indent  \"firstIncorrectCandidateLoss\": " +
            "${firstIncorrectCandidateLoss?.toJson() ?: "null"}\n$indent}",
    )
}

private fun Top1465IncorrectStep.toCompactText(): String =
    "$stepNumber:${pattern.name}:${('A'.code + column).toChar()}${row + 1}:$actualValue->$expectedValue"

private fun Top1465IncorrectStep.toJson(): String = buildString {
    append('{')
    append("\"stepNumber\":$stepNumber,")
    append("\"pattern\":${pattern.name.jsonString()},")
    append("\"row\":$row,")
    append("\"column\":$column,")
    append("\"expectedValue\":$expectedValue,")
    append("\"actualValue\":$actualValue")
    append('}')
}

private fun Top1465IncorrectElimination.toCompactText(): String =
    "$stepNumber:${pattern.name}:${('A'.code + column).toChar()}${row + 1}:-$removedValue"

private fun Top1465IncorrectElimination.toJson(): String = buildString {
    append('{')
    append("\"stepNumber\":$stepNumber,")
    append("\"pattern\":${pattern.name.jsonString()},")
    append("\"row\":$row,")
    append("\"column\":$column,")
    append("\"removedValue\":$removedValue")
    append('}')
}

private fun Top1465IncorrectCandidateLoss.toCompactText(): String =
    "$stepNumber:${triggerPattern.name}:${if (triggerWasPlacement) "placement" else "elimination"}:" +
        "${('A'.code + column).toChar()}${row + 1}:-$lostValue"

private fun Top1465IncorrectCandidateLoss.toJson(): String = buildString {
    append('{')
    append("\"stepNumber\":$stepNumber,")
    append("\"triggerPattern\":${triggerPattern.name.jsonString()},")
    append("\"triggerWasPlacement\":$triggerWasPlacement,")
    append("\"row\":$row,")
    append("\"column\":$column,")
    append("\"lostValue\":$lostValue")
    append('}')
}

private object Top1465BoardImage {
    private const val ImageWidth = 324
    private const val ImageHeight = 344
    private const val BoardOffsetX = 18
    private const val BoardOffsetY = 40
    private const val CellSize = 32

    fun write(lineNumber: Int, givens: String, output: File) {
        System.setProperty("java.awt.headless", "true")
        val image = BufferedImage(ImageWidth, ImageHeight, BufferedImage.TYPE_BYTE_INDEXED)
        val graphics = image.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.color = Color(0xF4, 0xF8, 0xF6)
            graphics.fillRect(0, 0, image.width, image.height)
            graphics.color = Color(0x17, 0x1D, 0x1B)
            graphics.font = Font(Font.SANS_SERIF, Font.BOLD, 15)
            graphics.drawString("top1465 #${lineNumber.toString().padStart(4, '0')}", BoardOffsetX, 24)

            for (index in 0..SudokuGrid.Size) {
                val coordinate = BoardOffsetX + index * CellSize
                graphics.stroke = BasicStroke(if (index % 3 == 0) 3f else 1f)
                graphics.drawLine(coordinate, BoardOffsetY, coordinate, BoardOffsetY + CellSize * SudokuGrid.Size)
                val rowCoordinate = BoardOffsetY + index * CellSize
                graphics.drawLine(
                    BoardOffsetX,
                    rowCoordinate,
                    BoardOffsetX + CellSize * SudokuGrid.Size,
                    rowCoordinate,
                )
            }

            graphics.color = Color(0x00, 0x6B, 0x5E)
            graphics.font = Font(Font.SANS_SERIF, Font.BOLD, 20)
            val metrics = graphics.fontMetrics
            givens.forEachIndexed { index, value ->
                if (value == '.') return@forEachIndexed
                val row = index / SudokuGrid.Size
                val column = index % SudokuGrid.Size
                val text = value.toString()
                val x = BoardOffsetX + column * CellSize + (CellSize - metrics.stringWidth(text)) / 2
                val y = BoardOffsetY + row * CellSize + (CellSize - metrics.height) / 2 + metrics.ascent
                graphics.drawString(text, x, y)
            }
        } finally {
            graphics.dispose()
        }
        output.parentFile?.mkdirs()
        check(ImageIO.write(image, "png", output)) { "PNG writer is unavailable." }
    }
}

private fun ByteArray.sha256(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this)
    .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

private fun String.toSudokuGrid(): SudokuGrid = SudokuGrid.fromRows(
    chunked(SudokuGrid.Size).map { row ->
        row.map { value -> value.takeUnless { it == '.' }?.digitToInt() }
    },
)

private fun SudokuGrid.replay(steps: List<SudokuSolutionStep>): SudokuGrid {
    var current = withCandidateNotes()
    steps.forEach { step ->
        current = if (step.isPlacement) {
            current.setValue(step.row, step.column, step.value)
        } else {
            step.eliminations.fold(current) { grid, elimination ->
                grid.removeNotes(elimination.row, elimination.column, elimination.values)
            }
        }
    }
    return current
}

private fun SudokuGrid.toPuzzleLine(): String = values().joinToString("") { it?.toString() ?: "." }

private fun List<IntArray>.classification(): Top1465Classification = when (size) {
    0 -> Top1465Classification.Unsolvable
    1 -> Top1465Classification.Unique
    else -> Top1465Classification.Multiple
}

private fun String.jsonString(): String = buildString {
    append('"')
    this@jsonString.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
    append('"')
}
