package com.atm.webflux.dto

import java.math.BigDecimal

data class TransferRequest(val targetName: String, val amount: BigDecimal)