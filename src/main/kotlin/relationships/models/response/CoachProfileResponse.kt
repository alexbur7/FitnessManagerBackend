package relationships.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CoachProfileResponse(
    @SerialName("relationship_id") val relationshipId: Long,
    @SerialName("first_name") val firstName: String?,
    @SerialName("last_name") val lastName: String?,
    @SerialName("remaining_count") val remainingCount: Int,
)
