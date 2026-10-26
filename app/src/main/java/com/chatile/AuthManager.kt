package com.chatile

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()
    
    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        return try {
            val serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (serverClientId.isBlank()) {
                return Result.failure(Exception("La connexion Google n’est pas configurée."))
            }
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()
            
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            
            if (credential is CustomCredential && 
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                
                // Stocker le token Firebase dans ApiClient pour les requêtes suivantes
                val idToken = authResult.user?.getIdToken(false)?.await()?.token
                if (!idToken.isNullOrBlank()) {
                    ApiClient.setFirebaseIdToken(idToken)
                    ApiClient.loginWithFirebase(idToken)
                }
                
                Result.success(authResult.user!!)
            } else {
                Result.failure(Exception("Type de credential invalide"))
            }
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Aucun compte Google n’est disponible sur cet appareil. Ajoute un compte Google dans les paramètres de l’émulateur."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun isSignedIn(): Boolean = firebaseAuth.currentUser != null
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser
    suspend fun getIdToken(): String? = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
    fun signOut() = firebaseAuth.signOut()
}
