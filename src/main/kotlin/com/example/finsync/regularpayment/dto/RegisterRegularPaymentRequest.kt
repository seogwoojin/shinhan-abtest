package com.example.finsync.regularpayment.dto

data class RegisterRegularPaymentRequest(
    val username: String,
    val payments: List<PaymentDto>
)

data class PaymentDto(
    val merchant_name: String,
    val predicted_amount: Int,
    val predicted_date: String,
    val account_number: String,
    val is_active: Boolean
)