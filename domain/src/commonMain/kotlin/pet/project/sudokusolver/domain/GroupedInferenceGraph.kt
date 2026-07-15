package pet.project.sudokusolver.domain

internal class GroupedCandidateNode private constructor(
    val members: List<CandidateNode>,
) : Comparable<GroupedCandidateNode> {
    val value: Int = members.first().value
    val isGroup: Boolean = members.size > 1
    val memberIndexes: Set<Int> = members.map { member -> member.index }.toSet()

    override fun equals(other: Any?): Boolean =
        other is GroupedCandidateNode && members == other.members

    override fun hashCode(): Int = members.hashCode()

    override fun toString(): String = "GroupedCandidateNode(members=$members)"

    override fun compareTo(other: GroupedCandidateNode): Int {
        val firstComparison = members.first().compareTo(other.members.first())
        if (firstComparison != 0) return firstComparison
        val sizeComparison = members.size.compareTo(other.members.size)
        if (sizeComparison != 0) return sizeComparison
        for (index in members.indices) {
            val comparison = members[index].compareTo(other.members[index])
            if (comparison != 0) return comparison
        }
        return 0
    }

    companion object {
        fun single(candidate: CandidateNode): GroupedCandidateNode =
            GroupedCandidateNode(listOf(candidate))

        fun groupOrNull(candidates: Collection<CandidateNode>): GroupedCandidateNode? {
            val members = candidates.distinct().sorted()
            if (members.size !in 2..3 || members.map { member -> member.value }.distinct().size != 1) {
                return null
            }
            val indexes = members.map { member -> member.index }
            val inOneBox = indexes.map(::boxIndex).distinct().size == 1
            val inOneLine = indexes.map { index -> index.row() }.distinct().size == 1 ||
                indexes.map { index -> index.column() }.distinct().size == 1
            return GroupedCandidateNode(members).takeIf { inOneBox && inOneLine }
        }

        private fun boxIndex(index: Int): Int = index.row() / 3 * 3 + index.column() / 3
    }
}

internal class GroupedCandidateInferenceGraph private constructor(
    override val nodes: List<GroupedCandidateNode>,
    private val strongAdjacency: Map<GroupedCandidateNode, Set<GroupedCandidateNode>>,
    private val weakAdjacency: Map<GroupedCandidateNode, Set<GroupedCandidateNode>>,
) : InferenceLinkGraph<GroupedCandidateNode> {
    private val nodeSet = nodes.toSet()

    val singleNodes: List<GroupedCandidateNode> = nodes.filter { node -> !node.isGroup }
    val groupNodes: List<GroupedCandidateNode> = nodes.filter { node -> node.isGroup }

    override fun neighbors(
        node: GroupedCandidateNode,
        linkType: InferenceLinkType,
    ): List<GroupedCandidateNode> {
        if (node !in nodeSet) return emptyList()
        return when (linkType) {
            InferenceLinkType.Strong -> strongAdjacency[node].orEmpty().sorted()
            InferenceLinkType.Weak -> weakAdjacency[node].orEmpty().sorted()
        }
    }

    override fun areLinked(
        first: GroupedCandidateNode,
        second: GroupedCandidateNode,
        linkType: InferenceLinkType,
    ): Boolean {
        if (first !in nodeSet || second !in nodeSet || first == second) return false
        return when (linkType) {
            InferenceLinkType.Strong -> second in strongAdjacency[first].orEmpty()
            InferenceLinkType.Weak -> second in weakAdjacency[first].orEmpty()
        }
    }

    fun areUniversallyWeaklyLinked(
        first: GroupedCandidateNode,
        second: GroupedCandidateNode,
    ): Boolean = areLinked(first, second, InferenceLinkType.Weak)

    companion object {
        fun from(state: SudokuBoardState): GroupedCandidateInferenceGraph {
            val source = CandidateInferenceGraph.from(state)
            val singles = source.nodes.map(GroupedCandidateNode::single)
            val groups = buildGroups(state)
            val nodes = (singles + groups).distinct().sorted()
            val strongAdjacency = nodes.associateWith {
                mutableSetOf<GroupedCandidateNode>()
            }.toMutableMap()
            val weakAdjacency = nodes.associateWith {
                mutableSetOf<GroupedCandidateNode>()
            }.toMutableMap()

            for (firstIndex in nodes.indices) {
                val first = nodes[firstIndex]
                for (secondIndex in firstIndex + 1 until nodes.size) {
                    val second = nodes[secondIndex]
                    if (universallyWeak(source, first, second)) {
                        weakAdjacency.addUndirectedEdge(first, second)
                    }
                    if (
                        !first.isGroup &&
                        !second.isGroup &&
                        source.areStronglyLinked(first.members.single(), second.members.single())
                    ) {
                        strongAdjacency.addUndirectedEdge(first, second)
                    }
                }
            }

            addGroupedStrongLinks(
                state = state,
                nodes = nodes,
                adjacency = strongAdjacency,
            )
            return GroupedCandidateInferenceGraph(
                nodes = nodes,
                strongAdjacency = strongAdjacency.mapValues { (_, neighbors) -> neighbors.toSet() },
                weakAdjacency = weakAdjacency.mapValues { (_, neighbors) -> neighbors.toSet() },
            )
        }

        private fun buildGroups(state: SudokuBoardState): List<GroupedCandidateNode> = buildList {
            for (box in sudokuBoxes()) {
                for (value in 1..SudokuGrid.Size) {
                    for (row in box.map { index -> index.row() }.distinct()) {
                        val members = box
                            .filter { index -> index.row() == row && value in state.candidates[index] }
                            .map { index -> CandidateNode(index, value) }
                        GroupedCandidateNode.groupOrNull(members)?.let(::add)
                    }
                    for (column in box.map { index -> index.column() }.distinct()) {
                        val members = box
                            .filter { index -> index.column() == column && value in state.candidates[index] }
                            .map { index -> CandidateNode(index, value) }
                        GroupedCandidateNode.groupOrNull(members)?.let(::add)
                    }
                }
            }
        }

        private fun universallyWeak(
            source: CandidateInferenceGraph,
            first: GroupedCandidateNode,
            second: GroupedCandidateNode,
        ): Boolean {
            if (first.members.toSet().intersect(second.members.toSet()).isNotEmpty()) return false
            return first.members.all { firstMember ->
                second.members.all { secondMember ->
                    source.areWeaklyLinked(firstMember, secondMember)
                }
            }
        }

        private fun addGroupedStrongLinks(
            state: SudokuBoardState,
            nodes: List<GroupedCandidateNode>,
            adjacency: MutableMap<GroupedCandidateNode, MutableSet<GroupedCandidateNode>>,
        ) {
            for (unit in sudokuUnits()) {
                val unitIndexes = unit.toSet()
                for (value in 1..SudokuGrid.Size) {
                    val candidateIndexes = unit.filter { index ->
                        state.board[index] == 0 && value in state.candidates[index]
                    }.toSet()
                    if (candidateIndexes.size < 2) continue
                    val eligibleNodes = nodes.filter { node ->
                        node.value == value && node.memberIndexes.all { index -> index in unitIndexes }
                    }
                    for (firstIndex in eligibleNodes.indices) {
                        val first = eligibleNodes[firstIndex]
                        for (secondIndex in firstIndex + 1 until eligibleNodes.size) {
                            val second = eligibleNodes[secondIndex]
                            if (first.memberIndexes.intersect(second.memberIndexes).isNotEmpty()) continue
                            if (first.memberIndexes + second.memberIndexes == candidateIndexes) {
                                adjacency.addUndirectedEdge(first, second)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun MutableMap<GroupedCandidateNode, MutableSet<GroupedCandidateNode>>.addUndirectedEdge(
    first: GroupedCandidateNode,
    second: GroupedCandidateNode,
) {
    getValue(first).add(second)
    getValue(second).add(first)
}
