package pet.project.sudokusolver.domain

class SudokuSolver {
    fun solve(grid: SudokuGrid): SudokuSolveResult = solveInternal(grid)

    fun hint(grid: SudokuGrid): SudokuSolutionStep? {
        val state = SudokuBoardState.from(grid, useCellNotes = true) ?: return null
        return SudokuStrategyRegistry.findNextStep(state)
    }

    fun hintForPattern(grid: SudokuGrid, pattern: SudokuSolvingPattern): SudokuSolutionStep? {
        val state = SudokuBoardState.from(grid) ?: return null
        return SudokuStrategyRegistry.strategyFor(pattern)?.findStep(state)
    }

    fun catalogPatterns(): List<SudokuSolvingPattern> = SudokuSolvingPattern.entries

    fun supportedPatterns(): List<SudokuSolvingPattern> = SudokuStrategyRegistry.executablePatterns

    fun automaticPatterns(): List<SudokuSolvingPattern> = SudokuStrategyRegistry.automaticSearchOrder

    private fun solveInternal(grid: SudokuGrid): SudokuSolveResult {
        val validation = grid.validate()
        if (!validation.isValid) return SudokuSolveResult.Invalid(validation)

        val state = checkNotNull(SudokuBoardState.from(grid, useCellNotes = false))
        val steps = mutableListOf<SudokuSolutionStep>()

        while (true) {
            val step = SudokuStrategyRegistry.findNextStep(state) ?: break
            state.apply(step)
            steps += step
        }

        val solvedBoards = SudokuBacktrackingSolver.findSolutions(state.board, limit = 2)
        if (solvedBoards.isEmpty()) return SudokuSolveResult.Unsolvable

        val solutions = solvedBoards.map { solvedBoard ->
            state.toSolvedGrid(source = grid, solvedBoard = solvedBoard)
        }

        return if (solutions.size == 1) {
            SudokuSolveResult.Unique(solvedGrid = solutions.single(), steps = steps)
        } else {
            SudokuSolveResult.Multiple(solutions = solutions, steps = steps)
        }
    }
}
