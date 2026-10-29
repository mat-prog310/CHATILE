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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatile.ChatViewModel
import com.chatile.ui.theme.ChatileScreenBackground
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent
import org.json.JSONArray

@Composable
fun CreatorEarningsScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onOpenLeaderboard: () -> Unit,
) {
    val earnings by vm.creatorEarnings.collectAsState()

    LaunchedEffect(Unit) {
        vm.loadCreatorEarnings()
    }

    val totalEarnings = earnings?.optInt("totalEarnings", 0) ?: 0
    val characterEarnings = earnings?.optInt("characterEarnings", 0) ?: 0
    val storyEarnings = earnings?.optInt("storyEarnings", 0) ?: 0
    val transactions = earnings?.optJSONArray("recentTransactions") ?: JSONArray()

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = TextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Mes Revenus Créateur",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenLeaderboard) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = "Classement",
                        tint = VioletAccent
                    )
                }
            }

            // Summary Cards
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurfaceRaised),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = VioletAccent,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Total gagné", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinOrb(size = 18.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "$totalEarnings Pièces",
                                color = TextPrimary,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Personnages", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinOrb(size = 14.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("+$characterEarnings", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Histoires", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinOrb(size = 14.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("+$storyEarnings", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenLeaderboard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Voir le classement des créateurs")
            }

            Text("Historique des transactions", color = TextPrimary, style = MaterialTheme.typography.titleLarge)

            if (transactions.length() == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NightSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucune transaction pour le moment", color = TextSecondary)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0 until transactions.length()) {
                        val item = transactions.optJSONObject(i) ?: continue
                        val buyerName = item.optString("buyerName", "Utilisateur")
                        val buyerAvatarUrl = item.optString("buyerAvatarUrl").ifBlank { null }
                        val amount = item.optInt("amount", 0)
                        val description = item.optString("description", "")
                        val date = item.optString("createdAt", "").take(10)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = NightSurface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!buyerAvatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = buyerAvatarUrl,
                                        contentDescription = buyerName,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(NightSurfaceRaised, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Person, contentDescription = null, tint = TextSecondary)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(description, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Par $buyerName · $date", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CoinOrb(size = 14.dp)
                                    Spacer(Modifier.width(4.dp))
                                    Text("+$amount", color = VioletAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
