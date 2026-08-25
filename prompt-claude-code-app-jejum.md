# Prompt para Claude Code — App Android de Controle de Jejum

Cole o texto abaixo no Claude Code (dentro da pasta vazia do projeto).

---

## Contexto

Quero criar um app Android simples, para uso pessoal, que será instalado via APK (sideload), **sem publicação na Google Play**. O app serve para eu registrar diariamente dados sobre meu jejum intermitente, alimentação e peso, e visualizar isso em gráficos.

## Stack técnica (sugestão — pode ajustar se tiver motivo melhor)

- **Kotlin + Jetpack Compose** (nativo Android), app single-module, sem backend.
- **Room (SQLite)** para persistência 100% local no dispositivo.
- **MPAndroidChart** ou **Vico** (biblioteca de gráficos em Compose) para os gráficos.
- Build gerando **APK assinado local** (debug/release key simples), sem dependência de Play Services.
- Não precisa de login, internet, nem sync em nuvem.

Se você (Claude Code) tiver uma recomendação melhor de stack para um app pessoal simples, sideload, 100% offline, pode sugerir antes de começar — mas o padrão acima é o que prefiro caso não haja motivo forte para mudar.

## Modelo de dados

Uma tabela principal `fasting_log`, uma linha por dia (`date` como chave única), com todos os campos **opcionais** (nullable):

| Campo | Tipo | Observação |
|---|---|---|
| date | DATE (PK) | dia do registro |
| last_meal_time | TIME | última refeição do dia |
| first_meal_time | TIME | primeira refeição do dia seguinte |
| caloric_deficit | ENUM(sim, talvez, não) | |
| meal_quality | ENUM(bom, médio, ruim) | |
| water_2l | ENUM(sim, talvez, não) | |
| alcohol | ENUM(sim, não) | |
| weight | DECIMAL | em kg, com 1 casa decimal |
| notes | TEXT | campo livre opcional (sugestão minha — ver abaixo) |

**Cálculo do jejum:** o jejum de um dia começa em `last_meal_time` do dia anterior e termina em `first_meal_time` do dia atual. Ou seja, para calcular o jejum "de hoje" o app precisa olhar `last_meal_time` de `date - 1`.

## Telas

### 1. Calendário (tela inicial)
- Visão de mês, navegação entre meses.
- Cada dia mostra um indicador visual leve (ex: ponto colorido) se já tem dado registrado.
- Ao tocar em um dia, abre o formulário de registro daquele dia.

### 2. Formulário de registro do dia
- Todos os campos da tabela acima, todos opcionais.
- Campos de hora com time picker simples.
- Campos enum como chips/segmented buttons (sim/talvez/não, bom/médio/ruim).
- Peso com teclado numérico.
- Botão salvar (upsert no Room).

### 3. Relógio de jejum em tempo real
- Componente sempre visível (pode ser no topo da tela inicial, tipo card fixo).
- Calcula em tempo real, a partir da última `last_meal_time` registrada, quantas horas e minutos de jejum já se passaram (atualiza a cada segundo/minuto, sem precisar recarregar tela).
- Mostra marcos: horário previsto para bater 16h, 18h, 20h e 24h de jejum, com indicação visual de quais marcos já foram batidos.
- Se não houver `last_meal_time` registrada para o dia anterior, mostrar estado vazio ("nenhum jejum em andamento — registre sua última refeição").

### 4. Dashboard / KPIs customizáveis
- Tela onde eu posso **adicionar, remover e configurar cards de gráfico**.
- Cada card tem:
  - Tipo de visualização: linha, dispersão (scatter), heatmap (mapa de calor tipo GitHub contribution graph), big number (número grande com média/total/streak).
  - Métrica: horas de jejum, peso, % de dias com déficit calórico, % de dias com água ≥2L, qualidade média das refeições, dias com álcool, etc.
  - Período: últimos 7/30/90 dias, mês atual, intervalo customizado.
  - Agregação (quando aplicável): média, soma, contagem, mínimo, máximo.
- Layout em grid, arrastável/reordenável se possível (não é obrigatório na v1).
- Configuração de cada card salva localmente (para persistir entre sessões).

## Feats extra:

1. **Streak de jejum**: contador de dias seguidos batendo alguma meta (ex: ≥16h), como big number no dashboard.
2. **Exportar/Importar backup**: botão para exportar todos os dados em CSV ou pdf para a pasta de Downloads, e importar de volta — importante porque é sideload e não tem sync em nuvem, então isso é o "backup manual" antes de trocar de celular ou reinstalar.
3. **Campo de observações livre** (`notes`) por dia, para anotar contexto (ex: "jantar fora", "gripado").
4. **Modo escuro** seguindo o tema do sistema.
5. **Notificação local opcional** avisando quando bater 16h/18h/20h de jejum (sem precisar internet, só AlarmManager/WorkManager local).

## Prioridade de entrega

1. Modelo de dados + Room configurado.
2. Calendário + formulário de registro.
3. Relógio de jejum em tempo real.
4. Dashboard com pelo menos 2-3 tipos de gráfico funcionando (line, big number, heatmap).
5. Build do APK release para eu instalar via sideload.
6. Export/import de backup csv ou pdf,

## Como quero que você trabalhe

- Vá construindo em etapas, usando a estrutura de agentes disponiveis na pasta e me mostrando o que foi feito a cada etapa em vez de tentar entregar tudo de uma vez.
- Priorize simplicidade e código legível — é um app pessoal, não precisa de arquitetura enterprise. Use o taste-skill para elaborar o design frontend.
- Ao final, me dê o passo a passo de como gerar o APK e instalar no celular via ADB ou transferência direta do arquivo.
