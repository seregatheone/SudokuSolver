package pet.project.sudokusolver.domain

internal class FishStrategy(
    private val size: Int,
    override val pattern: SudokuSolvingPattern,
) : SudokuStrategy {
    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        findByBaseUnits(sudokuRows(), sudokuColumns(), state)?.let { return it }
        return findByBaseUnits(sudokuColumns(), sudokuRows(), state)
    }

    private fun findByBaseUnits(
        baseUnits: List<List<Int>>,
        coverUnits: List<List<Int>>,
        state: SudokuBoardState,
    ): SudokuSolutionStep? {
        for (value in 1..9) {
            val baseOptions = baseUnits
                .mapIndexed { unitIndex, indexes ->
                    unitIndex to indexes.filter { index -> state.board[index] == 0 && value in state.candidates[index] }
                }
                .filter { (_, positions) -> positions.size in 2..size }

            for (subset in baseOptions.combinations(size)) {
                val coverIndexes = subset
                    .flatMap { (_, positions) -> positions.map { index -> coverUnits.indexOfFirst { index in it } } }
                    .distinct()
                if (coverIndexes.size != size) continue

                val fishCells = subset.flatMap { (_, positions) -> positions }.distinct()
                val eliminations = coverIndexes
                    .flatMap { coverUnits[it] }
                    .filter { index -> index !in fishCells && state.board[index] == 0 && value in state.candidates[index] }
                    .map { index -> CandidateElimination(index.row(), index.column(), setOf(value)) }
                SudokuStepFactory.elimination(pattern, fishCells, eliminations)?.let { return it }
            }
        }
        return null
    }
}

internal class FinnedFishStrategy(
    private val size: Int,
    override val pattern: SudokuSolvingPattern,
    private val requireSashimi: Boolean = false,
) : SudokuStrategy {
    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        findByBaseUnits(sudokuRows(), sudokuColumns(), state)?.let { return it }
        return findByBaseUnits(sudokuColumns(), sudokuRows(), state)
    }

    private fun findByBaseUnits(
        baseUnits: List<List<Int>>,
        coverUnits: List<List<Int>>,
        state: SudokuBoardState,
    ): SudokuSolutionStep? {
        for (value in 1..9) {
            val baseOptions = baseUnits
                .mapIndexed { unitIndex, indexes ->
                    unitIndex to indexes.filter { index -> state.board[index] == 0 && value in state.candidates[index] }
                }
                .filter { (_, positions) -> positions.size in 2..(size + 1) }

            for (subset in baseOptions.combinations(size)) {
                val coverCounts = subset
                    .flatMap { (_, positions) -> positions.map { index -> coverUnits.indexOfFirst { index in it } } }
                    .groupingBy { it }
                    .eachCount()
                val coverIndexes = coverCounts.filterValues { it >= 1 }.keys.toList()
                if (coverIndexes.size != size + 1) continue

                val fishCoverIndexes = coverCounts.filterValues { it >= 2 }.keys
                if (fishCoverIndexes.size != size) continue

                val fishCells = subset.flatMap { (_, positions) -> positions }.distinct()
                val fins = fishCells.filter { index -> coverUnits.indexOfFirst { index in it } !in fishCoverIndexes }
                if (fins.isEmpty()) continue
                if (requireSashimi && subset.none { (_, positions) -> positions.count { it !in fins } == 1 }) continue

                val finPeerIndexes = fins.map { SudokuRules.peerIndexes(it) }.reduce { acc, peers -> acc.intersect(peers) }
                val eliminations = fishCoverIndexes
                    .flatMap { coverUnits[it] }
                    .filter { index ->
                        index !in fishCells &&
                            index in finPeerIndexes &&
                            state.board[index] == 0 &&
                            value in state.candidates[index]
                    }
                    .map { index -> CandidateElimination(index.row(), index.column(), setOf(value)) }
                SudokuStepFactory.elimination(pattern, fishCells, eliminations)?.let { return it }
            }
        }
        return null
    }
}
