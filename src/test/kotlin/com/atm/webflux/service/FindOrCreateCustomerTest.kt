package com.atm.webflux.service

import com.atm.webflux.model.Customer
import com.atm.webflux.repository.CustomerRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class FindOrCreateCustomerTest {

    @Mock
    private lateinit var customerRepository: CustomerRepository

    @InjectMocks
    private lateinit var atmService: AtmService

    @Test
    fun `findOrCreateCustomer when customer exists should return existing customer`() {
        // Arrange
        val customerName = "Alice"
        val existingCustomer = Customer(id = "1", name = customerName)
        whenever(customerRepository.findByName(customerName)).thenReturn(Mono.just(existingCustomer))

        // Act
        val result = atmService.findOrCreateCustomer(customerName)

        // Assert
        StepVerifier.create(result)
            .expectNext(existingCustomer)
            .verifyComplete()

        verify(customerRepository).findByName(customerName)
        verify(customerRepository, never()).save(any())
    }

    @Test
    fun `findOrCreateCustomer when customer does not exist should create and return new customer`() {
        // Arrange
        val customerName = "Bob"
        val newCustomer = Customer(id = "2", name = customerName)
        whenever(customerRepository.findByName(customerName)).thenReturn(Mono.empty())
        whenever(customerRepository.save(any<Customer>())).thenReturn(Mono.just(newCustomer))

        // Act
        val result = atmService.findOrCreateCustomer(customerName)

        // Assert
        StepVerifier.create(result)
            .expectNext(newCustomer)
            .verifyComplete()

        verify(customerRepository).findByName(customerName)
        verify(customerRepository).save(any<Customer>())
    }
}