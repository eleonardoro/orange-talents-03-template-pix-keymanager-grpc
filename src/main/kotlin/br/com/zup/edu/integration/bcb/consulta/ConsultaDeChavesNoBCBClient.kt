package br.com.zup.edu.integration.bcb.consulta

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("\${bcb.pix.url}")
interface ConsultaDeChavesNoBCBClient {

    @Get(value = "/api/v1/pix/keys/{key}", produces = [MediaType.APPLICATION_XML])
    fun consultaChave(@PathVariable key: String): HttpResponse<ConsultaChaveBCBResponse>
}