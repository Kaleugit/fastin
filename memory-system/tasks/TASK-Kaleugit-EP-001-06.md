# TASK-Kaleugit-EP-001-06 - Elevar versao para v1.2 e gerar o APK

- Status: COMPLETED
- Priority: 4
- Description: Sem `versionCode` novo o Android recusa a atualizacao por cima e a entrega inteira fica inutilizavel no aparelho. Elevar versao, rodar a bateria completa e conferir que o APK segue sem permissao de rede.
- Depends On: TASK-Kaleugit-EP-001-01, TASK-Kaleugit-EP-001-02, TASK-Kaleugit-EP-001-03, TASK-Kaleugit-EP-001-04, TASK-Kaleugit-EP-001-05
- Blocked By: None
- Branch: TASK-Kaleugit-EP-001-06-implement
- Workstreams: None
- Execution Mode: Quick
- Last Updated: 2026-08-27 14:30
- Completed: 2026-08-27 14:30
- Evidence: PASS — apk summary: dev.kaleu.fastin 4 1.2; apksigner: v2+v3, SHA-256 66d96c44… idêntico ao APK instalado; apkanalyzer manifest permissions sem permissão de rede
- prior-art: docs/build-apk.md §4 — procedimento de bump e sideload já documentado

## Contexto de entrada
- app/build.gradle.kts
- docs/build-apk.md
- HANDOFF.md

## Expected Output
- `app/build.gradle.kts`: `versionCode = 4`, `versionName = "1.2"`.
- `app/build/outputs/apk/release/app-release.apk` assinado com `versionCode = 4`.
- `apkanalyzer manifest permissions` sem nenhuma permissao de rede.
- `./gradlew.bat test` e `./gradlew.bat lint` com 0 falhas / 0 erros.
- HANDOFF.md atualizado com o estado da v1.2 e o que ficou por verificar em aparelho.

## Escalation
- Ao humano: se a assinatura falhar. `fastin-release.jks` e insubstituivel; sem ela a atualizacao por cima nao instala.
- Ao orquestrador: nenhuma prevista.
