package br.com.zup.edu.pix.registra

import br.com.zup.edu.grpc.KeymanagerRegistraGrpcServiceGrpc
import br.com.zup.edu.grpc.RegistraChavePixRequest
import br.com.zup.edu.grpc.TipoDeChave
import br.com.zup.edu.grpc.TipoDeConta
import br.com.zup.edu.integration.itau.ContasDeClientesNoItauClient
import br.com.zup.edu.integration.itau.DadosDaContaResponse
import br.com.zup.edu.integration.itau.InstituicaoResponse
import br.com.zup.edu.integration.itau.TitularResponse
import br.com.zup.edu.pix.ChavePixRepository
import br.com.zup.edu.pix.ContaAssociada
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub,
) {

    @Inject
    lateinit var itauClient: ContasDeClientesNoItauClient

    companion object {
        val CLIENTE_ID: UUID = UUID.randomUUID()
    }

    @BeforeEach
    internal fun setup() {
        repository.deleteAll()
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `deve criar chave cpf`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.CPF)
            .setChave("02110126108")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        // ########## ação ##########
        val response = grpcClient.registra(request)

        // ########## validação ##########
        with(response) {
            assertNotNull(pixId)
            assertNotNull(clienteId)
            assertEquals(CLIENTE_ID.toString(), clienteId)
            assertTrue(repository.existsById(UUID.fromString(pixId)))
        }
        assertEquals(1, repository.findAll().size)
    }

    @Test
    fun `deve criar chave celular`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.CELULAR)
            .setChave("+5562996368679")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        // ########## ação ##########
        val response = grpcClient.registra(request)

        // ########## validação ##########
        with(response) {
            assertNotNull(pixId)
            assertNotNull(clienteId)
            assertTrue(repository.existsById(UUID.fromString(pixId)))
        }
        assertEquals(1, repository.findAll().size)
    }

    @Test
    fun `deve criar chave email`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.EMAIL)
            .setChave("eleonardo.ro@gmail.com")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        // ########## ação ##########
        val response = grpcClient.registra(request)

        // ########## validação ##########
        with(response) {
            assertNotNull(pixId)
            assertNotNull(clienteId)
            assertTrue(repository.existsById(UUID.fromString(pixId)))
        }
        assertEquals(1, repository.findAll().size)
    }

    @Test
    fun `deve criar chave aleatoria`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.ALEATORIA)
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        // ########## ação ##########
        val response = grpcClient.registra(request)

        // ########## validação ##########
        with(response) {
            assertNotNull(pixId)
            assertNotNull(clienteId)
            assertTrue(repository.existsById(UUID.fromString(pixId)))
        }
        assertEquals(1, repository.findAll().size)
    }

    @Test
    fun `nao deve criar chave quando o id do cliente e um UUID invalido`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId("aaaaaaaaaa")
            .setTipoDeChave(TipoDeChave.ALEATORIA)
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve criar chave com cpf invalido`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.CPF)
            .setChave("02110126109")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve criar chave com celular invalido`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.CELULAR)
            .setChave("+62")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve criar chave com email invalido`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.EMAIL)
            .setChave("eleonardo.rogmail.com")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve criar chave aleatoria quando passar algum dado de chave`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.ALEATORIA)
            .setChave("eleonardo.ro@gmail.com")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve criar chave quando cliente Id esta valido mas nao cadastrado no Itau ou nao tem essa conta`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.ALEATORIA)
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error){
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itau", status.description)
        }
    }

    @Test
    fun `nao deve criar chave quando nao especificar tipo de conta`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.CPF)
            .setChave("02110126108")
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve criar chave quando nao especificar tipo de chave`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setChave("02110126108")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve criar chave quando enviar apenas um objeto em branco`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve criar chave quando nao enviar dado algum`() {
        // ########## preparação ##########

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(null)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve criar chave repetida`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.CPF)
            .setChave("02110126108")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        // ########## ação ##########
        grpcClient.registra(request)
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave Pix '02110126108' existente", status.description)
        }
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {
        return DadosDaContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("UNIBANCO ITAU SA", ContaAssociada.ITAU_UNIBANCO_ISPB),
            agencia = "1218",
            numero = "291900",
            titular = TitularResponse("Rafael Ponte", "63657520325")
        )
    }

    @MockBean(ContasDeClientesNoItauClient::class)
    fun itauClient(): ContasDeClientesNoItauClient? {
        return Mockito.mock(ContasDeClientesNoItauClient::class.java)
    }

    @Factory
    class Chaves {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub {
            return KeymanagerRegistraGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}

