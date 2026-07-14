package pet.project.sudokusolver.domain

internal fun collectAlsEliminations(
    state: SudokuBoardState,
    candidate: Int,
    occurrences: List<Int>,
    excludedIndexes: Set<Int>,
): List<CandidateElimination> {
    if (occurrences.isEmpty()) return emptyList()
    return state.board.indices
        .filter { index ->
            index !in excludedIndexes &&
                state.board[index] == 0 &&
                candidate in state.candidates[index] &&
                occurrences.all { occurrence -> cellsSeeEachOther(index, occurrence) }
        }
        .map { index ->
            CandidateElimination(
                row = index.row(),
                column = index.column(),
                values = setOf(candidate),
            )
        }
}
