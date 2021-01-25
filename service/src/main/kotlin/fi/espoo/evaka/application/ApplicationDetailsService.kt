// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.placement.Placement
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
        preferredUnits = application.form.preferences.preferredUnits.map { it.id },
        connectedDaycare = application.form.preferences.serviceNeed != null
    )
}

fun Database.Read.applicationFlags(
    childId: UUID,
    formType: ApplicationType,
    startDate: LocalDate,
    preferredUnits: List<UUID>,
    connectedDaycare: Boolean
): ApplicationFlags {
    return when (formType) {
        ApplicationType.CLUB -> ApplicationFlags(
            isTransferApplication = handle.getPlacementsForChildDuring(childId, startDate, null)
                .any { it.type == PlacementType.CLUB }
        )
        ApplicationType.DAYCARE -> ApplicationFlags(
            isTransferApplication = handle.getPlacementsForChildDuring(childId, startDate, null)
                .any { listOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME).contains(it.type) }
        )
        ApplicationType.PRESCHOOL -> {
            val existingPlacements = handle.getPlacementsForChildDuring(childId, startDate, null)
                .filter {
                    listOf(
                        PlacementType.PRESCHOOL,
                        PlacementType.PRESCHOOL_DAYCARE,
                        PlacementType.PREPARATORY,
                        PlacementType.PREPARATORY_DAYCARE
                    ).contains(it.type)
                }

            val isAdditionalDaycareApplication =
                isAdditionalDaycareApplication(connectedDaycare, preferredUnits, existingPlacements)

            val isTransferApplication = !isAdditionalDaycareApplication && existingPlacements.isNotEmpty()

            ApplicationFlags(
                isTransferApplication = isTransferApplication,
                isAdditionalDaycareApplication = isAdditionalDaycareApplication
            )
        }
    }
}

fun isAdditionalDaycareApplication(
    connectedDaycare: Boolean,
    preferredUnits: List<UUID>,
    existingPreschoolPlacements: List<Placement>
): Boolean {
    if (!connectedDaycare) {
        return false
    }

    return preferredUnits.size == 1 &&
        existingPreschoolPlacements.none {
            listOf(
                PlacementType.PRESCHOOL_DAYCARE,
                PlacementType.PREPARATORY_DAYCARE
            ).contains(it.type)
        } &&
        existingPreschoolPlacements.any { it.unitId == preferredUnits[0] }
}

data class ApplicationFlags(
    val isTransferApplication: Boolean = false,
    val isAdditionalDaycareApplication: Boolean = false
)
