package br.com.zup.edu.pix.remove

import br.com.zup.edu.grpc.RemoveChavePixRequest

fun RemoveChavePixRequest.toModel(): RemovidaChavePix {
    return RemovidaChavePix(
        clienteId = clienteId,
        pixId = pixId,
    )
}