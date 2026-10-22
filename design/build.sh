#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

DOC="pingo.figcraft"
BUILD=".build"
MAP="$BUILD/components.json"

declare -A TONES=(
  [ink]="#0B2A33"
  [muted]="#4A6A72"
  [primary]="#088395"
  [deep]="#09637E"
  [soft]="#7AB2B2"
  [white]="#FFFFFF"
  [danger]="#C8453B"
  [success]="#2E7D5B"
  [warning]="#C9802B"
  [disabled]="#9DB3B8"
  [onwarning]="#7A4E12"
)

if [[ -f "$DOC" ]]; then
  mkdir -p .backups
  cp "$DOC" ".backups/pingo-$(date +%Y%m%d-%H%M%S).figcraft"
  ls -1t .backups | tail -n +11 | while read -r old; do rm -f ".backups/$old"; done
fi
rm -rf "$BUILD" "$DOC"
mkdir -p "$BUILD/resolved" exports

for tone in "${!TONES[@]}"; do
  mkdir -p "$BUILD/icons/$tone"
  for svg in icons/src/*.svg; do
    sed "s/currentColor/${TONES[$tone]}/g" "$svg" > "$BUILD/icons/$tone/$(basename "$svg")"
  done
done

echo '{}' > "$MAP"

refresh_map() {
  echo '[{"command":"component.list","params":{}}]' > "$BUILD/list.json"
  figcraft-cli run "$BUILD/list.json" --in "$DOC" \
    | jq -c 'select(.command=="component.list") | .result
             | map(if (.type=="COMPONENT" and (.name | contains("=")))
                   then {key: ((.folder[-1]) + "/" + .name), value: .id}
                   else {key: .name, value: .id} end)
             | from_entries' > "$MAP"
}

for script in scripts/*.jsonl; do
  name="$(basename "$script")"
  problems="$(jq -n -r -f tools/check-refs.jq "$script")"
  if [[ -n "$problems" ]]; then echo "bad step refs in $name:"; echo "$problems"; exit 1; fi
  resolved="$BUILD/resolved/$name"
  jq -c --slurpfile m "$MAP" '
    walk(if type == "object" and has("@ref")
         then ($m[0][.["@ref"]] // error("unknown component: " + .["@ref"]))
         else . end)' "$script" > "$resolved"
  if [[ -f "$DOC" ]]; then
    figcraft-cli run "$resolved" --in "$DOC" --out "$DOC" > "$BUILD/$name.log"
  else
    figcraft-cli run "$resolved" --out "$DOC" > "$BUILD/$name.log"
  fi
  echo "ok  $name ($(grep -c '"ok":true' "$BUILD/$name.log") steps)"
  refresh_map
done

tools/layout-screens.sh "$DOC"
tools/check-overlap.sh "$DOC"

figcraft-cli info "$DOC" | sed -n 1p
figcraft-cli render "$DOC" -o "exports/Foundations.png" --page Foundations --scale 2 > /dev/null
for page in Components Motion Screens; do
  rm -rf "exports/$page"
  mkdir -p "exports/$page"
  figcraft-cli export "$DOC" -o "exports/$page" --page "$page" --scale 2 > /dev/null 2>&1 || true
done
ls exports/*/ 2>/dev/null | wc -l
