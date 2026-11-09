# EncerraLab

**Laboratório de aprendizagem com inteligência artificial explicável para a avaliação de planos de encerramento de lixões.**

Protótipo funcional submetido à Categoria III (Soluções Tecnológicas) do Prêmio Inteligência Artificial e Formação Universitária, da Cátedra Luíza de Teodoro (UFC), Edital nº 1/2026.

> ⚠️ **Dados fictícios, apenas para fins didáticos.** Os municípios, lixões, planos e rótulos de risco desta demonstração foram inventados. Eles não representam avaliações técnicas reais.

**Versão web para testar no navegador ou no celular:** https://karinaquerqueufc-cpu.github.io/encerralab/
O arquivo está em `docs/index.html` e roda sem instalação.

## O que o sistema faz

1. **O estudante analisa primeiro.** Ele lê a narrativa de um plano de encerramento e avalia 10 critérios técnicos e socioambientais (0 = não atende, 1 = atende parcialmente, 2 = atende), além do risco ambiental residual.
2. **A IA analisa depois e se explica:**
   - **Índice de Conformidade do Plano (ICP)**, uma soma ponderada de 0 a 100 com todas as parcelas visíveis;
   - **árvore de decisão** (aprendizado de máquina, implementada do zero), que estima o risco e mostra o caminho de regras usado.
3. **Diagnóstico de aprendizagem.** O sistema compara o parecer do estudante com a referência validada pelo docente, critério a critério.
4. **Tutor reflexivo.** Em vez de dar a resposta, devolve perguntas sobre cada divergência.
5. **Contestação.** O estudante pode discordar da IA, com argumentos. O docente decide (supervisão humana).
6. **Painel docente.** Mostra em quais critérios a turma mais erra, as contestações pendentes, a acurácia do modelo e as regras aprendidas.

## Como executar

Requisito: **JDK 21**. Não há dependências externas no modo demonstração.

```bash
# Linux/macOS
./executar.sh            # compila, roda os 18 testes e abre em http://localhost:8080

# Windows
executar.bat
```

Com Maven e PostgreSQL:

```bash
mvn package
createdb encerralab
java -jar target/encerralab-jar-with-dependencies.jar \
     --postgres jdbc:postgresql://localhost:5432/encerralab USUARIO SENHA
```

O esquema (`src/main/resources/db/schema.sql`) é criado automaticamente na primeira execução. Ele inclui as visões analíticas `vw_concordancia_por_criterio` e `vw_desempenho_estudante`.

Tutor generativo (opcional): defina `ENCERRALAB_API_KEY` e, se quiser, `ENCERRALAB_MODELO`. Sem essas variáveis, o sistema usa o tutor local, que funciona offline e não envia dados a terceiros. O nome do estudante nunca é enviado a serviços externos.

## Arquitetura

```
dominio/   Criterio, Nivel, Contexto, Risco, CasoEstudo, AvaliacaoAluno, Contestacao
ia/        AvaliadorMulticriterio (ICP), ArvoreDecisao (Gini, CART/ID3), ModeloRisco,
           ComparadorAprendizagem, Tutor, TutorReflexivoLocal, TutorGenerativo
dados/     Repositorio (porta), RepositorioMemoria, RepositorioJdbc (PostgreSQL), DadosDemonstracao
web/       ServidorWeb (com.sun.net.httpserver), Html
Servico    casos de uso, independentes da interface
```

Decisões de projeto:
- **Modelo interpretável** (árvore de decisão) em vez de caixa-preta: toda previsão pode ser auditada e contestada.
- **Casos de estudo fora da base de treino:** a IA é testada neles do mesmo modo que o estudante.
- **Acurácia por validação cruzada leave-one-out**, exibida no painel. Na demonstração ela fica em torno de 75%, mas esse valor reflete apenas a regra ilustrativa usada para rotular os dados sintéticos.
- **Pesos dos critérios configuráveis**, a serem definidos pela matriz de critérios da pesquisa (por exemplo, pelo método AHP).

## Testes

`src/test/java/br/ufc/encerralab/TestesEncerraLab.java` traz 18 testes automatizados (cálculo do ICP, treinamento e explicação da árvore, separação entre treino e estudo, fluxo completo de parecer, diagnóstico, contestação e painel). Eles rodam só com o JDK.
