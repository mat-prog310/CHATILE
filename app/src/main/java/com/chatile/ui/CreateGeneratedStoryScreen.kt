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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatile.ChatViewModel
import com.chatile.ui.theme.ChatileScreenBackground
import com.chatile.ui.theme.ErrorAccent
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent

@Composable
fun CreateGeneratedStoryScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onStoryGenerated: (Long) -> Unit,
) {
    var prompt by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Aventure") }
    val genres = listOf("Aventure", "Sci-Fi", "Horreur", "Enquête", "Romance")
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextPrimary)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = VioletAccent, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text("Génération IA d'Histoire", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }

            Text(
                "Décris l'idée de ton histoire. L'IA Mistral générera un arbre d'aventure complet avec choix multiples et au moins 4 fins différentes.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )

            Text("Genre", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = { selectedGenre = genre },
                        label = { Text(genre) }
                    )
                }
            }

            Text("Thème ou Scénario de départ", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = prompt,
                onValueChange = { if (it.length <= 400) prompt = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(VioletAccent),
                minLines = 4,
                maxLines = 6,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NightSurface)
                            .border(1.dp, SubtleBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        if (prompt.isEmpty()) {
                            Text("Ex. Un explorateur coincé dans un temple maya abandonné truffé de pièges...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        innerTextField()
                    }
                }
            )

            error?.let {
                Text(it, color = ErrorAccent, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = VioletAccent)
                        Text("Génération de l'arbre d'histoire par l'IA...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            vm.generateStory(prompt.trim(), selectedGenre) { storyId ->
                                onStoryGenerated(storyId)
                            }
                        }
                    },
                    enabled = prompt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Générer l'histoire")
                }
            }
        }
    }
}
