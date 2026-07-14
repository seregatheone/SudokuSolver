package pet.project.sudokusolver.domain

internal object NishioStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.Nishio

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val proof = findProof(state) ?: return null
        return SudokuStepFactory.elimination(
            pattern = pattern,
            relatedIndexes = proof.branch.trail.map { node -> node.index }.distinct().sorted(),
            eliminations = listOf(
                CandidateElimination(
                    row = proof.assumption.index.row(),
                    column = proof.assumption.index.column(),
                    values = setOf(proof.assumption.value),
                ),
            ),
        )
    }

    internal fun findProof(
        state: SudokuBoardState,
        maximumAssignments: Int = MaximumAssignments,
    ): NishioProof? {
        if (hasBaselineContradiction(state)) return null
        val graph = CandidateInferenceGraph.from(state)
        for (value in 1..SudokuGrid.Size) {
            val digitGraph = graph.forDigit(value)
            for (assumption in digitGraph.nodes) {
                val branch = CandidateImplicationEngine.propagate(
                    state = state,
                    graph = digitGraph,
                    assumption = assumption,
                    assumedTrue = true,
                    maximumAssignments = maximumAssignments,
                    singleDigit = value,
                )
                if (branch.outcome == AssumptionOutcome.Contradiction) {
                    return NishioProof(assumption = assumption, branch = branch)
                }
            }
        }
        return null
    }

    private const val MaximumAssignments = 128
}

internal data class NishioProof(
    val assumption: CandidateNode,
    val branch: ImplicationBranch,
)
