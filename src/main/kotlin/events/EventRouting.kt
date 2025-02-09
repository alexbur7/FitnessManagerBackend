package ru.alexbur.backend.events

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
import ru.alexbur.backend.client_card.checkClientCard
import ru.alexbur.backend.client_card.service.ClientsCardService
import ru.alexbur.backend.events.mapper.EventMapper
import ru.alexbur.backend.events.models.request.EventCreateRequest
import ru.alexbur.backend.events.models.request.EventGetByTimeRequest
import ru.alexbur.backend.events.models.response.EventByTimeResponse
import ru.alexbur.backend.events.models.response.EventResponse
import ru.alexbur.backend.events.service.Event
import ru.alexbur.backend.events.service.EventService

fun Application.configureEventRouting(
    mapper: EventMapper,
    clientCardService: ClientsCardService,
    eventService: EventService
) {
    routing {
        authenticate("auth-jwt") {
            post("/event/create") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<EventCreateRequest>()
                if (request.startTime >= request.endTime) {
                    call.respond(
                        HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.INVALID_END_TIME)
                    )
                    return@post
                }
                if (checkClientCard(clientCardService, request.clientCardId, userId)) return@post
                val hasActivities = eventService.hasActivities(
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

                val id = eventService.create(mapper.map(request, userId))
                val data = getEvent(eventService, id, userId) ?: return@post
                sendEventResponse(mapper, data)
            }

            post("/event/get-by-time") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<EventGetByTimeRequest>()
                val result = eventService.readByTime(
                    userId = userId,
                    startTime = request.startTime,
                    endTime = request.endTime,
                )
                val response = EventByTimeResponse(
                    activities = result.map { data -> mapper.map(data) }
                ).toSuccess(EventByTimeResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            get("/event/{id}") {
                val userId = call.getUserId() ?: return@get
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@get
                }
                val data = getEvent(eventService, id, userId) ?: return@get
                sendEventResponse(mapper, data)
            }

            put("/event/{id}") {
                val userId = call.getUserId() ?: return@put
                val request = call.receive<EventCreateRequest>()
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@put
                }
                val isUpdate = eventService.update(id, mapper.map(request, userId))
                if (!isUpdate) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.ERROR_UPDATE))
                    return@put
                }
                val data = getEvent(eventService, id, userId) ?: return@put
                sendEventResponse(mapper, data)
            }

            delete("/event/{id}") {
                val userId = call.getUserId() ?: return@delete
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@delete
                }
                val isDeleted = eventService.delete(id, userId)
                if (!isDeleted) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.ERROR_DELETE))
                    return@delete
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private suspend fun RoutingContext.sendEventResponse(mapper: EventMapper, data: Event) {
    val response = mapper.map(data).toSuccess(EventResponse.serializer())
    call.respond(HttpStatusCode.OK, response)
}

private suspend fun RoutingContext.getEvent(
    service: EventService,
    id: Long,
    userId: Long
): Event? {
    val data = service.readById(id, userId)
    if (data == null) {
        call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_EVENT))
        return null
    }
    return data
}