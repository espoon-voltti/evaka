// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children.consent

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.PropagateNull

@ConstList("childConsentTypes")
enum class ChildConsentType {
    EVAKA_PROFILE_PICTURE
}

data class ChildConsent(
    val type: ChildConsentType,
    val given: Boolean?,
    val givenByGuardian: String?,
    val givenByEmployee: String?,
    val givenAt: HelsinkiDateTime?
)

data class CitizenChildConsent(
    @PropagateNull
    val type: ChildConsentType,
    val given: Boolean?
)
