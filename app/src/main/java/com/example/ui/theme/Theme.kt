package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = LightVioletBackground,
    surface = LightVioletBackground,
    onBackground = HighDensityTextDark,
    onSurface = HighDensityTextDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = LightPurpleContainer,
    onPrimaryContainer = DarkPurpleText,
    secondary = SecondaryPurple,
    secondaryContainer = SecondaryPurpleContainer,
    onSecondaryContainer = OnSecondaryPurpleContainer,
    background = LightVioletBackground,
    onBackground = HighDensityTextDark,
    surface = LightVioletBackground,
    onSurface = HighDensityTextDark,
    surfaceVariant = SoftGrayBackground,
    onSurfaceVariant = HighDensityTextDark,
    outline = LightGrayBorder,
    error = HighDensityRed,
    onError = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color to enforce the unique High Density branding aesthetic
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
