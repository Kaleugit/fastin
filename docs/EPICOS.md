# EPICOS

## Metadata
- Last Updated: 2026-09-03
- Owner: Architect
- Status: ACTIVE

## Rules
- Arquivo canônico: este documento.
- IDs de épico são estáveis e nunca reutilizados.
- Versionamento de arquivo é feito pelo histórico do Git.
- Adaptação para o fastin: `docs/PROJECT.md` cumpre o papel de `PROJECT_SPECS.md` do fluxo
  ODA. O projeto não tem `memory-system/2-tasks.md`, então o gate de bootstrap não se aplica
  — o app já está em produção (v1.0.2 instalada no aparelho do usuário).

## Epic List

### EP-001 - Correção de bugs do primeiro ciclo de uso real (v1.2)
- Status: DONE
- Domain: App Android fastin (UI + notificações locais)
- Objective: corrigir os cinco defeitos relatados pelo usuário após uso continuado da
  v1.0.2 e entregar a v1.2 instalável por cima da versão atual.
- Scope In:
  - Persistir a preferência de notificações entre execuções do processo.
  - Inverter a ordem dos campos de refeição no formulário do dia (primeira antes de última).
  - Impedir que o teclado do sistema cubra o campo de observações.
  - Desenhar rótulos de eixo (valor e data) nos gráficos de linha e dispersão, e legenda de
    escala no heatmap.
  - Avisar sobre alterações não salvas ao sair do formulário do dia.
  - Elevar `versionCode` para 4 e `versionName` para "1.2".
- Scope Out:
  - Qualquer feature nova de `docs/PROJECT.md` §5 (período customizado, reordenar cards,
    meta de streak configurável).
  - Verificação em aparelho dos buracos do `HANDOFF.md` §2 (import de CSV, sombras API 26-27).
  - Mudança no modelo de dados: a tabela `fasting_log` não muda, não há migração de Room.
  - Alteração da regra de cálculo do jejum (`FastingCalculator`).
- Dependencies: None
- Completion Signal:
  - `./gradlew.bat test` passa com 0 falhas e com pelo menos um teste de regressão novo por bug.
  - `./gradlew.bat lint` reporta 0 erros.
  - `./gradlew.bat assembleRelease` gera APK assinado com `versionCode = 4`.
  - `apkanalyzer manifest permissions` continua sem permissão de rede.
- Escalation Triggers:
  - Se a correção exigir migração de schema Room, parar e escalar ao humano.
  - Se a correção do teclado exigir sair de `enableEdgeToEdge` no `MainActivity`, escalar:
    é uma decisão de tema registrada em ADR-002.
  - Se qualquer correção precisar de permissão nova no manifest, parar e escalar — o critério
    "funciona em modo avião" (`docs/PROJECT.md` §6) é inegociável.
- Change Log:
  - 2026-08-27 - Épico criado a partir da lista de bugs do usuário após uso real da v1.0.2.
  - 2026-08-27 - Todas as 6 tasks concluídas. 114 testes / 0 falhas, lint 0 erros, APK v1.2
    (versionCode 4) assinado com a mesma chave da versão instalada. **A verificação em
    aparelho segue pendente** e não fazia parte do Completion Signal: a correção do teclado
    não é demonstrável na JVM e o ciclo real das notificações nunca foi observado.

### EP-002 - Relógio compacto, marcos configuráveis e swipe entre abas (v1.3)
- Status: DONE
- Domain: App Android fastin (UI + preferências + notificações locais)
- Objective: atender os quatro pedidos do usuário após uma semana com a v1.2 — calendário
  visível inteiro, marcos do relógio ligados às notificações, escolha de quais marcos
  (12h a 48h) e swipe entre abas — e entregar a v1.3 instalável por cima da v1.2.
- Scope In:
  - Uma lista única de marcos (`MilestoneHours`) alimentando o relógio **e** as notificações.
  - Escolha dos marcos em Ajustes, persistida em DataStore, com opções fechadas entre 12h e
    48h. Vazio é escolha válida.
  - Card do relógio com metade da altura: anel de 140dp à esquerda, marcos em lista à direita.
  - `HorizontalPager` para as três abas; toque na barra continua funcionando; voltar de uma
    aba secundária leva ao calendário.
  - Ícone de engrenagem na aba Ajustes, desenhado à mão como manda o design-system §7.
  - Elevar `versionCode` para 5 e `versionName` para "1.3".
- Scope Out:
  - Anel de progresso continua mirando 24h — não acompanha o maior marco escolhido.
  - ~~Mudança na regra de cálculo~~ — **reaberto pelo humano**: o marco de 48h coincidia com
    `FastingCalculator.MAX_PLAUSIBLE` (48h) e nunca acenderia nem notificaria. O usuário
    decidiu subir o limite de abandono para 100h (DA-016).
  - Horas fora da lista fechada (13h, 25h…): não há campo livre.
  - Verificação em aparelho do disparo real das notificações (buraco herdado do EP-001).
  - Mudança no modelo de dados: `fasting_log` não muda, não há migração de Room.
- Dependencies: None
- Completion Signal:
  - `./gradlew.bat test` passa com 0 falhas e com pelo menos um par positivo/negativo novo
    por item.
  - `./gradlew.bat lint` reporta 0 erros.
  - `./gradlew.bat assembleRelease` gera APK assinado com `versionCode = 5`.
  - `apkanalyzer manifest permissions` continua sem permissão de rede.
- Escalation Triggers:
  - Se a seleção de marcos exigir migração de schema Room, parar e escalar ao humano.
  - Se o pager exigir sair de `enableEdgeToEdge` ou mexer no `MainActivity`, escalar (ADR-002).
  - Se qualquer item precisar de permissão nova no manifest, parar e escalar — o critério
    "funciona em modo avião" (`docs/PROJECT.md` §6) é inegociável.
- Change Log:
  - 2026-09-03 - Épico criado a partir dos quatro pedidos do usuário ("Correção de bugs para
    fastin v1.0.3"). A versão pedida estava errada: a instalada é a v1.2 / versionCode 4, então
    a entrega é **v1.3 / versionCode 5**.
  - 2026-09-03 - Cinco tasks concluídas. Detalhes de teste, lint e APK no
    `docs/EPICO-EP-002-relogio-marcos-swipe-v1-3-TASKS.md` e no `HANDOFF.md`.

## Decisoes Autonomas
- DA-001: `docs/PROJECT.md` é usado como `PROJECT_SPECS.md` — Criterio: padrão de adaptação —
  Racional: o fastin nunca teve bootstrap ODA; `PROJECT.md` já declara escopo, modelo de
  dados e critérios de aceitação, que é exatamente o que o fluxo consome.
- DA-002: gate de bootstrap (`memory-system/2-tasks.md`) não é criado nem consultado —
  Criterio: padrão — Racional: o gate existe para autorizar o início de execução de um
  projeto novo; este app já está em produção há três dias de uso real.
- DA-003: os cinco bugs viram um único épico em vez de cinco — Criterio: um domínio por
  épico — Racional: todos pertencem ao mesmo bounded context (o app) e compartilham um único
  sinal de conclusão observável, que é a v1.2 instalada.
- DA-010: a correção do teclado foi feita no root (`MainActivity`) e não no formulário —
  Criterio: escalação prevista na task 03 — Racional: o `Box` do root aplica
  `windowInsetsPadding(systemBars)` sobre todas as telas; um `imePadding()` local somaria com
  ele e contaria a nav bar duas vezes. Corrigir no root vale para todas as telas.
- DA-004: o bump de versão vira task própria e não um passo solto — Criterio: ownership de
  wiring — Racional: sem `versionCode` novo o Android recusa a atualização por cima, e a
  entrega inteira fica inutilizável no aparelho. É deliverable com dono, não efeito colateral.
- DA-011 (EP-002): o default dos marcos é 16/18/20/24 para relógio **e** notificação —
  Criterio: pedido do usuário de uma lista só — Racional: a v1.2 mostrava 24h no relógio e
  notificava só 16/18/20; unificar sem mudar o que o usuário vê na tela significa 24h passar a
  notificar também. Ele pode desmarcar em Ajustes.
- DA-012 (EP-002): opções fechadas (12, 14, 16, 18, 20, 22, 24, 36, 48), não campo livre —
  Criterio: KISS — Racional: o pedido diz "mínimo 12h, máximo 48h, o usuário escolhe quais";
  uma grade de chips responde isso sem teclado, validação nem parsing. Acima de 24h só 36h:
  jejum nessa faixa é raro e um chip a cada 2h viraria parede.
- DA-013 (EP-002): seleção vazia é válida — Criterio: "todo campo é opcional" (CLAUDE.md) —
  Racional: obrigar ao menos um marco seria required disfarçado. O relógio mostra um aviso
  curto no lugar da lista e nada é agendado.
- DA-014 (EP-002): as abas viraram `HorizontalPager` num único destino de navegação, não
  detector de gesto sobre o `NavHost` — Criterio: o pedido é "side slide", com o dedo
  arrastando a tela — Racional: o pager entrega o arrasto de graça; um detector só navegaria
  ao soltar, sem feedback. Custo: os três ViewModels passam a viver no destino `home`, o que
  na prática é o que já acontecia com `saveState`/`restoreState`.
- DA-015 (EP-002): o card do relógio mudou de layout (anel à esquerda, marcos à direita) em
  vez de só encolher — Criterio: métrica do pedido ("50% mais curto", calendário inteiro) —
  Racional: medido no screenshot, o card tinha 386dp e o calendário 442dp; para caber sem
  rolar numa tela de 891dp o card precisa ficar em ~240dp, o que empilhar anel + linha de
  marcos não alcança. Lado a lado dá ~214dp com o default.
- DA-016 (EP-002): `FastingCalculator.MAX_PLAUSIBLE` sobe de 48h para 100h — Criterio:
  **decisão do humano** ("subir o limite de abandono pra 100h"), não autônoma — Racional: o
  maior marco escolhível é 48h e, com o limite igual a ele, o jejum era descartado como
  abandonado no instante em que o marco chegava. Trade-off registrado: pares de horários com
  49h a 100h de distância, antes tratados como esquecimento de registro, passam a contar como
  jejum fechado nos gráficos e no streak.
