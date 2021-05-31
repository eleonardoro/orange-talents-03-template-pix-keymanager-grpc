package br.com.zup.edu.pix.consulta

import br.com.zup.edu.grpc.*
import br.com.zup.edu.integration.bcb.consulta.ConsultaDeChavesNoBCBClient
import br.com.zup.edu.integration.bcb.modelos.ISPBMap
import br.com.zup.edu.pix.modelos.ChavePix
import br.com.zup.edu.pix.modelos.ChavePixRepository
import br.com.zup.edu.shared.exceptions.ChavePixNaoEncontradaException
import com.google.protobuf.Timestamp
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class ConsultaChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val bcbClient: ConsultaDeChavesNoBCBClient,
) {

    @Transactional
    fun consulta(@Valid request: ConsultaChavePixRequest): ConsultaChavePixResponse {

        // 1. buscando chave no banco de dados
        val chavePix: ChavePix =
            repository.findByIdAndClienteId(id = UUID.fromString(request.pixId),
                clienteId = UUID.fromString(request.clienteId))
                .orElseThrow {
                    ChavePixNaoEncontradaException("Chave Pix '${request.pixId}' ou Cliente '${request.clienteId}' não existente")
                }

        // 2. buscando se a chave está cadastrada no BCB
        val responseBcb = bcbClient.consultaChave(chavePix.chave)

        if (responseBcb.status.equals(HttpStatus.NOT_FOUND))
            throw IllegalStateException("Chave nao registrada no BCB")

        // 3. montando retorno
        return consultaChavePixResponse(chavePix)
    }

    @Transactional
    fun consultaExterna(@Valid request: ConsultaExternaChavePixRequest): ConsultaChavePixResponse {

        // 1. buscando chave no banco de dados
        val chavePixOptional = repository.findByChave(request.chavePix)

        if (chavePixOptional.isEmpty) {// 2. não tem a chave cadastrada no nosso banco de dados, vou buscar no BCB
            val responseBcb = bcbClient.consultaChave(request.chavePix.toString())

            if (responseBcb.status.equals(HttpStatus.NOT_FOUND))
                throw IllegalStateException("Chave não registrada no BCB")

            with(responseBcb.body()) {
                val contaVinculada = ConsultaChavePixResponse.ContaVinculada
                    .newBuilder()
                    .setNomeInstituicao(ISPBMap.ispbsPorCodigo[bankAccount.participant])
                    .setAgencia(bankAccount.branch)
                    .setConta(bankAccount.accountNumber)
                    .setTipoDeConta(bankAccount.accountType.converterParaTipoConta())
                    .build()

                return ConsultaChavePixResponse
                    .newBuilder()
                    .setPixId(key)
                    .setClienteId(owner.taxIdNumber)
                    .setTipoDeChave(keyType.converterParaTipoDeChave())
                    .setChave(key)
                    .setNomeDoTitular(owner.name)
                    .setCpfDoTitular(owner.taxIdNumber)
                    .addContaVinculada(contaVinculada)
                    .setDataCriacao(responseBcb.body().createdAt.let {
                        val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                        Timestamp.newBuilder()
                            .setSeconds(createdAt.epochSecond)
                            .setNanos(createdAt.nano)
                            .build()
                    })
                    .build()
            }
        } else { // 3. já existe a chave cadastrada no banco, só retornar ela
            return consultaChavePixResponse(chavePixOptional.get())
        }
    }

    private fun consultaChavePixResponse(chavePix: ChavePix): ConsultaChavePixResponse {
        val contaVinculada = ConsultaChavePixResponse.ContaVinculada
            .newBuilder()
            .setNomeInstituicao(chavePix.conta.instituicao)
            .setAgencia(chavePix.conta.agencia)
            .setConta(chavePix.conta.numeroDaConta)
            .setTipoDeConta(TipoDeConta.valueOf(chavePix.tipoDeConta.toString()))
            .build()

        val instant: Instant = chavePix.criadaEm.toInstant(ZoneOffset.UTC)

        val timestamp = Timestamp.newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()

        return ConsultaChavePixResponse
            .newBuilder()
            .setPixId(chavePix.id.toString())
            .setClienteId(chavePix.clienteId.toString())
            .setTipoDeChave(TipoDeChave.valueOf(chavePix.tipoDeChave.toString()))
            .setChave(chavePix.chave)
            .setNomeDoTitular(chavePix.conta.nomeDoTitular)
            .setCpfDoTitular(chavePix.conta.cpfDoTitular)
            .addContaVinculada(contaVinculada)
            .setDataCriacao(timestamp)
            .build()
    }
}