package dev.kaleu.fastin.domain.model

/**
 * Marcos de jejum que o usuário pode escolher (EP-002).
 *
 * Uma lista só para as duas coisas que dependem dela — os pills do relógio e as notificações
 * — porque o usuário pediu exatamente isso: "as horas que aparecem ali embaixo são as horas
 * das notificações que eu configurei". Duas listas divergiriam com o tempo.
 *
 * Opções fechadas, não campo livre: de 12h a 48h em passos que fazem sentido para jejum
 * intermitente. Entre 24h e 48h só 36h — jejuns nessa faixa são raros e um marco a cada
 * duas horas viraria uma parede de chips.
 */
object MilestoneHours {

    const val MIN = 12
    const val MAX = 48

    val OPTIONS: List<Int> = listOf(12, 14, 16, 18, 20, 22, 24, 36, 48)

    /**
     * Preserva o que a v1.2 mostrava no relógio (PROJECT.md §3.3). 24h passa a notificar
     * também — decisão registrada em EPICOS.md (DA-011).
     */
    val DEFAULT: List<Int> = listOf(16, 18, 20, 24)

    /**
     * Normaliza um conjunto vindo do disco ou da UI: descarta o que não é opção e devolve
     * em ordem crescente. Vazio é válido — quem não quer marco nenhum não vê pill nem recebe
     * aviso.
     */
    fun sanitize(raw: Collection<Int>): List<Int> =
        raw.filter { it in OPTIONS }.distinct().sorted()
}
