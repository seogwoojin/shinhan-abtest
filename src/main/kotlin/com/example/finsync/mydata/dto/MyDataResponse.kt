package com.example.finsync.mydata.dto

// 1. 공통 데이터 모델
data class MyDataResponse<T>(
    val rsp_code: String,
    val rsp_msg: String,
    val data: T? = null,
    val next_page: String? = null
)