package ru.alexbur.backend.base.utils

import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneId

fun getCurrentTimestamp(): Timestamp {
    return Timestamp.from(Instant.now())
}

fun compareTimeWithCurrent(time: Timestamp, hours: Long): Boolean {
    val localDateTime = time.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .plusHours(hours)
    val newTime = Timestamp.valueOf(localDateTime)

    val currentTime = getCurrentTimestamp()
    return newTime > currentTime
}