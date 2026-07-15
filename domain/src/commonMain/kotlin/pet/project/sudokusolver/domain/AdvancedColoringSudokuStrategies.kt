package pet.project.sudokusolver.domain

internal object MultiColoringStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.MultiColoring

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val graph = CandidateInferenceGraph.from(state)
        for (value in 1..SudokuGrid.Size) {
            val digitGraph = graph.forDigit(value)
            val components = coloredStrongComponents(digitGraph)
            if (components.size < 2) continue

            findTypeOne(graph, components, value)?.let { return it }
            findTypeTwo(state, graph, components, value)?.let { return it }
        }
        return null
    }

    private fun findTypeOne(
        graph: CandidateInferenceGraph,
        components: List<ColoredStrongComponent>,
        value: Int,
    ): SudokuSolutionStep? {
        for (target in components) {
            for (targetColor in 0..1) {
                val targetGroup = target.nodesFor(targetColor)
                for (other in components) {
                    if (other === target) continue
                    val seesFirstColor = targetGroup.any { targetNode ->
                        other.nodesFor(0).any { otherNode -> graph.areWeaklyLinked(targetNode, otherNode) }
                    }
                    val seesSecondColor = targetGroup.any { targetNode ->
                        other.nodesFor(1).any { otherNode -> graph.areWeaklyLinked(targetNode, otherNode) }
                    }
                    if (!seesFirstColor || !seesSecondColor) continue

                    return eliminationStep(
                        pattern = pattern,
                        relatedNodes = target.nodes + other.nodes,
                        eliminations = targetGroup.map { node -> node.index to value },
                    )
                }
            }
        }
        return null
    }

    private fun findTypeTwo(
        state: SudokuBoardState,
        graph: CandidateInferenceGraph,
        components: List<ColoredStrongComponent>,
        value: Int,
    ): SudokuSolutionStep? {
        for (firstIndex in components.indices) {
            val first = components[firstIndex]
            for (secondIndex in firstIndex + 1 until components.size) {
                val second = components[secondIndex]
                for (firstColor in 0..1) {
                    for (secondColor in 0..1) {
                        if (
                            first.nodesFor(firstColor).none { firstNode ->
                                second.nodesFor(secondColor).any { secondNode ->
                                    graph.areWeaklyLinked(firstNode, secondNode)
                                }
                            }
                        ) {
                            continue
                        }

                        val firstOpposite = first.nodesFor(1 - firstColor)
                        val secondOpposite = second.nodesFor(1 - secondColor)
                        val componentNodes = first.nodes.toSet() + second.nodes
                        val eliminations = state.board.indices.mapNotNull { index ->
                            val candidate = CandidateNode(index, value)
                            if (
                                state.board[index] != 0 ||
                                value !in state.candidates[index] ||
                                candidate in componentNodes
                            ) {
                                return@mapNotNull null
                            }
                            val seesFirst = firstOpposite.any { graph.areWeaklyLinked(candidate, it) }
                            val seesSecond = secondOpposite.any { graph.areWeaklyLinked(candidate, it) }
                            (index to value).takeIf { seesFirst && seesSecond }
                        }

                        eliminationStep(
                            pattern = pattern,
                            relatedNodes = first.nodes + second.nodes,
                            eliminations = eliminations,
                        )?.let { return it }
                    }
                }
            }
        }
        return null
    }
}

internal object ThreeDMedusaStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.ThreeDMedusa

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val graph = CandidateInferenceGraph.from(state)
        for (component in coloredStrongComponents(graph)) {
            if (component.nodes.map { it.value }.distinct().size < 2) continue

            findFalseColor(graph, component)?.let { falseColor ->
                return eliminationStep(
                    pattern = pattern,
                    relatedNodes = component.nodes,
                    eliminations = component.nodesFor(falseColor).map { node -> node.index to node.value },
                )
            }

            val componentNodes = component.nodes.toSet()
            val eliminations = graph.nodes.mapNotNull { candidate ->
                if (candidate in componentNodes) return@mapNotNull null
                val seesFirst = component.nodesFor(0).any { graph.areWeaklyLinked(candidate, it) }
                val seesSecond = component.nodesFor(1).any { graph.areWeaklyLinked(candidate, it) }
                (candidate.index to candidate.value).takeIf { seesFirst && seesSecond }
            }
            eliminationStep(
                pattern = pattern,
                relatedNodes = component.nodes,
                eliminations = eliminations,
            )?.let { return it }

            findEmptyByColor(state, graph, component)?.let { falseColor ->
                return eliminationStep(
                    pattern = pattern,
                    relatedNodes = component.nodes,
                    eliminations = component.nodesFor(falseColor).map { node -> node.index to node.value },
                )
            }
        }
        return null
    }

    private fun findFalseColor(
        graph: CandidateInferenceGraph,
        component: ColoredStrongComponent,
    ): Int? = (0..1).firstOrNull { color ->
        val nodes = component.nodesFor(color)
        nodes.indices.any { firstIndex ->
            (firstIndex + 1 until nodes.size).any { secondIndex ->
                graph.areWeaklyLinked(nodes[firstIndex], nodes[secondIndex])
            }
        }
    }

    private fun findEmptyByColor(
        state: SudokuBoardState,
        graph: CandidateInferenceGraph,
        component: ColoredStrongComponent,
    ): Int? = (0..1).firstOrNull { color ->
        val assumedTrue = component.nodesFor(color)
        val emptiesCell = state.board.indices.any { index ->
            state.board[index] == 0 &&
                state.candidates[index].isNotEmpty() &&
                state.candidates[index].all { value ->
                    val candidate = CandidateNode(index, value)
                    assumedTrue.any { graph.areWeaklyLinked(candidate, it) }
                }
        }
        val emptiesUnit = sudokuUnits().any { unit ->
            (1..SudokuGrid.Size).any { value ->
                val candidates = unit
                    .filter { index -> state.board[index] == 0 && value in state.candidates[index] }
                    .map { index -> CandidateNode(index, value) }
                candidates.isNotEmpty() && candidates.all { candidate ->
                    assumedTrue.any { graph.areWeaklyLinked(candidate, it) }
                }
            }
        }
        emptiesCell || emptiesUnit
    }
}

internal data class ColoredStrongComponent(
    val colors: Map<CandidateNode, Int>,
) {
    val nodes: List<CandidateNode> = colors.keys.sorted()

    fun nodesFor(color: Int): List<CandidateNode> = nodes.filter { colors.getValue(it) == color }
}

internal fun coloredStrongComponents(
    graph: InferenceLinkGraph<CandidateNode>,
): List<ColoredStrongComponent> {
    val unvisited = graph.nodes
        .filter { node -> graph.neighbors(node, InferenceLinkType.Strong).isNotEmpty() }
        .toMutableSet()
    val components = mutableListOf<ColoredStrongComponent>()
    while (unvisited.isNotEmpty()) {
        val start = checkNotNull(unvisited.minOrNull())
        val colors = mutableMapOf(start to 0)
        val queue = ArrayDeque<CandidateNode>().apply { addLast(start) }
        var isBipartite = true
        while (queue.isNotEmpty() && isBipartite) {
            val node = queue.removeFirst()
            val oppositeColor = 1 - colors.getValue(node)
            for (neighbor in graph.neighbors(node, InferenceLinkType.Strong).sorted()) {
                val existingColor = colors[neighbor]
                if (existingColor == null) {
                    colors[neighbor] = oppositeColor
                    queue.addLast(neighbor)
                } else if (existingColor != oppositeColor) {
                    isBipartite = false
                    break
                }
            }
        }
        unvisited.removeAll(colors.keys)
        if (isBipartite && colors.size >= 2) components += ColoredStrongComponent(colors.toMap())
    }
    return components.sortedBy { component -> component.nodes.first() }
}

private fun eliminationStep(
    pattern: SudokuSolvingPattern,
    relatedNodes: List<CandidateNode>,
    eliminations: List<Pair<Int, Int>>,
): SudokuSolutionStep? {
    val grouped = eliminations
        .groupBy(keySelector = { (index, _) -> index }, valueTransform = { (_, value) -> value })
        .entries
        .sortedBy { entry -> entry.key }
        .map { (index, values) ->
            CandidateElimination(index.row(), index.column(), values.sorted().toSet())
        }
    return SudokuStepFactory.elimination(
        pattern = pattern,
        relatedIndexes = relatedNodes.map { it.index }.distinct().sorted(),
        eliminations = grouped,
    )
}
