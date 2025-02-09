package ru.alexbur.backend.events.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.Timestamp

data class EventCreate(
    val userId: Long,
    val startTime: Timestamp,
    val endTime: Timestamp,
    val comment: String?,
    val clientCardId: Long,
    val isEnded: Boolean,
)

data class Event(
    val id: Long,
    val userId: Long,
    val startTime: Timestamp,
    val endTime: Timestamp,
    val isEnded: Boolean,
    val comment: String?,
    val clientCardId: Long,
)

class EventService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {
    companion object {
        private const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS EVENTS (id SERIAL PRIMARY KEY, user_id INT NOT NULL, " +
                    "start_time TIMESTAMP NOT NULL, " +
                    "end_time TIMESTAMP NOT NULL, is_ended BOOLEAN DEFAULT FALSE, " +
                    "comment TEXT, client_card_id INT NOT NULL);"
        private const val INSERT = "INSERT INTO EVENTS (user_id, start_time, end_time, " +
                "comment, client_card_id, is_ended) VALUES (?, ?, ?, ?, ?, ?);"
        private const val SELECT_BY_ID = "SELECT * FROM EVENTS WHERE id = ? AND user_id = ?;"
        private const val SELECT_BY_TIME = "SELECT * FROM EVENTS WHERE user_id = ? " +
                "AND start_time >= ? AND end_time <= ?;"

        private const val SELECT_BY_TIME_WITH_CLIENT_ID = "SELECT id FROM EVENTS WHERE user_id = ? " +
                "AND client_card_id = ? AND start_time < ? AND end_time > ?;"
        private const val UPDATE = "UPDATE EVENTS SET start_time = ?, end_time = ?, comment = ?, " +
                "client_card_id = ?, is_ended = ? WHERE id = ? AND user_id = ?"
        private const val DELETE = "DELETE FROM EVENTS WHERE id = ? AND user_id = ?"
    }

    init {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_TABLE)
            }
        }
    }

    suspend fun create(activity: EventCreate): Long = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS).use { statement: PreparedStatement ->
                statement.setLong(1, activity.userId)
                statement.setTimestamp(2, activity.startTime)
                statement.setTimestamp(3, activity.endTime)
                statement.setString(4, activity.comment)
                statement.setLong(5, activity.clientCardId)
                statement.setBoolean(6, activity.isEnded)
                statement.executeUpdate()
                val generatedKeys = statement.generatedKeys
                return@withContext if (generatedKeys.next()) {
                    generatedKeys.getLong(1)
                } else {
                    throw IllegalStateException("Unknown error")
                }
            }
        }
    }

    suspend fun readById(id: Long, userId: Long): Event? = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_ID).use { statement: PreparedStatement ->
                statement.setLong(1, id)
                statement.setLong(2, userId)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val userId = resultSet.getLong("user_id")
                    val startTime = resultSet.getTimestamp("start_time")
                    val endTime = resultSet.getTimestamp("end_time")
                    val comment = resultSet.getString("comment")
                    val isEnded = resultSet.getBoolean("is_ended")
                    val clientCardId = resultSet.getLong("client_card_id")
                    Event(
                        id = id,
                        userId = userId,
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
        }
    }

    suspend fun readByTime(
        userId: Long,
        startTime: Timestamp,
        endTime: Timestamp
    ): List<Event> = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_TIME).use { statement: PreparedStatement ->
                statement.setLong(1, userId)
                statement.setTimestamp(2, startTime)
                statement.setTimestamp(3, endTime)
                val resultSet = statement.executeQuery()
                val result = mutableListOf<Event>()

                while (resultSet.next()) {
                    val id = resultSet.getLong("id")
                    val userId = resultSet.getLong("user_id")
                    val startTime = resultSet.getTimestamp("start_time")
                    val endTime = resultSet.getTimestamp("end_time")
                    val comment = resultSet.getString("comment")
                    val isEnded = resultSet.getBoolean("is_ended")
                    val clientCardId = resultSet.getLong("client_card_id")
                    result.add(
                        Event(
                            id = id,
                            userId = userId,
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
        }
    }

    suspend fun hasActivities(
        userId: Long,
        startTime: Timestamp,
        endTime: Timestamp,
        clientCardId: Long,
    ): Boolean = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_TIME_WITH_CLIENT_ID).use { statement: PreparedStatement ->
                statement.setLong(1, userId)
                statement.setLong(2, clientCardId)
                statement.setTimestamp(3, endTime)
                statement.setTimestamp(4, startTime)
                val resultSet = statement.executeQuery()

                resultSet.next()
            }
        }
    }

    suspend fun update(id: Long, activity: EventCreate): Boolean = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(UPDATE, Statement.RETURN_GENERATED_KEYS).use { statement: PreparedStatement ->
                statement.setTimestamp(1, activity.startTime)
                statement.setTimestamp(2, activity.endTime)
                statement.setString(3, activity.comment)
                statement.setLong(4, activity.clientCardId)
                statement.setBoolean(5, activity.isEnded)
                statement.setLong(6, id)
                statement.setLong(7, activity.userId)
                val updatedCount = statement.executeUpdate()
                updatedCount > 0
            }
        }
    }

    suspend fun delete(id: Long, userId: Long): Boolean = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(DELETE).use { statement: PreparedStatement ->
                statement.setLong(1, id)
                statement.setLong(2, userId)
                val deletedCount = statement.executeUpdate()
                deletedCount > 0
            }
        }
    }
}