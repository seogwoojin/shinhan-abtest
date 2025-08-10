package com.example.finsync.mydata.domain

import com.example.finsync.mydata.dto.TransactionInfo
import com.example.finsync.mydata.repository.TransactionJpaRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
class RecurringPaymentService(
    private val repository: TransactionJpaRepository
) {

    /**
     * 정기적 지출 패턴 분석 (최근 3개월)
     */
    fun getRecurringPayments(username: String): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            try {
                val now = LocalDate.now()
                val threeMonthsAgo = now.minusMonths(3)

                val startDate = threeMonthsAgo.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val endDate = now.format(DateTimeFormatter.ISO_LOCAL_DATE)

                println("분석 기간: $startDate ~ $endDate")

                // 최근 3개월 거래 내역 조회 (지출만)
                val transactions =
                    repository.findByUsernameAndDateRange(username, startDate, endDate)
                        .filter { it.tranAmt < 0 } // 지출만 필터링
                        .map { entity ->
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

                println("총 지출 거래 수: ${transactions.size}")

                analyzeRecurringPatterns(transactions, threeMonthsAgo, now)
            } catch (e: Exception) {
                println("getRecurringPayments 오류: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * 정기 결제 예측 (다음 달 예상 지출)
     */
    fun predictNextMonthRecurringPayments(username: String): Mono<Map<String, Any>> {
        return getRecurringPayments(username)
            .map { analysis ->
                val recurringPayments = analysis["recurring_payments"] as List<Map<String, Any>>

                val nextMonthPredictions = recurringPayments.map { payment ->
                    val avgAmount = payment["average_amount"] as Double
                    val frequency = payment["frequency"] as Int
                    val merchantName = payment["merchant_name"] as String
                    val confidence = payment["confidence_score"] as Double

                    // 다음 달 예상 지출일 계산
                    val lastTransactionDates = payment["transaction_dates"] as List<String>
                    val predictedDate = calculateNextPaymentDate(lastTransactionDates)

                    mapOf(
                        "merchant_name" to merchantName,
                        "predicted_amount" to avgAmount.toLong(),
                        "predicted_date" to predictedDate,
                        "confidence_score" to confidence,
                        "frequency_per_month" to frequency / 3.0 // 3개월 평균
                    )
                }.sortedByDescending { it["predicted_amount"] as Long }

                val totalPredictedAmount = nextMonthPredictions.sumOf {
                    (it["predicted_amount"] as Long) * (it["frequency_per_month"] as Double).toInt()
                }

                mapOf(
                    "next_month" to LocalDate.now().plusMonths(1)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    "predicted_payments" to nextMonthPredictions,
                    "total_predicted_amount" to totalPredictedAmount,
                    "prediction_confidence" to calculateOverallConfidence(nextMonthPredictions),
                    "generated_at" to System.currentTimeMillis()
                )
            }
    }

    /**
     * 카테고리별 정기 지출 분석
     */
    fun getRecurringPaymentsByCategory(username: String): Mono<Map<String, Any>> {
        return getRecurringPayments(username)
            .map { analysis ->
                val recurringPayments = analysis["recurring_payments"] as List<Map<String, Any>>

                val categoryAnalysis = recurringPayments
                    .filter { it["category_code"] != null }
                    .groupBy { it["category_code"] as String }
                    .mapValues { (category, payments) ->
                        val totalAmount = payments.sumOf { (it["total_amount"] as Double).toLong() }
                        val avgConfidence =
                            payments.map { it["confidence_score"] as Double }.average()
                        val merchantCount = payments.size

                        mapOf(
                            "category" to category,
                            "merchant_count" to merchantCount,
                            "total_monthly_average" to totalAmount / 3, // 3개월 평균
                            "confidence_score" to avgConfidence,
                            "merchants" to payments.map {
                                mapOf(
                                    "name" to it["merchant_name"],
                                    "amount" to it["average_amount"]
                                )
                            }
                        )
                    }

                val uncategorizedPayments = recurringPayments
                    .filter { it["category_code"] == null }
                    .map { payment ->
                        mapOf(
                            "merchant_name" to payment["merchant_name"],
                            "average_amount" to payment["average_amount"],
                            "frequency" to payment["frequency"]
                        )
                    }

                mapOf(
                    "category_analysis" to categoryAnalysis,
                    "uncategorized_payments" to uncategorizedPayments,
                    "total_categories" to categoryAnalysis.size,
                    "analysis_period" to "최근 3개월"
                )
            }
    }

    /**
     * 정기 지출 임계값 알림 설정
     */
    fun getPaymentAnomalies(
        username: String,
        thresholdPercentage: Double = 20.0
    ): Mono<Map<String, Any>> {
        return Mono.fromCallable {
            val now = LocalDate.now()
            val currentMonthStart = now.withDayOfMonth(1)
            val currentMonthEnd = now

            val currentMonthStartStr = currentMonthStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val currentMonthEndStr = currentMonthEnd.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // 이번 달 거래 내역
            val currentMonthTransactions = repository.findByUsernameAndDateRange(
                username,
                currentMonthStartStr,
                currentMonthEndStr
            ).filter { it.tranAmt < 0 }

            currentMonthTransactions
        }.subscribeOn(Schedulers.boundedElastic())
            .flatMap { currentMonthTransactions ->
                getRecurringPayments(username).map { recurringAnalysis ->
                    val recurringPayments =
                        recurringAnalysis["recurring_payments"] as List<Map<String, Any>>

                    val anomalies = currentMonthTransactions.mapNotNull { transaction ->
                        val merchantName = transaction.merchantName ?: return@mapNotNull null
                        val currentAmount = abs(transaction.tranAmt)

                        // 정기 지출 패턴에서 해당 가맹점 찾기
                        val expectedPayment = recurringPayments.find {
                            it["merchant_name"] == merchantName
                        }

                        expectedPayment?.let { payment ->
                            val expectedAmount = (payment["average_amount"] as Double).toLong()
                            val difference = currentAmount - expectedAmount
                            val percentageDiff = (difference.toDouble() / expectedAmount) * 100

                            if (abs(percentageDiff) > thresholdPercentage) {
                                mapOf(
                                    "merchant_name" to merchantName,
                                    "transaction_date" to transaction.getTransactionDate(),
                                    "current_amount" to currentAmount,
                                    "expected_amount" to expectedAmount,
                                    "difference" to difference,
                                    "percentage_difference" to percentageDiff,
                                    "anomaly_type" to if (difference > 0) "과다지출" else "절약",
                                    "confidence_score" to payment["confidence_score"]
                                )
                            } else null
                        }
                    }.sortedByDescending { abs(it["percentage_difference"] as Double) }

                    mapOf(
                        "current_month" to LocalDate.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        "threshold_percentage" to thresholdPercentage,
                        "anomalies" to anomalies,
                        "anomaly_count" to anomalies.size,
                        "total_anomaly_amount" to anomalies.sumOf { it["difference"] as Long },
                        "analyzed_at" to System.currentTimeMillis()
                    )
                }
            }
    }

    /**
     * 정기 지출 패턴 분석 로직
     */
    private fun analyzeRecurringPatterns(
        transactions: List<TransactionInfo>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<String, Any> {
        try {
            // 가맹점별로 그룹화
            val merchantGroups = transactions
                .filter { it.merchant_name != null }
                .groupBy { it.merchant_name!! }

            println("분석 대상 가맹점 수: ${merchantGroups.size}")

            val recurringPayments =
                merchantGroups.mapNotNull { (merchantName, merchantTransactions) ->
                    try {
                        if (merchantTransactions.size < 2) return@mapNotNull null // 최소 2회 이상

                        val amounts = merchantTransactions.map { abs(it.tran_amt) }
                        val avgAmount = amounts.average()
                        val amountVariance = calculateVariance(amounts.map { it.toDouble() })

                        // 정기성 판단을 위한 날짜 분석
                        val transactionDates = merchantTransactions.map {
                            it.tran_dtime.split("T")[0]
                        }.sorted().distinct() // 중복 날짜 제거

                        println("거래 날짜들: $transactionDates")

                        val dayIntervals = calculateDayIntervals(transactionDates)
                        val avgInterval =
                            if (dayIntervals.isNotEmpty()) dayIntervals.average() else 0.0

                        // 신뢰도 점수 계산 (0-1)
                        val intervalConsistency = calculateIntervalConsistency(dayIntervals)
                        val confidenceScore = calculateConfidenceScore(
                            frequency = merchantTransactions.size,
                            amountVariance = amountVariance,
                            avgAmount = avgAmount,
                            intervalConsistency = intervalConsistency
                        )

                        // 디버깅 로그
                        println("Merchant: $merchantName")
                        println("  Frequency: ${merchantTransactions.size}")
                        println("  AvgAmount: $avgAmount")
                        println("  AmountVariance: $amountVariance")
                        println("  DayIntervals: $dayIntervals")
                        println("  IntervalConsistency: $intervalConsistency")
                        println("  ConfidenceScore: $confidenceScore")
                        println("---")

                        // 정기성 임계값 (신뢰도 0.6 이상)
                        if (confidenceScore >= 0.6) {
                            mapOf(
                                "merchant_name" to merchantName,
                                "frequency" to merchantTransactions.size,
                                "average_amount" to avgAmount,
                                "amount_variance" to amountVariance,
                                "total_amount" to amounts.sum().toDouble(), // Long을 Double로 변환
                                "confidence_score" to confidenceScore,
                                "average_interval_days" to avgInterval,
                                "category_code" to merchantTransactions.first().category_code,
                                "transaction_dates" to transactionDates,
                                "source" to merchantTransactions.first().source
                            )
                        } else null
                    } catch (e: Exception) {
                        println("가맹점 '$merchantName' 분석 중 오류: ${e.message}")
                        e.printStackTrace()
                        null
                    }
                }.sortedByDescending { it["confidence_score"] as Double }

            // 카테고리별 요약
            val categoryBreakdown = try {
                recurringPayments
                    .filter { it["category_code"] != null }
                    .groupBy { it["category_code"] as String }
                    .mapValues { (_, payments) ->
                        mapOf(
                            "payment_count" to payments.size,
                            "total_amount" to payments.sumOf {
                                val totalAmount = it["total_amount"]
                                when (totalAmount) {
                                    is Double -> totalAmount.toLong()
                                    is Long -> totalAmount
                                    else -> 0L
                                }
                            },
                            "avg_confidence" to payments.map { it["confidence_score"] as Double }
                                .average()
                        )
                    }
            } catch (e: Exception) {
                println("카테고리 분석 중 오류: ${e.message}")
                emptyMap<String, Map<String, Any>>()
            }

            return mapOf(
                "analysis_period" to mapOf(
                    "start_date" to startDate.toString(),
                    "end_date" to endDate.toString(),
                    "days" to startDate.until(endDate).days
                ),
                "recurring_payments" to recurringPayments,
                "total_recurring_merchants" to recurringPayments.size,
                "total_recurring_amount" to recurringPayments.sumOf {
                    val totalAmount = it["total_amount"]
                    when (totalAmount) {
                        is Double -> totalAmount.toLong()
                        is Long -> totalAmount
                        else -> 0L
                    }
                },
                "monthly_average_recurring" to recurringPayments.sumOf {
                    val totalAmount = it["total_amount"]
                    when (totalAmount) {
                        is Double -> totalAmount.toLong()
                        is Long -> totalAmount
                        else -> 0L
                    }
                } / 3,
                "category_breakdown" to categoryBreakdown,
                "high_confidence_count" to recurringPayments.count {
                    (it["confidence_score"] as Double) >= 0.8
                }
            )
        } catch (e: Exception) {
            println("analyzeRecurringPatterns 전체 오류: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun calculateVariance(amounts: List<Double>): Double {
        if (amounts.isEmpty()) return 0.0
        if (amounts.size == 1) return 0.0 // 단일 값의 경우 분산은 0

        val mean = amounts.average()
        val variance = amounts.map { (it - mean).let { diff -> diff * diff } }.average()

        return if (variance.isFinite()) variance else 0.0
    }

    private fun calculateDayIntervals(dates: List<String>): List<Double> {
        if (dates.size < 2) return emptyList()

        return dates.zipWithNext { date1, date2 ->
            try {
                val d1 = LocalDate.parse(date1)
                val d2 = LocalDate.parse(date2)
                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(d1, d2).toDouble()
                kotlin.math.abs(daysBetween) // 절댓값으로 처리
            } catch (e: Exception) {
                println("날짜 파싱 오류: $date1 -> $date2, 오류: ${e.message}")
                0.0
            }
        }.filter { it > 0.0 } // 0일 간격은 제외
    }

    private fun calculateIntervalConsistency(intervals: List<Double>): Double {
        if (intervals.isEmpty()) return 0.0
        if (intervals.size == 1) return 0.8 // 단일 간격의 경우 높은 점수 부여

        val avgInterval = intervals.average()
        if (avgInterval <= 0) return 0.0

        val variance = intervals.map { (it - avgInterval).let { diff -> diff * diff } }.average()
        if (variance.isNaN() || variance.isInfinite()) return 0.0

        // 간격이 일정할수록 높은 점수 (월간격 기준 최적화)
        val consistencyScore = when {
            variance == 0.0 -> 1.0 // 완전히 일치
            avgInterval >= 28 && avgInterval <= 32 -> { // 월 단위 간격
                val normalizedVariance = variance / (avgInterval * avgInterval)
                1.0 / (1.0 + normalizedVariance * 10) // 월단위는 더 관대하게
            }

            else -> 1.0 / (1.0 + variance / avgInterval)
        }

        return if (consistencyScore.isFinite()) consistencyScore else 0.0
    }

    private fun calculateConfidenceScore(
        frequency: Int,
        amountVariance: Double,
        avgAmount: Double,
        intervalConsistency: Double
    ): Double {
        // 안전한 계산을 위한 검증
        if (avgAmount <= 0 || amountVariance.isNaN() || intervalConsistency.isNaN()) {
            return 0.0
        }

        // 빈도 점수 (3회 이상부터 신뢰도 상승)
        val frequencyScore = when {
            frequency >= 6 -> 1.0
            frequency >= 3 -> frequency / 6.0
            else -> 0.0
        }

        // 금액 일관성 점수 계산 (안전한 방식)
        val amountConsistencyScore = if (avgAmount > 0) {
            when {
                amountVariance == 0.0 -> 1.0 // 완전히 동일한 금액
                else -> {
                    val varianceRatio = amountVariance / (avgAmount * avgAmount)
                    1.0 / (1.0 + varianceRatio * 100) // 정규화된 분산 사용
                }
            }
        } else {
            0.0
        }

        // 간격 일관성 점수 (NaN 체크)
        val intervalScore = if (intervalConsistency.isFinite()) intervalConsistency else 0.0

        // 가중 평균 계산 (정기 결제는 간격이 중요하므로 비중 조정)
        val result = (frequencyScore * 0.3 + amountConsistencyScore * 0.3 + intervalScore * 0.4)

        // 최종 결과 검증
        return if (result.isFinite()) result else 0.0
    }

    private fun calculateNextPaymentDate(transactionDates: List<String>): String {
        if (transactionDates.size < 2) {
            return LocalDate.now().plusMonths(1).toString()
        }

        val intervals = calculateDayIntervals(transactionDates.sorted())
        val avgInterval = intervals.average()

        val lastDate = LocalDate.parse(transactionDates.maxOrNull()!!)
        return lastDate.plusDays(avgInterval.toLong()).toString()
    }

    private fun calculateOverallConfidence(predictions: List<Map<String, Any>>): Double {
        if (predictions.isEmpty()) return 0.0
        return predictions.map { it["confidence_score"] as Double }.average()
    }
}