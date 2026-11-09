package br.ufc.encerralab;

import br.ufc.encerralab.dados.DadosDemonstracao;
import br.ufc.encerralab.dados.Repositorio;
import br.ufc.encerralab.dados.RepositorioJdbc;
import br.ufc.encerralab.dados.RepositorioMemoria;
import br.ufc.encerralab.ia.AvaliadorMulticriterio;
import br.ufc.encerralab.ia.TutorGenerativo;
import br.ufc.encerralab.web.ServidorWeb;

/**
 * Ponto de entrada.
 *
 * <pre>
 * Demonstração (banco em memória, dados fictícios):
 *   java -jar encerralab.jar
 *
 * PostgreSQL:
 *   java -jar encerralab.jar --postgres jdbc:postgresql://localhost:5432/encerralab usuario senha
 *
 * Porta (padrão 8080):
 *   java -jar encerralab.jar --porta 9090
 * </pre>
 */
public final class EncerraLabApplication {

    public static void main(String[] args) throws Exception {
        int porta = 8080;
        Repositorio repo = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--porta" -> porta = Integer.parseInt(args[++i]);
                case "--postgres" -> repo = new RepositorioJdbc(args[++i], args[++i], args[++i]);
                default -> throw new IllegalArgumentException("Argumento desconhecido: " + args[i]);
            }
        }
        if (repo == null) repo = new RepositorioMemoria();
        boolean semCasos = repo.casos().isEmpty();
        if (semCasos) DadosDemonstracao.carregar(repo);
        boolean demonstracao = repo.casos().stream().anyMatch(c -> c.ficticio());

        Servico servico = new Servico(repo, new AvaliadorMulticriterio(), TutorGenerativo.configurado());
        new ServidorWeb(servico, demonstracao).iniciar(porta);

        System.out.printf("EncerraLab em execução: http://localhost:%d%n", porta);
        System.out.printf("Tutor: %s%n", servico.tutor().nome());
        System.out.printf("Modelo de risco: %d casos de treinamento, acurácia LOO = %.1f%%%n",
                servico.modelo().baseTreino(), servico.modelo().acuraciaLeaveOneOut() * 100);
        if (demonstracao) System.out.println("Atenção: dados fictícios, apenas para fins didáticos.");
    }
}
