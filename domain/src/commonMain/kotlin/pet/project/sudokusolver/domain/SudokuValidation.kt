package pet.project.sudokusolver.domain

class SudokuValidationConflict(
    val type: SudokuConflict,
    val unitIndex: Int,
    val value: Int,
    positions: Set<CellPosition>,
) {
    val positions: Set<CellPosition> = positions.toSet()

    init {
        require(unitIndex in 0 until SudokuGrid.Size) { "Sudoku unit index must be 0..8." }
        require(value in 1..9) { "Sudoku conflict value must be 1..9." }
        require(positions.size >= 2) { "Sudoku conflict must contain at least two positions." }
    }

    override fun equals(other: Any?): Boolean = other is SudokuValidationConflict &&
        type == other.type &&
        unitIndex == other.unitIndex &&
        value == other.value &&
        positions == other.positions

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + unitIndex
        result = 31 * result + value
        result = 31 * result + positions.hashCode()
        return result
    }

    override fun toString(): String =
        "SudokuValidationConflict(type=$type, unitIndex=$unitIndex, value=$value, positions=$positions)"
}

class SudokuValidation(conflicts: List<SudokuValidationConflict>) {
    val conflicts: List<SudokuValidationConflict> = conflicts.toList()
    val isValid: Boolean = this.conflicts.isEmpty()
    val conflictPositions: Set<CellPosition> = this.conflicts.flatMap { it.positions }.toSet()

    fun conflictsAt(position: CellPosition): List<SudokuValidationConflict> =
        conflicts.filter { position in it.positions }

    override fun equals(other: Any?): Boolean = other is SudokuValidation && conflicts == other.conflicts

    override fun hashCode(): Int = conflicts.hashCode()

    override fun toString(): String = "SudokuValidation(conflicts=$conflicts)"
}

fun SudokuGrid.validate(): SudokuValidation {
    val units = buildList {
        sudokuRows().forEachIndexed { index, cells -> add(ValidationUnit(SudokuConflict.Row, index, cells)) }
        sudokuColumns().forEachIndexed { index, cells -> add(ValidationUnit(SudokuConflict.Column, index, cells)) }
        sudokuBoxes().forEachIndexed { index, cells -> add(ValidationUnit(SudokuConflict.Box, index, cells)) }
    }

    val conflicts = units.flatMap { unit ->
        unit.cells
            .mapNotNull { index -> cells[index].value?.let { value -> value to index } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .filterValues { indexes -> indexes.size > 1 }
            .map { (value, indexes) ->
                SudokuValidationConflict(
                    type = unit.type,
                    unitIndex = unit.index,
                    value = value,
                    positions = indexes.mapTo(linkedSetOf()) { it.toCellPosition() },
                )
            }
    }
    return SudokuValidation(conflicts)
}

private data class ValidationUnit(
    val type: SudokuConflict,
    val index: Int,
    val cells: List<Int>,
)
