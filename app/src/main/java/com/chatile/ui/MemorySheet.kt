package com.chatile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatile.ChatViewModel
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorySheet(characterName: String, characterId: Long, vm: ChatViewModel, onDismiss: () -> Unit) {
    val memory by vm.memory.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Mémoire de $characterName", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                Text("Voici ce que $characterName se rappelle de toi. Tu peux oublier un détail à tout moment.", color = TextSecondary)
            }
            if (memory.seedMemory.isNotEmpty()) {
                item { Text("À la création", color = TextPrimary, style = MaterialTheme.typography.titleMedium) }
                itemsIndexed(memory.seedMemory) { index, fact ->
                    MemoryFact(fact) { vm.deleteMemory(characterId, "seed", index) }
                }
            }
            if (memory.learnedMemory.isNotEmpty()) {
                item { Text("Appris au fil des conversations", color = TextPrimary, style = MaterialTheme.typography.titleMedium) }
                itemsIndexed(memory.learnedMemory) { index, fact ->
                    MemoryFact(fact) { vm.deleteMemory(characterId, "learned", index) }
                }
            }
            if (memory.seedMemory.isEmpty() && memory.learnedMemory.isEmpty()) {
                item { Text("Aucun souvenir pour le moment.", color = TextSecondary) }
            }
            item { HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable
private fun MemoryFact(fact: String, onForget: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(fact, color = TextSecondary, modifier = Modifier.weight(1f))
        TextButton(onClick = onForget) { Text("Oublier") }
    }
}