package ru.alexbur.backend.client_card.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import ru.alexbur.backend.base.utils.getIntOrNull
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.sql.Types

data class ClientCardCreate(
    val name: String,
    val age: Int?,
    val weightGm: Int?,
    val phone: String?,
    val coachId: Long,
)

data class ClientCard(
    val id: Long,
    val name: String,
    val photoUrl: String?,
)

data class ClientCardFull(
    val id: Long,
    val name: String,
    val photoUrl: String?,
    val age: Int?,
    val weightGm: Int?,
    val phone: String?
)

data class ClientsCard(
    val totalCount: Int,
    val clients: List<ClientCard>,
)

class ClientsCardService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {
    private companion object {
        const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS ClientsCard (id SERIAL PRIMARY KEY, coach_id INT NOT NULL, " +
                    "user_id INT DEFAULT NULL, name VARCHAR(255) NOT NULL, photo_url TEXT DEFAULT NULL, " +
                    "age INT DEFAULT NULL, weight_gm INT DEFAULT NULL, phone CHAR(11) DEFAULT NULL);"
        const val INSERT = "INSERT INTO ClientsCard (coach_id, name, age, weight_gm, phone) VALUES (?, ?, ?, ?, ?);"
        const val SELECT_BY_ID = "SELECT * FROM ClientsCard WHERE id = ? AND coach_id = ?;"
    }

    init {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_TABLE)
            }
        }
    }

    suspend fun create(clientCard: ClientCardCreate): Long = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS).use { statement: PreparedStatement ->
                statement.setLong(1, clientCard.coachId)
                statement.setString(2, clientCard.name)
                if (clientCard.age == null) {
                    statement.setNull(3, Types.INTEGER)
                } else {
                    statement.setInt(3, clientCard.age)
                }
                if (clientCard.weightGm == null) {
                    statement.setNull(4, Types.INTEGER)
                } else {
                    statement.setInt(4, clientCard.weightGm)
                }
                if (clientCard.phone == null) {
                    statement.setNull(5, Types.VARCHAR)
                } else {
                    statement.setString(5, clientCard.phone)
                }
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

    suspend fun readById(id: Long, coachId: Long): ClientCardFull? = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_BY_ID).use { statement: PreparedStatement ->
                statement.setLong(1, id)
                statement.setLong(2, coachId)
                val resultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val name = resultSet.getString("name")
                    val photoUrl = resultSet.getString("photo_url")
                    val age = resultSet.getInt("age")
                    val weight = resultSet.getIntOrNull("weight_gm")
                    val phone = resultSet.getString("phone")

                    ClientCardFull(
                        id = id,
                        name = name,
                        photoUrl = photoUrl,
                        age = age,
                        weightGm = weight,
                        phone = phone
                    )
                } else {
                    null
                }
            }
        }
    }

    suspend fun getClientsByCoachId(
        coachId: Long,
        limit: Int,
        offset: Int
    ): ClientsCard = withContext(dispatcherProvider.io()) {
        val sql = """
        WITH filtered_clients AS (
            SELECT id, name, photo_url 
            FROM ClientsCard 
            WHERE coach_id = $coachId
        )
        SELECT 
            (SELECT COUNT(*) FROM filtered_clients) AS total_count, 
            id, 
            name, 
            photo_url
        FROM filtered_clients
        ORDER BY id ASC
        LIMIT $limit OFFSET $offset;
    """.trimIndent()

        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(sql)

                val clients = mutableListOf<ClientCard>()
                var totalCount = 0

                while (resultSet.next()) {
                    if (totalCount == 0) {
                        totalCount = resultSet.getInt("total_count")
                    }
                    clients.add(
                        ClientCard(
                            id = resultSet.getLong("id"),
                            name = resultSet.getString("name"),
                            photoUrl = resultSet.getString("photo_url")
                        )
                    )
                }

                ClientsCard(
                    totalCount = totalCount,
                    clients = clients.toList()
                )
            }
        }
    }
}