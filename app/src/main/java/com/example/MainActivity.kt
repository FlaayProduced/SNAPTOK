package com.example

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.DownloadItem
import com.example.data.DownloadRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { DownloadRepository(database.downloadDao()) }
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Custom geometric logo matching the HTML theme
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(2.dp, Color.White, CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "SNAPTOK",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        letterSpacing = (-0.5).sp
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = true,
                                onClick = {},
                                icon = { 
                                    Icon(
                                        imageVector = Icons.Default.Home, 
                                        contentDescription = "Home",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) 
                                },
                                label = { Text("Home", fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {},
                                icon = { 
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary, 
                                        contentDescription = "Library",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                },
                                label = { Text("Library") },
                                colors = NavigationBarItemDefaults.colors(
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {},
                                icon = { 
                                    Icon(
                                        imageVector = Icons.Default.Settings, 
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    MainScreenContent(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkClipboard()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.handleSharedText(sharedText, this)
            }
        }
    }

    private fun checkClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank()) {
                    viewModel.checkClipboardText(text)
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val parsedVideoData by viewModel.parsedVideoData.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val clipboardAutoDetect by viewModel.clipboardAutoDetectEnabled.collectAsStateWithLifecycle()
    val detectedClipboardUrl by viewModel.detectedClipboardUrl.collectAsStateWithLifecycle()
    val showSuccessDialog by viewModel.showSuccessDialog.collectAsStateWithLifecycle()
    val downloadHistory by viewModel.downloadHistory.collectAsStateWithLifecycle()

    var activePlaybackPath by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            // Massive Bold Typography Hero Title Section
            item {
                val heroHeading: String
                val heroSub: String
                
                when {
                    isDownloading || isLoading -> {
                        heroHeading = "DOWN-\nLOADING"
                        heroSub = "PULLING VIDEO STREAM"
                    }
                    parsedVideoData != null -> {
                        heroHeading = "PREVIEW\nREADY"
                        heroSub = "READY TO SAVE WATERMARK-FREE"
                    }
                    detectedClipboardUrl != null -> {
                        heroHeading = "LINK\nREADY"
                        heroSub = "CLIPBOARD SYNC ACTIVE"
                    }
                    else -> {
                        heroHeading = "SNAP\nTOK"
                        heroSub = "SUPER-FAST NO-WATERMARK SAVER"
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = heroHeading,
                        fontSize = 48.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("hero_title")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = heroSub,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: Dashboard and Clipboard toggle
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Detect Link",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Checks clipboard on app entry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = clipboardAutoDetect,
                            onCheckedChange = { viewModel.setClipboardAutoDetect(it) },
                            modifier = Modifier.testTag("clipboard_detect_switch")
                        )
                    }
                }
            }

            // Clipboard Detection Banner (Sliding Card)
            item {
                AnimatedVisibility(
                    visible = detectedClipboardUrl != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    detectedClipboardUrl?.let { url ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clipboard_detected_banner"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "LINK DETECTED",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            letterSpacing = 1.2.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Ready in Clipboard",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.onPrimaryContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "CLIP",
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = url,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { viewModel.dismissDetectedClipboard() },
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Text(
                                            text = "Dismiss", 
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = { viewModel.parseUrlAndDownload(url, context) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            contentColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(44.dp)
                                    ) {
                                        Text("Analyze Link", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input field Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Download Video",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = (-0.3).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { viewModel.onUrlInputChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("url_input_field"),
                            placeholder = { Text("Paste TikTok link here...") },
                            trailingIcon = {
                                if (urlInput.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onUrlInputChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear text")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Paste Button
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    if (clipboard.hasPrimaryClip()) {
                                        val clipData = clipboard.primaryClip
                                        if (clipData != null && clipData.itemCount > 0) {
                                            val text = clipData.getItemAt(0).text?.toString() ?: ""
                                            viewModel.onUrlInputChange(text)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("paste_button"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste Link", fontWeight = FontWeight.Bold)
                            }

                            // Download/Analyze button
                            Button(
                                onClick = { viewModel.parseUrlAndDownload(urlInput, context) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(52.dp)
                                    .testTag("download_button"),
                                shape = RoundedCornerShape(16.dp),
                                enabled = urlInput.isNotBlank() && !isLoading && !isDownloading
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Error Display Card
            item {
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    errorMessage?.let { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error icon",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = msg,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loading / Progress Card (Active Download card layout)
            item {
                AnimatedVisibility(
                    visible = isLoading || isDownloading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (isLoading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Analyzing TikTok Video...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            } else {
                                // Active downloading card styling matching HTML perfectly
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text(
                                            text = "DOWNLOADING NOW",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.5.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Processing Video Stream...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.onPrimaryContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "4K HD",
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(18.dp))
                                
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Status: Watermark-free bypass",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pre-download Video Preview card
            item {
                AnimatedVisibility(
                    visible = parsedVideoData != null,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    parsedVideoData?.let { data ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                ) {
                                    AsyncImage(
                                        model = data.cover ?: data.originCover,
                                        contentDescription = "Cover Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.35f))
                                    )
                                    // Author Info Overlay
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = data.author?.avatar,
                                            contentDescription = "Avatar",
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, Color.White, CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = data.author?.nickname ?: "TikTok Creator",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "@${data.author?.uniqueId ?: "unknown"}",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = data.title ?: "No Title provided",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = { viewModel.startDownloadVideo(context, data) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Download No-Watermark", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // History Header (Matching "Recent Saved")
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recent Saved icon",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RECENT SAVED",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${downloadHistory.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // History Empty State
            if (downloadHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Empty history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your history is clean",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Detected links ready in clipboard will sync here automatically inside SnapTok.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // History Items List
                items(downloadHistory, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onPlay = { activePlaybackPath = item.localFilePath },
                        onShare = {
                            try {
                                val uri = Uri.parse(item.localFilePath)
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    type = "video/mp4"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share TikTok Video"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        onDelete = { viewModel.deleteHistoryItem(item) }
                    )
                }
            }
        }
    }

    // Success dialog celebration popup
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSuccessDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Download Success!")
                }
            },
            text = {
                Text(
                    text = "The TikTok video has been downloaded watermark-free to your device's TikTokDownloader folder. You can play, share or delete it from the history list below.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissSuccessDialog() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Awesome")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // In-app premium video playback overlay player dialog
    activePlaybackPath?.let { path ->
        PlayVideoDialog(
            videoPath = path,
            onDismiss = { activePlaybackPath = null }
        )
    }
}

@Composable
fun HistoryItemCard(
    item: DownloadItem,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MM-dd-yyyy HH:mm", Locale.getDefault()) }
    val dateString = formatter.format(Date(item.downloadTimestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_item_card_${item.videoId}")
            .clickable { onPlay() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left - Video Thumbnail Image
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Duration Badge
                if (item.duration > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.duration}s",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Small overlay play circle
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle - Text detail Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.authorAvatar,
                        contentDescription = "Author Avatar",
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.authorNickname ?: "TikTok Creator",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right - Action Operations column
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("action_share_${item.videoId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("action_delete_${item.videoId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayVideoDialog(
    videoPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Window Header Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Local Video Player",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close player",
                            tint = Color.White
                        )
                    }
                }

                // Video Display View Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val mediaController = MediaController(ctx)
                                mediaController.setAnchorView(this)
                                setMediaController(mediaController)
                                
                                val parsedUri = Uri.parse(videoPath)
                                if (videoPath.startsWith("content://") || videoPath.startsWith("http")) {
                                    setVideoURI(parsedUri)
                                } else {
                                    setVideoPath(videoPath)
                                }
                                setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
