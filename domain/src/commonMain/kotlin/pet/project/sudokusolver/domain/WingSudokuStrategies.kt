package pet.project.sudokusolver.domain

internal object XYWingStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.XYWing

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val bivalueIndexes = state.board.indices.filter { index ->
            state.board[index] == 0 && state.candidates[index].size == 2
        }

        for (pivotIndex in bivalueIndexes) {
            val pivotValues = state.candidates[pivotIndex]
            val pivotPeers = SudokuRules.peerIndexes(pivotIndex)
            val pincerIndexes = bivalueIndexes.filter { index ->
                index in pivotPeers && state.candidates[index].intersect(pivotValues).size == 1
            }

            for ((firstPincerIndex, secondPincerIndex) in pincerIndexes.combinations(2)) {
                val firstPincerValues = state.candidates[firstPincerIndex]
                val secondPincerValues = state.candidates[secondPincerIndex]
                val firstPivotValue = firstPincerValues.intersect(pivotValues).single()
                val secondPivotValue = secondPincerValues.intersect(pivotValues).single()
                if (firstPivotValue == secondPivotValue) continue

                val firstOuterValue = (firstPincerValues - pivotValues).singleOrNull() ?: continue
                val secondOuterValue = (secondPincerValues - pivotValues).singleOrNull() ?: continue
                if (firstOuterValue != secondOuterValue) continue

                val eliminationValue = firstOuterValue
                val commonPeers = SudokuRules.peerIndexes(firstPincerIndex)
                    .intersect(SudokuRules.peerIndexes(secondPincerIndex))
                val eliminations = commonPeers
                    .filter { index ->
                        index != pivotIndex &&
                            index != firstPincerIndex &&
                            index != secondPincerIndex &&
                            state.board[index] == 0 &&
                            eliminationValue in state.candidates[index]
                    }
                    .sorted()
                    .map { index ->
                        CandidateElimination(
                            row = index.row(),
                            column = index.column(),
                            values = setOf(eliminationValue),
                        )
                    }

                SudokuStepFactory.elimination(
                    pattern = pattern,
                    relatedIndexes = listOf(pivotIndex, firstPincerIndex, secondPincerIndex),
                    eliminations = eliminations,
                )?.let { return it }
            }
        }
        return null
    }
}
