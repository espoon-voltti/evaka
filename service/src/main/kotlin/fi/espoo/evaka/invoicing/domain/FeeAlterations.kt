// ktlint-disable filename
// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class FeeAlteration(
    val id: UUID? = null,
    val personId: UUID,
    val type: Type,
    val amount: Int,
    @get:JsonProperty("isAbsolute") val isAbsolute: Boolean,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val notes: String,
    val updatedAt: Instant? = null,
    val updatedBy: UUID? = null
) {
    enum class Type {
        DISCOUNT,
        INCREASE,
        RELIEF;
    }
}
