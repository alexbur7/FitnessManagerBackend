package ru.alexbur.backend.events.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.Timestamp

data class EventCreate(
    val startTime: Timestamp,
    val endTime: Timestamp,
    val comment: String?,
    val relationshipId: Long,
    val isCancelled: Boolean,
)

data class Event(
    val id: Long,
    val startTime: Timestamp,
    val endTime: Timestamp,
    val isCancelled: Boolean,
    val comment: String?,
    val relationshipId: Long,
)

class EventService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {
    companion object {
        private const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS EVENTS (id SERIAL PRIMARY KEY, " +
                    "start_time TIMESTAMP NOT NULL, " +
                    "end_time TIMESTAMP NOT NULL, is_cancelled BOOLEAN DEFAULT FALSE, " +
                    "comment TEXT, relationship_id BIGINT NOT NULL);"
        private const val INSERT = "INSERT INTO EVENTS (start_time, end_time, comment, relationship_id, is_cancelled) VALUES (?, ?, ?, ?, ?);"
        private const val SELECT_BY_ID =
            "SELECT e.id, e.relationship_id, e.start_time, e.end_time, e.is_cancelled, e.comment " +
                    "FROM EVENTS e " +
                    "JOIN RELATIONSHIPS r ON r.id = e.relationship_id " +
                    "WHERE e.id = ? AND r.coach_id = ?;"
        private const val SELECT_BY_TIME =
            "SELECT e.id, e.relationship_id, e.start_time, e.end_time, e.is_cancelled, e.comment " +
                    "FROM EVENTS e " +
                    "JOIN RELATIONSHIPS r ON r.id = e.relationship_id " +
                    "WHERE r.coach_id = ? AND e.start_time >= ? AND e.end_time <= ?;"

        private const val SELECT_BY_TIME_WITH_RELATIONSHIP_ID = "SELECT id FROM EVENTS WHERE relationship_id = ? AND start_time < ? AND end_time > ?;"
        private const val UPDATE =
            "UPDATE EVENTS SET start_time = ?, end_time = ?, comment = ?, relationship_id = ?, is_cancelled = ? " +
                    "WHERE id = ? AND relationship_id IN (SELECT id FROM RELATIONSHIPS WHERE coach_id = ?)"
        private const val DELETE =
            "DELETE FROM EVENTS WHERE id = ? AND relationship_id IN (SELECT id FROM RELATIONSHIPS WHERE coach_id = ?)"

        private const val SELECT_REMAINING_BY_RELATIONSHIP_IDS = """
            SELECT COALESCE(tp.relationships_id, e.relationship_id) AS relationship_id,
                COALESCE(tp.total_purchased, 0) - COALESCE(e.total_conducted, 0) AS remaining_count
            FROM (
                SELECT relationships_id, SUM(event_counts) AS total_purchased
                FROM TRAINING_PURCHASES
                WHERE relationships_id = ANY(?)
                GROUP BY relationships_id
            ) tp
            FULL OUTER JOIN (
                SELECT relationship_id, COUNT(*) AS total_conducted
                FROM EVENTS
                WHERE relationship_id = ANY(?) AND is_cancelled = FALSE
                GROUP BY relationship_id
            ) e ON e.relationship_id = tp.relationships_id"""
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
                statement.setTimestamp(1, activity.startTime)
                statement.setTimestamp(2, activity.endTime)
                statement.setString(3, activity.comment)
                statement.setLong(4, activity.relationshipId)
                statement.setBoolean(5, activity.isCancelled)
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

    suspend fun readById(id: Long, coachId: Long): Event? = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_ID).use { statement: PreparedStatement ->
                statement.setLong(1, id)
                statement.setLong(2, coachId)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val startTime = resultSet.getTimestamp("start_time")
                    val endTime = resultSet.getTimestamp("end_time")
                    val comment = resultSet.getString("comment")
                    val isCancelled = resultSet.getBoolean("is_cancelled")
                    val relationshipId = resultSet.getLong("relationship_id")
                    Event(
                        id = id,
                        startTime = startTime,
                        endTime = endTime,
                        comment = comment,
                        isCancelled = isCancelled,
                        relationshipId = relationshipId
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun readByTime(
        coachId: Long,
        startTime: Timestamp,
        endTime: Timestamp
    ): List<Event> = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_TIME).use { statement: PreparedStatement ->
                statement.setLong(1, coachId)
                statement.setTimestamp(2, startTime)
                statement.setTimestamp(3, endTime)
                val resultSet = statement.executeQuery()
                val result = mutableListOf<Event>()

                while (resultSet.next()) {
                    val id = resultSet.getLong("id")
                    val startTime = resultSet.getTimestamp("start_time")
                    val endTime = resultSet.getTimestamp("end_time")
                    val comment = resultSet.getString("comment")
                    val isCancelled = resultSet.getBoolean("is_cancelled")
                    val relationshipId = resultSet.getLong("relationship_id")
                    result.add(
                        Event(
                            id = id,
                            startTime = startTime,
                            endTime = endTime,
                            comment = comment,
                            isCancelled = isCancelled,
                            relationshipId = relationshipId
                        )
                    )
                }
                result.toList()
            }
        }
    }

    suspend fun hasActivities(
        startTime: Timestamp,
        endTime: Timestamp,
        relationshipId: Long,
    ): Boolean = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_TIME_WITH_RELATIONSHIP_ID).use { statement: PreparedStatement ->
                statement.setLong(1, relationshipId)
                statement.setTimestamp(2, endTime)
                statement.setTimestamp(3, startTime)
                val resultSet = statement.executeQuery()

                resultSet.next()
            }
        }
    }

    suspend fun update(id: Long, activity: EventCreate, coachId: Long): Boolean = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(UPDATE, Statement.RETURN_GENERATED_KEYS).use { statement: PreparedStatement ->
                statement.setTimestamp(1, activity.startTime)
                statement.setTimestamp(2, activity.endTime)
                statement.setString(3, activity.comment)
                statement.setLong(4, activity.relationshipId)
                statement.setBoolean(5, activity.isCancelled)
                statement.setLong(6, id)
                statement.setLong(7, coachId)
                val updatedCount = statement.executeUpdate()
                updatedCount > 0
            }
        }
    }

    suspend fun getRemainingCountsByRelationshipIds(relationshipIds: List<Long>): Map<Long, Int> =
        withContext(dispatcherProvider.io()) {
            if (relationshipIds.isEmpty()) return@withContext emptyMap()
            getConnection().use { connection ->
                val array = connection.createArrayOf("bigint", relationshipIds.toTypedArray())
                connection.prepareStatement(SELECT_REMAINING_BY_RELATIONSHIP_IDS).use { statement ->
                    statement.setArray(1, array)
                    statement.setArray(2, array)
                    val rs = statement.executeQuery()
                    val result = mutableMapOf<Long, Int>()
                    while (rs.next()) {
                        result[rs.getLong("relationship_id")] = rs.getInt("remaining_count")
                    }
                    result
                }
            }
        }

    suspend fun delete(id: Long, coachId: Long): Boolean = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(DELETE).use { statement: PreparedStatement ->
                statement.setLong(1, id)
                statement.setLong(2, coachId)
                val deletedCount = statement.executeUpdate()
                deletedCount > 0
            }
        }
    }
}