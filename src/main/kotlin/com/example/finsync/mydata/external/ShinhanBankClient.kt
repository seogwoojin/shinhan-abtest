package com.example.finsync.mydata.external

import com.example.finsync.mydata.dto.AccountInfo
import com.example.finsync.mydata.dto.MyDataResponse
import com.example.finsync.mydata.dto.TransactionInfo
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class ShinhanBankClient(private val webClient: WebClient) {

    private val baseUrl = "http://localhost:8080" // 모킹 서버 주소

    fun getTransactions(
        accountNum: String,
        fromDate: String,
        toDate: String,
        limit: Int = 1000
    ): Mono<List<TransactionInfo>> {
        return webClient.get()
            .uri("$baseUrl/v1/bank/transactions?account_num=$accountNum&from_date=$fromDate&to_date=$toDate&limit=$limit")
            .retrieve()
            .bodyToMono(object :
                ParameterizedTypeReference<MyDataResponse<List<TransactionInfo>>>() {})
            .map { response ->
                response.data?.map { it.copy(source = "신한은행") } ?: emptyList()
            }
            .onErrorReturn(
                listOf(
                    TransactionInfo(
                        tran_dtime = "2024-01-15T09:00:00",
                        tran_amt = 2500000,
                        merchant_name = "급여입금",
                        tran_type = "입금",
                        balance_amt = 3200000,
                        source = "신한은행"
                    ),
                    TransactionInfo(
                        tran_dtime = "2024-01-15T14:30:00",
                        tran_amt = -50000,
                        merchant_name = "신한카드 결제",
                        tran_type = "출금",
                        balance_amt = 3150000,
                        source = "신한은행"
                    )
                )
            )
    }

    fun getAccounts(): Mono<List<AccountInfo>> {
        return webClient.get()
            .uri("$baseUrl/v1/bank/accounts")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<MyDataResponse<List<AccountInfo>>>() {})
            .map { response ->
                response.data?.map { it.copy(source = "신한은행") } ?: emptyList()
            }
            .onErrorReturn(
                listOf(
                    AccountInfo(
                        account_num = "110-***-******",
                        account_type = "입출금통장",
                        balance_amt = 2350000,
                        account_name = "신한은행 주거래통장",
                        source = "신한은행"
                    ),
                    AccountInfo(
                        account_num = "100-***-******",
                        account_type = "정기예금",
                        balance_amt = 10000000,
                        account_name = "신한은행 정기예금",
                        source = "신한은행"
                    )
                )
            )
    }
}