package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatile.ApiClient
import com.chatile.ChatViewModel
import com.chatile.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun VirtualAvatarScreen(
    vm: ChatViewModel,
    onBack: () -> Unit
) {
    val summary by vm.virtualSummary.collectAsState()
    var avatarData by remember { mutableStateOf<JSONObject?>(null) }
    var inventoryItems by remember { mutableStateOf<JSONArray>(JSONArray()) }

    val scope = rememberCoroutineScope()

    fun loadAvatarInfo() {
        scope.launch {
            runCatching { ApiClient.getVirtualAvatar() }
                .onSuccess { avatarData = it }
            runCatching { ApiClient.getInventory() }
                .onSuccess { inventoryItems = it }
        }
    }

    LaunchedEffect(Unit) {
        loadAvatarInfo()
    }

    val avatarName = avatarData?.optString("name") ?: summary?.optString("avatarName") ?: "Avatar"
    val avatarImageUrl = avatarData?.optString("avatarImageUrl")
    val equippedArr = avatarData?.optJSONArray("equippedItems") ?: JSONArray()
    val equippedList = remember(avatarData) {
        val list = mutableListOf<JSONObject>()
        for (i in 0 until equippedArr.length()) list.add(equippedArr.getJSONObject(i))
        list
    }

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text("Mon Avatar & Équipement", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            // Large Avatar Preview
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VirtualAvatar(
                        avatarUrl = avatarImageUrl,
                        equippedItems = equippedList,
                        size = 140.dp
                    )
                    Text(avatarName, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Éléments équipés : ${equippedList.size}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Text("Inventaire Virtuel (Équipement)", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val virtualInventoryList = remember(inventoryItems) {
                val list = mutableListOf<JSONObject>()
                for (i in 0 until inventoryItems.length()) {
                    val obj = inventoryItems.getJSONObject(i)
                    val cat = obj.optString("type")
                    if (cat in listOf("clothing", "shoes", "accessory")) {
                        list.add(obj)
                    }
                }
                list
            }

            if (virtualInventoryList.isEmpty()) {
                Text("Aucun vêtement ou accessoire dans ton inventaire. Visite la boutique !", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(virtualInventoryList, key = { it.getLong("inventoryId") }) { item ->
                        val invId = item.getLong("inventoryId")
                        val itemId = item.getLong("itemId")
                        val name = item.getString("name")
                        val imageUrl = item.optString("imageUrl")
                        val isEquipped = equippedList.any { it.optLong("id") == itemId }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isEquipped) NightSurfaceRaised else NightSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        runCatching { ApiClient.equipItem(invId) }
                                            .onSuccess { loadAvatarInfo() }
                                    }
                                }
                                .border(1.dp, if (isEquipped) VioletAccent else SubtleBorder, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = name,
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(NightSurfaceRaised),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(name, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                Spacer(Modifier.height(2.dp))
                                Text(if (isEquipped) "✓ Équipé" else "Équiper", color = if (isEquipped) VioletAccent else TextSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
