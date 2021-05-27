package br.com.zup.edu.pix.registra

import br.com.zup.edu.pix.modelos.ChavePix
import br.com.zup.edu.pix.modelos.ContaAssociada
import br.com.zup.edu.pix.modelos.TipoDeChave
import br.com.zup.edu.pix.modelos.TipoDeConta
import br.com.zup.edu.shared.validation.ValidPixKey
import br.com.zup.edu.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidPixKey
@Introspected
data class NovaChavePix(
    @field:ValidUUID
    @field:NotBlank
    val clienteId: String?,

    @field:NotNull
    val tipoDeChave: TipoDeChave?,

    @field:Size(max = 77)
    val chave: String?,

    @field:NotNull
    val tipoDeConta: TipoDeConta?,
) {

    fun toModel(conta: ContaAssociada): ChavePix {
        return ChavePix(
            clienteId = UUID.fromString(this.clienteId),
            tipoDeChave = TipoDeChave.valueOf(this.tipoDeChave!!.name),
            chave = if (this.tipoDeChave == TipoDeChave.ALEATORIA) "c56dfef4-7901-44fb-84e2-a2cefb157890" else this.chave!!,
            tipoDeConta = TipoDeConta.valueOf(this.tipoDeConta!!.name),
            conta = conta
        )
    }

}
