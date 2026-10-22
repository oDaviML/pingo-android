#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
DOC="${1:-pingo.figcraft}"
GAP_X=160
GAP_Y=260
HEAD=150

source tools/authoring/lib.sh

figcraft-cli convert "$DOC" .build/layout.json > /dev/null
PAGE_ID="$(figcraft-cli info "$DOC" --json | jq '.pages[] | select(.name=="Screens") | .id')"

jq -c --argjson p "$PAGE_ID" '
  [.nodes[] | select(.parent == $p and .type == "frame" and .plugin_data.row != null)
   | {id, w: .width, h: .height, row: .plugin_data.row, order: (.plugin_data.order | tonumber), cap: (.plugin_data.caption // "")}]
  | group_by(.row) | map(sort_by(.order)) | sort_by(.[0].row)' .build/layout.json > .build/rows.json

COUNT="$(jq 'length' .build/rows.json)"
if [[ "$COUNT" == "0" ]]; then echo "layout: no screens"; exit 0; fi

jq -c --argjson hx "$GAP_X" --argjson gy "$GAP_Y" --argjson head "$HEAD" '
  reduce .[] as $row ({y: 0, out: []};
    . as $acc
    | ($row | map(.h) | max) as $mh
    | ($row | reduce .[] as $it ({x: 0, items: []};
        {x: (.x + $it.w + $hx), items: (.items + [{id: $it.id, x: .x, y: ($acc.y + $head), cap: $it.cap}])})) as $placed
    | {y: ($acc.y + $head + $mh + $gy),
       out: ($acc.out + [{title: ($row[0].row | sub("^[0-9]+ "; "")), y: $acc.y, items: $placed.items}])})
  | .out[]' .build/rows.json > .build/placement.jsonl

begin .build/resolved/99-layout.jsonl
step page.select '{"index":3}'
while IFS= read -r row; do
  title="$(jq -r '.title' <<< "$row")"
  ry="$(jq '.y' <<< "$row")"
  step text.create "$(jq -cn --arg t "$title" --argjson y "$ry" '{text:$t, x:0, y:$y, name:"Row title"}')"
  style_fx "$R" "Display"
  paint "$R" fill brand
  while IFS= read -r it; do
    id="$(jq '.id' <<< "$it")"; x="$(jq '.x' <<< "$it")"; y="$(jq '.y' <<< "$it")"; cap="$(jq -r '.cap' <<< "$it")"
    step transform.moveTo "$(jq -cn --argjson i "$id" --argjson x "$x" --argjson y "$y" '{ids:[$i], x:$x, y:$y}')"
    step text.create "$(jq -cn --arg t "$cap" --argjson x "$x" --argjson y "$((y - 44))" '{text:$t, x:$x, y:$y, name:"Caption"}')"
    style_fx "$R" "Label"
    paint "$R" fill onSurfaceVariant
  done < <(jq -c '.items[]' <<< "$row")
done < .build/placement.jsonl

figcraft-cli run .build/resolved/99-layout.jsonl --in "$DOC" --out "$DOC" > .build/99-layout.log
echo "layout: $(jq -s 'map(.items | length) | add' .build/placement.jsonl) screens in $COUNT rows"
