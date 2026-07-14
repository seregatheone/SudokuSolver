package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssumptionStrategiesTest {
    @Test
    fun forcingChainEliminatesAnAssumptionThatContradictsTheImplicationGraph() {
        val step = ForcingChainStrategy.findStep(singleDigitContradictionState())
        val proof = ForcingChainStrategy.findContradictionProof(singleDigitContradictionState())

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.ForcingChain, step.pattern)
        assertEquals(listOf(CandidateElimination(0, 0, setOf(1))), step.eliminations)
        val contradiction = assertIs<ForcingChainProof.Contradiction>(proof)
        assertEquals(CandidateNode(0, 1), contradiction.assumption)
        assertTrue(contradiction.assumedTrue)
    }

    @Test
    fun forcingChainFindsAConclusionCommonToEveryCandidateOfACell() {
        val state = forcingCommonConclusionState()

        val step = ForcingChainStrategy.findStep(state)
        val proof = ForcingChainStrategy.findProof(state)

        assertNotNull(step)
        assertEquals(listOf(CandidateElimination(0, 8, setOf(1))), step.eliminations)
        val common = assertIs<ForcingChainProof.CommonConclusion>(proof)
        assertEquals(0, common.sourceIndex)
        assertEquals(CandidateNode(8, 1), common.conclusion)
        assertEquals(false, common.isTrue)
        assertEquals(2, common.branches.size)
    }

    @Test
    fun nishioUsesOnlyASingleDigitContradiction() {
        val state = singleDigitContradictionState()

        val step = NishioStrategy.findStep(state)
        val proof = NishioStrategy.findProof(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.Nishio, step.pattern)
        assertEquals(listOf(CandidateElimination(0, 0, setOf(1))), step.eliminations)
        assertNotNull(proof)
        assertTrue(proof.branch.assignments.keys.all { node -> node.value == 1 })
    }

    @Test
    fun bowmansBingoUsesOnlyASequenceOfSinglesToReachContradiction() {
        val state = bowmanState(lastCellValues = intArrayOf(3, 4))

        val step = BowmansBingoStrategy.findStep(state)
        val proof = BowmansBingoStrategy.findProof(state)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.BowmansBingo, step.pattern)
        assertEquals(listOf(CandidateElimination(0, 0, setOf(1))), step.eliminations)
        assertNotNull(proof)
        assertEquals(
            listOf(
                SinglesPlacement(0, 1),
                SinglesPlacement(1, 3),
                SinglesPlacement(2, 4),
            ),
            proof.trail,
        )
    }

    @Test
    fun brokenBranchesAndStoppedSinglesDoNotProduceProofs() {
        assertNull(ForcingChainStrategy.findProof(brokenForcingState()))
        assertNull(NishioStrategy.findProof(singleDigitContradictionState(extraColumnCandidate = true)))
        assertNull(BowmansBingoStrategy.findProof(bowmanState(lastCellValues = intArrayOf(3, 4, 5))))
    }

    @Test
    fun everyStrategyHonorsItsPropagationCap() {
        val state = singleDigitContradictionState()
        val graph = CandidateInferenceGraph.from(state)
        val forcingBranch = CandidateImplicationEngine.propagate(
            state = state,
            graph = graph,
            assumption = CandidateNode(0, 1),
            assumedTrue = true,
            maximumAssignments = 2,
        )
        val nishioBranch = CandidateImplicationEngine.propagate(
            state = state,
            graph = graph.forDigit(1),
            assumption = CandidateNode(0, 1),
            assumedTrue = true,
            maximumAssignments = 2,
            singleDigit = 1,
        )

        assertEquals(AssumptionOutcome.Capped, forcingBranch.outcome)
        assertEquals(AssumptionOutcome.Capped, nishioBranch.outcome)
        assertNull(
            BowmansBingoStrategy.findProof(
                bowmanState(lastCellValues = intArrayOf(3, 4)),
                maximumPlacements = 1,
            ),
        )
    }

    @Test
    fun baselineContradictionIsNotAttributedToAnAssumption() {
        val state = baselineContradictionState()

        assertTrue(hasBaselineContradiction(state))
        assertNull(ForcingChainStrategy.findStep(state))
        assertNull(NishioStrategy.findStep(state))
        assertNull(BowmansBingoStrategy.findStep(state))
    }

    @Test
    fun assumptionStrategiesPreserveTheOriginalBoardAndCandidates() {
        val state = singleDigitContradictionState()
        val boardBefore = state.board.copyOf()
        val candidatesBefore = state.candidates.map { candidates -> candidates.toSet() }

        ForcingChainStrategy.findStep(state)
        NishioStrategy.findStep(state)
        BowmansBingoStrategy.findStep(state)

        assertContentEquals(boardBefore, state.board)
        assertEquals(candidatesBefore, state.candidates.map { candidates -> candidates.toSet() })
    }

    @Test
    fun assumptionSearchIsDeterministic() {
        val forcing = List(10) { ForcingChainStrategy.findStep(forcingCommonConclusionState()) }
        val nishio = List(10) { NishioStrategy.findStep(singleDigitContradictionState()) }
        val bowman = List(10) {
            BowmansBingoStrategy.findStep(bowmanState(lastCellValues = intArrayOf(3, 4)))
        }

        assertEquals(1, forcing.distinct().size)
        assertEquals(1, nishio.distinct().size)
        assertEquals(1, bowman.distinct().size)
    }

    @Test
    fun bowmanAndNishioDoNotEscalateWhenTheirRestrictedPropagationStops() {
        val emptyState = checkNotNull(SudokuBoardState.from(SudokuGrid(emptyCells())))

        assertNull(BowmansBingoStrategy.findStep(emptyState))
        assertNull(NishioStrategy.findStep(emptyState))
    }

    private fun forcingCommonConclusionState(): SudokuBoardState = candidateState {
        (1..7).filter { index -> index != 4 }.forEach { index ->
            removeCandidate(index, 1)
            removeCandidate(index, 2)
        }
        candidate(0, 1, 2)
        candidate(4, 1, 2)
        candidate(8, 1, 3, 4)
    }

    private fun brokenForcingState(): SudokuBoardState = candidateState {
        (1..7).filter { index -> index != 4 }.forEach { index ->
            removeCandidate(index, 1)
            removeCandidate(index, 2)
        }
        candidate(0, 1, 2)
        candidate(4, 1, 2, 5)
        candidate(8, 1, 3, 4)
    }

    private fun bowmanState(lastCellValues: IntArray): SudokuBoardState = candidateState {
        candidate(0, 1, 2)
        candidate(1, 1, 3)
        candidate(2, 3, 4)
        candidate(3, *lastCellValues)
    }

    private fun singleDigitContradictionState(
        extraColumnCandidate: Boolean = false,
    ): SudokuBoardState {
        val cells = MutableList(SudokuGrid.CellCount) {
            SudokuCell(notes = (1..9).toSet())
        }
        for (row in 0 until SudokuGrid.Size) {
            val index = row * SudokuGrid.Size + 1
            cells[index] = SudokuCell(notes = (2..9).toSet())
        }
        cells[1] = SudokuCell(notes = (1..9).toSet())
        cells[10] = SudokuCell(notes = (1..9).toSet())
        if (extraColumnCandidate) cells[28] = SudokuCell(notes = (1..9).toSet())
        return checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))
    }

    private fun baselineContradictionState(): SudokuBoardState {
        val cells = emptyCells().toMutableList()
        cells[0] = SudokuCell(notes = setOf(1))
        cells[1] = SudokuCell(value = 1, isGiven = true)
        return checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))
    }

    private fun candidateState(configure: CandidateFixture.() -> Unit): SudokuBoardState {
        val fixture = CandidateFixture()
        fixture.configure()
        return checkNotNull(SudokuBoardState.from(SudokuGrid(fixture.cells)))
    }

    private fun emptyCells(): List<SudokuCell> =
        List(SudokuGrid.CellCount) { SudokuCell(notes = (1..9).toSet()) }

    private class CandidateFixture {
        val cells = MutableList(SudokuGrid.CellCount) {
            SudokuCell(notes = (1..9).toSet())
        }

        fun candidate(index: Int, vararg values: Int) {
            cells[index] = SudokuCell(notes = values.toSet())
        }

        fun removeCandidate(index: Int, value: Int) {
            cells[index] = SudokuCell(notes = cells[index].notes - value)
        }
    }
}
