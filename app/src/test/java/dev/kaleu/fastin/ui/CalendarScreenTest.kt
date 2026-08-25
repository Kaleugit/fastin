package dev.kaleu.fastin.ui

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kaleu.fastin.data.db.FastinDatabase
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.ui.calendar.CalendarScreen
import dev.kaleu.fastin.ui.calendar.CalendarViewModel
import dev.kaleu.fastin.ui.theme.FastinTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Calendário pelo **caminho de produção**: ViewModel real + Room real + composable real
 * (`.claude/skills/testing/SKILL.md` § Verificação de UI Android).
 *
 * Um teste do `buildGrid` sozinho não provaria que a tela renderiza o dia certo nem que o
 * marcador de registro acompanha o banco.
 *
 * Duas notas de mecânica que custaram caro:
 * - Os finders de dia usam `useUnmergedTree = true`: `DayCell` usa `clearAndSetSemantics`,
 *   e a `testTag` do dia só existe na árvore não-mesclada.
 * - O banco é semeado **antes** de `setContent`. Mutar o Room no meio da composição faz o
 *   `runBlocking` travar a main thread que o ambiente de teste do Compose já está usando.
 */
@RunWith(RobolectricTestRunner::class)
class CalendarScreenTest {

    @get:Rule val compose = createComposeRule()

    private val zone = ZoneId.of("America/Sao_Paulo")

    // Relógio congelado: sem isso o teste quebraria sozinho na virada de mês real.
    private val clock = Clock.fixed(
        LocalDate.parse("2026-03-10").atTime(10, 0).atZone(zone).toInstant(),
        zone,
    )

    private lateinit var db: FastinDatabase
    private lateinit var repo: FastingLogRepository

    @Before fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, FastinDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = FastingLogRepository(db.fastingLogDao())
    }

    @After fun tearDown() = db.close()

    private fun seed(vararg logs: FastingLog) = runBlocking { logs.forEach { repo.save(it) } }

    private fun setContent(onDayClick: (LocalDate) -> Unit = {}) {
        compose.setContent {
            val vm = remember { CalendarViewModel(repo, clock) }
            val state by vm.uiState.collectAsStateWithLifecycle()
            FastinTheme {
                CalendarScreen(
                    state = state,
                    onPreviousMonth = vm::previousMonth,
                    onNextMonth = vm::nextMonth,
                    onDayClick = onDayClick,
                )
            }
        }
        // Drena o main looper do Robolectric: o primeiro valor do Flow do Room chega por ele.
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        awaitGrid()
    }

    /**
     * O `uiState` nasce com `days` vazio e só é preenchido quando o Flow do Room emite
     * (`SharingStarted.WhileSubscribed`). Sem esperar, os testes viram corrida: passam
     * quando a máquina está ociosa e falham sob carga.
     */
    private fun awaitGrid() {
        val anyDayCell = SemanticsMatcher("celula de dia") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("day_") == true
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(anyDayCell, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun day(date: String) = compose.onNodeWithTag("day_$date", useUnmergedTree = true)

    @Test
    fun `abre no mes de hoje e mostra o nome do mes em portugues`() {
        setContent()
        compose.onNodeWithTag("monthLabel").assertIsDisplayed()
        compose.onNodeWithText("Março").assertIsDisplayed()
    }

    @Test
    fun `as setas navegam entre meses`() {
        setContent()
        compose.onNodeWithText("Março").assertIsDisplayed()

        compose.onNodeWithTag("prevMonth").performClick()
        compose.onNodeWithText("Fevereiro").assertIsDisplayed()

        compose.onNodeWithTag("nextMonth").performClick()
        compose.onNodeWithTag("nextMonth").performClick()
        compose.onNodeWithText("Abril").assertIsDisplayed()
    }

    @Test
    fun `a grade inclui a borda do mes anterior e do seguinte`() {
        setContent()
        // Março/2026 começa num domingo, então a grade puxa o fim de fevereiro.
        day("2026-02-23").assertExists()
        day("2026-03-01").assertExists()
        day("2026-03-31").assertExists()
    }

    @Test
    fun `tocar num dia devolve a data daquele dia`() {
        var clicked: LocalDate? = null
        setContent(onDayClick = { clicked = it })

        day("2026-03-17").performScrollTo().performClick()
        assertEquals(LocalDate.parse("2026-03-17"), clicked)
    }

    /**
     * Metade negativa do controle no-op. Junto com o teste seguinte, prova que o marcador
     * está ligado ao banco: se ambos passassem com a mesma asserção, o marcador estaria
     * desconectado e o verde seria vazio.
     */
    @Test
    fun `dia sem dado anuncia sem registro`() {
        setContent()
        day("2026-03-12").assertContentDescriptionContains("sem registro", substring = true)
    }

    /** Metade positiva do controle no-op. */
    @Test
    fun `dia com dado anuncia com registro`() {
        seed(
            FastingLog(
                date = LocalDate.parse("2026-03-12"),
                lastMealTime = LocalTime.parse("20:00"),
            ),
        )
        setContent()
        day("2026-03-12").assertContentDescriptionContains("com registro", substring = true)
    }

    @Test
    fun `hoje e anunciado como hoje`() {
        setContent()
        day("2026-03-10").assertContentDescriptionContains("hoje", substring = true)
    }

    @Test
    fun `base vazia nao quebra a tela`() {
        setContent()
        compose.onNodeWithTag("monthLabel").assertIsDisplayed()
        day("2026-03-10").assertExists()
    }
}
