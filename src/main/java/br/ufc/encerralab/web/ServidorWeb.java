package br.ufc.encerralab.web;

import br.ufc.encerralab.Servico;
import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Contestacao;
import br.ufc.encerralab.dominio.Criterio;
import br.ufc.encerralab.dominio.Nivel;
import br.ufc.encerralab.dominio.Risco;
import br.ufc.encerralab.ia.AvaliadorMulticriterio;
import br.ufc.encerralab.ia.ComparadorAprendizagem;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;

/** Interface web do EncerraLab, construída apenas com o servidor HTTP do JDK. */
public final class ServidorWeb {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Servico servico;
    private final boolean demonstracao;
    private HttpServer server;

    public ServidorWeb(Servico servico, boolean demonstracao) {
        this.servico = servico;
        this.demonstracao = demonstracao;
    }

    public void iniciar(int porta) throws IOException {
        server = HttpServer.create(new InetSocketAddress(porta), 0);
        server.createContext("/", this::rotear);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    public void parar() { if (server != null) server.stop(0); }

    // ------------------------------------------------------------ roteamento

    private void rotear(HttpExchange ex) throws IOException {
        try {
            String caminho = ex.getRequestURI().getPath();
            String metodo = ex.getRequestMethod();
            Map<String, String> q = parametros(ex.getRequestURI().getRawQuery());
            switch (metodo + " " + caminho) {
                case "GET /" -> html(ex, 200, inicio());
                case "GET /caso" -> html(ex, 200, caso(Long.parseLong(q.get("id")), null));
                case "POST /avaliar" -> avaliar(ex, parametros(corpo(ex)));
                case "GET /resultado" -> html(ex, 200, resultado(Long.parseLong(q.get("id")), q.get("msg")));
                case "POST /contestar" -> contestar(ex, parametros(corpo(ex)));
                case "GET /docente" -> html(ex, 200, docente());
                case "POST /docente/decidir" -> decidir(ex, parametros(corpo(ex)));
                case "GET /transparencia" -> html(ex, 200, transparencia());
                default -> html(ex, 404, Html.pagina("Não encontrado", "<h1>Página não encontrada</h1>", demonstracao));
            }
        } catch (NoSuchElementException | NumberFormatException | NullPointerException e) {
            html(ex, 404, Html.pagina("Não encontrado", "<h1>Registro não encontrado</h1>", demonstracao));
        } catch (IllegalArgumentException e) {
            html(ex, 400, Html.pagina("Dados inválidos", "<h1>Dados inválidos</h1><p>" + Html.esc(e.getMessage())
                    + "</p><p><a href=\"javascript:history.back()\">Voltar</a></p>", demonstracao));
        } catch (Exception e) {
            html(ex, 500, Html.pagina("Erro", "<h1>Erro interno</h1><p>" + Html.esc(e.toString()) + "</p>", demonstracao));
        } finally {
            ex.close();
        }
    }

    // --------------------------------------------------------------- páginas

    private String inicio() {
        StringBuilder b = new StringBuilder("""
                <h1>Laboratório de avaliação de planos de encerramento de lixões</h1>
                <p>Escolha um caso, leia o plano de encerramento e dê o <b>seu</b> parecer critério a critério.
                Só depois você verá a análise de referência e a da inteligência artificial, com a explicação de cada
                resultado. Se discordar da IA, argumente: um docente vai analisar a sua contestação.</p>
                <h2>Casos disponíveis</h2>""");
        for (CasoEstudo c : servico.casosDeEstudo()) {
            String resumo = c.narrativa().length() > 230 ? c.narrativa().substring(0, 230) + "…" : c.narrativa();
            b.append("<div class=\"cartao\"><b>").append(Html.esc(c.titulo())).append("</b>")
             .append(c.ficticio() ? " <small>(fictício)</small>" : "")
             .append("<p>").append(Html.esc(resumo)).append("</p>")
             .append("<a href=\"/caso?id=").append(c.id()).append("\">Analisar este caso →</a></div>");
        }
        return Html.pagina("Início", b.toString(), demonstracao);
    }

    private String caso(long id, String erro) {
        CasoEstudo c = servico.caso(id);
        if (!c.paraEstudo()) throw new NoSuchElementException();
        StringBuilder b = new StringBuilder();
        b.append("<h1>").append(Html.esc(c.titulo())).append("</h1>");
        if (erro != null) b.append("<div class=\"cartao menos\">").append(Html.esc(erro)).append("</div>");
        b.append("<div class=\"cartao narrativa\">").append(Html.esc(c.narrativa())).append("</div>");
        b.append("<h2>Seu parecer</h2><form method=\"post\" action=\"/avaliar\">")
         .append("<input type=\"hidden\" name=\"caso\" value=\"").append(c.id()).append("\">")
         .append("<label>Código do estudante (use um código, não o seu nome)<input type=\"text\" name=\"estudante\" required maxlength=\"80\"></label>");
        for (Criterio cr : Criterio.values()) {
            b.append("<div class=\"crit\"><b>").append(Html.esc(cr.nome())).append("</b><small>")
             .append(Html.esc(cr.descricao())).append("</small><div class=\"opcoes\">");
            for (Nivel n : Nivel.values()) {
                b.append("<label><input type=\"radio\" required name=\"").append(cr.name()).append("\" value=\"")
                 .append(n.valor()).append("\"> ").append(n.rotulo()).append("</label>");
            }
            b.append("</div></div>");
        }
        b.append("<div class=\"crit\"><b>Risco ambiental residual após o plano</b><select name=\"risco\" required>")
         .append("<option value=\"\">Selecione…</option>");
        for (Risco r : Risco.values()) b.append("<option value=\"").append(r.name()).append("\">").append(r.rotulo()).append("</option>");
        b.append("</select></div>")
         .append("<label>Justificativa (o que pesou mais na sua análise?)<textarea name=\"justificativa\"></textarea></label>")
         .append("<button type=\"submit\">Enviar parecer e ver a análise</button></form>");
        return Html.pagina(c.titulo(), b.toString(), demonstracao);
    }

    private void avaliar(HttpExchange ex, Map<String, String> f) throws IOException {
        Map<Criterio, Nivel> niveis = new EnumMap<>(Criterio.class);
        for (Criterio c : Criterio.values()) {
            String v = f.get(c.name());
            if (v == null) throw new IllegalArgumentException("Avalie o critério: " + c.nome());
            niveis.put(c, Nivel.deValor(Integer.parseInt(v)));
        }
        String risco = f.get("risco");
        if (risco == null || risco.isBlank()) throw new IllegalArgumentException("Informe o risco estimado.");
        AvaliacaoAluno a = servico.registrarAvaliacao(Long.parseLong(f.get("caso")), f.get("estudante"),
                niveis, Risco.valueOf(risco), f.get("justificativa"));
        redirecionar(ex, "/resultado?id=" + a.id());
    }

    private String resultado(long avaliacaoId, String msg) {
        Servico.Resultado r = servico.resultado(avaliacaoId);
        ComparadorAprendizagem.Diagnostico d = r.diagnostico();
        StringBuilder b = new StringBuilder();
        b.append("<h1>Resultado: ").append(Html.esc(r.caso().titulo())).append("</h1>");
        if (msg != null) b.append("<div class=\"cartao ok\">").append(Html.esc(msg)).append("</div>");

        b.append("<div class=\"metricas\">")
         .append(metrica("Concordância com a referência", fmt(d.concordanciaCriterios()) + "%"))
         .append(metrica("Seu índice de conformidade", fmt(d.icpEstudante())))
         .append(metrica("Índice de referência", fmt(d.icpReferencia())))
         .append(metrica("Risco: você × referência", d.riscoEstudante().rotulo() + " × " + d.riscoReferencia().rotulo()))
         .append("</div>");

        b.append("<h2>Critério a critério</h2><table><tr><th>Critério</th><th>Seu parecer</th><th>Referência</th><th>Situação</th><th>Fundamento</th></tr>");
        for (ComparadorAprendizagem.Divergencia v : d.itens()) {
            String sit = switch (v.tipo()) {
                case CONCORDA -> "<span class=\"ok\">Concorda</span>";
                case SUPERESTIMOU -> "<span class=\"mais\">Mais otimista</span>";
                case SUBESTIMOU -> "<span class=\"menos\">Mais rigoroso(a)</span>";
            };
            b.append("<tr><td>").append(Html.esc(v.criterio().nome())).append("</td><td>").append(v.doEstudante().rotulo())
             .append("</td><td>").append(v.referencia().rotulo()).append("</td><td>").append(sit).append("</td><td><small>")
             .append(Html.esc(v.criterio().fundamento())).append("</small></td></tr>");
        }
        b.append("</table>");

        b.append("<h2>Perguntas para você refletir</h2><div class=\"cartao\"><ul>");
        r.orientacoes().forEach(o -> b.append("<li>").append(Html.esc(o)).append("</li>"));
        b.append("</ul><small>").append(Html.esc(servico.tutor().nome())).append("</small></div>");

        b.append("<h2>O que a inteligência artificial previu, e por quê</h2><div class=\"cartao\">")
         .append("<p>O modelo de aprendizado de máquina (árvore de decisão) estimou risco <b>")
         .append(r.previsaoModelo().classe().rotulo()).append("</b>, com base em ").append(r.previsaoModelo().suporte())
         .append(" casos semelhantes da base de treinamento (").append(fmt(r.previsaoModelo().confianca() * 100))
         .append("% deles nessa classe). Caminho de decisão:</p><div class=\"caminho\">");
        r.previsaoModelo().caminho().forEach(p -> b.append(Html.esc(p)).append("<br>"));
        b.append("ENTÃO risco = ").append(r.previsaoModelo().classe().rotulo()).append("</div>");
        if (r.referenciaDoEspecialista() && r.previsaoModelo().classe() != r.riscoReferencia()) {
            b.append("<p class=\"mais\">Atenção: a IA discordou da classificação do especialista (")
             .append(r.riscoReferencia().rotulo()).append("). Modelos erram. O que o modelo pode não estar considerando neste caso?</p>");
        }
        b.append("</div>");

        b.append("<h2>Índice de conformidade da referência (cálculo transparente)</h2><table><tr><th>Critério</th><th>Nível</th><th>Peso</th><th>Pontos obtidos</th><th>Pontos perdidos</th></tr>");
        for (AvaliadorMulticriterio.Parcela p : r.analiseReferencia().parcelas()) {
            b.append("<tr><td>").append(Html.esc(p.criterio().nome())).append("</td><td>").append(p.nivel().rotulo())
             .append("</td><td>").append(fmt(p.peso())).append("</td><td>").append(fmt(p.contribuicao()))
             .append("</td><td>").append(fmt(p.perdaMaxima())).append("</td></tr>");
        }
        b.append("<tr><th colspan=\"3\">Total (0 a 100)</th><th colspan=\"2\">").append(fmt(r.analiseReferencia().indice()))
         .append(" · ").append(Html.esc(r.analiseReferencia().faixa())).append("</th></tr></table>");

        b.append("<h2>Discorda da análise? Conteste.</h2><form class=\"cartao\" method=\"post\" action=\"/contestar\">")
         .append("<input type=\"hidden\" name=\"avaliacao\" value=\"").append(avaliacaoId).append("\">")
         .append("<label>O que você contesta?<select name=\"alvo\"><option value=\"RISCO\">Classificação de risco</option>");
        for (Criterio c : Criterio.values()) b.append("<option>").append(Html.esc(c.nome())).append("</option>");
        b.append("</select></label><label>Argumento (cite o trecho do caso e a norma ou a literatura)<textarea name=\"argumento\" required minlength=\"20\"></textarea></label>")
         .append("<button type=\"submit\">Registrar contestação</button></form>");
        return Html.pagina("Resultado", b.toString(), demonstracao);
    }

    private void contestar(HttpExchange ex, Map<String, String> f) throws IOException {
        long id = Long.parseLong(f.get("avaliacao"));
        servico.contestar(id, f.getOrDefault("alvo", "RISCO"), f.get("argumento"));
        redirecionar(ex, "/resultado?id=" + id + "&msg=" + java.net.URLEncoder.encode(
                "Contestação registrada. Um docente vai analisá-la.", StandardCharsets.UTF_8));
    }

    private String docente() {
        StringBuilder b = new StringBuilder("<h1>Painel docente</h1>");
        List<AvaliacaoAluno> avs = servico.avaliacoes();
        long pend = servico.contestacoes().stream().filter(c -> c.status() == Contestacao.Status.PENDENTE).count();
        b.append("<div class=\"metricas\">")
         .append(metrica("Pareceres enviados", String.valueOf(avs.size())))
         .append(metrica("Contestações pendentes", String.valueOf(pend)))
         .append(metrica("Acurácia do modelo (LOO)", fmt(servico.modelo().acuraciaLeaveOneOut() * 100) + "%"))
         .append(metrica("Casos de treinamento", String.valueOf(servico.modelo().baseTreino())))
         .append("</div>");

        b.append("<h2>Onde a turma mais diverge (do menor para o maior acerto)</h2><table><tr><th>Critério</th><th>Respostas</th><th>% concordância</th><th>Mais otimistas</th><th>Mais rigorosos</th></tr>");
        for (Servico.EstatisticaCriterio e : servico.estatisticasPorCriterio()) {
            b.append("<tr><td>").append(Html.esc(e.criterio().nome())).append("</td><td>").append(e.respostas())
             .append("</td><td>").append(e.respostas() == 0 ? "—" : fmt(e.pctConcordancia()) + "%")
             .append("</td><td>").append(e.superestimou()).append("</td><td>").append(e.subestimou()).append("</td></tr>");
        }
        b.append("</table>");

        b.append("<h2>Contestações</h2>");
        List<Contestacao> cs = servico.contestacoes();
        if (cs.isEmpty()) b.append("<p>Nenhuma contestação registrada.</p>");
        for (Contestacao c : cs) {
            b.append("<div class=\"cartao\"><b>#").append(c.id()).append(" · ").append(Html.esc(c.alvo()))
             .append("</b> · parecer <a href=\"/resultado?id=").append(c.avaliacaoId()).append("\">#").append(c.avaliacaoId())
             .append("</a> · ").append(c.criadaEm().format(DATA)).append(" · <b>").append(c.status()).append("</b>")
             .append("<p>").append(Html.esc(c.argumento())).append("</p>");
            if (c.status() == Contestacao.Status.PENDENTE) {
                b.append("<form method=\"post\" action=\"/docente/decidir\"><input type=\"hidden\" name=\"id\" value=\"")
                 .append(c.id()).append("\"><label>Resposta ao estudante<textarea name=\"resposta\" required></textarea></label>")
                 .append("<select name=\"status\"><option value=\"ACEITA\">Aceitar (a referência será revista)</option>")
                 .append("<option value=\"REJEITADA\">Rejeitar</option></select><button type=\"submit\">Registrar decisão</button></form>");
            } else {
                b.append("<p><i>Resposta docente:</i> ").append(Html.esc(c.respostaDocente())).append("</p>");
            }
            b.append("</div>");
        }

        b.append("<h2>Regras aprendidas pelo modelo (auditoria)</h2><div class=\"caminho\">");
        servico.modelo().regras().forEach(rg -> b.append(Html.esc(rg)).append("<br>"));
        b.append("</div>");
        return Html.pagina("Painel docente", b.toString(), demonstracao);
    }

    private void decidir(HttpExchange ex, Map<String, String> f) throws IOException {
        servico.decidirContestacao(Long.parseLong(f.get("id")), Contestacao.Status.valueOf(f.get("status")), f.get("resposta"));
        redirecionar(ex, "/docente");
    }

    private String transparencia() {
        StringBuilder b = new StringBuilder("""
                <h1>Como a IA do EncerraLab funciona</h1>
                <div class="cartao"><b>1. Índice de conformidade (regra explícita).</b> Cada critério recebe um nível
                (0, 1 ou 2) e um peso. O índice é a soma ponderada normalizada de 0 a 100. Todas as parcelas aparecem
                na tela de resultado e podem ser conferidas à mão.</div>
                <div class="cartao"><b>2. Modelo de aprendizado de máquina (árvore de decisão).</b> Aprende, a partir de
                casos já classificados por especialistas, a estimar o risco ambiental residual. Foi escolhido por ser
                interpretável: toda previsão mostra o caminho de regras que a gerou. A acurácia é medida por validação
                cruzada <i>leave-one-out</i> e exibida no painel docente.</div>
                <div class="cartao"><b>3. Tutor.</b> Devolve perguntas de reflexão, nunca a resposta pronta. Por padrão,
                funciona offline, com perguntas escritas pelos docentes. Um tutor generativo opcional pode ser ativado;
                nesse caso, o nome do estudante não é enviado a serviços externos.</div>
                <div class="cartao"><b>4. Supervisão humana.</b> A referência de cada caso é validada por docente. O
                estudante pode contestar qualquer resultado, e a decisão final é sempre humana.</div>
                <div class="cartao"><b>5. Limitações.</b> Nesta demonstração, os casos e os rótulos de treinamento são
                fictícios e foram gerados por uma regra ilustrativa. As métricas do modelo, portanto, não indicam o
                desempenho real, que depende de uma base rotulada por especialistas.</div>
                <h2>Pesos atuais dos critérios</h2><table><tr><th>Critério</th><th>Peso</th><th>Fundamento</th></tr>""");
        servico.avaliador().pesos().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> b.append("<tr><td>").append(Html.esc(e.getKey().nome())).append("</td><td>")
                        .append(fmt(e.getValue())).append("</td><td><small>").append(Html.esc(e.getKey().fundamento()))
                        .append("</small></td></tr>"));
        b.append("</table>");
        return Html.pagina("Transparência", b.toString(), demonstracao);
    }

    // --------------------------------------------------------------- apoio

    private static String metrica(String rotulo, String valor) {
        return "<div class=\"metrica\">" + Html.esc(rotulo) + "<b>" + Html.esc(valor) + "</b></div>";
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", v);
    }

    private static String corpo(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static Map<String, String> parametros(String bruto) {
        Map<String, String> m = new HashMap<>();
        if (bruto == null || bruto.isEmpty()) return m;
        for (String par : bruto.split("&")) {
            int i = par.indexOf('=');
            String k = URLDecoder.decode(i < 0 ? par : par.substring(0, i), StandardCharsets.UTF_8);
            String v = i < 0 ? "" : URLDecoder.decode(par.substring(i + 1), StandardCharsets.UTF_8);
            m.put(k, v);
        }
        return m;
    }

    private static void html(HttpExchange ex, int status, String conteudo) throws IOException {
        byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void redirecionar(HttpExchange ex, String destino) throws IOException {
        ex.getResponseHeaders().set("Location", destino);
        ex.sendResponseHeaders(303, -1);
    }
}
