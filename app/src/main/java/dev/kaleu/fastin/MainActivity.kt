package dev.kaleu.fastin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import dev.kaleu.fastin.ui.FastinNavHost
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Barras transparentes com ícones claros: o app é dark-only (ADR-002), então não há
        // caso em que ícones escuros façam sentido.
        val bars = SystemBarStyle.dark(FastinColors.surfaceBase.toArgb())
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        super.onCreate(savedInstanceState)

        val app = application as FastinApplication
        val container = app.container

        setContent {
            FastinTheme {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxSize()
                        .background(FastinColors.surfaceBase)
                        .windowInsetsPadding(WindowInsets.systemBars),
                ) {
                    FastinNavHost(
                        container = container,
                        onNotificationsToggled = app::setNotificationsEnabled,
                    )
                }
            }
        }
    }
}
