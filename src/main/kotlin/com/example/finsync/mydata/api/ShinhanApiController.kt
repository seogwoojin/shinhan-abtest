package com.example.finsync.mydata.api

import com.example.finsync.mydata.domain.ShinhanMyDataService
import com.example.finsync.mydata.domain.TransactionSaveService
import com.example.finsync.mydata.repository.TransactionJpaRepository
import com.example.finsync.mydata.repository.UserTransactionInfo
import com.example.finsync.mydata.dto.UserTransactionRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/shinhan/api")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class ShinhanApiController(
    private val shinhanMyDataService: ShinhanMyDataService,
    private val transactionSaveService: TransactionSaveService,
    private val transactionJpaRepository: TransactionJpaRepository
) {

    @GetMapping("/transactions/{username}")
    fun getTransactions(
        @PathVariable username: String,
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return Mono.fromCallable {
            transactionJpaRepository.findByUsernameOrderByTranDtimeDesc(username)
        }
            .flatMap { userInfo ->
                if (userInfo.isNotEmpty()) {
                    Mono.just(
                        ResponseEntity.ok(
                            mapOf(
                                "result" to "success",
                                "data" to userInfo,
                                "total_count" to userInfo.size
                            )
                        )
                    )
                } else {
                    shinhanMyDataService.getAllTransactions(username)
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
            }
    }


    // 통합 계좌 조회
    @GetMapping("/accounts")
    fun getAccounts(): Mono<ResponseEntity<Map<String, Any>>> {
        return shinhanMyDataService.getAllAccounts()
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
        return shinhanMyDataService.getAssetSummary()
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
        return shinhanMyDataService.getAllTransactions(username)
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

    @PostMapping("/transactions")
    fun addTransaction(@RequestBody transactionRequest: UserTransactionRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val userTransaction = UserTransactionInfo(
                username = transactionRequest.username,
                tranDtime = transactionRequest.tranDtime,
                tranAmt = transactionRequest.tranAmt,
                currencyCode = transactionRequest.currencyCode,
                merchantName = transactionRequest.merchantName,
                tranType = transactionRequest.tranType,
                balanceAmt = transactionRequest.balanceAmt,
                categoryCode = transactionRequest.categoryCode,
                source = transactionRequest.source
            )
            
            val savedTransaction = transactionJpaRepository.save(userTransaction)
            
            ResponseEntity.ok(
                mapOf(
                    "result" to "success",
                    "message" to "거래 내역이 성공적으로 추가되었습니다.",
                    "data" to savedTransaction
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "result" to "error",
                    "message" to "거래 내역 추가 중 오류가 발생했습니다: ${e.message}"
                )
            )
        }
    }
}
