package fi.espoo.evaka.pairing

import java.time.Instant
import java.util.UUID

data class Pairing(
    val id: UUID,
    val unitId: UUID,
    val challengeKey: String,
    val responseKey: String?,
    val expires: Instant,
    val status: PairingStatus
)

enum class PairingStatus {
    WAITING_CHALLENGE, WAITING_RESPONSE, READY, PAIRED
}
