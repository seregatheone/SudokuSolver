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

internal object XChainStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.XChain

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (value in 1..9) {
            val strongLinks = sudokuUnits()
                .mapNotNull { unit ->
                    unit.filter { index ->
                        state.board[index] == 0 && value in state.candidates[index]
                    }.takeIf { it.size == 2 }
                }
                .distinct()

            for ((firstLink, secondLink) in strongLinks.combinations(2)) {
                if (firstLink.any { it in secondLink }) continue

                for (firstBridge in firstLink) {
                    for (secondBridge in secondLink) {
                        if (secondBridge !in SudokuRules.peerIndexes(firstBridge)) continue

                        val firstEnd = firstLink.single { it != firstBridge }
                        val secondEnd = secondLink.single { it != secondBridge }
                        val linkIndexes = firstLink + secondLink
                        val eliminations = SudokuRules.peerIndexes(firstEnd)
                            .intersect(SudokuRules.peerIndexes(secondEnd))
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
        return null
    }
}

internal object EmptyRectangleStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.EmptyRectangle

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (value in 1..9) {
            for (box in sudokuBoxes()) {
                val boxCandidates = box.filter { index ->
                    state.board[index] == 0 && value in state.candidates[index]
                }
                if (boxCandidates.size < 3) continue

                val boxRows = box.map { it.row() }.distinct()
                val boxColumns = box.map { it.column() }.distinct()
                for (emptyRectangleRow in boxRows) {
                    for (emptyRectangleColumn in boxColumns) {
                        if (
                            boxCandidates.any { index ->
                                index.row() != emptyRectangleRow && index.column() != emptyRectangleColumn
                            }
                        ) {
                            continue
                        }
                        val rowArm = boxCandidates.filter { index ->
                            index.row() == emptyRectangleRow && index.column() != emptyRectangleColumn
                        }
                        val columnArm = boxCandidates.filter { index ->
                            index.column() == emptyRectangleColumn && index.row() != emptyRectangleRow
                        }
                        if (rowArm.isEmpty() || columnArm.isEmpty()) continue

                        findUsingRowArm(
                            state = state,
                            value = value,
                            box = box,
                            emptyRectangleRow = emptyRectangleRow,
                            emptyRectangleColumn = emptyRectangleColumn,
                            boxCandidates = boxCandidates,
                        )?.let { return it }
                        findUsingColumnArm(
                            state = state,
                            value = value,
                            box = box,
                            emptyRectangleRow = emptyRectangleRow,
                            emptyRectangleColumn = emptyRectangleColumn,
                            boxCandidates = boxCandidates,
                        )?.let { return it }
                    }
                }
            }
        }
        return null
    }

    private fun findUsingRowArm(
        state: SudokuBoardState,
        value: Int,
        box: List<Int>,
        emptyRectangleRow: Int,
        emptyRectangleColumn: Int,
        boxCandidates: List<Int>,
    ): SudokuSolutionStep? {
        val boxColumns = box.map { it.column() }.toSet()
        for (linkColumn in 0 until SudokuGrid.Size) {
            if (linkColumn in boxColumns) continue
            val nearEnd = emptyRectangleRow * SudokuGrid.Size + linkColumn
            val strongLink = sudokuColumns()[linkColumn].filter { index ->
                state.board[index] == 0 && value in state.candidates[index]
            }
            if (strongLink.size != 2 || nearEnd !in strongLink) continue

            val remoteEnd = strongLink.single { it != nearEnd }
            if (remoteEnd.row() in box.map { it.row() }) continue
            val target = remoteEnd.row() * SudokuGrid.Size + emptyRectangleColumn
            val eliminations = target.takeIf { index ->
                state.board[index] == 0 && value in state.candidates[index]
            }?.let { index ->
                listOf(CandidateElimination(index.row(), index.column(), setOf(value)))
            }.orEmpty()

            SudokuStepFactory.elimination(
                pattern = pattern,
                relatedIndexes = boxCandidates + strongLink,
                eliminations = eliminations,
            )?.let { return it }
        }
        return null
    }

    private fun findUsingColumnArm(
        state: SudokuBoardState,
        value: Int,
        box: List<Int>,
        emptyRectangleRow: Int,
        emptyRectangleColumn: Int,
        boxCandidates: List<Int>,
    ): SudokuSolutionStep? {
        val boxRows = box.map { it.row() }.toSet()
        for (linkRow in 0 until SudokuGrid.Size) {
            if (linkRow in boxRows) continue
            val nearEnd = linkRow * SudokuGrid.Size + emptyRectangleColumn
            val strongLink = sudokuRows()[linkRow].filter { index ->
                state.board[index] == 0 && value in state.candidates[index]
            }
            if (strongLink.size != 2 || nearEnd !in strongLink) continue

            val remoteEnd = strongLink.single { it != nearEnd }
            if (remoteEnd.column() in box.map { it.column() }) continue
            val target = emptyRectangleRow * SudokuGrid.Size + remoteEnd.column()
            val eliminations = target.takeIf { index ->
                state.board[index] == 0 && value in state.candidates[index]
            }?.let { index ->
                listOf(CandidateElimination(index.row(), index.column(), setOf(value)))
            }.orEmpty()

            SudokuStepFactory.elimination(
                pattern = pattern,
                relatedIndexes = boxCandidates + strongLink,
                eliminations = eliminations,
            )?.let { return it }
        }
        return null
    }
}
