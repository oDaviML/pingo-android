radio_row() {
  local parent="$1" bank="$2" sub="$3" state="$4"
  frame "$parent" "Institution row" '{"layout":"horizontal","gap":12,"padding":[12,16,12,16],"crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
  local r=$R
  inst "$r" InstitutionAvatar; props_of "$R" "$(jq -cn --arg b "$bank" '{Bank:$b}')"
  vcol "$r" "Text" 0; local t=$R; fillw "$t"
  txt "$t" "$bank" "Body Strong" onBackground
  txt "$t" "$sub" Caption onSurfaceVariant
  inst "$r" Radio; props_of "$R" "$(jq -cn --arg s "$state" '{State:$s, Label:" "}')"; set_props "$R" '{"width":28}'
  R=$r
}

stat_tile() {
  inst "$1" StatTile
  local t=$R
  props_of "$t" "$(jq -cn --arg k "$2" '{Tone:$k}')"
  props_of "$t" "$(jq -cn --arg v "$3" --arg l "$4" '{Value:$v, Label:$l}')"
  fillw "$t"
  R=$t
}

# ---------------- T-09 Cadastro ----------------
H="10 T-09 · Cadastro de conta"

add_screen() {
  screen "$1" "$H" "$2" "$3" "$4"
  appbar "$SC" Modal "$5"; link "$R" acc up 250
  body_hug "$SC" 14 20 4 24; AB=$R
}

add_screen add_inst 1 "1 · Instituição" "T-09.1 Cadastro — instituição" "Nova conta"
txt "$AB" "Qual é a instituição?" Headline onBackground
txt "$AB" "Os 5 bancos suportados leem as notificações sozinhos. Outra serve para lançamentos manuais." Body onSurfaceVariant "$WRAP"
card "$AB" "Instituições"; IC=$R
radio_row "$IC" Nubank "Cartão de crédito e conta" Selected; divider "$IC"
radio_row "$IC" Inter "Cartão de crédito e conta" Unselected; divider "$IC"
radio_row "$IC" Itaú "Conta-salário e débito" Unselected; divider "$IC"
radio_row "$IC" "Mercado Pago" "Compras online" Unselected; divider "$IC"
radio_row "$IC" Caju "Benefícios VR, VA e VT" Unselected; divider "$IC"
radio_row "$IC" Outra "Só lançamentos manuais" Unselected
field "$AB" Filled "Apelido da conta" "Nubank pessoal" "Como aparece nas listas" pencil
txt "$AB" "Tipo da conta" Overline onSurfaceVariant
hrow "$AB" "Tipos" 8; TP=$R
inst "$TP" FilterChip; props_of "$R" '{"Kind":"Toggle","Label":"Conta corrente","State":"Selected"}'
inst "$TP" FilterChip; props_of "$R" '{"Kind":"Toggle","Label":"Benefício","State":"Unselected"}'
button "$AB" Primary Medium "Continuar" false wide; link "$R" add_credit

add_screen add_credit 2 "2 · Cartão de crédito" "T-09.2 Cadastro — cartão de crédito" "Nubank pessoal"
hrow "$AB" "Header" 12; HD=$R
inst "$HD" InstitutionAvatar; props_of "$R" '{"Bank":"Nubank"}'
vcol "$HD" "Text" 0; HT=$R
txt "$HT" "Nubank pessoal" Title onBackground
txt "$HT" "Conta corrente" "Body Small" onSurfaceVariant
txt "$AB" "Instrumentos" Overline onSurfaceVariant
hrow "$AB" "Tipos" 8; TP=$R
inst "$TP" FilterChip; props_of "$R" '{"Kind":"Toggle","Label":"Crédito","State":"Selected"}'
inst "$TP" FilterChip; props_of "$R" '{"Kind":"Toggle","Label":"Débito","State":"Unselected"}'
inst "$TP" FilterChip; props_of "$R" '{"Kind":"Toggle","Label":"Voucher","State":"Unselected"}'; link "$R" add_voucher dissolve 150
field "$AB" Filled "Apelido do cartão" "Nubank Ultravioleta" " " credit-card
field "$AB" Default "Últimos 4 dígitos (opcional)" "4321" " " lock
hrow "$AB" "Dias" 12; DY=$R
select_field "$DY" "Fechamento" "Dia 3" "" ; link "$R" add_day dissolve 150
select_field "$DY" "Vencimento" "Dia 10" ""
button "$AB" Outline Medium "Adicionar outro instrumento" true wide
button "$AB" Primary Medium "Salvar conta" false wide; link "$R" acc dissolve 150
button "$AB" Text Medium "Excluir conta" false wide; link "$R" add_archive up 250

sheet_screen add_day "$H" 3 "3 · Dia do mês" "T-09.3 Cadastro — dia de fechamento"
BH=$R
appbar "$BH" Modal "Nubank pessoal"
body "$BH" 14 20 4 24; B=$R
field "$B" Filled "Apelido do cartão" "Nubank Ultravioleta" " " credit-card
hrow "$B" "Dias" 12; DY=$R
select_field "$DY" "Fechamento" "Dia 3" ""
select_field "$DY" "Vencimento" "Dia 10" ""
overlay_inst "$BH" DayOfMonthPicker; link "$R" add_credit dissolve 150

add_screen add_voucher 4 "4 · Voucher" "T-09.4 Cadastro — voucher" "Caju"
hrow "$AB" "Header" 12; HD=$R
inst "$HD" InstitutionAvatar; props_of "$R" '{"Bank":"Caju"}'
vcol "$HD" "Text" 0; HT=$R
txt "$HT" "Caju" Title onBackground
txt "$HT" "Conta benefício" "Body Small" onSurfaceVariant
hrow "$AB" "Tipos" 8; TP=$R
inst "$TP" FilterChip; props_of "$R" '{"Kind":"Toggle","Label":"Crédito","State":"Unselected"}'
inst "$TP" FilterChip; props_of "$R" '{"Kind":"Toggle","Label":"Débito","State":"Unselected"}'
inst "$TP" FilterChip; props_of "$R" '{"Kind":"Toggle","Label":"Voucher","State":"Selected"}'
field "$AB" Filled "Apelido do voucher" "Caju VR" " " ticket
txt "$AB" "Tipo de benefício" Overline onSurfaceVariant
pad_card "$AB" "Tipo" 16 12; TC=$R
inst "$TC" Radio; props_of "$R" '{"State":"Selected","Label":"VR · Vale-refeição"}'; fillw "$R"
inst "$TC" Radio; props_of "$R" '{"State":"Unselected","Label":"VA · Vale-alimentação"}'; fillw "$R"
inst "$TC" Radio; props_of "$R" '{"State":"Unselected","Label":"VT · Vale-transporte"}'; fillw "$R"

amount_field "$AB" Filled "486,20" "Saldo inicial"
button "$AB" Primary Medium "Salvar conta" false wide; link "$R" acc dissolve 150

add_screen add_error 5 "5 · Validação" "T-09.5 Cadastro — erro de validação" "Nubank pessoal"
field "$AB" Filled "Apelido do cartão" "Nubank Ultravioleta" " " credit-card
hrow "$AB" "Dias" 12; DY=$R
field "$DY" Filled "Fechamento (1 a 31)" "3" " "
field "$DY" Error "Vencimento (1 a 31)" "32" "Use um dia de 1 a 31"
banner "$AB" Warning "Revise os dias" "Fechamento e vencimento precisam estar entre 1 e 31."
button "$AB" Primary Medium "Salvar conta" false wide Disabled

dialog_beh_screen() {
  sheet_screen "$1" "$H" "$2" "$3" "$4"
  BH=$R
  appbar "$BH" Modal "Nubank pessoal"
  body "$BH" 14 20 4 24; B=$R
  field "$B" Filled "Apelido do cartão" "Nubank Ultravioleta" " " credit-card
  hrow "$B" "Dias" 12; DY=$R
  select_field "$DY" "Fechamento" "Dia 3" ""
  select_field "$DY" "Vencimento" "Dia 10" ""
}

dialog_beh_screen add_archive 6 "6 · Arquivar conta" "T-09.6 Cadastro — arquivar conta com lançamentos"
dialog_screen "$BH" Archive "Arquivar esta conta?" "Ela tem 38 lançamentos. Sai das listas, mas o histórico continua nos relatórios."; link "$R" acc dissolve 150

# ---------------- T-10 Status da captura ----------------
H="11 T-10 · Status da captura"

last_read() {
  card "$1" "Última leitura"; local c=$R
  local item
  for item in "Nubank|há 3 min" "Inter|há 2 h" "Itaú|ontem, 19:20" "Mercado Pago|há 3 dias" "Caju|há 12 dias"; do
    frame "$c" "App row" '{"layout":"horizontal","gap":12,"padding":[12,16,12,16],"crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
    local r=$R
    inst "$r" InstitutionAvatar; props_of "$R" "$(jq -cn --arg b "${item%|*}" '{Bank:$b}')"
    txt "$r" "${item%|*}" "Body Strong" onBackground '{"props":{"sizingH":"fill"}}'
    txt "$r" "${item#*|}" "Body Small" onSurfaceVariant
  done
}

cap_counters() {
  hrow "$1" "Counters 1" 12; local a=$R
  stat_tile "$a" Recognized "$2" "Reconhecidas"
  stat_tile "$a" Review "$3" "Para revisão"
  hrow "$1" "Counters 2" 12; a=$R
  stat_tile "$a" Ignored "$4" "Ignoradas"
  stat_tile "$a" Unknown "$5" "Não entendidas"
}

screen cap_on "$H" 1 "1 · Ativa" "T-10.1 Status da captura — ativa"
appbar "$SC" Small "Status da captura"; link "$R" set dissolve 150
body_hug "$SC" 16 20 4 24; B=$R
pad_card "$B" "Estado" 18 12; PC=$R
hrow "$PC" "Estado" 8; ST=$R
inst "$ST" StatusTag; props_of "$R" '{"State":"Active"}'
txt "$PC" "Lendo as notificações dos seus apps financeiros" Subtitle onBackground "$WRAP"
txt "$PC" "Última leitura há 3 minutos, do Nubank." "Body Small" onSurfaceVariant
section "$B" "Neste mês"
cap_counters "$B" 184 12 37 3
section "$B" "Última notificação por app"
last_read "$B"
section "$B" "Permissões"
perm "$B" Granted "Acesso a notificações" "Ativo — lendo notificações dos bancos"
perm "$B" Granted "Sem restrição de bateria" "O Android não pausa a leitura"
perm "$B" Granted "Início automático" "Retoma a captura ao ligar o celular"
pad_card "$B" "Regras" 16 12; PC=$R
hrow "$PC" "Regras" 12; RR=$R
vcol "$RR" "Text" 2; RT=$R; fillw "$RT"
txt "$RT" "Regras do parser" "Body Strong" onBackground
txt "$RT" "Versão 2026.10.3 · atualizadas há 2 dias" Caption onSurfaceVariant "$WRAP"
button "$RR" Outline Small "Atualizar regras" false
button "$B" Outline Medium "Reconectar" false wide

screen cap_off "$H" 2 "2 · Parada" "T-10.2 Status da captura — parada"
appbar "$SC" Small "Status da captura"; link "$R" home dissolve 150
body_hug "$SC" 16 20 4 24; B=$R
pad_card "$B" "Estado" 18 12; PC=$R
hrow "$PC" "Estado" 8; ST=$R
inst "$ST" StatusTag; props_of "$R" '{"State":"Stopped"}'; props_of "$R" '{"Label":"Captura parada"}'
txt "$PC" "Sem leitura desde 08:14" Subtitle onBackground
txt "$PC" "Compras feitas agora podem não aparecer. Corrija as permissões abaixo." "Body Small" onSurfaceVariant "$WRAP"
banner "$B" Warning "O Android encerrou o serviço" "Isso costuma acontecer por economia de bateria. Toque em Reconectar."
section "$B" "Permissões"
perm "$B" Granted "Acesso a notificações" "Ativo, mas o serviço está parado"
perm "$B" Denied "Restrição de bateria" "O Android está limitando o Pingo"
perm "$B" Pending "Início automático" "Ative para retomar após reiniciar"
section "$B" "Neste mês"
cap_counters "$B" 184 12 37 3
button "$B" Primary Medium "Corrigir permissão" false wide
button "$B" Outline Medium "Reconectar" false wide; link "$R" cap_on dissolve 150

# ---------------- T-11 Configurações ----------------
H="12 T-11 · Configurações"

settings_body() {
  local B="$1"
  pad_card "$B" "Conta" 16 12; local PC=$R
  hrow "$PC" "Account" 12; local AR=$R
  frame "$AR" "Avatar" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 48 48
  paint "$R" fill primaryContainer
  txt "$R" "L" Title onPrimaryContainer
  vcol "$AR" "Text" 0; local t=$R; fillw "$t"
  txt "$t" "Lucas Martins" "Body Strong" onBackground
  txt "$t" "lucas.martins@gmail.com" Caption onSurfaceVariant
  button "$AR" Text Small "Sair" false
  section "$B" "Segurança"
  card "$B" "Segurança"; local c=$R
  settings_row "$c" Switch "Biometria" "Pedir ao abrir o app" fingerprint
  divider "$c"
  settings_row "$c" Value "Bloquear após" "" lock; SET_LOCK=$R
  divider "$c"
  settings_row "$c" Navigation "Categorias e regras aprendidas" "14 regras" tag; SET_RULES=$R
  section "$B" "Notificações do Pingo"
  card "$B" "Notificações"; c=$R
  settings_row "$c" Switch "Revisar pela notificação" "Confirmar compras sem abrir o app" bell
  divider "$c"
  settings_row "$c" Switch "Alerta de captura parada" "Avisar se o Pingo parar de ler" triangle-alert
  divider "$c"
  settings_row "$c" SwitchOff "Aviso antes de assinatura" "1 dia antes da cobrança estimada" repeat
  section "$B" "Privacidade e dados"
  card "$B" "Privacidade"; c=$R
  settings_row "$c" SwitchOff "Telemetria (opcional)" "Ajuda a melhorar a captura. Nunca envia o texto das notificações." chart-pie
  divider "$c"
  settings_row "$c" Navigation "Termo de privacidade" "Como o Pingo trata seus dados" file-text
  divider "$c"
  settings_row "$c" Danger "Excluir conta e dados" "Apaga tudo no aparelho e na nuvem"; SET_DEL=$R
  section "$B" "Sobre"
  pad_card "$B" "Sobre" 16 12; PC=$R
  txt "$PC" "Pingo 0.1.0 (build 12)" "Body Strong" onBackground
  txt "$PC" "Distribuição direta por APK, fora da Play Store." Caption onSurfaceVariant
  button "$PC" Outline Medium "Procurar atualização do APK" true wide
}

screen set "$H" 1 "1 · Padrão" "T-11.1 Configurações — padrão"
appbar "$SC" Small "Configurações"; link "$R" home dissolve 150
body_hug "$SC" 16 20 4 24; B=$R
settings_body "$B"
link "$SET_LOCK" set_lock up 250
link "$SET_RULES" set_rules
link "$SET_DEL" set_delete up 250

sheet_screen set_lock "$H" 2 "2 · Bloquear após" "T-11.2 Configurações — tempo para bloquear"
BH=$R
appbar "$BH" Small "Configurações"
body "$BH" 16 20 4 24; B=$R
settings_body "$B"
scrim_over "$BH"
inst "$SC" BottomSheet; SHEET=$R
props_of "$SHEET" '{"Title":"Bloquear após","Subtitle":"Tempo fora do app antes de pedir biometria ou PIN"}'
slot_first "$SHEET" "Radio/State=Unselected"; props_of "$R" '{"State":"Unselected","Label":"Imediatamente"}'; set_props "$R" '{"width":372}'
for opt in "1 minuto|Selected" "5 minutos|Unselected" "15 minutos|Unselected" "Nunca (não recomendado)|Unselected"; do
  slot_add "$SHEET" "Radio/State=Unselected"; props_of "$R" "$(jq -cn --arg l "${opt%|*}" --arg s "${opt#*|}" '{Label:$l, State:$s}')"; set_props "$R" '{"width":372}'
done
link "$SHEET" set dissolve 150

screen set_rules "$H" 3 "3 · Categorias e regras" "T-11.3 Configurações — categorias e regras"
appbar "$SC" Small "Categorias e regras"; link "$R" set dissolve 150
body_hug "$SC" 16 20 4 24; B=$R
banner "$B" Info "14 regras aprendidas" "Quando você troca a categoria de um estabelecimento, o Pingo lembra da escolha."
card "$B" "Regras"; RC=$R
for rule in "Padaria do Zé|Alimentação|Sempre Alimentação · 8 lançamentos" "Uber|Transporte|Sempre Transporte · 14 lançamentos" "Supermercado Dia|Mercado|Sempre Mercado · 6 lançamentos" "Farmácia Pague Menos|Saúde|Sempre Saúde · 3 lançamentos" "Spotify|Assinaturas|Sempre Assinaturas · 5 lançamentos"; do
  IFS='|' read -r n k d <<< "$rule"
  frame "$RC" "Rule row" '{"layout":"horizontal","gap":12,"padding":[10,8,10,16],"crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
  RW=$R
  inst "$RW" CategoryAvatar; props_of "$R" "$(jq -cn --arg c "$k" '{Category:$c}')"
  vcol "$RW" "Text" 0; RT=$R; fillw "$RT"
  txt "$RT" "$n" "Body Strong" onBackground
  txt "$RT" "$d" Caption onSurfaceVariant
  frame "$RW" "Delete" '{"layout":"horizontal","padding":10,"cornerRadius":12,"sizingH":"hug","sizingV":"hug","fills":[]}'
  iconi "$R" trash-2
  divider "$RC"
done
section "$B" "Categorias"
hrow "$B" "Categorias" 8; CH=$R; set_props "$CH" '{"wrap":true,"crossGap":8}'
for c in Alimentação Mercado Transporte Moradia Saúde Lazer Assinaturas Educação Outros; do
  inst "$CH" FilterChip; props_of "$R" "$(jq -cn --arg l "$c" '{Kind:"Toggle", Label:$l, State:"Unselected"}')"
done

sheet_screen set_delete "$H" 4 "4 · Excluir conta" "T-11.4 Configurações — excluir conta e dados"
BH=$R
appbar "$BH" Small "Configurações"
body "$BH" 16 20 4 24; B=$R
settings_body "$B"
dialog_screen "$BH" Destructive "Excluir conta e dados?" "Isso apaga tudo no celular e na nuvem e não dá para desfazer."; link "$R" set dissolve 150
