package com.atm.webflux.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal

@Document(collection = "customers")
data class Customer(
    @Id
    var id: String? = null,

    @Indexed(unique = true)
    var name: String = "",

    var balance: BigDecimal = BigDecimal.ZERO,

    var debts: MutableMap<String, BigDecimal> = mutableMapOf()
)