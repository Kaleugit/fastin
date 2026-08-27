package dev.kaleu.fastin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
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
                        // `systemBars` **não** inclui o teclado. Sozinho, ele deixava o IME
                        // subir por cima do conteúdo: o usuário digitava nas observações sem
                        // ver o que escrevia. `adjustResize` no manifest é pré-requisito,
                        // mas com edge-to-edge quem redimensiona é este inset — não a janela.
                        //
                        // `union` em vez de `safeDrawing` de propósito: preserva exatamente o
                        // recorte que o aparelho do usuário já mostra e só acrescenta o
                        // teclado. `safeDrawing` traria `displayCutout` junto e mexeria nas
                        // margens de quem tem notch, sem necessidade.
                        .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.ime)),
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
