package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SudokuSolverFullPatternCatalogTest {
    @Test
    fun exposesEveryRequestedPatternInStepPipeline() {
        val expected = listOf(
            SudokuSolvingPattern.NakedSingle,
            SudokuSolvingPattern.HiddenSingle,
            SudokuSolvingPattern.LockedCandidatesPointing,
            SudokuSolvingPattern.ClaimingBoxLineReduction,
            SudokuSolvingPattern.NakedPair,
            SudokuSolvingPattern.NakedTriple,
            SudokuSolvingPattern.NakedQuad,
            SudokuSolvingPattern.HiddenPair,
            SudokuSolvingPattern.HiddenTriple,
            SudokuSolvingPattern.HiddenQuad,
            SudokuSolvingPattern.XWing,
            SudokuSolvingPattern.Swordfish,
            SudokuSolvingPattern.Jellyfish,
            SudokuSolvingPattern.XYWing,
            SudokuSolvingPattern.XYZWing,
            SudokuSolvingPattern.WWing,
            SudokuSolvingPattern.Skyscraper,
            SudokuSolvingPattern.TwoStringKite,
            SudokuSolvingPattern.EmptyRectangle,
            SudokuSolvingPattern.UniqueRectangle,
            SudokuSolvingPattern.SimpleColoring,
            SudokuSolvingPattern.MultiColoring,
            SudokuSolvingPattern.RemotePair,
            SudokuSolvingPattern.XChain,
            SudokuSolvingPattern.XYChain,
            SudokuSolvingPattern.AlternatingInferenceChain,
            SudokuSolvingPattern.ForcingChain,
            SudokuSolvingPattern.NiceLoop,
            SudokuSolvingPattern.ContinuousLoop,
            SudokuSolvingPattern.DiscontinuousLoop,
            SudokuSolvingPattern.GroupedAic,
            SudokuSolvingPattern.AlmostLockedSet,
            SudokuSolvingPattern.AlsXz,
            SudokuSolvingPattern.AlsXyWing,
            SudokuSolvingPattern.DeathBlossom,
            SudokuSolvingPattern.FinnedXWing,
            SudokuSolvingPattern.SashimiXWing,
            SudokuSolvingPattern.FinnedSwordfish,
            SudokuSolvingPattern.KrakenFish,
            SudokuSolvingPattern.SueDeCoq,
            SudokuSolvingPattern.Exocet,
            SudokuSolvingPattern.ThreeDMedusa,
            SudokuSolvingPattern.BowmansBingo,
            SudokuSolvingPattern.Nishio,
        )

        assertEquals(expected, SudokuSolver().supportedPatterns())
    }

    @Test
    fun detectsXWingAsCandidateEliminationStep() {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = (1..9).toSet() - 5) }
        listOf(0, 4, 9, 13, 18, 22).forEach { index ->
            cells[index] = cells[index].copy(notes = cells[index].notes + 5)
        }

        val step = SudokuSolver().hintForPattern(SudokuGrid(cells), SudokuSolvingPattern.XWing)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.XWing, step.pattern)
        assertTrue(step.eliminations.any { it.row == 2 && it.column == 0 && it.values == setOf(5) })
        assertTrue(step.eliminations.any { it.row == 2 && it.column == 4 && it.values == setOf(5) })
    }

    @Test
    fun detectsUniqueRectangleAsCandidateEliminationStep() {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = (1..9).toSet()) }
        cells[0] = SudokuCell(notes = setOf(1, 2))
        cells[3] = SudokuCell(notes = setOf(1, 2))
        cells[9] = SudokuCell(notes = setOf(1, 2))
        cells[12] = SudokuCell(notes = setOf(1, 2, 3))

        val step = SudokuSolver().hintForPattern(SudokuGrid(cells), SudokuSolvingPattern.UniqueRectangle)

        assertNotNull(step)
        assertEquals(SudokuSolvingPattern.UniqueRectangle, step.pattern)
        assertTrue(step.eliminations.any { it.row == 1 && it.column == 3 && it.values == setOf(1, 2) })
    }

    @Test
    fun uniqueRectangleStepKeepsAnImmutableCandidateSnapshot() {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = (1..9).toSet()) }
        cells[0] = SudokuCell(notes = setOf(1, 2))
        cells[3] = SudokuCell(notes = setOf(1, 2))
        cells[9] = SudokuCell(notes = setOf(1, 2))
        cells[12] = SudokuCell(notes = setOf(1, 2, 3))
        val state = checkNotNull(SudokuBoardState.from(SudokuGrid(cells)))
        val step = checkNotNull(UniqueRectangleStrategy.findStep(state))

        state.apply(
            SudokuSolutionStep(
                row = 0,
                column = 0,
                value = 1,
                pattern = SudokuSolvingPattern.NakedSingle,
            ),
        )

        assertEquals(setOf(1, 2), step.eliminations.single().values)
    }

    @Test
    fun rejectsUniqueRectangleThatSpansFourBoxes() {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell(notes = (1..9).toSet()) }
        cells[0] = SudokuCell(notes = setOf(1, 2))
        cells[4] = SudokuCell(notes = setOf(1, 2))
        cells[36] = SudokuCell(notes = setOf(1, 2))
        cells[40] = SudokuCell(notes = setOf(1, 2, 3))

        val step = SudokuSolver().hintForPattern(SudokuGrid(cells), SudokuSolvingPattern.UniqueRectangle)

        assertNull(step)
    }
}
