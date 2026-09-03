package dev.kaleu.fastin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.kaleu.fastin.ui.components.pressable
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinIcons
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.accentGlow
import dev.kaleu.fastin.ui.theme.neumorphic
import kotlinx.coroutines.launch

/** As três abas de topo, na ordem em que o swipe as percorre. */
enum class HomeTab(val route: String, val label: String, val icon: ImageVector) {
    CALENDAR("calendar", "Calendário", FastinIcons.Calendar),
    DASHBOARD("dashboard", "Dashboard", FastinIcons.Chart),
    SETTINGS("settings", "Ajustes", FastinIcons.Gear),
}

/**
 * Abas de topo com swipe (EP-002): um [HorizontalPager] com a barra inferior embaixo.
 *
 * Antes cada aba era um destino do `NavHost` e só o toque na barra trocava de tela. O
 * usuário pediu "deslizar para o lado troca de tela" — e um pager entrega isso com o dedo
 * arrastando a tela junto, coisa que um detector de gesto sobre o `NavHost` não dá.
 *
 * Separado do `NavHost` para ser testável com páginas de mentira: montar o grafo inteiro
 * exigiria o `AppContainer` real, cujo banco e DataStore são singletons por processo.
 *
 * @param userScrollEnabled desliga o swipe enquanto um overlay (o editor de card do
 *   dashboard) está aberto, para o dedo não trocar de aba por baixo dele.
 */
@Composable
fun HomeTabs(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    userScrollEnabled: Boolean = true,
    page: @Composable (HomeTab) -> Unit,
) {
    val scope = rememberCoroutineScope()

    // Voltar de uma aba secundária leva ao calendário, como o `popUpTo(start)` de antes
    // fazia; só do calendário o botão sai do app.
    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    Column(modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .testTag("homePager"),
            userScrollEnabled = userScrollEnabled,
            // Só a página visível fica composta: o ticker do relógio e os gráficos não devem
            // trabalhar em aba que ninguém está vendo.
            beyondViewportPageCount = 0,
        ) { index ->
            page(HomeTab.entries[index])
        }

        BottomBar(
            current = HomeTab.entries[pagerState.currentPage],
            onSelect = { tab -> scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
        )
    }
}

@Composable
private fun BottomBar(current: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(FastinColors.surfaceBase)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        HomeTab.entries.forEach { tab ->
            val selected = current == tab
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .semantics { this.selected = selected; role = Role.Tab }
                    .pressable(onClick = { onSelect(tab) })
                    .testTag("tab_${tab.route}")
                    .then(
                        if (selected) {
                            Modifier
                                .accentGlow(shape = FastinShapes.chip, elevation = 14.dp)
                                .background(FastinColors.accentGradient, FastinShapes.chip)
                        } else {
                            Modifier.neumorphic(
                                shape = FastinShapes.chip,
                                color = FastinColors.surface,
                                elevation = 8.dp,
                            )
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) FastinColors.onAccent else FastinColors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
