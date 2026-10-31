package com.chatile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chatile.AuthManager
import com.chatile.R
import com.chatile.ui.theme.TextPrimary
import com.chatile.ui.theme.TextSecondary
import com.chatile.ui.theme.VioletAccent
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { AuthManager(context) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    NightBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("CHATILE", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Text(
                "750 Pièces offertes à l'inscription",
                style = MaterialTheme.typography.titleMedium,
                color = VioletAccent,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isSignUp) "Créez votre compte pour commencer." else "Connectez-vous pour accéder à vos Pièces.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator(color = VioletAccent)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                error = "Veuillez remplir tous les champs."
                                return@Button
                            }
                            scope.launch {
                                isLoading = true
                                error = null
                                val result = if (isSignUp) {
                                    authManager.signUpWithEmail(email.trim(), password)
                                } else {
                                    authManager.signInWithEmail(email.trim(), password)
                                }
                                isLoading = false
                                result.onSuccess { onLoginSuccess() }
                                      .onFailure { error = "Erreur : ${it.message}" }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = VioletAccent)
                    ) {
                        Text(if (isSignUp) "Créer un compte" else "Se connecter", color = Color.White)
                    }

                    TextButton(
                        onClick = { isSignUp = !isSignUp },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            if (isSignUp) "Déjà un compte ? Se connecter" else "Pas de compte ? S'inscrire",
                            color = TextSecondary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)

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
                        Text("Continuer avec Google", color = Color(0xFF4285F4))
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}
