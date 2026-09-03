# TASK-Kaleugit-EP-002-04 - Ajustes: escolha dos marcos e icone de engrenagem

- Status: COMPLETED
- Priority: 3
- Description: Ajustes so tinha o toggle liga/desliga das notificacoes e a aba usava o icone de relogio. Adicionar um card "Marcos de jejum" com grade de chips de multipla escolha (12h a 48h), sempre visivel porque tambem define o relogio; e trocar o icone da aba por uma engrenagem desenhada a mao (design-system §7).
- Depends On: TASK-Kaleugit-EP-002-01, TASK-Kaleugit-EP-002-03
- Blocked By: None
- Branch: TASK-Kaleugit-EP-002-04-implement
- Workstreams: None
- Execution Mode: Standard
- Last Updated: 2026-09-03
- Completed: 2026-09-03
- Evidence: PASS — SettingsNotificationsTest (toque em 12h acrescenta, toque em 16h remove, ambos no disco); MilestoneHoursTest (linhas 5+4 e 3+3); ScreenshotTest regenerou 05-ajustes.png
- prior-art: app/src/main/java/dev/kaleu/fastin/ui/components/ChoiceChips.kt — `ChoiceChip` reaproveitado com `Role.Checkbox`

## Contexto de entrada
- app/src/main/java/dev/kaleu/fastin/ui/settings/SettingsScreen.kt
- app/src/main/java/dev/kaleu/fastin/ui/settings/SettingsViewModel.kt
- app/src/main/java/dev/kaleu/fastin/ui/components/ChoiceChips.kt
- app/src/main/java/dev/kaleu/fastin/ui/theme/FastinIcons.kt
- app/src/main/java/dev/kaleu/fastin/ui/HomeTabs.kt

## Expected Output
- `ToggleChipGrid` + `balancedChunks` em `ChoiceChips.kt`.
- `SettingsScreen(onToggleMilestoneHour)` com card "Marcos de jejum" acima de "Notificacoes"; chips com tag `hour_N`.
- `SettingsUiState.milestoneHours`; `SettingsViewModel.toggleMilestoneHour()`.
- `FastinIcons.Gear`; `HomeTab.SETTINGS` usa ele; `design-system.md` §7 atualizado.

## Escalation
- Ao humano: se a grade exigir `material-icons-extended` ou cor fora de `ui/theme/`.
