package com.chatile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.chatile.ui.theme.ChatileScreenBackground
import com.chatile.R
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.chatile.ChatViewModel
import com.chatile.Character
import com.chatile.PublicCharacter
import com.chatile.ui.theme.BadgeIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PublicCharactersScreen(
    navController: androidx.navigation.NavHostController = androidx.navigation.compose.rememberNavController(),
    viewModel: ChatViewModel = viewModel(),
) {
    val characters by viewModel.publicCharacters.collectAsState()
    val accountCharacters by viewModel.characters.collectAsState()
    val sort by viewModel.publicCharactersSort.collectAsState()
    val query by viewModel.publicCharactersQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMore by viewModel.publicCharactersHasMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val baseCharacters = remember(accountCharacters, query) {
        accountCharacters.asSequence()
            .filter { it.isSystem }
            .sortedBy { it.id }
            .take(4)
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .toList()
    }
    val totalGridItems = baseCharacters.size + characters.size +
        (if (baseCharacters.isEmpty()) 0 else 1) + (if (characters.isEmpty()) 0 else 1)
    val gridState = rememberLazyGridState()
    val refreshing = isLoading && characters.isNotEmpty()
    val refreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { viewModel.loadPublicCharacters(reset = true) },
    )
    var searchText by remember { mutableStateOf(query) }
    val tabs = listOf("Tendances", "Récents", "Top")
    val sorts = listOf("trending", "new", "top")

    LaunchedEffect(Unit) {
        viewModel.loadPublicCharacters(reset = true)
    }
    LaunchedEffect(searchText) {
        delay(300)
        viewModel.setPublicCharactersQuery(searchText)
    }
    LaunchedEffect(query) {
        searchText = query
    }
    LaunchedEffect(gridState, totalGridItems, hasMore, isLoading) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (characters.isNotEmpty() && hasMore && !isLoading && lastVisible >= totalGridItems - 4) {
                    viewModel.loadPublicCharacters()
                }
            }
    }

    ChatileScreenBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.logo_chatile),
                        contentDescription = "CHATILE Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "CHATILE",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
                androidx.compose.material3.TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    singleLine = true,
                    placeholder = { Text("Rechercher un personnage", color = androidx.compose.ui.graphics.Color(0xFF666666)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF666666)) },
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color(0xFF141414),
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color(0xFF141414),
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedTextColor = androidx.compose.ui.graphics.Color.White,
                        unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.height(12.dp))
                TabRow(
                    selectedTabIndex = sorts.indexOf(sort).coerceAtLeast(0),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    indicator = { tabPositions ->
                        androidx.compose.material3.TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[sorts.indexOf(sort).coerceAtLeast(0)]),
                            color = androidx.compose.ui.graphics.Color(0xFFB47CFF)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = sort == sorts[index],
                            onClick = { viewModel.setPublicCharactersSort(sorts[index]) },
                            text = { 
                                Text(
                                    tab, 
                                    color = if (sort == sorts[index]) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF666666),
                                    fontWeight = if (sort == sorts[index]) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxSize().pullRefresh(refreshState),
                ) {
                when {
                    isLoading && characters.isEmpty() && baseCharacters.isEmpty() -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(6) { SkeletonCharacterCard() }
                    }
                    error != null && characters.isEmpty() && baseCharacters.isEmpty() -> EmptyMessage(
                        message = error ?: "Impossible de charger les personnages.",
                        buttonText = "Réessayer",
                        onClick = { viewModel.clearError(); viewModel.loadPublicCharacters(reset = true) },
                    )
                    characters.isEmpty() && baseCharacters.isEmpty() -> EmptyMessage(
                        message = "Aucun personnage public pour l’instant. Sois le premier à en créer un !",
                        buttonText = "Créer un personnage",
                        onClick = { navController.navigate("create_character") },
                    )
                    else -> LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (baseCharacters.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text("Personnages de base", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 6.dp))
                            }
                            items(baseCharacters, key = { "base-${it.id}" }) { character ->
                                BaseCharacterCard(character) {
                                    character.id?.let { navController.navigate("chat/$it") }
                                }
                            }
                        }
                        if (characters.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text("Créations de la communauté", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                        items(characters, key = { it.id }) { character ->
                            PublicCharacterCard(
                                character = character,
                                onOpenDetails = { navController.navigate("character_detail/${character.id}") },
                                onLike = { viewModel.likeCharacter(character.id) },
                                onImport = {
                                    viewModel.importCharacter(character.id) { importedId ->
                                        navController.navigate("chat/$importedId")
                                    }
                                },
                            )
                        }
                        if (characters.isEmpty() && baseCharacters.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyMessage(
                                    message = error ?: "Aucun personnage public pour l’instant. Sois le premier à en créer un !",
                                    buttonText = if (error == null) "Créer un personnage" else "Réessayer",
                                    onClick = {
                                        if (error == null) navController.navigate("create_character")
                                        else { viewModel.clearError(); viewModel.loadPublicCharacters(reset = true) }
                                    },
                                )
                            }
                        }
                        if (isLoading) items(2) { SkeletonCharacterCard() }
                    }
                }
                PullRefreshIndicator(
                    refreshing = refreshing,
                    state = refreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}
}

@Composable
private fun BaseCharacterCard(character: Character, onChat: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onChat),
    ) {
        Column(Modifier.padding(10.dp)) {
            if (character.avatarUrl.isNullOrBlank()) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(character.name.take(1), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                AsyncImage(
                    model = character.avatarUrl,
                    contentDescription = "Avatar de ${character.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)),
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(character.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("CHATILE · Officiel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(character.greeting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            Button(onClick = onChat, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Discuter") }
        }
    }
}

@Composable
private fun EmptyMessage(message: String, buttonText: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClick) { Text(buttonText) }
    }
}

@Composable
private fun SkeletonCharacterCard() {
    val transition = rememberInfiniteTransition(label = "catalogue-skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.62f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
            Box(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
            Box(Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
            Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
        }
    }
}

@Composable
private fun PublicCharacterCard(
    character: PublicCharacter,
    onOpenDetails: () -> Unit,
    onLike: () -> Unit,
    onImport: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenDetails),
    ) {
        Column(Modifier.padding(10.dp)) {
            if (character.avatarUrl.isNullOrBlank()) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(character.name.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                AsyncImage(
                    model = character.avatarUrl,
                    contentDescription = "Avatar de ${character.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)),
                )
            }
            Spacer(Modifier.height(9.dp))
            Text(character.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Par ${character.creatorName}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                character.creatorBadge?.let { BadgeIcon(it) }
            }
            Text(
                character.greeting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLike, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (character.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (character.isLikedByCurrentUser) "Retirer le j’aime" else "Liker",
                            tint = if (character.isLikedByCurrentUser) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(character.likesCount.toString(), style = MaterialTheme.typography.labelSmall)
                }
                if (character.isImported) {
                    Text("✓ Importé", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                } else {
                    Button(onClick = onImport, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("Utiliser", maxLines = 1)
                    }
                }
            }
        }
    }
}
