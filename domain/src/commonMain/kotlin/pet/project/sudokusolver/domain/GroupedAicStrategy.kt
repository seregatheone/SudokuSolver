package pet.project.sudokusolver.domain

internal object GroupedAicStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.GroupedAic

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val proof = findProof(state) ?: return null
        val eliminations = proof.eliminations
            .groupBy(
                keySelector = { node -> node.members.single().index },
                valueTransform = { node -> node.value },
            )
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
            relatedIndexes = proof.path.nodes
                .flatMap { node -> node.memberIndexes }
                .distinct()
                .sorted(),
            eliminations = eliminations,
        )
    }

    internal fun findProof(
        state: SudokuBoardState,
        maxDepth: Int = MaximumDepth,
        maximumStates: Int = MaximumStates,
    ): GroupedAicProof? {
        require(maxDepth >= MinimumDepth) { "Grouped AIC depth must allow a useful alternating path." }
        require(maximumStates >= 1) { "Grouped AIC search state cap must be positive." }

        val graph = GroupedCandidateInferenceGraph.from(state)
        var queuedStates = 0
        var bestProof: GroupedAicProof? = null
        for (start in graph.nodes) {
            if (graph.neighbors(start, InferenceLinkType.Strong).isEmpty()) continue
            if (queuedStates >= maximumStates) return bestProof
            val queue = ArrayDeque<GroupedAicSearchState>().apply {
                addLast(
                    GroupedAicSearchState(
                        node = start,
                        pathNodes = listOf(start),
                        pathLinks = emptyList(),
                        nextLink = InferenceLinkType.Strong,
                        groups = if (start.isGroup) setOf(start) else emptySet(),
                    ),
                )
            }
            val visited = mutableSetOf<GroupedAicSearchKey>()
            queuedStates++
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val key = GroupedAicSearchKey(
                    node = current.node,
                    nextLink = current.nextLink,
                    groups = current.groups,
                )
                if (!visited.add(key)) continue
                val depth = current.pathLinks.size
                if (
                    depth >= MinimumDepth &&
                    current.pathLinks.lastOrNull() == InferenceLinkType.Strong &&
                    current.node != start &&
                    current.node.value == start.value &&
                    current.groups.isNotEmpty() &&
                    start.members.toSet().intersect(current.node.members.toSet()).isEmpty() &&
                    !graph.areUniversallyWeaklyLinked(start, current.node)
                ) {
                    val pathMembers = current.pathNodes.flatMap { node -> node.members }.toSet()
                    val eliminations = graph.singleNodes.filter { victim ->
                        victim.value == start.value &&
                            victim.members.single() !in pathMembers &&
                            graph.areUniversallyWeaklyLinked(victim, start) &&
                            graph.areUniversallyWeaklyLinked(victim, current.node)
                    }
                    if (eliminations.isNotEmpty()) {
                        val proof = GroupedAicProof(
                            path = AlternatingInferencePath(
                                nodes = current.pathNodes,
                                links = current.pathLinks,
                            ),
                            eliminations = eliminations,
                        )
                        if (
                            bestProof == null ||
                            proof.eliminations.size > bestProof.eliminations.size
                        ) {
                            bestProof = proof
                        }
                    }
                }
                if (depth == maxDepth) continue

                for (neighbor in graph.neighbors(current.node, current.nextLink)) {
                    if (
                        neighbor in current.pathNodes ||
                        overlapsPathMember(neighbor, current.pathNodes)
                    ) {
                        continue
                    }
                    if (queuedStates >= maximumStates) return bestProof
                    val nextGroups = if (neighbor.isGroup) current.groups + neighbor else current.groups
                    queue.addLast(
                        GroupedAicSearchState(
                            node = neighbor,
                            pathNodes = current.pathNodes + neighbor,
                            pathLinks = current.pathLinks + current.nextLink,
                            nextLink = current.nextLink.opposite(),
                            groups = nextGroups,
                        ),
                    )
                    queuedStates++
                }
            }
        }
        return bestProof
    }

    internal fun overlapsPathMember(
        node: GroupedCandidateNode,
        path: List<GroupedCandidateNode>,
    ): Boolean = path.any { existing ->
        (node.isGroup || existing.isGroup) &&
            node.members.toSet().intersect(existing.members.toSet()).isNotEmpty()
    }

    private const val MinimumDepth = 3
    private const val MaximumDepth = 15
    private const val MaximumStates = 100_000
}

internal data class GroupedAicProof(
    val path: AlternatingInferencePath<GroupedCandidateNode>,
    val eliminations: List<GroupedCandidateNode>,
)

private data class GroupedAicSearchState(
    val node: GroupedCandidateNode,
    val pathNodes: List<GroupedCandidateNode>,
    val pathLinks: List<InferenceLinkType>,
    val nextLink: InferenceLinkType,
    val groups: Set<GroupedCandidateNode>,
)

private data class GroupedAicSearchKey(
    val node: GroupedCandidateNode,
    val nextLink: InferenceLinkType,
    val groups: Set<GroupedCandidateNode>,
)
