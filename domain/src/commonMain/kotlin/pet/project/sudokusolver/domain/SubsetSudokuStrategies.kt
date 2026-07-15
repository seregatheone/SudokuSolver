package pet.project.sudokusolver.domain

internal class NakedSubsetStrategy(
    private val size: Int,
    override val pattern: SudokuSolvingPattern,
) : SudokuStrategy {
    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (unit in sudokuUnits()) {
            val candidateIndexes = unit.filter { index ->
                state.board[index] == 0 && state.candidates[index].size in 2..size
            }
            for (subset in candidateIndexes.combinations(size)) {
                val subsetValues = subset.flatMap { index -> state.candidates[index] }.toSet()
                if (subsetValues.size != size) continue

                val eliminations = unit
                    .filter { index -> index !in subset && state.board[index] == 0 }
                    .mapNotNull { index ->
                        val removed = state.candidates[index].intersect(subsetValues).sorted().toSet()
                        if (removed.isEmpty()) null else CandidateElimination(index.row(), index.column(), removed)
                    }

                SudokuStepFactory.elimination(pattern, subset, eliminations)?.let { return it }
            }
        }
        return null
    }
}

internal class HiddenSubsetStrategy(
    private val size: Int,
    override val pattern: SudokuSolvingPattern,
) : SudokuStrategy {
    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (unit in sudokuUnits()) {
            for (values in (1..9).toList().combinations(size)) {
                val positionsByValue = values.map { value ->
                    unit.filter { index -> state.board[index] == 0 && value in state.candidates[index] }
                }
                if (positionsByValue.any { it.isEmpty() }) continue

                val subset = positionsByValue.flatten().distinct()
                if (subset.size != size) continue

                val allowedValues = values.toSet()
                val eliminations = subset.mapNotNull { index ->
                    val removed = (state.candidates[index] - allowedValues).sorted().toSet()
                    if (removed.isEmpty()) null else CandidateElimination(index.row(), index.column(), removed)
                }

                SudokuStepFactory.elimination(pattern, subset, eliminations)?.let { return it }
            }
        }
        return null
    }
}
