package com.example.finsync.mydata.api

import com.example.finsync.mydata.domain.TossMyDataService
import com.example.finsync.mydata.domain.TransactionSaveService
import com.example.finsync.mydata.repository.TransactionJpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

// 8. 토스 API 컨트롤러 (사용자 대면) - WebFlux 방식
@RestController
@RequestMapping("/toss/api")
class TossApiController(
    private val tossMyDataService: TossMyDataService,
    private val transactionJpaRepository: TransactionJpaRepository,
    private val transactionSaveService: TransactionSaveService,
) {

    @GetMapping("/transactions/{username}")
    fun getTransactions(
        @PathVariable username: String,
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return tossMyDataService.getAllTransactions(username)
            .flatMap { transactions ->
                transactionSaveService.saveTransactions(username, transactions)
                    .thenReturn(transactions)
            }
            .map { transactions ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to transactions,
                        "total_count" to transactions.values.sumOf { it.size }
                    )
                )
            }
    }


    // 통합 계좌 조회
    @GetMapping("/accounts")
    fun getAccounts(): Mono<ResponseEntity<Map<String, Any>>> {
        return tossMyDataService.getAllAccounts()
            .map { accounts ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to accounts
                    )
                )
            }
    }

    // 자산 요약 정보
    @GetMapping("/asset-summary")
    fun getAssetSummary(): Mono<ResponseEntity<Map<String, Any>>> {
        return tossMyDataService.getAssetSummary()
            .map { summary ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to summary
                    )
                )
            }
    }

    // 카테고리별 소비 분석
    @GetMapping("/spending-analysis/{username}")
    fun getSpendingAnalysis(
        @RequestParam from_date: String,
        @RequestParam to_date: String, @PathVariable username: String,
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return tossMyDataService.getAllTransactions(username)
            .map { transactions ->
                // 카드 거래만 필터링하여 카테고리별 분석
                val cardTransactions = transactions["shinhan_card"] ?: emptyList()
                val categorySpending = cardTransactions
                    .groupBy { it.category_code ?: "기타" }
                    .mapValues { (_, transactions) ->
                        mapOf(
                            "total_amount" to transactions.sumOf { it.tran_amt },
                            "count" to transactions.size,
                            "avg_amount" to if (transactions.isNotEmpty()) transactions.map { it.tran_amt }
                                .average().toLong() else 0L
                        )
                    }

                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to mapOf(
                            "period" to "$from_date ~ $to_date",
                            "category_analysis" to categorySpending,
                            "total_spending" to cardTransactions.sumOf { it.tran_amt }
                        )
                    ))
            }
    }
}
