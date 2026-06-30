package ru.alexbur.backend.training_purchases

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
import ru.alexbur.backend.profile.data.ProfileType
import ru.alexbur.backend.profile.service.ProfileService
import ru.alexbur.backend.relationships.service.RelationshipsService
import training_purchases.models.request.TrainingPurchaseCreateRequest
import training_purchases.models.response.TrainingPurchaseResponse
import training_purchases.models.response.TrainingPurchasesResponse
import training_purchases.service.TrainingPurchaseCreate
import training_purchases.service.TrainingPurchasesService

internal fun Application.configureTrainingPurchasesRouting(
    service: TrainingPurchasesService,
    relationshipsService: RelationshipsService,
    profileService: ProfileService,
) {
    routing {
        authenticate("auth-jwt") {
            post("/training-purchases/create") {
                val userId = call.getUserId() ?: return@post
                val typeProfile = profileService.getTypeProfile(userId)
                if (typeProfile != ProfileType.COACH) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        createBadRequestError(FitnessManagerErrors.INVALID_PROFILE_TYPE_COACH)
                    )
                    return@post
                }
                val request = call.receive<TrainingPurchaseCreateRequest>()
                if (!relationshipsService.isCoachOfRelationship(userId, request.relationshipsId)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        createBadRequestError(FitnessManagerErrors.UNKNOWN_CLIENT_CARD)
                    )
                    return@post
                }
                val purchase = service.create(
                    TrainingPurchaseCreate(
                        relationshipsId = request.relationshipsId,
                        eventCounts = request.eventCounts,
                    )
                )
                val response = TrainingPurchaseResponse(
                    id = purchase.id,
                    relationshipsId = purchase.relationshipsId,
                    eventCounts = purchase.eventCounts,
                    purchaseDate = purchase.purchaseDate,
                ).toSuccess(TrainingPurchaseResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            get("/training-purchases/{relationshipsId}") {
                val userId = call.getUserId() ?: return@get
                val typeProfile = profileService.getTypeProfile(userId)
                if (typeProfile != ProfileType.COACH) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        createBadRequestError(FitnessManagerErrors.INVALID_PROFILE_TYPE_COACH)
                    )
                    return@get
                }
                val relationshipsId = call.parameters["relationshipsId"]?.toLongOrNull()
                if (relationshipsId == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.UNKNOWN_ID)
                    )
                    return@get
                }
                if (!relationshipsService.isParticipantOfRelationship(userId, relationshipsId)) {
                    call.respond(
                        HttpStatusCode.Forbidden,
                        createBadRequestError(FitnessManagerErrors.UNKNOWN_CLIENT_CARD)
                    )
                    return@get
                }
                val purchases = service.getByRelationshipId(relationshipsId)
                val response = TrainingPurchasesResponse(
                    purchases = purchases.map { purchase ->
                        TrainingPurchaseResponse(
                            id = purchase.id,
                            relationshipsId = purchase.relationshipsId,
                            eventCounts = purchase.eventCounts,
                            purchaseDate = purchase.purchaseDate,
                        )
                    }
                ).toSuccess(TrainingPurchasesResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
