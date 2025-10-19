package com.earthmax.data.model

import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Message(
    val id: String,
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val content: String,
    val timestamp: Long,
    val messageType: MessageType = MessageType.TEXT,
    val isRead: Boolean = false,
    val replyToMessageId: String? = null
)

@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    SYSTEM
}

@Serializable
data class ChatRoom(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val participantCount: Int,
    val lastMessage: Message? = null,
    val lastActivity: Long,
    val isActive: Boolean = true
)

@Serializable
data class ChatParticipant(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val joinedAt: Long,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null
)