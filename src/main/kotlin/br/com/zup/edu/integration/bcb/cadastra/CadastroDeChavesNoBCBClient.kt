package br.com.zup.edu.integration.bcb.cadastra

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("\${bcb.pix.url}")
interface CadastroDeChavesNoBCBClient {

    @Post(value = "/api/v1/pix/keys", produces = [MediaType.APPLICATION_XML])
    fun cadastraChave(@Body body: CadastraChaveBCBRequest): HttpResponse<CadastraChaveBCBResponse>
}