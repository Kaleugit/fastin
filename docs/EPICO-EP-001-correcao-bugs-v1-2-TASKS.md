# EPICO-EP-001-correcao-bugs-v1-2-TASKS

## Metadata
- Epic ID: EP-001
- Epic Title: Correção de bugs do primeiro ciclo de uso real (v1.2)
- Last Updated: 2026-08-27
- Owner: Architect
- Status: PENDING APPROVAL

## Epic Context
- Objective: corrigir os cinco defeitos relatados após uso continuado da v1.0.2 e entregar a
  v1.2 instalável por cima da versão atual.
- Scope Boundaries: só correção. Nenhuma feature de `docs/PROJECT.md` §5 entra, o schema do
  Room não muda e a regra de cálculo do jejum não é tocada.
- Key Dependencies: nenhuma externa. As tasks 02, 03 e 05 tocam o mesmo arquivo
  (`DayEntryScreen.kt`) e por isso são encadeadas por dependência, não paralelizadas.

## Diagnóstico prévio

Cada task abaixo nasce de uma causa já localizada no código, não de um sintoma:

| Bug relatado | Causa encontrada | Arquivo |
|---|---|---|
| Notificação desativa ao fechar o app | `notificationsEnabled` é `@Volatile var` em memória, sem persistência; `SettingsUiState` nasce `false` | `FastinApplication.kt:36`, `SettingsViewModel.kt:27` |
| Última refeição aparece antes da primeira | Ordem literal dos dois `TimeField` no card "Jejum" | `DayEntryScreen.kt:118-133` |
| Teclado cobre as observações | `enableEdgeToEdge` + `windowInsetsPadding(systemBars)` sem nenhum `imePadding()`; com edge-to-edge o `adjustResize` do manifest não sobe o conteúdo sozinho | `MainActivity.kt:29`, `DayEntryScreen.kt:78` |
| Gráficos sem índice | `Charts.kt` desenha só o traço: nenhum texto é emitido em nenhum dos renderizadores | `Charts.kt` (todo) |
| Horário se perde sem salvar | `onBack` chama `popBackStack()` direto; nada compara o estado atual com o carregado | `FastinNavHost.kt:230`, `DayEntryViewModel.kt` |

## Approved Task List

### Task 01 - Persistir a preferência de notificações entre sessões
- Task ID: TASK-Kaleugit-EP-001-01
- Status: PENDING
- Priority: 1
- Execution Mode: Standard
- Domain: Notificações locais (preferência + reagendamento)
- Description:
  - Criar `data/prefs/NotificationPrefsStore.kt` espelhando o padrão de
    `DashboardConfigStore`: `DataStore<Preferences>` **injetado** (nunca obtido do `Context`
    dentro da classe), `booleanPreferencesKey("notifications_enabled")`, expondo
    `val enabled: Flow<Boolean>` e `suspend fun setEnabled(value: Boolean)`.
  - Adicionar `fun notificationPrefsDataStoreOf(context: Context): DataStore<Preferences>`
    com `by preferencesDataStore(name = "notifications")` — arquivo separado do `dashboard`.
  - Registrar o store em `AppContainer`.
  - Em `FastinApplication.onCreate`, ler o valor persistido e, se `true`, iniciar o
    `rescheduleJob` que hoje só nasce a partir do toque em Ajustes.
  - `FastinApplication.setNotificationsEnabled` passa a gravar no store além de alternar o
    estado em memória.
  - `SettingsViewModel` passa a **coletar** `enabled` do store para popular
    `SettingsUiState.notificationsEnabled`, em vez de assumir `false`.
  - Não adicionar permissão nova ao manifest. `POST_NOTIFICATIONS` já está declarada.
- Depends On: None
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-001-01.md
- Suggested Branch: TASK-Kaleugit-EP-001-01-implement
- Input Context (max 5 files):
  - `app/src/main/java/dev/kaleu/fastin/FastinApplication.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/settings/SettingsViewModel.kt`
  - `app/src/main/java/dev/kaleu/fastin/data/prefs/DashboardConfigStore.kt`
  - `app/src/main/java/dev/kaleu/fastin/AppContainer.kt`
  - `app/src/main/java/dev/kaleu/fastin/notify/MilestoneNotifier.kt`
- Done Criteria:
  - `app/src/main/java/dev/kaleu/fastin/data/prefs/NotificationPrefsStore.kt` existe e expõe
    `enabled: Flow<Boolean>` e `setEnabled(Boolean)`.
  - Novo `NotificationPrefsStoreTest` com **par positivo/negativo**: gravar `true` e reler
    devolve `true` num store novo (simula processo reiniciado); o default sem nada gravado
    devolve `false`.
  - `SettingsScreenTest` (novo ou estendido) prova que a tela reflete `true` vindo do store
    em vez do default.
  - Cada teste recebe **arquivo de DataStore próprio**, pela mesma razão registrada em
    `DashboardConfigStore` (o delegate guarda uma instância por processo).
  - `./gradlew.bat test` passa com 0 falhas.
- Escalation Conditions:
  - To human: se restaurar o agendamento no boot exigir `BOOT_COMPLETED` próprio além do que
    o WorkManager já faz.
  - To orchestrator: se a mudança alcançar mais de 6 arquivos.

### Task 02 - Inverter a ordem dos campos de refeição no formulário
- Task ID: TASK-Kaleugit-EP-001-02
- Status: PENDING
- Priority: 2
- Execution Mode: Quick
- Domain: Formulário do dia (UI)
- Description:
  - Em `DayEntryScreen.kt`, card `FastinCard(eyebrow = "Jejum")`: mover o `TimeField` de
    `"Primeira refeição do dia"` para **antes** do de `"Última refeição do dia"`.
  - Trocar apenas a ordem de composição. Não renomear `testTag`, não mexer nos callbacks,
    não tocar em `DayEntryUiState` nem no schema.
  - Regenerar `docs/screenshots/` com `--tests "*ScreenshotTest*"`.
- Depends On: None
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-001-02.md
- Suggested Branch: TASK-Kaleugit-EP-001-02-implement
- Input Context (max 5 files):
  - `app/src/main/java/dev/kaleu/fastin/ui/entry/DayEntryScreen.kt`
  - `app/src/test/java/dev/kaleu/fastin/ui/DayEntryScreenTest.kt`
  - `docs/PROJECT.md`
- Done Criteria:
  - Teste em `DayEntryScreenTest` afirma que o nó `firstMealTime` tem coordenada Y **menor**
    que a de `lastMealTime` (`onNodeWithTag(...).fetchSemanticsNode().positionInRoot.y`).
  - `./gradlew.bat test` passa com 0 falhas.
  - Screenshots regeneradas em `docs/screenshots/`.
- Escalation Conditions:
  - To human: se a inversão visual sugerir também inverter a semântica do cálculo do jejum —
    **não inverter**: a regra de `docs/PROJECT.md` §2 é `lastMealTime(D-1) → firstMealTime(D)`
    e não muda com a ordem na tela.
  - To orchestrator: nenhuma prevista.

### Task 03 - Impedir que o teclado cubra o campo de observações
- Task ID: TASK-Kaleugit-EP-001-03
- Status: PENDING
- Priority: 2
- Execution Mode: Standard
- Domain: Formulário do dia (UI / window insets)
- Description:
  - Aplicar `.imePadding()` no `Column` rolável de `DayEntryScreen` (o que já tem
    `verticalScroll`), **depois** do `verticalScroll` e antes do `padding` de conteúdo.
  - Manter `windowSoftInputMode="adjustResize"` no manifest — ele é pré-requisito para o
    inset do IME chegar; o que faltava era consumi-lo.
  - Não remover `enableEdgeToEdge` do `MainActivity` (ADR-002). Se o padding do `Box` do
    `MainActivity` impedir o IME de chegar à tela, ajustar ali, documentando no código.
  - Garantir que o `BasicTextField` de `NotesField` role para dentro da janela ao receber
    foco (`bringIntoViewRequester` só se o `imePadding` sozinho não bastar — preferir a
    solução mínima).
- Depends On: TASK-Kaleugit-EP-001-02
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-001-03.md
- Suggested Branch: TASK-Kaleugit-EP-001-03-implement
- Input Context (max 5 files):
  - `app/src/main/java/dev/kaleu/fastin/ui/entry/DayEntryScreen.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/components/Fields.kt`
  - `app/src/main/java/dev/kaleu/fastin/MainActivity.kt`
  - `app/src/main/AndroidManifest.xml`
  - `HANDOFF.md`
- Done Criteria:
  - `./gradlew.bat test` passa com 0 falhas, incluindo `DayEntryScreenTest` com
    `performScrollTo()` antes do toque no campo `notes` e digitação verificada.
  - `./gradlew.bat lint` reporta 0 erros.
  - Verificação visual em aparelho registrada no `HANDOFF.md` — **este bug não é
    demonstrável na JVM**: Robolectric não instancia IME real. O teste cobre a regressão de
    composição; a prova final é o aparelho.
- Escalation Conditions:
  - To human: se a correção exigir abandonar edge-to-edge (decisão de tema, ADR-002).
  - To orchestrator: se exigir mudar o layout raiz compartilhado por todas as telas.

### Task 04 - Rótulos de eixo nos gráficos
- Task ID: TASK-Kaleugit-EP-001-04
- Status: PENDING
- Priority: 3
- Execution Mode: Standard
- Domain: Dashboard (renderização de gráficos)
- Description:
  - Em `Charts.kt`, `LineChart` e `ScatterChart` passam a exibir:
    - eixo Y: valor **mínimo** e **máximo** da série, formatados com a unidade de
      `data.config.metric.unit` (`isRate = true` exibe `%`).
    - eixo X: data do primeiro e do último ponto, formato `d MMM` em pt-BR.
  - `HeatmapChart` ganha legenda de escala ("menos → mais") com os degraus de intensidade.
  - Os rótulos são `Text` do Compose sobre/ao redor do `Canvas`, **não** `drawText` nativo —
    coerente com ADR-001 e com a proibição de canvas nativo do `HANDOFF.md` §4.
  - O plot precisa encolher para caber os rótulos: reservar a faixa e manter
    `CHART_HEIGHT_DP` como altura da **área de desenho**, não do card inteiro.
  - Nenhuma cor, tamanho ou raio literal: tudo de `ui/theme/` (`docs/design-system.md` §8).
  - Série de um ponto só continua com a régua horizontal e o aviso já existentes; o rótulo
    de eixo Y nesse caso exibe o valor único.
- Depends On: None
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-001-04.md
- Suggested Branch: TASK-Kaleugit-EP-001-04-implement
- Input Context (max 5 files):
  - `app/src/main/java/dev/kaleu/fastin/ui/dashboard/charts/Charts.kt`
  - `app/src/main/java/dev/kaleu/fastin/domain/metrics/Metrics.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/dashboard/DashboardScreen.kt`
  - `docs/design-system.md`
  - `app/src/test/java/dev/kaleu/fastin/ui/DashboardScreenTest.kt`
- Done Criteria:
  - `DashboardScreenTest` com **par positivo/negativo**: série com 2+ pontos exibe os quatro
    rótulos (min, max, data inicial, data final); série vazia (`ChartEmptyState`) não exibe
    nenhum deles.
  - Teste cobrindo série constante (min == max), que hoje já cai no caso especial de `yScale`.
  - `./gradlew.bat test` passa com 0 falhas.
  - Screenshots regeneradas mostrando os rótulos.
- Escalation Conditions:
  - To human: se os rótulos não couberem em 140dp sem prejudicar a leitura do traço — a
    alternativa (aumentar o card) muda o layout do dashboard e é decisão de produto.
  - To orchestrator: se exigir mudança em `MetricEngine` ou em `ChartData`.

### Task 05 - Avisar sobre alterações não salvas ao sair do formulário
- Task ID: TASK-Kaleugit-EP-001-05
- Status: PENDING
- Priority: 1
- Execution Mode: Critical
- Domain: Formulário do dia (estado + navegação)
- Description:
  - `DayEntryViewModel` guarda o **snapshot carregado** do banco ao fim do `init` e expõe
    `val hasUnsavedChanges: Boolean` comparando o estado atual com esse snapshot.
  - `hasUnsavedChanges` é `false` enquanto `isLoading` é `true` — nunca acusar alteração
    sobre um formulário que ainda não carregou.
  - Após `save()` bem-sucedido, o snapshot é atualizado: salvar zera o estado "sujo".
  - `DayEntryScreen` recebe `onBack` e, quando há alteração pendente, abre um diálogo com
    três saídas: **Salvar e sair** · **Sair sem salvar** · **Cancelar**.
  - O diálogo cobre também o botão voltar do sistema (`BackHandler`), não só o botão da tela.
  - Nenhum autosave: o `@Upsert` grava a linha inteira e formulário vazio apaga o dia. O save
    continua sendo um ato explícito — foi essa fronteira que corrigiu a perda de dados da
    v1.0.2 (`HANDOFF.md` §6b).
- Depends On: TASK-Kaleugit-EP-001-02
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-001-05.md
- Suggested Branch: TASK-Kaleugit-EP-001-05-implement
- Input Context (max 5 files):
  - `app/src/main/java/dev/kaleu/fastin/ui/entry/DayEntryViewModel.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/entry/DayEntryScreen.kt`
  - `app/src/main/java/dev/kaleu/fastin/ui/FastinNavHost.kt`
  - `app/src/test/java/dev/kaleu/fastin/ui/DayEntryLoadRaceTest.kt`
  - `HANDOFF.md`
- Done Criteria:
  - Teste com **par positivo/negativo**: alterar um horário e tocar em voltar exibe o
    diálogo; voltar **sem** ter alterado nada não exibe diálogo nenhum.
  - Teste provando que "Salvar e sair" grava e navega, e que "Sair sem salvar" **não** grava
    (asserção de que o repositório continua com o valor anterior).
  - Teste provando que o diálogo não aparece enquanto `isLoading` é `true`.
  - `DayEntryLoadRaceTest` continua passando — a garantia de não sobrescrever durante a carga
    não pode regredir.
  - `./gradlew.bat test` passa com 0 falhas.
- Escalation Conditions:
  - To human: se surgir pedido de autosave durante a implementação — a decisão de produto
    está tomada e registrada em DA-006.
  - To orchestrator: se o `BackHandler` conflitar com a navegação por abas do `FastinNavHost`.

### Task 06 - Elevar versão para v1.2 e gerar o APK
- Task ID: TASK-Kaleugit-EP-001-06
- Status: PENDING
- Priority: 4
- Execution Mode: Quick
- Domain: Build / release
- Description:
  - `app/build.gradle.kts`: `versionCode = 3` → `4`, `versionName = "1.0.2"` → `"1.2"`.
  - Rodar `./gradlew.bat test`, `./gradlew.bat lint` e `./gradlew.bat assembleRelease`.
  - Conferir que o APK continua sem permissão de rede (`apkanalyzer manifest permissions`).
  - Atualizar `HANDOFF.md` com o estado da v1.2 e o que ficou por verificar em aparelho.
- Depends On: TASK-Kaleugit-EP-001-01, TASK-Kaleugit-EP-001-02, TASK-Kaleugit-EP-001-03, TASK-Kaleugit-EP-001-04, TASK-Kaleugit-EP-001-05
- Canonical File: memory-system/tasks/TASK-Kaleugit-EP-001-06.md
- Suggested Branch: TASK-Kaleugit-EP-001-06-implement
- Input Context (max 5 files):
  - `app/build.gradle.kts`
  - `docs/build-apk.md`
  - `HANDOFF.md`
- Done Criteria:
  - `app/build/outputs/apk/release/app-release.apk` existe, assinado, com `versionCode = 4`.
  - `apkanalyzer manifest permissions` não lista nenhuma permissão de rede.
  - `./gradlew.bat test` e `./gradlew.bat lint` com 0 falhas / 0 erros.
- Escalation Conditions:
  - To human: se a assinatura falhar — `fastin-release.jks` é insubstituível e sem ela a
    atualização por cima não instala.
  - To orchestrator: nenhuma prevista.

## Planning Notes
- Política padrão: `planning`/`report` criados sob demanda quando a task entra em
  `IN_PROGRESS`. Tasks `Quick` (02, 06) não os exigem.
- Cada task é auto-contida: o arquivo canônico + o *Input Context* listado bastam para
  executá-la sem conhecimento implícito das demais.
- **Contenção de arquivo, não de contrato:** 02, 03 e 05 escrevem em `DayEntryScreen.kt`.
  Não há contrato cruzado entre elas, mas a ordem 02 → 03 → 05 evita rebase manual.

## Decisoes Autonomas
- DA-005: bug 4 entregue como rótulos de eixo (min/max + datas), não como tooltip
  interativo — Criterio: escolha do humano na abertura do épico — Racional: responde "quanto"
  e "quando" sem introduzir gesto novo sobre Canvas puro.
- DA-006: bug 5 entregue como diálogo de saída, não como autosave — Criterio: escolha do
  humano na abertura do épico — Racional: `save()` é upsert da linha inteira e formulário
  vazio apaga o dia; autosave transformaria cada limpeza de campo em escrita imediata,
  reabrindo a classe de bug que a v1.0.2 corrigiu.
- DA-007: task 05 é `Critical`, não `Standard` — Criterio: `mode-selection-rules.md`, alto
  risco de regressão — Racional: mexe no mesmo ponto que causou a única perda de dados do
  projeto. `DayEntryLoadRaceTest` é regressão obrigatória.
- DA-008: task 01 é `Standard` mesmo criando arquivo novo — Criterio: sem mudança de schema
  nem de contrato — Racional: `NotificationPrefsStore` é aditivo, espelha um padrão que já
  existe e não migra dado nenhum.
- DA-009: nenhuma task altera o manifest para adicionar permissão — Criterio: `PROJECT.md` §6
  (modo avião) — Racional: é critério de aceitação transversal, acima de qualquer correção.
