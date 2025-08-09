package com.example.finsync.mockapi.dto

data class InvestmentInfo(
    val stock_code: String,
    val stock_name: String,
    val holding_qty: Int,
    val avg_buy_price: Long,
    val current_price: Long,
    val eval_amt: Long,
    val profit_loss_amt: Long
)