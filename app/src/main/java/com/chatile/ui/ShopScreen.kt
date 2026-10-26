package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.ChatViewModel
import com.chatile.ShopItem
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.PinkAccent
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent

@Composable
fun ShopScreen(vm: ChatViewModel, onPurchase: (String) -> Unit) {
    val items by vm.shopItems.collectAsState()
    val tier by vm.subscriptionTier.collectAsState()
    val isLoading by vm.shopLoading.collectAsState()
    val error by vm.shopError.collectAsState()
    LaunchedEffect(Unit) { vm.loadShop() }

    NightBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Boutique", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
            Text("Abonnez-vous ou achetez des packs pour débloquer toutes les fonctionnalités.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

            when {
                isLoading && items.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Chargement de la boutique…", color = TextSecondary)
                    }
                }
                error != null && items.isEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(error ?: "Boutique indisponible.", color = MaterialTheme.colorScheme.error)
                        Button(onClick = vm::loadShop) { Text("Réessayer") }
                    }
                }
                else -> {
                    items.forEach { item -> ShopProduct(item, tier, onPurchase) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShopProduct(item: ShopItem, currentTier: String, onPurchase: (String) -> Unit) {
    val isSubscription = item.type == "subscription"
    val isPremiumPlusProduct = item.productId == "sub_premium_plus_monthly"
    val isPremiumProduct = item.productId == "sub_premium_monthly"
    
    val isCurrentTier = when {
        isPremiumPlusProduct -> currentTier == "premium_plus"
        isPremiumProduct -> currentTier == "premium" || currentTier == "premium_plus"
        else -> false
    }

    val cardBackground = when {
        isPremiumPlusProduct -> NightSurfaceRaised
        isSubscription -> NightSurface
        else -> NightSurface
    }

    val borderColor = when {
        isPremiumPlusProduct -> Color(0xFFFFD700)
        isPremiumProduct -> VioletAccent
        else -> SubtleBorder
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isSubscription) {
                    Text(
                        text = if (isPremiumPlusProduct) "L'expérience ultime" else "L'abonnement recommandé",
                        color = if (isPremiumPlusProduct) Color(0xFFFFD700) else VioletAccent,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(
                text = when {
                    isPremiumPlusProduct -> "14,99 €/mois"
                    isPremiumProduct -> "7,99 €/mois"
                    else -> item.priceLabel.ifBlank { "${item.price} Pièces" }
                },
                color = if (isPremiumPlusProduct) Color(0xFFFFD700) else VioletAccent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val perks = when {
            isPremiumPlusProduct -> listOf(
                "✦ 7 000 Pièces/mois créditées automatiquement",
                "✦ Tout Premium inclus",
                "✦ Mémoire structurée illimitée (Identité, Émotions)",
                "✦ Appels vocaux (60 min/jour)",
                "✦ Badge Couronne Dorée (premium_plus)",
                "✦ 6 thèmes exclusifs (rubis, obsidienne, ivoire...)",
                "✦ Export .html interactif + 3 boosts/semaine"
            )
            isPremiumProduct -> listOf(
                "✦ 4 000 Pièces/mois créditées automatiquement",
                "✦ Message premium à 1 Pièce (au lieu de 3)",
                "✦ Création de personnages illimitée",
                "✦ Zéro publicité (interstitielle & récompensée)",
                "✦ Badge Diamant Violet (premium)",
                "✦ 3 thèmes exclusifs (or, émeraude, améthyste)",
                "✦ Export .json et .pdf"
            )
            else -> listOf(item.description.ifBlank { "${item.price} Pièces créditées immédiatement sur votre compte." })
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            perks.forEach { perk ->
                Text(
                    text = perk,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Button(
            onClick = { onPurchase(item.productId) },
            enabled = !isCurrentTier,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPremiumPlusProduct) Color(0xFFFFD700) else VioletAccent,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = when {
                    isCurrentTier -> "Abonnement actif"
                    isSubscription -> "S'abonner (${item.priceLabel.ifBlank { if (isPremiumPlusProduct) "14,99 €" else "7,99 €" }})"
                    else -> "Acheter"
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}
