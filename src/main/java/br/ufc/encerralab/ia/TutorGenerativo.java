package br.ufc.encerralab.ia;

import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tutor OPCIONAL baseado em modelo de linguagem (API da Anthropic). Só é ativado se a
 * variável de ambiente ENCERRALAB_API_KEY estiver definida.
 *
 * <p>Salvaguardas de projeto:</p>
 * <ul>
 *   <li>Minimização de dados (LGPD): o nome do estudante NUNCA é enviado; apenas o texto do
 *       caso (fictício ou anonimizado) e as divergências.</li>
 *   <li>Instrução de sistema que proíbe entregar a resposta e exige perguntas de reflexão.</li>
 *   <li>Em qualquer falha, o sistema volta ao {@link TutorReflexivoLocal}.</li>
 * </ul>
 */
public final class TutorGenerativo implements Tutor {

    private static final String URL = "https://api.anthropic.com/v1/messages";
    private static final String INSTRUCAO = """
            Você é um tutor de Engenharia Sanitária e Ambiental em uma universidade pública brasileira.
            O estudante avaliou um plano de encerramento de lixão e divergiu da referência validada pelo docente.
            Regras: (1) NÃO diga qual é o nível correto; (2) faça no máximo 3 perguntas curtas que levem o
            estudante a reler o caso e a legislação (Lei 12.305/2010; ABNT NBR 13896 e NBR 8419);
            (3) se a referência puder estar errada, diga isso e sugira que ele registre uma contestação;
            (4) responda em português, uma pergunta por linha, sem markdown.""";

    private final String chave;
    private final String modelo;
    private final Tutor reserva = new TutorReflexivoLocal();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public TutorGenerativo(String chave, String modelo) {
        this.chave = chave;
        this.modelo = modelo;
    }

    public static Tutor configurado() {
        String chave = System.getenv("ENCERRALAB_API_KEY");
        String modelo = System.getenv().getOrDefault("ENCERRALAB_MODELO", "claude-sonnet-5-5");
        return (chave == null || chave.isBlank()) ? new TutorReflexivoLocal() : new TutorGenerativo(chave, modelo);
    }

    @Override
    public String nome() { return "Tutor generativo (" + modelo + "), com reserva local"; }

    @Override
    public List<String> orientar(CasoEstudo caso, AvaliacaoAluno avaliacao, ComparadorAprendizagem.Diagnostico d) {
        if (d.divergencias().isEmpty() && d.riscoConcorda()) return reserva.orientar(caso, avaliacao, d);
        StringBuilder prompt = new StringBuilder("CASO:\n").append(caso.narrativa()).append("\n\nDIVERGÊNCIAS:\n");
        d.divergencias().forEach(v -> prompt.append("- ").append(v.criterio().nome())
                .append(": estudante = ").append(v.doEstudante().rotulo())
                .append("; referência = ").append(v.referencia().rotulo()).append('\n'));
        if (!d.riscoConcorda()) prompt.append("- Risco residual: estudante = ").append(d.riscoEstudante().rotulo())
                .append("; referência = ").append(d.riscoReferencia().rotulo()).append('\n');
        prompt.append("\nJUSTIFICATIVA DO ESTUDANTE:\n").append(avaliacao.justificativa());

        String corpo = "{\"model\":" + json(modelo) + ",\"max_tokens\":400,\"system\":" + json(INSTRUCAO)
                + ",\"messages\":[{\"role\":\"user\",\"content\":" + json(prompt.toString()) + "}]}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                    .timeout(Duration.ofSeconds(30))
                    .header("content-type", "application/json")
                    .header("x-api-key", chave)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return reserva.orientar(caso, avaliacao, d);
            String texto = extrairTexto(resp.body());
            List<String> linhas = new ArrayList<>();
            for (String l : texto.split("\\R")) if (!l.isBlank()) linhas.add(l.strip());
            return linhas.isEmpty() ? reserva.orientar(caso, avaliacao, d) : linhas;
        } catch (Exception e) {
            return reserva.orientar(caso, avaliacao, d);
        }
    }

    static String json(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }

    static String extrairTexto(String respostaJson) {
        Matcher m = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(respostaJson);
        StringBuilder sb = new StringBuilder();
        while (m.find()) sb.append(desescapar(m.group(1)));
        return sb.toString();
    }

    private static String desescapar(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) { b.append(c); continue; }
            char n = s.charAt(++i);
            switch (n) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'r' -> b.append('\r');
                case 'u' -> { b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; }
                default -> b.append(n);
            }
        }
        return b.toString();
    }
}
