package dev.kaleu.fastin

import android.app.Application
import dev.kaleu.fastin.domain.fasting.FastingCalculator
import dev.kaleu.fastin.notify.MilestoneNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
     */
    @Volatile
    var notificationsEnabled: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        MilestoneNotifier.ensureChannel(this)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
        rescheduleJob?.cancel()
        rescheduleJob = null

        if (!enabled) {
            MilestoneNotifier.cancelAll(this)
            return
        }
        rescheduleJob = scope.launch {
            // Reagenda a partir do jejum que estiver correndo agora. Sem jejum em
            // andamento, reschedule() apenas limpa o que houver pendente.
            container.fastingLogRepository.observeAllByDate().collect { logs ->
                val now = container.clock.instant()
                val window = FastingCalculator.currentWindow(logs, now, container.clock.zone)
                MilestoneNotifier.reschedule(this@FastinApplication, window?.start, now)
            }
        }
    }
}
