package ru.alexbur.backend.relationships.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import java.sql.Connection

internal data class RelationshipCreate(
    val coachId: Long,
    val clientId: Long,
)

internal data class RelationshipClients(
    val totalCount: Int,
    val clientIds: List<Long>,
)

internal class RelationshipsService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {

    private companion object {
        const val CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS RELATIONSHIPS (
                id SERIAL PRIMARY KEY,
                coach_id BIGINT NOT NULL,
                client_id BIGINT NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE
            );
        """
        const val INSERT = "INSERT INTO RELATIONSHIPS (coach_id, client_id) VALUES (?, ?) RETURNING id;"
        const val SELECT_CLIENTS =
            "SELECT client_id, COUNT(*) OVER() AS total_count " +
            "FROM RELATIONSHIPS WHERE coach_id = ? AND is_deleted = FALSE " +
            "ORDER BY client_id ASC LIMIT ? OFFSET ?;"
        const val SELECT_COACHES = "SELECT coach_id FROM RELATIONSHIPS WHERE client_id = ? AND is_deleted = FALSE;"
        const val SELECT_RELATIONSHIP =
            "SELECT 1 FROM RELATIONSHIPS WHERE coach_id = ? AND client_id = ? AND is_deleted = FALSE LIMIT 1;"
    }

    init {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_TABLE.trimIndent())
            }
        }
    }

    suspend fun create(data: RelationshipCreate): Long = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(INSERT).use { statement ->
                statement.setLong(1, data.coachId)
                statement.setLong(2, data.clientId)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    resultSet.getLong(1)
                } else {
                    throw IllegalStateException("Failed to create relationship")
                }
            }
        }
    }

    suspend fun getClientsByCoachId(coachId: Long, limit: Int, offset: Int): RelationshipClients =
        withContext(dispatcherProvider.io()) {
            getConnection().use { connection ->
                connection.prepareStatement(SELECT_CLIENTS).use { statement ->
                    statement.setLong(1, coachId)
                    statement.setInt(2, limit)
                    statement.setInt(3, offset)
                    val resultSet = statement.executeQuery()
                    val ids = mutableListOf<Long>()
                    var totalCount = 0
                    while (resultSet.next()) {
                        if (ids.isEmpty()) totalCount = resultSet.getInt("total_count")
                        ids.add(resultSet.getLong("client_id"))
                    }
                    RelationshipClients(totalCount = totalCount, clientIds = ids)
                }
            }
        }

    suspend fun hasRelationship(coachId: Long, clientId: Long): Boolean = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_RELATIONSHIP).use { statement ->
                statement.setLong(1, coachId)
                statement.setLong(2, clientId)
                statement.executeQuery().next()
            }
        }
    }

    suspend fun getCoachesByClientId(clientId: Long): List<Long> = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_COACHES).use { statement ->
                statement.setLong(1, clientId)
                val resultSet = statement.executeQuery()
                val ids = mutableListOf<Long>()
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("coach_id"))
                }
                ids
            }
        }
    }
}
