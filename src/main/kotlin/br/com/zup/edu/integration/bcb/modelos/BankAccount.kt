package br.com.zup.edu.integration.bcb.modelos

data class BankAccount(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: AccountTypeBCB,
)