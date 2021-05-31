package br.com.zup.edu.integration.bcb.consulta

import br.com.zup.edu.integration.bcb.modelos.BankAccount
import br.com.zup.edu.integration.bcb.modelos.Owner
import br.com.zup.edu.integration.bcb.modelos.TipoDeChaveBCB
import java.time.LocalDateTime

class ConsultaChaveBCBResponse(
    val keyType: TipoDeChaveBCB,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner,
    val createdAt: LocalDateTime
)