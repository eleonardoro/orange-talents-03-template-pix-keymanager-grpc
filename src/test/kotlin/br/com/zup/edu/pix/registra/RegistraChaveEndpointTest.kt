package br.com.zup.edu.pix.registra

import br.com.zup.edu.grpc.KeymanagerRegistraGrpcServiceGrpc
import br.com.zup.edu.grpc.RegistraChavePixRequest
import br.com.zup.edu.grpc.TipoDeChave
import br.com.zup.edu.grpc.TipoDeConta
import br.com.zup.edu.integration.bcb.cadastra.CadastraChaveBCBRequest
import br.com.zup.edu.integration.bcb.cadastra.CadastraChaveBCBResponse
import br.com.zup.edu.integration.bcb.cadastra.CadastroDeChavesNoBCBClient
import br.com.zup.edu.integration.bcb.modelos.BankAccount
import br.com.zup.edu.integration.bcb.modelos.ISPBMap
import br.com.zup.edu.integration.bcb.modelos.Owner
import br.com.zup.edu.integration.bcb.modelos.TipoDeChaveBCB
import br.com.zup.edu.integration.itau.conta.ContasNoItauClient
import br.com.zup.edu.integration.itau.conta.DadosDaContaResponse
import br.com.zup.edu.integration.itau.modelos.InstituicaoResponse
import br.com.zup.edu.integration.itau.modelos.TitularResponse
import br.com.zup.edu.pix.modelos.ChavePixRepository
import br.com.zup.edu.pix.modelos.ContaAssociada
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
import br.com.zup.edu.pix.modelos.TipoDeConta as TipoDeConta1

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub,
) {

    @Inject
    lateinit var itauClient: ContasNoItauClient

    @Inject
    lateinit var bcbClient: CadastroDeChavesNoBCBClient

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
            .setChave("63657520325")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(cadastraChaveBCBRequest(
            TipoDeConta1.CONTA_CORRENTE,
            "63657520325",
            TipoDeChaveBCB.CPF)))
            .thenReturn(HttpResponse.ok(cadastraChaveBCBResponse(
                TipoDeConta1.CONTA_CORRENTE,
                "63657520325",
                TipoDeChaveBCB.CPF)))

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
    fun `nao deve criar chave quando ja esta cadastrada no BCB`() {
        // ########## preparação ##########
        val request: RegistraChavePixRequest = RegistraChavePixRequest
            .newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.CPF)
            .setChave("63657520325")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(cadastraChaveBCBRequest(
            TipoDeConta1.CONTA_CORRENTE,
            "63657520325",
            TipoDeChaveBCB.CPF)))
            .thenReturn(HttpResponse.status(HttpStatus.UNPROCESSABLE_ENTITY))

        // ########## ação ##########

        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Chave já cadastrada no Banco Central do Brasil", status.description)
        }
    }

    private fun cadastraChaveBCBRequest(
        tipoDeConta: TipoDeConta1,
        key: String,
        tipoDeChave: TipoDeChaveBCB,
    ): CadastraChaveBCBRequest {
        return CadastraChaveBCBRequest(
            keyType = tipoDeChave,
            key = key,
            bankAccount = bankAccount(tipoDeConta),
            owner = owner()
        )
    }

    private fun bankAccount(
        tipoDeConta: TipoDeConta1,
    ): BankAccount {
        return BankAccount(
            ISPBMap.ispbsPorNome["ITAÚ UNIBANCO S.A."]!!,
            "1218",
            "291900",
            tipoDeConta.mapToTipoDeContaBCB())
    }

    private fun owner(): Owner {
        return Owner(
            "NATURAL_PERSON",
            "Rafael Ponte",
            "63657520325"
        )
    }

    private fun cadastraChaveBCBResponse(
        tipoDeConta: TipoDeConta1,
        key: String,
        tipoDeChave: TipoDeChaveBCB,
    ): CadastraChaveBCBResponse {
        return CadastraChaveBCBResponse(
            keyType = tipoDeChave,
            key = key,
            bankAccount = bankAccount(tipoDeConta),
            owner = owner(),
        )
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

        `when`(bcbClient.cadastraChave(cadastraChaveBCBRequest(
            TipoDeConta1.CONTA_CORRENTE,
            "+5562996368679",
            TipoDeChaveBCB.PHONE)))
            .thenReturn(HttpResponse.ok(cadastraChaveBCBResponse(
                TipoDeConta1.CONTA_CORRENTE,
                "+5562996368679",
                TipoDeChaveBCB.PHONE)))

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

        `when`(bcbClient.cadastraChave(cadastraChaveBCBRequest(
            TipoDeConta1.CONTA_CORRENTE,
            "eleonardo.ro@gmail.com",
            TipoDeChaveBCB.EMAIL)))
            .thenReturn(HttpResponse.ok(cadastraChaveBCBResponse(
                TipoDeConta1.CONTA_CORRENTE,
                "eleonardo.ro@gmail.com",
                TipoDeChaveBCB.EMAIL)))

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

        `when`(bcbClient.cadastraChave(cadastraChaveBCBRequest(
            TipoDeConta1.CONTA_CORRENTE,
            "",
            TipoDeChaveBCB.RANDOM)))
            .thenReturn(HttpResponse.ok(cadastraChaveBCBResponse(
                TipoDeConta1.CONTA_CORRENTE,
                "c56dfef4-7901-44fb-84e2-a2cefb157890",
                TipoDeChaveBCB.RANDOM)))

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
            .setChave("63657520325")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build()

        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        `when`(bcbClient.cadastraChave(cadastraChaveBCBRequest(
            TipoDeConta1.CONTA_CORRENTE,
            "63657520325",
            TipoDeChaveBCB.CPF)))
            .thenReturn(HttpResponse.ok(cadastraChaveBCBResponse(
                TipoDeConta1.CONTA_CORRENTE,
                "63657520325",
                TipoDeChaveBCB.CPF)))

        // ########## ação ##########
        grpcClient.registra(request)
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.registra(request)
        }

        // ########## validação ##########
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave Pix '63657520325' existente", status.description)
        }
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {
        return DadosDaContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("ITAÚ UNIBANCO S.A.", ContaAssociada.ITAU_UNIBANCO_ISPB),
            agencia = "1218",
            numero = "291900",
            titular = TitularResponse("Rafael Ponte", "63657520325")
        )
    }

    @MockBean(ContasNoItauClient::class)
    fun itauClient(): ContasNoItauClient? {
        return Mockito.mock(ContasNoItauClient::class.java)
    }

    @MockBean(CadastroDeChavesNoBCBClient::class)
    fun bcbClient(): CadastroDeChavesNoBCBClient? {
        return Mockito.mock(CadastroDeChavesNoBCBClient::class.java)
    }

    @Factory
    class Chaves {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub {
            return KeymanagerRegistraGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}

