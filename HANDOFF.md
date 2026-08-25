# HANDOFF — estado do projeto

Escrito ao fim da sessão de construção inicial e **atualizado após o primeiro uso real**.

**Estado em 2026-08-25:** v1.0.2 instalada e rodando no aparelho do usuário. Três dias de
uso revelaram dois bugs — um deles de perda de dados — e ambos estão corrigidos e
confirmados no aparelho. O usuário atualizou, os dados continuaram, e funcionou.

---

## 1. Onde as coisas estão

| Item | Onde |
|---|---|
| Código | `C:\Users\kaleu\dev\fastin` · [github.com/Kaleugit/fastin](https://github.com/Kaleugit/fastin) (público) |
| APK pronto para instalar | `C:\Users\kaleu\Desktop\fastin.apk` — 1,5 MB, assinado v2+v3, **v1.0.2 / versionCode 3** |
| APK gerado pelo build | `app/build/outputs/apk/release/app-release.apk` |
| Chave de assinatura | `fastin-release.jks` na raiz — **não versionada** |
| Screenshots das telas | `docs/screenshots/*.png` |
| Página no portfólio | `kaleu.dev/projetos/fastin` — já em produção |

**A chave é insubstituível.** Sem `fastin-release.jks` + `keystore.properties`, a próxima
versão não instala por cima da atual: o Android recusa update assinado por chave diferente, e
desinstalar apaga o banco. Guarde os dois junto dos CSVs de backup.

---

## 2. O que está verificado e o que não está

Esta é a parte mais importante deste documento.

### Verificado

- **95 testes na JVM, 0 falhas.** Rodados duas vezes com `--rerun-tasks` para descartar flakiness.
- **Lint: 0 erros.**
- **APK release compila, é assinado (v2+v3) e não declara nenhuma permissão de rede** —
  conferido com `apkanalyzer manifest permissions`.
- **O R8 preserva os `serializer()`** de cada `@Serializable` — conferido com
  `apkanalyzer dex packages`. Se isso quebrar, o usuário perde a configuração do dashboard.
- **As seis telas renderizam** — o `ScreenshotTest` compõe e desenha cada uma de verdade.

### Verificado no aparelho — uso real, 2026-08-25

O usuário instalou, usou por três dias e depois atualizou para a v1.0.2. Confirmado por ele:

- **Sideload e atualização por cima preservando o banco.** `versionCode` novo + mesma chave,
  como `docs/build-apk.md` §4 descreve. Ele confirmou que os dados continuaram.
- **Formulário do dia**, incluindo o `TimePickerDialog` do sistema para os horários.
- **Export CSV**: gera o arquivo em Downloads e o conteúdo é legível. Foi o export que
  revelou a perda de dados — a coluna `first_meal_time` vinha vazia.
- **Calendário e dashboard** renderizam e refletem o que foi registrado.
- **Fontes e sombras** no aparelho dele: nenhuma queixa visual.

### NÃO verificado — ainda sem observação em uso

| O quê | Por que ainda importa |
|---|---|
| **Notificações** | O agendamento tem teste (`MilestoneNotifierTest`), mas o disparo real do WorkManager e o pedido de permissão do Android 13+ nunca foram vistos. **É o maior buraco restante.** |
| **Import de CSV** | O export foi exercitado de verdade; o import não. O picker do sistema e o `content://` que ele devolve seguem sem verificação — e é justamente o caminho de restaurar backup ao trocar de aparelho. |
| **Sombras em API 26–27** | `spotColor` exige API 28+. No aparelho do usuário funcionou; em Android 8–8.1 a sombra sai preta padrão. |
| **Streak em uso prolongado** | Só jejuns fechados contam. Com poucos dias não dá para julgar se a regra é a certa. |
| **Performance do tick** | O relógio recompõe 1×/s. Testado, não medido em aparelho. |

---

## 3. Ambiente — leia antes de buildar

Esta máquina é **Windows 11 ARM64**, e isso tem duas consequências que não são óbvias:

1. **Não existe emulador Android para Windows ARM64.** O Google não publica o pacote
   `emulator` para essa plataforma — ele nem aparece no `sdkmanager --list`. Por isso as
   telas são vistas via `ScreenshotTest`, não por emulador.
2. **O Robolectric só publica binário nativo x86_64 para Windows**, e uma JVM ARM64 não
   carrega DLL x64. Por isso `gradle.properties` tem `fastin.testJdkX64` apontando para um
   **segundo JDK, x64**, usado *apenas* pela tarefa de teste. Compilação, Kotlin, KSP, R8 e o
   empacotamento do APK seguem em ARM64 nativo — o APK nunca passa por emulação.

Detalhes em `docs/decisions.md` → ADR-007.

```powershell
# Terminal PowerShell, aberto DEPOIS da configuração das variáveis de ambiente.
cd $HOME/dev/fastin
./gradlew.bat test              # 95 testes
./gradlew.bat assembleRelease   # APK assinado
./gradlew.bat test --tests "*ScreenshotTest*"   # regenera docs/screenshots/
```

`JAVA_HOME` e `ANDROID_HOME` estão nas variáveis do usuário. Terminal aberto antes disso não
as enxerga — é o erro `JAVA_HOME is not set`. Troubleshooting completo em `docs/build-apk.md` §8.

---

## 4. Convenções que o próximo trabalho precisa manter

Estas não são preferências de estilo; cada uma nasceu de um bug real desta sessão.

### Testes

- **Toda asserção que exige um efeito precisa de um par que exige a ausência dele.**
  Sem isso, uma função quebrada passa para sempre. Aconteceu: o clique em salvar caía fora da
  janela, nada era salvo, e dois testes que afirmavam `assertNull(...)` passavam provando nada.
- **`performScrollTo()` antes de todo toque.** Toque em nó fora da janela é descartado em
  silêncio — sem exceção, sem aviso.
- **`useUnmergedTree = true`** ao procurar nós dentro de `clearAndSetSemantics` (células do
  calendário, pills de marco, campos de hora).
- **Espera ativa para Room e DataStore.** As emissões são assíncronas em outra thread; drenar
  o looper não basta. Ver o helper `awaitUntil` em `DashboardScreenTest`.
- **ViewModel de teste vem de um `ViewModelStore`**, não de `remember {}`. O ticker do relógio
  é um loop infinito: sem `store.clear()` no `@After` ele sobrevive ao teste e explode no
  teste *seguinte*.
- **DataStore recebe arquivo próprio por teste.** O delegate `by preferencesDataStore(...)`
  guarda uma instância por processo — foi por isso que `DashboardConfigStore` passou a receber
  o store por injeção.

### Domínio

- **Nenhum cálculo lê o relógio.** Tudo recebe `Clock` injetado. É o que torna virada de dia
  e horário de verão testáveis.
- **Dado ausente nunca vira zero.** "Não registrei se bebi água" ≠ "não bebi água". Dias sem o
  campo ficam fora da série.

### UI

- **Nenhuma cor, raio, sombra ou tamanho literal fora de `ui/theme/`.** O checklist de review
  está em `docs/design-system.md` §8.
- **Sem `BlurMaskFilter`/canvas nativo para sombra** — não é acelerado por GPU e vira repaint
  em software no scroll.

---

## 5. Aberto — candidatos a melhoria

Nada aqui é bug conhecido; é escopo que ficou de fora conscientemente.

| Item | Situação |
|---|---|
| **Período customizado no dashboard** | `MetricEngine` já suporta `Period.CUSTOM`, inclusive corrigindo intervalo invertido, e tem teste. **Falta só o seletor de datas na UI.** É o item mais barato de completar. |
| **Reordenar cards por arrasto** | Marcado como opcional na spec v1. Exige gesto customizado sobre coluna rolável. |
| **Meta de streak fixa em 16h** | `StreakCalculator` já aceita `goal` como parâmetro e tem teste com meta customizada. Falta expor em Ajustes. |
| **Streak não conta jejum em andamento** | Hoje só jejuns fechados contam. Se você estiver em 17h abertas, o streak não considera. Decisão defensável, mas revisível com uso real. |
| **`docs/PROJECT.md` §6** | Critério "funciona em modo avião" foi verificado só por ausência de permissão no manifesto, não em uso. |
| **`prompt-claude-code-app-jejum.md`** | A spec original segue versionada na raiz. Mantida de propósito (mostra de onde o projeto partiu); remover é um `git rm`. |

### Fora deste repositório

- **`kaleu.dev-site/ARCHITECTURE.md` está desatualizada** — descreve o site como "under
  construction com cena 3D de escavadeira", sem mencionar as 7 páginas de projeto, o `Nav`, o
  `LanguageProvider` (i18n pt/en) nem a decisão do `images.unoptimized` em dev. Não foi tocada
  porque estava fora do pedido.

---

## 6. Mapa rápido

```
docs/PROJECT.md        fonte da verdade: escopo, modelo de dados, telas, aceitação
docs/architecture.md   camadas, fluxo de dados, o cálculo de jejum
docs/decisions.md      9 ADRs — leia o 007 antes de mexer no build
docs/design-system.md  tokens e checklist de review de UI
docs/build-apk.md      build, sideload, troubleshooting (PowerShell)

app/src/main/java/dev/kaleu/fastin/
  domain/fasting/FastingCalculator.kt   ← a regra central do app
  domain/metrics/MetricEngine.kt        ← métrica nova mexe só aqui
  ui/theme/                             ← única fonte de cor/raio/sombra
```

As personas de desenvolvimento estão em `.claude/skills/` (`architect`, `frontend`, `backend`,
`devops`, `testing`, `review`), adaptadas de `~/dev/agentes-oda` para contexto Android.

---

## 6b. Achados do primeiro uso real (2026-08-25)

O usuário instalou, registrou 3 dias e relatou que "os gráficos não se formaram". Não era
falta de dados — eram três bugs de desenho em `ui/dashboard/charts/Charts.kt`:

| Bug | Correção |
|---|---|
| Ponto único desenhado em `x = 0`, metade fora do card — o gráfico parecia vazio | Centralizado, com régua horizontal dando contexto de "este é o nível medido" |
| Eixo X ia de `0` a `largura` exata, sem margem | `PLOT_INSET_DP = 8` nas duas pontas |
| Marcador do último ponto em `x = largura`, metade cortado | Idem — afetava **qualquer** volume de dados, dava para ver cortado até com 100 dias |

Além disso, o card agora avisa `"1 dia com registro — a linha aparece a partir do segundo"`
quando há um ponto só. A dúvida do usuário era legítima e agora é respondida dentro do app.

**Regressão coberta** em `DashboardScreenTest`, com o par positivo/negativo: um ponto mostra
o aviso, dois pontos fazem o aviso sumir.

### Perda de dados no formulário — corrigida (o achado mais grave)

O usuário relatou que a "primeira refeição" que ele havia registrado sumiu, e que a coluna
veio vazia no CSV. Não era a exportação: o dado tinha sido apagado do banco.

`DayEntryViewModel.init` buscava o registro do dia de forma assíncrona e fazia
`_uiState.value = ...` — **sobrescrita incondicional** — enquanto `DayEntryScreen` já
renderizava o formulário editável, porque nada consultava `isLoading`. Dois caminhos:

1. **Salvar antes da carga chegar.** O `@Upsert` grava a linha inteira; sobre um estado em
   branco, zerava os campos que o usuário nunca tocou. Foi este que atingiu o usuário.
2. **A carga chegar depois de o usuário digitar.** A atribuição descartava o que ele tinha
   acabado de preencher.

Corrigido em três frentes: o `init` **mescla** (valor do banco só preenche campo vazio),
`save()` recusa enquanto `isLoading`, e a tela mostra estado de carregamento em vez de um
formulário editável.

Coberto por `DayEntryLoadRaceTest`, com controle negativo. **Nota de mecânica:** o Flow do
Room emite do executor dele, que `StandardTestDispatcher` não governa — `advanceUntilIdle()`
volta com o `init` ainda suspenso. Para exercitar "digitou antes da carga" foi preciso um DAO
falso com a emissão sob controle do teste. O primeiro teste que escrevi falhava por esse
motivo, não pelo bug.

### O mínimo real de dias (não é bug, é a regra do domínio)

- **Peso**: 1 dia já vira ponto; 2+ viram linha.
- **Horas de jejum**: precisa de **dois dias consecutivos** — o jejum de um dia usa a última
  refeição do dia anterior. E o ponto de *hoje* só existe depois que a primeira refeição de
  hoje for registrada.
- **Heatmap**: funciona com pouco dado, mas só faz sentido visual com algumas semanas.

---

## 7. Primeira coisa a fazer na volta

O ciclo de instalar-e-usar já rodou uma vez e valeu a pena: os dois únicos bugs do projeto
saíram dele, não dos 95 testes. Repetir é o melhor uso do tempo.

1. **Continuar usando.** Cada semana de uso real cobre território que teste nenhum alcança.
2. **Fechar o maior buraco: as notificações.** É a única feature entregue que nunca foi vista
   funcionando. Ligue em Ajustes, registre uma última refeição, e veja se o aviso de 16h
   chega. Se não chegar, comece por `MilestoneNotifier.reschedule` e pela permissão
   `POST_NOTIFICATIONS` no Android 13+.
3. **Testar o import de CSV** ao menos uma vez. É o caminho de restaurar backup ao trocar de
   aparelho — descobrir que ele não funciona no dia da troca seria o pior momento possível.
4. **Trazer o que incomodar.** Ajuste vindo de uso real vale mais que qualquer item da §5.
