package com.example.finsync.regularpayment.repository

import jakarta.persistence.*

@Entity
@Table(name = "regular_payments")
data class RegularPayment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val username: String,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @JoinColumn(name = "regular_payment_id")
    val payments: List<Payment>
)
