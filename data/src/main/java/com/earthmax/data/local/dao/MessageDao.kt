package com.earthmax.data.local.dao

import androidx.room.*
import com.earthmax.data.local.entities.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages WHERE eventId = :eventId ORDER BY timestamp ASC")
    fun getMessagesByEventId(eventId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE eventId = :eventId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(eventId: String, limit: Int = 50): List<MessageEntity>
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
    
    @Query("SELECT * FROM messages WHERE isSynced = 0")
    suspend fun getUnsyncedMessages(): List<MessageEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Query("UPDATE messages SET isRead = 1 WHERE eventId = :eventId AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(eventId: String, currentUserId: String)
    
    @Query("UPDATE messages SET isSynced = 1 WHERE id = :messageId")
    suspend fun markMessageAsSynced(messageId: String)
    
    @Query("DELETE FROM messages WHERE eventId = :eventId")
    suspend fun deleteMessagesByEventId(eventId: String)
    
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    @Query("SELECT COUNT(*) FROM messages WHERE eventId = :eventId AND senderId != :currentUserId AND isRead = 0")
    suspend fun getUnreadMessageCount(eventId: String, currentUserId: String): Int
    
    @Query("SELECT * FROM messages WHERE eventId = :eventId AND timestamp > :timestamp ORDER BY timestamp ASC")
    suspend fun getMessagesAfterTimestamp(eventId: String, timestamp: Long): List<MessageEntity>
}