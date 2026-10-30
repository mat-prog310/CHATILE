package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatile.ApiClient
import com.chatile.ChatViewModel
import com.chatile.ui.theme.BadgeIcon
import com.chatile.ui.theme.ChatileScreenBackground
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PublicProfileScreen(
    userId: Long,
    vm: ChatViewModel,
    onBack: () -> Unit,
) {
    var profileData by remember { mutableStateOf<JSONObject?>(null) }

    LaunchedEffect(userId) {
        runCatching { ApiClient.getPublicProfile(userId) }
            .onSuccess { profileData = it }
    }

    val user = profileData?.optJSONObject("user")
    val displayName = user?.optString("displayName", "Utilisateur") ?: "Utilisateur"
    val avatarUrl = user?.optString("avatarUrl")?.takeIf { it.isNotBlank() }
    val badgeType = user?.optString("badgeType")?.takeIf { it.isNotBlank() }
    val totalEarnings = profileData?.optInt("totalEarnings", 0) ?: 0
    val equippedItems = profileData?.optJSONArray("equippedItems")

    var equippedTitle = ""
    var equippedFrame = ""
    var equippedEffect = ""
    var equippedTheme = ""
    val cosmeticList = mutableListOf<String>()

    if (equippedItems != null) {
        for (i in 0 until equippedItems.length()) {
            val item = equippedItems.optJSONObject(i) ?: continue
            val type = item.optString("type")
            val name = item.optString("name")
            if (type == "title") equippedTitle = name
            if (type == "frame") equippedFrame = name
            if (type == "avatar_effect") equippedEffect = name
            if (type == "theme") equippedTheme = name
            cosmeticList.add("$name (${type.replace("_", " ")})")
        }
    }

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Text("Profil Public", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            // Avatar & Name
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = displayName,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.dp, if (equippedFrame.contains("Braise")) androidx.compose.ui.graphics.Color.Red else VioletAccent, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(NightSurfaceRaised, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (!badgeType.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    BadgeIcon(badgeType)
                }
            }

            if (equippedTitle.isNotBlank()) {
                Text("• $equippedTitle •", color = VioletAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }

            // Stats Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalEarnings", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Pièces gagnées", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (cosmeticList.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Cosmétiques Équipés", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        cosmeticList.forEach { cosmetic ->
                            Text("• $cosmetic", color = VioletAccent, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Public Characters
            val charactersArray = profileData?.optJSONArray("characters")
            if (charactersArray != null && charactersArray.length() > 0) {
                Text(
                    text = "Personnages Créés",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                for (i in 0 until charactersArray.length()) {
                    val charObj = charactersArray.optJSONObject(i) ?: continue
                    val charName = charObj.optString("name", "Inconnu")
                    val charAvatar = charObj.optString("avatar_url").takeIf { it.isNotBlank() } ?: "https://api.dicebear.com/7.x/bottts/png?seed=${charObj.optInt("id")}"
                    val greeting = charObj.optString("greeting")
                    
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NightSurfaceRaised),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = charAvatar,
                                contentDescription = charName,
                                modifier = Modifier.size(50.dp).clip(CircleShape).background(NightSurface),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(charName, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(greeting, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Public Stories
            val storiesArray = profileData?.optJSONArray("stories")
            if (storiesArray != null && storiesArray.length() > 0) {
                Text(
                    text = "Histoires Créées",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                for (i in 0 until storiesArray.length()) {
                    val storyObj = storiesArray.optJSONObject(i) ?: continue
                    val title = storyObj.optString("title", "Inconnu")
                    val genre = storyObj.optString("genre", "Aventure")
                    
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = NightSurfaceRaised),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(genre, color = VioletAccent, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}
