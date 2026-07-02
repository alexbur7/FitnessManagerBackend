package ru.alexbur.backend.relationships.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import java.sql.Connection

internal data class RelationshipCreate(
    val coachId: Long,
    val clientId: Long,
)

internal data class CoachProfile(
    val coachId: Long,
    val firstName: String?,
    val lastName: String?,
)

internal data class ClientProfile(
    val relationshipId: Long,
    val clientId: Long,
)

internal data class RelationshipClients(
    val totalCount: Int,
    val clients: List<ClientProfile>,
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
            "SELECT r.id AS relationship_id, r.client_id, COUNT(*) OVER() AS total_count " +
            "FROM RELATIONSHIPS r " +
            "WHERE r.coach_id = ? AND r.is_deleted = FALSE " +
            "ORDER BY r.id ASC LIMIT ? OFFSET ?;"
        const val SELECT_COACHES =
            "SELECT r.coach_id, p.first_name, p.second_name " +
                    "FROM RELATIONSHIPS r " +
                    "LEFT JOIN PROFILE p ON p.user_id = r.coach_id " +
                    "WHERE r.client_id = ? AND r.is_deleted = FALSE " +
                    "ORDER BY r.coach_id ASC;"
        const val SELECT_RELATIONSHIP =
            "SELECT 1 FROM RELATIONSHIPS WHERE coach_id = ? AND client_id = ? AND is_deleted = FALSE LIMIT 1;"
        const val SELECT_COACH_OF_RELATIONSHIP =
            "SELECT 1 FROM RELATIONSHIPS WHERE id = ? AND coach_id = ? AND is_deleted = FALSE LIMIT 1;"
        const val SELECT_PARTICIPANT_OF_RELATIONSHIP =
            "SELECT 1 FROM RELATIONSHIPS WHERE id = ? AND (coach_id = ? OR client_id = ?) AND is_deleted = FALSE LIMIT 1;"
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
                    val clients = mutableListOf<ClientProfile>()
                    var totalCount = 0
                    while (resultSet.next()) {
                        if (clients.isEmpty()) totalCount = resultSet.getInt("total_count")
                        clients.add(
                            ClientProfile(
                                relationshipId = resultSet.getLong("relationship_id"),
                                clientId = resultSet.getLong("client_id"),
                            )
                        )
                    }
                    RelationshipClients(totalCount = totalCount, clients = clients)
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

    suspend fun isCoachOfRelationship(
        coachId: Long,
        relationshipsId: Long
    ): Boolean = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_COACH_OF_RELATIONSHIP).use { statement ->
                statement.setLong(1, relationshipsId)
                statement.setLong(2, coachId)
                statement.executeQuery().next()
            }
        }
    }

    suspend fun isParticipantOfRelationship(userId: Long, relationshipsId: Long): Boolean =
        withContext(dispatcherProvider.io()) {
            getConnection().use { connection ->
                connection.prepareStatement(SELECT_PARTICIPANT_OF_RELATIONSHIP).use { statement ->
                    statement.setLong(1, relationshipsId)
                    statement.setLong(2, userId)
                    statement.setLong(3, userId)
                    statement.executeQuery().next()
                }
            }
        }

    suspend fun getCoachesByClientId(clientId: Long): List<CoachProfile> = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_COACHES).use { statement ->
                statement.setLong(1, clientId)
                val resultSet = statement.executeQuery()
                val coaches = mutableListOf<CoachProfile>()
                while (resultSet.next()) {
                    coaches.add(
                        CoachProfile(
                            coachId = resultSet.getLong("coach_id"),
                            firstName = resultSet.getString("first_name"),
                            lastName = resultSet.getString("second_name"),
                        )
                    )
                }
                coaches
            }
        }
    }
}
