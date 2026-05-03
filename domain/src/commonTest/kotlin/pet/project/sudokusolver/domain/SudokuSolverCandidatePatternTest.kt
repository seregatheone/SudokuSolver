package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SudokuSolverCandidatePatternTest {
    @Test
    fun emitsNakedPairAsCandidateEliminationStep() {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = (1..9).toSet()) }
        cells[0] = SudokuCell(notes = setOf(1, 2))
        cells[1] = SudokuCell(notes = setOf(1, 2))
        cells[2] = SudokuCell(notes = setOf(1, 2, 3))

        val step = SudokuSolver().hint(SudokuGrid(cells))

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.NakedPair, step.pattern)
        assertTrue(step.eliminations.any { it.row == 0 && it.column == 2 && it.values == setOf(1, 2) })
    }
}
