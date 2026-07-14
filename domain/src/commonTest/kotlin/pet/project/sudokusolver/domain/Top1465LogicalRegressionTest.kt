package pet.project.sudokusolver.domain

import kotlin.test.Test
import kotlin.test.assertTrue

class Top1465LogicalRegressionTest {
    @Test
    fun line158IsSolvedByRegisteredLogicalStrategies() {
        val puzzle = "9.....7.1.2.4...........5..7.1..5....8.....3.............82..4.1.9.........39...."
            .toGrid()
        val state = checkNotNull(SudokuBoardState.from(puzzle, useCellNotes = false))
        val patterns = buildList {
            while (true) {
                val step = SudokuStrategyRegistry.findNextStep(state) ?: break
                state.apply(step)
                add(step.pattern)
            }
        }

        assertTrue(state.board.none { it == 0 }, "top1465 line 158 must not require backtracking")
        assertTrue(SudokuRules.isValidBoard(state.board))
        assertTrue(SudokuSolvingPattern.XYWing in patterns)
    }

    private fun String.toGrid(): SudokuGrid = SudokuGrid.fromRows(
        chunked(SudokuGrid.Size).map { row ->
            row.map { value -> value.takeUnless { it == '.' }?.digitToInt() }
        },
    )
}
