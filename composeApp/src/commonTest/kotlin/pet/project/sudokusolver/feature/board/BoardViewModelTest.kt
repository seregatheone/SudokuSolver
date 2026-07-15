package pet.project.sudokusolver.feature.board

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import pet.project.sudokusolver.domain.SolutionMode
import pet.project.sudokusolver.domain.SudokuGrid

class BoardViewModelTest {
    @Test
    fun fastModeStillSolvesUniqueBoard() {
        val viewModel = BoardViewModel(classicPuzzle())

        viewModel.onIntent(BoardIntent.SolutionModeSelected(SolutionMode.Fast))

        assertIs<BoardStatus.FastSolved>(viewModel.state.status)
        assertEquals(4, viewModel.state.grid.valueAt(0, 2))
    }

    @Test
    fun fastModeDoesNotChooseArbitrarySolutionForEmptyBoard() {
        val viewModel = BoardViewModel(SudokuGrid.Empty)

        viewModel.onIntent(BoardIntent.SolutionModeSelected(SolutionMode.Fast))

        assertIs<BoardStatus.SolveFailed>(viewModel.state.status)
        assertEquals(List(SudokuGrid.CellCount) { null }, viewModel.state.grid.values())
    }

    private fun classicPuzzle() = SudokuGrid.fromRows(
        listOf(
            listOf(5, 3, null, null, 7, null, null, null, null),
            listOf(6, null, null, 1, 9, 5, null, null, null),
            listOf(null, 9, 8, null, null, null, null, 6, null),
            listOf(8, null, null, null, 6, null, null, null, 3),
            listOf(4, null, null, 8, null, 3, null, null, 1),
            listOf(7, null, null, null, 2, null, null, null, 6),
            listOf(null, 6, null, null, null, null, 2, 8, null),
            listOf(null, null, null, 4, 1, 9, null, null, 5),
            listOf(null, null, null, null, 8, null, null, 7, 9),
        ),
    )
}
