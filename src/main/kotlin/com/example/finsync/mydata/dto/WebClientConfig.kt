package com.example.finsync.mydata.dto

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

// 3. WebClient 설정
@Configuration
class WebClientConfig {

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("User-Agent", "TossMyData/1.0")
            .build()
    }
}
