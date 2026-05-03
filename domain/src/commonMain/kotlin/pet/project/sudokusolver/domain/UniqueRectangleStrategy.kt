package pet.project.sudokusolver.domain

internal object UniqueRectangleStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.UniqueRectangle

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (rowPair in (0 until SudokuGrid.Size).toList().combinations(2)) {
            for (columnPair in (0 until SudokuGrid.Size).toList().combinations(2)) {
                val indexes = rowPair.flatMap { row -> columnPair.map { column -> row * SudokuGrid.Size + column } }
                if (indexes.any { state.board[it] != 0 }) continue

                val pairs = indexes.filter { state.candidates[it].size == 2 }
                if (pairs.size != 3) continue
                val pairValues = pairs.map { state.candidates[it] }.distinct().singleOrNull() ?: continue
                val extraIndex = indexes.first { it !in pairs }
                if (!state.candidates[extraIndex].containsAll(pairValues) || state.candidates[extraIndex].size <= 2) continue

                val eliminations = listOf(CandidateElimination(extraIndex.row(), extraIndex.column(), pairValues))
                SudokuStepFactory.elimination(pattern, indexes, eliminations)?.let { return it }
            }
        }
        return null
    }
}
