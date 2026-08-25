# Decisões técnicas — fastin

Formato enxuto: contexto → decisão → alternativas rejeitadas → consequência.

---

## ADR-001 — Gráficos desenhados em Compose Canvas, sem biblioteca

**Contexto.** A spec sugere MPAndroidChart ou Vico. Os tipos exigidos são `LINE`, `SCATTER`,
`HEATMAP` (estilo GitHub) e `BIG_NUMBER`. O design de referência pede linha laranja
incandescente com glow e preenchimento em gradiente sobre superfície escura.

**Decisão.** Desenhar os quatro tipos com `Canvas` do Compose, em `ui/dashboard/charts/`.

**Alternativas rejeitadas.**
- *MPAndroidChart*: é biblioteca de View — exigiria `AndroidView` interop, tema próprio a
  ser sobrescrito, e distribuição via JitPack (resolução instável). Não faz heatmap.
- *Vico*: é Compose-native e boa, mas também não tem heatmap nem big number — metade dos
  cards seria Canvas de qualquer jeito, e sobraria uma dependência para dois tipos.

**Consequência.** Zero dependência de gráfico; controle total do visual (glow, gradiente,
raio); heatmap e big number saem naturais. Em troca, escrevemos eixos e escala à mão — o
que para 4 tipos simples é ~250 linhas, não uma engine.

---

## ADR-002 — Tema dark-only (desvio consciente da spec)

**Contexto.** A spec §Feats-4 pede "modo escuro seguindo o tema do sistema". As duas
referências em `design-ref/` são exclusivamente dark neumórficas: a legibilidade do
neumorfismo depende de sombra dupla (escura + clara) sobre uma superfície de luminância
média, e a transposição fiel para light exige um segundo sistema de sombras.

**Decisão.** v1 é **dark-only**. `FastinTheme` força o esquema escuro independente do
sistema; a estrutura de tokens já sai preparada para um segundo esquema.

**Alternativas rejeitadas.**
- *Seguir o sistema com Material 3 padrão*: cumpre a spec, mas descarta as referências.
- *Neumorfismo em light*: dobra a superfície de design e teste sem ganho para um app que o
  autor usa majoritariamente à noite.

**Consequência.** Desvio explícito e autorizado pelo usuário. Custo de reverter é baixo:
os tokens são um `data class` — um segundo `lightColors()` e a leitura de `isSystemInDarkTheme()`
reativam o comportamento da spec sem tocar em nenhum composable.

---

## ADR-003 — Injeção de dependência manual (`AppContainer`)

**Contexto.** ~6 ViewModels, um banco, um DataStore. Nenhum grafo dinâmico, nenhum escopo
além de singleton e de tela.

**Decisão.** `AppContainer` instanciado na `Application`, ViewModels criados por
`viewModelFactory { }`.

**Alternativas rejeitadas.** *Hilt* (plugin KAPT/KSP, `@HiltAndroidApp`, anotação por classe,
build mais lento e um passo a mais no ARM64 emulado); *Koin* (dependência + resolução em
runtime que falha tarde).

**Consequência.** Grafo explícito e legível; erro de fiação vira erro de compilação. Se o
app crescer muito, migrar para Hilt é mecânico.

---

## ADR-004 — Datas e horas com `java.time` + desugaring

**Contexto.** `minSdk` 26 já tem `java.time` nativo, mas o desugaring garante o mesmo
comportamento se o mínimo baixar depois. O cálculo de jejum atravessa meia-noite e precisa
respeitar horário de verão.

**Decisão.** `LocalDate`/`LocalTime`/`Duration` no domínio, `Clock` **injetado** em toda
função que precisa de "agora", persistência como TEXT ISO-8601, `coreLibraryDesugaring`
habilitado.

**Consequência.** Testes de virada de dia e DST são determinísticos (`Clock.fixed`). Nenhum
`System.currentTimeMillis()` dentro de `domain/`.

---

## ADR-005 — `compileSdk` 35, `minSdk` 26, `targetSdk` 35

**Contexto.** Sideload puro, um único aparelho, sem restrição de Play Store.

**Decisão.** `minSdk` 26 (Android 8.0) — patamar onde `java.time` e canais de notificação
existem nativamente. `compileSdk`/`targetSdk` 35 (versão instalada no SDK local).

**Consequência.** Nenhuma biblioteca de compatibilidade legada. Se o aparelho for mais novo
que API 35, `targetSdk` 35 continua válido — Android é retrocompatível para frente.

---

## ADR-006 — Backup em CSV, não PDF

**Contexto.** A spec aceita "CSV ou PDF". O propósito declarado é **restaurar** os dados ao
trocar de aparelho.

**Decisão.** CSV (uma linha por dia, header nomeado, `date` como chave de upsert na
importação).

**Alternativas rejeitadas.** *PDF*: não é reimportável — serviria como relatório, não como
backup, e o requisito é backup.

**Consequência.** Export e import são simétricos e testáveis. Se um relatório visual fizer
falta depois, entra como feature separada sem mexer no backup.

---

## ADR-007 — JVM de teste em x64 no Windows ARM64

**Contexto.** A máquina é Windows 11 ARM64. Duas paredes apareceram, ambas de ecossistema,
não de configuração:

1. O Robolectric instala o **Conscrypt** como security provider por padrão, e o Conscrypt
   não publica `conscrypt_openjdk_jni-windows-aarch_64`.
2. O runtime nativo do Robolectric (`nativeruntime-dist-compat`) publica
   `linux/x86_64`, `mac/aarch64`, `mac/x86_64` e `windows/x86_64` — **não existe
   `windows/aarch64`**. Sem ele, Room e Compose UI tests não sobem.

Uma JVM ARM64 não carrega DLL x64. Logo, nenhuma configuração faz esses testes rodarem
numa JVM ARM64 no Windows.

**Decisão.** Duas medidas, cada uma no seu escopo:

- `app/src/test/resources/robolectric.properties` → `conscryptMode=OFF`. Seguro porque o
  app não faz rede (sem permissão `INTERNET`), então nenhum caminho testado usa TLS.
- Um **segundo JDK, x64**, usado exclusivamente pela tarefa de teste, via a propriedade
  `fastin.testJdkX64` em `gradle.properties`. Compilação, Kotlin, KSP, R8 e o empacotamento
  do APK seguem no JDK ARM64 nativo.

**Alternativas rejeitadas.**
- *Trocar todo o build para JDK x64*: penalizaria o Kotlin e o KSP — a parte cara do build —
  para resolver um problema que só existe na execução dos testes.
- *Abandonar o Robolectric*: perderia teste de SQL e de UI na JVM, jogando a verificação
  para device conectado, que não é garantido (`.claude/skills/testing/SKILL.md`).
- *Testar o repositório com DAO falso*: não exercita SQL nenhum — é justamente onde os
  bugs de `@Query` e conversão de tipo moram.

**Consequência.** `./gradlew test` passa: 84/84. Em máquina x64, remover
a linha `fastin.testJdkX64` e tudo funciona igual. O APK entregue nunca passa por emulação.

---

## ADR-008 — Auto Backup do Android desligado

**Contexto.** O `android:allowBackup="true"` (default) faz o Android enviar o banco do app
para o Google Drive da conta do usuário, e restaurá-lo automaticamente numa reinstalação.
Seria uma rede de segurança gratuita para um log longitudinal.

**Decisão.** `allowBackup="false"`.

**Por quê.** O app foi especificado como 100% offline, sem nuvem e sem conta (PROJECT.md §1),
e a própria spec define o CSV como o backup manual justamente porque "não tem sync em nuvem"
(§4.2). Mandar um registro de saúde para o Google por padrão, sem o usuário pedir,
contrariaria a premissa que motivou o app inteiro.

**Consequência.** Desinstalar apaga os dados. `adb install -r` (atualizar por cima) preserva,
que é o fluxo documentado em `docs/build-apk.md`. A tela de Ajustes diz explicitamente que o
CSV é o único backup.

---

## ADR-009 — Testes unitários só na variante debug

**Contexto.** `./gradlew test` rodava `testDebugUnitTest` **e** `testReleaseUnitTest`. A
segunda falhava inteira: `compose.ui.test.manifest` — que fornece a Activity exigida por
`createComposeRule()` — é `debugImplementation`, e promovê-la a `implementation` a
empacotaria no APK de release.

**Decisão.** `testReleaseUnitTest` desabilitado em `app/build.gradle.kts`.

**Por quê.** Testes unitários rodam sobre classes **não minificadas** em qualquer variante:
`testReleaseUnitTest` executaria exatamente o mesmo bytecode do debug. Custo dobrado, zero
cobertura nova.

**Consequência.** `./gradlew test` significa "a suíte inteira, uma vez". O comportamento do
APK minificado continua verificado de outra forma: as regras de `keep` do R8 são conferidas
com `apkanalyzer dex packages` (os métodos `serializer()` de cada `@Serializable` precisam
sobreviver, senão o usuário perde a configuração do dashboard).
