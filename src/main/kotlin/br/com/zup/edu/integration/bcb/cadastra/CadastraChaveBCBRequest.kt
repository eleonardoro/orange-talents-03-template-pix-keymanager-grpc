package br.com.zup.edu.integration.bcb.cadastra

import br.com.zup.edu.integration.bcb.modelos.BankAccount
import br.com.zup.edu.integration.bcb.modelos.Owner
import br.com.zup.edu.integration.bcb.modelos.TipoDeChaveBCB

data class CadastraChaveBCBRequest(
    val keyType: TipoDeChaveBCB,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner,
)