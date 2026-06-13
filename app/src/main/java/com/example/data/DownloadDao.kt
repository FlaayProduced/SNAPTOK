package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_history ORDER BY downloadTimestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItem): Long

    @Update
    suspend fun updateDownload(item: DownloadItem)

    @Delete
    suspend fun deleteDownload(item: DownloadItem)

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM download_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getDownloadByVideoId(videoId: String): DownloadItem?

    @Query("SELECT * FROM download_history WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Int): DownloadItem?
}
