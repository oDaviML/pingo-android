# pingo-android

Aplicativo Android do Pingo: captura automática de transações financeiras a partir das notificações dos apps de banco, parser executado no aparelho e interface para acompanhamento de gastos.

## Estado atual (F1)

O F1 entrega o fluxo completo de captura automática e as telas principais:

- Onboarding passos 1–7: login Google via Credential Manager, termo LGPD, acesso às notificações, permissão para o Pingo notificar, bateria/autostart e escolha de bancos/instrumentos. Biometria fica para F3.
- Início: saldo do mês (receitas − despesas, transferências excluídas), seletor de mês, avisos de "N para revisar" e "captura parada", top 3 categorias de despesa, últimos 5 lançamentos e atalho para lançamento manual.
- Lançamentos: lista por dia, busca, filtros, detalhe/edição e novo lançamento.
- Revisão de pendências, notificações por pendente (Confirmar sem abrir o app e Corrigir abrindo o detalhe) e Status da captura com permissões, contadores e reconexão.
- Contas e Relatórios são placeholders nesta fase.

## Como buildar

Requisitos: JDK 21 e Android SDK com a plataforma API 36.

```bash
./gradlew assembleDebug
./gradlew build
```

O `build` roda compilação, testes unitários, detekt e ktlint (Spotless).

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

## Autenticação (Auth0 + Google nativo)

O login usa o **Credential Manager** do Android (seletor nativo de contas Google) e troca o ID token do Google por tokens do Auth0 com `loginWithNativeSocialToken`. Os tokens ficam cifrados com uma chave AES-256 do Android Keystore. Não há navegador nem callback URL no fluxo.

Pré-requisitos:
- **Google Cloud**: OAuth client **Android** (package `com.pingo` + SHA-1 do keystore) e OAuth client **Web**.
- **Auth0**: conexão Google configurada com o **Web client ID/secret**; na Native App, *Advanced Settings → Device Settings*, habilitar **Enable Sign in with Google (Android 4.4+) using Credentials Manager** e definir o **App Package Name** `com.pingo`. Na API (`https://api.pingo.app/`), habilitar **Allow Offline Access** para receber refresh token.

Configure os valores fora do repositório, em `local.properties` (ou variáveis de ambiente do CI):

```properties
pingo.auth0.domain=seu-tenant.eu.auth0.com
pingo.auth0.clientId=seu-client-id
pingo.auth0.scheme=pingo
pingo.auth0.audience=https://api.pingo.app/
pingo.auth0.googleWebClientId=seu-web-client-id.apps.googleusercontent.com
pingo.api.baseUrl=http://10.0.2.2:8080/
```

O `app` injeta esses valores via `BuildConfig` e `manifestPlaceholders`; o `core:auth` lê o token do Keystore e o `core:network` o envia como `Authorization: Bearer` nas chamadas à API.

Em builds **debug**, o `network_security_config` permite cleartext apenas para `10.0.2.2` e `localhost` (emulador); os demais builds exigem HTTPS.

## Como rodar

Com um dispositivo ou emulador conectado:

```bash
./gradlew installDebug
```

Depois da primeira instalação, habilite em Ajustes do Android:

- **Acesso às notificações**: Apps → Acesso especial → Acesso a notificações → Pingo.
- **Notificações do Pingo**: permitir notificações do app.
- **Bateria**: desativar a otimização para o Pingo (o passo 6 do onboarding orienta por fabricante).

O `PingoApplication` inicia o listener e agenda a sincronização. Em builds debug, o `SeamInstituicaoResolver` aceita notificações sintéticas postadas pelo shell com a tag `pingo-teste-<INSTITUICAO>` e mapeia para a instituição correspondente; builds de release usam apenas o pacote real do app de banco.

## Testes

```bash
./gradlew test                 # unitários de todos os módulos
./gradlew connectedAndroidTest # instrumentados (device/emulador conectado)
```

Os testes instrumentados de UI usam `createAndroidComposeRule` com Room em memória nas features e, nos fluxos entre telas, o banco real do app via entry points de teste. As suítes de captura usam a seam de debug (`pingo-teste-<INSTITUICAO>`) para postar notificações sintéticas e comprovar o caminho real do listener.

Os testes de sync/HTTP precisam do `Pingo.Api.TestHost`, que escuta apenas em loopback e usa uma fixture de autenticação exclusiva de testes:

```bash
# em ../pingo-api, com o Postgres do compose de pé
ASPNETCORE_URLS=http://127.0.0.1:8080 \
ConnectionStrings__Postgres="Host=localhost;Port=5432;Database=pingo_testhost;Username=pingo;Password=pingo" \
dotnet run --project tests/Pingo.Api.TestHost/Pingo.Api.TestHost.csproj --no-launch-profile
```

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.pingo.syncHost=http://10.0.2.2:8080/
```

O TestHost não vai para produção e não contém segredos: a autenticação de teste é uma fixture do assembly de testes. Credenciais reais do Auth0 continuam fora do repositório, em `local.properties` ou variáveis de ambiente do CI.

## Documentação

- [AGENTS.md](AGENTS.md) — contexto, arquitetura, comandos, boas práticas e fluxo de PRs
