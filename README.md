# pingo-android

Aplicativo Android do Pingo: captura automática de transações financeiras a partir das notificações dos apps de banco, parser executado no aparelho e interface para acompanhamento de gastos.

## Estado atual (F2)

O F1 entregou o fluxo completo de captura automática e as telas principais. O F2 concluiu a organização de contas, faturas, vouchers, relatórios, assinaturas, lembretes e o alinhamento ao Design System:

- Onboarding passos 1–7: login Google via Credential Manager, termo LGPD, acesso às notificações, permissão para o Pingo notificar, bateria/autostart e escolha de bancos/instrumentos. Biometria fica para F3.
- Contas e Faturas (`STR-197`, `STR-198`): lista agrupada por instituição com subcards por instrumento (fatura aberta/fechamento/vencimento no crédito, gastos do mês no débito e saldo/última recarga no voucher — sem exibir saldo de conta corrente), modal `Completar datas` para cartões legados, `FaturasScreen` com abas `Aberta`, `Próximas` e `Fechadas` (`X/N` e estornos abatendo o total com clamp `>= 0`), além de arquivamento/reativação em cascata preservando histórico.
- Cadastro compartilhado entre onboarding e Contas (`STR-181`): instituição, apelido, tipo de conta, crédito (fechamento/vencimento e últimos quatro dígitos opcionais), débito e voucher (benefício, saldo inicial e recarga opcional). Outra instituição fica restrita a lançamentos manuais.
- Vouchers e Revisão (`STR-199`): saldo por referência válida mais recente (`Abertura`/`Observado`/`Ajuste`), materialização idempotente de recargas programadas, modal `Ajustar saldo` e revisão explícita para `BENEFICIO_AMBIGUO` e `DIVERGENCIA_RECARGA`.
- Início (`STR-200`): saldo do mês (receitas − despesas, transferências excluídas), seletor de mês, avisos de "N para revisar" e "captura parada", blocos independentes de faturas abertas e saldos de benefícios (com isolamento de falha e sem `R$ 0,00` fora do saldo do mês), top 3 categorias de despesa, últimos 5 lançamentos e atalho para lançamento manual.
- Lançamentos: lista por dia, busca, filtros, detalhe/edição e novo lançamento.
- Revisão de pendências, notificações por pendente (Confirmar sem abrir o app e Corrigir abrindo o detalhe) e Status da captura com permissões, contadores e reconexão.
- Relatórios e Assinaturas (`STR-201`, `STR-202`): visão mensal em `America/Sao_Paulo`, gráfico de rosca acessível com comparação MoM (`"sem base"` quando o mês anterior é zero), barras de receitas × despesas de 6 meses, drill-down para `Lançamentos` filtrado por categoria e mês, detecção/decisão de assinaturas por instrumento + estabelecimento normalizado e seção `Assinaturas` com total mensal e estado vazio.
- Lembretes de assinatura (`STR-203`): preferência DataStore `avisoAssinaturasAtivo` (padrão `false`), `AvisoDeAssinaturaWorker` via WorkManager (`09:00 America/Sao_Paulo`, 2 dias antes, janela do mesmo dia `<= 23:59`, sem `textoBruto` conforme RNF-11), toggle e tratamento de `POST_NOTIFICATIONS` em Configurações.

## Como buildar

Requisitos: JDK 21 e Android SDK com a plataforma API 36.

```bash
./gradlew assembleDebug
./gradlew build
```

O `build` roda compilação, testes unitários, detekt e ktlint (Spotless).

## Release local

A variante **release** fixa `API_BASE_URL=https://api-pingo.davimartinslage.com.br/` em `app/build.gradle.kts` e não é debuggable. A variante **debug** continua lendo `pingo.api.baseUrl` de `local.properties` (padrão `http://10.0.2.2:8080/`) e mantém a seam de notificações sintéticas.

O `assembleRelease` gera um APK não assinado. A assinatura é feita fora do repositório, por linha de comando, com chave e senha guardadas em `../release-private/`:

```bash
./gradlew :app:assembleRelease
$ANDROID_HOME/build-tools/36.0.0/zipalign -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk /tmp/pingo-aligned.apk
$ANDROID_HOME/build-tools/36.0.0/apksigner sign --ks ../release-private/pingo-release.p12 \
  --ks-type PKCS12 --ks-key-alias pingo-release \
  --ks-pass "file:../release-private/pingo-release.password" \
  --out ../releases/Pingo-0.2.0-release.apk /tmp/pingo-aligned.apk
```

Nenhum caminho de chave, senha ou `signingConfig` fica no repositório. O cadastro do SHA-1 no Google/Auth0 e a compatibilidade da API de produção são pendências externas descritas em `../releases/README.md`.

### Release pelo GitHub Actions

O workflow `Release` publica o APK assinado em GitHub Releases ao enviar uma tag `v*` ou ao disparar manualmente com uma tag existente. Ele grava o `local.properties` com os valores do Auth0, assina com a chave de release guardada nos secrets, verifica a assinatura e anexa o APK e o `sha256`.

Variáveis do repositório (Settings → Secrets and variables → Actions → Variables): `AUTH0_DOMAIN`, `AUTH0_CLIENT_ID`, `AUTH0_AUDIENCE`, `AUTH0_GOOGLE_WEB_CLIENT_ID` e, opcional, `ANDROID_KEY_ALIAS` (padrão `pingo-release`).

Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD` e `ANDROID_KEY_PASSWORD`. Sem eles o workflow falha antes de construir.

```bash
gh secret set ANDROID_KEYSTORE_BASE64 --repo oDaviML/pingo-android < <(base64 -w0 ../release-private/pingo-release.p12)
gh secret set ANDROID_KEYSTORE_PASSWORD --repo oDaviML/pingo-android < ../release-private/pingo-release.password
gh secret set ANDROID_KEY_PASSWORD --repo oDaviML/pingo-android < ../release-private/pingo-release.password
```

A tag define a `versionName` (`v0.2.0` gera `0.2.0`); o `versionCode` continua sendo alterado em `app/build.gradle.kts`.

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

Transferências guardam a direção financeira de cada linha em `tipoSemTransferencia` (`DESPESA`, `RECEITA` ou nulo quando desconhecida), inclusive depois que as duas linhas passam a `TRANSFERENCIA`. O schema 4 adiciona a coluna nullable pela migration 3→4, sem backfill: registros antigos continuam nulos e o Desfazer desses pares é recusado com aviso, em vez de inferir a direção por categoria, lado aberto, horário ou UUID. O campo é opcional no push/pull; ausência ou `null` na mesma `transferenciaId` preserva o valor local conhecido, enquanto outra transferência ou uma linha que deixou de ser transferência não herda a direção.

O cadastro compartilhado de contas e instrumentos exige fechamento e vencimento entre 1 e 31 para crédito, aceita últimos quatro dígitos opcionais e, para voucher, exige benefício e saldo inicial com recarga opcional apenas quando dia e valor são informados juntos. A edição preserva `publicId` e `criadoEm` de conta e instrumentos e grava tudo numa única transação, sem deixar grafo parcial.

Valores monetários seguem o padrão brasileiro: o ponto separa milhares e a vírgula separa centavos, então `1.000` vale R$ 1.000,00 e `1.000,00` mantém o mesmo valor. Dia e valor da recarga são validados como texto antes da conversão: os dois vazios são aceitos, mas um valor malformado ou um par incompleto mantém o erro no campo e impede salvar, em vez de descartar o que foi digitado. Durante o salvamento, onboarding e Contas congelam os campos mutáveis e recusam eventos de edição concorrentes; toques repetidos em salvar não criam entidades e uma falha libera a edição para a nova tentativa. Quando o onboarding já gravou as contas no Room e a conclusão nas preferências falha, a tela é reaberta pelas identidades persistidas: a nova tentativa atualiza as mesmas contas e instrumentos, sem duplicar o grafo nem ignorar valores corrigidos.

O schema 5 adiciona `instrumentos.arquivadoPorCascata` pela migration 4→5, `INTEGER NOT NULL DEFAULT 0`, sem backfill: registros legados não ganham procedência inventada. Arquivar a conta marca só os instrumentos ativos; reativar restaura apenas os arquivados por essa cascata, e instrumentos arquivados antes continuam arquivados. No push o campo vai explícito; no pull, um payload legado sem o campo preserva o valor local conhecido e um snapshot com valor explícito aplica a reativação. Reativar no servidor sem o campo limpa a procedência incompatível de um instrumento ativo, e arquivar manualmente também zera a procedência, de modo que a reativação posterior da conta não revive um item arquivado fora da cascata. Para os demais campos opcionais do instrumento, o push envia `null` explícito quando a configuração é removida, e o pull que omite um campo (servidor antigo) preserva o valor local já conhecido em vez de apagá-lo.

Arquivar não apaga histórico: a conta e seus instrumentos saem das consultas selecionáveis (`observeContasAtivas`/`observeInstrumentosAtivos`), mas continuam nas consultas históricas usadas por Lançamentos, revisão e relatórios. Por isso novos lançamentos manuais e destinos de transferência da revisão só oferecem conta e instrumento ativos, e a captura só associa notificações a instrumentos de conta ativa. Na edição de um lançamento histórico, o instrumento arquivado original continua aceito, mas não é possível trocar para outro arquivado. "Excluir conta" no formulário abre a confirmação de arquivamento em vez de remover transações.

O fluxo de arquivamento é coberto em três camadas: ViewModel (instrumento isolado, cascata com cancelamento, reativação e falha), Room (`ContaDao`/`InstrumentoDao` separando consultas ativas e históricas) e app (`ArquivamentoFluxoTest` alterna conta e instrumento, reabre a conta, reativa e confirma que o lançamento histórico continua em Lançamentos). O teste de sync (`SyncHttpIntegrationTest`) empurra um grafo com conta arquivada por cascata e instrumento arquivado manualmente, restaura num banco vazio, reativa depois da restauração e repete o pull sem duplicar, cobrindo todos os campos de conta/instrumento, isolamento entre dois usuários e a procedência preservada em HTTP real.

O schema 6 (`MIGRACAO_5_6`, `6.json`) adiciona `configuracoes_cartao`, `referencias_saldo_voucher`, `recargas_programadas_voucher`, `assinaturas.instrumentoPublicId` nullable e os campos F2 em `lancamentos` (`beneficio`, `saldoObservadoCentavos`, `estabelecimentoNormalizado`). O sync F2 espelha a API com `CampoPresenca` para preservar omissão vs `null` explícito e não envia `textoBruto` (RNF-11).

## Design system

O `core:designsystem` traduz o design system do FigCraft (`design/design-system.md` e `design/pingo.figcraft`) para Compose, sem Material 3: tema próprio (`PingoTheme`) com tokens de cor, tipografia Nunito, raios, espaçamento, elevação e movimento (`PingoMotion` e animações `"Pingar"`), 79 ícones Lucide, `PingoBottomBar` persistente com botão gota `AddDrop`, `PingoTopBar`, `InstitutionAvatar`, `CategoryAvatar`, além dos componentes financeiros e de gráficos (`PingoInvoiceCard`, `PingoDebitCard`, `PingoVoucherCard`, `PingoTabs`, `PingoDonutChart`, `PingoComparisonRow`, `PingoIncomeExpenseBars`, `PingoSubscriptionRow`, `PingoSubscriptionSuggestionCard`) e do stepper do onboarding.

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

Checagens locais do cadastro de contas (unitários e instrumentados focados):

```bash
./gradlew --max-workers=2 :core:ui:testDebugUnitTest :feature:accounts:impl:testDebugUnitTest \
  :feature:onboarding:impl:testDebugUnitTest :core:data:testDebugUnitTest

ANDROID_SERIAL=emulator-5580 ./gradlew --max-workers=2 :feature:accounts:impl:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.pingo.feature.accounts.impl.AccountsContentTest,com.pingo.feature.accounts.impl.ContaFormContentTest,com.pingo.feature.accounts.impl.ContaFormRoomTest,com.pingo.feature.accounts.impl.ContaFormSalvamentoTest \
  -Pandroid.testInstrumentationRunnerArguments.pingo.syncHost=http://10.0.2.2:8080/

ANDROID_SERIAL=emulator-5580 ./gradlew --max-workers=2 :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.pingo.contas.ContasFluxoTest,com.pingo.contas.ArquivamentoFluxoTest,com.pingo.captura.CapturaListenerTest,com.pingo.sync.SyncHttpIntegrationTest \
  -Pandroid.testInstrumentationRunnerArguments.pingo.syncHost=http://10.0.2.2:8080/

ANDROID_SERIAL=emulator-5580 ./gradlew --max-workers=2 :core:database:connectedDebugAndroidTest \
  :feature:review:impl:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.pingo.core.database.ContaDaoTest,com.pingo.core.database.InstrumentoDaoTest,com.pingo.core.database.CapturaDaoTest,com.pingo.feature.review.impl.ReviewRoomTest \
  -Pandroid.testInstrumentationRunnerArguments.pingo.syncHost=http://10.0.2.2:8080/
```

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

Gate F2 verificado: `./gradlew --max-workers=2 test spotlessCheck detekt assembleDebug` (1585/1585 unitários verdes, `spotlessCheck` e `detekt` sem erros) e `./gradlew --max-workers=2 connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.pingo.syncHost=http://10.0.2.2:8080/` nos 12 módulos em Android 15 (`API 35`) e Android 16 (`API 36.1`) contra `Pingo.Api.TestHost` + PostgreSQL 18 (0 falhas, 0 erros, apenas os 2 skips esperados em `FixtureDeAberturaFriaTest`), incluindo `SyncHttpIntegrationTest` (6/6), `AvisoDeAssinaturaWorkManagerTest` (8/8), `FaturasEIndicadoresFluxoTest`, `VoucherCajuEFluxoFinanceiroTest`, `RelatoriosAssinaturasEToggleTest` e o benchmark `VAL-REP-006` (`<= 1.500 ms`).

## Documentação

- [AGENTS.md](AGENTS.md) — contexto, arquitetura, comandos, boas práticas e fluxo de PRs
