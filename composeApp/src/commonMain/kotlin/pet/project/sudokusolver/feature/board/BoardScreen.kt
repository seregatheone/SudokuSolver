package pet.project.sudokusolver.feature.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pet.project.sudokusolver.domain.SolutionMode
import pet.project.sudokusolver.domain.SudokuGrid
import pet.project.sudokusolver.ui.SudokuTheme
import pet.project.sudokusolver.ui.useWideBoardLayout
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
        ) {
            if (useWideBoardLayout(maxWidth.value)) {
                WideBoardContent(
                    state = state,
                    onIntent = onIntent,
                    onBack = onBack,
                )
            } else {
                CompactBoardContent(
                    state = state,
                    onIntent = onIntent,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun CompactBoardContent(
    state: BoardState,
    onIntent: (BoardIntent) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoardToolbar(
            modifier = Modifier.widthIn(max = 680.dp),
            onBack = onBack,
            onClear = { onIntent(BoardIntent.ClearClicked) },
        )
        Spacer(Modifier.height(10.dp))
        SudokuBoard(
            modifier = Modifier,
            grid = state.grid,
            selectedCell = state.selectedCell,
            highlightedValue = state.highlightedValue,
            patternStep = (state.status as? BoardStatus.StepApplied)?.step,
            onCellSelected = { onIntent(BoardIntent.CellSelected(it)) },
        )
        Spacer(Modifier.height(16.dp))
        BoardControlsPanel(
            modifier = Modifier.widthIn(max = 680.dp),
            state = state,
            onIntent = onIntent,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun WideBoardContent(
    state: BoardState,
    onIntent: (BoardIntent) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        BoardToolbar(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1120.dp)
                .align(Alignment.CenterHorizontally),
            onBack = onBack,
            onClear = { onIntent(BoardIntent.ClearClicked) },
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .widthIn(max = 1120.dp)
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SudokuBoard(
                    modifier = Modifier,
                    grid = state.grid,
                    selectedCell = state.selectedCell,
                    highlightedValue = state.highlightedValue,
                    patternStep = (state.status as? BoardStatus.StepApplied)?.step,
                    onCellSelected = { onIntent(BoardIntent.CellSelected(it)) },
                )
                Spacer(Modifier.height(12.dp))
            }
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
            ) {
                BoardControlsPanel(
                    modifier = Modifier.fillMaxWidth(),
                    state = state,
                    onIntent = onIntent,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun BoardControlsPanel(
    modifier: Modifier,
    state: BoardState,
    onIntent: (BoardIntent) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoardStatusCard(
            modifier = Modifier.fillMaxWidth(),
            status = state.status,
        )
        Spacer(Modifier.height(14.dp))
        ModeChooser(
            enabled = state.grid.hasAnyValue(),
            selectedMode = state.selectedMode,
            onModeSelected = { onIntent(BoardIntent.SolutionModeSelected(it)) },
        )
        Spacer(Modifier.height(14.dp))
        NumberPad(
            hasSelectedCell = state.selectedCell != null,
            allowedValues = state.allowedValues,
            onValueSelected = { onIntent(BoardIntent.ValueSelected(it)) },
        )
        Spacer(Modifier.height(10.dp))
        InputTools(
            enabled = state.selectedCell != null,
            isPencilMode = state.isPencilMode,
            onPencilModeToggle = { onIntent(BoardIntent.PencilModeToggled) },
            onDelete = { onIntent(BoardIntent.ValueSelected(null)) },
        )

        if (state.selectedMode == SolutionMode.StepByStep && state.steps.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            StepControls(
                nextStepIndex = state.nextStepIndex,
                stepCount = state.steps.size,
                onPrevious = { onIntent(BoardIntent.PreviousStepClicked) },
                onNext = { onIntent(BoardIntent.NextStepClicked) },
            )
        }

        if (state.selectedMode == SolutionMode.SelfPractice) {
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = { onIntent(BoardIntent.HintClicked) },
            ) {
                Text(
                    text = stringResource(Res.string.board_hint),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun BoardStatusCard(
    modifier: Modifier,
    status: BoardStatus,
) {
    val visual = when (status) {
        BoardStatus.SolveFailed,
        BoardStatus.HintUnavailable,
        is BoardStatus.Conflict,
        -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "!",
        )

        BoardStatus.FastSolved -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "✓",
        )

        is BoardStatus.StepApplied -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "→",
        )

        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "i",
        )
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = visual.first,
        border = BorderStroke(1.dp, visual.second.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = visual.third,
                color = visual.second,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = statusText(status),
                color = visual.second,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(name = "Board compact dark", widthDp = 360, heightDp = 800)
@Composable
private fun BoardCompactPreview() {
    SudokuTheme(darkTheme = true) {
        BoardScreen(
            state = BoardState(grid = SudokuGrid.Empty),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Board wide", widthDp = 1000, heightDp = 720)
@Composable
private fun BoardWidePreview() {
    SudokuTheme(darkTheme = false) {
        BoardScreen(
            state = BoardState(grid = SudokuGrid.Empty),
            onIntent = {},
            onBack = {},
        )
    }
}
