#!/usr/bin/env bash
OUT=""
N=0
R=0

begin() { OUT="$1"; : > "$OUT"; N=0; }

emit() {
  printf '%s\n' "$1" >> "$OUT"
  if [[ "$1" =~ \"command\":\"(frame\.create|text\.create|node\.create|file\.import|instance\.create|instance\.slotInsert|component\.combineVariants)\" ]]; then R=$N; fi
  N=$((N + 1))
}

ref() { printf '{"$step":%s}' "$1"; }

step() {
  local cmd="$1" params="$2"
  emit "$(jq -cn --arg c "$cmd" --argjson p "$params" '{command:$c, params:$p}')"
}

frame() {
  local parent="$1" name="$2" props="$3" w="${4:-}" h="${5:-}"
  local p
  p=$(jq -cn --arg n "$name" --argjson pr "$props" '{name:$n, props:$pr}')
  if [[ "$parent" != "-" ]]; then p=$(jq -c --argjson r "$(ref "$parent")" '. + {parent:$r}' <<< "$p"); fi
  if [[ -n "$w" ]]; then p=$(jq -c --argjson w "$w" '. + {width:$w}' <<< "$p"); fi
  if [[ -n "$h" ]]; then p=$(jq -c --argjson h "$h" '. + {height:$h}' <<< "$p"); fi
  step frame.create "$p"
}

at() {
  local id="$1" x="$2" y="$3"
  step node.set "$(jq -cn --argjson r "$(ref "$id")" --argjson x "$x" --argjson y "$y" '{ids:[$r], x:$x, y:$y}')"
}

set_props() {
  local id="$1" props="$2"
  step node.set "$(jq -cn --argjson r "$(ref "$id")" --argjson pr "$props" '{ids:[$r]} + $pr')"
}

bindv() {
  local id="$1" field="$2" var="$3"
  step variable.bind "$(jq -cn --argjson r "$(ref "$id")" --arg f "$field" --arg v "$var" '{ids:[$r], field:$f, variable:$v}')"
}

paint() {
  local id="$1" field="$2" value="$3"
  if [[ "$value" == \#* ]]; then
    set_props "$id" "$(jq -cn --arg f "$field" --arg v "$value" '{($f):$v}')"
  elif [[ "$value" == "none" ]]; then
    if [[ "$field" == "fill" ]]; then set_props "$id" '{"fills":[]}'; else set_props "$id" '{"strokes":[]}'; fi
  else
    bindv "$id" "$field" "color/$value"
  fi
}

txt() {
  local parent="$1" text="$2" style="$3" color="$4" extra="${5:-}"
  [[ -z "$extra" ]] && extra="{}"
  local p
  p=$(jq -cn --argjson r "$(ref "$parent")" --arg t "$text" '{parent:$r, text:$t}')
  p=$(jq -c --argjson e "$extra" '. + $e' <<< "$p")
  step text.create "$p"
  local id=$R
  step style.apply "$(jq -cn --argjson r "$(ref "$id")" --arg s "$style" '{ids:[$r], style:$s}')"
  paint "$id" fill "$color"
  if [[ "$style" == "Overline" ]]; then set_props "$id" '{"textCase":"upper","letterSpacing":"8%"}'; fi
  R=$id
}

icon() {
  local parent="$1" tone="$2" name="$3" layer="${4:-Icon}"
  step file.import "$(jq -cn --arg p ".build/icons/$tone/$name.svg" '{path:$p, x:0, y:0}')"
  local id=$R
  step node.reparent "$(jq -cn --argjson r "$(ref "$id")" --argjson p "$(ref "$parent")" '{ids:[$r], parent:$p}')"
  set_props "$id" "$(jq -cn --arg n "$layer" '{name:$n}')"
  R=$id
}

inst() {
  local parent="$1" comp="$2"
  local c
  if [[ "$comp" =~ ^[0-9]+$ ]]; then c=$(ref "$comp"); else c=$(jq -cn --arg n "$comp" '{"@ref":$n}'); fi
  step instance.create "$(jq -cn --argjson c "$c" --argjson p "$(ref "$parent")" '{component:$c, parent:$p}')"
}

props_of() {
  local id="$1" props="$2"
  step instance.setProperties "$(jq -cn --argjson r "$(ref "$id")" --argjson pr "$props" '{ids:[$r], properties:$pr}')"
}

comp() {
  local id="$1"
  step component.create "$(jq -cn --argjson r "$(ref "$id")" '{ids:[$r]}')"
  R=$id
}

prop() {
  local cid="$1" name="$2" type="$3" def="$4" layer="$5"
  step component.addProperty "$(jq -cn --argjson c "$(ref "$cid")" --arg n "$name" --arg t "$type" --argjson d "$def" --argjson l "$(ref "$layer")" '{id:$c, name:$n, type:$t, default:$d, layer:$l}')"
}

combine() {
  local name="$1" layout="$2"; shift 2
  local ids
  ids=$(for i in "$@"; do ref "$i"; echo; done | jq -cs '.')
  step component.combineVariants "$(jq -cn --argjson i "$ids" '{ids:$i}')"
  local sid=$R
  set_props "$sid" "$(jq -cn --arg n "$name" --argjson l "$layout" '{name:$n} + $l')"
  R=$sid
}

style_fx() {
  local id="$1" style="$2"
  step style.apply "$(jq -cn --argjson r "$(ref "$id")" --arg s "$style" '{ids:[$r], style:$s}')"
}

board() {
  local name="$1" title="$2" desc="$3" x="$4" y="$5" w="${6:-}"
  local pr='{"layout":"vertical","gap":28,"padding":48,"cornerRadius":28,"fill":"#FFFFFF","sizingH":"hug","sizingV":"hug"}'
  if [[ -n "$w" ]]; then pr='{"layout":"vertical","gap":28,"padding":48,"cornerRadius":28,"fill":"#FFFFFF","sizingH":"fixed","sizingV":"hug"}'; fi
  frame - "$name" "$pr" "$w"
  local b=$R
  at "$b" "$x" "$y"
  txt "$b" "$title" Headline onBackground
  txt "$b" "$desc" "Body Small" onSurfaceVariant
  R=$b
}

group() {
  local parent="$1" title="$2" pr="${3:-}"
  [[ -z "$pr" ]] && pr='{"layout":"vertical","gap":16,"sizingH":"hug","sizingV":"hug","fills":[]}'
  frame "$parent" "Group/$title" "$pr"
  local g=$R
  txt "$g" "$title" Overline onSurfaceVariant
  R=$g
}

row() {
  local parent="$1" name="${2:-Row}" gap="${3:-16}"
  frame "$parent" "$name" "{\"layout\":\"horizontal\",\"gap\":$gap,\"crossAlign\":\"center\",\"sizingH\":\"hug\",\"sizingV\":\"hug\",\"fills\":[],\"wrap\":false}"
}

col() {
  local parent="$1" name="${2:-Col}" gap="${3:-12}"
  frame "$parent" "$name" "{\"layout\":\"vertical\",\"gap\":$gap,\"sizingH\":\"hug\",\"sizingV\":\"hug\",\"fills\":[]}"
}

iconi() {
  local parent="$1" name="$2" layer="${3:-Icon}"
  inst "$parent" "Icon/$name"
  local id=$R
  set_props "$id" "$(jq -cn --arg n "$layer" '{name:$n}')"
  R=$id
}

iconref() { jq -cn --arg n "Icon/$1" '{"@ref":$n}'; }
