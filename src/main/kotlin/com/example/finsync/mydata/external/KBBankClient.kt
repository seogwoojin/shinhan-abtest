package com.example.finsync.mydata.external

import com.example.finsync.mydata.dto.AccountInfo
import com.example.finsync.mydata.dto.MyDataResponse
import com.example.finsync.mydata.dto.TransactionInfo
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono

// 5. 국민은행 API 클라이언트
@Service
class KBBankClient(private val webClient: WebClient) {

    private val baseUrl = "http://localhost:8080" // 모킹 서버 주소

    fun getTransactions(
        accountNum: String,
        fromDate: String,
        toDate: String,
        limit: Int = 10
    ): Mono<List<TransactionInfo>> {
        return webClient.get()
            .uri("$baseUrl/v1/bank/kb/transactions?account_num=$accountNum&from_date=$fromDate&to_date=$toDate&limit=$limit")
            .retrieve()
            .bodyToMono(object :
                ParameterizedTypeReference<MyDataResponse<List<TransactionInfo>>>() {})
            .map { response ->
                response.data?.map { it.copy(source = "국민은행") } ?: emptyList()
            }
            .onErrorReturn(
                listOf(
                    TransactionInfo(
                        tran_dtime = "2024-01-15T08:30:00",
                        tran_amt = 500000,
                        merchant_name = "이체입금",
                        tran_type = "입금",
                        balance_amt = 1200000,
                        source = "국민은행"
                    ),
                    TransactionInfo(
                        tran_dtime = "2024-01-15T16:45:00",
                        tran_amt = -120000,
                        merchant_name = "통신비자동이체",
                        tran_type = "출금",
                        balance_amt = 1080000,
                        source = "국민은행"
                    )
                )
            )
    }

    fun getAccounts(): Mono<List<AccountInfo>> {
        return webClient.get()
            .uri("$baseUrl/v1/bank/kb/accounts")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<MyDataResponse<List<AccountInfo>>>() {})
            .map { response ->
                response.data?.map { it.copy(source = "국민은행") } ?: emptyList()
            }
            .onErrorReturn(
                listOf(
                    AccountInfo(
                        account_num = "123456-**-******",
                        account_type = "자유입출금",
                        balance_amt = 1080000,
                        account_name = "KB스타뱅킹통장",
                        source = "국민은행"
                    )
                )
            )
    }
}