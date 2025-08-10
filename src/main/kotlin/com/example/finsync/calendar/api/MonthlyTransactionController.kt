package com.example.finsync.calendar.api

import com.example.finsync.mydata.domain.MonthlyTransactionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/shinhan/api/monthly")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class MonthlyTransactionController(
    private val monthlyTransactionService: MonthlyTransactionService
) {

    /**
     * 월별 거래 내역 조회
     * GET /shinhan/api/monthly/transactions/{username}?year=2024&month=8
     */
    @GetMapping("/transactions/{username}")
    fun getMonthlyTransactions(
        @PathVariable username: String,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return monthlyTransactionService.getMonthlyTransactions(username, year, month)
            .map { transactions ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "year" to year,
                        "month" to month,
                        "data" to transactions,
                        "total_days" to transactions.size
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    mapOf(
                        "result" to "error",
                        "message" to "거래 내역 조회 중 오류가 발생했습니다."
                    )
                )
            )
    }

    /**
     * 특정 날짜 거래 내역 조회
     * GET /shinhan/api/monthly/daily/{username}?date=2024-08-09
     */
    @GetMapping("/daily/{username}")
    fun getDailyTransactions(
        @PathVariable username: String,
        @RequestParam date: String // YYYY-MM-DD 형식
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return monthlyTransactionService.getDailyTransactions(username, date)
            .map { transactions ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "date" to date,
                        "data" to transactions,
                        "count" to transactions.size
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    mapOf(
                        "result" to "error",
                        "message" to "일별 거래 내역 조회 중 오류가 발생했습니다."
                    )
                )
            )
    }

    /**
     * 월별 요약 정보 조회
     * GET /shinhan/api/monthly/summary/{username}?year=2024&month=8
     */
    @GetMapping("/summary/{username}")
    fun getMonthlySummary(
        @PathVariable username: String,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return monthlyTransactionService.getMonthlySummary(username, year, month)
            .map { summary ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to summary
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    mapOf(
                        "result" to "error",
                        "message" to "월별 요약 정보 조회 중 오류가 발생했습니다."
                    )
                )
            )
    }

    /**
     * 여러 달의 요약 정보 조회 (차트용)
     * GET /shinhan/api/monthly/multi-summary/{username}?startYear=2024&startMonth=1&endYear=2024&endMonth=12
     */
    @GetMapping("/multi-summary/{username}")
    fun getMultiMonthlySummary(
        @PathVariable username: String,
        @RequestParam startYear: Int,
        @RequestParam startMonth: Int,
        @RequestParam endYear: Int,
        @RequestParam endMonth: Int
    ): Mono<ResponseEntity<Map<String, Any>>> {
        val summaries = mutableListOf<Mono<Map<String, Any>>>()

        var currentYear = startYear
        var currentMonth = startMonth

        while (currentYear < endYear || (currentYear == endYear && currentMonth <= endMonth)) {
            summaries.add(
                monthlyTransactionService.getMonthlySummary(
                    username,
                    currentYear,
                    currentMonth
                )
            )

            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
        }

        return Mono.zip(summaries) { results ->
            ResponseEntity.ok(
                mapOf(
                    "result" to "success",
                    "data" to results.toList(),
                    "period" to "$startYear-$startMonth ~ $endYear-$endMonth"
                )
            )
        }.onErrorReturn(
            ResponseEntity.badRequest().body(
                mapOf(
                    "result" to "error",
                    "message" to "복수 월별 요약 정보 조회 중 오류가 발생했습니다."
                )
            )
        )
    }
}