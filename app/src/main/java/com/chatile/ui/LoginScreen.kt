package com.chatile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatile.AuthManager
import com.chatile.R
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { AuthManager(context) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    NightBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("CHATILE", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Connecte-toi pour accéder à tes Pièces et à tes personnages.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Sans compte, tu ne peux pas gagner de Pièces ni sauvegarder ton profil.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            error = null
                            val result = authManager.signInWithGoogle()
                            isLoading = false
                            result.onSuccess { onLoginSuccess() }
                                  .onFailure { error = "Connexion Google échouée : ${it.message}" }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Se connecter avec Google", color = Color(0xFF4285F4))
                }
            }
            
            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            
            Spacer(Modifier.height(24.dp))
            Text(
                "Connexion sécurisée avec Google.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
        }
    }
}
