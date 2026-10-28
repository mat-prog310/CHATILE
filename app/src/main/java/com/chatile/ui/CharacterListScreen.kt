package com.chatile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.Character
import com.chatile.ChatViewModel
import com.chatile.R
import com.chatile.effectiveAvatarUrl
import androidx.compose.ui.res.painterResource
import com.chatile.ui.theme.ErrorAccent
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent
import com.chatile.ui.theme.VioletDeep
import coil.compose.AsyncImage

@Composable
fun CharacterListScreen(
    vm: ChatViewModel,
    onOpenChat: (Long?) -> Unit,
    onCreateCharacter: () -> Unit,
) {
    val characters by vm.characters.collectAsState()
    val pieces by vm.pieces.collectAsState()
    val characterOfTheDay by vm.characterOfTheDay.collectAsState()
    val error by vm.error.collectAsState()
    val recentBots = remember(characters) { characters.filter { !it.lastMessageAt.isNullOrBlank() || it.messageCount > 0 || !it.isSystem } }
    val systemCharacters = remember(characters) { characters.filter { it.isSystem } }
    val motionEnabled = rememberMotionEnabled()

    LaunchedEffect(Unit) { vm.loadCharacters() }

    NightBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.logo_chatile),
                        contentDescription = "CHATILE Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "CHATILE",
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (characters.isEmpty()) "Créez votre premier compagnon" else "${characters.size} compagnons à retrouver",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(17.dp))
                        .background(NightSurfaceRaised)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(17.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .semantics { contentDescription = "Solde : $pieces Pièces" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoinOrb(size = 17.dp)
                    Spacer(Modifier.width(7.dp))
                    Text(pieces.toString(), color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                }
            }

            characterOfTheDay?.let { featured ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp)).background(NightSurface)
                        .border(1.dp, VioletAccent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        .clickable { vm.selectCharacter(featured.id); onOpenChat(featured.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✦", color = VioletAccent, style = MaterialTheme.typography.titleLarge)
                    Text("Personnage du jour · ${featured.name}", color = TextPrimary, modifier = Modifier.padding(start = 10.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ActionPill("Surprends-moi", {
                    systemCharacters.randomOrNull()?.let { vm.selectCharacter(it.id); onOpenChat(it.id) }
                }, modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                ActionPill(
                    label = "Créer un personnage",
                    onClick = onCreateCharacter,
                    emphasized = true,
                    leading = {
                        Text("+", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    },
                )
                ActionPill(
                    label = "Chat général",
                    onClick = {
                        vm.selectCharacter(null)
                        onOpenChat(null)
                    },
                )
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = error.orEmpty(),
                    color = ErrorAccent,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            if (recentBots.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Aucune conversation récente", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Découvrez des personnages dans le catalogue ou créez le vôtre pour commencer à discuter !",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (recentBots.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Conversations récentes", color = TextPrimary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                    }
                    itemsIndexed(recentBots, key = { _, character -> character.id ?: 0L }) { index, character ->
                        CharacterTile(character, index, motionEnabled, { character.id?.let { vm.deleteCharacter(it) } }, {
                            vm.selectCharacter(character.id)
                            onOpenChat(character.id ?: 0L)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterTile(
    character: Character,
    index: Int,
    motionEnabled: Boolean,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && motionEnabled) 0.97f else 1f,
        animationSpec = if (motionEnabled) spring(stiffness = 700f) else tween(0),
        label = "character-press-scale",
    )
    val transition = rememberInfiniteTransition(label = "character-breathe-$index")
    val breatheState = if (motionEnabled) {
        transition.animateFloat(
            initialValue = 0.995f,
            targetValue = 1.008f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2_600, delayMillis = (index % 2) * 260),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "character-breathe-scale-$index",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }
    val breathe by breatheState
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale * breathe
                scaleY = pressScale * breathe
            }
            .clip(shape)
            .background(Brush.linearGradient(listOf(NightSurfaceRaised, NightSurface)))
            .border(1.dp, SubtleBorder, shape)
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpen,
            )
            .semantics { contentDescription = "Ouvrir la conversation avec ${character.name}" }
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(character.effectiveAvatarUrl(), "Avatar de ${character.name}", Modifier.size(52.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (character.isNsfw) {
                    Text(
                        "18+",
                        color = VioletAccent,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(VioletDeep.copy(alpha = 0.25f))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                }
                if (!character.isSystem) IconAction(
                    symbol = "×",
                    description = "Supprimer ${character.name}",
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = character.name,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = character.greeting,
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${character.lastMood} · ${"♥".repeat((character.relationshipLevel / 20 + 1).coerceIn(1, 5))}",
                color = VioletAccent,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            "Ouvrir la conversation  →",
            color = VioletAccent,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
