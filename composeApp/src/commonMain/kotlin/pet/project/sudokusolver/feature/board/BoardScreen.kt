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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import pet.project.sudokusolver.domain.SudokuCell
import pet.project.sudokusolver.domain.SudokuConflict
import pet.project.sudokusolver.domain.SudokuGrid
import pet.project.sudokusolver.domain.SudokuSolutionStep
import pet.project.sudokusolver.domain.SudokuSolvingPattern
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
                Row(
                    modifier = Modifier.widthIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = state.nextStepIndex > 0,
                        onClick = { onIntent(BoardIntent.PreviousStepClicked) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(stringResource(Res.string.board_previous_step), fontSize = 12.sp)
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        enabled = state.nextStepIndex < state.steps.size,
                        onClick = { onIntent(BoardIntent.NextStepClicked) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            if (state.nextStepIndex < state.steps.size) {
                                stringResource(Res.string.board_next_step)
                            } else {
                                stringResource(Res.string.board_all_steps_done)
                            },
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (state.selectedMode == SolutionMode.SelfPractice) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onIntent(BoardIntent.HintClicked) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
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

@Composable
private fun BoardToolbar(
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .height(38.dp),
    ) {
        IconButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(38.dp),
            onClick = onBack,
        ) {
            Text(
                text = stringResource(Res.string.board_back_symbol),
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        OutlinedButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(32.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            onClick = onClear,
        ) {
            Text(
                text = stringResource(Res.string.board_clear),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun statusText(status: BoardStatus): String = when (status) {
    BoardStatus.Initial -> stringResource(Res.string.board_status_initial)
    BoardStatus.GridCleared -> stringResource(Res.string.board_status_cleared)
    BoardStatus.DigitRemoved -> stringResource(Res.string.board_status_digit_removed)
    BoardStatus.NotesCleared -> stringResource(Res.string.board_status_notes_cleared)
    is BoardStatus.NoteToggled -> stringResource(Res.string.board_status_note_toggled, status.value)
    is BoardStatus.DigitEntered -> stringResource(Res.string.board_status_digit_entered, status.value)
    BoardStatus.SolveFailed -> stringResource(Res.string.board_status_solve_failed)
    BoardStatus.FastSolved -> stringResource(Res.string.board_status_fast_solved)
    BoardStatus.NoStepsAvailable -> stringResource(Res.string.board_status_no_steps)
    BoardStatus.StepByStepReady -> stringResource(Res.string.board_status_step_by_step_ready)
    BoardStatus.SelfPracticeReady -> stringResource(Res.string.board_status_self_practice_ready)
    BoardStatus.HintUnavailable -> stringResource(Res.string.board_status_hint_unavailable)
    is BoardStatus.StepApplied -> stringResource(
        Res.string.board_status_step,
        coordinateText(status.step.row, status.step.column),
        status.step.value,
        patternText(status.step.pattern),
        relatedCellsText(status.step.relatedCells),
    )
    is BoardStatus.Conflict -> conflictText(status.conflict, status.value)
}

@Composable
private fun relatedCellsText(cells: List<CellPosition>): String {
    if (cells.isEmpty()) return stringResource(Res.string.board_status_step_no_related_cells)
    return cells
        .take(6)
        .joinToString { cell -> coordinateText(cell.row, cell.column) }
}

private fun coordinateText(row: Int, column: Int): String = "${columnLabel(column)}${row + 1}"

private fun columnLabel(column: Int): Char = ('A'.code + column).toChar()

@Composable
private fun patternText(pattern: SudokuSolvingPattern): String = when (pattern) {
    SudokuSolvingPattern.NakedSingle -> stringResource(Res.string.pattern_naked_single)
    SudokuSolvingPattern.HiddenSingleRow -> stringResource(Res.string.pattern_hidden_single_row)
    SudokuSolvingPattern.HiddenSingleColumn -> stringResource(Res.string.pattern_hidden_single_column)
    SudokuSolvingPattern.HiddenSingleBox -> stringResource(Res.string.pattern_hidden_single_box)
    SudokuSolvingPattern.CalculatedCandidate -> stringResource(Res.string.pattern_calculated_candidate)
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
    highlightedValue: Int?,
    patternStep: SudokuSolutionStep?,
    onCellSelected: (CellPosition) -> Unit,
) {
    val thickLine = MaterialTheme.colorScheme.onSurface
    val thinLine = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val labelWeight = 0.55f
    val patternTarget = patternStep?.let { CellPosition(it.row, it.column) }
    val patternRelatedCells = patternStep?.relatedCells.orEmpty().toSet()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(labelWeight))
            Row(Modifier.weight(9f)) {
                for (column in 0 until SudokuGrid.Size) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = columnLabel(column).toString(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(labelWeight)
                    .aspectRatio(labelWeight / 9f),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (row in 0 until SudokuGrid.Size) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (row + 1).toString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(9f)
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
                                val position = CellPosition(row, column)
                                val isSelected = selectedCell == position
                                val isHighlighted = selectedCell == null &&
                                    highlightedValue != null &&
                                    (cell.value == highlightedValue || highlightedValue in cell.notes)
                                SudokuCellView(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    cell = cell,
                                    isSelected = isSelected,
                                    isHighlighted = isHighlighted,
                                    isPatternTarget = patternTarget == position,
                                    isPatternRelated = position in patternRelatedCells,
                                    onClick = { onCellSelected(position) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuCellView(
    modifier: Modifier,
    cell: SudokuCell,
    isSelected: Boolean,
    isHighlighted: Boolean,
    isPatternTarget: Boolean,
    isPatternRelated: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isPatternTarget -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f)
        isPatternRelated -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f)
        isHighlighted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        cell.isGiven -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val textColor = if (cell.isGiven) {
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
        if (cell.value != null) {
            Text(
                text = cell.value.toString(),
                color = textColor,
                fontSize = 20.sp,
                fontWeight = if (cell.isGiven) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        } else if (cell.notes.isNotEmpty()) {
            NotesGrid(notes = cell.notes)
        }
    }
}

@Composable
private fun NotesGrid(notes: Set<Int>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        (1..9).chunked(3).forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                rowValues.forEach { value ->
                    Text(
                        modifier = Modifier.weight(1f),
                        text = if (value in notes) value.toString() else "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 8.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun InputTools(
    enabled: Boolean,
    isPencilMode: Boolean,
    onPencilModeToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.widthIn(max = 420.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .clickable(enabled = enabled, onClick = onPencilModeToggle),
            shape = RoundedCornerShape(8.dp),
            color = if (isPencilMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.board_pencil_symbol),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    fontSize = 17.sp,
                    fontWeight = if (isPencilMode) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        OutlinedButton(
            modifier = Modifier
                .weight(2f)
                .height(30.dp),
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp),
            onClick = onDelete,
        ) {
            Text(stringResource(Res.string.board_delete_digit), fontSize = 11.sp)
        }
    }
}

@Composable
private fun NumberPad(
    hasSelectedCell: Boolean,
    allowedValues: Set<Int>,
    onValueSelected: (Int?) -> Unit,
) {
    Column(
        modifier = Modifier.widthIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (1..9).chunked(3).forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowValues.forEach { value ->
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        enabled = !hasSelectedCell || value in allowedValues,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = { onValueSelected(value) },
                    ) {
                        Text(value.toString(), fontSize = 14.sp)
                    }
                }
            }
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
        modifier = Modifier.widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(Res.string.board_solution_mode),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            .height(40.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = text,
                textAlign = TextAlign.Center,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
