package com.chatile.ui

import androidx.compose.ui.graphics.Color

data class VirtualItem(
    val id: Long,
    val name: String,
    val category: String, // 'house' | 'clothing' | 'vehicle' | 'pet' | 'furniture'
    val price: Int,
    val rarity: String = "commun" // 'commun' | 'rare' | 'epique' | 'legendaire'
)

data class VirtualAvatarModel(
    val name: String,
    val skinColor: Color = Color(0xFFFFD1A9),
    val hairColor: Color = Color(0xFF5D4037),
    val equippedItems: List<VirtualItem> = emptyList()
)

object MockVirtualData {
    val shopItems = listOf(
        VirtualItem(1, "Studio Cosy", "house", 500, "commun"),
        VirtualItem(2, "Villa de Luxe", "house", 10000, "epique"),
        VirtualItem(3, "T-shirt Tye-Die", "clothing", 50, "commun"),
        VirtualItem(4, "Costume Sur-Mesure", "clothing", 250, "rare"),
        VirtualItem(5, "Voiture Sport", "vehicle", 5000, "epique"),
        VirtualItem(6, "Scooter Urbain", "vehicle", 500, "commun"),
        VirtualItem(7, "Chat Joueur", "pet", 300, "commun"),
        VirtualItem(8, "Dragon de Feu", "pet", 15000, "legendaire"),
        VirtualItem(9, "Canapé Gamer RGB", "furniture", 800, "rare")
    )
}
