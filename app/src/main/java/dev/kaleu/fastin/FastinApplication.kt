package dev.kaleu.fastin

import android.app.Application
import dev.kaleu.fastin.domain.fasting.FastingCalculator
import dev.kaleu.fastin.notify.MilestoneNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FastinApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val scope = CoroutineScope(SupervisorJob())

    /**
     * Coletor do reagendamento. Guardado para poder ser cancelado: sem isso, ligar as
     * notificações duas vezes deixaria dois coletores vivos reagendando os mesmos workers.
     */
    private var rescheduleJob: Job? = null

    /**
     * Ligado pela tela de Ajustes. Enquanto estiver desligado, nada é agendado — o usuário
     * que não quer aviso não deve ter trabalho em background nenhum.
     *
     * Espelho em memória do que está no [NotificationPrefsStore]. A fonte da verdade é o
     * disco: antes este campo *era* a única cópia, e cada morte do processo desligava as
     * notificações sozinha.
     */
    @Volatile
    var notificationsEnabled: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        MilestoneNotifier.ensureChannel(this)

        // Restaura a escolha do usuário. Sem isto o app subia sempre com as notificações
        // desligadas e ninguém reagendava os marcos até alguém abrir Ajustes e tocar no
        // toggle — que era exatamente o bug relatado.
        scope.launch {
            // Só age quando está **ligado**. Chamar o caminho de desligar aqui faria todo
            // boot tocar o WorkManager para cancelar o que este processo nunca agendou —
            // trabalho à toa e um caminho de crash no `onCreate` (que mataria o app inteiro)
            // em qualquer ambiente onde o WorkManager não suba.
            if (container.notificationPrefsStore.enabled.first()) {
                // Falha de agendamento não pode impedir o app de abrir: o usuário perderia o
                // acesso aos próprios dados por causa de um aviso de jejum.
                runCatching { applyNotificationsEnabled(true) }
            }
        }
    }

    /** Grava a escolha **e** aplica. Único caminho a partir da UI. */
    fun setNotificationsEnabled(enabled: Boolean) {
        scope.launch { container.notificationPrefsStore.setEnabled(enabled) }
        applyNotificationsEnabled(enabled)
    }

    /**
     * Aplica o efeito sem gravar. Separado de [setNotificationsEnabled] porque a restauração
     * na abertura do app lê do disco: regravar o que acabou de ser lido seria escrita à toa.
     */
    private fun applyNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
        rescheduleJob?.cancel()
        rescheduleJob = null

        if (!enabled) {
            MilestoneNotifier.cancelAll(this)
            return
        }
        rescheduleJob = scope.launch {
            // Reagenda a partir do jejum que estiver correndo agora **e** dos marcos
            // escolhidos em Ajustes: mudar qualquer um dos dois reagenda na hora. Sem jejum
            // em andamento, reschedule() apenas limpa o que houver pendente.
            combine(
                container.fastingLogRepository.observeAllByDate(),
                container.notificationPrefsStore.milestoneHours,
            ) { logs, hours -> logs to hours }.collect { (logs, hours) ->
                val now = container.clock.instant()
                val window = FastingCalculator.currentWindow(logs, now, container.clock.zone)
                MilestoneNotifier.reschedule(this@FastinApplication, window?.start, now, hours)
            }
        }
    }
}
