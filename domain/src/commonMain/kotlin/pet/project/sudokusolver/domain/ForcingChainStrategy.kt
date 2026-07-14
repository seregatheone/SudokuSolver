package pet.project.sudokusolver.domain

internal object ForcingChainStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.ForcingChain

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val proof = findProof(state) ?: return null
        return when (proof) {
            is ForcingChainProof.Contradiction -> if (proof.assumedTrue) {
                eliminationStep(
                    node = proof.assumption,
                    relatedNodes = proof.branch.trail,
                )
            } else {
                SudokuStepFactory.placement(
                    index = proof.assumption.index,
                    value = proof.assumption.value,
                    pattern = pattern,
                    relatedIndexes = proof.branch.trail.map { node -> node.index },
                )
            }

            is ForcingChainProof.CommonConclusion -> if (proof.isTrue) {
                SudokuStepFactory.placement(
                    index = proof.conclusion.index,
                    value = proof.conclusion.value,
                    pattern = pattern,
                    relatedIndexes = proof.branches.flatMap { branch ->
                        branch.trail.map { node -> node.index }
                    },
                )
            } else {
                eliminationStep(
                    node = proof.conclusion,
                    relatedNodes = proof.branches.flatMap { branch -> branch.trail },
                )
            }
        }
    }

    internal fun findProof(
        state: SudokuBoardState,
        maximumAssignments: Int = MaximumAssignments,
    ): ForcingChainProof? = findCommonConclusionProof(state, maximumAssignments)
        ?: findContradictionProof(state, maximumAssignments)

    internal fun findCommonConclusionProof(
        state: SudokuBoardState,
        maximumAssignments: Int = MaximumAssignments,
    ): ForcingChainProof.CommonConclusion? {
        if (hasBaselineContradiction(state)) return null
        val graph = CandidateInferenceGraph.from(state)
        for (index in state.board.indices) {
            if (state.board[index] != 0 || state.candidates[index].size < 2) continue
            val assumptions = state.candidates[index]
                .sorted()
                .map { value -> CandidateNode(index, value) }
            val branches = assumptions.map { assumption ->
                CandidateImplicationEngine.propagate(
                    state = state,
                    graph = graph,
                    assumption = assumption,
                    assumedTrue = true,
                    maximumAssignments = maximumAssignments,
                )
            }
            if (branches.any { branch -> branch.outcome != AssumptionOutcome.Completed }) continue
            val assumptionSet = assumptions.toSet()
            val firstAssignments = branches.first().assignments
            val common = firstAssignments.entries
                .asSequence()
                .filter { (node, _) -> node !in assumptionSet }
                .filter { (node, isTrue) ->
                    branches.drop(1).all { branch -> branch.assignments[node] == isTrue }
                }
                .sortedWith(
                    compareByDescending<Map.Entry<CandidateNode, Boolean>> { entry -> entry.value }
                        .thenBy { entry -> entry.key },
                )
                .firstOrNull()
                ?: continue
            return ForcingChainProof.CommonConclusion(
                sourceIndex = index,
                conclusion = common.key,
                isTrue = common.value,
                branches = branches,
            )
        }
        return null
    }

    internal fun findContradictionProof(
        state: SudokuBoardState,
        maximumAssignments: Int = MaximumAssignments,
    ): ForcingChainProof.Contradiction? {
        if (hasBaselineContradiction(state)) return null
        val graph = CandidateInferenceGraph.from(state)
        for (assumption in graph.nodes) {
            for (assumedTrue in listOf(true, false)) {
                val branch = CandidateImplicationEngine.propagate(
                    state = state,
                    graph = graph,
                    assumption = assumption,
                    assumedTrue = assumedTrue,
                    maximumAssignments = maximumAssignments,
                )
                if (branch.outcome == AssumptionOutcome.Contradiction) {
                    return ForcingChainProof.Contradiction(
                        assumption = assumption,
                        assumedTrue = assumedTrue,
                        branch = branch,
                    )
                }
            }
        }
        return null
    }

    private fun eliminationStep(
        node: CandidateNode,
        relatedNodes: List<CandidateNode>,
    ): SudokuSolutionStep? = SudokuStepFactory.elimination(
        pattern = pattern,
        relatedIndexes = relatedNodes.map { related -> related.index }.distinct().sorted(),
        eliminations = listOf(
            CandidateElimination(node.index.row(), node.index.column(), setOf(node.value)),
        ),
    )

    private const val MaximumAssignments = 256
}

internal sealed interface ForcingChainProof {
    data class Contradiction(
        val assumption: CandidateNode,
        val assumedTrue: Boolean,
        val branch: ImplicationBranch,
    ) : ForcingChainProof

    data class CommonConclusion(
        val sourceIndex: Int,
        val conclusion: CandidateNode,
        val isTrue: Boolean,
        val branches: List<ImplicationBranch>,
    ) : ForcingChainProof
}
