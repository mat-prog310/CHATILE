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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    val summary by vm.virtualSummary.collectAsState()
    val userMoney = summary?.optInt("money", 0) ?: 0

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Maisons", "Vêtements", "Véhicules", "Animaux")
    val categoryKeys = listOf("house", "clothing", "vehicle", "pet")

    var selectedItemForDetail by remember { mutableStateOf<JSONObject?>(null) }

    LaunchedEffect(Unit) {
        vm.loadVirtualShop()
        vm.loadVirtualSummary()
    }

    val itemsArray = shopData?.optJSONArray("items") ?: JSONArray()
    val itemList = remember(shopData, selectedTab) {
        val cat = categoryKeys[selectedTab]
        val list = mutableListOf<JSONObject>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            if (obj.optString("category") == cat) {
                list.add(obj)
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

            if (itemList.isEmpty()) {
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
                        val itemId = item.getLong("id")
                        val name = item.getString("name")
                        val price = item.optInt("pricePieces", 100)
                        val imageUrl = item.optString("imageUrl")
                        val canAfford = userMoney >= price

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = NightSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedItemForDetail = item }
                                .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = name,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NightSurfaceRaised),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    name,
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "💰 $price Pièces",
                                    color = if (canAfford) VioletAccent else Color.Red,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    if (selectedItemForDetail != null) {
        val item = selectedItemForDetail!!
        val itemId = item.getLong("id")
        val name = item.getString("name")
        val desc = item.optString("description", "")
        val price = item.optInt("pricePieces", 100)
        val imageUrl = item.optString("imageUrl")
        val canAfford = userMoney >= price

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
                AsyncImage(
                    model = imageUrl,
                    contentDescription = name,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NightSurfaceRaised),
                    contentScale = ContentScale.Crop
                )
                Text(name, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("Prix : 💰 $price Pièces", color = if (canAfford) VioletAccent else Color.Red, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Button(
                    onClick = {
                        vm.buyVirtualItem(itemId) { selectedItemForDetail = null }
                    },
                    enabled = canAfford,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                ) {
                    Text(if (canAfford) "Acheter" else "Pièces insuffisantes")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
