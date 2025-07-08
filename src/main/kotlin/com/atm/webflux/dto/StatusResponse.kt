package com.atm.webflux.dto

import com.atm.webflux.model.Customer
import java.math.BigDecimal

data class StatusResponse(
    val message: String,
    val name: String,
    val balance: BigDecimal,
    val debtInfo: List<String>
) {
    companion object {
        fun from(customer: Customer, message: String): StatusResponse {
            val debtMessages = customer.debts.mapNotNull { (person, amount) ->
                when {
                    amount > BigDecimal.ZERO -> "Owed $amount from $person"
                    amount < BigDecimal.ZERO -> "Owed ${amount.abs()} to $person"
                    else -> null
                }
            }
            return StatusResponse(message, customer.name, customer.balance, debtMessages)
        }
    }
}