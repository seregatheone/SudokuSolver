package pet.project.sudokusolver.domain

data class SudokuCell(
    val value: Int? = null,
    val notes: Set<Int> = emptySet(),
    val isGiven: Boolean = false,
)

data class CellPosition(
    val row: Int,
    val column: Int,
) {
    val index: Int = row * SudokuGrid.Size + column
}

data class SudokuGrid(
    val cells: List<SudokuCell>,
) {
    init {
        require(cells.size == CellCount) { "Sudoku grid must contain exactly $CellCount cells." }
        cells.forEach { cell ->
            require(cell.value == null || cell.value in 1..9) { "Sudoku cell value must be 1..9." }
            require(cell.notes.all { it in 1..9 }) { "Sudoku notes must be 1..9." }
        }
    }

    fun valueAt(row: Int, column: Int): Int? = cells[row * Size + column].value

    fun cellAt(row: Int, column: Int): SudokuCell = cells[row * Size + column]

    fun setValue(row: Int, column: Int, value: Int?, isGiven: Boolean = false): SudokuGrid {
        require(value == null || value in 1..9) { "Sudoku cell value must be 1..9." }
        val index = row * Size + column
        val affectedPeerIndexes: Set<Int> = if (value == null) emptySet() else peerIndexes(row, column)
        return copy(
            cells = cells.mapIndexed { currentIndex, cell ->
                when {
                    currentIndex == index -> {
                        cell.copy(value = value, notes = if (value == null) cell.notes else emptySet(), isGiven = isGiven)
                    }
                    currentIndex in affectedPeerIndexes -> {
                        cell.copy(notes = cell.notes - requireNotNull(value))
                    }
                    else -> cell
                }
            },
        )
    }

    fun toggleNote(row: Int, column: Int, value: Int): SudokuGrid {
        require(value in 1..9) { "Sudoku note value must be 1..9." }
        val index = row * Size + column
        return copy(
            cells = cells.mapIndexed { currentIndex, cell ->
                if (currentIndex == index && cell.value == null) {
                    val notes = if (value in cell.notes) cell.notes - value else cell.notes + value
                    cell.copy(notes = notes)
                } else {
                    cell
                }
            },
        )
    }

    fun clearCell(row: Int, column: Int): SudokuGrid {
        val index = row * Size + column
        return copy(
            cells = cells.mapIndexed { currentIndex, cell ->
                if (currentIndex == index) cell.copy(value = null, notes = emptySet()) else cell
            },
        )
    }

    fun removeNotes(row: Int, column: Int, values: Set<Int>): SudokuGrid {
        require(values.all { it in 1..9 }) { "Sudoku notes must be 1..9." }
        val index = row * Size + column
        return copy(
            cells = cells.mapIndexed { currentIndex, cell ->
                if (currentIndex == index && cell.value == null) {
                    cell.copy(notes = cell.notes - values)
                } else {
                    cell
                }
            },
        )
    }

    fun withCandidateNotes(): SudokuGrid = copy(
        cells = cells.mapIndexed { index, cell ->
            if (cell.value == null) {
                val row = index / Size
                val column = index % Size
                cell.copy(notes = allowedValuesAt(row, column))
            } else {
                cell.copy(notes = emptySet())
            }
        },
    )

    fun canSetValue(row: Int, column: Int, value: Int): Boolean {
        require(value in 1..9) { "Sudoku cell value must be 1..9." }
        return conflictFor(row, column, value) == null
    }

    fun allowedValuesAt(row: Int, column: Int): Set<Int> = (1..9)
        .filter { value -> canSetValue(row, column, value) }
        .toSet()

    fun conflictFor(row: Int, column: Int, value: Int): SudokuConflict? {
        require(value in 1..9) { "Sudoku cell value must be 1..9." }
        val sourceIndex = row * Size + column

        for (currentColumn in 0 until Size) {
            val index = row * Size + currentColumn
            if (index != sourceIndex && cells[index].value == value) return SudokuConflict.Row
        }

        for (currentRow in 0 until Size) {
            val index = currentRow * Size + column
            if (index != sourceIndex && cells[index].value == value) return SudokuConflict.Column
        }

        val boxRow = (row / 3) * 3
        val boxColumn = (column / 3) * 3
        for (currentRow in boxRow until boxRow + 3) {
            for (currentColumn in boxColumn until boxColumn + 3) {
                val index = currentRow * Size + currentColumn
                if (index != sourceIndex && cells[index].value == value) return SudokuConflict.Box
            }
        }

        return null
    }

    private fun peerIndexes(row: Int, column: Int): Set<Int> {
        val indexes = mutableSetOf<Int>()

        for (i in 0 until Size) {
            indexes += row * Size + i
            indexes += i * Size + column
        }

        val boxRow = (row / 3) * 3
        val boxColumn = (column / 3) * 3
        for (currentRow in boxRow until boxRow + 3) {
            for (currentColumn in boxColumn until boxColumn + 3) {
                indexes += currentRow * Size + currentColumn
            }
        }

        indexes -= row * Size + column
        return indexes
    }

    fun hasAnyValue(): Boolean = cells.any { it.value != null }

    fun values(): List<Int?> = cells.map { it.value }

    companion object {
        const val Size = 9
        const val CellCount = Size * Size

        val Empty = SudokuGrid(List(CellCount) { SudokuCell() })

        fun fromRows(rows: List<List<Int?>>, markAsGiven: Boolean = true): SudokuGrid {
            require(rows.size == Size) { "Sudoku must contain 9 rows." }
            require(rows.all { it.size == Size }) { "Each sudoku row must contain 9 values." }

            return SudokuGrid(
                rows.flatten().map { value ->
                    SudokuCell(value = value, isGiven = markAsGiven && value != null)
                },
            )
        }
    }
}

enum class SudokuConflict {
    Row,
    Column,
    Box,
}

enum class SolutionMode {
    Fast,
    StepByStep,
    SelfPractice,
}

enum class SudokuSolvingPattern {
    NakedSingle,
    HiddenSingleRow,
    HiddenSingleColumn,
    HiddenSingleBox,
    NakedPair,
    NakedTriple,
    NakedQuad,
    HiddenPair,
    HiddenTriple,
    HiddenQuad,
    PointingPair,
    PointingTriple,
    BoxLineReduction,
}

data class CandidateElimination(
    val row: Int,
    val column: Int,
    val values: Set<Int>,
)

data class SudokuSolutionStep(
    val row: Int,
    val column: Int,
    val value: Int,
    val pattern: SudokuSolvingPattern,
    val relatedCells: List<CellPosition> = emptyList(),
    val eliminations: List<CandidateElimination> = emptyList(),
) {
    val isPlacement: Boolean = eliminations.isEmpty()
}

data class SudokuSolveResult(
    val solvedGrid: SudokuGrid,
    val steps: List<SudokuSolutionStep>,
)
