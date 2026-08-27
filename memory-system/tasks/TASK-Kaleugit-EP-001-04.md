# TASK-Kaleugit-EP-001-04 - Rotulos de eixo nos graficos

- Status: COMPLETED
- Priority: 3
- Description: `Charts.kt` desenha so o traco: nenhum renderizador emite texto, entao o grafico nao diz quanto nem quando. Adicionar rotulos de eixo Y (min/max com a unidade da metrica) e de eixo X (data inicial/final) em LINE e SCATTER, e legenda de escala no HEATMAP.
- Depends On: None
- Blocked By: None
- Branch: TASK-Kaleugit-EP-001-04-implement
- Workstreams: None
- Execution Mode: Standard
- Last Updated: 2026-08-27 14:30
- Completed: 2026-08-27 14:30
- Evidence: PASS — DashboardScreenTest: rótulos com dados, ausência com estado vazio, série constante com um valor só, legenda do heatmap
- prior-art: app/src/main/java/dev/kaleu/fastin/ui/dashboard/DashboardScreen.kt:189 — formatScalar; mesma regra de casas decimais aplicada aos rótulos

## Contexto de entrada
- app/src/main/java/dev/kaleu/fastin/ui/dashboard/charts/Charts.kt
- app/src/main/java/dev/kaleu/fastin/domain/metrics/Metrics.kt
- app/src/main/java/dev/kaleu/fastin/ui/dashboard/DashboardScreen.kt
- docs/design-system.md
- app/src/test/java/dev/kaleu/fastin/ui/DashboardScreenTest.kt

## Expected Output
- `LineChart` e `ScatterChart` exibem min e max do eixo Y com `data.config.metric.unit` (`isRate` exibe `%`) e as datas inicial/final no eixo X em `d MMM` pt-BR.
- `HeatmapChart` exibe legenda de escala (menos -> mais).
- Rotulos sao `Text` do Compose, nunca `drawText` nativo (ADR-001; HANDOFF.md 4).
- `CHART_HEIGHT_DP` permanece a altura da area de desenho; a faixa de rotulos e reservada a parte.
- Nenhuma cor, tamanho ou raio literal fora de `ui/theme/` (design-system.md 8).
- `DashboardScreenTest` com par positivo/negativo: serie com 2+ pontos exibe os quatro rotulos; serie vazia nao exibe nenhum. Teste para serie constante (min == max).
- `./gradlew.bat test` com 0 falhas; screenshots regeneradas.

## Escalation
- Ao humano: se os rotulos nao couberem em 140dp sem prejudicar a leitura do traco.
- Ao orquestrador: se exigir mudanca em `MetricEngine` ou em `ChartData`.
