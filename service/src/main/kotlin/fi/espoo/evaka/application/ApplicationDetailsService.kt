// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.application.persistence.FormType
import fi.espoo.evaka.placement.Placement
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.db.withSpringTx
import org.jdbi.v3.core.Handle
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

// Only created because DaycareApplication and ClubApplication tests rely purely on mocks
@Service
class ApplicationDetailsService(val dataSource: DataSource, val txManager: PlatformTransactionManager) {
    fun applicationFlags(
        childId: UUID,
        formType: FormType,
        startDate: LocalDate,
        preferredUnits: List<UUID>,
        connectedDaycare: Boolean
    ): ApplicationFlags = withSpringTx(txManager) {
        withSpringHandle(dataSource) { h ->
            applicationFlags(h, childId, formType, startDate, preferredUnits, connectedDaycare)
        }
    }

    fun applicationFlags(application: ApplicationDetails): ApplicationFlags {
        return applicationFlags(
            childId = application.childId,
            formType = when (application.type) {
                ApplicationType.CLUB -> FormType.CLUB
                ApplicationType.DAYCARE -> FormType.DAYCARE
                ApplicationType.PRESCHOOL -> FormType.PRESCHOOL
            },
            startDate = application.form.preferences.preferredStartDate ?: LocalDate.now(),
            preferredUnits = application.form.preferences.preferredUnits.map { it.id },
            connectedDaycare = application.form.preferences.serviceNeed != null
        )
    }
}

fun applicationFlags(
    h: Handle,
    childId: UUID,
    formType: FormType,
    startDate: LocalDate,
    preferredUnits: List<UUID>,
    connectedDaycare: Boolean
): ApplicationFlags {
    return when (formType) {
        FormType.CLUB -> ApplicationFlags(
            isTransferApplication = h.getPlacementsForChildDuring(childId, startDate, null)
                .any { it.type == PlacementType.CLUB }
        )
        FormType.DAYCARE -> ApplicationFlags(
            isTransferApplication = h.getPlacementsForChildDuring(childId, startDate, null)
                .any { listOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME).contains(it.type) }
        )
        FormType.PRESCHOOL -> {
            val existingPlacements = h.getPlacementsForChildDuring(childId, startDate, null)
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
