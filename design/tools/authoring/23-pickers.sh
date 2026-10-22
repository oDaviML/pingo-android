#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
begin "$1"

step page.select '{"index":1}'
SET='{"layout":"horizontal","wrap":true,"gap":32,"crossGap":32,"padding":24,"sizingH":"hug","sizingV":"hug","cornerRadius":16,"stroke":"#B794F6","strokeWeight":1,"dashes":[6,4]}'

board "Board/Pickers" "Datas e horários" "Seletor de mês (cabeçalho das telas), grade de meses, calendário com seleção simples e por período, diálogo de data, dia do mês (fechamento/vencimento) e seletor de hora em relógio e roda." 4800 0
B=$R

group "$B" "MonthSelector"
G=$R
VARIANTS=()
for state in Default Current; do
  frame "$G" "State=$state" '{"layout":"horizontal","gap":4,"crossAlign":"center","mainAlign":"spaceBetween","sizingH":"fixed","sizingV":"hug","fills":[]}' 360
  V=$R
  frame "$V" "Prev" '{"layout":"horizontal","padding":10,"cornerRadius":12,"sizingH":"hug","sizingV":"hug"}'; paint "$R" fill surfaceVariant; icon "$R" ink chevron-left
  frame "$V" "Month" '{"layout":"horizontal","gap":6,"padding":[8,12,8,12],"cornerRadius":12,"crossAlign":"center","sizingH":"hug","sizingV":"hug","fills":[]}'
  M=$R
  txt "$M" "Outubro 2026" Subtitle onBackground; T=$R
  icon "$M" muted chevron-down
  frame "$V" "Next" '{"layout":"horizontal","padding":10,"cornerRadius":12,"sizingH":"hug","sizingV":"hug"}'
  if [[ $state == Current ]]; then paint "$R" fill disabledContainer; icon "$R" disabled chevron-right; else paint "$R" fill surfaceVariant; icon "$R" ink chevron-right; fi
  comp "$V"; prop "$V" Month text '"Outubro 2026"' "$T"
  VARIANTS+=("$V")
done
combine "MonthSelector" "$SET" "${VARIANTS[@]}"

group "$B" "MonthGrid"
G=$R
frame "$G" "MonthGrid" '{"layout":"vertical","gap":16,"padding":20,"cornerRadius":20,"sizingH":"fixed","sizingV":"hug"}' 360
V=$R; paint "$V" fill surface; style_fx "$V" "Elevation/2"
row "$V" "Year" 8; Y=$R; set_props "$Y" '{"sizingH":"fill","mainAlign":"spaceBetween"}'
icon "$Y" ink chevron-left; txt "$Y" "2026" Subtitle onBackground; icon "$Y" ink chevron-right
frame "$V" "Grid" '{"layout":"grid","gridColumns":4,"gap":8,"crossGap":8,"sizingH":"fill","sizingV":"hug","fills":[]}'
GR=$R
i=0
for m in jan fev mar abr mai jun jul ago set out nov dez; do
  i=$((i+1))
  frame "$GR" "Month/$m" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","padding":[10,0,10,0],"cornerRadius":12,"sizingH":"fill","sizingV":"hug"}'
  C=$R
  if [[ $m == out ]]; then paint "$C" fill primary; txt "$C" "$m" Label onPrimary
  elif (( i > 10 )); then paint "$C" fill none; txt "$C" "$m" Label onDisabled
  else paint "$C" fill none; txt "$C" "$m" Label onBackground; fi
done
comp "$V"

calendar() {
  local parent="$1" name="$2" mode="$3"
  frame "$parent" "$name" '{"layout":"vertical","gap":12,"padding":[16,16,20,16],"cornerRadius":20,"sizingH":"fixed","sizingV":"hug"}' 360
  local C=$R
  paint "$C" fill surface
  row "$C" "Header" 8; local H=$R; set_props "$H" '{"sizingH":"fill","mainAlign":"spaceBetween"}'
  frame "$H" "Prev" '{"layout":"horizontal","padding":8,"cornerRadius":10,"sizingH":"hug","sizingV":"hug","fills":[]}'; icon "$R" ink chevron-left
  txt "$H" "Outubro 2026" Subtitle onBackground
  frame "$H" "Next" '{"layout":"horizontal","padding":8,"cornerRadius":10,"sizingH":"hug","sizingV":"hug","fills":[]}'; icon "$R" ink chevron-right
  frame "$C" "Weekdays" '{"layout":"grid","gridColumns":7,"gap":0,"sizingH":"fill","sizingV":"hug","fills":[]}'
  local W=$R
  for d in D S T Q Q S S; do
    frame "$W" "WD" '{"layout":"horizontal","mainAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
    txt "$R" "$d" "Label Small" onSurfaceVariant
  done
  frame "$C" "Days" '{"layout":"grid","gridColumns":7,"gap":0,"crossGap":4,"sizingH":"fill","sizingV":"hug","fills":[]}'
  local D=$R
  for e in 1 2 3 4; do frame "$D" "Empty" '{"sizingH":"fill","sizingV":"fixed","fills":[]}' "" 44; done
  for day in $(seq 1 31); do
    local band=none
    if [[ $mode == Range ]]; then
      if (( day > 10 && day < 17 )); then band=primaryContainer; fi
    fi
    frame "$D" "Day/$day" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","sizingH":"fill","sizingV":"fixed"}' "" 44
    local cell=$R
    paint "$cell" fill "$band"
    frame "$cell" "Dot" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999,"sizingH":"fixed","sizingV":"fixed","strokeAlign":"inside"}' 40 40
    local dot=$R
    local style=Body color=onBackground
    if [[ $mode == Single && $day == 15 ]] || [[ $mode == Range && ( $day == 10 || $day == 17 ) ]]; then
      paint "$dot" fill primary; color=onPrimary; style=Label
    elif [[ $day == 22 ]]; then
      paint "$dot" fill none; paint "$dot" stroke primary; set_props "$dot" '{"strokeWeight":1.5}'; color=primary; style=Label
    else
      paint "$dot" fill none
      if (( day > 22 )); then color=onDisabled; fi
      if [[ $band != none ]]; then color=onPrimaryContainer; fi
    fi
    txt "$dot" "$day" "$style" "$color"
  done
  R=$C
}

group "$B" "DatePicker"
G=$R
calendar "$G" "Mode=Single" Single; S1=$R; comp "$S1"
calendar "$G" "Mode=Range" Range; S2=$R; comp "$S2"
combine "DatePicker" "$SET" "$S1" "$S2"
DP=$R

group "$B" "DatePickerDialog"
G=$R
frame "$G" "DatePickerDialog" '{"layout":"vertical","gap":0,"cornerRadius":24,"sizingH":"fixed","sizingV":"hug","clip":true}' 360
V=$R; paint "$V" fill surface; style_fx "$V" "Elevation/3"
frame "$V" "Header" '{"layout":"vertical","gap":4,"padding":[20,24,16,24],"sizingH":"fill","sizingV":"hug","fills":[]}'
H=$R
txt "$H" "Data do lançamento" Overline onSurfaceVariant
txt "$H" "Qui, 15 de out." Headline onBackground; T=$R
frame "$V" "Divider" '{"sizingH":"fill","sizingV":"fixed"}' "" 1; paint "$R" fill outlineVariant
inst "$V" "$DP"
row "$V" "Actions" 8; A=$R; set_props "$A" '{"sizingH":"fill","mainAlign":"max","padding":[0,16,16,16]}'
inst "$A" Button; props_of "$R" '{"Type":"Text","Size":"Medium","Label":"Cancelar","Icon":false}'
inst "$A" Button; props_of "$R" '{"Type":"Primary","Size":"Medium","Label":"Confirmar","Icon":false}'
comp "$V"; prop "$V" Title text '"Qui, 15 de out."' "$T"

group "$B" "DayOfMonthPicker"
G=$R
frame "$G" "DayOfMonthPicker" '{"layout":"vertical","gap":14,"padding":20,"cornerRadius":20,"sizingH":"fixed","sizingV":"hug"}' 360
V=$R; paint "$V" fill surface; style_fx "$V" "Elevation/2"
txt "$V" "Dia do fechamento" Subtitle onBackground; T=$R
txt "$V" "A fatura fecha neste dia todo mês" Caption onSurfaceVariant
frame "$V" "Days" '{"layout":"grid","gridColumns":7,"gap":6,"crossGap":6,"sizingH":"fill","sizingV":"hug","fills":[]}'
D=$R
for day in $(seq 1 31); do
  frame "$D" "Day/$day" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":10,"sizingH":"fill","sizingV":"fixed"}' "" 38
  C=$R
  if [[ $day == 3 ]]; then paint "$C" fill primary; txt "$C" "$day" Label onPrimary
  else paint "$C" fill surfaceVariant; txt "$C" "$day" Body onBackground; fi
done
comp "$V"; prop "$V" Title text '"Dia do fechamento"' "$T"

group "$B" "TimePicker"
G=$R
VARIANTS=()
frame "$G" "Style=Dial" '{"layout":"vertical","gap":20,"padding":24,"cornerRadius":24,"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 328
V=$R; paint "$V" fill surface; style_fx "$V" "Elevation/3"
txt "$V" "Horário" Overline onSurfaceVariant
row "$V" "Display" 8
DI=$R
frame "$DI" "Hour" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":14,"sizingH":"fixed","sizingV":"fixed","strokeWeight":2,"strokeAlign":"inside"}' 96 72
paint "$R" fill primaryContainer; paint "$R" stroke primary; txt "$R" "14" Display onPrimaryContainer; HT=$R
txt "$DI" ":" Display onBackground
frame "$DI" "Minute" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":14,"sizingH":"fixed","sizingV":"fixed"}' 96 72
paint "$R" fill surfaceVariant; txt "$R" "32" Display onBackground; MT=$R
frame "$V" "Dial" '{"cornerRadius":999,"sizingH":"fixed","sizingV":"fixed"}' 256 256
DL=$R; paint "$DL" fill surfaceVariant
step node.create "$(jq -cn --argjson p "$(ref "$DL")" '{type:"vector", parent:$p, name:"Hand", x:128, y:94, path:"M0 34 L59 0", props:{fills:[], stroke:"#088395", strokeWeight:2, strokeCap:"round"}}')"
frame "$DL" "Center" '{"cornerRadius":999}' 8 8; at "$R" 124 124; paint "$R" fill primary
for i in $(seq 0 11); do
  for ring in outer inner; do
    if [[ $ring == outer ]]; then rad=104; size=36; label=$(printf '%02d' "$i"); [[ $i == 0 ]] && label=00
    else rad=68; size=32; label=$((i + 12)); fi
    read -r x y < <(awk -v i="$i" -v r="$rad" -v s="$size" 'BEGIN{a=(-90+30*i)*3.14159265/180; printf "%d %d\n", 128+r*cos(a)-s/2, 128+r*sin(a)-s/2}')
    frame "$DL" "H$label" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","cornerRadius":999}' "$size" "$size"
    C=$R; at "$C" "$x" "$y"
    if [[ $label == 14 ]]; then paint "$C" fill primary; txt "$C" "$label" Label onPrimary
    else paint "$C" fill none; txt "$C" "$label" "$( [[ $ring == outer ]] && echo 'Body Strong' || echo 'Body Small')" "$( [[ $ring == outer ]] && echo onBackground || echo onSurfaceVariant)"; fi
  done
done
row "$V" "Actions" 8; A=$R; set_props "$A" '{"sizingH":"fill","mainAlign":"max"}'
inst "$A" Button; props_of "$R" '{"Type":"Text","Size":"Medium","Label":"Cancelar","Icon":false}'
inst "$A" Button; props_of "$R" '{"Type":"Primary","Size":"Medium","Label":"OK","Icon":false}'
comp "$V"; prop "$V" Hour text '"14"' "$HT"; prop "$V" Minute text '"32"' "$MT"
VARIANTS+=("$V")

frame "$G" "Style=Wheel" '{"layout":"vertical","gap":16,"padding":24,"cornerRadius":24,"crossAlign":"center","sizingH":"fixed","sizingV":"hug"}' 328
V=$R; paint "$V" fill surface; style_fx "$V" "Elevation/3"
txt "$V" "Horário" Overline onSurfaceVariant
frame "$V" "Wheel" '{"layout":"horizontal","gap":0,"padding":[0,24,0,24],"cornerRadius":16,"crossAlign":"center","mainAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
WH=$R
for colname in Hour Minute; do
  if [[ $colname == Hour ]]; then vals="12 13 14 15 16"; else vals="30 31 32 33 34"; fi
  frame "$WH" "$colname" '{"layout":"vertical","gap":0,"crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
  CL=$R
  j=0
  for v in $vals; do
    j=$((j+1))
    frame "$CL" "Item" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","sizingH":"fill","sizingV":"fixed"}' "" 44
    I=$R
    case $j in
      3) paint "$I" fill primaryContainer; set_props "$I" "$( [[ $colname == Hour ]] && echo '{"cornerRadius":[12,0,0,12]}' || echo '{"cornerRadius":[0,12,12,0]}')"; txt "$I" "$v" Headline onPrimaryContainer ;;
      2|4) paint "$I" fill none; txt "$I" "$v" Title onSurfaceVariant ;;
      *) paint "$I" fill none; txt "$I" "$v" Body onDisabled ;;
    esac
  done
  if [[ $colname == Hour ]]; then
    frame "$WH" "Sep" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","sizingH":"hug","sizingV":"fixed"}' "" 44
    paint "$R" fill primaryContainer; txt "$R" ":" Headline onPrimaryContainer
  fi
done
row "$V" "Actions" 8; A=$R; set_props "$A" '{"sizingH":"fill","mainAlign":"max"}'
inst "$A" Button; props_of "$R" '{"Type":"Text","Size":"Medium","Label":"Cancelar","Icon":false}'
inst "$A" Button; props_of "$R" '{"Type":"Primary","Size":"Medium","Label":"OK","Icon":false}'
comp "$V"
VARIANTS+=("$V")
combine "TimePicker" "$SET" "${VARIANTS[@]}"
