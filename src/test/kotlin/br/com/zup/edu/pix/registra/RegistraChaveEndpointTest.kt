package br.com.zup.edu.pix.registra

import br.com.zup.edu.grpc.KeymanagerRegistraGrpcServiceGrpc
import br.com.zup.edu.grpc.RegistraChavePixRequest
import br.com.zup.edu.grpc.TipoDeChave
import br.com.zup.edu.grpc.TipoDeConta
import br.com.zup.edu.pix.ChavePixRepository
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.inject.Singleton
import org.junit.jupiter.api.assertThrows

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub,
) {

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
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setTipoDeChave(TipoDeChave.CPF)
            .setChave("02110126108")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

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
    fun `deve criar chave celular`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setTipoDeChave(TipoDeChave.CELULAR)
            .setChave("+5562996368679")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

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
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setTipoDeChave(TipoDeChave.EMAIL)
            .setChave("eleonardo.ro@gmail.com")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

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
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setTipoDeChave(TipoDeChave.ALEATORIA)
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

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
    fun `nao deve criar chave quando o id do cliente e invalido`() {
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
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
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
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
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
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
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
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
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
    fun `nao deve criar chave quando passar um id de Cliente valido mas nao cadastrado no Itau`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157891")
            .setTipoDeChave(TipoDeChave.ALEATORIA)
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itau", status.description)
        }
    }

    @Test
    fun `nao deve criar chave quando cliente id esta certo mas nao tem a conta do tipo especificado`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setTipoDeChave(TipoDeChave.CPF)
            .setChave("02110126108")
            .setTipoDeConta(TipoDeConta.CONTA_POUPANCA)
            .build()

        // ########## ação ##########
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itau", status.description)
        }
    }

    @Test
    fun `nao deve criar chave quando nao especificar tipo de conta`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
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
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
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
    fun `deve criar chave repetida`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setTipoDeChave(TipoDeChave.CPF)
            .setChave("02110126108")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

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
}

@Factory
class Chaves {
    @Singleton
    fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub {
        return KeymanagerRegistraGrpcServiceGrpc.newBlockingStub(channel)
    }
}