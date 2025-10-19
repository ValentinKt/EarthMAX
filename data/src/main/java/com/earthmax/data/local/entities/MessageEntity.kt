package com.earthmax.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.earthmax.data.model.Message
import com.earthmax.data.model.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val eventId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val content: String,
    val timestamp: Long,
    val messageType: String = MessageType.TEXT.name,
    val isRead: Boolean = false,
    val replyToMessageId: String? = null,
    val isSynced: Boolean = false
)

fun MessageEntity.toMessage(): Message {
    return Message(
        id = id,
        eventId = eventId,
        senderId = senderId,
        senderName = senderName,
        senderAvatarUrl = senderAvatarUrl,
        content = content,
        timestamp = timestamp,
        messageType = MessageType.valueOf(messageType),
        isRead = isRead,
        replyToMessageId = replyToMessageId
    )
}

fun Message.toEntity(isSynced: Boolean = true): MessageEntity {
    return MessageEntity(
        id = id,
        eventId = eventId,
        senderId = senderId,
        senderName = senderName,
        senderAvatarUrl = senderAvatarUrl,
        content = content,
        timestamp = timestamp,
        messageType = messageType.name,
        isRead = isRead,
        replyToMessageId = replyToMessageId,
        isSynced = isSynced
    )
}