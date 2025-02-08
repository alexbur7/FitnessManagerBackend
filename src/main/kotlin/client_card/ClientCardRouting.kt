package ru.alexbur.backend.client_card

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
import ru.alexbur.backend.client_card.models.request.ClientCardCreateRequest
import ru.alexbur.backend.client_card.models.response.ClientCardFullResponse
import ru.alexbur.backend.client_card.models.response.ClientsCardResponse
import ru.alexbur.backend.client_card.service.ClientsCardService
import ru.alexbur.backend.db.getConnection
import ru.alexbur.backend.di.BaseModule
import ru.alexbur.backend.di.MappersModule

fun Application.configureClientCardRouting() {

    val mapper = MappersModule.provideClientCardMapper()
    val service = ClientsCardService(BaseModule.dispatcherProvider) { getConnection(embedded = false) }
    routing {
        authenticate("auth-jwt") {
            post("/client-card/create") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<ClientCardCreateRequest>()

                val id = service.create(mapper.map(request, userId))
                val data = service.readById(id, userId)
                if (data == null) {
                    call.respond(
                        HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_CLIENT_CARD)
                    )
                    return@post
                }
                val response = mapper.map(data).toSuccess(ClientCardFullResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            get("/client-card") {
                val userId = call.getUserId() ?: return@get
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: DEFAULT_OFFSET

                val data = service.getClientsByCoachId(userId, limit, offset)
                val response = mapper.map(data).toSuccess(ClientsCardResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            get("/client-card/{id}") {
                val userId = call.getUserId() ?: return@get
                val id = call.parameters["id"]?.toLong()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_ID))
                    return@get
                }

                val data = service.readById(id, userId)
                if (data == null) {
                    call.respond(
                        HttpStatusCode.BadRequest, createBadRequestError(FitnessManagerErrors.UNKNOWN_CLIENT_CARD)
                    )
                    return@get
                }
                val response = mapper.map(data).toSuccess(ClientCardFullResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}