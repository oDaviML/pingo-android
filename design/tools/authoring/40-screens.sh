#!/usr/bin/env bash
set -euo pipefail
HERE="$(dirname "$0")"
source "$HERE/lib.sh"
source "$HERE/screens/lib.sh"
begin "$1"

step page.select '{"index":3}'

for part in "$HERE"/screens/t*.sh; do
  source "$part"
done

wire_nav
flush_links
