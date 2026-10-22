invoice() {
  local parent="$1" status="$2" bank="$3" title="$4" amount="$5" closing="$6" due="$7"
  inst "$parent" InvoiceCard
  local i=$R
  props_of "$i" "$(jq -cn --arg s "$status" '{Status:$s}')"
  props_of "$i" "$(jq -cn --arg t "$title" --arg a "$amount" --arg c "$closing" --arg d "$due" --arg b "InstitutionAvatar/Bank=$bank" '{Title:$t, Amount:$a, Closing:$c, Due:$d, Bank:{"@ref":$b}}')"
  fillw "$i"
  R=$i
}

voucher() {
  inst "$1" VoucherCard
  local v=$R
  props_of "$v" "$(jq -cn --arg t "$2" --arg b "$3" --arg r "$4" '{Title:$t, Balance:$b, Recharge:$r, Bank:{"@ref":"InstitutionAvatar/Bank=Caju"}}')"
  fillw "$v"
  R=$v
}

debit() {
  inst "$1" DebitCard
  local d=$R
  props_of "$d" "$(jq -cn --arg t "$2" --arg a "$3" --arg b "InstitutionAvatar/Bank=$4" '{Title:$t, Amount:$a, Bank:{"@ref":$b}}')"
  fillw "$d"
  R=$d
}

catrow() {
  inst "$1" CategoryRow
  local c=$R
  props_of "$c" "$(jq -cn --arg s "$2" '{Share:$s}')"
  props_of "$c" "$(jq -cn --arg k "CategoryAvatar/Category=$3" --arg t "$4" --arg d "$5" --arg a "$6" '{Category:{"@ref":$k}, Title:$t, Detail:$d, Amount:$a}')"
  fillw "$c"
  R=$c
}

month_sel() {
  inst "$1" MonthSelector
  props_of "$R" "$(jq -cn --arg m "${2:-Outubro 2026}" '{Month:$m}')"
  fillw "$R"
}

recent_card() {
  card "$1" "Últimos lançamentos"; local c=$R
  tx_item "$c" Expense Confirmed "Padaria do Zé" "Nubank · crédito · 14:32" "- R\$ 42,90" Alimentação; FIRST_TX=$R
  divider "$c"
  tx_item "$c" Expense Pending "Uber *Trip" "Nubank · crédito · 12:05" "- R\$ 18,90" Transporte
  divider "$c"
  tx_item "$c" Income Confirmed "Pix de Ana Souza" "Inter · conta · 09:10" "+ R\$ 150,00" Receita
  if [[ "${2:-5}" -ge 4 ]]; then
    divider "$c"
    tx_item "$c" Expense Confirmed "Supermercado Dia" "Itaú · débito · ontem" "- R\$ 187,45" Mercado
    divider "$c"
    tx_item "$c" Transfer Confirmed "Inter → Mercado Pago" "Transferência · ontem" "R\$ 500,00" Transferência
  fi
  R=$c
}

# ---------------- T-02 Início ----------------
H="03 T-02 · Início"

screen home "$H" 1 "1 · Padrão" "T-02.1 Início — padrão"
appbar "$SC" Home Lucas; link "$R" set
body_hug "$SC" 16 20 4 24; B=$R
month_sel "$B"
banner "$B" Info "3 lançamentos para revisar" "Toque para conferir o que o Pingo não teve certeza."; link "$R" rev
inst "$B" BalanceCard; fillw "$R"
section "$B" "Faturas de crédito" "Ver contas"; link "$R" acc
invoice "$B" Open Nubank "Nubank · crédito" "R\$ 1.284,50" "Fecha dia 3 · vence dia 10" "Vence em 19 dias"; link "$R" acc_invoice
invoice "$B" Closed Inter "Inter · crédito" "R\$ 642,10" "Fechou dia 20 · pagar até 27/10" "Vence em 5 dias"; link "$R" acc_invoice
section "$B" "Saldos de benefícios"
voucher "$B" "VR · Refeição" "R\$ 486,20" "Última recarga 01/10 · R\$ 800,00"
voucher "$B" "VA · Alimentação" "R\$ 214,70" "Última recarga 01/10 · R\$ 450,00"
section "$B" "Top categorias" "Ver relatório"; link "$R" rep
card "$B" "Top categorias"; TC=$R
catrow "$TC" 30 Alimentação "Alimentação" "32% das despesas" "R\$ 1.190,00"
divider "$TC"
catrow "$TC" 20 Mercado "Mercado" "21% das despesas" "R\$ 781,00"
divider "$TC"
catrow "$TC" 20 Transporte "Transporte" "17% das despesas" "R\$ 632,00"
section "$B" "Últimos lançamentos" "Ver tudo"; link "$R" tx
recent_card "$B" 5; link "$FIRST_TX" det
bottom_nav "$SC" home Início

screen home_empty "$H" 2 "2 · Vazio" "T-02.2 Início — vazio"
appbar "$SC" Home Lucas
body "$SC" 16 20 4 24; B=$R
month_sel "$B"
spacer "$B"
empty_state "$B" NoData; link "$R" new up 250
spacer "$B"
bottom_nav "$SC" home_empty Início

screen home_off "$H" 3 "3 · Captura parada" "T-02.3 Início — captura parada"
appbar "$SC" Home Lucas
body_hug "$SC" 16 20 4 24; B=$R
month_sel "$B"
banner "$B" Warning "Captura parada" "O Pingo não lê notificações desde 08:14. Toque para corrigir."; link "$R" cap_off
banner "$B" Info "3 lançamentos para revisar" "Toque para conferir o que o Pingo não teve certeza."; link "$R" rev
inst "$B" BalanceCard; fillw "$R"
section "$B" "Faturas de crédito" "Ver contas"
invoice "$B" Open Nubank "Nubank · crédito" "R\$ 1.284,50" "Fecha dia 3 · vence dia 10" "Vence em 19 dias"
section "$B" "Últimos lançamentos" "Ver tudo"
recent_card "$B" 3
bottom_nav "$SC" home_off Início

# ---------------- T-03 Lançamentos ----------------
H="04 T-03 · Lançamentos"

filter_row() {
  hrow "$1" "Search row" 8
  local r=$R
  inst "$r" SearchField; props_of "$R" "$(jq -cn --arg s "${2:-Default}" --arg q "${3:-Buscar lançamentos}" '{State:$s, Query:$q}')"; SEARCH=$R; fillw "$SEARCH"
  inst "$r" IconButton; props_of "$R" '{"Style":"Tonal"}'; FILTERBTN=$R
}

chips_row() {
  frame "$1" "Chips" '{"layout":"horizontal","gap":8,"clip":true,"sizingH":"fill","sizingV":"hug","fills":[]}'
  local r=$R chip
  shift
  for chip in "$@"; do
    IFS='|' read -r kind label state <<< "$chip"
    inst "$r" FilterChip
    props_of "$R" "$(jq -cn --arg k "$kind" --arg l "$label" --arg s "$state" '{Kind:$k, Label:$l, State:$s}')"
  done
}

tx_days() {
  local B="$1" mode="${2:-plain}"
  day_header "$B" "Hoje · qui, 22 out" "- R\$ 60,00"
  card "$B" "Hoje"; local c=$R
  if [[ $mode == swipe ]]; then
    inst "$c" SwipeAction; props_of "$R" '{"Action":"Delete"}'; SWIPE_DEL=$R
    divider "$c"
    inst "$c" SwipeAction; props_of "$R" '{"Action":"Category"}'
    divider "$c"
  else
    tx_item "$c" Expense Confirmed "Padaria do Zé" "Nubank · crédito · 14:32" "- R\$ 42,90" Alimentação; TXFIRST=$R
    divider "$c"
    tx_item "$c" Expense Pending "Uber *Trip" "Nubank · crédito · 12:05" "- R\$ 18,90" Transporte
    divider "$c"
  fi
  tx_item "$c" Income Confirmed "Pix de Ana Souza" "Inter · conta · 09:10" "+ R\$ 150,00" Receita
  day_header "$B" "Ontem · qua, 21 out" "- R\$ 208,35"
  card "$B" "Ontem"; c=$R
  tx_item "$c" Expense Confirmed "Supermercado Dia" "Itaú · débito · 19:20" "- R\$ 187,45" Mercado
  divider "$c"
  tx_item "$c" Expense Confirmed "Spotify" "Nubank · crédito · 08:00" "- R\$ 21,90" Assinaturas
  divider "$c"
  tx_item "$c" Transfer Confirmed "Inter → Mercado Pago" "Transferência entre contas · 08:02" "R\$ 500,00" Transferência
  day_header "$B" "Seg · 20 out" "- R\$ 118,00"
  card "$B" "Segunda"; c=$R
  tx_item "$c" Expense Manual "iFood" "Itaú · débito · 21:44" "- R\$ 63,80" Alimentação
  divider "$c"
  tx_item "$c" Expense Confirmed "Farmácia Pague Menos" "Mercado Pago · crédito · 18:12" "- R\$ 54,20" Saúde
}

screen tx "$H" 1 "1 · Padrão" "T-03.1 Lançamentos — padrão"
appbar "$SC" Large "Lançamentos"
body_hug "$SC" 12 20 0 24; B=$R
filter_row "$B"; link "$FILTERBTN" tx_filters up 250; link "$SEARCH" tx_search dissolve 150
chips_row "$B" "Menu|Outubro|Selected" "Toggle|Despesas|Unselected" "Menu|Contas|Unselected" "Menu|Categoria|Unselected" "Toggle|Pendentes|Unselected"
tx_days "$B"; link "$TXFIRST" det
bottom_nav "$SC" tx Lançamentos

screen tx_search "$H" 2 "2 · Busca" "T-03.2 Lançamentos — busca"
appbar "$SC" Large "Lançamentos"
body "$SC" 12 20 0 24; B=$R
filter_row "$B" Typing "ifood"
txt "$B" "2 resultados para “ifood”" "Body Small" onSurfaceVariant
card "$B" "Resultados"; c=$R
tx_item "$c" Expense Manual "iFood" "Itaú · débito · seg, 20 out" "- R\$ 63,80" Alimentação; link "$R" det_manual
divider "$c"
tx_item "$c" Expense Confirmed "iFood *Restaurante" "Nubank · crédito · sex, 17 out" "- R\$ 38,50" Alimentação
spacer "$B"
bottom_nav "$SC" tx_search Lançamentos

sheet_screen tx_filters "$H" 3 "3 · Filtros (sheet)" "T-03.3 Lançamentos — filtros"
BH=$R
appbar "$BH" Large "Lançamentos"
body "$BH" 12 20 0 24; B=$R
filter_row "$B"
chips_row "$B" "Menu|Outubro|Selected" "Toggle|Despesas|Selected" "Menu|Contas|Selected" "Menu|Categoria|Selected"
tx_days "$B"
scrim_over "$BH"
inst "$SC" BottomSheet; SHEET=$R
props_of "$SHEET" '{"Title":"Filtros","Subtitle":"Outubro 2026 · 3 filtros ativos"}'
slot_first "$SHEET" SheetLabel; props_of "$R" '{"Text":"Tipo"}'
sheet_chip "$SHEET" "Despesas" Selected; sheet_chip "$SHEET" "Receitas"; sheet_chip "$SHEET" "Transferências"
sheet_label "$SHEET" "Conta ou instrumento"
sheet_chip "$SHEET" "Mercado Pago" Selected; sheet_chip "$SHEET" "Nubank crédito"; sheet_chip "$SHEET" "Inter"; sheet_chip "$SHEET" "Itaú débito"; sheet_chip "$SHEET" "Caju VR"
sheet_label "$SHEET" "Categoria"
sheet_chip "$SHEET" "Saúde" Selected; sheet_chip "$SHEET" "Alimentação"; sheet_chip "$SHEET" "Mercado"; sheet_chip "$SHEET" "Transporte"; sheet_chip "$SHEET" "Lazer"
sheet_label "$SHEET" "Status"
sheet_chip "$SHEET" "Pendentes"; sheet_chip "$SHEET" "Confirmados"; sheet_chip "$SHEET" "Manuais"
link "$SHEET" tx_empty dissolve 150

screen tx_empty "$H" 4 "4 · Sem resultado" "T-03.4 Lançamentos — nada com esses filtros"
appbar "$SC" Large "Lançamentos"
body "$SC" 12 20 0 24; B=$R
filter_row "$B"
chips_row "$B" "Menu|Outubro|Selected" "Toggle|Despesas|Selected" "Menu|Mercado Pago|Selected" "Menu|Saúde|Selected"
spacer "$B"
empty_state "$B" NoResults; link "$R" tx dissolve 150
spacer "$B"
bottom_nav "$SC" tx_empty Lançamentos

screen tx_swipe "$H" 5 "5 · Deslizar e desfazer" "T-03.5 Lançamentos — deslizar e desfazer"
appbar "$SC" Large "Lançamentos"
body_hug "$SC" 12 20 0 24; B=$R
filter_row "$B"
chips_row "$B" "Menu|Outubro|Selected" "Toggle|Despesas|Unselected" "Menu|Contas|Unselected" "Menu|Categoria|Unselected"
tx_days "$B" swipe
inst "$B" Snackbar; props_of "$R" '{"Kind":"Undo","Message":"Lançamento excluído"}'; fillw "$R"; link "$R" tx dissolve 150
bottom_nav "$SC" tx_swipe Lançamentos

# ---------------- T-04 Contas ----------------
H="05 T-04 · Contas"

screen acc "$H" 1 "1 · Padrão" "T-04.1 Contas — padrão"
appbar "$SC" Large "Contas"
body_hug "$SC" 12 20 0 24; B=$R
section "$B" "Nubank"
invoice "$B" Open Nubank "Nubank · crédito" "R\$ 1.284,50" "Fecha dia 3 · vence dia 10" "Vence em 19 dias"; link "$R" acc_invoice
debit "$B" "Nubank · conta" "R\$ 1.020,00" Nubank
section "$B" "Inter"
invoice "$B" Closed Inter "Inter · crédito" "R\$ 642,10" "Fechou dia 20 · pagar até 27/10" "Vence em 5 dias"
debit "$B" "Inter · débito" "R\$ 380,00" Inter
section "$B" "Itaú"
debit "$B" "Itaú · débito" "R\$ 2.140,00" Itaú
section "$B" "Mercado Pago"
invoice "$B" Open "Mercado Pago" "Mercado Pago · crédito" "R\$ 312,40" "Fecha dia 12 · vence dia 19" "Vence em 28 dias"
section "$B" "Caju · benefícios"
voucher "$B" "VR · Refeição" "R\$ 486,20" "Última recarga 01/10 · R\$ 800,00"
voucher "$B" "VA · Alimentação" "R\$ 214,70" "Última recarga 01/10 · R\$ 450,00"
button "$B" Outline Medium "Adicionar conta" true wide; link "$R" add_inst
bottom_nav "$SC" acc Contas

screen acc_invoice "$H" 2 "2 · Faturas do cartão" "T-04.2 Contas — faturas do cartão"
appbar "$SC" Small "Nubank · crédito"; link "$R" back
body_hug "$SC" 16 20 4 24; B=$R
inst "$B" Tabs; props_of "$R" '{"Selected":"Aberta"}'; fillw "$R"
pad_card "$B" "Resumo" 20 12; PC=$R
txt "$PC" "Fatura de novembro" Overline onSurfaceVariant
txt "$PC" "R\$ 1.284,50" "Amount Large" brand
inst "$PC" LinearProgress; props_of "$R" '{"Value":"40"}'; fillw "$R"
hrow "$PC" "Dates" 8; DR=$R; set_props "$DR" '{"mainAlign":"spaceBetween"}'
txt "$DR" "Fecha dia 3 · vence dia 10" Caption onSurfaceVariant
txt "$DR" "Vence em 19 dias" "Label Small" primary
day_header "$B" "Compras desta fatura" "17 lançamentos"
card "$B" "Compras"; c=$R
tx_item "$c" Expense Confirmed "Padaria do Zé" "22 out · 14:32" "- R\$ 42,90" Alimentação; link "$R" det
divider "$c"
tx_item "$c" Expense Pending "Uber *Trip" "22 out · 12:05" "- R\$ 18,90" Transporte
divider "$c"
tx_item "$c" Expense Confirmed "Spotify" "21 out · 08:00" "- R\$ 21,90" Assinaturas
divider "$c"
tx_item "$c" Expense Confirmed "Magazine Luiza" "18 out · 20:41" "- R\$ 349,90" Outros
divider "$c"
tx_item "$c" Expense Confirmed "Cinemark" "17 out · 19:05" "- R\$ 58,00" Lazer
divider "$c"
tx_item "$c" Expense Confirmed "iFood *Restaurante" "17 out · 12:58" "- R\$ 38,50" Alimentação

# ---------------- T-05 Relatórios ----------------
H="06 T-05 · Relatórios"

screen rep "$H" 1 "1 · Padrão" "T-05.1 Relatórios — padrão"
appbar "$SC" Large "Relatórios"
body_hug "$SC" 16 20 0 24; B=$R
month_sel "$B"
section "$B" "Despesas por categoria"
inst "$B" DonutChart; fillw "$R"; link "$R" tx
section "$B" "Comparado a setembro"
card "$B" "Comparação"; c=$R
inst "$c" ComparisonRow; props_of "$R" '{"Trend":"Up"}'; props_of "$R" '{"Category":"Alimentação","Delta":"12%"}'; fillw "$R"; link "$R" tx
divider "$c"
inst "$c" ComparisonRow; props_of "$R" '{"Trend":"Down"}'; props_of "$R" '{"Category":"Transporte","Delta":"8%"}'; fillw "$R"; link "$R" tx
section "$B" "Por categoria"
card "$B" "Categorias"; c=$R
catrow "$c" 30 Alimentação "Alimentação" "32% · ↑ 12% vs set" "R\$ 1.190,00"; link "$R" tx
divider "$c"
catrow "$c" 20 Mercado "Mercado" "21% · ↓ 3% vs set" "R\$ 781,00"; link "$R" tx
divider "$c"
catrow "$c" 20 Transporte "Transporte" "17% · ↓ 8% vs set" "R\$ 632,00"; link "$R" tx
divider "$c"
catrow "$c" 10 Assinaturas "Assinaturas" "12% · = set" "R\$ 446,00"; link "$R" tx
section "$B" "Receitas × despesas"
inst "$B" IncomeExpenseBars; fillw "$R"
section "$B" "Assinaturas"
pad_card "$B" "Assinaturas" 0 0; PC=$R
hrow "$PC" "Total" 8; TT=$R; set_props "$TT" '{"padding":[16,16,12,16],"mainAlign":"spaceBetween"}'
txt "$TT" "Total mensal estimado" "Body Small" onSurfaceVariant
txt "$TT" "R\$ 87,30" Amount brand
for sub in "Spotify|Próxima cobrança ≈ 12/11 · Nubank|R\$ 21,90" "Netflix|Próxima cobrança ≈ 18/11 · Inter|R\$ 44,90" "iCloud+|Próxima cobrança ≈ 05/11 · Itaú|R\$ 9,90" "YouTube Music|Próxima cobrança ≈ 27/10 · Nubank|R\$ 10,60"; do
  IFS='|' read -r t d a <<< "$sub"
  divider "$PC"
  inst "$PC" SubscriptionRow; props_of "$R" "$(jq -cn --arg t "$t" --arg d "$d" --arg a "$a" '{Title:$t, Detail:$d, Amount:$a}')"; fillw "$R"
done
bottom_nav "$SC" rep Relatórios

screen rep_empty "$H" 2 "2 · Sem dados" "T-05.2 Relatórios — sem dados"
appbar "$SC" Large "Relatórios"
body "$SC" 16 20 0 24; B=$R
month_sel "$B" "Novembro 2026"
spacer "$B"
empty_state "$B" NoData "Sem dados neste mês" "Os relatórios aparecem quando houver lançamentos no período."
spacer "$B"
bottom_nav "$SC" rep_empty Relatórios
