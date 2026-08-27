package dev.kaleu.fastin

import android.content.Context
import dev.kaleu.fastin.data.backup.CsvBackup
import dev.kaleu.fastin.data.db.FastinDatabase
import dev.kaleu.fastin.data.prefs.DashboardConfigStore
import dev.kaleu.fastin.data.prefs.NotificationPrefsStore
import dev.kaleu.fastin.data.prefs.dashboardDataStoreOf
import dev.kaleu.fastin.data.prefs.notificationPrefsDataStoreOf
import dev.kaleu.fastin.data.repo.FastingLogRepository
import java.time.Clock

/**
 * Grafo de dependências explícito (ADR-003). [clock] é campo para os testes poderem
 * congelar o tempo sem tocar em produção.
 */
class AppContainer(
    private val context: Context,
    val clock: Clock = Clock.systemDefaultZone(),
) {
    private val db = FastinDatabase.get(context)

    val fastingLogRepository = FastingLogRepository(db.fastingLogDao())
    val dashboardConfigStore = DashboardConfigStore(dashboardDataStoreOf(context))
    val notificationPrefsStore = NotificationPrefsStore(notificationPrefsDataStoreOf(context))
    val csvBackup = CsvBackup(context, fastingLogRepository)
}
