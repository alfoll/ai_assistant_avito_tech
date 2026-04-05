package com.alfoll.aiassistant.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query(
        """
        SELECT * FROM chats
        WHERE (:searchQuery = '' OR title LIKE '%' || :searchQuery || '%')
        ORDER BY createdAt DESC
        """
    )
    fun pagingSource(searchQuery: String): PagingSource<Int, ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    fun observeChat(chatId: String): Flow<ChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("UPDATE chats SET title = :title WHERE id = :chatId")
    suspend fun updateChatTitle(chatId: String, title: String)
}