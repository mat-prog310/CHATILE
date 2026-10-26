package com.chatile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatile.ChatViewModel
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary

private val tutorialSteps = listOf(
    "Une conversation à la fois" to "Choisis un personnage et démarre la conversation. Chaque message standard coûte une Pièce.",
    "Une mémoire que tu contrôles" to "Les souvenirs initiaux et appris restent visibles. Tu peux les modifier ou les oublier.",
    "Des Pièces chaque jour" to "Récupère ta récompense quotidienne et entretiens ta série de connexion.",
    "Des personnages à découvrir" to "Explore les personnages système, puis crée le tien quand tu es prêt.",
    "À toi de créer" to "Donne une voix, une mémoire et un objectif à ton personnage.",
)

@Composable
fun OnboardingScreen(vm: ChatViewModel, onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val (title, description) = tutorialSteps[step]
    NightBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text("CHATILE", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(26.dp))
            Text("${step + 1} / ${tutorialSteps.size}", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(title, color = TextPrimary, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 10.dp))
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(30.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.skipTutorial(onFinish) }, modifier = Modifier.weight(1f)) { Text("Passer") }
                Button(
                    onClick = {
                        vm.completeTutorialStep(step + 1) {
                            if (step == tutorialSteps.lastIndex) onFinish() else step += 1
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(if (step == tutorialSteps.lastIndex) "Commencer" else "Continuer · +10") }
            }
        }
    }
}
