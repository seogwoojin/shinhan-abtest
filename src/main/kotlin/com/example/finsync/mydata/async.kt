package com.example.finsync.mydata// 코루틴을 위한 확장 함수
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun <T> async(block: suspend () -> T) = coroutineScope {
    async { block() }
}