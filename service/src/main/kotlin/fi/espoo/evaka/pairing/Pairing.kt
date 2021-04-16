// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

data class Pairing(
    val id: UUID,
    val unitId: UUID,
    val challengeKey: String,
    val responseKey: String?,
    val expires: HelsinkiDateTime,
    val status: PairingStatus,
    val mobileDeviceId: UUID? = null
)

enum class PairingStatus {
    WAITING_CHALLENGE, WAITING_RESPONSE, READY, PAIRED
}

data class MobileDevice(
    val id: UUID,
    val name: String,
    val unitId: UUID
)

data class MobileDeviceIdentity(val id: UUID, val longTermToken: UUID)
