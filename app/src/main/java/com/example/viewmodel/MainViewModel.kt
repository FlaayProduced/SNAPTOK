package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DownloadItem
import com.example.data.DownloadRepository
import com.example.network.RetrofitClient
import com.example.network.TikWmVideoData
import com.example.downloader.VideoDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: DownloadRepository) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _parsedVideoData = MutableStateFlow<TikWmVideoData?>(null)
    val parsedVideoData: StateFlow<TikWmVideoData?> = _parsedVideoData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _clipboardAutoDetectEnabled = MutableStateFlow(true)
    val clipboardAutoDetectEnabled: StateFlow<Boolean> = _clipboardAutoDetectEnabled.asStateFlow()

    private val _detectedClipboardUrl = MutableStateFlow<String?>(null)
    val detectedClipboardUrl: StateFlow<String?> = _detectedClipboardUrl.asStateFlow()

    private val _showSuccessDialog = MutableStateFlow(false)
    val showSuccessDialog: StateFlow<Boolean> = _showSuccessDialog.asStateFlow()

    private var lastCheckedUrl: String? = null

    val downloadHistory: StateFlow<List<DownloadItem>> = repository.allDownloads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onUrlInputChange(value: String) {
        _urlInput.value = value
    }

    fun setClipboardAutoDetect(enabled: Boolean) {
        _clipboardAutoDetectEnabled.value = enabled
    }

    fun dismissDetectedClipboard() {
        _detectedClipboardUrl.value = null
    }

    fun dismissSuccessDialog() {
        _showSuccessDialog.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun extractUrl(text: String): String? {
        val regex = """(https?://[^\s]+)""".toRegex()
        val found = regex.find(text)?.value
        if (found != null && (found.contains("tiktok.com") || found.contains("douyin.com") || found.contains("tokkit") || found.contains("tt"))) {
            return found
        }
        return null
    }

    fun checkClipboardText(text: String) {
        val url = extractUrl(text) ?: return
        if (url == lastCheckedUrl) return // Avoid constant annoying prompts for the same video link
        lastCheckedUrl = url
        
        if (_clipboardAutoDetectEnabled.value) {
            _detectedClipboardUrl.value = url
            _urlInput.value = url
        }
    }

    fun handleSharedText(text: String, context: Context) {
        val url = extractUrl(text) ?: return
        _urlInput.value = url
        _detectedClipboardUrl.value = null
        parseUrlAndDownload(url, context = context, autoStartDownload = true)
    }

    fun parseUrlAndDownload(text: String, context: Context, autoStartDownload: Boolean = false) {
        val url = extractUrl(text)
        if (url == null) {
            _errorMessage.value = "No valid TikTok Link found in the text!"
            return
        }
        _urlInput.value = url
        _detectedClipboardUrl.value = null

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _parsedVideoData.value = null

            try {
                // Post Form is highly stable and recommended
                val response = RetrofitClient.apiService.getVideoDetailsForm(url)
                if (response.code == 0 && response.data != null) {
                    _parsedVideoData.value = response.data
                    if (autoStartDownload) {
                        startDownloadVideo(context, response.data)
                    }
                } else {
                    _errorMessage.value = "Failed to parse video: ${response.msg}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Unable to connect. Check internet connection."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startDownloadVideo(context: Context, data: TikWmVideoData) {
        val downloadUrl = data.play ?: data.wmplay
        if (downloadUrl == null) {
            _errorMessage.value = "No downloadable video link found!"
            return
        }

        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            _errorMessage.value = null

            try {
                val filePath = VideoDownloader.downloadVideo(
                    context = context,
                    videoUrl = downloadUrl,
                    videoId = data.id,
                    onProgress = { progress ->
                        _downloadProgress.value = progress
                    }
                )

                if (filePath != null) {
                    val downloadItem = DownloadItem(
                        videoId = data.id,
                        title = data.title ?: "TikTok Video - ${data.id}",
                        originalUrl = _urlInput.value,
                        downloadUrl = downloadUrl,
                        coverUrl = data.cover ?: data.originCover,
                        authorUsername = data.author?.uniqueId ?: "unknown",
                        authorNickname = data.author?.nickname ?: "TikTok Creator",
                        authorAvatar = data.author?.avatar,
                        duration = data.duration ?: 0,
                        localFilePath = filePath,
                        status = "completed"
                    )
                    repository.insertDownload(downloadItem)
                    _showSuccessDialog.value = true
                    _urlInput.value = ""
                    _parsedVideoData.value = null
                } else {
                    _errorMessage.value = "Download failed. Could not write file."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Download error: ${e.localizedMessage}"
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun deleteHistoryItem(item: DownloadItem) {
        viewModelScope.launch {
            repository.deleteDownload(item)
            try {
                if (item.localFilePath != null && !item.localFilePath.startsWith("content://")) {
                    val file = java.io.File(item.localFilePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class MainViewModelFactory(private val repository: DownloadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
