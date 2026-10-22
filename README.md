# pingo-android

Aplicativo Android do Pingo: captura automática de transações financeiras a partir das notificações dos apps de banco, parser executado no aparelho e interface para acompanhamento de gastos.

## Como buildar

Requisitos: JDK 21 e Android SDK com a plataforma API 36.

```bash
./gradlew assembleDebug
./gradlew build
```

## Estrutura resumida

| Pasta | Papel |
| --- | --- |
| `app` | Aplicativo: `PingoApplication`, `MainActivity` e o NavHost |
| `core/*` | Fundamentos compartilhados: modelo, dados, banco, rede, parser, autenticação, notificações |
| `feature/*` | Uma feature por módulo, dividida em `api` (rotas) e `impl` (telas) |
| `sync` | Workers de sincronização (WorkManager) |
| `build-logic` | Convention plugins do Gradle (padrão Now in Android) |
| `config/detekt` | Configuração do detekt |

## Banco local

O banco fica em `core:database`: Room sobre SQLCipher, com as entidades do modelo, soft delete (`excluidoEm`) e marcadores de sincronização (`criadoEm`/`atualizadoEm`). A passphrase do SQLCipher é aleatória e protegida por uma chave AES-256 do Android Keystore. Se a chave não puder ser recuperada, o banco local é apagado e recriado; a restauração vem do servidor. O schema exportado fica em `core/database/schemas/` e é versionado junto com as migrations.

## Documentação

- [AGENTS.md](AGENTS.md) — contexto, arquitetura, comandos, boas práticas e fluxo de PRs
