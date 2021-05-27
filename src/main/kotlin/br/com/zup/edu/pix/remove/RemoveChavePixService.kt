package br.com.zup.edu.pix.remove

import br.com.zup.edu.integration.bcb.remove.RemoveChaveBCBRequest
import br.com.zup.edu.integration.bcb.remove.RemoveChavesNoBCBClient
import br.com.zup.edu.integration.itau.cliente.ClientesNoItauClient
import br.com.zup.edu.shared.exceptions.ChavePixNaoEncontradaException
import br.com.zup.edu.pix.modelos.ChavePixRepository
import io.grpc.Status.NOT_FOUND
import io.grpc.Status.PERMISSION_DENIED
import io.micronaut.validation.Validated
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class RemoveChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val itauClient: ClientesNoItauClient,
    @Inject val bcbClient: RemoveChavesNoBCBClient,
) {

    @Transactional
    fun remove(@Valid removidaChave: RemovidaChavePix) {

        // 1. verifica se chave já existe no sistema
        val chaveExistente = repository.findByIdAndClienteId(UUID.fromString(removidaChave.pixId),
            UUID.fromString(removidaChave.clienteId)).orElseThrow {
            ChavePixNaoEncontradaException("Chave Pix '${removidaChave.pixId}' ou Cliente '${removidaChave.clienteId}' não existente")
        }

        // 2. busca dados do cliente no ERP do ITAU
        val response = itauClient.buscaClientePorId(removidaChave.clienteId!!)
        val conta = response.body() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        // 3. remove do BCB
        val removeChaveBCBRequest = RemoveChaveBCBRequest(
            chaveExistente.chave,
            conta.instituicao.ispb
        )

        val responseBcb = bcbClient.removeChave(chaveExistente.chave, removeChaveBCBRequest)

        if (responseBcb.status.toString() == NOT_FOUND.code.toString())
            throw IllegalStateException("Chave não encontrada")

        if (responseBcb.status.toString() == "FORBIDDEN")
            throw IllegalStateException("Operação não permitida")

        // 4. remove no banco de dados
        repository.delete(chaveExistente)
    }
}