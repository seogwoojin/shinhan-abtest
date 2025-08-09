package com.example.finsync.calendar.api

import com.example.finsync.calendar.domain.RecurringPaymentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/toss/api/recurring")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class RecurringPaymentController(
    private val recurringPaymentService: RecurringPaymentService
) {

    /**
     * 정기적 지출 패턴 분석 (최근 3개월)
     * GET /toss/api/recurring/analysis/{username}
     */
    @GetMapping("/analysis/{username}")
    fun getRecurringPaymentAnalysis(
        @PathVariable username: String
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return recurringPaymentService.getRecurringPayments(username)
            .map { analysis ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to analysis,
                        "message" to "정기 지출 패턴 분석이 완료되었습니다."
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    mapOf(
                        "result" to "error",
                        "message" to "정기 지출 분석 중 오류가 발생했습니다."
                    )
                )
            )
    }

    /**
     * 다음 달 정기 결제 예측
     * GET /toss/api/recurring/prediction/{username}
     */
    @GetMapping("/prediction/{username}")
    fun getNextMonthPrediction(
        @PathVariable username: String
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return recurringPaymentService.predictNextMonthRecurringPayments(username)
            .map { prediction ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to prediction,
                        "message" to "다음 달 정기 결제 예측이 완료되었습니다."
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    mapOf(
                        "result" to "error",
                        "message" to "정기 결제 예측 중 오류가 발생했습니다."
                    )
                )
            )
    }

    /**
     * 카테고리별 정기 지출 분석
     * GET /toss/api/recurring/categories/{username}
     */
    @GetMapping("/categories/{username}")
    fun getRecurringPaymentsByCategory(
        @PathVariable username: String
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return recurringPaymentService.getRecurringPaymentsByCategory(username)
            .map { categoryAnalysis ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to categoryAnalysis,
                        "message" to "카테고리별 정기 지출 분석이 완료되었습니다."
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    mapOf(
                        "result" to "error",
                        "message" to "카테고리별 정기 지출 분석 중 오류가 발생했습니다."
                    )
                )
            )
    }

    /**
     * 정기 지출 이상 패턴 감지
     * GET /toss/api/recurring/anomalies/{username}?threshold=20.0
     */
    @GetMapping("/anomalies/{username}")
    fun getPaymentAnomalies(
        @PathVariable username: String,
        @RequestParam(defaultValue = "20.0") threshold: Double
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return recurringPaymentService.getPaymentAnomalies(username, threshold)
            .map { anomalies ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to anomalies,
                        "message" to "정기 지출 이상 패턴 감지가 완료되었습니다."
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    mapOf(
                        "result" to "error",
                        "message" to "이상 패턴 감지 중 오류가 발생했습니다."
                    )
                )
            )
    }

    /**
     * 정기 지출 요약 정보
     * GET /toss/api/recurring/summary/{username}
     */
    @GetMapping("/summary/{username}")
    fun getRecurringPaymentSummary(
        @PathVariable username: String
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return recurringPaymentService.getRecurringPayments(username)
            .flatMap { analysis ->
                recurringPaymentService.predictNextMonthRecurringPayments(username)
                    .map { prediction ->
                        val recurringPayments =
                            analysis["recurring_payments"] as List<Map<String, Any>>
                        val totalMonthlyAverage = analysis["monthly_average_recurring"] as Long
                        val nextMonthPredicted = prediction["total_predicted_amount"] as Long

                        val topPayments = recurringPayments
                            .sortedByDescending { (it["average_amount"] as Double) }
                            .take(5)
                            .map {
                                mapOf(
                                    "merchant" to it["merchant_name"],
                                    "amount" to it["average_amount"],
                                    "confidence" to it["confidence_score"]
                                )
                            }

                        mapOf(
                            "current_monthly_average" to totalMonthlyAverage,
                            "next_month_predicted" to nextMonthPredicted,
                            "total_recurring_merchants" to recurringPayments.size,
                            "high_confidence_count" to recurringPayments.count {
                                (it["confidence_score"] as Double) >= 0.8
                            },
                            "top_5_payments" to topPayments,
                            "savings_opportunity" to maxOf(
                                0,
                                totalMonthlyAverage - nextMonthPredicted
                            ),
                            "analysis_period" to "최근 3개월",
                            "generated_at" to System.currentTimeMillis()
                        )
                    }
            }
            .map { summary ->
                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "data" to summary,
                        "message" to "정기 지출 요약 정보가 생성되었습니다."
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    mapOf(
                        "result" to "error",
                        "message" to "정기 지출 요약 정보 생성 중 오류가 발생했습니다."
                    )
                )
            )
    }

    /**
     * 정기 지출 캘린더 데이터 (월별)
     * GET /toss/api/recurring/calendar/{username}?year=2024&month=8
     */
    @GetMapping("/calendar/{username}")
    fun getRecurringPaymentCalendar(
        @PathVariable username: String,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return recurringPaymentService.predictNextMonthRecurringPayments(username)
            .map { prediction ->
                val predictedPayments = prediction["predicted_payments"] as List<Map<String, Any>>

                // 해당 월에 예상되는 정기 결제들을 날짜별로 정리
                val calendarData = predictedPayments.mapNotNull { payment ->
                    val predictedDate = payment["predicted_date"] as String
                    val paymentDate = try {
                        java.time.LocalDate.parse(predictedDate)
                    } catch (e: Exception) {
                        return@mapNotNull null
                    }

                    if (paymentDate.year == year && paymentDate.monthValue == month) {
                        mapOf(
                            "date" to predictedDate,
                            "merchant_name" to payment["merchant_name"],
                            "predicted_amount" to payment["predicted_amount"],
                            "confidence_score" to payment["confidence_score"],
                            "type" to "predicted_recurring"
                        )
                    } else null
                }.groupBy { it["date"] }

                ResponseEntity.ok(
                    mapOf(
                        "result" to "success",
                        "year" to year,
                        "month" to month,
                        "calendar_data" to calendarData,
                        "total_predicted_days" to calendarData.size,
                        "total_predicted_amount" to predictedPayments.sumOf {
                            it["predicted_amount"] as Long
                        },
                        "message" to "정기 지출 캘린더 데이터가 생성되었습니다."
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    mapOf(
                        "result" to "error",
                        "message" to "정기 지출 캘린더 데이터 생성 중 오류가 발생했습니다."
                    )
                )
            )
    }
}