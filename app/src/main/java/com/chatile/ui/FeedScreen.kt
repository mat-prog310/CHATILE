package com.chatile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import coil.compose.AsyncImage
import com.chatile.ChatViewModel
import com.chatile.Post
import com.chatile.ui.theme.BadgeIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: androidx.navigation.NavController = androidx.navigation.compose.rememberNavController(),
    viewModel: ChatViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val feed by viewModel.feed.collectAsState()
    val userPosts by viewModel.userPosts.collectAsState()
    val profile by viewModel.profile.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Pour toi", "Abonnements", "Récent", "Mon profil")

    LaunchedEffect(selectedTab, profile) {
        if (selectedTab == 3) {
            val myId = profile?.id ?: profile?.userId
            if (myId != null) {
                viewModel.loadUserProfile(myId)
                viewModel.loadUserPosts(myId, 0)
            }
        } else {
            val type = when (selectedTab) {
                0 -> "for_you"
                1 -> "following"
                else -> "recent"
            }
            viewModel.loadFeed(type, 0)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            IconButton(
                onClick = { navController.navigate("create_post") },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Créer un post",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        topBar = {
            Column {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val isLoading by viewModel.isLoading.collectAsState()
        val currentPosts = if (selectedTab == 3) userPosts else feed

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (currentPosts.isEmpty()) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            if (selectedTab == 3) "Vous n'avez pas encore de post" else "Aucun post pour le moment",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (selectedTab == 3) "Partagez votre première pensée ou création sur le réseau social !" else "Soyez le premier à partager quelque chose sur le réseau !",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { navController.navigate("create_post") }) {
                            Text("Créer une publication")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentPosts) { post ->
                        PostCard(
                            post = post,
                            onCharacterClick = { characterId ->
                                navController.navigate("character_detail/$characterId")
                            },
                            onLike = {
                                coroutineScope.launch {
                                    viewModel.togglePostLike(post.id ?: return@launch)
                                }
                            },
                            onComment = {
                                navController.navigate("post_detail/${post.id}")
                            },
                            onShare = {
                                coroutineScope.launch {
                                    viewModel.sharePost(post.id ?: return@launch)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Lien copié dans le presse-papiers")
                                    }
                                }
                            },
                            onUserClick = {
                                navController.navigate("user_profile/${post.userId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onUserClick: () -> Unit,
    onCharacterClick: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(
                if (post.isPinnedGlobal) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        onClick = onComment
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with author info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = post.authorAvatarUrl,
                    contentDescription = null,
                    placeholder = painterResource(id = android.R.drawable.ic_menu_camera),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.size(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorDisplayName ?: "Utilisateur inconnu",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        if (!post.authorBadgeType.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.size(6.dp))
                            BadgeIcon(badgeType = post.authorBadgeType)
                        }
                        
                        if (post.authorUsername != null) {
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "@${post.authorUsername}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    if (post.isPinnedGlobal) {
                        Text(
                            text = "Ⓦ Épinglé par CHATILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                IconButton(onClick = { /* More options */ }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Post content
            if (post.postSubtype == "image" && !post.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = if (post.postSubtype == "excerpt") 6 else Int.MAX_VALUE
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Character reference if linked
            if (post.characterId != null && post.characterName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                        .fillMaxWidth()
                        .clickable { post.characterId?.let(onCharacterClick) }
                ) {
                    if (!post.characterAvatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = post.characterAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    Text(
                        text = "Personnage: ${post.characterName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onLike)
                ) {
                    Icon(
                        if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLikedByMe) MaterialTheme.colorScheme.error 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = post.likesCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (post.isLikedByMe) MaterialTheme.colorScheme.error 
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onComment)
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = "Commenter",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = post.commentsCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Partager",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    FeedScreen()
}
