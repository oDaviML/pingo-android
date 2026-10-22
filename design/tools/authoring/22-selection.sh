#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
begin "$1"

step page.select '{"index":1}'
SET='{"layout":"horizontal","wrap":true,"gap":24,"crossGap":24,"padding":24,"crossAlign":"center","sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'

board "Board/Selection" "Seleção e filtros" "Checkbox, Radio e Switch com estados; chips de filtro (com e sem menu), chip removível, chip de categoria, controle segmentado e abas." 3200 0
B=$R

group "$B" "Checkbox"
G=$R
VARIANTS=()
for state in Unchecked Checked Indeterminate Disabled; do
  frame "$G" "State=$state" '{"layout":"horizontal","gap":12,"crossAlign":"center","sizingH":"fixed","sizingV":"hug","fills":[]}' 300
  V=$R
  frame "$V" "Box" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":6,"sizingH":"fixed","sizingV":"fixed","strokeAlign":"inside"}' 24 24
  X=$R
  case $state in
    Unchecked) paint "$X" fill surface; paint "$X" stroke onSurfaceVariant; set_props "$X" '{"strokeWeight":2}' ;;
    Checked) paint "$X" fill primary; icon "$X" white check Mark ;;
    Indeterminate) paint "$X" fill primary; icon "$X" white minus Mark ;;
    Disabled) paint "$X" fill disabledContainer; paint "$X" stroke onDisabled; set_props "$X" '{"strokeWeight":2}' ;;
  esac
  txt "$V" "Lembrar regra" Body "$( [[ $state == Disabled ]] && echo onDisabled || echo onBackground)" '{"props":{"sizingH":"fill","textAutoResize":"height"}}'
  T=$R
  comp "$V"; prop "$V" Label text '"Lembrar regra"' "$T"
  VARIANTS+=("$V")
done
combine "Checkbox" "$SET" "${VARIANTS[@]}"

group "$B" "Radio"
G=$R
VARIANTS=()
for state in Unselected Selected Disabled; do
  frame "$G" "State=$state" '{"layout":"horizontal","gap":12,"crossAlign":"center","sizingH":"fixed","sizingV":"hug","fills":[]}' 300
  V=$R
  frame "$V" "Ring" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed","strokeAlign":"inside","strokeWeight":2}' 24 24
  X=$R
  paint "$X" fill none
  case $state in
    Unselected) paint "$X" stroke onSurfaceVariant ;;
    Selected) paint "$X" stroke primary; frame "$X" "Dot" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 12 12; paint "$R" fill primary ;;
    Disabled) paint "$X" stroke onDisabled ;;
  esac
  txt "$V" "5 minutos" Body "$( [[ $state == Disabled ]] && echo onDisabled || echo onBackground)" '{"props":{"sizingH":"fill","textAutoResize":"height"}}'
  T=$R
  comp "$V"; prop "$V" Label text '"5 minutos"' "$T"
  VARIANTS+=("$V")
done
combine "Radio" "$SET" "${VARIANTS[@]}"

group "$B" "Switch"
G=$R
VARIANTS=()
for state in Off On DisabledOff DisabledOn; do
  frame "$G" "State=$state" '{"layout":"horizontal","padding":4,"cornerRadius":999,"crossAlign":"center","sizingH":"fixed","sizingV":"fixed","strokeAlign":"inside"}' 52 32
  V=$R
  case $state in
    Off) paint "$V" fill surfaceVariant; paint "$V" stroke outline; set_props "$V" '{"strokeWeight":2,"mainAlign":"min"}'; th=onSurfaceVariant; ts=16 ;;
    On) paint "$V" fill primary; set_props "$V" '{"mainAlign":"max"}'; th=onPrimary; ts=24 ;;
    DisabledOff) paint "$V" fill disabledContainer; set_props "$V" '{"mainAlign":"min"}'; th=onDisabled; ts=16 ;;
    DisabledOn) paint "$V" fill onDisabled; set_props "$V" '{"mainAlign":"max"}'; th=surface; ts=24 ;;
  esac
  if [[ $ts == 16 ]]; then set_props "$V" '{"padding":[4,8,4,8]}'; fi
  frame "$V" "Thumb" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' "$ts" "$ts"
  paint "$R" fill "$th"
  comp "$V"
  VARIANTS+=("$V")
done
combine "Switch" "$SET" "${VARIANTS[@]}"

group "$B" "FilterChip"
G=$R
VARIANTS=()
for kind in Toggle Menu; do
  for state in Unselected Selected; do
    frame "$G" "Kind=$kind, State=$state" '{"layout":"horizontal","gap":6,"padding":[7,14,7,14],"cornerRadius":10,"crossAlign":"center","sizingH":"hug","sizingV":"hug","strokeAlign":"inside","strokeWeight":1}'
    V=$R
    if [[ $state == Selected ]]; then paint "$V" fill primaryContainer; paint "$V" stroke primary; c=onPrimaryContainer; it=deep; icon "$V" deep check Check
    else paint "$V" fill surface; paint "$V" stroke outline; c=onBackground; it=muted; fi
    if [[ $kind == Menu ]]; then lbl="Outubro"; else lbl="Despesas"; fi
    txt "$V" "$lbl" Label "$c"; T=$R
    if [[ $kind == Menu ]]; then icon "$V" "$it" chevron-down Chevron; fi
    comp "$V"; prop "$V" Label text "$(jq -cn --arg v "$lbl" '$v')" "$T"
    VARIANTS+=("$V")
  done
done
combine "FilterChip" "$SET" "${VARIANTS[@]}"

group "$B" "InputChip"
G=$R
frame "$G" "InputChip" '{"layout":"horizontal","gap":6,"padding":[6,8,6,12],"cornerRadius":10,"crossAlign":"center","sizingH":"hug","sizingV":"hug"}'
V=$R; paint "$V" fill primaryContainer
txt "$V" "Nubank" Label onPrimaryContainer; T=$R
icon "$V" deep x Remove
comp "$V"; prop "$V" Label text '"Nubank"' "$T"

group "$B" "CategoryChip"
G=$R
VARIANTS=()
for state in Unselected Selected; do
  frame "$G" "State=$state" '{"layout":"vertical","gap":8,"padding":[14,10,12,10],"cornerRadius":16,"crossAlign":"center","sizingH":"fixed","sizingV":"hug","strokeAlign":"inside"}' 96
  V=$R
  if [[ $state == Selected ]]; then paint "$V" fill primaryContainer; paint "$V" stroke primary; set_props "$V" '{"strokeWeight":2}'
  else paint "$V" fill surface; paint "$V" stroke outlineVariant; set_props "$V" '{"strokeWeight":1}'; fi
  frame "$V" "Badge" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 40 40
  paint "$R" fill "$( [[ $state == Selected ]] && echo primary || echo surfaceVariant)"
  icon "$R" "$( [[ $state == Selected ]] && echo white || echo deep)" utensils
  txt "$V" "Alimentação" "Label Small" "$( [[ $state == Selected ]] && echo onPrimaryContainer || echo onBackground)"; T=$R
  comp "$V"; prop "$V" Label text '"Alimentação"' "$T"
  VARIANTS+=("$V")
done
combine "CategoryChip" "$SET" "${VARIANTS[@]}"

group "$B" "SegmentedControl"
G=$R
VARIANTS=()
for sel in Despesa Receita Transferência; do
  frame "$G" "Selected=$sel" '{"layout":"horizontal","gap":4,"padding":4,"cornerRadius":12,"sizingH":"fixed","sizingV":"hug"}' 360
  V=$R; paint "$V" fill surfaceVariant
  for opt in Despesa Receita Transferência; do
    case $opt in Despesa) tone=expense ;; Receita) tone=income ;; *) tone=transfer ;; esac
    frame "$V" "Segment/$opt" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","padding":[9,8,9,8],"cornerRadius":9,"sizingH":"fill","sizingV":"hug"}'
    S=$R
    if [[ $opt == "$sel" ]]; then paint "$S" fill surface; style_fx "$S" "Elevation/1"; txt "$S" "$opt" Label "$tone"
    else paint "$S" fill none; txt "$S" "$opt" Label onSurfaceVariant; fi
  done
  comp "$V"
  VARIANTS+=("$V")
done
combine "SegmentedControl" "$SET" "${VARIANTS[@]}"

group "$B" "Tabs"
G=$R
VARIANTS=()
for sel in Aberta Próximas Fechadas; do
  frame "$G" "Selected=$sel" '{"layout":"horizontal","gap":0,"sizingH":"fixed","sizingV":"hug","strokeAlign":"inside","strokeSides":[false,false,true,false],"strokeWeight":1}' 360
  V=$R; paint "$V" fill none; paint "$V" stroke outlineVariant
  for opt in Aberta Próximas Fechadas; do
    frame "$V" "Tab/$opt" '{"layout":"vertical","gap":10,"padding":[12,0,0,0],"crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
    T=$R
    if [[ $opt == "$sel" ]]; then txt "$T" "$opt" Label primary; frame "$T" "Indicator" '{"cornerRadius":[3,3,0,0],"sizingH":"fixed","sizingV":"fixed"}' 48 3; paint "$R" fill primary
    else txt "$T" "$opt" Label onSurfaceVariant; frame "$T" "Indicator" '{"sizingH":"fixed","sizingV":"fixed","fills":[]}' 48 3; fi
  done
  comp "$V"
  VARIANTS+=("$V")
done
combine "Tabs" "$SET" "${VARIANTS[@]}"
