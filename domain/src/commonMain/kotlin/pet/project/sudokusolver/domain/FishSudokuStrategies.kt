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

internal object KrakenFishStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.KrakenFish

    private const val MaxChainDepth = 7
    private const val MaximumFinCount = 3

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val inferenceGraph = CandidateInferenceGraph.from(state)
        for (size in 2..3) {
            findByBaseUnits(
                size = size,
                baseUnits = sudokuRows(),
                coverUnits = sudokuColumns(),
                state = state,
                inferenceGraph = inferenceGraph,
            )?.let { return it }
            findByBaseUnits(
                size = size,
                baseUnits = sudokuColumns(),
                coverUnits = sudokuRows(),
                state = state,
                inferenceGraph = inferenceGraph,
            )?.let { return it }
        }
        return null
    }

    private fun findByBaseUnits(
        size: Int,
        baseUnits: List<List<Int>>,
        coverUnits: List<List<Int>>,
        state: SudokuBoardState,
        inferenceGraph: CandidateInferenceGraph,
    ): SudokuSolutionStep? {
        val coverIndexByCell = IntArray(SudokuGrid.CellCount)
        coverUnits.forEachIndexed { coverIndex, unit ->
            unit.forEach { index -> coverIndexByCell[index] = coverIndex }
        }

        for (value in 1..SudokuGrid.Size) {
            val singleDigitGraph = inferenceGraph.forDigit(value)
            val pathCache = mutableMapOf<Pair<Int, Int>, AlternatingInferencePath<CandidateNode>?>()
            val baseOptions = baseUnits
                .mapIndexed { unitIndex, indexes ->
                    unitIndex to indexes.filter { index ->
                        state.board[index] == 0 && value in state.candidates[index]
                    }
                }
                .filter { (_, positions) -> positions.size in 2..(size + MaximumFinCount) }

            for (baseSubset in baseOptions.combinations(size)) {
                val possibleCoverIndexes = baseSubset
                    .flatMap { (_, positions) -> positions.map { index -> coverIndexByCell[index] } }
                    .distinct()
                    .sorted()
                for (coverIndexes in possibleCoverIndexes.combinations(size)) {
                    val coverIndexSet = coverIndexes.toSet()
                    if (
                        baseSubset.any { (_, positions) ->
                            positions.none { index -> coverIndexByCell[index] in coverIndexSet }
                        }
                    ) {
                        continue
                    }

                    val fishCells = baseSubset.flatMap { (_, positions) -> positions }.distinct().sorted()
                    val bodyCells = fishCells.filter { index -> coverIndexByCell[index] in coverIndexSet }
                    if (
                        coverIndexes.any { coverIndex ->
                            bodyCells.count { index -> coverIndexByCell[index] == coverIndex } < 2
                        }
                    ) {
                        continue
                    }

                    val fins = fishCells.filter { index -> coverIndexByCell[index] !in coverIndexSet }
                    if (fins.size !in 1..MaximumFinCount) continue

                    val victims = coverIndexes
                        .flatMap { coverIndex -> coverUnits[coverIndex] }
                        .distinct()
                        .filter { index ->
                            index !in fishCells &&
                                state.board[index] == 0 &&
                                value in state.candidates[index]
                        }
                        .sorted()

                    for (victim in victims) {
                        // All fins false activates the fish; every true-fin branch must also make the victim false.
                        val paths = fins.map { fin ->
                            findFinPath(
                                graph = singleDigitGraph,
                                value = value,
                                fin = fin,
                                victim = victim,
                                cache = pathCache,
                            )
                        }
                        if (paths.any { path -> path == null }) continue

                        val provenPaths = paths.filterNotNull()
                        if (provenPaths.none { path -> path.depth >= 3 }) continue

                        val relatedIndexes = buildList {
                            addAll(fishCells)
                            provenPaths.forEach { path ->
                                addAll(path.nodes.dropLast(1).map { node -> node.index })
                            }
                        }.distinct().sorted()
                        val elimination = CandidateElimination(
                            row = victim.row(),
                            column = victim.column(),
                            values = setOf(value),
                        )
                        return SudokuStepFactory.elimination(
                            pattern = pattern,
                            relatedIndexes = relatedIndexes,
                            eliminations = listOf(elimination),
                        )
                    }
                }
            }
        }
        return null
    }

    private fun findFinPath(
        graph: SingleDigitLinkGraph,
        value: Int,
        fin: Int,
        victim: Int,
        cache: MutableMap<Pair<Int, Int>, AlternatingInferencePath<CandidateNode>?>,
    ): AlternatingInferencePath<CandidateNode>? {
        val key = fin to victim
        if (key in cache) return cache[key]

        return findAlternatingPath(
            graph = graph,
            start = CandidateNode(index = fin, value = value),
            end = CandidateNode(index = victim, value = value),
            firstLink = InferenceLinkType.Weak,
            lastLink = InferenceLinkType.Weak,
            maxDepth = MaxChainDepth,
        ).also { path -> cache[key] = path }
    }
}
