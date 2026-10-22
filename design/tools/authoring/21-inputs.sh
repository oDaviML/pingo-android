#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
begin "$1"

step page.select '{"index":1}'
SET='{"layout":"horizontal","wrap":true,"gap":24,"crossGap":24,"padding":24,"sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'
SETW='{"layout":"horizontal","wrap":true,"gap":24,"crossGap":24,"padding":24,"sizingH":"fixed","width":1100,"sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'

board "Board/Inputs" "Campos de entrada" "TextField com 6 estados, busca, select (fechado/aberto), campo de valor, área de texto, PIN de desbloqueio e teclado numérico do lançamento manual." 1600 0
B=$R

group "$B" "TextField"
G=$R
VARIANTS=()
for state in Default Focused Filled Error Disabled ReadOnly; do
  case $state in
    Default)  bg=surface; st=outline; sw=1; val="Ex.: Padaria do Zé"; vc=onSurfaceVariant; sup="Como aparece na notificação"; sc=onSurfaceVariant; lc=onSurfaceVariant; it=muted ;;
    Focused)  bg=surface; st=primary; sw=2; val="Padaria do"; vc=onBackground; sup="Como aparece na notificação"; sc=onSurfaceVariant; lc=primary; it=primary ;;
    Filled)   bg=surface; st=outline; sw=1; val="Padaria do Zé"; vc=onBackground; sup="Como aparece na notificação"; sc=onSurfaceVariant; lc=onSurfaceVariant; it=muted ;;
    Error)    bg=surface; st=expense; sw=2; val=""; vc=onBackground; sup="Informe o estabelecimento"; sc=expense; lc=expense; it=danger ;;
    Disabled) bg=disabledContainer; st=outlineVariant; sw=1; val="Padaria do Zé"; vc=onDisabled; sup="Bloqueado em lançamentos capturados"; sc=onDisabled; lc=onDisabled; it=disabled ;;
    ReadOnly) bg=surfaceVariant; st=none; sw=0; val="Padaria do Zé"; vc=onBackground; sup="Lido da notificação"; sc=onSurfaceVariant; lc=onSurfaceVariant; it=muted ;;
  esac
  frame "$G" "State=$state" '{"layout":"vertical","gap":6,"sizingH":"fixed","sizingV":"hug","fills":[]}' 320
  V=$R
  txt "$V" "Estabelecimento" "Label Small" "$lc"
  L=$R
  frame "$V" "Field" '{"layout":"horizontal","gap":10,"padding":[13,14,13,14],"cornerRadius":12,"crossAlign":"center","sizingH":"fill","sizingV":"hug","strokeAlign":"inside"}'
  F=$R
  paint "$F" fill "$bg"
  if [[ $st != none ]]; then paint "$F" stroke "$st"; set_props "$F" "{\"strokeWeight\":$sw}"; fi
  iconi "$F" store Leading; LI=$R; SW=1
  if [[ $state == Disabled ]]; then set_props "$LI" '{"opacity":0.45}'; fi
  txt "$F" "${val:- }" Body "$vc" '{"props":{"sizingH":"fill"}}'
  T=$R
  if [[ $state == Focused ]]; then frame "$F" "Caret" '{"sizingH":"fixed","sizingV":"fixed","cornerRadius":1}' 2 22; paint "$R" fill primary; fi
  if [[ $state == Filled || $state == Focused ]]; then icon "$F" muted x Trailing; fi
  if [[ $state == Error ]]; then icon "$F" danger triangle-alert Trailing; fi
  if [[ $state == ReadOnly ]]; then icon "$F" muted lock Trailing; fi
  txt "$V" "$sup" Caption "$sc"
  S=$R
  comp "$V"
  prop "$V" Label text '"Estabelecimento"' "$L"
  prop "$V" Value text "$(jq -cn --arg v "${val:- }" '$v')" "$T"
  prop "$V" Supporting text "$(jq -cn --arg v "$sup" '$v')" "$S"
  prop "$V" "Leading icon" boolean true "$LI"
  if [[ $SW == 1 ]]; then prop "$V" Leading instanceSwap "$(iconref store)" "$LI"; fi
  VARIANTS+=("$V")
done
combine "TextField" "$SETW" "${VARIANTS[@]}"

group "$B" "SearchField"
G=$R
VARIANTS=()
for state in Default Typing; do
  frame "$G" "State=$state" '{"layout":"horizontal","gap":10,"padding":[12,14,12,14],"cornerRadius":12,"crossAlign":"center","sizingH":"fixed","sizingV":"hug","strokeAlign":"inside"}' 360
  V=$R
  paint "$V" fill surfaceVariant
  if [[ $state == Typing ]]; then
    paint "$V" stroke primary; set_props "$V" '{"strokeWeight":2}'
    icon "$V" primary search; txt "$V" "ifood" Body onBackground '{"props":{"sizingH":"fill"}}'; T=$R; icon "$V" muted x Clear
  else
    icon "$V" muted search; txt "$V" "Buscar lançamentos" Body onSurfaceVariant '{"props":{"sizingH":"fill"}}'; T=$R
  fi
  comp "$V"
  prop "$V" Query text "$(jq -cn --arg v "$( [[ $state == Typing ]] && echo ifood || echo 'Buscar lançamentos')" '$v')" "$T"
  VARIANTS+=("$V")
done
combine "SearchField" "$SET" "${VARIANTS[@]}"

group "$B" "Select"
G=$R
VARIANTS=()
for state in Closed Open Disabled; do
  frame "$G" "State=$state" '{"layout":"vertical","gap":6,"sizingH":"fixed","sizingV":"hug","fills":[]}' 320
  V=$R
  txt "$V" "Instrumento" "Label Small" "$( [[ $state == Open ]] && echo primary || ([[ $state == Disabled ]] && echo onDisabled || echo onSurfaceVariant))"
  L=$R
  frame "$V" "Field" '{"layout":"horizontal","gap":10,"padding":[13,14,13,14],"cornerRadius":12,"crossAlign":"center","sizingH":"fill","sizingV":"hug","strokeAlign":"inside"}'
  F=$R
  if [[ $state == Disabled ]]; then paint "$F" fill disabledContainer; paint "$F" stroke outlineVariant; set_props "$F" '{"strokeWeight":1}'
  elif [[ $state == Open ]]; then paint "$F" fill surface; paint "$F" stroke primary; set_props "$F" '{"strokeWeight":2}'
  else paint "$F" fill surface; paint "$F" stroke outline; set_props "$F" '{"strokeWeight":1}'; fi
  iconi "$F" credit-card Leading; LI=$R; SW=1
  if [[ $state == Disabled ]]; then set_props "$LI" '{"opacity":0.45}'; fi
  txt "$F" "Nubank · crédito" Body "$( [[ $state == Disabled ]] && echo onDisabled || echo onBackground)" '{"props":{"sizingH":"fill"}}'
  T=$R
  icon "$F" "$( [[ $state == Disabled ]] && echo disabled || echo muted)" "$( [[ $state == Open ]] && echo chevron-up || echo chevron-down)" Chevron
  if [[ $state == Open ]]; then
    frame "$V" "Menu" '{"layout":"vertical","gap":2,"padding":6,"cornerRadius":16,"sizingH":"fill","sizingV":"hug"}'
    M=$R; paint "$M" fill surface; style_fx "$M" "Elevation/2"
    for opt in "Nubank · crédito|1" "Inter · débito|0" "Itaú · débito|0" "Caju · VR|0"; do
      label="${opt%|*}"; sel="${opt#*|}"
      frame "$M" "Option" '{"layout":"horizontal","gap":12,"cornerRadius":10,"padding":[12,12,12,12],"crossAlign":"center","sizingH":"fill","sizingV":"hug"}'
      O=$R
      if [[ $sel == 1 ]]; then paint "$O" fill primaryContainer; else paint "$O" fill none; fi
      txt "$O" "$label" "$( [[ $sel == 1 ]] && echo 'Body Strong' || echo Body)" "$( [[ $sel == 1 ]] && echo onPrimaryContainer || echo onBackground)" '{"props":{"sizingH":"fill"}}'
      if [[ $sel == 1 ]]; then icon "$O" deep check; fi
    done
  fi
  comp "$V"
  prop "$V" Label text '"Instrumento"' "$L"
  prop "$V" Value text '"Nubank · crédito"' "$T"
  prop "$V" "Leading icon" boolean true "$LI"
  if [[ $SW == 1 ]]; then prop "$V" Leading instanceSwap "$(iconref credit-card)" "$LI"; fi
  VARIANTS+=("$V")
done
combine "Select" "$SET" "${VARIANTS[@]}"

group "$B" "AmountField"
G=$R
VARIANTS=()
for state in Empty Filled Error; do
  frame "$G" "State=$state" '{"layout":"vertical","gap":6,"padding":[20,16,20,16],"cornerRadius":20,"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 320
  V=$R
  paint "$V" fill "$( [[ $state == Error ]] && echo expenseContainer || echo surfaceVariant)"
  txt "$V" "Valor" "Label Small" "$( [[ $state == Error ]] && echo expense || echo onSurfaceVariant)"
  frame "$V" "Amount" '{"layout":"horizontal","gap":6,"crossAlign":"baseline","sizingH":"hug","sizingV":"hug","fills":[]}'
  A=$R
  txt "$A" "R\$" Subtitle "$( [[ $state == Empty ]] && echo onSurfaceVariant || echo brand)"
  if [[ $state == Filled ]]; then txt "$A" "42,90" "Amount Large" brand; else txt "$A" "0,00" "Amount Large" "$( [[ $state == Error ]] && echo expense || echo onSurfaceVariant)"; fi
  T=$R
  if [[ $state == Error ]]; then txt "$V" "O valor precisa ser maior que zero" Caption expense; else txt "$V" "Digite no teclado abaixo" Caption onSurfaceVariant; fi
  S=$R
  comp "$V"
  prop "$V" Value text "$( [[ $state == Filled ]] && echo '"42,90"' || echo '"0,00"')" "$T"
  prop "$V" Supporting text "$(jq -cn --arg v "$( [[ $state == Error ]] && echo 'O valor precisa ser maior que zero' || echo 'Digite no teclado abaixo')" '$v')" "$S"
  VARIANTS+=("$V")
done
combine "AmountField" "$SET" "${VARIANTS[@]}"

group "$B" "TextArea"
G=$R
frame "$G" "TextArea" '{"layout":"vertical","gap":6,"sizingH":"fixed","sizingV":"hug","fills":[]}' 360
V=$R
txt "$V" "Observação" "Label Small" onSurfaceVariant
L=$R
frame "$V" "Field" '{"layout":"vertical","gap":0,"padding":14,"cornerRadius":12,"sizingH":"fill","sizingV":"fixed","strokeAlign":"inside"}' "" 112
F=$R; paint "$F" fill surface; paint "$F" stroke outline; set_props "$F" '{"strokeWeight":1}'
txt "$F" "Almoço com o time, dividido no Pix depois." Body onBackground '{"props":{"sizingH":"fill"}}'
T=$R
txt "$V" "42/140" Caption onSurfaceVariant '{"props":{"textAlign":"right","sizingH":"fill"}}'
comp "$V"
prop "$V" Label text '"Observação"' "$L"
prop "$V" Value text '"Almoço com o time, dividido no Pix depois."' "$T"

group "$B" "PinInput"
G=$R
VARIANTS=()
for state in Empty Partial Error; do
  frame "$G" "State=$state" '{"layout":"horizontal","gap":16,"padding":[16,20,16,20],"crossAlign":"center","sizingH":"hug","sizingV":"hug","fills":[]}'
  V=$R
  for k in 1 2 3 4 5 6; do
    frame "$V" "Dot $k" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed","strokeAlign":"inside"}' 18 18
    D=$R
    if [[ $state == Error ]]; then paint "$D" fill expense
    elif [[ $state == Partial && $k -le 4 ]]; then paint "$D" fill primary
    else paint "$D" fill none; paint "$D" stroke outline; set_props "$D" '{"strokeWeight":2}'; fi
  done
  comp "$V"
  VARIANTS+=("$V")
done
combine "PinInput" "$SET" "${VARIANTS[@]}"

group "$B" "NumericKeypad"
G=$R
frame "$G" "NumericKeypad" '{"layout":"grid","gridColumns":3,"gap":8,"crossGap":8,"padding":12,"cornerRadius":20,"sizingH":"fixed","sizingV":"hug"}' 360
K=$R
paint "$K" fill surfaceVariant
for key in 1 2 3 4 5 6 7 8 9 , 0 DEL; do
  frame "$K" "Key $key" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":14,"sizingH":"fill","sizingV":"fixed"}' "" 56
  KK=$R
  if [[ $key == DEL ]]; then paint "$KK" fill none; icon "$KK" ink delete; else paint "$KK" fill surface; txt "$KK" "$key" Title onBackground; fi
done
comp "$K"
