# TASK-Kaleugit-EP-001-03 - Impedir que o teclado cubra o campo de observacoes

- Status: PENDING
- Priority: 2
- Description: Com `enableEdgeToEdge` no `MainActivity` e `windowInsetsPadding(systemBars)`, o inset do IME nunca e consumido: o `adjustResize` do manifest sozinho nao sobe o conteudo. O usuario digita nas observacoes sem ver o que escreve. Consumir o inset do IME na tela do formulario.
- Depends On: TASK-Kaleugit-EP-001-02
- Blocked By: None
- Branch: TASK-Kaleugit-EP-001-03-implement
- Workstreams: None
- Execution Mode: Standard
- Last Updated: 2026-08-27 00:00

## Contexto de entrada
- app/src/main/java/dev/kaleu/fastin/ui/entry/DayEntryScreen.kt
- app/src/main/java/dev/kaleu/fastin/ui/components/Fields.kt
- app/src/main/java/dev/kaleu/fastin/MainActivity.kt
- app/src/main/AndroidManifest.xml
- HANDOFF.md

## Expected Output
- `.imePadding()` aplicado no `Column` rolavel de `DayEntryScreen`, depois do `verticalScroll`.
- `windowSoftInputMode="adjustResize"` mantido no manifest; `enableEdgeToEdge` mantido (ADR-002).
- `DayEntryScreenTest` com `performScrollTo()` antes do toque em `notes` e digitacao verificada.
- `./gradlew.bat test` com 0 falhas e `./gradlew.bat lint` com 0 erros.
- Verificacao em aparelho registrada no HANDOFF.md: Robolectric nao instancia IME real.

## Escalation
- Ao humano: se a correcao exigir abandonar edge-to-edge (ADR-002).
- Ao orquestrador: se exigir mudar o layout raiz compartilhado por todas as telas.
