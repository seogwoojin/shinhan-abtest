package com.example.finsync.mockapi.api

import com.example.finsync.mockapi.dto.AccountInfo
import com.example.finsync.mockapi.dto.MyDataResponse
import com.example.finsync.mockapi.dto.TransactionInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 4. 국민은행 API 컨트롤러
@RestController
@RequestMapping("/v1/bank")
class KBBankController {

    @GetMapping("/kb/transactions")
    fun getKBTransactions(
        @RequestParam limit: Int = 10,
        @RequestParam(required = false) next_page: String?,
        @RequestParam account_num: String,
        @RequestParam from_date: String,
        @RequestParam to_date: String
    ): MyDataResponse<List<TransactionInfo>> {

        val mockTransactions = listOf(
            TransactionInfo(
                tran_dtime = "2024-01-15T08:30:00",
                tran_amt = 500000,
                merchant_name = "이체입금",
                tran_type = "입금",
                balance_amt = 1200000
            ),
            TransactionInfo(
                tran_dtime = "2024-01-15T16:45:00",
                tran_amt = -120000,
                merchant_name = "통신비자동이체",
                tran_type = "출금",
                balance_amt = 1080000
            )
        )

        return MyDataResponse(
            rsp_code = "00000",
            rsp_msg = "정상처리",
            data = mockTransactions.take(limit)
        )
    }

    @GetMapping("/kb/accounts")
    fun getKBAccounts(): MyDataResponse<List<AccountInfo>> {
        val mockAccounts = listOf(
            AccountInfo(
                account_num = "123456-**-******",
                account_type = "자유입출금",
                balance_amt = 1080000,
                account_name = "KB스타뱅킹통장"
            )
        )

        return MyDataResponse(
            rsp_code = "00000",
            rsp_msg = "정상처리",
            data = mockAccounts
        )
    }
}