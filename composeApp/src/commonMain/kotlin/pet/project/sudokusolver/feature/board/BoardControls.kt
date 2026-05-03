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
import androidx.compose.foundation.shape.RoundedCornerShape
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
        modifier = Modifier.widthIn(max = 420.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedButton(
            modifier = Modifier
                .weight(1f)
                .height(34.dp),
            enabled = nextStepIndex > 0,
            onClick = onPrevious,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text(stringResource(Res.string.board_previous_step), fontSize = 12.sp)
        }
        Button(
            modifier = Modifier
                .weight(1f)
                .height(34.dp),
            enabled = nextStepIndex < stepCount,
            onClick = onNext,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text(
                if (nextStepIndex < stepCount) {
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

@Composable
internal fun InputTools(
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
internal fun NumberPad(
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
internal fun ModeChooser(
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
