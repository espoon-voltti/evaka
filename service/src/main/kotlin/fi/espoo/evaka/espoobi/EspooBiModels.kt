// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID

data class BiArea(val id: UUID, val updated: HelsinkiDateTime, val name: String)

data class BiUnit(
    val id: UUID,
    val updated: HelsinkiDateTime,
    val area: UUID,
    val name: String,
    val providerType: ProviderType,
    val costCenter: String?,
    val club: Boolean,
    val daycare: BiUnitDaycareType?,
    val preschool: Boolean,
    val preparatoryEducation: Boolean,
)

enum class BiUnitDaycareType {
    DAYCARE,
    FAMILY,
    GROUP_FAMILY,
}

data class BiGroup(
    val id: UUID,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
)

data class BiChild(
    val id: UUID,
    val updated: HelsinkiDateTime,
    val birthDate: LocalDate,
    val language: String?,
    val languageAtHome: String,
    val vtjNonDisclosure: Boolean,
    val postalCode: String,
    val postOffice: String,
)

data class BiPlacement(
    val id: UUID,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val unit: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isBackup: Boolean,
    val type: PlacementType?,
)

data class BiGroupPlacement(
    val id: UUID,
    val updated: HelsinkiDateTime,
    val placement: UUID,
    val group: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class BiAbsence(
    val id: UUID,
    val updated: HelsinkiDateTime,
    val child: UUID,
    val date: LocalDate,
    val category: AbsenceCategory,
)
