package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExocetStrategyTest {
    @Test
    fun juniorExocetRemovesNonBaseDigitsFromTargets() {
        val step = ExocetStrategy.findStep(exocetState())

        assertEquals(SudokuSolvingPattern.Exocet, step?.pattern)
        assertEquals(
            listOf(
                CandidateElimination(row = 1, column = 3, values = setOf(4)),
                CandidateElimination(row = 2, column = 6, values = setOf(5)),
            ),
            step?.eliminations,
        )
    }

    @Test
    fun transposedJuniorExocetSupportsStackOrientation() {
        val step = ExocetStrategy.findStep(exocetState(transposed = true))

        assertEquals(SudokuSolvingPattern.Exocet, step?.pattern)
        assertEquals(
            listOf(
                CandidateElimination(row = 3, column = 1, values = setOf(4)),
                CandidateElimination(row = 6, column = 2, values = setOf(5)),
            ),
            step?.eliminations,
        )
    }

    @Test
    fun companionContainingBaseCandidateRejectsPattern() {
        assertNull(ExocetStrategy.findStep(exocetState(companionCandidate = true)))
    }

    @Test
    fun companionSolvedWithBaseDigitRejectsPattern() {
        assertNull(ExocetStrategy.findStep(exocetState(companionGiven = true)))
    }

    @Test
    fun thirdPerpendicularCoverLineRejectsPattern() {
        assertNull(ExocetStrategy.findStep(exocetState(thirdCoverLine = true)))
    }

    @Test
    fun missingSRegionContinuationRejectsPattern() {
        assertNull(ExocetStrategy.findStep(exocetState(missingSContinuation = true)))
    }

    @Test
    fun missingBaseDigitAcrossTargetsRejectsPattern() {
        assertNull(ExocetStrategy.findStep(exocetState(missingTargetThree = true)))
    }

    @Test
    fun collinearTargetsAreOutsideBoundedSubset() {
        assertNull(ExocetStrategy.findStep(exocetState(collinearTargets = true)))
    }

    @Test
    fun solverRoutesExocetThroughPatternRegistry() {
        val step = SudokuSolver().hintForPattern(exocetGrid(), SudokuSolvingPattern.Exocet)

        assertEquals(SudokuSolvingPattern.Exocet, step?.pattern)
        assertEquals(
            listOf(
                CandidateElimination(row = 1, column = 3, values = setOf(4)),
                CandidateElimination(row = 2, column = 6, values = setOf(5)),
            ),
            step?.eliminations,
        )
    }

    @Test
    fun repeatedSearchIsDeterministic() {
        val state = exocetState()

        val steps = List(20) { ExocetStrategy.findStep(state) }

        assertEquals(1, steps.distinct().size)
        assertEquals(
            listOf(
                CandidateElimination(row = 1, column = 3, values = setOf(4)),
                CandidateElimination(row = 2, column = 6, values = setOf(5)),
            ),
            steps.first()?.eliminations,
        )
    }

    private fun exocetState(
        transposed: Boolean = false,
        companionCandidate: Boolean = false,
        companionGiven: Boolean = false,
        thirdCoverLine: Boolean = false,
        missingSContinuation: Boolean = false,
        missingTargetThree: Boolean = false,
        collinearTargets: Boolean = false,
    ): SudokuBoardState = checkNotNull(
        SudokuBoardState.from(
            exocetGrid(
                transposed = transposed,
                companionCandidate = companionCandidate,
                companionGiven = companionGiven,
                thirdCoverLine = thirdCoverLine,
                missingSContinuation = missingSContinuation,
                missingTargetThree = missingTargetThree,
                collinearTargets = collinearTargets,
            ),
        ),
    )

    private fun exocetGrid(
        transposed: Boolean = false,
        companionCandidate: Boolean = false,
        companionGiven: Boolean = false,
        thirdCoverLine: Boolean = false,
        missingSContinuation: Boolean = false,
        missingTargetThree: Boolean = false,
        collinearTargets: Boolean = false,
    ): SudokuGrid {
        val notesByIndex = mutableMapOf(
            0 to setOf(1, 2, 3),
            1 to setOf(1, 2, 3),
            12 to setOf(1, 2, 4),
            24 to if (missingTargetThree || collinearTargets) setOf(9) else setOf(2, 3, 5),
            15 to if (collinearTargets) setOf(2, 3, 5) else setOf(9),
            21 to if (companionCandidate) setOf(1, 9) else setOf(9),
            29 to setOf(1, 3, 9),
            30 to setOf(2, 3, 9),
            38 to setOf(2, 9),
            39 to setOf(1, 9),
            42 to if (missingSContinuation) setOf(1, 2, 9) else setOf(1, 2, 3, 9),
            43 to setOf(2, 3, 9),
        )
        if (missingTargetThree) notesByIndex[24] = setOf(2, 5)
        if (thirdCoverLine) notesByIndex[47] = setOf(1, 9)

        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = setOf(9)) }
        notesByIndex.forEach { (sourceIndex, notes) ->
            val index = if (transposed) transpose(sourceIndex) else sourceIndex
            cells[index] = SudokuCell(notes = notes)
        }
        if (companionGiven) {
            val companionIndex = if (transposed) transpose(21) else 21
            cells[companionIndex] = SudokuCell(value = 1, isGiven = true)
        }
        return SudokuGrid(cells)
    }

    private fun transpose(index: Int): Int = index.column() * SudokuGrid.Size + index.row()
}
