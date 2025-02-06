package routings.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class GetOtpResponse(
    @SerialName("user_id")
    val userId: Long
)