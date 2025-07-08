package com.atm.webflux.controller

import com.atm.webflux.dto.AmountRequest
import com.atm.webflux.dto.StatusResponse
import com.atm.webflux.dto.TransferRequest
import com.atm.webflux.service.AtmService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api")
class AtmController(private val atmService: AtmService) {

    @Operation(summary = "Login as a user to ATM service")
    @PostMapping("/login/{name}")
    fun login(@PathVariable name: String): Mono<StatusResponse> {
        return atmService.findOrCreateCustomer(name)
            .map { StatusResponse.from(it, "Hello, $name!") }
    }

    @Operation(summary = "Logout from ATM service")
    @PostMapping("/logout/{name}")
    fun logout(@PathVariable name: String): Mono<String> {
        return Mono.just("Goodbye, $name!")
    }

    @Operation(summary = "As a logged in user, deposit some money")
    @PostMapping("/deposit/{name}")
    fun deposit(@PathVariable name: String, @RequestBody request: AmountRequest): Mono<StatusResponse> {
        return atmService.deposit(name, request.amount)
    }

    @Operation(summary = "As a logged in user, withdraw some money from ATM")
    @PostMapping("/withdraw/{name}")
    fun withdraw(@PathVariable name: String, @RequestBody request: AmountRequest): Mono<StatusResponse> {
        return atmService.withdraw(name, request.amount)
    }

    @Operation(summary = "As a logged in user, transfer some money to another user")
    @PostMapping("/transfer/{sourceName}")
    fun transfer(@PathVariable sourceName: String, @RequestBody request: TransferRequest): Mono<StatusResponse> {
        return atmService.transfer(sourceName, request.targetName, request.amount)
    }
}