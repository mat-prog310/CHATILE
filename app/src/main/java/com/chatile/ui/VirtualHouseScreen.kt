package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.chatile.ChatViewModel
import com.chatile.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun VirtualHouseScreen(
    vm: ChatViewModel,
    onBack: () -> Unit
) {
    val houseData by vm.virtualHouse.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadVirtualHouse()
    }

    val house = houseData?.optJSONObject("house")
    val pets = houseData?.optJSONArray("pets") ?: JSONArray()
    val furniture = houseData?.optJSONArray("furniture") ?: JSONArray()

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
                Text("Ma Maison & Animaux", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            // House Image & Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val imageUrl = house?.optString("imageUrl")
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = house.optString("name"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(NightSurfaceRaised),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("🏠 Résidence actuelle", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        Text(house?.optString("name") ?: "Sans domicile", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(house?.optString("description") ?: "Achète une maison dans la boutique !", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text("🐾 Mes Animaux (${pets.length()})", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (pets.length() == 0) {
                Text("Aucun animal adopté. Visite la boutique pour adopter un chat, un chien ou un dragon !", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            } else {
                val petList = remember(pets) {
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until pets.length()) list.add(pets.getJSONObject(i))
                    list
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(petList, key = { it.getLong("id") }) { pet ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = NightSurfaceRaised),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                AsyncImage(
                                    model = pet.optString("imageUrl"),
                                    contentDescription = pet.optString("name"),
                                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(pet.optString("name"), color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
