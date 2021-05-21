// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.daycare.getActiveClubTermAt
import fi.espoo.evaka.daycare.getActivePreschoolTermAt
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyPlacementPlanApplied
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@Service
class PlacementPlanService(
    private val asyncJobRunner: AsyncJobRunner,
    env: Environment
) {
    private val useFiveYearsOldDaycare = env.getProperty("fi.espoo.evaka.five_years_old_daycare.enabled", Boolean::class.java, true)

    fun getPlacementPlanDraft(tx: Database.Read, applicationId: UUID): PlacementPlanDraft {
        val application = tx.fetchApplicationDetails(applicationId)
            ?: throw NotFound("Application $applicationId not found")

        val type = derivePlacementType(application)
        val form = application.form
        val child = tx.getPlacementDraftChild(application.childId)
            ?: throw NotFound("Cannot find child with id ${application.childId} to application ${application.id}")
        val guardianHasRestrictedDetails = tx.getGuardiansRestrictedStatus(application.guardianId)
            ?: throw NotFound("Cannot find guardian with id ${application.guardianId} to application ${application.id}")
        val preferredUnits = form.preferences.preferredUnits.map {
            PlacementDraftUnit(
                id = it.id,
                name = it.name
            )
        }
        val placements = tx.getPlacementDraftPlacements(application.childId)

        val startDate = form.preferences.preferredStartDate!!

        when (application.type) {
            ApplicationType.PRESCHOOL -> {
                val preschoolTerms = tx.getActivePreschoolTermAt(startDate)
                    ?: throw Exception("No suitable preschool term found for start date $startDate")
                val exactTerm = if (isSvebiUnit(tx, preferredUnits[0].id)) preschoolTerms.swedishPreschool else preschoolTerms.finnishPreschool
                val period = FiniteDateRange(startDate, exactTerm.end)
                val preschoolDaycarePeriod = if (type == PlacementType.PRESCHOOL_DAYCARE || type == PlacementType.PREPARATORY_DAYCARE) {
                    FiniteDateRange(startDate, LocalDate.of(preschoolTerms.extendedTerm.end.year, 7, 31))
                } else null

                return PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredUnits = preferredUnits,
                    period = period,
                    preschoolDaycarePeriod = preschoolDaycarePeriod,
                    placements = placements,
                    guardianHasRestrictedDetails = guardianHasRestrictedDetails
                )
            }
            ApplicationType.DAYCARE -> {
                val endFromBirthDate = LocalDate.of(child.dob.year + 6, 7, 31)
                val endFromStartDate = if (startDate.month >= Month.AUGUST)
                    LocalDate.of(startDate.year + 1, 7, 31)
                else
                    LocalDate.of(startDate.year, 7, 31)
                val period =
                    FiniteDateRange(startDate, if (endFromBirthDate < startDate) endFromStartDate else endFromBirthDate)
                return PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredUnits = preferredUnits,
                    period = period,
                    preschoolDaycarePeriod = null,
                    placements = placements,
                    guardianHasRestrictedDetails = guardianHasRestrictedDetails
                )
            }
            ApplicationType.CLUB -> {
                val (term) = tx.getActiveClubTermAt(startDate)
                    ?: throw Exception("No suitable club term found for start date $startDate")
                val period = FiniteDateRange(startDate, term.end)
                return PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredUnits = preferredUnits,
                    period = period,
                    preschoolDaycarePeriod = null,
                    placements = placements,
                    guardianHasRestrictedDetails = guardianHasRestrictedDetails
                )
            }
            else -> throw IllegalArgumentException("Unsupported application form type ${application.type}")
        }
    }

    fun softDeleteUnusedPlacementPlanByApplication(tx: Database.Transaction, applicationId: UUID) =
        tx.softDeletePlacementPlanIfUnused(applicationId)

    fun createPlacementPlan(tx: Database.Transaction, application: ApplicationDetails, placementPlan: DaycarePlacementPlan) =
        tx.createPlacementPlan(application.id, derivePlacementType(application), placementPlan)

    fun applyPlacementPlan(
        tx: Database.Transaction,
        childId: UUID,
        plan: PlacementPlan,
        allowPreschool: Boolean = true,
        allowPreschoolDaycare: Boolean = false,
        requestedStartDate: LocalDate? = null
    ) {
        var effectivePeriod: FiniteDateRange? = null
        if (plan.type in listOf(PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY_DAYCARE)) {
            if (allowPreschool) {
                val period = requestedStartDate?.let { plan.period.copy(start = it) } ?: plan.period
                createPlacement(
                    tx,
                    type = if (plan.type == PlacementType.PRESCHOOL_DAYCARE) PlacementType.PRESCHOOL else PlacementType.PREPARATORY,
                    childId = childId,
                    unitId = plan.unitId,
                    startDate = period.start,
                    endDate = period.end,
                    useFiveYearsOldDaycare = useFiveYearsOldDaycare
                )
                effectivePeriod = period
            }
            if (allowPreschoolDaycare) {
                val period = (
                    requestedStartDate?.let { plan.preschoolDaycarePeriod?.copy(start = it) }
                        ?: plan.preschoolDaycarePeriod
                    )!!

                val preschoolTerms = tx.getActivePreschoolTermAt(period.start)
                    ?: throw Exception("No suitable preschool term found for start date ${period.start}")

                val exactTerm = if (isSvebiUnit(tx, plan.unitId)) preschoolTerms.swedishPreschool else preschoolTerms.finnishPreschool

                // if the preschool daycare extends beyond the end of the preschool term, a normal daycare
                // placement is used because invoices are handled differently
                if (period.end.isAfter(exactTerm.end)) {
                    createPlacement(
                        tx,
                        type = plan.type,
                        childId = childId,
                        unitId = plan.unitId,
                        startDate = period.start,
                        endDate = exactTerm.end,
                        useFiveYearsOldDaycare = useFiveYearsOldDaycare
                    )
                    createPlacement(
                        tx,
                        type = PlacementType.DAYCARE,
                        childId = childId,
                        unitId = plan.unitId,
                        startDate = exactTerm.end.plusDays(1),
                        endDate = period.end,
                        useFiveYearsOldDaycare = useFiveYearsOldDaycare
                    )
                } else {
                    createPlacement(
                        tx,
                        type = plan.type,
                        childId = childId,
                        unitId = plan.unitId,
                        startDate = period.start,
                        endDate = period.end,
                        useFiveYearsOldDaycare = useFiveYearsOldDaycare
                    )
                }
                effectivePeriod = effectivePeriod?.let {
                    FiniteDateRange(
                        start = minOf(it.start, period.start),
                        end = maxOf(it.end, period.end)
                    )
                } ?: period
            }
        } else {
            val period = requestedStartDate?.let { plan.period.copy(start = it) } ?: plan.period
            createPlacement(
                tx,
                type = plan.type,
                childId = childId,
                unitId = plan.unitId,
                startDate = period.start,
                endDate = period.end,
                useFiveYearsOldDaycare = useFiveYearsOldDaycare
            )
            effectivePeriod = period
        }
        effectivePeriod?.also {
            asyncJobRunner.plan(tx, listOf(NotifyPlacementPlanApplied(childId, it.start, it.end)))
        }
    }

    fun isSvebiUnit(tx: Database.Read, unitId: UUID): Boolean {
        return tx.createQuery(
            """
            SELECT ca.short_name = 'svenska-bildningstjanster' AS is_svebi
            FROM daycare d LEFT JOIN care_area ca ON d.care_area_id = ca.id
            WHERE d.id = :unitId
            """.trimIndent()
        )
            .bind("unitId", unitId)
            .map { rs, _ -> rs.getBoolean("is_svebi") }
            .first()
    }

    private fun derivePlacementType(application: ApplicationDetails): PlacementType =
        when (application.type) {
            ApplicationType.PRESCHOOL -> {
                if (application.form.preferences.preparatory) {
                    if (application.form.preferences.serviceNeed != null) PlacementType.PREPARATORY_DAYCARE else PlacementType.PREPARATORY
                } else {
                    if (application.form.preferences.serviceNeed != null) PlacementType.PRESCHOOL_DAYCARE else PlacementType.PRESCHOOL
                }
            }
            ApplicationType.DAYCARE -> if (application.form.preferences.serviceNeed?.partTime == true) PlacementType.DAYCARE_PART_TIME else PlacementType.DAYCARE
            ApplicationType.CLUB -> PlacementType.CLUB
        }
}
