package pet.project.sudokusolver.domain

internal object UniqueRectangleStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.UniqueRectangle

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (rowPair in (0 until SudokuGrid.Size).toList().combinations(2)) {
            for (columnPair in (0 until SudokuGrid.Size).toList().combinations(2)) {
                val indexes = rowPair.flatMap { row -> columnPair.map { column -> row * SudokuGrid.Size + column } }
                if (indexes.any { state.board[it] != 0 }) continue
                if (indexes.map(::boxIndex).distinct().size != 2) continue

                findTypeOne(state, indexes)?.let { return it }
                findHiddenRectangle(state, indexes)?.let { return it }
            }
        }
        return null
    }

    private fun findTypeOne(
        state: SudokuBoardState,
        indexes: List<Int>,
    ): SudokuSolutionStep? {
        val pairs = indexes.filter { state.candidates[it].size == 2 }
        if (pairs.size != 3) return null
        val pairValues = pairs.map { state.candidates[it].toSet() }.distinct().singleOrNull() ?: return null
        val extraIndex = indexes.first { it !in pairs }
        if (!state.candidates[extraIndex].containsAll(pairValues) || state.candidates[extraIndex].size <= 2) return null

        val eliminations = listOf(CandidateElimination(extraIndex.row(), extraIndex.column(), pairValues))
        return SudokuStepFactory.elimination(pattern, indexes, eliminations)
    }

    private fun findHiddenRectangle(
        state: SudokuBoardState,
        indexes: List<Int>,
    ): SudokuSolutionStep? {
        val pairValuesOptions = indexes
            .filter { state.candidates[it].size == 2 }
            .map { state.candidates[it].toSet() }
            .distinct()
        for (pairValues in pairValuesOptions) {
            if (indexes.any { !state.candidates[it].containsAll(pairValues) }) continue
            val purePairIndexes = indexes.filter { state.candidates[it] == pairValues }
            if (purePairIndexes.size !in 1..2) continue

            for (startIndex in purePairIndexes) {
                val targetIndex = indexes.single { index ->
                    index.row() != startIndex.row() && index.column() != startIndex.column()
                }
                for (strongValue in pairValues) {
                    val rowHasOutsideCandidate = sudokuRows()[targetIndex.row()].any { index ->
                        index !in indexes && state.board[index] == 0 && strongValue in state.candidates[index]
                    }
                    val columnHasOutsideCandidate = sudokuColumns()[targetIndex.column()].any { index ->
                        index !in indexes && state.board[index] == 0 && strongValue in state.candidates[index]
                    }
                    if (rowHasOutsideCandidate || columnHasOutsideCandidate) continue

                    val removedValue = pairValues.single { it != strongValue }
                    if (removedValue !in state.candidates[targetIndex]) continue
                    val eliminations = listOf(
                        CandidateElimination(targetIndex.row(), targetIndex.column(), setOf(removedValue)),
                    )
                    SudokuStepFactory.elimination(pattern, indexes, eliminations)?.let { return it }
                }
            }
        }
        return null
    }

    private fun boxIndex(index: Int): Int = index.row() / 3 * 3 + index.column() / 3
}
