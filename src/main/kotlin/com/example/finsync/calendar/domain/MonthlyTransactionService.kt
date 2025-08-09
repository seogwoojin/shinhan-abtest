package com.example.finsync.mydata.domain

import com.example.finsync.mydata.dto.TransactionInfo
import com.example.finsync.mydata.repository.TransactionJpaRepository
import com.example.finsync.mydata.repository.UserTransactionInfo
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class MonthlyTransactionService(
    private val repository: TransactionJpaRepository
) {

    /**
     * 특정 사용자의 월별 거래 내역 조회 (WebFlux)
     */
    fun getMonthlyTransactions(
        username: String,
        year: Int,
        month: Int
    ): Mono<Map<String, List<TransactionInfo>>> {
        return Mono.fromCallable {
            // 해당 월의 시작일과 마지막일 계산
            val startDate = LocalDate.of(year, month, 1)
            val endDate = startDate.plusMonths(1).minusDays(1)

            // 날짜 형식을 거래 데이터의 형식에 맞게 변환 (YYYY-MM-DD)
            val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // DB에서 해당 기간의 거래 내역 조회
            val transactions =
                repository.findByUsernameAndDateRange(username, startDateStr, endDateStr)

            // 날짜별로 그룹화
            transactions.groupBy { transaction ->
                // tran_dtime에서 날짜 부분만 추출 (YYYY-MM-DDTHH:mm:ss -> YYYY-MM-DD)
                transaction.tranDtime.split("T")[0]
            }.mapValues { (_, transactions) ->
                transactions.map { entity ->
                    TransactionInfo(
                        tran_dtime = entity.tranDtime,
                        tran_amt = entity.tranAmt,
                        currency_code = entity.currencyCode,
                        merchant_name = entity.merchantName,
                        tran_type = entity.tranType,
                        balance_amt = entity.balanceAmt,
                        category_code = entity.categoryCode,
                        source = entity.source
                    )
                }
            }
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * 특정 날짜의 거래 내역 조회 (WebFlux)
     */
    fun getDailyTransactions(
        username: String,
        date: String // YYYY-MM-DD 형식
    ): Mono<List<TransactionInfo>> {
        return Mono.fromCallable {
            val transactions = repository.findByUsernameAndDate(username, date)
            transactions.map { entity ->
                TransactionInfo(
                    tran_dtime = entity.tranDtime,
                    tran_amt = entity.tranAmt,
                    currency_code = entity.currencyCode,
                    merchant_name = entity.merchantName,
                    tran_type = entity.tranType,
                    balance_amt = entity.balanceAmt,
                    category_code = entity.categoryCode,
                    source = entity.source
                )
            }
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * 월별 요약 정보 (총 수입, 총 지출, 순수익) (WebFlux)
     */
    fun getMonthlySummary(
        username: String,
        year: Int,
        month: Int
    ): Mono<Map<String, Any>> {
        return getMonthlyTransactions(username, year, month)
            .map { dailyTransactions ->
                val allTransactions = dailyTransactions.values.flatten()

                val totalIncome = allTransactions
                    .filter { it.tran_amt > 0 }
                    .sumOf { it.tran_amt }

                val totalExpense = allTransactions
                    .filter { it.tran_amt < 0 }
                    .sumOf { kotlin.math.abs(it.tran_amt) }

                val netAmount = totalIncome - totalExpense

                // 카테고리별 지출 분석
                val categoryExpenses = allTransactions
                    .filter { it.tran_amt < 0 && it.category_code != null }
                    .groupBy { it.category_code!! }
                    .mapValues { (_, transactions) ->
                        transactions.sumOf { kotlin.math.abs(it.tran_amt) }
                    }

                // 기관별 거래 분석
                val sourceAnalysis = allTransactions
                    .groupBy { it.source ?: "알 수 없음" }
                    .mapValues { (_, transactions) ->
                        mapOf(
                            "count" to transactions.size,
                            "total_amount" to transactions.sumOf { it.tran_amt },
                            "income" to transactions.filter { it.tran_amt > 0 }
                                .sumOf { it.tran_amt },
                            "expense" to transactions.filter { it.tran_amt < 0 }
                                .sumOf { kotlin.math.abs(it.tran_amt) }
                        )
                    }

                mapOf(
                    "year" to year,
                    "month" to month,
                    "total_income" to totalIncome,
                    "total_expense" to totalExpense,
                    "net_amount" to netAmount,
                    "transaction_count" to allTransactions.size,
                    "category_expenses" to categoryExpenses,
                    "source_analysis" to sourceAnalysis,
                    "daily_count" to dailyTransactions.size,
                    "avg_daily_transactions" to if (dailyTransactions.isNotEmpty())
                        allTransactions.size.toDouble() / dailyTransactions.size else 0.0
                )
            }
    }

    /**
     * 여러 달의 요약 정보 조회 (차트용) (WebFlux)
     */
    fun getMultiMonthlySummary(
        username: String,
        startYear: Int,
        startMonth: Int,
        endYear: Int,
        endMonth: Int
    ): Mono<List<Map<String, Any>>> {
        val summaryMonos = mutableListOf<Mono<Map<String, Any>>>()

        var currentYear = startYear
        var currentMonth = startMonth

        while (currentYear < endYear || (currentYear == endYear && currentMonth <= endMonth)) {
            summaryMonos.add(getMonthlySummary(username, currentYear, currentMonth))

            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
        }

        return if (summaryMonos.isNotEmpty()) {
            Mono.zip(summaryMonos) { results ->
                results.map { it as Map<String, Any> }
            }
        } else {
            Mono.just(emptyList())
        }
    }

    /**
     * 월별 거래 통계 조회 (WebFlux)
     */
    fun getMonthlyStatistics(
        username: String,
        year: Int,
        month: Int
    ): Mono<Map<String, Any>> {
        return getMonthlyTransactions(username, year, month)
            .map { dailyTransactions ->
                val allTransactions = dailyTransactions.values.flatten()

                // 일별 거래 금액 통계
                val dailyAmounts = dailyTransactions.mapValues { (_, transactions) ->
                    transactions.sumOf { it.tran_amt }
                }

                // 거래 유형별 통계
                val typeStats = allTransactions
                    .groupBy { it.tran_type }
                    .mapValues { (_, transactions) ->
                        mapOf(
                            "count" to transactions.size,
                            "total_amount" to transactions.sumOf { it.tran_amt },
                            "avg_amount" to if (transactions.isNotEmpty())
                                transactions.map { it.tran_amt }.average() else 0.0
                        )
                    }

                // 가맹점별 통계 (상위 10개)
                val merchantStats = allTransactions
                    .filter { it.merchant_name != null }
                    .groupBy { it.merchant_name!! }
                    .mapValues { (_, transactions) ->
                        mapOf(
                            "count" to transactions.size,
                            "total_amount" to transactions.sumOf { kotlin.math.abs(it.tran_amt) }
                        )
                    }
                    .toList()
                    .sortedByDescending { (_, stats) -> (stats["total_amount"] as Long) }
                    .take(10)
                    .toMap()

                // 일별 거래 패턴
                val dailyPattern = dailyTransactions.mapValues { (_, transactions) ->
                    mapOf(
                        "transaction_count" to transactions.size,
                        "total_amount" to transactions.sumOf { it.tran_amt },
                        "income_count" to transactions.count { it.tran_amt > 0 },
                        "expense_count" to transactions.count { it.tran_amt < 0 }
                    )
                }

                mapOf(
                    "year" to year,
                    "month" to month,
                    "daily_amounts" to dailyAmounts,
                    "type_statistics" to typeStats,
                    "top_merchants" to merchantStats,
                    "daily_pattern" to dailyPattern,
                    "analysis_date" to System.currentTimeMillis()
                )
            }
    }

    /**
     * 사용자의 거래 패턴 분석 (WebFlux)
     */
    fun getUserTransactionPattern(
        username: String,
        months: Int = 6 // 최근 몇 개월
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val now = LocalDate.now()
            val startDate = now.minusMonths(months.toLong()).withDayOfMonth(1)
            val endDate = now.withDayOfMonth(now.lengthOfMonth())

            val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            repository.findByUsernameAndDateRange(username, startDateStr, endDateStr)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .map { transactions ->
                // 월별 패턴 분석
                val monthlyPattern = transactions
                    .groupBy { it.getTransactionMonth() }
                    .mapValues { (_, monthTransactions) ->
                        mapOf(
                            "total_transactions" to monthTransactions.size,
                            "total_income" to monthTransactions.filter { it.tranAmt > 0 }
                                .sumOf { it.tranAmt },
                            "total_expense" to monthTransactions.filter { it.tranAmt < 0 }
                                .sumOf { kotlin.math.abs(it.tranAmt) },
                            "avg_transaction_amount" to if (monthTransactions.isNotEmpty())
                                monthTransactions.map { kotlin.math.abs(it.tranAmt) }
                                    .average() else 0.0
                        )
                    }

                // 요일별 패턴 분석
                val dayOfWeekPattern = transactions
                    .groupBy {
                        try {
                            LocalDate.parse(it.getTransactionDate()).dayOfWeek.value
                        } catch (e: Exception) {
                            0
                        }
                    }
                    .mapValues { (_, dayTransactions) ->
                        mapOf(
                            "transaction_count" to dayTransactions.size,
                            "avg_amount" to if (dayTransactions.isNotEmpty())
                                dayTransactions.map { kotlin.math.abs(it.tranAmt) }
                                    .average() else 0.0
                        )
                    }

                // 자주 사용하는 가맹점/카테고리
                val frequentMerchants = transactions
                    .filter { it.merchantName != null }
                    .groupBy { it.merchantName!! }
                    .mapValues { (_, merchantTransactions) ->
                        mapOf(
                            "frequency" to merchantTransactions.size,
                            "total_amount" to merchantTransactions.sumOf { kotlin.math.abs(it.tranAmt) }
                        )
                    }
                    .toList()
                    .sortedByDescending { (_, stats) -> stats["frequency"] as Int }
                    .take(10)
                    .toMap()

                mapOf(
                    "analysis_period_months" to months,
                    "total_transactions" to transactions.size,
                    "monthly_pattern" to monthlyPattern,
                    "day_of_week_pattern" to dayOfWeekPattern,
                    "frequent_merchants" to frequentMerchants,
                    "analysis_date" to System.currentTimeMillis()
                )
            }
    }
}