package pet.project.sudokusolver.domain

internal interface SudokuStrategy {
    val pattern: SudokuSolvingPattern

    fun findStep(state: SudokuBoardState): SudokuSolutionStep?
}
