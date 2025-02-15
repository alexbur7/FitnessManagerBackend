package ru.alexbur.backend.history_weight

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.base.success.toSuccess
import ru.alexbur.backend.base.utils.DEFAULT_LIMIT
import ru.alexbur.backend.base.utils.DEFAULT_OFFSET
import ru.alexbur.backend.base.utils.getUserId
import ru.alexbur.backend.client_card.service.ClientsCardService
import ru.alexbur.backend.history_weight.mappers.HistoryWeightMapper
import ru.alexbur.backend.history_weight.models.request.HistoryWeightUpdateRequest
import ru.alexbur.backend.history_weight.models.response.HistoryWeightResponse
import ru.alexbur.backend.history_weight.models.response.HistoryWeightsResponse
import ru.alexbur.backend.history_weight.service.HistoryWeight
import ru.alexbur.backend.history_weight.service.HistoryWeightService

fun Application.configureHistoryWeightRouting(
    service: HistoryWeightService,
    clientsCardService: ClientsCardService,
    mapper: HistoryWeightMapper,
) {

    routing {
        authenticate("auth-jwt") {
            put("/history-weight/{id}") {
                val userId = call.getUserId() ?: return@put
                val id = call.parameters["id"]?.toLong()
                val historyWeight = getHistoryWeight(id, service, clientsCardService, userId) ?: return@put
                val request = call.receive<HistoryWeightUpdateRequest>()
                val isUpdated = service.update(historyWeight.id, request.weightGm)
                if (!isUpdated) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.DONT_EDITING_WEIGHT)
                    )
                    return@put
                }
                val response = mapper
                    .map(historyWeight.copy(weightGm = request.weightGm)).toSuccess(HistoryWeightResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            delete("/history-weight/{id}") {
                val userId = call.getUserId() ?: return@delete
                val id = call.parameters["id"]?.toLong()
                val historyWeight = getHistoryWeight(id, service, clientsCardService, userId) ?: return@delete
                val isDeleted = service.delete(historyWeight.id)
                if (!isDeleted) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.DONT_DELETED_WEIGHT)
                    )
                    return@delete
                }
                call.respond(HttpStatusCode.OK)
            }

            get("/history-weight/{clientCardId}") {
                val id = call.parameters["clientCardId"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@get
                }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: DEFAULT_OFFSET

                val data = service.getWeightByClientCardId(id, limit, offset)
                val response = mapper.map(data).toSuccess(HistoryWeightsResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}

private suspend fun RoutingContext.getHistoryWeight(
    id: Long?,
    service: HistoryWeightService,
    clientsCardService: ClientsCardService,
    userId: Long,
): HistoryWeight? {
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
        return null
    }
    val historyWeight = service.getById(id)
    if (historyWeight == null) {
        call.respond(
            HttpStatusCode.BadRequest,
            createBadRequestError(FitnessManagerErrors.UNKNOWN_HISTORY_WEIGHT)
        )
        return null
    }
    val clientCard = clientsCardService.getById(historyWeight.clientCardId, userId)
    if (clientCard == null) {
        call.respond(
            HttpStatusCode.BadRequest,
            createBadRequestError(FitnessManagerErrors.EDITING_IS_PROHIBITED)
        )
        return null
    }
    return historyWeight
}