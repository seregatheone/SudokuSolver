package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SueDeCoqStrategyTest {
    @Test
    fun eliminatesDisjointLineAndBoxValues() {
        val otherValues = (1..9).toSet() - setOf(3, 4, 5, 9)
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = otherValues) }
        cells[54] = SudokuCell(notes = setOf(3, 4, 5))
        cells[55] = SudokuCell(value = 8)
        cells[56] = SudokuCell(notes = setOf(3, 5, 9))
        cells[60] = SudokuCell(notes = setOf(4, 5))
        cells[65] = SudokuCell(notes = setOf(3, 9))
        cells[58] = SudokuCell(notes = setOf(1, 4))
        cells[61] = SudokuCell(notes = setOf(2, 5))
        cells[63] = SudokuCell(notes = setOf(6, 9))
        cells[72] = SudokuCell(notes = setOf(7, 9))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        val step = SueDeCoqStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.SueDeCoq, step.pattern)
        assertEquals(
            listOf(
                CandidateElimination(row = 6, column = 4, values = setOf(4)),
                CandidateElimination(row = 6, column = 7, values = setOf(5)),
                CandidateElimination(row = 7, column = 0, values = setOf(9)),
                CandidateElimination(row = 8, column = 0, values = setOf(9)),
            ),
            step.eliminations,
        )
        assertEquals(
            setOf(CellPosition(6, 0), CellPosition(6, 2), CellPosition(6, 6), CellPosition(7, 2)),
            step.relatedCells.toSet(),
        )
    }
}
