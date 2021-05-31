package br.com.zup.edu.integration.bcb.modelos

import br.com.zup.edu.grpc.TipoDeChave

enum class TipoDeChaveBCB {
    CPF, CNPJ, PHONE, EMAIL, RANDOM;

    fun converterParaTipoDeChave(): TipoDeChave {
        return when(this){
            CPF -> TipoDeChave.CPF
            CNPJ -> TipoDeChave.CNPJ
            PHONE-> TipoDeChave.CELULAR
            EMAIL -> TipoDeChave.EMAIL
            RANDOM -> TipoDeChave.ALEATORIA
        }
    }
}