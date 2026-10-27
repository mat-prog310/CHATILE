package com.chatile

import android.os.Bundle
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.chatile.ui.AppNavHost
import com.chatile.ui.theme.PiecesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: ChatViewModel by viewModels()
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var billingClient: BillingClient? = null
    private val products = mutableMapOf<String, ProductDetails>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(16, 21, 19)
        window.navigationBarColor = AndroidColor.rgb(16, 21, 19)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        ApiClient.init(applicationContext)
        FirebaseApp.initializeApp(this)
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUser.getIdToken(false).addOnSuccessListener { token ->
                ApiClient.setFirebaseIdToken(token.token)
                vm.refresh()
            }
        }

        importSharedLink(intent)
        MobileAds.initialize(this) { loadRewardedAd(); loadInterstitialAd() }
        connectBilling()

        setContent {
            val theme by vm.theme.collectAsState()
            PiecesTheme(theme) {
                AppNavHost(
                    onWatchAd = ::showRewardedAd,
                    onPurchase = ::launchPurchase
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importSharedLink(intent)
    }

    private fun importSharedLink(intent: Intent?) {
        val token = intent?.data?.takeIf { it.host == "chatile.app" && it.path?.startsWith("/c/") == true }
            ?.lastPathSegment ?: return
        vm.importSharedCharacter(token)
    }

    private fun signInWithGoogle() {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            Toast.makeText(this, "Configurez googleWebClientId dans Gradle pour activer Google Sign-In.", Toast.LENGTH_LONG).show()
            return
        }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        lifecycleScope.launch {
            try {
                val result = CredentialManager.create(this@MainActivity).getCredential(this@MainActivity, request)
                val credential = result.credential
                if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    Toast.makeText(this@MainActivity, "Identifiant Google non reconnu.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).addOnCompleteListener(this@MainActivity) { task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Connexion Firebase impossible.", Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }
                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { token ->
                        ApiClient.setFirebaseIdToken(token.token)
                        vm.refresh()
                    }
                }
            } catch (error: Exception) {
                Toast.makeText(this@MainActivity, error.message ?: "Connexion Google annulée.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun connectBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(::onPurchasesUpdated)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
            )
            .build()
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: com.android.billingclient.api.BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts(BillingClient.ProductType.INAPP, listOf("pack_500", "pack_1500", "pack_5000", "pack_15000"))
                    queryProducts(BillingClient.ProductType.SUBS, listOf("sub_premium_monthly", "sub_premium_plus_monthly"))
                    queryActiveSubscriptions()
                }
            }

            override fun onBillingServiceDisconnected() {
                products.clear()
            }
        })
    }

    private fun queryProducts(type: String, ids: List<String>) {
        val client = billingClient ?: return
        val productList = ids.map { id ->
            QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                queryResult.productDetailsList.forEach { products[it.productId] = it }
            }
        }
    }

    private fun launchPurchase(productId: String) {
        val client = billingClient
        val product = products[productId]
        if (client == null || product == null) {
            Toast.makeText(this, "Google Play Billing n’est pas prêt.", Toast.LENGTH_SHORT).show()
            return
        }
        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
        if (product.productType == BillingClient.ProductType.SUBS) {
            val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken == null) {
                Toast.makeText(this, "Aucune offre d’abonnement disponible.", Toast.LENGTH_SHORT).show()
                return
            }
            productParamsBuilder.setOfferToken(offerToken)
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()
        client.launchBillingFlow(this, flowParams)
    }

    private fun onPurchasesUpdated(
        result: com.android.billingclient.api.BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) {
            if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                Toast.makeText(this, "L’achat n’a pas abouti.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase -> verifyPurchase(purchase) }
    }

    private fun verifyPurchase(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: return
        lifecycleScope.launch {
            runCatching { ApiClient.verifyPurchase(purchase.purchaseToken, productId) }
                .onSuccess {
                    vm.refresh()
                    if (!productId.startsWith("sub_")) {
                        billingClient?.consumeAsync(
                            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
                        ) { consumeResult, _ ->
                            if (consumeResult.responseCode != BillingClient.BillingResponseCode.OK) {
                                Toast.makeText(this@MainActivity, "Achat vérifié; consommation à réessayer.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                .onFailure {
                    Toast.makeText(this@MainActivity, it.message ?: "Vérification du paiement impossible.", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun queryActiveSubscriptions() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .forEach(::verifyPurchase)
            }
        }
    }

    private fun loadRewardedAd() {
        val request = AdRequest.Builder().build()
        // Production Rewarded Ad Unit ID
        RewardedAd.load(
            this,
            "ca-app-pub-8964953556750058/8168818299",
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            },
        )
    }

    private fun showRewardedAd() {
        val ad = rewardedAd
        if (ad == null) {
            Toast.makeText(this, "La publicité n’est pas encore disponible.", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
            return
        }
        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                loadRewardedAd()
            }
        }
        ad.show(this) {
            vm.watchAd()
        }
    }

    private fun loadInterstitialAd() {
        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
            },
        )
    }

    private fun showInterstitialAd() {
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitialAd()
            return
        }
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { loadInterstitialAd() }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) { loadInterstitialAd() }
        }
        ad.show(this)
    }

    override fun onDestroy() {
        billingClient?.endConnection()
        super.onDestroy()
    }
}
