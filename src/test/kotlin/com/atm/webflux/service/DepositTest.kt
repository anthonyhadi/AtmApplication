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
class DepositTest {

    @Mock
    private lateinit var customerRepository: CustomerRepository

    @InjectMocks
    private lateinit var atmService: AtmService

    @Captor
    private lateinit var customerCaptor: ArgumentCaptor<Customer>

    @Test
    fun `deposit with no debts should update balance correctly`() {
        // Arrange
        val alice = Customer(id = "1", name = "Alice", balance = BigDecimal(100))
        whenever(customerRepository.findByName("Alice")).thenReturn(Mono.just(alice))
        whenever(customerRepository.save(any<Customer>())).thenAnswer { Mono.just(it.arguments[0] as Customer) }

        // Act
        val result = atmService.deposit("Alice", BigDecimal(50))

        // Assert
        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(BigDecimal(150), response.balance)
                assertEquals("Your balance is $150", response.message)
            }
            .verifyComplete()

        verify(customerRepository).save(customerCaptor.capture())
        assertEquals(BigDecimal(150), customerCaptor.value.balance)
    }

    @Test
    fun `deposit with sufficient amount should settle debt completely`() {
        // Arrange
        val alice = Customer(
            id = "1", name = "Alice", balance = BigDecimal(50),
            debts = mutableMapOf("Bob" to BigDecimal(-70))
        )
        val bob = Customer(
            id = "2", name = "Bob", balance = BigDecimal(100),
            debts = mutableMapOf("Alice" to BigDecimal(70))
        )

        whenever(customerRepository.findByName("Alice")).thenReturn(Mono.just(alice))
        whenever(customerRepository.findByName("Bob")).thenReturn(Mono.just(bob))
        whenever(customerRepository.save(any<Customer>())).thenAnswer { Mono.just(it.arguments[0] as Customer) }

        // Act
        val result = atmService.deposit("Alice", BigDecimal(100))

        // Assert
        StepVerifier.create(result)
            .assertNext { response ->
                // Initial 50 + (100 deposit - 70 debt payment) = 80
                assertEquals(BigDecimal(80), response.balance)
                assertEquals(0, response.debtInfo.size)
            }
            .verifyComplete()

        verify(customerRepository, times(2)).save(customerCaptor.capture())
        val savedCustomers = customerCaptor.allValues
        val savedAlice = savedCustomers.first { it.name == "Alice" }
        val savedBob = savedCustomers.first { it.name == "Bob" }

        assertEquals(BigDecimal(80), savedAlice.balance)
        assertEquals(0, savedAlice.debts.size)
        assertEquals(BigDecimal(170), savedBob.balance) // 100 + 70 payment
        assertEquals(1, savedBob.debts.size)
    }

    @Test
    fun `deposit with insufficient amount should settle debt partially`() {
        // Arrange
        val alice = Customer(
            id = "1", name = "Alice", balance = BigDecimal(50),
            debts = mutableMapOf("Bob" to BigDecimal(-70))
        )
        val bob = Customer(
            id = "2", name = "Bob", balance = BigDecimal(100),
            debts = mutableMapOf("Alice" to BigDecimal(70))
        )

        whenever(customerRepository.findByName("Alice")).thenReturn(Mono.just(alice))
        whenever(customerRepository.findByName("Bob")).thenReturn(Mono.just(bob))
        whenever(customerRepository.save(any<Customer>())).thenAnswer { Mono.just(it.arguments[0] as Customer) }

        // Act
        val result = atmService.deposit("Alice", BigDecimal(30))

        // Assert
        StepVerifier.create(result)
            .assertNext { response ->
                // Initial 50 + (30 deposit - 30 debt payment) = 50
                assertEquals(BigDecimal(50), response.balance)
                assertEquals(1, response.debtInfo.size)
                assertEquals("Owed 40 to Bob", response.debtInfo[0])
            }
            .verifyComplete()

        verify(customerRepository, times(2)).save(customerCaptor.capture())
        val savedCustomers = customerCaptor.allValues
        val savedAlice = savedCustomers.first { it.name == "Alice" }
        val savedBob = savedCustomers.first { it.name == "Bob" }

        assertEquals(BigDecimal(50), savedAlice.balance)
        assertEquals(BigDecimal(-40), savedAlice.debts["Bob"])
        assertEquals(BigDecimal(130), savedBob.balance) // 100 + 30 payment
        assertEquals(BigDecimal(40), savedBob.debts["Alice"])
    }
}