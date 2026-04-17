// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pairing

import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.MobileDeviceId
import evaka.core.shared.PairingId
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.HelsinkiDateTime
import java.util.UUID

data class Pairing(
    val id: PairingId,
    val unitId: DaycareId? = null,
    val employeeId: EmployeeId? = null,
    val challengeKey: String,
    val responseKey: String?,
    val expires: HelsinkiDateTime,
    val status: PairingStatus,
    val mobileDeviceId: MobileDeviceId? = null,
)

enum class PairingStatus : DatabaseEnum {
    WAITING_CHALLENGE,
    WAITING_RESPONSE,
    READY,
    PAIRED;

    override val sqlType: String = "pairing_status"
}

data class MobileDeviceDetails(
    val id: MobileDeviceId,
    val name: String,
    val unitIds: List<DaycareId>,
    val employeeId: EmployeeId?,
    val personalDevice: Boolean,
    val pushApplicationServerKey: String? = null,
)

data class MobileDevice(val id: MobileDeviceId, val name: String)

data class MobileDeviceIdentity(val id: MobileDeviceId, val longTermToken: UUID)
