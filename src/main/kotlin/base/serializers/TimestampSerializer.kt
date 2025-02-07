@file:Suppress("EXTERNAL_SERIALIZER_USELESS")

package base.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Timestamp::class)
object TimestampSerializer : KSerializer<Timestamp> {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Timestamp) {
        encoder.encodeString(dateFormat.format(Date(value.time)))
    }

    override fun deserialize(decoder: Decoder): Timestamp {
        val dateString = decoder.decodeString()
        return Timestamp(dateFormat.parse(dateString).time)
    }
}