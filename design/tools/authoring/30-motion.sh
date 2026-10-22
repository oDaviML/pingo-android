#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/lib.sh"
begin "$1"

step page.select '{"index":2}'

board "Board/Pingar" "Microanimação “pingar”" "Quando uma transação é capturada: a gota cai, toca o topo da lista, abre duas ondas e o item entra com fade + deslize. 400 ms no total, uma vez, curva padrão. Os 5 quadros estão ligados por Smart Animate (Apresentar a partir do Quadro 0)." 0 0
B=$R

FRAMES=()
for k in 0 1 2 3 4; do
  case $k in
    0) t="0 ms · gota surge"; dx=140; dy=8; dw=20; dh=24; dop=0; r1=0; r1o=0; r2=0; r2o=0; iy=136; io=0; hl=0 ;;
    1) t="120 ms · queda"; dx=141; dy=58; dw=18; dh=30; dop=1; r1=0; r1o=0; r2=0; r2o=0; iy=136; io=0; hl=0 ;;
    2) t="200 ms · toque"; dx=135; dy=98; dw=30; dh=18; dop=1; r1=44; r1o=0.9; r2=0; r2o=0; iy=134; io=0.2; hl=0 ;;
    3) t="300 ms · ondas"; dx=146; dy=110; dw=8; dh=6; dop=0; r1=170; r1o=0.35; r2=84; r2o=0.8; iy=128; io=0.7; hl=0.6 ;;
    4) t="400 ms · assentou"; dx=146; dy=110; dw=8; dh=6; dop=0; r1=240; r1o=0; r2=160; r2o=0; iy=124; io=1; hl=0 ;;
  esac
  frame - "Quadro $k" '{"clip":true,"cornerRadius":20,"sizingH":"fixed","sizingV":"fixed"}' 300 220
  F=$R; paint "$F" fill background; at "$F" $((k * 324)) -300
  FRAMES+=("$F")
  r1x=$(( 150 - r1 / 2 )); r1y=$(( 116 - r1 / 8 )); r2x=$(( 150 - r2 / 2 )); r2y=$(( 116 - r2 / 8 ))
  step node.create "$(jq -cn --argjson p "$(ref "$F")" --argjson x "$r1x" --argjson y "$r1y" --argjson w "$( (( r1 > 0 )) && echo $r1 || echo 1)" --argjson h "$( (( r1 > 0 )) && echo $((r1/4)) || echo 1)" --argjson o "$r1o" '{type:"ellipse", parent:$p, name:"Onda 1", x:$x, y:$y, width:$w, height:$h, props:{fills:[], stroke:"#088395", strokeWeight:2, opacity:$o}}')"
  step node.create "$(jq -cn --argjson p "$(ref "$F")" --argjson x "$r2x" --argjson y "$r2y" --argjson w "$( (( r2 > 0 )) && echo $r2 || echo 1)" --argjson h "$( (( r2 > 0 )) && echo $((r2/4)) || echo 1)" --argjson o "$r2o" '{type:"ellipse", parent:$p, name:"Onda 2", x:$x, y:$y, width:$w, height:$h, props:{fills:[], stroke:"#7AB2B2", strokeWeight:2, opacity:$o}}')"
  frame "$F" "Item" '{"layout":"horizontal","gap":10,"padding":[10,12,10,12],"cornerRadius":14,"crossAlign":"center","sizingH":"fixed","sizingV":"hug","strokeWeight":2,"strokeAlign":"inside"}' 260
  IT=$R
  at "$IT" 20 "$iy"
  paint "$IT" fill surface
  set_props "$IT" "$(jq -cn --argjson o "$io" --argjson h "$hl" '{opacity:$o, strokes:[{type:"solid", color:"#088395", opacity:$h}]}')"
  frame "$IT" "Avatar" '{"cornerRadius":10,"sizingH":"fixed","sizingV":"fixed"}' 36 36; paint "$R" fill primaryContainer
  col "$IT" "Text" 2; TX=$R; set_props "$TX" '{"sizingH":"fill"}'
  txt "$TX" "Padaria do Zé" Label onBackground
  txt "$TX" "agora · Nubank" Caption onSurfaceVariant
  txt "$IT" "- R\$ 42,90" Amount expense
  step node.create "$(jq -cn --argjson p "$(ref "$F")" --argjson x "$dx" --argjson y "$dy" --argjson w "$dw" --argjson h "$dh" --argjson o "$dop" '{type:"vector", parent:$p, name:"Gota", x:$x, y:$y, path:"M24 2 C24 2 46 27 46 36 A22 22 0 1 1 2 36 C2 27 24 2 24 2 Z", props:{width:$w, height:$h, opacity:$o, fills:[{type:"linearGradient", stops:["#1AA7BC","#088395","#09637E"], from:[0.5,0], to:[0.5,1]}]}}')"
  step text.create "$(jq -cn --arg t "$t" --argjson x $((k * 324 + 4)) '{text:$t, x:$x, y:-64}')"
  CAP=$R; style_fx "$CAP" "Label"; paint "$CAP" fill onSurfaceVariant
done
for k in 0 1 2 3; do
  case $k in 0) d=120 ;; 1) d=80 ;; 2) d=100 ;; 3) d=100 ;; esac
  step proto.interaction.add "$(jq -cn --argjson f "$(ref "${FRAMES[$k]}")" --argjson to "$(ref "${FRAMES[$((k+1))]}")" --argjson d "$d" '{id:$f, trigger:{type:"afterDelay", ms:10}, action:{type:"navigate", dest:$to}, transition:{type:"smartAnimate"}, durationMs:$d, easing:{customBezier:{x1:0.2, y1:0, x2:0, y2:1}}}')"
done
step proto.flow.add "$(jq -cn --argjson s "$(ref "${FRAMES[0]}")" '{start:$s, name:"Pingar"}')"

board "Board/Motion tokens" "Movimento" "Animações curtas e informativas (150–400 ms). Nada pisca, nada quica forte. Com “Remover animações” do Android ativo, tudo vira corte seco ou fade de 100 ms." 0 520 1400
M=$R

group "$M" "Easing e duração"
G=$R
row "$G" "Tokens" 16; set_props "$G" '{"sizingH":"fixed","width":243}'; set_props "$R" '{"wrap":true,"crossGap":16,"sizingH":"fill"}'
TK=$R
for e in "motion/short|150 ms|toques, switch, checkbox, troca de aba" "motion/medium|250 ms|entrada de lista, snackbar, contador de saldo" "motion/long|400 ms|pingar, bottom sheet, troca de tela" "easing/standard|cubic(0.2, 0, 0, 1)|padrão de tudo que se move na tela" "easing/decelerate|cubic(0, 0, 0, 1)|o que entra na tela" "easing/accelerate|cubic(0.3, 0, 1, 1)|o que sai da tela" "spring/gentle|damping 0.8 · stiffness 380|FAB e swipe ao soltar"; do
  IFS='|' read -r name val use <<< "$e"
  frame "$TK" "Token/$name" '{"layout":"vertical","gap":4,"padding":16,"cornerRadius":16,"sizingH":"fixed","sizingV":"hug"}' 180
  T=$R; paint "$T" fill surfaceVariant
  txt "$T" "$name" "Label Small" primary
  txt "$T" "$val" Label onBackground
  txt "$T" "$use" Caption onSurfaceVariant '{"props":{"sizingH":"fill","textAutoResize":"height"}}'
done

group "$M" "Catálogo"
G=$R
frame "$G" "Table" '{"layout":"vertical","gap":0,"cornerRadius":16,"clip":true,"sizingH":"fixed","sizingV":"hug","strokeWeight":1,"strokeAlign":"inside"}' 1300
TB=$R; paint "$TB" fill surface; paint "$TB" stroke outlineVariant
hdr=1
for r in "Animação|Onde|Como|Duração · curva" \
         "Pingar|nova transação (Início, Lançamentos, Revisão)|gota cai, toca, 2 ondas, item entra com fade + 8 dp|400 ms · standard" \
         "Entrada de lista|Lançamentos, Contas, Revisão|fade + translateY 12 dp, escalonado 30 ms por item (máx. 8)|250 ms · decelerate" \
         "Contador de saldo|card de saldo (Início)|número corre do valor antigo ao novo, algarismos tabulares|250 ms · standard" \
         "Toque no FAB|botão + em gota|escala 0,94 ao pressionar, volta com mola|150 ms · spring/gentle" \
         "Troca de aba|barra inferior|indicador desliza + crossfade do conteúdo|150 ms · standard" \
         "Deslizar item|lista de lançamentos|revela ação com resistência; passa de 40% e confirma; solta e volta com mola|— · spring/gentle" \
         "Puxar para atualizar|Início, Status da captura|gota enche conforme puxa e pinga ao soltar|250 ms · decelerate" \
         "Snackbar|exclusão com Desfazer|sobe 16 dp + fade; some sozinho em 4 s|200 ms · decelerate" \
         "Bottom sheet / diálogo|trocar categoria, confirmações|sobe da base + scrim em fade|250 ms · decelerate" \
         "Confirmar revisão|Revisão de pendentes|card encolhe e sai para a direita, lista fecha o espaço|250 ms · accelerate" \
         "Switch / checkbox|configurações, diálogos|thumb desliza, check desenha o traço|150 ms · standard" \
         "Skeleton|carregamento|brilho suave passando, sem pulsar|1200 ms · linear, em loop"; do
  IFS='|' read -r a b c d <<< "$r"
  frame "$TB" "Row" '{"layout":"horizontal","gap":16,"padding":[12,20,12,20],"crossAlign":"center","sizingH":"fill","sizingV":"hug","strokeSides":[false,false,true,false],"strokeWeight":1}'
  RW=$R
  paint "$RW" fill "$( (( hdr )) && echo surfaceVariant || echo none)"; paint "$RW" stroke outlineVariant
  st="Body Small"; (( hdr )) && st=Label
  txt "$RW" "$a" "$( (( hdr )) && echo Label || echo 'Body Strong')" onBackground '{"width":200,"props":{"textAutoResize":"height"}}'
  txt "$RW" "$b" "$st" onSurfaceVariant '{"width":320,"props":{"textAutoResize":"height"}}'
  txt "$RW" "$c" "$st" onBackground '{"width":480,"props":{"textAutoResize":"height"}}'
  txt "$RW" "$d" "$st" primary '{"props":{"sizingH":"fill","textAutoResize":"height"}}'
  hdr=0
done
