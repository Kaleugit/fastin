# TASK-Kaleugit-EP-001-02 - Inverter a ordem dos campos de refeicao no formulario

- Status: PENDING
- Priority: 2
- Description: No card "Jejum" do formulario do dia, "Ultima refeicao" e composta antes de "Primeira refeicao", contrariando a ordem cronologica do dia. Inverter apenas a ordem de composicao dos dois `TimeField`.
- Depends On: None
- Blocked By: None
- Branch: TASK-Kaleugit-EP-001-02-implement
- Workstreams: None
- Execution Mode: Quick
- Last Updated: 2026-08-27 00:00

## Contexto de entrada
- app/src/main/java/dev/kaleu/fastin/ui/entry/DayEntryScreen.kt
- app/src/test/java/dev/kaleu/fastin/ui/DayEntryScreenTest.kt
- docs/PROJECT.md

## Expected Output
- Em `DayEntryScreen.kt`, `TimeField` de `firstMealTime` composto antes do de `lastMealTime`.
- Teste em `DayEntryScreenTest` afirmando `positionInRoot.y` de `firstMealTime` menor que a de `lastMealTime`.
- `testTag`, callbacks, `DayEntryUiState` e schema inalterados.
- Screenshots regeneradas em `docs/screenshots/`.
- `./gradlew.bat test` com 0 falhas.

## Escalation
- Ao humano: se a inversao visual sugerir inverter a semantica do calculo do jejum. NAO inverter: a regra e `lastMealTime(D-1) -> firstMealTime(D)` (PROJECT.md 2).
- Ao orquestrador: nenhuma prevista.
