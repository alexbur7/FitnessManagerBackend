package ru.alexbur.backend.linking.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.Timestamp

data class LinkingInfo(
    val id: Long,
    val coachId: Long,
    val clientCardId: Long,
    val code: String,
    val createdDate: Timestamp,
)

data class LinkingCreate(
    val coachId: Long,
    val clientCardId: Long,
    val code: String,
    val createdDate: Timestamp,
)

class LinkingService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {

    companion object {
        private const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS LINKING (id SERIAL PRIMARY KEY, coach_id INT NOT NULL, " +
                    "client_card_id INT NOT NULL, code VARCHAR(8) NOT NULL UNIQUE, created_date TIMESTAMP NOT NULL);"
        private const val INSERT = "INSERT INTO LINKING (coach_id, client_card_id, code, created_date) " +
                "VALUES (?, ?, ?, ?);"
        private const val SELECT_BY_CODE = "SELECT * FROM LINKING WHERE code = ?;"
        private const val SELECT_BY_ID = "SELECT id, code, created_date FROM LINKING " +
                "WHERE coach_id = ? AND client_card_id = ?;"
        private const val DELETE = "DELETE FROM LINKING WHERE id = ?"
    }

    init {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_TABLE)
            }
        }
    }

    suspend fun create(linking: LinkingCreate): Long = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)
                .use { statement: PreparedStatement ->
                    statement.setLong(1, linking.coachId)
                    statement.setLong(2, linking.clientCardId)
                    statement.setString(3, linking.code)
                    statement.setTimestamp(4, linking.createdDate)
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

    suspend fun readByCode(code: String): LinkingInfo? = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_CODE).use { statement: PreparedStatement ->
                statement.setString(1, code)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val id = resultSet.getLong("id")
                    val coachId = resultSet.getLong("coach_id")
                    val clientCardId = resultSet.getLong("client_card_id")
                    val createdDate = resultSet.getTimestamp("created_date")
                    LinkingInfo(
                        id = id,
                        coachId = coachId,
                        clientCardId = clientCardId,
                        code = code,
                        createdDate = createdDate,
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun readByCoachId(coachId: Long, clientCardId: Long): LinkingInfo? = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_ID).use { statement: PreparedStatement ->
                statement.setLong(1, coachId)
                statement.setLong(2, clientCardId)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val id = resultSet.getLong("id")
                    val code = resultSet.getString("code")
                    val createdDate = resultSet.getTimestamp("created_date")
                    LinkingInfo(
                        id = id,
                        coachId = coachId,
                        clientCardId = clientCardId,
                        code = code,
                        createdDate = createdDate
                    )
                } else {
                    null
                }
            }
        }
    }

    fun delete(id: Long, connection: Connection) {
        connection.prepareStatement(DELETE).use { statement: PreparedStatement ->
            statement.setLong(1, id)
            val deleteCount = statement.executeUpdate()
            if (deleteCount <= 0) throw IllegalStateException("Error")
        }
    }
}