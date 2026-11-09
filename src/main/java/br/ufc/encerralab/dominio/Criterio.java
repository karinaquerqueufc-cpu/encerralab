package br.ufc.encerralab.dominio;

/**
 * Componentes técnicos e socioambientais de um plano de encerramento de lixão.
 *
 * <p>Os pesos são valores PADRÃO de demonstração (todos iguais). Na versão de
 * pesquisa, os pesos devem ser definidos pela matriz de critérios da tese e
 * validados por especialistas (ex.: método AHP), podendo ser alterados pelo
 * docente sem recompilar o sistema (ver {@link br.ufc.encerralab.ia.AvaliadorMulticriterio}).</p>
 */
public enum Criterio {

    CESSACAO_DISPOSICAO("Cessação da disposição e destinação adequada",
            "Interrupção do lançamento de resíduos na área e envio para aterro sanitário licenciado.",
            "Lei 12.305/2010 (PNRS), art. 47 (proíbe o lançamento in natura a céu aberto)."),
    ISOLAMENTO_AREA("Isolamento e controle de acesso",
            "Cercamento, sinalização e vigilância para impedir novos lançamentos e o acesso de pessoas e animais.",
            "Boa prática de encerramento e remediação de áreas de disposição."),
    INCLUSAO_CATADORES("Inclusão socioprodutiva de catadores",
            "Cadastro das pessoas que trabalham no lixão e plano de inclusão (cooperativas, coleta seletiva, renda).",
            "Lei 12.305/2010 (PNRS): eliminação de lixões associada à inclusão social e emancipação econômica de catadores."),
    COBERTURA_FINAL("Conformação e cobertura dos resíduos",
            "Regularização da massa de resíduos e cobertura final com solo para reduzir infiltração e exposição.",
            "Boa prática de engenharia sanitária para recuperação de áreas degradadas por resíduos."),
    DRENAGEM_PLUVIAL("Drenagem de águas pluviais",
            "Sistema que desvia a água da chuva da massa de resíduos, reduzindo erosão e geração de lixiviado.",
            "ABNT NBR 13896:1997 (critérios de projeto, implantação e operação de aterros)."),
    LIXIVIADO("Coleta e tratamento de lixiviado (chorume)",
            "Drenagem, coleta e tratamento do líquido percolado para evitar contaminação do solo e da água.",
            "ABNT NBR 13896:1997; ABNT NBR 8419:1992."),
    GASES("Drenagem e controle de gases",
            "Drenos verticais e queima ou aproveitamento do biogás (metano), reduzindo risco de explosão e emissões.",
            "ABNT NBR 8419:1992 (apresentação de projetos de aterros sanitários)."),
    MONITORAMENTO_AGUAS("Monitoramento de águas superficiais e subterrâneas",
            "Poços e pontos de amostragem com análises periódicas para acompanhar a contaminação.",
            "ABNT NBR 13896:1997 (monitoramento das águas subterrâneas)."),
    RECUPERACAO_AREA("Recuperação da área degradada e uso futuro",
            "Plano de recuperação (revegetação, estabilidade geotécnica) e definição de uso futuro compatível.",
            "Boa prática de recuperação de áreas degradadas (PRAD)."),
    MONITORAMENTO_POS("Plano de monitoramento pós-encerramento",
            "Responsáveis, frequência, indicadores e orçamento para acompanhar a área após o encerramento.",
            "Boa prática de gestão pós-encerramento.");

    private final String nome;
    private final String descricao;
    private final String fundamento;

    Criterio(String nome, String descricao, String fundamento) {
        this.nome = nome;
        this.descricao = descricao;
        this.fundamento = fundamento;
    }

    public String nome() { return nome; }
    public String descricao() { return descricao; }
    public String fundamento() { return fundamento; }
}
