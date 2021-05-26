package br.com.zup.edu.pix.remove

import br.com.zup.edu.shared.validation.ValidUUID
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Introspected
data class RemovidaChavePix(
    @field:ValidUUID
    @field:NotBlank
    val clienteId: String?,

    @field:ValidUUID
    @field:NotBlank
    @field:Size(max = 77)
    val pixId: String?,
) {}
