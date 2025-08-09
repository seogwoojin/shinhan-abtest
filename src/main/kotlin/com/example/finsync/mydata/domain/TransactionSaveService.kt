package com.example.finsync.mydata.domain

import com.example.finsync.mydata.dto.TransactionInfo
import com.example.finsync.mydata.repository.TransactionJpaRepository
import com.example.finsync.mydata.repository.UserTransactionInfo
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class TransactionSaveService(
    private val repository: TransactionJpaRepository
) {
    fun saveTransactions(
        username: String,
        transactions: Map<String, List<TransactionInfo>>
    ): Mono<Void> {
        return Mono.fromCallable {
            val entities = transactions
                .flatMap { (_, list) ->
                    list.map { dto -> UserTransactionInfo.from(username, dto) }
                }
            repository.saveAll(entities) // List<UserTransactionInfo> 반환
        }
            .subscribeOn(Schedulers.boundedElastic()) // 블로킹 DB 저장을 별도 스레드에서
            .then() // 반환값 버리고 Mono<Void>로 변환
    }
}