package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SudokuSolverPatternTest {
    @Test
    fun emitsNakedSinglePatternForSingleCandidateCell() {
        val puzzle = SudokuGrid.fromRows(
            listOf(
                listOf(1, 2, 3, 4, 5, 6, 7, 8, null),
                listOf(null, null, null, null, null, null, null, null, null),
                listOf(null, null, null, null, null, null, null, null, null),
                listOf(null, null, null, null, null, null, null, null, null),
                listOf(null, null, null, null, null, null, null, null, null),
                listOf(null, null, null, null, null, null, null, null, null),
                listOf(null, null, null, null, null, null, null, null, null),
                listOf(null, null, null, null, null, null, null, null, null),
                listOf(null, null, null, null, null, null, null, null, null),
            ),
        )

        val firstStep = SudokuSolver().hint(puzzle)

        assertNotNull(firstStep)
        assertEquals(8, firstStep.column)
        assertEquals(9, firstStep.value)
        assertEquals(SudokuSolvingPattern.NakedSingle, firstStep.pattern)
        kotlin.test.assertTrue(firstStep.relatedCells.isNotEmpty())
    }
}
