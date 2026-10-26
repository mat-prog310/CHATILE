package com.chatile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chatile.AuthManager
import com.chatile.ChatViewModel

@Composable
fun AppNavHost(onWatchAd: () -> Unit = {}) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    var isLoggedIn by remember { mutableStateOf(authManager.isSignedIn()) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val viewModel: ChatViewModel = viewModel()

    if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = {
            viewModel.refresh()
            isLoggedIn = true
        })
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (currentRoute != "chat/{characterId}") {
                    BottomNavigationBar(navController = navController)
                }
            }
        ) { paddingValues ->
            NavGraph(
                navController = navController,
                viewModel = viewModel,
                onSignIn = { 
                    authManager.signOut()
                    isLoggedIn = false 
                },
                onWatchAd = onWatchAd,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        NavigationItem.Home,
        NavigationItem.Chats,
        NavigationItem.Feed,
        NavigationItem.Characters,
        NavigationItem.Profile
    )
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { item ->
            val selected = currentDestination?.route == item.route
            
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (selected) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                label = { Text(item.label) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun NavGraph(
    navController: androidx.navigation.NavHostController,
    viewModel: ChatViewModel,
    onSignIn: () -> Unit = {},
    onWatchAd: () -> Unit = {},
    modifier: Modifier = Modifier
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
        
        // Feed
        composable("feed") {
            FeedScreen(
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
                onSignIn = onSignIn,
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
        
        // Post Detail
        composable("post_detail/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId")?.toLong()
            PostDetailScreen(
                postId = postId,
                navController = navController,
                viewModel = viewModel
            )
        }
        
        // Create Post
        composable("create_post") {
            CreatePostScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
        
        // User Profile
        composable("user_profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLong()
            UserProfileScreen(
                userId = userId,
                navController = navController,
                viewModel = viewModel
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
                onPurchase = { productId -> /* Handle purchase */ }
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
    data object Feed : NavigationItem("feed", "Feed", Icons.Default.TheaterComedy)
    data object Characters : NavigationItem("characters", "Personnages", Icons.Default.People)
    data object Profile : NavigationItem("profile", "Profil", Icons.Default.Person)
}
