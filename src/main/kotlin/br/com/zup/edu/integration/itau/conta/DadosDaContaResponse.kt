package br.com.zup.edu.integration.itau.conta

import br.com.zup.edu.integration.itau.modelos.InstituicaoResponse
import br.com.zup.edu.integration.itau.modelos.TitularResponse
import br.com.zup.edu.pix.modelos.ContaAssociada


data class DadosDaContaResponse(
    val tipo: String,
    val instituicao: InstituicaoResponse,
    val agencia: String,
    val numero: String,
    val titular: TitularResponse
) {

    fun toModel(): ContaAssociada {
        return ContaAssociada(
                instituicao = this.instituicao.nome,
                nomeDoTitular = this.titular.nome,
                cpfDoTitular = this.titular.cpf,
                agencia = this.agencia,
                numeroDaConta = this.numero
        )
    }

}