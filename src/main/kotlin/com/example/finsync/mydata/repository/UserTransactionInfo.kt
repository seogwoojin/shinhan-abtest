package com.example.finsync.mydata.repository

import com.example.finsync.mydata.dto.TransactionInfo
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType

@Entity
data class UserTransactionInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val username: String,         // 사용자 식별자
    val tranDtime: String,
    val tranAmt: Long,
    val currencyCode: String,
    val merchantName: String?,
    val tranType: String,
    val balanceAmt: Long?,
    val categoryCode: String?,
    val source: String?
) {
    /**
     * 거래 월 추출 (YYYY-MM 형식)
     */
    fun getTransactionMonth(): String {
        return tranDtime.substring(0, 7) // "2024-08-15T10:30:00" -> "2024-08"
    }

    /**
     * 거래 날짜 추출 (YYYY-MM-DD 형식)
     */
    fun getTransactionDate(): String {
        return tranDtime.split("T")[0] // "2024-08-15T10:30:00" -> "2024-08-15"
    }

    companion object {
        fun from(username: String, dto: TransactionInfo): UserTransactionInfo {
            return UserTransactionInfo(
                username = username,
                tranDtime = dto.tran_dtime,
                tranAmt = dto.tran_amt,
                currencyCode = dto.currency_code,
                merchantName = dto.merchant_name,
                tranType = dto.tran_type,
                balanceAmt = dto.balance_amt,
                categoryCode = dto.category_code,
                source = dto.source
            )
        }
    }
}
