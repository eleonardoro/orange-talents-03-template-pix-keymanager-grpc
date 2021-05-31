package br.com.zup.edu.integration.bcb.modelos

import br.com.zup.edu.grpc.TipoDeConta

enum class AccountTypeBCB {
    CACC, SVGS;

    fun converterParaTipoConta(): TipoDeConta {
        return if (this == CACC) TipoDeConta.CONTA_POUPANCA else TipoDeConta.CONTA_CORRENTE
    }
}