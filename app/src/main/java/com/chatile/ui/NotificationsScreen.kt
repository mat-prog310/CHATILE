package com.chatile.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import coil.compose.AsyncImage
import com.chatile.ChatViewModel
import com.chatile.Notification
import com.chatile.ui.theme.BadgeIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: androidx.navigation.NavController = androidx.navigation.compose.rememberNavController(),
    viewModel: ChatViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications(0)
    }

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.weight(1f))
                    androidx.compose.material3.Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(unreadCount.toString())
                    }
                }
            }
        },
        floatingActionButton = {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        viewModel.markAllNotificationsRead()
                    }
                }
            ) {
                Text(
                    text = "Tout marquer comme lu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (notifications.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucune notification",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                coroutineScope.launch {
                                    notification.id?.let { viewModel.markNotificationRead(it) }
                                    // Navigate based on notification type
                                    when (notification.type) {
                                        "post_like", "post_comment" -> {
                                            navController.navigate("post_detail/${notification.targetId}")
                                        }
                                        "follow" -> {
                                            navController.navigate("user_profile/${notification.actorId}")
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit
) {
    val icon = when (notification.type) {
        "post_like", "comment_like" -> Icons.Default.Favorite
        "post_comment", "comment_reply" -> Icons.Default.Create
        "follow" -> Icons.Default.Person
        "official_post" -> Icons.Default.Star
        "new_feedback", "new_report" -> Icons.Default.Notifications
        else -> Icons.Default.Notifications
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (notification.read) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (notification.read) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer
                )
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (notification.read) 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else 
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).align(Alignment.Center)
            )
        }
        
        Spacer(modifier = Modifier.size(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (notification.actorDisplayName != null) {
                    Text(
                        text = notification.actorDisplayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (!notification.actorBadgeType.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.size(6.dp))
                    BadgeIcon(badgeType = notification.actorBadgeType)
                }
                
                if (notification.actorUsername != null) {
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "@${notification.actorUsername}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Text(
                text = getNotificationText(notification),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 2
            )
        }
        
        if (!notification.read) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun getNotificationText(notification: Notification): String {
    return when (notification.type) {
        "post_like" -> "a aimé votre post"
        "comment_like" -> "a aimé votre commentaire"
        "post_comment" -> "a commenté votre post"
        "comment_reply" -> "a répondu à votre commentaire"
        "follow" -> "vous suit maintenant"
        "official_post" -> "a publié une annonce"
        "new_feedback" -> "Nouveau feedback reçu"
        "new_report" -> "Nouveau signalement"
        "warning" -> "Avertissement: ${notification.postContent ?: ""}"
        else -> notification.type
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationsScreenPreview() {
    NotificationsScreen()
}
