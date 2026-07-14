package pet.project.sudokusolver.domain

internal object BowmansBingoStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.BowmansBingo

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val proof = findProof(state) ?: return null
        return SudokuStepFactory.elimination(
            pattern = pattern,
            relatedIndexes = proof.trail.map { placement -> placement.index }.distinct(),
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
        maximumPlacements: Int = MaximumPlacements,
    ): BowmansBingoProof? {
        require(maximumPlacements >= 1) { "Bowman's Bingo placement cap must be positive." }
        if (hasBaselineContradiction(state)) return null
        val assumptions = state.board.indices.flatMap { index ->
            if (state.board[index] == 0) {
                state.candidates[index].sorted().map { value -> CandidateNode(index, value) }
            } else {
                emptyList()
            }
        }
        for (assumption in assumptions) {
            val branch = propagateSingles(
                state = state,
                assumption = assumption,
                maximumPlacements = maximumPlacements,
            )
            if (branch.outcome == AssumptionOutcome.Contradiction) {
                return BowmansBingoProof(
                    assumption = assumption,
                    trail = branch.trail,
                )
            }
        }
        return null
    }

    private fun propagateSingles(
        state: SudokuBoardState,
        assumption: CandidateNode,
        maximumPlacements: Int,
    ): SinglesBranch {
        val branch = MutableSinglesState(
            board = state.board.copyOf(),
            candidates = Array(state.candidates.size) { index -> state.candidates[index].toMutableSet() },
        )
        val trail = mutableListOf<SinglesPlacement>()
        if (!branch.place(assumption.index, assumption.value)) {
            return SinglesBranch(AssumptionOutcome.Contradiction, trail)
        }
        trail += SinglesPlacement(assumption.index, assumption.value)
        if (branch.hasContradiction()) {
            return SinglesBranch(AssumptionOutcome.Contradiction, trail)
        }

        while (true) {
            val single = branch.nextNakedSingle() ?: branch.nextHiddenSingle()
                ?: return SinglesBranch(AssumptionOutcome.Completed, trail)
            if (trail.size >= maximumPlacements) {
                return SinglesBranch(AssumptionOutcome.Capped, trail)
            }
            if (!branch.place(single.index, single.value)) {
                return SinglesBranch(AssumptionOutcome.Contradiction, trail + single)
            }
            trail += single
            if (branch.hasContradiction()) {
                return SinglesBranch(AssumptionOutcome.Contradiction, trail)
            }
        }
    }

    private const val MaximumPlacements = 32
}

internal data class BowmansBingoProof(
    val assumption: CandidateNode,
    val trail: List<SinglesPlacement>,
)

internal data class SinglesPlacement(
    val index: Int,
    val value: Int,
)

private data class SinglesBranch(
    val outcome: AssumptionOutcome,
    val trail: List<SinglesPlacement>,
)

private class MutableSinglesState(
    val board: IntArray,
    val candidates: Array<MutableSet<Int>>,
) {
    fun place(index: Int, value: Int): Boolean {
        if (board[index] != 0 || value !in candidates[index]) return false
        if (!SudokuRules.canPlace(board, index.row(), index.column(), value)) return false
        board[index] = value
        candidates[index].clear()
        SudokuRules.peerIndexes(index).forEach { peer ->
            if (board[peer] == 0) candidates[peer].remove(value)
        }
        return true
    }

    fun nextNakedSingle(): SinglesPlacement? = board.indices.firstNotNullOfOrNull { index ->
        if (board[index] == 0 && candidates[index].size == 1) {
            SinglesPlacement(index, candidates[index].first())
        } else {
            null
        }
    }

    fun nextHiddenSingle(): SinglesPlacement? {
        for (unit in sudokuUnits()) {
            for (value in 1..SudokuGrid.Size) {
                if (unit.any { index -> board[index] == value }) continue
                val positions = unit.filter { index -> board[index] == 0 && value in candidates[index] }
                if (positions.size == 1) return SinglesPlacement(positions.single(), value)
            }
        }
        return null
    }

    fun hasContradiction(): Boolean {
        if (!SudokuRules.isValidBoard(board.copyOf())) return true
        if (board.indices.any { index -> board[index] == 0 && candidates[index].isEmpty() }) return true
        return sudokuUnits().any { unit ->
            (1..SudokuGrid.Size).any { value ->
                unit.none { index -> board[index] == value } &&
                    unit.none { index -> board[index] == 0 && value in candidates[index] }
            }
        }
    }
}
