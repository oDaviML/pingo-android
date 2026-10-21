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

## Documentação

- [AGENTS.md](AGENTS.md) — contexto, arquitetura, comandos, boas práticas e fluxo de PRs
