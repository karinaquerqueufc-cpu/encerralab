package br.ufc.encerralab.dominio;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Caso de plano de encerramento de lixão.
 *
 * <ul>
 *   <li>{@code paraEstudo = true}: caso apresentado ao estudante como narrativa; os níveis
 *       de referência ficam ocultos até ele enviar a sua avaliação.</li>
 *   <li>{@code rotuloEspecialista}: classe de risco atribuída por especialista; os casos
 *       rotulados formam a base de treinamento do modelo de aprendizado de máquina.</li>
 *   <li>{@code ficticio}: indica dados fictícios, usados apenas para fins didáticos.</li>
 * </ul>
 */
public final class CasoEstudo {

    private final long id;
    private final String titulo;
    private final String narrativa;
    private final Map<Criterio, Nivel> plano;
    private final Contexto contexto;
    private final Risco rotuloEspecialista;
    private final boolean paraEstudo;
    private final boolean ficticio;

    public CasoEstudo(long id, String titulo, String narrativa, Map<Criterio, Nivel> plano,
                      Contexto contexto, Risco rotuloEspecialista, boolean paraEstudo, boolean ficticio) {
        if (plano.size() != Criterio.values().length) {
            throw new IllegalArgumentException("O plano deve informar o nível de todos os " + Criterio.values().length + " critérios.");
        }
        this.id = id;
        this.titulo = titulo;
        this.narrativa = narrativa;
        this.plano = Collections.unmodifiableMap(new EnumMap<>(plano));
        this.contexto = contexto;
        this.rotuloEspecialista = rotuloEspecialista;
        this.paraEstudo = paraEstudo;
        this.ficticio = ficticio;
    }

    public long id() { return id; }
    public String titulo() { return titulo; }
    public String narrativa() { return narrativa; }
    public Map<Criterio, Nivel> plano() { return plano; }
    public Nivel nivel(Criterio c) { return plano.get(c); }
    public Contexto contexto() { return contexto; }
    public Risco rotuloEspecialista() { return rotuloEspecialista; }
    public boolean paraEstudo() { return paraEstudo; }
    public boolean ficticio() { return ficticio; }

    public CasoEstudo comId(long novoId) {
        return new CasoEstudo(novoId, titulo, narrativa, plano, contexto, rotuloEspecialista, paraEstudo, ficticio);
    }
}
