# TASK-Kaleugit-EP-002-03 - Swipe entre as abas

- Status: COMPLETED
- Priority: 2
- Description: Cada aba era um destino do `NavHost` e so o toque na barra trocava de tela. O usuario pediu "deslizar para o lado troca de tela". Substituir os tres destinos por um unico `home` com `HorizontalPager` (`ui/HomeTabs.kt`), barra inferior lendo `pagerState.currentPage`, `BackHandler` levando ao calendario. O formulario do dia continua rota separada.
- Depends On: None
- Blocked By: None
- Branch: TASK-Kaleugit-EP-002-03-implement
- Workstreams: None
- Execution Mode: Standard
- Last Updated: 2026-09-03
- Completed: 2026-09-03
- Evidence: PASS — HomeTabsTest 6/6 (swipe esquerdo, dois swipes e volta, swipe direito no calendario e no-op, toque na barra, swipe travado com editor aberto)
- prior-art: app/src/main/java/dev/kaleu/fastin/ui/FastinNavHost.kt — `BottomBar` movida sem alteracao visual para `HomeTabs.kt`

## Contexto de entrada
- app/src/main/java/dev/kaleu/fastin/ui/FastinNavHost.kt
- app/src/main/java/dev/kaleu/fastin/ui/dashboard/DashboardScreen.kt

## Expected Output
- `ui/HomeTabs.kt` com `enum HomeTab` e composable `HomeTabs(pagerState, userScrollEnabled, page)`.
- `FastinNavHost` com destinos `home` e `entry/{date}`; `DashboardViewModel` criado no `home` para travar o swipe com o editor de card aberto.
- `HomeTabsTest` com paginas de mentira.

## Escalation
- Ao humano: se o pager exigir mexer em `MainActivity` (ADR-002).
