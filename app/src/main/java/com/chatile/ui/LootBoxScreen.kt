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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.chatile.ApiClient
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun LootBoxScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onOpenOpening: () -> Unit,
) {
    val pieces by vm.pieces.collectAsState()
    var showDropRates by remember { mutableStateOf(false) }
    var boxesOpened by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        runCatching { ApiClient.getLootPity() }
            .onSuccess { res ->
                boxesOpened = res.optInt("boxesOpenedSinceLegendary", 0)
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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text("Coffres Mystères", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .background(NightSurfaceRaised, RoundedCornerShape(16.dp))
                        .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoinOrb(size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("$pieces", color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Chest Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, VioletAccent.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(NightSurfaceRaised),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(64.dp))
                    }

                    Text("Coffre Mystère Légendaire", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Contient des titres exclusifs, cadres d'avatar, effets et thèmes de profil uniques.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoinOrb(size = 20.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("300 Pièces", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onOpenOpening,
                        enabled = pieces >= 300,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ouvrir un coffre (300 Pièces)")
                    }
                }
            }

            // Pity Timer & Drop rates
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Garantie Légendaire (Pity)", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("$boxesOpened / 500", color = VioletAccent, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { boxesOpened.toFloat() / 500f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = VioletAccent,
                    )
                    Text("Un objet légendaire est garanti tous les 500 coffres ouverts.", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }

            OutlinedButton(
                onClick = { showDropRates = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Voir les taux de drop")
            }
        }
    }

    if (showDropRates) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showDropRates = false },
            sheetState = sheetState,
            containerColor = NightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Taux de Drop des Coffres", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Commun", color = TextSecondary)
                    Text("60.0%", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rare", color = TextSecondary)
                    Text("30.0%", color = Color(0xFF4A90E2), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Épique", color = TextSecondary)
                    Text("9.9%", color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Légendaire", color = TextSecondary)
                    Text("0.1%", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
