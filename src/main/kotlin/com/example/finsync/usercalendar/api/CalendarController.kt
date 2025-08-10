package com.example.finsync.usercalendar.api

import com.example.finsync.usercalendar.dto.CalendarEventRequest
import com.example.finsync.usercalendar.dto.CalendarEventResponse
import com.example.finsync.usercalendar.service.CalendarService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
@RequestMapping("/toss/api/calendar")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class CalendarController(
    private val calendarService: CalendarService
) {

    /**
     * 정기 지출 항목을 캘린더에 등록
     * POST /toss/api/calendar/recurring-payments
     */
    @PostMapping("/recurring-payments")
    fun registerRecurringPayments(
        @RequestBody request: CalendarEventRequest
    ): Mono<ResponseEntity<CalendarEventResponse>> {

        return Mono.fromFuture(
            calendarService.createRecurringPaymentEvents(request.username, request.events)
        )
            .subscribeOn(Schedulers.boundedElastic())
            .map { createdCount ->
                ResponseEntity.ok(
                    CalendarEventResponse(
                        result = "success",
                        message = "${request.events.size}개 항목의 캘린더 일정이 등록되었습니다. (총 ${createdCount}개 이벤트 생성)",
                        created_events = createdCount
                    )
                )
            }
            .onErrorReturn(
                ResponseEntity.badRequest().body(
                    CalendarEventResponse(
                        result = "error",
                        message = "캘린더 등록 중 오류가 발생했습니다.",
                        created_events = 0
                    )
                )
            )
    }

    /**
     * 캘린더 연결 상태 확인
     * GET /toss/api/calendar/health
     */
    @GetMapping("/health")
    fun checkCalendarHealth(): ResponseEntity<Map<String, Any>> {
        return try {
            // 간단한 AppleScript 실행으로 캘린더 접근 가능한지 확인
            val process = ProcessBuilder(
                "osascript",
                "-e",
                "tell application \"Calendar\" to get name of calendars"
            ).start()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                ResponseEntity.ok(
                    mapOf(
                        "status" to "healthy",
                        "message" to "캘린더 접근 가능",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            } else {
                ResponseEntity.ok(
                    mapOf(
                        "status" to "unavailable",
                        "message" to "캘린더 접근 불가",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            ResponseEntity.ok(
                mapOf(
                    "status" to "error",
                    "message" to "캘린더 상태 확인 실패: ${e.message}",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
}