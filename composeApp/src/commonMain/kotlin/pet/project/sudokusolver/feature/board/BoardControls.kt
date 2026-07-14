package pet.project.sudokusolver.feature.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
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
import pet.project.sudokusolver.domain.SolutionMode
import sudokusolver.composeapp.generated.resources.Res
import sudokusolver.composeapp.generated.resources.*

@Composable
internal fun StepControls(
    nextStepIndex: Int,
    stepCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            enabled = nextStepIndex > 0,
            onClick = onPrevious,
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(
                text = stringResource(Res.string.board_previous_step),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Button(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            enabled = nextStepIndex < stepCount,
            onClick = onNext,
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Text(
                if (nextStepIndex < stepCount) {
                    stringResource(Res.string.board_next_step)
                } else {
                    stringResource(Res.string.board_all_steps_done)
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
internal fun InputTools(
    enabled: Boolean,
    isPencilMode: Boolean,
    onPencilModeToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clickable(enabled = enabled, onClick = onPencilModeToggle),
            shape = MaterialTheme.shapes.small,
            color = if (isPencilMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isPencilMode) {
                        "✓ ${stringResource(Res.string.board_pencil_symbol)}"
                    } else {
                        stringResource(Res.string.board_pencil_symbol)
                    },
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    fontSize = 17.sp,
                    fontWeight = if (isPencilMode) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        OutlinedButton(
            modifier = Modifier
                .weight(2f)
                .height(48.dp),
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(0.dp),
            onClick = onDelete,
        ) {
            Text(
                text = stringResource(Res.string.board_delete_digit),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
internal fun NumberPad(
    hasSelectedCell: Boolean,
    allowedValues: Set<Int>,
    onValueSelected: (Int?) -> Unit,
    dense: Boolean = false,
) {
    val spacing = if (dense) 6.dp else 8.dp
    val rowSize = if (dense) 5 else 3
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        (1..9).chunked(rowSize).forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                rowValues.forEach { value ->
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !hasSelectedCell || value in allowedValues,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(0.dp),
                        onClick = { onValueSelected(value) },
                    ) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ModeChooser(
    enabled: Boolean,
    selectedMode: SolutionMode?,
    onModeSelected: (SolutionMode) -> Unit,
    dense: Boolean = false,
) {
    val spacing = if (dense) 4.dp else 8.dp
    val controlHeight = if (dense) 48.dp else 52.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        Text(
            text = stringResource(Res.string.board_solution_mode),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.solution_mode_fast),
                enabled = enabled,
                selected = selectedMode == SolutionMode.Fast,
                onClick = { onModeSelected(SolutionMode.Fast) },
                height = controlHeight,
            )
            ModeButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.solution_mode_step_by_step),
                enabled = enabled,
                selected = selectedMode == SolutionMode.StepByStep,
                onClick = { onModeSelected(SolutionMode.StepByStep) },
                height = controlHeight,
            )
            ModeButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.solution_mode_self_practice),
                enabled = enabled,
                selected = selectedMode == SolutionMode.SelfPractice,
                onClick = { onModeSelected(SolutionMode.SelfPractice) },
                height = controlHeight,
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
    height: androidx.compose.ui.unit.Dp,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Surface(
        modifier = modifier
            .height(height)
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = container,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = if (selected) "✓ $text" else text,
                textAlign = TextAlign.Center,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
