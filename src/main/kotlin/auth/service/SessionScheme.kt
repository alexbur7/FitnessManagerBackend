package ru.alexbur.backend.auth.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import java.sql.Connection
import java.sql.Statement

data class Session(
    val id: Long,
    val userId: Long,
    val refreshToken: String,
    val userAgent: String,
)

class SessionService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {
    companion object {
        private const val CREATE_TABLE_SESSION =
            "CREATE TABLE IF NOT EXISTS SESSIONS (id SERIAL PRIMARY KEY, user_id INT NOT NULL, " +
                    "refresh_token VARCHAR(255) NOT NULL, user_agent VARCHAR(255) NOT NULL, " +
                    "UNIQUE (user_id, user_agent));"
        private const val INSERT_SESSION = "INSERT INTO SESSIONS (user_id, refresh_token, user_agent) " +
                "VALUES (?, ?, ?) ON CONFLICT (user_id, user_agent)" +
                "DO UPDATE SET refresh_token = EXCLUDED.refresh_token;"
        private const val SELECT_SESSION_BY_USER_ID = "SELECT id, refresh_token FROM SESSIONS " +
                "WHERE user_id = ? AND user_agent = ?"
        private const val UPDATE_SESSION = "UPDATE SESSIONS SET refresh_token = ? WHERE id = ?"
    }

    init {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_TABLE_SESSION)
            }
        }
    }

    suspend fun create(userId: Long, refreshToken: String, userAgent: String) = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(INSERT_SESSION, Statement.RETURN_GENERATED_KEYS).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, refreshToken)
                statement.setString(3, userAgent)
                statement.executeUpdate()
                val generatedKeys = statement.generatedKeys
                if (generatedKeys.next()) {
                    generatedKeys.getLong(1)
                } else {
                    IllegalStateException("Unknown error")
                }
            }
        }
    }

    suspend fun read(userId: Long, userAgent: String): Session? = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_SESSION_BY_USER_ID).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, userAgent)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val id = resultSet.getLong("id")
                    val refreshToken = resultSet.getString("refresh_token")
                    Session(
                        id = id,
                        userId = userId,
                        refreshToken = refreshToken,
                        userAgent = userAgent,
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun update(refreshToken: String, id: Long) = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(UPDATE_SESSION).use { statement ->
                statement.setString(1, refreshToken)
                statement.setLong(2, id)
                statement.executeUpdate()
            }
        }
    }
}