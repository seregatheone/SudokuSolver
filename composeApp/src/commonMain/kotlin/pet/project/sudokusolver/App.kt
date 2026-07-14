package pet.project.sudokusolver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import pet.project.sudokusolver.data.recognition.SudokuPhotoPicker
import pet.project.sudokusolver.data.recognition.UnavailableSudokuPhotoPicker
import pet.project.sudokusolver.domain.SudokuGrid
import pet.project.sudokusolver.feature.board.BoardScreen
import pet.project.sudokusolver.feature.board.BoardViewModel
import pet.project.sudokusolver.feature.input.InputChoiceEffect
import pet.project.sudokusolver.feature.input.InputChoiceScreen
import pet.project.sudokusolver.feature.input.InputChoiceViewModel
import pet.project.sudokusolver.ui.SudokuTheme

private sealed interface AppRoute {
    data object InputChoice : AppRoute
    data class Board(val initialGrid: SudokuGrid) : AppRoute
}

@Composable
@Preview
fun App(
    photoPicker: SudokuPhotoPicker = UnavailableSudokuPhotoPicker,
) {
    SudokuTheme {
        val appViewModel = viewModel { AppViewModel() }

        when (val currentRoute = appViewModel.route) {
            AppRoute.InputChoice -> {
                val inputChoiceViewModel = viewModel { InputChoiceViewModel() }
                InputChoiceScreen(
                    state = inputChoiceViewModel.state,
                    onIntent = { intent ->
                        inputChoiceViewModel.onIntent(intent, photoPicker) { effect ->
                            when (effect) {
                                is InputChoiceEffect.NavigateToBoard -> appViewModel.openBoard(effect.grid)
                            }
                        }
                    },
                )
            }

            is AppRoute.Board -> {
                val boardViewModel = viewModel(
                    key = "board-${currentRoute.initialGrid.hashCode()}",
                ) {
                    BoardViewModel(currentRoute.initialGrid)
                }
                BoardScreen(
                    state = boardViewModel.state,
                    onIntent = boardViewModel::onIntent,
                    onBack = appViewModel::openInputChoice,
                )
            }
        }
    }
}

private class AppViewModel : androidx.lifecycle.ViewModel() {
    var route by mutableStateOf<AppRoute>(AppRoute.InputChoice)
        private set

    fun openInputChoice() {
        route = AppRoute.InputChoice
    }

    fun openBoard(grid: SudokuGrid) {
        route = AppRoute.Board(grid)
    }
}
