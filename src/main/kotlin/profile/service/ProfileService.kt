package ru.alexbur.backend.profile.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import ru.alexbur.backend.profile.data.ProfileType
import java.sql.Connection

internal data class ProfileNames(
    val firstName: String?,
    val lastName: String?,
)

internal data class ProfileTypeUpdate(
    val profileType: ProfileType,
)

internal class ProfileService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {

    companion object {
        private const val CREATE_TABLE = "CREATE TABLE IF NOT EXISTS PROFILE (" +
                "id SERIAL PRIMARY KEY, " +
                "user_id BIGINT NOT NULL UNIQUE, " +
                "first_name TEXT DEFAULT NULL, " +
                "second_name TEXT DEFAULT NULL, " +
                "date_birthday DATE DEFAULT NULL, " +
                "type_profile INT NOT NULL DEFAULT 0);"

        private const val UPDATE_TYPE = "UPDATE PROFILE SET type_profile = ? WHERE user_id = ?"
        private const val SELECT_TYPE = "SELECT type_profile FROM PROFILE WHERE user_id = ?;"
        private const val SELECT_BY_USER_IDS =
            "SELECT user_id, first_name, second_name FROM PROFILE WHERE user_id = ANY(?);"
    }

    init {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_TABLE)
            }
        }
    }

    suspend fun getTypeProfile(userId: Long): ProfileType = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(SELECT_TYPE).use { statement ->
                statement.setLong(1, userId)
                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    ProfileType.fromInt(resultSet.getInt("type_profile"))
                } else {
                    ProfileType.UNKNOWN
                }
            }
        }
    }

    suspend fun getProfilesByUserIds(userIds: List<Long>): Map<Long, ProfileNames> =
        withContext(dispatcherProvider.io()) {
            if (userIds.isEmpty()) return@withContext emptyMap()
            getConnection().use { connection ->
                val array = connection.createArrayOf("bigint", userIds.toTypedArray())
                connection.prepareStatement(SELECT_BY_USER_IDS).use { statement ->
                    statement.setArray(1, array)
                    val resultSet = statement.executeQuery()
                    val result = mutableMapOf<Long, ProfileNames>()
                    while (resultSet.next()) {
                        result[resultSet.getLong("user_id")] = ProfileNames(
                            firstName = resultSet.getString("first_name"),
                            lastName = resultSet.getString("second_name"),
                        )
                    }
                    result
                }
            }
        }

    suspend fun typeUpdate(userId: Long, data: ProfileTypeUpdate) = withContext(dispatcherProvider.io()) {
        getConnection().use { connection ->
            connection.prepareStatement(UPDATE_TYPE).use { statement ->
                statement.setInt(1, data.profileType.value)
                statement.setLong(2, userId)
                statement.executeUpdate()
            }
        }
    }
}
