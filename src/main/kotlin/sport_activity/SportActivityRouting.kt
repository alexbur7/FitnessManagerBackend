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
import ru.alexbur.backend.base.utils.getUserId
import ru.alexbur.backend.client_card.service.ClientsCardService
import ru.alexbur.backend.db.getConnection
import ru.alexbur.backend.di.BaseModule
import ru.alexbur.backend.sport_activity.mapper.SportActivityMapper
import ru.alexbur.backend.sport_activity.models.request.SportActivityCreateRequest
import ru.alexbur.backend.sport_activity.models.request.SportActivityGetByTimeRequest
import ru.alexbur.backend.sport_activity.models.response.SportActivityByTimeResponse
import ru.alexbur.backend.sport_activity.models.response.SportActivityResponse
import ru.alexbur.backend.sport_activity.service.SportActivity
import ru.alexbur.backend.sport_activity.service.SportActivityService

fun Application.configureSportActivityRouting(
    mapper: SportActivityMapper
) {
    val sportActivityService = SportActivityService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    val clientCardService = ClientsCardService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }

    routing {
        authenticate("auth-jwt") {
            post("/sport-activity/create") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<SportActivityCreateRequest>()
                if (request.startTime >= request.endTime) {
                    call.respond(
                        HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.INVALID_END_TIME)
                    )
                    return@post
                }
                val clientCard = clientCardService.readById(request.clientCardId, userId)
                if (clientCard == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.UNKNOWN_CLIENT_CARD)
                    )
                    return@post
                }
                val hasActivities = sportActivityService.hasActivities(
                    userId = userId,
                    startTime = request.startTime,
                    endTime = request.endTime,
                    clientCardId = request.clientCardId
                )
                if (hasActivities) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.ERROR_CREATE_SPORT_ACTIVITY)
                    )
                    return@post
                }

                val id = sportActivityService.create(mapper.map(request, userId))
                val data = getSportActivity(sportActivityService, id, userId) ?: return@post
                sendSportActivityResponse(mapper, data)
            }

            post("/sport-activity/get-by-time") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<SportActivityGetByTimeRequest>()
                val result = sportActivityService.readByTime(
                    userId = userId,
                    startTime = request.startTime,
                    endTime = request.endTime,
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
                val data = getSportActivity(sportActivityService, id, userId) ?: return@get
                sendSportActivityResponse(mapper, data)
            }

            put("/sport-activity/{id}") {
                val userId = call.getUserId() ?: return@put
                val request = call.receive<SportActivityCreateRequest>()
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@put
                }
                val isUpdate = sportActivityService.update(id, mapper.map(request, userId))
                if (!isUpdate) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.ERROR_UPDATE))
                    return@put
                }
                val data = getSportActivity(sportActivityService, id, userId) ?: return@put
                sendSportActivityResponse(mapper, data)
            }

            delete("/sport-activity/{id}") {
                val userId = call.getUserId() ?: return@delete
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@delete
                }
                val isDeleted = sportActivityService.delete(id, userId)
                if (!isDeleted) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.ERROR_DELETE))
                    return@delete
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private suspend fun RoutingContext.sendSportActivityResponse(mapper: SportActivityMapper, data: SportActivity) {
    val response = mapper.map(data).toSuccess(SportActivityResponse.serializer())
    call.respond(HttpStatusCode.OK, response)
}

private suspend fun RoutingContext.getSportActivity(
    service: SportActivityService,
    id: Long,
    userId: Long
): SportActivity? {
    val data = service.readById(id, userId)
    if (data == null) {
        call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_SPORT_ACTIVITY))
        return null
    }
    return data
}