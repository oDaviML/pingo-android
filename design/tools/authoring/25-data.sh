#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
begin "$1"

step page.select '{"index":1}'
SET='{"layout":"vertical","gap":16,"padding":24,"sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'
SETH='{"layout":"horizontal","wrap":true,"gap":24,"crossGap":24,"padding":24,"sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'
SETW='{"layout":"horizontal","wrap":true,"gap":24,"crossGap":24,"padding":24,"sizingH":"fixed","width":1340,"sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'

board "Board/Data" "Listas e cartões" "Item de lançamento (tipo × status), ações de deslizar, cabeçalho do dia, cartões de saldo, fatura, voucher e débito, avatar de instituição, linhas de categoria, configuração, permissão e assinatura, cartão de revisão e contador." 9696 24 1440
B=$R

group "$B" "InstitutionAvatar"
G=$R
VARIANTS=()
for bank in "Nubank|#820AD1|N" "Inter|#FF7A00|i" "Itaú|#EC7000|it" "Mercado Pago|#00B1EA|mp" "Caju|#FF4F64|c" "Outra|none|?"; do
  name="${bank%%|*}"; rest="${bank#*|}"; color="${rest%%|*}"; letter="${rest#*|}"
  frame "$G" "Bank=$name" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 40 40
  V=$R
  if [[ $color == none ]]; then paint "$V" fill surfaceVariant; icon "$V" deep landmark; else paint "$V" fill "$color"; txt "$V" "$letter" Label "#FFFFFF"; set_props "$R" '{"fill":"#FFFFFF","fontStyle":"Black"}'; fi
  comp "$V"
  VARIANTS+=("$V")
done
combine "InstitutionAvatar" "$SETH" "${VARIANTS[@]}"
AV=$R

group "$B" "CategoryAvatar"
G=$R
VARIANTS=()
for cat in "Alimentação|utensils" "Mercado|shopping-cart" "Transporte|car" "Moradia|house-plug" "Saúde|heart-pulse" "Lazer|gamepad-2" "Assinaturas|repeat" "Educação|graduation-cap" "Receita|arrow-down-left" "Transferência|arrow-left-right" "Outros|tag"; do
  name="${cat%|*}"; ic="${cat#*|}"
  frame "$G" "Category=$name" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":14,"sizingH":"fixed","sizingV":"fixed"}' 44 44
  V=$R
  case $name in
    Receita) paint "$V" fill incomeContainer; icon "$V" success "$ic" ;;
    Transferência) paint "$V" fill surfaceVariant; icon "$V" muted "$ic" ;;
    *) paint "$V" fill primaryContainer; icon "$V" deep "$ic" ;;
  esac
  comp "$V"
  VARIANTS+=("$V")
done
combine "CategoryAvatar" "$SETH" "${VARIANTS[@]}"
CA=$R

group "$B" "TransactionItem"
G=$R
VARIANTS=()
for type in Expense Income Transfer; do
  for status in Confirmed Pending Manual; do
    case $type in
      Expense) title="Padaria do Zé"; sub="Nubank · crédito · 14:32"; amt="- R\$ 42,90"; tone=expense; cat=Alimentação ;;
      Income) title="Pix de Ana Souza"; sub="Inter · conta · 09:10"; amt="+ R\$ 150,00"; tone=income; cat=Receita ;;
      Transfer) title="Inter → Mercado Pago"; sub="Transferência entre contas · 08:02"; amt="R\$ 500,00"; tone=transfer; cat=Transferência ;;
    esac
    frame "$G" "Type=$type, Status=$status" '{"layout":"horizontal","gap":12,"padding":[12,16,12,16],"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 412
    V=$R; paint "$V" fill surface
    inst "$V" "$CA"; CAI=$R; props_of "$CAI" "$(jq -cn --arg c "$cat" '{Category:$c}')"
    col "$V" "Text" 2; TX=$R; set_props "$TX" '{"sizingH":"fill"}'
    row "$TX" "TitleRow" 6
    TR=$R
    txt "$TR" "$title" "Body Strong" onBackground; T=$R
    if [[ $status == Pending ]]; then
      frame "$TR" "Badge" '{"layout":"horizontal","padding":[2,8,2,8],"cornerRadius":6,"sizingH":"hug","sizingV":"hug"}'; paint "$R" fill warningContainer; txt "$R" "Pendente" "Label Small" onWarningContainer
    elif [[ $status == Manual ]]; then
      frame "$TR" "Badge" '{"layout":"horizontal","padding":[2,8,2,8],"cornerRadius":6,"sizingH":"hug","sizingV":"hug"}'; paint "$R" fill surfaceVariant; txt "$R" "Manual" "Label Small" onSurfaceVariant
    fi
    txt "$TX" "$sub" Caption onSurfaceVariant; S=$R
    txt "$V" "$amt" Amount "$tone"; A=$R
    comp "$V"
    prop "$V" Title text "$(jq -cn --arg v "$title" '$v')" "$T"
    prop "$V" Subtitle text "$(jq -cn --arg v "$sub" '$v')" "$S"
    prop "$V" Amount text "$(jq -cn --arg v "$amt" '$v')" "$A"
    prop "$V" Category instanceSwap "$(ref "$CA")" "$CAI"
    VARIANTS+=("$V")
  done
done
combine "TransactionItem" "$SETW" "${VARIANTS[@]}"
TI=$R

group "$B" "SwipeAction"
G=$R
VARIANTS=()
for dir in Delete Category; do
  frame "$G" "Action=$dir" '{"layout":"horizontal","clip":true,"sizingH":"fixed","sizingV":"fixed"}' 372 68
  V=$R
  frame "$V" "Reveal" '{"layout":"horizontal","padding":[0,28,0,28],"crossAlign":"center","sizingH":"fixed","sizingV":"fixed"}' 372 68
  RV=$R
  set_props "$RV" '{"absolute":true}'
  if [[ $dir == Delete ]]; then
    paint "$RV" fill expense; set_props "$RV" '{"mainAlign":"max"}'; ic=trash-2; lb=Excluir; off=-84
  else
    paint "$RV" fill primary; set_props "$RV" '{"mainAlign":"min"}'; ic=tag; lb=Categoria; off=84
  fi
  frame "$RV" "Content" '{"layout":"vertical","gap":4,"crossAlign":"center","sizingH":"hug","sizingV":"hug","fills":[]}'
  CT=$R; icon "$CT" white "$ic"; txt "$CT" "$lb" "Label Small" onPrimary
  inst "$V" "$TI"; IT=$R
  set_props "$IT" '{"absolute":true}'
  set_props "$IT" "$(jq -cn --argjson o "$off" '{x:$o, y:0, width:372}')"
  comp "$V"
  VARIANTS+=("$V")
done
combine "SwipeAction" "$SETH" "${VARIANTS[@]}"

group "$B" "DayHeader"
G=$R
frame "$G" "DayHeader" '{"layout":"horizontal","mainAlign":"spaceBetween","crossAlign":"center","padding":[16,16,8,16],"sizingH":"fixed","sizingV":"hug","fills":[]}' 412
V=$R
txt "$V" "Hoje · qui, 22 out" Overline onSurfaceVariant; T=$R
txt "$V" "- R\$ 128,40" "Label Small" onSurfaceVariant; A=$R
comp "$V"; prop "$V" Day text '"Hoje · qui, 22 out"' "$T"; prop "$V" Total text '"- R$ 128,40"' "$A"

group "$B" "Cards" '{"layout":"horizontal","wrap":true,"gap":24,"crossGap":24,"sizingH":"fixed","width":1340,"sizingV":"hug","fills":[]}'
G=$R

frame "$G" "BalanceCard" '{"layout":"vertical","gap":16,"padding":20,"cornerRadius":20,"sizingH":"fixed","sizingV":"hug","clip":true,"fills":[{"type":"linearGradient","stops":["#09637E","#088395"],"from":[0,0],"to":[1,1]}]}' 372
V=$R
step node.create "$(jq -cn --argjson p "$(ref "$V")" '{type:"ellipse", parent:$p, name:"Ripple", x:250, y:-60, width:180, height:180, props:{absolute:true, fills:[], stroke:"#FFFFFF", strokeWeight:18, opacity:0.08}}')"
step node.create "$(jq -cn --argjson p "$(ref "$V")" '{type:"ellipse", parent:$p, name:"Ripple 2", x:290, y:-20, width:100, height:100, props:{absolute:true, fills:[], stroke:"#FFFFFF", strokeWeight:12, opacity:0.08}}')"
txt "$V" "Saldo de outubro" Overline "#FFFFFF"; set_props "$R" '{"fill":"#CDE6EA"}'
txt "$V" "R\$ 2.481,90" "Amount Large" "#FFFFFF"; set_props "$R" '{"fill":"#FFFFFF"}'; BAL=$R
row "$V" "Split" 12; SP=$R; set_props "$SP" '{"sizingH":"fill"}'
for k in "Receitas|+ R\$ 6.200,00|arrow-down-left" "Despesas|- R\$ 3.718,10|arrow-up-right"; do
  lab="${k%%|*}"; rest="${k#*|}"; val="${rest%%|*}"; ic="${rest#*|}"
  frame "$SP" "$lab" '{"layout":"horizontal","gap":10,"padding":[10,12,10,12],"cornerRadius":14,"crossAlign":"center","sizingH":"fill","sizingV":"hug","fill":"#FFFFFF1F"}'
  P=$R
  icon "$P" white "$ic"
  col "$P" "T" 0; C=$R
  txt "$C" "$lab" Caption "#FFFFFF"; set_props "$R" '{"fill":"#CDE6EA"}'
  txt "$C" "$val" "Label" "#FFFFFF"; set_props "$R" '{"fill":"#FFFFFF"}'
done
comp "$V"; prop "$V" Balance text '"R$ 2.481,90"' "$BAL"

VARIANTS=()
for status in Open Closed; do
  frame "$G" "Status=$status" '{"layout":"vertical","gap":14,"padding":18,"cornerRadius":20,"sizingH":"fixed","sizingV":"hug","strokeWeight":1,"strokeAlign":"inside"}' 372
  V=$R; paint "$V" fill surface; paint "$V" stroke outlineVariant
  row "$V" "Header" 12; H=$R; set_props "$H" '{"sizingH":"fill"}'
  inst "$H" "$AV"; AVI=$R; props_of "$AVI" '{"Bank":"Nubank"}'
  col "$H" "Name" 0; NM=$R; set_props "$NM" '{"sizingH":"fill"}'
  txt "$NM" "Nubank · crédito" "Body Strong" onBackground; T=$R
  txt "$NM" "final 4321" Caption onSurfaceVariant
  frame "$H" "Status" '{"layout":"horizontal","padding":[4,10,4,10],"cornerRadius":8,"sizingH":"hug","sizingV":"hug"}'
  if [[ $status == Open ]]; then paint "$R" fill primaryContainer; txt "$R" "Aberta" "Label Small" onPrimaryContainer
  else paint "$R" fill warningContainer; txt "$R" "Fechada" "Label Small" onWarningContainer; fi
  txt "$V" "R\$ 1.284,50" Headline onBackground; A=$R
  frame "$V" "Progress" '{"layout":"horizontal","cornerRadius":999,"sizingH":"fill","sizingV":"fixed","clip":true}' "" 6
  PR=$R; paint "$PR" fill surfaceVariant
  frame "$PR" "Fill" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fill"}' "$( [[ $status == Open ]] && echo 210 || echo 336)"; paint "$R" fill "$( [[ $status == Open ]] && echo primary || echo warning)"
  row "$V" "Dates" 6; DT=$R; set_props "$DT" '{"sizingH":"fill","mainAlign":"spaceBetween"}'
  if [[ $status == Open ]]; then txt "$DT" "Fecha dia 3 · vence dia 10" Caption onSurfaceVariant; CL=$R; txt "$DT" "Vence em 5 dias" "Label Small" primary; DU=$R
  else txt "$DT" "Fechou dia 3 · pagar até 10/11" Caption onSurfaceVariant; CL=$R; txt "$DT" "Vence em 2 dias" "Label Small" warning; DU=$R; fi
  comp "$V"; prop "$V" Title text '"Nubank · crédito"' "$T"; prop "$V" Amount text '"R$ 1.284,50"' "$A"; prop "$V" Bank instanceSwap "$(ref "$AV")" "$AVI"; prop "$V" Closing text "$(jq -cn --arg v "$( [[ $status == Open ]] && echo 'Fecha dia 3 · vence dia 10' || echo 'Fechou dia 3 · pagar até 10/11')" '$v')" "$CL"; prop "$V" Due text "$(jq -cn --arg v "$( [[ $status == Open ]] && echo 'Vence em 5 dias' || echo 'Vence em 2 dias')" '$v')" "$DU"
  VARIANTS+=("$V")
done
combine "InvoiceCard" "$SETH" "${VARIANTS[@]}"

frame "$G" "VoucherCard" '{"layout":"vertical","gap":12,"padding":18,"cornerRadius":20,"sizingH":"fixed","sizingV":"hug","strokeWeight":1,"strokeAlign":"inside"}' 372
V=$R; paint "$V" fill surface; paint "$V" stroke outlineVariant
row "$V" "Header" 12; H=$R; set_props "$H" '{"sizingH":"fill"}'
inst "$H" "$AV"; AVI=$R; props_of "$AVI" '{"Bank":"Caju"}'
col "$H" "Name" 0; NM=$R; set_props "$NM" '{"sizingH":"fill"}'
txt "$NM" "VR · Refeição" "Body Strong" onBackground; T=$R
txt "$NM" "Caju · benefício" Caption onSurfaceVariant
txt "$V" "R\$ 486,20" Headline income; A=$R
txt "$V" "Última recarga 01/10 · R\$ 800,00" Caption onSurfaceVariant; S=$R
comp "$V"; prop "$V" Title text '"VR · Refeição"' "$T"; prop "$V" Balance text '"R$ 486,20"' "$A"; prop "$V" Recharge text '"Última recarga 01/10 · R$ 800,00"' "$S"; prop "$V" Bank instanceSwap "$(ref "$AV")" "$AVI"

frame "$G" "DebitCard" '{"layout":"horizontal","gap":12,"padding":18,"cornerRadius":20,"crossAlign":"center","sizingH":"fixed","sizingV":"hug","strokeWeight":1,"strokeAlign":"inside"}' 372
V=$R; paint "$V" fill surface; paint "$V" stroke outlineVariant
inst "$V" "$AV"; AVI=$R; props_of "$AVI" '{"Bank":"Itaú"}'
col "$V" "Name" 0; NM=$R; set_props "$NM" '{"sizingH":"fill"}'
txt "$NM" "Itaú · débito" "Body Strong" onBackground; T=$R
txt "$NM" "Gasto do mês" Caption onSurfaceVariant
txt "$V" "R\$ 2.140,00" Amount onBackground; A=$R
comp "$V"; prop "$V" Title text '"Itaú · débito"' "$T"; prop "$V" Amount text '"R$ 2.140,00"' "$A"; prop "$V" Bank instanceSwap "$(ref "$AV")" "$AVI"

group "$B" "Rows" '{"layout":"horizontal","wrap":true,"gap":24,"crossGap":24,"sizingH":"fixed","width":1340,"sizingV":"hug","fills":[]}'
G=$R

VARIANTS=()
for share in 10 20 30 40; do
frame "$G" "Share=$share" '{"layout":"vertical","gap":8,"padding":[12,16,12,16],"sizingH":"fixed","sizingV":"hug"}' 412
V=$R; paint "$V" fill surface
row "$V" "Top" 12; TP=$R; set_props "$TP" '{"sizingH":"fill"}'
inst "$TP" "$CA"; CAI=$R; props_of "$CAI" '{"Category":"Alimentação"}'
col "$TP" "Name" 0; NM=$R; set_props "$NM" '{"sizingH":"fill"}'
txt "$NM" "Alimentação" "Body Strong" onBackground; T=$R
txt "$NM" "32% das despesas · ↑ 12% vs set" Caption onSurfaceVariant; S=$R
txt "$TP" "R\$ 1.190,00" Amount onBackground; A=$R
frame "$V" "Bar" '{"layout":"horizontal","cornerRadius":999,"sizingH":"fill","sizingV":"fixed","clip":true}' "" 6
PR=$R; paint "$PR" fill surfaceVariant
frame "$PR" "Fill" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fill"}' $((share * 34 / 10)); paint "$R" fill primary
comp "$V"; prop "$V" Title text '"Alimentação"' "$T"; prop "$V" Detail text '"32% das despesas · ↑ 12% vs set"' "$S"; prop "$V" Amount text '"R$ 1.190,00"' "$A"; prop "$V" Category instanceSwap "$(ref "$CA")" "$CAI"
VARIANTS+=("$V")
done
combine "CategoryRow" "$SETH" "${VARIANTS[@]}"

VARIANTS=()
for type in Navigation Switch SwitchOff Value Danger; do
  case $type in
    Navigation) ic=tag; ti="Categorias e regras"; su="14 regras aprendidas"; tone=deep ;;
    Switch|SwitchOff) ic=fingerprint; ti="Biometria"; su="Pedir ao abrir o app"; tone=deep ;;
    Value) ic=lock; ti="Bloquear após"; su=""; tone=deep ;;
    Danger) ic=trash-2; ti="Excluir conta e dados"; su="Apaga tudo no aparelho e na nuvem"; tone=danger ;;
  esac
  frame "$G" "Type=$type" '{"layout":"horizontal","gap":14,"padding":[14,16,14,16],"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 412
  V=$R; paint "$V" fill surface
  frame "$V" "Icon" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":12,"sizingH":"fixed","sizingV":"fixed"}' 40 40
  IB=$R; paint "$IB" fill "$( [[ $type == Danger ]] && echo expenseContainer || echo primaryContainer)"
  if [[ $type == Danger ]]; then icon "$IB" "$tone" "$ic"; II=; else iconi "$IB" "$ic"; II=$R; fi
  col "$V" "Text" 2; TX=$R; set_props "$TX" '{"sizingH":"fill"}'
  txt "$TX" "$ti" "Body Strong" "$( [[ $type == Danger ]] && echo expense || echo onBackground)"; T=$R
  if [[ -n $su ]]; then txt "$TX" "$su" Caption onSurfaceVariant '{"props":{"sizingH":"fill","textAutoResize":"height"}}'; S=$R; fi
  case $type in
    Navigation) icon "$V" muted chevron-right Trailing ;;
    Switch) inst "$V" Switch; props_of "$R" '{"State":"On"}' ;;
    SwitchOff) inst "$V" Switch; props_of "$R" '{"State":"Off"}' ;;
    Value) txt "$V" "1 minuto" Body primary; VT=$R; icon "$V" muted chevron-right Trailing ;;
    Danger) icon "$V" danger chevron-right Trailing ;;
  esac
  comp "$V"; prop "$V" Title text "$(jq -cn --arg v "$ti" '$v')" "$T"
  if [[ -n $su ]]; then prop "$V" Subtitle text "$(jq -cn --arg v "$su" '$v')" "$S"; fi
  if [[ $type == Value ]]; then prop "$V" Value text '"1 minuto"' "$VT"; fi
  if [[ -n $II ]]; then prop "$V" Icon instanceSwap "$(iconref "$ic")" "$II"; fi
  VARIANTS+=("$V")
done
combine "SettingsRow" "$SET" "${VARIANTS[@]}"

VARIANTS=()
for state in Granted Pending Denied; do
  case $state in
    Granted) bg=incomeContainer; it=success; ic=circle-check; desc="Ativo — lendo notificações dos bancos" ;;
    Pending) bg=warningContainer; it=warning; ic=triangle-alert; desc="Toque para permitir no Android" ;;
    Denied) bg=expenseContainer; it=danger; ic=circle-x; desc="Sem isso o Pingo fica só no manual" ;;
  esac
  frame "$G" "State=$state" '{"layout":"horizontal","gap":14,"padding":16,"cornerRadius":16,"crossAlign":"center","sizingH":"fixed","sizingV":"hug","strokeWeight":1,"strokeAlign":"inside"}' 412
  V=$R; paint "$V" fill surface; paint "$V" stroke outlineVariant
  frame "$V" "Status" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 40 40
  paint "$R" fill "$bg"; icon "$R" "$it" "$ic"
  col "$V" "Text" 2; TX=$R; set_props "$TX" '{"sizingH":"fill"}'
  txt "$TX" "Acesso a notificações" "Body Strong" onBackground; T=$R
  txt "$TX" "$desc" Caption onSurfaceVariant '{"props":{"sizingH":"fill","textAutoResize":"height"}}'; S=$R
  if [[ $state != Granted ]]; then inst "$V" Button; props_of "$R" "$(jq -cn --arg s "$( [[ $state == Denied ]] && echo Primary || echo Tonal)" '{Type:$s, Size:"Small", Label:"Corrigir", Icon:false}')"; fi
  comp "$V"; prop "$V" Title text '"Acesso a notificações"' "$T"; prop "$V" Description text "$(jq -cn --arg v "$desc" '$v')" "$S"
  VARIANTS+=("$V")
done
combine "PermissionItem" "$SET" "${VARIANTS[@]}"

frame "$G" "SubscriptionRow" '{"layout":"horizontal","gap":12,"padding":[12,16,12,16],"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 412
V=$R; paint "$V" fill surface
inst "$V" "$CA"; props_of "$R" '{"Category":"Assinaturas"}'
col "$V" "Text" 2; TX=$R; set_props "$TX" '{"sizingH":"fill"}'
txt "$TX" "Spotify" "Body Strong" onBackground; T=$R
txt "$TX" "Próxima cobrança ≈ 12/11 · Nubank" Caption onSurfaceVariant; S=$R
txt "$V" "R\$ 21,90" Amount onBackground; A=$R
comp "$V"; prop "$V" Title text '"Spotify"' "$T"; prop "$V" Detail text '"Próxima cobrança ≈ 12/11 · Nubank"' "$S"; prop "$V" Amount text '"R$ 21,90"' "$A"

VARIANTS=()
for tone in Recognized Review Ignored Unknown; do
  case $tone in
    Recognized) bg=incomeContainer; c=income; n=184; l="Reconhecidas" ;;
    Review) bg=warningContainer; c=onWarningContainer; n=12; l="Para revisão" ;;
    Ignored) bg=surfaceVariant; c=onSurfaceVariant; n=37; l="Ignoradas" ;;
    Unknown) bg=expenseContainer; c=expense; n=3; l="Não entendidas" ;;
  esac
  frame "$G" "Tone=$tone" '{"layout":"vertical","gap":2,"padding":16,"cornerRadius":16,"sizingH":"fixed","sizingV":"hug"}' 176
  V=$R; paint "$V" fill "$bg"
  txt "$V" "$n" Display "$c"; NM=$R
  txt "$V" "$l" "Label Small" "$c"; L=$R
  comp "$V"; prop "$V" Value text "\"$n\"" "$NM"; prop "$V" Label text "$(jq -cn --arg v "$l" '$v')" "$L"
  VARIANTS+=("$V")
done
combine "StatTile" "$SETH" "${VARIANTS[@]}"

group "$B" "ReviewCard"
G=$R
VARIANTS=()
for reason in LowConfidence Duplicate Transfer NewMerchant; do
  case $reason in
    LowConfidence) rl="Baixa confiança · 62%"; ex="Compra aprovada R\$ 42,90 PADARIA DO ZE"; title="Padaria do Zé"; amt="- R\$ 42,90"; cat=Alimentação ;;
    Duplicate) rl="Possível duplicata"; ex="Pix enviado R\$ 80,00 para Mercado do Bairro"; title="Mercado do Bairro"; amt="- R\$ 80,00"; cat=Mercado ;;
    Transfer) rl="Transferência ambígua"; ex="Você recebeu R\$ 500,00 de Lucas M."; title="Pix de Lucas M."; amt="+ R\$ 500,00"; cat=Transferência ;;
    NewMerchant) rl="Estabelecimento novo"; ex="Compra no débito R\$ 19,90 ZPT*LOJA2231"; title="ZPT*LOJA2231"; amt="- R\$ 19,90"; cat=Outros ;;
  esac
  frame "$G" "Reason=$reason" '{"layout":"vertical","gap":14,"padding":16,"cornerRadius":20,"sizingH":"fixed","sizingV":"hug","strokeWeight":1,"strokeAlign":"inside"}' 380
  V=$R; paint "$V" fill surface; paint "$V" stroke outlineVariant
  frame "$V" "Reason" '{"layout":"horizontal","gap":6,"padding":[4,10,4,8],"cornerRadius":8,"crossAlign":"center","sizingH":"hug","sizingV":"hug"}'
  RS=$R; paint "$RS" fill warningContainer; icon "$RS" onwarning sparkles; txt "$RS" "$rl" "Label Small" onWarningContainer; RL=$R
  row "$V" "Summary" 12; SM=$R; set_props "$SM" '{"sizingH":"fill"}'
  inst "$SM" "$CA"; CAI=$R; props_of "$CAI" "$(jq -cn --arg c "$cat" '{Category:$c}')"
  col "$SM" "Text" 2; TX=$R; set_props "$TX" '{"sizingH":"fill"}'
  txt "$TX" "$title" "Body Strong" onBackground; T=$R
  txt "$TX" "Nubank · hoje, 14:32" Caption onSurfaceVariant
  txt "$SM" "$amt" Amount "$( [[ $amt == +* ]] && echo income || echo expense)"; A=$R
  frame "$V" "Excerpt" '{"layout":"horizontal","gap":8,"padding":12,"cornerRadius":12,"sizingH":"fill","sizingV":"hug"}'
  EX=$R; paint "$EX" fill surfaceVariant; paint "$EX" stroke secondary; set_props "$EX" '{"strokeLeftWeight":3,"strokeAlign":"inside"}'
  txt "$EX" "“$ex”" Caption onSurfaceVariant '{"props":{"sizingH":"fill","textAutoResize":"height","fontStyle":"Italic"}}'; E=$R
  row "$V" "Actions" 8; AC=$R; set_props "$AC" '{"sizingH":"fill","mainAlign":"max"}'
  inst "$AC" Button; props_of "$R" '{"Type":"Text","Size":"Small","Label":"Descartar","Icon":false}'
  inst "$AC" Button; props_of "$R" "$(jq -cn --arg l "$( [[ $reason == Transfer ]] && echo 'É transferência' || echo 'Trocar categoria')" '{Type:"Outline", Size:"Small", Label:$l, Icon:false}')"
  inst "$AC" Button; props_of "$R" '{"Type":"Primary","Size":"Small","Label":"Confirmar","Icon":false}'
  comp "$V"
  prop "$V" "Reason label" text "$(jq -cn --arg v "$rl" '$v')" "$RL"
  prop "$V" Title text "$(jq -cn --arg v "$title" '$v')" "$T"
  prop "$V" Amount text "$(jq -cn --arg v "$amt" '$v')" "$A"
  prop "$V" Excerpt text "$(jq -cn --arg v "“$ex”" '$v')" "$E"
  prop "$V" Category instanceSwap "$(ref "$CA")" "$CAI"
  VARIANTS+=("$V")
done
combine "ReviewCard" "$SETW" "${VARIANTS[@]}"
