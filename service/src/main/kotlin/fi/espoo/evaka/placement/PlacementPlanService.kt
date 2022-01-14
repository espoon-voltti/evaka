// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.daycare.getActiveClubTermAt
import fi.espoo.evaka.daycare.getActivePreschoolTermAt
import fi.espoo.evaka.serviceneed.findServiceNeedOptionById
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.PlacementPlanId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Month
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class PlacementPlanService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    env: EvakaEnv
) {
    private val useFiveYearsOldDaycare = env.fiveYearsOldDaycareEnabled

    fun getPlacementPlanDraft(tx: Database.Read, applicationId: ApplicationId): PlacementPlanDraft {
        val application = tx.fetchApplicationDetails(applicationId)
            ?: throw NotFound("Application $applicationId not found")

        if (application.status !== ApplicationStatus.WAITING_PLACEMENT) {
            throw Conflict("Cannot get placement plan drafts for application with status ${application.status}")
        }

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

        return when (application.type) {
            ApplicationType.PRESCHOOL -> {
                val preschoolTerms = tx.getActivePreschoolTermAt(startDate)
                    ?: throw Exception("No suitable preschool term found for start date $startDate")
                val exactTerm = if (isSvebiUnit(
                        tx,
                        preferredUnits[0].id
                    )
                ) preschoolTerms.swedishPreschool else preschoolTerms.finnishPreschool
                val period = FiniteDateRange(startDate, exactTerm.end)
                val preschoolDaycarePeriod =
                    if (type == PlacementType.PRESCHOOL_DAYCARE || type == PlacementType.PREPARATORY_DAYCARE) {
                        FiniteDateRange(startDate, LocalDate.of(preschoolTerms.extendedTerm.end.year, 7, 31))
                    } else null

                PlacementPlanDraft(
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
                PlacementPlanDraft(
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
                PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredUnits = preferredUnits,
                    period = period,
                    preschoolDaycarePeriod = null,
                    placements = placements,
                    guardianHasRestrictedDetails = guardianHasRestrictedDetails
                )
            }
        }
    }

    fun softDeleteUnusedPlacementPlanByApplication(tx: Database.Transaction, applicationId: ApplicationId) =
        tx.softDeletePlacementPlanIfUnused(applicationId)

    fun createPlacementPlan(
        tx: Database.Transaction,
        application: ApplicationDetails,
        placementPlan: DaycarePlacementPlan
    ) =
        tx.createPlacementPlan(application.id, derivePlacementType(application), placementPlan)

    fun getPlacementTypePeriods(
        tx: Database.Read,
        childId: ChildId,
        plan: PlacementPlan,
        allowPreschool: Boolean,
        allowPreschoolDaycare: Boolean,
        requestedStartDate: LocalDate? = null
    ): List<Pair<FiniteDateRange, PlacementType>> =
        if (plan.type in listOf(PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY_DAYCARE)) {
            if (allowPreschool) {
                val type =
                    if (plan.type == PlacementType.PRESCHOOL_DAYCARE) PlacementType.PRESCHOOL else PlacementType.PREPARATORY
                val period = requestedStartDate?.let { plan.period.copy(start = it) } ?: plan.period
                listOf(period to type)
            } else if (allowPreschoolDaycare) {
                val period = (
                    requestedStartDate?.let { plan.preschoolDaycarePeriod?.copy(start = it) }
                        ?: plan.preschoolDaycarePeriod
                    )!!

                val preschoolTerms = tx.getActivePreschoolTermAt(period.start)
                    ?: throw Exception("No suitable preschool term found for start date ${period.start}")

                val exactTerm = if (isSvebiUnit(
                        tx,
                        plan.unitId
                    )
                ) preschoolTerms.swedishPreschool else preschoolTerms.finnishPreschool

                // if the preschool daycare extends beyond the end of the preschool term, a normal daycare
                // placement is used because invoices are handled differently
                if (period.end.isAfter(exactTerm.end)) {
                    listOf(
                        FiniteDateRange(period.start, exactTerm.end) to plan.type,
                        FiniteDateRange(exactTerm.end.plusDays(1), period.end) to PlacementType.DAYCARE
                    )
                } else {
                    listOf(period to plan.type)
                }
            } else {
                throw Exception("Unable to create a placement")
            }
        } else {
            val period = requestedStartDate?.let { plan.period.copy(start = it) } ?: plan.period
            listOf(period to plan.type)
        }.let { placementPeriods ->
            if (useFiveYearsOldDaycare) {
                resolveFiveYearOldPlacementPeriods(tx, childId, placementPeriods)
            } else {
                placementPeriods
            }
        }

    fun applyPlacementPlan(
        tx: Database.Transaction,
        application: ApplicationDetails,
        plan: PlacementPlan,
        allowPreschool: Boolean,
        allowPreschoolDaycare: Boolean,
        requestedStartDate: LocalDate?
    ) {
        val childId = application.childId
        val placementTypePeriods = getPlacementTypePeriods(
            tx,
            childId,
            plan,
            allowPreschool,
            allowPreschoolDaycare,
            requestedStartDate
        )
        val serviceNeed = resolveServiceNeedFromApplication(tx, application)

        createPlacements(
            tx,
            childId = childId,
            unitId = plan.unitId,
            placementTypePeriods = placementTypePeriods,
            serviceNeed = serviceNeed
        )
        getPlacementTypePeriodBounds(placementTypePeriods)?.let { effectivePeriod ->
            asyncJobRunner.plan(
                tx,
                listOf(AsyncJob.GenerateFinanceDecisions.forChild(childId, effectivePeriod.asDateRange()))
            )
        }
    }

    fun calculateSpeculatedPlacements(
        tx: Database.Read,
        unitId: DaycareId,
        application: ApplicationDetails,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange?
    ): List<Placement> {
        val placementType = derivePlacementType(application)

        val allowPreschool = placementType in listOf(PlacementType.PRESCHOOL, PlacementType.PREPARATORY)
        val allowPreschoolDaycare = placementType in listOf(PlacementType.PRESCHOOL_DAYCARE)

        val placementTypePeriods = getPlacementTypePeriods(
            tx,
            childId = application.childId,
            plan = PlacementPlan(
                id = PlacementPlanId(UUID.randomUUID()),
                unitId = unitId,
                applicationId = application.id,
                type = placementType,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
            ),
            allowPreschool = allowPreschool,
            allowPreschoolDaycare = allowPreschoolDaycare,
        )

        return placementTypePeriods.map { (period, placementType) ->
            Placement(
                id = PlacementId(UUID.randomUUID()),
                type = placementType,
                childId = application.childId,
                unitId = unitId,
                startDate = period.start,
                endDate = period.end,
                terminationRequestedBy = null,
                terminationRequestedDate = null,
            )
        }
    }

    private fun resolveServiceNeedFromApplication(
        tx: Database.Read,
        application: ApplicationDetails
    ): ApplicationServiceNeed? {
        val serviceNeedOptionId = application.form.preferences.serviceNeed?.serviceNeedOption?.id ?: return null
        if (tx.findServiceNeedOptionById(serviceNeedOptionId) == null) {
            logger.warn { "Application ${application.id} has non-existing service need option: $serviceNeedOptionId" }
            return null
        }
        return ApplicationServiceNeed(serviceNeedOptionId, application.form.preferences.serviceNeed.shiftCare)
    }

    fun isSvebiUnit(tx: Database.Read, unitId: DaycareId): Boolean {
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
