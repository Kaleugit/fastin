---
name: frontend
description: Use this skill when implementing or refactoring frontend code (web/mobile) including component architecture, state management, routing, forms, accessibility, responsiveness, UI performance, API integration in the client, and PWA/hybrid concerns. Use for both feature delivery and frontend technical debt reduction.
user-invocable: false
metadata:
  kind: persona
---

# Frontend Skill

This skill standardizes frontend delivery for this boilerplate with a pragmatic quality bar.

## When To Use
- When implementing or refactoring frontend/UI code, components, accessibility, or client integrations.

## Additional Inputs To Read
Assume baseline context from `CLAUDE.md` is already loaded.

1. `docs/PROJECT.md` (mandatory, source of truth)
2. `docs/design-system.md` (mandatory — tokens, superfícies neumórficas, acento)
3. `docs/architecture.md`
4. `design-ref/img-ref01.png` e `design-ref/img-ref02.png`

## Workflow
1. Clarify user flow, acceptance criteria, and UI constraints.
2. Define component boundaries and state ownership before coding.
3. Implement minimal UI and interaction path first.
4. Consuma o data layer (Room/Flow) por contratos explícitos de ViewModel.
5. Validate accessibility, responsiveness, and performance basics.
6. Run tests/checks according to a estratégia em `.claude/skills/testing/SKILL.md`, including **mandatory e2e** that exercises functional wiring (`.claude/skills/testing/SKILL.md` § Verificação de UI Android).
7. Registre a evidência de aceitação na etapa entregue ao humano.

## Mandatory Rules
- Código de produção fica em `app/src/main/java/dev/kaleu/fastin/`. Testes unitários em `app/src/test/`, instrumentados em `app/src/androidTest/`.
- Prefer simple, maintainable patterns over UI abstraction overload.
- Preserve design-system conventions when they exist.
- If expected **UX** behavior is ambiguous, ask the human before finalizing. UX is the human's only testing responsibility.
- **Correção funcional/wiring é verificada pelo agente** via Compose UI tests sob Robolectric, nunca delegada ao humano (`.claude/skills/testing/SKILL.md` § Verificação de UI Android). O humano valida só UX no aparelho.

## Required Outputs
- Frontend changes implemented under `app/src/main/java/dev/kaleu/fastin/`.
- Acceptance criteria status with evidence (`PASS`/`FAIL`).

## Quality Bar
- Correct behavior first, then polish.
- Accessible by default (keyboard flow, labels, contrast baseline).
- Legível em telas de 5" a 6.8", com fonte do sistema ampliada até 130%.
- No fake UI states to hide backend/frontend defects.

## Reference Files
- Frontend delivery checklist: `references/frontend-delivery-checklist.md`
- UI architecture and state rules: `references/ui-architecture-rules.md`
- Client API integration rules: `references/frontend-api-integration.md`
