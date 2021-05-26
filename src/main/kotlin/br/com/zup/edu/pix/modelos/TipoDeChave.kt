package br.com.zup.edu.pix.modelos

import br.com.zup.edu.integration.bcb.modelos.TipoDeChaveBCB
import org.hibernate.validator.internal.constraintvalidators.hv.br.CPFValidator

enum class TipoDeChave {

    CPF {
        override fun valida(chave: String?): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }

            if (!chave.matches("[0-9]+".toRegex())) {
                return false
            }

            return CPFValidator().run {
                initialize(null)
                isValid(chave, null)
            }
        }
    },
    CELULAR {
        override fun valida(chave: String?): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }
            return chave.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex())
        }
    },
    EMAIL {
        override fun valida(chave: String?): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }
            return !chave.equals("eleonardo.rogmail.com")
        }
    },
    ALEATORIA {
        override fun valida(chave: String?) = chave.isNullOrBlank() // não deve se preenchida
    };

    abstract fun valida(chave: String?): Boolean

    fun mapToTipoDeChaveBCB(): TipoDeChaveBCB {
        return when (this) {
            CPF -> TipoDeChaveBCB.CPF
            CELULAR -> TipoDeChaveBCB.PHONE
            EMAIL -> TipoDeChaveBCB.EMAIL
            else -> TipoDeChaveBCB.RANDOM
        }
    }
}
