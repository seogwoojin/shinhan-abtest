package com.example.finsync.mydata.dto

data class AccountInfo(
    val account_num: String,
    val account_type: String,
    val balance_amt: Long,
    val currency_code: String = "KRW",
    val account_name: String,
    val source: String? = null
)