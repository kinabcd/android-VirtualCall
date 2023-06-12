package tw.lospot.kin.call.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tw.lospot.kin.call.ui.theme.Shapes


@Composable
fun TwoLineInfoCard(
    title: String,
    content: String,
    contentFontFamily: FontFamily? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null
) {
    InfoCard(
        backgroundColor = backgroundColor,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(content, fontFamily = contentFontFamily)
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.then(modifier)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = Shapes.small,
        color = backgroundColor,
        border = BorderStroke(1.dp, contentColorFor(backgroundColor).copy(alpha = 0.2f)),
        content = content
    )
}