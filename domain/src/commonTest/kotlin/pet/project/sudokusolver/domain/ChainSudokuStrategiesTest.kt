package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChainSudokuStrategiesTest {
    @Test
    fun alternatingInferenceChainEliminatesCandidateSeeingBothStrongEndpoints() {
        val step = AlternatingInferenceChainStrategy.findStep(aicState())

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.AlternatingInferenceChain, step.pattern)
        assertEquals(listOf(CandidateElimination(0, 7, setOf(1))), step.eliminations)
    }

    @Test
    fun alternatingInferenceChainRequiresAnUnbrokenAlternatingProof() {
        val state = stateWithout(setOf(1, 2, 3)) {
            candidate(0, 1, 2)
            candidate(4, 2, 4, 5)
            candidate(40, 3, 6, 7)
            candidate(43, 3, 4, 5)
            candidate(79, 1, 3)
            candidate(7, 1, 4, 9)
        }

        assertNull(AlternatingInferenceChainStrategy.findStep(state))
    }

    @Test
    fun alternatingInferenceChainDoesNotExceedItsDepthGuard() {
        val state = stateWithout(setOf(1, 2, 3, 4, 5)) {
            candidate(0, 1, 2)
            candidate(4, 2, 3)
            candidate(31, 3, 4)
            candidate(27, 4, 5)
            candidate(35, 1, 5)
            candidate(5, 1, 6, 7)
            candidate(6, 1, 6, 7)
            candidate(7, 1, 6, 7)
            candidate(8, 1, 2, 6, 7)
            candidate(33, 4, 5, 6, 7)
            candidate(34, 5, 6, 7)
            candidate(67, 3, 6, 7)
            candidate(80, 1, 6, 7)
        }

        assertNull(AlternatingInferenceChainStrategy.findStep(state))
    }

    @Test
    fun continuousLoopEliminatesCandidatesOnEveryWeakLink() {
        val step = ContinuousLoopStrategy.findStep(continuousLoopState())

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.ContinuousLoop, step.pattern)
        assertEquals(
            listOf(
                CandidateElimination(0, 7, setOf(1)),
                CandidateElimination(0, 8, setOf(2)),
            ),
            step.eliminations,
        )
    }

    @Test
    fun discontinuousWeakWeakLoopEliminatesTheDiscontinuousCandidate() {
        val step = DiscontinuousLoopStrategy.findStep(discontinuousWeakWeakState())

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.DiscontinuousLoop, step.pattern)
        assertFalse(step.isPlacement)
        assertEquals(listOf(CandidateElimination(0, 0, setOf(1))), step.eliminations)
    }

    @Test
    fun discontinuousStrongStrongLoopPlacesTheDiscontinuousCandidate() {
        val step = DiscontinuousLoopStrategy.findStep(discontinuousStrongStrongState())

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.DiscontinuousLoop, step.pattern)
        assertTrue(step.isPlacement)
        assertEquals(0, step.row)
        assertEquals(0, step.column)
        assertEquals(1, step.value)
    }

    @Test
    fun niceLoopIsAnExplicitFacadeAndIsSkippedByAutomaticSearch() {
        val state = continuousLoopState()
        val explicit = NiceLoopStrategy.findStep(state)
        val automatic = SudokuStrategyRegistry.findNextStep(state)

        assertNotNull(explicit)
        assertEquals(SudokuSolvingPattern.NiceLoop, explicit.pattern)
        assertNotNull(SudokuStrategyRegistry.strategyFor(SudokuSolvingPattern.NiceLoop))
        assertNotNull(automatic)
        assertTrue(automatic.pattern != SudokuSolvingPattern.NiceLoop)
    }

    @Test
    fun chainSearchIsDeterministic() {
        val aicResults = List(10) { AlternatingInferenceChainStrategy.findStep(aicState()) }
        val continuousResults = List(10) { ContinuousLoopStrategy.findStep(continuousLoopState()) }
        val discontinuousResults = List(10) {
            DiscontinuousLoopStrategy.findStep(discontinuousWeakWeakState())
        }

        assertEquals(1, aicResults.distinct().size)
        assertEquals(1, continuousResults.distinct().size)
        assertEquals(1, discontinuousResults.distinct().size)
    }

    private fun aicState(): SudokuBoardState = stateWithout(setOf(1, 2, 3)) {
        candidate(0, 1, 2)
        candidate(4, 2, 4, 5)
        candidate(8, 2, 7, 8)
        candidate(40, 2, 3, 6)
        candidate(43, 1, 3, 8)
        candidate(79, 1, 3)
        candidate(25, 3, 4, 5)
        candidate(5, 1, 4, 5)
        candidate(7, 1, 4, 9)
    }

    private fun continuousLoopState(): SudokuBoardState = stateWithout(setOf(1, 2)) {
        candidate(0, 1, 2)
        candidate(4, 1, 2)
        candidate(7, 1, 5, 6)
        candidate(8, 2, 7, 8)
    }

    private fun discontinuousWeakWeakState(): SudokuBoardState = stateWithout(setOf(1, 2)) {
        candidate(0, 1, 3, 4)
        candidate(4, 1, 2)
        candidate(8, 1, 2)
        candidate(6, 2, 5, 6)
    }

    private fun discontinuousStrongStrongState(): SudokuBoardState = stateWithout(setOf(1, 2, 3)) {
        candidate(0, 1, 2)
        candidate(4, 2, 3)
        candidate(8, 2, 4, 5)
        candidate(27, 1, 4, 5)
        candidate(31, 1, 3)
        candidate(35, 1, 6, 7)
        candidate(67, 3, 6, 7)
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
