package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.ApiClient
import com.chatile.ChatViewModel
import com.chatile.ui.theme.ChatileScreenBackground
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun InventoryScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var inventoryItems by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Titres", "Cadres", "Effets", "Thèmes")
    val typeKeys = listOf("title", "frame", "avatar_effect", "theme")

    var itemToRecycle by remember { mutableStateOf<JSONObject?>(null) }

    fun loadInventory() {
        scope.launch {
            runCatching { ApiClient.getInventory() }
                .onSuccess { arr ->
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
                    inventoryItems = list
                }
        }
    }

    LaunchedEffect(Unit) {
        loadInventory()
    }

    val currentTypeKey = typeKeys[selectedTab]
    val filteredItems = inventoryItems.filter { it.optString("type") == currentTypeKey }

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mon Inventaire", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        androidx.compose.material3.TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = VioletAccent
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) Color.White else Color(0xFF666666),
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun objet dans cette catégorie", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.getLong("inventoryId") }) { item ->
                        val invId = item.getLong("inventoryId")
                        val name = item.getString("name")
                        val desc = item.optString("description", "")
                        val rarity = item.optString("rarity", "commun")
                        val valuePieces = item.optInt("valuePieces", 10)
                        val isEquipped = item.optInt("isEquipped", 0) == 1

                        val rarityColor = when (rarity) {
                            "legendaire" -> Color(0xFFFFD700)
                            "epique" -> Color(0xFF9C27B0)
                            "rare" -> Color(0xFF4A90E2)
                            else -> TextSecondary
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isEquipped) NightSurfaceRaised else NightSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isEquipped) VioletAccent else SubtleBorder, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(8.dp))
                                        Text(rarity.uppercase(), color = rarityColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Recyclage : +$valuePieces Pièces", color = VioletAccent, style = MaterialTheme.typography.labelSmall)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                runCatching { ApiClient.equipItem(invId) }
                                                    .onSuccess {
                                                        loadInventory()
                                                        vm.refresh() // Met à jour le thème global de l'appli et les pièces
                                                    }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isEquipped) VioletAccent else NightSurfaceRaised)
                                    ) {
                                        if (isEquipped) {
                                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(if (isEquipped) "Équipé" else "Équiper")
                                    }

                                    OutlinedButton(
                                        onClick = { itemToRecycle = item },
                                        enabled = !isEquipped
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (itemToRecycle != null) {
        val item = itemToRecycle!!
        val invId = item.getLong("inventoryId")
        val name = item.getString("name")
        val valuePieces = item.optInt("valuePieces", 10)

        AlertDialog(
            onDismissRequest = { itemToRecycle = null },
            title = { Text("Recycler l'objet") },
            text = { Text("Recycler $name ? Tu recevras $valuePieces Pièces. Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            runCatching { ApiClient.recycleItem(invId) }
                                .onSuccess {
                                    itemToRecycle = null
                                    loadInventory()
                                    vm.refresh()
                                }
                        }
                    }
                ) {
                    Text("Recycler", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRecycle = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}
