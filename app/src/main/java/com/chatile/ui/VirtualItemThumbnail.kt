package com.chatile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.R
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.VioletAccent

fun getImageResourceForItem(itemName: String): Int {
    return when (itemName.trim().lowercase()) {
        "studio", "studio cosy" -> R.drawable.studio
        "appartement" -> R.drawable.appartement
        "maison de ville" -> R.drawable.appartement
        "villa" -> R.drawable.villa
        "manoir" -> R.drawable.manoir
        "château", "chateau" -> R.drawable.chateau
        "chaussures de sport", "sneakers" -> R.drawable.chaussure
        "montre de luxe", "montre" -> R.drawable.montre
        "vélo", "velo" -> R.drawable.v_lo
        "scooter" -> R.drawable.moto
        "voiture citadine" -> R.drawable.voiture_citadine
        "voiture de sport", "voiture sport" -> R.drawable.voiture_sport
        "jet privé", "jet prive" -> R.drawable.jet_priv_
        "chat" -> R.drawable.chat
        "chien" -> R.drawable.chien
        "dragon", "dragon de compagnie" -> R.drawable.dragon
        "licorne" -> R.drawable.licorne
        "fontaine de jardin" -> R.drawable.villa
        "chien robot" -> R.drawable.robot
        else -> R.drawable.logo_chatile
    }
}

@Composable
fun VirtualItemThumbnail(
    item: VirtualItem,
    modifier: Modifier = Modifier
) {
    val isRareOrEpic = item.rarity in listOf("rare", "epique", "legendaire")
    val borderColor = when (item.rarity) {
        "legendaire" -> Color(0xFFFFD700)
        "epique" -> Color(0xFF9C27B0)
        "rare" -> Color(0xFF2196F3)
        else -> Color(0xFF262626)
    }

    Box(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NightSurface)
            .border(width = if (isRareOrEpic) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF2A1B4E), Color(0xFF140B24)),
                            start = Offset(0f, 0f),
                            end = Offset(90f, 90f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val imageRes = getImageResourceForItem(item.name)
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = item.name,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🪙", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${item.price}",
                    color = VioletAccent,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
