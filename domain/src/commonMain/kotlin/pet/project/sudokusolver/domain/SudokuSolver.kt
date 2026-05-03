package pet.project.sudokusolver.domain

class SudokuSolver {
    fun solve(grid: SudokuGrid): SudokuSolveResult? = solveInternal(grid)

    fun hint(grid: SudokuGrid): SudokuSolutionStep? = solveInternal(grid)?.steps?.firstOrNull()

    private fun solveInternal(grid: SudokuGrid): SudokuSolveResult? {
        val workingBoard = grid.values().map { it ?: 0 }.toIntArray()
        if (!isValidBoard(workingBoard)) return null

        val steps = mutableListOf<SudokuSolutionStep>()
        while (true) {
            val step = findLogicalStep(workingBoard) ?: break
            workingBoard[step.row * SudokuGrid.Size + step.column] = step.value
            steps += step
        }

        val solvedBoard = workingBoard.copyOf()
        if (!solveBoard(solvedBoard)) return null

        for (index in solvedBoard.indices) {
            if (workingBoard[index] != 0) continue
            val row = index / SudokuGrid.Size
            val column = index % SudokuGrid.Size
            steps += SudokuSolutionStep(
                row = row,
                column = column,
                value = solvedBoard[index],
                pattern = SudokuSolvingPattern.CalculatedCandidate,
                relatedCells = filledPeers(workingBoard, index),
            )
        }

        val solvedGrid = SudokuGrid(
            solvedBoard.mapIndexed { index, value ->
                val source = grid.cells[index]
                SudokuCell(value = value, isGiven = source.isGiven)
            },
        )

        return SudokuSolveResult(solvedGrid = solvedGrid, steps = steps)
    }

    private fun findLogicalStep(board: IntArray): SudokuSolutionStep? {
        findNakedSingle(board)?.let { return it }
        findHiddenSingleInRows(board)?.let { return it }
        findHiddenSingleInColumns(board)?.let { return it }
        return findHiddenSingleInBoxes(board)
    }

    private fun findNakedSingle(board: IntArray): SudokuSolutionStep? {
        for (index in board.indices) {
            if (board[index] != 0) continue
            val candidates = candidatesFor(board, index)
            if (candidates.size == 1) {
                return SudokuSolutionStep(
                    row = index / SudokuGrid.Size,
                    column = index % SudokuGrid.Size,
                    value = candidates.first(),
                    pattern = SudokuSolvingPattern.NakedSingle,
                    relatedCells = filledPeers(board, index),
                )
            }
        }
        return null
    }

    private fun findHiddenSingleInRows(board: IntArray): SudokuSolutionStep? {
        for (row in 0 until SudokuGrid.Size) {
            for (value in 1..9) {
                val rowIndexes = (0 until SudokuGrid.Size).map { column -> row * SudokuGrid.Size + column }
                val positions = rowIndexes
                    .filter { index -> board[index] == 0 && canPlace(board, row, index % SudokuGrid.Size, value) }
                if (positions.size == 1) {
                    val index = positions.first()
                    return SudokuSolutionStep(
                        row = row,
                        column = index % SudokuGrid.Size,
                        value = value,
                        pattern = SudokuSolvingPattern.HiddenSingleRow,
                        relatedCells = rowIndexes.filter { it != index }.map { it.toCellPosition() },
                    )
                }
            }
        }
        return null
    }

    private fun findHiddenSingleInColumns(board: IntArray): SudokuSolutionStep? {
        for (column in 0 until SudokuGrid.Size) {
            for (value in 1..9) {
                val columnIndexes = (0 until SudokuGrid.Size).map { row -> row * SudokuGrid.Size + column }
                val positions = columnIndexes
                    .filter { index -> board[index] == 0 && canPlace(board, index / SudokuGrid.Size, column, value) }
                if (positions.size == 1) {
                    val index = positions.first()
                    return SudokuSolutionStep(
                        row = index / SudokuGrid.Size,
                        column = column,
                        value = value,
                        pattern = SudokuSolvingPattern.HiddenSingleColumn,
                        relatedCells = columnIndexes.filter { it != index }.map { it.toCellPosition() },
                    )
                }
            }
        }
        return null
    }

    private fun findHiddenSingleInBoxes(board: IntArray): SudokuSolutionStep? {
        for (boxRow in 0 until SudokuGrid.Size step 3) {
            for (boxColumn in 0 until SudokuGrid.Size step 3) {
                val boxIndexes = buildList {
                    for (row in boxRow until boxRow + 3) {
                        for (column in boxColumn until boxColumn + 3) {
                            add(row * SudokuGrid.Size + column)
                        }
                    }
                }
                for (value in 1..9) {
                    val positions = boxIndexes
                        .filter { index -> board[index] == 0 && canPlace(board, index / SudokuGrid.Size, index % SudokuGrid.Size, value) }
                    if (positions.size == 1) {
                        val index = positions.first()
                        return SudokuSolutionStep(
                            row = index / SudokuGrid.Size,
                            column = index % SudokuGrid.Size,
                            value = value,
                            pattern = SudokuSolvingPattern.HiddenSingleBox,
                            relatedCells = boxIndexes.filter { it != index }.map { it.toCellPosition() },
                        )
                    }
                }
            }
        }
        return null
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

    private fun filledPeers(board: IntArray, index: Int): List<CellPosition> = peerIndexes(index)
        .filter { board[it] != 0 }
        .map { it.toCellPosition() }

    private fun peerIndexes(index: Int): Set<Int> {
        val row = index / SudokuGrid.Size
        val column = index % SudokuGrid.Size
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

    private fun Int.toCellPosition() = CellPosition(
        row = this / SudokuGrid.Size,
        column = this % SudokuGrid.Size,
    )
}
