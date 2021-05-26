package br.com.zup.edu.integration.bcb.cadastra

import br.com.zup.edu.integration.bcb.modelos.BankAccount
import br.com.zup.edu.integration.bcb.modelos.Owner
import br.com.zup.edu.integration.bcb.modelos.TipoDeChaveBCB

class CadastraChaveBCBResponse(
    val keyType: TipoDeChaveBCB,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner,
)