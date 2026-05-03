package pet.project.sudokusolver.domain

class SudokuSolver {
    fun solve(grid: SudokuGrid): SudokuSolveResult? = solveInternal(grid)

    fun hint(grid: SudokuGrid): SudokuSolutionStep? = solveInternal(grid)?.steps?.firstOrNull()

    private fun solveInternal(grid: SudokuGrid): SudokuSolveResult? {
        val board = grid.values().map { it ?: 0 }.toIntArray()
        if (!isValidBoard(board)) return null
        if (!solveBoard(board)) return null

        val solvedGrid = SudokuGrid(
            board.mapIndexed { index, value ->
                val source = grid.cells[index]
                SudokuCell(value = value, isGiven = source.isGiven)
            },
        )

        val steps = mutableListOf<SudokuSolutionStep>()
        for (index in board.indices) {
            val solvedValue = board[index]
            val sourceValue = grid.cells[index].value
            if (sourceValue != solvedValue) {
                val row = index / SudokuGrid.Size
                val column = index % SudokuGrid.Size
                steps += SudokuSolutionStep(
                    row = row,
                    column = column,
                    value = solvedValue,
                )
            }
        }

        return SudokuSolveResult(solvedGrid = solvedGrid, steps = steps)
    }

    private fun solveBoard(board: IntArray): Boolean {
        val emptyIndex = findEmptyCellWithFewestCandidates(board) ?: return true
        for (candidate in candidatesFor(board, emptyIndex)) {
            board[emptyIndex] = candidate
            if (solveBoard(board)) return true
            board[emptyIndex] = 0
        }
        return false
    }

    private fun findEmptyCellWithFewestCandidates(board: IntArray): Int? {
        var bestIndex: Int? = null
        var bestCandidateCount = Int.MAX_VALUE

        for (index in board.indices) {
            if (board[index] != 0) continue
            val candidateCount = candidatesFor(board, index).size
            if (candidateCount == 0) return index
            if (candidateCount < bestCandidateCount) {
                bestCandidateCount = candidateCount
                bestIndex = index
            }
        }

        return bestIndex
    }

    private fun candidatesFor(board: IntArray, index: Int): List<Int> {
        val row = index / SudokuGrid.Size
        val column = index % SudokuGrid.Size
        return (1..9).filter { value -> canPlace(board, row, column, value) }
    }

    private fun canPlace(board: IntArray, row: Int, column: Int, value: Int): Boolean {
        for (i in 0 until SudokuGrid.Size) {
            if (board[row * SudokuGrid.Size + i] == value) return false
            if (board[i * SudokuGrid.Size + column] == value) return false
        }

        val boxRow = (row / 3) * 3
        val boxColumn = (column / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxColumn until boxColumn + 3) {
                if (board[r * SudokuGrid.Size + c] == value) return false
            }
        }

        return true
    }

    private fun isValidBoard(board: IntArray): Boolean {
        for (index in board.indices) {
            val value = board[index]
            if (value == 0) continue
            board[index] = 0
            val isValid = canPlace(board, index / SudokuGrid.Size, index % SudokuGrid.Size, value)
            board[index] = value
            if (!isValid) return false
        }
        return true
    }
}
