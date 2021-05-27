package br.com.zup.edu.integration.bcb.remove

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.client.annotation.Client

@Client("\${bcb.pix.url}")
interface RemoveChavesNoBCBClient {

    @Delete(value = "/api/v1/pix/keys/{key}", produces = [MediaType.APPLICATION_XML])
    fun removeChave(@PathVariable key: String, @Body body: RemoveChaveBCBRequest): HttpResponse<RemoveChaveBCBResponse>
}