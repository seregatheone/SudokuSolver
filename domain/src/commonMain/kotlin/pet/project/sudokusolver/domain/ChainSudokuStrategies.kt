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
