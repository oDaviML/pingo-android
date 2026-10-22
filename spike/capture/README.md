# spike:capture

App de spike do épico STR-157 (`STR-173` e `STR-174`). Grava localmente as notificações dos 5 apps financeiros para formar o corpus e validar os riscos de captura antes do parser de produção. Não faz parte do app Pingo e não deve ser publicado.

## Instalação

```bash
./gradlew :spike:capture:installDebug
```

Depois de instalar, abra o app e conceda acesso a notificações. Sem esse acesso a tela mostra o aviso e o botão para as configurações do Android.

## Pacotes monitorados

| App | Package |
| --- | --- |
| Nubank | `com.nu.production` |
| Inter | `br.com.intermedium` |
| Itaú | `br.com.itau.internet` |
| Mercado Pago | `com.mercadopago.wallet` |
| Caju | `com.caju.employeeApp` |

Notificação de qualquer outro app é ignorada. Se um pacote não bater no aparelho, ligue **Capturar todos os apps** para descobrir o identificador real e ajuste `KnownApps.kt`.

## Corpus

- Gravação em `files/corpus/corpus.tsv`, uma linha por notificação.
- Campos: horário de captura, horário da postagem, pacote, título, texto e big text. Os campos de texto são codificados em Base64 para preservar tabs e quebras de linha.
- **Exportar** gera um `.txt` legível em `cache/exports/pingo-corpus.txt` e abre o compartilhamento.
- **Limpar** apaga o arquivo após confirmação.
- O conteúdo permanece no aparelho; nada é enviado para a API.

## Coleta STR-174

Para cada combinação de aparelho (Android 15/16, Xiaomi/Samsung) e app, gere compra, Pix enviado e Pix recebido e registre:

- chegou ao spike? conteúdo completo ou oculto?
- tempo entre a postagem e a captura (`postedAt` vs `capturedAt`)?
- listener sobreviveu com tela bloqueada, em Doze e após reboot?
- qual ajuste foi necessário (bateria ilimitada, autostart, remover de Sleeping Apps)?
- a notificação do Caju traz saldo?

Exporte o corpus de cada aparelho e mantenha fora do repositório.
