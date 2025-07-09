package com.atm.webflux.controller

import com.atm.webflux.dto.AmountRequest
import com.atm.webflux.dto.StatusResponse
import com.atm.webflux.dto.TransferRequest
import com.atm.webflux.model.Customer
import com.atm.webflux.service.AtmService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.math.BigDecimal

@WebFluxTest(AtmController::class)
class AtmControllerTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var atmService: AtmService

    @Test
    fun `login should return status response`() {
        // Arrange
        val customerName = "Alice"
        val customer = Customer(name = customerName, balance = BigDecimal.ZERO)
        val expectedResponse = StatusResponse.from(customer, "Hello, $customerName!")

        whenever(atmService.findOrCreateCustomer(customerName)).thenReturn(Mono.just(customer))

        // Act & Assert
        webTestClient.post().uri("/api/login/$customerName")
            .exchange()
            .expectStatus().isOk
            .expectBody(StatusResponse::class.java)
            .isEqualTo(expectedResponse)
    }

    @Test
    fun `logout should return a goodbye message`() {
        // Arrange
        val customerName = "Alice"

        // Act & Assert
        webTestClient.post().uri("/api/logout/$customerName")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Goodbye, $customerName!")
    }

    @Test
    fun `deposit should return updated status`() {
        // Arrange
        val customerName = "Alice"
        val amount = BigDecimal(100)
        val request = AmountRequest(amount)
        val response = StatusResponse("Your balance is $100", customerName, amount, emptyList())

        whenever(atmService.deposit(customerName, amount)).thenReturn(Mono.just(response))

        // Act & Assert
        webTestClient.post().uri("/api/deposit/$customerName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(StatusResponse::class.java)
            .isEqualTo(response)
    }

    @Test
    fun `withdraw should return updated status`() {
        // Arrange
        val customerName = "Alice"
        val amount = BigDecimal(50)
        val request = AmountRequest(amount)
        val response = StatusResponse("Your balance is $50", customerName, BigDecimal(50), emptyList())

        whenever(atmService.withdraw(customerName, amount)).thenReturn(Mono.just(response))

        // Act & Assert
        webTestClient.post().uri("/api/withdraw/$customerName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(StatusResponse::class.java)
            .isEqualTo(response)
    }

    @Test
    fun `transfer should return updated status`() {
        // Arrange
        val sourceName = "Alice"
        val targetName = "Bob"
        val amount = BigDecimal(50)
        val request = TransferRequest(targetName, amount)
        val response = StatusResponse("Transferred 50 to Bob", sourceName, BigDecimal(50), emptyList())

        whenever(atmService.transfer(sourceName, targetName, amount)).thenReturn(Mono.just(response))

        // Act & Assert
        webTestClient.post().uri("/api/transfer/$sourceName")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(StatusResponse::class.java)
            .isEqualTo(response)
    }
}