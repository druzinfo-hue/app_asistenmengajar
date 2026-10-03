package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {

    fun getAllMessages(): Flow<List<ChatEntity>> = chatDao.getAllMessages()

    suspend fun insertMessage(role: String, message: String, imageUrl: String? = null): Long {
        val entity = ChatEntity(
            timestamp = System.currentTimeMillis(),
            role = role,
            message = message,
            imageUrl = imageUrl
        )
        return chatDao.insertMessage(entity)
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }
}
