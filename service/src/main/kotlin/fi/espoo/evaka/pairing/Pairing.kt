// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

data class Pairing(
    val id: UUID,
    val unitId: DaycareId,
    val challengeKey: String,
    val responseKey: String?,
    val expires: HelsinkiDateTime,
    val status: PairingStatus,
    val mobileDeviceId: MobileDeviceId? = null
)

enum class PairingStatus {
    WAITING_CHALLENGE, WAITING_RESPONSE, READY, PAIRED
}

data class MobileDevice(
    val id: MobileDeviceId,
    val name: String,
    val unitId: DaycareId
)

data class MobileDeviceIdentity(val id: MobileDeviceId, val longTermToken: UUID)
