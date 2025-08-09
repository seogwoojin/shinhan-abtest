package com.example.finsync.mockapi.api

import com.example.finsync.mockapi.dto.AccountInfo
import com.example.finsync.mockapi.dto.MyDataResponse
import com.example.finsync.mockapi.dto.TransactionInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/card")
class ShinhanCardController {

    @GetMapping("/transactions")
    fun getCardTransactions(
        @RequestParam limit: Int = 10,
        @RequestParam(required = false) next_page: String?,
        @RequestParam from_date: String,
        @RequestParam to_date: String
    ): MyDataResponse<List<TransactionInfo>> {

        val mockTransactions = listOf(
            TransactionInfo(
                tran_dtime = "2024-01-15T10:30:00",
                tran_amt = 15000,
                merchant_name = "스타벅스 강남점",
                tran_type = "결제",
                category_code = "CAFE"
            ),
            TransactionInfo(
                tran_dtime = "2024-01-15T12:45:00",
                tran_amt = 32000,
                merchant_name = "올리브영 홍대점",
                tran_type = "결제",
                category_code = "BEAUTY"
            ),
            TransactionInfo(
                tran_dtime = "2024-01-16T19:20:00",
                tran_amt = 45000,
                merchant_name = "교촌치킨 역삼점",
                tran_type = "결제",
                category_code = "FOOD"
            )
        )

        return MyDataResponse(
            rsp_code = "00000",
            rsp_msg = "정상처리",
            data = mockTransactions.take(limit)
        )
    }

    @GetMapping("/accounts")
    fun getCardAccounts(): MyDataResponse<List<AccountInfo>> {
        val mockAccounts = listOf(
            AccountInfo(
                account_num = "1234-****-****-5678",
                account_type = "신용카드",
                balance_amt = -450000, // 카드 사용금액
                account_name = "신한카드 Deep Dream"
            )
        )

        return MyDataResponse(
            rsp_code = "00000",
            rsp_msg = "정상처리",
            data = mockAccounts
        )
    }
}
