package routings.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class LoginResponse(
    @SerialName("token")
    val token: String
)