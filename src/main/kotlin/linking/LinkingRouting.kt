package ru.alexbur.backend.linking

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.withContext
import ru.alexbur.backend.auth.service.UserInfo
import ru.alexbur.backend.auth.service.UserService
import ru.alexbur.backend.base.errors.FitnessManagerErrors
import ru.alexbur.backend.base.errors.createBadRequestError
import ru.alexbur.backend.base.success.toSuccess
import ru.alexbur.backend.base.utils.DispatcherProvider
import ru.alexbur.backend.base.utils.compareTimeWithCurrent
import ru.alexbur.backend.base.utils.getCurrentTimestamp
import ru.alexbur.backend.base.utils.getUserId
import ru.alexbur.backend.db.getConnection
import ru.alexbur.backend.linking.models.request.LinkingConnectRequest
import ru.alexbur.backend.linking.models.request.LinkingCreateRequest
import ru.alexbur.backend.linking.models.response.LinkingInfoResponse
import ru.alexbur.backend.linking.service.LinkingCreate
import ru.alexbur.backend.linking.service.LinkingInfo
import ru.alexbur.backend.linking.service.LinkingService
import ru.alexbur.backend.relationships.service.RelationshipCreate
import ru.alexbur.backend.relationships.service.RelationshipsService
import java.security.SecureRandom

internal const val LINKING_CODE_LENGTH = 8
private const val EXPIRED_LINKING_CODE_HOUR = 24L

internal fun Application.configureLinkingRouting(
    linkingService: LinkingService,
    relationshipsService: RelationshipsService,
    userService: UserService,
    dispatcherProvider: DispatcherProvider
) {
    routing {
        authenticate("auth-jwt") {
            post("/linking/generate-code") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<LinkingCreateRequest>()

                val linkingInfo = linkingService.readByCoachId(
                    coachId = userId,
                    clientId = request.clientId
                )

                // если код уже есть для данной связки тренера и клиента, и он не протух, то возвращаем его
                if (linkingInfo != null && compareTimeWithCurrent(linkingInfo.createdDate, EXPIRED_LINKING_CODE_HOUR)) {
                    val response = LinkingInfoResponse(linkingInfo.code).toSuccess(LinkingInfoResponse.serializer())
                    call.respond(HttpStatusCode.OK, response)
                    return@post
                }

                val code = generateAndStoreUniqueCode(
                    coachId = userId,
                    clientId = request.clientId,
                    service = linkingService
                )

                val response = LinkingInfoResponse(code).toSuccess(LinkingInfoResponse.serializer())
                call.respond(HttpStatusCode.OK, response)
            }

            post("/linking/bind") {
                val userId = call.getUserId() ?: return@post
                val request = call.receive<LinkingConnectRequest>()

                val linkingInfo = linkingService.readByCode(request.code)
                if (linkingInfo == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.UNKNOWN_LINKING_CODE)
                    )
                    return@post
                }

                if (!compareTimeWithCurrent(linkingInfo.createdDate, EXPIRED_LINKING_CODE_HOUR)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.EXPIRED_LINKING_CODE)
                    )
                    return@post
                }
                val userInfo = userService.read(userId)
                if (userInfo == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        createBadRequestError(FitnessManagerErrors.UNKNOWN_USER)
                    )
                    return@post
                }
                bind(relationshipsService, linkingInfo, userInfo, linkingService, dispatcherProvider)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private suspend fun Application.bind(
    relationshipsService: RelationshipsService,
    linkingInfo: LinkingInfo,
    userInfo: UserInfo,
    linkingService: LinkingService,
    dispatcherProvider: DispatcherProvider,
) {
    relationshipsService.create(
        RelationshipCreate(
            coachId = linkingInfo.coachId,
            clientId = userInfo.userId,
        )
    )
    withContext(dispatcherProvider.io()) {
        getConnection(embedded = false).use { connection ->
            linkingService.delete(linkingInfo.id, connection)
        }
    }
}

private suspend fun generateAndStoreUniqueCode(coachId: Long, clientId: Long, service: LinkingService): String {
    var code: String
    do {
        code = generateUniqueCode()
    } while (service.readByCode(code) != null)
    service.create(
        LinkingCreate(
            coachId = coachId,
            clientId = clientId,
            code = code,
            createdDate = getCurrentTimestamp()
        )
    )
    return code
}

private fun generateUniqueCode(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val secureRandom = SecureRandom()
    return (1..LINKING_CODE_LENGTH)
        .map { chars[secureRandom.nextInt(chars.length)] }
        .joinToString("")
}
