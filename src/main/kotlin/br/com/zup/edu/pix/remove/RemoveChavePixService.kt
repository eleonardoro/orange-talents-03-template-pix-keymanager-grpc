package br.com.zup.edu.pix.remove

import br.com.zup.edu.integration.itau.ContasDeClientesNoItauClient
import br.com.zup.edu.pix.ChavePixNaoEncontradaException
import br.com.zup.edu.pix.ChavePixRepository
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
    @Inject val itauClient: ContasDeClientesNoItauClient,
) {

    @Transactional
    fun remove(@Valid removidaChave: RemovidaChavePix) {

        // 1. verifica se chave já existe no sistema
        val chaveExistente = repository.findByIdAndClienteId(UUID.fromString(removidaChave.pixId),
            UUID.fromString(removidaChave.clienteId))
        if (chaveExistente.isEmpty)
            throw ChavePixNaoEncontradaException("Chave Pix '${removidaChave.pixId}' ou Cliente '${removidaChave.clienteId}' não existente")

        // 2. busca dados do cliente no ERP do ITAU
        val response = itauClient.buscaClientePorId(removidaChave.clienteId!!)
        val conta = response.body() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        // 3. remove no banco de dados
        repository.delete(chaveExistente.get())
    }
}