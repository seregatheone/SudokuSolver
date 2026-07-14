package pet.project.sudokusolver.feature.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pet.project.sudokusolver.domain.SolutionMode
import pet.project.sudokusolver.domain.SudokuGrid
import pet.project.sudokusolver.ui.SudokuTheme
import pet.project.sudokusolver.ui.boardWidthForViewport
import pet.project.sudokusolver.ui.useDenseBoardControls
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
            if (useWideBoardLayout(maxWidth.value, maxHeight.value)) {
                WideBoardContent(
                    state = state,
                    onIntent = onIntent,
                    onBack = onBack,
                    denseControls = useDenseBoardControls(maxHeight.value),
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoardToolbar(
            modifier = Modifier.widthIn(max = 680.dp),
            onBack = onBack,
            onClear = { onIntent(BoardIntent.ClearClicked) },
        )
        Spacer(Modifier.height(4.dp))
        BoardViewport(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = state,
            onIntent = onIntent,
        )
        Spacer(Modifier.height(4.dp))
        BoardControlsPanel(
            modifier = Modifier.widthIn(max = 680.dp),
            state = state,
            onIntent = onIntent,
            dense = true,
        )
    }
}

@Composable
private fun BoardViewport(
    modifier: Modifier,
    state: BoardState,
    onIntent: (BoardIntent) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        SudokuBoard(
            modifier = Modifier.width(
                boardWidthForViewport(maxWidth.value, maxHeight.value).dp,
            ),
            grid = state.grid,
            selectedCell = state.selectedCell,
            highlightedValue = state.highlightedValue,
            patternStep = (state.status as? BoardStatus.StepApplied)?.step,
            onCellSelected = { onIntent(BoardIntent.CellSelected(it)) },
        )
    }
}

@Composable
private fun WideBoardContent(
    state: BoardState,
    onIntent: (BoardIntent) -> Unit,
    onBack: () -> Unit,
    denseControls: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1120.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            BoardToolbar(
                modifier = Modifier.fillMaxWidth(),
                onBack = onBack,
                onClear = { onIntent(BoardIntent.ClearClicked) },
            )
            if (denseControls) {
                BoardStatusCard(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = 72.dp, end = 112.dp)
                        .fillMaxWidth(),
                    status = state.status,
                    dense = true,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .widthIn(max = 1120.dp)
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(if (denseControls) 16.dp else 28.dp),
            verticalAlignment = Alignment.Top,
        ) {
            BoardViewport(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                state = state,
                onIntent = onIntent,
            )
            Column(
                modifier = Modifier
                    .width(if (denseControls) 300.dp else 360.dp)
                    .fillMaxHeight()
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                BoardControlsPanel(
                    modifier = Modifier.fillMaxWidth(),
                    state = state,
                    onIntent = onIntent,
                    dense = denseControls,
                    showStatus = !denseControls,
                )
            }
        }
    }
}

@Composable
private fun BoardControlsPanel(
    modifier: Modifier,
    state: BoardState,
    onIntent: (BoardIntent) -> Unit,
    dense: Boolean,
    showStatus: Boolean = true,
) {
    val sectionSpacing = if (dense) 4.dp else 14.dp
    val controlHeight = if (dense) 36.dp else 48.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showStatus) {
            BoardStatusCard(
                modifier = Modifier.fillMaxWidth(),
                status = state.status,
                dense = dense,
            )
            Spacer(Modifier.height(sectionSpacing))
        }
        ModeChooser(
            enabled = state.grid.hasAnyValue(),
            selectedMode = state.selectedMode,
            onModeSelected = { onIntent(BoardIntent.SolutionModeSelected(it)) },
            dense = dense,
        )
        Spacer(Modifier.height(sectionSpacing))
        if (dense) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberPad(
                    modifier = Modifier.weight(1f),
                    hasSelectedCell = state.selectedCell != null,
                    allowedValues = state.allowedValues,
                    onValueSelected = { onIntent(BoardIntent.ValueSelected(it)) },
                    dense = true,
                )
                InputTools(
                    modifier = Modifier.width(88.dp),
                    enabled = state.selectedCell != null,
                    isPencilMode = state.isPencilMode,
                    onPencilModeToggle = { onIntent(BoardIntent.PencilModeToggled) },
                    onDelete = { onIntent(BoardIntent.ValueSelected(null)) },
                    dense = true,
                )
            }
        } else {
            NumberPad(
                hasSelectedCell = state.selectedCell != null,
                allowedValues = state.allowedValues,
                onValueSelected = { onIntent(BoardIntent.ValueSelected(it)) },
            )
            Spacer(Modifier.height(sectionSpacing))
            InputTools(
                enabled = state.selectedCell != null,
                isPencilMode = state.isPencilMode,
                onPencilModeToggle = { onIntent(BoardIntent.PencilModeToggled) },
                onDelete = { onIntent(BoardIntent.ValueSelected(null)) },
            )
        }

        if (state.selectedMode == SolutionMode.StepByStep && state.steps.isNotEmpty()) {
            Spacer(Modifier.height(sectionSpacing))
            StepControls(
                nextStepIndex = state.nextStepIndex,
                stepCount = state.steps.size,
                onPrevious = { onIntent(BoardIntent.PreviousStepClicked) },
                onNext = { onIntent(BoardIntent.NextStepClicked) },
                dense = dense,
            )
        }

        if (state.selectedMode == SolutionMode.SelfPractice) {
            Spacer(Modifier.height(sectionSpacing))
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(controlHeight),
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
    dense: Boolean,
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
            modifier = Modifier.padding(
                horizontal = if (dense) 12.dp else 16.dp,
                vertical = if (dense) 8.dp else 14.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(if (dense) 8.dp else 12.dp),
            verticalAlignment = if (dense) Alignment.CenterVertically else Alignment.Top,
        ) {
            Text(
                text = visual.third,
                color = visual.second,
                fontWeight = FontWeight.Bold,
                style = if (dense) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = statusText(status),
                color = visual.second,
                style = if (dense) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                maxLines = if (dense) 2 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
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

@Preview(name = "Board compact short", widthDp = 360, heightDp = 640)
@Composable
private fun BoardCompactShortPreview() {
    SudokuTheme(darkTheme = false) {
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
