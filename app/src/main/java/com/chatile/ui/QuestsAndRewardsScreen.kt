package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun QuestsAndRewardsScreen(
    vm: ChatViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dailyCanClaim by vm.dailyCanClaim.collectAsState()
    val todayReward by vm.todayReward.collectAsState()
    val streakDays by vm.streakDays.collectAsState()
    val canOpenMysteryBox by vm.canOpenMysteryBox.collectAsState()

    var adChallenges by remember { mutableStateOf<JSONArray>(JSONArray()) }
    var rewards by remember { mutableStateOf<JSONArray>(JSONArray()) }
    var quests by remember { mutableStateOf<JSONArray>(JSONArray()) }

    fun loadData() {
        scope.launch {
            vm.loadDailyStatus()
            runCatching { ApiClient.getAdChallenges() }
                .onSuccess { adChallenges = it.optJSONArray("challenges") ?: JSONArray() }
            runCatching { ApiClient.getRewards() }
                .onSuccess { rewards = it }
            runCatching { ApiClient.getQuests() }
                .onSuccess { quests = it }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
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
                Text("Défis & Récompenses", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Daily Claim & Mystery Box
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NightSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🎁 Récompenses Quotidiennes", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Récompense quotidienne", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                    Text("Série $streakDays jours · +$todayReward Pièces", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(onClick = { vm.claimDailyReward(); loadData() }, enabled = dailyCanClaim) {
                                    Text(if (dailyCanClaim) "Récupérer" else "Récupérée")
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Coffre mystère", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                    Text("10 à 50 Pièces · une fois par semaine", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                OutlinedButton(onClick = { vm.claimMysteryBox(); loadData() }, enabled = canOpenMysteryBox) {
                                    Text(if (canOpenMysteryBox) "Ouvrir" else "Déjà ouvert")
                                }
                            }
                        }
                    }
                }

                // Ad Challenges (10, 25, 50, 100... 1000)
                item {
                    Text("🎯 Défis Publicitaires", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(adChallenges.length()) { index ->
                    val ch = adChallenges.optJSONObject(index) ?: return@items
                    val target = ch.optInt("target")
                    val reward = ch.optInt("reward")
                    val progress = ch.optInt("progress")
                    val claimed = ch.optInt("claimed") == 1
                    val completed = progress >= target

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NightSurfaceRaised),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Regarder $target publicités en 1 jour", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Progression : $progress / $target · Récompense : 🪙 $reward Pièces", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { ApiClient.claimAdChallenge(target) }
                                            .onSuccess { loadData(); vm.refresh() }
                                    }
                                },
                                enabled = completed && !claimed
                            ) {
                                Text(if (claimed) "Réclamé" else if (completed) "Récupérer" else "$progress/$target")
                            }
                        }
                    }
                }

                // Weekly Quests
                item {
                    Text("📜 Quêtes Hebdomadaires", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(quests.length()) { index ->
                    val quest = quests.optJSONObject(index) ?: return@items
                    val qId = quest.optLong("id")
                    val qTitle = quest.optString("title")
                    val qDesc = quest.optString("description")
                    val qProgress = quest.optInt("progress")
                    val qTarget = quest.optInt("target")
                    val qReward = quest.optInt("reward")
                    val qClaimed = quest.optInt("claimed") == 1
                    val qCompleted = qProgress >= qTarget

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NightSurfaceRaised),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(qTitle, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(qDesc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("Progression : $qProgress / $qTarget · Récompense : 🪙 $qReward", color = VioletAccent, style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { vm.claimQuest(qId); loadData() },
                                enabled = qCompleted && !qClaimed
                            ) {
                                Text(if (qClaimed) "Réclamé" else if (qCompleted) "Récupérer" else "$qProgress/$qTarget")
                            }
                        }
                    }
                }
            }
        }
    }
}
