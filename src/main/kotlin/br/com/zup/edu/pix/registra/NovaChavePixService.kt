package br.com.zup.edu.pix.registra

import br.com.zup.edu.integration.bcb.cadastra.CadastraChaveBCBRequest
import br.com.zup.edu.integration.bcb.cadastra.CadastroDeChavesNoBCBClient
import br.com.zup.edu.integration.bcb.modelos.BankAccount
import br.com.zup.edu.integration.bcb.modelos.ISPBMap
import br.com.zup.edu.integration.bcb.modelos.Owner
import br.com.zup.edu.integration.itau.cliente.ClientesNoItauClient
import br.com.zup.edu.pix.modelos.ChavePix
import br.com.zup.edu.shared.exceptions.ChavePixExistenteException
import br.com.zup.edu.pix.modelos.ChavePixRepository
import br.com.zup.edu.pix.modelos.TipoDeChave
import io.grpc.Status
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val itauClient: ClientesNoItauClient,
    @Inject val bcbClient: CadastroDeChavesNoBCBClient,
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        // 1. verifica se chave já existe no sistema
        if (repository.existsByChave(novaChave.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChave.chave}' existente")

        // 2. busca dados da conta no ERP do ITAU
        val responseItau = itauClient.buscaContaPorTipo(novaChave.clienteId!!, novaChave.tipoDeConta!!.name)
        val conta = responseItau.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        // 3. grava no banco do brasil
        val bankAccount = BankAccount(
            ISPBMap.ispbs[conta.instituicao]!!,
            conta.agencia,
            conta.numeroDaConta,
            novaChave.tipoDeConta.mapToTipoDeContaBCB())

        val owner = Owner(
            "NATURAL_PERSON",
            conta.nomeDoTitular,
            conta.cpfDoTitular
        )

        val cadastraChaveBCBRequest = CadastraChaveBCBRequest(
            keyType = novaChave.tipoDeChave!!.mapToTipoDeChaveBCB(),
            key = novaChave.chave!!,
            bankAccount = bankAccount,
            owner = owner)

        val responseBcb = bcbClient.cadastraChave(cadastraChaveBCBRequest)

        if (responseBcb.status.equals(Status.ALREADY_EXISTS))
            throw IllegalStateException("Chave já cadastrada no Banco Central do Brasil")

        // 4. grava no banco de dados
        val chave = novaChave.toModel(conta)

        if (chave.tipoDeChave == TipoDeChave.ALEATORIA) // A chave aleatória eu pego a que o BCB me retornou
            chave.chave = responseBcb.body().key

        repository.save(chave)

        return chave
    }

}