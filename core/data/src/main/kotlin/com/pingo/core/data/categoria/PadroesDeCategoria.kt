package com.pingo.core.data.categoria

import com.pingo.core.model.TipoCategoria

internal data class RegraPadraoCategoria(
    val categoriaNome: String,
    val tipo: TipoCategoria,
    val termosExatos: List<String> = emptyList(),
    val prefixosLongos: List<String> = emptyList(),
)

internal object PadroesDeCategoria {
    val regrasDespesa: List<RegraPadraoCategoria> = listOf(
        RegraPadraoCategoria(
            categoriaNome = "Assinaturas",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "amazon prime", "prime video", "apple com bill", "apple services", "google one",
                "google storage", "google youtube", "youtube premium", "microsoft 365", "office 365",
                "meli+", "meli mais", "smart fit", "xbox game pass", "playstation plus", "kindle unlimited",
                "netflix", "spotify", "youtube", "disney", "disney+", "star+", "hbo", "max",
                "globoplay", "paramount", "icloud", "chatgpt", "openai", "claude", "anthropic",
                "canva", "adobe", "dropbox", "onedrive", "psn", "xbox", "nintendo", "twitch",
                "crunchyroll", "deezer", "tidal", "audible", "smartfit", "bluefit", "bodytech",
                "totalpass", "wellhub", "gympass", "assinatura",
            ),
            prefixosLongos = listOf(
                "netflix", "spotify", "globoplay", "crunchyroll", "smartfit", "wellhub", "gympass",
                "totalpass", "chatgpt", "openai", "adobe", "canva", "disney",
            ),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Alimentação",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "uber eats", "ze delivery", "99 food", "99food", "mc donalds", "burger king",
                "china in box", "pizza hut", "cacau show", "bacio di latte", "coco bambu",
                "vivenda do camarao", "paris 6", "comida mineira", "ifood", "ifd", "rappi",
                "aiqfome", "restaurante", "lanchonete", "padaria", "panificadora", "confeitaria",
                "cafeteria", "cafe", "doceria", "sorveteria", "acai", "pizzaria", "pizza",
                "hamburgueria", "burger", "burguer", "churrascaria", "pastelaria", "pastel",
                "boteco", "bistro", "cantina", "sushi", "temakeria", "comida", "marmita",
                "marmitaria", "refeicao", "almoco", "jantar", "salgados", "outback", "madero",
                "mcdonalds", "bk", "bobs", "subway", "kfc", "popeyes", "habibs", "starbucks",
                "kopenhagen", "spoleto", "spoletto", "giraffas", "dominos",
            ),
            prefixosLongos = listOf(
                "ifood", "rappi", "aiqfome", "restaurante", "lanchonete", "padaria", "panificadora",
                "confeitaria", "cafeteria", "doceria", "sorveteria", "pizzaria", "hamburgueria",
                "churrascaria", "pastelaria", "temakeria", "marmitaria", "mcdonalds", "starbucks",
                "outback", "giraffas", "habibs", "subway", "dominos",
            ),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Compras",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "mercado livre", "mercado pago", "magazine luiza", "casas bahia", "ponto frio",
                "fast shop", "le biscuit", "epoca cosmeticos", "beleza na web", "tok stok",
                "amazon", "mercadolivre", "shopee", "shein", "aliexpress", "magalu", "pontofrio",
                "kabum", "pichau", "terabyte", "americanas", "submarino", "shoptime", "renner",
                "riachuelo", "c&a", "cea", "zara", "hering", "marisa", "pernambucanas", "centauro",
                "netshoes", "decathlon", "nike", "adidas", "puma", "tokstok", "etna", "petz",
                "cobasi", "boticario", "natura", "sephora", "vivara", "pandora",
                "bazar", "magazine", "eletronicos",
            ),
            prefixosLongos = listOf(
                "mercadolivre", "shopee", "shein", "aliexpress", "magalu", "casasbahia",
                "pontofrio", "americanas", "submarino", "kabum", "pichau", "renner", "riachuelo",
                "centauro", "netshoes", "decathlon", "boticario", "sephora", "vivara",
            ),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Mercado",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "pao de acucar", "sams club", "sam s club", "super nosso", "mart minas",
                "apoio mineiro", "supermercado dia", "tenda atacado", "oba hortifruti",
                "st marche", "natural da terra", "supermercado", "mercado", "hipermercado",
                "minimercado", "mercearia", "atacadao", "atacado", "atacarejo", "hortifruti",
                "sacolao", "acougue", "peixaria", "verdurao", "emporio", "armazem", "quitanda",
                "carrefour", "extra", "assai", "big", "nacional", "zaffari", "epa", "verdemar",
                "supernosso", "mateus", "guanabara", "mundial", "prezunic", "angeloni", "giassi",
                "bistek", "komprao", "koch", "condor", "festval", "muffato", "roldao", "akki",
                "swift", "oxxo", "hirota",
            ),
            prefixosLongos = listOf(
                "supermercado", "hipermercado", "minimercado", "mercearia", "atacadao",
                "atacarejo", "hortifruti", "sacolao", "acougue", "carrefour", "verdemar",
                "supernosso", "guanabara", "angeloni", "muffato", "zaffari",
            ),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Saúde",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "pague menos", "sao joao", "hermes pardini", "bradesco saude", "drogaria",
                "farmacia", "farma", "raia", "drogasil", "pacheco", "panvel", "araujo",
                "venancio", "nissei", "extrafarma", "ultrafarma", "onofre", "bifarma", "droga",
                "hospital", "clinica", "consultorio", "laboratorio", "fleury", "einstein", "sirio",
                "unimed", "amil", "sulamerica", "hapvida", "notredame", "odonto", "dentista",
                "ortodontia", "otica", "exame", "vacina", "medico", "saude", "petlove",
            ),
            prefixosLongos = listOf(
                "drogaria", "farmacia", "drogasil", "extrafarma", "ultrafarma", "paguemenos",
                "panvel", "venancio", "hospital", "clinica", "consultorio", "laboratorio",
                "unimed", "sulamerica", "hapvida", "dentista", "psicolog", "fisioterap",
            ),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Transporte",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "99 pop", "bilhete unico", "cartao otimo", "top sp", "sem parar", "br mania",
                "auto posto", "lava jato", "zona azul", "azul linhas", "gol linhas", "uber",
                "99", "99app", "99pop", "cabify", "indrive", "blablacar", "buser", "clickbus",
                "otimo", "riocard", "metro", "cptm", "sptrans", "semparar", "veloe", "conectcar",
                "taggy", "estapar", "estacionamento", "indigo", "pedagio", "rodovia", "ecovias",
                "ccr", "posto", "combustivel", "gasolina", "etanol", "ipiranga", "shell",
                "petrobras", "raizen", "ale", "autoposto", "borracharia", "lavajato", "oficina",
                "mecanica", "pneu", "localiza", "movida", "unidas", "turbi", "latam", "passagem",
            ),
            prefixosLongos = listOf(
                "99pop", "99app", "cabify", "indrive", "blablacar", "buser", "clickbus",
                "riocard", "semparar", "conectcar", "estapar", "estacionamento", "combustivel",
                "ipiranga", "petrobras", "autoposto", "borracharia", "lavajato", "localiza",
                "movida",
            ),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Contas da casa",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "tim celular", "conta de luz", "claro", "vivo", "tim", "oi", "algar", "brisanet",
                "sky", "net", "directv", "cemig", "enel", "copel", "celesc", "cpfl", "light",
                "equatorial", "energisa", "neoenergia", "coelba", "celpe", "cosern", "elektro",
                "sabesp", "copasa", "sanepar", "cedae", "cagece", "embasa", "caesb", "compesa",
                "comgas", "ceg", "naturgy", "ultragaz", "supergasbras", "liquigas", "consigaz",
                "energia", "saneamento", "internet", "fibra",
            ),
            prefixosLongos = listOf(
                "brisanet", "equatorial", "energisa", "neoenergia", "sabesp", "copasa", "sanepar",
                "ultragaz", "supergasbras", "liquigas", "consigaz",
            ),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Educação",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "pos graduacao", "cruzeiro do sul", "cultura inglesa", "sociedade mineira de cult",
                "faculdade", "universidade", "colegio", "escola", "creche", "curso", "mba",
                "puc", "usp", "unicamp", "mackenzie", "estacio", "unip", "anhanguera",
                "unicesumar", "descomplica", "alura", "rocketseat", "udemy", "coursera",
                "duolingo", "hotmart", "kiwify", "eduzz", "livraria", "saraiva", "leitura",
                "papelaria", "kalunga", "enem", "vestibular", "idioma", "wizard", "ccaa",
                "fisk", "yazigi",
            ),
            prefixosLongos = listOf(
                "faculdade", "universidade", "colegio", "mackenzie", "estacio", "anhanguera",
                "unicesumar", "descomplica", "rocketseat", "coursera", "duolingo", "hotmart",
                "livraria", "papelaria", "kalunga",
            ),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Moradia",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "quinto andar", "leroy merlin", "ferreira costa", "material de construcao",
                "aluguel", "imobiliaria", "quintoandar", "loft", "condominio", "iptu",
                "telhanorte", "c&c", "sodimac", "cassol", "madeireira", "marcenaria",
                "chaveiro", "dedetizacao", "lavanderia", "5asec",
            ),
            prefixosLongos = listOf(
                "imobiliaria", "quintoandar", "condominio", "telhanorte", "sodimac", "madeireira",
                "marcenaria", "chaveiro", "dedetizacao", "lavanderia", "5asec",
            ),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Lazer",
            tipo = TipoCategoria.DESPESA,
            termosExatos = listOf(
                "riot games", "epic games", "cinema", "cinemark", "uci", "cinepolis", "kinoplex",
                "ingresso", "sympla", "eventim", "ticketmaster", "teatro", "museu", "parque",
                "clube", "hotel", "pousada", "airbnb", "booking", "decolar", "123milhas", "hurb",
                "bar", "pub", "cervejaria", "balada", "show", "nuuvem", "steam", "boliche",
                "kart", "zoologico",
            ),
            prefixosLongos = listOf(
                "cinemark", "cinepolis", "kinoplex", "sympla", "eventim", "ticketmaster",
                "pousada", "airbnb", "booking", "decolar", "cervejaria", "zoologico",
            ),
        ),
    )

    val regrasReceita: List<RegraPadraoCategoria> = listOf(
        RegraPadraoCategoria(
            categoriaNome = "Reembolso",
            tipo = TipoCategoria.RECEITA,
            termosExatos = listOf(
                "reembolso",
                "estorno",
                "devolucao",
                "cashback",
                "meliuz",
                "ressarcimento",
                "chargeback",
            ),
            prefixosLongos = listOf("reembolso", "estorno", "devolucao", "cashback", "ressarcimento"),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Benefício",
            tipo = TipoCategoria.RECEITA,
            termosExatos = listOf(
                "vr beneficios", "ifood beneficios", "vale alimentacao", "vale refeicao",
                "recarga caju", "caju", "flash", "swile", "alelo", "sodexo", "pluxee", "ticket",
                "auxilio", "beneficio",
            ),
            prefixosLongos = listOf("beneficio", "sodexo", "pluxee", "alelo"),
        ),
        RegraPadraoCategoria(
            categoriaNome = "Salário",
            tipo = TipoCategoria.RECEITA,
            termosExatos = listOf(
                "pagamento mensal", "adiantamento salarial", "decimo terceiro", "salario",
                "folha", "proventos", "remuneracao", "13o", "ferias", "fgts",
            ),
            prefixosLongos = listOf("salario", "proventos", "remuneracao"),
        ),
    )
}
