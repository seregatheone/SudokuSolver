package pet.project.sudokusolver.feature.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
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
import pet.project.sudokusolver.domain.CellPosition
import pet.project.sudokusolver.domain.SudokuCell
import pet.project.sudokusolver.domain.SudokuGrid
import pet.project.sudokusolver.domain.SudokuSolutionStep

@Composable
internal fun SudokuBoard(
    modifier: Modifier = Modifier,
    grid: SudokuGrid,
    selectedCell: CellPosition?,
    highlightedValue: Int?,
    patternStep: SudokuSolutionStep?,
    onCellSelected: (CellPosition) -> Unit,
) {
    val thickLine = MaterialTheme.colorScheme.onSurface
    val thinLine = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val labelWeight = 0.55f
    val patternTargets = patternStep?.let { step ->
        if (step.isPlacement) {
            setOf(CellPosition(step.row, step.column))
        } else {
            step.eliminations.map { CellPosition(it.row, it.column) }.toSet()
        }
    }.orEmpty()
    val patternRelatedCells = patternStep?.relatedCells.orEmpty().toSet()

    Column(
        modifier = modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth(),
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
                                    isPatternTarget = position in patternTargets,
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
    val indicator = when {
        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isPatternTarget -> BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
        isHighlighted -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        else -> null
    }

    Box(
        modifier = modifier
            .background(background)
            .then(if (indicator == null) Modifier else Modifier.border(indicator))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (cell.value != null) {
            Text(
                text = cell.value.toString(),
                color = textColor,
                fontSize = 20.sp,
                fontWeight = when {
                    cell.isGiven -> FontWeight.Bold
                    isSelected || isPatternTarget || isHighlighted -> FontWeight.SemiBold
                    else -> FontWeight.Medium
                },
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
