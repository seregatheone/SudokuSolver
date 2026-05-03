package pet.project.sudokusolver.feature.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import pet.project.sudokusolver.domain.SolutionMode
import sudokusolver.composeapp.generated.resources.Res
import sudokusolver.composeapp.generated.resources.board_hint

@Composable
fun BoardScreen(
    state: BoardState,
    onIntent: (BoardIntent) -> Unit,
    onBack: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoardToolbar(
                onBack = onBack,
                onClear = { onIntent(BoardIntent.ClearClicked) },
            )

            Spacer(Modifier.height(8.dp))

            SudokuBoard(
                grid = state.grid,
                selectedCell = state.selectedCell,
                highlightedValue = state.highlightedValue,
                patternStep = (state.status as? BoardStatus.StepApplied)?.step,
                onCellSelected = { onIntent(BoardIntent.CellSelected(it)) },
            )

            Spacer(Modifier.height(10.dp))

            ModeChooser(
                enabled = state.grid.hasAnyValue(),
                selectedMode = state.selectedMode,
                onModeSelected = { onIntent(BoardIntent.SolutionModeSelected(it)) },
            )

            Spacer(Modifier.height(10.dp))

            NumberPad(
                hasSelectedCell = state.selectedCell != null,
                allowedValues = state.allowedValues,
                onValueSelected = { onIntent(BoardIntent.ValueSelected(it)) },
            )

            Spacer(Modifier.height(6.dp))

            InputTools(
                enabled = state.selectedCell != null,
                isPencilMode = state.isPencilMode,
                onPencilModeToggle = { onIntent(BoardIntent.PencilModeToggled) },
                onDelete = { onIntent(BoardIntent.ValueSelected(null)) },
            )

            if (state.selectedMode == SolutionMode.StepByStep && state.steps.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                StepControls(
                    nextStepIndex = state.nextStepIndex,
                    stepCount = state.steps.size,
                    onPrevious = { onIntent(BoardIntent.PreviousStepClicked) },
                    onNext = { onIntent(BoardIntent.NextStepClicked) },
                )
            }

            if (state.selectedMode == SolutionMode.SelfPractice) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { onIntent(BoardIntent.HintClicked) }) {
                    Text(stringResource(Res.string.board_hint), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                modifier = Modifier.widthIn(max = 640.dp),
                text = statusText(state.status),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
