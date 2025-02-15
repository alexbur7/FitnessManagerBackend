package ru.alexbur.backend.base.sql

import io.ktor.server.application.*
import ru.alexbur.backend.db.getConnection
import java.sql.Connection

fun Application.transaction(action: (Connection) -> Unit) {
    getConnection(embedded = false).use { connection ->
        try {
            connection.autoCommit = false
            action(connection)
            connection.commit()
        } catch (e: Throwable) {
            connection.rollback()
            throw e
        }
    }
}