package pet.project.sudokusolver.domain

internal object ExocetStrategy : SudokuStrategy {
    override val pattern: SudokuSolvingPattern = SudokuSolvingPattern.Exocet

    override fun findStep(state: SudokuBoardState): SudokuSolutionStep? {
        for (orientation in ExocetOrientation.entries) {
            findStep(state, orientation)?.let { return it }
        }
        return null
    }

    private fun findStep(
        state: SudokuBoardState,
        orientation: ExocetOrientation,
    ): SudokuSolutionStep? {
        for (chuteStart in 0 until SudokuGrid.Size step 3) {
            val chuteCoordinates = (chuteStart until chuteStart + 3).toSet()
            for (baseCoordinate in chuteCoordinates) {
                for (baseBoxStart in 0 until SudokuGrid.Size step 3) {
                    val miniLine = (baseBoxStart until baseBoxStart + 3)
                        .map { crossCoordinate -> orientation.cell(baseCoordinate, crossCoordinate) }
                    for (bases in miniLine.combinations(2)) {
                        if (bases.any { cell -> state.board[cell.index] != 0 }) continue

                        val baseDigits = bases
                            .flatMap { cell -> state.candidates[cell.index] }
                            .toSet()
                        if (baseDigits.size !in 3..4) continue

                        val unusedCrossCoordinate = miniLine
                            .single { cell -> cell !in bases }
                            .crossCoordinate
                        val targetBoxStarts = (0 until SudokuGrid.Size step 3)
                            .filter { boxStart -> boxStart != baseBoxStart }
                        val firstTargets = targetCells(
                            state = state,
                            orientation = orientation,
                            chuteCoordinates = chuteCoordinates,
                            baseCoordinate = baseCoordinate,
                            boxStart = targetBoxStarts[0],
                            baseDigits = baseDigits,
                        )
                        val secondTargets = targetCells(
                            state = state,
                            orientation = orientation,
                            chuteCoordinates = chuteCoordinates,
                            baseCoordinate = baseCoordinate,
                            boxStart = targetBoxStarts[1],
                            baseDigits = baseDigits,
                        )

                        for (firstTarget in firstTargets) {
                            for (secondTarget in secondTargets) {
                                if (firstTarget.chuteCoordinate == secondTarget.chuteCoordinate) continue
                                val targets = listOf(firstTarget, secondTarget)
                                val targetDigits = targets
                                    .flatMap { target -> state.candidates[target.index] }
                                    .toSet()
                                if (!targetDigits.containsAll(baseDigits)) continue

                                val companions = listOf(
                                    orientation.cell(
                                        firstTarget.chuteCoordinate,
                                        secondTarget.crossCoordinate,
                                    ),
                                    orientation.cell(
                                        secondTarget.chuteCoordinate,
                                        firstTarget.crossCoordinate,
                                    ),
                                )
                                if (companions.any { companion -> companion.containsAny(state, baseDigits) }) continue

                                val crossCoordinates = listOf(
                                    unusedCrossCoordinate,
                                    firstTarget.crossCoordinate,
                                    secondTarget.crossCoordinate,
                                )
                                val sCells = buildList {
                                    for (chuteCoordinate in 0 until SudokuGrid.Size) {
                                        if (chuteCoordinate in chuteCoordinates) continue
                                        crossCoordinates.forEach { crossCoordinate ->
                                            add(orientation.cell(chuteCoordinate, crossCoordinate))
                                        }
                                    }
                                }
                                if (
                                    !hasValidCoverLines(
                                        state = state,
                                        baseDigits = baseDigits,
                                        crossCoordinates = crossCoordinates,
                                        sCells = sCells,
                                    )
                                ) {
                                    continue
                                }

                                val eliminations = targets.mapNotNull { target ->
                                    val removed = state.candidates[target.index] - baseDigits
                                    removed.takeIf { values -> values.isNotEmpty() }?.let { values ->
                                        CandidateElimination(
                                            row = target.index.row(),
                                            column = target.index.column(),
                                            values = values.sorted().toSet(),
                                        )
                                    }
                                }.sortedWith(compareBy(CandidateElimination::row, CandidateElimination::column))
                                if (eliminations.isEmpty()) continue

                                val relatedIndexes = buildList {
                                    addAll(bases.map(ExocetCell::index))
                                    addAll(targets.map(ExocetCell::index))
                                    addAll(companions.map(ExocetCell::index))
                                    addAll(
                                        sCells
                                            .filter { cell -> baseDigits.any { value -> cell.contains(state, value) } }
                                            .map(ExocetCell::index),
                                    )
                                }.distinct().sorted()
                                return SudokuStepFactory.elimination(
                                    pattern = pattern,
                                    relatedIndexes = relatedIndexes,
                                    eliminations = eliminations,
                                )
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun targetCells(
        state: SudokuBoardState,
        orientation: ExocetOrientation,
        chuteCoordinates: Set<Int>,
        baseCoordinate: Int,
        boxStart: Int,
        baseDigits: Set<Int>,
    ): List<ExocetCell> = buildList {
        for (chuteCoordinate in chuteCoordinates) {
            if (chuteCoordinate == baseCoordinate) continue
            for (crossCoordinate in boxStart until boxStart + 3) {
                val cell = orientation.cell(chuteCoordinate, crossCoordinate)
                if (
                    state.board[cell.index] == 0 &&
                    state.candidates[cell.index].any { value -> value in baseDigits }
                ) {
                    add(cell)
                }
            }
        }
    }

    private fun hasValidCoverLines(
        state: SudokuBoardState,
        baseDigits: Set<Int>,
        crossCoordinates: List<Int>,
        sCells: List<ExocetCell>,
    ): Boolean = baseDigits.all { value ->
        val support = sCells.filter { cell -> cell.contains(state, value) }
        val isCoveredByAtMostTwoPerpendicularLines =
            support.map(ExocetCell::chuteCoordinate).distinct().size <= 2
        val continuesInEveryCrossLine = crossCoordinates.all { crossCoordinate ->
            support.any { cell -> cell.crossCoordinate == crossCoordinate }
        }
        isCoveredByAtMostTwoPerpendicularLines && continuesInEveryCrossLine
    }
}

private enum class ExocetOrientation {
    Band,
    Stack,
    ;

    fun cell(chuteCoordinate: Int, crossCoordinate: Int): ExocetCell {
        val index = when (this) {
            Band -> chuteCoordinate * SudokuGrid.Size + crossCoordinate
            Stack -> crossCoordinate * SudokuGrid.Size + chuteCoordinate
        }
        return ExocetCell(
            index = index,
            chuteCoordinate = chuteCoordinate,
            crossCoordinate = crossCoordinate,
        )
    }
}

private data class ExocetCell(
    val index: Int,
    val chuteCoordinate: Int,
    val crossCoordinate: Int,
) {
    fun contains(state: SudokuBoardState, value: Int): Boolean =
        state.board[index] == value || value in state.candidates[index]

    fun containsAny(state: SudokuBoardState, values: Set<Int>): Boolean =
        state.board[index] in values || state.candidates[index].any { value -> value in values }
}
