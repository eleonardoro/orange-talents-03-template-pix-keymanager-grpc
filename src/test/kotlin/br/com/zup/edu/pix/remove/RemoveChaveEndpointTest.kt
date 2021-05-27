package br.com.zup.edu.pix.remove

import br.com.zup.edu.grpc.KeymanagerRemoveGrpcServiceGrpc
import br.com.zup.edu.grpc.RemoveChavePixRequest
import br.com.zup.edu.integration.bcb.remove.RemoveChaveBCBRequest
import br.com.zup.edu.integration.bcb.remove.RemoveChaveBCBResponse
import br.com.zup.edu.integration.bcb.remove.RemoveChavesNoBCBClient
import br.com.zup.edu.integration.itau.cliente.ClientesNoItauClient
import br.com.zup.edu.integration.itau.cliente.DadosDoClienteResponse
import br.com.zup.edu.integration.itau.modelos.InstituicaoResponse
import br.com.zup.edu.pix.modelos.*
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RemoveChaveEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceBlockingStub,
) {

    @Inject
    lateinit var itauClient: ClientesNoItauClient

    @Inject
    lateinit var bcbClient: RemoveChavesNoBCBClient

    private lateinit var CHAVE_EXISTENTE: ChavePix

    @BeforeEach
    internal fun setup() {
        repository.deleteAll()

        val chavePix = ChavePix(
            UUID.randomUUID(),
            TipoDeChave.CPF,
            "40825844045",
            TipoDeConta.CONTA_CORRENTE,
            ContaAssociada(
                InstituicaoResponse("ITAU", "ITAU").toString(),
                "Eleonardo Oliveira",
                "40825844045",
                "0001",
                "0002"
            )
        )

        CHAVE_EXISTENTE = repository.save(chavePix)
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `deve remover chave quando passar dados corretos`() {
        //preparação
        `when`(itauClient.buscaClientePorId(clienteId = CHAVE_EXISTENTE.clienteId.toString()))
            .thenReturn(HttpResponse.ok(dadosDoClienteResponse()))

        `when`(bcbClient.removeChave(CHAVE_EXISTENTE.chave, removeChaveBCBRequest()))
            .thenReturn(HttpResponse.ok(removeChaveBCBResponse()))

        //ação e validação
        assertEquals(1, repository.findAll().size)

        grpcClient.remove(RemoveChavePixRequest.newBuilder().setPixId(CHAVE_EXISTENTE.id.toString())
            .setClienteId(CHAVE_EXISTENTE.clienteId.toString()).build())

        assertEquals(0, repository.findAll().size)
    }

    @Test
    fun `nao deve remover chave quando passar chave nao cadastrada no BCB`() {
        //preparação
        `when`(itauClient.buscaClientePorId(clienteId = CHAVE_EXISTENTE.clienteId.toString()))
            .thenReturn(HttpResponse.ok(dadosDoClienteResponse()))

        `when`(bcbClient.removeChave(CHAVE_EXISTENTE.chave, removeChaveBCBRequest()))
            .thenReturn(HttpResponse.notFound())

        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
            .setPixId(CHAVE_EXISTENTE.id.toString())
            .build()

        //ação e validação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        assertEquals(1, repository.findAll().size)

        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Chave não encontrada", status.description)
        }
    }

    @Test
    fun `nao deve remover chave quando a operacao for nao permitida no BCB`() {
        //preparação
        `when`(itauClient.buscaClientePorId(clienteId = CHAVE_EXISTENTE.clienteId.toString()))
            .thenReturn(HttpResponse.ok(dadosDoClienteResponse()))

        `when`(bcbClient.removeChave(CHAVE_EXISTENTE.chave, removeChaveBCBRequest()))
            .thenReturn(HttpResponse.status(HttpStatus.FORBIDDEN))

        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
            .setPixId(CHAVE_EXISTENTE.id.toString())
            .build()

        //ação e validação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        assertEquals(1, repository.findAll().size)

        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Operação não permitida", status.description)
        }
    }

    private fun removeChaveBCBRequest(): RemoveChaveBCBRequest {
        return RemoveChaveBCBRequest(
            key = CHAVE_EXISTENTE.chave,
            participant = "60701190"
        )
    }

    private fun removeChaveBCBResponse(): RemoveChaveBCBResponse {
        return RemoveChaveBCBResponse(
            key = CHAVE_EXISTENTE.chave.toString(),
            participant = "60701190",
            deletedAt = LocalDateTime.now().toString()
        )
    }

    @Test
    fun `nao deve remover chave quando o id do cliente e um UUID invalido`() {
        // ########## preparação ##########
        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId("aaaaaaaaaa")
            .setPixId(CHAVE_EXISTENTE.id.toString())
            .build()

        //ação e validação
        assertEquals(1, repository.findAll().size)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        assertEquals(1, repository.findAll().size)

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve remover chave quando o id pix e um UUID invalido`() {
        // ########## preparação ##########
        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
            .setPixId("aaaaaaaaaa")
            .build()

        //ação e validação
        assertEquals(1, repository.findAll().size)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        assertEquals(1, repository.findAll().size)

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve remover chave quando passar id da chave que nao existe`() {
        //preparação
        `when`(itauClient.buscaClientePorId(clienteId = CHAVE_EXISTENTE.clienteId.toString()))
            .thenReturn(HttpResponse.ok(dadosDoClienteResponse()))

        val uuid = UUID.randomUUID().toString()

        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
            .setPixId(uuid)
            .build()

        //ação e validação
        assertEquals(1, repository.findAll().size)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        assertEquals(1, repository.findAll().size)

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix '$uuid' ou Cliente '${CHAVE_EXISTENTE.clienteId}' não existente",
                status.description)
        }
    }

    @Test
    fun `nao deve excluir chave quando passar id de cliente que nao existe`() {
        //preparação
        `when`(itauClient.buscaClientePorId(clienteId = CHAVE_EXISTENTE.clienteId.toString()))
            .thenReturn(HttpResponse.ok(dadosDoClienteResponse()))

        val uuid = UUID.randomUUID().toString()

        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId(uuid)
            .setPixId(CHAVE_EXISTENTE.id.toString())
            .build()

        //ação e validação
        assertEquals(1, repository.findAll().size)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        assertEquals(1, repository.findAll().size)

        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix '${CHAVE_EXISTENTE.id.toString()}' ou Cliente '$uuid' não existente",
                status.description)
        }
    }

    @Test
    fun `nao deve remover chave quando enviar apenas um objeto em branco`() {
        // ########## preparação ##########
        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve remover chave quando nao enviar dado algum`() {
        // ########## preparação ##########

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(null)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    private fun dadosDoClienteResponse(): DadosDoClienteResponse {
        return DadosDoClienteResponse(
            CHAVE_EXISTENTE.clienteId.toString(),
            "Eleonardo",
            "40825844045",
            InstituicaoResponse("ITAÚ UNIBANCO S.A.", "60701190")
        )
    }

    @MockBean(ClientesNoItauClient::class)
    fun itauClient(): ClientesNoItauClient? {
        return Mockito.mock(ClientesNoItauClient::class.java)
    }

    @MockBean(RemoveChavesNoBCBClient::class)
    fun bcbClient(): RemoveChavesNoBCBClient? {
        return Mockito.mock(RemoveChavesNoBCBClient::class.java)
    }

    @Factory
    class ChavesRemove {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceBlockingStub {
            return KeymanagerRemoveGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}