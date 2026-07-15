package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AlsXyWingStrategyTest {
    @Test
    fun eliminatesEndpointCandidateSeenByBothEndpointSets() {
        val state = linkedAlsState(
            overrides = mapOf(
                0 to setOf(1, 3),
                1 to setOf(1, 4),
                11 to setOf(1, 2),
                29 to setOf(2, 3),
                2 to setOf(3, 8, 9),
            ),
        )

        val first = AlsXyWingStrategy.findStep(state)
        val second = AlsXyWingStrategy.findStep(state)

        assertNotNull(first)
        assertEquals(SudokuSolvingPattern.AlsXyWing, first.pattern)
        assertEquals(
            listOf(CandidateElimination(row = 0, column = 2, values = setOf(3))),
            first.eliminations,
        )
        assertEquals(first, second)
    }

    @Test
    fun rejectsWingWhoseAdjacentLinksUseTheSameRcc() {
        val state = linkedAlsState(
            overrides = mapOf(
                10 to setOf(1, 2),
                13 to setOf(1, 3),
                37 to setOf(1, 3),
                40 to setOf(3, 8, 9),
            ),
        )

        assertNull(AlsXyWingStrategy.findStep(state))
    }

    @Test
    fun uniformCandidateStateDoesNotInventAResult() {
        val state = linkedAlsState(backgroundCandidates = (1..9).toSet())

        assertNull(AlsXyWingStrategy.findStep(state))
    }
}

class DeathBlossomStrategyTest {
    @Test
    fun eliminatesCandidateSeenByEveryPetal() {
        val state = linkedAlsState(
            overrides = mapOf(
                10 to setOf(1, 2),
                12 to setOf(1, 4),
                13 to setOf(3, 4),
                28 to setOf(2, 5),
                37 to setOf(3, 5),
                40 to setOf(3, 8, 9),
            ),
        )

        val first = DeathBlossomStrategy.findStep(state)
        val second = DeathBlossomStrategy.findStep(state)

        assertNotNull(first)
        assertEquals(SudokuSolvingPattern.DeathBlossom, first.pattern)
        assertEquals(
            listOf(CandidateElimination(row = 4, column = 4, values = setOf(3))),
            first.eliminations,
        )
        assertEquals(first, second)
    }

    @Test
    fun rejectsStemWithoutAPetalForEveryCandidate() {
        val state = linkedAlsState(
            overrides = mapOf(
                10 to setOf(1, 2),
                13 to setOf(1, 3),
                40 to setOf(3, 8, 9),
            ),
        )

        assertNull(DeathBlossomStrategy.findStep(state))
    }

    @Test
    fun rejectsPetalsThatCanOnlyBeFormedByReusingOverlappingCells() {
        val state = linkedAlsState(
            overrides = mapOf(
                10 to setOf(1, 2),
                12 to setOf(1, 2, 3),
                13 to setOf(1, 2, 3),
                14 to setOf(3, 8, 9),
            ),
        )

        assertNull(DeathBlossomStrategy.findStep(state))
    }

    @Test
    fun rejectsACommonCandidateThatIsAlsoAStemRcc() {
        val state = linkedAlsState(
            overrides = mapOf(
                10 to setOf(1, 2),
                13 to setOf(1, 2),
                37 to setOf(2, 3),
                40 to setOf(2, 8, 9),
            ),
        )

        assertNull(DeathBlossomStrategy.findStep(state))
    }

    @Test
    fun denseThreeCandidateStateIsPrunedDeterministically() {
        val state = linkedAlsState(backgroundCandidates = setOf(1, 2, 3))

        assertNull(DeathBlossomStrategy.findStep(state))
        assertNull(DeathBlossomStrategy.findStep(state))
    }
}

private fun linkedAlsState(
    backgroundCandidates: Set<Int> = emptySet(),
    overrides: Map<Int, Set<Int>> = emptyMap(),
): SudokuBoardState {
    val state = checkNotNull(SudokuBoardState.from(SudokuGrid.Empty))
    state.candidates.forEach { candidates ->
        candidates.clear()
        candidates.addAll(backgroundCandidates)
    }
    overrides.forEach { (index, candidates) ->
        state.candidates[index].clear()
        state.candidates[index].addAll(candidates)
    }
    return state
}
