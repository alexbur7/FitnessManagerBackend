package ru.alexbur.backend.base.utils

import java.sql.ResultSet

fun ResultSet.getIntOrNull(columnLabel: String): Int? {
    return getInt(columnLabel).takeIf { it != 0 }
}