package relationships.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RelationshipCoachesResponse(
    @SerialName("coaches") val coaches: List<CoachProfileResponse>,
)
