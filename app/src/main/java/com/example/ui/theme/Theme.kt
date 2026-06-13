package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFE1E2EC),
    onSurface = Color(0xFFE1E2EC)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BoldPrimary,
    onPrimary = Color.White,
    primaryContainer = BoldContainerActive,
    onPrimaryContainer = BoldTextDarkPurple,
    background = BoldBackground,
    onBackground = BoldOnBackgroundText,
    surface = BoldSurface,
    onSurface = BoldOnBackgroundText,
    surfaceVariant = BoldNavBackground,
    onSurfaceVariant = BoldGreyHeading,
    outline = BoldBorderLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set default dynamicColor to false to prioritize our custom Bold Typography theme colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
