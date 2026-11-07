package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import org.json.JSONArray

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

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🏠 Résidence actuelle", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                    Text(house?.optString("name") ?: "Sans domicile", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(house?.optString("description") ?: "Achète une maison dans la boutique !", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text("🐾 Mes Animaux (${pets.length()})", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (pets.length() == 0) {
                Text("Aucun animal adopté. Visite la boutique pour adopter un chat, un chien ou un dragon !", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            } else {
                for (i in 0 until pets.length()) {
                    val pet = pets.optJSONObject(i) ?: continue
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NightSurfaceRaised),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pet.optString("name"), color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(pet.optString("description"), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
