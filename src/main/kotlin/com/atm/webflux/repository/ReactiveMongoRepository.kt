package com.atm.webflux.repository

import com.atm.webflux.model.Customer
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface CustomerRepository : ReactiveMongoRepository<Customer, String> {
    fun findByName(name: String): Mono<Customer>
}