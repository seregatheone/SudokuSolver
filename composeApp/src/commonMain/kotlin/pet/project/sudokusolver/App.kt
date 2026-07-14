package pet.project.sudokusolver

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
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

private val SudokuColorScheme = lightColorScheme(
    primary = Color(0xFF1E6F5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F2EA),
    onPrimaryContainer = Color(0xFF08251F),
    secondary = Color(0xFF7B4B1F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF8DEC5),
    onSecondaryContainer = Color(0xFF2D1604),
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF171D1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1B),
    surfaceVariant = Color(0xFFE2E8E5),
    onSurfaceVariant = Color(0xFF414946),
    outline = Color(0xFF707976),
)

private sealed interface AppRoute {
    data object InputChoice : AppRoute
    data class Board(val initialGrid: SudokuGrid) : AppRoute
}

@Composable
@Preview
fun App(
    photoPicker: SudokuPhotoPicker = UnavailableSudokuPhotoPicker,
) {
    MaterialTheme(colorScheme = SudokuColorScheme) {
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
