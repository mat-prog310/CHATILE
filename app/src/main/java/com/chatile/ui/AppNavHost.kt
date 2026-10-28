package com.chatile.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chatile.AuthManager
import com.chatile.ChatViewModel
import com.chatile.ui.theme.ChatileBackground
import com.chatile.ui.theme.ChatileBorder
import com.chatile.ui.theme.ChatileScreenBackground

@Composable
fun AppNavHost(onWatchAd: () -> Unit = {}, onPurchase: (String) -> Unit = {}) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    var isCheckingAuth by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val viewModel: ChatViewModel = viewModel()

    LaunchedEffect(Unit) {
        val signedIn = authManager.isSignedIn()
        if (signedIn) {
            val token = authManager.getIdToken()
            if (!token.isNullOrBlank()) {
                com.chatile.ApiClient.setFirebaseIdToken(token)
                viewModel.refresh()
                isLoggedIn = true
            } else {
                isLoggedIn = false
            }
        } else {
            isLoggedIn = false
        }
        kotlinx.coroutines.delay(600)
        isCheckingAuth = false
    }

    if (isCheckingAuth) {
        SplashScreen()
    } else if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = {
            viewModel.refresh()
            isLoggedIn = true
        })
    } else {
        ChatileScreenBackground {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                bottomBar = {
                    if (currentRoute != "chat/{characterId}") {
                        CustomBottomNavigationBar(navController = navController)
                    }
                }
            ) { paddingValues ->
                NavGraph(
                    navController = navController,
                    viewModel = viewModel,
                    onWatchAd = onWatchAd,
                    onPurchase = onPurchase,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun CustomBottomNavigationBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Chats,
        NavigationItem.Characters,
        NavigationItem.Profile
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        color = ChatileBackground,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = ChatileBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
        items.forEach { item ->
            val selected = currentDestination?.route == item.route
            var tapped by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (tapped) 1.2f else 1f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
                label = "scale"
            )

            LaunchedEffect(tapped) {
                if (tapped) {
                    kotlinx.coroutines.delay(100)
                    tapped = false
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable {
                        tapped = true
                        if (!selected) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (selected) Color.White else Color(0xFF666666),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                )
                Text(
                    text = item.label,
                    color = if (selected) Color.White else Color(0xFF666666),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp
                )
            }
        }
    }
}
}

@Composable
fun NavGraph(
    navController: androidx.navigation.NavHostController,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    onWatchAd: () -> Unit = {},
    onPurchase: (String) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        // Home
        composable("home") {
            HomeScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        // Chats and personal characters
        composable("chats") {
            CharacterListScreen(
                vm = viewModel,
                onOpenChat = { characterId ->
                    if (characterId != null) {
                        navController.navigate("chat/$characterId")
                    } else {
                        navController.navigate("chat/0")
                    }
                },
                onCreateCharacter = {
                    navController.navigate("create_character")
                }
            )
        }
        
        // Public character catalogue
        composable("characters") {
            PublicCharactersScreen(
                navController = navController,
                viewModel = viewModel
            )
        }

        composable("character_detail/{characterId}") { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId")?.toLongOrNull()
            if (characterId != null) {
                CharacterDetailScreen(
                    characterId = characterId,
                    onBack = { navController.popBackStack() },
                    onOpenChat = { importedId -> navController.navigate("chat/$importedId") },
                    viewModel = viewModel,
                )
            }
        }
        
        // Profile
        composable("profile") {
            ProfileScreen(
                vm = viewModel,
                onOpenShop = { navController.navigate("shop") },
                onWatchAd = onWatchAd,
                onOpenAdmin = { navController.navigate("admin") }
            )
        }
        
        // Chat
        composable("chat/{characterId}") { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId")?.toLong()
            LaunchedEffect(characterId) {
                if (characterId != null) viewModel.selectCharacter(characterId)
            }
            ChatScreen(
                vm = viewModel,
                onBack = { navController.popBackStack() },
                onBuy = { productId -> navController.navigate("shop") },
                onWatchAd = { /* Handle ad watch */ },
                onShowInterstitial = { /* Handle interstitial */ }
            )
        }
        
        // Create Character
        composable("create_character") {
            CreateCharacterScreen(
                vm = viewModel,
                onBack = { navController.popBackStack() },
                onCreated = { characterId ->
                    navController.navigate("chat/$characterId")
                }
            )
        }
        
        // Search
        composable("search") {
            SearchScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        // Shop
        composable("shop") {
            ShopScreen(
                vm = viewModel,
                onPurchase = onPurchase
            )
        }
        
        // Feedback
        composable("feedback") {
            FeedbackScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        // Admin
        composable("admin") {
            AdminScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        // Onboarding
        composable("onboarding") {
            OnboardingScreen(
                vm = viewModel,
                onFinish = { navController.navigate("home") }
            )
        }
    }
}

sealed class NavigationItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : NavigationItem("home", "Accueil", Icons.Default.Home)
    data object Chats : NavigationItem("chats", "Chats", Icons.AutoMirrored.Filled.Chat)
    data object Characters : NavigationItem("characters", "Personnages", Icons.Default.People)
    data object Profile : NavigationItem("profile", "Profil", Icons.Default.Person)
}
