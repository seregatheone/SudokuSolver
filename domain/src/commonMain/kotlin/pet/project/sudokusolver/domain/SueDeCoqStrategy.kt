package pet.project.sudokusolver.domain

internal object SueDeCoqStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.SueDeCoq

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (box in sudokuBoxes()) {
            val rows = box.map { it.row() }.distinct()
            for (row in rows) {
                findInIntersection(
                    state = state,
                    box = box,
                    line = sudokuRows()[row],
                    intersection = box.filter { it.row() == row && state.board[it] == 0 },
                )?.let { return it }
            }

            val columns = box.map { it.column() }.distinct()
            for (column in columns) {
                findInIntersection(
                    state = state,
                    box = box,
                    line = sudokuColumns()[column],
                    intersection = box.filter { it.column() == column && state.board[it] == 0 },
                )?.let { return it }
            }
        }
        return null
    }

    private fun findInIntersection(
        state: SudokuBoardState,
        box: List<Int>,
        line: List<Int>,
        intersection: List<Int>,
    ): SudokuSolutionStep? {
        if (intersection.size !in 2..3) return null
        val intersectionValues = intersection.flatMap { state.candidates[it] }.toSet()
        if (intersectionValues.size != intersection.size + 2) return null

        val lineCells = line.filter { index ->
            index !in box && state.board[index] == 0 &&
                state.candidates[index].size == 2 &&
                intersectionValues.containsAll(state.candidates[index])
        }
        val boxCells = box.filter { index ->
            index !in line && state.board[index] == 0 &&
                state.candidates[index].size == 2 &&
                intersectionValues.containsAll(state.candidates[index])
        }

        for (lineCell in lineCells) {
            val lineValues = state.candidates[lineCell].toSet()
            for (boxCell in boxCells) {
                val boxValues = state.candidates[boxCell].toSet()
                if (lineValues.intersect(boxValues).isNotEmpty()) continue

                val sharedValues = intersectionValues - lineValues - boxValues
                val relatedIndexes = intersection + lineCell + boxCell
                val removals = mutableMapOf<Int, MutableSet<Int>>()
                collectRemovals(
                    state = state,
                    indexes = line.filter { it !in relatedIndexes },
                    values = lineValues + sharedValues,
                    removals = removals,
                )
                collectRemovals(
                    state = state,
                    indexes = box.filter { it !in relatedIndexes },
                    values = boxValues + sharedValues,
                    removals = removals,
                )
                val eliminations = removals.entries
                    .sortedBy { it.key }
                    .map { (index, values) ->
                        CandidateElimination(index.row(), index.column(), values.toSet())
                    }

                SudokuStepFactory.elimination(
                    pattern = pattern,
                    relatedIndexes = relatedIndexes,
                    eliminations = eliminations,
                )?.let { return it }
            }
        }
        return null
    }

    private fun collectRemovals(
        state: SudokuBoardState,
        indexes: List<Int>,
        values: Set<Int>,
        removals: MutableMap<Int, MutableSet<Int>>,
    ) {
        for (index in indexes) {
            if (state.board[index] != 0) continue
            val removed = state.candidates[index].intersect(values)
            if (removed.isNotEmpty()) removals.getOrPut(index, ::mutableSetOf).addAll(removed)
        }
    }
}
