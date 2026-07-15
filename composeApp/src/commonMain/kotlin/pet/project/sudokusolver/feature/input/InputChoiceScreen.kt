package pet.project.sudokusolver.feature.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pet.project.sudokusolver.data.recognition.SudokuPhotoPickFailure
import pet.project.sudokusolver.ui.SudokuTheme
import pet.project.sudokusolver.ui.stackInputActions
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
        BoxWithConstraints(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            val stackActions = stackInputActions(maxWidth.value)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PuzzleMark()
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(Res.string.app_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(Res.string.input_choice_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(32.dp))

                    InputActions(
                        stacked = stackActions,
                        isPhotoLoading = state.isPhotoLoading,
                        onManualInput = { onIntent(InputChoiceIntent.ManualInputClicked) },
                        onPhotoInput = { onIntent(InputChoiceIntent.PhotoInputClicked) },
                    )

                    val photoFailure = state.photoFailure
                    val photoMessage = when {
                        photoFailure != null -> photoFailureText(photoFailure)
                        state.wasPhotoCancelled -> stringResource(Res.string.photo_cancelled)
                        else -> null
                    }
                    if (photoMessage != null) {
                        Spacer(Modifier.height(20.dp))
                        InputMessage(
                            message = photoMessage,
                            isError = photoFailure != null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleMark() {
    Surface(
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            repeat(3) { row ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    repeat(3) { column ->
                        val emphasized = row == column || row + column == 2
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (emphasized) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputActions(
    stacked: Boolean,
    isPhotoLoading: Boolean,
    onManualInput: () -> Unit,
    onPhotoInput: () -> Unit,
) {
    if (stacked) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ManualInputButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPhotoLoading,
                onClick = onManualInput,
            )
            PhotoInputButton(
                modifier = Modifier.fillMaxWidth(),
                isLoading = isPhotoLoading,
                onClick = onPhotoInput,
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ManualInputButton(
                modifier = Modifier.weight(1f),
                enabled = !isPhotoLoading,
                onClick = onManualInput,
            )
            PhotoInputButton(
                modifier = Modifier.weight(1f),
                isLoading = isPhotoLoading,
                onClick = onPhotoInput,
            )
        }
    }
}

@Composable
private fun ManualInputButton(
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier.height(52.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(
            text = stringResource(Res.string.input_manual),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun PhotoInputButton(
    modifier: Modifier,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = modifier.height(52.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        enabled = !isLoading,
        onClick = onClick,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = stringResource(Res.string.input_photo),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun InputMessage(
    message: String,
    isError: Boolean,
) {
    val container = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isError) "!" else "i",
                color = content,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = message,
                color = content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun photoFailureText(failure: SudokuPhotoPickFailure): String = when (failure) {
    SudokuPhotoPickFailure.Unavailable -> stringResource(Res.string.photo_error_unavailable)
    SudokuPhotoPickFailure.DecodeFailed -> stringResource(Res.string.photo_error_decode_failed)
    SudokuPhotoPickFailure.NativeProcessingUnavailable ->
        stringResource(Res.string.photo_error_native_processing_unavailable)
    SudokuPhotoPickFailure.ImageTooSmall -> stringResource(Res.string.photo_error_image_too_small)
    SudokuPhotoPickFailure.BoardNotFound -> stringResource(Res.string.photo_error_board_not_found)
    SudokuPhotoPickFailure.InvalidBoardGeometry ->
        stringResource(Res.string.photo_error_invalid_board_geometry)
    SudokuPhotoPickFailure.ModelUnavailable -> stringResource(Res.string.photo_error_model_unavailable)
    SudokuPhotoPickFailure.ModelIncompatible -> stringResource(Res.string.photo_error_model_incompatible)
    SudokuPhotoPickFailure.ModelIntegrityFailed ->
        stringResource(Res.string.photo_error_model_integrity_failed)
    SudokuPhotoPickFailure.InferenceFailed -> stringResource(Res.string.photo_error_recognition_failed)
    SudokuPhotoPickFailure.RecognitionFailed -> stringResource(Res.string.photo_error_recognition_failed)
}

@Preview(name = "Input compact", widthDp = 360, heightDp = 760)
@Composable
private fun InputChoiceCompactPreview() {
    SudokuTheme(darkTheme = false) {
        InputChoiceScreen(
            state = InputChoiceState(),
            onIntent = {},
        )
    }
}

@Preview(name = "Input dark", widthDp = 700, heightDp = 760)
@Composable
private fun InputChoiceDarkPreview() {
    SudokuTheme(darkTheme = true) {
        InputChoiceScreen(
            state = InputChoiceState(photoFailure = SudokuPhotoPickFailure.BoardNotFound),
            onIntent = {},
        )
    }
}
