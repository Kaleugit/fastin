# TASK-Kaleugit-EP-001-01 - Persistir a preferencia de notificacoes entre sessoes

- Status: PENDING
- Priority: 1
- Description: A preferencia de notificacoes vive so em memoria (`FastinApplication.notificationsEnabled`) e o `SettingsUiState` nasce `false`. Ao fechar o app o toggle volta a desligado e o coletor de reagendamento nunca reinicia. Persistir em DataStore, restaurar no `onCreate` do Application e alimentar a tela de Ajustes a partir do store.
- Depends On: None
- Blocked By: None
- Branch: TASK-Kaleugit-EP-001-01-implement
- Workstreams: None
- Execution Mode: Standard
- Last Updated: 2026-08-27 00:00

## Contexto de entrada
- app/src/main/java/dev/kaleu/fastin/FastinApplication.kt
- app/src/main/java/dev/kaleu/fastin/ui/settings/SettingsViewModel.kt
- app/src/main/java/dev/kaleu/fastin/data/prefs/DashboardConfigStore.kt
- app/src/main/java/dev/kaleu/fastin/AppContainer.kt
- app/src/main/java/dev/kaleu/fastin/notify/MilestoneNotifier.kt

## Expected Output
- `data/prefs/NotificationPrefsStore.kt` com `enabled: Flow<Boolean>` e `setEnabled(Boolean)`, DataStore injetado.
- `notificationPrefsDataStoreOf(context)` usando `preferencesDataStore(name = "notifications")`.
- Store registrado em `AppContainer`; `FastinApplication.onCreate` restaura o agendamento quando o valor persistido e `true`.
- `SettingsViewModel` coleta o store em vez de assumir `false`.
- `NotificationPrefsStoreTest` com par positivo/negativo, arquivo de DataStore proprio por teste.
- `./gradlew.bat test` com 0 falhas.

## Escalation
- Ao humano: se restaurar apos reboot exigir `BOOT_COMPLETED` proprio.
- Ao orquestrador: se a mudanca alcancar mais de 6 arquivos.
