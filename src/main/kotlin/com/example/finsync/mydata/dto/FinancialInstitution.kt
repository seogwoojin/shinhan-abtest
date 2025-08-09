package com.example.finsync.mydata.dto

// 2. 은행별 API 클라이언트 설정 정보
data class FinancialInstitution(
    val name: String,
    val baseUrl: String,
    val industry: String // bank, card, invest
)
