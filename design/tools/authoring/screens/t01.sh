CENTER='{"props":{"textAlign":"center","sizingH":"fill","textAutoResize":"height"}}'
WRAP='{"props":{"sizingH":"fill","textAutoResize":"height"}}'

hero() {
  frame "$1" "Hero" '{"layout":"vertical","gap":0,"mainAlign":"center","crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
  local h=$R
  inst "$h" "Logo/Mascote"
  step transform.scale "$(jq -cn --argjson r "$(ref "$R")" --argjson f "$2" '{ids:[$r], factor:$f}')"
  R=$h
}

actions() {
  frame "$1" "Actions" '{"layout":"vertical","gap":8,"sizingH":"fill","sizingV":"hug","fills":[]}'
}

onb() {
  local key="$1" stepn="$2" order="$3" cap="$4" name="$5"
  screen "$key" "01 T-01 · Onboarding" "$order" "$cap" "$name"
  frame "$SC" "Stepper" '{"layout":"horizontal","padding":[8,20,0,20],"sizingH":"fill","sizingV":"hug","fills":[]}'
  inst "$R" OnboardingStepper
  props_of "$R" "$(jq -cn --arg s "$stepn" '{Step:$s}')"
  fillw "$R"
  body "$SC" 16 24 24 24
  OB=$R
}

icon_row() {
  local parent="$1" ic="$2" title="$3" desc="$4"
  hrow "$parent" "Row" 14
  local r=$R
  set_props "$r" '{"crossAlign":"min"}'
  frame "$r" "Badge" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":12,"sizingH":"fixed","sizingV":"fixed"}' 40 40
  paint "$R" fill primaryContainer
  iconi "$R" "$ic"
  vcol "$r" "Text" 2
  local t=$R
  txt "$t" "$title" "Body Strong" onBackground
  txt "$t" "$desc" "Body Small" onSurfaceVariant "$WRAP"
}

num_row() {
  local parent="$1" n="$2" text="$3"
  hrow "$parent" "Step" 12
  local r=$R
  set_props "$r" '{"crossAlign":"min"}'
  frame "$r" "Number" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 24 24
  paint "$R" fill primary
  txt "$R" "$n" "Label Small" onPrimary
  txt "$r" "$text" "Body Small" onBackground "$WRAP"
}

perm() {
  inst "$1" PermissionItem
  local p=$R
  props_of "$p" "$(jq -cn --arg s "$2" '{State:$s}')"
  props_of "$p" "$(jq -cn --arg t "$3" --arg d "$4" '{Title:$t, Description:$d}')"
  fillw "$p"
  R=$p
}

onb onb1 1 1 "1 · Boas-vindas" "T-01.1 Onboarding — boas-vindas"
spacer "$OB"
hero "$OB" 1.9
txt "$OB" "Seu dinheiro,\ngota a gota" Display onBackground "$CENTER"
txt "$OB" "O Pingo lê as notificações dos seus bancos e registra cada compra, Pix e recebimento sozinho. Sem digitar nada." Body onSurfaceVariant "$CENTER"
spacer "$OB"
actions "$OB"; AC=$R
button "$AC" Primary Medium "Começar" false wide; link "$R" onb2
button "$AC" Text Medium "Já tenho conta" false wide; link "$R" onb2

onb onb2 2 2 "2 · Login com Google" "T-01.2 Onboarding — login Google"
spacer "$OB"
hero "$OB" 1.2
txt "$OB" "Entre com sua conta Google" Headline onBackground "$CENTER"
txt "$OB" "Sua conta guarda e restaura seus dados se você trocar de celular." Body onSurfaceVariant "$CENTER"
frame "$OB" "Google button" '{"layout":"horizontal","gap":12,"padding":[14,20,14,20],"cornerRadius":12,"mainAlign":"center","crossAlign":"center","sizingH":"fill","sizingV":"hug","strokeWeight":1.5,"strokeAlign":"inside"}'
GB=$R; paint "$GB" fill surface; paint "$GB" stroke outline
txt "$GB" "G" Headline onBackground; set_props "$R" '{"fill":"#4285F4","fontStyle":"Black"}'
txt "$GB" "Continuar com Google" Label onBackground
link "$GB" onb3
spacer "$OB"
hrow "$OB" "Privacy hint" 8; PH=$R; set_props "$PH" '{"mainAlign":"center"}'
iconi "$PH" lock
txt "$PH" "Usamos a conta só para sincronizar e restaurar." Caption onSurfaceVariant

onb onb3 3 3 "3 · Termo e LGPD" "T-01.3 Onboarding — termo LGPD"
txt "$OB" "Antes de começar" Headline onBackground
txt "$OB" "O Pingo lê dados sensíveis. Veja com clareza o que acontece com eles." Body onSurfaceVariant "$WRAP"
pad_card "$OB" "O que o Pingo faz" 20 18; PC=$R
icon_row "$PC" shield-check "O que o Pingo lê" "Só notificações de Nubank, Inter, Itaú, Mercado Pago e Caju. Nada de mensagens, fotos ou contatos."
icon_row "$PC" smartphone "O texto fica no seu celular" "A leitura acontece no aparelho. Só dados estruturados são sincronizados, e só com seu consentimento."
icon_row "$PC" trash-2 "Você manda nos seus dados" "Exclua tudo, no aparelho e na nuvem, quando quiser."
inst "$OB" Checkbox; props_of "$R" '{"State":"Checked","Label":"Li e aceito o termo de privacidade (LGPD)"}'; fillw "$R"

button "$OB" Text Small "Ler termo completo" false
spacer "$OB"
actions "$OB"; AC=$R
button "$AC" Primary Medium "Aceitar e continuar" false wide; link "$R" onb4

onb onb4 4 4 "4 · Acesso às notificações" "T-01.4 Onboarding — acesso a notificações"
txt "$OB" "Libere o acesso às notificações" Headline onBackground
txt "$OB" "É assim que o Pingo vê suas compras. O Android pede essa permissão em Ajustes." Body onSurfaceVariant "$WRAP"
perm "$OB" Pending "Acesso a notificações" "Falta ativar o Pingo nos Ajustes do Android"
pad_card "$OB" "Como fazer" 20 14; PC=$R
txt "$PC" "Como fazer" Overline onSurfaceVariant
num_row "$PC" 1 "Toque em Abrir ajustes."
num_row "$PC" 2 "Procure Pingo na lista de apps com acesso."
num_row "$PC" 3 "Ative a chave e confirme o aviso do Android."
hrow "$PC" "Mock" 12; MK=$R
paint "$MK" fill surfaceVariant; set_props "$MK" '{"cornerRadius":14,"padding":[10,14,10,14]}'
frame "$MK" "Avatar" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 28 28; paint "$R" fill primary
txt "$MK" "Pingo" "Body Strong" onBackground '{"props":{"sizingH":"fill"}}'
inst "$MK" Switch; props_of "$R" '{"State":"Off"}'
spacer "$OB"
actions "$OB"; AC=$R
button "$AC" Primary Medium "Abrir ajustes" false wide; link "$R" onb5
button "$AC" Text Medium "Agora não" false wide; link "$R" onb4d

onb onb4d 4 5 "4b · Acesso negado" "T-01.4b Onboarding — acesso negado"
txt "$OB" "Tudo bem, você decide" Headline onBackground
banner "$OB" Error "Sem acesso às notificações" "O Pingo vai funcionar só no modo manual até você liberar."
perm "$OB" Denied "Acesso a notificações" "Sem isso o Pingo fica só no manual"
pad_card "$OB" "No modo manual" 20 14; PC=$R
txt "$PC" "No modo manual" Overline onSurfaceVariant
icon_row "$PC" plus "Você lança cada gasto" "Pelo botão + do app, em até 3 toques."
icon_row "$PC" bell-off "Sem captura nem revisão" "O Pingo não vê notificações dos bancos."
icon_row "$PC" settings "Dá para ativar depois" "Em Configurações › Status da captura."
spacer "$OB"
actions "$OB"; AC=$R
button "$AC" Primary Medium "Tentar de novo" false wide; link "$R" onb4
button "$AC" Outline Medium "Continuar só no manual" false wide; link "$R" onb5

onb onb5 5 6 "5 · Notificações do Pingo" "T-01.5 Onboarding — permissão para notificar"
txt "$OB" "Deixe o Pingo avisar você" Headline onBackground
txt "$OB" "Para confirmar uma compra na hora, sem abrir o app. Assim:" Body onSurfaceVariant "$WRAP"
pad_card "$OB" "Exemplo de notificação" 16 10; PC=$R
set_props "$PC" '{"cornerRadius":24}'
hrow "$PC" "Head" 8; HD=$R
frame "$HD" "Icon" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 20 20
paint "$R" fill primary
txt "$HD" "Pingo · agora" "Label Small" onSurfaceVariant
txt "$PC" "Revisar compra de R\$ 42,90?" "Body Strong" onBackground
txt "$PC" "Padaria do Zé · Nubank crédito. Alimentação?" "Body Small" onSurfaceVariant "$WRAP"
hrow "$PC" "Notif actions" 8; NA=$R
button "$NA" Text Small "Confirmar" false
button "$NA" Text Small "Trocar categoria" false
perm "$OB" Pending "Notificações do Pingo" "Para revisar compras pela notificação"
spacer "$OB"
actions "$OB"; AC=$R
button "$AC" Primary Medium "Permitir notificações" false wide; link "$R" onb6
button "$AC" Text Medium "Agora não" false wide; link "$R" onb6

onb onb6 6 7 "6 · Bateria e início automático" "T-01.6 Onboarding — bateria e autostart"
txt "$OB" "Mantenha a captura ligada" Headline onBackground
txt "$OB" "Alguns celulares fecham apps em segundo plano. Escolha o seu para ver o ajuste certo." Body onSurfaceVariant "$WRAP"
hrow "$OB" "Fabricantes" 8; FB=$R; set_props "$FB" '{"wrap":true}'
for m in "Samsung|Selected" "Xiaomi|Unselected" "Motorola|Unselected" "Outro|Unselected"; do
  inst "$FB" FilterChip; props_of "$R" "$(jq -cn --arg l "${m%|*}" --arg s "${m#*|}" '{Label:$l, State:$s}')"
done
pad_card "$OB" "Samsung" 20 14; PC=$R
txt "$PC" "Samsung · One UI" Overline onSurfaceVariant
num_row "$PC" 1 "Ajustes › Bateria › Limites de uso em segundo plano."
num_row "$PC" 2 "Adicione o Pingo em Apps nunca suspensos."
num_row "$PC" 3 "Em Apps › Pingo, deixe a bateria como Sem restrições."
perm "$OB" Pending "Sem restrição de bateria" "Evita que o Android pare a leitura"
perm "$OB" Pending "Início automático" "Retoma a captura ao ligar o celular"
spacer "$OB"
actions "$OB"; AC=$R
button "$AC" Primary Medium "Abrir ajustes de bateria" false wide; link "$R" onb7
button "$AC" Text Medium "Pular por enquanto" false wide; link "$R" onb7

onb onb7 7 8 "7 · Escolher bancos" "T-01.7 Onboarding — bancos"
txt "$OB" "Quais bancos você usa?" Headline onBackground
txt "$OB" "O Pingo cria as contas e instrumentos. Para cartões de crédito, informe fechamento e vencimento." Body onSurfaceVariant "$WRAP"
card "$OB" "Bancos"; BK=$R
bank_row() {
  local parent="$1" bank="$2" sub="$3" state="$4"
  frame "$parent" "Bank row" '{"layout":"horizontal","gap":12,"padding":[14,16,14,16],"crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
  local r=$R
  inst "$r" InstitutionAvatar; props_of "$R" "$(jq -cn --arg b "$bank" '{Bank:$b}')"
  vcol "$r" "Text" 0; local t=$R; fillw "$t"
  txt "$t" "$bank" "Body Strong" onBackground
  txt "$t" "$sub" Caption onSurfaceVariant
  inst "$r" Checkbox; props_of "$R" "$(jq -cn --arg s "$state" '{State:$s, Label:" "}')"; set_props "$R" '{"width":28}'
  R=$r
}
bank_row "$BK" Nubank "Cartão de crédito e conta" Checked
frame "$BK" "Nubank dates" '{"layout":"horizontal","gap":12,"padding":[0,16,16,16],"sizingH":"fill","sizingV":"hug","fills":[]}'
ND=$R
for f in "Fechamento|Dia 3" "Vencimento|Dia 10"; do
  inst "$ND" Select; props_of "$R" '{"State":"Closed","Leading icon":false}'; props_of "$R" "$(jq -cn --arg l "${f%|*}" --arg v "${f#*|}" '{Label:$l, Value:$v}')"; fillw "$R"
done
divider "$BK"
bank_row "$BK" Inter "Cartão de crédito e conta" Checked
divider "$BK"
bank_row "$BK" Itaú "Conta-salário e débito" Unchecked
divider "$BK"
bank_row "$BK" "Mercado Pago" "Compras online" Unchecked
divider "$BK"
bank_row "$BK" Caju "Benefícios VR, VA e VT" Unchecked
txt "$OB" "Seu banco não está aqui? Cadastre como Outra depois, só para lançamentos manuais." Caption onSurfaceVariant "$WRAP"
spacer "$OB"
actions "$OB"; AC=$R
button "$AC" Primary Medium "Continuar" false wide; link "$R" onb8

onb onb8 8 9 "8 · Biometria" "T-01.8 Onboarding — biometria"
spacer "$OB"
frame "$OB" "Fingerprint" '{"layout":"vertical","mainAlign":"center","crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
FP=$R
frame "$FP" "Circle" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 128 128
paint "$R" fill primaryContainer
iconi "$R" fingerprint
step transform.scale "$(jq -cn --argjson r "$(ref "$R")" '{ids:[$r], factor:3}')"
txt "$OB" "Proteja seus dados" Headline onBackground "$CENTER"
txt "$OB" "Peça digital ou PIN ao abrir o Pingo. Seus lançamentos ficam só com você." Body onSurfaceVariant "$CENTER"
spacer "$OB"
actions "$OB"; AC=$R
button "$AC" Primary Medium "Ativar biometria" false wide; link "$R" home
button "$AC" Outline Medium "Criar um PIN" false wide; link "$R" lock_pin
button "$AC" Text Medium "Agora não" false wide; link "$R" home

lock() {
  screen "$1" "02 Bloqueio · biometria e PIN" "$2" "$3" "$4"
  body "$SC" 16 24 24 24
  LB=$R
}

lock lock_bio 1 "Biometria" "Bloqueio — biometria"
spacer "$LB"
hero "$LB" 1.6
txt "$LB" "Pingo bloqueado" Headline onBackground "$CENTER"
txt "$LB" "Use sua digital para continuar." Body onSurfaceVariant "$CENTER"
spacer "$LB"
actions "$LB"; AC=$R
button "$AC" Primary Medium "Usar biometria" false wide; link "$R" home
button "$AC" Text Medium "Usar PIN" false wide; link "$R" lock_pin

lock lock_pin 2 "PIN" "Bloqueio — PIN"
spacer "$LB"
hero "$LB" 1.0
txt "$LB" "Digite seu PIN" Headline onBackground "$CENTER"
frame "$LB" "Pin center" '{"layout":"horizontal","mainAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
inst "$R" PinInput; props_of "$R" '{"State":"Partial"}'
KP1=$R
txt "$LB" "Esqueci meu PIN" Label primary "$CENTER"
spacer "$LB"
inst "$LB" NumericKeypad; fillw "$R"
link "$KP1" lock_pin_err

lock lock_pin_err 3 "PIN incorreto" "Bloqueio — PIN incorreto"
spacer "$LB"
hero "$LB" 1.0
txt "$LB" "Digite seu PIN" Headline onBackground "$CENTER"
frame "$LB" "Pin center" '{"layout":"horizontal","mainAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
inst "$R" PinInput; props_of "$R" '{"State":"Error"}'
txt "$LB" "PIN incorreto. Restam 2 tentativas." "Body Small" expense "$CENTER"
spacer "$LB"
inst "$LB" NumericKeypad; fillw "$R"
