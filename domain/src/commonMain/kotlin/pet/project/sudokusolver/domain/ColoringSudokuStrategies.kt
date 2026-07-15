package pet.project.sudokusolver.domain

internal object SimpleColoringStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.SimpleColoring

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (value in 1..9) {
            val graph = buildStrongLinkGraph(state, value)
            val unvisited = graph.keys.toMutableSet()
            while (unvisited.isNotEmpty()) {
                val start = unvisited.first()
                val colors = colorComponent(graph, start)
                if (colors == null) {
                    unvisited.remove(start)
                    continue
                }
                unvisited.removeAll(colors.keys)
                val component = colors.keys.sorted()
                val colorGroups = (0..1).associateWith { color ->
                    colors.filterValues { it == color }.keys
                }

                for (color in 0..1) {
                    val group = colorGroups.getValue(color)
                    val hasConflict = group.any { first ->
                        SudokuRules.peerIndexes(first).any { it in group }
                    }
                    if (!hasConflict) continue

                    val eliminations = group.sorted().map { index ->
                        CandidateElimination(index.row(), index.column(), setOf(value))
                    }
                    SudokuStepFactory.elimination(
                        pattern = pattern,
                        relatedIndexes = component,
                        eliminations = eliminations,
                    )?.let { return it }
                }

                val firstColor = colorGroups.getValue(0)
                val secondColor = colorGroups.getValue(1)
                val eliminations = state.board.indices
                    .filter { index ->
                        index !in colors &&
                            state.board[index] == 0 &&
                            value in state.candidates[index] &&
                            SudokuRules.peerIndexes(index).any { it in firstColor } &&
                            SudokuRules.peerIndexes(index).any { it in secondColor }
                    }
                    .map { index ->
                        CandidateElimination(index.row(), index.column(), setOf(value))
                    }
                SudokuStepFactory.elimination(
                    pattern = pattern,
                    relatedIndexes = component,
                    eliminations = eliminations,
                )?.let { return it }
            }
        }
        return null
    }

    private fun buildStrongLinkGraph(
        state: SudokuBoardState,
        value: Int,
    ): Map<Int, Set<Int>> {
        val graph = mutableMapOf<Int, MutableSet<Int>>()
        for (unit in sudokuUnits()) {
            val indexes = unit.filter { index ->
                state.board[index] == 0 && value in state.candidates[index]
            }
            if (indexes.size != 2) continue
            val (first, second) = indexes
            graph.getOrPut(first, ::mutableSetOf).add(second)
            graph.getOrPut(second, ::mutableSetOf).add(first)
        }
        return graph
    }

    private fun colorComponent(
        graph: Map<Int, Set<Int>>,
        start: Int,
    ): Map<Int, Int>? {
        val colors = mutableMapOf(start to 0)
        val queue = ArrayDeque<Int>().apply { addLast(start) }
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val nextColor = 1 - colors.getValue(index)
            for (neighbor in graph[index].orEmpty()) {
                val existingColor = colors[neighbor]
                if (existingColor == null) {
                    colors[neighbor] = nextColor
                    queue.addLast(neighbor)
                } else if (existingColor != nextColor) {
                    return null
                }
            }
        }
        return colors
    }
}

internal object RemotePairStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.RemotePair

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val cellsByValues = state.board.indices
            .filter { index -> state.board[index] == 0 && state.candidates[index].size == 2 }
            .groupBy { index -> state.candidates[index].toSet() }

        for ((pairValues, indexes) in cellsByValues) {
            if (indexes.size < 4) continue
            val indexSet = indexes.toSet()
            val graph = indexes.associateWith { index ->
                SudokuRules.peerIndexes(index).intersect(indexSet)
            }
            val unvisited = indexes.toMutableSet()
            while (unvisited.isNotEmpty()) {
                val start = unvisited.first()
                val colors = colorComponent(graph, start)
                if (colors == null) {
                    unvisited.remove(start)
                    continue
                }
                unvisited.removeAll(colors.keys)
                if (colors.size < 4) continue
                val firstColor = colors.filterValues { it == 0 }.keys
                val secondColor = colors.filterValues { it == 1 }.keys
                val eliminations = state.board.indices.mapNotNull { index ->
                    if (
                        index in colors ||
                        state.board[index] != 0 ||
                        SudokuRules.peerIndexes(index).none { it in firstColor } ||
                        SudokuRules.peerIndexes(index).none { it in secondColor }
                    ) {
                        return@mapNotNull null
                    }
                    val removed = state.candidates[index].intersect(pairValues).toSet()
                    removed.takeIf { it.isNotEmpty() }?.let { values ->
                        CandidateElimination(index.row(), index.column(), values)
                    }
                }

                SudokuStepFactory.elimination(
                    pattern = pattern,
                    relatedIndexes = colors.keys.sorted(),
                    eliminations = eliminations,
                )?.let { return it }
            }
        }
        return null
    }

    private fun colorComponent(
        graph: Map<Int, Set<Int>>,
        start: Int,
    ): Map<Int, Int>? {
        val colors = mutableMapOf(start to 0)
        val queue = ArrayDeque<Int>().apply { addLast(start) }
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val nextColor = 1 - colors.getValue(index)
            for (neighbor in graph[index].orEmpty()) {
                val existingColor = colors[neighbor]
                if (existingColor == null) {
                    colors[neighbor] = nextColor
                    queue.addLast(neighbor)
                } else if (existingColor != nextColor) {
                    return null
                }
            }
        }
        return colors
    }
}
