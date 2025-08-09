package com.example.finsync.mydata.api

import com.example.finsync.mydata.external.KBBankClient
import com.example.finsync.mydata.external.ShinhanBankClient
import com.example.finsync.mydata.external.ShinhanCardClient
import com.example.finsync.mydata.external.ShinhanInvestClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

// 9. 헬스체크 및 연결 상태 확인
@RestController
@RequestMapping("/toss/health")
class HealthCheckController(
    private val shinhanCardClient: ShinhanCardClient,
    private val shinhanBankClient: ShinhanBankClient,
    private val kbBankClient: KBBankClient,
    private val shinhanInvestClient: ShinhanInvestClient
) {

    @GetMapping("/check")
    fun healthCheck(): Mono<ResponseEntity<Map<String, Any>>> {
        val cardHealth =
            shinhanCardClient.getAccounts().map { "healthy" }.onErrorReturn("unhealthy")
        val shinhanBankHealth =
            shinhanBankClient.getAccounts().map { "healthy" }.onErrorReturn("unhealthy")
        val kbBankHealth = kbBankClient.getAccounts().map { "healthy" }.onErrorReturn("unhealthy")
        val investHealth =
            shinhanInvestClient.getPortfolio().map { "healthy" }.onErrorReturn("unhealthy")

        return Mono.zip(cardHealth, shinhanBankHealth, kbBankHealth, investHealth)
            .map { tuple ->
                val institutionStatus = mapOf(
                    "shinhan_card" to tuple.t1,
                    "shinhan_bank" to tuple.t2,
                    "kb_bank" to tuple.t3,
                    "shinhan_invest" to tuple.t4
                )

                val allHealthy = institutionStatus.values.all { it == "healthy" }

                ResponseEntity.ok(
                    mapOf(
                        "overall_status" to if (allHealthy) "healthy" else "degraded",
                        "institutions" to institutionStatus,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            }
    }
}