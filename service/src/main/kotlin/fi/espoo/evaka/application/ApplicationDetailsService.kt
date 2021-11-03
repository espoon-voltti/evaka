// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.shared.db.Database
import java.time.LocalDate
import java.util.UUID

fun Database.Read.applicationFlags(application: ApplicationDetails): ApplicationFlags {
    return applicationFlags(
        childId = application.childId,
        formType = application.type,
        startDate = application.form.preferences.preferredStartDate ?: LocalDate.now(),
        connectedDaycare = application.form.preferences.serviceNeed != null
    )
}

fun Database.Read.applicationFlags(
    childId: UUID,
    formType: ApplicationType,
    startDate: LocalDate,
    connectedDaycare: Boolean
): ApplicationFlags {
    return when (formType) {
        ApplicationType.CLUB -> ApplicationFlags(
            isTransferApplication = getPlacementsForChildDuring(childId, startDate, null)
                .any { it.type == PlacementType.CLUB }
        )
        ApplicationType.DAYCARE -> ApplicationFlags(
            isTransferApplication = getPlacementsForChildDuring(childId, startDate, null)
                .any {
                    listOf(
                        PlacementType.DAYCARE,
                        PlacementType.DAYCARE_PART_TIME,
                        PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                        PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
                    ).contains(it.type)
                }
        )
        ApplicationType.PRESCHOOL -> {
            val existingPlacements = getPlacementsForChildDuring(childId, startDate, null)
                .filter {
                    listOf(
                        PlacementType.PRESCHOOL,
                        PlacementType.PRESCHOOL_DAYCARE,
                        PlacementType.PREPARATORY,
                        PlacementType.PREPARATORY_DAYCARE
                    ).contains(it.type)
                }

            val isAdditionalDaycareApplication = connectedDaycare &&
                existingPlacements.isNotEmpty() &&
                existingPlacements.none {
                    it.type == PlacementType.PRESCHOOL_DAYCARE || it.type == PlacementType.PREPARATORY_DAYCARE
                }

            val isTransferApplication = !isAdditionalDaycareApplication && existingPlacements.isNotEmpty()

            ApplicationFlags(
                isTransferApplication = isTransferApplication,
                isAdditionalDaycareApplication = isAdditionalDaycareApplication
            )
        }
    }
}

data class ApplicationFlags(
    val isTransferApplication: Boolean = false,
    val isAdditionalDaycareApplication: Boolean = false
)
