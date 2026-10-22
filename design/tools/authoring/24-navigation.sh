#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
begin "$1"

step page.select '{"index":1}'
SET='{"layout":"vertical","gap":24,"padding":24,"sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'
SETH='{"layout":"horizontal","wrap":true,"gap":24,"crossGap":24,"padding":24,"sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'

board "Board/Navigation" "Navegação" "Barras do sistema, top app bars (início, título grande, interna e modal), barra inferior com 4 abas e o + em gota, stepper do onboarding, bottom sheet e cabeçalho de seção." 6400 0
B=$R

group "$B" "System bars"
G=$R
frame "$G" "StatusBar" '{"layout":"horizontal","mainAlign":"spaceBetween","crossAlign":"center","padding":[0,20,0,24],"sizingH":"fixed","sizingV":"fixed","fills":[]}' 412 36
V=$R
txt "$V" "14:32" "Label" onBackground
row "$V" "Icons" 6; I=$R
icon "$I" ink wifi; icon "$I" ink battery-charging
comp "$V"
frame "$G" "GestureBar" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","sizingH":"fixed","sizingV":"fixed","fills":[]}' 412 24
V=$R
frame "$V" "Handle" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 108 4; paint "$R" fill onBackground
comp "$V"
GB=$V

group "$B" "TopAppBar"
G=$R
VARIANTS=()

frame "$G" "Type=Home" '{"layout":"horizontal","gap":12,"padding":[12,16,12,20],"crossAlign":"center","sizingH":"fixed","sizingV":"hug","fills":[]}' 412
V=$R
step node.create '{"type":"vector","parent":{"$step":'"$V"'},"name":"Gota","path":"M17 1 C17 1 33 19 33 26 A16 16 0 1 1 1 26 C1 19 17 1 17 1 Z","props":{"fills":[{"type":"linearGradient","stops":["#1AA7BC","#088395","#09637E"],"from":[0.5,0],"to":[0.5,1]}]}}'
col "$V" "Greeting" 0; GR=$R; set_props "$GR" '{"sizingH":"fill"}'
txt "$GR" "Boa tarde," Caption onSurfaceVariant
txt "$GR" "Lucas" Title onBackground; T=$R
frame "$V" "Bell" '{"layout":"horizontal","padding":10,"cornerRadius":12,"sizingH":"hug","sizingV":"hug"}'; BL=$R; paint "$BL" fill surface; icon "$BL" ink bell
frame "$V" "Settings" '{"layout":"horizontal","padding":10,"cornerRadius":12,"sizingH":"hug","sizingV":"hug"}'; paint "$R" fill surface; icon "$R" ink settings
comp "$V"; prop "$V" Name text '"Lucas"' "$T"
VARIANTS+=("$V")

frame "$G" "Type=Large" '{"layout":"vertical","gap":4,"padding":[12,20,16,20],"sizingH":"fixed","sizingV":"hug","fills":[]}' 412
V=$R
row "$V" "Actions" 8; A=$R; set_props "$A" '{"sizingH":"fill","mainAlign":"max"}'
frame "$A" "Search" '{"layout":"horizontal","padding":10,"cornerRadius":12,"sizingH":"hug","sizingV":"hug","fills":[]}'; icon "$R" ink search
frame "$A" "More" '{"layout":"horizontal","padding":10,"cornerRadius":12,"sizingH":"hug","sizingV":"hug","fills":[]}'; icon "$R" ink ellipsis-vertical
txt "$V" "Lançamentos" Display onBackground; T=$R
comp "$V"; prop "$V" Title text '"Lançamentos"' "$T"
VARIANTS+=("$V")

frame "$G" "Type=Small" '{"layout":"horizontal","gap":8,"padding":[8,8,8,8],"crossAlign":"center","sizingH":"fixed","sizingV":"hug","fills":[]}' 412
V=$R
frame "$V" "Back" '{"layout":"horizontal","padding":12,"cornerRadius":12,"sizingH":"hug","sizingV":"hug","fills":[]}'; icon "$R" ink arrow-left
txt "$V" "Detalhe do lançamento" Subtitle onBackground '{"props":{"sizingH":"fill"}}'; T=$R
frame "$V" "Action" '{"layout":"horizontal","padding":12,"cornerRadius":12,"sizingH":"hug","sizingV":"hug","fills":[]}'; ACF=$R; iconi "$ACF" trash-2 "Action icon"; ACI=$R
comp "$V"; prop "$V" Title text '"Detalhe do lançamento"' "$T"; prop "$V" Action instanceSwap "$(iconref trash-2)" "$ACI"; prop "$V" "Show action" boolean true "$ACF"
VARIANTS+=("$V")

frame "$G" "Type=Modal" '{"layout":"horizontal","gap":8,"padding":[8,12,8,8],"crossAlign":"center","sizingH":"fixed","sizingV":"hug","fills":[]}' 412
V=$R
frame "$V" "Close" '{"layout":"horizontal","padding":12,"cornerRadius":12,"sizingH":"hug","sizingV":"hug","fills":[]}'; icon "$R" ink x
txt "$V" "Novo lançamento" Subtitle onBackground '{"props":{"sizingH":"fill"}}'; T=$R
inst "$V" Button; props_of "$R" '{"Type":"Primary","Size":"Small","Label":"Salvar","Icon":false}'
comp "$V"; prop "$V" Title text '"Novo lançamento"' "$T"
VARIANTS+=("$V")
combine "TopAppBar" "$SET" "${VARIANTS[@]}"

group "$B" "NavItem"
G=$R
VARIANTS=()
for state in Inactive Active; do
  for tab in "Início|house" "Lançamentos|receipt" "Contas|wallet" "Relatórios|chart-pie"; do
    label="${tab%|*}"; ic="${tab#*|}"
    frame "$G" "Tab=$label, State=$state" '{"layout":"vertical","gap":4,"padding":[12,0,14,0],"crossAlign":"center","sizingH":"fixed","sizingV":"hug","fills":[]}' 80
    V=$R
    frame "$V" "Indicator" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 56 32
    IN=$R
    if [[ $state == Active ]]; then paint "$IN" fill primaryContainer; icon "$IN" deep "$ic"; txt "$V" "$label" "Label Small" primary; set_props "$R" '{"fontStyle":"ExtraBold"}'
    else paint "$IN" fill none; icon "$IN" muted "$ic"; txt "$V" "$label" "Label Small" onSurfaceVariant; fi
    comp "$V"
    VARIANTS+=("$V")
  done
done
combine "NavItem" "$SETH" "${VARIANTS[@]}"
NAV=$R

group "$B" "AddDrop"
G=$R
frame "$G" "AddDrop" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","sizingH":"fixed","sizingV":"fixed","fills":[]}' 64 72
AW=$R
frame "$AW" "Drop" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":[6,26,26,26],"rotation":-45,"sizingH":"fixed","sizingV":"fixed"}' 50 50
ADD=$R; paint "$ADD" fill primary; style_fx "$ADD" "Elevation/2"
icon "$ADD" white plus; set_props "$R" '{"rotation":45}'
comp "$AW"
ADDDROP=$AW

group "$B" "BottomNavBar"
G=$R
VARIANTS=()
for active in Início Lançamentos Contas Relatórios; do
  frame "$G" "Active=$active" '{"layout":"vertical","gap":0,"sizingH":"fixed","sizingV":"hug","clip":false}' 412
  V=$R; paint "$V" fill surface; style_fx "$V" "Elevation/2"
  frame "$V" "Bar" '{"layout":"horizontal","gap":0,"padding":[0,8,0,8],"mainAlign":"spaceBetween","crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
  BAR=$R
  for tab in Início Lançamentos FAB Contas Relatórios; do
    if [[ $tab == FAB ]]; then
      frame "$BAR" "FAB slot" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","sizingH":"fixed","sizingV":"fixed","fills":[]}' 64 72
      SLOT=$R
      inst "$SLOT" "$ADDDROP"
      continue
    fi
    inst "$BAR" "$NAV"
    st=Inactive; [[ $tab == "$active" ]] && st=Active
    props_of "$R" "$(jq -cn --arg t "$tab" --arg s "$st" '{Tab:$t, State:$s}')"
  done
  inst "$V" "$GB"
  comp "$V"
  VARIANTS+=("$V")
done
combine "BottomNavBar" "$SET" "${VARIANTS[@]}"

group "$B" "OnboardingStepper"
G=$R
VARIANTS=()
for s in 1 2 3 4 5 6 7 8; do
  frame "$G" "Step=$s" '{"layout":"vertical","gap":8,"sizingH":"fixed","sizingV":"hug","fills":[]}' 360
  V=$R
  row "$V" "Meta" 8; M=$R; set_props "$M" '{"sizingH":"fill","mainAlign":"spaceBetween"}'
  txt "$M" "Passo $s de 8" "Label Small" primary
  frame "$M" "Skip" '{"layout":"horizontal","sizingH":"hug","sizingV":"hug","fills":[]}'
  txt "$R" "Pular" "Label Small" onSurfaceVariant
  frame "$V" "Segments" '{"layout":"horizontal","gap":4,"sizingH":"fill","sizingV":"hug","fills":[]}'
  SG=$R
  for k in 1 2 3 4 5 6 7 8; do
    frame "$SG" "Seg $k" '{"cornerRadius":999,"sizingH":"fill","sizingV":"fixed"}' "" 4
    if (( k <= s )); then paint "$R" fill primary; else paint "$R" fill surfaceVariant; fi
  done
  comp "$V"
  VARIANTS+=("$V")
done
combine "OnboardingStepper" "$SETH" "${VARIANTS[@]}"

group "$B" "BottomSheet"
G=$R
frame "$G" "BottomSheet" '{"layout":"vertical","gap":16,"padding":[12,20,24,20],"cornerRadius":[28,28,0,0],"sizingH":"fixed","sizingV":"hug"}' 412
V=$R; paint "$V" fill surface; style_fx "$V" "Elevation/3"
frame "$V" "HandleRow" '{"layout":"horizontal","mainAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
frame "$R" "Handle" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 36 4; paint "$R" fill outline
txt "$V" "Trocar categoria" Title onBackground; T=$R
txt "$V" "Padaria do Zé · - R\$ 42,90" "Body Small" onSurfaceVariant; S=$R
frame "$V" "Content" '{"layout":"horizontal","wrap":true,"gap":8,"crossGap":10,"sizingH":"fill","sizingV":"hug","fills":[]}'
CT=$R
frame "$CT" "Placeholder" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":16,"sizingH":"fill","sizingV":"fixed","strokeWeight":1,"dashes":[4,4]}' "" 96
PH=$R; paint "$PH" fill surfaceVariant; paint "$PH" stroke outline
txt "$PH" "Conteúdo (slot)" "Label Small" onSurfaceVariant
row "$V" "Actions" 12; A=$R; set_props "$A" '{"sizingH":"fill","mainAlign":"max"}'
inst "$A" Button; props_of "$R" '{"Type":"Text","Size":"Medium","Label":"Cancelar","Icon":false}'
inst "$A" Button; props_of "$R" '{"Type":"Primary","Size":"Medium","Label":"Aplicar","Icon":false}'
comp "$V"
prop "$V" Title text '"Trocar categoria"' "$T"
prop "$V" Subtitle text '"Padaria do Zé · - R$ 42,90"' "$S"
step component.makeSlot "$(jq -cn --argjson r "$(ref "$CT")" '{id:$r}')"

group "$B" "SectionHeader"
G=$R
frame "$G" "SectionHeader" '{"layout":"horizontal","mainAlign":"spaceBetween","crossAlign":"center","padding":[0,4,0,4],"sizingH":"fixed","sizingV":"hug","fills":[]}' 372
V=$R
txt "$V" "Últimos lançamentos" Subtitle onBackground; T=$R
frame "$V" "Link" '{"layout":"horizontal","gap":2,"crossAlign":"center","sizingH":"hug","sizingV":"hug","fills":[]}'; L=$R
txt "$L" "Ver tudo" Label primary; LT=$R
icon "$L" primary chevron-right
comp "$V"; prop "$V" Title text '"Últimos lançamentos"' "$T"; prop "$V" Action text '"Ver tudo"' "$LT"; prop "$V" "Show action" boolean true "$L"

group "$B" "SheetLabel"
G=$R
frame "$G" "SheetLabel" '{"layout":"horizontal","padding":[10,0,0,0],"sizingH":"fixed","sizingV":"hug","fills":[]}' 372
V=$R
txt "$V" "Categoria" Overline onSurfaceVariant; T=$R
comp "$V"; prop "$V" Text text '"Categoria"' "$T"
