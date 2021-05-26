package br.com.zup.edu.pix.modelos

import br.com.zup.edu.integration.bcb.modelos.AccountTypeBCB

enum class TipoDeConta {
    CONTA_CORRENTE,
    CONTA_POUPANCA;

    fun mapToTipoDeContaBCB(): AccountTypeBCB {
        return when (this) {
            CONTA_CORRENTE -> AccountTypeBCB.CACC
            else -> AccountTypeBCB.SVGS
        }
    }
}
