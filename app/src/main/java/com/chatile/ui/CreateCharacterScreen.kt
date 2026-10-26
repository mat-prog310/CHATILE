package com.chatile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.chatile.ChatViewModel
import com.chatile.ui.theme.ErrorAccent
import com.chatile.ui.theme.NightSurface
import com.chatile.ui.theme.SubtleBorder
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent

@Composable
fun CreateCharacterScreen(
    vm: ChatViewModel,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("Salut !") }
    var isNsfw by remember { mutableStateOf(false) }
    var scenarioGoal by remember { mutableStateOf("") }
    var boundaries by remember { mutableStateOf("") }
    var appearance by remember { mutableStateOf("") }
    var bannedWords by remember { mutableStateOf("") }
    var scenarioSteps by remember { mutableStateOf("") }
    var seedMemory by remember { mutableStateOf(listOf("")) }
    var avatarUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var step by remember { mutableStateOf(1) }
    var isCreating by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(true) }
    val error by vm.error.collectAsState()
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { avatarUri = it }

    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) isCreating = false
    }

    NightBackdrop {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconAction("‹", "Retour aux personnages", onBack)
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text("Nouveau personnage", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                            Text("Étape $step sur 5", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

                    LinearProgressIndicator(progress = { step / 5f }, modifier = Modifier.fillMaxWidth())
                    when (step) {
                        1 -> {
                            CreationField("Nom", name, { name = it }, "Ex. Camille", 1, 1)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (avatarUri != null) AsyncImage(avatarUri, "Avatar sélectionné", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)))
                                OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text("Choisir une photo") }
                            }
                            Text("Sans photo, un avatar sera généré après la création.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        2 -> CreationField(
                            "Personnalité", prompt, { prompt = it }, "Tu es ${name.ifBlank { "…" }}, un personnage qui…", 5, 8,
                        )
                        3 -> {
                            CreationField("Premier message", greeting, { greeting = it }, "Coucou ! Ça fait plaisir de te voir", 3, 5)
                            Text("Aperçu", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                            Text(
                                greeting.ifBlank { "Salut !" }, color = TextPrimary,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurface).padding(16.dp),
                            )
                        }
                        4 -> {
                            Text("Ce que ${name.ifBlank { "ce personnage" }} sait déjà de toi. Ces informations resteront en mémoire.", color = TextSecondary)
                            seedMemory.forEachIndexed { index, fact ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CreationField("Fait ${index + 1}", fact, { value -> seedMemory = seedMemory.toMutableList().also { it[index] = value } }, "Un détail à retenir", 1, 2)
                                    if (seedMemory.size > 1) TextButton(onClick = { seedMemory = seedMemory.filterIndexed { i, _ -> i != index } }) { Text("−") }
                                }
                            }
                            OutlinedButton(onClick = { if (seedMemory.size < 15) seedMemory = seedMemory + "" }, enabled = seedMemory.size < 15) {
                                Text("+ Ajouter un fait")
                            }
                            CreationField("Apparence physique", appearance, { appearance = it }, "Cheveux, yeux, taille, style…", 2, 3)
                            CreationField("Limites", boundaries, { boundaries = it }, "Sujets ou comportements à éviter…", 2, 3)
                            CreationField("Mots interdits", bannedWords, { bannedWords = it }, "Sépare les mots par des virgules", 1, 2)
                        }
                        5 -> {
                            // For Google Play publication, move NSFW settings to the website.
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurface)
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Personnage NSFW", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                    Text("Ce réglage est enregistré avec le personnage.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = isNsfw, onCheckedChange = { isNsfw = it })
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NightSurface)
                                    .border(1.dp, SubtleBorder, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Rendre ce personnage public", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                    Text("Les autres utilisateurs pourront voir et utiliser ce personnage. Tu pourras le rendre privé à tout moment.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                            }
                            CreationField("Objectif du scénario (facultatif)", scenarioGoal, { scenarioGoal = it }, "Ex. Résoudre ensemble une énigme", 3, 5)
                            CreationField("Étapes (facultatif)", scenarioSteps, { scenarioSteps = it }, "Une étape par ligne", 3, 6)
                        }
                    }

            if (!error.isNullOrBlank()) {
                Text(error.orEmpty(), color = ErrorAccent, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(3.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 1) OutlinedButton(onClick = { step -= 1 }, modifier = Modifier.weight(1f)) { Text("Retour") }
                Button(
                    onClick = {
                        if (step < 5) step += 1
                        else {
                            isCreating = true
                            vm.createCharacter(
                                name.trim(), prompt.trim(), greeting.trim().ifBlank { "Salut !" }, isNsfw,
                                scenarioGoal.trim(), seedMemory.map(String::trim).filter(String::isNotEmpty),
                                boundaries.trim(), appearance.trim(), bannedWords.trim(),
                                scenarioSteps.lines().map(String::trim).filter(String::isNotEmpty),
                                avatarUri, context,
                            ) { id -> 
                                vm.selectCharacter(id)
                                if (isPublic) {
                                    vm.publishCharacter(id) { onCreated(id) }
                                } else {
                                    onCreated(id)
                                }
                            }
                        }
                    },
                    enabled = !isCreating && (step != 1 || name.isNotBlank()) && (step != 2 || prompt.isNotBlank()),
                    modifier = Modifier.weight(1f),
                ) { Text(if (isCreating) "Création…" else if (step == 5) "Créer le personnage" else "Continuer") }
            }
        }
    }
}

@Composable
private fun CreationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int,
    maxLines: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush = SolidColor(VioletAccent),
            minLines = minLines,
            maxLines = maxLines,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(NightSurface)
                        .border(1.dp, SubtleBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    innerTextField()
                }
            },
        )
    }
}
