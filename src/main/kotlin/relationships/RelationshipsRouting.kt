package ru.alexbur.backend.relationships

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.alexbur.backend.base.success.toSuccess
import ru.alexbur.backend.base.utils.DEFAULT_LIMIT
import ru.alexbur.backend.base.utils.DEFAULT_OFFSET
import ru.alexbur.backend.base.utils.getUserId
import ru.alexbur.backend.relationships.models.response.RelationshipClientsResponse
import ru.alexbur.backend.relationships.models.response.RelationshipIdsResponse
import ru.alexbur.backend.relationships.service.RelationshipsService

internal fun Application.configureRelationshipsRouting(service: RelationshipsService) {
    routing {
        authenticate("auth-jwt") {
            get("/relationships/clients") {
                val userId = call.getUserId() ?: return@get
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: DEFAULT_OFFSET
                val result = service.getClientsByCoachId(userId, limit, offset)
                call.respond(
                    HttpStatusCode.OK,
                    RelationshipClientsResponse(
                        totalCount = result.totalCount,
                        ids = result.clientIds,
                    ).toSuccess(RelationshipClientsResponse.serializer())
                )
            }

            get("/relationships/coaches") {
                val userId = call.getUserId() ?: return@get
                val ids = service.getCoachesByClientId(userId)
                call.respond(HttpStatusCode.OK, RelationshipIdsResponse(ids).toSuccess(RelationshipIdsResponse.serializer()))
            }
        }
    }
}
