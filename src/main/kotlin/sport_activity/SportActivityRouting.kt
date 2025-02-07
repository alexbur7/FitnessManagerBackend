package ru.alexbur.backend.sport_activity

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.base.success.toSuccess
import ru.alexbur.backend.di.BaseModule
import ru.alexbur.backend.sport_activity.mapper.SportActivityMapper
import ru.alexbur.backend.sport_activity.models.request.SportActivityCreateRequest
import ru.alexbur.backend.sport_activity.models.request.SportActivityGetByTimeRequest
import ru.alexbur.backend.sport_activity.models.response.SportActivityByTimeResponse
import ru.alexbur.backend.sport_activity.models.response.SportActivityResponse
import ru.alexbur.backend.sport_activity.service.CreateSportActivity
import ru.alexbur.backend.sport_activity.service.SportActivityService
import ru.alexbur.backend.utils.getUserId
import java.sql.Connection
import java.sql.Timestamp

fun Application.configureCalendarRouting(
    dbConnection: Connection,
    mapper: SportActivityMapper
) {
    val sportActivityService = SportActivityService(dbConnection, BaseModule.dispatcherProvider)

    routing {
        authenticate("auth-jwt") {
            post("/sport-activity/get-by-time") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<SportActivityGetByTimeRequest>()
                val result = sportActivityService.readByTime(
                    userId = userId,
                    startTime = request.startTime,
                    endTime = request.endTime
                )
                val response = SportActivityByTimeResponse(
                    activities = result.map { data -> mapper.map(data) }
                ).toSuccess(SportActivityByTimeResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            get("/sport-activity/{id}") {
                val userId = call.getUserId() ?: return@get
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@get
                }
                val data = sportActivityService.readById(id, userId)
                if (data == null) {
                    call.respond(
                        HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_SPORT_ACTIVITY)
                    )
                    return@get
                }
                val response = mapper.map(data).toSuccess(SportActivityResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            post("/sport-activity/create") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<SportActivityCreateRequest>()
                sportActivityService.create(
                    CreateSportActivity(
                        userId = userId,
                        name = request.name,
                        startTime = Timestamp.valueOf(request.startTime),
                        endTime = Timestamp.valueOf(request.endTime),
                        comment = request.comment,
                        clientCardId = request.clientCardId
                    )
                )
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}