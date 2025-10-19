package com.earthmax.data.chat

import android.util.Log
import com.earthmax.core.network.SupabaseClient
import com.earthmax.core.utils.Logger
import com.earthmax.data.model.Message
import com.earthmax.data.model.MessageType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MessageDto(
    val id: String,
    val event_id: String,
    val sender_id: String,
    val sender_name: String,
    val sender_avatar_url: String? = null,
    val content: String,
    val timestamp: Long,
    val message_type: String = MessageType.TEXT.name,
    val is_read: Boolean = false,
    val reply_to_message_id: String? = null,
    val created_at: String? = null
)

@Singleton
class SupabaseChatRepository @Inject constructor() {
    
    private val supabase = SupabaseClient.client
    
    companion object {
        private const val TAG = "SupabaseChatRepository"
    }
    
    suspend fun sendMessage(
        eventId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String?,
        content: String,
        messageType: MessageType = MessageType.TEXT,
        replyToMessageId: String? = null
    ): Result<Message> {
        Logger.enter(TAG, "sendMessage", mapOf(
            "eventId" to Logger.maskSensitiveData(eventId),
            "senderId" to Logger.maskSensitiveData(senderId),
            "senderName" to senderName,
            "messageType" to messageType.name,
            "contentLength" to content.length.toString()
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            val messageDto = MessageDto(
                id = UUID.randomUUID().toString(),
                event_id = eventId,
                sender_id = senderId,
                sender_name = senderName,
                sender_avatar_url = senderAvatarUrl,
                content = content,
                timestamp = System.currentTimeMillis(),
                message_type = messageType.name,
                is_read = false,
                reply_to_message_id = replyToMessageId
            )
            
            val response = supabase.from("messages")
                .insert(messageDto)
                .decodeSingle<MessageDto>()
            
            val message = Message(
                id = response.id,
                eventId = response.event_id,
                senderId = response.sender_id,
                senderName = response.sender_name,
                senderAvatarUrl = response.sender_avatar_url,
                content = response.content,
                timestamp = response.timestamp,
                messageType = MessageType.valueOf(response.message_type),
                isRead = response.is_read,
                replyToMessageId = response.reply_to_message_id
            )
            
            Logger.logPerformance(
                TAG,
                "sendMessage",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "messageId" to Logger.maskSensitiveData(response.id),
                    "messageType" to messageType.name,
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "message_sent",
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "messageId" to Logger.maskSensitiveData(response.id),
                    "senderId" to Logger.maskSensitiveData(senderId),
                    "messageType" to messageType.name,
                    "contentLength" to content.length.toString(),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "sendMessage")
            Result.success(message)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "sendMessage",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "senderId" to Logger.maskSensitiveData(senderId),
                    "messageType" to messageType.name,
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to send message", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "senderId" to Logger.maskSensitiveData(senderId),
                "messageType" to messageType.name,
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "sendMessage")
            Result.failure(e)
        }
    }
    
    suspend fun getMessages(eventId: String, limit: Int = 50): Result<List<Message>> {
        Logger.enter(TAG, "getMessages", mapOf(
            "eventId" to Logger.maskSensitiveData(eventId)
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            val response = supabase.from("messages")
                .select {
                    filter {
                        eq("event_id", eventId)
                    }
                }
                .decodeList<MessageDto>()
            
            val messages = response.map { messageDto -> messageDto.toMessage() }
            
            Logger.logPerformance(
                TAG,
                "getMessages",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "messageCount" to messages.size.toString(),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "messages_retrieved",
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "messageCount" to messages.size.toString(),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "getMessages")
            Result.success(messages)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "getMessages",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to get messages", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "getMessages")
            Result.failure(e)
        }
    }
    
    fun subscribeToMessages(eventId: String): Flow<Message> = flow {
        // For now, return empty flow - realtime will be implemented later
        // This allows the app to compile and run without realtime functionality
    }
    
    suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        Logger.enter(TAG, "markMessageAsRead", mapOf(
            "messageId" to Logger.maskSensitiveData(messageId)
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            supabase.from("messages")
                .update(mapOf("is_read" to true)) {
                    filter {
                        eq("id", messageId)
                    }
                }
            
            Logger.logPerformance(
                TAG,
                "markMessageAsRead",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "message_marked_read",
                mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "markMessageAsRead")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "markMessageAsRead",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to mark message as read", e, mapOf(
                "messageId" to Logger.maskSensitiveData(messageId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "markMessageAsRead")
            Result.failure(e)
        }
    }
    
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        Logger.enter(TAG, "deleteMessage", mapOf(
            "messageId" to Logger.maskSensitiveData(messageId)
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            supabase.from("messages")
                .delete {
                    filter {
                        eq("id", messageId)
                    }
                }
            
            Logger.logPerformance(
                TAG,
                "deleteMessage",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "success" to "true"
                )
            )
            
            Logger.logBusinessEvent(
                TAG,
                "message_deleted",
                mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "source" to "supabase"
                )
            )
            
            Logger.exit(TAG, "deleteMessage")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.logPerformance(
                TAG,
                "deleteMessage",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "success" to "false",
                    "errorType" to e::class.simpleName.toString()
                )
            )
            
            Logger.e(TAG, "Failed to delete message", e, mapOf(
                "messageId" to Logger.maskSensitiveData(messageId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "deleteMessage")
            Result.failure(e)
        }
    }
}

private fun MessageDto.toMessage(): Message {
    return Message(
        id = id,
        eventId = event_id,
        senderId = sender_id,
        senderName = sender_name,
        senderAvatarUrl = sender_avatar_url,
        content = content,
        timestamp = timestamp,
        messageType = MessageType.valueOf(message_type),
        isRead = is_read,
        replyToMessageId = reply_to_message_id
    )
}