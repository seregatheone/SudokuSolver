package pet.project.sudokusolver

import androidx.compose.ui.window.ComposeUIViewController
import pet.project.sudokusolver.data.recognition.UnavailableSudokuPhotoPicker

fun MainViewController() = ComposeUIViewController {
    App(photoPicker = UnavailableSudokuPhotoPicker)
}
