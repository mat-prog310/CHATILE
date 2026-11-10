package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chatile.ChatViewModel
import com.chatile.PublicCharacter
import com.chatile.ui.theme.BadgeIcon

@Composable
fun CharacterDetailScreen(
    characterId: Long,
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit,
    viewModel: ChatViewModel = viewModel(),
) {
    val character by viewModel.publicCharacterDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(characterId) { viewModel.loadCharacterDetail(characterId) }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Signaler ce personnage ?") },
            text = { Text("Le signalement sera transmis à la modération.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.reportContent("character", characterId, "Contenu inapproprié") {
                        showReportDialog = false
                    }
                }) { Text("Signaler") }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Annuler") } },
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Signaler") },
                            onClick = { menuExpanded = false; showReportDialog = true },
                        )
                    }
                }
            }
        },
        bottomBar = {
            character?.let { current ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { viewModel.importCharacter(current.id, onOpenChat) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Utiliser ce personnage") }
                    OutlinedButton(onClick = { viewModel.likeCharacter(current.id) }) {
                        Icon(
                            if (current.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Liker",
                            tint = if (current.isLikedByCurrentUser) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("${current.likesCount}")
                    }
                }
            }
        },
    ) { padding ->
        when {
            character == null && isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            character == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Ce personnage n’est plus disponible.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> CharacterDetailContent(character!!, Modifier.padding(padding))
        }
    }
}

@Composable
private fun CharacterDetailContent(character: PublicCharacter, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (!character.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = character.avatarUrl,
                contentDescription = "Avatar de ${character.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
            )
        } else {
            Box(
                Modifier.fillMaxWidth().height(200.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(character.name.take(1).uppercase(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(character.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Par ${character.creatorName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                character.creatorBadge?.let {
                    Spacer(Modifier.size(6.dp))
                    BadgeIcon(it)
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(16.dp),
            ) {
                Text("Premier message", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(character.greeting, style = MaterialTheme.typography.bodyLarge)
            }
            character.description?.let { description ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("À propos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            character.scenarioGoal?.let { goal ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Scénario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(goal, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Créé le", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(character.createdAt.substringBefore('T'), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
