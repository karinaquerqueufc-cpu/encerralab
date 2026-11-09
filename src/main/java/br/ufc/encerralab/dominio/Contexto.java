package br.ufc.encerralab.dominio;

/**
 * Características do sítio do lixão que influenciam o risco ambiental residual.
 * As faixas de distância usam como referência os parâmetros da ABNT NBR 13896:1997
 * para localização de aterros (200 m de corpos hídricos; 500 m de núcleos populacionais).
 */
public record Contexto(DistanciaHidrica distanciaHidrica,
                       DistanciaUrbana distanciaUrbana,
                       Porte porte,
                       boolean catadoresAtuando) {

    public enum DistanciaHidrica {
        MENOS_DE_200M("menos de 200 m de corpo hídrico"),
        DE_200_A_500M("entre 200 m e 500 m de corpo hídrico"),
        MAIS_DE_500M("mais de 500 m de corpo hídrico");
        private final String rotulo;
        DistanciaHidrica(String r) { rotulo = r; }
        public String rotulo() { return rotulo; }
    }

    public enum DistanciaUrbana {
        MENOS_DE_500M("menos de 500 m de núcleo populacional"),
        MAIS_DE_500M("mais de 500 m de núcleo populacional");
        private final String rotulo;
        DistanciaUrbana(String r) { rotulo = r; }
        public String rotulo() { return rotulo; }
    }

    public enum Porte {
        PEQUENO("pequeno porte"), MEDIO("médio porte"), GRANDE("grande porte");
        private final String rotulo;
        Porte(String r) { rotulo = r; }
        public String rotulo() { return rotulo; }
    }
}
