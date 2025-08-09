package com.example.finsync.mydata.dto

data class TransactionInfo(
    val tran_dtime: String,        // 거래일시
    val tran_amt: Long,            // 거래금액
    val currency_code: String = "KRW",
    val merchant_name: String?,    // 가맹점명
    val tran_type: String,         // 거래유형 (출금/입금/결제)
    val balance_amt: Long? = null, // 잔액
    val category_code: String? = null, // 카테고리 코드
    val source: String? = null     // 데이터 출처 (신한카드, 신한은행 등)
)