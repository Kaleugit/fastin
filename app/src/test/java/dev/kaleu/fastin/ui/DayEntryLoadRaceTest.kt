package dev.kaleu.fastin.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kaleu.fastin.data.db.FastinDatabase
import dev.kaleu.fastin.data.db.FastingLogDao
import dev.kaleu.fastin.data.db.FastingLogEntity
import dev.kaleu.fastin.data.db.toEntity
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.ui.entry.DayEntryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

/**
 * Perda de dados relatada em uso real: a "primeira refeição" já registrada sumia.
 *
 * A causa era o `init` do ViewModel buscar o registro de forma assíncrona e fazer
 * `_uiState.value = ...` — sobrescrita incondicional — enquanto a tela já estava editável,
 * porque nada consultava `isLoading`. Dois caminhos de perda, um teste para cada.
 */
@RunWith(RobolectricTestRunner::class)
class DayEntryLoadRaceTest {

    private val dispatcher = StandardTestDispatcher()
    private val date = LocalDate.parse("2026-08-25")

    private val diaCompleto = FastingLog(
        date = date,
        lastMealTime = LocalTime.of(20, 0),
        firstMealTime = LocalTime.of(12, 0),
        mealQuality = Quality.GOOD,
        weight = 81.5,
    )

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    // ------------------------------------------------------------ caminho 1: Room real

    /**
     * Salvar antes da carga terminar não pode apagar o que já estava gravado.
     *
     * É o relato do usuário. O `@Upsert` grava a linha inteira: disparado sobre um estado
     * ainda em branco, zerava os campos que ele nunca tocou.
     */
    @Test
    fun `salvar antes da carga nao pode apagar campos ja gravados`() = runTest(dispatcher) {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val db = Room.inMemoryDatabaseBuilder(ctx, FastinDatabase::class.java)
            .allowMainThreadQueries().build()
        val repo = FastingLogRepository(db.fastingLogDao())
        runBlocking { repo.save(diaCompleto) }

        val vm = DayEntryViewModel(repo, date)
        // Sem avançar o dispatcher: o init ainda não leu o banco, exatamente como quando o
        // usuário toca na tela antes de o Room responder.
        vm.setWeightText("80.0")
        vm.save()
        testScheduler.advanceUntilIdle()

        val stored = runBlocking { repo.get(date) }
        assertNotNull("o registro do dia não pode desaparecer", stored)
        assertEquals(
            "a primeira refeição foi apagada por um save disparado antes da carga",
            LocalTime.of(12, 0),
            stored!!.firstMealTime,
        )
        assertEquals(LocalTime.of(20, 0), stored.lastMealTime)
        assertEquals(Quality.GOOD, stored.mealQuality)
        db.close()
    }

    // ------------------------------------------------------------ caminho 2: DAO controlado

    /**
     * DAO falso com a emissão sob controle do teste.
     *
     * O Flow do Room emite a partir do executor dele, que o dispatcher de teste não governa —
     * `advanceUntilIdle()` retorna com o `init` ainda suspenso. Para exercitar "o usuário
     * digitou **antes** de a carga chegar" é preciso decidir a hora da emissão, e é isso que
     * este DAO permite.
     */
    private class FakeDao(private val flow: MutableStateFlow<FastingLogEntity?>) : FastingLogDao {
        override fun observeAll(): Flow<List<FastingLogEntity>> = flow.map { listOfNotNull(it) }
        override fun observeRange(from: String, to: String): Flow<List<FastingLogEntity>> =
            flow.map { listOfNotNull(it) }
        override fun observeByDate(date: String): Flow<FastingLogEntity?> = flow
        override suspend fun getByDate(date: String): FastingLogEntity? = flow.value
        override suspend fun upsert(entity: FastingLogEntity) { flow.value = entity }
        override suspend fun upsertAll(entities: List<FastingLogEntity>) {
            flow.value = entities.lastOrNull()
        }
        override suspend fun delete(entity: FastingLogEntity) { flow.value = null }
        override suspend fun deleteByDate(date: String) { flow.value = null }
    }

    /** A carga não pode descartar o que o usuário digitou antes dela chegar. */
    @Test
    fun `carga nao pode sobrescrever edicao feita antes dela`() = runTest(dispatcher) {
        // Começa sem emissão: o init fica suspenso, como durante a leitura real do disco.
        val flow = MutableStateFlow<FastingLogEntity?>(null)
        val vm = DayEntryViewModel(FastingLogRepository(FakeDao(flow)), date)

        vm.setWeightText("77.7") // usuário digita antes de a carga chegar
        flow.value = diaCompleto.toEntity() // agora o banco responde
        testScheduler.advanceUntilIdle()

        assertEquals(
            "o peso digitado pelo usuário foi descartado pela carga",
            "77.7",
            vm.uiState.value.weightText,
        )
        assertEquals(
            "os demais campos do banco precisam continuar disponíveis",
            LocalTime.of(12, 0),
            vm.uiState.value.firstMealTime,
        )
        assertEquals(LocalTime.of(20, 0), vm.uiState.value.lastMealTime)
    }

    /** Controle negativo: sem edição pendente, a carga preenche tudo normalmente. */
    @Test
    fun `sem edicao pendente a carga preenche o formulario`() = runTest(dispatcher) {
        val flow = MutableStateFlow<FastingLogEntity?>(diaCompleto.toEntity())
        val vm = DayEntryViewModel(FastingLogRepository(FakeDao(flow)), date)
        testScheduler.advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(LocalTime.of(20, 0), s.lastMealTime)
        assertEquals(LocalTime.of(12, 0), s.firstMealTime)
        assertEquals(Quality.GOOD, s.mealQuality)
        assertEquals("81.5", s.weightText)
        assertEquals("a carga precisa liberar o formulário", false, s.isLoading)
    }
}
