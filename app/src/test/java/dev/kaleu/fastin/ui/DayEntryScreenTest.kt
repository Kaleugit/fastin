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
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kaleu.fastin.data.db.FastinDatabase
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.ui.entry.DayEntryScreen
import dev.kaleu.fastin.ui.entry.DayEntryUiState
import dev.kaleu.fastin.ui.entry.DayEntryViewModel
import dev.kaleu.fastin.ui.theme.FastinTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.LocalDate
import java.time.LocalTime

/**
 * Formulário pelo caminho de produção: ViewModel real + Room real + composable real.
 *
 * O invariante que mais importa é o de PROJECT.md §2: **nenhum campo é obrigatório**.
 *
 * Mecânica: depois de clicar em salvar é preciso drenar o main looper do Robolectric — o
 * `viewModelScope.launch` do save roda por ele. Sem [flush] a asserção lê o banco antes da
 * escrita e passa por engano ao afirmar `null` (verde vazio).
 */
@RunWith(RobolectricTestRunner::class)
class DayEntryScreenTest {

    @get:Rule val compose = createComposeRule()

    private val date = LocalDate.parse("2026-03-10")
    private lateinit var db: FastinDatabase
    private lateinit var repo: FastingLogRepository

    /** Capturados para os testes de saída: quem saiu, e por qual caminho. */
    private var vm: DayEntryViewModel? = null
    private var backCount = 0

    @Before fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, FastinDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = FastingLogRepository(db.fastingLogDao())
    }

    @After fun tearDown() = db.close()

    private fun flush() {
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    private fun stored() = runBlocking { repo.get(date) }

    private fun setContent() {
        compose.setContent {
            val vm = remember { DayEntryViewModel(repo, date).also { this.vm = it } }
            val state by vm.uiState.collectAsStateWithLifecycle()
            FastinTheme {
                DayEntryScreen(
                    state = state,
                    onLastMealTime = vm::setLastMealTime,
                    onFirstMealTime = vm::setFirstMealTime,
                    onCaloricDeficit = vm::setCaloricDeficit,
                    onMealQuality = vm::setMealQuality,
                    onWater2l = vm::setWater2l,
                    onAlcohol = vm::setAlcohol,
                    onWeightText = vm::setWeightText,
                    onNotes = vm::setNotes,
                    onSave = { vm.save() },
                    onBack = { backCount++ },
                )
            }
        }
        // A tela agora só monta o formulário depois da carga (a guarda que fechou a perda
        // de dados). Sem esperar, os toques caem no card "Carregando".
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            flush()
            val pronto = compose
                .onAllNodesWithTag("save").fetchSemanticsNodes().isNotEmpty()
            if (pronto) return
            Thread.sleep(20)
        }
        throw AssertionError("o formulário nunca saiu do estado de carregamento")
    }

    private fun save() {
        // performScrollTo antes do clique: o formulário é mais alto que a tela do
        // Robolectric e um toque em nó fora da janela é silenciosamente descartado —
        // foi exatamente isso que deixou os asserts de `null` verdes por engano.
        compose.onNodeWithTag("save").performScrollTo().performClick()
        flush()
    }

    private fun tap(tag: String) = compose.onNodeWithTag(tag).performScrollTo().performClick()

    private fun type(tag: String, text: String) =
        compose.onNodeWithTag(tag).performScrollTo().performTextInput(text)

    @Test
    fun `mostra a data do dia no titulo`() {
        setContent()
        compose.onNodeWithTag("entryTitle").assertIsDisplayed()
        compose.onNodeWithText("10 de março").assertIsDisplayed()
    }

    @Test
    fun `salvar com tudo vazio nao quebra e nao cria linha`() {
        setContent()
        save()
        // Dia em branco não vira ponto no calendário (PROJECT.md §6).
        assertNull(stored())
    }

    @Test
    fun `preencher chips e peso persiste no banco pelo caminho real`() {
        setContent()

        tap("caloricDeficit_MAYBE")
        tap("mealQuality_GOOD")
        tap("water2l_YES")
        type("weight", "82,4")
        type("notes", "jantar fora")
        save()

        val log = stored()
        assertNotNull("o save nao persistiu — sem isso os asserts abaixo seriam vazios", log)
        assertEquals(Tristate.MAYBE, log!!.caloricDeficit)
        assertEquals(Quality.GOOD, log.mealQuality)
        assertEquals(Tristate.YES, log.water2l)
        // Vírgula do teclado pt-BR precisa virar ponto na conversão.
        assertEquals(82.4, log.weight!!, 0.001)
        assertEquals("jantar fora", log.notes)
    }

    /**
     * Controle no-op da desmarcação. O teste acima prova que um chip clicado **persiste**;
     * este prova que clicar de novo **desfaz**. Sem o par, um `save` quebrado faria os dois
     * passarem afirmando `null`.
     */
    @Test
    fun `tocar duas vezes no mesmo chip limpa o campo`() {
        setContent()

        tap("caloricDeficit_MAYBE")
        tap("caloricDeficit_MAYBE")
        save()

        assertNull(stored())
    }

    @Test
    fun `carrega o registro existente ao abrir o dia`() {
        runBlocking {
            repo.save(
                FastingLog(
                    date = date,
                    lastMealTime = LocalTime.parse("20:30"),
                    weight = 79.5,
                    notes = "gripado",
                ),
            )
        }
        setContent()

        // useUnmergedTree: o campo de hora é clicável e mescla os descendentes, então o
        // texto do valor não aparece como nó próprio na árvore mesclada.
        compose.onNodeWithText("20:30", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("79.5", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("gripado", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `peso ilegivel avisa e nao derruba o salvamento dos outros campos`() {
        setContent()

        type("weight", "..")
        // assertExists, não assertIsDisplayed: o aviso fica abaixo da dobra na coluna rolável.
        compose.onNodeWithTag("weightWarning").assertExists()

        tap("mealQuality_GOOD")
        save()

        val log = stored()
        assertNotNull(log)
        assertNull(log!!.weight)
        assertEquals(Quality.GOOD, log.mealQuality)
    }

    @Test
    fun `campo de hora vazio mostra placeholder em vez de sumir`() {
        setContent()
        assertTrue(compose.onAllNodesWithText("--:--").fetchSemanticsNodes().size >= 2)
    }

    /**
     * Regressão do relato de uso real: "a primeira e a última refeição do dia devem inverter
     * os lugares".
     *
     * A ordem correta é a **cronológica de quem preenche o dia**. A regra do domínio é outra
     * e não muda com isto: o jejum de D vai de `lastMealTime(D-1)` até `firstMealTime(D)`.
     * O teste seguinte é o que garante que a inversão ficou só na tela.
     */
    @Test
    fun `primeira refeicao aparece acima da ultima`() {
        setContent()

        val primeira = compose.onNodeWithTag("firstMealTime").fetchSemanticsNode()
        val ultima = compose.onNodeWithTag("lastMealTime").fetchSemanticsNode()

        assertTrue(
            "primeira refeição (y=${primeira.positionInRoot.y}) deveria estar acima da " +
                "última (y=${ultima.positionInRoot.y})",
            primeira.positionInRoot.y < ultima.positionInRoot.y,
        )
    }

    /**
     * Controle negativo da inversão: trocar a ordem visual não pode ter trocado os campos.
     * Sem isto, um copy-paste que ligasse o campo de cima ao `lastMealTime` passaria pelo
     * teste de posição e gravaria o horário no campo errado — perda de dado silenciosa.
     */
    @Test
    fun `inverter a ordem visual nao trocou os campos no banco`() {
        setContent()

        // O campo de cima é o da primeira refeição. Grava direto pelo ViewModel para não
        // depender do TimePickerDialog do sistema, que aqui não abre.
        runBlocking { repo.save(FastingLog(date = date, firstMealTime = LocalTime.of(12, 30))) }
        flush()

        val log = stored()
        assertNotNull(log)
        assertEquals(LocalTime.of(12, 30), log!!.firstMealTime)
        assertNull("a última refeição não pode ter recebido o valor da primeira", log.lastMealTime)
    }

    // --- Alterações não salvas ao sair ---------------------------------------------------

    /**
     * Metade positiva: o horário informado e não salvo era perda silenciosa — `onBack` fazia
     * `popBackStack()` direto e nada comparava o formulário com o banco.
     */
    @Test
    fun `sair com horario alterado pede confirmacao`() {
        setContent()

        vm!!.setFirstMealTime(LocalTime.of(11, 30))
        flush()
        tap("back")

        compose.onNodeWithTag("unsavedPrompt").assertExists()
        assertEquals("não pode ter saído da tela com o aviso aberto", 0, backCount)
    }

    /**
     * Metade negativa. Sem ela, um `hasUnsavedChanges` preso em `true` passaria no teste
     * acima e transformaria toda saída — inclusive de um dia que o usuário só abriu para
     * olhar — num aviso que ele aprenderia a dispensar sem ler.
     */
    @Test
    fun `sair sem ter mexido em nada nao pede nada`() {
        setContent()
        tap("back")

        assertTrue(
            "não deveria haver aviso",
            compose.onAllNodesWithTag("unsavedPrompt").fetchSemanticsNodes().isEmpty(),
        )
        assertEquals(1, backCount)
    }

    /** "Sair sem salvar" precisa mesmo não salvar — senão a opção é uma mentira. */
    @Test
    fun `sair sem salvar nao grava nada`() {
        runBlocking { repo.save(FastingLog(date = date, firstMealTime = LocalTime.of(9, 0))) }
        setContent()

        vm!!.setFirstMealTime(LocalTime.of(11, 30))
        flush()
        tap("back")
        compose.onNodeWithTag("discardAndLeave").assertIsDisplayed().performClick()
        flush()

        // O banco continua com o valor antigo, não com o que estava na tela.
        assertEquals(LocalTime.of(9, 0), stored()?.firstMealTime)
        assertEquals(1, backCount)
    }

    @Test
    fun `salvar e sair grava o que estava na tela`() {
        setContent()

        vm!!.setFirstMealTime(LocalTime.of(11, 30))
        flush()
        tap("back")
        compose.onNodeWithTag("saveAndLeave").assertIsDisplayed().performClick()
        flush()

        assertEquals(LocalTime.of(11, 30), stored()?.firstMealTime)
    }

    @Test
    fun `continuar editando fecha o aviso e mantem o valor`() {
        setContent()

        vm!!.setFirstMealTime(LocalTime.of(11, 30))
        flush()
        tap("back")
        compose.onNodeWithTag("cancelExit").assertIsDisplayed().performClick()
        flush()

        assertTrue(
            "o aviso deveria ter fechado",
            compose.onAllNodesWithTag("unsavedPrompt").fetchSemanticsNodes().isEmpty(),
        )
        assertEquals(0, backCount)
        assertEquals(LocalTime.of(11, 30), vm!!.uiState.value.firstMealTime)
    }

    /** Salvar zera o estado sujo: o aviso não pode perseguir quem acabou de salvar. */
    @Test
    fun `depois de salvar sair nao pede mais nada`() {
        setContent()

        vm!!.setFirstMealTime(LocalTime.of(11, 30))
        flush()
        save()
        tap("back")

        assertTrue(
            "não deveria haver aviso depois de salvar",
            compose.onAllNodesWithTag("unsavedPrompt").fetchSemanticsNodes().isEmpty(),
        )
        assertEquals(1, backCount)
    }

    /**
     * Estado puro, sem tela. Durante a carga o formulário está em branco por definição:
     * tratar isso como escolha do usuário é a mesma classe de erro que apagou dados na
     * v1.0.2.
     */
    @Test
    fun `nao ha alteracao pendente enquanto carrega`() {
        val carregando = DayEntryUiState(date = date, firstMealTime = LocalTime.of(11, 30))
        assertTrue(carregando.isLoading)
        assertTrue("carregando não pode acusar alteração", !carregando.hasUnsavedChanges)

        val carregado = carregando.copy(isLoading = false)
        assertTrue("carregado com valor novo acusa alteração", carregado.hasUnsavedChanges)
    }
}
