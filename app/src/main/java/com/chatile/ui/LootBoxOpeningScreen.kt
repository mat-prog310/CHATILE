package com.chatile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatile.ApiClient
import com.chatile.ChatViewModel
import com.chatile.ui.theme.ChatileScreenBackground
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
fun LootBoxOpeningScreen(
    vm: ChatViewModel,
    onDone: () -> Unit,
) {
    var openedResult by remember { mutableStateOf<JSONObject?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isShaking by remember { mutableStateOf(true) }

    val shakeAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Shake animation
        repeat(5) {
            shakeAnim.animateTo(15f, animationSpec = tween(100, easing = FastOutSlowInEasing))
            shakeAnim.animateTo(-15f, animationSpec = tween(100, easing = FastOutSlowInEasing))
        }
        shakeAnim.animateTo(0f)
        isShaking = false

        runCatching { ApiClient.openLootBox() }
            .onSuccess { res ->
                openedResult = res
                vm.refresh()
            }
            .onFailure { errorMessage = it.message ?: "Échec de l'ouverture du coffre." }
    }

    ChatileScreenBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isShaking || openedResult == null) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer { rotationZ = shakeAnim.value }
                            .clip(RoundedCornerShape(24.dp))
                            .background(NightSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(80.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = if (errorMessage != null) errorMessage!! else "Ouverture du coffre en cours...",
                        color = if (errorMessage != null) Color.Red else TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    val res = openedResult!!
                    val isNew = res.optBoolean("isNew", true)
                    val compensation = res.optInt("compensation", 0)
                    val item = res.optJSONObject("item")
                    val itemName = item?.optString("name") ?: "Objet mystère"
                    val itemDesc = item?.optString("description") ?: ""
                    val itemRarity = item?.optString("rarity") ?: "commun"

                    val rarityColor = when (itemRarity) {
                        "legendaire" -> Color(0xFFFFD700)
                        "epique" -> Color(0xFF9C27B0)
                        "rare" -> Color(0xFF4A90E2)
                        else -> TextSecondary
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = NightSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, rarityColor, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(rarityColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Star, contentDescription = null, tint = rarityColor, modifier = Modifier.size(48.dp))
                            }

                            Text(
                                text = itemRarity.uppercase(),
                                color = rarityColor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = itemName,
                                color = TextPrimary,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = itemDesc,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )

                            if (itemRarity == "legendaire") {
                                Text(
                                    text = "LÉGENDAIRE ! Tu fais partie des rares élus.",
                                    color = Color(0xFFFFD700),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (!isNew) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Doublon possédé. +$compensation Pièces de compensation.", color = VioletAccent, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = onDone,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Super !")
                            }
                        }
                    }
                }
            }
        }
    }
}
