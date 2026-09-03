package com.github.mykolab31.plugincalculator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ButtonType {
    NUMBER,
    OPERATION,
    ACTION,
    PLUGIN
}

@Composable
fun CalculatorButton(
    label: String,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.NUMBER,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val backgroundColor = when (type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.surfaceVariant
        ButtonType.OPERATION -> MaterialTheme.colorScheme.primaryContainer
        ButtonType.ACTION -> MaterialTheme.colorScheme.errorContainer
        ButtonType.PLUGIN -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val textColor = when (type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.onSurfaceVariant
        ButtonType.OPERATION -> MaterialTheme.colorScheme.onPrimaryContainer
        ButtonType.ACTION -> MaterialTheme.colorScheme.onErrorContainer
        ButtonType.PLUGIN -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    val labelFontSize = if (type == ButtonType.PLUGIN) 20.sp else 26.sp

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = labelFontSize,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}