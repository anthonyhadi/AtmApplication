package com.atm.webflux.service

import com.atm.webflux.dto.StatusResponse
import com.atm.webflux.model.Customer
import com.atm.webflux.repository.CustomerRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal

@Service
class AtmService(private val customerRepository: CustomerRepository) {

    fun findOrCreateCustomer(name: String): Mono<Customer> =
        customerRepository.findByName(name)
            .switchIfEmpty { customerRepository.save(Customer(name = name)) }

    fun deposit(name: String, amount: BigDecimal): Mono<StatusResponse> {
        return customerRepository.findByName(name)
            .switchIfEmpty { Mono.error(RuntimeException("User not found: $name")) }
            .flatMap { customer ->
                var remainingAmount = amount
                val debtsToSettle = customer.debts.filter { it.value < BigDecimal.ZERO }

                val debtorUpdates = debtsToSettle.map { (debtorName, debtAmount) ->
                    if (remainingAmount <= BigDecimal.ZERO) return@map Mono.empty()

                    val payment = remainingAmount.min(debtAmount.abs())
                    remainingAmount -= payment

                    customer.debts.computeIfPresent(debtorName) { _, v -> v + payment }

                    customerRepository.findByName(debtorName).flatMap { debtor ->
                        debtor.balance += payment
                        debtor.debts.computeIfPresent(name) { _, v -> v - payment }
                        customerRepository.save(debtor)
                    }
                }

                customer.balance += remainingAmount
                customer.debts.entries.removeIf { it.value == BigDecimal.ZERO }

                Flux.concat(debtorUpdates)
                    .then(customerRepository.save(customer))
                    .map { StatusResponse.from(it, "Your balance is $${it.balance}") }
            }
    }


    fun withdraw(name: String, amount: BigDecimal): Mono<StatusResponse> {
        return customerRepository.findByName(name)
            .switchIfEmpty { Mono.error(RuntimeException("User not found: $name")) }
            .flatMap { customer ->
                if (customer.balance >= amount) {
                    customer.balance -= amount
                    customerRepository.save(customer)
                        .map { StatusResponse.from(it, "Your balance is $${it.balance}") }
                } else {
                    Mono.error(RuntimeException("Insufficient funds"))
                }
            }
    }

    fun transfer(sourceName: String, targetName: String, amount: BigDecimal): Mono<StatusResponse> {
        if (sourceName == targetName) {
            return Mono.error(RuntimeException("Cannot transfer to yourself."))
        }

        val sourceMono = customerRepository.findByName(sourceName)
            .switchIfEmpty { Mono.error(RuntimeException("Source user not found: $sourceName")) }
        val targetMono = findOrCreateCustomer(targetName)

        return Mono.zip(sourceMono, targetMono)
            .flatMap { tuple ->
                val source = tuple.t1
                val target = tuple.t2

                val transferAmount = source.balance.min(amount)
                source.balance -= transferAmount
                target.balance += transferAmount

                val debt = amount - transferAmount
                if (debt > BigDecimal.ZERO) {
                    source.debts.merge(targetName, debt.negate(), BigDecimal::add)
                    target.debts.merge(sourceName, debt, BigDecimal::add)
                }

                source.debts.entries.removeIf { it.value == BigDecimal.ZERO }
                target.debts.entries.removeIf { it.value == BigDecimal.ZERO }

                customerRepository.save(source)
                    .then(customerRepository.save(target))
                    .thenReturn(StatusResponse.from(source, "Transferred $transferAmount to $targetName"))
            }
    }
}