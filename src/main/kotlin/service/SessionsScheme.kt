package ru.alexbur.backend.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.utils.DispatcherProvider
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp

data class Session(
    val userId: Long,
    val otp: String,
    val countLogin: Int,
    val blockedTime: Timestamp?,
)

class SessionService(
    private val connection: Connection,
    private val dispatcherProvider: DispatcherProvider,
) {
    companion object {
        private const val CREATE_TABLE_SESSIONS =
            "CREATE TABLE IF NOT EXISTS SESSIONS (id SERIAL PRIMARY KEY, user_id INT NOT NULL UNIQUE, " +
                    "code CHAR(6) NOT NULL, count_login INT, blocked_time TIMESTAMP DEFAULT NULL);"
        private const val INSERT_SESSION = "INSERT INTO SESSIONS (user_id, code, count_login) VALUES (?, ?, ?)"
        private const val SELECT_CODE_BY_ID = "SELECT code, count_login, blocked_time FROM SESSIONS WHERE user_id = ?"
        private const val DELETE_SESSION = "DELETE FROM SESSIONS WHERE user_id = ?"
        private const val UPDATE_SESSION =
            "UPDATE SESSIONS SET code = ?, count_login = ?, blocked_time = ? WHERE id = ?"
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_SESSIONS)
    }

    suspend fun create(session: Session) = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(INSERT_SESSION, Statement.RETURN_GENERATED_KEYS)
        statement.setLong(1, session.userId)
        statement.setString(2, session.otp)
        statement.setInt(3, session.countLogin)
        statement.executeUpdate()
        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            return@withContext generatedKeys.getInt(1)
        } else {
            throw Exception("Unable to retrieve the id of the newly inserted session")
        }
    }

    suspend fun read(userId: Long): Session? = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(SELECT_CODE_BY_ID)
        statement.setLong(1, userId)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val otp = resultSet.getString("code")
            val countLogin = resultSet.getInt("count_login")
            val blockedTime: Timestamp? = resultSet.getTimestamp("blocked_time")
            Session(
                userId = userId,
                otp = otp,
                countLogin = countLogin,
                blockedTime = blockedTime
            )
        } else {
            null
        }
    }

    suspend fun update(session: Session) = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(UPDATE_SESSION)
        statement.setString(1, session.otp)
        statement.setInt(2, session.countLogin)
        statement.setTimestamp(3, session.blockedTime)
        statement.setLong(4, session.userId)
        statement.executeUpdate()
    }

    suspend fun delete(userId: Long) = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(DELETE_SESSION)
        statement.setLong(1, userId)
        statement.executeUpdate()
    }
}