package com.chatile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatile.ChatViewModel
import com.chatile.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent

@Composable
fun ProfileScreen(
    vm: ChatViewModel,
    onOpenShop: () -> Unit,
    onWatchAd: () -> Unit = {},
    onOpenAdmin: () -> Unit = {},
    onOpenCreatorEarnings: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by vm.profile.collectAsState()
    val pieces by vm.pieces.collectAsState()
    val characters by vm.characters.collectAsState()
    val dailyCanClaim by vm.dailyCanClaim.collectAsState()
    val todayReward by vm.todayReward.collectAsState()
    val streakDays by vm.streakDays.collectAsState()
    val canOpenMysteryBox by vm.canOpenMysteryBox.collectAsState()
    val mysteryBox by vm.mysteryBox.collectAsState()
    val nsfw by vm.nsfwEnabled.collectAsState()
    val tier by vm.subscriptionTier.collectAsState()
    val expiresAt by vm.subscriptionExpiresAt.collectAsState()
    val stats by vm.stats.collectAsState()
    val rewards by vm.rewards.collectAsState()
    val quests by vm.quests.collectAsState()
    val piecesHistory by vm.piecesHistory.collectAsState()
    val referralCode by vm.referralCode.collectAsState()
    val creatorEarnings by vm.creatorEarnings.collectAsState()
    val error by vm.error.collectAsState()
    var showStats by remember { mutableStateOf(false) }
    var showRewards by remember { mutableStateOf(false) }
    var showQuests by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showCreatorEarnings by remember { mutableStateOf(false) }
    var referralInput by remember { mutableStateOf("") }
    var name by remember(profile) { mutableStateOf(profile?.displayName.orEmpty()) }
    var bio by remember(profile) { mutableStateOf(profile?.bio.orEmpty()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.uploadProfileAvatar(context, uri)
    }

    LaunchedEffect(Unit) { vm.loadProfile(); vm.loadDailyStatus() }
    
    // Show error as toast
    LaunchedEffect(error) {
        error?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            vm.clearError()
        }
    }
    NightBackdrop {
        Column(
            modifier = Modifier.fillMaxHeight().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.logo_chatile),
                    contentDescription = "CHATILE Logo",
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "CHATILE",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (profile?.avatarUrl != null) {
                    AsyncImage(profile?.avatarUrl, contentDescription = "Photo de profil", modifier = Modifier.size(76.dp).clip(CircleShape))
                } else {
                    CharacterAvatar(name.ifBlank { "Moi" }, size = 76.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name.ifBlank { "Votre profil" }, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("Modifier la photo") }
                }
            }
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom affiché") },
                modifier = Modifier.fillMaxWidth(),
                colors = profileFieldColors(),
                singleLine = true,
            )
            TextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                colors = profileFieldColors(),
                minLines = 3,
            )
            Button(onClick = { vm.updateProfile(name, bio) }, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer le profil") }
            if (profile?.role == "admin") {
                Button(
                    onClick = onOpenAdmin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DA1F2))
                ) {
                    Text("⚙️ Panneau d'Administration")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurfaceRaised).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$pieces Pièces", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                    Text("Solde disponible", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Text("✦", color = VioletAccent, style = MaterialTheme.typography.headlineMedium)
            }
            Button(onClick = onOpenShop, modifier = Modifier.fillMaxWidth()) { Text("Boutique") }
            Text("Gains gratuits", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NightSurfaceRaised).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Récompense quotidienne", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("Série $streakDays jours · +$todayReward Pièces", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = vm::claimDailyReward, enabled = dailyCanClaim) {
                    Text(if (dailyCanClaim) "Récupérer" else "Récupérée")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NightSurfaceRaised).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Coffre mystère", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text(
                        mysteryBox?.let { "${it.optString("rarity")} · +${it.optInt("amount")} Pièces" } ?: "10 à 50 Pièces · une fois par semaine",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(onClick = vm::claimMysteryBox, enabled = canOpenMysteryBox) {
                    Text(if (canOpenMysteryBox) "Ouvrir" else "Déjà ouvert")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NightSurfaceRaised).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Regarder une publicité", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("+5 Pièces (max 10/jour, attente 5 min)", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = onWatchAd,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                ) {
                    Text("Regarder (+5)")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showRewards = !showRewards; if (!showRewards) vm.loadRewards() }, modifier = Modifier.weight(1f)) { Text("Récompenses") }
                OutlinedButton(onClick = { showQuests = !showQuests; if (!showQuests) vm.loadQuests() }, modifier = Modifier.weight(1f)) { Text("Quêtes") }
            }
            if (showRewards) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Récompenses uniques", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                    for (index in 0 until rewards.length()) {
                        val reward = rewards.optJSONObject(index) ?: continue
                        val action = reward.optString("action").replace("_", " ")
                        val amount = reward.optInt("reward")
                        val status = reward.optString("status", "locked")
                        val conditionMet = reward.optBoolean("condition_met", false)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NightSurfaceRaised).padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$action · $amount Pièces", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = when (status) {
                                        "claimed" -> "✅ Réclamé"
                                        "available" -> "🎯 À faire"
                                        "locked" -> "🔒 Verrouillé"
                                        else -> ""
                                    },
                                    color = when (status) {
                                        "claimed" -> Color.Green
                                        "available" -> Color(0xFFFFC107) // Yellow
                                        else -> TextSecondary
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (status == "available") {
                                OutlinedButton(
                                    onClick = { vm.claimReward(reward.optString("action")) },
                                    enabled = conditionMet
                                ) { Text("Récupérer") }
                            } else if (status == "claimed") {
                                Text("✓", color = Color.Green, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            if (showQuests) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (index in 0 until quests.length()) {
                        val quest = quests.optJSONObject(index) ?: continue
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${quest.optString("title")} · ${quest.optInt("progress")}/${quest.optInt("target")}", color = TextSecondary, modifier = Modifier.weight(1f))
                            if (quest.optInt("claimed") == 0) OutlinedButton(
                                onClick = { vm.claimQuest(quest.optLong("id")) },
                                enabled = quest.optInt("progress") >= quest.optInt("target"),
                            ) { Text("+${quest.optInt("reward")}") }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showHistory = !showHistory; if (!showHistory) vm.loadPiecesHistory() }, modifier = Modifier.weight(1f)) { Text("Historique Pièces") }
                OutlinedButton(onClick = vm::loadReferralCode, modifier = Modifier.weight(1f)) { Text("Parrainage") }
            }
            if (showHistory) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (index in 0 until piecesHistory.length()) {
                        val entry = piecesHistory.optJSONObject(index) ?: continue
                        Text("${entry.optString("description")} · ${if (entry.optInt("amount") > 0) "+" else ""}${entry.optInt("amount")}", color = TextSecondary)
                    }
                }
            }
            referralCode?.let { code ->
                Text("Ton code : $code", color = VioletAccent, style = MaterialTheme.typography.titleMedium)
                TextField(value = referralInput, onValueChange = { referralInput = it }, label = { Text("Code reçu") }, modifier = Modifier.fillMaxWidth(), colors = profileFieldColors(), singleLine = true)
                OutlinedButton(onClick = { vm.applyReferral(referralInput) }, enabled = referralInput.isNotBlank()) { Text("Utiliser un code") }
            }
            OutlinedButton(
                onClick = { if (!showStats) vm.loadStats(); showStats = !showStats },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (showStats) "Masquer mes stats" else "Mes stats") }
            if (showStats) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("${stats?.totalMessages ?: 0} messages échangés", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("Personnage préféré : ${stats?.preferredCharacter ?: "Pas encore déterminé"}", color = TextSecondary)
                    stats?.messagesByCharacter?.forEach { row ->
                        Text("${row.characterName} · ${row.messageCount} messages", color = TextSecondary)
                    }
                }
            }
            
            var showPublicCharacters by remember { mutableStateOf(false) }
            
            OutlinedButton(
                onClick = {
                    if (!showPublicCharacters) {
                        showPublicCharacters = true
                        vm.loadCharacters()
                    } else {
                        showPublicCharacters = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (showPublicCharacters) "Masquer mes personnages" else "Mes personnages") }
            
            if (showPublicCharacters) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Mes personnages", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    val userCharacters = characters.filter { it.userId != null && !it.isSystem }
                    if (userCharacters.isEmpty()) {
                        Text("Aucune création pour le moment", color = TextSecondary)
                    } else {
                        userCharacters.forEach { character ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${character.name} · ${if (character.isPublic) "Public" else "Privé"}",
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedButton(
                                    onClick = { 
                                        val id = character.id ?: return@OutlinedButton
                                        if (character.isPublic) vm.unpublishCharacter(id) else vm.publishCharacter(id)
                                    }
                                ) { Text(if (character.isPublic) "Dépublier" else "Publier") }
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    if (!showCreatorEarnings) {
                        showCreatorEarnings = true
                        vm.loadCreatorEarnings()
                    } else {
                        showCreatorEarnings = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (showCreatorEarnings) "Masquer mes revenus créateur" else "Mes revenus créateur") }

            if (showCreatorEarnings) {
                val totalEarnings = creatorEarnings?.optInt("totalEarnings", 0) ?: 0
                val characterEarnings = creatorEarnings?.optInt("characterEarnings", 0) ?: 0
                val storyEarnings = creatorEarnings?.optInt("storyEarnings", 0) ?: 0
                val transactions = creatorEarnings?.optJSONArray("recentTransactions") ?: org.json.JSONArray()

                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Mes Revenus Créateur", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total gagné", color = TextSecondary)
                        Text("$totalEarnings Pièces", color = VioletAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Personnages", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("+$characterEarnings Pièces", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Histoires", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("+$storyEarnings Pièces", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = onOpenCreatorEarnings,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text("Classement & Détails complets")
                    }

                    if (transactions.length() > 0) {
                        Text("Dernières ventes", color = TextPrimary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
                        for (i in 0 until minOf(transactions.length(), 5)) {
                            val item = transactions.optJSONObject(i) ?: continue
                            val description = item.optString("description", "")
                            val amount = item.optInt("amount", 0)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text("+$amount", color = VioletAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurface).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Paramètres NSFW", color = TextPrimary)
                    Text("Enregistré sur votre compte", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = nsfw, onCheckedChange = vm::setNsfw)
            }
            if (tier != "free") {
                Text("${if (tier == "premium_plus") "Premium+" else "Premium"} · expire le ${expiresAt ?: "date inconnue"}", color = VioletAccent, style = MaterialTheme.typography.titleMedium)
            } else {
                OutlinedButton(onClick = onOpenShop, modifier = Modifier.fillMaxWidth()) { Text("Passer Premium") }
            }
            if (!error.isNullOrBlank()) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun profileFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = NightSurface,
    unfocusedContainerColor = NightSurface,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedIndicatorColor = VioletAccent,
    unfocusedIndicatorColor = SubtleBorder,
    focusedLabelColor = VioletAccent,
    unfocusedLabelColor = TextSecondary,
)