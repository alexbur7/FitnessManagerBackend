package ru.alexbur.backend.history_weight.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.Timestamp

data class HistoryWeightCreate(
    val weightGm: Int,
    val clientCardId: Long,
    val date: Timestamp,
)

data class HistoryWeight(
    val id: Long,
    val weightGm: Int,
    val date: Timestamp,
    val clientCardId: Long,
)

data class HistoryWeights(
    val totalCount: Int,
    val weights: List<HistoryWeight>,
)

class HistoryWeightService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {

    companion object {
        private const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS HISTORY_WEIGHT (id SERIAL PRIMARY KEY, weight_gm INT NOT NULL, " +
                    "client_card_id INT NOT NULL, date TIMESTAMP NOT NULL);"
        private const val INSERT = "INSERT INTO HISTORY_WEIGHT (weight_gm, client_card_id, date) VALUES (?, ?, ?);"
        private const val SELECT_BY_ID = "SELECT * FROM HISTORY_WEIGHT WHERE id = ?"
        private const val UPDATE = "UPDATE HISTORY_WEIGHT SET weight_gm = ? WHERE id = ?"
        private const val DELETE = "DELETE FROM HISTORY_WEIGHT WHERE id = ?"
    }

    init {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_TABLE)
            }
        }
    }

    fun create(data: HistoryWeightCreate, connection: Connection): Long {
        connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS).use { statement: PreparedStatement ->
            statement.setInt(1, data.weightGm)
            statement.setLong(2, data.clientCardId)
            statement.setTimestamp(3, data.date)
            statement.executeUpdate()
            val generatedKeys = statement.generatedKeys
            return if (generatedKeys.next()) {
                generatedKeys.getLong(1)
            } else {
                throw IllegalStateException("Unknown error")
            }
        }
    }

    suspend fun getById(id: Long): HistoryWeight? = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_ID).use { statement: PreparedStatement ->
                statement.setLong(1, id)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    HistoryWeight(
                        id = resultSet.getLong("id"),
                        weightGm = resultSet.getInt("weight_gm"),
                        date = resultSet.getTimestamp("date"),
                        clientCardId = resultSet.getLong("client_card_id"),
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun getWeightByClientCardId(
        clientCardId: Long,
        limit: Int,
        offset: Int
    ): HistoryWeights = withContext(dispatcherProvider.io()) {
        val sql = """
            WITH filtered_data AS (
                SELECT id, weight_gm, date, client_card_id
                FROM HISTORY_WEIGHT
                WHERE client_card_id = $clientCardId
            ), total_count AS (
                SELECT COUNT(*) AS count FROM filtered_data
            )
            SELECT *, (SELECT count FROM total_count) AS total_records
            FROM filtered_data
            ORDER BY date DESC
            LIMIT $limit OFFSET $offset;
        """.trimIndent()

        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(sql)

                val clients = mutableListOf<HistoryWeight>()
                var totalCount = 0

                while (resultSet.next()) {
                    if (totalCount == 0) {
                        totalCount = resultSet.getInt("total_records")
                    }
                    clients.add(
                        HistoryWeight(
                            id = resultSet.getLong("id"),
                            weightGm = resultSet.getInt("weight_gm"),
                            date = resultSet.getTimestamp("date"),
                            clientCardId = resultSet.getLong("client_card_id"),
                        )
                    )
                }

                HistoryWeights(
                    totalCount = totalCount,
                    weights = clients.toList()
                )
            }
        }
    }

    suspend fun update(id: Long, weightGm: Int): Boolean = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(UPDATE, Statement.RETURN_GENERATED_KEYS).use { statement: PreparedStatement ->
                statement.setInt(1, weightGm)
                statement.setLong(2, id)
                val updatedCount = statement.executeUpdate()
                updatedCount > 0
            }
        }
    }

    suspend fun delete(id: Long) = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(DELETE).use { statement: PreparedStatement ->
                statement.setLong(1, id)
                val deleteCount = statement.executeUpdate()
                deleteCount > 0
            }
        }
    }
}