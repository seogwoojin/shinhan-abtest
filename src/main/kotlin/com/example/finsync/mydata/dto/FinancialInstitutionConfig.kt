package com.example.finsync.mydata.dto

import org.springframework.stereotype.Component

@Component
class FinancialInstitutionConfig {
    val institutions = listOf(
        FinancialInstitution("신한카드", "https://api.shinhancard.co.kr", "card"),
        FinancialInstitution("신한은행", "https://api.shinhan.com", "bank"),
        FinancialInstitution("국민은행", "https://api.kbstar.com", "bank"),
        FinancialInstitution("신한투자증권", "https://api.shinhansec.com", "invest")
    )
}