package com.example.finsync.mydata.external

import com.example.finsync.mydata.dto.AccountInfo
import com.example.finsync.mydata.dto.MyDataResponse
import com.example.finsync.mydata.dto.TransactionInfo
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono

// 3. 신한카드 API 클라이언트
@Service
class ShinhanCardClient(private val webClient: WebClient) {

    private val baseUrl = "http://localhost:8080" // 모킹 서버 주소

    fun getTransactions(
        fromDate: String,
        toDate: String,
        limit: Int = 10
    ): Mono<List<TransactionInfo>> {
        return webClient.get()
            .uri("$baseUrl/v1/card/transactions?from_date=$fromDate&to_date=$toDate&limit=$limit")
            .retrieve()
            .bodyToMono(object :
                ParameterizedTypeReference<MyDataResponse<List<TransactionInfo>>>() {})
            .map { response ->
                response.data?.map { it.copy(source = "신한카드") } ?: emptyList()
            }
            .onErrorReturn(
                listOf(
                    TransactionInfo(
                        tran_dtime = "2024-01-15T10:30:00",
                        tran_amt = 15000,
                        merchant_name = "스타벅스 강남점",
                        tran_type = "결제",
                        category_code = "CAFE",
                        source = "신한카드"
                    ),
                    TransactionInfo(
                        tran_dtime = "2024-01-15T12:45:00",
                        tran_amt = 32000,
                        merchant_name = "올리브영 홍대점",
                        tran_type = "결제",
                        category_code = "BEAUTY",
                        source = "신한카드"
                    )
                )
            )
    }

    fun getAccounts(): Mono<List<AccountInfo>> {
        return webClient.get()
            .uri("$baseUrl/v1/card/accounts")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<MyDataResponse<List<AccountInfo>>>() {})
            .map { response ->
                response.data?.map { it.copy(source = "신한카드") } ?: emptyList()
            }
            .onErrorReturn(
                listOf(
                    AccountInfo(
                        account_num = "1234-****-****-5678",
                        account_type = "신용카드",
                        balance_amt = -450000,
                        account_name = "신한카드 Deep Dream",
                        source = "신한카드"
                    )
                )
            )
    }
}
