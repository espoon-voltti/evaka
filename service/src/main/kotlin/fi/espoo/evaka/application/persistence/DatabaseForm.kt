// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.persistence

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import fi.espoo.evaka.application.ApplicationType
import java.time.LocalDate

sealed class DatabaseForm {
    abstract val type: ApplicationType

    abstract class ClubForm : DatabaseForm() {
        final override val type = ApplicationType.CLUB
        abstract val preferredStartDate: LocalDate?
    }

    abstract class DaycareForm : DatabaseForm()

    abstract fun hideGuardianAddress(): DatabaseForm
    abstract fun hideChildAddress(): DatabaseForm
}

fun jsonMapper(): JsonMapper =
    jacksonMapperBuilder()
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()
