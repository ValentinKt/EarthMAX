package com.earthmax.data.chat

import com.earthmax.core.utils.Logger
import com.earthmax.data.local.dao.MessageDao
import com.earthmax.data.local.entities.MessageEntity
import com.earthmax.data.local.entities.toMessage
import com.earthmax.data.local.entities.toEntity
import com.earthmax.data.model.Message
import com.earthmax.data.model.MessageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    suspend fun sendMessage(
        eventId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String?,
        content: String,
        messageType: MessageType = MessageType.TEXT,
        replyToMessageId: String? = null
    ): Result<Message>
    
    suspend fun getMessages(eventId: String): Flow<List<Message>>
    fun subscribeToMessages(eventId: String): Flow<Message>
    suspend fun markMessageAsRead(messageId: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun getUnreadMessageCount(eventId: String): Int
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val supabaseChatRepository: SupabaseChatRepository,
    private val messageDao: MessageDao
) : ChatRepository {
    
    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }
    
    override suspend fun sendMessage(
        eventId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String?,
        content: String,
        messageType: MessageType,
        replyToMessageId: String?
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
            val result = supabaseChatRepository.sendMessage(
                eventId = eventId,
                senderId = senderId,
                senderName = senderName,
                senderAvatarUrl = senderAvatarUrl,
                content = content,
                messageType = messageType,
                replyToMessageId = replyToMessageId
            )
            
            // Cache message locally
            result.getOrNull()?.let { message ->
                messageDao.insertMessage(message.toEntity())
            }
            
            Logger.logPerformance(
                TAG,
                "sendMessage",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "senderId" to Logger.maskSensitiveData(senderId),
                    "messageType" to messageType.name,
                    "success" to result.isSuccess.toString(),
                    "cached" to (result.getOrNull() != null).toString()
                )
            )
            
            if (result.isSuccess) {
                Logger.logBusinessEvent(
                    TAG,
                    "message_sent_and_cached",
                    mapOf(
                        "eventId" to Logger.maskSensitiveData(eventId),
                        "senderId" to Logger.maskSensitiveData(senderId),
                        "messageType" to messageType.name,
                        "contentLength" to content.length.toString(),
                        "source" to "hybrid"
                    )
                )
            } else {
                Logger.e(TAG, "Failed to send message", null, mapOf(
                    "eventId" to Logger.maskSensitiveData(eventId),
                    "senderId" to Logger.maskSensitiveData(senderId),
                    "messageType" to messageType.name,
                    "errorType" to "SendMessageFailure"
                ))
            }
            
            Logger.exit(TAG, "sendMessage")
            result
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
            
            Logger.e(TAG, "Exception in sendMessage", e, mapOf(
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
    
    override suspend fun getMessages(eventId: String): Flow<List<Message>> {
        Logger.enter(TAG, "getMessages", mapOf(
            "eventId" to Logger.maskSensitiveData(eventId)
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            // Combine remote and local messages
            val remoteMessages = supabaseChatRepository.getMessages(eventId)
            val localMessages = messageDao.getMessagesByEventId(eventId)
            
            val combinedFlow = combine(remoteMessages, localMessages) { remote, local ->
                val remoteList = remote.getOrElse { emptyList() }
                val localList = local.map { it.toMessage() }
                
                // Merge and deduplicate messages
                val allMessages = (remoteList + localList)
                    .distinctBy { it.id }
                    .sortedBy { it.timestamp }
                
                Logger.logPerformance(
                    TAG,
                    "getMessages",
                    System.currentTimeMillis() - startTime,
                    mapOf(
                        "eventId" to Logger.maskSensitiveData(eventId),
                        "remoteCount" to remoteList.size.toString(),
                        "localCount" to localList.size.toString(),
                        "totalCount" to allMessages.size.toString(),
                        "success" to "true"
                    )
                )
                
                Logger.logBusinessEvent(
                    TAG,
                    "messages_retrieved_hybrid",
                    mapOf(
                        "eventId" to Logger.maskSensitiveData(eventId),
                        "remoteCount" to remoteList.size.toString(),
                        "localCount" to localList.size.toString(),
                        "totalCount" to allMessages.size.toString(),
                        "source" to "hybrid"
                    )
                )
                
                allMessages
            }
            
            Logger.exit(TAG, "getMessages")
            combinedFlow
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
            
            Logger.e(TAG, "Exception in getMessages", e, mapOf(
                "eventId" to Logger.maskSensitiveData(eventId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "getMessages")
            flow { emit(emptyList<Message>()) }
        }
    }
    
    override fun subscribeToMessages(eventId: String): Flow<Message> {
        return supabaseChatRepository.subscribeToMessages(eventId)
    }
    
    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        Logger.enter(TAG, "markMessageAsRead", mapOf(
            "messageId" to Logger.maskSensitiveData(messageId)
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = supabaseChatRepository.markMessageAsRead(messageId)
            
            Logger.logPerformance(
                TAG,
                "markMessageAsRead",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "success" to result.isSuccess.toString()
                )
            )
            
            if (result.isSuccess) {
                Logger.logBusinessEvent(
                    TAG,
                    "message_marked_as_read",
                    mapOf(
                        "messageId" to Logger.maskSensitiveData(messageId),
                        "source" to "remote"
                    )
                )
            } else {
                Logger.e(TAG, "Failed to mark message as read", null, mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "errorType" to "MarkAsReadFailure"
                ))
            }
            
            Logger.exit(TAG, "markMessageAsRead")
            result
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
            
            Logger.e(TAG, "Exception in markMessageAsRead", e, mapOf(
                "messageId" to Logger.maskSensitiveData(messageId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "markMessageAsRead")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        Logger.enter(TAG, "deleteMessage", mapOf(
            "messageId" to Logger.maskSensitiveData(messageId)
        ))
        val startTime = System.currentTimeMillis()
        
        return try {
            val result = supabaseChatRepository.deleteMessage(messageId)
            
            // Delete from local cache if remote deletion was successful
            if (result.isSuccess) {
                messageDao.deleteMessage(messageId)
            }
            
            Logger.logPerformance(
                TAG,
                "deleteMessage",
                System.currentTimeMillis() - startTime,
                mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "success" to result.isSuccess.toString(),
                    "localDeleted" to result.isSuccess.toString()
                )
            )
            
            if (result.isSuccess) {
                Logger.logBusinessEvent(
                    TAG,
                    "message_deleted_hybrid",
                    mapOf(
                        "messageId" to Logger.maskSensitiveData(messageId),
                        "source" to "hybrid"
                    )
                )
            } else {
                Logger.e(TAG, "Failed to delete message", null, mapOf(
                    "messageId" to Logger.maskSensitiveData(messageId),
                    "errorType" to "DeleteMessageFailure"
                ))
            }
            
            Logger.exit(TAG, "deleteMessage")
            result
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
            
            Logger.e(TAG, "Exception in deleteMessage", e, mapOf(
                "messageId" to Logger.maskSensitiveData(messageId),
                "errorType" to e::class.simpleName.toString(),
                "errorMessage" to e.message.toString()
            ))
            
            Logger.exit(TAG, "deleteMessage")
            Result.failure(e)
        }
    }
    
    override suspend fun getUnreadMessageCount(eventId: String): Int {
        // Note: MessageDao requires currentUserId parameter
        // For now, return 0 until we have proper user context
        return 0
    }
}