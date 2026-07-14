package pet.project.sudokusolver.domain

internal object SkyscraperStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.Skyscraper

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (value in 1..9) {
            findStepInUnits(state, value, sudokuRows())?.let { return it }
            findStepInUnits(state, value, sudokuColumns())?.let { return it }
        }
        return null
    }

    private fun findStepInUnits(
        state: SudokuBoardState,
        value: Int,
        units: List<List<Int>>,
    ): SudokuSolutionStep? {
        val strongLinks = units.mapNotNull { unit ->
            val indexes = unit.filter { index ->
                state.board[index] == 0 && value in state.candidates[index]
            }
            indexes.takeIf { it.size == 2 }
        }

        for ((firstLink, secondLink) in strongLinks.combinations(2)) {
            val connectedPairs = firstLink.flatMap { firstIndex ->
                secondLink.mapNotNull { secondIndex ->
                    (firstIndex to secondIndex).takeIf {
                        secondIndex in SudokuRules.peerIndexes(firstIndex)
                    }
                }
            }
            if (connectedPairs.size != 1) continue

            val (firstBaseIndex, secondBaseIndex) = connectedPairs.single()
            val firstRoofIndex = firstLink.single { it != firstBaseIndex }
            val secondRoofIndex = secondLink.single { it != secondBaseIndex }
            val eliminations = SudokuRules.peerIndexes(firstRoofIndex)
                .intersect(SudokuRules.peerIndexes(secondRoofIndex))
                .filter { index ->
                    index !in firstLink &&
                        index !in secondLink &&
                        state.board[index] == 0 &&
                        value in state.candidates[index]
                }
                .sorted()
                .map { index ->
                    CandidateElimination(
                        row = index.row(),
                        column = index.column(),
                        values = setOf(value),
                    )
                }

            SudokuStepFactory.elimination(
                pattern = pattern,
                relatedIndexes = firstLink + secondLink,
                eliminations = eliminations,
            )?.let { return it }
        }
        return null
    }
}
