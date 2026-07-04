package relationships

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import relationships.models.response.ClientProfileResponse
import relationships.models.response.CoachProfileResponse
import relationships.models.response.RelationshipClientsResponse
import relationships.models.response.RelationshipCoachesResponse
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.base.success.toSuccess
import ru.alexbur.backend.base.utils.DEFAULT_LIMIT
import ru.alexbur.backend.base.utils.DEFAULT_OFFSET
import ru.alexbur.backend.base.utils.getUserId
import ru.alexbur.backend.profile.data.ProfileType
import ru.alexbur.backend.events.service.EventService
import ru.alexbur.backend.profile.service.ProfileService
import ru.alexbur.backend.relationships.service.RelationshipsService

internal fun Application.configureRelationshipsRouting(
    service: RelationshipsService,
    profileService: ProfileService,
    eventService: EventService,
) {
    routing {
        authenticate("auth-jwt") {
            get("/relationships/clients") {
                val coachId = call.getUserId() ?: return@get
                val typeProfile = profileService.getTypeProfile(coachId)
                if (typeProfile != ProfileType.COACH) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        createBadRequestError(FitnessManagerErrors.INVALID_PROFILE_TYPE_COACH)
                    )
                    return@get
                }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: DEFAULT_OFFSET
                val result = service.getClientsByCoachId(coachId = coachId, limit = limit, offset = offset)
                val clientIds = result.clients.map { it.clientId }
                val relationshipIds = result.clients.map { it.relationshipId }
                val profiles = profileService.getProfilesByUserIds(userIds = clientIds)
                val remainingCounts = eventService.getRemainingCountsByRelationshipIds(relationshipIds = relationshipIds)
                call.respond(
                    HttpStatusCode.OK,
                    RelationshipClientsResponse(
                        totalCount = result.totalCount,
                        clients = result.clients.map { client ->
                            val profile = profiles[client.clientId]
                            ClientProfileResponse(
                                relationshipId = client.relationshipId,
                                firstName = profile?.firstName,
                                lastName = profile?.lastName,
                                remainingCount = remainingCounts.getOrDefault(key = client.relationshipId, defaultValue = 0),
                            )
                        },
                    ).toSuccess(RelationshipClientsResponse.serializer())
                )
            }

            get("/relationships/coaches") {
                val userId = call.getUserId() ?: return@get
                val typeProfile = profileService.getTypeProfile(userId)
                if (typeProfile != ProfileType.CLIENT) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        createBadRequestError(FitnessManagerErrors.INVALID_PROFILE_TYPE)
                    )
                    return@get
                }
                val coaches = service.getCoachesByClientId(clientId = userId)
                val relationshipIds = coaches.map { it.relationshipId }
                val remainingCounts = eventService.getRemainingCountsByRelationshipIds(relationshipIds = relationshipIds)
                call.respond(
                    HttpStatusCode.OK,
                    RelationshipCoachesResponse(
                        coaches = coaches.map { coach ->
                            CoachProfileResponse(
                                relationshipId = coach.relationshipId,
                                firstName = coach.firstName,
                                lastName = coach.lastName,
                                remainingCount = remainingCounts.getOrDefault(key = coach.relationshipId, defaultValue = 0),
                            )
                        }
                    ).toSuccess(RelationshipCoachesResponse.serializer())
                )
            }
        }
    }
}
