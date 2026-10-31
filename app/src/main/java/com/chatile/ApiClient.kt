package com.chatile

import android.content.Context
import android.net.Uri
import java.io.IOException
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.auth.FirebaseAuth

import com.chatile.BuildConfig

object ApiClient {
    // URL du backend configurée via BuildConfig
    // - Debug : http://10.0.2.2:3000 (émulateur)
    // - Release : https://chatile.onrender.com (production)
    private val BASE = BuildConfig.API_BASE_URL
    private const val PREFS = "pieces_ia"
    private const val DEVICE_ID_KEY = "device_id"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var deviceId: String? = null

    @Volatile
    private var firebaseIdToken: String? = null

    fun setFirebaseIdToken(token: String?) {
        firebaseIdToken = token
    }

    fun init(context: Context) {
        val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        deviceId = preferences.getString(DEVICE_ID_KEY, null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(DEVICE_ID_KEY, it).apply()
        }
    }

    suspend fun me(): MeResult {
        val json = request("GET", "/api/me")
        return MeResult(
            json.getInt("pieces"), json.optBoolean("nsfw_enabled"),
            json.optString("subscription_tier", "free"), json.optString("subscription_expires_at").ifBlank { null },
            json.optInt("messages_until_next_ad", 50), json.optInt("streak_days"), json.optBoolean("tutorial_completed"),
            json.optString("theme", "violet"),
            json.optString("badge_type").ifBlank { null },
            json.optString("display_name").ifBlank { null },
            json.optString("avatar_url").ifBlank { null },
            json.optBoolean("canClaimDaily", false),
            json.optString("nextDailyClaimAt").ifBlank { null },
            json.optBoolean("canOpenMysteryBox", false),
            json.optString("nextMysteryBoxAt").ifBlank { null },
            json.optBoolean("canWatchAd", false),
            json.optString("nextWatchAdAt").ifBlank { null },
        )
    }

    suspend fun loginWithFirebase(idToken: String): JSONObject =
        request("POST", "/api/auth/firebase", JSONObject().put("idToken", idToken))

    suspend fun watchAd(): Int = request("POST", "/api/watch-ad", JSONObject()).getInt("pieces")

    suspend fun dailyClaim(): DailyClaimResult {
        val json = request("POST", "/api/daily-claim", JSONObject())
        return DailyClaimResult(
            json.optBoolean("success"), json.optInt("pieces"), json.optString("nextClaimAt").ifBlank { null },
            json.optInt("streak_days"), json.optInt("reward"),
            json.optString("error").ifBlank { null }, json.optString("message").ifBlank { null },
        )
    }

    suspend fun dailyStatus(): DailyStatus {
        val json = request("GET", "/api/daily-status")
        return DailyStatus(
            json.optBoolean("canClaim"), json.optString("nextClaimAt").ifBlank { null },
            json.optInt("streak_days"), json.optInt("todayReward"),
        )
    }

    suspend fun mysteryBox(): JSONObject = request("POST", "/api/mystery-box", JSONObject())

    suspend fun getPiecesHistory(): JSONArray = request("GET", "/api/pieces/history").getJSONArray("transactions")

    suspend fun getRewards(): JSONArray = request("GET", "/api/rewards").getJSONArray("rewards")

    suspend fun claimReward(action: String): JSONObject =
        request("POST", "/api/rewards/action", JSONObject().put("action", action))

    suspend fun getQuests(): JSONArray = request("GET", "/api/quests").getJSONArray("quests")

    suspend fun claimQuest(id: Long): JSONObject = request("POST", "/api/quests/$id/claim", JSONObject())

    suspend fun applyReferral(code: String): JSONObject =
        request("POST", "/api/referral/apply", JSONObject().put("code", code))

    suspend fun getReferralCode(): String = request("GET", "/api/referral/code").getString("code")

    suspend fun getReferralStats(): JSONObject = request("GET", "/api/referral/stats")

    suspend fun setTheme(theme: String): String =
        request("POST", "/api/settings/theme", JSONObject().put("theme", theme)).getString("theme")

    suspend fun chat(message: String, characterId: Long?, usePremiumModel: Boolean, mode: String = "normal"): com.chatile.ChatResult {
        val body = JSONObject().put("message", message).put("usePremiumModel", usePremiumModel).put("mode", mode)
        if (characterId != null) body.put("characterId", characterId)
        val json = request("POST", "/api/chat", body)
        return com.chatile.ChatResult(
            reply = json.getString("reply"),
            pieces = json.getInt("pieces"),
            relationshipLevel = json.optInt("relationship_level"),
            lastMood = json.optString("last_mood", "neutral"),
            userMessageId = null,
            assistantMessageId = null,
            suggestions = json.optJSONArray("suggestions")?.toStringList().orEmpty(),
            showInterstitial = json.optBoolean("showInterstitial"),
        )
    }

    suspend fun setNsfw(enabled: Boolean): Boolean =
        request("POST", "/api/settings/nsfw", JSONObject().put("enabled", enabled))
            .getBoolean("nsfw_enabled")

    suspend fun createCharacter(
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
        avatarUrl: String? = null,
    ): JSONObject {
        val body = JSONObject()
            .put("name", name)
            .put("systemPrompt", prompt)
            .put("greeting", greeting)
            .put("isNsfw", isNsfw)
            .put("scenarioGoal", scenarioGoal)
            .put("seedMemory", JSONArray(seedMemory))
            .put("boundaries", boundaries)
            .put("appearance", appearance)
            .put("bannedWords", bannedWords)
            .put("scenarioSteps", JSONArray(scenarioSteps))
        val urlToUse = if (!avatarUrl.isNullOrBlank()) avatarUrl else "https://ui-avatars.com/api/?name=${java.net.URLEncoder.encode(name, "UTF-8")}&background=B47CFF&color=fff"
        body.put("avatar_url", urlToUse)
        return request("POST", "/api/characters", body)
    }

    suspend fun listCharacters(): List<Character> {
        val json = request("GET", "/api/characters")
        val items = json.getJSONArray("characters")
        return buildList {
            for (index in 0 until items.length()) add(items.getJSONObject(index).toCharacter())
        }
    }

    suspend fun deleteCharacter(characterId: Long) {
        request("DELETE", "/api/characters/$characterId")
    }

    suspend fun getCharacterAvatarStatus(characterId: Long): JSONObject =
        request("GET", "/api/characters/$characterId/avatar-status")

    suspend fun getMemory(characterId: Long): MemoryResult {
        val json = request("GET", "/api/characters/$characterId/memory")
        return MemoryResult(json.getJSONArray("seedMemory").toStringList(), json.getJSONArray("learnedMemory").toStringList())
    }

    suspend fun deleteMemory(characterId: Long, type: String, index: Int) {
        request("DELETE", "/api/characters/$characterId/memory/$type/$index")
    }

    suspend fun shareCharacter(characterId: Long): String =
        request("GET", "/api/characters/$characterId/share").getString("url")

    suspend fun importSharedCharacter(token: String): Long =
        request("POST", "/api/shared/$token/import", JSONObject()).getLong("id")

    suspend fun getStats(): UserStats {
        val json = request("GET", "/api/stats")
        val rows = json.getJSONArray("messagesByCharacter")
        val byCharacter = buildList {
            for (index in 0 until rows.length()) {
                val row = rows.getJSONObject(index)
                add(CharacterStats(
                    characterName = row.getString("characterName"),
                    messageCount = row.getInt("messages"),
                    lastInteraction = row.optString("lastInteraction")
                ))
            }
        }
        return UserStats(
            totalMessages = json.optInt("totalMessages"),
            preferredCharacter = json.optString("preferredCharacter").ifBlank { null },
            messagesByCharacter = byCharacter,
            streakDays = json.optInt("streakDays"),
            totalPiecesEarned = json.optInt("totalPiecesEarned"),
            totalPiecesSpent = json.optInt("totalPiecesSpent")
        )
    }

    suspend fun cowrite(characterId: Long, lastParagraph: String): JSONObject =
        request("POST", "/api/chat/cowrite", JSONObject().put("characterId", characterId).put("lastParagraph", lastParagraph))

    suspend fun getMessages(characterId: Long, page: Int): JSONArray =
        request("GET", "/api/characters/$characterId/messages?page=$page&limit=50").getJSONArray("messages")

    suspend fun updateMessage(id: Long, content: String): JSONObject =
        request("PUT", "/api/messages/$id", JSONObject().put("content", content))

    suspend fun deleteMessage(id: Long) { request("DELETE", "/api/messages/$id") }

    suspend fun regenerate(id: Long): JSONObject = request("POST", "/api/messages/$id/regenerate", JSONObject())

    suspend fun favoriteMessage(id: Long): Boolean =
        request("POST", "/api/messages/$id/favorite", JSONObject()).getBoolean("favorite")

    suspend fun rewind(characterId: Long, messageId: Long): JSONObject =
        request("POST", "/api/characters/$characterId/rewind", JSONObject().put("messageId", messageId))

    suspend fun search(query: String, characterId: Long? = null): JSONArray {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val character = characterId?.let { "&characterId=$it" }.orEmpty()
        return request("GET", "/api/search?q=$encoded$character").getJSONArray("results")
    }

    suspend fun pinMessage(characterId: Long, text: String): Long = request(
        "POST", "/api/characters/$characterId/pin", JSONObject().put("messageText", text),
    ).getLong("id")

    suspend fun getEvents(characterId: Long): JSONArray =
        request("GET", "/api/characters/$characterId/events").getJSONArray("events")

    suspend fun createEvent(characterId: Long, eventDate: String, label: String, recurring: Boolean): Long = request(
        "POST", "/api/characters/$characterId/events",
        JSONObject().put("eventDate", eventDate).put("label", label).put("recurring", recurring),
    ).getLong("id")

    suspend fun getNotes(characterId: Long): JSONArray =
        request("GET", "/api/characters/$characterId/notes").getJSONArray("notes")

    suspend fun createNote(characterId: Long, content: String): Long = request(
        "POST", "/api/characters/$characterId/notes", JSONObject().put("content", content),
    ).getLong("id")

    suspend fun getGallery(characterId: Long): JSONArray =
        request("GET", "/api/characters/$characterId/gallery").getJSONArray("images")

    suspend fun getJournal(characterId: Long): JSONArray =
        request("GET", "/api/characters/$characterId/journal").getJSONArray("events")

    suspend fun getThought(characterId: Long): String =
        request("GET", "/api/characters/$characterId/thought").getString("thought")

    suspend fun getMoodToday(): JSONObject = request("GET", "/api/mood/today").optJSONObject("mood") ?: JSONObject()

    suspend fun setMood(mood: String, note: String, characterId: Long? = null): JSONObject {
        val body = JSONObject().put("mood", mood).put("note", note)
        if (characterId != null) body.put("characterId", characterId)
        return request("POST", "/api/mood", body)
    }

    suspend fun generateChallenge(characterId: Long): JSONObject =
        request("POST", "/api/challenge/generate", JSONObject().put("characterId", characterId))

    suspend fun completeChallenge(id: Long): JSONObject =
        request("POST", "/api/challenge/$id/complete", JSONObject())

    suspend fun getRetrospective(): JSONObject = request("GET", "/api/retrospective")

    suspend fun getWidgetData(): JSONObject = request("GET", "/api/widget-data")

    suspend fun getCharacterOfTheDay(): JSONObject = request("GET", "/api/character-of-the-day")

    suspend fun getCreatorEarnings(): JSONObject = request("GET", "/api/creator/earnings")

    suspend fun getCreatorLeaderboard(): JSONObject = request("GET", "/api/creator/leaderboard")

    suspend fun startVoiceCall(characterId: Long): JSONObject =
        request("POST", "/api/voice-call/start", JSONObject().put("characterId", characterId))

    suspend fun endVoiceCall(callId: Long, durationSeconds: Int): JSONObject =
        request("POST", "/api/voice-call/end", JSONObject().put("callId", callId).put("durationSeconds", durationSeconds))

    suspend fun transcribeVoice(text: String): JSONObject =
        request("POST", "/api/voice-call/transcribe", JSONObject().put("text", text))

    suspend fun speakVoice(text: String, characterId: Long): JSONObject =
        request("POST", "/api/voice-call/speak", JSONObject().put("text", text).put("characterId", characterId))

    // ========== Stories Features ==========

    private fun JSONObject.toStory(): Story = Story(
        id = getLong("id"),
        userId = optLong("userId", optLong("user_id")),
        characterId = optLong("characterId").takeIf { it != 0L },
        title = getString("title"),
        description = optString("description", ""),
        genre = optString("genre", "Aventure"),
        coverImage = optString("coverImage").ifBlank { optString("cover_image").ifBlank { null } },
        isPublic = optBoolean("isPublic", optInt("is_public") != 0),
        status = optString("status", "published"),
        playsCount = optInt("playsCount", optInt("plays_count")),
        creatorEarnings = optInt("creatorEarnings", optInt("creator_earnings")),
        createdAt = optString("createdAt", optString("created_at")),
    )

    fun JSONObject.toStoryNode(): StoryNode {
        val choicesJson = optJSONArray("choices") ?: JSONArray()
        val choicesList = buildList {
            for (i in 0 until choicesJson.length()) {
                val c = choicesJson.getJSONObject(i)
                add(StoryNodeChoice(
                    id = c.getLong("id"),
                    text = c.getString("text"),
                    targetNodeId = c.optLong("targetNodeId").takeIf { it != 0L }
                ))
            }
        }
        return StoryNode(
            id = getLong("id"),
            storyId = optLong("storyId"),
            content = getString("content"),
            nodeType = optString("nodeType", "scene"),
            isEnding = optBoolean("isEnding", false),
            choices = choicesList
        )
    }

    suspend fun getPublicStories(): List<Story> {
        val json = request("GET", "/api/stories/public")
        val array = json.getJSONArray("stories")
        return buildList { for (i in 0 until array.length()) add(array.getJSONObject(i).toStory()) }
    }

    suspend fun getMyStories(): List<Story> {
        val json = request("GET", "/api/stories/my")
        val array = json.getJSONArray("stories")
        return buildList { for (i in 0 until array.length()) add(array.getJSONObject(i).toStory()) }
    }

    suspend fun playStory(storyId: Long): JSONObject =
        request("POST", "/api/stories/$storyId/play")

    suspend fun replayStory(storyId: Long, method: String): JSONObject =
        request("POST", "/api/stories/$storyId/replay", JSONObject().put("method", method))

    suspend fun makeStoryChoice(sessionId: Long, choiceId: Long): JSONObject =
        request("POST", "/api/stories/session/$sessionId/choice", JSONObject().put("choiceId", choiceId))

    suspend fun generateStory(prompt: String, genre: String): JSONObject =
        request("POST", "/api/stories/generate", JSONObject().put("prompt", prompt).put("genre", genre))

    suspend fun createStoryManual(title: String, description: String, genre: String, nodes: JSONArray): JSONObject =
        request("POST", "/api/stories", JSONObject().put("title", title).put("description", description).put("genre", genre).put("nodes", nodes))

    suspend fun openLootBox(): JSONObject = request("POST", "/api/loot-boxes/open")
    suspend fun getLootPity(): JSONObject = request("GET", "/api/loot-boxes/pity")
    suspend fun getInventory(): JSONArray = request("GET", "/api/inventory").getJSONArray("items")
    suspend fun recycleItem(inventoryId: Long): JSONObject = request("POST", "/api/inventory/$inventoryId/recycle")
    suspend fun equipItem(inventoryId: Long): JSONObject = request("POST", "/api/inventory/$inventoryId/equip")
    suspend fun getPublicProfile(userId: Long): JSONObject = request("GET", "/api/users/$userId/public-profile")

    suspend fun tutorialStep(step: Int): JSONObject =
        request("POST", "/api/tutorial/step", JSONObject().put("step", step))

    suspend fun tutorialSkip() { request("POST", "/api/tutorial/skip", JSONObject()) }

    // ========== Social Features ==========

    suspend fun getFeed(type: String = "for_you", page: Int = 0, limit: Int = 20): List<Post> {
        val json = request("GET", "/api/feed?type=$type&page=$page&limit=$limit")
        val items = json.getJSONArray("posts")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toPost())
            }
        }
    }

    suspend fun createPost(
        postType: String,
        postSubtype: String? = null,
        content: String,
        characterId: Long? = null,
        imageUrl: String? = null,
        visibility: String = "public"
    ): Long {
        val body = JSONObject()
            .put("postType", postType)
            .put("content", content)
            .put("visibility", visibility)
        if (postSubtype != null) body.put("postSubtype", postSubtype)
        if (characterId != null) body.put("characterId", characterId)
        if (imageUrl != null) body.put("imageUrl", imageUrl)
        return request("POST", "/api/posts", body).getLong("id")
    }

    suspend fun getPost(postId: Long): Post {
        val json = request("GET", "/api/posts/$postId")
        return json.getJSONObject("post").toPost()
    }

    suspend fun deletePost(postId: Long) {
        request("DELETE", "/api/posts/$postId")
    }

    suspend fun togglePostLike(postId: Long): Boolean {
        val json = request("POST", "/api/posts/$postId/like")
        return json.getBoolean("liked")
    }

    suspend fun sharePost(postId: Long): Int {
        val json = request("POST", "/api/posts/$postId/share")
        return json.getInt("sharesCount")
    }

    suspend fun postComment(postId: Long, content: String): Comment {
        val json = request("POST", "/api/posts/$postId/comments", JSONObject().put("content", content))
        return json.getJSONObject("comment").toComment()
    }

    suspend fun getComments(postId: Long, page: Int = 0, limit: Int = 20): List<Comment> {
        val json = request("GET", "/api/posts/$postId/comments?page=$page&limit=$limit")
        val items = json.getJSONArray("comments")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toComment())
            }
        }
    }

    suspend fun deleteComment(commentId: Long) {
        request("DELETE", "/api/comments/$commentId")
    }

    suspend fun toggleCommentLike(commentId: Long): Boolean {
        val json = request("POST", "/api/comments/$commentId/like")
        return json.getBoolean("liked")
    }

    suspend fun getNotifications(page: Int = 0, limit: Int = 20): List<Notification> {
        val json = request("GET", "/api/notifications?page=$page&limit=$limit")
        val items = json.getJSONArray("notifications")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toNotification())
            }
        }
    }

    suspend fun markNotificationRead(notificationId: Long) {
        request("POST", "/api/notifications/$notificationId/read")
    }

    suspend fun markAllNotificationsRead() {
        request("POST", "/api/notifications/read-all")
    }

    suspend fun getUnreadCount(): Int {
        val json = request("GET", "/api/notifications/unread-count")
        return json.getInt("count")
    }

    suspend fun socialSearch(query: String, type: String, page: Int = 0): List<SearchResult> {
        val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
        val json = request("GET", "/api/social/search?q=$encoded&type=$type&page=$page")
        val items = json.getJSONArray("results")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toSearchResult())
            }
        }
    }

    suspend fun getUserProfile(userId: Long): UserProfile {
        val json = request("GET", "/api/users/$userId")
        return json.getJSONObject("user").toUserProfile()
    }

    suspend fun getUserPosts(userId: Long, page: Int = 0, limit: Int = 20): List<Post> {
        val json = request("GET", "/api/users/$userId/posts?page=$page&limit=$limit")
        val items = json.getJSONArray("posts")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toPost())
            }
        }
    }

    suspend fun toggleFollow(userId: Long): Boolean {
        val json = request("POST", "/api/users/$userId/follow")
        return json.getBoolean("following")
    }

    suspend fun getFollowers(userId: Long, page: Int = 0): List<UserProfile> {
        val json = request("GET", "/api/users/$userId/followers?page=$page")
        val items = json.getJSONArray("followers")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toUserProfile())
            }
        }
    }

    suspend fun getFollowing(userId: Long, page: Int = 0): List<UserProfile> {
        val json = request("GET", "/api/users/$userId/following?page=$page")
        val items = json.getJSONArray("following")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toUserProfile())
            }
        }
    }

    suspend fun getTrendingCharacters(): List<Character> {
        val json = request("GET", "/api/characters/public?sort=trending")
        val items = json.getJSONArray("characters")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toCharacter())
            }
        }
    }

    // Public characters functions
    suspend fun getPublicCharacters(
        sort: String = "trending",
        page: Int = 0,
        q: String? = null,
        nsfw: Boolean = false,
        limit: Int = 20,
    ): List<PublicCharacter> {
        val query = q?.takeIf { it.isNotBlank() }?.let { "&q=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
        val includeNsfw = if (nsfw) "&nsfw=include" else ""
        val json = request("GET", "/api/characters/public?sort=$sort&page=$page&limit=$limit$query$includeNsfw")
        val items = json.getJSONArray("characters")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toPublicCharacter())
            }
        }
    }

    suspend fun getCharacterDetail(id: Long): PublicCharacter =
        request("GET", "/api/characters/$id/public").toPublicCharacter()

    suspend fun likeCharacter(id: Long): Boolean {
        val json = request("POST", "/api/characters/$id/like")
        return json.getBoolean("liked")
    }

    suspend fun importCharacter(id: Long): Long {
        val json = request("POST", "/api/characters/$id/import")
        return json.getLong("id")
    }

    suspend fun publishCharacter(id: Long) {
        request("POST", "/api/characters/$id/publish")
    }

    suspend fun unpublishCharacter(id: Long) {
        request("POST", "/api/characters/$id/unpublish")
    }

    suspend fun sendFeedback(category: String, content: String, contactEmail: String? = null): JSONObject {
        val body = JSONObject()
            .put("category", category)
            .put("content", content)
        if (contactEmail != null) body.put("contactEmail", contactEmail)
        return request("POST", "/api/feedback", body)
    }

    suspend fun reportContent(targetType: String, targetId: Long, reason: String): JSONObject {
        val body = JSONObject()
            .put("targetType", targetType)
            .put("targetId", targetId)
            .put("reason", reason)
        return request("POST", "/api/reports", body)
    }

    suspend fun getUserBadge(userId: Long): String? {
        val json = request("GET", "/api/users/$userId/badge")
        return json.optString("badgeType").ifBlank { null }
    }

    // ========== Admin Features ==========

    suspend fun createAdminPost(
        content: String,
        postType: String = "announcement",
        postSubtype: String? = null,
        characterId: Long? = null
    ): Long {
        val body = JSONObject()
            .put("postType", postType)
            .put("content", content)
        if (postSubtype != null) body.put("postSubtype", postSubtype)
        if (characterId != null) body.put("characterId", characterId)
        return request("POST", "/api/admin/posts", body).getLong("id")
    }

    suspend fun pinPostGlobal(postId: Long) {
        request("POST", "/api/admin/posts/$postId/pin-global")
    }

    suspend fun unpinPostGlobal(postId: Long) {
        request("POST", "/api/admin/posts/$postId/unpin-global")
    }

    suspend fun getAdminPosts(page: Int = 0, limit: Int = 20): List<Post> {
        val json = request("GET", "/api/admin/posts?page=$page&limit=$limit")
        val items = json.getJSONArray("posts")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toPost())
            }
        }
    }

    suspend fun getReports(status: String = "pending", page: Int = 0): List<Report> {
        val json = request("GET", "/api/admin/reports?status=$status&page=$page")
        val items = json.getJSONArray("reports")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toReport())
            }
        }
    }

    suspend fun resolveReport(reportId: Long, action: String, duration: String? = null): JSONObject {
        val body = JSONObject()
            .put("action", action)
        if (duration != null) body.put("duration", duration)
        return request("POST", "/api/admin/reports/$reportId/resolve", body)
    }

    suspend fun banUser(userId: Long, duration: String, reason: String): JSONObject {
        val body = JSONObject()
            .put("duration", duration)
            .put("reason", reason)
        return request("POST", "/api/admin/users/$userId/ban", body)
    }

    suspend fun unbanUser(userId: Long): JSONObject {
        return request("POST", "/api/admin/users/$userId/unban", JSONObject())
    }

    suspend fun warnUser(userId: Long, reason: String): JSONObject {
        return request("POST", "/api/admin/users/$userId/warn", JSONObject().put("reason", reason))
    }

    suspend fun getFeedback(status: String = "new", page: Int = 0): List<Feedback> {
        val json = request("GET", "/api/admin/feedback?status=$status&page=$page")
        val items = json.getJSONArray("feedback")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(item.toFeedback())
            }
        }
    }

    suspend fun resolveFeedback(feedbackId: Long, response: String? = null): JSONObject {
        val body = JSONObject()
        if (response != null) body.put("response", response)
        return request("POST", "/api/admin/feedback/$feedbackId/resolve", body)
    }

    suspend fun getAdminStats(): AdminStats {
        val json = request("GET", "/api/admin/stats")
        return AdminStats(
            totalUsers = json.optInt("totalUsers", 0),
            activeUsers = json.optInt("activeUsers", 0),
            totalMessages = json.optInt("totalMessages", 0),
            totalCharacters = json.optInt("totalCharacters", 0),
            totalPosts = json.optInt("totalPosts", 0),
            premiumUsers = json.optInt("premiumUsers", 0),
            piecesDistributed = json.optInt("piecesDistributed", 0)
        )
    }

    // ========== Poll Features ==========

    suspend fun createPoll(postId: Long, question: String, options: List<String>, endsAt: String? = null): Long {
        val body = JSONObject()
            .put("question", question)
            .put("options", JSONArray(options))
        if (endsAt != null) body.put("endsAt", endsAt)
        return request("POST", "/api/posts/$postId/poll", body).getLong("id")
    }

    suspend fun votePoll(pollId: Long, optionIndex: Int): JSONObject {
        return request("POST", "/api/posts/$pollId/vote", JSONObject().put("optionIndex", optionIndex))
    }

    suspend fun getPollResults(postId: Long): JSONObject {
        return request("GET", "/api/posts/$postId/poll-results")
    }

    suspend fun getProfile(): UserProfile {
        val json = request("GET", "/api/profile")
        return UserProfile(
            id = json.optLong("id").takeIf { it != 0L },
            userId = json.optLong("userId").takeIf { it != 0L },
            displayName = json.optString("displayName", ""),
            bio = json.optString("bio", ""),
            avatarUrl = json.optString("avatarUrl").ifBlank { null },
            badgeType = json.optString("badgeType").ifBlank { null },
            role = json.optString("role", "user"),
            createdAt = json.optString("createdAt"),
            equippedTheme = json.optString("equippedTheme").ifBlank { null },
            equippedTitle = json.optString("equippedTitle").ifBlank { null },
            equippedFrame = json.optString("equippedFrame").ifBlank { null },
            equippedEffect = json.optString("equippedEffect").ifBlank { null }
        )
    }

    suspend fun updateProfile(displayName: String, bio: String): UserProfile {
        val json = request("PUT", "/api/profile", JSONObject().put("displayName", displayName).put("bio", bio))
        return UserProfile(
            displayName = json.optString("displayName", ""),
            bio = json.optString("bio", ""),
            avatarUrl = json.optString("avatarUrl").ifBlank { null }
        )
    }

    suspend fun uploadAvatar(context: Context, uri: Uri): String = uploadImage(context, "/api/profile/avatar", uri)

    suspend fun uploadCharacterAvatar(context: Context, characterId: Long, uri: Uri): String =
        uploadImage(context, "/api/characters/$characterId/avatar", uri)

    private suspend fun uploadImage(context: Context, path: String, uri: Uri): String = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Impossible de lire l’image sélectionnée.")
        val mimeType = context.contentResolver.getType(uri)?.takeIf { it in setOf("image/jpeg", "image/png", "image/webp") }
            ?: "image/jpeg"
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("avatar", "avatar", bytes.toRequestBody(mimeType.toMediaType()))
            .build()
        val id = checkNotNull(deviceId) { "ApiClient.init(context) must be called first." }
        val freshToken = try {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        } catch (_: Exception) {
            null
        } ?: firebaseIdToken
        val requestBuilder = Request.Builder().url("$BASE$path").header("x-device-id", id).post(body)
        if (!freshToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $freshToken")
        }
        client.newCall(requestBuilder.build()).execute().use { response ->
            var finalResponse = response
            if (finalResponse.code == 401) {
                val newToken = try {
                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
                } catch (_: Exception) { null }
                if (!newToken.isNullOrBlank()) {
                    firebaseIdToken = newToken
                    requestBuilder.header("Authorization", "Bearer $newToken")
                    finalResponse.close()
                    finalResponse = client.newCall(requestBuilder.build()).execute()
                }
            }
            val json = JSONObject(finalResponse.body?.string().orEmpty())
            if (!finalResponse.isSuccessful) throw IOException(json.optString("error", "Erreur HTTP ${finalResponse.code}"))
            json.optString("avatar_url").ifBlank { json.optString("avatarUrl") }
        }
    }

    suspend fun getShop(): List<com.chatile.ShopItem> {
        val json = request("GET", "/api/shop").getJSONArray("items")
        return buildList {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(com.chatile.ShopItem(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    description = item.optString("description", ""),
                    price = item.optInt("pieces", 0),
                    priceLabel = item.optString("price", ""),
                    type = item.getString("type"),
                    productId = item.getString("productId"),
                    isPopular = item.optBoolean("isPopular", false),
                    benefits = item.optJSONArray("benefits")?.toStringList() ?: emptyList()
                ))
            }
        }
    }

    suspend fun verifyPurchase(token: String, productId: String): MeResult {
        val json = request(
        "POST",
        "/api/verify-purchase",
        JSONObject().put("purchaseToken", token).put("productId", productId),
        )
        return MeResult(json.getInt("pieces"), false, json.optString("subscription_tier", "free"), null, 50, 0, true, "violet")
    }

    private suspend fun request(method: String, path: String, payload: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val id = deviceId ?: "anonymous"
            val requestBuilder = Request.Builder()
                .url("$BASE$path")
                .header("x-device-id", id)
                .header("Accept", "application/json")
            val currentToken = try {
                FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
            } catch (e: Exception) {
                null
            } ?: firebaseIdToken
            currentToken?.takeIf { it.isNotBlank() }?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }
            
            val body = payload?.toString()?.toRequestBody(jsonMediaType)
            when (method.uppercase()) {
                "POST" -> requestBuilder.post(body ?: "".toRequestBody(jsonMediaType))
                "PUT" -> requestBuilder.put(body ?: "".toRequestBody(jsonMediaType))
                "DELETE" -> if (body != null) requestBuilder.delete(body) else requestBuilder.delete()
                else -> requestBuilder.get()
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                var finalResponse = response
                var responseText = finalResponse.body?.string().orEmpty()

                if (finalResponse.code == 401) {
                    val newToken = try {
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
                    } catch (e: Exception) {
                        null
                    }
                    if (!newToken.isNullOrBlank()) {
                        firebaseIdToken = newToken
                        requestBuilder.header("Authorization", "Bearer $newToken")
                        finalResponse.close()
                        finalResponse = client.newCall(requestBuilder.build()).execute()
                        responseText = finalResponse.body?.string().orEmpty()
                    }

                    if (finalResponse.code == 401) {
                        try {
                            FirebaseAuth.getInstance().signOut()
                        } catch (_: Exception) {}
                        firebaseIdToken = null
                        throw IOException("Erreur : Token invalide. Essaie de te reconnecter.")
                    }
                }

                val json = try {
                    if (responseText.isBlank()) JSONObject() else JSONObject(responseText)
                } catch (e: Exception) {
                    // Si la réponse n'est pas du JSON (ex: HTML error page)
                    val errorMsg = if (responseText.contains("<!doctype") || responseText.contains("<html")) {
                        "Server returned HTML (is the backend deployed correctly? URL: $BASE$path)"
                    } else {
                        "Invalid server response (expected JSON, got: ${responseText.take(200)})"
                    }
                    throw IOException("$errorMsg\nHTTP ${finalResponse.code}")
                }
                if (!finalResponse.isSuccessful) {
                    throw IOException(json.optString("error", "Erreur HTTP ${finalResponse.code}"))
                }
                json
            }
        }

    private fun JSONObject.toCharacter() = com.chatile.Character(
        id = getLong("id"),
        userId = optLong("user_id").takeIf { it != 0L },
        name = getString("name"),
        systemPrompt = optString("system_prompt", optString("systemPrompt")),
        greeting = optString("greeting", "Salut !"),
        avatarUrl = optString("avatar_url").ifBlank { null },
        isNsfw = optInt("is_nsfw", optInt("isNsfw")) != 0,
        isSystem = optInt("is_system") != 0,
        isPublic = optInt("is_public") != 0,
        likesCount = optInt("likes_count"),
        scenarioGoal = optString("scenario_goal").ifBlank { null },
        scenarioSteps = optString("scenario_steps").ifBlank { null },
        boundaries = optString("boundaries").ifBlank { null },
        appearance = optString("appearance").ifBlank { null },
        ttsVoice = optString("tts_voice").ifBlank { null },
        bannedWords = optString("banned_words").ifBlank { null },
        proactiveEnabled = optInt("proactive_enabled") != 0,
        lastProactiveMessage = optString("last_proactive_message").ifBlank { null },
        silentModeEnabled = optInt("silent_mode_enabled") != 0,
        expiresAt = optString("expires_at").ifBlank { null },
        relationshipLevel = optInt("relationship_level"),
        lastMood = optString("last_mood", "neutral"),
        createdAt = optString("created_at"),
        lastMessageAt = optString("last_message_at").ifBlank { null },
        messageCount = optInt("message_count", 0),
    )

    private fun JSONObject.toPublicCharacter() = com.chatile.PublicCharacter(
        id = getLong("id"),
        name = getString("name"),
        greeting = optString("greeting", "Salut !"),
        avatarUrl = optString("avatar_url").ifBlank { optString("avatarUrl").ifBlank { null } },
        isNsfw = optBoolean("isNsfw", optInt("is_nsfw") != 0),
        likesCount = optInt("likesCount", optInt("likes_count")),
        creatorId = optLong("creatorId", optLong("creator_id")),
        creatorName = optString("creatorName").ifBlank { optString("creator_name", "Utilisateur") },
        creatorBadge = optString("creatorBadge").ifBlank { optString("creator_badge").ifBlank { null } },
        scenarioGoal = optString("scenarioGoal").ifBlank { optString("scenario_goal").ifBlank { null } },
        description = optString("description").ifBlank { null },
        createdAt = optString("createdAt", optString("created_at")),
        isImported = optBoolean("isImported", optInt("is_imported") != 0),
        isLikedByCurrentUser = optBoolean("isLikedByCurrentUser", optInt("is_liked_by_current_user") != 0),
    )

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (index in 0 until length()) add(getString(index))
    }

    private fun JSONObject.toPost(): com.chatile.Post {
        val user = optJSONObject("user")
        val character = optJSONObject("character")
        return com.chatile.Post(
            id = optLong("id"),
            userId = optLong("user_id"),
            postType = optString("post_type", ""),
            postSubtype = optString("post_subtype").ifBlank { null },
            content = optString("content", ""),
            characterId = optLong("character_id").takeIf { it != 0L },
            imageUrl = optString("image_url").ifBlank { null },
            visibility = optString("visibility", "public"),
            likesCount = optInt("likes_count"),
            commentsCount = optInt("comments_count"),
            sharesCount = optInt("shares_count"),
            isPinnedGlobal = optInt("is_pinned_global") != 0,
            createdAt = optString("created_at"),
            authorDisplayName = user?.optString("display_name"),
            authorAvatarUrl = user?.optString("avatar_url"),
            authorUsername = user?.optString("username"),
            authorBadgeType = user?.optString("badge_type"),
            characterName = character?.optString("name"),
            characterAvatarUrl = character?.optString("avatar_url"),
            isLikedByMe = optInt("is_liked_by_me") != 0
        )
    }

    private fun JSONObject.toComment(): com.chatile.Comment {
        val user = optJSONObject("user")
        return com.chatile.Comment(
            id = optLong("id"),
            postId = optLong("post_id"),
            userId = optLong("user_id"),
            content = optString("content", ""),
            parentCommentId = optLong("parent_comment_id").takeIf { it != 0L },
            likesCount = optInt("likes_count"),
            createdAt = optString("created_at"),
            authorDisplayName = user?.optString("display_name"),
            authorAvatarUrl = user?.optString("avatar_url"),
            authorUsername = user?.optString("username"),
            authorBadgeType = user?.optString("badge_type"),
            isLikedByMe = optInt("is_liked_by_me") != 0
        )
    }

    private fun JSONObject.toNotification(): com.chatile.Notification {
        val actor = optJSONObject("actor")
        return com.chatile.Notification(
            id = optLong("id"),
            userId = optLong("user_id"),
            type = optString("type", ""),
            actorId = optLong("actor_id").takeIf { it != 0L },
            targetType = optString("target_type").ifBlank { null },
            targetId = optLong("target_id").takeIf { it != 0L },
            read = optInt("read") != 0,
            createdAt = optString("created_at"),
            actorDisplayName = actor?.optString("display_name"),
            actorAvatarUrl = actor?.optString("avatar_url"),
            actorUsername = actor?.optString("username"),
            actorBadgeType = actor?.optString("badge_type"),
            postType = optString("post_type").ifBlank { null },
            postContent = optString("post_content").ifBlank { null }
        )
    }

    private fun JSONObject.toSearchResult(): com.chatile.SearchResult {
        return com.chatile.SearchResult(
            type = optString("type", ""),
            id = optLong("id").takeIf { it != 0L },
            name = optString("name", optString("username", "")),
            avatarUrl = optString("avatar_url").ifBlank { null },
            username = optString("username").ifBlank { null },
            description = optString("description").ifBlank { optString("bio").ifBlank { null } }
        )
    }

    private fun JSONObject.toUserProfile(): com.chatile.UserProfile {
        return com.chatile.UserProfile(
            id = optLong("id"),
            userId = optLong("user_id", optLong("userId")),
            displayName = optString("displayName", optString("display_name", "")),
            bio = optString("bio", ""),
            avatarUrl = optString("avatarUrl", optString("avatar_url")).ifBlank { null },
            badgeType = optString("badgeType", optString("badge_type")).ifBlank { null },
            equippedTheme = optString("equippedTheme").ifBlank { null },
            equippedTitle = optString("equippedTitle").ifBlank { null },
            equippedFrame = optString("equippedFrame").ifBlank { null },
            equippedEffect = optString("equippedEffect").ifBlank { null },
            createdAt = optString("createdAt", optString("created_at"))
        )
    }

    private fun JSONObject.toReport(): com.chatile.Report {
        return com.chatile.Report(
            id = optLong("id"),
            reporterId = optLong("reporter_id"),
            targetType = optString("target_type", ""),
            targetId = optLong("target_id"),
            reason = optString("reason", ""),
            status = optString("status", "pending"),
            createdAt = optString("created_at")
        )
    }

    private fun JSONObject.toFeedback(): com.chatile.Feedback {
        return com.chatile.Feedback(
            id = optLong("id"),
            userId = optLong("user_id"),
            category = optString("category", ""),
            content = optString("content", ""),
            contactEmail = optString("contact_email").ifBlank { null },
            status = optString("status", "new"),
            createdAt = optString("created_at")
        )
    }
}

// ApiClient-specific response types (not in DataModels)
data class MeResult(
    val pieces: Int,
    val nsfwEnabled: Boolean,
    val subscriptionTier: String,
    val subscriptionExpiresAt: String?,
    val messagesUntilNextAd: Int,
    val streakDays: Int,
    val tutorialCompleted: Boolean,
    val theme: String,
    val badgeType: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val canClaimDaily: Boolean = false,
    val nextDailyClaimAt: String? = null,
    val canOpenMysteryBox: Boolean = false,
    val nextMysteryBoxAt: String? = null,
    val canWatchAd: Boolean = false,
    val nextWatchAdAt: String? = null,
)

data class DailyClaimResult(
    val success: Boolean,
    val pieces: Int,
    val nextClaimAt: String?,
    val streakDays: Int,
    val reward: Int,
    val error: String? = null,
    val message: String? = null,
)

data class DailyStatus(val canClaim: Boolean, val nextClaimAt: String?, val streakDays: Int, val todayReward: Int)

data class MemoryResult(val seedMemory: List<String>, val learnedMemory: List<String>)
