package pet.project.sudokusolver.domain

internal object SudokuBacktrackingSolver {
    fun solve(board: IntArray): Boolean {
        val solution = findSolutions(board, limit = 1).firstOrNull() ?: return false
        solution.copyInto(board)
        return true
    }

    fun findSolutions(board: IntArray, limit: Int = 2): List<IntArray> {
        require(limit > 0) { "Solution limit must be positive." }
        val workingBoard = board.copyOf()
        val solutions = mutableListOf<IntArray>()
        collectSolutions(workingBoard, solutions, limit)
        return solutions
    }

    private fun collectSolutions(
        board: IntArray,
        solutions: MutableList<IntArray>,
        limit: Int,
    ): Boolean {
        val emptyIndex = findEmptyCellWithFewestCandidates(board)
        if (emptyIndex == null) {
            solutions += board.copyOf()
            return solutions.size >= limit
        }

        for (candidate in SudokuRules.candidatesFor(board, emptyIndex)) {
            board[emptyIndex] = candidate
            val reachedLimit = collectSolutions(board, solutions, limit)
            board[emptyIndex] = 0
            if (reachedLimit) return true
        }
        return false
    }

    private fun findEmptyCellWithFewestCandidates(board: IntArray): Int? {
        var bestIndex: Int? = null
        var bestCandidateCount = Int.MAX_VALUE

        for (index in board.indices) {
            if (board[index] != 0) continue
            val candidateCount = SudokuRules.candidatesFor(board, index).size
            if (candidateCount == 0) return index
            if (candidateCount < bestCandidateCount) {
                bestCandidateCount = candidateCount
                bestIndex = index
            }
        }

        return bestIndex
    }
}
