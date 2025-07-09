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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class TransferTest {

    @Mock
    private lateinit var customerRepository: CustomerRepository

    @InjectMocks
    private lateinit var atmService: AtmService

    @Captor
    private lateinit var customerCaptor: ArgumentCaptor<Customer>

    @Test
    fun `transfer with sufficient funds should complete successfully`() {
        // Arrange
        val alice = Customer(id = "1", name = "Alice", balance = BigDecimal(100))
        val bob = Customer(id = "2", name = "Bob", balance = BigDecimal(50))
        whenever(customerRepository.findByName("Alice")).thenReturn(Mono.just(alice))
        whenever(customerRepository.findByName("Bob")).thenReturn(Mono.just(bob))
        whenever(customerRepository.save(any<Customer>())).thenAnswer { Mono.just(it.arguments[0] as Customer) }

        // Act
        val result = atmService.transfer("Alice", "Bob", BigDecimal(80))

        // Assert
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals("Alice", response.name)
                assertEquals(BigDecimal(20), response.balance)
                assertEquals("Transferred 80 to Bob", response.message)
            }
            .verifyComplete()

        verify(customerRepository, times(2)).save(customerCaptor.capture())
        val savedAlice = customerCaptor.allValues.first { it.name == "Alice" }
        val savedBob = customerCaptor.allValues.first { it.name == "Bob" }
        assertEquals(BigDecimal(20), savedAlice.balance)
        assertEquals(BigDecimal(130), savedBob.balance)
    }

    @Test
    fun `transfer with insufficient funds should create debt`() {
        // Arrange
        val alice = Customer(id = "1", name = "Alice", balance = BigDecimal(30))
        val bob = Customer(id = "2", name = "Bob", balance = BigDecimal(50))
        whenever(customerRepository.findByName("Alice")).thenReturn(Mono.just(alice))
        whenever(customerRepository.findByName("Bob")).thenReturn(Mono.just(bob))
        whenever(customerRepository.save(any<Customer>())).thenAnswer { Mono.just(it.arguments[0] as Customer) }

        // Act
        val result = atmService.transfer("Alice", "Bob", BigDecimal(100))

        // Assert
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(BigDecimal.ZERO, response.balance)
                assertEquals("Transferred 30 to Bob", response.message)
                assertEquals(1, response.debtInfo.size)
                assertEquals("Owed 70 to Bob", response.debtInfo[0])
            }
            .verifyComplete()

        verify(customerRepository, times(2)).save(customerCaptor.capture())
        val savedAlice = customerCaptor.allValues.first { it.name == "Alice" }
        val savedBob = customerCaptor.allValues.first { it.name == "Bob" }
        assertEquals(BigDecimal.ZERO, savedAlice.balance)
        assertEquals(BigDecimal(-70), savedAlice.debts["Bob"])
        assertEquals(BigDecimal(80), savedBob.balance)
        assertEquals(BigDecimal(70), savedBob.debts["Alice"])
    }

    @Test
    fun `transfer to self should return error`() {
        // Act
        val result = atmService.transfer("Alice", "Alice", BigDecimal(50))

        // Assert
        StepVerifier.create(result)
            .expectErrorMessage("Cannot transfer to yourself.")
            .verify()
    }
}