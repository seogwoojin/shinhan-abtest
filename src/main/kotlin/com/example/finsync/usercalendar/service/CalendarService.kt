package com.example.finsync.usercalendar.service

import com.example.finsync.usercalendar.dto.CalendarEventInfo
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
class CalendarService {
    /**
     * 정기 지출 항목들을 캘린더에 등록
     */
    fun createRecurringPaymentEvents(
        username: String,
        events: List<CalendarEventInfo>
    ): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            var createdCount = 0

            events.forEach { event ->
                try {
                    val originalDate = LocalDate.parse(event.predicted_date)   // 해당 날짜
                    val nextMonth = getNextMonthDate(event.predicted_date)     // 다음달 날짜

                    // 해당 날짜 일정 등록
                    val originalSummary = createEventSummary(event, originalDate)
                    addCalendarEvent(originalSummary, originalDate, event.merchant_name)

                    // 다음달 일정 등록
                    val nextMonthSummary = createEventSummary(event, nextMonth)
                    addCalendarEvent(nextMonthSummary, nextMonth, event.merchant_name)

                    createdCount += 2

                    println("✅ ${event.merchant_name} 캘린더 등록 완료: $originalDate, $nextMonth")
                } catch (e: Exception) {
                    println("❌ ${event.merchant_name} 캘린더 등록 실패: ${e.message}")
                }
            }

            createdCount
        }
    }


    /**
     * 다음달 날짜 계산
     */
    private fun getNextMonthDate(predictedDate: String): LocalDate {
        return try {
            val date = LocalDate.parse(predictedDate)
            date.plusMonths(1)
        } catch (e: Exception) {
            // 파싱 실패시 다음달 같은 날로 설정
            LocalDate.now().plusMonths(1).withDayOfMonth(15)
        }
    }

    /**
     * 다다음달 날짜 계산
     */
    private fun getNextNextMonthDate(predictedDate: String): LocalDate {
        return try {
            val date = LocalDate.parse(predictedDate)
            date.plusMonths(2)
        } catch (e: Exception) {
            LocalDate.now().plusMonths(2).withDayOfMonth(15)
        }
    }

    /**
     * 이벤트 요약 생성
     */
    private fun createEventSummary(event: CalendarEventInfo, date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("M월 d일")
        val formattedAmount = String.format("%,d", event.predicted_amount)
        val confidencePercent = (event.confidence_score * 100).toInt()

        return "${event.merchant_name} 정기결제 (${formattedAmount}원) - 신뢰도 ${confidencePercent}%"
    }

    /**
     * 캘린더 이벤트 추가 (AppleScript 사용)
     */
    private fun addCalendarEvent(summary: String, date: LocalDate, merchantName: String) {
        val appleDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a", Locale.ENGLISH)
        val eventDateString =
            date.atTime(9, 0).format(appleDateFormat)   // ex) 10/10/2025 9:00:00 AM
        val endDateString =
            date.atTime(10, 0).format(appleDateFormat)    // ex) 10/10/2025 10:00:00 AM

        val baseDateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH))
        val appleScript = """
tell application "Calendar"
    tell calendar "Work"
        set baseDate to date "$baseDateString"
        set eventDate to baseDate + 9 * hours
        set endDate to eventDate + 1 * hours
        make new event at end with properties {summary:"$summary", start date:eventDate, end date:endDate, description:"신한에서 자동 등록된 정기결제 일정입니다. 가맹점: $merchantName"}
    end tell
end tell
""".trimIndent()
//
//        val appleScript = """
//        tell application "Calendar"
//            tell calendar "Work"
//                make new event at end with properties {summary:"$summary", start date:(current date), end date:(current date) + (1 * hours)}
//            end tell
//        end tell
//    """.trimIndent()
//
//        val process =
//            ProcessBuilder("osascript", "-e", appleScript).redirectErrorStream(true).start()
//        val output = process.inputStream.bufferedReader().readText()
//        val exitCode = process.waitFor()
//
//        println("AppleScript output: $output")
//        println("Exit code: $exitCode")

        executeAppleScript(appleScript)
    }

    /**
     * 단일 캘린더 이벤트 추가 (기존 메서드)
     */
    private fun addCalendarEvent(summary: String) {
        val appleScript = """
            tell application "Calendar"
                tell calendar "Work"
                    make new event at end with properties {summary:"$summary", start date:(current date), end date:(current date) + (1 * hours)}
                end tell
            end tell
        """.trimIndent()

        executeAppleScript(appleScript)
    }

    /**
     * AppleScript 실행
     */
    private fun executeAppleScript(script: String) {
        try {
            val process =
                ProcessBuilder("osascript", "-e", script).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            println("AppleScript output: $output")
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                println("✅ AppleScript 실행 성공")
            } else {

                println("❌ AppleScript 실행 실패: exit code $exitCode")
            }
        } catch (e: Exception) {
            println("❌ AppleScript 실행 중 오류: ${e.message}")
        }
    }
}
