package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SudokuValidationTest {
    @Test
    fun reportsEveryRowColumnAndBoxConflictWithAllPositions() {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell() }
        set(cells, row = 0, column = 0, value = 5)
        set(cells, row = 0, column = 4, value = 5)
        set(cells, row = 1, column = 8, value = 6)
        set(cells, row = 7, column = 8, value = 6)
        set(cells, row = 3, column = 3, value = 7)
        set(cells, row = 4, column = 4, value = 7)
        val validation = SudokuGrid(cells).validate()

        assertFalse(validation.isValid)
        assertEquals(
            setOf(CellPosition(0, 0), CellPosition(0, 4)),
            validation.conflicts.single { it.type == SudokuConflict.Row }.positions,
        )
        assertEquals(
            setOf(CellPosition(1, 8), CellPosition(7, 8)),
            validation.conflicts.single { it.type == SudokuConflict.Column }.positions,
        )
        assertEquals(
            setOf(CellPosition(3, 3), CellPosition(4, 4)),
            validation.conflicts.single { it.type == SudokuConflict.Box }.positions,
        )
        assertEquals(6, validation.conflictPositions.size)
    }

    @Test
    fun editRecalculatesConflictsWithoutMutatingPreviousValidation() {
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell() }
        set(cells, row = 2, column = 0, value = 4)
        set(cells, row = 2, column = 8, value = 4)
        val conflicted = SudokuGrid(cells)
        val before = conflicted.validate()

        val corrected = conflicted.edit(SudokuEditAction.SetValue(CellPosition(2, 8), value = 9))

        assertFalse(before.isValid)
        assertTrue(corrected.validate().isValid)
        assertEquals(4, conflicted.valueAt(2, 8))
    }

    @Test
    fun validPartialBoardHasNoConflictPositions() {
        val validation = classicPuzzle().validate()

        assertTrue(validation.isValid)
        assertTrue(validation.conflicts.isEmpty())
        assertTrue(validation.conflictPositions.isEmpty())
    }

    @Test
    fun validationDefensivelyCopiesConflictCollections() {
        val positions = mutableSetOf(CellPosition(0, 0), CellPosition(0, 1))
        val conflict = SudokuValidationConflict(
            type = SudokuConflict.Row,
            unitIndex = 0,
            value = 5,
            positions = positions,
        )
        val conflicts = mutableListOf(conflict)
        val validation = SudokuValidation(conflicts)

        positions.clear()
        conflicts.clear()

        assertFalse(validation.isValid)
        assertEquals(2, validation.conflictPositions.size)
        assertEquals(2, validation.conflicts.single().positions.size)
    }

    private fun set(cells: MutableList<SudokuCell>, row: Int, column: Int, value: Int) {
        cells[row * SudokuGrid.Size + column] = SudokuCell(value = value)
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
