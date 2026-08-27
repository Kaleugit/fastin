# EPICOS

## Metadata
- Last Updated: 2026-08-27
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
