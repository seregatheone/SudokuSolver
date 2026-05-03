package pet.project.sudokusolver.domain

internal object SudokuBacktrackingSolver {
    fun solve(board: IntArray): Boolean {
        val emptyIndex = findEmptyCellWithFewestCandidates(board) ?: return true
        for (candidate in SudokuRules.candidatesFor(board, emptyIndex)) {
            board[emptyIndex] = candidate
            if (solve(board)) return true
            board[emptyIndex] = 0
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
