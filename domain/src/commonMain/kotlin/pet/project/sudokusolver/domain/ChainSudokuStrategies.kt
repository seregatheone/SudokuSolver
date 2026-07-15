package pet.project.sudokusolver.domain

internal object XYChainStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.XYChain

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val bivalueIndexes = state.board.indices.filter { index ->
            state.board[index] == 0 && state.candidates[index].size == 2
        }

        for (start in bivalueIndexes) {
            for (eliminationValue in state.candidates[start]) {
                val outgoingValue = state.candidates[start].single { it != eliminationValue }
                val queue = ArrayDeque<ChainPath>().apply {
                    addLast(ChainPath(indexes = listOf(start), outgoingValue = outgoingValue))
                }
                while (queue.isNotEmpty()) {
                    val path = queue.removeFirst()
                    val current = path.indexes.last()
                    for (next in bivalueIndexes) {
                        if (
                            next in path.indexes ||
                            next !in SudokuRules.peerIndexes(current) ||
                            path.outgoingValue !in state.candidates[next]
                        ) {
                            continue
                        }

                        val nextOutgoingValue = state.candidates[next].single { it != path.outgoingValue }
                        val nextIndexes = path.indexes + next
                        if (nextOutgoingValue == eliminationValue && nextIndexes.size >= MinimumCellCount) {
                            val eliminations = SudokuRules.peerIndexes(start)
                                .intersect(SudokuRules.peerIndexes(next))
                                .filter { index ->
                                    index !in nextIndexes &&
                                        state.board[index] == 0 &&
                                        eliminationValue in state.candidates[index]
                                }
                                .sorted()
                                .map { index ->
                                    CandidateElimination(index.row(), index.column(), setOf(eliminationValue))
                                }
                            SudokuStepFactory.elimination(
                                pattern = pattern,
                                relatedIndexes = nextIndexes,
                                eliminations = eliminations,
                            )?.let { return it }
                        }

                        if (nextIndexes.size < MaximumCellCount) {
                            queue.addLast(ChainPath(indexes = nextIndexes, outgoingValue = nextOutgoingValue))
                        }
                    }
                }
            }
        }
        return null
    }

    private data class ChainPath(
        val indexes: List<Int>,
        val outgoingValue: Int,
    )

    private const val MinimumCellCount = 4
    private const val MaximumCellCount = 12
}

internal object AlternatingInferenceChainStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.AlternatingInferenceChain

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val graph = CandidateInferenceGraph.from(state)
        val budget = ChainSearchBudget()
        for (target in graph.nodes) {
            val proofGraph = CandidateExclusionGraph(source = graph, excluded = target)
            val endpoints = graph.neighbors(target, InferenceLinkType.Weak)
                .filter { endpoint ->
                    graph.neighbors(endpoint, InferenceLinkType.Strong).isNotEmpty()
                }
            for (firstIndex in endpoints.indices) {
                val first = endpoints[firstIndex]
                for (secondIndex in firstIndex + 1 until endpoints.size) {
                    val second = endpoints[secondIndex]
                    if (graph.areWeaklyLinked(first, second)) continue
                    val path = budget.find(
                        graph = proofGraph,
                        start = first,
                        end = second,
                        firstLink = InferenceLinkType.Strong,
                        lastLink = InferenceLinkType.Strong,
                        minimumDepth = 3,
                    ) ?: continue

                    return chainEliminationStep(
                        pattern = pattern,
                        relatedNodes = path.nodes,
                        eliminations = listOf(target),
                    )
                }
            }
        }
        return null
    }
}

internal object ContinuousLoopStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.ContinuousLoop

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val graph = CandidateInferenceGraph.from(state)
        val budget = ChainSearchBudget()
        for (start in graph.nodes) {
            if (graph.neighbors(start, InferenceLinkType.Strong).isEmpty()) continue
            val closingNodes = graph.neighbors(start, InferenceLinkType.Weak)
                .filter { node ->
                    node > start && graph.neighbors(node, InferenceLinkType.Strong).isNotEmpty()
                }
            for (end in closingNodes) {
                val path = budget.find(
                    graph = graph,
                    start = start,
                    end = end,
                    firstLink = InferenceLinkType.Strong,
                    lastLink = InferenceLinkType.Strong,
                    minimumDepth = 3,
                ) ?: continue
                val cycleNodes = path.nodes.toSet()
                val weakEdges = buildList {
                    path.links.forEachIndexed { index, link ->
                        if (link == InferenceLinkType.Weak) {
                            add(path.nodes[index] to path.nodes[index + 1])
                        }
                    }
                    add(end to start)
                }
                val eliminations = graph.nodes.filter { candidate ->
                    candidate !in cycleNodes && weakEdges.any { (first, second) ->
                        graph.areWeaklyLinked(candidate, first) &&
                            graph.areWeaklyLinked(candidate, second)
                    }
                }

                chainEliminationStep(
                    pattern = pattern,
                    relatedNodes = path.nodes,
                    eliminations = eliminations,
                )?.let { return it }
            }
        }
        return null
    }
}

internal object DiscontinuousLoopStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.DiscontinuousLoop

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val graph = CandidateInferenceGraph.from(state)
        val budget = ChainSearchBudget()
        for (start in graph.nodes) {
            findWeakWeakLoop(graph, budget, start)?.let { path ->
                return chainEliminationStep(
                    pattern = pattern,
                    relatedNodes = path.nodes,
                    eliminations = listOf(start),
                )
            }
            findStrongStrongLoop(graph, budget, start)?.let { path ->
                return SudokuStepFactory.placement(
                    index = start.index,
                    value = start.value,
                    pattern = pattern,
                    relatedIndexes = path.nodes.map { node -> node.index },
                )
            }
        }
        return null
    }

    private fun findWeakWeakLoop(
        graph: CandidateInferenceGraph,
        budget: ChainSearchBudget,
        start: CandidateNode,
    ): AlternatingInferencePath<CandidateNode>? {
        for (end in graph.neighbors(start, InferenceLinkType.Weak)) {
            val path = budget.find(
                graph = graph,
                start = start,
                end = end,
                firstLink = InferenceLinkType.Weak,
                lastLink = InferenceLinkType.Strong,
                minimumDepth = 2,
            )
            if (path != null) return path
        }
        return null
    }

    private fun findStrongStrongLoop(
        graph: CandidateInferenceGraph,
        budget: ChainSearchBudget,
        start: CandidateNode,
    ): AlternatingInferencePath<CandidateNode>? {
        for (end in graph.neighbors(start, InferenceLinkType.Strong)) {
            val path = budget.find(
                graph = graph,
                start = start,
                end = end,
                firstLink = InferenceLinkType.Strong,
                lastLink = InferenceLinkType.Weak,
                minimumDepth = 2,
            )
            if (path != null) return path
        }
        return null
    }
}

internal object NiceLoopStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.NiceLoop

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val concreteStep = ContinuousLoopStrategy.findStep(state)
            ?: DiscontinuousLoopStrategy.findStep(state)
            ?: return null
        return concreteStep.copy(pattern = pattern)
    }
}

private class ChainSearchBudget(
    private var remainingAttempts: Int = MaximumAttempts,
) {
    fun find(
        graph: InferenceLinkGraph<CandidateNode>,
        start: CandidateNode,
        end: CandidateNode,
        firstLink: InferenceLinkType,
        lastLink: InferenceLinkType,
        minimumDepth: Int,
    ): AlternatingInferencePath<CandidateNode>? {
        if (remainingAttempts == 0) return null
        remainingAttempts--
        return findAlternatingPath(
            graph = graph,
            start = start,
            end = end,
            firstLink = firstLink,
            lastLink = lastLink,
            maxDepth = MaximumDepth,
            minimumDepth = minimumDepth,
            maximumStates = MaximumStatesPerAttempt,
        )
    }

    private companion object {
        const val MaximumDepth = 7
        const val MaximumStatesPerAttempt = 10_000
        const val MaximumAttempts = 2_000
    }
}

private class CandidateExclusionGraph(
    private val source: CandidateInferenceGraph,
    private val excluded: CandidateNode,
) : InferenceLinkGraph<CandidateNode> {
    override val nodes: List<CandidateNode> = source.nodes.filter { node -> node != excluded }

    override fun neighbors(
        node: CandidateNode,
        linkType: InferenceLinkType,
    ): List<CandidateNode> = if (node == excluded) {
        emptyList()
    } else {
        source.neighbors(node, linkType).filter { neighbor -> neighbor != excluded }
    }
}

private fun chainEliminationStep(
    pattern: SudokuSolvingPattern,
    relatedNodes: List<CandidateNode>,
    eliminations: List<CandidateNode>,
): SudokuSolutionStep? {
    val grouped = eliminations
        .distinct()
        .groupBy(keySelector = { node -> node.index }, valueTransform = { node -> node.value })
        .entries
        .sortedBy { entry -> entry.key }
        .map { (index, values) ->
            CandidateElimination(
                row = index.row(),
                column = index.column(),
                values = values.sorted().toSet(),
            )
        }
    return SudokuStepFactory.elimination(
        pattern = pattern,
        relatedIndexes = relatedNodes.map { node -> node.index }.distinct().sorted(),
        eliminations = grouped,
    )
}
