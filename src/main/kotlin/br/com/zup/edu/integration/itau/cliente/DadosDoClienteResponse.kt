package br.com.zup.edu.integration.itau.cliente

import br.com.zup.edu.integration.itau.modelos.InstituicaoResponse


data class DadosDoClienteResponse(
    val id: String,
    val nome: String,
    val cpf: String,
    val instituicao: InstituicaoResponse,
) {}