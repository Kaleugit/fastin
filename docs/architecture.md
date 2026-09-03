# Arquitetura — fastin

Single-module Android app. Sem backend, sem rede, sem DI framework. O objetivo é o menor
número de peças que ainda deixa a lógica de jejum testável na JVM.

## Camadas

```
ui/            Compose. Telas + componentes do design system. Sem lógica de domínio.
 ├─ theme/         tokens (FastinTheme), superfícies neumórficas, gradiente de acento
 ├─ components/    Surface, Chip, TimeField, WeightField, marcos do relógio
 ├─ calendar/      CalendarScreen + CalendarViewModel
 ├─ entry/         DayEntryScreen + DayEntryViewModel
 ├─ dashboard/     DashboardScreen + DashboardViewModel + charts/ (Canvas puro)
 └─ settings/      backup CSV, notificações

domain/        Kotlin puro. Zero import de android.*. 100% testável na JVM.
 ├─ model/         FastingLog, Tristate, Quality, YesNo, FastingWindow, Milestone
 ├─ fasting/       FastingCalculator  (função pura, Clock injetado)
 ├─ metrics/       MetricEngine       (série/agregação por métrica+período)
 └─ streak/        StreakCalculator

data/          Room + DataStore. Único lugar que conhece persistência.
 ├─ db/            FastinDatabase, FastingLogEntity, FastingLogDao, Converters
 ├─ repo/          FastingLogRepository (expõe Flow<...> de modelos de domínio)
 ├─ prefs/         DashboardConfigStore (DataStore, JSON dos cards)
 └─ backup/        CsvExporter, CsvImporter

notify/        WorkManager + canal de notificação dos marcos.
```

**Regra de dependência:** `ui → domain ← data`. `domain` não depende de ninguém.
`ui` nunca importa `androidx.room` nem toca DAO direto.

## Fluxo de dados

`FastingLogDao` expõe `Flow<List<FastingLogEntity>>` → `Repository` mapeia para modelos de
domínio → `ViewModel` combina com `Clock`/tick e expõe `StateFlow<UiState>` → Compose coleta
com `collectAsStateWithLifecycle`. Escrita é `suspend` + `@Upsert`.

## O ponto delicado: cálculo de jejum

`FastingCalculator` é objeto sem estado:

```kotlin
fun window(day: LocalDate, previous: FastingLog?, current: FastingLog?): FastingWindow?
fun elapsed(window: FastingWindow, now: Instant): Duration
fun milestones(window: FastingWindow): List<Milestone>   // 16h, 18h, 20h, 24h
```

- Início = `previous.lastMealTime` em `day - 1`. Fim = `current.firstMealTime` em `day`.
- `previous.lastMealTime == null` → retorna `null` (sem jejum calculável).
- `current.firstMealTime == null` → janela **aberta**; duração medida contra `now`.
- Conversão para instante usa `ZoneId.systemDefault()` **na borda**, não dentro do cálculo,
  para o horário de verão não distorcer a duração.
- Guarda: duração negativa ou > 100h ⇒ janela inválida, não exibida (`MAX_PLAUSIBLE`; era 48h até a v1.2).

O tick do relógio é um `flow { while(true) { emit(clock.instant()); delay(1_000) } }` no
ViewModel. O composable não tem timer próprio.

## Dashboard

Cada card é um `ChartCardConfig(id, type, metric, period, aggregation)` serializado em JSON
no DataStore. `MetricEngine` recebe config + `List<FastingLog>` e devolve `ChartData`
(série de pontos, ou um escalar para `BIG_NUMBER`). Os renderizadores em `ui/dashboard/charts/`
só desenham `ChartData` — não sabem o que é jejum ou peso. Adicionar métrica nova = tocar
só em `MetricEngine`.

## Injeção de dependência

Manual, via um `AppContainer` criado na `Application` e passado por `ViewModelProvider.Factory`.
Hilt/Koin seriam mais cerimônia do que valor para ~6 ViewModels. Ver ADR-003.

## Testes

- `app/src/test/` — JUnit puro para `domain/` (calculadora, métricas, streak).
- `app/src/test/` — Room in-memory para `data/`.
- `app/src/test/` — Compose UI test sob Robolectric para as telas.
- `app/src/androidTest/` — só se houver device conectado; não é gate.
