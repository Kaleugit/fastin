package dev.kaleu.fastin.ui

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kaleu.fastin.data.db.FastinDatabase
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.MilestoneHours
import dev.kaleu.fastin.ui.clock.FastingClockCard
import dev.kaleu.fastin.ui.clock.FastingClockViewModel
import dev.kaleu.fastin.ui.theme.FastinTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Relógio de jejum pelo caminho de produção: Room real + ViewModel real + card real.
 *
 * O relógio é congelado com [Clock.fixed] — testar contagem regressiva contra o relógio do
 * sistema produziria falha aleatória na virada do minuto.
 */
@RunWith(RobolectricTestRunner::class)
class FastingClockTest {

    @get:Rule val compose = createComposeRule()

    private val zone = ZoneId.of("America/Sao_Paulo")
    private lateinit var db: FastinDatabase
    private lateinit var repo: FastingLogRepository

    /**
     * ViewModel criado por store de verdade, não por `remember {}`.
     *
     * O ticker é um loop infinito no `viewModelScope`. Com `remember {}` o ViewModel nunca
     * é limpo, o loop sobrevive ao teste e explode no tick seguinte ao `db.close()` — e a
     * exceção reaparece como falha do **teste seguinte**, que é o pior tipo de falha.
     * `store.clear()` chama `onCleared()` e cancela o escopo antes de fechar o banco.
     */
    private val store = ViewModelStore()

    @Before fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, FastinDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = FastingLogRepository(db.fastingLogDao())
    }

    @After fun tearDown() {
        store.clear()
        db.close()
    }

    private fun clockAt(date: String, hour: Int, minute: Int) = Clock.fixed(
        LocalDate.parse(date).atTime(hour, minute).atZone(zone).toInstant(),
        zone,
    )

    private fun seed(vararg logs: FastingLog) = runBlocking { logs.forEach { repo.save(it) } }

    private fun setContent(clock: Clock, hours: List<Int> = MilestoneHours.DEFAULT) {
        compose.setContent {
            val vm = remember {
                ViewModelProvider(
                    store,
                    viewModelFactory {
                        initializer { FastingClockViewModel(repo, clock, flowOf(hours)) }
                    },
                )[FastingClockViewModel::class.java]
            }
            val state by vm.uiState.collectAsStateWithLifecycle()
            FastinTheme { FastingClockCard(state = state, zone = zone) }
        }
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    /**
     * Espera o relógio sair do estado inicial vazio.
     *
     * O `uiState` nasce vazio e só vira "em andamento" quando o Flow do Room emite. Sem
     * esperar, o teste vira corrida: passa com a máquina ociosa e falha sob carga — que foi
     * exatamente como esta suíte quebrou ao crescer.
     */
    private fun awaitRunningClock() {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            flush()
            val running = compose
                .onAllNodesWithTag("clockValue", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
            if (running) return
            Thread.sleep(20)
        }
        throw AssertionError("o relógio nunca saiu do estado vazio")
    }

    private fun flush() {
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    /** Metade negativa do controle: sem última refeição registrada, não há jejum. */
    @Test
    fun `base vazia mostra o estado vazio da spec`() {
        setContent(clockAt("2026-03-10", 10, 0))

        compose.onNodeWithTag("clockEmpty").assertExists()
        compose.onNodeWithText(
            "nenhum jejum em andamento — registre sua última refeição",
        ).assertExists()
        compose.onNodeWithTag("clockValue", useUnmergedTree = true).assertDoesNotExist()
    }

    /**
     * Metade positiva: com `lastMealTime` em 09/03 às 20:00 e "agora" em 10/03 às 10:00,
     * o jejum tem exatamente 14h00. Se o relógio mostrasse outra coisa, o cálculo estaria
     * desconectado da tela.
     */
    @Test
    fun `com ultima refeicao registrada conta o tempo decorrido`() {
        seed(FastingLog(date = LocalDate.parse("2026-03-09"), lastMealTime = LocalTime.parse("20:00")))
        setContent(clockAt("2026-03-10", 10, 0))

        awaitRunningClock()

        compose.onNodeWithTag("clockEmpty").assertDoesNotExist()
        compose.onNodeWithTag("clockValue", useUnmergedTree = true)
            .assertContentDescriptionContains("14 horas e 0 minutos", substring = true)
    }

    @Test
    fun `o jejum atravessa a meia-noite sem zerar`() {
        seed(FastingLog(date = LocalDate.parse("2026-03-09"), lastMealTime = LocalTime.parse("22:30")))
        // 01:00 do dia seguinte: 2h30 de jejum, não "menos que zero" nem reinício.
        setContent(clockAt("2026-03-10", 1, 0))
        awaitRunningClock()

        compose.onNodeWithTag("clockValue", useUnmergedTree = true)
            .assertContentDescriptionContains("2 horas e 30 minutos", substring = true)
    }

    /**
     * Controle no-op dos marcos. Às 14h30 do dia seguinte a um jantar de 20:00 são 18h30
     * de jejum: 16h e 18h batidos, 20h e 24h pendentes. Se todos aparecessem iguais, o
     * estado de "batido" estaria desligado do cálculo.
     */
    @Test
    fun `marcos acendem conforme o decorrido e nao antes`() {
        seed(FastingLog(date = LocalDate.parse("2026-03-09"), lastMealTime = LocalTime.parse("20:00")))
        setContent(clockAt("2026-03-10", 14, 30))
        awaitRunningClock()

        compose.onNodeWithTag("milestone_16", useUnmergedTree = true)
            .assertContentDescriptionContains("batido", substring = true)
        compose.onNodeWithTag("milestone_18", useUnmergedTree = true)
            .assertContentDescriptionContains("batido", substring = true)
        compose.onNodeWithTag("milestone_20", useUnmergedTree = true)
            .assertContentDescriptionContains("pendente", substring = true)
        compose.onNodeWithTag("milestone_24", useUnmergedTree = true)
            .assertContentDescriptionContains("pendente", substring = true)
    }

    @Test
    fun `mostra o horario previsto de cada marco`() {
        seed(FastingLog(date = LocalDate.parse("2026-03-09"), lastMealTime = LocalTime.parse("20:00")))
        setContent(clockAt("2026-03-10", 10, 0))
        awaitRunningClock()

        // 20:00 + 16h = 12:00 · +18h = 14:00 · +20h = 16:00 · +24h = 20:00
        //
        // useUnmergedTree: o pill usa clearAndSetSemantics para o TalkBack anunciar
        // "marco de 16 horas, batido" em vez de ler os fragmentos soltos — então o texto
        // do horário só existe na árvore não-mesclada. É o comportamento desejado.
        listOf("12:00", "14:00", "16:00", "20:00").forEach { hour ->
            compose.onNodeWithText(hour, useUnmergedTree = true).assertExists()
        }
    }

    /** Registrar a primeira refeição encerra o jejum: o card volta ao estado vazio. */
    @Test
    fun `primeira refeicao registrada encerra o jejum em andamento`() {
        seed(
            FastingLog(
                date = LocalDate.parse("2026-03-09"),
                lastMealTime = LocalTime.parse("20:00"),
            ),
            FastingLog(
                date = LocalDate.parse("2026-03-10"),
                firstMealTime = LocalTime.parse("12:00"),
            ),
        )
        setContent(clockAt("2026-03-10", 14, 0))

        compose.onNodeWithTag("clockEmpty").assertExists()
    }

    /**
     * **O teste que realmente importa nesta etapa.**
     *
     * Todos os outros usam [Clock.fixed], então provariam um relógio parado tão bem quanto
     * um funcionando. A spec §3.3 exige que o valor avance sozinho, sem recarregar a tela —
     * é isso que este exercita: relógio que anda + tempo do looper avançando.
     */
    @Test
    fun `o relogio avanca sozinho sem recompor a tela`() {
        seed(FastingLog(date = LocalDate.parse("2026-03-09"), lastMealTime = LocalTime.parse("20:00")))

        // Relógio mutável: o ViewModel lê `instant()` a cada tick, então mexer aqui é o
        // equivalente a o tempo passar de verdade.
        var now = LocalDate.parse("2026-03-10").atTime(10, 0).atZone(zone).toInstant()
        // `testZone` com nome distinto de propósito: dentro de `object : Clock()` o nome
        // `zone` resolveria para a propriedade sintética do próprio getZone(), e o override
        // chamaria a si mesmo até estourar a pilha. O compilador avisou disso como
        // "recursive problem" e eu só tinha silenciado anotando o tipo.
        val testZone = zone
        val movingClock: Clock = object : Clock() {
            override fun getZone(): ZoneId = testZone
            override fun withZone(z: ZoneId): Clock = this
            override fun instant(): Instant = now
        }

        setContent(movingClock)
        awaitRunningClock()
        compose.onNodeWithTag("clockValue", useUnmergedTree = true)
            .assertContentDescriptionContains("14 horas e 0 minutos", substring = true)

        // Passa um minuto de tempo real e deixa o ticker emitir.
        //
        // `mainClock` do Compose governa o clock de *frames*, não o `delay` do ticker: esse
        // roda no Dispatchers.Main, que sob Robolectric é o main looper. Quem avança o
        // tempo dele é `idleFor`.
        now = now.plusSeconds(60)
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2))
        compose.waitForIdle()

        compose.onNodeWithTag("clockValue", useUnmergedTree = true)
            .assertContentDescriptionContains("14 horas e 1 minutos", substring = true)
    }

    /** Jejum "esquecido" há mais de 100h é dado velho, não jejum de 9 dias em andamento. */
    @Test
    fun `jejum abandonado ha mais de 100h nao aparece como em andamento`() {
        seed(FastingLog(date = LocalDate.parse("2026-03-01"), lastMealTime = LocalTime.parse("20:00")))
        setContent(clockAt("2026-03-10", 10, 0))

        compose.onNodeWithTag("clockEmpty").assertExists()
    }

    // --- Marcos escolhidos em Ajustes (EP-002) ---------------------------------------------

    /**
     * Os marcos do relógio são os que o usuário escolheu, não uma lista fixa. Par
     * positivo/negativo: com [12, 48] aparecem 12h e 48h — e o 16h do default **não**.
     * Às 10:00 do dia seguinte a um jantar de 20:00 são 14h: 12h batido, 48h pendente.
     */
    @Test
    fun `marcos seguem a escolha do usuario e nao o default`() {
        seed(FastingLog(date = LocalDate.parse("2026-03-09"), lastMealTime = LocalTime.parse("20:00")))
        setContent(clockAt("2026-03-10", 10, 0), hours = listOf(12, 48))
        awaitRunningClock()

        compose.onNodeWithTag("milestone_12", useUnmergedTree = true)
            .assertContentDescriptionContains("batido", substring = true)
        compose.onNodeWithTag("milestone_48", useUnmergedTree = true)
            .assertContentDescriptionContains("pendente", substring = true)
        compose.onNodeWithTag("milestone_16", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("milestonesEmpty").assertDoesNotExist()
    }

    /** Sem marco nenhum o card avisa onde escolher, em vez de deixar um vazio ao lado do anel. */
    @Test
    fun `sem marco escolhido o relogio avisa em vez de sumir`() {
        seed(FastingLog(date = LocalDate.parse("2026-03-09"), lastMealTime = LocalTime.parse("20:00")))
        setContent(clockAt("2026-03-10", 10, 0), hours = emptyList())
        awaitRunningClock()

        compose.onNodeWithTag("milestonesEmpty").assertExists()
        compose.onNodeWithTag("milestone_16", useUnmergedTree = true).assertDoesNotExist()
    }
}
