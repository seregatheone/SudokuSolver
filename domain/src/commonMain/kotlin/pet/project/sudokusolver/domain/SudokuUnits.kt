package pet.project.sudokusolver.domain

internal fun sudokuUnits(): List<List<Int>> = sudokuRows() + sudokuColumns() + sudokuBoxes()

internal fun sudokuRows(): List<List<Int>> = (0 until SudokuGrid.Size).map { row ->
    (0 until SudokuGrid.Size).map { column -> row * SudokuGrid.Size + column }
}

internal fun sudokuColumns(): List<List<Int>> = (0 until SudokuGrid.Size).map { column ->
    (0 until SudokuGrid.Size).map { row -> row * SudokuGrid.Size + column }
}

internal fun sudokuBoxes(): List<List<Int>> = buildList {
    for (boxRow in 0 until SudokuGrid.Size step 3) {
        for (boxColumn in 0 until SudokuGrid.Size step 3) {
            add(
                buildList {
                    for (row in boxRow until boxRow + 3) {
                        for (column in boxColumn until boxColumn + 3) {
                            add(row * SudokuGrid.Size + column)
                        }
                    }
                },
            )
        }
    }
}

internal fun <T> List<T>.combinations(size: Int): List<List<T>> {
    if (size == 0) return listOf(emptyList())
    if (size > this.size) return emptyList()

    val result = mutableListOf<List<T>>()
    fun collect(start: Int, current: List<T>) {
        if (current.size == size) {
            result += current
            return
        }
        for (index in start until this.size) {
            collect(index + 1, current + this[index])
        }
    }
    collect(start = 0, current = emptyList())
    return result
}

internal fun Int.row(): Int = this / SudokuGrid.Size

internal fun Int.column(): Int = this % SudokuGrid.Size

internal fun Int.toCellPosition() = CellPosition(row = row(), column = column())
