package br.com.zup.edu.integration.itau.cliente

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.client.annotation.Client

@Client("\${itau.contas.url}")
interface ClientesNoItauClient {

    @Get("/api/v1/clientes/{clienteId}")
    fun buscaClientePorId(@PathVariable clienteId: String): HttpResponse<DadosDoClienteResponse>
}