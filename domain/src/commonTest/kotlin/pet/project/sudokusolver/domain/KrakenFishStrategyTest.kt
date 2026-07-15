package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KrakenFishStrategyTest {
    @Test
    fun finnedXWingUsesAlternatingChainToEliminateRemoteVictim() {
        val step = KrakenFishStrategy.findStep(krakenState())

        assertEquals(SudokuSolvingPattern.KrakenFish, step?.pattern)
        assertEquals(
            listOf(CandidateElimination(row = 6, column = 4, values = setOf(5))),
            step?.eliminations,
        )
    }

    @Test
    fun transposedFinnedXWingSupportsColumnBaseOrientation() {
        val step = KrakenFishStrategy.findStep(krakenState(transposed = true))

        assertEquals(SudokuSolvingPattern.KrakenFish, step?.pattern)
        assertEquals(
            listOf(CandidateElimination(row = 4, column = 6, values = setOf(5))),
            step?.eliminations,
        )
    }

    @Test
    fun finnedSwordfishUsesAlternatingChainToEliminateRemoteVictim() {
        val state = stateWithFiveAt(
            setOf(
                0,
                3,
                7,
                12,
                15,
                27,
                33,
                51,
                61,
                69,
            ),
        )

        val step = KrakenFishStrategy.findStep(state)

        assertEquals(SudokuSolvingPattern.KrakenFish, step?.pattern)
        assertEquals(
            listOf(CandidateElimination(row = 5, column = 6, values = setOf(5))),
            step?.eliminations,
        )
    }

    @Test
    fun solverRoutesKrakenFishThroughPatternRegistry() {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = setOf(9)) }
        setOf(0, 4, 7, 13, 16, 21, 27, 31, 58).forEach { index ->
            cells[index] = SudokuCell(notes = setOf(5, 9))
        }

        val step = SudokuSolver().hintForPattern(
            SudokuGrid(cells),
            SudokuSolvingPattern.KrakenFish,
        )

        assertEquals(SudokuSolvingPattern.KrakenFish, step?.pattern)
        assertEquals(
            listOf(CandidateElimination(row = 6, column = 4, values = setOf(5))),
            step?.eliminations,
        )
    }

    @Test
    fun extraCandidateBreakingConjugateLinkProtectsVictim() {
        val step = KrakenFishStrategy.findStep(krakenState(additionalFiveIndexes = setOf(11)))

        assertTrue(
            step == null || step.eliminations.none { elimination ->
                elimination.row == 6 && elimination.column == 4 && 5 in elimination.values
            },
        )
    }

    @Test
    fun ordinaryFinnedFishWithOnlyDirectFinVisibilityIsNotCalledKraken() {
        val state = stateWithFiveAt(
            setOf(
                0,
                1,
                4,
                9,
                27,
                31,
            ),
        )

        assertNull(KrakenFishStrategy.findStep(state))
    }

    @Test
    fun oneUnprovedFinCancelsRemoteElimination() {
        val step = KrakenFishStrategy.findStep(krakenState(additionalFiveIndexes = setOf(35)))

        assertNull(step)
    }

    @Test
    fun krakenProofPathIsDeterministicAndHonorsDepthBound() {
        val graph = CandidateInferenceGraph.from(krakenState()).forDigit(5)
        val paths = List(20) {
            findAlternatingPath(
                graph = graph,
                start = CandidateNode(index = 7, value = 5),
                end = CandidateNode(index = 58, value = 5),
                firstLink = InferenceLinkType.Weak,
                lastLink = InferenceLinkType.Weak,
                maxDepth = 7,
            )
        }

        assertEquals(1, paths.distinct().size)
        assertEquals(3, paths.first()?.depth)
        assertEquals(listOf(7, 16, 13, 58), paths.first()?.nodes?.map { node -> node.index })
        assertNull(
            findAlternatingPath(
                graph = graph,
                start = CandidateNode(index = 7, value = 5),
                end = CandidateNode(index = 58, value = 5),
                firstLink = InferenceLinkType.Weak,
                lastLink = InferenceLinkType.Weak,
                maxDepth = 2,
            ),
        )
    }

    @Test
    fun repeatedSearchIsDeterministic() {
        val state = krakenState()

        val steps = List(20) { KrakenFishStrategy.findStep(state) }

        assertEquals(1, steps.distinct().size)
        assertEquals(
            listOf(CandidateElimination(row = 6, column = 4, values = setOf(5))),
            steps.first()?.eliminations,
        )
    }

    private fun krakenState(
        transposed: Boolean = false,
        additionalFiveIndexes: Set<Int> = emptySet(),
    ): SudokuBoardState {
        val fiveIndexes = setOf(
            0,
            4,
            7,
            13,
            16,
            21,
            27,
            31,
            58,
        ) + additionalFiveIndexes
        return stateWithFiveAt(
            if (transposed) fiveIndexes.mapTo(mutableSetOf(), ::transpose) else fiveIndexes,
        )
    }

    private fun stateWithFiveAt(indexes: Set<Int>): SudokuBoardState {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = setOf(9)) }
        indexes.forEach { index -> cells[index] = SudokuCell(notes = setOf(5, 9)) }
        return checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))
    }

    private fun transpose(index: Int): Int = index.column() * SudokuGrid.Size + index.row()
}
