package ru.alexbur.backend.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.utils.DispatcherProvider
import java.sql.Connection
import java.sql.Statement

data class User(
    val userId: Long,
    val phone: String,
)

class UserService(
    private val connection: Connection,
    private val dispatcherProvider: DispatcherProvider,
) {
    companion object {
        private const val CREATE_TABLE_USER =
            "CREATE TABLE IF NOT EXISTS USERS (id SERIAL PRIMARY KEY, phone CHAR(11) NOT NULL UNIQUE);"
        private const val INSERT_USER = "INSERT INTO USERS (phone) VALUES (?) " +
                "ON CONFLICT (phone) DO NOTHING RETURNING id;"
        private const val SELECT_CODE_BY_ID = "SELECT phone FROM USERS WHERE id = ?"
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_USER)
    }

    suspend fun create(phone: String): Long = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, phone)
        statement.executeUpdate()
        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) {
            generatedKeys.getLong(1)
        } else {
            val selectStatement = connection.prepareStatement("SELECT id FROM USERS WHERE phone = ?;")
            selectStatement.setString(1, phone)
            val selectResultSet = selectStatement.executeQuery()
            if (selectResultSet.next()) {
                selectResultSet.getLong("id")
            } else {
                throw IllegalStateException("Unknown error")
            }
        }
    }

    suspend fun read(userId: Long): User? = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(SELECT_CODE_BY_ID)
        statement.setLong(1, userId)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val phone = resultSet.getString("phone")
            User(
                userId = userId,
                phone = phone
            )
        } else {
            null
        }
    }
}