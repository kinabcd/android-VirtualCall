package tw.lospot.kin.call.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IconButton(
    painter: Painter,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 56.dp,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = Modifier
            .size(size)
            .then(modifier),
        enabled = enabled,
        contentPadding = contentPadding,
        onClick = onClick
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription
        )
    }
}