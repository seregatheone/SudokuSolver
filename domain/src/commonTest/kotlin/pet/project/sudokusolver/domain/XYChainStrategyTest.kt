package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class XYChainStrategyTest {
    @Test
    fun eliminatesEndpointValueSeenByBothEnds() {
        val otherValues = (1..9).toSet() - setOf(1, 2, 3, 4)
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = otherValues) }
        cells[0] = SudokuCell(notes = setOf(1, 2))
        cells[4] = SudokuCell(notes = setOf(2, 3))
        cells[40] = SudokuCell(notes = setOf(3, 4))
        cells[37] = SudokuCell(notes = setOf(1, 4))
        cells[10] = SudokuCell(notes = setOf(1, 5))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        val step = XYChainStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.XYChain, step.pattern)
        assertEquals(listOf(CandidateElimination(row = 1, column = 1, values = setOf(1))), step.eliminations)
        assertEquals(
            listOf(CellPosition(0, 0), CellPosition(0, 4), CellPosition(4, 4), CellPosition(4, 1)),
            step.relatedCells,
        )
    }
}
