package pet.project.sudokusolver.domain

internal data class CandidateNode(
    val index: Int,
    val value: Int,
) : Comparable<CandidateNode> {
    init {
        require(index in 0 until SudokuGrid.CellCount) { "Candidate index must point to a Sudoku cell." }
        require(value in 1..SudokuGrid.Size) { "Candidate value must be 1..9." }
    }

    override fun compareTo(other: CandidateNode): Int {
        val indexComparison = index.compareTo(other.index)
        return if (indexComparison != 0) indexComparison else value.compareTo(other.value)
    }
}

internal enum class InferenceLinkType {
    Strong,
    Weak,
    ;

    fun opposite(): InferenceLinkType = when (this) {
        Strong -> Weak
        Weak -> Strong
    }
}

internal interface InferenceLinkGraph<N : Comparable<N>> {
    val nodes: List<N>

    fun neighbors(node: N, linkType: InferenceLinkType): List<N>

    fun areLinked(first: N, second: N, linkType: InferenceLinkType): Boolean =
        second in neighbors(first, linkType)
}

internal data class AlternatingInferencePath<N : Comparable<N>>(
    val nodes: List<N>,
    val links: List<InferenceLinkType>,
) {
    init {
        require(nodes.size == links.size + 1) { "A path must have exactly one more node than links." }
        require(nodes.distinct().size == nodes.size) { "An alternating path cannot repeat nodes." }
        require(links.zipWithNext().all { (first, second) -> first != second }) {
            "Inference links must alternate."
        }
    }

    val depth: Int get() = links.size
}

internal fun <N : Comparable<N>> findAlternatingPath(
    graph: InferenceLinkGraph<N>,
    start: N,
    end: N,
    firstLink: InferenceLinkType,
    lastLink: InferenceLinkType,
    maxDepth: Int,
    minimumDepth: Int = 1,
    maximumStates: Int = 20_000,
): AlternatingInferencePath<N>? = findAlternatingPath(
    graph = graph,
    start = start,
    isEnd = { node -> node == end },
    firstLink = firstLink,
    lastLink = lastLink,
    maxDepth = maxDepth,
    minimumDepth = minimumDepth,
    maximumStates = maximumStates,
)

internal fun <N : Comparable<N>> findAlternatingPath(
    graph: InferenceLinkGraph<N>,
    start: N,
    isEnd: (N) -> Boolean,
    firstLink: InferenceLinkType,
    lastLink: InferenceLinkType,
    maxDepth: Int,
    minimumDepth: Int = 1,
    maximumStates: Int = 20_000,
): AlternatingInferencePath<N>? {
    require(start in graph.nodes) { "The start node must belong to the graph." }
    require(minimumDepth >= 1) { "Minimum path depth must be positive." }
    require(maxDepth >= minimumDepth) { "Maximum path depth must not be smaller than minimum depth." }
    require(maximumStates >= 1) { "Maximum search states must be positive." }

    val queue = ArrayDeque<AlternatingSearchState<N>>()
    queue.addLast(
        AlternatingSearchState(
            node = start,
            pathNodes = listOf(start),
            pathLinks = emptyList(),
            nextLink = firstLink,
        ),
    )
    var queuedStates = 1

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        val depth = current.pathLinks.size
        if (
            depth >= minimumDepth &&
            current.pathLinks.lastOrNull() == lastLink &&
            isEnd(current.node)
        ) {
            return AlternatingInferencePath(
                nodes = current.pathNodes,
                links = current.pathLinks,
            )
        }
        if (depth == maxDepth) continue

        for (neighbor in graph.neighbors(current.node, current.nextLink).sorted()) {
            if (neighbor in current.pathNodes) continue
            if (queuedStates == maximumStates) return null
            queue.addLast(
                AlternatingSearchState(
                    node = neighbor,
                    pathNodes = current.pathNodes + neighbor,
                    pathLinks = current.pathLinks + current.nextLink,
                    nextLink = current.nextLink.opposite(),
                ),
            )
            queuedStates++
        }
    }
    return null
}

internal class CandidateInferenceGraph private constructor(
    override val nodes: List<CandidateNode>,
    private val strongAdjacency: Map<CandidateNode, Set<CandidateNode>>,
) : InferenceLinkGraph<CandidateNode> {
    private val nodeSet = nodes.toSet()
    private val nodesByIndex = nodes.groupBy { node -> node.index }

    override fun neighbors(node: CandidateNode, linkType: InferenceLinkType): List<CandidateNode> {
        if (node !in nodeSet) return emptyList()
        return when (linkType) {
            InferenceLinkType.Strong -> strongAdjacency[node].orEmpty().sorted()
            InferenceLinkType.Weak -> weakNeighbors(node)
        }
    }

    override fun areLinked(
        first: CandidateNode,
        second: CandidateNode,
        linkType: InferenceLinkType,
    ): Boolean {
        if (first !in nodeSet || second !in nodeSet || first == second) return false
        return when (linkType) {
            InferenceLinkType.Strong -> second in strongAdjacency[first].orEmpty()
            InferenceLinkType.Weak -> areWeaklyLinked(first, second)
        }
    }

    fun areStronglyLinked(first: CandidateNode, second: CandidateNode): Boolean =
        areLinked(first, second, InferenceLinkType.Strong)

    fun areWeaklyLinked(first: CandidateNode, second: CandidateNode): Boolean {
        if (first !in nodeSet || second !in nodeSet || first == second) return false
        return first.index == second.index ||
            first.value == second.value && second.index in SudokuRules.peerIndexes(first.index)
    }

    fun forDigit(value: Int): SingleDigitLinkGraph {
        require(value in 1..SudokuGrid.Size) { "Candidate value must be 1..9." }
        return SingleDigitLinkGraph(source = this, value = value)
    }

    private fun weakNeighbors(node: CandidateNode): List<CandidateNode> {
        val neighbors = mutableSetOf<CandidateNode>()
        neighbors += nodesByIndex[node.index].orEmpty().filter { candidate -> candidate != node }
        for (peerIndex in SudokuRules.peerIndexes(node.index)) {
            nodesByIndex[peerIndex]
                .orEmpty()
                .firstOrNull { candidate -> candidate.value == node.value }
                ?.let(neighbors::add)
        }
        return neighbors.sorted()
    }

    companion object {
        fun from(state: SudokuBoardState): CandidateInferenceGraph {
            val nodes = state.board.indices
                .asSequence()
                .filter { index -> state.board[index] == 0 }
                .flatMap { index ->
                    state.candidates[index]
                        .asSequence()
                        .sorted()
                        .map { value -> CandidateNode(index = index, value = value) }
                }
                .sorted()
                .toList()
            val adjacency = nodes.associateWith { mutableSetOf<CandidateNode>() }.toMutableMap()

            for (index in state.board.indices) {
                if (state.board[index] != 0 || state.candidates[index].size != 2) continue
                val values = state.candidates[index].sorted()
                adjacency.addUndirectedEdge(
                    CandidateNode(index, values[0]),
                    CandidateNode(index, values[1]),
                )
            }

            for (unit in sudokuUnits()) {
                for (value in 1..SudokuGrid.Size) {
                    val indexes = unit.filter { index ->
                        state.board[index] == 0 && value in state.candidates[index]
                    }
                    if (indexes.size != 2) continue
                    adjacency.addUndirectedEdge(
                        CandidateNode(indexes[0], value),
                        CandidateNode(indexes[1], value),
                    )
                }
            }

            return CandidateInferenceGraph(
                nodes = nodes,
                strongAdjacency = adjacency.mapValues { (_, neighbors) -> neighbors.toSet() },
            )
        }
    }
}

internal class SingleDigitLinkGraph(
    private val source: CandidateInferenceGraph,
    val value: Int,
) : InferenceLinkGraph<CandidateNode> {
    override val nodes: List<CandidateNode> = source.nodes.filter { node -> node.value == value }

    override fun neighbors(node: CandidateNode, linkType: InferenceLinkType): List<CandidateNode> =
        source.neighbors(node, linkType).filter { neighbor -> neighbor.value == value }

    override fun areLinked(
        first: CandidateNode,
        second: CandidateNode,
        linkType: InferenceLinkType,
    ): Boolean = first.value == value && second.value == value && source.areLinked(first, second, linkType)
}

private data class AlternatingSearchState<N : Comparable<N>>(
    val node: N,
    val pathNodes: List<N>,
    val pathLinks: List<InferenceLinkType>,
    val nextLink: InferenceLinkType,
)

private fun MutableMap<CandidateNode, MutableSet<CandidateNode>>.addUndirectedEdge(
    first: CandidateNode,
    second: CandidateNode,
) {
    getValue(first).add(second)
    getValue(second).add(first)
}
