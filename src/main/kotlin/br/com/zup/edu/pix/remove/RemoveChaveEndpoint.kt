package br.com.zup.edu.pix.remove

import br.com.zup.edu.grpc.KeymanagerRemoveGrpcServiceGrpc
import br.com.zup.edu.grpc.RemoveChavePixRequest
import br.com.zup.edu.grpc.RemoveChavePixResponse
import br.com.zup.edu.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RemoveChaveEndpoint(@Inject private val service: RemoveChavePixService) :
    KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceImplBase() {

    override fun remove(
        request: RemoveChavePixRequest,
        responseObserver: StreamObserver<RemoveChavePixResponse>,
    ) {

        val removidaChave = request.toModel()
        service.remove(removidaChave)

        responseObserver.onNext(RemoveChavePixResponse.newBuilder()
            .setPixId(removidaChave.pixId)
            .setClienteId(removidaChave.clienteId)
            .build())
        responseObserver.onCompleted()
    }
}