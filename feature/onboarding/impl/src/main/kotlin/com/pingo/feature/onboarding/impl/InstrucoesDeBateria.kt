package com.pingo.feature.onboarding.impl

data class InstrucoesDeBateria(val titulo: String, val passos: List<String>)

object InstrucoesPorFabricante {
    fun de(fabricante: FabricanteDoSistema): InstrucoesDeBateria = when (fabricante) {
        FabricanteDoSistema.SAMSUNG -> InstrucoesDeBateria(
            titulo = "Samsung · One UI",
            passos = listOf(
                "Ajustes › Bateria › Limites de uso em segundo plano.",
                "Adicione o Pingo em Apps nunca suspensos.",
                "Em Apps › Pingo, deixe a bateria como Sem restrições.",
            ),
        )

        FabricanteDoSistema.XIAOMI -> InstrucoesDeBateria(
            titulo = "Xiaomi · MIUI e HyperOS",
            passos = listOf(
                "Ajustes › Apps › Gerenciar apps › Pingo.",
                "Ative Início automático.",
                "Em Economia de bateria, escolha Sem restrições.",
            ),
        )

        FabricanteDoSistema.MOTOROLA -> InstrucoesDeBateria(
            titulo = "Motorola",
            passos = listOf(
                "Ajustes › Bateria › Otimização de bateria.",
                "Encontre o Pingo e escolha Não otimizar.",
                "Em Apps › Pingo › Bateria, permita atividade em segundo plano.",
            ),
        )

        FabricanteDoSistema.OUTRO -> InstrucoesDeBateria(
            titulo = "Instruções gerais",
            passos = listOf(
                "Ajustes › Bateria › Otimização de bateria.",
                "Encontre o Pingo e deixe a bateria sem restrições.",
                "Se o aparelho tiver início automático, permita o Pingo iniciar sozinho.",
            ),
        )
    }
}
