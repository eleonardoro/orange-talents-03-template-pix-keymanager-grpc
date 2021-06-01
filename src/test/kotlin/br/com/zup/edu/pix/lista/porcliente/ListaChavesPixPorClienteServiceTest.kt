package br.com.zup.edu.pix.lista.porcliente

import br.com.zup.edu.grpc.KeymanagerListaPorClienteGrpcServiceGrpc
import br.com.zup.edu.grpc.ListaChavesPixPorClientePixRequest
import br.com.zup.edu.integration.itau.modelos.InstituicaoResponse
import br.com.zup.edu.pix.modelos.*
import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class ListaChavesPixPorClienteServiceTest(
    @Inject val repository: ChavePixRepository,
    private val grpcClient: KeymanagerListaPorClienteGrpcServiceGrpc.KeymanagerListaPorClienteGrpcServiceBlockingStub,
) {

    private lateinit var CHAVE_EXISTENTE1: ChavePix
    private lateinit var CHAVE_EXISTENTE2: ChavePix
    private lateinit var CHAVE_EXISTENTE3: ChavePix

    @BeforeEach
    internal fun setup() {
        repository.deleteAll()

        val clienteId = UUID.randomUUID()

        val chavePix1 = ChavePix(
            clienteId,
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

        val chavePix2 = ChavePix(
            clienteId,
            TipoDeChave.CPF,
            "71022634062",
            TipoDeConta.CONTA_CORRENTE,
            ContaAssociada(
                InstituicaoResponse("ITAU", "ITAU").toString(),
                "Eleonardo Oliveira",
                "40825844045",
                "0001",
                "0002"
            )
        )

        val chavePix3 = ChavePix(
            UUID.randomUUID(),
            TipoDeChave.CPF,
            "40840105029",
            TipoDeConta.CONTA_CORRENTE,
            ContaAssociada(
                InstituicaoResponse("ITAU", "ITAU").toString(),
                "Eleonardo Oliveira",
                "40825844045",
                "0001",
                "0002"
            )
        )

        CHAVE_EXISTENTE1 = repository.save(chavePix1)
        CHAVE_EXISTENTE2 = repository.save(chavePix2)
        CHAVE_EXISTENTE3 = repository.save(chavePix3)
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `deve retornar chaves quando passar cliente id com chaves registradas`() {
        // preparação

        // ação
        val response = grpcClient.lista(ListaChavesPixPorClientePixRequest
            .newBuilder()
            .setClienteId(CHAVE_EXISTENTE1.clienteId.toString())
            .build())

        // validação
        Assertions.assertEquals(response.chavesCount, 2)
    }

    @Test
    fun `deve retornar array vazio quando passar cliente id sem chaves registradas`() {
        // preparação

        // ação
        val response = grpcClient.lista(ListaChavesPixPorClientePixRequest
            .newBuilder()
            .setClienteId(UUID.randomUUID().toString())
            .build())

        // validação
        Assertions.assertEquals(response.chavesCount, 0)
    }

    @Test
    fun `deve retornar excecao quando passar cliente id vazio`() {
        // preparação

        // ação
        val error = assertThrows<StatusRuntimeException> {
            grpcClient.lista(ListaChavesPixPorClientePixRequest
                .newBuilder()
                .setClienteId("")
                .build())
        }

        // validação
        with(error) {
            Assertions.assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            Assertions.assertEquals("O id do Cliente não pode ser nulo ou vazio", status.description)
        }
    }

    @Factory
    class Chaves {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): KeymanagerListaPorClienteGrpcServiceGrpc.KeymanagerListaPorClienteGrpcServiceBlockingStub {
            return KeymanagerListaPorClienteGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}