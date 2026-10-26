package com.chatile.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding as navBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.chatile.ChatViewModel
import com.chatile.Msg
import com.chatile.ui.theme.ErrorAccent
import com.chatile.ui.theme.NightBackground
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.PinkAccent
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent
import com.chatile.ui.theme.VioletDeep
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun ChatScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onBuy: (String) -> Unit,
    onWatchAd: () -> Unit,
    onShowInterstitial: () -> Unit,
) {
    val messages by vm.messages.collectAsState()
    val pieces by vm.pieces.collectAsState()
    val characters by vm.characters.collectAsState()
    val activeCharacterId by vm.activeCharacterId.collectAsState()
    val subscriptionTier by vm.subscriptionTier.collectAsState()
    val shouldShowInterstitial by vm.showInterstitial.collectAsState()
    val error by vm.error.collectAsState()
    val motionEnabled = rememberMotionEnabled()
    val characterName = characters.firstOrNull { it.id == activeCharacterId }?.name ?: "Assistant IA"
    var draft by remember { mutableStateOf("") }
    var isReplying by remember { mutableStateOf(false) }
    var previousPieces by remember { mutableIntStateOf(pieces) }
    var coinFlying by remember { mutableStateOf(false) }
    var usePremium by remember { mutableStateOf(false) }
    var showMemory by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coinProgress = remember { Animatable(0f) }
    val inputShake = remember { Animatable(0f) }
    val listState = rememberLazyListState()

    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            isReplying = false
            if (motionEnabled) {
                inputShake.snapTo(4f)
                inputShake.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 280
                        -9f at 45
                        8f at 90
                        -6f at 140
                        5f at 190
                        0f at 280
                    },
                )
            }
            delay(3_000)
            vm.clearError()
        }
    }

    LaunchedEffect(shouldShowInterstitial) {
        if (shouldShowInterstitial && subscriptionTier == "free") {
            vm.appendSystemMessage("Une petite pub pour continuer à profiter de CHATILE gratuitement. Merci ! 🙏")
            onShowInterstitial()
            vm.dismissInterstitial()
        }
    }

    LaunchedEffect(messages.size, isReplying) {
        if (messages.lastOrNull()?.isUser == false) isReplying = false
        val showTyping = isReplying && messages.lastOrNull()?.isUser == true
        val lastIndex = messages.size + if (showTyping) 0 else -1
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    LaunchedEffect(pieces, motionEnabled) {
        if (motionEnabled && previousPieces > 0 && pieces < previousPieces) {
            coinProgress.snapTo(0f)
            coinFlying = true
            coinProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing),
            )
            coinFlying = false
        }
        previousPieces = pieces
    }

    fun sendDraft() {
        val content = draft.trim()
        if (content.isBlank()) return
        isReplying = true
        vm.send(content, usePremium)
        draft = ""
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        NightBackdrop {
            Column(modifier = Modifier.fillMaxSize()) {
                ChatHeader(
                    name = characterName,
                    mood = characters.firstOrNull { it.id == activeCharacterId }?.lastMood ?: "neutral",
                    avatarUrl = characters.firstOrNull { it.id == activeCharacterId }?.avatarUrl,
                    pieces = pieces,
                    isPremium = subscriptionTier != "free",
                    onBack = onBack,
                    onBuy = { onBuy("pack_500") },
                    onWatchAd = onWatchAd,
                    onMemory = {
                        activeCharacterId?.let { vm.loadMemory(it); showMemory = true }
                    },
                    onShare = {
                        activeCharacterId?.let { id ->
                            vm.shareCharacter(id) { link ->
                                val sendIntent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link)
                                context.startActivity(Intent.createChooser(sendIntent, "Partager le personnage"))
                            }
                        }
                    },
                )

                if (!error.isNullOrBlank()) {
                    Text(
                        text = error.orEmpty(),
                        color = ErrorAccent,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 7.dp)
                            .semantics { contentDescription = "Erreur : ${error.orEmpty()}" },
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    itemsIndexed(messages, key = { index, message -> "$index-${message.isUser}-${message.text.hashCode()}" }) { _, message ->
                        ChatMessage(message, motionEnabled)
                    }
                    if (isReplying && messages.lastOrNull()?.isUser == true) {
                        item(key = "typing-indicator") { TypingIndicator(motionEnabled) }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (subscriptionTier != "free") "Modèle premium (1 Pièce)" else "Modèle premium (3 Pièces)",
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = usePremium, onCheckedChange = { usePremium = it })
                }
                ChatComposer(
                    draft = draft,
                    onDraftChange = { draft = it },
                    onSend = ::sendDraft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navBarsPadding()
                        .imePadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        .graphicsLayer { translationX = inputShake.value },
                )
                }
            }
        }

        if (coinFlying) {
            CoinOrb(
                modifier = Modifier
                    .offset(
                        x = maxWidth * 0.66f * (1f - coinProgress.value) + (maxWidth - 42.dp) * coinProgress.value,
                        y = (maxHeight - 190.dp) * (1f - coinProgress.value) + 42.dp * coinProgress.value,
                    )
                    .graphicsLayer {
                        scaleX = 0.82f + coinProgress.value * 0.28f
                        scaleY = 0.82f + coinProgress.value * 0.28f
                        alpha = 1f - coinProgress.value * 0.2f
                    },
                size = 20.dp,
                description = "Pièce débitée",
            )
        }
    }
    if (showMemory && activeCharacterId != null) {
        MemorySheet(characterName, activeCharacterId!!, vm) { showMemory = false }
    }
}

@Composable
private fun ChatHeader(
    name: String,
    mood: String,
    avatarUrl: String?,
    pieces: Int,
    isPremium: Boolean,
    onBack: () -> Unit,
    onBuy: () -> Unit,
    onWatchAd: () -> Unit,
    onMemory: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NightBackground.copy(alpha = 0.72f))
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconAction("‹", "Retour aux personnages", onBack)
            Spacer(Modifier.width(11.dp))
            if (avatarUrl != null) AsyncImage(avatarUrl, "Avatar de $name", Modifier.size(42.dp).clip(CircleShape))
            else CharacterAvatar(name = name, size = 42.dp)
            Column(
                modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(PinkAccent, CircleShape)
                            .semantics { contentDescription = "En ligne" },
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (name == "Assistant IA") "Disponible" else mood,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(NightSurfaceRaised)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp)
                    .semantics { contentDescription = "Solde : $pieces Pièces" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinOrb(size = 16.dp)
                Spacer(Modifier.width(6.dp))
                Text(text = pieces.toString(), color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (!isPremium) {
                ActionPill(
                    label = "+500 · 2 €",
                    onClick = onBuy,
                    emphasized = true,
                    leading = { CoinOrb(size = 15.dp) },
                )
                ActionPill(
                    label = "Pub · +5",
                    onClick = onWatchAd,
                    leading = {
                        Text("▶", color = VioletAccent, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clearAndSetSemantics { })
                    },
                )
            }
            if (name != "Assistant IA") {
                IconAction("🧠", "Ouvrir la mémoire", onMemory)
                IconAction("↗", "Partager le personnage", onShare)
            }
        }
    }
}

@Composable
private fun ChatMessage(message: Msg, motionEnabled: Boolean) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(if (motionEnabled) 150 else 0)) + slideInVertically(
            animationSpec = tween(if (motionEnabled) 150 else 0),
            initialOffsetY = { it / 5 },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!message.isUser) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp, bottom = 5.dp)
                        .size(24.dp)
                        .background(VioletDeep.copy(alpha = 0.46f), CircleShape)
                        .semantics { contentDescription = "Message de l’assistant" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✦", color = VioletAccent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clearAndSetSemantics { })
                }
            }
            Text(
                text = message.text,
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .then(
                        if (message.isUser) {
                            Modifier
                                .background(Brush.linearGradient(listOf(VioletDeep, VioletAccent)))
                                .background(NightBackground.copy(alpha = 0.4f))
                        } else {
                            Modifier.background(NightSurfaceRaised)
                                .border(1.dp, SubtleBorder, RoundedCornerShape(22.dp))
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TypingIndicator(motionEnabled: Boolean) {
    val first: Float
    val second: Float
    val third: Float
    if (motionEnabled) {
        val transition = rememberInfiniteTransition(label = "typing")
        val animatedFirst by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(460), RepeatMode.Reverse),
            label = "typing-dot-1",
        )
        val animatedSecond by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(460, delayMillis = 140), RepeatMode.Reverse),
            label = "typing-dot-2",
        )
        val animatedThird by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(460, delayMillis = 280), RepeatMode.Reverse),
            label = "typing-dot-3",
        )
        first = animatedFirst
        second = animatedSecond
        third = animatedThird
    } else {
        first = 0.72f
        second = 0.72f
        third = 0.72f
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NightSurfaceRaised)
            .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 15.dp, vertical = 12.dp)
            .semantics { contentDescription = "L’assistant écrit" },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(first, second, third).forEach { alpha ->
            Box(modifier = Modifier.size(7.dp).graphicsLayer { this.alpha = alpha }.background(VioletAccent, CircleShape))
        }
    }
}

@Composable
private fun ChatComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(NightSurface)
            .border(1.dp, SubtleBorder, RoundedCornerShape(24.dp))
            .padding(start = 17.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Message à envoyer" },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(VioletAccent),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            decorationBox = { innerTextField ->
                Box {
                    if (draft.isEmpty()) {
                        Text("Écrire un message…", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    innerTextField()
                }
            },
        )
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (draft.isBlank()) NightSurfaceRaised else VioletAccent)
                .border(1.dp, if (draft.isBlank()) SubtleBorder else VioletAccent, CircleShape)
                .clickable(
                    enabled = draft.isNotBlank(),
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onSend,
                )
                .semantics { contentDescription = "Envoyer le message" },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "↑",
                color = if (draft.isBlank()) TextSecondary else NightBackground,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}
