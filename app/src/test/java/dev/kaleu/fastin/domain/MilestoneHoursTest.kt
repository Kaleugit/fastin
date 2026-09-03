package dev.kaleu.fastin.domain

import dev.kaleu.fastin.domain.model.MilestoneHours
import dev.kaleu.fastin.ui.components.balancedChunks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lista única de marcos (EP-002) e a divisão em linhas dos chips que a exibem. */
class MilestoneHoursTest {

    @Test
    fun `opcoes ficam entre 12h e 48h em ordem crescente`() {
        assertEquals(MilestoneHours.OPTIONS.sorted(), MilestoneHours.OPTIONS)
        assertTrue(MilestoneHours.OPTIONS.all { it in MilestoneHours.MIN..MilestoneHours.MAX })
        assertEquals(MilestoneHours.MIN, MilestoneHours.OPTIONS.first())
        assertEquals(MilestoneHours.MAX, MilestoneHours.OPTIONS.last())
    }

    /** O default precisa ser selecionável — senão a tela nasceria com chips que não existem. */
    @Test
    fun `default e subconjunto das opcoes e reproduz a v1_2`() {
        assertTrue(MilestoneHours.OPTIONS.containsAll(MilestoneHours.DEFAULT))
        assertEquals(listOf(16, 18, 20, 24), MilestoneHours.DEFAULT)
    }

    @Test
    fun `sanitize descarta o que nao e opcao, remove repetido e ordena`() {
        assertEquals(listOf(12, 16, 48), MilestoneHours.sanitize(listOf(48, 16, 13, 16, 12, 0, 99)))
        assertTrue(MilestoneHours.sanitize(emptyList()).isEmpty())
    }

    // --- Linhas equilibradas dos chips ------------------------------------------------------

    @Test
    fun `nove opcoes viram 5 mais 4, nao 5 mais 4 esticado nem 5 5 e sobra`() {
        val rows = balancedChunks((1..9).toList(), maxPerRow = 5)
        assertEquals(listOf(5, 4), rows.map { it.size })
    }

    @Test
    fun `seis itens viram 3 mais 3`() {
        assertEquals(listOf(3, 3), balancedChunks((1..6).toList(), maxPerRow = 5).map { it.size })
    }

    @Test
    fun `ate o maximo cabe numa linha so`() {
        assertEquals(listOf(5), balancedChunks((1..5).toList(), maxPerRow = 5).map { it.size })
        assertTrue(balancedChunks(emptyList<Int>(), maxPerRow = 5).isEmpty())
    }
}
