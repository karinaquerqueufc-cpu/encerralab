package br.ufc.encerralab.ia;

import br.ufc.encerralab.dominio.CasoEstudo;
import br.ufc.encerralab.dominio.Contexto;
import br.ufc.encerralab.dominio.Criterio;
import br.ufc.encerralab.dominio.Nivel;

import java.util.LinkedHashMap;
import java.util.Map;

/** Converte um caso em atributos categóricos legíveis, usados pela árvore de decisão. */
public final class ExtratorAtributos {

    private ExtratorAtributos() {}

    public static Map<String, String> extrair(Map<Criterio, Nivel> plano, Contexto ctx) {
        Map<String, String> a = new LinkedHashMap<>();
        for (Criterio c : Criterio.values()) a.put(c.nome(), plano.get(c).rotulo());
        a.put("Distância de corpo hídrico", ctx.distanciaHidrica().rotulo());
        a.put("Distância de núcleo populacional", ctx.distanciaUrbana().rotulo());
        a.put("Porte do lixão", ctx.porte().rotulo());
        a.put("Catadores atuando na área", ctx.catadoresAtuando() ? "sim" : "não");
        return a;
    }

    public static Map<String, String> extrair(CasoEstudo caso) {
        return extrair(caso.plano(), caso.contexto());
    }
}
