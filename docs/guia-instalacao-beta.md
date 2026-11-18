# Guia de Instalação — Pingo (Beta)

Bem-vindo(a) ao beta do **Pingo**! Este guia explica como instalar o aplicativo no seu celular Android, manter as atualizações em dia e configurar as permissões para que a captura automática de transações funcione sem interrupções.

---

## 1. Como instalar o Pingo

Como o Pingo está em fase beta e fora da Google Play Store, você pode instalar de duas formas: **diretamente pelo GitHub Releases** (mais rápido para a primeira instalação) ou pelo **Obtainium** (recomendado para receber atualizações automáticas).

### Opção A — Instalação direta pelo GitHub Releases

1. No celular, abra o link da versão mais recente:
   **<https://github.com/oDaviML/pingo-android/releases/latest>**
2. Na seção **Assets** da página, toque no arquivo **`Pingo-<versão>-release.apk`** para baixar.
3. Ao terminar o download, abra o arquivo `.apk` pela barra de notificações ou pelo aplicativo **Arquivos / Meus Arquivos**.
4. Se o Android pedir permissão para instalar aplicativos de fontes desconhecidas, siga o passo a passo da [seção 3](#3-como-liberar-instalar-apps-desconhecidos).
5. Toque em **Instalar** e aguarde a conclusão.

### Opção B — Instalação e atualização automática via Obtainium (recomendado)

O **Obtainium** é um aplicativo gratuito e de código aberto que verifica novas versões diretamente no GitHub e avisa (ou atualiza) quando sai uma atualização do Pingo.

1. **Baixe e instale o Obtainium:**
   - Acesse <https://github.com/ImranR98/Obtainium/releases/latest> no celular.
   - Baixe o APK do Obtainium (ex.: `app-release.apk`) e instale-o (liberando "Instalar apps desconhecidos" para o navegador, se solicitado).
2. **Adicione o Pingo no Obtainium:**
   - Abra o **Obtainium** e toque em **Adicionar app** (*Add App*).
   - No campo **URL da fonte do app** (*App source URL*), cole:
     ```text
     https://github.com/oDaviML/pingo-android
     ```
   - Toque em **Adicionar** (*Add*) e depois em **Instalar**.
   - Se o Android pedir, libere "Instalar apps desconhecidos" também para o **Obtainium**.
3. A partir daí, o Obtainium verificará automaticamente novas releases do Pingo e permitirá atualizar com um toque.

---

## 2. Como atualizar pelo próprio Pingo

Além do Obtainium e do GitHub Releases, você pode verificar e baixar atualizações diretamente dentro do aplicativo:

1. Abra o **Pingo**.
2. Acesse **Configurações → Sobre → Procurar atualização do APK**.
3. Se houver uma versão mais nova disponível, siga as instruções na tela para baixar e instalar o novo APK.

---

## 3. Como liberar "Instalar apps desconhecidos"

Por padrão, o Android bloqueia a instalação de arquivos `.apk` baixados fora da Play Store. Para liberar apenas para o aplicativo que você usou no download (Chrome, Samsung Internet, Arquivos ou Obtainium):

1. Ao tocar no arquivo `.apk`, aparecerá um aviso dizendo que o app não tem permissão para instalar apps desconhecidos. Toque em **Configurações** no próprio aviso.
   - *Caminho manual caso o aviso não apareça:* abra **Configurações do Android → Aplicativos → Acesso especial** (ou menu `⋮` → *Acesso especial*) **→ Instalar apps desconhecidos**.
2. Selecione o aplicativo usado (ex.: **Chrome**, **Meus Arquivos** ou **Obtainium**).
3. Ative a chave **Permitir desta fonte**.
4. Volte para a tela anterior e toque em **Instalar**.

---

## 4. Aviso do Google Play Protect e verificação de integridade (SHA-256)

### Por que o Google Play Protect pode mostrar um aviso?

Por decisão de arquitetura (**ADR-08**), o Pingo é distribuído fora da Google Play Store e utiliza a permissão de **acesso às notificações** para ler alertas bancários no próprio aparelho. Por esse motivo, o **Google Play Protect** pode exibir um alerta preventivo ("App desconhecido" ou "Play Protect não reconhece o desenvolvedor deste app") durante a instalação.

Se esse aviso aparecer:
1. Toque em **Mais detalhes** (*More details*).
2. Toque em **Instalar assim mesmo** (*Install anyway*).
3. Caso o Play Protect peça verificação adicional, confirme que deseja prosseguir.

> **Privacidade:** O Pingo processa suas notificações localmente no aparelho; o texto bruto das notificações nunca é enviado para servidores externos.

### Como conferir a integridade do APK (SHA-256) — opcional

Cada release no GitHub publica, junto com o `.apk`, um arquivo **`Pingo-<versão>-release.apk.sha256`** contendo a assinatura criptográfica SHA-256 oficial do build.

- **No computador (Linux/macOS):**
  ```bash
  sha256sum -c Pingo-<versão>-release.apk.sha256
  ```
- **No celular:** você pode usar um app verificador de hash (ou conferir manualmente o código SHA-256 gerado para o `.apk` baixado comparando com o conteúdo do arquivo `.sha256` na página da release). Os 64 caracteres devem ser idênticos.

---

## 5. Permissões essenciais após instalar

Ao abrir o Pingo pela primeira vez, o onboarding guiará você pelas permissões abaixo. Se alguma etapa for pulada ou precisar ser ajustada depois, configure manualmente conforme seu aparelho:

### 5.1 Acesso às notificações (obrigatório para captura automática)

1. Abra **Configurações do Android → Aplicativos → Acesso especial** (ou busque por **"Acesso a notificações"** na lupa das Configurações).
2. Toque em **Acesso a notificações** (*Notification access*).
3. Encontre o **Pingo** e ative a chave.
4. Confirme no aviso do sistema (em Android 13+, se a opção estiver bloqueada como "Configuração restrita", abra **Configurações → Aplicativos → Pingo → menu `⋮` (canto superior direito) → Permitir configurações restritas** e depois volte para ativar o acesso).

### 5.2 Desativar restrições de bateria e ativar inicialização automática

O Android e alguns fabricantes encerram aplicativos em segundo plano para economizar bateria, o que pode interromper a captura de notificações. Configure o Pingo como **sem restrições**:

#### Samsung (One UI)
1. **Configurações → Aplicativos → Pingo → Bateria** → selecione **Irrestrito** (*Unrestricted*).
2. **Configurações → Assistência do aparelho → Bateria → Limites de uso em segundo plano** → verifique que o Pingo **não** está em *Aplicativos suspensos* nem *Aplicativos em suspensão profunda* (se preferir, adicione-o em **Aplicativos nunca suspensos automaticamente**).

#### Xiaomi / Redmi / POCO (MIUI / HyperOS)
1. **Configurações → Apps → Gerenciar apps → Pingo**:
   - Ative **Inicialização automática** (*Autostart*).
   - Em **Economia de bateria**, selecione **Sem restrições** (*No restrictions*).
2. Na tela de aplicativos recentes, opcionalmente puxe o card do Pingo para baixo (ou segure) e toque no ícone de **cadeado** para evitar que seja fechado ao limpar a memória.

#### Motorola (Android próximo ao padrão)
1. **Configurações → Apps → Ver todos os apps → Pingo → Uso da bateria do app** (ou *Bateria*) → selecione **Irrestrito** (*Unrestricted*).
2. Se o aparelho tiver **Bateria Adaptável** muito agressiva interferindo na captura, mantenha o Pingo fora de otimização em **Configurações → Bateria → Uso da bateria**.

---

## 6. Suporte e envio de feedback

Encontrou algum problema na instalação, bug no app ou uma notificação de banco que não foi capturada? Entre no grupo oficial do beta para falar com a equipe:

> **Grupo de Suporte Beta:** [INSERIR LINK DO GRUPO WHATSAPP/TELEGRAM DO BETA]

Ao reportar um problema no grupo, se possível informe:
- Modelo do celular e versão do Android;
- Versão do Pingo instalada (visível em **Configurações → Sobre**);
- O que aconteceu e, se possível, um print da tela.
