package com.example.finsync.mydata.dto

data class UserTransactionRequest(
    val username: String,
    val tranDtime: String,
    val tranAmt: Long,
    val currencyCode: String,
    val merchantName: String?,
    val tranType: String,
    val balanceAmt: Long?,
    val categoryCode: String?,
    val source: String?
)