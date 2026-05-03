package pet.project.sudokusolver.domain

internal object NakedSingleStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.NakedSingle

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (index in state.board.indices) {
            if (state.board[index] != 0) continue
            if (state.candidates[index].size == 1) {
                return SudokuStepFactory.placement(
                    index = index,
                    value = state.candidates[index].first(),
                    pattern = pattern,
                    relatedIndexes = SudokuRules.filledPeers(state.board, index).map { it.index },
                )
            }
        }
        return null
    }
}

internal object HiddenSingleStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.HiddenSingle

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        sudokuRows().forEach { indexes -> hiddenSingleInUnit(indexes, state)?.let { return it } }
        sudokuColumns().forEach { indexes -> hiddenSingleInUnit(indexes, state)?.let { return it } }
        sudokuBoxes().forEach { indexes -> hiddenSingleInUnit(indexes, state)?.let { return it } }
        return null
    }

    private fun hiddenSingleInUnit(
        indexes: List<Int>,
        state: SudokuBoardState,
    ): SudokuSolutionStep? {
        for (value in 1..9) {
            val positions = indexes.filter { index -> state.board[index] == 0 && value in state.candidates[index] }
            if (positions.size == 1) {
                val index = positions.first()
                return SudokuStepFactory.placement(
                    index = index,
                    value = value,
                    pattern = pattern,
                    relatedIndexes = indexes.filter { it != index },
                )
            }
        }
        return null
    }
}
