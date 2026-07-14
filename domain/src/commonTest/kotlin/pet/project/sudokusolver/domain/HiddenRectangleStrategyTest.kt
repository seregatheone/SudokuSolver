package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HiddenRectangleStrategyTest {
    @Test
    fun eliminatesOtherRectangleValueFromOppositeCorner() {
        val withoutOneOrTwo = (1..9).toSet() - 1 - 2
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = withoutOneOrTwo) }
        cells[0] = SudokuCell(notes = setOf(1, 2))
        cells[3] = SudokuCell(notes = setOf(1, 2, 3))
        cells[9] = SudokuCell(notes = setOf(1, 2, 4))
        cells[12] = SudokuCell(notes = setOf(1, 2, 5))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        val step = UniqueRectangleStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.UniqueRectangle, step.pattern)
        assertEquals(listOf(CandidateElimination(row = 1, column = 3, values = setOf(2))), step.eliminations)
    }

    @Test
    fun rejectsHiddenRectangleWithoutRequiredStrongLinks() {
        val withoutOneOrTwo = (1..9).toSet() - 1 - 2
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = withoutOneOrTwo) }
        cells[0] = SudokuCell(notes = setOf(1, 2))
        cells[3] = SudokuCell(notes = setOf(1, 2, 3))
        cells[9] = SudokuCell(notes = setOf(1, 2, 4))
        cells[12] = SudokuCell(notes = setOf(1, 2, 5))
        cells[10] = SudokuCell(notes = setOf(1, 6))
        cells[11] = SudokuCell(notes = setOf(2, 7))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        assertNull(UniqueRectangleStrategy.findStep(state))
    }
}
