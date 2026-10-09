package com.dd3boh.outertune.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A light-weight glass treatment which works on every Android version.
 *
 * Compose does not provide a reliable cross-version backdrop blur. A translucent
 * surface, subtle highlight and restrained elevation give the same hierarchy
 * without the GPU cost or the readability problems of a fake blur.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val liquidGradient = remember(colors) {
        Brush.linearGradient(
            listOf(
                colors.surfaceContainerHigh.copy(alpha = 0.97f),
                colors.surfaceContainer.copy(alpha = 0.95f),
                colors.surfaceContainerLow.copy(alpha = 0.96f),
            )
        )
    }
    CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
        Box(
            modifier = modifier
                .shadow(3.dp, shape, clip = false)
                .clip(shape)
                .background(liquidGradient)
                .border(1.dp, colors.outlineVariant.copy(alpha = 0.45f), shape)
                .padding(contentPadding),
        ) { content() }
    }
}
