package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlmostLockedSetCatalogTest {
    @Test
    fun findsCanonicalAlsAndDeduplicatesCellSetsAcrossHouses() {
        val state = stateWith(
            overrides = mapOf(
                0 to setOf(1, 2),
                1 to setOf(1, 3),
            ),
        )

        val sets = AlmostLockedSetCatalog.find(state)

        val twoCellSet = sets.single { it.indexes == listOf(0, 1) }
        assertEquals(setOf(1, 2, 3), twoCellSet.candidates)
        assertEquals(1, sets.count { it.indexes == listOf(0) })
        assertEquals(setOf(1, 2), sets.single { it.indexes == listOf(0) }.candidates)
    }

    @Test
    fun rejectsCandidateGroupsThatAreNotAnAlsInOneHouse() {
        val state = stateWith(
            overrides = mapOf(
                0 to setOf(1, 2),
                1 to setOf(3, 4),
                40 to setOf(1, 3),
            ),
        )

        val sets = AlmostLockedSetCatalog.find(state)

        assertFalse(sets.any { it.indexes == listOf(0, 1) })
        assertFalse(sets.any { it.indexes == listOf(0, 40) })
    }

    @Test
    fun enumeratesTheBoundedEightCellAls() {
        val state = stateWith(
            overrides = (0 until 8).associateWith { (1..9).toSet() },
        )

        val sets = AlmostLockedSetCatalog.find(state)

        assertTrue(sets.any { set ->
            set.indexes == (0 until 8).toList() && set.candidates == (1..9).toSet()
        })
    }

    @Test
    fun rccRequiresEveryOccurrenceToSeeEveryOccurrenceInTheOtherSet() {
        val state = stateWith(
            overrides = mapOf(
                0 to setOf(1, 2),
                1 to setOf(1, 3),
                11 to setOf(1, 4),
                29 to setOf(1, 3, 4),
            ),
        )
        val sets = AlmostLockedSetCatalog.find(state)
        val first = sets.single { it.indexes == listOf(0, 1) }
        val second = sets.single { it.indexes == listOf(11, 29) }

        val links = AlmostLockedSetRccGraph.build(state, listOf(first, second))

        assertTrue(links.isEmpty())
    }
}

class AlsXzStrategyTest {
    @Test
    fun singlyLinkedAlsXzEliminatesCommonCandidateSeenByBothSets() {
        val state = stateWith(
            overrides = mapOf(
                0 to setOf(1, 2),
                1 to setOf(1, 3),
                11 to setOf(1, 4),
                29 to setOf(3, 4),
                2 to setOf(3, 8, 9),
            ),
        )

        val step = AlsXzStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.AlsXz, step.pattern)
        assertEquals(
            listOf(CandidateElimination(row = 0, column = 2, values = setOf(3))),
            step.eliminations,
        )
    }

    @Test
    fun rejectsAlsPairWithoutARestrictedCommonCandidate() {
        val state = stateWith(
            overrides = mapOf(
                0 to setOf(1, 2),
                1 to setOf(1, 3),
                29 to setOf(1, 4),
                38 to setOf(3, 4),
                2 to setOf(3, 8, 9),
            ),
        )

        assertNull(AlsXzStrategy.findStep(state))
    }

    @Test
    fun doublyLinkedAlsXzLocksRccsAndTheRemainingCandidates() {
        val state = stateWith(
            overrides = mapOf(
                0 to setOf(1, 3),
                1 to setOf(2, 3),
                9 to setOf(1, 4),
                10 to setOf(2, 4),
                2 to setOf(3, 8, 9),
                11 to setOf(4, 8, 9),
                20 to setOf(1, 2, 8),
            ),
        )

        val step = AlsXzStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(
            listOf(
                CandidateElimination(row = 0, column = 2, values = setOf(3)),
                CandidateElimination(row = 1, column = 2, values = setOf(4)),
                CandidateElimination(row = 2, column = 2, values = setOf(1, 2)),
            ),
            step.eliminations,
        )
    }

    @Test
    fun doublyLinkedAlsXzCanEliminateCannibalisticallyFromTheOtherSet() {
        val state = stateWith(
            overrides = mapOf(
                0 to setOf(1, 3),
                1 to setOf(2, 3),
                11 to setOf(1, 2, 3),
                29 to setOf(3, 4),
                38 to setOf(3, 4),
            ),
        )

        val step = AlsXzStrategy.findStep(state)

        assertNotNull(step)
        assertEquals(
            listOf(CandidateElimination(row = 1, column = 2, values = setOf(3))),
            step.eliminations,
        )
    }

    @Test
    fun uniformCandidateStateTerminatesWithoutInventingAnAlsXzStep() {
        val state = stateWith(backgroundCandidates = (1..9).toSet())

        assertNull(AlsXzStrategy.findStep(state))
        assertNull(AlsXzStrategy.findStep(state))
    }
}

private fun stateWith(
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
