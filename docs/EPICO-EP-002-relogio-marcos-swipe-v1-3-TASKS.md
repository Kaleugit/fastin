# EPICO-EP-002-relogio-marcos-swipe-v1-3-TASKS

## Metadata
- Epic ID: EP-002
- Epic Title: Relógio compacto, marcos configuráveis e swipe entre abas (v1.3)
- Last Updated: 2026-09-03
- Owner: Architect
- Status: APPROVED

## Epic Context
- Objective: atender os quatro pedidos do usuário após uma semana de uso da v1.2 e entregar
  a v1.3 instalável por cima. O pedido chegou como "v1.0.3"; a versão instalada é a v1.2
  (versionCode 4), então a entrega é **v1.3 / versionCode 5**.
- Scope Boundaries: sem mudança de schema Room, sem permissão nova no manifest, sem tocar
  em `MainActivity`. O anel de progresso continua mirando 24h.
- Key Dependencies: a task 01 define o contrato (`MilestoneHours` + `Flow<List<Int>>` no
  store) que 02 e 04 consomem. 03 é independente. 05 fecha.

## Diagnóstico prévio

| Pedido | O que existe hoje | Arquivo |
|---|---|---|
| Card do relógio 50% mais curto, calendário inteiro | Anel de 220dp empilhado sobre a linha de marcos: card com ~386dp; o calendário tem ~442dp e não cabe junto numa tela de 891dp | `FastingClockCard.kt`, `Type.kt` (`clock` 56sp) |
| Marcos do relógio = horas das notificações | Duas listas fixas e diferentes: `FastingCalculator.MILESTONE_HOURS = [16,18,20,24]` e `MilestoneNotifier.NOTIFIED_HOURS = [16,18,20]` | `FastingCalculator.kt:21`, `MilestoneNotifier.kt:42` |
| Escolher quais marcos (12h a 48h) | Nenhuma preferência; só o toggle liga/desliga em `NotificationPrefsStore` | `NotificationPrefsStore.kt`, `SettingsScreen.kt` |
| Swipe troca de tela | Cada aba é destino do `NavHost`; só a barra troca | `FastinNavHost.kt` |
| Engrenagem em Ajustes | `Tab.SETTINGS` usa `FastinIcons.Clock` | `FastinNavHost.kt:62`, `FastinIcons.kt` |

## Approved Task List

### Task 01 - Lista única de marcos, persistida e compartilhada
- Task ID: TASK-Kaleugit-EP-002-01
- Status: COMPLETED
- Priority: 1
- Execution Mode: Standard
- Domain: Domínio + preferências + notificações
- Description:
  - Criar `domain/model/MilestoneHours.kt`: `OPTIONS` (12, 14, 16, 18, 20, 22, 24, 36, 48),
    `DEFAULT` (16, 18, 20, 24), `MIN`/`MAX`, `sanitize()`.
  - `FastingCalculator.milestones(window, now, hours = DEFAULT)`; remover `MILESTONE_HOURS`.
  - `MilestoneNotifier.pendingMilestones(start, now, hours)` e `reschedule(..., hours)`;
    `cancelAll`/`reschedule` cancelam **todas** as `OPTIONS`, não só as escolhidas.
  - `NotificationPrefsStore.milestoneHours: Flow<List<Int>>` + `setMilestoneHours()`, chave
    `stringSetPreferencesKey("milestone_hours")`, sanitizada na leitura. Vazio gravado é
    escolha; ausência de chave é default.
  - `FastinApplication`: coletor combina `observeAllByDate()` com `milestoneHours`.
  - `FastingClockViewModel` recebe `milestoneHours: Flow<List<Int>>` com default.
- Depends On: None
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-002-01.md
- Suggested Branch: TASK-Kaleugit-EP-002-01-implement
- Input Context (max 5 files):
  - `app/src/main/java/dev/kaleu/fastin/domain/fasting/FastingCalculator.kt`
  - `app/src/main/java/dev/kaleu/fastin/notify/MilestoneNotifier.kt`
  - `app/src/main/java/dev/kaleu/fastin/data/prefs/NotificationPrefsStore.kt`
  - `app/src/main/java/dev/kaleu/fastin/FastinApplication.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/clock/FastingClockViewModel.kt`
- Done Criteria:
  - `MilestoneNotifierTest`: par positivo/negativo — `[48, 12]` agenda 12 e 48 e **não** 16;
    lista vazia não agenda nada; default de notificação == default do relógio.
  - `SettingsNotificationsTest`: horas gravadas sobrevivem a store reaberto; sem nada gravado
    volta o default; vazio persiste como vazio; valor fora das opções é descartado.
  - `MilestoneHoursTest`: opções dentro de 12..48, default ⊂ opções, `sanitize` ordena.
  - `./gradlew.bat test` com 0 falhas.
- Escalation Conditions:
  - To human: se a preferência exigir migração de Room (não exige: é DataStore).
  - To orchestrator: se a mudança alcançar mais de 6 arquivos de produção.

### Task 02 - Card do relógio com metade da altura
- Task ID: TASK-Kaleugit-EP-002-02
- Status: COMPLETED
- Priority: 2
- Execution Mode: Standard
- Domain: UI (tela inicial)
- Description:
  - `FastingClockCard`: anel de 140dp/8dp à esquerda, marcos em `Column` à direita
    (duas colunas acima de 5 marcos). Estado "nenhum marco" com tag `milestonesEmpty`.
  - `FastinType.clock`: 56sp → 40sp; atualizar `design-system.md` §2.
  - Preservar tags `clockValue`, `clockSeconds`, `milestone_N` e as descrições
    "batido"/"pendente".
- Depends On: TASK-Kaleugit-EP-002-01
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-002-02.md
- Suggested Branch: TASK-Kaleugit-EP-002-02-implement
- Input Context (max 5 files):
  - `app/src/main/java/dev/kaleu/fastin/ui/clock/FastingClockCard.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/theme/Type.kt`
  - `docs/design-system.md`
  - `docs/screenshots/01-calendario.png`
- Done Criteria:
  - `FastingClockTest` existente continua verde (tags e descrições preservadas).
  - Par novo: com `[12, 48]` aparecem `milestone_12`/`milestone_48` e **não** `milestone_16`;
    com lista vazia aparece `milestonesEmpty`.
  - `ScreenshotTest` regenera `01-calendario.png` com o card compacto.
- Escalation Conditions:
  - To human: se o layout exigir tamanho literal fora de `ui/theme/` além dos que o card já
    tinha (220dp/10dp → 140dp/8dp).

### Task 03 - Swipe entre as abas
- Task ID: TASK-Kaleugit-EP-002-03
- Status: COMPLETED
- Priority: 2
- Execution Mode: Standard
- Domain: UI (navegação)
- Description:
  - Criar `ui/HomeTabs.kt`: `enum HomeTab` + composable `HomeTabs(pagerState,
    userScrollEnabled, page)` com `HorizontalPager` + barra inferior + `BackHandler` para
    voltar ao calendário.
  - `FastinNavHost`: destinos `home` (pager) e `entry/{date}`. `DashboardViewModel` criado
    no nível do `home` para travar o swipe com o editor de card aberto.
- Depends On: None
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-002-03.md
- Suggested Branch: TASK-Kaleugit-EP-002-03-implement
- Input Context (max 5 files):
  - `app/src/main/java/dev/kaleu/fastin/ui/FastinNavHost.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/dashboard/DashboardScreen.kt`
- Done Criteria:
  - `HomeTabsTest` com páginas de mentira: swipe esquerdo vai ao dashboard; dois swipes
    chegam em Ajustes e um direito volta; no calendário o swipe direito **não** muda nada;
    toque na barra segue funcionando; com `userScrollEnabled = false` o gesto é ignorado.
- Escalation Conditions:
  - To human: se o pager exigir mexer em `MainActivity` (ADR-002).

### Task 04 - Ajustes: escolha dos marcos e ícone de engrenagem
- Task ID: TASK-Kaleugit-EP-002-04
- Status: COMPLETED
- Priority: 3
- Execution Mode: Standard
- Domain: UI (Ajustes)
- Description:
  - `ToggleChipGrid` em `ChoiceChips.kt` (múltipla escolha, linhas equilibradas de até 5).
  - `SettingsScreen`: card "Marcos de jejum" sempre visível, acima do card de notificações;
    novo callback `onToggleMilestoneHour`.
  - `SettingsViewModel`: `milestoneHours` no estado, `toggleMilestoneHour()` escreve no store.
  - `FastinIcons.Gear` desenhado à mão; `HomeTab.SETTINGS` usa ele; `design-system.md` §7.
- Depends On: TASK-Kaleugit-EP-002-01, TASK-Kaleugit-EP-002-03
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-002-04.md
- Suggested Branch: TASK-Kaleugit-EP-002-04-implement
- Input Context (max 5 files):
  - `app/src/main/java/dev/kaleu/fastin/ui/settings/SettingsScreen.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/settings/SettingsViewModel.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/components/ChoiceChips.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/theme/FastinIcons.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/HomeTabs.kt`
- Done Criteria:
  - `SettingsNotificationsTest`: ligar 12h acrescenta (16h continua ligado); desligar 16h
    remove só ele; ambos chegam ao disco.
  - `MilestoneHoursTest`: 9 opções → linhas 5 + 4; 6 → 3 + 3.
  - `ScreenshotTest` regenera `05-ajustes.png` com a grade de chips.
- Escalation Conditions:
  - To human: se a grade exigir `material-icons-extended` ou cor fora de `ui/theme/`.

### Task 05 - Elevar versão para v1.3 e gerar o APK
- Task ID: TASK-Kaleugit-EP-002-05
- Status: COMPLETED
- Priority: 4
- Execution Mode: Quick
- Domain: Build / release
- Description:
  - `app/build.gradle.kts`: `versionCode = 4` → `5`, `versionName = "1.2"` → `"1.3"`.
  - Rodar `./gradlew.bat test`, `./gradlew.bat lint` e `./gradlew.bat assembleRelease`.
  - Conferir que o APK continua sem permissão de rede (`apkanalyzer manifest permissions`).
  - Copiar o APK para `Desktop\fastin-v1.3.apk`; atualizar `HANDOFF.md`.
- Depends On: TASK-Kaleugit-EP-002-01, TASK-Kaleugit-EP-002-02, TASK-Kaleugit-EP-002-03, TASK-Kaleugit-EP-002-04
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-002-05.md
- Suggested Branch: TASK-Kaleugit-EP-002-05-implement
- Input Context (max 5 files):
  - `app/build.gradle.kts`
  - `docs/build-apk.md`
  - `HANDOFF.md`
- Done Criteria:
  - `app/build/outputs/apk/release/app-release.apk` existe, assinado, com `versionCode = 5`.
  - `apkanalyzer manifest permissions` não lista nenhuma permissão de rede.
  - `./gradlew.bat test` e `./gradlew.bat lint` com 0 falhas / 0 erros.
- Escalation Conditions:
  - To human: se a assinatura falhar — `fastin-release.jks` é insubstituível.

## Planning Notes
- Política padrão: `planning`/`report` criados sob demanda quando a task entra em
  `IN_PROGRESS`. A task `Quick` (05) não os exige.
- 01 define o contrato que 02 e 04 consomem — é a única dependência real. 03 não toca em
  nada que as outras tocam, exceto o `FastinNavHost`, que 04 só ajusta na ligação do
  callback novo.
- O `ScreenshotTest` é o único teste que compõe `SettingsScreen` com estado fixo: ganhar um
  parâmetro novo quebra a compilação dele, e por isso a task 04 o atualiza.

## Decisoes Autonomas
- Registradas em `docs/EPICOS.md` como DA-011 a DA-015 (default 16/18/20/24 para os dois
  usos; opções fechadas; vazio válido; `HorizontalPager`; layout lado a lado do relógio).
