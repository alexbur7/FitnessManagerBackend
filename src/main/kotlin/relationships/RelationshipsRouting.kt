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
import ru.alexbur.backend.profile.service.ProfileService
import ru.alexbur.backend.relationships.service.RelationshipsService

internal fun Application.configureRelationshipsRouting(service: RelationshipsService, profileService: ProfileService) {
    routing {
        authenticate("auth-jwt") {
            get("/relationships/clients") {
                val userId = call.getUserId() ?: return@get
                val typeProfile = profileService.getTypeProfile(userId)
                if (typeProfile != ProfileType.COACH) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        createBadRequestError(FitnessManagerErrors.INVALID_PROFILE_TYPE_COACH)
                    )
                    return@get
                }
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: DEFAULT_OFFSET
                val result = service.getClientsByCoachId(userId, limit, offset)
                call.respond(
                    HttpStatusCode.OK,
                    RelationshipClientsResponse(
                        totalCount = result.totalCount,
                        clients = result.clients.map { profile ->
                            ClientProfileResponse(
                                clientId = profile.clientId,
                                firstName = profile.firstName,
                                lastName = profile.lastName,
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
                val coaches = service.getCoachesByClientId(userId)
                call.respond(
                    HttpStatusCode.OK,
                    RelationshipCoachesResponse(
                        coaches = coaches.map { coach ->
                            CoachProfileResponse(
                                coachId = coach.coachId,
                                firstName = coach.firstName,
                                lastName = coach.lastName,
                            )
                        }
                    ).toSuccess(RelationshipCoachesResponse.serializer())
                )
            }
        }
    }
}
