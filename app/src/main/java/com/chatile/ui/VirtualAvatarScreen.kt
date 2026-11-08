package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatile.ChatViewModel
import com.chatile.R
import com.chatile.ui.theme.*

data class ClothingItem(
    val id: Long,
    val name: String,
    val drawableRes: Int
)

@Composable
fun VirtualAvatarScreen(
    vm: ChatViewModel,
    onBack: () -> Unit
) {
    var equippedClothing by remember { mutableStateOf<Int?>(R.drawable.tshirt) }

    // Adjustable alignment state for perfect layering
    var offsetY by remember { mutableStateOf(90f) }
    var offsetX by remember { mutableStateOf(0f) }
    var itemWidth by remember { mutableStateOf(150f) }
    var itemHeight by remember { mutableStateOf(200f) }

    val clothingOptions = listOf(
        ClothingItem(1, "T-shirt", R.drawable.tshirt),
        ClothingItem(2, "Costume", R.drawable.costume),
        ClothingItem(3, "Robe", R.drawable.robe),
        ClothingItem(4, "Aucun", 0)
    )

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                Text("Éditeur d'Avatar", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            // 1 & 2. The Avatar Box (Portrait 300x380)
            Box(
                modifier = Modifier
                    .size(width = 300.dp, height = 380.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NightSurface)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Base Avatar Body (Layer 0)
                AsyncImage(
                    model = R.drawable.avatar_homme,
                    contentDescription = "Avatar de base",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Superimposed Clothing Layer (Layer 1, zIndex = 1f)
                if (equippedClothing != null && equippedClothing != 0) {
                    AsyncImage(
                        model = equippedClothing,
                        contentDescription = "Vêtement équipé",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = offsetX.dp, y = offsetY.dp)
                            .size(width = itemWidth.dp, height = itemHeight.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // 4. Temporary adjustment controls for live alignment
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurfaceRaised),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Ajustement direct du vêtement", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Y: ${offsetY.toInt()}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { offsetY -= 5f }, modifier = Modifier.height(32.dp)) { Text("- Y") }
                        Button(onClick = { offsetY += 5f }, modifier = Modifier.height(32.dp)) { Text("+ Y") }

                        Spacer(Modifier.width(8.dp))
                        Text("H: ${itemHeight.toInt()}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { itemHeight -= 10f }, modifier = Modifier.height(32.dp)) { Text("- H") }
                        Button(onClick = { itemHeight += 10f }, modifier = Modifier.height(32.dp)) { Text("+ H") }
                    }
                }
            }

            // 3. Equipment Selection Row
            Text("Vêtements disponibles", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(clothingOptions, key = { it.id }) { item ->
                    val isEquipped = equippedClothing == item.drawableRes
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isEquipped) NightSurfaceRaised else NightSurface),
                        modifier = Modifier
                            .size(100.dp)
                            .clickable {
                                equippedClothing = if (item.drawableRes == 0) null else item.drawableRes
                            }
                            .border(1.dp, if (isEquipped) VioletAccent else SubtleBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (item.drawableRes != 0) {
                                AsyncImage(
                                    model = item.drawableRes,
                                    contentDescription = item.name,
                                    modifier = Modifier.size(50.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(item.name, color = TextPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
