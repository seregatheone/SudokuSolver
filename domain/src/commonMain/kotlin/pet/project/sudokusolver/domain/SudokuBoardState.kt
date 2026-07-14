package pet.project.sudokusolver.domain

internal class SudokuBoardState private constructor(
    val board: IntArray,
    val candidates: Array<MutableSet<Int>>,
) {
    fun apply(step: SudokuSolutionStep) {
        if (step.isPlacement) {
            val index = step.row * SudokuGrid.Size + step.column
            board[index] = step.value
            candidates[index].clear()
            SudokuRules.peerIndexes(index).forEach { peer -> candidates[peer].remove(step.value) }
            return
        }

        step.eliminations.forEach { elimination ->
            val index = elimination.row * SudokuGrid.Size + elimination.column
            candidates[index].removeAll(elimination.values)
        }
    }

    fun toSolvedGrid(source: SudokuGrid, solvedBoard: IntArray): SudokuGrid = SudokuGrid(
        solvedBoard.mapIndexed { index, value ->
            val sourceCell = source.cells[index]
            sourceCell.copy(value = value, notes = emptySet())
        },
    )

    companion object {
        fun from(grid: SudokuGrid, useCellNotes: Boolean = true): SudokuBoardState? {
            val board = grid.values().map { it ?: 0 }.toIntArray()
            if (!SudokuRules.isValidBoard(board)) return null

            val candidates = Array(SudokuGrid.CellCount) { index ->
                if (board[index] != 0) {
                    mutableSetOf()
                } else {
                    val legalCandidates = SudokuRules.candidatesFor(board, index).toSet()
                    val notes = if (useCellNotes) grid.cells[index].notes else emptySet()
                    if (notes.isEmpty()) legalCandidates.toMutableSet() else notes.intersect(legalCandidates).toMutableSet()
                }
            }

            return SudokuBoardState(board = board, candidates = candidates)
        }
    }
}
