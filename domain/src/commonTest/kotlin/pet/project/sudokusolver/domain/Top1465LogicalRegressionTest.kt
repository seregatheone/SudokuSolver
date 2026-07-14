package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class Top1465LogicalRegressionTest {
    @Test
    fun line158IsSolvedByRegisteredLogicalStrategies() {
        assertSolved(
            lineNumber = 158,
            givens = "9.....7.1.2.4...........5..7.1..5....8.....3.............82..4.1.9.........39....",
            requiredPattern = SudokuSolvingPattern.XYWing,
        )
    }

    @Test
    fun line81IsSolvedByRegisteredLogicalStrategies() {
        assertSolved(
            lineNumber = 81,
            givens = "2...6...8.743.........2....62......1...4..5..8...........5..34......1..........7.",
            requiredPattern = SudokuSolvingPattern.XYZWing,
        )
    }

    @Test
    fun line741IsSolvedWithoutCorruptingItsUniqueSolution() {
        assertSolved(
            lineNumber = 741,
            givens = ".3..6......4.5...7.5.7...8......1.........7..3.....465.1...7..4..581.....7....29.",
            requiredPattern = SudokuSolvingPattern.XYZWing,
        )
    }

    @Test
    fun line810KeepsItsSafeLogicalSolution() {
        assertSolved(
            lineNumber = 810,
            givens = "..8....54...25.8.64..8....3.......2...9...7..71..4.5....798.........5...16...7...",
            requiredPattern = SudokuSolvingPattern.SueDeCoq,
        )
    }

    @Test
    fun line1140IsSolvedWithoutIncorrectNakedQuadElimination() {
        assertSolved(
            lineNumber = 1140,
            givens = "..85...6..........9...184......2....8.714...214.8..65.6....3....3..8.......2.71..",
            requiredPattern = SudokuSolvingPattern.WWing,
        )
    }

    private fun assertSolved(
        lineNumber: Int,
        givens: String,
        requiredPattern: SudokuSolvingPattern,
    ) {
        val state = checkNotNull(SudokuBoardState.from(givens.toGrid(), useCellNotes = false))
        val patterns = buildList {
            while (true) {
                val step = SudokuStrategyRegistry.findNextStep(state) ?: break
                state.apply(step)
                add(step.pattern)
            }
        }

        assertTrue(
            state.board.none { it == 0 },
            "top1465 line $lineNumber must not require backtracking; " +
                "remaining=${state.board.count { it == 0 }}, patterns=$patterns",
        )
        assertTrue(SudokuRules.isValidBoard(state.board))
        assertTrue(requiredPattern in patterns, "top1465 line $lineNumber must use $requiredPattern")
    }

    private fun String.toGrid(): SudokuGrid = SudokuGrid.fromRows(
        chunked(SudokuGrid.Size).map { row ->
            row.map { value -> value.takeUnless { it == '.' }?.digitToInt() }
        },
    )
}
