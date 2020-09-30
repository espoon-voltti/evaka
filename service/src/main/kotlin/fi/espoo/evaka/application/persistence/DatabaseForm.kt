// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.persistence

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

sealed class DatabaseForm {
    abstract val type: FormType

    abstract class ClubForm : DatabaseForm() {
        final override val type = FormType.CLUB
        abstract val preferredStartDate: LocalDate?
    }

    abstract class DaycareForm : DatabaseForm()

    abstract fun hideGuardianAddress(): DatabaseForm
    abstract fun hideChildAddress(): DatabaseForm
}

const val FORM_TYPE_DAYCARE = "daycare"
const val FORM_TYPE_PRESCHOOL = "preschool"
const val FORM_TYPE_CLUB = "club"

enum class FormType(val id: Long) {
    @JsonProperty(FORM_TYPE_DAYCARE)
    DAYCARE(1L),
    @JsonProperty(FORM_TYPE_PRESCHOOL)
    PRESCHOOL(2L),
    @JsonProperty(FORM_TYPE_CLUB)
    CLUB(3L)
}
