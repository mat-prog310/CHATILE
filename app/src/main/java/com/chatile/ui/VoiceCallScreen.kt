package com.chatile.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.chatile.ApiClient
import com.chatile.ChatViewModel
import com.chatile.effectiveAvatarUrl
import com.chatile.ui.theme.ChatileScreenBackground
import com.chatile.ui.theme.ErrorAccent
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.NightSurfaceRaised
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceCallScreen(
    characterId: Long,
    vm: ChatViewModel,
    onBack: () -> Unit,
) {
    val characters by vm.characters.collectAsState()
    val character = characters.firstOrNull { it.id == characterId }
    val characterName = character?.name ?: "Compagnon IA"
    val avatarUrl = character?.effectiveAvatarUrl()

    var callId by remember { mutableStateOf<Long?>(null) }
    var durationSeconds by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(1800) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Connexion...") }
    var lastTranscript by remember { mutableStateOf(character?.greeting ?: "Bonjour !") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val micInteractionSource = remember { MutableInteractionSource() }
    val isMicPressed by micInteractionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSpeaking) 1.15f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar-pulse"
    )

    // Start call on server
    LaunchedEffect(characterId) {
        runCatching { ApiClient.startVoiceCall(characterId) }
            .onSuccess { res ->
                callId = res.optLong("callId")
                remainingSeconds = res.optInt("remainingSeconds", 1800)
                statusText = "En ligne"
            }
            .onFailure { err ->
                errorMessage = err.message ?: "Impossible de démarrer l'appel."
                statusText = "Erreur"
            }
    }

    // Call timer
    LaunchedEffect(callId) {
        if (callId != null) {
            while (true) {
                delay(1000)
                durationSeconds += 1
                if (remainingSeconds > 0) remainingSeconds -= 1
            }
        }
    }

    // Handle mic press to speak
    LaunchedEffect(isMicPressed) {
        if (isMicPressed && !isMuted) {
            statusText = "Écoute en cours..."
        } else if (!isMicPressed && statusText == "Écoute en cours...") {
            statusText = "Traitement de votre message..."
            scope.launch {
                val sampleUserSpeech = "Bonjour ${characterName}, comment vas-tu aujourd'hui ?"
                runCatching { ApiClient.transcribeVoice(sampleUserSpeech) }
                    .onSuccess { transcribed ->
                        val userText = transcribed.optString("text", sampleUserSpeech)
                        lastTranscript = "Vous : $userText"
                        statusText = "$characterName réfléchit..."
                        
                        vm.send(userText, usePremium = false)
                        delay(1200)
                        
                        val lastMsg = vm.messages.value.lastOrNull { !it.isUser }?.text ?: "Je vais très bien !"
                        lastTranscript = "$characterName : $lastMsg"
                        isSpeaking = true
                        statusText = "$characterName parle..."

                        runCatching { ApiClient.speakVoice(lastMsg, characterId) }
                        
                        delay(3500)
                        isSpeaking = false
                        statusText = "En ligne"
                    }
                    .onFailure {
                        statusText = "En ligne"
                    }
            }
        }
    }

    // End call on leave
    DisposableEffect(Unit) {
        onDispose {
            callId?.let { id ->
                scope.launch {
                    runCatching { ApiClient.endVoiceCall(id, durationSeconds) }
                }
            }
        }
    }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    ChatileScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = characterName,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusText,
                    color = if (isSpeaking) VioletAccent else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Durée : $formattedDuration · Quota restant : ${remainingSeconds / 60} min",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                errorMessage?.let {
                    Text(it, color = ErrorAccent, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Avatar with pulse
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(CircleShape)
                        .background(VioletAccent.copy(alpha = if (isSpeaking) 0.35f else 0.12f))
                )

                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = characterName,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .border(2.dp, VioletAccent, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(NightSurfaceRaised)
                            .border(2.dp, VioletAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = characterName.take(1),
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
            }

            // Transcript Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp))
            ) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lastTranscript,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(NightSurfaceRaised)
                        .border(1.dp, SubtleBorder, CircleShape)
                        .clickable { isMuted = !isMuted },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Outlined.MicOff else Icons.Outlined.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) ErrorAccent else TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Hold to speak mic
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (isMicPressed) VioletAccent else NightSurfaceRaised)
                        .border(1.dp, VioletAccent, RoundedCornerShape(28.dp))
                        .clickable(
                            interactionSource = micInteractionSource,
                            indication = null,
                            onClick = {}
                        )
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isMicPressed) "Relâchez pour envoyer" else "Maintenir pour parler",
                        color = if (isMicPressed) Color.White else TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // End call
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ErrorAccent)
                        .clickable {
                            callId?.let { id ->
                                scope.launch { ApiClient.endVoiceCall(id, durationSeconds) }
                            }
                            onBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CallEnd,
                        contentDescription = "Raccrocher",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
