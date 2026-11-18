#!/usr/bin/env bash
set -euo pipefail

PACOTE="com.pingo"
ATIVIDADE="com.pingo/.MainActivity"
META_ABERTURA_MS=1500

uso() {
  cat <<'EOF'
Uso: medir-desempenho-bateria.sh [-s <serial>] <subcomando> [opcoes]

Subcomandos:
  abertura [iteracoes]         Mede cold start (TotalTime) em N iteracoes (padrao: 5; meta <= 1500 ms)
  reset-bateria                Executa dumpsys batterystats --reset no aparelho conectado
  coletar-bateria [diretorio]  Coleta dumpsys batterystats --charged e dumpsys battery
EOF
}

executar_adb() {
  if [[ -n "${SERIAL_DISPOSITIVO:-}" ]]; then
    adb -s "$SERIAL_DISPOSITIVO" "$@"
  else
    adb "$@"
  fi
}

verificar_adb() {
  if ! command -v adb >/dev/null 2>&1; then
    echo "Erro: adb nao encontrado no PATH." >&2
    exit 1
  fi
  if ! executar_adb get-state >/dev/null 2>&1; then
    echo "Erro: nenhum dispositivo Android conectado ou autorizado via adb." >&2
    exit 1
  fi
}

medir_abertura() {
  local iteracoes="${1:-5}"
  if ! [[ "$iteracoes" =~ ^[1-9][0-9]*$ ]]; then
    echo "Erro: numero de iteracoes invalido: $iteracoes" >&2
    exit 1
  fi

  verificar_adb

  local soma=0
  local minimo=0
  local maximo=0
  local i=1

  echo "Medindo abertura a frio de $ATIVIDADE ($iteracoes iteracoes, meta <= ${META_ABERTURA_MS} ms)..."

  while [[ "$i" -le "$iteracoes" ]]; do
    executar_adb shell am force-stop "$PACOTE" >/dev/null
    sleep 1
    local saida
    saida="$(executar_adb shell am start -W -S "$ATIVIDADE" 2>&1 | tr -d '\r')"
    local total_time
    total_time="$(printf '%s\n' "$saida" | awk -F': ' '/^TotalTime:/ {print $2; exit}')"

    if ! [[ "$total_time" =~ ^[0-9]+$ ]]; then
      echo "Erro: nao foi possivel extrair TotalTime na iteracao $i." >&2
      printf '%s\n' "$saida" >&2
      exit 1
    fi

    echo "Iteracao $i/$iteracoes: TotalTime=${total_time} ms"
    soma=$((soma + total_time))
    if [[ "$i" -eq 1 || "$total_time" -lt "$minimo" ]]; then
      minimo="$total_time"
    fi
    if [[ "$total_time" -gt "$maximo" ]]; then
      maximo="$total_time"
    fi

    i=$((i + 1))
  done

  local media=$((soma / iteracoes))
  echo "Resumo RNF-04: media=${media} ms | min=${minimo} ms | max=${maximo} ms | meta<=${META_ABERTURA_MS} ms"

  if [[ "$media" -le "$META_ABERTURA_MS" ]]; then
    echo "Resultado RNF-04: APROVADO"
  else
    echo "Resultado RNF-04: ACIMA DA META" >&2
    exit 2
  fi
}

resetar_bateria() {
  verificar_adb
  executar_adb shell dumpsys batterystats --reset
  echo "Estatisticas de bateria resetadas. Use o aparelho normalmente por 24h antes de rodar coletar-bateria."
}

coletar_bateria() {
  verificar_adb
  local timestamp
  timestamp="$(date +%Y%m%d-%H%M%S)"
  local dir_saida="${1:-medicoes-bateria-${timestamp}}"
  mkdir -p "$dir_saida"

  local arquivo_stats="$dir_saida/batterystats-${PACOTE}.txt"
  local arquivo_battery="$dir_saida/battery.txt"

  executar_adb shell dumpsys batterystats --charged "$PACOTE" | tr -d '\r' > "$arquivo_stats"
  executar_adb shell dumpsys battery | tr -d '\r' > "$arquivo_battery"

  echo "Arquivos salvos em: $dir_saida"
  echo "--- dumpsys battery ---"
  grep -E 'level:|scale:|status:|charge counter:' "$arquivo_battery" || cat "$arquivo_battery"
  echo "--- dumpsys batterystats ($PACOTE) ---"
  grep -iE 'Estimated power use|Uid |Total cpu time|Foreground|Wake lock|Job' "$arquivo_stats" | head -n 30 || true
  echo "Compare o consumo estimado do $PACOTE nas ultimas 24h contra a meta RNF-02 (<= 2%/dia)."
}

SERIAL_DISPOSITIVO="${ANDROID_SERIAL:-}"

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    -s)
      if [[ "$#" -lt 2 ]]; then
        echo "Erro: -s exige o serial do dispositivo." >&2
        exit 1
      fi
      SERIAL_DISPOSITIVO="$2"
      shift 2
      ;;
    -h|--help)
      uso
      exit 0
      ;;
    *)
      break
      ;;
  esac
done

if [[ "$#" -lt 1 ]]; then
  uso >&2
  exit 1
fi

SUBCOMANDO="$1"
shift

case "$SUBCOMANDO" in
  abertura)
    medir_abertura "${1:-5}"
    ;;
  reset-bateria)
    resetar_bateria
    ;;
  coletar-bateria)
    coletar_bateria "${1:-}"
    ;;
  *)
    echo "Erro: subcomando desconhecido: $SUBCOMANDO" >&2
    uso >&2
    exit 1
    ;;
esac
