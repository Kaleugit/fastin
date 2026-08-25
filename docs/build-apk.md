# Gerar o APK e instalar no celular

Tudo já está instalado e configurado nesta máquina. Este documento é o passo a passo para
repetir o processo — e para reconstruir o ambiente se você trocar de computador.

> **Os comandos aqui são PowerShell**, o terminal padrão do Windows. Onde a sintaxe do Git
> Bash difere, há uma segunda versão marcada. Colar um comando Bash no PowerShell dá erro de
> sintaxe (`Unexpected token`), não erro de build.
>
> **Abra um terminal novo antes de começar.** `JAVA_HOME` e `ANDROID_HOME` foram gravadas
> nas variáveis do usuário durante a configuração; um terminal aberto antes disso não as
> enxerga — é o erro `JAVA_HOME is not set`.

---

## 1. Gerar o APK

```powershell
cd $HOME/dev/fastin
./gradlew.bat assembleRelease
```

O arquivo sai em `app/build/outputs/apk/release/app-release.apk`.

Leva cerca de 1 minuto. O APK tem **~1,5 MB** (o R8 remove o código não usado; a versão
debug, sem otimização, tem 18 MB).

### Conferir que saiu assinado

Tudo **numa linha só** — no PowerShell a crase (`` ` ``) quebra linha, não a barra invertida.
O `&` na frente é obrigatório para executar um programa cujo caminho está entre aspas:

```powershell
& "$env:LOCALAPPDATA/Android/Sdk/build-tools/35.0.0/apksigner.bat" verify --verbose "app/build/outputs/apk/release/app-release.apk"
```

Resposta esperada:

```
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

`v1 scheme: false` é esperado — v1 é o formato antigo, dispensável a partir do Android 7, e
o nosso `minSdk` é 26 (Android 8).

<details>
<summary>A mesma verificação no Git Bash</summary>

```bash
"$LOCALAPPDATA/Android/Sdk/build-tools/35.0.0/apksigner.bat" verify --verbose \
  app/build/outputs/apk/release/app-release.apk
```
</details>

---

## 2. Instalar no celular

### Opção A — cabo USB (recomendada)

1. No celular: **Ajustes → Sobre o telefone → toque 7 vezes em "Número da versão"**.
   Isso libera as Opções do desenvolvedor.
2. **Ajustes → Sistema → Opções do desenvolvedor → Depuração USB: ligar.**
3. Conecte o cabo. O celular pergunta "Permitir depuração USB?" — aceite e marque
   "Sempre permitir deste computador".
4. No PC:

```powershell
adb devices
adb install -r app/build/outputs/apk/release/app-release.apk
```

O aparelho precisa aparecer como `device` no `adb devices`. Se aparecer `unauthorized`,
o diálogo de autorização ainda está pendente na tela do celular.

O `-r` reinstala por cima preservando os dados. Na primeira instalação tanto faz.

### Opção B — transferir o arquivo

1. Copie o `app-release.apk` para o celular (cabo, Google Drive, Telegram para si mesmo,
   o que for).
2. Abra o arquivo pelo gerenciador de arquivos do celular.
3. O Android avisa que o app veio de fonte desconhecida — autorize o gerenciador de arquivos
   a instalar apps e confirme.

---

## 3. Depois de instalar

- **Ative as notificações** em Ajustes dentro do app, se quiser os avisos de 16h/18h/20h.
  O Android 13+ pede permissão na primeira vez.
- **Faça um export de teste** (Ajustes → Exportar para Downloads) e confira que o arquivo
  apareceu. É o seu único backup; melhor descobrir agora se algo não funciona.

---

## 4. Atualizar o app depois

```powershell
./gradlew.bat assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Os dados são preservados porque o `applicationId` e a **chave de assinatura** são os mesmos.

> ### Guarde a chave junto do backup
>
> `fastin-release.jks` está na raiz do projeto e **não é versionado**. Se você perder essa
> chave, a próxima versão do app não instala por cima da atual — o Android recusa uma
> atualização assinada por chave diferente. Você teria que desinstalar, e desinstalar
> **apaga o banco**.
>
> Copie `fastin-release.jks` e `keystore.properties` para o mesmo lugar onde guarda os CSVs.

Para publicar uma versão nova, suba `versionCode` e `versionName` em `app/build.gradle.kts`.
O Android recusa instalar por cima um APK com `versionCode` menor que o instalado.

---

## 5. Ver as telas sem celular

Não existe emulador Android para Windows ARM64 — o Google não publica o pacote `emulator`
para essa plataforma. Em vez disso, as telas são renderizadas na JVM e salvas como PNG:

```powershell
./gradlew.bat test --tests "*ScreenshotTest*"
```

As imagens saem em `docs/screenshots/`. Como cada tela é composta e desenhada de verdade,
isso também funciona como smoke test de renderização: uma tela que lançasse ao medir ou
desenhar quebraria aqui, e nenhum outro teste pegaria — os demais só consultam a árvore de
semântica, que existe mesmo quando o desenho falha.

---

## 6. Rodar os testes

```powershell
./gradlew.bat test
```

95 testes, todos na JVM — não precisa de emulador nem de aparelho conectado.

| Suíte | O que cobre |
|---|---|
| `FastingCalculatorTest` | virada de meia-noite, horário de verão, marcos, jejum abandonado |
| `FastingLogRepositoryTest` | round-trip no Room, upsert, campos nulos |
| `MetricEngineTest` | métricas, agregações, períodos, dado ausente ≠ zero |
| `StreakCalculatorTest` | streak, quebra de sequência, borda do dia corrente |
| `CsvBackupTest` | round-trip do backup, vírgula em `notes`, CSV corrompido |
| `MilestoneNotifierTest` | agendamento dos avisos, sem notificação retroativa |
| `CalendarScreenTest` | grade, navegação de mês, marcador de registro |
| `DayEntryScreenTest` | todos os campos opcionais, persistência pelo caminho real |
| `FastingClockTest` | relógio andando de verdade, marcos acendendo |
| `DashboardScreenTest` | cards, adicionar/remover/configurar, persistência |
| `ScreenshotTest` | render de todas as telas + geração dos PNGs |

---

## 7. Reconstruir o ambiente noutra máquina

Foi assim que esta máquina foi configurada. Windows ARM64 tem particularidades — ver ADR-007.

```powershell
# JDK 21 (nativo ARM64 nesta máquina)
winget install --id Microsoft.OpenJDK.21

# Android SDK command-line tools -> %LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest
# https://developer.android.com/studio#command-line-tools-only

# Licenças + componentes
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Variáveis necessárias:

| Variável | Valor nesta máquina |
|---|---|
| `JAVA_HOME` | `C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot` |
| `ANDROID_HOME` | `C:\Users\kaleu\AppData\Local\Android\Sdk` |

E `local.properties` na raiz do projeto:

```properties
sdk.dir=C:/Users/kaleu/AppData/Local/Android/Sdk
```

### Só no Windows ARM64

`gradle.properties` aponta a JVM **de teste** para um JDK x64:

```properties
fastin.testJdkX64=C:/Users/kaleu/AppData/Local/JDKs/jdk-21.0.12.1+1
```

O Robolectric não publica binário nativo para `windows/aarch64` e uma JVM ARM64 não carrega
DLL x64 (ADR-007). Só os **testes** usam essa JVM — compilação, Kotlin, KSP, R8 e o
empacotamento do APK rodam em ARM64 nativo, e o APK entregue nunca passa por emulação.

**Numa máquina x64, apague essa linha** e tudo funciona igual.

---

## 8. Se algo der errado

| Sintoma | Causa |
|---|---|
| `JAVA_HOME is not set` | Terminal aberto antes da configuração. Feche e abra outro. |
| `Unexpected token 'verify'` | Comando Bash colado no PowerShell. Use a versão PowerShell: uma linha só, com `&` antes do caminho entre aspas. |
| `$LOCALAPPDATA` vazio | No PowerShell é `$env:LOCALAPPDATA`. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | APK assinado com chave diferente. Use a `fastin-release.jks` original, ou desinstale (perde os dados — exporte antes). |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | `versionCode` menor que o instalado. Suba o número. |
| `adb devices` mostra `unauthorized` | Aceite o diálogo de depuração USB no celular. |
| `adb` não encontra o aparelho | Cabo só de carga, ou falta driver. Teste outro cabo primeiro. |
| Build falha com `SDK location not found` | Falta `local.properties` (§7). |
| Testes falham com `UnsatisfiedLinkError` | Falta `fastin.testJdkX64` no `gradle.properties` (ADR-007). |
