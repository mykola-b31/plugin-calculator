package com.github.mykolab31.plugincalculator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
    isWide: Boolean = false,
    type: ButtonType = ButtonType.NUMBER,
    onClick: () -> Unit
) {
    val backgroundColor = when (type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.surfaceVariant
        ButtonType.OPERATION -> MaterialTheme.colorScheme.secondaryContainer
        ButtonType.ACTION -> MaterialTheme.colorScheme.errorContainer
        ButtonType.PLUGIN -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val textColor = when (type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.onSurfaceVariant
        ButtonType.OPERATION -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonType.ACTION -> MaterialTheme.colorScheme.onErrorContainer
        ButtonType.PLUGIN -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = modifier
            .padding(4.dp)
            .then(if (!isWide) Modifier.aspectRatio(1f) else Modifier)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}