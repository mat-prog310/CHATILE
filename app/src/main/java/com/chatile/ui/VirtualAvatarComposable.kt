package com.chatile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BaseAvatarBody(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        // Head
        drawCircle(color = Color(0xFFFFD1A9), radius = size.width * 0.22f, center = Offset(centerX, size.height * 0.3f))
        // Hair
        drawCircle(color = Color(0xFF5D4037), radius = size.width * 0.23f, center = Offset(centerX, size.height * 0.25f))
        // Torso / Body
        drawRect(
            color = Color(0xFF4A90E2),
            topLeft = Offset(centerX - size.width * 0.2f, size.height * 0.52f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.4f, size.height * 0.4f)
        )
    }
}

@Composable
fun VirtualAvatarWithLayers(
    equippedItems: List<VirtualItem>,
    size: Dp = 150.dp
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        BaseAvatarBody(modifier = Modifier.fillMaxSize())

        equippedItems.filter { it.category == "clothing" }.forEach { _ ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.toPx() / 2f
                drawRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(centerX - size.toPx() * 0.22f, size.toPx() * 0.54f),
                    size = androidx.compose.ui.geometry.Size(size.toPx() * 0.44f, size.toPx() * 0.35f)
                )
            }
        }

        equippedItems.filter { it.category == "pet" }.forEach { _ ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFFFFB74D), radius = 16f, center = Offset(size.toPx() * 0.75f, size.toPx() * 0.75f))
            }
        }
    }
}
