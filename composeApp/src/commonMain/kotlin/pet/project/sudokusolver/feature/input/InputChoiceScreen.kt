package pet.project.sudokusolver.feature.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickFailure
import sudokusolver.composeapp.generated.resources.Res
import sudokusolver.composeapp.generated.resources.*

@Composable
fun InputChoiceScreen(
    state: InputChoiceState,
    onIntent: (InputChoiceIntent) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.app_title),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.input_choice_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(36.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
                        onClick = { onIntent(InputChoiceIntent.ManualInputClicked) },
                    ) {
                        Text(stringResource(Res.string.input_manual), textAlign = TextAlign.Center)
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
                        enabled = !state.isPhotoLoading,
                        onClick = { onIntent(InputChoiceIntent.PhotoInputClicked) },
                    ) {
                        if (state.isPhotoLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(stringResource(Res.string.input_photo), textAlign = TextAlign.Center)
                    }
                }

                val photoFailure = state.photoFailure
                if (photoFailure != null) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = photoFailureText(photoFailure),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun photoFailureText(failure: SudokuPhotoPickFailure): String = when (failure) {
    SudokuPhotoPickFailure.Unavailable -> stringResource(Res.string.photo_error_unavailable)
    SudokuPhotoPickFailure.RecognitionFailed -> stringResource(Res.string.photo_error_recognition_failed)
}
