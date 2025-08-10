package com.example.finsync.usercalendar.dto

data class CalendarEventResponse(
    val result: String,
    val message: String,
    val created_events: Int
)