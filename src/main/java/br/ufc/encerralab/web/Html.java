package br.ufc.encerralab.web;

/** Utilitários de HTML: escape e leiaute comum das páginas. */
final class Html {

    private Html() {}

    static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '&' -> b.append("&amp;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                default -> b.append(c);
            }
        }
        return b.toString();
    }

    static String pagina(String titulo, String corpo, boolean modoDemonstracao) {
        String aviso = modoDemonstracao
                ? "<div class=\"aviso\">Dados fictícios, apenas para fins didáticos. Municípios, lixões e planos "
                  + "desta demonstração foram inventados.</div>"
                : "";
        return """
                <!DOCTYPE html>
                <html lang="pt-BR"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s · EncerraLab</title>
                <style>
                  :root{--verde:#2f6f4f;--verde-claro:#e8f2ec;--tinta:#1f2a24;--cinza:#5d6b63;--borda:#d5ddd8;--alerta:#fff4d6;--ruim:#b3412e;--bom:#2f6f4f;--meio:#a8741a}
                  *{box-sizing:border-box} body{margin:0;font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif;color:var(--tinta);background:#fafcfb;line-height:1.55}
                  header{background:var(--verde);color:#fff;padding:14px 24px;display:flex;gap:24px;align-items:center;flex-wrap:wrap}
                  header a{color:#fff;text-decoration:none;opacity:.9} header a:hover{opacity:1;text-decoration:underline}
                  header strong{font-size:1.15rem;margin-right:auto}
                  main{max-width:980px;margin:0 auto;padding:24px}
                  .aviso{background:var(--alerta);border-bottom:1px solid #e9d9a6;padding:8px 24px;font-size:.9rem;text-align:center}
                  h1{font-size:1.6rem;margin:.2em 0 .6em} h2{font-size:1.2rem;margin-top:1.8em;border-bottom:2px solid var(--verde-claro);padding-bottom:4px}
                  .cartao{background:#fff;border:1px solid var(--borda);border-radius:10px;padding:16px 20px;margin:12px 0}
                  .narrativa{white-space:pre-line}
                  table{border-collapse:collapse;width:100%%;background:#fff} th,td{border:1px solid var(--borda);padding:7px 10px;text-align:left;vertical-align:top;font-size:.94rem}
                  th{background:var(--verde-claro)}
                  .crit{margin:10px 0;padding:10px 12px;border:1px solid var(--borda);border-radius:8px;background:#fff}
                  .crit small{color:var(--cinza);display:block}
                  .opcoes label{margin-right:16px;white-space:nowrap}
                  input[type=text],textarea,select{width:100%%;padding:8px;border:1px solid var(--borda);border-radius:6px;font:inherit}
                  textarea{min-height:90px}
                  button{background:var(--verde);color:#fff;border:0;border-radius:6px;padding:10px 18px;font:inherit;cursor:pointer;margin-top:10px}
                  .ok{color:var(--bom);font-weight:600} .mais{color:var(--meio);font-weight:600} .menos{color:var(--ruim);font-weight:600}
                  .metricas{display:flex;gap:12px;flex-wrap:wrap} .metrica{flex:1;min-width:180px;background:#fff;border:1px solid var(--borda);border-radius:10px;padding:12px 16px}
                  .metrica b{display:block;font-size:1.6rem;color:var(--verde)}
                  .caminho{font-family:ui-monospace,Consolas,monospace;font-size:.86rem;background:#f3f6f4;padding:8px 12px;border-radius:6px}
                  footer{color:var(--cinza);font-size:.85rem;text-align:center;padding:24px}
                </style></head>
                <body>
                <header><strong>EncerraLab</strong>
                  <a href="/">Casos</a><a href="/docente">Painel docente</a><a href="/transparencia">Como a IA funciona</a></header>
                %s
                <main>%s</main>
                <footer>EncerraLab · protótipo acadêmico · laboratório de aprendizagem com IA explicável</footer>
                </body></html>
                """.formatted(esc(titulo), aviso, corpo);
    }
}
