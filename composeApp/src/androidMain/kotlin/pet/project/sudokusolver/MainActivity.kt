package pet.project.sudokusolver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import pet.project.sudokusolver.recognition.AndroidSudokuPhotoPicker

class MainActivity : ComponentActivity() {
    private lateinit var photoPicker: AndroidSudokuPhotoPicker

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        photoPicker = AndroidSudokuPhotoPicker(this)

        setContent {
            App(photoPicker = photoPicker)
        }
    }

    override fun onDestroy() {
        if (::photoPicker.isInitialized) photoPicker.close()
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
