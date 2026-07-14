package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SimpleColoringStrategyTest {
    @Test
    fun colorWrapEliminatesAColorThatConflictsInsideAHouse() {
        val withoutFive = (1..9).toSet() - 5
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = withoutFive) }
        cells[0] = SudokuCell(notes = setOf(1, 5))
        cells[1] = SudokuCell(notes = setOf(2, 5))
        cells[10] = SudokuCell(notes = setOf(3, 5))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        val step = SimpleColoringStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.SimpleColoring, step.pattern)
        assertEquals(
            listOf(
                CandidateElimination(row = 0, column = 0, values = setOf(5)),
                CandidateElimination(row = 1, column = 1, values = setOf(5)),
            ),
            step.eliminations,
        )
    }

    @Test
    fun colorTrapEliminatesAnUncoloredCandidateThatSeesBothColors() {
        val withoutFive = (1..9).toSet() - 5
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = withoutFive) }
        cells[0] = SudokuCell(notes = setOf(1, 5))
        cells[4] = SudokuCell(notes = setOf(2, 5))
        cells[40] = SudokuCell(notes = setOf(3, 5))
        cells[37] = SudokuCell(notes = setOf(4, 5))
        cells[10] = SudokuCell(notes = setOf(5, 6))
        cells[20] = SudokuCell(notes = setOf(5, 7))
        cells[64] = SudokuCell(notes = setOf(5, 8))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        val step = SimpleColoringStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.SimpleColoring, step.pattern)
        assertEquals(listOf(CandidateElimination(row = 1, column = 1, values = setOf(5))), step.eliminations)
        assertEquals(
            setOf(CellPosition(0, 0), CellPosition(0, 4), CellPosition(4, 4), CellPosition(4, 1)),
            step.relatedCells.toSet(),
        )
    }
}
