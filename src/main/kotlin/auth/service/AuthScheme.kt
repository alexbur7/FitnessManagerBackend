package ru.alexbur.backend.auth.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.utils.DispatcherProvider
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp

data class AuthInfo(
    val userId: Long,
    val otp: String,
    val countLogin: Int,
    val blockedTime: Timestamp?,
)

class AuthService(
    private val connection: Connection,
    private val dispatcherProvider: DispatcherProvider,
) {
    companion object {
        private const val CREATE_TABLE_AUTH =
            "CREATE TABLE IF NOT EXISTS AUTH (id SERIAL PRIMARY KEY, user_id INT NOT NULL UNIQUE, " +
                    "code CHAR(6) NOT NULL, count_login INT, blocked_time TIMESTAMP DEFAULT NULL);"
        private const val INSERT_AUTH = "INSERT INTO AUTH (user_id, code, count_login) VALUES (?, ?, ?)"
        private const val SELECT_CODE_BY_ID = "SELECT code, count_login, blocked_time FROM AUTH WHERE user_id = ?"
        private const val DELETE_AUTH = "DELETE FROM AUTH WHERE user_id = ?"
        private const val UPDATE_AUTH = "UPDATE AUTH SET code = ?, count_login = ?, blocked_time = ? WHERE id = ?"
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_AUTH)
    }

    suspend fun create(authInfo: AuthInfo) = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(INSERT_AUTH, Statement.RETURN_GENERATED_KEYS)
        statement.setLong(1, authInfo.userId)
        statement.setString(2, authInfo.otp)
        statement.setInt(3, authInfo.countLogin)
        statement.executeUpdate()
        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            return@withContext generatedKeys.getInt(1)
        } else {
            throw Exception("Unable to retrieve the id of the newly inserted session")
        }
    }

    suspend fun read(userId: Long): AuthInfo? = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(SELECT_CODE_BY_ID)
        statement.setLong(1, userId)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val otp = resultSet.getString("code")
            val countLogin = resultSet.getInt("count_login")
            val blockedTime: Timestamp? = resultSet.getTimestamp("blocked_time")
            AuthInfo(
                userId = userId,
                otp = otp,
                countLogin = countLogin,
                blockedTime = blockedTime
            )
        } else {
            null
        }
    }

    suspend fun update(authInfo: AuthInfo) = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(UPDATE_AUTH)
        statement.setString(1, authInfo.otp)
        statement.setInt(2, authInfo.countLogin)
        statement.setTimestamp(3, authInfo.blockedTime)
        statement.setLong(4, authInfo.userId)
        statement.executeUpdate()
    }

    suspend fun delete(userId: Long) = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(DELETE_AUTH)
        statement.setLong(1, userId)
        statement.executeUpdate()
    }
}