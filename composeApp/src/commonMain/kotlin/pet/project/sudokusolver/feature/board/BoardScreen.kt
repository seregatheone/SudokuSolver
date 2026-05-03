package pet.project.sudokusolver.feature.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import pet.project.sudokusolver.domain.CellPosition
import pet.project.sudokusolver.domain.SolutionMode
import pet.project.sudokusolver.domain.SudokuConflict
import pet.project.sudokusolver.domain.SudokuGrid
import sudokusolver.composeapp.generated.resources.Res
import sudokusolver.composeapp.generated.resources.*

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
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(Res.string.board_back))
                }
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.board_title),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(onClick = { onIntent(BoardIntent.ClearClicked) }) {
                    Text(stringResource(Res.string.board_clear))
                }
            }

            Spacer(Modifier.height(16.dp))

            SudokuBoard(
                grid = state.grid,
                selectedCell = state.selectedCell,
                onCellSelected = { onIntent(BoardIntent.CellSelected(it)) },
            )

            Spacer(Modifier.height(16.dp))

            NumberPad(
                enabled = state.selectedCell != null,
                allowedValues = state.allowedValues,
                onValueSelected = { onIntent(BoardIntent.ValueSelected(it)) },
            )

            Spacer(Modifier.height(18.dp))

            ModeChooser(
                enabled = state.grid.hasAnyValue(),
                selectedMode = state.selectedMode,
                onModeSelected = { onIntent(BoardIntent.SolutionModeSelected(it)) },
            )

            if (state.selectedMode == SolutionMode.StepByStep && state.steps.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = state.nextStepIndex < state.steps.size,
                    onClick = { onIntent(BoardIntent.NextStepClicked) },
                ) {
                    Text(
                        if (state.nextStepIndex < state.steps.size) {
                            stringResource(Res.string.board_next_step)
                        } else {
                            stringResource(Res.string.board_all_steps_done)
                        },
                    )
                }
            }

            if (state.selectedMode == SolutionMode.SelfPractice) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { onIntent(BoardIntent.HintClicked) }) {
                    Text(stringResource(Res.string.board_hint))
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                modifier = Modifier.widthIn(max = 620.dp),
                text = statusText(state.status),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun statusText(status: BoardStatus): String = when (status) {
    BoardStatus.Initial -> stringResource(Res.string.board_status_initial)
    BoardStatus.GridCleared -> stringResource(Res.string.board_status_cleared)
    BoardStatus.DigitRemoved -> stringResource(Res.string.board_status_digit_removed)
    is BoardStatus.DigitEntered -> stringResource(Res.string.board_status_digit_entered, status.value)
    BoardStatus.SolveFailed -> stringResource(Res.string.board_status_solve_failed)
    BoardStatus.FastSolved -> stringResource(Res.string.board_status_fast_solved)
    BoardStatus.NoStepsAvailable -> stringResource(Res.string.board_status_no_steps)
    BoardStatus.StepByStepReady -> stringResource(Res.string.board_status_step_by_step_ready)
    BoardStatus.SelfPracticeReady -> stringResource(Res.string.board_status_self_practice_ready)
    BoardStatus.HintUnavailable -> stringResource(Res.string.board_status_hint_unavailable)
    is BoardStatus.StepApplied -> stringResource(
        Res.string.board_status_step,
        status.step.row + 1,
        status.step.column + 1,
        status.step.value,
    )
    is BoardStatus.Conflict -> conflictText(status.conflict, status.value)
}

@Composable
private fun conflictText(conflict: SudokuConflict, value: Int): String = when (conflict) {
    SudokuConflict.Row -> stringResource(Res.string.board_conflict_row, value)
    SudokuConflict.Column -> stringResource(Res.string.board_conflict_column, value)
    SudokuConflict.Box -> stringResource(Res.string.board_conflict_box, value)
}

@Composable
private fun SudokuBoard(
    grid: SudokuGrid,
    selectedCell: CellPosition?,
    onCellSelected: (CellPosition) -> Unit,
) {
    val thickLine = MaterialTheme.colorScheme.onSurface
    val thinLine = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(2.dp, thickLine))
            .drawBehind {
                val cell = size.width / SudokuGrid.Size
                for (index in 1 until SudokuGrid.Size) {
                    val stroke = if (index % 3 == 0) 3.dp.toPx() else 1.dp.toPx()
                    val color = if (index % 3 == 0) thickLine else thinLine
                    val position = cell * index
                    drawLine(color, Offset(position, 0f), Offset(position, size.height), stroke)
                    drawLine(color, Offset(0f, position), Offset(size.width, position), stroke)
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until SudokuGrid.Size) {
                Row(Modifier.weight(1f)) {
                    for (column in 0 until SudokuGrid.Size) {
                        val cell = grid.cellAt(row, column)
                        val isSelected = selectedCell?.row == row && selectedCell.column == column
                        SudokuCellView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            value = cell.value,
                            isGiven = cell.isGiven,
                            isSelected = isSelected,
                            onClick = { onCellSelected(CellPosition(row, column)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuCellView(
    modifier: Modifier,
    value: Int?,
    isGiven: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isGiven -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val textColor = if (isGiven) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value?.toString().orEmpty(),
            color = textColor,
            fontSize = 24.sp,
            fontWeight = if (isGiven) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NumberPad(
    enabled: Boolean,
    allowedValues: Set<Int>,
    onValueSelected: (Int?) -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..9).chunked(3).forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowValues.forEach { value ->
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = enabled && value in allowedValues,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = { onValueSelected(value) },
                    ) {
                        Text(value.toString(), fontSize = 18.sp)
                    }
                }
            }
        }
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            onClick = { onValueSelected(null) },
        ) {
            Text(stringResource(Res.string.board_delete_digit))
        }
    }
}

@Composable
private fun ModeChooser(
    enabled: Boolean,
    selectedMode: SolutionMode?,
    onModeSelected: (SolutionMode) -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 620.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.board_solution_mode),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.solution_mode_fast),
                enabled = enabled,
                selected = selectedMode == SolutionMode.Fast,
                onClick = { onModeSelected(SolutionMode.Fast) },
            )
            ModeButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.solution_mode_step_by_step),
                enabled = enabled,
                selected = selectedMode == SolutionMode.StepByStep,
                onClick = { onModeSelected(SolutionMode.StepByStep) },
            )
            ModeButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.solution_mode_self_practice),
                enabled = enabled,
                selected = selectedMode == SolutionMode.SelfPractice,
                onClick = { onModeSelected(SolutionMode.SelfPractice) },
            )
        }
    }
}

@Composable
private fun ModeButton(
    modifier: Modifier,
    text: String,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.padding(horizontal = 6.dp),
                text = text,
                textAlign = TextAlign.Center,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
