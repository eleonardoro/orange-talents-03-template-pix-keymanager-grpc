package br.com.zup.edu.pix.consulta

import br.com.zup.edu.grpc.ConsultaChavePixRequest
import br.com.zup.edu.grpc.ConsultaChavePixResponse
import br.com.zup.edu.grpc.ConsultaExternaChavePixRequest
import br.com.zup.edu.grpc.KeymanagerConsultaGrpcServiceGrpc
import br.com.zup.edu.shared.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ConsultaChaveEndpoint(@Inject private val service: ConsultaChavePixService) :
    KeymanagerConsultaGrpcServiceGrpc.KeymanagerConsultaGrpcServiceImplBase() {

    override fun consulta(
        request: ConsultaChavePixRequest?,
        responseObserver: StreamObserver<ConsultaChavePixResponse>?,
    ) {
        responseObserver?.onNext(service.consulta(request!!))
        responseObserver?.onCompleted()
    }

    override fun consultaExterna(
        request: ConsultaExternaChavePixRequest?,
        responseObserver: StreamObserver<ConsultaChavePixResponse>?
    ) {
        responseObserver?.onNext(service.consultaExterna(request!!))
        responseObserver?.onCompleted()
    }

}