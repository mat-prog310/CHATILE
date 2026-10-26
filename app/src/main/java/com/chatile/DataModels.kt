package com.chatile

import java.time.Instant

// User and Profile models
data class User(
    val id: Long? = null,
    val deviceId: String? = null,
    val username: String? = null,
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val pieces: Int = 1000,
    val nsfwEnabled: Boolean = false,
    val subscriptionTier: String = "free",
    val subscriptionExpiresAt: String? = null,
    val messagesSinceLastAd: Int = 0,
    val badgeType: String? = null,
    val role: String = "user",
    val isVerified: Boolean = false,
    val isSystemAccount: Boolean = false,
    val bannedUntil: String? = null,
    val streakDays: Int = 0,
    val tutorialCompleted: Boolean = false,
    val theme: String = "violet",
    val createdAt: String? = null
)

data class UserProfile(
    val id: Long? = null,
    val userId: Long? = null,
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val badgeType: String? = null,
    val username: String? = null,
    val role: String = "user",
    val isCurrentUser: Boolean = false,
    val isFollowing: Boolean = false,
    val createdAt: String? = null
)

// Character models
data class Character(
    val id: Long? = null,
    val userId: Long? = null,
    val name: String = "",
    val systemPrompt: String = "",
    val greeting: String = "Salut !",
    val avatarUrl: String? = null,
    val isNsfw: Boolean = false,
    val isSystem: Boolean = false,
    val isPublic: Boolean = false,
    val likesCount: Int = 0,
    val scenarioGoal: String? = null,
    val scenarioSteps: String? = null,
    val boundaries: String? = null,
    val appearance: String? = null,
    val ttsVoice: String? = null,
    val bannedWords: String? = null,
    val proactiveEnabled: Boolean = true,
    val lastProactiveMessage: String? = null,
    val silentModeEnabled: Boolean = false,
    val expiresAt: String? = null,
    val relationshipLevel: Int = 0,
    val lastMood: String? = null,
    val createdAt: String? = null
)

data class CharacterMemory(
    val seedMemory: List<String> = emptyList(),
    val learnedMemory: List<String> = emptyList(),
    val pinned: List<String> = emptyList(),
    val structured: List<StructuredMemoryItem> = emptyList()
)

data class StructuredMemoryItem(
    val category: String = "",
    val key: String = "",
    val value: String = "",
    val confidence: Double = 0.5
)

// Message models
data class Message(
    val id: Long? = null,
    val userId: Long? = null,
    val characterId: Long? = null,
    val role: String = "", // 'user' or 'assistant'
    val content: String = "",
    val isFavorite: Boolean = false,
    val createdAt: String? = null
)

data class ChatResult(
    val reply: String = "",
    val pieces: Int = 0,
    val relationshipLevel: Int = 0,
    val lastMood: String = "neutral",
    val userMessageId: Long? = null,
    val assistantMessageId: Long? = null,
    val suggestions: List<String> = emptyList(),
    val showInterstitial: Boolean = false
)

// Social models
data class Post(
    val id: Long? = null,
    val userId: Long? = null,
    val postType: String = "",
    val postSubtype: String? = null,
    val content: String = "",
    val characterId: Long? = null,
    val imageUrl: String? = null,
    val visibility: String = "public",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isPinnedGlobal: Boolean = false,
    val createdAt: String? = null,
    // Denormalized fields
    val authorDisplayName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorUsername: String? = null,
    val authorBadgeType: String? = null,
    val characterName: String? = null,
    val characterAvatarUrl: String? = null,
    val isLikedByMe: Boolean = false
)

data class Comment(
    val id: Long? = null,
    val postId: Long? = null,
    val userId: Long? = null,
    val content: String = "",
    val parentCommentId: Long? = null,
    val likesCount: Int = 0,
    val createdAt: String? = null,
    // Denormalized fields
    val authorDisplayName: String? = null,
    val authorAvatarUrl: String? = null,
    val authorUsername: String? = null,
    val authorBadgeType: String? = null,
    val isLikedByMe: Boolean = false
)

data class Notification(
    val id: Long? = null,
    val userId: Long? = null,
    val type: String = "",
    val actorId: Long? = null,
    val targetType: String? = null,
    val targetId: Long? = null,
    val read: Boolean = false,
    val createdAt: String? = null,
    // Denormalized fields
    val actorDisplayName: String? = null,
    val actorAvatarUrl: String? = null,
    val actorUsername: String? = null,
    val actorBadgeType: String? = null,
    val postType: String? = null,
    val postContent: String? = null
)

data class Poll(
    val id: Long? = null,
    val postId: Long? = null,
    val question: String = "",
    val options: List<String> = emptyList(),
    val endsAt: String? = null,
    val totalVotes: Int = 0,
    val results: List<PollResult> = emptyList(),
    val ended: Boolean = false
)

data class PollResult(
    val option: String = "",
    val votes: Int = 0
)

// Shop models
data class ShopItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Int = 0, // in Pièces or cents
    val priceLabel: String = "",
    val type: String = "", // 'product' or 'subscription'
    val productId: String = "",
    val isPopular: Boolean = false,
    val benefits: List<String> = emptyList()
)

// Stats models
data class UserStats(
    val totalMessages: Int = 0,
    val messagesByCharacter: List<CharacterStats> = emptyList(),
    val preferredCharacter: String? = null,
    val streakDays: Int = 0,
    val totalPiecesEarned: Int = 0,
    val totalPiecesSpent: Int = 0
)

data class CharacterStats(
    val characterId: Long? = null,
    val characterName: String = "",
    val messageCount: Int = 0,
    val lastInteraction: String? = null
)

// Quest models
data class Quest(
    val id: Long? = null,
    val title: String = "",
    val description: String = "",
    val target: Int = 0,
    val reward: Int = 0,
    val type: String = "",
    val progress: Int = 0,
    val claimed: Boolean = false,
    val weekStart: String? = null
)


// Search models
data class SearchResult(
    val type: String = "", // 'user', 'post', 'character'
    val id: Long? = null,
    val name: String = "",
    val avatarUrl: String? = null,
    val username: String? = null,
    val description: String? = null
)

// Feedback models
data class FeedbackCategory(
    val id: String = "",
    val name: String = ""
)

data class Report(
    val id: Long? = null,
    val reporterId: Long? = null,
    val targetType: String = "",
    val targetId: Long? = null,
    val reason: String = "",
    val status: String = "pending",
    val createdAt: String? = null
)

data class Feedback(
    val id: Long? = null,
    val userId: Long? = null,
    val category: String = "",
    val content: String = "",
    val contactEmail: String? = null,
    val status: String = "new",
    val createdAt: String? = null
)

// Reward models
data class Reward(
    val action: String = "",
    val amount: Int = 0,
    val claimed: Boolean = false,
    val claimedAt: String? = null
)

// Transaction models
data class PieceTransaction(
    val id: Long? = null,
    val userId: Long? = null,
    val amount: Int = 0,
    val type: String = "",
    val description: String = "",
    val createdAt: String? = null
)

// Referral models
data class ReferralStats(
    val code: String = "",
    val referrals: Int = 0,
    val piecesEarned: Int = 0
)

// Daily claim models
data class DailyClaimStatus(
    val canClaim: Boolean = false,
    val nextClaimAt: String? = null,
    val streakDays: Int = 0,
    val todayReward: Int = 0
)

// Retrospective models
data class WeeklyRetrospective(
    val totalMessages: Int = 0,
    val charactersInteracted: List<String> = emptyList(),
    val topCharacter: String? = null,
    val piecesEarned: Int = 0,
    val piecesSpent: Int = 0,
    val questsCompleted: List<String> = emptyList(),
    val streakMaintained: Boolean = false,
    val newCharactersCreated: Int = 0
)

// Widget models
data class WidgetData(
    val pieces: Int = 0,
    val dailyCanClaim: Boolean = false,
    val characterOfTheDay: Character? = null
)

// Admin models
data class AdminStats(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val totalMessages: Int = 0,
    val totalCharacters: Int = 0,
    val totalPosts: Int = 0,
    val premiumUsers: Int = 0,
    val piecesDistributed: Int = 0
)

// Public characters models
data class PublicCharacter(
    val id: Long,
    val name: String,
    val greeting: String,
    val avatarUrl: String?,
    val isNsfw: Boolean,
    val likesCount: Int,
    val creatorId: Long,
    val creatorName: String,
    val creatorBadge: String?,
    val scenarioGoal: String?,
    val description: String?,
    val createdAt: String,
    val isImported: Boolean,
    val isLikedByCurrentUser: Boolean = false,
)
