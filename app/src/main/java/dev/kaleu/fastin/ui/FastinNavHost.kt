package dev.kaleu.fastin.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.kaleu.fastin.AppContainer
import dev.kaleu.fastin.ui.calendar.CalendarScreen
import dev.kaleu.fastin.ui.calendar.CalendarViewModel
import dev.kaleu.fastin.ui.clock.FastingClockCard
import dev.kaleu.fastin.ui.clock.FastingClockViewModel
import dev.kaleu.fastin.ui.components.pressable
import dev.kaleu.fastin.ui.dashboard.DashboardScreen
import dev.kaleu.fastin.ui.dashboard.DashboardViewModel
import dev.kaleu.fastin.ui.entry.DayEntryScreen
import dev.kaleu.fastin.ui.entry.DayEntryViewModel
import dev.kaleu.fastin.ui.settings.SettingsScreen
import dev.kaleu.fastin.ui.settings.SettingsViewModel
import dev.kaleu.fastin.ui.theme.FastinColors
import dev.kaleu.fastin.ui.theme.FastinIcons
import dev.kaleu.fastin.ui.theme.FastinShapes
import dev.kaleu.fastin.ui.theme.Spacing
import dev.kaleu.fastin.ui.theme.accentGlow
import dev.kaleu.fastin.ui.theme.neumorphic
import java.time.LocalDate

private const val ARG_DATE = "date"
private const val ROUTE_ENTRY = "entry/{$ARG_DATE}"

private fun entryRoute(date: LocalDate) = "entry/$date"

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    CALENDAR("calendar", "Calendário", FastinIcons.Calendar),
    DASHBOARD("dashboard", "Dashboard", FastinIcons.Chart),
    SETTINGS("settings", "Ajustes", FastinIcons.Clock),
}

/**
 * Grafo de navegação. ViewModels criados por factory manual a partir do [container]
 * (ADR-003) — sem Hilt.
 *
 * O formulário do dia é rota separada, não aba: tem "voltar" natural para o calendário e
 * não faz sentido como destino de topo — por isso a barra some nele.
 */
@Composable
fun FastinNavHost(
    container: AppContainer,
    onNotificationsToggled: (Boolean) -> Unit = {},
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = Tab.entries.any { it.route == currentRoute }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(navController = navController, startDestination = Tab.CALENDAR.route) {

                composable(Tab.CALENDAR.route) {
                    val vm: CalendarViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                CalendarViewModel(container.fastingLogRepository, container.clock)
                            }
                        },
                    )
                    val state by vm.uiState.collectAsStateWithLifecycle()

                    // ViewModel próprio para o relógio: o tick de 1s não deve forçar o
                    // calendário a recompor a grade inteira a cada segundo.
                    val clockVm: FastingClockViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                FastingClockViewModel(container.fastingLogRepository, container.clock)
                            }
                        },
                    )
                    val clockState by clockVm.uiState.collectAsStateWithLifecycle()

                    CalendarScreen(
                        state = state,
                        onPreviousMonth = vm::previousMonth,
                        onNextMonth = vm::nextMonth,
                        onDayClick = { date -> navController.navigate(entryRoute(date)) },
                        header = { FastingClockCard(state = clockState, zone = container.clock.zone) },
                    )
                }

                composable(Tab.DASHBOARD.route) {
                    val vm: DashboardViewModel = viewModel(
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

                composable(Tab.SETTINGS.route) {
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

                    // Picker do sistema: sem permissão de armazenamento, o usuário escolhe o
                    // arquivo e o app recebe acesso só àquele URI.
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
                    )
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

        if (showBar) {
            BottomBar(
                current = currentRoute,
                onSelect = { tab ->
                    navController.navigate(tab.route) {
                        // Sem isto, alternar abas empilharia destinos indefinidamente e o
                        // botão voltar percorreria todo o histórico de troca de aba.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

@Composable
private fun BottomBar(current: String?, onSelect: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(FastinColors.surfaceBase)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Tab.entries.forEach { tab ->
            val selected = current == tab.route
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .pressable(onClick = { onSelect(tab) })
                    .testTag("tab_${tab.route}")
                    .then(
                        if (selected) {
                            Modifier
                                .accentGlow(shape = FastinShapes.chip, elevation = 14.dp)
                                .background(FastinColors.accentGradient, FastinShapes.chip)
                        } else {
                            Modifier.neumorphic(
                                shape = FastinShapes.chip,
                                color = FastinColors.surface,
                                elevation = 8.dp,
                            )
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) FastinColors.onAccent else FastinColors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
