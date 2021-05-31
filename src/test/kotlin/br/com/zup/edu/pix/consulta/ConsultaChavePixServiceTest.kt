package br.com.zup.edu.pix.consulta

import br.com.zup.edu.grpc.ConsultaChavePixRequest
import br.com.zup.edu.grpc.ConsultaExternaChavePixRequest
import br.com.zup.edu.grpc.KeymanagerConsultaGrpcServiceGrpc
import br.com.zup.edu.integration.bcb.consulta.ConsultaChaveBCBResponse
import br.com.zup.edu.integration.bcb.consulta.ConsultaDeChavesNoBCBClient
import br.com.zup.edu.integration.bcb.modelos.AccountTypeBCB
import br.com.zup.edu.integration.bcb.modelos.BankAccount
import br.com.zup.edu.integration.bcb.modelos.Owner
import br.com.zup.edu.integration.bcb.modelos.TipoDeChaveBCB
import br.com.zup.edu.integration.itau.modelos.InstituicaoResponse
import br.com.zup.edu.pix.modelos.*
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
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class ConsultaChavePixServiceTest(
    @Inject val repository: ChavePixRepository,
    private val grpcClient: KeymanagerConsultaGrpcServiceGrpc.KeymanagerConsultaGrpcServiceBlockingStub,
) {

    @Inject
    lateinit var bcbClient: ConsultaDeChavesNoBCBClient

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
    fun `consulta interna - deve retorna chave cadastrada no banco de dados e no BCB`() {
        // preparação
        `when`(bcbClient.consultaChave(key = CHAVE_EXISTENTE.chave))
            .thenReturn(HttpResponse.ok())

        // ação
        val response = grpcClient.consulta(ConsultaChavePixRequest
            .newBuilder()
            .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
            .setPixId(CHAVE_EXISTENTE.id.toString())
            .build())

        // validação
        assertEquals(CHAVE_EXISTENTE.id, UUID.fromString(response.pixId))
        assertEquals(CHAVE_EXISTENTE.chave, response.chave)
        assertEquals(CHAVE_EXISTENTE.clienteId, UUID.fromString(response.clienteId))
    }

    @Test
    fun `consulta interna - nao deve retorna chave nao cadastrada no banco de dados`() {
        // preparação
        val idAux = UUID.randomUUID().toString()

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest
                .newBuilder()
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .setPixId(idAux)
                .build())
        }

        // validação
        with(error) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix '${idAux}' ou Cliente '${CHAVE_EXISTENTE.clienteId}' não existente",
                status.description)
        }
    }

    @Test
    fun `consulta interna - nao deve retorna chave nao cadastrada no BCB`() {
        // preparação
        `when`(bcbClient.consultaChave(key = CHAVE_EXISTENTE.chave.toString()))
            .thenReturn(HttpResponse.notFound())

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(ConsultaChavePixRequest
                .newBuilder()
                .setClienteId(CHAVE_EXISTENTE.clienteId.toString())
                .setPixId(CHAVE_EXISTENTE.id.toString())
                .build())
        }

        // validação
        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Chave nao registrada no BCB", status.description)
        }
    }

    @Test
    fun `consulta externa - deve retorna chave cadastrada no banco de dados`() {
        // preparação

        // ação
        val response = grpcClient.consultaExterna(ConsultaExternaChavePixRequest
            .newBuilder()
            .setChavePix(CHAVE_EXISTENTE.chave)
            .build())

        // validação
        assertEquals(CHAVE_EXISTENTE.id, UUID.fromString(response.pixId))
        assertEquals(CHAVE_EXISTENTE.chave, response.chave)
        assertEquals(CHAVE_EXISTENTE.clienteId, UUID.fromString(response.clienteId))
    }

    @Test
    fun `consulta externa - deve retorna chave nao cadastrada no banco de dados, mas cadastrada no BCB`() {
        // preparação
        val chaveAuxCpf: String = "99034578046"

        `when`(bcbClient.consultaChave(key = chaveAuxCpf))
            .thenReturn(HttpResponse.ok(consultaChaveBCBResponse(chaveAuxCpf)))

        // ação
        val response = grpcClient.consultaExterna(ConsultaExternaChavePixRequest
            .newBuilder()
            .setChavePix(chaveAuxCpf)
            .build())

        // validação
        assertEquals(response.chave, chaveAuxCpf)
        assertNotEquals(CHAVE_EXISTENTE.chave, chaveAuxCpf)
        assertTrue(repository.findByChave(chaveAuxCpf).isEmpty)
    }

    @Test
    fun `consulta externa - nao deve retorna chave nao cadastrada no banco de dados, nem no BCB`() {
        // preparação
        val chaveAuxCpf: String = "99034578046"

        `when`(bcbClient.consultaChave(key = chaveAuxCpf))
            .thenReturn(HttpResponse.notFound())

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.consultaExterna(ConsultaExternaChavePixRequest
                .newBuilder()
                .setChavePix(chaveAuxCpf)
                .build())
        }

        // validação
        with(error) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Chave não registrada", status.description)
        }
    }

    fun consultaChaveBCBResponse(chaveAuxCpf: String): ConsultaChaveBCBResponse {
        return ConsultaChaveBCBResponse(
            TipoDeChaveBCB.CPF,
            chaveAuxCpf,
            BankAccount("60701190", "0001", "0001", AccountTypeBCB.CACC),
            Owner("PF", "Eleonardo Oliveira", chaveAuxCpf),
            LocalDateTime.now()
        )
    }

    @MockBean(ConsultaDeChavesNoBCBClient::class)
    fun bcbClient(): ConsultaDeChavesNoBCBClient? {
        return Mockito.mock(ConsultaDeChavesNoBCBClient::class.java)
    }

    @Factory
    class Chaves {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeymanagerConsultaGrpcServiceGrpc.KeymanagerConsultaGrpcServiceBlockingStub {
            return KeymanagerConsultaGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}