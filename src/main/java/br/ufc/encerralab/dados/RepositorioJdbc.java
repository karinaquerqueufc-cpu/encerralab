package br.ufc.encerralab.dados;

import br.ufc.encerralab.dominio.AvaliacaoAluno;
import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Contestacao;
import br.ufc.encerralab.dominio.Contexto;
import br.ufc.encerralab.dominio.Criterio;
import br.ufc.encerralab.dominio.Nivel;
import br.ufc.encerralab.dominio.Risco;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositório PostgreSQL via JDBC puro. Requer o driver org.postgresql no classpath
 * (o pom.xml já o declara). Cria o esquema automaticamente na primeira execução.
 */
public final class RepositorioJdbc implements Repositorio {

    private final String url, usuario, senha;

    public RepositorioJdbc(String url, String usuario, String senha) {
        this.url = url; this.usuario = usuario; this.senha = senha;
        criarEsquema();
    }

    private Connection conectar() throws SQLException { return DriverManager.getConnection(url, usuario, senha); }

    private void criarEsquema() {
        try (InputStream in = getClass().getResourceAsStream("/db/schema.sql");
             Connection c = conectar(); Statement st = c.createStatement()) {
            if (in == null) throw new IllegalStateException("schema.sql não encontrado no classpath");
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String comando : sql.split(";\\s*\\n")) {
                String limpo = comando.lines().filter(l -> !l.strip().startsWith("--")).reduce("", (a, b) -> a + "\n" + b).strip();
                if (!limpo.isEmpty()) st.execute(limpo);
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO criterio(codigo, nome, descricao, fundamento) VALUES (?,?,?,?) ON CONFLICT (codigo) DO NOTHING")) {
                for (Criterio cr : Criterio.values()) {
                    ps.setString(1, cr.name()); ps.setString(2, cr.nome());
                    ps.setString(3, cr.descricao()); ps.setString(4, cr.fundamento());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Falha ao preparar o banco: " + e.getMessage(), e);
        }
    }

    // --------------------------------------------------------------- casos

    @Override public List<CasoEstudo> casos() {
        try (Connection c = conectar(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM caso_estudo ORDER BY id")) {
            List<CasoEstudo> lista = new ArrayList<>();
            while (rs.next()) lista.add(lerCaso(c, rs));
            return lista;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    @Override public Optional<CasoEstudo> caso(long id) {
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement("SELECT * FROM caso_estudo WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(lerCaso(c, rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    @Override public CasoEstudo salvarCaso(CasoEstudo caso) {
        String sql = """
                INSERT INTO caso_estudo(titulo, narrativa, dist_hidrica, dist_urbana, porte, catadores_atuando,
                                        rotulo_especialista, para_estudo, ficticio)
                VALUES (?,?,?,?,?,?,?,?,?) RETURNING id""";
        try (Connection c = conectar()) {
            c.setAutoCommit(false);
            long id;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, caso.titulo()); ps.setString(2, caso.narrativa());
                ps.setString(3, caso.contexto().distanciaHidrica().name());
                ps.setString(4, caso.contexto().distanciaUrbana().name());
                ps.setString(5, caso.contexto().porte().name());
                ps.setBoolean(6, caso.contexto().catadoresAtuando());
                ps.setString(7, caso.rotuloEspecialista() == null ? null : caso.rotuloEspecialista().name());
                ps.setBoolean(8, caso.paraEstudo()); ps.setBoolean(9, caso.ficticio());
                try (ResultSet rs = ps.executeQuery()) { rs.next(); id = rs.getLong(1); }
            }
            gravarNiveis(c, "INSERT INTO caso_criterio(caso_id, criterio, nivel) VALUES (?,?,?)", id, caso.plano());
            c.commit();
            return caso.comId(id);
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    private CasoEstudo lerCaso(Connection c, ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String rot = rs.getString("rotulo_especialista");
        Contexto ctx = new Contexto(
                Contexto.DistanciaHidrica.valueOf(rs.getString("dist_hidrica")),
                Contexto.DistanciaUrbana.valueOf(rs.getString("dist_urbana")),
                Contexto.Porte.valueOf(rs.getString("porte")),
                rs.getBoolean("catadores_atuando"));
        return new CasoEstudo(id, rs.getString("titulo"), rs.getString("narrativa"),
                lerNiveis(c, "SELECT criterio, nivel FROM caso_criterio WHERE caso_id = ?", id),
                ctx, rot == null ? null : Risco.valueOf(rot), rs.getBoolean("para_estudo"), rs.getBoolean("ficticio"));
    }

    // ---------------------------------------------------------- avaliações

    @Override public AvaliacaoAluno salvarAvaliacao(AvaliacaoAluno a) {
        try (Connection c = conectar()) {
            c.setAutoCommit(false);
            long id;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO avaliacao_aluno(caso_id, estudante, risco_estimado, justificativa, enviada_em) VALUES (?,?,?,?,?) RETURNING id")) {
                ps.setLong(1, a.casoId()); ps.setString(2, a.estudante()); ps.setString(3, a.riscoEstimado().name());
                ps.setString(4, a.justificativa()); ps.setTimestamp(5, Timestamp.valueOf(a.enviadaEm()));
                try (ResultSet rs = ps.executeQuery()) { rs.next(); id = rs.getLong(1); }
            }
            gravarNiveis(c, "INSERT INTO avaliacao_criterio(avaliacao_id, criterio, nivel) VALUES (?,?,?)", id, a.niveis());
            c.commit();
            return a.comId(id);
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    @Override public Optional<AvaliacaoAluno> avaliacao(long id) {
        return consultarAvaliacoes("SELECT * FROM avaliacao_aluno WHERE id = " + id).stream().findFirst();
    }

    @Override public List<AvaliacaoAluno> avaliacoes() {
        return consultarAvaliacoes("SELECT * FROM avaliacao_aluno ORDER BY id");
    }

    private List<AvaliacaoAluno> consultarAvaliacoes(String sql) {
        try (Connection c = conectar(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<AvaliacaoAluno> lista = new ArrayList<>();
            while (rs.next()) {
                long id = rs.getLong("id");
                lista.add(new AvaliacaoAluno(id, rs.getLong("caso_id"), rs.getString("estudante"),
                        lerNiveis(c, "SELECT criterio, nivel FROM avaliacao_criterio WHERE avaliacao_id = ?", id),
                        Risco.valueOf(rs.getString("risco_estimado")), rs.getString("justificativa"),
                        rs.getTimestamp("enviada_em").toLocalDateTime()));
            }
            return lista;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    // -------------------------------------------------------- contestações

    @Override public Contestacao salvarContestacao(Contestacao ct) {
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO contestacao(avaliacao_id, alvo, argumento, status, criada_em) VALUES (?,?,?,?,?) RETURNING id")) {
            ps.setLong(1, ct.avaliacaoId()); ps.setString(2, ct.alvo()); ps.setString(3, ct.argumento());
            ps.setString(4, ct.status().name()); ps.setTimestamp(5, Timestamp.valueOf(ct.criadaEm()));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return ct.comId(rs.getLong(1)); }
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    @Override public List<Contestacao> contestacoes() {
        try (Connection c = conectar(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM contestacao ORDER BY id")) {
            List<Contestacao> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(new Contestacao(rs.getLong("id"), rs.getLong("avaliacao_id"), rs.getString("alvo"),
                        rs.getString("argumento"), rs.getTimestamp("criada_em").toLocalDateTime(),
                        Contestacao.Status.valueOf(rs.getString("status")), rs.getString("resposta_docente")));
            }
            return lista;
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    @Override public void atualizarContestacao(Contestacao ct) {
        try (Connection c = conectar(); PreparedStatement ps = c.prepareStatement(
                "UPDATE contestacao SET status = ?, resposta_docente = ? WHERE id = ?")) {
            ps.setString(1, ct.status().name()); ps.setString(2, ct.respostaDocente()); ps.setLong(3, ct.id());
            ps.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    // ------------------------------------------------------------ apoio

    private static void gravarNiveis(Connection c, String sql, long id, Map<Criterio, Nivel> niveis) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Map.Entry<Criterio, Nivel> e : niveis.entrySet()) {
                ps.setLong(1, id); ps.setString(2, e.getKey().name()); ps.setInt(3, e.getValue().valor());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static Map<Criterio, Nivel> lerNiveis(Connection c, String sql, long id) throws SQLException {
        Map<Criterio, Nivel> m = new EnumMap<>(Criterio.class);
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) m.put(Criterio.valueOf(rs.getString(1)), Nivel.deValor(rs.getInt(2)));
            }
        }
        return m;
    }
}
