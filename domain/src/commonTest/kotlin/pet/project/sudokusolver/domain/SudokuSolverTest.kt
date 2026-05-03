package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SudokuSolverTest {
    @Test
    fun solvesClassicPuzzle() {
        val puzzle = classicPuzzle()

        val solution = SudokuSolver().solve(puzzle)

        assertNotNull(solution)
        assertEquals(5, solution.solvedGrid.valueAt(0, 0))
        assertEquals(4, solution.solvedGrid.valueAt(0, 2))
        assertEquals(9, solution.solvedGrid.valueAt(8, 8))
    }

    @Test
    fun rejectsConflictingPuzzle() {
        val puzzle = SudokuGrid.fromRows(
            listOf(
                listOf(5, 5, null, null, 7, null, null, null, null),
                listOf(6, null, null, 1, 9, 5, null, null, null),
                listOf(null, 9, 8, null, null, null, null, 6, null),
                listOf(8, null, null, null, 6, null, null, null, 3),
                listOf(4, null, null, 8, null, 3, null, null, 1),
                listOf(7, null, null, null, 2, null, null, null, 6),
                listOf(null, 6, null, null, null, null, 2, 8, null),
                listOf(null, null, null, 4, 1, 9, null, null, 5),
                listOf(null, null, null, null, 8, null, null, 7, 9),
            ),
        )

        assertNull(SudokuSolver().solve(puzzle))
    }

    @Test
    fun preventsDuplicateManualValuesInRowColumnAndBox() {
        val puzzle = classicPuzzle()

        assertFalse(puzzle.canSetValue(row = 0, column = 2, value = 5))
        assertEquals(SudokuConflict.Row, puzzle.conflictFor(row = 0, column = 2, value = 5))

        assertFalse(puzzle.canSetValue(row = 2, column = 0, value = 4))
        assertEquals(SudokuConflict.Column, puzzle.conflictFor(row = 2, column = 0, value = 4))

        assertFalse(puzzle.canSetValue(row = 1, column = 2, value = 3))
        assertEquals(SudokuConflict.Box, puzzle.conflictFor(row = 1, column = 2, value = 3))

        assertTrue(puzzle.canSetValue(row = 0, column = 2, value = 4))
    }


    @Test
    fun togglesCellNotesAndClearsThemWhenValueIsSet() {
        val withNote = SudokuGrid.Empty.toggleNote(row = 0, column = 0, value = 4)
        assertEquals(setOf(4), withNote.cellAt(row = 0, column = 0).notes)

        val withoutNote = withNote.toggleNote(row = 0, column = 0, value = 4)
        assertEquals(emptySet(), withoutNote.cellAt(row = 0, column = 0).notes)

        val solvedCell = withNote.setValue(row = 0, column = 0, value = 4)
        assertEquals(emptySet(), solvedCell.cellAt(row = 0, column = 0).notes)
    }


    @Test
    fun fillsCandidateNotesForEmptyCells() {
        val withNotes = classicPuzzle().withCandidateNotes()

        assertEquals(setOf(1, 2, 4), withNotes.cellAt(row = 0, column = 2).notes)
        assertEquals(emptySet(), withNotes.cellAt(row = 0, column = 0).notes)
    }

    private fun classicPuzzle() = SudokuGrid.fromRows(
        listOf(
            listOf(5, 3, null, null, 7, null, null, null, null),
            listOf(6, null, null, 1, 9, 5, null, null, null),
            listOf(null, 9, 8, null, null, null, null, 6, null),
            listOf(8, null, null, null, 6, null, null, null, 3),
            listOf(4, null, null, 8, null, 3, null, null, 1),
            listOf(7, null, null, null, 2, null, null, null, 6),
            listOf(null, 6, null, null, null, null, 2, 8, null),
            listOf(null, null, null, 4, 1, 9, null, null, 5),
            listOf(null, null, null, null, 8, null, null, 7, 9),
        ),
    )
}
