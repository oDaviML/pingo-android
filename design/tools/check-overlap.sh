#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
DOC="${1:-pingo.figcraft}"

figcraft-cli convert "$DOC" .build/overlap.json > /dev/null
PAGES="$(figcraft-cli info "$DOC" --json | jq -c '[.pages[] | {id, name}]')"

jq -r --argjson pages "$PAGES" '
  def box: {id, name, x: ((.transform // [1,0,0,1,0,0])[4]), y: ((.transform // [1,0,0,1,0,0])[5]), w: .width, h: .height};
  def hit($a; $b): ($a.x < $b.x + $b.w) and ($b.x < $a.x + $a.w) and ($a.y < $b.y + $b.h) and ($b.y < $a.y + $a.h);
  . as $doc
  | $pages[] as $pg
  | [$doc.nodes[] | select(.parent == $pg.id and (.type == "frame" or .type == "component")) | box] as $fr
  | [range(0; $fr | length) as $i | range($i + 1; $fr | length) as $j
     | select(hit($fr[$i]; $fr[$j])) | "\($pg.name): \($fr[$i].name) × \($fr[$j].name)"]
  | .[]' .build/overlap.json > .build/overlap.txt

if [[ -s .build/overlap.txt ]]; then
  echo "overlapping frames:"
  cat .build/overlap.txt
  exit 1
fi
echo "overlap check: ok"
