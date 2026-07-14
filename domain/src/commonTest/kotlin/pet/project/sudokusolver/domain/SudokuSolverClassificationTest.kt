package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SudokuSolverClassificationTest {
    @Test
    fun classifiesUniquePuzzleAndPreservesOcrMetadataInSolution() {
        val puzzle = classicPuzzle().setCellMetadata(
            position = CellPosition(0, 2),
            confidence = 0.91f,
            source = SudokuCellSource.Ocr,
        )

        val result = assertIs<SudokuSolveResult.Unique>(SudokuSolver().solve(puzzle))

        assertEquals(4, result.solvedGrid.valueAt(0, 2))
        assertEquals(0.91f, result.solvedGrid.cellAt(0, 2).confidence)
        assertEquals(SudokuCellSource.Ocr, result.solvedGrid.cellAt(0, 2).source)
        assertTrue(result.solvedGrid.validate().isValid)
    }

    @Test
    fun ignoresStalePencilNotesWhenClassifyingSolutions() {
        val position = CellPosition(0, 2)
        val puzzle = classicPuzzle().copy(
            cells = classicPuzzle().cells.mapIndexed { index, cell ->
                if (index == position.index) cell.copy(notes = setOf(1)) else cell
            },
        )

        val result = assertIs<SudokuSolveResult.Unique>(SudokuSolver().solve(puzzle))

        assertEquals(4, result.solvedGrid.valueAt(position.row, position.column))
    }

    @Test
    fun classifiesConflictingPuzzleAsInvalid() {
        val invalid = classicPuzzle().setValue(row = 0, column = 2, value = 5)

        val result = assertIs<SudokuSolveResult.Invalid>(SudokuSolver().solve(invalid))

        assertTrue(result.validation.conflicts.any { conflict ->
            conflict.type == SudokuConflict.Row && conflict.value == 5
        })
    }

    @Test
    fun classifiesValidPuzzleWithoutSolutionsAsUnsolvable() {
        val puzzle = SudokuGrid.fromRows(
            listOf(
                listOf(5, 1, 6, 8, 4, 9, 7, 3, 2),
                listOf(3, null, 7, 6, null, 5, null, null, null),
                listOf(8, null, 9, 7, null, null, null, 6, 5),
                listOf(1, 3, 5, null, 6, null, 9, null, 7),
                listOf(4, 7, 2, 5, 9, 1, null, null, 6),
                listOf(9, 6, 8, 3, 7, null, null, 5, null),
                listOf(2, 5, 3, 1, 8, 6, null, 7, 4),
                listOf(6, 8, 4, 2, null, 7, 5, null, null),
                listOf(7, 9, 1, null, 5, null, 6, null, 8),
            ),
        )

        assertTrue(puzzle.validate().isValid)
        assertIs<SudokuSolveResult.Unsolvable>(SudokuSolver().solve(puzzle))
    }

    @Test
    fun classifiesTwoSolutionRectangleAsMultiple() {
        val puzzle = SudokuGrid.fromRows(
            listOf(
                listOf(5, 3, 4, null, null, 8, 9, 1, 2),
                listOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
                listOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
                listOf(8, 5, 9, null, null, 1, 4, 2, 3),
                listOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
                listOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
                listOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
                listOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
                listOf(3, 4, 5, 2, 8, 6, 1, 7, 9),
            ),
        )

        val result = assertIs<SudokuSolveResult.Multiple>(SudokuSolver().solve(puzzle))

        assertEquals(2, result.solutions.size)
        assertNotEquals(result.solutions[0].values(), result.solutions[1].values())
        assertTrue(result.solutions.all { it.validate().isValid })
    }

    @Test
    fun solveResultsDefensivelyCopySolutionAndStepLists() {
        val solutions = mutableListOf(
            SudokuGrid.Empty,
            SudokuGrid.Empty.setValue(row = 0, column = 0, value = 1),
        )
        val steps = mutableListOf(
            SudokuSolutionStep(
                row = 0,
                column = 0,
                value = 1,
                pattern = SudokuSolvingPattern.NakedSingle,
            ),
        )
        val multiple = SudokuSolveResult.Multiple(solutions, steps)
        val unique = SudokuSolveResult.Unique(solutions.first(), steps)

        solutions.clear()
        steps.clear()

        assertEquals(2, multiple.solutions.size)
        assertEquals(1, multiple.steps.size)
        assertEquals(1, unique.steps.size)
        assertFailsWith<IllegalArgumentException> {
            SudokuSolveResult.Multiple(listOf(SudokuGrid.Empty), emptyList())
        }
    }

    private fun SudokuGrid.setCellMetadata(
        position: CellPosition,
        confidence: Float,
        source: SudokuCellSource,
    ): SudokuGrid = copy(
        cells = cells.mapIndexed { index, cell ->
            if (index == position.index) cell.copy(confidence = confidence, source = source) else cell
        },
    )

    private fun classicPuzzle() = SudokuGrid.fromRows(
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
}
