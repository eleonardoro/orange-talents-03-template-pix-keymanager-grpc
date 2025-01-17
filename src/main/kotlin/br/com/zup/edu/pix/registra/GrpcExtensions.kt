package br.com.zup.edu.pix.registra

import br.com.zup.edu.grpc.RegistraChavePixRequest
import br.com.zup.edu.grpc.TipoDeChave.*
import br.com.zup.edu.grpc.TipoDeConta.*
import br.com.zup.edu.pix.modelos.TipoDeChave
import br.com.zup.edu.pix.modelos.TipoDeConta

fun RegistraChavePixRequest.toModel() : NovaChavePix {
    return NovaChavePix(
        clienteId = clienteId,
        tipoDeChave = when (tipoDeChave) {
            UNKNOWN_TIPO_CHAVE -> null
            else -> TipoDeChave.valueOf(tipoDeChave.name)
        },
        chave = chave,
        tipoDeConta = when (tipoDeConta) {
            UNKNOWN_TIPO_CONTA -> null
            else -> TipoDeConta.valueOf(tipoDeConta.name)
        }
    )
}
