package relationships.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CoachProfileResponse(
    @SerialName("coach_id") val coachId: Long,
    @SerialName("first_name") val firstName: String?,
    @SerialName("last_name") val lastName: String?,
)