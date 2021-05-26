package br.com.zup.edu.integration.bcb.remove

data class RemoveChaveBCBResponse(
    val key: String,
    val participant: String,
    val deletedAt: String,
)