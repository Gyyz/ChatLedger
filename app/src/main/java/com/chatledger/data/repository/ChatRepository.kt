package com.chatledger.data.repository

import com.chatledger.data.dao.ChatMessageDao
import com.chatledger.data.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: ChatMessageDao) {

    fun getAllMessages(): Flow<List<ChatMessage>> = dao.getAllMessages()

    fun getRecentMessages(limit: Int = 50): Flow<List<ChatMessage>> = dao.getRecentMessages(limit)

    suspend fun insert(message: ChatMessage): Long = dao.insert(message)

    suspend fun delete(message: ChatMessage) = dao.delete(message)

    suspend fun clearAll() = dao.clearAll()
}
