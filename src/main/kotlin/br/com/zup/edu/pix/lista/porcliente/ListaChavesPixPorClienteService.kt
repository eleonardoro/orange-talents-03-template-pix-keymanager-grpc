package br.com.zup.edu.pix.lista.porcliente

import br.com.zup.edu.grpc.ListaChavesPixPorClientePixRequest
import br.com.zup.edu.grpc.ListaChavesPixPorClienteResponse.ChavePixCliente
import br.com.zup.edu.grpc.TipoDeChave
import br.com.zup.edu.grpc.TipoDeConta
import br.com.zup.edu.integration.bcb.consulta.ConsultaDeChavesNoBCBClient
import br.com.zup.edu.pix.modelos.ChavePixRepository
import com.google.protobuf.Timestamp
import io.micronaut.validation.Validated
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class ListaChavesPixPorClienteService(
    @Inject val repository: ChavePixRepository,
    @Inject val bcbClient: ConsultaDeChavesNoBCBClient,
) {

    @Transactional
    fun lista(@Valid request: ListaChavesPixPorClientePixRequest): List<ChavePixCliente> {
        return repository.findAllByClienteId(clienteId = UUID.fromString(request.clienteId)).map { chavePix ->
            ChavePixCliente
                .newBuilder()
                .setPixId(chavePix.id.toString())
                .setTipoDeChave(TipoDeChave.valueOf(chavePix.tipoDeChave.toString()))
                .setChavePix(chavePix.chave)
                .setTipoDeConta(TipoDeConta.valueOf(chavePix.tipoDeConta.toString()))
                .setDataCriacao(chavePix.criadaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
                .build()
        }
    }
}