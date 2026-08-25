"""Gera docs/screenshots/galeria.html com os PNGs embutidos como data URI."""
import base64
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent

SHOTS = [
    ("01-calendario", "Calendário", "Tela inicial",
     "O relógio de jejum fica fixo no topo. Só <b>hoje</b> ganha o disco laranja — dias com "
     "registro levam um ponto discreto sob o número."),
    ("06-relogio-vazio", "Relógio sem jejum", "Estado vazio",
     "Sem última refeição registrada não há o que contar. A tela diz o que fazer em vez de "
     "ficar em branco."),
    ("02-formulario", "Registro do dia", "Uso diário",
     "Os oito campos são opcionais. Tocar de novo no chip já marcado limpa o campo — sem isso "
     "um toque errado seria irreversível."),
    ("03-dashboard", "Dashboard", "Análise",
     "Cards configuráveis. Os gráficos são Canvas puro: nenhuma biblioteca faz heatmap e big "
     "number, então metade seria à mão de qualquer jeito."),
    ("04-editor-de-card", "Editor de card", "Análise",
     "Métrica, visualização, período e agregação. Escolher <i>streak</i> força o tipo para "
     "número grande — não existe série de streak por dia."),
    ("05-ajustes", "Ajustes", "Manutenção",
     "O CSV é o único backup: não há nuvem nem conta. O texto diz isso sem rodeio, porque "
     "trocar de aparelho sem exportar perde o histórico."),
]

CSS = """
  /* Tokens do próprio app (docs/design-system.md). Dark-only por ADR-002: mostrar estas
     telas sobre fundo claro representaria mal o produto. */
  :root {
    --base:      #14161A;
    --sunken:    #101216;
    --raised:    #22262C;
    --hairline:  rgba(255,255,255,.06);
    --accent:      #FF4D00;
    --accent-core: #FF8A00;
    --accent-glow: rgba(255,77,0,.35);
    --text:      #E8EAED;
    --text-2:    #9AA0A8;
    --text-3:    #5A6069;
    --display: "Outfit", "Segoe UI", system-ui, sans-serif;
    --ui: "Manrope", "Segoe UI", system-ui, sans-serif;
    --step: clamp(1rem, .6rem + 1.2vw, 1.5rem);
  }

  * { box-sizing: border-box; }

  body {
    margin: 0;
    background: var(--base);
    color: var(--text);
    font-family: var(--ui);
    line-height: 1.6;
    -webkit-font-smoothing: antialiased;
  }

  .wrap { max-width: 1180px; margin: 0 auto; padding: clamp(2.5rem, 6vw, 5rem) var(--step) 6rem; }

  header { display: flex; flex-direction: column; gap: 1.25rem; margin-bottom: clamp(3rem, 7vw, 5rem); }

  .kicker {
    font-size: .625rem; font-weight: 600; letter-spacing: .16em;
    text-transform: uppercase; color: var(--text-3);
  }

  h1 {
    font-family: var(--display); font-weight: 200;
    font-size: clamp(2.5rem, 7vw, 4.5rem); line-height: 1.02;
    letter-spacing: -.035em; margin: 0; text-wrap: balance;
  }
  h1 em {
    font-style: normal;
    background: linear-gradient(135deg, var(--accent-core), var(--accent));
    -webkit-background-clip: text; background-clip: text; color: transparent;
  }

  .lede { max-width: 62ch; color: var(--text-2); font-size: 1.0625rem; margin: 0; }
  .lede strong { color: var(--text); font-weight: 500; }

  .meta {
    display: flex; flex-wrap: wrap; gap: .5rem 1.75rem;
    padding-top: 1.25rem; border-top: 1px solid var(--hairline);
    font-size: .8125rem; color: var(--text-3);
  }
  .meta b { color: var(--text-2); font-weight: 500; font-variant-numeric: tabular-nums; }

  .grid {
    display: grid; gap: clamp(2rem, 4vw, 3.5rem);
    grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  }

  .shot { margin: 0; display: flex; flex-direction: column; gap: 1.125rem; }

  /* Moldura neumórfica: mesma sombra dupla + hairline superior do app. */
  .frame {
    all: unset; display: block; cursor: zoom-in;
    border-radius: 26px; padding: 8px;
    background: var(--raised);
    border-top: 1px solid rgba(255,255,255,.07);
    box-shadow: 0 18px 40px -12px rgba(7,8,10,.75), 0 2px 6px rgba(7,8,10,.5);
    transition: transform .45s cubic-bezier(.32,.72,0,1), box-shadow .45s cubic-bezier(.32,.72,0,1);
  }
  .frame:hover, .frame:focus-visible {
    transform: translateY(-4px);
    box-shadow: 0 26px 52px -12px rgba(7,8,10,.85), 0 0 0 1px var(--accent-glow);
  }
  .frame:focus-visible { outline: 2px solid var(--accent); outline-offset: 4px; }
  .frame img { display: block; width: 100%; height: auto; border-radius: 19px; }

  figcaption { display: flex; flex-direction: column; gap: .375rem; }
  .eyebrow {
    font-size: .5625rem; font-weight: 600; letter-spacing: .18em;
    text-transform: uppercase; color: var(--accent);
  }
  figcaption h2 {
    font-family: var(--display); font-weight: 300; font-size: 1.375rem;
    letter-spacing: -.01em; margin: 0; color: var(--text);
  }
  figcaption p { margin: 0; font-size: .875rem; color: var(--text-2); line-height: 1.55; }
  figcaption b { color: var(--text); font-weight: 600; }

  footer {
    margin-top: clamp(3.5rem, 8vw, 6rem); padding-top: 1.75rem;
    border-top: 1px solid var(--hairline);
    color: var(--text-3); font-size: .8125rem; max-width: 72ch;
  }
  footer b { color: var(--text-2); font-weight: 500; }
  footer code {
    font-family: ui-monospace, "Cascadia Code", Consolas, monospace;
    font-size: .8125em; color: var(--text-2);
    background: var(--sunken); padding: .15em .45em; border-radius: 5px;
  }

  /* Lightbox */
  #lb {
    position: fixed; inset: 0; z-index: 50; display: none;
    place-items: center; padding: 2rem 1rem;
    background: rgba(10,11,13,.94); cursor: zoom-out;
  }
  #lb.on { display: grid; }
  #lb img {
    max-width: min(420px, 92vw); max-height: 92vh; width: auto; height: auto;
    border-radius: 22px; box-shadow: 0 30px 70px -20px rgba(0,0,0,.9);
  }
  #lb .hint {
    position: fixed; bottom: 1.5rem; left: 0; right: 0; text-align: center;
    font-size: .75rem; letter-spacing: .1em; text-transform: uppercase; color: var(--text-3);
  }

  @media (prefers-reduced-motion: reduce) {
    * { transition-duration: .01ms !important; animation-duration: .01ms !important; }
  }
"""

JS = """
  (function () {
    var lb = document.getElementById('lb');
    var img = document.getElementById('lbimg');
    var last = null;

    document.querySelectorAll('.frame').forEach(function (btn) {
      btn.addEventListener('click', function () {
        last = btn;
        img.src = btn.dataset.full;
        img.alt = btn.querySelector('img').alt;
        lb.classList.add('on');
      });
    });

    function close() {
      lb.classList.remove('on');
      img.removeAttribute('src');
      if (last) { last.focus(); last = null; }
    }

    lb.addEventListener('click', close);
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && lb.classList.contains('on')) { close(); }
    });
  })();
"""


def build():
    cards = []
    for slug, name, group, note in SHOTS:
        raw = (ROOT / "docs" / "screenshots" / f"{slug}.png").read_bytes()
        uri = "data:image/png;base64," + base64.b64encode(raw).decode()
        cards.append(
            '      <figure class="shot">\n'
            f'        <button class="frame" type="button" data-full="{uri}" '
            f'aria-label="Ampliar {name}">\n'
            f'          <img src="{uri}" alt="Tela {name} do app fastin" loading="lazy" />\n'
            '        </button>\n'
            '        <figcaption>\n'
            f'          <span class="eyebrow">{group}</span>\n'
            f'          <h2>{name}</h2>\n'
            f'          <p>{note}</p>\n'
            '        </figcaption>\n'
            '      </figure>'
        )

    html = (
        "<title>Telas do fastin</title>\n"
        '<link rel="preconnect" href="https://fonts.googleapis.com">\n'
        '<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>\n'
        '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?'
        'family=Outfit:wght@200;300;400&family=Manrope:wght@400;500;600&display=swap">\n\n'
        "<style>" + CSS + "</style>\n\n"
        '<div class="wrap">\n'
        "  <header>\n"
        '    <span class="kicker">fastin · app pessoal de jejum intermitente</span>\n'
        "    <h1>Seis telas,<br>renderizadas <em>sem celular</em>.</h1>\n"
        '    <p class="lede">\n'
        "      Não existe emulador Android para Windows ARM64 — o Google não publica o pacote\n"
        "      para essa plataforma. Então cada tela é composta e desenhada de verdade na JVM,\n"
        "      com <strong>100 dias de histórico sintético</strong>, e salva em PNG. Também serve\n"
        "      de smoke test: uma tela que quebrasse ao medir ou desenhar falharia aqui, e nenhum\n"
        "      outro teste pegaria — os demais consultam a árvore de semântica, que existe mesmo\n"
        "      quando o desenho falha.\n"
        "    </p>\n"
        '    <div class="meta">\n'
        "      <span><b>90</b> testes · <b>0</b> falhas</span>\n"
        "      <span>APK assinado · <b>1,5 MB</b></span>\n"
        "      <span>Kotlin · Compose · Room</span>\n"
        "      <span>Sem rede, sem nuvem, sem conta</span>\n"
        "    </div>\n"
        "  </header>\n\n"
        '  <div class="grid">\n'
        + "\n".join(cards) + "\n"
        "  </div>\n\n"
        "  <footer>\n"
        "    Fontes <b>Outfit</b> e <b>Manrope</b> — as mesmas empacotadas no app. Roboto é\n"
        "    proibido pelo design system: é o default do Android e entrega o app como template.\n"
        "    Regenerar as imagens com <code>./gradlew.bat test --tests \"*ScreenshotTest*\"</code>.\n"
        "  </footer>\n"
        "</div>\n\n"
        '<div id="lb" role="dialog" aria-modal="true" aria-label="Tela ampliada">\n'
        '  <img id="lbimg" alt="" />\n'
        '  <span class="hint">clique ou Esc para fechar</span>\n'
        "</div>\n\n"
        "<script>" + JS + "</script>\n"
    )

    out = ROOT / "docs" / "screenshots" / "galeria.html"
    out.write_text(html, encoding="utf-8")
    print(f"gerado: {out}")
    print(f"tamanho: {out.stat().st_size / 1024 / 1024:.2f} MB")


if __name__ == "__main__":
    build()
