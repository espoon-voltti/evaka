// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.shared.PersonEmailVerificationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

data class EmailVerification(
    val id: PersonEmailVerificationId,
    val email: String,
    val expiresAt: HelsinkiDateTime,
    val sentAt: HelsinkiDateTime?,
)

data class EmailVerificationTarget(val person: PersonId, val email: String)

data class NewEmailVerification(val verificationCode: String, val expiresAt: HelsinkiDateTime)
