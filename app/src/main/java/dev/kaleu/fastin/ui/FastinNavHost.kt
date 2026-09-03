package dev.kaleu.fastin.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.kaleu.fastin.AppContainer
import dev.kaleu.fastin.ui.calendar.CalendarScreen
import dev.kaleu.fastin.ui.calendar.CalendarViewModel
import dev.kaleu.fastin.ui.clock.FastingClockCard
import dev.kaleu.fastin.ui.clock.FastingClockViewModel
import dev.kaleu.fastin.ui.dashboard.DashboardScreen
import dev.kaleu.fastin.ui.dashboard.DashboardViewModel
import dev.kaleu.fastin.ui.entry.DayEntryScreen
import dev.kaleu.fastin.ui.entry.DayEntryViewModel
import dev.kaleu.fastin.ui.settings.SettingsScreen
import dev.kaleu.fastin.ui.settings.SettingsViewModel
import java.time.LocalDate

private const val ROUTE_HOME = "home"
private const val ARG_DATE = "date"
private const val ROUTE_ENTRY = "entry/{$ARG_DATE}"

private fun entryRoute(date: LocalDate) = "entry/$date"

/**
 * Grafo de navegação. ViewModels criados por factory manual a partir do [container]
 * (ADR-003) — sem Hilt.
 *
 * Dois destinos: **home**, que é o pager das três abas ([HomeTabs]), e o formulário do dia.
 * O formulário é rota separada, não aba: tem "voltar" natural para o calendário e não faz
 * sentido como destino de topo — por isso a barra some nele.
 *
 * Os ViewModels das abas pertencem ao destino `home`, então trocar de aba (ou abrir o
 * formulário e voltar) não os recria; só a composição da página sai de cena.
 */
@Composable
fun FastinNavHost(
    container: AppContainer,
    onNotificationsToggled: (Boolean) -> Unit = {},
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_HOME) {

        composable(ROUTE_HOME) {
            val pagerState = rememberPagerState { HomeTab.entries.size }

            // O ViewModel do dashboard é criado aqui, um nível acima da página, porque o
            // pager precisa saber se o editor de card está aberto para travar o swipe.
            val dashboardVm: DashboardViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        DashboardViewModel(
                            container.fastingLogRepository,
                            container.dashboardConfigStore,
                            container.clock,
                        )
                    }
                },
            )
            val editor by dashboardVm.editor.collectAsStateWithLifecycle()

            HomeTabs(pagerState = pagerState, userScrollEnabled = editor == null) { tab ->
                when (tab) {
                    HomeTab.CALENDAR -> CalendarTab(
                        container = container,
                        onDayClick = { date -> navController.navigate(entryRoute(date)) },
                    )
                    HomeTab.DASHBOARD -> DashboardTab(vm = dashboardVm)
                    HomeTab.SETTINGS -> SettingsTab(
                        container = container,
                        onNotificationsToggled = onNotificationsToggled,
                    )
                }
            }
        }

        composable(
            route = ROUTE_ENTRY,
            arguments = listOf(navArgument(ARG_DATE) { type = NavType.StringType }),
        ) { backStackEntry ->
            // A data vem da rota como ISO-8601. Se vier corrompida, cair em "hoje" é
            // melhor que derrubar o app numa tela de formulário.
            val date = runCatching {
                LocalDate.parse(backStackEntry.arguments?.getString(ARG_DATE))
            }.getOrElse { LocalDate.now(container.clock) }

            val vm: DayEntryViewModel = viewModel(
                key = "entry-$date",
                factory = viewModelFactory {
                    initializer { DayEntryViewModel(container.fastingLogRepository, date) }
                },
            )
            val state by vm.uiState.collectAsStateWithLifecycle()

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
                onSave = { vm.save(onDone = { navController.popBackStack() }) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun CalendarTab(container: AppContainer, onDayClick: (LocalDate) -> Unit) {
    val vm: CalendarViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CalendarViewModel(container.fastingLogRepository, container.clock) }
        },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()

    // ViewModel próprio para o relógio: o tick de 1s não deve forçar o calendário a
    // recompor a grade inteira a cada segundo.
    val clockVm: FastingClockViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                FastingClockViewModel(
                    repository = container.fastingLogRepository,
                    clock = container.clock,
                    milestoneHours = container.notificationPrefsStore.milestoneHours,
                )
            }
        },
    )
    val clockState by clockVm.uiState.collectAsStateWithLifecycle()

    CalendarScreen(
        state = state,
        onPreviousMonth = vm::previousMonth,
        onNextMonth = vm::nextMonth,
        onDayClick = onDayClick,
        header = { FastingClockCard(state = clockState, zone = container.clock.zone) },
    )
}

@Composable
private fun DashboardTab(vm: DashboardViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val editor by vm.editor.collectAsStateWithLifecycle()

    DashboardScreen(
        state = state,
        editor = editor,
        onAdd = vm::startAdd,
        onEdit = vm::startEdit,
        onRemove = vm::removeCard,
        onDraftChange = vm::updateDraft,
        onConfirmEdit = vm::confirmEdit,
        onCancelEdit = vm::cancelEdit,
    )
}

@Composable
private fun SettingsTab(container: AppContainer, onNotificationsToggled: (Boolean) -> Unit) {
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.fastingLogRepository,
                    container.csvBackup,
                    container.clock,
                    container.notificationPrefsStore,
                    onNotificationsToggled,
                )
            }
        },
    )
    val state by vm.uiState.collectAsStateWithLifecycle()

    // Picker do sistema: sem permissão de armazenamento, o usuário escolhe o arquivo e o
    // app recebe acesso só àquele URI.
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::import) }

    SettingsScreen(
        state = state,
        onExport = vm::export,
        onPickImport = {
            picker.launch(
                arrayOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "text/plain",
                    "*/*",
                ),
            )
        },
        onToggleNotifications = vm::setNotificationsEnabled,
        onToggleMilestoneHour = vm::toggleMilestoneHour,
    )
}
