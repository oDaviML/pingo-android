review_card() {
  local parent="$1" reason="$2" title="$3" amount="$4" excerpt="$5" cat="$6"
  inst "$parent" ReviewCard
  local c=$R
  local rl
  case "$reason" in LowConfidence) rl="Baixa confiança · 62%" ;; Duplicate) rl="Possível duplicata" ;; Transfer) rl="Transferência ambígua" ;; NewMerchant) rl="Estabelecimento novo" ;; esac
  props_of "$c" "$(jq -cn --arg r "$reason" '{Reason:$r}')"
  props_of "$c" "$(jq -cn --arg l "$rl" '{"Reason label":$l}')"
  props_of "$c" "$(jq -cn --arg t "$title" --arg a "$amount" --arg e "“$excerpt”" --arg k "CategoryAvatar/Category=$cat" '{Title:$t, Amount:$a, Excerpt:$e, Category:{"@ref":$k}}')"
  fillw "$c"
  R=$c
}

# ---------------- T-06 Revisão ----------------
H="07 T-06 · Revisão de pendentes"

rev_header() {
  hrow "$1" "Header" 12; local h=$R
  vcol "$h" "Text" 2; local t=$R; fillw "$t"
  txt "$t" "3 para revisar" Title onBackground
  txt "$t" "O Pingo não teve certeza destes." "Body Small" onSurfaceVariant
  button "$h" Tonal Small "Confirmar todos" false
  REV_ALL=$R
}

screen rev "$H" 1 "1 · Padrão" "T-06.1 Revisão — padrão"
appbar "$SC" Small "Revisão de pendentes"; link "$R" home dissolve 150
body_hug "$SC" 16 20 4 24; B=$R
rev_header "$B"; link "$REV_ALL" rev_empty dissolve 150
review_card "$B" LowConfidence "Padaria do Zé" "- R\$ 42,90" "Compra aprovada R\$ 42,90 PADARIA DO ZE" Alimentação; link "$R" rev_sheet up 250
review_card "$B" Duplicate "Mercado do Bairro" "- R\$ 80,00" "Pix enviado R\$ 80,00 para Mercado do Bairro" Mercado
review_card "$B" NewMerchant "ZPT*LOJA2231" "- R\$ 19,90" "Compra no débito R\$ 19,90 ZPT*LOJA2231" Outros

sheet_screen rev_sheet "$H" 2 "2 · Trocar categoria" "T-06.2 Revisão — trocar categoria"
BH=$R
appbar "$BH" Small "Revisão de pendentes"
body "$BH" 16 20 4 24; B=$R
rev_header "$B"
review_card "$B" LowConfidence "Padaria do Zé" "- R\$ 42,90" "Compra aprovada R\$ 42,90 PADARIA DO ZE" Alimentação
scrim_over "$BH"
inst "$SC" BottomSheet; SHEET=$R
props_of "$SHEET" '{"Title":"Trocar categoria","Subtitle":"Padaria do Zé · R$ 42,90"}'
slot_first "$SHEET" "FilterChip/Kind=Toggle, State=Unselected"; props_of "$R" '{"Label":"Alimentação","State":"Selected"}'
for c in Mercado Transporte Moradia Saúde Lazer Assinaturas Educação Outros; do sheet_chip "$SHEET" "$c"; done
slot_add "$SHEET" "Checkbox/State=Unchecked"; props_of "$R" '{"State":"Checked","Label":"Sempre usar para este estabelecimento"}'; set_props "$R" '{"width":372}'
link "$SHEET" rev dissolve 150

screen rev_empty "$H" 3 "3 · Tudo em dia" "T-06.3 Revisão — tudo em dia"
appbar "$SC" Small "Revisão de pendentes"; link "$R" home dissolve 150
body "$SC" 16 20 4 24; B=$R
spacer "$B"
empty_state "$B" AllDone; link "$R" home dissolve 150
spacer "$B"

# ---------------- T-07 Novo lançamento ----------------
H="08 T-07 · Novo lançamento"

new_form() {
  local parent="$1" kind="$2" astate="$3" avalue="$4" asup="$5" place="$6"
  segmented "$parent" "$kind"; NEW_SEG=$R
  amount_field "$parent" "$astate" "$avalue" "$asup"; NEW_AMT=$R
  if [[ "$kind" == Transferência ]]; then
    select_field "$parent" "Origem" "Inter · conta" landmark
    select_field "$parent" "Destino" "Mercado Pago · conta" landmark
  else
    select_field "$parent" "Instrumento" "Nubank · crédito" credit-card
    select_field "$parent" "Categoria" "$place" tag
  fi
  select_field "$parent" "Data" "Hoje, 22 de outubro" calendar-days; NEW_DATE=$R
}

new_screen() {
  screen "$1" "$H" "$2" "$3" "$4"
  appbar "$SC" Modal "Novo lançamento"; link "$R" home up 250
  body_hug "$SC" 14 20 4 20; NB=$R
}

new_screen new 1 "1 · Vazio" "T-07.1 Novo lançamento — vazio"
new_form "$NB" Despesa Empty "0,00" "Digite no teclado abaixo" "Escolher categoria"; link "$NEW_DATE" new_date dissolve 150
field "$NB" Default "Estabelecimento ou descrição" "Opcional" " " store
inst "$NB" NumericKeypad; fillw "$R"

new_screen new_filled 2 "2 · Preenchido" "T-07.2 Novo lançamento — preenchido"
new_form "$NB" Despesa Filled "42,90" "Pronto para salvar" "Alimentação"
field "$NB" Filled "Estabelecimento ou descrição" "Padaria do Zé" "Categoria sugerida pelo estabelecimento" store
inst "$NB" NumericKeypad; fillw "$R"

new_screen new_transfer 3 "3 · Transferência" "T-07.3 Novo lançamento — transferência"
new_form "$NB" Transferência Filled "500,00" "Origem e destino precisam ser diferentes" ""
field "$NB" Default "Descrição" "Opcional" " " pencil
inst "$NB" NumericKeypad; fillw "$R"

new_screen new_error 4 "4 · Validação" "T-07.4 Novo lançamento — erro de validação"
segmented "$NB" Despesa
amount_field "$NB" Error "0,00" "O valor precisa ser maior que zero"
field "$NB" Error "Instrumento" "Escolha um instrumento" "Informe de onde saiu o dinheiro" credit-card
select_field "$NB" "Data" "Hoje, 22 de outubro" calendar-days
field "$NB" Default "Estabelecimento ou descrição" "Opcional" " " store
inst "$NB" NumericKeypad; fillw "$R"

sheet_screen new_date "$H" 5 "5 · Escolher data" "T-07.5 Novo lançamento — data"
BH=$R
appbar "$BH" Modal "Novo lançamento"
body "$BH" 14 20 4 20; B=$R
segmented "$B" Despesa
amount_field "$B" Filled "42,90" "Pronto para salvar"
select_field "$B" "Instrumento" "Nubank · crédito" credit-card
select_field "$B" "Data" "15 de outubro" calendar-days
overlay_inst "$BH" DatePickerDialog; link "$R" new_filled dissolve 150

# ---------------- T-08 Detalhe e edição ----------------
H="09 T-08 · Detalhe e edição"

origin_card() {
  pad_card "$1" "Origem" 18 12; local c=$R
  txt "$c" "Origem" Overline onSurfaceVariant
  if [[ "$2" == captured ]]; then
    hrow "$c" "App" 10; local r=$R
    inst "$r" InstitutionAvatar; props_of "$R" '{"Bank":"Nubank"}'
    vcol "$r" "Text" 0; local t=$R; fillw "$t"
    txt "$t" "Nubank" "Body Strong" onBackground
    txt "$t" "Lido hoje às 14:32" Caption onSurfaceVariant
    inst "$r" StatusTag; props_of "$R" '{"State":"Active"}'; props_of "$R" '{"Label":"Confiança 92%"}'
    txt "$c" "Texto interpretado (fica só neste aparelho)" "Label Small" onSurfaceVariant
    frame "$c" "Quote" '{"layout":"horizontal","padding":12,"cornerRadius":12,"sizingH":"fill","sizingV":"hug"}'
    paint "$R" fill surfaceVariant; paint "$R" stroke secondary; set_props "$R" '{"strokeLeftWeight":3,"strokeAlign":"inside"}'
    txt "$R" "“Compra aprovada no crédito: R\$ 42,90 em PADARIA DO ZE”" Caption onSurfaceVariant "$WRAP"
  else
    hrow "$c" "Manual" 10; local r=$R
    frame "$r" "Badge" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":12,"sizingH":"fixed","sizingV":"fixed"}' 40 40
    paint "$R" fill primaryContainer; iconi "$R" pencil
    vcol "$r" "Text" 0; local t=$R; fillw "$t"
    txt "$t" "Manual" "Body Strong" onBackground
    txt "$t" "Lançado por você em 20 de out, 21:44" Caption onSurfaceVariant
  fi
}

det_fields() {
  local parent="$1" mode="$2"
  amount_field "$parent" Filled "42,90" "Valor do lançamento"
  segmented "$parent" Despesa
  select_field "$parent" "Instrumento" "Nubank · crédito" credit-card
  select_field "$parent" "Categoria" "Alimentação" tag
  inst "$parent" Checkbox; props_of "$R" '{"State":"Unchecked","Label":"Aplicar a todos deste estabelecimento"}'; fillw "$R"
  select_field "$parent" "Data" "22 de outubro, 14:32" calendar-days
  field "$parent" Filled "Estabelecimento" "Padaria do Zé" " " store
}

det_screen() {
  screen "$1" "$H" "$2" "$3" "$4"
  appbar "$SC" Small "Detalhe do lançamento" action; link "$R" tx dissolve 150
  body_hug "$SC" 14 20 4 24; DB=$R
}

det_screen det 1 "1 · Capturado" "T-08.1 Detalhe — capturado"
det_fields "$DB" captured
origin_card "$DB" captured
button "$DB" Primary Medium "Salvar alterações" false wide; link "$R" tx dissolve 150
button "$DB" Outline Medium "Marcar como transferência" false wide; link "$R" det_transfer dissolve 150
button "$DB" Text Medium "Excluir lançamento" false wide; link "$R" det_delete up 250

det_screen det_manual 2 "2 · Manual" "T-08.2 Detalhe — manual"
amount_field "$DB" Filled "63,80" "Valor do lançamento"
segmented "$DB" Despesa
select_field "$DB" "Instrumento" "Itaú · débito" credit-card
select_field "$DB" "Categoria" "Alimentação" tag
select_field "$DB" "Data" "20 de outubro, 21:44" calendar-days
field "$DB" Filled "Descrição" "iFood" " " pencil
origin_card "$DB" manual
button "$DB" Primary Medium "Salvar alterações" false wide
button "$DB" Text Medium "Excluir lançamento" false wide

det_screen det_transfer 3 "3 · Transferência" "T-08.3 Detalhe — transferência"
amount_field "$DB" Filled "500,00" "Valor transferido"
segmented "$DB" Transferência
select_field "$DB" "Origem" "Inter · conta" landmark
select_field "$DB" "Destino" "Mercado Pago · conta" landmark
select_field "$DB" "Data" "21 de outubro, 08:02" calendar-days
pad_card "$DB" "Par" 0 0; PC=$R
frame "$PC" "Pair header" '{"layout":"horizontal","padding":[16,16,8,16],"sizingH":"fill","sizingV":"hug","fills":[]}'
txt "$R" "Par da transferência" Overline onSurfaceVariant
tx_item "$PC" Transfer Confirmed "Entrada em Mercado Pago" "Mercado Pago · conta · 08:02" "R\$ 500,00" Transferência
banner "$DB" Info "Duas notificações, um lançamento" "O Pingo juntou a saída do Inter com a entrada no Mercado Pago."
button "$DB" Primary Medium "Salvar alterações" false wide
button "$DB" Outline Medium "Desfazer transferência" false wide; link "$R" det dissolve 150

sheet_screen det_delete "$H" 4 "4 · Excluir" "T-08.4 Detalhe — confirmar exclusão"
BH=$R
appbar "$BH" Small "Detalhe do lançamento" action
body "$BH" 14 20 4 24; B=$R
amount_field "$B" Filled "42,90" "Valor do lançamento"
segmented "$B" Despesa
select_field "$B" "Instrumento" "Nubank · crédito" credit-card
select_field "$B" "Categoria" "Alimentação" tag
dialog_screen "$BH" Destructive "Excluir lançamento?" "Padaria do Zé, R\$ 42,90. Você pode desfazer logo depois."; link "$R" tx_swipe dissolve 150
