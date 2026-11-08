package com.chatile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.ChatViewModel
import com.chatile.ui.theme.*

@Composable
fun VirtualAvatarScreen(
    vm: ChatViewModel,
    onBack: () -> Unit
) {
    var equippedItems by remember {
        mutableStateOf(
            listOf(
                VirtualItem(3, "T-shirt Tye-Die", "clothing", 50, "commun")
            )
        )
    }

    val availableItems = MockVirtualData.shopItems

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text("Éditeur d'Avatar", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VirtualAvatarWithLayers(equippedItems = equippedItems, size = 160.dp)
                    Text("Avatar Personnalisé", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Éléments équipés : ${equippedItems.size}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Équipements disponibles", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(availableItems, key = { it.id }) { item ->
                    val isEquipped = equippedItems.any { it.id == item.id }
                    Box(
                        modifier = Modifier
                            .clickable {
                                equippedItems = if (isEquipped) {
                                    equippedItems.filter { it.id != item.id }
                                } else {
                                    if (item.category == "clothing" || item.category == "house") {
                                        equippedItems.filter { it.category != item.category } + item
                                    } else {
                                        equippedItems + item
                                    }
                                }
                            }
                    ) {
                        VirtualItemThumbnail(item = item)
                    }
                }
            }
        }
    }
}
