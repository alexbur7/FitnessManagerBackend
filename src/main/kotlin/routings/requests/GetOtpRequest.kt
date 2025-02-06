package routings.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class GetOtpRequest(
    @SerialName("phone_number")
    val phoneNumber: String
)