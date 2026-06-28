package ru.alexbur.backend.profile.service

import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.utils.DispatcherProvider
import ru.alexbur.backend.profile.models.request.ProfileUpdateRequest
import ru.alexbur.backend.profile.models.response.ProfileResponse
import java.sql.Connection
import java.sql.Date
import java.sql.Types

internal class ProfileService(
    private val dispatcherProvider: DispatcherProvider,
    private val getConnection: () -> Connection,
) {

    companion object {
        private const val CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS PROFILE (" +
                "id SERIAL PRIMARY KEY, " +
                "user_id BIGINT NOT NULL UNIQUE, " +
                "email TEXT DEFAULT NULL, " +
                "phone CHAR(11) DEFAULT NULL, " +
                "first_name TEXT DEFAULT NULL, " +
                "second_name TEXT DEFAULT NULL, " +
                "date_birthday DATE DEFAULT NULL, " +
                "is_trainer BOOLEAN NOT NULL DEFAULT FALSE);"

        // is_trainer appears twice: COALESCE(?,FALSE) for INSERT default, COALESCE(?,PROFILE.is_trainer) for UPDATE preserve
        private const val UPSERT =
            "INSERT INTO PROFILE (user_id, email, phone, first_name, second_name, date_birthday, is_trainer) " +
                "VALUES (?, ?, ?, ?, ?, ?, COALESCE(?::boolean, FALSE)) " +
                "ON CONFLICT (user_id) DO UPDATE SET " +
                "email = COALESCE(EXCLUDED.email, PROFILE.email), " +
                "phone = COALESCE(EXCLUDED.phone, PROFILE.phone), " +
                "first_name = COALESCE(EXCLUDED.first_name, PROFILE.first_name), " +
                "second_name = COALESCE(EXCLUDED.second_name, PROFILE.second_name), " +
                "date_birthday = COALESCE(EXCLUDED.date_birthday, PROFILE.date_birthday), " +
                "is_trainer = COALESCE(?::boolean, PROFILE.is_trainer) " +
                "RETURNING user_id, email, phone, first_name, second_name, date_birthday, is_trainer;"
    }

    init {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(CREATE_TABLE)
            }
        }
    }

    suspend fun upsert(userId: Long, request: ProfileUpdateRequest): ProfileResponse? =
        withContext(dispatcherProvider.io()) {
            getConnection().use { connection ->
                connection.prepareStatement(UPSERT).use { statement ->
                    statement.setLong(1, userId)
                    statement.setString(2, request.email)
                    statement.setString(3, request.phone)
                    statement.setString(4, request.firstName)
                    statement.setString(5, request.secondName)
                    if (request.dateBirthday != null) {
                        statement.setDate(6, Date.valueOf(request.dateBirthday))
                    } else {
                        statement.setNull(6, Types.DATE)
                    }
                    if (request.isTrainer != null) {
                        statement.setBoolean(7, request.isTrainer)
                        statement.setBoolean(8, request.isTrainer)
                    } else {
                        statement.setNull(7, Types.BOOLEAN)
                        statement.setNull(8, Types.BOOLEAN)
                    }
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) {
                        ProfileResponse(
                            userId = resultSet.getLong("user_id"),
                            email = resultSet.getString("email"),
                            phone = resultSet.getString("phone"),
                            firstName = resultSet.getString("first_name"),
                            secondName = resultSet.getString("second_name"),
                            dateBirthday = resultSet.getDate("date_birthday")?.toString(),
                            isTrainer = resultSet.getBoolean("is_trainer"),
                        )
                    } else {
                        null
                    }
                }
            }
        }
}
