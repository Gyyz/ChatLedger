package com.chatledger.data.dao

import androidx.room.*
import com.chatledger.data.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert
    suspend fun insert(message: ChatMessage): Long

    @Delete
    suspend fun delete(message: ChatMessage)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int = 50): Flow<List<ChatMessage>>

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
