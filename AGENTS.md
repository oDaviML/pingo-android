# AGENTS.md — pingo-android

## 1. Contexto do projeto

O Pingo é um app de finanças pessoais com captura automática de transações. O app lê as notificações emitidas pelos apps financeiros instalados no aparelho (bancos digitais, carteiras, cartões), extrai os dados da transação com um motor de regras que roda no próprio aparelho e organiza lançamentos, contas, faturas e categorias sem exigir que o usuário digite qualquer valor.

Persona principal: Lucas, 22 a 35 anos, CLT, conta em vários bancos digitais, já usou apps de finanças e os abandonou por causa do trabalho manual de lançar gastos. O produto precisa ser invisível na maior parte do tempo: capturar sozinho, pedir o mínimo de atenção e responder rápido quando Lucas abrir o app.

O papel deste repositório, `pingo-android`, é o aplicativo Android: o listener de notificações, o parser on-device, o armazenamento local e a interface com Jetpack Compose. As regras de negócio detalhadas, os fluxos de tela e os requisitos (RNFs) estão na especificação do projeto; este documento descreve como o código é organizado e como trabalhar nele.

## 2. Arquitetura

- Multimódulo no padrão Now in Android: `app`, `core:*`, `feature:*` e `sync`, com Gradle Kotlin DSL e version catalog centralizado em `gradle/libs.versions.toml`.
- Camadas:
  - `app` — monta o NavHost, aplica Hilt e conecta as features.
  - `core:*` — fundamentos compartilhados e sem dependência de produto (modelos, dados, banco, rede, parser, autenticação, notificações, analytics).
  - `feature:*` — uma feature por área de produto, sempre dividida em `api` (chaves e rotas de navegação, sem UI) e `impl` (implementação, depende do próprio `api`).
  - `sync` — sincronização em background com WorkManager.
- Regras de dependência:
  - `core:*` nunca depende de `feature` nem de `app`.
  - `feature:*` depende apenas dos `core:*` que usa. O `AndroidFeatureConventionPlugin` já adiciona `core:model`, `core:ui` e `core:designsystem`; qualquer outro `core:*` precisa ser declarado explicitamente no módulo.
  - `feature:*:impl` depende do próprio `feature:*:api`.
  - Os convention plugins em `build-logic` adicionam as dependências padrão (`core:model`, `core:ui` e `core:designsystem`); as regras são garantidas por revisão (humana e por agente de revisão) e por este documento.
- `spike:capture` — app de spike isolado (`com.pingo.spike`) do épico STR-157, para coletar o corpus de notificações em aparelhos de teste. Não é produto e não é publicado; por isso abre mão de Hilt, ViewModel/UDF e dos `core:*`/`feature:*`, e pode escrever direto em disco. As exceções valem só para o spike e não servem de precedente para as features.
- ADR-03: Kotlin + Jetpack Compose como stack única do app.
- ADR-09: dois repositórios (`pingo-api` e `pingo-android`), app Android multimódulo, AGENTS.md como documentação viva e agente de revisão assistida de PRs.

## 3. Estrutura de pastas

```
pingo-android/
├─ settings.gradle.kts          include dos módulos e includeBuild do build-logic
├─ build.gradle.kts             plugins globais, detekt e spotless para todos os módulos
├─ gradle.properties            JVM args, AndroidX e paralelismo
├─ gradle/
│  ├─ libs.versions.toml        version catalog central (versões, libs, plugins)
│  └─ wrapper/                  Gradle wrapper
├─ build-logic/                 composite build de convention plugins (não é buildSrc)
│  ├─ settings.gradle.kts
│  └─ convention/
│     ├─ build.gradle.kts
│     └─ src/main/kotlin/
│        ├─ AndroidApplicationConventionPlugin.kt  pingo.android.application
│        ├─ AndroidLibraryConventionPlugin.kt       pingo.android.library
│        ├─ AndroidFeatureConventionPlugin.kt       pingo.android.feature
│        ├─ AndroidComposeConventionPlugin.kt       pingo.android.compose
│        ├─ AndroidHiltConventionPlugin.kt          pingo.android.hilt
│        ├─ AndroidRoomConventionPlugin.kt          pingo.android.room
│        ├─ JvmLibraryConventionPlugin.kt           pingo.jvm.library
│        └─ AndroidConfig.kt                        configuração compartilhada via version catalog
├─ spike/
│  └─ capture/                  app de spike (com.pingo.spike) para coletar o corpus de notificações
├─ app/                         applicationId com.pingo
│  └─ src/
│     ├─ main/kotlin/com/pingo/
│     │  ├─ PingoApplication.kt      @HiltAndroidApp
│     │  ├─ MainActivity.kt          @AndroidEntryPoint, setContent
│     │  └─ navigation/              PingoNavHost conectando as rotas das features
│     ├─ test/                       testes unitários
│     └─ androidTest/                testes instrumentados
├─ core/
│  ├─ model/        modelos de domínio: Usuario, Conta, Instrumento, Lancamento, Categoria, RegraCategoria, Fatura, Assinatura, RegraParser
│  ├─ common/       dispatchers (AppDispatchers) e Result (AppResult), Kotlin puro
│  ├─ designsystem/ casca do design system (tema, cores, tipografia e componentes virão depois)
│  ├─ ui/           componentes de UI que renderizam os models
│  ├─ data/         repositórios offline-first com Flow do Room
│  ├─ database/     Room + SQLCipher, entidades, DAOs e schema versionado
│  ├─ datastore/    preferências e estado de sync
│  ├─ network/      cliente da pingo-api
│  ├─ notifications/ NotificationListenerService, foreground service e health check
│  ├─ parser/       motor de regras versionadas, confiança, dedup e transferência
│  ├─ auth/         Auth0 SDK, token store e biometria
│  ├─ analytics/    fachada Firebase Analytics/Crashlytics opt-in
│  └─ testing/      fakes e TestDispatcherRule
├─ sync/                        workers do WorkManager
├─ feature/
│  ├─ onboarding/     {api, impl}
│  ├─ home/           {api, impl}
│  ├─ transactions/   {api, impl}
│  ├─ accounts/       {api, impl}
│  ├─ reports/        {api, impl}
│  ├─ review/         {api, impl}
│  ├─ capturestatus/  {api, impl}
│  └─ settings/       {api, impl}
├─ config/detekt/detekt.yml     configuração do detekt
├─ .github/workflows/           build.yml, release.yml
├─ .gitignore
├─ AGENTS.md
└─ README.md
```

Namespace dos módulos Android: `com.pingo.<area>.<nome>` (por exemplo, `com.pingo.core.model`, `com.pingo.feature.home.api`). O `app` usa `applicationId` `com.pingo`.

## 4. Comandos

```bash
./gradlew build                build completo: compila, roda testes, detekt e ktlint (spotless)
./gradlew assembleDebug        gera o APK de debug
./gradlew test                 testes unitários
./gradlew connectedAndroidTest testes instrumentados (device/emulador conectado)
./gradlew spotlessCheck        ktlint em todo o código
./gradlew spotlessApply        corrige o formato automaticamente
./gradlew detekt               análise estática em todos os módulos
./gradlew projects             lista os módulos
```

Para rodar localmente: `./gradlew installDebug` com um dispositivo ou emulador conectado, ou `./gradlew assembleDebug` e instalar o APK gerado. O listener de notificações precisa ser habilitado manualmente em Ajustes do Android após a primeira instalação.

## 5. Boas práticas

- UDF (unidirectional data flow): estado desce, eventos sobem. Cada tela tem um ViewModel que expõe um `StateFlow<UiState>` e a UI coleta com `collectAsStateWithLifecycle()`.
- Composables sem estado (state hoisting): recebem `state` e callbacks `onEvent`; nunca guardam estado de negócio.
- Hilt para injeção de dependência via convention plugin; ViewModels com `@HiltViewModel`.
- Dispatchers injetados (`AppDispatchers`), nunca `Dispatchers.IO` fixo no código.
- Repositórios expõem `Flow` do Room como fonte de verdade; rede só alimenta o banco.
- Estado imutável (`@Immutable`/`@Stable`), sem coleções mutáveis em parâmetros de composables.
- Listener de notificações e parser sempre fora da main thread.
- Chave do SQLCipher no Android Keystore.
- Testes: unitários em ViewModel, parser e repositórios; UI com `createAndroidComposeRule`; screenshot tests no design system.
- ktlint (Spotless) + detekt obrigatórios, localmente e no CI.

## 6. Estilo de código

Regra estrita: código sem comentários. Nenhum comentário em fontes Kotlin, Gradle ou YAML. A intenção é expressa pelos nomes e pela estrutura: nomes descritivos, funções pequenas, um conceito por arquivo. Nada de código comentado deixado "para depois" — se não serve, remove.

## 7. Segurança e privacidade

- O texto bruto das notificações nunca sai do aparelho (RNF-11): o parser roda on-device e só dados estruturados podem ser sincronizados, com consentimento.
- Segredos (chaves de API, credenciais do Auth0, configuração do Firebase) ficam fora do repositório, em `local.properties` ou variáveis de ambiente do CI.
- LGPD: exclusão de dados sob pedido do usuário, com rotina de apagamento local e remoto.

## 8. PRs

- Título no padrão `STR-xxx — descrição curta` (ex.: `STR-172 — design system e protótipos`), corpo explicando o porquê e o impacto.
- Checks obrigatórios: CI verde (`./gradlew build`, que roda compilação, testes, detekt e ktlint) e revisão por agente de revisão no PR.
- PRs pequenos, por feature ou correção; PRs de fundação podem agrupar os cards do mesmo épico, com justificativa no corpo.
- Atualizar o AGENTS.md quando a arquitetura ou os comandos mudarem.
