# TASK-Kaleugit-EP-002-02 - Card do relogio com metade da altura

- Status: COMPLETED
- Priority: 2
- Description: O card do relogio tem ~386dp e o calendario ~442dp; numa tela de 891dp o calendario nunca aparece inteiro sem rolar. Encolher o anel (220dp → 140dp, `clock` 56sp → 40sp) e levar os marcos para uma lista a direita do anel. Com o default o card fica em ~214dp.
- Depends On: TASK-Kaleugit-EP-002-01
- Blocked By: None
- Branch: TASK-Kaleugit-EP-002-02-implement
- Workstreams: None
- Execution Mode: Standard
- Last Updated: 2026-09-03
- Completed: 2026-09-03
- Evidence: PASS — FastingClockTest (10 testes, incluindo o par novo de marcos escolhidos / lista vazia); ScreenshotTest regenerou 01-calendario.png
- prior-art: app/src/main/java/dev/kaleu/fastin/ui/clock/FastingClockCard.kt — anel e pills preservados; so o arranjo mudou

## Contexto de entrada
- app/src/main/java/dev/kaleu/fastin/ui/clock/FastingClockCard.kt
- app/src/main/java/dev/kaleu/fastin/ui/theme/Type.kt
- docs/design-system.md
- docs/screenshots/01-calendario.png

## Expected Output
- `FastingClockCard`: `Row` com anel a esquerda e `MilestoneList` a direita (duas colunas acima de 5 marcos); tag `milestonesEmpty` quando nao ha marco.
- `FastinType.clock` em 40sp/44sp; `design-system.md` §2 atualizado.
- Tags `clockValue`, `clockSeconds`, `milestone_N` e descricoes "batido"/"pendente" preservadas.

## Escalation
- Ao humano: se precisar de tamanho literal novo fora de `ui/theme/` alem dos que o card ja tinha.
