package com.example.finsync.mydata.external

import com.example.finsync.mydata.dto.InvestmentInfo
import com.example.finsync.mydata.dto.MyDataResponse
import com.example.finsync.mydata.dto.TransactionInfo
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono

@Service
class ShinhanInvestClient(private val webClient: WebClient) {

    private val baseUrl = "http://localhost:8080" // 모킹 서버 주소

    fun getTransactions(
        fromDate: String,
        toDate: String,
        limit: Int = 10
    ): Mono<List<TransactionInfo>> {
        return webClient.get()
            .uri("$baseUrl/v1/invest/transactions?from_date=$fromDate&to_date=$toDate&limit=$limit")
            .retrieve()
            .bodyToMono(object :
                ParameterizedTypeReference<MyDataResponse<List<TransactionInfo>>>() {})
            .map { response ->
                response.data?.map { it.copy(source = "신한투자증권") } ?: emptyList()
            }
            .onErrorReturn(
                listOf(
                    TransactionInfo(
                        tran_dtime = "2024-01-15T09:30:00",
                        tran_amt = -150000,
                        merchant_name = "삼성전자 매수",
                        tran_type = "매수",
                        balance_amt = 2850000,
                        source = "신한투자증권"
                    ),
                    TransactionInfo(
                        tran_dtime = "2024-01-16T14:20:00",
                        tran_amt = 80000,
                        merchant_name = "배당금 지급",
                        tran_type = "배당",
                        balance_amt = 2930000,
                        source = "신한투자증권"
                    )
                )
            )
    }

    fun getPortfolio(): Mono<List<InvestmentInfo>> {
        return webClient.get()
            .uri("$baseUrl/v1/invest/portfolio")
            .retrieve()
            .bodyToMono(object :
                ParameterizedTypeReference<MyDataResponse<List<InvestmentInfo>>>() {})
            .map { response ->
                response.data?.map { it.copy(source = "신한투자증권") } ?: emptyList()
            }
            .onErrorReturn(
                listOf(
                    InvestmentInfo(
                        stock_code = "005930",
                        stock_name = "삼성전자",
                        holding_qty = 10,
                        avg_buy_price = 75000,
                        current_price = 78000,
                        eval_amt = 780000,
                        profit_loss_amt = 30000,
                        source = "신한투자증권"
                    ),
                    InvestmentInfo(
                        stock_code = "035420",
                        stock_name = "NAVER",
                        holding_qty = 5,
                        avg_buy_price = 180000,
                        current_price = 185000,
                        eval_amt = 925000,
                        profit_loss_amt = 25000,
                        source = "신한투자증권"
                    )
                )
            )
    }
}