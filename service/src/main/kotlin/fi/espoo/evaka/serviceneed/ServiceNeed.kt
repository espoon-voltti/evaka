// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

data class ServiceNeed(
    val id: UUID,
    val childId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val updatedByName: String,
    val updated: ZonedDateTime,
    val hoursPerWeek: Double,
    val partDay: Boolean,
    val partWeek: Boolean,
    val shiftCare: Boolean
)

data class ServiceNeedRequest(
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val hoursPerWeek: Double,
    val partDay: Boolean = false,
    val partWeek: Boolean = false,
    val shiftCare: Boolean = false
)
