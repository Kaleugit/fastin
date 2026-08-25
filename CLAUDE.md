# CLAUDE.md — fastin

App Android **pessoal** de controle de jejum intermitente. Sideload via APK, sem Play Store,
sem backend, sem login, sem internet. 100% offline.

## Contexto obrigatório
- `docs/PROJECT.md` — fonte da verdade: escopo, modelo de dados, telas, critérios de aceitação.
- `docs/design-system.md` — tokens visuais. Nenhuma cor/sombra/raio hardcoded fora dele.
- `docs/architecture.md` — camadas e fronteiras.
- `docs/decisions.md` — decisões técnicas com trade-off registrado.
- `docs/build-apk.md` — como gerar e instalar o APK.
- `design-ref/` — referências visuais originais (dark neumórfico, acento laranja).

## Personas disponíveis
`architect`, `frontend`, `backend`, `devops`, `testing`, `review` — em `.claude/skills/`,
adaptadas de `~/dev/agentes-oda` para contexto Android/Compose.

## Stack
Kotlin + Jetpack Compose (Material 3 como base, tema custom neumórfico) · Room · Vico (gráficos)
· WorkManager (notificações locais) · DataStore (config do dashboard) · Gradle KTS + version catalog.

## Ambiente (verificado)
- Windows 11 **ARM64**. JDK: Microsoft OpenJDK 21 (`C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot`).
- Android SDK: `%LOCALAPPDATA%\Android\Sdk` (command-line tools, sem Android Studio).
- **Sem emulador e sem device garantido.** Testes rodam na JVM (Robolectric). Ver
  `.claude/skills/testing/SKILL.md` § Verificação de UI Android.

## Regras
- Simplicidade acima de tudo: app pessoal, não enterprise. KISS/YAGNI.
- Todo campo do registro diário é **opcional/nullable**. Nada de required disfarçado.
- Cálculo de jejum é **função pura com `Clock` injetado** — nunca `System.currentTimeMillis()`
  dentro da lógica, senão não dá para testar.
- Entrega em etapas. Cada etapa é mostrada ao humano antes de seguir.
- Comunicação com o usuário em **português (pt-BR)**. Código e identificadores em inglês.
