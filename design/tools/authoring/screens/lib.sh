declare -A FR NAVID FABID NAVACT
LINKS=()
PROPSTEPS=0

tag_screen() {
  set_props "$1" "$(jq -cn --arg r "$2" --arg o "$3" --arg c "$4" '{pluginData:{row:$r, order:$o, caption:$c}}')"
}

screen() {
  local key="$1" row="$2" order="$3" cap="$4" name="$5"
  frame - "$name" '{"layout":"vertical","gap":0,"sizingH":"fixed","sizingV":"hug","minHeight":917,"clip":true,"cornerRadius":28,"strokeWeight":1,"strokeAlign":"inside"}' 412
  SC=$R
  FR[$key]=$SC
  paint "$SC" fill background
  paint "$SC" stroke outlineVariant
  tag_screen "$SC" "$row" "$order" "$cap"
  inst "$SC" StatusBar
  R=$SC
}

sheet_screen() {
  local key="$1" row="$2" order="$3" cap="$4" name="$5"
  frame - "$name" '{"layout":"vertical","gap":0,"sizingH":"fixed","sizingV":"fixed","clip":true,"cornerRadius":28,"strokeWeight":1,"strokeAlign":"inside"}' 412 917
  SC=$R
  FR[$key]=$SC
  paint "$SC" fill background
  paint "$SC" stroke outlineVariant
  tag_screen "$SC" "$row" "$order" "$cap"
  frame "$SC" "Behind" '{"layout":"vertical","gap":0,"sizingH":"fill","sizingV":"fill","clip":true}'
  BEH=$R
  paint "$BEH" fill background
  inst "$BEH" StatusBar
  R=$BEH
}

scrim_over() {
  frame "$1" "Scrim" '{"absolute":true,"sizingH":"fixed","sizingV":"fixed","x":0,"y":0}' 412 917
  paint "$R" fill scrim
}

dialog_screen() {
  local beh="$1" kind="$2" title="$3" desc="$4"
  frame "$beh" "Scrim" '{"absolute":true,"layout":"horizontal","mainAlign":"center","crossAlign":"center","sizingH":"fixed","sizingV":"fixed","x":0,"y":0}' 412 917
  local o=$R
  paint "$o" fill scrim
  inst "$o" Dialog
  local d=$R
  props_of "$d" "$(jq -cn --arg k "$kind" '{Kind:$k}')"
  props_of "$d" "$(jq -cn --arg t "$title" --arg s "$desc" '{Title:$t, Description:$s}')"
  R=$d
}

body() {
  local gap="${2:-16}" h="${3:-20}" t="${4:-8}" b="${5:-24}"
  frame "$1" "Body" "$(jq -cn --argjson g "$gap" --argjson h "$h" --argjson t "$t" --argjson b "$b" '{layout:"vertical",gap:$g,padding:[$t,$h,$b,$h],sizingH:"fill",sizingV:"fill",fills:[]}')"
}

body_hug() {
  local gap="${2:-16}" h="${3:-20}" t="${4:-8}" b="${5:-24}"
  frame "$1" "Body" "$(jq -cn --argjson g "$gap" --argjson h "$h" --argjson t "$t" --argjson b "$b" '{layout:"vertical",gap:$g,padding:[$t,$h,$b,$h],sizingH:"fill",sizingV:"hug",fills:[]}')"
}

spacer() { frame "$1" "Spacer" '{"sizingH":"fill","sizingV":"fill","fills":[]}'; }

fillw() { set_props "$1" '{"sizingH":"fill"}'; }

divider() { frame "$1" "Divider" '{"sizingH":"fill","sizingV":"fixed"}' "" 1; paint "$R" fill outlineVariant; }

card() {
  frame "$1" "${2:-Card}" '{"layout":"vertical","gap":0,"cornerRadius":20,"clip":true,"sizingH":"fill","sizingV":"hug"}'
  paint "$R" fill surface
}

pad_card() {
  frame "$1" "${2:-Card}" "$(jq -cn --argjson p "${3:-16}" --argjson g "${4:-12}" '{layout:"vertical",gap:$g,padding:$p,cornerRadius:20,sizingH:"fill",sizingV:"hug",strokeWeight:1,strokeAlign:"inside"}')"
  paint "$R" fill surface
  paint "$R" stroke outlineVariant
}

hrow() {
  frame "$1" "${2:-Row}" "$(jq -cn --argjson g "${3:-12}" '{layout:"horizontal",gap:$g,crossAlign:"center",sizingH:"fill",sizingV:"hug",fills:[]}')"
}

vcol() {
  frame "$1" "${2:-Col}" "$(jq -cn --argjson g "${3:-8}" '{layout:"vertical",gap:$g,sizingH:"fill",sizingV:"hug",fills:[]}')"
}

setp() {
  local inst="$1"; shift
  props_of "$inst" "$(jq -cn '$ARGS.named' "$@")"
}

appbar() {
  inst "$1" TopAppBar
  local a=$R
  props_of "$a" "$(jq -cn --arg t "$2" '{Type:$t}')"
  if [[ "$2" == Home ]]; then props_of "$a" "$(jq -cn --arg n "$3" '{Name:$n}')"; else props_of "$a" "$(jq -cn --arg n "$3" '{Title:$n}')"; fi
  if [[ "$2" == Small && "${4:-}" != action ]]; then props_of "$a" '{"Show action":false}'; fi
  R=$a
}

button() {
  local parent="$1" type="$2" size="$3" label="$4" icon="${5:-false}" wide="${6:-}" state="${7:-Default}"
  inst "$parent" Button
  local b=$R
  props_of "$b" "$(jq -cn --arg t "$type" --arg s "$size" --arg st "$state" --arg l "$label" --argjson i "$icon" '{Type:$t, Size:$s, State:$st, Label:$l, Icon:$i}')"
  if [[ -n "$wide" ]]; then fillw "$b"; fi
  R=$b
}

banner() {
  inst "$1" Banner
  local b=$R
  props_of "$b" "$(jq -cn --arg t "$2" '{Tone:$t}')"
  props_of "$b" "$(jq -cn --arg a "$3" --arg d "$4" '{Title:$a, Description:$d}')"
  fillw "$b"
  R=$b
}

section() {
  inst "$1" SectionHeader
  local s=$R
  if [[ -n "${3:-}" ]]; then props_of "$s" "$(jq -cn --arg t "$2" --arg a "$3" '{Title:$t, Action:$a, "Show action":true}')"; else props_of "$s" "$(jq -cn --arg t "$2" '{Title:$t, "Show action":false}')"; fi
  fillw "$s"
  R=$s
}

tx_item() {
  local parent="$1" type="$2" status="$3" title="$4" sub="$5" amt="$6" cat="$7"
  inst "$parent" TransactionItem
  local i=$R
  props_of "$i" "$(jq -cn --arg t "$type" --arg s "$status" '{Type:$t, Status:$s}')"
  props_of "$i" "$(jq -cn --arg a "$title" --arg b "$sub" --arg c "$amt" --arg k "CategoryAvatar/Category=$cat" '{Title:$a, Subtitle:$b, Amount:$c, Category:{"@ref":$k}}')"
  fillw "$i"
  R=$i
}

day_header() {
  inst "$1" DayHeader
  local d=$R
  props_of "$d" "$(jq -cn --arg a "$2" --arg b "$3" '{Day:$a, Total:$b}')"
  fillw "$d"
  R=$d
}

slot_add() {
  local sheet="$1" comp="$2"
  step instance.slotInsert "$(jq -cn --argjson s "$(ref "$sheet")" --arg c "$comp" '{id:$s, slot:"Content", component:{"@ref":$c}}')"
}

slot_first() {
  local sheet="$1" comp="$2"
  step instance.slotInsert "$(jq -cn --argjson s "$(ref "$sheet")" --arg c "$comp" '{id:$s, slot:"Content", component:{"@ref":$c}, replace:true}')"
}

sheet_label() { slot_add "$1" SheetLabel; props_of "$R" "$(jq -cn --arg t "$2" '{Text:$t}')"; }

sheet_chip() {
  local sheet="$1" label="$2" sel="${3:-Unselected}" kind="${4:-Toggle}"
  slot_add "$sheet" "FilterChip/Kind=Toggle, State=Unselected"
  props_of "$R" "$(jq -cn --arg l "$label" --arg s "$sel" --arg k "$kind" '{Label:$l, State:$s, Kind:$k}')"
}

bottom_nav() {
  local sc="$1" key="$2" active="$3"
  NAVACT[$key]="$active"
  frame "$sc" "Bottom nav" '{"layout":"vertical","gap":0,"sizingH":"fill","sizingV":"hug"}'
  local w=$R
  paint "$w" fill surface
  style_fx "$w" "Elevation/2"
  frame "$w" "Bar" '{"layout":"horizontal","gap":0,"padding":[0,8,0,8],"mainAlign":"spaceBetween","crossAlign":"center","sizingH":"fill","sizingV":"hug","fills":[]}'
  local bar=$R
  local tab st
  for tab in Início Lançamentos FAB Contas Relatórios; do
    if [[ "$tab" == FAB ]]; then
      frame "$bar" "FAB slot" '{"layout":"horizontal","mainAlign":"center","crossAlign":"center","sizingH":"fixed","sizingV":"fixed","fills":[]}' 64 72
      inst "$R" AddDrop
      FABID[$key]=$R
      continue
    fi
    inst "$bar" NavItem
    local n=$R
    st=Inactive
    [[ "$tab" == "$active" ]] && st=Active
    props_of "$n" "$(jq -cn --arg t "$tab" --arg s "$st" '{Tab:$t, State:$s}')"
    NAVID["$key:$tab"]=$n
  done
  inst "$w" GestureBar
  R=$w
}

link() {
  LINKS+=("$1|$2|${3:-push}|${4:-250}")
}

flush_links() {
  local l src dst tr ms
  for l in "${LINKS[@]}"; do
    IFS='|' read -r src dst tr ms <<< "$l"
    if [[ "$dst" == back ]]; then
      step proto.interaction.add "$(jq -cn --argjson f "$(ref "$src")" '{id:$f, trigger:{type:"onClick"}, action:{type:"back"}}')"
      continue
    fi
    if [[ -z "${FR[$dst]:-}" ]]; then echo "link: unknown screen $dst" >&2; [[ -n "${ALLOW_MISSING:-}" ]] && continue; exit 1; fi
    step proto.interaction.add "$(jq -cn --argjson f "$(ref "$src")" --argjson d "$(ref "${FR[$dst]}")" --arg t "$tr" --argjson ms "$ms" '{id:$f, trigger:{type:"onClick"}, action:{type:"navigate", dest:$d}, transition:(if $t=="push" then {type:"push", direction:"left"} elif $t=="up" then {type:"moveIn", direction:"bottom"} else {type:$t} end), durationMs:$ms, easing:{customBezier:{x1:0.2, y1:0, x2:0, y2:1}}}')"
  done
}

wire_nav() {
  local key tab dst
  for key in "${!NAVACT[@]}"; do
    for tab in Início Lançamentos Contas Relatórios; do
      [[ "$tab" == "${NAVACT[$key]}" ]] && continue
      case "$tab" in Início) dst=home ;; Lançamentos) dst=tx ;; Contas) dst=acc ;; Relatórios) dst=rep ;; esac
      link "${NAVID["$key:$tab"]}" "$dst" dissolve 150
    done
    link "${FABID[$key]}" new up 250
  done
}

empty_state() {
  local parent="$1" kind="$2" title desc
  case "$kind" in
    NoData) title="Nenhum pingo ainda"; desc="Assim que um banco avisar uma compra, ela aparece aqui. Ou lance agora." ;;
    NoResults) title="Nada com esses filtros"; desc="Tente outro mês ou limpe os filtros." ;;
    AllDone) title="Tudo em dia"; desc="Nenhum lançamento esperando revisão." ;;
    ManualOnly) title="Modo só manual"; desc="Sem acesso às notificações o Pingo não captura sozinho." ;;
  esac
  [[ -n "${3:-}" ]] && title="$3"
  [[ -n "${4:-}" ]] && desc="$4"
  inst "$parent" EmptyState
  local e=$R
  props_of "$e" "$(jq -cn --arg k "$kind" '{Kind:$k}')"
  props_of "$e" "$(jq -cn --arg t "$title" --arg d "$desc" '{Title:$t, Description:$d}')"
  fillw "$e"
  R=$e
}

overlay_inst() {
  local beh="$1" comp="$2"
  frame "$beh" "Scrim" '{"absolute":true,"layout":"horizontal","mainAlign":"center","crossAlign":"center","sizingH":"fixed","sizingV":"fixed","x":0,"y":0}' 412 917
  local o=$R
  paint "$o" fill scrim
  inst "$o" "$comp"
}

field() {
  local parent="$1" state="$2" label="$3" value="$4" support="${5:- }" icon="${6:-}"
  inst "$parent" TextField
  local f=$R
  props_of "$f" "$(jq -cn --arg s "$state" '{State:$s}')"
  props_of "$f" "$(jq -cn --arg l "$label" --arg v "$value" --arg u "$support" '{Label:$l, Value:$v, Supporting:$u}')"
  if [[ -n "$icon" ]]; then props_of "$f" "$(jq -cn --arg i "Icon/$icon" '{Leading:{"@ref":$i}}')"; else props_of "$f" '{"Leading icon":false}'; fi
  fillw "$f"
  R=$f
}

select_field() {
  local parent="$1" label="$2" value="$3" icon="${4:-}" state="${5:-Closed}"
  inst "$parent" Select
  local f=$R
  props_of "$f" "$(jq -cn --arg s "$state" '{State:$s}')"
  props_of "$f" "$(jq -cn --arg l "$label" --arg v "$value" '{Label:$l, Value:$v}')"
  if [[ -n "$icon" ]]; then props_of "$f" "$(jq -cn --arg i "Icon/$icon" '{Leading:{"@ref":$i}}')"; else props_of "$f" '{"Leading icon":false}'; fi
  fillw "$f"
  R=$f
}

amount_field() {
  inst "$1" AmountField
  local a=$R
  props_of "$a" "$(jq -cn --arg s "$2" '{State:$s}')"
  props_of "$a" "$(jq -cn --arg v "$3" --arg u "$4" '{Value:$v, Supporting:$u}')"
  fillw "$a"
  R=$a
}

segmented() {
  inst "$1" SegmentedControl
  props_of "$R" "$(jq -cn --arg s "$2" '{Selected:$s}')"
  fillw "$R"
}

settings_row() {
  inst "$1" SettingsRow
  local r=$R
  props_of "$r" "$(jq -cn --arg t "$2" '{Type:$t}')"
  props_of "$r" "$(jq -cn --arg t "$3" '{Title:$t}')"
  if [[ -n "${4:-}" ]]; then props_of "$r" "$(jq -cn --arg t "$4" '{Subtitle:$t}')"; fi
  if [[ -n "${5:-}" ]]; then props_of "$r" "$(jq -cn --arg i "Icon/$5" '{Icon:{"@ref":$i}}')"; fi
  fillw "$r"
  R=$r
}
