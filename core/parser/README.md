# core:parser

Motor de regras on-device que transforma notificações de apps financeiros em transações. O texto bruto da notificação nunca sai do aparelho (RNF-11).

## Como uma notificação é classificada

1. `TextoNormalizer` troca NBSP por espaço, remove emojis e colapsa espaços.
2. `DefaultParserEngine` percorre os padrões da instituição na ordem da regra. O primeiro padrão cujas âncoras casam e cujas exclusões não casam decide o resultado.
3. Padrão de ruído devolve `IGNORADA` (contado, sem lançamento). Padrão transacional devolve `TRANSACAO`. Sem padrão casando, o resultado é `NAO_ENTENDIDA`. Notificação sem título, texto nem big text é `DESCARTADA`.

Padrões transacionais excluem a palavra `negada` (compras recusadas não geram lançamento). Os padrões de ruído da instituição pegam essas negadas, então elas saem como `IGNORADA`.

## Regras v1

A versão 1 está em `src/main/resources/regras/regras-v1.json`, lida por `RegrasParserSeed`. Cada instituição tem seu próprio conjunto de padrões, e a seleção é sempre por instituição.

| Instituição | Padrões transacionais | Tipo |
| --- | --- | --- |
| Nubank | compra crédito aprovada (com últimos-4), compra débito aprovada | COMPRA |
| Nubank | transferência recebida: sem remetente, remetente comum e remetente com fragmento de CPF | TRANSFERENCIA_RECEBIDA |
| Inter | Pix recebido (NBSP, "conta final"), compra débito (título vazio), compra crédito (com últimos-4) | PIX_RECEBIDO, COMPRA |
| Inter | Pix Automático | PIX_ENVIADO |
| Itaú | compra aprovada em reais (com últimos-4 quando presentes), compra aprovada em USD (moeda `USD`) | COMPRA |
| Itaú | Pix enviado, Pix recebido, boleto pago de terceiro | PIX_ENVIADO, PIX_RECEBIDO, COMPRA |
| Mercado Pago | pagamento em conta ("Você pagou X" + "Debitamos R$ Y da sua conta"), pagamento de cartão com valor no título | COMPRA |
| Caju | compra aprovada (extrai também saldo e benefício do texto), depósito "Seu Caju caiu!" | COMPRA, DEPOSITO |

Cada instituição também tem padrões de ruído para marketing, lembretes e negadas. A lista completa está no próprio JSON.

Campos extraídos: valor (centavos), tipo, direção, estabelecimento, últimos-4 dígitos do cartão quando presentes, moeda (BRL por padrão), saldo e benefício (só Caju). Estabelecimento sem fragmento de CPF e sem pontuação final.

## Seed local e fallback

`RegraParserRepositoryImpl.observeRegrasAtivas()` devolve as regras do cache Room (`regras_parser`). Enquanto o cache estiver vazio, devolve `RegrasParserSeed.regras`, o seed embutido. Quando a feature de download (STR-179) gravar regras no cache, elas passam a valer no lugar do seed. O consumidor aplica `RegrasVigentes.selecionar` sobre essa lista.

## Resultado no corpus

O corpus de 212 entradas está em `src/test/resources/corpus/`, com a tabela de expectativas `expectativas.tsv`. O replay (`CorpusReplayTest`) confere as contagens por classe e por instituição e tipo, e valida o valor de cada transação contra o primeiro valor monetário do texto original.

| Instituição | Transações | Ignoradas (ruído + negadas) | Não entendidas |
| --- | --- | --- | --- |
| Itaú | 18 | 6 (2 ruído + 4 negadas) | 7 (faturas, F2) |
| Inter | 9 | 20 | 0 |
| Nubank | 11 | 31 (26 ruído + 5 negadas) | 1 (pagamento recebido ambíguo) |
| Mercado Pago | 23 | 26 | 2 (cashback e devolução de Pix) |
| Caju | 19 | 1 | 0 |
| Não bancárias | | | 7 (`NAO_BANCARIO`, sem contagem bancária) |

Totais: 80 transações, 84 ignoradas (75 ruído e 9 negadas), 31 descartadas (vazias) e 10 não entendidas.

## Lacunas de corpus (pendentes)

O corpus não tem amostras destes casos. Não há regra para eles, e o resultado é `NAO_ENTENDIDA`:

- Pix enviado da Inter (exceto Pix Automático, que tem uma amostra), do Nubank e do Mercado Pago. Zero amostras.
- Pix recebido do Mercado Pago. Zero amostras.
- Faturas: fatura fechada com valor e boleto de fatura emitido (7 entradas do Itaú). Fica para F2.
- Nubank "Recebemos seu pagamento no valor de R$ X": ambíguo entre fatura e transferência, sem regra automática.
- Mercado Pago devolução de Pix e cashback: fora da v1.
- Nubank compra aprovada: só 2 positivos no corpus. A regra vale, mas a confiança precisa de monitoramento.

## Fora do escopo desta versão

- Data e hora embutidas no texto (Itaú "no dia DD/MM às HH:MM", Inter Pix Automático "DD/MM/AAAA") não são extraídas. O timestamp da notificação é a fonte autoritativa.
- Compra em moeda estrangeira sai com `moeda = USD`. A marcação `PENDENTE MOEDA_ESTRANGEIRA` é do pós-processamento (STR-182).
- Itaú boleto pago entra como `COMPRA`, porque o enum não tem tipo próprio.
- Pós-processamento (dedup, categoria, confiança, transferência) fica no STR-182.
- Validação com aparelhos reais continua com o usuário. O corpus é a única evidência nesta versão.

## Testes

```bash
./gradlew :core:parser:testDebugUnitTest --console=plain
./gradlew :core:data:testDebugUnitTest --console=plain
```

Os testes do replay são `CorpusReplayTest` e `ExtracaoRegrasV1Test`. `RegrasParserSeedTest` cobre o seed e `RegraParserRepositoryImplTest` cobre o fallback.
