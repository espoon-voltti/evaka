// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.db.DatabaseEnum

@ConstList("pilotFeatures")
enum class PilotFeature : DatabaseEnum {
    MESSAGING,
    MOBILE,
    RESERVATIONS,
    VASU_AND_PEDADOC,
    MOBILE_MESSAGING,
    PLACEMENT_TERMINATION,
    REALTIME_STAFF_ATTENDANCE,
    PUSH_NOTIFICATIONS,
    SERVICE_APPLICATIONS;

    override val sqlType: String = "pilot_feature"
}
