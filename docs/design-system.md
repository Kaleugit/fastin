# Design System — fastin

Fonte: `design-ref/img-ref01.png` e `img-ref02.png`. Linguagem: **dark neumórfico**, superfícies
que parecem peça usinada, acento laranja incandescente com glow real. Princípios de disciplina
tirados do taste-skill (`high-end-visual-design`) e traduzidos para Compose.

Nenhum composable declara cor, raio, sombra ou tamanho de fonte literal. Tudo vem de
`ui/theme/`. Um `Color(0xFF...)` fora de `ui/theme/` é bug de review.

---

## 1. Cor

Dark-only (ADR-002). Luminância crescente = elevação crescente.

| Token | Hex | Uso |
|---|---|---|
| `surfaceBase` | `#14161A` | fundo da janela |
| `surfaceSunken` | `#101216` | poços: trilha de slider, campo de input |
| `surface` | `#1B1E23` | card padrão |
| `surfaceRaised` | `#22262C` | card sobre card, botão em repouso |
| `hairline` | `#FFFFFF` @ 6% | borda de 1dp, só no topo do card |
| `shadowDark` | `#07080A` @ 55% | sombra inferior-direita |
| `shadowLight` | `#FFFFFF` @ 4% | luz superior-esquerda |

**Acento** — nunca chapado, sempre gradiente com direção:

| Token | Hex | Uso |
|---|---|---|
| `accentCore` | `#FF8A00` | topo do gradiente (a "brasa") |
| `accent` | `#FF4D00` | cor sólida de referência |
| `accentDeep` | `#E02D00` | base do gradiente |
| `accentGlow` | `accent` @ 45% | halo desenhado **fora** da forma |

`accentGradient = Brush.linearGradient(listOf(accentCore, accentDeep))`, sempre no eixo
diagonal (topo-esquerda → base-direita), coerente com a luz do neumorfismo.

**Texto** — nunca branco puro:

| Token | Hex | Uso |
|---|---|---|
| `textPrimary` | `#E8EAED` | números grandes, valores |
| `textSecondary` | `#9AA0A8` | rótulos, dias do mês |
| `textMuted` | `#5A6069` | dias fora do mês, placeholder, unidades |
| `onAccent` | `#FFFFFF` | texto sobre laranja |

**Semântica de qualidade** (chips e heatmap) — derivada do acento, não cores de semáforo
genéricas: `GOOD/YES` = `accent` · `AVERAGE/MAYBE` = `#8A7A6B` (laranja dessaturado) ·
`BAD/NO` = `#4A4F57` (cinza frio). Verde/vermelho de UI genérica são proibidos: quebram
a paleta e o app não tem semântica de erro nesses campos.

---

## 2. Tipografia

**Roboto é proibido** — é o default do sistema e entrega o app como "template Android".
Duas famílias, empacotadas em `res/font/` (o app é offline; nada de downloadable fonts):

- **Outfit** — display. Geométrica, numerais circulares, bate com os números das refs.
  Usada em: relógio de jejum, big numbers, nome do mês, números do calendário.
- **Manrope** — UI. Rótulos, corpo, notas, botões.

**Numerais tabulares obrigatórios** onde o número muda sozinho (relógio, contadores):
`fontFeatureSettings = "tnum"`. Sem isso o relógio treme a cada segundo — é o detalhe que
separa "feito com cuidado" de "feito rápido".

| Estilo | Família | Tam. | Peso | Tracking |
|---|---|---|---|---|
| `clock` | Outfit | 40sp | 200 ExtraLight | -1.5sp |
| `displayLarge` | Outfit | 40sp | 300 Light | -1.5sp |
| `displayMedium` | Outfit | 28sp | 300 Light | -0.5sp |
| `title` | Outfit | 20sp | 400 Regular | 0 |
| `body` | Manrope | 15sp | 400 Regular | 0 |
| `label` | Manrope | 13sp | 500 Medium | 0.2sp |
| `eyebrow` | Manrope | 10sp | 600 SemiBold | **1.6sp**, CAIXA ALTA |

O `eyebrow` é a assinatura do sistema: todo card é precedido por um rótulo microscópico,
espaçadíssimo e em caixa alta (`DISTANCE (km)` nas refs). É o que dá o ar editorial.

Contraste: `textSecondary` sobre `surface` = 5.9:1, `textMuted` = 3.2:1 — o muted só é usado
em texto não-essencial (dias fora do mês), nunca em informação que precise ser lida.

---

## 3. Espaçamento e raio

Base 4dp. Escala: `xs 4 · sm 8 · md 12 · lg 16 · xl 24 · xxl 32 · huge 48`.

- Padding interno de card: `xl` (24dp). Nunca menos — é o que faz respirar.
- Gap entre cards: `lg` (16dp). Entre seções: `xxl` (32dp).
- Margem lateral da tela: `lg` (16dp).
- Alvo de toque mínimo **48dp**, mesmo quando o desenho parece menor.

**Raio — squircle, não círculo de canto.** `RoundedCornerShape` do Compose gera arco
circular, que num raio grande denuncia o canto. `FastinShapes.squircle(r)` implementa
superelipse (n≈4), que é a curva contínua do iOS/refs.

| Token | Raio | Uso |
|---|---|---|
| `card` | 28dp | card principal |
| `inner` | 20dp | conteúdo dentro de card (concêntrico: 28 − 8 de padding) |
| `chip` | 999dp (pill) | chips, botões |
| `control` | 16dp | campos de input |

**Raio concêntrico é regra**: raio interno = raio externo − padding. Card 28dp com padding
8dp ⇒ filho 20dp. Curvas paralelas; caso contrário o aninhamento parece acidente.

---

## 4. Sombra — a peça central

Compose não tem sombra dupla nem inset. A tentação é desenhar em canvas nativo com
`BlurMaskFilter` — é o que a maioria das libs de neumorfismo faz, e é errado aqui:
mask filter **não é acelerado por GPU**, então cada card vira repaint em software durante
o scroll.

A partir da **API 28**, `Modifier.shadow()` aceita `spotColor` e `ambientColor`. É uma
sombra real do render pipeline, blur de verdade, acelerada. É essa que usamos — tingida,
não a cinza do Material. `Modifier.neumorphic()` compõe:

```
1. shadow(elevation, shape, spotColor = shadowDark, ambientColor = shadowDark)  <- GPU
2. background(surface, shape)
3. hairline: borda 1dp com gradiente vertical white@8% -> transparent (topo -> ~40%)
```

O passo 3 é o que a maioria pula, e é exatamente o que faz a peça parecer iluminada por
cima em vez de um retângulo cinza. A luz **sempre** vem de cima; inverter em um componente
quebra a coerência da tela.

> **Degradação em API 26–27:** `spotColor` é ignorado e a sombra sai preta padrão. O app
> continua correto, só menos refinado. Não vale subir o `minSdk` por isso.

**Poço (`Modifier.sunken()`)** para inputs e trilhas: Compose não tem inset shadow. Simulamos
com fundo `surfaceSunken` + borda interna de 1dp em gradiente **invertido** (escuro no topo,
claro na base). A inversão da direção da luz é o que lê como "afundado".

**Glow do acento (`Modifier.accentGlow()`)**: `shadow()` com `spotColor = accentGlow` e
`elevation` alta. A sombra do elemento laranja é da cor do acento, não preta — é o que faz
o dia selecionado no calendário parecer aceso, como na `img-ref01`.

## 5. Card — arquitetura de casca dupla

Todo card significativo é **casca externa + núcleo**, não um retângulo só:

```
Box  .neumorphic(shape = squircle(28), surface)     <- casca
  Column padding(24)
    Text(eyebrow)  "JEJUM EM ANDAMENTO"             <- rótulo microscópico
    Spacer(16)
    Box .background(surfaceSunken, squircle(20))    <- núcleo, raio concêntrico
      conteúdo
```

Um cabeçalho com botões (setas do mês) fica na casca, separado do núcleo por um divisor de
1dp em `hairline` — exatamente o card de setembro da `img-ref01`.

---

## 6. Movimento

`ease-in-out` e `linear` são proibidos: entregam o movimento como interpolação de
computador. Curvas do sistema, em `FastinMotion`:

| Token | Curva | Duração | Uso |
|---|---|---|---|
| `standard` | `CubicBezier(0.32, 0.72, 0, 1)` | 400ms | transição de estado, troca de mês |
| `enter` | `CubicBezier(0.16, 1, 0.3, 1)` | 550ms | entrada de tela e de card |
| `press` | spring(stiffness Medium, damping 0.7) | — | escala 0.97 ao pressionar |

- Todo alvo tocável faz `scale(0.97)` ao pressionar. Sem ripple Material padrão — o feedback
  é a compressão da peça física.
- Cards da lista entram escalonados: `delay = index * 40ms`, fade + translação de 16dp.
- **O relógio de jejum não anima o número.** Ele muda a cada segundo; animar seria ruído.
  Anima só o preenchimento do anel de progresso, com `standard`.

---

## 7. Ícones

Material Icons de traço grosso são proibidos. Os poucos ícones necessários (seta esquerda,
seta direita, fechar, check, mais, reticências, relógio, gráfico, calendário e engrenagem)
são `ImageVector` desenhados à mão em `ui/theme/FastinIcons.kt`, com traço de **1.5dp**,
cap arredondado, sem preenchimento — o traço fino e preciso das refs.

A barra inferior usa calendário, gráfico e **engrenagem** (Ajustes). O relógio existe para
outros usos; um relógio na aba de ajustes lia como "horários", não como "configuração".

---

## 8. Checklist de review de UI

Uma tela só passa se:

- [ ] Nenhuma cor/raio/sombra/tamanho literal fora de `ui/theme/`
- [ ] Nenhum `BlurMaskFilter` / canvas nativo para sombra
- [ ] Sombra só via `.neumorphic()`/`.accentGlow()` — nunca `shadow()` cru com cor padrão
- [ ] Nenhum uso de Roboto ou de Material Icons de traço grosso
- [ ] Todo card usa `.neumorphic()` com hairline superior
- [ ] Raios internos são concêntricos com o externo
- [ ] Todo card tem `eyebrow` em caixa alta e tracking largo
- [ ] Padding interno de card >= 24dp
- [ ] Números que mudam sozinhos usam `tnum`
- [ ] Nenhuma transição `linear`/`ease-in-out`
- [ ] Alvos de toque >= 48dp
- [ ] Estado vazio desenhado — nunca tela em branco
