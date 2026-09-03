# TASK-Kaleugit-EP-002-01 - Lista unica de marcos, persistida e compartilhada

- Status: COMPLETED
- Priority: 1
- Description: Relogio e notificacoes usam duas listas fixas e diferentes (`MILESTONE_HOURS` = 16/18/20/24, `NOTIFIED_HOURS` = 16/18/20). O usuario quer uma lista so, escolhida por ele entre 12h e 48h. Criar `MilestoneHours` no dominio, persistir a escolha em `NotificationPrefsStore` e fazer calculador, notifier, Application e ViewModel do relogio lerem dela.
- Depends On: None
- Blocked By: None
- Branch: TASK-Kaleugit-EP-002-01-implement
- Workstreams: None
- Execution Mode: Standard
- Last Updated: 2026-09-03
- Completed: 2026-09-03
- Evidence: PASS — MilestoneNotifierTest, SettingsNotificationsTest (persistencia de horas), MilestoneHoursTest; suite completa verde (ver HANDOFF.md)
- prior-art: app/src/main/java/dev/kaleu/fastin/data/prefs/NotificationPrefsStore.kt — padrao de DataStore injetado, estendido com `stringSetPreferencesKey`

## Contexto de entrada
- app/src/main/java/dev/kaleu/fastin/domain/fasting/FastingCalculator.kt
- app/src/main/java/dev/kaleu/fastin/notify/MilestoneNotifier.kt
- app/src/main/java/dev/kaleu/fastin/data/prefs/NotificationPrefsStore.kt
- app/src/main/java/dev/kaleu/fastin/FastinApplication.kt
- app/src/main/java/dev/kaleu/fastin/ui/clock/FastingClockViewModel.kt

## Expected Output
- `domain/model/MilestoneHours.kt` com `OPTIONS`, `DEFAULT`, `MIN`, `MAX`, `sanitize()`.
- `FastingCalculator.milestones(window, now, hours)`; `MilestoneNotifier.pendingMilestones(start, now, hours)` e `reschedule(context, start, now, hours)`; cancelamento cobre todas as opcoes.
- `NotificationPrefsStore.milestoneHours: Flow<List<Int>>` e `setMilestoneHours()`.
- `FastinApplication` reagenda quando a lista muda (combine com o Room).
- `FastingClockViewModel(repo, clock, milestoneHours: Flow<List<Int>>)`.

## Escalation
- Ao humano: se exigir migracao de Room.
- Ao orquestrador: se passar de 6 arquivos de producao.
