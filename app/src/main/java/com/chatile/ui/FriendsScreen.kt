package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatile.ApiClient
import com.chatile.ChatViewModel
import com.chatile.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onOpenProfile: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var friendsList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var friendToGift by remember { mutableStateOf<JSONObject?>(null) }
    var giftAmount by remember { mutableStateOf("50") }

    fun loadFriends() {
        scope.launch {
            runCatching { ApiClient.getFriends() }
                .onSuccess { arr ->
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
                    friendsList = list
                }
        }
    }

    LaunchedEffect(Unit) {
        loadFriends()
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            runCatching { ApiClient.searchUsers(searchQuery) }
                .onSuccess { arr ->
                    val list = mutableListOf<JSONObject>()
                    for (i in 0 until arr.length()) list.add(arr.getJSONObject(i))
                    searchResults = list
                }
        } else {
            searchResults = emptyList()
        }
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
                Text("Mes Amis & Affinité", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Rechercher un utilisateur") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VioletAccent) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VioletAccent,
                    unfocusedBorderColor = SubtleBorder,
                    focusedLabelColor = VioletAccent,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            if (searchResults.isNotEmpty()) {
                Text("Résultats de recherche", color = VioletAccent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.height(140.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(searchResults, key = { it.getLong("id") }) { user ->
                        val uId = user.getLong("id")
                        val uName = user.optString("displayName", user.optString("username", "Utilisateur"))
                        val uAvatar = user.optString("avatarUrl").takeIf { it.isNotBlank() }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = NightSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (uAvatar != null) {
                                    AsyncImage(model = uAvatar, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Box(modifier = Modifier.size(36.dp).background(NightSurfaceRaised, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(uName.take(1).uppercase(), color = TextPrimary)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(uName, color = TextPrimary, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    scope.launch {
                                        runCatching { ApiClient.addFriend(uId) }
                                            .onSuccess { loadFriends(); searchQuery = "" }
                                    }
                                }) {
                                    Icon(Icons.Outlined.PersonAdd, contentDescription = "Ajouter", tint = VioletAccent)
                                }
                            }
                        }
                    }
                }
            }

            Text("Mes Amis (${friendsList.size})", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (friendsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aucun ami pour le moment. Utilise la barre de recherche ci-dessus pour en ajouter !", color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(friendsList, key = { it.getLong("id") }) { friend ->
                        val fId = friend.getLong("id")
                        val fName = friend.optString("displayName", friend.optString("username", "Ami"))
                        val fAvatar = friend.optString("avatarUrl").takeIf { it.isNotBlank() }
                        val affinity = friend.optInt("affinityPoints", 0)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = NightSurface),
                            modifier = Modifier.fillMaxWidth().border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (fAvatar != null) {
                                    AsyncImage(model = fAvatar, contentDescription = null, modifier = Modifier.size(46.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Box(modifier = Modifier.size(46.dp).background(NightSurfaceRaised, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(fName.take(1).uppercase(), color = TextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(fName, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("❤️‍🔥 Affinité : $affinity pts", color = VioletAccent, style = MaterialTheme.typography.bodySmall)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { friendToGift = friend }) {
                                        Icon(Icons.Outlined.CardGiftcard, contentDescription = "Cadeau", tint = VioletAccent, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Cadeau")
                                    }
                                    Button(
                                        onClick = { onOpenProfile(fId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                                    ) {
                                        Text("Profil")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (friendToGift != null) {
        val friend = friendToGift!!
        val fId = friend.getLong("id")
        val fName = friend.optString("displayName", "Ami")

        AlertDialog(
            onDismissRequest = { friendToGift = null },
            containerColor = NightSurface,
            title = { Text("Envoyer un cadeau à $fName", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Offre des Pièces pour augmenter votre point d'affinité !", color = TextSecondary)
                    OutlinedTextField(
                        value = giftAmount,
                        onValueChange = { giftAmount = it },
                        label = { Text("Montant en Pièces") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = giftAmount.toIntOrNull() ?: 50
                    scope.launch {
                        runCatching { ApiClient.giftFriend(fId, amt) }
                            .onSuccess {
                                friendToGift = null
                                loadFriends()
                                vm.refresh()
                            }
                    }
                }) { Text("Envoyer", color = VioletAccent) }
            },
            dismissButton = {
                TextButton(onClick = { friendToGift = null }) { Text("Annuler", color = TextSecondary) }
            }
        )
    }
}
