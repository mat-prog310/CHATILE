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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatile.ChatViewModel
import com.chatile.Feedback
import com.chatile.Post
import com.chatile.Report
import com.chatile.AdminStats
import com.chatile.ui.theme.BadgeIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    navController: androidx.navigation.NavController = androidx.navigation.compose.rememberNavController(),
    viewModel: ChatViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Posts", "Signalements", "Feedbacks", "Stats")
    val coroutineScope = rememberCoroutineScope()
    
    val adminPosts by viewModel.adminPosts.collectAsState()
    val adminReports by viewModel.adminReports.collectAsState()
    val adminFeedback by viewModel.adminFeedback.collectAsState()
    val adminStats by viewModel.adminStats.collectAsState()
    val adminLoading by viewModel.adminLoading.collectAsState()

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> viewModel.loadAdminPosts()
            1 -> viewModel.loadReports("pending")
            2 -> viewModel.loadFeedback("new")
            3 -> viewModel.loadAdminStats()
        }
    }

    Scaffold(
        topBar = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.ArrowBack,
                        contentDescription = "Retour",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Panneau Admin",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (selectedTab) {
                0 -> {
                    AdminPostsSection(
                        posts = adminPosts,
                        loading = adminLoading,
                        onPin = { postId, isPinned ->
                            if (isPinned) {
                                viewModel.unpinPostGlobal(postId)
                            } else {
                                viewModel.pinPostGlobal(postId)
                            }
                        }
                    )
                }
                1 -> {
                    AdminReportsSection(
                        reports = adminReports,
                        loading = adminLoading,
                        onResolve = { reportId, action ->
                            viewModel.resolveReport(reportId, action)
                        }
                    )
                }
                2 -> {
                    AdminFeedbackSection(
                        feedbacks = adminFeedback,
                        loading = adminLoading,
                        onResolve = { feedbackId, response ->
                            viewModel.resolveFeedback(feedbackId, response)
                        }
                    )
                }
                3 -> {
                    AdminStatsSection(
                        stats = adminStats,
                        loading = adminLoading
                    )
                }
            }
        }
    }
}

@Composable
fun AdminPostsSection(
    posts: List<Post>,
    loading: Boolean,
    onPin: (Long, Boolean) -> Unit
) {
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Create post button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                .padding(16.dp)
                .clickable { /* Navigate to create admin post */ }
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Default.Create,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Créer une annonce",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        if (posts.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aucun post admin",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "@${post.authorUsername ?: "Utilisateur"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onPin(post.id ?: return@IconButton, post.isPinnedGlobal) }
                                    ) {
                                        Icon(
                                            if (post.isPinnedGlobal) androidx.compose.material.icons.Icons.Default.PushPin else androidx.compose.material.icons.Icons.Default.Clear,
                                            contentDescription = if (post.isPinnedGlobal) "Désépingler" else "Épingler",
                                            tint = if (post.isPinnedGlobal) MaterialTheme.colorScheme.primary 
                                                   else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(post.content)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminReportsSection(
    reports: List<Report>,
    loading: Boolean,
    onResolve: (Long, String) -> Unit
) {
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (reports.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Default.Report,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun signalement en attente",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reports) { report ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Signalé par: Utilisateur #${report.reporterId}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = report.status,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (report.status) {
                                "pending" -> MaterialTheme.colorScheme.error
                                "resolved" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Raison: ${report.reason}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cible: ${report.targetType} #${report.targetId}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Button(
                            onClick = { onResolve(report.id ?: return@Button, "dismiss") },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text("Ignorer")
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        androidx.compose.material3.Button(
                            onClick = { onResolve(report.id ?: return@Button, "remove_content") },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text("Supprimer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminFeedbackSection(
    feedbacks: List<Feedback>,
    loading: Boolean,
    onResolve: (Long, String?) -> Unit
) {
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (feedbacks.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Default.Feedback,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun nouveau feedback",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(feedbacks) { feedback ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Utilisateur #${feedback.userId}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = feedback.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(feedback.content)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Button(
                            onClick = { onResolve(feedback.id ?: return@Button, null) },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = "Marquer comme résolu"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatsSection(
    stats: AdminStats?,
    loading: Boolean
) {
    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val actualStats = stats ?: AdminStats()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            label = "Utilisateurs totaux",
            value = actualStats.totalUsers.toString(),
            icon = androidx.compose.material.icons.Icons.Default.People
        )
        StatCard(
            label = "Utilisateurs actifs (7j)",
            value = actualStats.activeUsers.toString(),
            icon = androidx.compose.material.icons.Icons.Default.People
        )
        StatCard(
            label = "Messages totaux",
            value = actualStats.totalMessages.toString(),
            icon = androidx.compose.material.icons.Icons.Default.Sms
        )
        StatCard(
            label = "Personnages créés",
            value = actualStats.totalCharacters.toString(),
            icon = androidx.compose.material.icons.Icons.Default.People
        )
        StatCard(
            label = "Posts totaux",
            value = actualStats.totalPosts.toString(),
            icon = androidx.compose.material.icons.Icons.Default.Assignment
        )
        StatCard(
            label = "Abonnés Premium",
            value = actualStats.premiumUsers.toString(),
            icon = androidx.compose.material.icons.Icons.Default.Star
        )
        StatCard(
            label = "Pièces distribuées (30j)",
            value = actualStats.piecesDistributed.toString(),
            icon = androidx.compose.material.icons.Icons.Default.BarChart
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminScreenPreview() {
    AdminScreen()
}
