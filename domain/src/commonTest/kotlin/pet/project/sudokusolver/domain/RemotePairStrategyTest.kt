package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RemotePairStrategyTest {
    @Test
    fun eliminatesBothValuesFromCellSeeingOppositeParities() {
        val withoutOneOrTwo = (1..9).toSet() - 1 - 2
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = withoutOneOrTwo) }
        cells[0] = SudokuCell(notes = setOf(1, 2))
        cells[4] = SudokuCell(notes = setOf(1, 2))
        cells[40] = SudokuCell(notes = setOf(1, 2))
        cells[37] = SudokuCell(notes = setOf(1, 2))
        cells[10] = SudokuCell(notes = setOf(1, 2, 3))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        val step = RemotePairStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.RemotePair, step.pattern)
        assertEquals(listOf(CandidateElimination(row = 1, column = 1, values = setOf(1, 2))), step.eliminations)
        assertEquals(
            setOf(CellPosition(0, 0), CellPosition(0, 4), CellPosition(4, 4), CellPosition(4, 1)),
            step.relatedCells.toSet(),
        )
    }
}
