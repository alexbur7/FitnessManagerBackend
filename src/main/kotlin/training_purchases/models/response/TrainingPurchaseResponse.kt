package training_purchases.models.response

import base.serializers.TimestampSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.Timestamp

@Serializable
internal data class TrainingPurchaseResponse(
    @SerialName("id") val id: Long,
    @SerialName("relationships_id") val relationshipsId: Long,
    @SerialName("event_counts") val eventCounts: Int,
    @SerialName("purchase_date") @Serializable(with = TimestampSerializer::class) val purchaseDate: Timestamp,
)
