#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
begin "$1"

step page.select '{"index":1}'
SET='{"layout":"vertical","gap":16,"padding":24,"sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'
SETH='{"layout":"horizontal","wrap":true,"gap":24,"crossGap":24,"padding":24,"crossAlign":"center","sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'

board "Board/Feedback" "Feedback e gráficos" "Banners por tom, snackbar com Desfazer, diálogo de confirmação, badges, tag de status, progresso, skeleton, estados vazios com o mascote, tooltip e os gráficos dos relatórios." 11238 37 1440
B=$R

group "$B" "Banner"
G=$R
VARIANTS=()
for tone in Info Warning Error Success; do
  case $tone in
    Info) bg=primaryContainer; fg=onPrimaryContainer; it=deep; ic=bell; ti="3 lançamentos para revisar"; de="Toque para conferir o que o Pingo não teve certeza." ;;
    Warning) bg=warningContainer; fg=onWarningContainer; it=onwarning; ic=triangle-alert; ti="Captura parada"; de="O Pingo não lê notificações desde 08:14. Toque para corrigir." ;;
    Error) bg=expenseContainer; fg=expense; it=danger; ic=bell-off; ti="Sem acesso às notificações"; de="O app está só no modo manual." ;;
    Success) bg=incomeContainer; fg=income; it=success; ic=circle-check; ti="Tudo em dia"; de="Nenhum lançamento pendente." ;;
  esac
  frame "$G" "Tone=$tone" '{"layout":"horizontal","gap":12,"padding":[14,14,14,16],"cornerRadius":16,"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 380
  V=$R; paint "$V" fill "$bg"
  icon "$V" "$it" "$ic" Icon
  col "$V" "Text" 2; TX=$R; set_props "$TX" '{"sizingH":"fill"}'
  txt "$TX" "$ti" Label "$fg"; T=$R
  txt "$TX" "$de" Caption "$fg" '{"props":{"sizingH":"fill","textAutoResize":"height"}}'; D=$R
  icon "$V" "$it" chevron-right Trailing
  comp "$V"; prop "$V" Title text "$(jq -cn --arg v "$ti" '$v')" "$T"; prop "$V" Description text "$(jq -cn --arg v "$de" '$v')" "$D"
  VARIANTS+=("$V")
done
combine "Banner" "$(jq -c '. + {sizingH:"fixed", width:1340}' <<< "$SETH")" "${VARIANTS[@]}"

group "$B" "Snackbar"
G=$R
VARIANTS=()
for kind in Undo Simple; do
  frame "$G" "Kind=$kind" '{"layout":"horizontal","gap":12,"padding":[12,8,12,16],"cornerRadius":14,"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 380
  V=$R; paint "$V" fill onBackground; style_fx "$V" "Elevation/2"
  if [[ $kind == Undo ]]; then lbl="Lançamento excluído"; else lbl="Categoria aplicada a 8 lançamentos"; fi
  txt "$V" "$lbl" "Body Small" background '{"props":{"sizingH":"fill"}}'; T=$R
  if [[ $kind == Undo ]]; then
    frame "$V" "Action" '{"layout":"horizontal","gap":6,"padding":[8,12,8,12],"cornerRadius":10,"crossAlign":"center","sizingH":"hug","sizingV":"hug","fills":[]}'
    A=$R; icon "$A" soft undo-2; txt "$A" "Desfazer" Label secondary
  fi
  comp "$V"; prop "$V" Message text "$(jq -cn --arg v "$lbl" '$v')" "$T"
  VARIANTS+=("$V")
done
combine "Snackbar" "$SETH" "${VARIANTS[@]}"

group "$B" "Dialog"
G=$R
VARIANTS=()
for kind in Destructive Confirm Archive; do
  frame "$G" "Kind=$kind" '{"layout":"vertical","gap":16,"padding":24,"cornerRadius":24,"sizingH":"fixed","sizingV":"hug"}' 340
  V=$R; paint "$V" fill surface; style_fx "$V" "Elevation/3"
  frame "$V" "Icon" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 48 48
  if [[ $kind == Destructive ]]; then paint "$R" fill expenseContainer; icon "$R" danger trash-2; ti="Excluir lançamento?"; de="Padaria do Zé · - R\$ 42,90. Você pode desfazer logo depois."; ok="Excluir"; ty=Danger
  elif [[ $kind == Archive ]]; then paint "$R" fill primaryContainer; icon "$R" deep landmark; ti="Arquivar esta conta?"; de="Ela tem 38 lançamentos. Sai das listas, mas o histórico continua nos relatórios."; ok="Arquivar"; ty=Tonal
  else paint "$R" fill primaryContainer; icon "$R" deep tag; ti="Aplicar a todos?"; de="Usar Alimentação para os 8 lançamentos da Padaria do Zé e os próximos."; ok="Aplicar"; ty=Primary; fi
  txt "$V" "$ti" Title onBackground; T=$R
  txt "$V" "$de" "Body Small" onSurfaceVariant '{"props":{"sizingH":"fill","textAutoResize":"height"}}'; D=$R
  if [[ $kind == Confirm ]]; then inst "$V" Checkbox; props_of "$R" '{"State":"Checked","Label":"Lembrar para este estabelecimento"}'; set_props "$R" '{"sizingH":"fill"}'; fi
  row "$V" "Actions" 8; A=$R; set_props "$A" '{"sizingH":"fill","mainAlign":"max"}'
  inst "$A" Button; props_of "$R" '{"Type":"Text","Size":"Medium","Label":"Cancelar","Icon":false}'
  inst "$A" Button; props_of "$R" "$(jq -cn --arg t "$ty" --arg l "$ok" '{Type:$t, Size:"Medium", Label:$l, Icon:false}')"
  comp "$V"; prop "$V" Title text "$(jq -cn --arg v "$ti" '$v')" "$T"; prop "$V" Description text "$(jq -cn --arg v "$de" '$v')" "$D"
  VARIANTS+=("$V")
done
combine "Dialog" "$SETH" "${VARIANTS[@]}"

group "$B" "Badges e status"
G=$R
row "$G" "Items" 24
IR=$R
VARIANTS=()
for kind in Dot Count; do
  if [[ $kind == Dot ]]; then frame "$IR" "Kind=Dot" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 10 10; V=$R; paint "$V" fill expense
  else frame "$IR" "Kind=Count" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","padding":[1,6,1,6],"cornerRadius":999,"sizingH":"hug","sizingV":"fixed"}' "" 18; V=$R; paint "$V" fill expense; txt "$V" "12" "Label Small" onPrimary; T=$R; fi
  comp "$V"; if [[ $kind == Count ]]; then prop "$V" Count text '"12"' "$T"; fi
  VARIANTS+=("$V")
done
combine "Badge" "$SETH" "${VARIANTS[@]}"
VARIANTS=()
for st in Active Stopped Syncing; do
  case $st in Active) bg=incomeContainer; c=income; l="Captura ativa" ;; Stopped) bg=expenseContainer; c=expense; l="Captura parada" ;; Syncing) bg=primaryContainer; c=onPrimaryContainer; l="Sincronizando" ;; esac
  frame "$IR" "State=$st" '{"layout":"horizontal","gap":6,"padding":[5,10,5,8],"cornerRadius":999,"crossAlign":"center","sizingH":"hug","sizingV":"hug"}'
  V=$R; paint "$V" fill "$bg"
  frame "$V" "Pulse" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 8 8; paint "$R" fill "$c"
  txt "$V" "$l" "Label Small" "$c"; T=$R
  comp "$V"; prop "$V" Label text "$(jq -cn --arg v "$l" '$v')" "$T"
  VARIANTS+=("$V")
done
combine "StatusTag" "$SETH" "${VARIANTS[@]}"

group "$B" "Progress"
G=$R
VARIANTS=()
for v in 0 40 100; do
  frame "$G" "Value=$v" '{"layout":"horizontal","cornerRadius":999,"clip":true,"sizingH":"fixed","sizingV":"fixed"}' 240 6
  V=$R; paint "$V" fill surfaceVariant
  if (( v > 0 )); then frame "$V" "Fill" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fill"}' $((v * 240 / 100)); paint "$R" fill primary; fi
  comp "$V"; VARIANTS+=("$V")
done
combine "LinearProgress" "$SETH" "${VARIANTS[@]}"
frame "$G" "Spinner" '{"sizingH":"fixed","sizingV":"fixed","fills":[]}' 40 40
V=$R
step node.create "$(jq -cn --argjson p "$(ref "$V")" '{type:"ellipse", parent:$p, name:"Track", x:2, y:2, width:36, height:36, props:{fills:[], stroke:"#DCEAEC", strokeWeight:4}}')"
step node.create "$(jq -cn --argjson p "$(ref "$V")" '{type:"ellipse", parent:$p, name:"Arc", x:2, y:2, width:36, height:36, props:{fills:[], stroke:"#088395", strokeWeight:4, strokeCap:"round", arcStart:-90, arcSweep:110, arcInner:0.78}}')"
comp "$V"

group "$B" "Skeleton"
G=$R
frame "$G" "SkeletonItem" '{"layout":"horizontal","gap":12,"padding":[12,16,12,16],"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 412
V=$R; paint "$V" fill surface
frame "$V" "Avatar" '{"cornerRadius":14,"sizingH":"fixed","sizingV":"fixed"}' 44 44; paint "$R" fill surfaceVariant
col "$V" "Lines" 8; L=$R; set_props "$L" '{"sizingH":"fill"}'
frame "$L" "L1" '{"cornerRadius":6,"sizingH":"fixed","sizingV":"fixed"}' 160 12; paint "$R" fill surfaceVariant
frame "$L" "L2" '{"cornerRadius":6,"sizingH":"fixed","sizingV":"fixed"}' 110 10; paint "$R" fill surfaceVariant
frame "$V" "Amount" '{"cornerRadius":6,"sizingH":"fixed","sizingV":"fixed"}' 64 14; paint "$R" fill surfaceVariant
comp "$V"

group "$B" "EmptyState"
G=$R
VARIANTS=()
for kind in NoData NoResults AllDone ManualOnly; do
  case $kind in
    NoData) ti="Nenhum pingo ainda"; de="Assim que um banco avisar uma compra, ela aparece aqui. Ou lance agora."; cta="Lançar manualmente" ;;
    NoResults) ti="Nada com esses filtros"; de="Tente outro mês ou limpe os filtros."; cta="Limpar filtros" ;;
    AllDone) ti="Tudo em dia"; de="Nenhum lançamento esperando revisão."; cta="" ;;
    ManualOnly) ti="Modo só manual"; de="Sem acesso às notificações o Pingo não captura sozinho."; cta="Ativar captura" ;;
  esac
  frame "$G" "Kind=$kind" '{"layout":"vertical","gap":12,"padding":[28,24,28,24],"cornerRadius":20,"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 300
  V=$R; paint "$V" fill surface
  frame "$V" "Art" '{"sizingH":"fixed","sizingV":"fixed","fills":[]}' 120 104
  ART=$R
  step node.create "$(jq -cn --argjson p "$(ref "$ART")" '{type:"ellipse", parent:$p, name:"Ripple", x:4, y:72, width:112, height:28, props:{fills:[], stroke:"#7AB2B2", strokeWeight:2, opacity:0.6}}')"
  step node.create "$(jq -cn --argjson p "$(ref "$ART")" '{type:"ellipse", parent:$p, name:"Ripple 2", x:28, y:79, width:64, height:14, props:{fills:[], stroke:"#7AB2B2", strokeWeight:2, opacity:0.9}}')"
  if [[ $kind == NoResults || $kind == ManualOnly ]]; then dropfill='{"fills":[{"type":"linearGradient","stops":["#B9D4D7","#7AB2B2"],"from":[0.5,0],"to":[0.5,1]}]}'; else dropfill='{"fills":[{"type":"linearGradient","stops":["#1AA7BC","#088395","#09637E"],"from":[0.5,0],"to":[0.5,1]}]}'; fi
  step node.create "$(jq -cn --argjson p "$(ref "$ART")" --argjson f "$dropfill" '{type:"vector", parent:$p, name:"Gota", x:36, y:4, path:"M24 2 C24 2 46 27 46 36 A22 22 0 1 1 2 36 C2 27 24 2 24 2 Z", props:$f}')"
  step node.create "$(jq -cn --argjson p "$(ref "$ART")" '{type:"ellipse", parent:$p, name:"Olho E", x:51, y:40, width:5, height:8, props:{fill:"#04222A"}}')"
  step node.create "$(jq -cn --argjson p "$(ref "$ART")" '{type:"ellipse", parent:$p, name:"Olho D", x:64, y:40, width:5, height:8, props:{fill:"#04222A"}}')"
  if [[ $kind == AllDone ]]; then mouth="M0 0 Q5 5 10 0"; my=52; elif [[ $kind == NoData ]]; then mouth="M0 1 L8 1"; my=53; else mouth="M0 4 Q5 0 10 4"; my=52; fi
  step node.create "$(jq -cn --argjson p "$(ref "$ART")" --arg m "$mouth" --argjson y "$my" '{type:"vector", parent:$p, name:"Boca", x:55, y:$y, path:$m, props:{fills:[], stroke:"#04222A", strokeWeight:2, strokeCap:"round"}}')"
  txt "$V" "$ti" Subtitle onBackground '{"props":{"textAlign":"center"}}'; T=$R
  txt "$V" "$de" "Body Small" onSurfaceVariant '{"props":{"textAlign":"center","sizingH":"fill","textAutoResize":"height"}}'; D=$R
  if [[ -n $cta ]]; then inst "$V" Button; props_of "$R" "$(jq -cn --arg l "$cta" '{Type:"Tonal", Size:"Medium", Label:$l, Icon:false}')"; fi
  comp "$V"; prop "$V" Title text "$(jq -cn --arg v "$ti" '$v')" "$T"; prop "$V" Description text "$(jq -cn --arg v "$de" '$v')" "$D"
  VARIANTS+=("$V")
done
combine "EmptyState" "$SETH" "${VARIANTS[@]}"

group "$B" "Tooltip"
G=$R
frame "$G" "Tooltip" '{"layout":"vertical","gap":0,"crossAlign":"center","sizingH":"hug","sizingV":"hug","fills":[]}'
V=$R
frame "$V" "Body" '{"layout":"horizontal","padding":[8,12,8,12],"cornerRadius":10,"sizingH":"hug","sizingV":"hug"}'; BO=$R; paint "$BO" fill onBackground
txt "$BO" "Confiança da leitura: 62%" "Label Small" background; T=$R
step node.create "$(jq -cn --argjson p "$(ref "$V")" '{type:"polygon", parent:$p, name:"Arrow", width:14, height:7, props:{fill:"#0B2A33", rotation:180, pointCount:3}}')"
comp "$V"; prop "$V" Text text '"Confiança da leitura: 62%"' "$T"

board "Board/Charts" "Gráficos" "Rosca de despesas por categoria, comparação com o mês anterior, receitas × despesas dos últimos 6 meses e legenda." 12761 24
C=$R

group "$C" "DonutChart"
G=$R
frame "$G" "DonutChart" '{"layout":"horizontal","gap":24,"padding":20,"cornerRadius":20,"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 412
V=$R; paint "$V" fill surface
frame "$V" "Ring" '{"sizingH":"fixed","sizingV":"fixed","fills":[]}' 152 152
RG=$R
start=-90
for seg in "0.32|#09637E" "0.21|#088395" "0.17|#7AB2B2" "0.12|#C9802B" "0.18|#B9D4D7"; do
  frac="${seg%|*}"; col="${seg#*|}"
  step node.create "$(jq -cn --argjson p "$(ref "$RG")" --arg c "$col" --argjson s "$start" --argjson f "$frac" '{type:"ellipse", parent:$p, name:"Segment", x:0, y:0, width:152, height:152, props:{fill:$c, arcStart:$s, arcSweep:($f * 360 - 3), arcInner:0.68}}')"
  start=$(awk -v s="$start" -v f="$frac" 'BEGIN{printf "%.2f\n", s + f*360}')
done
frame "$RG" "Center" '{"layout":"vertical","gap":0,"mainAlign":"center","crossAlign":"center","sizingH":"fixed","sizingV":"fixed","fills":[]}' 152 152
CE=$R
txt "$CE" "Despesas" Caption onSurfaceVariant
txt "$CE" "R\$ 3.718" Title onBackground; TOT=$R
col "$V" "Legend" 10; LG=$R; set_props "$LG" '{"sizingH":"fill"}'
for it in "Alimentação|32%|#09637E" "Mercado|21%|#088395" "Transporte|17%|#7AB2B2" "Assinaturas|12%|#C9802B" "Outros|18%|#B9D4D7"; do
  name="${it%%|*}"; rest="${it#*|}"; pct="${rest%%|*}"; col="${rest#*|}"
  row "$LG" "Item" 8; LI=$R; set_props "$LI" '{"sizingH":"fill"}'
  frame "$LI" "Key" '{"cornerRadius":3,"sizingH":"fixed","sizingV":"fixed"}' 10 10; set_props "$R" "$(jq -cn --arg c "$col" '{fill:$c}')"
  txt "$LI" "$name" "Body Small" onBackground '{"props":{"sizingH":"fill"}}'
  txt "$LI" "$pct" "Label Small" onSurfaceVariant
done
comp "$V"; prop "$V" Total text '"R$ 3.718"' "$TOT"

group "$C" "ComparisonRow"
G=$R
VARIANTS=()
for trend in Up Down; do
  frame "$G" "Trend=$trend" '{"layout":"horizontal","gap":12,"padding":[12,16,12,16],"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 412
  V=$R; paint "$V" fill surface
  if [[ $trend == Up ]]; then cat=Alimentação; val="R\$ 1.190"; pct="12%"; tone=expense; bg=expenseContainer; ic=trending-up; it=danger
  else cat=Transporte; val="R\$ 632"; pct="8%"; tone=income; bg=incomeContainer; ic=trending-down; it=success; fi
  inst "$V" CategoryAvatar; props_of "$R" "$(jq -cn --arg c "$cat" '{Category:$c}')"
  col "$V" "Text" 2; TX=$R; set_props "$TX" '{"sizingH":"fill"}'
  txt "$TX" "$cat" "Body Strong" onBackground; T=$R
  txt "$TX" "$val em outubro" Caption onSurfaceVariant
  frame "$V" "Delta" '{"layout":"horizontal","gap":4,"padding":[4,8,4,6],"cornerRadius":8,"crossAlign":"center","sizingH":"hug","sizingV":"hug"}'
  DL=$R; paint "$DL" fill "$bg"; icon "$DL" "$it" "$ic"; txt "$DL" "$pct" "Label Small" "$tone"; P=$R
  comp "$V"; prop "$V" Category text "$(jq -cn --arg v "$cat" '$v')" "$T"; prop "$V" Delta text "\"$pct\"" "$P"
  VARIANTS+=("$V")
done
combine "ComparisonRow" "$SET" "${VARIANTS[@]}"

group "$C" "IncomeExpenseBars"
G=$R
frame "$G" "IncomeExpenseBars" '{"layout":"vertical","gap":16,"padding":20,"cornerRadius":20,"sizingH":"fixed","sizingV":"hug"}' 412
V=$R; paint "$V" fill surface
row "$V" "Legend" 16
LG=$R
for k in "Receitas|income" "Despesas|expense"; do
  row "$LG" "Key" 6; K=$R
  frame "$K" "Dot" '{"cornerRadius":3,"sizingH":"fixed","sizingV":"fixed"}' 10 10; paint "$R" fill "${k#*|}"
  txt "$K" "${k%|*}" "Label Small" onSurfaceVariant
done
frame "$V" "Plot" '{"layout":"horizontal","gap":0,"mainAlign":"spaceBetween","crossAlign":"max","sizingH":"fill","sizingV":"fixed","fills":[],"strokeSides":[false,false,true,false],"strokeWeight":1}' "" 160
PL=$R; paint "$PL" stroke outlineVariant
for m in "mai|120|98" "jun|120|110" "jul|124|132" "ago|126|104" "set|128|116" "out|130|78"; do
  IFS='|' read -r lab inc exp <<< "$m"
  frame "$PL" "Month/$lab" '{"layout":"vertical","gap":6,"crossAlign":"center","sizingH":"hug","sizingV":"hug","fills":[]}'
  MO=$R
  row "$MO" "Bars" 4; BR=$R; set_props "$BR" '{"crossAlign":"max"}'
  frame "$BR" "Income" '{"cornerRadius":[6,6,2,2],"sizingH":"fixed","sizingV":"fixed"}' 14 "$inc"; paint "$R" fill income
  frame "$BR" "Expense" '{"cornerRadius":[6,6,2,2],"sizingH":"fixed","sizingV":"fixed"}' 14 "$exp"; paint "$R" fill "$( [[ $lab == out ]] && echo expense || echo expenseContainer)"
done
row "$V" "Labels" 0; LB=$R; set_props "$LB" '{"sizingH":"fill","mainAlign":"spaceBetween","padding":[0,4,0,4]}'
for lab in mai jun jul ago set out; do txt "$LB" "$lab" Caption "$( [[ $lab == out ]] && echo primary || echo onSurfaceVariant)"; done
comp "$V"
