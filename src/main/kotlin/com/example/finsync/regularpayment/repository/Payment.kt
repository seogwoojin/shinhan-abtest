package com.example.finsync.regularpayment.repository

import jakarta.persistence.*

@Entity
@Table(name = "payments")
data class Payment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val merchantName: String,
    val predictedAmount: Int,
    val predictedDate: String,
    val accountNumber: String,
    val isActive: Boolean
)