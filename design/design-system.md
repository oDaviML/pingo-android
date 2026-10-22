# Pingo — Design System

Fonte de verdade da STR-172. Arquivo de design: `design/pingo.figcraft` (FigCraft).
Implementação alvo: `core:designsystem` (Jetpack Compose, ADR-03).

## 1. Identidade

Lúdico e limpo, identidade de gota: mascote gota com olhos, cantos moderados (nada de pílula
em tudo), teal vivo sobre fundo neutro azulado e movimento sutil, com o “pingar” como assinatura.

Logo (página Foundations): `Logo/Mascote`, `Logo/Lockup` (gota + “pingo” em Nunito Black),
`Logo/App icon` (adaptive, gradiente deep → primary) e `Logo/Mono`.

## 2. Cores

Paleta base: `#09637E` deep · `#088395` primary · `#7AB2B2` soft · `#EBF4F6` surface.
Coleção de variáveis `Pingo`, modos **Light** e **Dark**. Componentes sempre usam o token.

| Token | Light | Dark | Uso |
| --- | --- | --- | --- |
| background | #EBF4F6 | #0B2A33 | fundo das telas |
| surface | #FFFFFF | #0F3A47 | cards, sheets, campos |
| surfaceVariant | #DCEAEC | #13404E | apoio, trilhos, chips inativos |
| primary | #088395 | #1AA7BC | ação principal, seleção |
| primaryPressed | #09637E | #7AB2B2 | estado pressionado |
| onPrimary | #FFFFFF | #04222A | conteúdo sobre primary |
| primaryContainer | #CDE6EA | #13505F | botão tonal, chip selecionado, indicador da aba |
| onPrimaryContainer | #09637E | #CDE6EA | conteúdo sobre container |
| secondary | #7AB2B2 | #7AB2B2 | tonal pressionado, detalhes |
| brand | #09637E | #7AB2B2 | logo, títulos de marca |
| onBackground | #0B2A33 | #EBF4F6 | texto principal |
| onSurfaceVariant | #4A6A72 | #9FBEC4 | texto secundário |
| outline / outlineVariant | #AFCBCE / #D3E3E5 | #2C5560 / #1E4955 | bordas / divisórias |
| disabledContainer / onDisabled | #DCE3E5 / #9DB3B8 | #1B3F49 / #5E7F87 | desabilitado |
| expense · Pressed · Container | #C8453B · #A8362D · #F8E1DF | #F08A82 · #F4A59E · #4A2522 | despesa, destrutivo |
| income · Container | #2E7D5B · #DDEFE6 | #6FC79E · #1F3F31 | receita, sucesso |
| transfer | #6B7A80 | #9FB1B6 | transferência |
| warning · Container · onContainer | #C9802B · #FBEFD9 · #7A4E12 | #E6A84F · #4A3517 · #F3D29E | alertas, pendente |
| scrim | #0B2A33 40% | #000 60% | atrás de sheets e diálogos |

Contraste: `primary` sobre branco ≈ 3,9:1 — use em superfícies, ícones e texto ≥ 18 sp;
texto pequeno sobre claro usa `brand`/`onPrimaryContainer` (≈ 7:1).

## 3. Tipografia — Nunito

| Estilo | Peso | Tamanho/linha | Uso |
| --- | --- | --- | --- |
| Amount Large | ExtraBold | 40/48 · tnum | saldo, valor do lançamento manual |
| Display | ExtraBold | 32/40 | títulos grandes |
| Headline | Bold | 24/32 | valores de card, títulos de seção |
| Title | Bold | 20/28 | títulos de diálogo/sheet |
| Subtitle | SemiBold | 18/24 | top bars, seletor de mês |
| Body Strong / Body / Body Small | SemiBold / Regular / Regular | 16/24 · 16/24 · 14/20 | texto |
| Label / Label Small | Bold / SemiBold | 14/20 · 12/16 | botões, chips, rótulos |
| Caption | Regular | 12/16 | metadados |
| Overline | ExtraBold, caixa alta, +8% | 11/16 | cabeçalhos de grupo e de dia |
| Amount | Bold · tnum | 16/24 | valores em listas |

## 4. Forma, espaço e elevação

- Raios: `xs 4` badges · `sm 8` chips/checkbox · `md 12` botões/campos · `lg 16` cards ·
  `xl 20` cards de destaque e sheets · `full` FAB, avatar, switch.
- Espaço base 4: 4 · 8 · 12 · 16 · 20 · 24 · 32. Margem lateral 16–20 dp, área de toque ≥ 48 dp.
- Elevação (estilos de efeito): `Elevation/1` cards · `Elevation/2` menus, FAB, barra inferior ·
  `Elevation/3` sheets e diálogos. Sombra sempre tingida de `#0B2A33`, nunca preta.

## 5. Ícones

Lucide, traço 2, 24 dp (`design/icons/src`, componentes `Icon/<nome>`). O build gera cópias
tingidas por tom em `design/.build/icons/<tom>`; no Compose, use o mesmo conjunto com `tint`.

## 6. Movimento

Detalhado na página **Motion**. Durações `motion/short 150`, `motion/medium 250`,
`motion/long 400` ms; curvas `standard (0.2,0,0,1)`, `decelerate (0,0,0,1)`, `accelerate (0.3,0,1,1)`
e `spring/gentle`. “Pingar” (400 ms): gota cai, toca a lista, abre duas ondas e o item entra com
fade + 8 dp — os 5 quadros estão ligados por Smart Animate (flow “Pingar”). Com “Remover animações”
ativo no Android, tudo vira corte ou fade de 100 ms.

## 7. Componentes (página Components)

| Board | Componentes e variantes |
| --- | --- |
| Botões | `Button` Type (Primary, Tonal, Outline, Text, Danger) × State (Default, Pressed, Disabled, Loading) × Size (Medium 48, Small 36), props Label/Icon · `IconButton` Style × State · `FAB` Regular/Extended × Default/Pressed |
| Campos | `TextField` (Default, Focused, Filled, Error, Disabled, ReadOnly) · `SearchField` · `Select` (Closed, Open com menu, Disabled) · `AmountField` (Empty, Filled, Error) · `TextArea` · `PinInput` · `NumericKeypad` |
| Seleção | `Checkbox` · `Radio` · `Switch` · `FilterChip` (Toggle/Menu × Selected) · `InputChip` · `CategoryChip` · `SegmentedControl` (despesa/receita/transferência) · `Tabs` (faturas) |
| Datas e horários | `MonthSelector` · `MonthGrid` · `DatePicker` (Single, Range) · `DatePickerDialog` · `DayOfMonthPicker` (fechamento/vencimento) · `TimePicker` (Dial, Wheel) |
| Navegação | `StatusBar` · `GestureBar` · `TopAppBar` (Home, Large, Small, Modal) · `NavItem` · `BottomNavBar` (4 abas + “+” em gota) · `OnboardingStepper` (1–8) · `BottomSheet` (com slot) · `SectionHeader` |
| Listas e cartões | `InstitutionAvatar` (5 bancos + Outra) · `CategoryAvatar` · `TransactionItem` Type × Status (Confirmed, Pending, Manual) · `SwipeAction` · `DayHeader` · `BalanceCard` · `InvoiceCard` (Open, Closed) · `VoucherCard` · `DebitCard` · `CategoryRow` · `SettingsRow` (Navigation, Switch, Value, Danger) · `PermissionItem` (Granted, Pending, Denied) · `SubscriptionRow` · `StatTile` · `ReviewCard` (4 motivos) |
| Feedback | `Banner` (Info, Warning, Error, Success) · `Snackbar` (com Desfazer) · `Dialog` (Destructive, Confirm) · `Badge` · `StatusTag` · `LinearProgress` · `Spinner` · `SkeletonItem` · `EmptyState` (NoData, NoResults, AllDone, ManualOnly) · `Tooltip` |
| Gráficos | `DonutChart` · `ComparisonRow` (Up/Down) · `IncomeExpenseBars` (6 meses) |

Nomes dos componentes = nomes dos composables (`PingoButton`, `PingoTextField`…), e as
propriedades viram parâmetros.

## 8. Trabalhando no FigCraft

- CLI headless: **`figcraft-cli`**. O binário `figcraft` é o app desktop (abre janela).
- Fonte: `design/scripts/*.jsonl` — comandos FigCraft puros, um por linha, com `{"$step": n}`
  referenciando nós criados antes no mesmo script. Rodam em ordem; cada um é tudo-ou-nada.
- `design/build.sh` gera os ícones tingidos, valida as referências (`tools/check-refs.jq`),
  resolve `{"@ref": "Nome"}` para componentes de scripts anteriores, roda os scripts e exporta
  `exports/Foundations.png`, `exports/Components/*.png` e `exports/Motion/*.png`.
- `tools/authoring/*.sh` geram os `.jsonl` das matrizes de variantes (o que seria inviável
  numerar à mão). Edite o gerador e rode `bash tools/authoring/<board>.sh scripts/<board>.jsonl`.
- Para abrir e mexer visualmente: `figcraft design/pingo.figcraft`. Edições feitas no app não
  voltam para os scripts — combine antes qual dos dois é a fonte de uma mudança.
- Requisitos: `figcraft-cli` 0.7+, `jq`, fonte Nunito instalada (`~/.local/share/fonts`).

## 9. Limitações conhecidas (FigCraft 0.7)

- Modo de variável definido no componente principal não passa para as instâncias; por isso as
  variantes ligam as cores direto nos tokens, sem coleção de “tons”.
- Ícones importados de SVG têm cor fixa por tom (não seguem Light/Dark); no Compose a cor vem
  do `tint`.
- Emoji não renderiza na Nunito no export (aparece no app pela fonte do sistema).

## 10. Próximo passo

Protótipos completos das telas T-01 a T-11 na página **Screens**, montados só com instâncias
destes componentes, com estados (vazio, filtros, permissão negada) e navegação do §13.
