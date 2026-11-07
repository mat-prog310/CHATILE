package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatile.ui.theme.NightSurfaceRaised
import org.json.JSONObject

@Composable
fun VirtualAvatar(
    avatarUrl: String?,
    equippedItems: List<JSONObject>,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(NightSurfaceRaised),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            AsyncImage(model = "https://placehold.co/400x400/1A1428/B47CFF?text=Avatar", contentDescription = null, modifier = Modifier.fillMaxSize())
        }

        for (i in equippedItems.indices) {
            val item = equippedItems[i]
            val imgUrl = item.optString("imageUrl")
            if (imgUrl.isNotBlank()) {
                AsyncImage(model = imgUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
