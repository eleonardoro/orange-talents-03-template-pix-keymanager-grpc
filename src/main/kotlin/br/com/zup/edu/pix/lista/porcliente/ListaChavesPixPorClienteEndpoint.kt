package br.com.zup.edu.pix.lista.porcliente

import br.com.zup.edu.grpc.KeymanagerListaPorClienteGrpcServiceGrpc
import br.com.zup.edu.grpc.ListaChavesPixPorClientePixRequest
import br.com.zup.edu.grpc.ListaChavesPixPorClienteResponse
import br.com.zup.edu.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ListaChavesPixPorClienteEndpoint(@Inject private val service: ListaChavesPixPorClienteService) :
    KeymanagerListaPorClienteGrpcServiceGrpc.KeymanagerListaPorClienteGrpcServiceImplBase() {

    override fun lista(
        request: ListaChavesPixPorClientePixRequest,
        responseObserver: StreamObserver<ListaChavesPixPorClienteResponse>,
    ) {
        if (request.clienteId.isNullOrBlank())
            throw IllegalArgumentException("O id do Cliente não pode ser nulo ou vazio")

        responseObserver.onNext(
            ListaChavesPixPorClienteResponse
                .newBuilder()
                .setClienteId(request.clienteId)
                .addAllChaves(service.lista(request))
                .build())
        responseObserver.onCompleted()
    }
}