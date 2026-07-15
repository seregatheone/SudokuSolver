package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SudokuDomainModelTest {
    @Test
    fun rejectsMalformedCellsAndPositions() {
        assertFailsWith<IllegalArgumentException> { SudokuCell(value = 0) }
        assertFailsWith<IllegalArgumentException> { SudokuCell(notes = setOf(10)) }
        assertFailsWith<IllegalArgumentException> { SudokuCell(confidence = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { SudokuCell(confidence = 1.01f) }
        assertFailsWith<IllegalArgumentException> { CellPosition(row = -1, column = 0) }
        assertFailsWith<IllegalArgumentException> { CellPosition(row = 0, column = 9) }
    }

    @Test
    fun gridDefensivelyCopiesInputCells() {
        val input = MutableList(SudokuGrid.CellCount) { SudokuCell() }
        val grid = SudokuGrid(input)

        input[0] = SudokuCell(value = 9)

        assertNull(grid.cellAt(0, 0).value)
    }

    @Test
    fun editingOcrCellPreservesRecognitionMetadataAndOriginalGrid() {
        val position = CellPosition(row = 2, column = 3)
        val cells = MutableList(SudokuGrid.CellCount) { SudokuCell() }
        cells[CellPosition(row = 2, column = 0).index] = SudokuCell(value = 4)
        cells[position.index] = SudokuCell(
            value = 8,
            isGiven = true,
            confidence = 0.87f,
            source = SudokuCellSource.Ocr,
        )
        val original = SudokuGrid(cells)

        val edited = original.edit(SudokuEditAction.SetValue(position, value = 4))
        val editedCell = edited.cellAt(position.row, position.column)

        assertEquals(8, original.cellAt(position.row, position.column).value)
        assertEquals(4, editedCell.value)
        assertEquals(0.87f, editedCell.confidence)
        assertEquals(SudokuCellSource.Ocr, editedCell.source)
        assertEquals(false, editedCell.isGiven)
        assertNotEquals(original, edited)
        assertTrue(original.validate().isValid)
        assertTrue(edited.validate().conflictsAt(position).any { it.type == SudokuConflict.Row })
    }

    @Test
    fun clearAndNoteActionsKeepOcrConfidenceAndSource() {
        val position = CellPosition(row = 0, column = 0)
        val recognized = SudokuGrid(
            listOf(
                SudokuCell(value = 6, confidence = 0.73f, source = SudokuCellSource.Ocr),
            ) + List(SudokuGrid.CellCount - 1) { SudokuCell() },
        )

        val cleared = recognized.edit(SudokuEditAction.ClearCell(position))
        val noted = cleared.edit(SudokuEditAction.ToggleNote(position, value = 2))
        val cleaned = noted.edit(SudokuEditAction.RemoveNotes(position, setOf(2)))

        assertNull(cleared.cellAt(0, 0).value)
        assertEquals(false, cleared.cellAt(0, 0).isGiven)
        assertEquals(setOf(2), noted.cellAt(0, 0).notes)
        assertEquals(emptySet(), cleaned.cellAt(0, 0).notes)
        listOf(cleared, noted, cleaned).forEach { grid ->
            assertEquals(0.73f, grid.cellAt(0, 0).confidence)
            assertEquals(SudokuCellSource.Ocr, grid.cellAt(0, 0).source)
        }
    }

    @Test
    fun fromRowsMarksOnlyFilledCellsWithRequestedSource() {
        val rows = List(SudokuGrid.Size) { row ->
            List<Int?>(SudokuGrid.Size) { column -> if (row == 0 && column == 0) 5 else null }
        }

        val grid = SudokuGrid.fromRows(rows, source = SudokuCellSource.Ocr)

        assertEquals(SudokuCellSource.Ocr, grid.cellAt(0, 0).source)
        assertEquals(true, grid.cellAt(0, 0).isGiven)
        assertEquals(SudokuCellSource.Unknown, grid.cellAt(0, 1).source)
        assertEquals(false, grid.cellAt(0, 1).isGiven)
    }

    @Test
    fun enteringValueIntoUnknownCellMarksItAsManual() {
        val edited = SudokuGrid.Empty.edit(
            SudokuEditAction.SetValue(position = CellPosition(0, 0), value = 7),
        )

        assertEquals(SudokuCellSource.Manual, edited.cellAt(0, 0).source)
        assertEquals(SudokuCellSource.Unknown, SudokuGrid.Empty.cellAt(0, 0).source)
    }
}
