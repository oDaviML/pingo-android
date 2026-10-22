#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
begin "$1"

step page.select '{"index":1}'

board "Board/Buttons" "Botões" "Button: 5 tipos × 4 estados × 2 tamanhos, com propriedades Label (texto) e Icon (liga/desliga). IconButton e FAB para ações compactas e a ação principal (+)." 0 0
B=$R

group "$B" "Button"
G=$R

spec() {
  case "$1:$2" in
    Primary:Default)  echo "primary onPrimary none white" ;;
    Primary:Pressed)  echo "primaryPressed onPrimary none white" ;;
    Primary:Disabled) echo "disabledContainer onDisabled none disabled" ;;
    Primary:Loading)  echo "primary onPrimary none white" ;;
    Tonal:Default)    echo "primaryContainer onPrimaryContainer none deep" ;;
    Tonal:Pressed)    echo "secondary onPrimaryContainer none deep" ;;
    Tonal:Disabled)   echo "disabledContainer onDisabled none disabled" ;;
    Tonal:Loading)    echo "primaryContainer onPrimaryContainer none deep" ;;
    Outline:Default)  echo "none primary primary primary" ;;
    Outline:Pressed)  echo "primaryContainer primary primary primary" ;;
    Outline:Disabled) echo "none onDisabled outlineVariant disabled" ;;
    Outline:Loading)  echo "none primary primary primary" ;;
    Text:Default)     echo "none primary none primary" ;;
    Text:Pressed)     echo "primaryContainer primary none primary" ;;
    Text:Disabled)    echo "none onDisabled none disabled" ;;
    Text:Loading)     echo "none primary none primary" ;;
    Danger:Default)   echo "expense onPrimary none white" ;;
    Danger:Pressed)   echo "expensePressed onPrimary none white" ;;
    Danger:Disabled)  echo "disabledContainer onDisabled none disabled" ;;
    Danger:Loading)   echo "expense onPrimary none white" ;;
  esac
}

VARIANTS=()
for size in Medium Small; do
  if [[ $size == Medium ]]; then pad='[12,20,12,20]'; lstyle=Label; else pad='[6,14,6,14]'; lstyle="Label Small"; fi
  for type in Primary Tonal Outline Text Danger; do
    for state in Default Pressed Disabled Loading; do
      read -r bg fg stroke itone <<< "$(spec $type $state)"
      frame "$G" "Type=$type, State=$state, Size=$size" "{\"layout\":\"horizontal\",\"gap\":8,\"padding\":$pad,\"cornerRadius\":12,\"crossAlign\":\"center\",\"mainAlign\":\"center\",\"sizingH\":\"hug\",\"sizingV\":\"hug\"}"
      V=$R
      paint "$V" fill "$bg"
      if [[ $stroke != none ]]; then paint "$V" stroke "$stroke"; set_props "$V" '{"strokeWeight":1.5,"strokeAlign":"inside"}'; fi
      if [[ $state == Loading ]]; then icon "$V" "$itone" loader-circle; else icon "$V" "$itone" plus; fi
      I=$R
      txt "$V" "Salvar" "$lstyle" "$fg"
      T=$R
      comp "$V"
      prop "$V" Label text '"Salvar"' "$T"
      prop "$V" Icon boolean true "$I"
      VARIANTS+=("$V")
    done
  done
done
combine "Button" '{"layout":"grid","gridColumns":4,"gap":16,"crossGap":16,"padding":24,"sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}' "${VARIANTS[@]}"

group "$B" "IconButton"
G=$R
VARIANTS=()
for st in Filled Tonal Outline Standard; do
  for state in Default Disabled; do
    case "$st:$state" in
      Filled:Default) bg=primary; stroke=none; t=white ;;
      Tonal:Default) bg=primaryContainer; stroke=none; t=deep ;;
      Outline:Default) bg=none; stroke=outline; t=ink ;;
      Standard:Default) bg=none; stroke=none; t=ink ;;
      Outline:Disabled) bg=none; stroke=outlineVariant; t=disabled ;;
      Standard:Disabled) bg=none; stroke=none; t=disabled ;;
      *:Disabled) bg=disabledContainer; stroke=none; t=disabled ;;
    esac
    frame "$G" "Style=$st, State=$state" '{"layout":"horizontal","padding":12,"cornerRadius":12,"mainAlign":"center","crossAlign":"center","sizingH":"hug","sizingV":"hug"}'
    V=$R
    paint "$V" fill "$bg"
    if [[ $stroke != none ]]; then paint "$V" stroke "$stroke"; set_props "$V" '{"strokeWeight":1.5,"strokeAlign":"inside"}'; fi
    icon "$V" "$t" sliders-horizontal
    comp "$V"
    VARIANTS+=("$V")
  done
done
combine "IconButton" '{"layout":"grid","gridColumns":4,"gap":16,"crossGap":16,"padding":24,"sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}' "${VARIANTS[@]}"

group "$B" "FAB"
G=$R
VARIANTS=()
frame "$G" "Type=Regular, State=Default" '{"layout":"horizontal","padding":16,"cornerRadius":18,"mainAlign":"center","crossAlign":"center","sizingH":"hug","sizingV":"hug"}'
V=$R; paint "$V" fill primary; style_fx "$V" "Elevation/2"; icon "$V" white plus; comp "$V"; VARIANTS+=("$V")
frame "$G" "Type=Regular, State=Pressed" '{"layout":"horizontal","padding":16,"cornerRadius":18,"mainAlign":"center","crossAlign":"center","sizingH":"hug","sizingV":"hug"}'
V=$R; paint "$V" fill primaryPressed; style_fx "$V" "Elevation/1"; icon "$V" white plus; comp "$V"; VARIANTS+=("$V")
frame "$G" "Type=Extended, State=Default" '{"layout":"horizontal","gap":10,"padding":[16,22,16,18],"cornerRadius":18,"mainAlign":"center","crossAlign":"center","sizingH":"hug","sizingV":"hug"}'
V=$R; paint "$V" fill primary; style_fx "$V" "Elevation/2"; icon "$V" white plus; txt "$V" "Novo lançamento" Label onPrimary; T=$R; comp "$V"; prop "$V" Label text '"Novo lançamento"' "$T"; VARIANTS+=("$V")
frame "$G" "Type=Extended, State=Pressed" '{"layout":"horizontal","gap":10,"padding":[16,22,16,18],"cornerRadius":18,"mainAlign":"center","crossAlign":"center","sizingH":"hug","sizingV":"hug"}'
V=$R; paint "$V" fill primaryPressed; style_fx "$V" "Elevation/1"; icon "$V" white plus; txt "$V" "Novo lançamento" Label onPrimary; T=$R; comp "$V"; prop "$V" Label text '"Novo lançamento"' "$T"; VARIANTS+=("$V")
combine "FAB" '{"layout":"horizontal","gap":24,"padding":24,"crossAlign":"center","sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}' "${VARIANTS[@]}"
