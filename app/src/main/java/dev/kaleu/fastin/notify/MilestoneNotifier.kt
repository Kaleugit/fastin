package dev.kaleu.fastin.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.kaleu.fastin.FastinApplication
import dev.kaleu.fastin.R
import dev.kaleu.fastin.domain.fasting.FastingCalculator
import dev.kaleu.fastin.domain.model.MilestoneHours
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Notificação local ao bater os marcos de jejum escolhidos pelo usuário (PROJECT.md §4.5,
 * EP-002). A lista de horas é a **mesma** que o relógio mostra — [MilestoneHours].
 *
 * `WorkManager`, não `AlarmManager`: sobrevive a reboot sem `BOOT_COMPLETED` próprio e não
 * precisa de `SCHEDULE_EXACT_ALARM` (que no Android 13+ exige concessão do usuário). Um
 * lembrete de jejum não precisa ser exato ao segundo — alguns minutos de folga são
 * aceitáveis e custam muito menos bateria.
 *
 * Zero rede: nada aqui usa Play Services nem internet.
 */
object MilestoneNotifier {

    const val CHANNEL_ID = "fasting_milestones"
    private const val WORK_PREFIX = "milestone-"

    /** Canal sempre existe: `minSdk` 26 já é o Android 8, onde canais são obrigatórios. */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Marcos de jejum",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisa quando o jejum atinge os marcos escolhidos em Ajustes"
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Um marco a agendar: quantas horas ele representa e daqui a quanto tempo dispara. */
    data class Pending(val hours: Int, val delay: Duration)

    /**
     * **Decisão pura de agendamento**, separada do WorkManager de propósito: é a parte que
     * pode estar errada de um jeito que o usuário sente (aviso retroativo, aviso na hora
     * errada), e é a parte que dá para testar sem infraestrutura.
     *
     * @param start início da janela de jejum, ou null se não há jejum em andamento.
     * @param hours marcos escolhidos pelo usuário, em horas.
     * @return marcos ainda no futuro, em ordem crescente. Vazio se não há o que agendar.
     */
    fun pendingMilestones(
        start: Instant?,
        now: Instant,
        hours: List<Int> = MilestoneHours.DEFAULT,
    ): List<Pending> {
        if (start == null) return emptyList()
        // Jejum abandonado não deve disparar avisos ao ser reaberto dias depois.
        if (Duration.between(start, now) > FastingCalculator.MAX_PLAUSIBLE) return emptyList()

        return hours.sorted().mapNotNull { h ->
            val delay = Duration.between(now, start.plus(Duration.ofHours(h.toLong())))
            // Marco já batido não vira notificação retroativa.
            if (delay.isNegative || delay.isZero) null else Pending(h, delay)
        }
    }

    /**
     * Reagenda todos os marcos a partir da janela em andamento. Chamar sempre que a última
     * refeição **ou a lista de marcos** mudar — reagendar é idempotente por causa de
     * [ExistingWorkPolicy.REPLACE].
     *
     * Cancela **todas** as opções, não só as escolhidas: desmarcar 20h precisa matar o worker
     * de 20h que foi agendado quando ele ainda estava marcado.
     */
    fun reschedule(context: Context, start: Instant?, now: Instant, hours: List<Int>) {
        val wm = WorkManager.getInstance(context)
        MilestoneHours.OPTIONS.forEach { wm.cancelUniqueWork("$WORK_PREFIX$it") }

        pendingMilestones(start, now, hours).forEach { pending ->
            wm.enqueueUniqueWork(
                "$WORK_PREFIX${pending.hours}",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<MilestoneWorker>()
                    .setInitialDelay(pending.delay.toMinutes(), TimeUnit.MINUTES)
                    .setInputData(workDataOf(MilestoneWorker.KEY_HOURS to pending.hours))
                    .build(),
            )
        }
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        MilestoneHours.OPTIONS.forEach { wm.cancelUniqueWork("$WORK_PREFIX$it") }
    }
}

/**
 * Worker do marco. Revalida o jejum antes de notificar: entre o agendamento e o disparo o
 * usuário pode ter registrado a primeira refeição, e avisar "você bateu 18h" depois de ele
 * ter comido seria simplesmente errado.
 */
class MilestoneWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val hours = inputData.getInt(KEY_HOURS, 0)
        if (hours == 0) return Result.failure()

        val container = (applicationContext as? FastinApplication)?.container ?: return Result.success()
        val logs = container.fastingLogRepository.observeAllByDate().first()
        val now = container.clock.instant()

        val window = FastingCalculator.currentWindow(logs, now, container.clock.zone)
            ?: return Result.success() // jejum encerrado ou abandonado: nada a dizer

        val elapsed = FastingCalculator.elapsed(window, now)
        if (elapsed < Duration.ofHours(hours.toLong())) return Result.success()

        notify(hours)
        return Result.success()
    }

    private fun notify(hours: Int) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return // sem permissão: silêncio, nunca crash
        }

        MilestoneNotifier.ensureChannel(ctx)
        val notification = NotificationCompat.Builder(ctx, MilestoneNotifier.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${hours}h de jejum")
            .setContentText("Você atingiu ${hours} horas de jejum.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(ctx).notify(hours, notification) }
    }

    companion object {
        const val KEY_HOURS = "hours"
    }
}
