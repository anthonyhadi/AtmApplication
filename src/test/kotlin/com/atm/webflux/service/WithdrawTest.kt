package com.atm.webflux.service

import com.atm.webflux.model.Customer
import com.atm.webflux.repository.CustomerRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class WithdrawTest {

    @Mock
    private lateinit var customerRepository: CustomerRepository

    @InjectMocks
    private lateinit var atmService: AtmService

    @Captor
    private lateinit var customerCaptor: ArgumentCaptor<Customer>

    @Test
    fun `withdraw with sufficient funds should complete successfully`() {
        // Arrange
        val customer = Customer(id = "1", name = "Alice", balance = BigDecimal(100))
        whenever(customerRepository.findByName("Alice")).thenReturn(Mono.just(customer))
        whenever(customerRepository.save(any<Customer>())).thenAnswer { Mono.just(it.arguments[0] as Customer) }

        // Act
        val result = atmService.withdraw("Alice", BigDecimal(70))

        // Assert
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(BigDecimal(30), response.balance)
            }
            .verifyComplete()

        verify(customerRepository).save(customerCaptor.capture())
        assertEquals(BigDecimal(30), customerCaptor.value.balance)
    }

    @Test
    fun `withdraw with insufficient funds should return error`() {
        // Arrange
        val customer = Customer(id = "1", name = "Alice", balance = BigDecimal(50))
        whenever(customerRepository.findByName("Alice")).thenReturn(Mono.just(customer))

        // Act
        val result = atmService.withdraw("Alice", BigDecimal(100))

        // Assert
        StepVerifier.create(result)
            .expectErrorMessage("Insufficient funds")
            .verify()

        verify(customerRepository, never()).save(any())
    }

    @Test
    fun `withdraw when user not found should return error`() {
        // Arrange
        whenever(customerRepository.findByName("Unknown")).thenReturn(Mono.empty())

        // Act
        val result = atmService.withdraw("Unknown", BigDecimal(50))

        // Assert
        StepVerifier.create(result)
            .expectErrorMessage("User not found: Unknown")
            .verify()
    }
}