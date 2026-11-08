package com.chatile.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatile.ChatViewModel
import com.chatile.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualShopScreen(
    vm: ChatViewModel,
    onBack: () -> Unit
) {
    val shopData by vm.virtualShop.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Maisons", "Véhicules", "Animaux", "Vêtements")
    val categoryKeys = listOf("house", "vehicle", "pet", "clothing")

    var selectedItemForDetail by remember { mutableStateOf<JSONObject?>(null) }

    LaunchedEffect(Unit) {
        vm.loadVirtualShop()
        vm.loadVirtualSummary()
    }

    val itemsArray = shopData?.optJSONArray("items")
    val itemList = remember(shopData, selectedTab) {
        val cat = categoryKeys[selectedTab]
        val list = mutableListOf<JSONObject>()
        if (cat == "clothing") {
            return@remember list
        }
        if (itemsArray != null && itemsArray.length() > 0) {
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                if (obj.optString("category") == cat) {
                    list.add(obj)
                }
            }
        } else {
            val mockList = MockVirtualData.shopItems.filter { it.category == cat }
            for (mock in mockList) {
                val json = JSONObject().apply {
                    put("id", mock.id)
                    put("name", mock.name)
                    put("category", mock.category)
                    put("pricePieces", mock.price)
                    put("rarity", mock.rarity)
                }
                list.add(json)
            }
        }
        list
    }

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
                Text("Boutique Virtuelle", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
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

            if (categoryKeys[selectedTab] == "clothing") {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NightSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, VioletAccent, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Verrouillé",
                                tint = VioletAccent,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Vêtements & Avatar",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Fonctionnalité à venir dans la prochaine mise à jour !",
                                color = Color(0xFFA3A3A3),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (itemList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucun article dans cette catégorie", color = TextSecondary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(itemList, key = { it.getLong("id") }) { item ->
                        val virtualItem = VirtualItem(
                            id = item.getLong("id"),
                            name = item.getString("name"),
                            category = item.optString("category", "house"),
                            price = item.optInt("pricePieces", 100),
                            rarity = item.optString("rarity", "commun")
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedItemForDetail = item }
                        ) {
                            VirtualItemThumbnail(item = virtualItem, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

    if (selectedItemForDetail != null) {
        val item = selectedItemForDetail!!
        val itemId = item.getLong("id")
        val name = item.getString("name")
        val desc = item.optString("description", "Un superbe article pour votre monde virtuel.")
        val price = item.optInt("pricePieces", 100)

        ModalBottomSheet(
            onDismissRequest = { selectedItemForDetail = null },
            containerColor = NightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(name, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Text("Prix : 🪙 $price Pièces", color = VioletAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Button(
                    onClick = {
                        vm.buyVirtualItem(itemId) { selectedItemForDetail = null }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                ) {
                    Text("Acheter")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
