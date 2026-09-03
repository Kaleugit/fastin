# PROJECT.md — fastin (fonte da verdade)

Derivado de `prompt-claude-code-app-jejum.md`. Este documento é o contrato: nada é
implementado que não esteja aqui (sem invenção de feature).

## 1. Produto

App Android pessoal para registrar diariamente jejum intermitente, alimentação e peso, e
visualizar isso em gráficos. Um único usuário, um único dispositivo.

**Fora de escopo:** login, backend, sync em nuvem, Play Store, Play Services, multiusuário,
multi-dispositivo, internet de qualquer natureza.

## 2. Modelo de dados

Tabela única `fasting_log`, **uma linha por dia**, `date` como PK. **Todos os demais campos
são nullable** — o usuário pode preencher parcialmente e voltar depois.

| Campo | Tipo Kotlin | Coluna | Nulo? |
|---|---|---|---|
| `date` | `LocalDate` | `date` (PK, TEXT ISO-8601) | não |
| `lastMealTime` | `LocalTime?` | `last_meal_time` (TEXT HH:mm) | sim |
| `firstMealTime` | `LocalTime?` | `first_meal_time` (TEXT HH:mm) | sim |
| `caloricDeficit` | `Tristate?` | `caloric_deficit` | sim |
| `mealQuality` | `Quality?` | `meal_quality` | sim |
| `water2l` | `Tristate?` | `water_2l` | sim |
| `alcohol` | `YesNo?` | `alcohol` | sim |
| `weight` | `Double?` | `weight` (kg, 1 casa decimal) | sim |
| `notes` | `String?` | `notes` | sim |

Enums: `Tristate = YES | MAYBE | NO` · `Quality = GOOD | AVERAGE | BAD` · `YesNo = YES | NO`.

### Regra de cálculo do jejum (central)

> O jejum **do dia D** começa em `lastMealTime` de **D-1** e termina em `firstMealTime` de **D**.

- Se `lastMealTime(D-1)` é nulo → não há jejum calculável para D.
- Se `firstMealTime(D)` é nulo → o jejum de D está **em andamento** (duração = agora − início).
- Duração = `Duration.between(D-1 @ lastMealTime, D @ firstMealTime)`. Atravessa a meia-noite
  por construção; se der negativo ou > 100h (era 48h até a v1.2; subiu para o marco de 48h ser alcançável — DA-016), trate como dado inválido e não exiba.

## 3. Telas

### 3.1 Calendário (inicial)
- Visão de mês, navegação entre meses (setas, como em `design-ref/img-ref01.png`).
- Indicador leve (ponto) no dia que já tem qualquer dado registrado.
- Tocar num dia abre o formulário daquele dia.
- Card fixo do relógio de jejum no topo (§3.3).
- As três abas (Calendário, Dashboard, Ajustes) trocam por toque na barra inferior **ou por
  swipe horizontal** (v1.3, EP-002). O ícone de Ajustes é uma engrenagem.

### 3.2 Formulário do dia
- Todos os campos de §2, todos opcionais.
- Horas: time picker simples. Enums: chips/segmented. Peso: teclado numérico decimal.
- `notes`: campo de texto livre multilinha.
- Botão salvar = upsert no Room.

### 3.3 Relógio de jejum em tempo real
- Card sempre visível no topo da tela inicial.
- A partir do último `lastMealTime` registrado, mostra horas:minutos de jejum decorrido,
  **atualizando sozinho** (tick a cada segundo, sem recarregar tela).
- Marcos **escolhidos pelo usuário em Ajustes** (default 16h, 18h, 20h, 24h; opções de 12h a
  48h): exibe o horário previsto de cada um e marca visualmente os já batidos. A lista é a
  **mesma** que gera as notificações (§4.5) — v1.3, EP-002.
- Card compacto: anel de progresso à esquerda, marcos em lista à direita, para o calendário
  do mês caber inteiro na tela sem rolar (v1.3).
- Estado vazio: "nenhum jejum em andamento — registre sua última refeição".

### 3.4 Dashboard / KPIs customizáveis
Usuário **adiciona, remove e configura** cards de gráfico. Cada card tem:

- **Tipo:** `LINE` · `SCATTER` · `HEATMAP` (estilo GitHub contribution) · `BIG_NUMBER`.
- **Métrica:** horas de jejum · peso · % dias com déficit calórico · % dias com água ≥2L ·
  qualidade média das refeições · dias com álcool.
- **Período:** últimos 7/30/90 dias · mês atual · intervalo customizado.
- **Agregação** (quando aplicável): média · soma · contagem · mínimo · máximo.
- Layout em grid. Reordenar por arrasto é **opcional na v1**.
- Configuração de cada card **persiste entre sessões**.

## 4. Feats extra

1. **Streak de jejum** — dias seguidos batendo a meta (≥16h), como `BIG_NUMBER`.
2. **Exportar/Importar backup CSV** — exporta tudo para Downloads e reimporta. É o único
   backup existente (sideload, sem nuvem); crítico ao trocar de aparelho.
3. **`notes`** livre por dia (já em §2).
4. **Modo escuro seguindo o tema do sistema.** Ver ADR sobre isso em `docs/decisions.md`.
5. **Notificação local opcional** ao bater cada marco escolhido em Ajustes (§3.3; default
   16h/18h/20h/24h, opções de 12h a 48h). `WorkManager`/`AlarmManager`, sem internet, sem
   Play Services.

## 5. Ordem de entrega (prioridade do usuário)

| # | Etapa | Entrega | Status |
|---|---|---|---|
| 1 | Modelo de dados + Room | Entidade, DAO, cálculo de jejum testado | ✅ |
| 2 | Calendário + formulário | Telas 3.1 e 3.2 navegáveis e persistindo | ✅ |
| 3 | Relógio de jejum | Tela 3.3 com tick real e marcos | ✅ |
| 4 | Dashboard | 4 tipos de gráfico (line, scatter, heatmap, big number) | ✅ |
| 5 | APK release | Build assinado + `docs/build-apk.md` | ✅ |
| 6 | Export/import CSV | Backup manual simétrico | ✅ |
| — | Notificações locais | Marcos de 16/18/20h via WorkManager | ✅ |

### Fora do escopo entregue

- **Reordenar cards por arrasto** (§3.4). A spec marca como opcional na v1; exige gesto
  customizado sobre coluna rolável e o ganho num app pessoal é pequeno.
- **Período customizado na UI** (§3.4). O motor (`MetricEngine`) suporta `Period.CUSTOM`,
  inclusive corrigindo intervalo invertido, e há teste. O que falta é só o seletor de datas
  na tela de configuração do card — os períodos fixos cobrem o uso diário.

## 6. Critérios de aceitação transversais

- Nenhum campo obrigatório no formulário; salvar com tudo vazio não quebra.
- App abre e funciona em **modo avião** (zero permissão de internet no manifest).
- Cálculo de jejum correto atravessando meia-noite e horário de verão.
- Dados sobrevivem a fechar/reabrir o app e a reinstalar por cima (mesmo `applicationId`).
- Nenhum crash em dia sem registro, mês sem registro, ou base totalmente vazia.
