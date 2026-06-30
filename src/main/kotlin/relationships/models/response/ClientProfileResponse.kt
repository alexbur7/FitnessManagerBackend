package relationships.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ClientProfileResponse(
    @SerialName("client_id") val clientId: Long,
    @SerialName("first_name") val firstName: String?,
    @SerialName("last_name") val lastName: String?,
)