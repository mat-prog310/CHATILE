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
import androidx.compose.material.icons.outlined.Book
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
import androidx.compose.runtime.collectAsState
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
        val profile by viewModel.profile.collectAsState()
        androidx.compose.runtime.CompositionLocalProvider(com.chatile.ui.theme.LocalEquippedTheme provides profile?.equippedTheme) {
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
}

@Composable
fun CustomBottomNavigationBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Chats,
        NavigationItem.Characters,
        NavigationItem.Stories,
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
                onOpenAdmin = { navController.navigate("admin") },
                onOpenCreatorEarnings = { navController.navigate("creator_earnings") },
                onOpenLootBoxes = { navController.navigate("loot_boxes") },
                onOpenInventory = { navController.navigate("inventory") },
                onOpenPublicProfile = { userId -> navController.navigate("public_profile/$userId") }
            )
        }

        // Loot Boxes
        composable("loot_boxes") {
            LootBoxScreen(
                vm = viewModel,
                onBack = { navController.popBackStack() },
                onOpenOpening = { navController.navigate("loot_opening") }
            )
        }

        composable("loot_opening") {
            LootBoxOpeningScreen(
                vm = viewModel,
                onDone = { navController.popBackStack() }
            )
        }

        // Inventory
        composable("inventory") {
            InventoryScreen(
                vm = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Public Profile
        composable("public_profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull()
            if (userId != null) {
                PublicProfileScreen(
                    userId = userId,
                    vm = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Creator Earnings
        composable("creator_earnings") {
            CreatorEarningsScreen(
                vm = viewModel,
                onBack = { navController.popBackStack() },
                onOpenLeaderboard = { navController.navigate("creator_leaderboard") }
            )
        }

        // Creator Leaderboard
        composable("creator_leaderboard") {
            CreatorLeaderboardScreen(
                vm = viewModel,
                onBack = { navController.popBackStack() }
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
                onShowInterstitial = { /* Handle interstitial */ },
                onStartVoiceCall = { id -> navController.navigate("voice_call/$id") }
            )
        }

        // Voice Call
        composable("voice_call/{characterId}") { backStackEntry ->
            val characterId = backStackEntry.arguments?.getString("characterId")?.toLongOrNull()
            if (characterId != null) {
                VoiceCallScreen(
                    characterId = characterId,
                    vm = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
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
        
        // Stories
        composable("stories") {
            StoriesScreen(
                vm = viewModel,
                onBack = { navController.popBackStack() },
                onPlayStory = { storyId -> navController.navigate("play_story/$storyId") },
                onCreateStory = { navController.navigate("create_story") }
            )
        }

        composable("play_story/{storyId}") { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId")?.toLongOrNull()
            if (storyId != null) {
                PlayStoryScreen(
                    storyId = storyId,
                    vm = viewModel,
                    onBack = { navController.popBackStack() },
                    onShowRewardedAd = { onRewarded ->
                        // Show Rewarded Ad
                        onRewarded()
                    }
                )
            }
        }

        composable("create_story") {
            CreateGeneratedStoryScreen(
                vm = viewModel,
                onBack = { navController.popBackStack() },
                onStoryGenerated = { storyId ->
                    navController.navigate("play_story/$storyId")
                }
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
    data object Stories : NavigationItem("stories", "Histoires", Icons.Outlined.Book)
    data object Profile : NavigationItem("profile", "Profil", Icons.Default.Person)
}
