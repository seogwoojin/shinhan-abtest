package com.example.finsync.regularpayment.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RegularPaymentRepository : JpaRepository<RegularPayment, Long> {
    fun findAllByUsername(username: String): List<RegularPayment>
}