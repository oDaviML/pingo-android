# Procedimento de Medição de Desempenho e Bateria (RNF-01, RNF-02 e RNF-04)

Este documento descreve o procedimento reproduzível para validar os requisitos não funcionais de desempenho e consumo de bateria do **Pingo** (`com.pingo`) usando o script [`scripts/medir-desempenho-bateria.sh`](../scripts/medir-desempenho-bateria.sh) e a suíte instrumentada [`CapturaListenerTest`](../app/src/androidTest/kotlin/com/pingo/captura/CapturaListenerTest.kt).

---

## Resumo das metas

| Requisito | Descrição | Meta | Método |
| --- | --- | --- | --- |
| **RNF-01** | Latência entre recebimento da notificação e captura/persistência | `<= 2 s` | `CapturaListenerTest` + inspeção de logcat |
| **RNF-02** | Consumo de bateria em segundo plano (24h de uso típico) | `<= 2%/dia` | `reset-bateria` → 24h em aparelho físico → `coletar-bateria` |
| **RNF-04** | Tempo de abertura a frio (*cold start*) | `<= 1.500 ms` | `abertura` via `am force-stop` + `am start -W -S` |

---

## Pré-requisitos

1. **ADB** instalado e acessível no `PATH` (`adb version`).
2. **Aparelho físico Android** conectado via USB/Wi-Fi com **Depuração USB** habilitada (obrigatório para medição fiel de **RNF-02** e recomendado para **RNF-04**; **RNF-01** também pode ser executado em emulador).
3. Se houver mais de um dispositivo conectado, defina `ANDROID_SERIAL=<serial>` ou passe `-s <serial>` ao script.
4. APK do Pingo (`com.pingo`) instalado no aparelho (preferencialmente variante **release** para **RNF-02** e **RNF-04**; variante **debug** para o teste instrumentado de **RNF-01**).

---

## 1. RNF-04 — Tempo de abertura a frio (`<= 1.500 ms`)

O subcomando `abertura` força o encerramento do processo (`am force-stop com.pingo`), aguarda 1 segundo e inicia `com.pingo/.MainActivity` com `am start -W -S`, extraindo o campo `TotalTime` reportado pelo ActivityManager em `N` iterações (padrão: `5`).

```bash
./scripts/medir-desempenho-bateria.sh abertura 5
```

Com serial específico:

```bash
./scripts/medir-desempenho-bateria.sh -s <SERIAL> abertura 10
```

### Critério de aprovação
- O script calcula mínimo, máximo e média das iterações.
- Aprovado quando a média de `TotalTime` for **`<= 1.500 ms`** (código de saída `0`). Caso ultrapasse a meta, o script retorna código `2`.

---

## 2. RNF-02 — Consumo de bateria em 24h (`<= 2%/dia`)

A medição de bateria deve ser feita em **aparelho físico** durante **24 horas de uso típico**, com o Pingo configurado conforme o [guia de instalação beta](guia-instalacao-beta.md) (acesso às notificações ativo e bateria sem restrições).

### Passo 1 — Zerar as estatísticas de bateria

Com o aparelho conectado e preferencialmente carregado (ou desconectado logo após o reset):

```bash
./scripts/medir-desempenho-bateria.sh reset-bateria
```

Internamente, o comando executa `adb shell dumpsys batterystats --reset`.

### Passo 2 — Janela de 24 horas de uso típico

- Desconecte o cabo USB e utilize o celular normalmente por **24 horas** (recebendo notificações bancárias e abrindo o Pingo ocasionalmente).
- Evite carregar ou reiniciar o aparelho durante a janela, se possível, para manter o ciclo `--charged` contínuo.

### Passo 3 — Coletar e avaliar os dados

Após 24 horas, reconecte o aparelho via ADB e execute:

```bash
./scripts/medir-desempenho-bateria.sh coletar-bateria
```

Você também pode informar um diretório de saída específico:

```bash
./scripts/medir-desempenho-bateria.sh coletar-bateria ./evidencias/rnf02
```

O script executa `dumpsys batterystats --charged com.pingo` e `dumpsys battery`, salva os arquivos completos (`batterystats-com.pingo.txt` e `battery.txt`) e exibe um resumo no terminal.

### Critério de aprovação
- Verifique em `batterystats-com.pingo.txt` a estimativa de consumo de energia do UID do `com.pingo` (`Estimated power use (mAh)` / percentual proporcional à capacidade da bateria em `battery.txt`).
- Aprovado quando o consumo atribuído ao Pingo no período de 24h for **`<= 2%/dia`**.

---

## 3. RNF-01 — Latência de captura da notificação (`<= 2 s`)

O requisito **RNF-01** garante que uma notificação bancária recebida pelo `NotificationListenerService` seja capturada, processada pelo parser on-device e persistida localmente em até **2 segundos**.

### Método 1 — Teste instrumentado (`CapturaListenerTest`)

Em build **debug** com dispositivo ou emulador conectado:

```bash
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.pingo.captura.CapturaListenerTest
```

O `CapturaListenerTest` utiliza a seam de debug (`pingo-teste-<INSTITUICAO>`) para postar notificações sintéticas pelo shell e validar o fluxo real do listener até a persistência no Room dentro das janelas de timeout do teste.

### Método 2 — Inspeção de latência via `adb logcat`

Para medir o tempo exato em milissegundos entre a emissão da notificação e a persistência no banco durante um teste manual ou execução do `CapturaListenerTest`:

1. Limpe o buffer e monitore os logs do processo `com.pingo` com timestamps em milissegundos:
   ```bash
   adb logcat -c
   adb logcat -v threadtime | grep -iE "pingo|NotificationListener|Captura"
   ```
2. Dispare uma notificação bancária (real no aparelho ou sintética em build debug).
3. Compare o timestamp do recebimento da notificação (`onNotificationPosted`) com o timestamp de conclusão da gravação no banco.
4. **Critério de aprovação:** diferença **`<= 2.000 ms` (`<= 2 s`)**.
