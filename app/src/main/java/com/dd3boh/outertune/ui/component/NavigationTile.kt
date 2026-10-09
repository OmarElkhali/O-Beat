package com.dd3boh.outertune.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dd3boh.outertune.ui.theme.GlassSurface

@Composable
fun NavigationTile(
    title: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = NavigationTileContent(title, onClick, modifier) {
    Icon(painter = painterResource(icon), contentDescription = null)
}

@Composable
fun NavigationTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = NavigationTileContent(title, onClick, modifier) {
    Icon(icon, contentDescription = null)
}

@Composable
private fun NavigationTileContent(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(6.dp),
    ) {
        GlassSurface(shape = MaterialTheme.shapes.medium, modifier = Modifier.size(60.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                icon()
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
