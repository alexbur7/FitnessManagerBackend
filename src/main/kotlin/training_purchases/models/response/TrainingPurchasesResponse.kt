package training_purchases.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TrainingPurchasesResponse(
    @SerialName("purchases") val purchases: List<TrainingPurchaseResponse>,
)
