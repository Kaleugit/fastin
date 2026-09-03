package dev.kaleu.fastin.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.domain.model.YesNo
import dev.kaleu.fastin.ui.calendar.CalendarScreen
import dev.kaleu.fastin.ui.calendar.CalendarViewModel
import dev.kaleu.fastin.ui.clock.FastingClockCard
import dev.kaleu.fastin.ui.clock.FastingClockViewModel
import dev.kaleu.fastin.ui.dashboard.DashboardScreen
import dev.kaleu.fastin.ui.dashboard.DashboardViewModel
import dev.kaleu.fastin.ui.entry.DayEntryScreen
import dev.kaleu.fastin.ui.entry.DayEntryViewModel
import dev.kaleu.fastin.ui.settings.SettingsScreen
import dev.kaleu.fastin.ui.settings.SettingsUiState
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinTheme
import dev.kaleu.fastin.ui.theme.Spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.sin
import kotlin.random.Random

/**
 * Gera PNGs de cada tela em `docs/screenshots/`.
 *
 * Existe porque **não há emulador Android para Windows ARM64** — o Google não publica o
 * pacote `emulator` para essa plataforma. Renderizar na JVM é a forma de olhar a UI sem
 * aparelho conectado.
 *
 * Não é só documentação: como cada tela é composta de verdade e desenhada num bitmap, isto
 * também é um smoke test de renderização. Uma tela que lançasse ao medir ou desenhar
 * quebraria aqui, e nenhum outro teste pegaria isso — os demais só consultam a árvore de
 * semântica, que existe mesmo quando o desenho falha.
 *
 * `GraphicsMode.NATIVE` exige o runtime nativo do Robolectric, que só tem binário x86_64
 * para Windows — funciona porque a JVM de teste é x64 (ADR-007).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi") // Pixel-ish: o formato mais comum hoje
class ScreenshotTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val tempFolder = TemporaryFolder()

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.parse("2026-03-10")

    /** 10:32 da manhã: jejum de 14h32 correndo, marcos de 16h e 18h ainda à frente. */
    private val clock = Clock.fixed(today.atTime(10, 32).atZone(zone).toInstant(), zone)

    private lateinit var db: FastinDatabase
    private lateinit var repo: FastingLogRepository
    private lateinit var configStore: DashboardConfigStore
    private lateinit var dataStore: DataStore<Preferences>
    private val store = ViewModelStore()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val outputDir = File("../docs/screenshots").let {
        if (it.exists() || it.mkdirs()) it else File("docs/screenshots").apply { mkdirs() }
    }

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
        seed()
    }

    @After fun tearDown() {
        store.clear()
        db.close()
    }

    /**
     * 100 dias de histórico plausível: jejum oscilando em torno de 16h, peso descendo
     * devagar com ruído, alguns dias sem registro. Screenshot com dado sintético demais
     * (linha reta, todos os dias preenchidos) mente sobre como o app se parece de verdade.
     */
    private fun seed() = runBlocking {
        val rng = Random(42) // semente fixa: o screenshot precisa ser reprodutível
        val logs = mutableListOf<FastingLog>()

        for (i in 100 downTo 1) {
            val day = today.minusDays(i.toLong())
            if (rng.nextInt(100) < 12) continue // ~12% dos dias sem registro nenhum

            val lastMeal = LocalTime.of(19 + rng.nextInt(3), listOf(0, 15, 30, 45).random(rng))
            val fastHours = 14 + sin(i / 7.0) * 3 + rng.nextDouble(-1.0, 1.5)
            val firstMeal = lastMeal.plusHours(fastHours.toLong()).plusMinutes(rng.nextInt(60).toLong())

            logs += FastingLog(
                date = day,
                lastMealTime = lastMeal,
                firstMealTime = firstMeal,
                caloricDeficit = Tristate.entries.random(rng),
                mealQuality = Quality.entries.random(rng),
                water2l = Tristate.entries.random(rng),
                alcohol = if (rng.nextInt(100) < 20) YesNo.YES else YesNo.NO,
                weight = 84.0 - (100 - i) * 0.035 + rng.nextDouble(-0.4, 0.4),
                notes = if (rng.nextInt(100) < 15) "jantar fora" else null,
            )
        }

        // Os últimos 6 dias batem 16h de propósito: o card de streak precisa mostrar um
        // número real, e a distribuição aleatória raramente produz uma sequência.
        val recent = (1..6).map { back ->
            val day = today.minusDays(back.toLong())
            FastingLog(
                date = day,
                lastMealTime = LocalTime.of(20, 0),
                firstMealTime = LocalTime.of(12, 30),
                caloricDeficit = Tristate.YES,
                mealQuality = if (back % 3 == 0) Quality.AVERAGE else Quality.GOOD,
                water2l = Tristate.YES,
                alcohol = if (back == 4) YesNo.YES else YesNo.NO,
                weight = 80.9 - (6 - back) * 0.1,
                notes = if (back == 2) "jantar fora" else null,
            )
        }
        // Mescla por data: o registro recente substitui o gerado, sem duplicar a chave.
        // (Antes eu apenas concatenava e o upsert apagava o firstMealTime de ontem — o
        // streak aparecia como 0 no screenshot.)
        val merged = (logs + recent).associateBy { it.date }.values.toList()

        // O jejum em andamento já existe sem mexer em mais nada: ontem tem `lastMealTime`
        // 20:00 e **hoje não tem registro**, então a janela de hoje está aberta.
        //
        // Zerar a `firstMealTime` de ontem — que foi o que fiz antes — quebrava o jejum
        // concluído de ontem e zerava o streak. `firstMealTime` de ontem é a refeição que
        // encerrou o jejum *de ontem*, não tem relação com o jejum de hoje.
        repo.saveAll(merged)
    }

    private fun flush() {
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    private fun await(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            flush()
            if (condition()) return
            Thread.sleep(20)
        }
        throw AssertionError("timeout esperando: $what")
    }

    private fun tagExists(tag: String) = compose
        .onAllNodesWithTagCompat(tag).isNotEmpty()

    /**
     * `captureToImage()` do Compose usa `PixelCopy`, que exige uma janela real e não existe
     * sob Robolectric. O caminho aqui é rasterizar a hierarquia de views direto num bitmap —
     * é o que as bibliotecas de screenshot fazem por baixo.
     */
    private fun capture(name: String) {
        flush()
        val view = compose.activity.window.decorView
        require(view.width > 0 && view.height > 0) {
            "a view não foi medida: ${view.width}x${view.height}"
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        val file = File(outputDir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("### screenshot: ${file.absolutePath} (${bitmap.width}x${bitmap.height})")
    }

    private fun screen(content: @Composable () -> Unit) {
        compose.setContent {
            FastinTheme {
                Box(Modifier.fillMaxSize().background(FastinColors.surfaceBase)) { content() }
            }
        }
        flush()
    }

    @Test
    fun `01 calendario com relogio de jejum`() {
        screen {
            val cal = remember { vm { CalendarViewModel(repo, clock) } }
            val clk = remember { vmClock() }
            val state by cal.uiState.collectAsStateWithLifecycle()
            val clockState by clk.uiState.collectAsStateWithLifecycle()
            CalendarScreen(
                state = state,
                onPreviousMonth = cal::previousMonth,
                onNextMonth = cal::nextMonth,
                onDayClick = {},
                header = { FastingClockCard(state = clockState, zone = zone) },
            )
        }
        await("relógio e grade") { tagExists("clockValue") && tagExists("day_2026-03-10") }
        capture("01-calendario")
    }

    @Test
    fun `02 formulario do dia`() {
        screen {
            val entry = remember {
                vmEntry(today.minusDays(1))
            }
            val state by entry.uiState.collectAsStateWithLifecycle()
            DayEntryScreen(
                state = state,
                onLastMealTime = entry::setLastMealTime,
                onFirstMealTime = entry::setFirstMealTime,
                onCaloricDeficit = entry::setCaloricDeficit,
                onMealQuality = entry::setMealQuality,
                onWater2l = entry::setWater2l,
                onAlcohol = entry::setAlcohol,
                onWeightText = entry::setWeightText,
                onNotes = entry::setNotes,
                onSave = {},
                onBack = {},
            )
        }
        await("campos carregados") { tagExists("caloricDeficit_YES") }
        capture("02-formulario")
    }

    @Test
    fun `03 dashboard`() {
        screen {
            val dash = remember { vm { DashboardViewModel(repo, configStore, clock) } }
            val state by dash.uiState.collectAsStateWithLifecycle()
            val editor by dash.editor.collectAsStateWithLifecycle()
            DashboardScreen(
                state = state,
                editor = editor,
                onAdd = dash::startAdd,
                onEdit = dash::startEdit,
                onRemove = dash::removeCard,
                onDraftChange = dash::updateDraft,
                onConfirmEdit = dash::confirmEdit,
                onCancelEdit = dash::cancelEdit,
            )
        }
        await("cards com dados") { tagExists("chart_default-fasting") }
        capture("03-dashboard")
    }

    @Test
    fun `04 editor de card`() {
        screen {
            val dash = remember { vm { DashboardViewModel(repo, configStore, clock) } }
            val state by dash.uiState.collectAsStateWithLifecycle()
            val editor by dash.editor.collectAsStateWithLifecycle()
            DashboardScreen(
                state = state,
                editor = editor,
                onAdd = dash::startAdd,
                onEdit = dash::startEdit,
                onRemove = dash::removeCard,
                onDraftChange = dash::updateDraft,
                onConfirmEdit = dash::confirmEdit,
                onCancelEdit = dash::cancelEdit,
            )
        }
        await("dashboard pronto") { tagExists("addCard") }
        compose.onNodeWithTag("addCard").performClick()
        await("editor aberto") { tagExists("cardEditor") }
        capture("04-editor-de-card")
    }

    @Test
    fun `05 ajustes`() {
        screen {
            SettingsScreen(
                state = SettingsUiState(
                    totalDays = 88,
                    notificationsEnabled = true,
                    message = "Exportado: Downloads/fastin-backup-2026-03-10.csv",
                ),
                onExport = {},
                onPickImport = {},
                onToggleNotifications = {},
                onToggleMilestoneHour = {},
            )
        }
        capture("05-ajustes")
    }

    @Test
    fun `06 relogio sem jejum em andamento`() {
        // Base vazia: o estado que a spec §3.3 descreve textualmente.
        val emptyDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FastinDatabase::class.java,
        ).allowMainThreadQueries().build()
        val emptyRepo = FastingLogRepository(emptyDb.fastingLogDao())

        screen {
            val clk = remember {
                ViewModelProvider(
                    store,
                    viewModelFactory { initializer { FastingClockViewModel(emptyRepo, clock) } },
                )[FastingClockViewModel::class.java]
            }
            val state by clk.uiState.collectAsStateWithLifecycle()
            Box(Modifier.fillMaxSize().background(FastinColors.surfaceBase)) {
                FastingClockCard(
                    state = state,
                    zone = zone,
                    modifier = Modifier.padding(Spacing.lg),
                )
            }
        }
        await("estado vazio") { tagExists("clockEmpty") }
        capture("06-relogio-vazio")
        emptyDb.close()
    }

    /**
     * A barra inferior não aparece em nenhuma tela isolada acima. Este é o único lugar onde
     * dá para olhar o ícone de engrenagem desenhado à mão (EP-002).
     */
    @Test
    fun `07 barra de abas`() {
        screen {
            val pagerState = androidx.compose.foundation.pager.rememberPagerState { HomeTab.entries.size }
            HomeTabs(pagerState = pagerState) { tab ->
                Box(Modifier.fillMaxSize().background(FastinColors.surfaceBase).testTag("page_${tab.route}"))
            }
        }
        await("barra montada") { tagExists("tab_settings") }
        compose.onNodeWithTag("tab_settings").performClick()
        await("ajustes selecionado") { tagExists("page_settings") }
        capture("07-barra-de-abas")
    }

    // ---------------------------------------------------------------- helpers

    private inline fun <reified T : androidx.lifecycle.ViewModel> vm(
        crossinline create: () -> T,
    ): T = ViewModelProvider(
        store,
        viewModelFactory { initializer { create() } },
    )[T::class.java]

    private fun vmClock() = vm { FastingClockViewModel(repo, clock) }
    private fun vmEntry(date: LocalDate) = ViewModelProvider(
        store,
        viewModelFactory { initializer { DayEntryViewModel(repo, date) } },
    )[DayEntryViewModel::class.java]
}

private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithTagCompat(
    tag: String,
) = onAllNodes(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true)
    .fetchSemanticsNodes()
