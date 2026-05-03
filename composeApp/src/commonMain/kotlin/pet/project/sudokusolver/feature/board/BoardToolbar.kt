package pet.project.sudokusolver.feature.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import sudokusolver.composeapp.generated.resources.Res
import sudokusolver.composeapp.generated.resources.board_back_symbol
import sudokusolver.composeapp.generated.resources.board_clear

@Composable
internal fun BoardToolbar(
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
