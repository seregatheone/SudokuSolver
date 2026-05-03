package pet.project.sudokusolver.domain

internal object SudokuStepFactory {
    fun placement(
        index: Int,
        value: Int,
        pattern: SudokuSolvingPattern,
        relatedIndexes: List<Int>,
    ) = SudokuSolutionStep(
        row = index.row(),
        column = index.column(),
        value = value,
        pattern = pattern,
        relatedCells = relatedIndexes.distinct().filter { it != index }.map { it.toCellPosition() },
    )

    fun elimination(
        pattern: SudokuSolvingPattern,
        relatedIndexes: List<Int>,
        eliminations: List<CandidateElimination>,
    ): SudokuSolutionStep? {
        val first = eliminations.firstOrNull() ?: return null
        return SudokuSolutionStep(
            row = first.row,
            column = first.column,
            value = first.values.minOrNull() ?: return null,
            pattern = pattern,
            relatedCells = relatedIndexes.distinct().map { it.toCellPosition() },
            eliminations = eliminations,
        )
    }
}
