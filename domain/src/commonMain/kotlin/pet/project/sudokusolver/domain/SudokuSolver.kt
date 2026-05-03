package pet.project.sudokusolver.domain

class SudokuSolver {
    fun solve(grid: SudokuGrid): SudokuSolveResult? = solveInternal(grid)

    fun hint(grid: SudokuGrid): SudokuSolutionStep? = solveInternal(grid)?.steps?.firstOrNull()

    fun hintForPattern(grid: SudokuGrid, pattern: SudokuSolvingPattern): SudokuSolutionStep? {
        val state = SudokuBoardState.from(grid) ?: return null
        return SudokuStrategyRegistry.strategyFor(pattern)?.findStep(state)
    }

    fun supportedPatterns(): List<SudokuSolvingPattern> = SudokuStrategyRegistry.patternOrder

    private fun solveInternal(grid: SudokuGrid): SudokuSolveResult? {
        val state = SudokuBoardState.from(grid) ?: return null
        val steps = mutableListOf<SudokuSolutionStep>()

        while (true) {
            val step = SudokuStrategyRegistry.findNextStep(state) ?: break
            state.apply(step)
            steps += step
        }

        val solvedBoard = state.board.copyOf()
        if (!SudokuBacktrackingSolver.solve(solvedBoard)) return null

        return SudokuSolveResult(
            solvedGrid = state.toSolvedGrid(source = grid, solvedBoard = solvedBoard),
            steps = steps,
        )
    }
}
