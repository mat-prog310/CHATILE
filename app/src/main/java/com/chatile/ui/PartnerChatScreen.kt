package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.ApiClient
import com.chatile.ChatViewModel
import com.chatile.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun PartnerChatScreen(
    relationshipId: Long,
    vm: ChatViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var partnerData by remember { mutableStateOf<JSONObject?>(null) }
    var messages by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var draftText by remember { mutableStateOf("") }

    fun loadPartnerChat() {
        scope.launch {
            runCatching { ApiClient.getPartnerChat(relationshipId) }
                .onSuccess { res ->
                    partnerData = res.optJSONObject("partner")
                    val msgArr = res.optJSONArray("messages") ?: JSONArray()
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until msgArr.length()) list.add(msgArr.getJSONObject(i))
                    messages = list
                }
        }
    }

    LaunchedEffect(relationshipId) {
        loadPartnerChat()
    }

    val partnerName = partnerData?.optString("name") ?: "Mon Amour"
    val intimacyLevel = partnerData?.optInt("intimacyLevel", 3) ?: 3

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
                    .background(NightSurface.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(VioletAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(partnerName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(partnerName, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        for (i in 1..10) {
                            val active = i <= intimacyLevel
                            Text(
                                text = "❤️",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.Unspecified else Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.optLong("id", System.currentTimeMillis()) }) { msg ->
                    val isUser = msg.optBoolean("isUser", false)
                    val text = msg.optString("text", "")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isUser) Brush.linearGradient(listOf(VioletAccent, PinkAccent))
                                    else Brush.linearGradient(listOf(NightSurfaceRaised, NightSurface))
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            runCatching { ApiClient.giftPartner(relationshipId, 1L) }
                                .onSuccess { loadPartnerChat() }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PinkAccent)
                ) {
                    Icon(Icons.Outlined.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cadeau 🎁")
                }

                Button(
                    onClick = {
                        scope.launch {
                            runCatching { ApiClient.datePartner(relationshipId) }
                                .onSuccess { loadPartnerChat() }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                ) {
                    Icon(Icons.Outlined.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Date Virtuel 💖")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NightSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    placeholder = { Text("Discute avec ton amour...", color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VioletAccent,
                        unfocusedBorderColor = SubtleBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                IconButton(
                    onClick = {
                        if (draftText.isNotBlank()) {
                            val content = draftText
                            draftText = ""
                            scope.launch {
                                runCatching { ApiClient.interactPartner(relationshipId, content) }
                                    .onSuccess { loadPartnerChat() }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(VioletAccent, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = Color.White)
                }
            }
        }
    }
}
