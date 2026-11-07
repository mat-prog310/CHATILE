package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.ChatViewModel
import com.chatile.ui.theme.*

@Composable
fun VirtualLifeScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenHouse: () -> Unit,
    onOpenJob: () -> Unit,
    onOpenAvatar: () -> Unit
) {
    val summary by vm.virtualSummary.collectAsState()
    val event by vm.virtualEvent.collectAsState()
    var avatarNameInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.loadVirtualSummary()
        vm.loadVirtualEvents()
    }

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
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
                Text("Ma Vie Virtuelle", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            val hasAvatar = summary?.optBoolean("hasAvatar", false) == true

            if (!hasAvatar) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Crée ton avatar pour commencer ton aventure dans le monde virtuel !", color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        OutlinedTextField(
                            value = avatarNameInput,
                            onValueChange = { avatarNameInput = it },
                            label = { Text("Nom de ton avatar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (avatarNameInput.isNotBlank()) {
                                    vm.createAvatar(avatarNameInput) { avatarNameInput = "" }
                                }
                            },
                            enabled = avatarNameInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Commencer ma vie virtuelle")
                        }
                    }
                }
            } else {
                val name = summary?.optString("avatarName") ?: "Avatar"
                val level = summary?.optInt("level", 1) ?: 1
                val xp = summary?.optInt("xp", 0) ?: 0
                val xpToNext = summary?.optInt("xpToNextLevel", 100) ?: 100
                val money = summary?.optInt("money", 0) ?: 0
                val job = summary?.optString("job", "Sans emploi") ?: "Sans emploi"
                val house = summary?.optString("houseName", "Sans domicile") ?: "Sans domicile"
                val pets = summary?.optInt("petsCount", 0) ?: 0

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(VioletAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Niveau $level · XP : $xp / $xpToNext", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = onOpenAvatar) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Avatar", tint = VioletAccent)
                            }
                        }

                        LinearProgressIndicator(
                            progress = { xp.toFloat() / xpToNext.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = VioletAccent,
                            trackColor = NightSurfaceRaised
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💰 $money Pièces", color = VioletAccent, fontWeight = FontWeight.Bold)
                            Text("💼 $job", color = TextSecondary)
                            Text("🏠 $house", color = TextSecondary)
                        }
                    }
                }

                if (event != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NightSurfaceRaised),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = VioletAccent)
                            Spacer(Modifier.width(12.dp))
                            Text(event?.optString("description") ?: "", color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { vm.workVirtual { } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                    ) {
                        Icon(Icons.Outlined.Work, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Travailler")
                    }
                    Button(
                        onClick = onOpenShop,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NightSurfaceRaised)
                    ) {
                        Icon(Icons.Outlined.Store, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Boutique", color = TextPrimary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onOpenHouse, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ma Maison ($pets 🐾)")
                    }
                    OutlinedButton(onClick = onOpenJob, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Carrière & Métiers")
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pub Vie Virtuelle", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("+50 Pièces (1 fois par heure)", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { vm.watchVirtualAd { } }) {
                            Text("Regarder (+50)")
                        }
                    }
                }
            }
        }
    }
}
