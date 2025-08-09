package com.example.finsync.mockapi.api

import com.example.finsync.mockapi.dto.AccountInfo
import com.example.finsync.mockapi.dto.InvestmentInfo
import com.example.finsync.mockapi.dto.MyDataResponse
import com.example.finsync.mockapi.dto.TransactionInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 5. 신한투자증권 API 컨트롤러
@RestController
@RequestMapping("/v1/invest")
class ShinhanInvestController {

    @GetMapping("/transactions")
    fun getInvestTransactions(
        @RequestParam limit: Int = 10,
        @RequestParam(required = false) next_page: String?,
        @RequestParam from_date: String,
        @RequestParam to_date: String
    ): MyDataResponse<List<TransactionInfo>> {

        val mockTransactions = listOf(
            TransactionInfo(
                tran_dtime = "2024-01-15T09:30:00",
                tran_amt = -150000,
                merchant_name = "삼성전자 매수",
                tran_type = "매수",
                balance_amt = 2850000
            ),
            TransactionInfo(
                tran_dtime = "2024-01-16T14:20:00",
                tran_amt = 80000,
                merchant_name = "배당금 지급",
                tran_type = "배당",
                balance_amt = 2930000
            )
        )

        return MyDataResponse(
            rsp_code = "00000",
            rsp_msg = "정상처리",
            data = mockTransactions.take(limit)
        )
    }

    @GetMapping("/portfolio")
    fun getPortfolio(): MyDataResponse<List<InvestmentInfo>> {
        val mockPortfolio = listOf(
            InvestmentInfo(
                stock_code = "005930",
                stock_name = "삼성전자",
                holding_qty = 10,
                avg_buy_price = 75000,
                current_price = 78000,
                eval_amt = 780000,
                profit_loss_amt = 30000
            ),
            InvestmentInfo(
                stock_code = "035420",
                stock_name = "NAVER",
                holding_qty = 5,
                avg_buy_price = 180000,
                current_price = 185000,
                eval_amt = 925000,
                profit_loss_amt = 25000
            )
        )

        return MyDataResponse(
            rsp_code = "00000",
            rsp_msg = "정상처리",
            data = mockPortfolio
        )
    }
}