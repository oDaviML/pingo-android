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

## Design system

O `core:designsystem` traduz o design system do FigCraft (`design/design-system.md` e `design/pingo.figcraft`) para Compose, sem Material 3: tema próprio (`PingoTheme`) com tokens de cor, tipografia Nunito, raios, espaçamento, elevação e movimento, além dos componentes `Pingo*` e do stepper do onboarding.

## Autenticação (Auth0)

O login usa o SDK Android do Auth0 com a conexão Google e guarda os tokens cifrados com uma chave AES-256 do Android Keystore. A aplicação Auth0 precisa ser do tipo **Native App**, com a conexão Google habilitada e o callback `pingo://{dominio}/android/com.pingo/callback` permitido. Configure os valores fora do repositório, em `local.properties` (ou variáveis de ambiente do CI):

```properties
pingo.auth0.domain=seu-tenant.eu.auth0.com
pingo.auth0.clientId=seu-client-id
pingo.auth0.scheme=pingo
pingo.auth0.audience=https://api.pingo.app
pingo.api.baseUrl=http://10.0.2.2:8080/
```

O `app` injeta esses valores via `BuildConfig` e `manifestPlaceholders`; o `core:auth` lê o token do Keystore e o `core:network` o envia como `Authorization: Bearer` nas chamadas à API.

## Documentação

- [AGENTS.md](AGENTS.md) — contexto, arquitetura, comandos, boas práticas e fluxo de PRs
