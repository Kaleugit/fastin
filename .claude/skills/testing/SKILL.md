---
name: testing
description: Use this skill when you need to define, prioritize, or execute a pragmatic test strategy for a task, map requirements to tests, enforce acceptance criteria, and verify quality gates before completion.
user-invocable: false
metadata:
  kind: persona
---

# Testing Skill

This skill provides pragmatic test-first guidance for this boilerplate.
It is not strict TDD for every case; it is risk-based and focused on confidence.

## When To Use
- Before implementation to define what will be tested.
- When fixing bugs (reproduce first, then fix).
- Before marking a task as completed.
- When acceptance criteria are ambiguous or not measurable.

## Additional Inputs To Read
Assume baseline context from `CLAUDE.md` is already loaded.

1. `docs/PROJECT.md` (mandatory, source of truth — critérios de aceitação)
2. `docs/architecture.md`

## Workflow
1. Extract requirements and acceptance criteria.
2. Map each requirement to one or more tests.
3. Choose test levels by risk (unit/integration/e2e/manual). Para comportamento de UI, ver § Verificação de UI Android.
4. Define minimal test set required before coding starts.
6. Report pass/fail status by acceptance criterion.

## Escalation Rule (Mandatory)
If there is no clear, testable **definition** of expected behavior:
1. Stop implementation.
2. Ask the human for clarification.
3. Resume only after criteria are explicit and testable.

This rule is about *defining* expected behavior, not *executing* tests. Once criteria are
explicit, verification (including e2e) is the agent's responsibility and must NOT be
delegated to the human (verificação de comportamento não-UX é responsabilidade do agente).

## Verificação de UI Android (Mandatory)

Não existe emulador nem dispositivo garantido nesta máquina. A verificação funcional de UI
é responsabilidade do agente e roda **na JVM**, sem device:

- **Ferramenta padrão: Compose UI tests sob Robolectric** (`app/src/test/`, com
  `@RunWith(RobolectricTestRunner::class)` + `createComposeRule()`). Exercita navegação,
  formulários, estado e caminhos de erro reais.
- **Room:** teste com banco in-memory (`Room.inMemoryDatabaseBuilder`) na JVM.
- **Lógica de tempo** (cálculo de jejum, marcos, streak): função pura + teste unitário
  com `Clock` injetado. Nunca depender de `System.currentTimeMillis()` direto no cálculo.
- `app/src/androidTest/` só é usado se houver device/emulador conectado (`adb devices`).
  Se não houver, isso **não** vira `FAIL` — a cobertura equivalente sai por Robolectric.

O humano valida **apenas UX** no aparelho: qualidade visual, feel, polimento da interação.
Correção funcional/wiring é verificada pelo agente.

### Teste de produtor não prova wiring
Um teste unitário verde do produtor (o cálculo de jejum, o agregador do gráfico) chamado
diretamente **não** prova que a feature funciona. Dois buracos sobrevivem ao verde:
- **wiring-WRONG**: o produtor É chamado em produção, mas com argumento errado (data
  errada, timezone errado, período errado) — o resultado sai silenciosamente degradado.
- **saída emergente**: o bug só aparece no que a tela realmente renderiza.

Por isso, todo critério de aceitação de tela exige:
1. Exercitar pelo **caminho de produção** (ViewModel + composable reais), não só a função.
2. Asserção sobre o que a UI **de fato emite** (texto do relógio, marcos acesos, pontos
   do calendário, pontos da série do gráfico).
3. **Controle no-op que prova que a alavanca está viva**: com o dado ausente/zerado, a
   mesma asserção precisa FALHAR. Teste que passaria mesmo com o produtor desconectado é
   verde vazio e não conta.

Evidência (flows cobertos, `PASS/FAIL` por flow) faz parte da aceitação de qualquer task
de tela.

## Mandatory Rules

## Required Outputs
- Requirement-to-test mapping in planning/report artifacts.
- Explicit test level coverage per critical requirement.
- Acceptance criteria status with evidence (`PASS`/`FAIL`).

## Test-Suite Runtime Threshold
- Default threshold: **180 seconds**. Projects override via a single integer on the first line of `.governance/test-runtime-threshold`.
- When `tests_runtime` (see Workflow step 5) is at or above the threshold, append a `## Performance Warning` section to the task report (Standard/Critical) or include a one-line note in `Evidence` (Quick) with:
  - the measured `tests_runtime`,
  - the effective threshold,
  - at least one concrete suggestion (e.g. run in parallel, split the suite by module, move slow tests to a nightly job).
- The warning is informational and does not block delivery.

## Quality Bar
- No “test after guess” on critical behavior.
- Bugfixes must include a failing test scenario first.
- Acceptance criteria must be measurable, observable, and binary.
- No test manipulation to force green results.
- Integrity rules from `INTEGRITY-RULES.md` are non-negotiable.
- Source-layout convention is respected for changed application code (`src/` or `*/src/` in monorepo).
- Integration/e2e tests exercise the **production layout and code path**, not a convenience shortcut. A test that bypasses the real wiring (e.g. pointing the workspace directly at a source dir instead of cloning the bare repo the way production does) can pass while production is broken — the gate goes vacuously green. When the production path and the test path diverge, the test MUST follow the production path.

## Reference Files
- Integrity anti-patterns and corrective approach: `references/integrity-anti-patterns.md`
- Testing levels and selection rules: `references/testing-levels.md`
- Acceptance criteria rules: `references/acceptance-criteria-rules.md`
- Test-first checklist: `references/test-first-checklist.md`
