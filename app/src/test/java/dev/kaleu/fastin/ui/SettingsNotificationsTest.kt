package dev.kaleu.fastin.ui

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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
import dev.kaleu.fastin.data.backup.CsvBackup
import dev.kaleu.fastin.data.db.FastinDatabase
import dev.kaleu.fastin.data.prefs.NotificationPrefsStore
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.ui.settings.SettingsScreen
import dev.kaleu.fastin.ui.settings.SettingsViewModel
import dev.kaleu.fastin.ui.theme.FastinTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.time.Clock
import java.time.ZoneId

/**
 * Regressão do bug relatado em uso real: "a notificação, quando o app é fechado, ela desativa
 * automaticamente".
 *
 * A preferência morava só em memória — `FastinApplication.notificationsEnabled` era um campo
 * volátil e `SettingsUiState` nascia `false`. Morto o processo, o app subia com os avisos
 * desligados sem ninguém ter desligado nada.
 *
 * Cada teste tem **arquivo de DataStore próprio** (`TemporaryFolder`), pela mesma razão
 * registrada em `DashboardConfigStore`: o delegate `by preferencesDataStore(...)` guarda uma
 * instância por processo e testes que o usassem disputariam o mesmo arquivo.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsNotificationsTest {

    @get:Rule val compose = createComposeRule()
    @get:Rule val tempFolder = TemporaryFolder()

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val clock = Clock.system(zone)

    private lateinit var db: FastinDatabase
    private lateinit var repo: FastingLogRepository
    private lateinit var backup: CsvBackup
    private lateinit var prefsFile: File
    private val vmStore = ViewModelStore()

    /** Um scope por DataStore: fechar o scope é o que libera o arquivo para o store seguinte. */
    private val scopes = mutableListOf<CoroutineScope>()

    @Before fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, FastinDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = FastingLogRepository(db.fastingLogDao())
        backup = CsvBackup(ctx, repo)
        prefsFile = File(tempFolder.root, "notifications.preferences_pb")
    }

    @After fun tearDown() {
        vmStore.clear()
        closeScopes()
        db.close()
    }

    /**
     * Cancelar não basta: o cancelamento é assíncrono e o DataStore continua enxergando o
     * arquivo como ocupado por alguns instantes, o que faz o store seguinte estourar com
     * "multiple DataStores active for the same file". É preciso **esperar o job morrer**.
     */
    private fun closeScopes() {
        scopes.forEach { scope ->
            runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() }
        }
        scopes.clear()
    }

    /**
     * O DataStore recusa duas instâncias vivas sobre o mesmo arquivo. Criar um store novo é
     * justamente o que simula o processo reiniciado, então o anterior precisa morrer antes.
     */
    private fun openStore(): NotificationPrefsStore {
        closeScopes()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        val dataStore: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { prefsFile })
        return NotificationPrefsStore(dataStore)
    }

    private fun flush() {
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    /** As escritas do DataStore rodam em IO, fora do looper: drenar não basta, é preciso esperar. */
    private fun awaitUntil(what: String, timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            flush()
            if (condition()) return
            Thread.sleep(20)
        }
        throw AssertionError("timeout esperando: $what")
    }

    private fun textExists(text: String) =
        compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /**
     * Monta a tela pelo caminho de produção. O callback grava no store, que é o papel do
     * `FastinApplication` no app real — o ViewModel só lê.
     */
    private fun mountSettings(store: NotificationPrefsStore, writeScope: CoroutineScope) {
        compose.setContent {
            val vm = ViewModelProvider(
                vmStore,
                viewModelFactory {
                    initializer {
                        SettingsViewModel(repo, backup, clock, store) { enabled ->
                            writeScope.launch { store.setEnabled(enabled) }
                        }
                    }
                },
            )[SettingsViewModel::class.java]
            val state by vm.uiState.collectAsStateWithLifecycle()

            FastinTheme {
                SettingsScreen(
                    state = state,
                    onExport = {},
                    onPickImport = {},
                    onToggleNotifications = vm::setNotificationsEnabled,
                    onToggleMilestoneHour = vm::toggleMilestoneHour,
                )
            }
        }
    }

    // --- O par que prova a persistência --------------------------------------------------

    @Test
    fun preferencia_ligada_sobrevive_ao_fim_do_processo() {
        runBlocking { openStore().setEnabled(true) }

        // Store novo sobre o mesmo arquivo == app reaberto.
        val reopened = openStore()

        assertTrue(
            "a preferência ligada precisa voltar ligada quando o app reabre",
            runBlocking { reopened.enabled.first() },
        )
    }

    /**
     * Controle negativo. Sem ele o teste acima passaria mesmo se `enabled` devolvesse `true`
     * fixo — que é uma forma de "sempre ligado" tão errada quanto o bug original.
     */
    @Test
    fun sem_nada_gravado_a_preferencia_nasce_desligada() {
        assertFalse(
            "quem nunca escolheu não deve ter trabalho em background",
            runBlocking { openStore().enabled.first() },
        )
    }

    @Test
    fun preferencia_desligada_tambem_persiste() {
        runBlocking {
            openStore().setEnabled(true)
            openStore().setEnabled(false)
        }
        assertFalse(
            "desligar precisa persistir tanto quanto ligar",
            runBlocking { openStore().enabled.first() },
        )
    }

    // --- O par que prova que a tela reflete o disco ---------------------------------------

    @Test
    fun tela_abre_refletindo_a_preferencia_ja_gravada() {
        runBlocking { openStore().setEnabled(true) }
        val store = openStore()

        mountSettings(store, scopes.first())

        // "Desativar avisos" é o rótulo do botão quando a preferência está ligada: a tela
        // precisa nascer sabendo disso, não assumindo `false`.
        awaitUntil("a tela refletir a preferência ligada") { textExists("Desativar avisos") }
        assertFalse(
            "a tela não pode oferecer 'Ativar' com os avisos já ligados",
            textExists("Ativar avisos"),
        )
    }

    @Test
    fun tela_abre_desligada_quando_nada_foi_gravado() {
        val store = openStore()

        mountSettings(store, scopes.first())

        awaitUntil("a tela refletir o default desligado") { textExists("Ativar avisos") }
        assertFalse(textExists("Desativar avisos"))
    }

    @Test
    fun tocar_no_toggle_grava_e_a_tela_acompanha() {
        val store = openStore()
        mountSettings(store, scopes.first())
        awaitUntil("a tela montar desligada") { textExists("Ativar avisos") }

        // `performScrollTo()` antes de todo toque: toque em nó fora da janela é descartado em
        // silêncio, sem exceção e sem aviso.
        compose.onNodeWithTag("toggleNotifications").performScrollTo().performClick()

        awaitUntil("o rótulo virar 'Desativar avisos'") { textExists("Desativar avisos") }
        assertTrue(
            "o toque precisa chegar ao disco, não só à tela",
            runBlocking { store.enabled.first() },
        )
    }

    // --- Marcos escolhidos (EP-002) --------------------------------------------------------

    private fun hoursOnDisk(store: NotificationPrefsStore) =
        runBlocking { store.milestoneHours.first() }

    private fun isSelected(tag: String): Boolean =
        runCatching { compose.onNodeWithTag(tag).assertIsSelected() }.isSuccess

    /** Controle negativo: sem nada gravado, os marcos são os que a v1.2 mostrava. */
    @Test
    fun sem_nada_gravado_os_marcos_sao_os_da_v1_2() {
        assertEquals(listOf(16, 18, 20, 24), hoursOnDisk(openStore()))
    }

    @Test
    fun marcos_escolhidos_sobrevivem_ao_fim_do_processo() {
        runBlocking { openStore().setMilestoneHours(listOf(48, 12)) }

        // Store novo sobre o mesmo arquivo == app reaberto. Volta ordenado.
        assertEquals(listOf(12, 48), hoursOnDisk(openStore()))
    }

    /** Vazio é escolha, não ausência: "não quero marco nenhum" não pode voltar ao default. */
    @Test
    fun conjunto_vazio_persiste_como_escolha() {
        runBlocking { openStore().setMilestoneHours(emptyList()) }
        assertTrue(hoursOnDisk(openStore()).isEmpty())
    }

    @Test
    fun valor_fora_das_opcoes_e_descartado() {
        runBlocking { openStore().setMilestoneHours(listOf(16, 13, 99)) }
        assertEquals(listOf(16), hoursOnDisk(openStore()))
    }

    /**
     * O par de toques: ligar 12h **acrescenta** (16h continua), desligar 16h **remove** só
     * ele. Se o chip fosse de escolha única, o segundo assert falharia.
     */
    @Test
    fun tocar_num_marco_grava_e_a_tela_acompanha() {
        val store = openStore()
        mountSettings(store, scopes.first())
        awaitUntil("a tela montar") { textExists("Ativar avisos") }
        assertTrue("16h faz parte do default", isSelected("hour_16"))
        assertFalse("12h não faz parte do default", isSelected("hour_12"))

        compose.onNodeWithTag("hour_12").performScrollTo().performClick()
        awaitUntil("12h acender na tela") { isSelected("hour_12") }
        assertTrue("ligar 12h não pode desligar 16h", isSelected("hour_16"))
        assertEquals(listOf(12, 16, 18, 20, 24), hoursOnDisk(store))

        compose.onNodeWithTag("hour_16").performScrollTo().performClick()
        awaitUntil("16h apagar na tela") { !isSelected("hour_16") }
        compose.onNodeWithTag("hour_16").assertIsNotSelected()
        assertEquals(listOf(12, 18, 20, 24), hoursOnDisk(store))
    }
}
