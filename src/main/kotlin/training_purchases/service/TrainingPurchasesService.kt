package training_purchases.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import java.sql.Connection
import java.sql.Timestamp

internal data class TrainingPurchaseCreate(
    val relationshipsId: Long,
    val eventCounts: Int,
)

internal data class TrainingPurchase(
    val id: Long,
    val relationshipsId: Long,
    val eventCounts: Int,
    val purchaseDate: Timestamp,
)

internal class TrainingPurchasesService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {

    companion object {
        private const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS TRAINING_PURCHASES (" +
                "id SERIAL PRIMARY KEY, " +
                "relationships_id BIGINT NOT NULL, " +
                "event_counts INT NOT NULL, " +
                "purchase_date TIMESTAMP NOT NULL DEFAULT NOW());"

        private const val INSERT =
            "INSERT INTO TRAINING_PURCHASES (relationships_id, event_counts) VALUES (?, ?) RETURNING id, purchase_date;"

        private const val SELECT_BY_RELATIONSHIP =
            "SELECT * FROM TRAINING_PURCHASES WHERE relationships_id = ? ORDER BY purchase_date DESC;"
    }

    init {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_TABLE)
            }
        }
    }

    suspend fun create(data: TrainingPurchaseCreate): TrainingPurchase = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(INSERT).use { statement ->
                statement.setLong(1, data.relationshipsId)
                statement.setInt(2, data.eventCounts)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    TrainingPurchase(
                        id = resultSet.getLong("id"),
                        relationshipsId = data.relationshipsId,
                        eventCounts = data.eventCounts,
                        purchaseDate = resultSet.getTimestamp("purchase_date"),
                    )
                } else {
                    throw IllegalStateException("INSERT INTO TRAINING_PURCHASES returned no rows")
                }
            }
        }
    }

    suspend fun getByRelationshipId(relationshipsId: Long): List<TrainingPurchase> =
        withContext(dispatcherProvider.io()) {
            getConnection().use { connection ->
                connection.prepareStatement(SELECT_BY_RELATIONSHIP).use { statement ->
                    statement.setLong(1, relationshipsId)
                    val resultSet = statement.executeQuery()
                    val purchases = mutableListOf<TrainingPurchase>()
                    while (resultSet.next()) {
                        purchases.add(
                            TrainingPurchase(
                                id = resultSet.getLong("id"),
                                relationshipsId = resultSet.getLong("relationships_id"),
                                eventCounts = resultSet.getInt("event_counts"),
                                purchaseDate = resultSet.getTimestamp("purchase_date"),
                            )
                        )
                    }
                    purchases.toList()
                }
            }
        }
}
