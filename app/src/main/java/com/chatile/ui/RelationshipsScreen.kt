package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
fun RelationshipsScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onOpenPartnerChat: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var relationships by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    fun loadRelationships() {
        scope.launch {
            runCatching { ApiClient.getRelationships() }
                .onSuccess { res ->
                    val arr = res.optJSONArray("relationships") ?: JSONArray()
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
                    relationships = list
                }
                .onFailure {
                    android.widget.Toast.makeText(context, it.message ?: "Impossible de charger les relations", android.widget.Toast.LENGTH_LONG).show()
                }
        }
    }

    LaunchedEffect(Unit) {
        loadRelationships()
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
                Text("Trouve l'Amour 💖", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    scope.launch {
                        runCatching { ApiClient.generatePartner() }
                            .onSuccess { loadRelationships() }
                            .onFailure {
                                android.widget.Toast.makeText(context, it.message ?: "Erreur de génération", android.widget.Toast.LENGTH_LONG).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
            ) {
                Icon(Icons.Outlined.Favorite, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Rencontrer l'âme sœur (Nouveau Partenaire)", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            Text("Mes Relations Amoureuses", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (relationships.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune relation active. Clique ci-dessus pour trouver l'amour !", color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(relationships, key = { it.optLong("id") }) { rel ->
                        val relId = rel.optLong("id")
                        val partnerName = rel.optString("partnerName", "Partenaire")
                        val partnerJob = rel.optString("partnerJob", "Célibataire")
                        val intimacy = rel.optInt("intimacyLevel", 3)
                        val status = rel.optString("status", "dating")

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = NightSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPartnerChat(relId) }
                                .border(1.dp, VioletAccent, RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(54.dp).background(VioletAccent.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(partnerName.take(1).uppercase(), color = VioletAccent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(partnerName, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("💼 $partnerJob", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(4.dp))
                                    Text("❤️‍🔥 Intimité : $intimacy / 10 · ${status.uppercase()}", color = VioletAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onOpenPartnerChat(relId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                                ) {
                                    Text("Discuter")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
