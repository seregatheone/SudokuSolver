package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun xyWingEliminatesSharedOuterCandidateFromCommonPincerPeer() {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = (1..9).toSet()) }
        cells[40] = SudokuCell(notes = setOf(1, 2))
        cells[37] = SudokuCell(notes = setOf(1, 3))
        cells[13] = SudokuCell(notes = setOf(2, 3))
        cells[10] = SudokuCell(notes = setOf(3, 4))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        val step = XYWingStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.XYWing, step.pattern)
        assertEquals(
            setOf(3),
            step.eliminations.single { it.row == 1 && it.column == 1 }.values,
        )
        assertEquals(
            setOf(CellPosition(4, 4), CellPosition(4, 1), CellPosition(1, 4)),
            step.relatedCells.toSet(),
        )
    }

    @Test
    fun skyscraperEliminatesCandidateSeenByBothRoofs() {
        val withoutFive = (1..9).toSet() - 5
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = withoutFive) }
        cells[0] = SudokuCell(notes = setOf(1, 5))
        cells[3] = SudokuCell(notes = setOf(2, 5))
        cells[27] = SudokuCell(notes = setOf(3, 5))
        cells[31] = SudokuCell(notes = setOf(4, 5))
        cells[48] = SudokuCell(notes = setOf(5, 6))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        val step = SkyscraperStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.Skyscraper, step.pattern)
        assertTrue(step.eliminations.any { elimination ->
            elimination.row == 5 && elimination.column == 3 && elimination.values == setOf(5)
        })
        assertEquals(
            setOf(CellPosition(0, 0), CellPosition(0, 3), CellPosition(3, 0), CellPosition(3, 4)),
            step.relatedCells.toSet(),
        )
    }

    @Test
    fun twoStringKiteEliminatesCandidateSeenByBothRoofs() {
        val withoutFive = (1..9).toSet() - 5
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = withoutFive) }
        cells[0] = SudokuCell(notes = setOf(1, 5))
        cells[4] = SudokuCell(notes = setOf(2, 5))
        cells[10] = SudokuCell(notes = setOf(3, 5))
        cells[46] = SudokuCell(notes = setOf(4, 5))
        cells[49] = SudokuCell(notes = setOf(5, 6))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        val step = TwoStringKiteStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.TwoStringKite, step.pattern)
        assertEquals(
            setOf(5),
            step.eliminations.single { it.row == 5 && it.column == 4 }.values,
        )
        assertEquals(
            setOf(CellPosition(0, 0), CellPosition(0, 4), CellPosition(1, 1), CellPosition(5, 1)),
            step.relatedCells.toSet(),
        )
    }

    @Test
    fun twoStringKiteRejectsLinksThatShareAnEndpoint() {
        val withoutFive = (1..9).toSet() - 5
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = withoutFive) }
        cells[0] = SudokuCell(notes = setOf(1, 5))
        cells[4] = SudokuCell(notes = setOf(2, 5))
        cells[45] = SudokuCell(notes = setOf(3, 5))
        cells[49] = SudokuCell(notes = setOf(4, 5))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))

        assertNull(TwoStringKiteStrategy.findStep(state))
    }
}
