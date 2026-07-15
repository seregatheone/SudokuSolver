package pet.project.sudokusolver.domain

internal object SudokuRules {
    fun candidatesFor(board: IntArray, index: Int): List<Int> {
        val row = index.row()
        val column = index.column()
        return (1..9).filter { value -> canPlace(board, row, column, value) }
    }

    fun canPlace(board: IntArray, row: Int, column: Int, value: Int): Boolean {
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

    fun isValidBoard(board: IntArray): Boolean {
        for (index in board.indices) {
            val value = board[index]
            if (value == 0) continue
            board[index] = 0
            val isValid = canPlace(board, index.row(), index.column(), value)
            board[index] = value
            if (!isValid) return false
        }
        return true
    }

    fun filledPeers(board: IntArray, index: Int): List<CellPosition> = peerIndexes(index)
        .filter { board[it] != 0 }
        .map { it.toCellPosition() }

    fun peerIndexes(index: Int): Set<Int> {
        val row = index.row()
        val column = index.column()
        val peers = mutableSetOf<Int>()

        for (i in 0 until SudokuGrid.Size) {
            peers += row * SudokuGrid.Size + i
            peers += i * SudokuGrid.Size + column
        }

        val boxRow = (row / 3) * 3
        val boxColumn = (column / 3) * 3
        for (r in boxRow until boxRow + 3) {
            for (c in boxColumn until boxColumn + 3) {
                peers += r * SudokuGrid.Size + c
            }
        }

        peers -= index
        return peers
    }
}
