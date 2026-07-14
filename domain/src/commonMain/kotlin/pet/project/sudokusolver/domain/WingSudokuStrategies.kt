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

internal object XYZWingStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.XYZWing

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val bivalueIndexes = state.board.indices.filter { index ->
            state.board[index] == 0 && state.candidates[index].size == 2
        }
        val pivotIndexes = state.board.indices.filter { index ->
            state.board[index] == 0 && state.candidates[index].size == 3
        }

        for (pivotIndex in pivotIndexes) {
            val pivotValues = state.candidates[pivotIndex]
            val pivotPeers = SudokuRules.peerIndexes(pivotIndex)
            val pincerIndexes = bivalueIndexes.filter { index ->
                index in pivotPeers && pivotValues.containsAll(state.candidates[index])
            }

            for ((firstPincerIndex, secondPincerIndex) in pincerIndexes.combinations(2)) {
                val firstPincerValues = state.candidates[firstPincerIndex]
                val secondPincerValues = state.candidates[secondPincerIndex]
                if (firstPincerValues.union(secondPincerValues) != pivotValues) continue

                val eliminationValue = firstPincerValues.intersect(secondPincerValues).singleOrNull() ?: continue
                val commonPeers = pivotPeers
                    .intersect(SudokuRules.peerIndexes(firstPincerIndex))
                    .intersect(SudokuRules.peerIndexes(secondPincerIndex))
                val eliminations = commonPeers
                    .filter { index ->
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

internal object WWingStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.WWing

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        val bivalueIndexes = state.board.indices.filter { index ->
            state.board[index] == 0 && state.candidates[index].size == 2
        }

        for ((firstWing, secondWing) in bivalueIndexes.combinations(2)) {
            val wingValues = state.candidates[firstWing]
            if (state.candidates[secondWing] != wingValues) continue
            if (secondWing in SudokuRules.peerIndexes(firstWing)) continue

            for (linkValue in wingValues) {
                val strongLinks = sudokuUnits()
                    .mapNotNull { unit ->
                        unit.filter { index ->
                            state.board[index] == 0 && linkValue in state.candidates[index]
                        }.takeIf { it.size == 2 }
                    }
                    .distinct()

                for (strongLink in strongLinks) {
                    if (strongLink.any { it == firstWing || it == secondWing }) continue
                    val (firstLinkEnd, secondLinkEnd) = strongLink
                    val directConnection =
                        firstLinkEnd in SudokuRules.peerIndexes(firstWing) &&
                            secondLinkEnd in SudokuRules.peerIndexes(secondWing)
                    val reverseConnection =
                        secondLinkEnd in SudokuRules.peerIndexes(firstWing) &&
                            firstLinkEnd in SudokuRules.peerIndexes(secondWing)
                    if (!directConnection && !reverseConnection) continue

                    val eliminationValue = wingValues.single { it != linkValue }
                    val eliminations = SudokuRules.peerIndexes(firstWing)
                        .intersect(SudokuRules.peerIndexes(secondWing))
                        .filter { index ->
                            index !in strongLink &&
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
                        relatedIndexes = listOf(firstWing, secondWing) + strongLink,
                        eliminations = eliminations,
                    )?.let { return it }
                }
            }
        }
        return null
    }
}
