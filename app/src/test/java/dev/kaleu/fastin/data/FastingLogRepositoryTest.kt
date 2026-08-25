package dev.kaleu.fastin.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kaleu.fastin.data.db.FastinDatabase
import dev.kaleu.fastin.data.repo.FastingLogRepository
import dev.kaleu.fastin.domain.model.FastingLog
import dev.kaleu.fastin.domain.model.Quality
import dev.kaleu.fastin.domain.model.Tristate
import dev.kaleu.fastin.domain.model.YesNo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class FastingLogRepositoryTest {

    private lateinit var db: FastinDatabase
    private lateinit var repo: FastingLogRepository

    @Before fun setUp() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, FastinDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = FastingLogRepository(db.fastingLogDao())
    }

    @After fun tearDown() = db.close()

    @Test
    fun `round-trip preserva todos os campos, inclusive os enums e o peso decimal`() = runTest {
        val log = FastingLog(
            date = LocalDate.parse("2026-03-10"),
            lastMealTime = LocalTime.parse("20:30"),
            firstMealTime = LocalTime.parse("12:15"),
            caloricDeficit = Tristate.MAYBE,
            mealQuality = Quality.GOOD,
            water2l = Tristate.YES,
            alcohol = YesNo.NO,
            weight = 82.4,
            notes = "jantar fora",
        )
        repo.save(log)
        assertEquals(log, repo.get(log.date))
    }

    @Test
    fun `salvar duas vezes o mesmo dia atualiza em vez de duplicar`() = runTest {
        val date = LocalDate.parse("2026-03-10")
        repo.save(FastingLog(date, weight = 80.0))
        repo.save(FastingLog(date, weight = 79.5, notes = "pesado em jejum"))

        val all = repo.observeAll().first()
        assertEquals(1, all.size)
        assertEquals(79.5, all.single().weight!!, 0.001)
        assertEquals("pesado em jejum", all.single().notes)
    }

    @Test
    fun `campos nao preenchidos continuam nulos - nada de default disfarcado`() = runTest {
        val date = LocalDate.parse("2026-03-10")
        repo.save(FastingLog(date, weight = 80.0))

        val stored = repo.get(date)!!
        assertNull(stored.lastMealTime)
        assertNull(stored.firstMealTime)
        assertNull(stored.caloricDeficit)
        assertNull(stored.mealQuality)
        assertNull(stored.water2l)
        assertNull(stored.alcohol)
        assertNull(stored.notes)
    }

    @Test
    fun `salvar um dia esvaziado apaga a linha, para o calendario nao marcar ponto`() = runTest {
        val date = LocalDate.parse("2026-03-10")
        repo.save(FastingLog(date, weight = 80.0))
        assertEquals(1, repo.observeAll().first().size)

        repo.save(FastingLog(date)) // usuário limpou tudo e salvou
        assertTrue(repo.observeAll().first().isEmpty())
    }

    @Test
    fun `notes so com espacos conta como vazio`() = runTest {
        val date = LocalDate.parse("2026-03-10")
        repo.save(FastingLog(date, notes = "   "))
        assertTrue(repo.observeAll().first().isEmpty())
    }

    @Test
    fun `observeRange respeita o intervalo inclusivo e a ordem cronologica`() = runTest {
        listOf("2026-02-28", "2026-03-01", "2026-03-15", "2026-03-31", "2026-04-01")
            .forEach { repo.save(FastingLog(LocalDate.parse(it), weight = 80.0)) }

        val march = repo.observeRange(
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-31"),
        ).first()

        assertEquals(
            listOf("2026-03-01", "2026-03-15", "2026-03-31"),
            march.map { it.date.toString() },
        )
    }

    @Test
    fun `base vazia devolve lista vazia e nao null`() = runTest {
        assertTrue(repo.observeAll().first().isEmpty())
        assertTrue(repo.observeAllByDate().first().isEmpty())
        assertNull(repo.get(LocalDate.parse("2026-03-10")))
    }
}
