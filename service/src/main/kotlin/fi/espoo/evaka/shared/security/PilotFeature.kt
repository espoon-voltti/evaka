// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.ConstList

@ConstList("pilotFeatures")
enum class PilotFeature {
    MESSAGING,
    MOBILE,
    RESERVATIONS,
    VASU_AND_PEDADOC,
    MOBILE_MESSAGING,
    PLACEMENT_TERMINATION
}
