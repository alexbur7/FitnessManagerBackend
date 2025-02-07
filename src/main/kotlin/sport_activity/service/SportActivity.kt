package ru.alexbur.backend.sport_activity.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.utils.DispatcherProvider
import java.sql.Connection
import java.sql.Statement
import java.sql.Timestamp

data class CreateSportActivity(
    val userId: Long,
    val name: String,
    val startTime: Timestamp,
    val endTime: Timestamp,
    val comment: String?,
    val clientCardId: Long,
)

data class SportActivity(
    val id: Long,
    val userId: Long,
    val name: String,
    val startTime: Timestamp,
    val endTime: Timestamp,
    val isEnded: Boolean,
    val comment: String?,
    val clientCardId: Long,
)

class SportActivityService(
    private val connection: Connection,
    private val dispatcherProvider: DispatcherProvider,
) {
    companion object {
        private const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS SportActivities (id SERIAL PRIMARY KEY, user_id INT NOT NULL, " +
                    "name VARCHAR(255) NOT NULL, start_time TIMESTAMP NOT NULL, " +
                    "end_time TIMESTAMP NOT NULL, is_ended BOOLEAN DEFAULT FALSE, " +
                    "comment TEXT, client_card_id INT NOT NULL);"
        private const val INSERT = "INSERT INTO SportActivities (user_id, name, start_time, end_time, " +
                "comment, client_card_id) VALUES (?, ?, ?, ?, ?, ?);"
        private const val SELECT_BY_ID = "SELECT * FROM SportActivities WHERE id = ? AND user_id = ?;"
        private const val SELECT_BY_TIME = "SELECT * FROM SportActivities WHERE user_id = ? " +
                "AND start_time >= ? AND end_time <= ?;"

        private const val SELECT_BY_TIME_WITH_CLIENT_ID = "SELECT id FROM SportActivities WHERE user_id = ? " +
                "AND client_card_id = ? AND start_time < ? AND end_time > ?;"
    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE)
    }

    suspend fun create(activity: CreateSportActivity): Long = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)
        statement.setLong(1, activity.userId)
        statement.setString(2, activity.name)
        statement.setTimestamp(3, activity.startTime)
        statement.setTimestamp(4, activity.endTime)
        statement.setString(5, activity.comment)
        statement.setLong(6, activity.clientCardId)
        statement.executeUpdate()
        val generatedKeys = statement.generatedKeys
        return@withContext if (generatedKeys.next()) {
            generatedKeys.getLong(1)
        } else {
            throw IllegalStateException("Unknown error")
        }
    }

    suspend fun readById(id: Long, userId: Long): SportActivity? = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(SELECT_BY_ID)
        statement.setLong(1, id)
        statement.setLong(2, userId)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            val userId = resultSet.getLong("user_id")
            val name = resultSet.getString("name")
            val startTime = resultSet.getTimestamp("start_time")
            val endTime = resultSet.getTimestamp("end_time")
            val comment = resultSet.getString("comment")
            val isEnded = resultSet.getBoolean("is_ended")
            val clientCardId = resultSet.getLong("client_card_id")
            SportActivity(
                id = id,
                userId = userId,
                name = name,
                startTime = startTime,
                endTime = endTime,
                comment = comment,
                isEnded = isEnded,
                clientCardId = clientCardId
            )
        } else {
            null
        }
    }

    suspend fun readByTime(
        userId: Long,
        startTime: Timestamp,
        endTime: Timestamp
    ): List<SportActivity> = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(SELECT_BY_TIME)
        statement.setLong(1, userId)
        statement.setTimestamp(2, startTime)
        statement.setTimestamp(3, endTime)
        val resultSet = statement.executeQuery()
        val result = mutableListOf<SportActivity>()

        while (resultSet.next()) {
            val id = resultSet.getLong("id")
            val userId = resultSet.getLong("user_id")
            val name = resultSet.getString("name")
            val startTime = resultSet.getTimestamp("start_time")
            val endTime = resultSet.getTimestamp("end_time")
            val comment = resultSet.getString("comment")
            val isEnded = resultSet.getBoolean("is_ended")
            val clientCardId = resultSet.getLong("client_card_id")
            result.add(
                SportActivity(
                    id = id,
                    userId = userId,
                    name = name,
                    startTime = startTime,
                    endTime = endTime,
                    comment = comment,
                    isEnded = isEnded,
                    clientCardId = clientCardId
                )
            )
        }
        result.toList()
    }

    suspend fun hasActivities(
        userId: Long,
        startTime: Timestamp,
        endTime: Timestamp,
        clientCardId: Long,
    ): Boolean = withContext(dispatcherProvider.io()) {
        val statement = connection.prepareStatement(SELECT_BY_TIME_WITH_CLIENT_ID)
        statement.setLong(1, userId)
        statement.setLong(2, clientCardId)
        statement.setTimestamp(3, endTime)
        statement.setTimestamp(4, startTime)
        val resultSet = statement.executeQuery()

        resultSet.next()
    }
}