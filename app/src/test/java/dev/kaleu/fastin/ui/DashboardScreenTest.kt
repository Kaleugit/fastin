package dev.kaleu.fastin.ui

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kaleu.fastin.data.db.FastinDatabase
import dev.kaleu.fastin.data.prefs.DashboardConfigStore
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.metrics.ChartCardConfig
import dev.kaleu.fastin.domain.metrics.ChartType
import dev.kaleu.fastin.domain.metrics.Metric
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.ui.dashboard.DashboardScreen
import dev.kaleu.fastin.ui.dashboard.DashboardViewModel
import dev.kaleu.fastin.ui.theme.FastinTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Dashboard pelo caminho de produção: Room real + DataStore real + ViewModel real + tela real.
 *
 * Duas notas de mecânica:
 * - O `DataStore` é criado com **arquivo próprio por teste**. O delegate
 *   `by preferencesDataStore(...)` guarda uma instância por processo; usá-lo aqui faria os
 *   testes compartilharem arquivo, verem os cards uns dos outros e travarem em disputa de
 *   lock. Foi por isso que `DashboardConfigStore` passou a receber o store por injeção.
 * - As escritas do DataStore rodam em `Dispatchers.IO`, fora do looper do Robolectric.
 *   Drenar o looper não basta: [awaitUntil] alterna drenagem e espera real.
 */
@RunWith(RobolectricTestRunner::class)
class DashboardScreenTest {

    @get:Rule val compose = createComposeRule()
    @get:Rule val tempFolder = TemporaryFolder()

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.parse("2026-03-10")
    private val clock = Clock.fixed(today.atTime(10, 0).atZone(zone).toInstant(), zone)

    private lateinit var db: FastinDatabase
    private lateinit var repo: FastingLogRepository
    private lateinit var configStore: DashboardConfigStore
    private lateinit var dataStore: DataStore<Preferences>
    private val store = ViewModelStore()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Before fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, FastinDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = FastingLogRepository(db.fastingLogDao())
        dataStore = PreferenceDataStoreFactory.create(
            scope = ioScope,
            produceFile = { tempFolder.newFile("dashboard.preferences_pb") },
        )
        configStore = DashboardConfigStore(dataStore)
    }

    @After fun tearDown() {
        store.clear()
        db.close()
    }

    private fun flush() {
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    /**
     * Alterna drenagem do looper com espera real. `compose.waitUntil` sozinho avança só o
     * relógio virtual do Compose e nunca veria uma escrita que está acontecendo numa thread
     * de IO de verdade.
     */
    private fun awaitUntil(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            flush()
            if (condition()) return
            Thread.sleep(20)
        }
        throw AssertionError("timeout esperando: $what")
    }

    private fun tagExists(tag: String) =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun persisted(): List<ChartCardConfig> = runBlocking { configStore.cards.first() }

    /** Cinco dias consecutivos de jejum de 16h (20:00 -> 12:00), terminando hoje. */
    private fun seedFasting() = runBlocking {
        val byDate = mutableMapOf<LocalDate, FastingLog>()
        for (i in 0 until 5) {
            val day = today.minusDays(i.toLong())
            byDate[day] = (byDate[day] ?: FastingLog(day))
                .copy(firstMealTime = LocalTime.of(12, 0), weight = 80.0 - i)
            val prev = day.minusDays(1)
            byDate[prev] = (byDate[prev] ?: FastingLog(prev))
                .copy(lastMealTime = LocalTime.of(20, 0))
        }
        repo.saveAll(byDate.values.toList())
    }

    private fun setContent() {
        compose.setContent {
            val vm = remember {
                ViewModelProvider(
                    store,
                    viewModelFactory { initializer { DashboardViewModel(repo, configStore, clock) } },
                )[DashboardViewModel::class.java]
            }
            val state by vm.uiState.collectAsStateWithLifecycle()
            val editor by vm.editor.collectAsStateWithLifecycle()
            FastinTheme {
                DashboardScreen(
                    state = state,
                    editor = editor,
                    onAdd = vm::startAdd,
                    onEdit = vm::startEdit,
                    onRemove = vm::removeCard,
                    onDraftChange = vm::updateDraft,
                    onConfirmEdit = vm::confirmEdit,
                    onCancelEdit = vm::cancelEdit,
                )
            }
        }
        // A primeira emissão vem do DataStore em IO: sem esperar, a tela ainda está vazia.
        awaitUntil("cards padrão aparecerem") { tagExists("chart_default-streak") }
    }

    @Test
    fun `abre com os cards padrao em vez de tela vazia`() {
        setContent()
        DashboardConfigStore.DEFAULT_CARDS.forEach { card ->
            assertTrue("card ${card.id} não renderizou", tagExists("chart_${card.id}"))
        }
    }

    /**
     * Metade positiva do controle: com 5 dias de jejum de 16h o streak tem que ser 5. Se o
     * card mostrasse "—" ou 0, o cálculo estaria desconectado da tela.
     */
    @Test
    fun `streak reflete os dados do banco`() {
        seedFasting()
        setContent()

        compose.onNodeWithTag("value_default-streak", useUnmergedTree = true).performScrollTo()
        compose.onNodeWithText("5", useUnmergedTree = true).assertExists()
    }

    /** Metade negativa: sem dado nenhum, os cards dizem que não há dado — não inventam zero. */
    @Test
    fun `sem dados os cards mostram estado vazio`() {
        setContent()
        // Três dos quatro cards padrão são gráficos de série; todos ficam vazios.
        val empties = compose
            .onAllNodesWithText("sem dados no período", useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertEquals(3, empties.size)
    }

    @Test
    fun `remover um card tira ele da tela e do armazenamento`() {
        setContent()
        val target = DashboardConfigStore.DEFAULT_CARDS.last()
        assertTrue(tagExists("chart_${target.id}"))

        compose.onNodeWithTag("remove_${target.id}").performScrollTo().performClick()
        awaitUntil("card sumir da tela") { !tagExists("chart_${target.id}") }

        // Persistência: o DataStore precisa refletir a remoção, não só a tela.
        assertTrue(
            "o card continua no DataStore",
            persisted().none { it.id == target.id },
        )
    }

    @Test
    fun `adicionar um card abre o editor e persiste ao confirmar`() {
        setContent()
        val before = persisted().size

        compose.onNodeWithTag("addCard").performClick()
        awaitUntil("editor abrir") { tagExists("cardEditor") }

        compose.onNodeWithTag("metric_WEIGHT").performScrollTo().performClick()
        compose.onNodeWithTag("type_SCATTER").performScrollTo().performClick()
        compose.onNodeWithTag("period_LAST_7").performScrollTo().performClick()
        compose.onNodeWithTag("confirmEdit").performScrollTo().performClick()
        awaitUntil("editor fechar") { !tagExists("cardEditor") }

        val after = persisted()
        assertEquals("esperava um card a mais", before + 1, after.size)
        val created = after.last()
        assertEquals(Metric.WEIGHT, created.metric)
        assertEquals(ChartType.SCATTER, created.type)
    }

    @Test
    fun `cancelar o editor nao cria card`() {
        setContent()
        val before = persisted().size

        compose.onNodeWithTag("addCard").performClick()
        awaitUntil("editor abrir") { tagExists("cardEditor") }
        compose.onNodeWithTag("cancelEdit").performClick()
        awaitUntil("editor fechar") { !tagExists("cardEditor") }

        assertEquals("cancelar não pode criar card", before, persisted().size)
    }

    /**
     * Streak em linha é combinação inválida — não existe "série de streak" por dia. Escolher
     * a métrica precisa ajustar o tipo junto, senão o usuário salva um card que não desenha
     * nada e não entende por quê.
     */
    @Test
    fun `escolher streak forca o tipo numero grande`() {
        setContent()

        compose.onNodeWithTag("addCard").performClick()
        awaitUntil("editor abrir") { tagExists("cardEditor") }

        compose.onNodeWithTag("type_LINE").performScrollTo().performClick()
        compose.onNodeWithTag("metric_FASTING_STREAK").performScrollTo().performClick()
        compose.onNodeWithTag("confirmEdit").performScrollTo().performClick()
        awaitUntil("editor fechar") { !tagExists("cardEditor") }

        val created = persisted().last()
        assertEquals(Metric.FASTING_STREAK, created.metric)
        assertEquals(
            "streak precisa virar BIG_NUMBER mesmo tendo sido escolhido LINE antes",
            ChartType.BIG_NUMBER,
            created.type,
        )
    }

    @Test
    fun `titulo do dashboard aparece`() {
        setContent()
        compose.onNodeWithText("Dashboard").assertIsDisplayed()
    }
}
