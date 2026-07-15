package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AdvancedColoringSudokuStrategiesTest {
    @Test
    fun multiColoringTypeOneEliminatesAColorThatSeesBothColorsOfAnotherComponent() {
        val state = stateWithout(setOf(7)) {
            candidate(0, 1, 2, 7)
            candidate(36, 3, 4, 7)
            candidate(1, 3, 5, 7)
            candidate(10, 4, 6, 7)
            candidate(8, 1, 5, 7)
            candidate(20, 2, 6, 7)
        }

        val step = MultiColoringStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.MultiColoring, step.pattern)
        assertEquals(listOf(CandidateElimination(0, 0, setOf(7))), step.eliminations)
    }

    @Test
    fun multiColoringTypeTwoEliminatesCandidateSeeingOppositeColors() {
        val state = multiColoringTypeTwoState()

        val step = MultiColoringStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.MultiColoring, step.pattern)
        assertEquals(listOf(CandidateElimination(0, 5, setOf(7))), step.eliminations)
    }

    @Test
    fun multiColoringRequiresTwoIndependentStrongComponents() {
        val state = stateWithout(setOf(7)) {
            candidate(0, 1, 2, 7)
            candidate(36, 3, 4, 7)
        }

        assertNull(MultiColoringStrategy.findStep(state))
    }

    @Test
    fun multiColoringSearchIsDeterministic() {
        val results = List(20) { MultiColoringStrategy.findStep(multiColoringTypeTwoState()) }

        assertEquals(1, results.distinct().size)
        assertNotNull(results.first())
    }

    @Test
    fun threeDMedusaEliminatesCandidateThatSeesBothColors() {
        val state = stateWithout(setOf(4, 5, 9)) {
            candidate(0, 4, 5, 9)
            candidate(4, 4, 5)
            candidate(8, 1, 2, 3, 6, 7, 8, 9)
            candidate(20, 1, 2, 3, 6, 7, 8, 9)
            candidate(72, 1, 2, 3, 6, 7, 8, 9)
        }

        val step = ThreeDMedusaStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.ThreeDMedusa, step.pattern)
        assertEquals(listOf(CandidateElimination(0, 0, setOf(9))), step.eliminations)
    }

    @Test
    fun threeDMedusaEliminatesAColorWithSameDigitConflict() {
        val state = stateWithout(setOf(1, 2)) {
            candidate(0, 1, 2)
            candidate(10, 1, 2)
            candidate(9, 1, 7, 8)
            candidate(18, 1, 7, 8)
        }

        val step = ThreeDMedusaStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(
            listOf(
                CandidateElimination(0, 0, setOf(1)),
                CandidateElimination(1, 0, setOf(1)),
                CandidateElimination(1, 1, setOf(2)),
            ),
            step.eliminations,
        )
    }

    @Test
    fun threeDMedusaDoesNotClaimSingleDigitColoring() {
        val state = stateWithout(setOf(4)) {
            candidate(0, 1, 2, 4)
            candidate(4, 3, 4, 5)
            candidate(1, 4, 6, 7)
        }

        assertNull(ThreeDMedusaStrategy.findStep(state))
    }

    private fun multiColoringTypeTwoState(): SudokuBoardState = stateWithout(setOf(7)) {
        candidate(0, 1, 2, 7)
        candidate(36, 3, 4, 7)
        candidate(39, 1, 5, 7)
        candidate(50, 2, 6, 7)
        candidate(43, 3, 5, 7)
        candidate(77, 4, 6, 7)
        candidate(8, 1, 4, 7)
        candidate(5, 2, 5, 7)
    }

    private fun stateWithout(
        excludedValues: Set<Int>,
        configure: CandidateFixture.() -> Unit,
    ): SudokuBoardState {
        val fixture = CandidateFixture((1..9).toSet() - excludedValues)
        fixture.configure()
        return checkNotNull(SudokuBoardState.from(SudokuGrid(fixture.cells)))
    }

    private class CandidateFixture(background: Set<Int>) {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = background) }

        fun candidate(index: Int, vararg values: Int) {
            cells[index] = SudokuCell(notes = values.toSet())
        }
    }
}
