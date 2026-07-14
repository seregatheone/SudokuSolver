package pet.project.sudokusolver.domain

internal enum class AssumptionOutcome {
    Completed,
    Contradiction,
    Capped,
}

internal data class ImplicationBranch(
    val outcome: AssumptionOutcome,
    val assignments: Map<CandidateNode, Boolean>,
    val trail: List<CandidateNode>,
)

internal object CandidateImplicationEngine {
    fun propagate(
        state: SudokuBoardState,
        graph: InferenceLinkGraph<CandidateNode>,
        assumption: CandidateNode,
        assumedTrue: Boolean,
        maximumAssignments: Int,
        singleDigit: Int? = null,
    ): ImplicationBranch {
        require(maximumAssignments >= 1) { "Implication assignment cap must be positive." }
        require(assumption in graph.nodes) { "Assumption candidate must belong to the graph." }
        require(singleDigit == null || assumption.value == singleDigit) {
            "A single-digit implication branch cannot start with another digit."
        }
        if (hasBaselineContradiction(state)) {
            return ImplicationBranch(AssumptionOutcome.Completed, emptyMap(), emptyList())
        }

        val assignments = linkedMapOf(assumption to assumedTrue)
        val queue = ArrayDeque<CandidateNode>().apply { addLast(assumption) }
        val trail = mutableListOf<CandidateNode>()
        if (hasStructuralContradiction(state, assignments, singleDigit)) {
            return ImplicationBranch(AssumptionOutcome.Contradiction, assignments, listOf(assumption))
        }

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            trail += node
            val isTrue = assignments.getValue(node)
            val linkType = if (isTrue) InferenceLinkType.Weak else InferenceLinkType.Strong
            val impliedValue = !isTrue
            for (neighbor in graph.neighbors(node, linkType).sorted()) {
                val existing = assignments[neighbor]
                if (existing != null) {
                    if (existing != impliedValue) {
                        return ImplicationBranch(
                            outcome = AssumptionOutcome.Contradiction,
                            assignments = assignments.toMap(),
                            trail = (trail + neighbor).distinct(),
                        )
                    }
                    continue
                }
                if (assignments.size >= maximumAssignments) {
                    return ImplicationBranch(
                        outcome = AssumptionOutcome.Capped,
                        assignments = assignments.toMap(),
                        trail = trail.distinct(),
                    )
                }
                assignments[neighbor] = impliedValue
                queue.addLast(neighbor)
                if (hasStructuralContradiction(state, assignments, singleDigit)) {
                    return ImplicationBranch(
                        outcome = AssumptionOutcome.Contradiction,
                        assignments = assignments.toMap(),
                        trail = (trail + neighbor).distinct(),
                    )
                }
            }
        }
        return ImplicationBranch(
            outcome = AssumptionOutcome.Completed,
            assignments = assignments.toMap(),
            trail = trail.distinct(),
        )
    }

    private fun hasStructuralContradiction(
        state: SudokuBoardState,
        assignments: Map<CandidateNode, Boolean>,
        singleDigit: Int?,
    ): Boolean {
        val falseCandidates = assignments
            .asSequence()
            .filter { (_, isTrue) -> !isTrue }
            .map { (node, _) -> node }
            .toSet()
        if (singleDigit == null) {
            val emptyCell = state.board.indices.any { index ->
                state.board[index] == 0 &&
                    state.candidates[index].isNotEmpty() &&
                    state.candidates[index].all { value -> CandidateNode(index, value) in falseCandidates }
            }
            if (emptyCell) return true
        }
        val digits = singleDigit?.let(::listOf) ?: (1..SudokuGrid.Size).toList()
        return sudokuUnits().any { unit ->
            digits.any { value ->
                value !in unit.map { index -> state.board[index] } &&
                    unit.filter { index -> state.board[index] == 0 && value in state.candidates[index] }
                        .all { index -> CandidateNode(index, value) in falseCandidates }
            }
        }
    }
}

internal fun hasBaselineContradiction(state: SudokuBoardState): Boolean {
    if (!SudokuRules.isValidBoard(state.board.copyOf())) return true
    if (state.board.indices.any { index -> state.board[index] == 0 && state.candidates[index].isEmpty() }) {
        return true
    }
    return sudokuUnits().any { unit ->
        (1..SudokuGrid.Size).any { value ->
            value !in unit.map { index -> state.board[index] } &&
                unit.none { index -> state.board[index] == 0 && value in state.candidates[index] }
        }
    }
}
