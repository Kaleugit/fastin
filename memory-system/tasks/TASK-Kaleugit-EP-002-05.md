# TASK-Kaleugit-EP-002-05 - Elevar versao para v1.3 e gerar o APK

- Status: COMPLETED
- Priority: 4
- Description: O pedido chegou como "v1.0.3", mas a versao instalada e a v1.2 / versionCode 4; o Android recusa versionCode menor ou igual. Subir para v1.3 / versionCode 5, rodar testes, lint e `assembleRelease` com a mesma chave, conferir ausencia de permissao de rede e copiar o APK para o Desktop.
- Depends On: TASK-Kaleugit-EP-002-01, TASK-Kaleugit-EP-002-02, TASK-Kaleugit-EP-002-03, TASK-Kaleugit-EP-002-04
- Blocked By: None
- Branch: TASK-Kaleugit-EP-002-05-implement
- Workstreams: None
- Execution Mode: Quick
- Last Updated: 2026-09-03
- Completed: 2026-09-03
- Evidence: ver HANDOFF.md §2 (contagem de testes, lint, tamanho e SHA-256 do APK)
- prior-art: memory-system/tasks/TASK-Kaleugit-EP-001-06.md — mesmo roteiro

## Contexto de entrada
- app/build.gradle.kts
- docs/build-apk.md
- HANDOFF.md

## Expected Output
- `app/build/outputs/apk/release/app-release.apk` assinado, `versionCode = 5`, copiado para `C:\Users\kaleu\Desktop\fastin-v1.3.apk`.
- `apkanalyzer manifest permissions` sem permissao de rede.
- `HANDOFF.md` com o estado da v1.3.

## Escalation
- Ao humano: se a assinatura falhar — `fastin-release.jks` e insubstituivel.
