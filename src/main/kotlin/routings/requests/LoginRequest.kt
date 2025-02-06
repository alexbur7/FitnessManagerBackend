package routings.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class LoginRequest(
    @SerialName("user_id")
    val userId: Long,
    @SerialName("otp")
    val otp: String
)