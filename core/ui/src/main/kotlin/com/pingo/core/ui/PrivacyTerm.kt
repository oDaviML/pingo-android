package com.pingo.core.ui

import androidx.compose.runtime.Composable
import com.pingo.core.designsystem.component.PingoInfoDialog
import com.pingo.core.designsystem.icon.PingoIcons

const val PRIVACY_TERM_VERSION = "2026-10"

val PrivacyTermText: String = """
    Termo de privacidade do Pingo

    1. O que o Pingo lê
    O Pingo lê apenas as notificações dos aplicativos de banco que você habilitar. Nada de mensagens, fotos, contatos ou outros aplicativos.

    2. Onde o texto é processado
    A leitura das notificações acontece no seu celular. O texto bruto nunca sai do aparelho.

    3. O que é sincronizado
    Só dados estruturados — como valor, data, estabelecimento e categoria — são enviados ao servidor, e apenas com o seu consentimento. Eles permitem restaurar seus dados se você trocar de celular.

    4. Onde os dados ficam
    Os dados sincronizados ficam em um servidor próprio do Pingo na União Europeia. A transferência internacional é necessária para o funcionamento do serviço e está coberta por este termo.

    5. Seus direitos
    Você pode excluir sua conta e seus dados quando quiser, em Configurações > Privacidade e dados. A exclusão apaga os dados do servidor imediatamente e remove as cópias de backup em até 7 dias.

    6. Consentimento
    Ao aceitar este termo, você concorda com a leitura das notificações dos bancos escolhidos, com a sincronização dos dados estruturados e com o tratamento descrito acima.
""".trimIndent()

@Composable
fun PrivacyTermDialog(onDismiss: () -> Unit) {
    PingoInfoDialog(
        icon = PingoIcons.FileText,
        title = "Termo de privacidade",
        text = PrivacyTermText,
        onDismissRequest = onDismiss,
    )
}
