package dev.kaleu.fastin.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import dev.kaleu.fastin.ui.theme.FastinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Swipe entre as abas (EP-002).
 *
 * Páginas de mentira — um `Box` com tag por aba — porque o que está em teste é o gesto e a
 * barra, não as telas. Montar o `FastinNavHost` inteiro exigiria o `AppContainer` real, cujo
 * banco e DataStore são singletons por processo e brigariam entre os testes.
 */
@RunWith(RobolectricTestRunner::class)
class HomeTabsTest {

    @get:Rule val compose = createComposeRule()

    private fun mount(userScrollEnabled: Boolean = true) {
        compose.setContent {
            FastinTheme {
                val pagerState = rememberPagerState { HomeTab.entries.size }
                HomeTabs(pagerState = pagerState, userScrollEnabled = userScrollEnabled) { tab ->
                    Box(Modifier.fillMaxSize().testTag("page_${tab.route}"))
                }
            }
        }
        compose.waitForIdle()
    }

    private fun assertOn(tab: HomeTab) {
        compose.waitForIdle()
        HomeTab.entries.forEach { other ->
            if (other == tab) {
                compose.onNodeWithTag("page_${other.route}").assertExists()
                compose.onNodeWithTag("tab_${other.route}").assertIsSelected()
            } else {
                // beyondViewportPageCount = 0: página fora da tela nem existe na árvore.
                compose.onNodeWithTag("page_${other.route}").assertDoesNotExist()
                compose.onNodeWithTag("tab_${other.route}").assertIsNotSelected()
            }
        }
    }

    private fun swipeLeft() = compose.onNodeWithTag("homePager").performTouchInput { swipeLeft() }
    private fun swipeRight() = compose.onNodeWithTag("homePager").performTouchInput { swipeRight() }

    @Test
    fun `abre no calendario`() {
        mount()
        assertOn(HomeTab.CALENDAR)
    }

    @Test
    fun `deslizar para a esquerda vai para o dashboard`() {
        mount()
        swipeLeft()
        assertOn(HomeTab.DASHBOARD)
    }

    @Test
    fun `deslizar duas vezes chega em ajustes e volta`() {
        mount()
        swipeLeft()
        swipeLeft()
        assertOn(HomeTab.SETTINGS)

        swipeRight()
        assertOn(HomeTab.DASHBOARD)
    }

    /** Controle negativo: no calendário não há aba à direita; o gesto não pode fazer nada. */
    @Test
    fun `no calendario deslizar para a direita nao muda de aba`() {
        mount()
        swipeRight()
        assertOn(HomeTab.CALENDAR)
    }

    @Test
    fun `toque na barra continua trocando de aba`() {
        mount()
        compose.onNodeWithTag("tab_settings").performClick()
        assertOn(HomeTab.SETTINGS)

        compose.onNodeWithTag("tab_calendar").performClick()
        assertOn(HomeTab.CALENDAR)
    }

    /** Com o editor de card aberto o swipe é travado; o toque na barra segue funcionando. */
    @Test
    fun `com swipe desligado o gesto e ignorado mas a barra funciona`() {
        mount(userScrollEnabled = false)
        swipeLeft()
        assertOn(HomeTab.CALENDAR)

        compose.onNodeWithTag("tab_dashboard").performClick()
        assertOn(HomeTab.DASHBOARD)
    }
}
