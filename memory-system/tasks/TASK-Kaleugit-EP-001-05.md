# TASK-Kaleugit-EP-001-05 - Avisar sobre alteracoes nao salvas ao sair do formulario

- Status: COMPLETED
- Priority: 1
- Description: `onBack` chama `popBackStack()` direto e nada compara o estado atual com o carregado, entao horario informado e perdido em silencio. Rastrear alteracao pendente no ViewModel e exigir decisao do usuario ao sair. Sem autosave (DA-006).
- Depends On: TASK-Kaleugit-EP-001-02
- Blocked By: None
- Branch: TASK-Kaleugit-EP-001-05-implement
- Workstreams: None
- Execution Mode: Critical
- Last Updated: 2026-08-27 14:30
- Completed: 2026-08-27 14:30
- Evidence: PASS — DayEntryScreenTest 6 casos novos (aviso, ausência de aviso, descartar não grava, salvar grava, cancelar, pós-save) + DayEntryLoadRaceTest sem regressão
- prior-art: app/src/main/java/dev/kaleu/fastin/ui/entry/DayEntryViewModel.kt:135 — guarda de isLoading no save, reusada como base de hasUnsavedChanges

## Contexto de entrada
- app/src/main/java/dev/kaleu/fastin/ui/entry/DayEntryViewModel.kt
- app/src/main/java/dev/kaleu/fastin/ui/entry/DayEntryScreen.kt
- app/src/main/java/dev/kaleu/fastin/ui/FastinNavHost.kt
- app/src/test/java/dev/kaleu/fastin/ui/DayEntryLoadRaceTest.kt
- HANDOFF.md

## Expected Output
- `DayEntryViewModel` guarda o snapshot carregado e expoe `hasUnsavedChanges`.
- `hasUnsavedChanges` e `false` enquanto `isLoading` e `true`.
- `save()` bem-sucedido atualiza o snapshot e zera o estado sujo.
- Dialogo com tres saidas: Salvar e sair / Sair sem salvar / Cancelar, cobrindo tambem o `BackHandler` do sistema.
- Testes: par positivo/negativo do dialogo; "Sair sem salvar" NAO grava; dialogo nao aparece durante `isLoading`.
- `DayEntryLoadRaceTest` continua passando.
- `./gradlew.bat test` com 0 falhas.

## Escalation
- Ao humano: se surgir pedido de autosave. A decisao esta tomada em DA-006: `@Upsert` grava a linha inteira e formulario vazio apaga o dia.
- Ao orquestrador: se o `BackHandler` conflitar com a navegacao por abas do `FastinNavHost`.
