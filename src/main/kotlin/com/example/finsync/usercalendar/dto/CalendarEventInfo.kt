package com.example.finsync.usercalendar.dto

data class CalendarEventInfo(
    val merchant_name: String,
    val predicted_amount: Long,
    val predicted_date: String,
    val category: String?,
    val confidence_score: Double
)