package com.chatile.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatile.ChatViewModel
import com.chatile.ui.theme.ChatileScreenBackground
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayStoryScreen(
    storyId: Long,
    vm: ChatViewModel,
    onBack: () -> Unit,
    onShowRewardedAd: (onRewarded: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val currentNode by vm.activeStoryNode.collectAsState()
    val discoveredEndings by vm.discoveredEndings.collectAsState()
    val totalEndings by vm.totalEndings.collectAsState()
    val subscriptionTier by vm.subscriptionTier.collectAsState()
    val pieces by vm.pieces.collectAsState()
    val isPremium = subscriptionTier != "free"

    var showReplayModal by remember { mutableStateOf(false) }

    LaunchedEffect(storyId) {
        vm.playStory(
            storyId = storyId,
            onReplayRequired = { showReplayModal = true }
        )
    }

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Histoire interactive", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Fins découvertes : $discoveredEndings / $totalEndings", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Row(
                    modifier = Modifier
                        .background(NightSurfaceRaised, RoundedCornerShape(16.dp))
                        .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoinOrb(size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text("$pieces", color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }

            // Scene Content
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (currentNode?.isEnding == true) {
                        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(48.dp))
                        Text("Fin Atteinte !", color = VioletAccent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = currentNode?.content ?: "Chargement de la scène...",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = if (currentNode?.isEnding == true) TextAlign.Center else TextAlign.Start
                    )
                }
            }

            // Choices or Replay
            if (currentNode?.isEnding == true) {
                Button(
                    onClick = {
                        if (isPremium) {
                            vm.replayStory(storyId, "premium")
                        } else {
                            showReplayModal = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isPremium) "Rejouer l'histoire (Gratuit Premium)" else "Rejouer l'histoire")
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentNode?.choices?.forEach { choice ->
                        OutlinedButton(
                            onClick = { vm.makeStoryChoice(choice.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(choice.text, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }

    if (showReplayModal) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showReplayModal = false },
            sheetState = sheetState,
            containerColor = NightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rejouer cette Histoire", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Tu as déjà terminé cette histoire. Choisis une méthode pour débloquer un nouveau rejeu et découvrir les autres fins !",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = {
                        onShowRewardedAd {
                            showReplayModal = false
                            vm.replayStory(storyId, "ad")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Regarder une pub pour rejouer")
                }

                OutlinedButton(
                    onClick = {
                        if (pieces >= 100) {
                            showReplayModal = false
                            vm.replayStory(storyId, "pieces")
                        } else {
                            Toast.makeText(context, "Solde insuffisant (100 Pièces requises)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CoinOrb(size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Rejouer avec 100 Pièces (30% au créateur)")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
