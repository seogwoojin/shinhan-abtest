package com.example.finsync.usercalendar.dto

data class CalendarEventRequest(
    val username: String,
    val events: List<CalendarEventInfo>
)
