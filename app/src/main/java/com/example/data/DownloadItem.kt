package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: String,
    val title: String,
    val originalUrl: String,
    val downloadUrl: String,
    val coverUrl: String?,
    val authorUsername: String?,
    val authorNickname: String?,
    val authorAvatar: String?,
    val duration: Int,
    val downloadTimestamp: Long = System.currentTimeMillis(),
    val localFilePath: String? = null,
    val status: String = "completed" // "downloading", "completed", "failed"
)
