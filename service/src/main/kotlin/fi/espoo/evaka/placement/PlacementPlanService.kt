// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

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
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import java.time.Month
import java.util.UUID
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PlacementPlanService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    featureConfig: FeatureConfig
) {
    private val useFiveYearsOldDaycare = featureConfig.fiveYearsOldDaycareEnabled

    fun getPlacementPlanDraft(
        tx: Database.Read,
        applicationId: ApplicationId,
        minStartDate: LocalDate
    ): PlacementPlanDraft {
        val application =
            tx.fetchApplicationDetails(applicationId)
                ?: throw NotFound("Application $applicationId not found")

        if (application.status != ApplicationStatus.WAITING_PLACEMENT) {
            throw Conflict(
                "Cannot get placement plan drafts for application with status ${application.status}"
            )
        }

        val type = application.derivePlacementType()
        val form = application.form
        val child =
            tx.getPlacementDraftChild(application.childId)
                ?: throw NotFound(
                    "Cannot find child with id ${application.childId} to application ${application.id}"
                )
        val guardianHasRestrictedDetails =
            tx.getGuardiansRestrictedStatus(application.guardianId)
                ?: throw NotFound(
                    "Cannot find guardian with id ${application.guardianId} to application ${application.id}"
                )
        val preferredUnits =
            form.preferences.preferredUnits.map { PlacementDraftUnit(id = it.id, name = it.name) }
        val placements = tx.getPlacementSummary(application.childId)

        val startDate = maxOf(minStartDate, form.preferences.preferredStartDate!!)

        return when (application.type) {
            ApplicationType.PRESCHOOL -> {
                val preschoolTerms =
                    tx.getActivePreschoolTermAt(startDate)
                        ?: throw Exception(
                            "No suitable preschool term found for start date $startDate"
                        )
                val exactTerm =
                    if (isSvebiUnit(tx, preferredUnits[0].id)) {
                        preschoolTerms.swedishPreschool
                    } else {
                        preschoolTerms.finnishPreschool
                    }
                val period = FiniteDateRange(startDate, exactTerm.end)
                val preschoolDaycarePeriod =
                    when (type) {
                        PlacementType.PRESCHOOL_DAYCARE,
                        PlacementType.PREPARATORY_DAYCARE ->
                            FiniteDateRange(
                                maxOf(
                                    minStartDate,
                                    form.preferences.connectedDaycarePreferredStartDate ?: startDate
                                ),
                                LocalDate.of(preschoolTerms.extendedTerm.end.year, 7, 31)
                            )
                        PlacementType.PRESCHOOL_CLUB ->
                            FiniteDateRange(
                                maxOf(
                                    minStartDate,
                                    form.preferences.connectedDaycarePreferredStartDate ?: startDate
                                ),
                                exactTerm.end
                            )
                        else -> null
                    }

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
                val endFromStartDate =
                    if (startDate.month >= Month.AUGUST) {
                        LocalDate.of(startDate.year + 1, 7, 31)
                    } else {
                        LocalDate.of(startDate.year, 7, 31)
                    }
                val period =
                    FiniteDateRange(
                        startDate,
                        if (endFromBirthDate < startDate) endFromStartDate else endFromBirthDate
                    )
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
                val (_, term) =
                    tx.getActiveClubTermAt(startDate)
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

    fun softDeleteUnusedPlacementPlanByApplication(
        tx: Database.Transaction,
        applicationId: ApplicationId
    ) = tx.softDeletePlacementPlanIfUnused(applicationId)

    fun createPlacementPlan(
        tx: Database.Transaction,
        application: ApplicationDetails,
        placementPlan: DaycarePlacementPlan
    ) = tx.createPlacementPlan(application.id, application.derivePlacementType(), placementPlan)

    fun getPlacementTypePeriods(
        tx: Database.Read,
        childId: ChildId,
        unitId: DaycareId,
        type: PlacementType,
        extent: PlacementPlanExtent
    ): List<Pair<FiniteDateRange, PlacementType>> =
        when (type) {
            PlacementType.PRESCHOOL_DAYCARE,
            PlacementType.PRESCHOOL_CLUB,
            PlacementType.PREPARATORY_DAYCARE -> {
                val (preschoolPeriods, preschoolDaycarePeriod) =
                    when (extent) {
                        is PlacementPlanExtent.OnlyPreschool -> Pair(listOf(extent.period), null)
                        is PlacementPlanExtent.OnlyPreschoolDaycare ->
                            Pair(emptyList(), extent.period)
                        is PlacementPlanExtent.FullDouble ->
                            Pair(
                                extent.period.complement(extent.preschoolDaycarePeriod),
                                extent.preschoolDaycarePeriod
                            )
                        is PlacementPlanExtent.FullSingle ->
                            error("Single extent not supported for $type")
                    }
                val preschoolPlacementType =
                    when (type) {
                        PlacementType.PRESCHOOL_DAYCARE -> PlacementType.PRESCHOOL
                        PlacementType.PRESCHOOL_CLUB -> PlacementType.PRESCHOOL
                        PlacementType.PREPARATORY_DAYCARE -> PlacementType.PREPARATORY
                        else -> error("Invalid placement plan type")
                    }
                preschoolPeriods.map { it to preschoolPlacementType } +
                    (
                        preschoolDaycarePeriod?.let { period ->
                            val preschoolTerms =
                                tx.getActivePreschoolTermAt(period.start)
                                    ?: throw Exception(
                                        "No suitable preschool term found for start date ${period.start}"
                                    )

                            val exactTerm =
                                if (isSvebiUnit(tx, unitId)) {
                                    preschoolTerms.swedishPreschool
                                } else {
                                    preschoolTerms.finnishPreschool
                                }

                            // if the preschool daycare extends beyond the end of the preschool term, a
                            // normal daycare
                            // placement is used because invoices are handled differently
                            if (period.end.isAfter(exactTerm.end)) {
                                listOf(
                                    FiniteDateRange(period.start, exactTerm.end) to type,
                                    FiniteDateRange(exactTerm.end.plusDays(1), period.end) to
                                        PlacementType.DAYCARE
                                )
                            } else {
                                listOf(period to type)
                            }
                        } ?: emptyList()
                    )
            }
            else -> {
                check(extent is PlacementPlanExtent.FullSingle) {
                    "Only single extent is supported for $type"
                }
                listOf(extent.period to type)
            }
        }.let { placementPeriods ->
            if (useFiveYearsOldDaycare) {
                resolveFiveYearOldPlacementPeriods(tx, childId, placementPeriods)
            } else {
                placementPeriods
            }
        }

    fun applyPlacementPlan(
        tx: Database.Transaction,
        clock: EvakaClock,
        application: ApplicationDetails,
        unitId: DaycareId,
        type: PlacementType,
        extent: PlacementPlanExtent
    ) {
        val childId = application.childId
        val placementTypePeriods = getPlacementTypePeriods(tx, childId, unitId, type, extent)
        val serviceNeed = resolveServiceNeedFromApplication(tx, application)

        createPlacements(
            tx,
            childId = childId,
            unitId = unitId,
            placementTypePeriods = placementTypePeriods,
            serviceNeed = serviceNeed,
            cancelPlacementsAfterClub = true,
            placeGuarantee = false
        )
        val timeline = DateSet.of(placementTypePeriods.map { it.first })
        asyncJobRunner.plan(
            tx,
            timeline
                .ranges()
                .map { AsyncJob.GenerateFinanceDecisions.forChild(childId, it.asDateRange()) }
                .asIterable(),
            runAt = clock.now()
        )
    }

    fun calculateSpeculatedPlacements(
        tx: Database.Read,
        unitId: DaycareId,
        application: ApplicationDetails,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange?
    ): List<Placement> {
        val placementType = application.derivePlacementType()

        val placementTypePeriods =
            getPlacementTypePeriods(
                tx,
                application.childId,
                unitId,
                placementType,
                when (placementType) {
                    PlacementType.PRESCHOOL_DAYCARE,
                    PlacementType.PRESCHOOL_CLUB,
                    PlacementType.PREPARATORY_DAYCARE ->
                        PlacementPlanExtent.FullDouble(period, preschoolDaycarePeriod!!)
                    else -> PlacementPlanExtent.FullSingle(period)
                }
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
                placeGuarantee = false
            )
        }
    }

    private fun resolveServiceNeedFromApplication(
        tx: Database.Read,
        application: ApplicationDetails
    ): ApplicationServiceNeed? {
        val serviceNeedOptionId =
            application.form.preferences.serviceNeed
                ?.serviceNeedOption
                ?.id ?: return null
        val serviceNeedOption = serviceNeedOptionId.let { tx.findServiceNeedOptionById(it) }
        if (serviceNeedOption == null) {
            logger.warn {
                "Application ${application.id} has non-existing service need option: $serviceNeedOptionId"
            }
            return null
        }
        return ApplicationServiceNeed(
            serviceNeedOptionId,
            application.form.preferences.serviceNeed.shiftCare,
            serviceNeedOption.validPlacementType
        )
    }

    fun isSvebiUnit(
        tx: Database.Read,
        unitId: DaycareId
    ): Boolean =
        tx
            .createQuery {
                sql(
                    """
                    SELECT ca.short_name = 'svenska-bildningstjanster' AS is_svebi
                    FROM daycare d LEFT JOIN care_area ca ON d.care_area_id = ca.id
                    WHERE d.id = ${bind(unitId)}
                    """
                )
            }.exactlyOne<Boolean>()
}
