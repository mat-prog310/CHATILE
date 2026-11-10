package com.chatile.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatile.AuthManager
import com.chatile.ChatViewModel
import com.chatile.R
import com.chatile.ui.theme.*
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

@Composable
private fun profileFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = NightSurfaceRaised,
    unfocusedContainerColor = NightSurfaceRaised,
    focusedIndicatorColor = VioletAccent,
    unfocusedIndicatorColor = SubtleBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = VioletAccent,
    unfocusedLabelColor = TextSecondary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    vm: ChatViewModel,
    onOpenShop: () -> Unit,
    onWatchAd: () -> Unit = {},
    onOpenCreatorEarnings: () -> Unit = {},
    onOpenLootBoxes: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onOpenPublicProfile: (Long) -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by vm.profile.collectAsState()
    val pieces by vm.pieces.collectAsState()
    val subscriptionTier by vm.subscriptionTier.collectAsState()
    val subscriptionExpiresAt by vm.subscriptionExpiresAt.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCreatorEarnings by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    val nsfw by vm.nsfwEnabled.collectAsState()
    val referralCode by vm.referralCode.collectAsState()
    val creatorEarnings by vm.creatorEarnings.collectAsState()
    val error by vm.error.collectAsState()
    var referralInput by remember { mutableStateOf("") }
    var name by remember(profile) { mutableStateOf(profile?.displayName.orEmpty()) }
    var bio by remember(profile) { mutableStateOf(profile?.bio.orEmpty()) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.uploadProfileAvatar(context, uri)
    }

    LaunchedEffect(Unit) { vm.loadProfile(); vm.loadDailyStatus() }
    
    // Auto-save debounce on name & bio changes
    LaunchedEffect(name, bio) {
        if (name != profile?.displayName.orEmpty() || bio != profile?.bio.orEmpty()) {
            delay(1000)
            vm.updateProfile(name, bio)
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
                    val frameName = profile?.equippedFrame.orEmpty()
                    val hasLegendaryFrame = frameName.contains("Braise") || frameName.contains("Légendaire") || frameName.contains("Épique")
                    val frameColor = if (hasLegendaryFrame) Color(0xFFFF5722) else VioletAccent
                    val borderWidth = if (hasLegendaryFrame) 4.dp else 3.dp
                    val borderModifier = if (frameName.isNotBlank()) Modifier.border(borderWidth, frameColor, CircleShape) else Modifier
                    AsyncImage(profile?.avatarUrl, contentDescription = "Photo de profil", modifier = Modifier.size(76.dp).clip(CircleShape).then(borderModifier), contentScale = ContentScale.Crop)
                } else {
                    CharacterAvatar(name.ifBlank { "Moi" }, size = 76.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(name.ifBlank { "Votre profil" }, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                    if (!profile?.equippedTitle.isNullOrBlank()) {
                        Text("• ${profile?.equippedTitle} •", color = VioletAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
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

            // "Voir mon profil public" right below bio
            OutlinedButton(
                onClick = { onOpenPublicProfile(profile?.id ?: profile?.userId ?: 1L) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Voir mon profil public") }

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
                    Text("Regarder une publicité", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("+5 Pièces", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = onWatchAd,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                ) {
                    Text("Regarder (+5)")
                }
            }

            OutlinedButton(
                onClick = onOpenLootBoxes,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Coffres Mystères (50 Pièces)") }

            OutlinedButton(
                onClick = onOpenFriends,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Mes Amis & Affinité") }

            OutlinedButton(
                onClick = onOpenInventory,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Mon Inventaire & Équipement") }

            // Bouton Paramètres
            OutlinedButton(
                onClick = { showSettings = !showSettings },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showSettings) "Masquer les Paramètres" else "⚙️ Paramètres")
            }

            if (showSettings) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Paramètres", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NightSurfaceRaised).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Activer NSFW", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Text("Contenu mature", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = nsfw, onCheckedChange = vm::setNsfw)
                    }

                    OutlinedButton(onClick = vm::loadReferralCode, modifier = Modifier.fillMaxWidth()) { Text("Parrainage") }
                    referralCode?.let { code ->
                        Text("Ton code : $code", color = VioletAccent, style = MaterialTheme.typography.titleMedium)
                        TextField(value = referralInput, onValueChange = { referralInput = it }, label = { Text("Code reçu") }, modifier = Modifier.fillMaxWidth(), colors = profileFieldColors(), singleLine = true)
                        OutlinedButton(onClick = { vm.applyReferral(referralInput) }, enabled = referralInput.isNotBlank()) { Text("Utiliser un code") }
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
                        val transactions = creatorEarnings?.optJSONArray("recentTransactions") ?: JSONArray()

                        Column(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(NightSurfaceRaised).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Mes Revenus", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total gagné", color = TextSecondary)
                                Text("$totalEarnings Pièces", color = VioletAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
                        }
                    }

                    OutlinedButton(onClick = { showLegalDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Mentions légales")
                    }

                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Suppression du profil")
                    }

                    // Bouton Déconnexion dans les paramètres
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Se déconnecter", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Cadre de pub bannière tout en bas du profil
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Publicité", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(4.dp))
                    BannerAdView()
                }
            }

            if (!error.isNullOrBlank()) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showLegalDialog) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = false },
            containerColor = NightSurface,
            title = { Text("Mentions Légales", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("CHATILE est une application de discussion avec des personnages IA et d'histoires interactives.\n\nTous droits réservés © 2025 CHATILE.\n\nLes données personnelles sont traitées conformément aux règles de confidentialité.", color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = false }) { Text("Fermer", color = VioletAccent) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = NightSurface,
            title = { Text("Supprimer le compte", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Attention : cette action est irréversible. Toutes vos données, pièces et personnages seront supprimés.", color = TextPrimary)
                    Text("Tapez CONFIRMER ci-dessous :", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    TextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        singleLine = true,
                        colors = profileFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteConfirmText == "CONFIRMER") {
                            showDeleteDialog = false
                            vm.deleteAccount(onSuccess = onLogout)
                        }
                    },
                    enabled = deleteConfirmText == "CONFIRMER"
                ) {
                    Text("Supprimer", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler", color = TextPrimary)
                }
            }
        )
    }
}
