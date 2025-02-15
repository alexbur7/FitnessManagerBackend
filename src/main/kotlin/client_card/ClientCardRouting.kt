package ru.alexbur.backend.client_card

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.withContext
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.base.sql.transaction
import ru.alexbur.backend.base.success.toSuccess
import ru.alexbur.backend.base.utils.*
import ru.alexbur.backend.client_card.mapper.ClientCardMapper
import ru.alexbur.backend.client_card.models.request.ClientCardCreateRequest
import ru.alexbur.backend.client_card.models.response.ClientCardFullResponse
import ru.alexbur.backend.client_card.models.response.ClientsCardResponse
import ru.alexbur.backend.client_card.service.ClientCardFull
import ru.alexbur.backend.client_card.service.ClientsCardService
import ru.alexbur.backend.history_weight.service.HistoryWeightCreate
import ru.alexbur.backend.history_weight.service.HistoryWeightService

fun Application.configureClientCardRouting(
    service: ClientsCardService,
    mapper: ClientCardMapper,
    historyWeightService: HistoryWeightService,
    dispatcherProvider: DispatcherProvider,
) {
    routing {
        authenticate("auth-jwt") {
            post("/client-card/create") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<ClientCardCreateRequest>()

                var id = 0L
                withContext(dispatcherProvider.io()) {
                    transaction { connection ->
                        id = service.create(mapper.map(request, userId), connection)
                        if (request.weightGm != null) {
                            historyWeightService.create(
                                HistoryWeightCreate(
                                    weightGm = request.weightGm,
                                    clientCardId = id,
                                    date = getCurrentTimestamp()
                                ),
                                connection
                            )
                        }
                    }
                }
                val data = getClientCard(service, id, userId) ?: return@post
                val response = mapper.map(data).toSuccess(ClientCardFullResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            get("/client-card/{id}") {
                val userId = call.getUserId() ?: return@get
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@get
                }

                val data = getClientCard(service, id, userId) ?: return@get
                val response = mapper.map(data).toSuccess(ClientCardFullResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            put("/client-card/{id}") {
                val userId = call.getUserId() ?: return@put
                val request = call.receive<ClientCardCreateRequest>()
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@put
                }
                var isUpdate = false
                withContext(dispatcherProvider.io()) {
                    transaction { connection ->
                        isUpdate = service.update(id, mapper.map(request, userId), connection)
                        if (request.weightGm != null) {
                            historyWeightService.create(
                                HistoryWeightCreate(
                                    weightGm = request.weightGm,
                                    clientCardId = id,
                                    date = getCurrentTimestamp()
                                ),
                                connection
                            )
                        }
                    }
                }
                if (!isUpdate) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.ERROR_UPDATE))
                    return@put
                }
                val data = getClientCard(service, id, userId) ?: return@put
                val response = mapper.map(data).toSuccess(ClientCardFullResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            delete("/client-card/{id}") {
                val userId = call.getUserId() ?: return@delete
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@delete
                }
                val isDeleted = service.delete(id, userId)
                if (!isDeleted) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.ERROR_DELETE))
                    return@delete
                }
                call.respond(HttpStatusCode.OK)
            }

            get("/clients-card") {
                val userId = call.getUserId() ?: return@get
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: DEFAULT_OFFSET

                val data = service.getClientsByCoachId(userId, limit, offset)
                val response = mapper.map(data).toSuccess(ClientsCardResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}

suspend fun RoutingContext.checkClientCard(
    service: ClientsCardService,
    clientCardId: Long,
    userId: Long
): Boolean {
    val clientCard = service.getById(clientCardId, userId)
    if (clientCard == null) {
        call.respond(
            HttpStatusCode.BadRequest,
            createBadRequestError(FitnessManagerErrors.UNKNOWN_CLIENT_CARD)
        )
        return true
    }
    return false
}

private suspend fun RoutingContext.getClientCard(
    service: ClientsCardService,
    id: Long,
    userId: Long
): ClientCardFull? {
    val data = service.getById(id, userId)
    if (data == null) {
        call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_CLIENT_CARD))
        return null
    }
    return data
}