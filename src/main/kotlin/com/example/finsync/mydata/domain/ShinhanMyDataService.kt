package com.example.finsync.mydata.domain

import com.example.finsync.mydata.dto.AccountInfo
import com.example.finsync.mydata.dto.TransactionInfo
import com.example.finsync.mydata.external.KBBankClient
import com.example.finsync.mydata.external.ShinhanBankClient
import com.example.finsync.mydata.external.ShinhanCardClient
import com.example.finsync.mydata.external.ShinhanInvestClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

// 7. 토스 통합 마이데이터 서비스
@Service
class ShinhanMyDataService(
    private val shinhanCardClient: ShinhanCardClient,
    private val shinhanBankClient: ShinhanBankClient,
    private val kbBankClient: KBBankClient,
    private val shinhanInvestClient: ShinhanInvestClient
) {

    // 모든 금융기관의 거래내역을 통합 조회 (병렬 처리)
    fun getAllTransactions(
        username: String,
    ): Mono<Map<String, List<TransactionInfo>>> {
        val cardTransactions = shinhanCardClient.getTransactions("2025-01-01", "2025-12-31")
        val shinhanBankTransactions =
            shinhanBankClient.getTransactions("110-***-******", "2025-01-01", "2025-12-31")
        val kbBankTransactions =
            kbBankClient.getTransactions("123456-**-******", "2025-01-01", "2025-12-31")
        val investTransactions = shinhanInvestClient.getTransactions("2025-01-01", "2025-12-31")

        return Mono.zip(
            cardTransactions,
            shinhanBankTransactions,
            kbBankTransactions,
            investTransactions
        )
            .map { tuple ->
                mapOf(
                    "shinhan_card" to tuple.t1,
                    "shinhan_bank" to tuple.t2,
                    "kb_bank" to tuple.t3,
                    "shinhan_invest" to tuple.t4
                )
            }
    }

    // 모든 계좌 정보 통합 조회
    fun getAllAccounts(): Mono<Map<String, List<AccountInfo>>> {
        val cardAccounts = shinhanCardClient.getAccounts()
        val shinhanBankAccounts = shinhanBankClient.getAccounts()
        val kbBankAccounts = kbBankClient.getAccounts()

        return Mono.zip(cardAccounts, shinhanBankAccounts, kbBankAccounts)
            .map { tuple ->
                mapOf(
                    "shinhan_card" to tuple.t1,
                    "shinhan_bank" to tuple.t2,
                    "kb_bank" to tuple.t3
                )
            }
    }

    // 전체 자산 요약 정보
    fun getAssetSummary(): Mono<Map<String, Any>> {
        return Mono.zip(getAllAccounts(), shinhanInvestClient.getPortfolio())
            .map { tuple ->
                val accounts = tuple.t1
                val portfolio = tuple.t2

                val totalBankBalance = accounts.values.flatten()
                    .filter { it.account_type != "신용카드" }
                    .sumOf { it.balance_amt }

                val totalCreditUsed = accounts.values.flatten()
                    .filter { it.account_type == "신용카드" }
                    .sumOf { kotlin.math.abs(it.balance_amt) }

                val totalInvestmentValue = portfolio.sumOf { it.eval_amt }

                mapOf(
                    "total_bank_balance" to totalBankBalance,
                    "total_credit_used" to totalCreditUsed,
                    "total_investment_value" to totalInvestmentValue,
                    "total_assets" to (totalBankBalance + totalInvestmentValue - totalCreditUsed),
                    "last_updated" to System.currentTimeMillis()
                )
            }
    }
}
