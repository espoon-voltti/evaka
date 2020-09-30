// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.deriveClubTerm
import fi.espoo.evaka.derivePreschoolTerm
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyPlacementPlanApplied
import fi.espoo.evaka.shared.db.runAfterCommit
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import javax.sql.DataSource

@Service
@Transactional(readOnly = true)
class PlacementPlanService(
    private val placementService: PlacementService,
    private val asyncJobRunner: AsyncJobRunner,
    private val dataSource: DataSource
) {
    fun getPlacementPlanDraft(applicationId: UUID): PlacementPlanDraft {
        val application = withSpringHandle(dataSource) { h -> fetchApplicationDetails(h, applicationId) }
            ?: throw NotFound("Application $applicationId not found")

        val type = derivePlacementType(application)
        val form = application.form
        val child = getChild(application.childId)
            ?: throw NotFound("Cannot find child with id ${application.childId} to application ${application.id}")
        val preferredUnits = form.preferences.preferredUnits.map {
            PlacementDraftUnit(
                id = it.id,
                name = it.name
            )
        }
        val placements = placementService.getPlacementDraftPlacements(application.childId)

        val startDate = form.preferences.preferredStartDate!!

        when (application.type) {
            ApplicationType.PRESCHOOL -> {
                val term = derivePreschoolTerm(startDate)
                val period = ClosedPeriod(startDate, term.end)
                val preschoolDaycarePeriod = if (type == PlacementType.PRESCHOOL_DAYCARE || type == PlacementType.PREPARATORY_DAYCARE) {
                    ClosedPeriod(startDate, LocalDate.of(term.end.year, 7, 31))
                } else null

                return PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredUnits = preferredUnits,
                    period = period,
                    preschoolDaycarePeriod = preschoolDaycarePeriod,
                    placements = placements
                )
            }
            ApplicationType.DAYCARE -> {
                val endFromBirthDate = LocalDate.of(child.dob.year + 6, 7, 31)
                val endFromStartDate = if (startDate.month >= Month.AUGUST)
                    LocalDate.of(startDate.year + 1, 7, 31)
                else
                    LocalDate.of(startDate.year, 7, 31)
                val period =
                    ClosedPeriod(startDate, if (endFromBirthDate < startDate) endFromStartDate else endFromBirthDate)
                return PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredUnits = preferredUnits,
                    period = period,
                    preschoolDaycarePeriod = null,
                    placements = placements
                )
            }
            ApplicationType.CLUB -> {
                val term = deriveClubTerm(startDate)
                val period = ClosedPeriod(startDate, term.end)
                return PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredUnits = preferredUnits,
                    period = period,
                    preschoolDaycarePeriod = null,
                    placements = placements
                )
            }
            else -> throw IllegalArgumentException("Unsupported application form type ${application.type}")
        }
    }

    fun getChild(childId: UUID): PlacementDraftChild? =
        withSpringHandle(dataSource) { h -> getPlacementDraftChild(h, childId) }

    fun getPlacementPlanByApplication(applicationId: UUID): PlacementPlan? =
        withSpringHandle(dataSource) { h -> getPlacementPlan(h, applicationId) }

    fun getPlacementPlanUnitNameByApplication(applicationId: UUID): String =
        withSpringHandle(dataSource) { h -> getPlacementPlanUnitName(h, applicationId) }

    fun getPlacementPlansByUnit(unitId: UUID, from: LocalDate, to: LocalDate): List<PlacementPlanDetails> =
        withSpringHandle(dataSource) { h -> getPlacementPlans(h, unitId, from, to) }

    @Transactional
    fun deletePlacementPlanByApplication(applicationId: UUID) =
        withSpringHandle(dataSource) { h -> deletePlacementPlan(h, applicationId) }

    @Transactional
    fun softDeletePlacementPlanByApplication(applicationId: UUID) =
        withSpringHandle(dataSource) { h -> softDeletePlacementPlan(h, applicationId) }

    @Transactional
    fun softDeleteUnusedPlacementPlanByApplication(applicationId: UUID) =
        withSpringHandle(dataSource) { h -> softDeletePlacementPlanIfUnused(h, applicationId) }

    @Transactional
    fun createPlacementPlan(application: ApplicationDetails, placementPlan: DaycarePlacementPlan) =
        withSpringHandle(dataSource) { h ->
            createPlacementPlan(h, application.id, derivePlacementType(application), placementPlan)
        }

    @Transactional
    fun applyPlacementPlan(
        childId: UUID,
        plan: PlacementPlan,
        allowPreschool: Boolean = true,
        allowPreschoolDaycare: Boolean = false,
        requestedStartDate: LocalDate? = null
    ) {
        var effectivePeriod: ClosedPeriod? = null
        if (plan.type in listOf(PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY_DAYCARE)) {
            if (allowPreschool) {
                val period = requestedStartDate?.let { plan.period.copy(start = it) } ?: plan.period
                placementService.createPlacement(
                    type = if (plan.type == PlacementType.PRESCHOOL_DAYCARE) PlacementType.PRESCHOOL else PlacementType.PREPARATORY,
                    childId = childId,
                    unitId = plan.unitId,
                    startDate = period.start,
                    endDate = period.end
                )
                effectivePeriod = period
            }
            if (allowPreschoolDaycare) {
                val period = (
                    requestedStartDate?.let { plan.preschoolDaycarePeriod?.copy(start = it) }
                        ?: plan.preschoolDaycarePeriod
                    )!!
                // TODO: this should not be hard-coded
                val term = derivePreschoolTerm(period.start)
                // if the preschool daycare extends beyond the end of the preschool term, a normal daycare
                // placement is used because invoices are handled differently
                if (period.end.isAfter(term.end)) {
                    placementService.createPlacement(
                        type = plan.type,
                        childId = childId,
                        unitId = plan.unitId,
                        startDate = period.start,
                        endDate = term.end
                    )
                    placementService.createPlacement(
                        type = PlacementType.DAYCARE,
                        childId = childId,
                        unitId = plan.unitId,
                        startDate = term.end.plusDays(1),
                        endDate = period.end
                    )
                } else {
                    placementService.createPlacement(
                        type = plan.type,
                        childId = childId,
                        unitId = plan.unitId,
                        startDate = period.start,
                        endDate = period.end
                    )
                }
                effectivePeriod = effectivePeriod?.let {
                    ClosedPeriod(
                        start = minOf(it.start, period.start),
                        end = maxOf(it.end, period.end)
                    )
                } ?: period
            }
        } else {
            val period = requestedStartDate?.let { plan.period.copy(start = it) } ?: plan.period
            placementService.createPlacement(
                type = plan.type,
                childId = childId,
                unitId = plan.unitId,
                startDate = period.start,
                endDate = period.end
            )
            effectivePeriod = period
        }
        effectivePeriod?.also {
            asyncJobRunner.plan(listOf(NotifyPlacementPlanApplied(childId, it.start, it.end)))
        }
        runAfterCommit { asyncJobRunner.scheduleImmediateRun() }
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
