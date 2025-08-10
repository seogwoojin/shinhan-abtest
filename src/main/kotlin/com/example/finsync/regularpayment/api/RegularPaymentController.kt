package com.example.finsync.regularpayment.api

import com.example.finsync.regularpayment.dto.RegisterRegularPaymentRequest
import com.example.finsync.regularpayment.repository.Payment
import com.example.finsync.regularpayment.repository.RegularPayment
import com.example.finsync.regularpayment.repository.RegularPaymentRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/toss/api/recurring")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class RegularPaymentController(
    private val regularPaymentRepository: RegularPaymentRepository
) {

    @PostMapping("/auto-payment")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRegularPaymentRequest) {
        val payments = request.payments.map {
            Payment(
                merchantName = it.merchant_name,
                predictedAmount = it.predicted_amount,
                predictedDate = it.predicted_date,
                accountNumber = it.account_number,
                isActive = it.is_active
            )
        }
        val regularPayment = RegularPayment(
            username = request.username,
            payments = payments
        )
        regularPaymentRepository.save(regularPayment)
    }

    @GetMapping("/auto-payment/{username}")
    fun find(@PathVariable username: String): List<RegularPayment> {
        return regularPaymentRepository.findAllByUsername(username)
    }
}
