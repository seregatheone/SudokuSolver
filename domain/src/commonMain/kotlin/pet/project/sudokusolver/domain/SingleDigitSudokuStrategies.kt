package pet.project.sudokusolver.domain

internal object SkyscraperStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.Skyscraper

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (value in 1..9) {
            findStepInUnits(
                state = state,
                value = value,
                units = sudokuRows(),
                basesShareUnit = { first, second -> first.column() == second.column() },
            )?.let { return it }
            findStepInUnits(
                state = state,
                value = value,
                units = sudokuColumns(),
                basesShareUnit = { first, second -> first.row() == second.row() },
            )?.let { return it }
        }
        return null
    }

    private fun findStepInUnits(
        state: SudokuBoardState,
        value: Int,
        units: List<List<Int>>,
        basesShareUnit: (Int, Int) -> Boolean,
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
                    (firstIndex to secondIndex).takeIf { basesShareUnit(firstIndex, secondIndex) }
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

internal object TwoStringKiteStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.TwoStringKite

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (value in 1..9) {
            val rowLinks = strongLinks(state, value, sudokuRows())
            val columnLinks = strongLinks(state, value, sudokuColumns())

            for (rowLink in rowLinks) {
                for (columnLink in columnLinks) {
                    if (rowLink.any { it in columnLink }) continue

                    for (rowBridge in rowLink) {
                        for (columnBridge in columnLink) {
                            if (!shareBox(rowBridge, columnBridge)) continue

                            val rowRoof = rowLink.single { it != rowBridge }
                            val columnRoof = columnLink.single { it != columnBridge }
                            val linkIndexes = rowLink + columnLink
                            val eliminations = SudokuRules.peerIndexes(rowRoof)
                                .intersect(SudokuRules.peerIndexes(columnRoof))
                                .filter { index ->
                                    index !in linkIndexes &&
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
                                relatedIndexes = linkIndexes,
                                eliminations = eliminations,
                            )?.let { return it }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun strongLinks(
        state: SudokuBoardState,
        value: Int,
        units: List<List<Int>>,
    ): List<List<Int>> = units.mapNotNull { unit ->
        unit.filter { index ->
            state.board[index] == 0 && value in state.candidates[index]
        }.takeIf { it.size == 2 }
    }

    private fun shareBox(first: Int, second: Int): Boolean =
        first.row() / 3 == second.row() / 3 && first.column() / 3 == second.column() / 3
}
