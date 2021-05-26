package br.com.zup.edu.pix.remove

import br.com.zup.edu.grpc.KeymanagerRemoveGrpcServiceGrpc
import br.com.zup.edu.grpc.RemoveChavePixRequest
import br.com.zup.edu.integration.itau.ContasDeClientesNoItauClient
import br.com.zup.edu.integration.itau.DadosDoClienteResponse
import br.com.zup.edu.integration.itau.InstituicaoResponse
import br.com.zup.edu.pix.*
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.*
import org.mockito.Mockito
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RemoveChaveEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceBlockingStub,
) {

    @Inject
    lateinit var itauClient: ContasDeClientesNoItauClient

    companion object {
        val CLIENTE_ID: UUID = UUID.randomUUID()
        var PIX_ID: UUID = UUID.randomUUID()
        val TIPO_CHAVE = TipoDeChave.CPF
        val TIPO_CONTA = TipoDeConta.CONTA_CORRENTE
        const val CLIENTE_NOME = "Eleonardo Oliveira"
        const val CLIENTE_CPF = "40825844045"
        const val CLIENTE_AGENCIA = "0001"
        const val CLIENTE_CONTA = "0002"
        const val INSTITUICAO_NOME = "ITAU"
        const val INSTITUICAO_ISPB = "ITAU"
    }

    @BeforeEach
    internal fun setup() {
        repository.deleteAll()

        val chavePix = ChavePix(
            CLIENTE_ID,
            TIPO_CHAVE,
            CLIENTE_CPF,
            TIPO_CONTA,
            ContaAssociada(
                InstituicaoResponse(INSTITUICAO_NOME, INSTITUICAO_ISPB).toString(),
                CLIENTE_NOME,
                CLIENTE_CPF,
                CLIENTE_AGENCIA,
                CLIENTE_CONTA
            )
        )

        repository.save(chavePix)
        PIX_ID = chavePix.id!!
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `deve remover chave quando passar dados corretos`() {
        //preparação
        Mockito.`when`(itauClient.buscaClientePorId(clienteId = CLIENTE_ID.toString()))
            .thenReturn(HttpResponse.ok(dadosDoClienteResponse()))

        //ação e validação
        Assertions.assertEquals(1, repository.findAll().size)

        grpcClient.remove(RemoveChavePixRequest.newBuilder().setPixId(PIX_ID.toString())
            .setClienteId(CLIENTE_ID.toString()).build())

        Assertions.assertEquals(0, repository.findAll().size)
    }

    @Test
    fun `nao deve remover chave quando o id do cliente e um UUID invalido`() {
        // ########## preparação ##########
        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId("aaaaaaaaaa")
            .setPixId(PIX_ID.toString())
            .build()

        //ação e validação
        Assertions.assertEquals(1, repository.findAll().size)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        Assertions.assertEquals(1, repository.findAll().size)

        with(error) {
            Assertions.assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            Assertions.assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve remover chave quando o id pix e um UUID invalido`() {
        // ########## preparação ##########
        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setPixId("aaaaaaaaaa")
            .build()

        //ação e validação
        Assertions.assertEquals(1, repository.findAll().size)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        Assertions.assertEquals(1, repository.findAll().size)

        with(error) {
            Assertions.assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            Assertions.assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve remover chave quando passar id da chave que nao existe`() {
        //preparação
        Mockito.`when`(itauClient.buscaClientePorId(clienteId = CLIENTE_ID.toString()))
            .thenReturn(HttpResponse.ok(dadosDoClienteResponse()))

        val uuid = UUID.randomUUID().toString()

        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setPixId(uuid)
            .build()

        //ação e validação
        Assertions.assertEquals(1, repository.findAll().size)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        Assertions.assertEquals(1, repository.findAll().size)

        with(error) {
            Assertions.assertEquals(Status.NOT_FOUND.code, status.code)
            Assertions.assertEquals("Chave Pix '$uuid' ou Cliente '${CLIENTE_ID.toString()}' não existente", status.description)
        }
    }

    @Test
    fun `nao deve excluir chave quando passar id de cliente que nao existe`() {
        //preparação
        Mockito.`when`(itauClient.buscaClientePorId(clienteId = CLIENTE_ID.toString()))
            .thenReturn(HttpResponse.ok(dadosDoClienteResponse()))

        val uuid = UUID.randomUUID().toString()

        val request: RemoveChavePixRequest = RemoveChavePixRequest
            .newBuilder()
            .setClienteId(uuid)
            .setPixId(PIX_ID.toString())
            .build()

        //ação e validação
        Assertions.assertEquals(1, repository.findAll().size)

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.remove(request)
        }

        Assertions.assertEquals(1, repository.findAll().size)

        with(error) {
            Assertions.assertEquals(Status.NOT_FOUND.code, status.code)
            Assertions.assertEquals("Chave Pix '${PIX_ID.toString()}' ou Cliente '$uuid' não existente", status.description)
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
            Assertions.assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            Assertions.assertEquals("Dados inválidos", status.description)
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
            Assertions.assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            Assertions.assertEquals("Dados inválidos", status.description)
        }
    }

    private fun dadosDoClienteResponse(): DadosDoClienteResponse? {
        return DadosDoClienteResponse(
            CLIENTE_ID.toString(),
            CLIENTE_NOME,
            CLIENTE_CPF,
            InstituicaoResponse(INSTITUICAO_NOME, INSTITUICAO_ISPB)
        )

    }

    @MockBean(ContasDeClientesNoItauClient::class)
    fun itauClient(): ContasDeClientesNoItauClient? {
        return Mockito.mock(ContasDeClientesNoItauClient::class.java)
    }

    @Factory
    class ChavesRemove {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeymanagerRemoveGrpcServiceGrpc.KeymanagerRemoveGrpcServiceBlockingStub {
            return KeymanagerRemoveGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}