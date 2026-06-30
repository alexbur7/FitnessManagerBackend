package training_purchases.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class TrainingPurchaseCreateRequest(
    @SerialName("relationships_id") val relationshipsId: Long,
    @SerialName("event_counts") val eventCounts: Int,
)
