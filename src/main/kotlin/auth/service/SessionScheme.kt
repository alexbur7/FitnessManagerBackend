package ru.alexbur.backend.auth.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.utils.DispatcherProvider
import java.sql.Connection
import java.sql.Statement

data class Session(
    val id: Long,
    val userId: Long,
    val refreshToken: String,
)

class SessionService(
    private val connection: Connection,
    private val dispatcherProvider: DispatcherProvider,
) {
    companion object {
        private const val CREATE_TABLE_SESSION =
            "CREATE TABLE IF NOT EXISTS SESSION (id SERIAL PRIMARY KEY, user_id INT NOT NULL, " +
                    "refresh_token VARCHAR(255) NOT NULL);"
        private const val INSERT_SESSION = "INSERT INTO SESSION (user_id, refresh_token) VALUES (?, ?)"
        private const val SELECT_SESSION_BY_USER_ID = "SELECT id, refresh_token FROM SESSION WHERE user_id = ?"
        private const val UPDATE_SESSION = "UPDATE SESSION SET refresh_token = ? WHERE id = ?"
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_SESSION)
    }

    suspend fun create(userId: Long, refreshToken: String) = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(INSERT_SESSION, Statement.RETURN_GENERATED_KEYS)
        statement.setLong(1, userId)
        statement.setString(2, refreshToken)
        statement.executeUpdate()
        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            generatedKeys.getLong(1)
        } else {
            IllegalStateException("Unknown error")
        }
    }

    suspend fun read(userId: Long): Session? = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(SELECT_SESSION_BY_USER_ID)
        statement.setLong(1, userId)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val id = resultSet.getLong("id")
            val refreshToken = resultSet.getString("refresh_token")
            Session(
                id = id,
                userId = userId,
                refreshToken = refreshToken
            )
        } else {
            null
        }
    }

    suspend fun update(refreshToken: String, id: Long) = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(UPDATE_SESSION)
        statement.setString(1, refreshToken)
        statement.setLong(2, id)
        statement.executeUpdate()
    }
}