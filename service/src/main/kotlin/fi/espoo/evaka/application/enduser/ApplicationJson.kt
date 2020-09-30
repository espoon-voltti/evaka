// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.enduser

import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.enduser.daycare.OtherGuardianAgreementStatus
import fi.espoo.evaka.application.persistence.DatabaseForm
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ApplicationJson(
    val id: UUID,
    val status: ApplicationStatus,
    val origin: ApplicationOrigin,
    val dueDate: LocalDate?,
    val startDate: LocalDate?,
    val form: FormJson,
    val sentDate: LocalDate?,
    val createdDate: OffsetDateTime?,
    val modifiedDate: OffsetDateTime?,
    val childId: UUID,
    val guardianId: UUID,
    val transferApplication: Boolean,
    val otherGuardianAgreementStatus: OtherGuardianAgreementStatus?,
    val otherVtjGuardianHasSameAddress: Boolean,
    val hasOtherVtjGuardian: Boolean
)

sealed class FormJson {
    abstract val type: ApplicationJsonType
    abstract fun deserialize(): DatabaseForm

    abstract class ClubFormJSON : FormJson() {
        final override val type = ApplicationJsonType.CLUB
    }

    abstract class DaycareFormJSON : FormJson()
}

enum class ApplicationJsonType(val type: String) {
    @JsonProperty("club")
    CLUB("club"),

    @JsonProperty("daycare")
    DAYCARE("daycare"),

    @JsonProperty("preschool")
    PRESCHOOL("preschool");

    override fun toString(): String {
        return type
    }
}
