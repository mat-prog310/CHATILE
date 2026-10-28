package com.chatile

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Msg>>(emptyList())
    val messages: StateFlow<List<Msg>> = _messages.asStateFlow()

    private val _pieces = MutableStateFlow(0)
    val pieces: StateFlow<Int> = _pieces.asStateFlow()

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    private val _activeCharacterId = MutableStateFlow<Long?>(null)
    val activeCharacterId: StateFlow<Long?> = _activeCharacterId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _nsfwEnabled = MutableStateFlow(false)
    val nsfwEnabled: StateFlow<Boolean> = _nsfwEnabled.asStateFlow()

    private val _subscriptionTier = MutableStateFlow("free")
    val subscriptionTier: StateFlow<String> = _subscriptionTier.asStateFlow()

    private val _subscriptionExpiresAt = MutableStateFlow<String?>(null)
    val subscriptionExpiresAt: StateFlow<String?> = _subscriptionExpiresAt.asStateFlow()

    private val _dailyCanClaim = MutableStateFlow(false)
    val dailyCanClaim: StateFlow<Boolean> = _dailyCanClaim.asStateFlow()

    private val _nextDailyClaimAt = MutableStateFlow<String?>(null)
    val nextDailyClaimAt: StateFlow<String?> = _nextDailyClaimAt.asStateFlow()

    private val _canOpenMysteryBox = MutableStateFlow(false)
    val canOpenMysteryBox: StateFlow<Boolean> = _canOpenMysteryBox.asStateFlow()

    private val _nextMysteryBoxAt = MutableStateFlow<String?>(null)
    val nextMysteryBoxAt: StateFlow<String?> = _nextMysteryBoxAt.asStateFlow()

    private val _canWatchAd = MutableStateFlow(false)
    val canWatchAd: StateFlow<Boolean> = _canWatchAd.asStateFlow()

    private val _nextWatchAdAt = MutableStateFlow<String?>(null)
    val nextWatchAdAt: StateFlow<String?> = _nextWatchAdAt.asStateFlow()

    private val _shopItems = MutableStateFlow<List<ShopItem>>(emptyList())
    val shopItems: StateFlow<List<ShopItem>> = _shopItems.asStateFlow()

    private val _shopLoading = MutableStateFlow(true)
    val shopLoading: StateFlow<Boolean> = _shopLoading.asStateFlow()

    private val _shopError = MutableStateFlow<String?>(null)
    val shopError: StateFlow<String?> = _shopError.asStateFlow()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _stats = MutableStateFlow<UserStats?>(null)
    val stats: StateFlow<UserStats?> = _stats.asStateFlow()

    private val _memory = MutableStateFlow(MemoryResult(emptyList(), emptyList()))
    val memory: StateFlow<MemoryResult> = _memory.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _todayReward = MutableStateFlow(20)
    val todayReward: StateFlow<Int> = _todayReward.asStateFlow()

    private val _characterOfTheDay = MutableStateFlow<Character?>(null)
    val characterOfTheDay: StateFlow<Character?> = _characterOfTheDay.asStateFlow()

    private val _messagesUntilNextAd = MutableStateFlow(50)
    val messagesUntilNextAd: StateFlow<Int> = _messagesUntilNextAd.asStateFlow()

    private val _showInterstitial = MutableStateFlow(false)
    val showInterstitial: StateFlow<Boolean> = _showInterstitial.asStateFlow()

    private val _quests = MutableStateFlow(JSONArray())
    val quests: StateFlow<JSONArray> = _quests.asStateFlow()

    private val _rewards = MutableStateFlow(JSONArray())
    val rewards: StateFlow<JSONArray> = _rewards.asStateFlow()

    private val _piecesHistory = MutableStateFlow(JSONArray())
    val piecesHistory: StateFlow<JSONArray> = _piecesHistory.asStateFlow()

    private val _mysteryBox = MutableStateFlow<JSONObject?>(null)
    val mysteryBox: StateFlow<JSONObject?> = _mysteryBox.asStateFlow()

    private val _referralCode = MutableStateFlow<String?>(null)
    val referralCode: StateFlow<String?> = _referralCode.asStateFlow()

    private val _theme = MutableStateFlow("violet")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _tutorialCompleted = MutableStateFlow(false)
    val tutorialCompleted: StateFlow<Boolean> = _tutorialCompleted.asStateFlow()

    private val _retrospective = MutableStateFlow<JSONObject?>(null)
    val retrospective: StateFlow<JSONObject?> = _retrospective.asStateFlow()

    // Social features states
    private val _feed = MutableStateFlow<List<Post>>(emptyList())
    val feed: StateFlow<List<Post>> = _feed.asStateFlow()

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()

    private val _trendingCharacters = MutableStateFlow<List<Character>>(emptyList())
    val trendingCharacters: StateFlow<List<Character>> = _trendingCharacters.asStateFlow()

    private val _publicCharacters = MutableStateFlow<List<PublicCharacter>>(emptyList())
    val publicCharacters: StateFlow<List<PublicCharacter>> = _publicCharacters.asStateFlow()

    private val _publicCharactersSort = MutableStateFlow("trending")
    val publicCharactersSort: StateFlow<String> = _publicCharactersSort.asStateFlow()

    private val _publicCharactersPage = MutableStateFlow(0)
    val publicCharactersPage: StateFlow<Int> = _publicCharactersPage.asStateFlow()

    private val _publicCharactersQuery = MutableStateFlow("")
    val publicCharactersQuery: StateFlow<String> = _publicCharactersQuery.asStateFlow()

    private val _publicCharactersHasMore = MutableStateFlow(true)
    val publicCharactersHasMore: StateFlow<Boolean> = _publicCharactersHasMore.asStateFlow()

    private val _publicCharacterDetail = MutableStateFlow<PublicCharacter?>(null)
    val publicCharacterDetail: StateFlow<PublicCharacter?> = _publicCharacterDetail.asStateFlow()

    private var publicCharactersRequestId = 0

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _badgeType = MutableStateFlow<String?>(null)
    val badgeType: StateFlow<String?> = _badgeType.asStateFlow()

    private val _moodToday = MutableStateFlow<JSONObject?>(null)
    val moodToday: StateFlow<JSONObject?> = _moodToday.asStateFlow()

    // Admin states
    private val _adminPosts = MutableStateFlow<List<Post>>(emptyList())
    val adminPosts: StateFlow<List<Post>> = _adminPosts.asStateFlow()

    private val _adminReports = MutableStateFlow<List<Report>>(emptyList())
    val adminReports: StateFlow<List<Report>> = _adminReports.asStateFlow()

    private val _adminFeedback = MutableStateFlow<List<Feedback>>(emptyList())
    val adminFeedback: StateFlow<List<Feedback>> = _adminFeedback.asStateFlow()

    private val _adminStats = MutableStateFlow<AdminStats?>(null)
    val adminStats: StateFlow<AdminStats?> = _adminStats.asStateFlow()

    private val _adminLoading = MutableStateFlow(false)
    val adminLoading: StateFlow<Boolean> = _adminLoading.asStateFlow()

    private val fallbackCharacters = listOf(
        Character(
            id = 1L,
            name = "Léa",
            systemPrompt = "Tu es Léa, une amie douce et attentionnée.",
            greeting = "Bonjour ! Je suis Léa, ravie de faire ta connaissance. Comment s'passe ta journée ?",
            isSystem = true,
            relationshipLevel = 25,
            lastMood = "Joyeuse",
            likesCount = 120
        ),
        Character(
            id = 2L,
            name = "Alex",
            systemPrompt = "Tu es Alex, un coach sportif motivant et dynamique.",
            greeting = "Salut champion ! Prêt à tout donner aujourd'hui ?",
            isSystem = true,
            relationshipLevel = 10,
            lastMood = "Motivé",
            likesCount = 95
        ),
        Character(
            id = 3L,
            name = "Maya",
            systemPrompt = "Tu es Maya, une artiste créative et passionnée.",
            greeting = "Coucou ! Tu as une idée de dessin ou d'histoire à explorer ensemble ?",
            isSystem = true,
            relationshipLevel = 15,
            lastMood = "Inspirée",
            likesCount = 110
        )
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val me = ApiClient.me()
                val availableCharacters = runCatching { ApiClient.listCharacters() }.getOrNull().takeIf { !it.isNullOrEmpty() } ?: fallbackCharacters
                val daily = runCatching { ApiClient.dailyStatus() }.getOrNull() ?: DailyStatus(
                    canClaim = me.canClaimDaily,
                    nextClaimAt = me.nextDailyClaimAt,
                    streakDays = me.streakDays,
                    todayReward = 20
                )
                val dayCharacter = runCatching { ApiClient.getCharacterOfTheDay().optJSONObject("character") }.getOrNull()
                val mood = runCatching { ApiClient.getMoodToday() }.getOrNull() ?: JSONObject().put("mood", "Joyeuse").put("note", "Super journée !")
                
                _pieces.value = me.pieces
                _nsfwEnabled.value = me.nsfwEnabled
                _subscriptionTier.value = me.subscriptionTier
                _subscriptionExpiresAt.value = me.subscriptionExpiresAt
                _characters.value = availableCharacters
                _dailyCanClaim.value = me.canClaimDaily
                _nextDailyClaimAt.value = me.nextDailyClaimAt
                _canOpenMysteryBox.value = me.canOpenMysteryBox
                _nextMysteryBoxAt.value = me.nextMysteryBoxAt
                _canWatchAd.value = me.canWatchAd
                _nextWatchAdAt.value = me.nextWatchAdAt
                _streakDays.value = me.streakDays
                _messagesUntilNextAd.value = me.messagesUntilNextAd
                _tutorialCompleted.value = me.tutorialCompleted
                _theme.value = me.theme
                _todayReward.value = daily.todayReward
                _characterOfTheDay.value = availableCharacters.firstOrNull { it.id == dayCharacter?.optLong("id") } ?: availableCharacters.firstOrNull()
                _moodToday.value = mood
                _badgeType.value = me.badgeType
                
                // Update profile display name and avatar from me() response
                me.displayName?.let { displayName ->
                    val currentProfile = _profile.value ?: UserProfile(displayName = "", bio = "", avatarUrl = null)
                    _profile.value = currentProfile.copy(
                        displayName = displayName,
                        avatarUrl = me.avatarUrl ?: currentProfile.avatarUrl
                    )
                }
                
                runCatching { loadUser() }
            }.onFailure {
                _error.value = it.message ?: "Impossible de charger le compte et le solde."
            }
            _isLoading.value = false
        }
    }

    private suspend fun loadUser() {
        try {
            val userProfile = ApiClient.getProfile()
            _profile.value = userProfile
            _currentUser.value = User(
                displayName = userProfile.displayName,
                bio = userProfile.bio,
                avatarUrl = userProfile.avatarUrl
            )
        } catch (e: Exception) {
            _error.value = e.message ?: "Impossible de charger le profil utilisateur."
        }
    }

    fun send(text: String, usePremium: Boolean = false, mode: String = "normal") {
        val content = text.trim()
        if (content.isEmpty()) return
        val characterId = _activeCharacterId.value
        _messages.value = _messages.value + Msg(content, isUser = true)
        viewModelScope.launch {
            runCatching { ApiClient.chat(content, characterId, usePremium, mode) }
                .onSuccess { result ->
                    _messages.value = _messages.value + Msg(result.reply, isUser = false)
                    _pieces.value = result.pieces
                    _showInterstitial.value = result.showInterstitial
                    _messagesUntilNextAd.value = if (result.showInterstitial) 50 else (_messagesUntilNextAd.value - 1).coerceAtLeast(0)
                    if (characterId != null) {
                        _characters.value = _characters.value.map { character ->
                            if (character.id == characterId) character.copy(
                                relationshipLevel = result.relationshipLevel,
                                lastMood = result.lastMood,
                            ) else character
                        }
                    }
                }
                .onFailure { _error.value = it.message ?: "Le message n’a pas pu être envoyé." }
        }
    }

    fun watchAd() {
        viewModelScope.launch {
            runCatching { ApiClient.watchAd() }
                .onSuccess { _pieces.value = it }
                .onFailure { _error.value = it.message ?: "Récompense publicitaire indisponible." }
        }
    }

    fun claimDaily() {
        viewModelScope.launch {
            runCatching { ApiClient.dailyClaim() }
                .onSuccess { result ->
                    if (!result.success) {
                        _error.value = result.message ?: "La récompense quotidienne a déjà été réclamée."
                        loadDailyStatus()
                        return@onSuccess
                    }
                    _pieces.value = result.pieces
                    _dailyCanClaim.value = false
                    _streakDays.value = result.streakDays
                    _nextDailyClaimAt.value = result.nextClaimAt
                }
                .onFailure { _error.value = it.message ?: "Impossible de réclamer la récompense quotidienne." }
        }
    }

    fun openMysteryBox() {
        viewModelScope.launch {
            runCatching { ApiClient.mysteryBox() }
                .onSuccess {
                    _mysteryBox.value = it
                    _pieces.value = it.optInt("pieces", _pieces.value)
                    _canOpenMysteryBox.value = false
                }
                .onFailure { _error.value = it.message ?: "Impossible d'ouvrir le coffre mystère." }
        }
    }

    fun loadQuests() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { ApiClient.getQuests() }
                .onSuccess { _quests.value = it }
                .onFailure { _error.value = it.message ?: "Impossible de charger les quêtes." }
            _isLoading.value = false
        }
    }

    fun claimQuest(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { ApiClient.claimQuest(id) }
                .onSuccess { refresh(); loadQuests() }
                .onFailure { _error.value = it.message ?: "Impossible de réclamer cette quête." }
            _isLoading.value = false
        }
    }

    fun loadRewards() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { ApiClient.getRewards() }
                .onSuccess { _rewards.value = it }
                .onFailure {
                    _rewards.value = JSONArray()
                    _error.value = it.message ?: "Impossible de charger les récompenses."
                }
            _isLoading.value = false
        }
    }

    fun claimReward(action: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { ApiClient.claimReward(action) }
                .onSuccess { refresh(); loadRewards() }
                .onFailure { _error.value = it.message ?: "Impossible de réclamer cette récompense." }
            _isLoading.value = false
        }
    }
    fun loadPiecesHistory() { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.getPiecesHistory() }.onSuccess { _piecesHistory.value = it }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun loadReferralCode() { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.getReferralCode() }.onSuccess { _referralCode.value = it }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun applyReferral(code: String) { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.applyReferral(code) }.onSuccess { refresh() }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun setTheme(theme: String) { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.setTheme(theme) }.onSuccess { result -> _theme.value = result ?: theme }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun loadRetrospective() { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.getRetrospective() }.onSuccess { _retrospective.value = it }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun setMood(mood: String, note: String, characterId: Long? = null) { viewModelScope.launch { runCatching { ApiClient.setMood(mood, note, characterId) }.onFailure { _error.value = it.message } } }
    fun dismissInterstitial() { _showInterstitial.value = false }
    fun completeTutorialStep(step: Int, onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { ApiClient.tutorialStep(step) }
                .onSuccess { _pieces.value = it.optInt("pieces", _pieces.value); if (step == 5) _tutorialCompleted.value = true; onComplete() }
                .onFailure { _error.value = it.message }
        }
    }
    fun skipTutorial(onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching { ApiClient.tutorialSkip() }
                .onSuccess { _tutorialCompleted.value = true; onComplete() }
                .onFailure { _error.value = it.message }
        }
    }
    fun appendSystemMessage(text: String) { _messages.value = _messages.value + Msg(text, isUser = false) }
    fun loadMessages(id: Long, page: Int = 0) { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.getMessages(id, page) }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun editMessage(id: Long, content: String) { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.updateMessage(id, content) }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun removeMessage(id: Long) { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.deleteMessage(id) }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun favoriteMessage(id: Long) { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.favoriteMessage(id) }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun rewind(id: Long, messageId: Long) { viewModelScope.launch { _isLoading.value = true; runCatching { ApiClient.rewind(id, messageId) }.onFailure { _error.value = it.message }; _isLoading.value = false } }
    fun search(query: String, characterId: Long? = null) { viewModelScope.launch { runCatching { ApiClient.search(query, characterId) }.onFailure { _error.value = it.message } } }
    fun loadEvents(id: Long) { viewModelScope.launch { runCatching { ApiClient.getEvents(id) }.onFailure { _error.value = it.message } } }
    fun createEvent(id: Long, date: String, label: String, recurring: Boolean) { viewModelScope.launch { runCatching { ApiClient.createEvent(id, date, label, recurring) }.onFailure { _error.value = it.message } } }
    fun loadNotes(id: Long) { viewModelScope.launch { runCatching { ApiClient.getNotes(id) }.onFailure { _error.value = it.message } } }
    fun createNote(id: Long, content: String) { viewModelScope.launch { runCatching { ApiClient.createNote(id, content) }.onFailure { _error.value = it.message } } }
    fun loadGallery(id: Long) { viewModelScope.launch { runCatching { ApiClient.getGallery(id) }.onFailure { _error.value = it.message } } }
    fun loadJournal(id: Long) { viewModelScope.launch { runCatching { ApiClient.getJournal(id) }.onFailure { _error.value = it.message } } }
    fun loadThought(id: Long, onThought: (String) -> Unit) { viewModelScope.launch { runCatching { ApiClient.getThought(id) }.onSuccess(onThought).onFailure { _error.value = it.message } } }
    fun pinMessage(id: Long, text: String) { viewModelScope.launch { runCatching { ApiClient.pinMessage(id, text) }.onFailure { _error.value = it.message } } }
    fun cowrite(id: Long, paragraph: String) { viewModelScope.launch { runCatching { ApiClient.cowrite(id, paragraph) }.onFailure { _error.value = it.message } } }

    fun setNsfw(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { ApiClient.setNsfw(enabled) }
                .onSuccess { _nsfwEnabled.value = it }
                .onFailure { _error.value = it.message ?: "Paramètre indisponible." }
        }
    }

    fun createCharacter(
        name: String,
        prompt: String,
        greeting: String,
        isNsfw: Boolean,
        scenarioGoal: String = "",
        seedMemory: List<String> = emptyList(),
        boundaries: String = "",
        appearance: String = "",
        bannedWords: String = "",
        scenarioSteps: List<String> = emptyList(),
        avatarUri: Uri? = null,
        context: Context? = null,
        onCreated: (Long) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                val id = ApiClient.createCharacter(
                    name, prompt, greeting, isNsfw, scenarioGoal, seedMemory,
                    boundaries, appearance, bannedWords, scenarioSteps,
                )
                if (avatarUri != null && context != null) ApiClient.uploadCharacterAvatar(context, id, avatarUri)
                id
            }
                .onSuccess { id ->
                    refresh()
                    onCreated(id)
                    viewModelScope.launch {
                        repeat(20) {
                            delay(1500)
                            val status = runCatching { ApiClient.getCharacterAvatarStatus(id) }.getOrNull()
                                ?: return@launch
                            if (status.optString("avatarUrl").isNotBlank()) {
                                refresh()
                                return@launch
                            }
                            if (!status.optBoolean("generating")) return@launch
                        }
                    }
                }
                .onFailure { _error.value = it.message ?: "Création du personnage impossible." }
        }
    }

    fun selectCharacter(id: Long?) {
        _activeCharacterId.value = id
        if (id != null) {
            viewModelScope.launch {
                if (_characters.value.none { it.id == id }) {
                    loadCharacters()
                }
                runCatching { ApiClient.getMessages(id, 0) }
                    .onSuccess { jsonArray ->
                        val list = buildList {
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val role = obj.optString("role")
                                val content = obj.optString("content")
                                add(Msg(content, isUser = role == "user"))
                            }
                        }
                        if (list.isEmpty()) {
                            val greeting = _characters.value.firstOrNull { it.id == id }?.greeting
                            _messages.value = if (greeting.isNullOrBlank()) emptyList() else listOf(Msg(greeting, isUser = false))
                        } else {
                            _messages.value = list
                        }
                    }
                    .onFailure {
                        val greeting = _characters.value.firstOrNull { it.id == id }?.greeting
                        _messages.value = if (greeting.isNullOrBlank()) emptyList() else listOf(Msg(greeting, isUser = false))
                    }
            }
        } else {
            _messages.value = emptyList()
        }
    }

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            runCatching { ApiClient.deleteCharacter(id) }
                .onSuccess {
                    if (_activeCharacterId.value == id) selectCharacter(null)
                    refresh()
                }
                .onFailure { _error.value = it.message ?: "Suppression impossible." }
        }
    }

    fun loadCharacters() = refresh()

    fun updateCharacterAvatarInMemory(characterId: Long, newAvatarUrl: String) {
        _characters.value = _characters.value.map { char ->
            if (char.id == characterId) char.copy(avatarUrl = newAvatarUrl) else char
        }
    }

    fun loadMemory(id: Long) {
        viewModelScope.launch {
            runCatching { ApiClient.getMemory(id) }
                .onSuccess { _memory.value = it }
                .onFailure { _error.value = it.message ?: "Mémoire indisponible." }
        }
    }

    fun deleteMemory(id: Long, type: String, index: Int) {
        viewModelScope.launch {
            runCatching { ApiClient.deleteMemory(id, type, index) }
                .onSuccess { loadMemory(id) }
                .onFailure { _error.value = it.message ?: "Suppression impossible." }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            runCatching { ApiClient.getProfile() }
                .onSuccess { profile -> _profile.value = profile }
                .onFailure { _error.value = it.message ?: "Profil indisponible." }
        }
    }

    fun updateProfile(displayName: String, bio: String) {
        viewModelScope.launch {
            runCatching { ApiClient.updateProfile(displayName, bio) }
                .onSuccess { updatedProfile -> _profile.value = updatedProfile.copy(avatarUrl = _profile.value?.avatarUrl) }
                .onFailure { _error.value = it.message ?: "Mise à jour du profil impossible." }
        }
    }

    fun uploadProfileAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching { ApiClient.uploadAvatar(context, uri) }
                .onSuccess { avatarUrl ->
                    val current = _profile.value ?: UserProfile(displayName = "", bio = "", avatarUrl = null)
                    _profile.value = current.copy(avatarUrl = avatarUrl)
                }
                .onFailure { _error.value = it.message ?: "Envoi de la photo impossible." }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { ApiClient.getStats() }
                .onSuccess { stats -> _stats.value = stats }
                .onFailure { _error.value = it.message ?: "Statistiques indisponibles." }
            _isLoading.value = false
        }
    }

    fun loadShop() {
        viewModelScope.launch {
            _shopLoading.value = true
            _shopError.value = null
            runCatching { ApiClient.getShop() }
                .onSuccess {
                    _shopItems.value = it
                    _shopError.value = null
                }
                .onFailure { _shopError.value = it.message ?: "Boutique indisponible." }
            _shopLoading.value = false
        }
    }

    fun shareCharacter(id: Long, onShared: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { ApiClient.shareCharacter(id) }
                .onSuccess(onShared)
                .onFailure { _error.value = it.message ?: "Partage indisponible." }
        }
    }

    fun importSharedCharacter(token: String, onImported: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { ApiClient.importSharedCharacter(token) }
                .onSuccess { id -> refresh(); onImported(id) }
                .onFailure { _error.value = it.message ?: "Import impossible." }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // ========== Social Features ==========

    fun loadFeed(type: String = "for_you", page: Int = 0, limit: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.getFeed(type, page, limit)
            }.onSuccess { posts ->
                _feed.value = if (page == 0) posts else _feed.value + posts
            }.onFailure { _error.value = it.message ?: "Impossible de charger le fil d'actualité." }
            _isLoading.value = false
        }
    }

    fun loadPost(postId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.getPost(postId)
            }.onSuccess { post ->
                _post.value = post
            }.onFailure { _error.value = it.message ?: "Impossible de charger le post." }
            _isLoading.value = false
        }
    }

    fun loadComments(postId: Long, page: Int = 0, limit: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.getComments(postId, page, limit)
            }.onSuccess { comments ->
                _comments.value = if (page == 0) comments else _comments.value + comments
            }.onFailure { _error.value = it.message ?: "Impossible de charger les commentaires." }
            _isLoading.value = false
        }
    }

    fun togglePostLike(postId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.togglePostLike(postId)
            }.onSuccess {
                // Update local state
                _feed.value = _feed.value.map { post ->
                    if (post.id == postId) post.copy(
                        isLikedByMe = !post.isLikedByMe,
                        likesCount = if (post.isLikedByMe) post.likesCount - 1 else post.likesCount + 1
                    ) else post
                }
                _post.value = _post.value?.let { post ->
                    if (post.id == postId) post.copy(
                        isLikedByMe = !post.isLikedByMe,
                        likesCount = if (post.isLikedByMe) post.likesCount - 1 else post.likesCount + 1
                    ) else post
                }
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de modifier le like." }
        }
    }

    fun postComment(postId: Long, content: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.postComment(postId, content)
            }.onSuccess { comment ->
                _comments.value = listOf(comment) + _comments.value
                _post.value = _post.value?.copy(
                    commentsCount = (_post.value?.commentsCount ?: 0) + 1
                )
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible d'ajouter le commentaire." }
        }
    }

    fun sharePost(postId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.sharePost(postId)
            }.onSuccess {
                _post.value = _post.value?.copy(
                    sharesCount = (_post.value?.sharesCount ?: 0) + 1
                )
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de partager le post." }
        }
    }

    fun toggleCommentLike(commentId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.toggleCommentLike(commentId)
            }.onSuccess {
                _comments.value = _comments.value.map { comment ->
                    if (comment.id == commentId) comment.copy(
                        isLikedByMe = !comment.isLikedByMe,
                        likesCount = if (comment.isLikedByMe) comment.likesCount - 1 else comment.likesCount + 1
                    ) else comment
                }
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de modifier le like du commentaire." }
        }
    }

    fun loadNotifications(page: Int = 0, limit: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.getNotifications(page, limit)
            }.onSuccess { notifications ->
                _notifications.value = if (page == 0) notifications else _notifications.value + notifications
            }.onFailure { _error.value = it.message ?: "Impossible de charger les notifications." }
            _isLoading.value = false
        }
    }

    fun markNotificationRead(notificationId: Long) {
        viewModelScope.launch {
            runCatching {
                ApiClient.markNotificationRead(notificationId)
            }.onSuccess {
                _notifications.value = _notifications.value.map { notification ->
                    if (notification.id == notificationId) notification.copy(read = true) else notification
                }
                _unreadCount.value = _unreadCount.value - 1
            }.onFailure { _error.value = it.message ?: "Impossible de marquer la notification comme lue." }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            runCatching {
                ApiClient.markAllNotificationsRead()
            }.onSuccess {
                _notifications.value = _notifications.value.map { it.copy(read = true) }
                _unreadCount.value = 0
            }.onFailure { _error.value = it.message ?: "Impossible de marquer toutes les notifications comme lues." }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.getUnreadCount()
            }.onSuccess { count ->
                _unreadCount.value = count
            }.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun search(query: String, type: String, page: Int = 0) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.socialSearch(query, type, page)
            }.onSuccess { results ->
                _searchResults.value = if (page == 0) results else _searchResults.value + results
            }.onFailure { _error.value = it.message ?: "Impossible d'effectuer la recherche." }
            _isLoading.value = false
        }
    }

    fun loadUserProfile(userId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.getUserProfile(userId)
            }.onSuccess { profile ->
                _userProfile.value = profile
            }.onFailure { _error.value = it.message ?: "Impossible de charger le profil utilisateur." }
            _isLoading.value = false
        }
    }

    fun loadUserPosts(userId: Long, page: Int = 0, limit: Int = 20) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.getUserPosts(userId, page, limit)
            }.onSuccess { posts ->
                _userPosts.value = if (page == 0) posts else _userPosts.value + posts
            }.onFailure { _error.value = it.message ?: "Impossible de charger les posts de l'utilisateur." }
            _isLoading.value = false
        }
    }

    fun toggleFollow(userId: Long, onSuccess: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.toggleFollow(userId)
            }.onSuccess { isFollowing ->
                onSuccess(isFollowing)
            }.onFailure { _error.value = it.message ?: "Impossible de suivre/défollow." }
        }
    }

    fun loadTrendingCharacters() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.getTrendingCharacters()
            }.onSuccess { characters ->
                _trendingCharacters.value = characters
            }.onFailure { _error.value = it.message ?: "Impossible de charger les personnages populaires." }
            _isLoading.value = false
        }
    }

    // Public characters functions
    fun loadPublicCharacters(reset: Boolean = false) {
        if (!reset && (_isLoading.value || !_publicCharactersHasMore.value)) return
        val page = if (reset) 0 else _publicCharactersPage.value + 1
        val requestId = ++publicCharactersRequestId
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.getPublicCharacters(
                    sort = _publicCharactersSort.value,
                    page = page,
                    q = _publicCharactersQuery.value,
                    nsfw = _nsfwEnabled.value,
                )
            }.onSuccess { characters ->
                if (requestId == publicCharactersRequestId) {
                    _publicCharactersPage.value = page
                    _publicCharactersHasMore.value = characters.size >= 20
                    _publicCharacters.value = if (reset) characters else
                        (_publicCharacters.value + characters).distinctBy { it.id }
                }
            }.onFailure {
                if (requestId == publicCharactersRequestId) {
                    _error.value = it.message ?: "Impossible de charger les personnages publics."
                }
            }
            if (requestId == publicCharactersRequestId) _isLoading.value = false
        }
    }

    fun loadPublicCharacters(sort: String, page: Int) {
        if (sort !in setOf("trending", "new", "top") || page < 0) return
        _publicCharactersSort.value = sort
        if (page == 0) {
            _publicCharactersPage.value = 0
            loadPublicCharacters(reset = true)
        } else {
            _publicCharactersPage.value = page - 1
            loadPublicCharacters()
        }
    }

    fun setPublicCharactersSort(sort: String) {
        if (sort !in setOf("trending", "new", "top")) return
        _publicCharactersSort.value = sort
        loadPublicCharacters(reset = true)
    }

    fun setPublicCharactersQuery(query: String) {
        if (query == _publicCharactersQuery.value) return
        _publicCharactersQuery.value = query
        loadPublicCharacters(reset = true)
    }

    fun likeCharacter(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val isLiked = ApiClient.likeCharacter(id)
                // Update local state
                _publicCharacters.value = _publicCharacters.value.map { character ->
                    if (character.id == id) {
                        character.copy(
                            likesCount = if (isLiked) character.likesCount + 1 else character.likesCount - 1,
                            isLikedByCurrentUser = isLiked
                        )
                    } else character
                }
                _publicCharacterDetail.value = _publicCharacterDetail.value?.let { character ->
                    if (character.id == id) character.copy(
                        likesCount = if (isLiked) character.likesCount + 1 else (character.likesCount - 1).coerceAtLeast(0),
                        isLikedByCurrentUser = isLiked,
                    ) else character
                }
            }.onFailure { _error.value = it.message ?: "Impossible de liker le personnage." }
            _isLoading.value = false
        }
    }

    fun importCharacter(id: Long) {
        importCharacter(id) { }
    }

    fun importCharacter(id: Long, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.listCharacters().firstOrNull { it.id == id }?.id
                    ?: ApiClient.importCharacter(id)
            }.onSuccess { newId ->
                _publicCharacters.value = _publicCharacters.value.map { character ->
                    if (character.id == id) character.copy(isImported = true) else character
                }
                _publicCharacterDetail.value = _publicCharacterDetail.value?.let { character ->
                    if (character.id == id) character.copy(isImported = true) else character
                }
                refresh()
                onSuccess(newId)
            }.onFailure { _error.value = it.message ?: "Impossible d'importer le personnage." }
            _isLoading.value = false
        }
    }

    fun loadCharacterDetail(id: Long) {
        _publicCharacterDetail.value = null
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { ApiClient.getCharacterDetail(id) }
                .onSuccess { _publicCharacterDetail.value = it }
                .onFailure { _error.value = it.message ?: "Impossible de charger ce personnage." }
            _isLoading.value = false
        }
    }

    fun publishCharacter(id: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.publishCharacter(id)
            }.onSuccess {
                refresh()
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de publier le personnage." }
            _isLoading.value = false
        }
    }

    fun unpublishCharacter(id: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                ApiClient.unpublishCharacter(id)
            }.onSuccess {
                refresh()
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de dépublier le personnage." }
            _isLoading.value = false
        }
    }

    fun createPost(
        postType: String,
        postSubtype: String?,
        content: String,
        characterId: Long? = null,
        imageUrl: String? = null,
        visibility: String = "public",
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                ApiClient.createPost(postType, postSubtype, content, characterId, imageUrl, visibility)
            }.onSuccess { postId ->
                onSuccess(postId)
                refresh()
            }.onFailure { _error.value = it.message ?: "Impossible de créer le post." }
        }
    }

    fun deletePost(postId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.deletePost(postId)
            }.onSuccess {
                _feed.value = _feed.value.filter { it.id != postId }
                if (_post.value?.id == postId) _post.value = null
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de supprimer le post." }
        }
    }

    fun deleteComment(commentId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.deleteComment(commentId)
            }.onSuccess {
                _comments.value = _comments.value.filter { it.id != commentId }
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de supprimer le commentaire." }
        }
    }

    // ========== Admin Functions ==========
    fun loadAdminPosts() {
        viewModelScope.launch {
            _adminLoading.value = true
            runCatching {
                ApiClient.getAdminPosts()
            }.onSuccess { posts ->
                _adminPosts.value = posts
            }.onFailure { _error.value = it.message ?: "Impossible de charger les posts admin." }
            _adminLoading.value = false
        }
    }

    fun loadReports(status: String = "pending") {
        viewModelScope.launch {
            _adminLoading.value = true
            runCatching {
                ApiClient.getReports(status)
            }.onSuccess { reports ->
                _adminReports.value = reports
            }.onFailure { _error.value = it.message ?: "Impossible de charger les signalements." }
            _adminLoading.value = false
        }
    }

    fun loadFeedback(status: String = "new") {
        viewModelScope.launch {
            _adminLoading.value = true
            runCatching {
                ApiClient.getFeedback(status)
            }.onSuccess { feedbacks ->
                _adminFeedback.value = feedbacks
            }.onFailure { _error.value = it.message ?: "Impossible de charger les feedbacks." }
            _adminLoading.value = false
        }
    }

    fun loadAdminStats() {
        viewModelScope.launch {
            _adminLoading.value = true
            runCatching {
                ApiClient.getAdminStats()
            }.onSuccess { stats ->
                _adminStats.value = stats
            }.onFailure { _error.value = it.message ?: "Impossible de charger les stats admin." }
            _adminLoading.value = false
        }
    }

    fun pinPostGlobal(postId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.pinPostGlobal(postId)
            }.onSuccess {
                _adminPosts.value = _adminPosts.value.map { post ->
                    if (post.id == postId) post.copy(isPinnedGlobal = true) else post
                }
                _feed.value = _feed.value.map { post ->
                    if (post.id == postId) post.copy(isPinnedGlobal = true) else post
                }
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible d'épingler le post." }
        }
    }

    fun unpinPostGlobal(postId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.unpinPostGlobal(postId)
            }.onSuccess {
                _adminPosts.value = _adminPosts.value.map { post ->
                    if (post.id == postId) post.copy(isPinnedGlobal = false) else post
                }
                _feed.value = _feed.value.map { post ->
                    if (post.id == postId) post.copy(isPinnedGlobal = false) else post
                }
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de désépingler le post." }
        }
    }

    fun resolveReport(reportId: Long, action: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.resolveReport(reportId, action)
            }.onSuccess {
                _adminReports.value = _adminReports.value.map { report ->
                    if (report.id == reportId) report.copy(status = "resolved") else report
                }
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de résoudre le signalement." }
        }
    }

    fun resolveFeedback(feedbackId: Long, response: String?, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.resolveFeedback(feedbackId, response)
            }.onSuccess {
                _adminFeedback.value = _adminFeedback.value.map { feedback ->
                    if (feedback.id == feedbackId) feedback.copy(status = "resolved") else feedback
                }
                onSuccess()
            }.onFailure { _error.value = it.message ?: "Impossible de résoudre le feedback." }
        }
    }

    fun banUser(userId: Long, duration: String? = null, reason: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.banUser(userId, duration ?: "30d", reason ?: "")
            }.onSuccess { onSuccess() }
                .onFailure { _error.value = it.message ?: "Impossible de bannir l'utilisateur." }
        }
    }

    fun sendFeedback(category: String, content: String, contactEmail: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.sendFeedback(category, content, contactEmail)
            }.onSuccess { onSuccess() }
                .onFailure { _error.value = it.message ?: "Impossible d'envoyer le feedback." }
        }
    }

    fun reportContent(targetType: String, targetId: Long, reason: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.reportContent(targetType, targetId, reason)
            }.onSuccess { onSuccess() }
                .onFailure { _error.value = it.message ?: "Impossible de signaler le contenu." }
        }
    }

    fun loadCharacterOfTheDay() {
        viewModelScope.launch {
            runCatching {
                ApiClient.getCharacterOfTheDay()
            }.onSuccess { response ->
                val characterJson = response.optJSONObject("character") ?: response
                val id = characterJson.optLong("id")
                if (id != 0L) {
                    val fullChar = _characters.value.firstOrNull { it.id == id }
                    if (fullChar != null) {
                        _characterOfTheDay.value = fullChar
                    } else {
                        val name = characterJson.optString("name", "Assistant IA")
                        val rawAvatar = characterJson.optString("avatar_url").ifBlank { characterJson.optString("avatarUrl").ifBlank { null } }
                        val char = Character(
                            id = id,
                            name = name,
                            systemPrompt = characterJson.optString("system_prompt", characterJson.optString("systemPrompt")),
                            greeting = characterJson.optString("greeting", "Salut !"),
                            isNsfw = characterJson.optInt("is_nsfw", characterJson.optInt("isNsfw")) != 0,
                            avatarUrl = rawAvatar,
                            relationshipLevel = characterJson.optInt("relationship_level"),
                            lastMood = characterJson.optString("last_mood", "neutral"),
                            isSystem = true,
                            scenarioGoal = characterJson.optString("scenario_goal").ifBlank { null },
                        )
                        _characterOfTheDay.value = char
                    }
                }
            }.onFailure { _error.value = it.message ?: "Impossible de charger le personnage du jour." }
        }
    }

    fun claimDailyReward(onSuccess: (Int) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.dailyClaim()
            }.onSuccess { result ->
                if (!result.success) {
                    _error.value = result.message ?: "La récompense quotidienne a déjà été réclamée."
                    loadDailyStatus()
                    return@onSuccess
                }
                _pieces.value = result.pieces
                _dailyCanClaim.value = false
                _streakDays.value = result.streakDays
                _nextDailyClaimAt.value = result.nextClaimAt
                onSuccess(result.pieces)
            }.onFailure { _error.value = it.message ?: "Impossible de réclamer la récompense quotidienne." }
        }
    }

    fun claimMysteryBox(onSuccess: (Int) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                ApiClient.mysteryBox()
            }.onSuccess { result ->
                val pieces = result.optInt("pieces", _pieces.value)
                _pieces.value = pieces
                _mysteryBox.value = result
                _canOpenMysteryBox.value = false
                refresh()
                onSuccess(pieces)
            }.onFailure { _error.value = it.message ?: "Impossible d'ouvrir le coffre mystère." }
        }
    }

    fun loadDailyStatus() {
        viewModelScope.launch {
            runCatching {
                ApiClient.dailyStatus()
            }.onSuccess { status ->
                _dailyCanClaim.value = status.canClaim
                _streakDays.value = status.streakDays
                _todayReward.value = status.todayReward
            }.onFailure { _error.value = it.message }
        }
    }
}

data class Msg(val text: String, val isUser: Boolean)
