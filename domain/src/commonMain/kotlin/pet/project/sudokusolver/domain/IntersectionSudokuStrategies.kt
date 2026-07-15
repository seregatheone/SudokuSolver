package pet.project.sudokusolver.domain

internal object LockedCandidatesPointingStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.LockedCandidatesPointing

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (box in sudokuBoxes()) {
            for (value in 1..9) {
                val positions = box.filter { index -> state.board[index] == 0 && value in state.candidates[index] }
                if (positions.size !in 2..3) continue

                val sameRow = positions.map { it.row() }.distinct().singleOrNull()
                if (sameRow != null) {
                    val eliminations = sudokuRows()[sameRow]
                        .filter { index -> index !in box && state.board[index] == 0 && value in state.candidates[index] }
                        .map { index -> CandidateElimination(index.row(), index.column(), setOf(value)) }
                    SudokuStepFactory.elimination(pattern, positions, eliminations)?.let { return it }
                }

                val sameColumn = positions.map { it.column() }.distinct().singleOrNull()
                if (sameColumn != null) {
                    val eliminations = sudokuColumns()[sameColumn]
                        .filter { index -> index !in box && state.board[index] == 0 && value in state.candidates[index] }
                        .map { index -> CandidateElimination(index.row(), index.column(), setOf(value)) }
                    SudokuStepFactory.elimination(pattern, positions, eliminations)?.let { return it }
                }
            }
        }
        return null
    }
}

internal object ClaimingBoxLineReductionStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.ClaimingBoxLineReduction

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (row in sudokuRows()) {
            boxLineReductionInUnit(row, state)?.let { return it }
        }
        for (column in sudokuColumns()) {
            boxLineReductionInUnit(column, state)?.let { return it }
        }
        return null
    }

    private fun boxLineReductionInUnit(
        unit: List<Int>,
        state: SudokuBoardState,
    ): SudokuSolutionStep? {
        for (value in 1..9) {
            val positions = unit.filter { index -> state.board[index] == 0 && value in state.candidates[index] }
            if (positions.size < 2) continue

            val box = sudokuBoxes().singleOrNull { currentBox -> positions.all { it in currentBox } } ?: continue
            val eliminations = box
                .filter { index -> index !in unit && state.board[index] == 0 && value in state.candidates[index] }
                .map { index -> CandidateElimination(index.row(), index.column(), setOf(value)) }
            SudokuStepFactory.elimination(pattern, positions, eliminations)?.let { return it }
        }
        return null
    }
}
