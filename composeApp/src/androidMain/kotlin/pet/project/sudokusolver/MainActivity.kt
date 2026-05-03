package pet.project.sudokusolver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import pet.project.sudokusolver.recognition.AndroidSudokuPhotoPicker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val photoPicker = AndroidSudokuPhotoPicker(this)

        setContent {
            App(photoPicker = photoPicker)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
