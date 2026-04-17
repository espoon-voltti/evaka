// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.placement

import evaka.core.application.ApplicationDetails
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.DaycarePlacementPlan
import evaka.core.application.fetchApplicationDetails
import evaka.core.daycare.CareType
import evaka.core.daycare.Daycare
import evaka.core.daycare.getClubTerm
import evaka.core.daycare.getPreschoolTerm
import evaka.core.serviceneed.findServiceNeedOptionById
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.data.DateSet
import evaka.core.shared.db.Database
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.Month
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PlacementPlanService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    featureConfig: FeatureConfig,
) {
    private val useFiveYearsOldDaycare = featureConfig.fiveYearsOldDaycareEnabled
    private val daycarePlacementPlanEndMonthDay = featureConfig.daycarePlacementPlanEndMonthDay

    fun getPlacementPlanDraft(
        tx: Database.Read,
        applicationId: ApplicationId,
        minStartDate: LocalDate,
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

        val placementDraft =
            tx.createQuery {
                    sql(
                        """
            SELECT d.id, d.name, pd.start_date
            FROM placement_draft pd
            JOIN daycare d ON d.id = pd.unit_id
            WHERE pd.application_id = ${bind(applicationId)}
        """
                    )
                }
                .map {
                    PlacementDraftSummary(
                        unit = PlacementDraftUnit(column("id"), column("name")),
                        startDate = column("start_date"),
                    )
                }
                .exactlyOneOrNull()

        return when (application.type) {
            ApplicationType.PRESCHOOL -> {
                val preschoolTerms =
                    tx.getPreschoolTerm(startDate)
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
                        PlacementType.PREPARATORY_DAYCARE -> {
                            FiniteDateRange(
                                maxOf(
                                    minStartDate,
                                    form.preferences.connectedDaycarePreferredStartDate ?: startDate,
                                ),
                                LocalDate.of(preschoolTerms.extendedTerm.end.year, 7, 31),
                            )
                        }

                        PlacementType.PRESCHOOL_CLUB -> {
                            FiniteDateRange(
                                maxOf(
                                    minStartDate,
                                    form.preferences.connectedDaycarePreferredStartDate ?: startDate,
                                ),
                                exactTerm.end,
                            )
                        }

                        else -> {
                            null
                        }
                    }

                PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredStartDate = application.form.preferences.preferredStartDate,
                    dueDate = application.dueDate,
                    preferredUnits = preferredUnits,
                    placementDraft = placementDraft,
                    period = period,
                    preschoolDaycarePeriod = preschoolDaycarePeriod,
                    placements = placements,
                    guardianHasRestrictedDetails = guardianHasRestrictedDetails,
                )
            }

            ApplicationType.DAYCARE -> {
                val endFromBirthDate = daycarePlacementPlanEndMonthDay.atYear(child.dob.year + 6)
                val endFromStartDate =
                    if (startDate.month >= Month.AUGUST) {
                        daycarePlacementPlanEndMonthDay.atYear(startDate.year + 1)
                    } else {
                        daycarePlacementPlanEndMonthDay.atYear(startDate.year)
                    }
                val period =
                    FiniteDateRange(
                        startDate,
                        if (endFromBirthDate < startDate) endFromStartDate else endFromBirthDate,
                    )
                PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredStartDate = application.form.preferences.preferredStartDate,
                    dueDate = application.dueDate,
                    preferredUnits = preferredUnits,
                    placementDraft = placementDraft,
                    period = period,
                    preschoolDaycarePeriod = null,
                    placements = placements,
                    guardianHasRestrictedDetails = guardianHasRestrictedDetails,
                )
            }

            ApplicationType.CLUB -> {
                val clubTerm =
                    tx.getClubTerm(startDate)
                        ?: throw Exception("No suitable club term found for start date $startDate")
                val period = FiniteDateRange(startDate, clubTerm.term.end)
                PlacementPlanDraft(
                    child = child,
                    type = type,
                    preferredStartDate = application.form.preferences.preferredStartDate,
                    dueDate = application.dueDate,
                    preferredUnits = preferredUnits,
                    placementDraft = placementDraft,
                    period = period,
                    preschoolDaycarePeriod = null,
                    placements = placements,
                    guardianHasRestrictedDetails = guardianHasRestrictedDetails,
                )
            }
        }
    }

    fun softDeleteUnusedPlacementPlanByApplication(
        tx: Database.Transaction,
        applicationId: ApplicationId,
    ) = tx.softDeletePlacementPlanIfUnused(applicationId)

    fun createPlacementPlan(
        tx: Database.Transaction,
        application: ApplicationDetails,
        placementPlan: DaycarePlacementPlan,
    ) = tx.createPlacementPlan(application.id, application.derivePlacementType(), placementPlan)

    fun getPlacementTypePeriods(
        tx: Database.Read,
        childId: ChildId,
        unitId: DaycareId,
        type: PlacementType,
        extent: PlacementPlanExtent,
    ): List<Pair<FiniteDateRange, PlacementType>> =
        when (type) {
            PlacementType.PRESCHOOL_DAYCARE,
            PlacementType.PRESCHOOL_CLUB,
            PlacementType.PREPARATORY_DAYCARE -> {
                val (preschoolPeriods, preschoolDaycarePeriod) =
                    when (extent) {
                        is PlacementPlanExtent.OnlyPreschool -> {
                            Pair(listOf(extent.period), null)
                        }

                        is PlacementPlanExtent.OnlyPreschoolDaycare -> {
                            Pair(emptyList(), extent.period)
                        }

                        is PlacementPlanExtent.FullDouble -> {
                            Pair(
                                extent.period.complement(extent.preschoolDaycarePeriod),
                                extent.preschoolDaycarePeriod,
                            )
                        }

                        is PlacementPlanExtent.FullSingle -> {
                            error("Single extent not supported for $type")
                        }
                    }
                val preschoolPlacementType =
                    when (type) {
                        PlacementType.PRESCHOOL_DAYCARE -> PlacementType.PRESCHOOL
                        PlacementType.PRESCHOOL_CLUB -> PlacementType.PRESCHOOL
                        PlacementType.PREPARATORY_DAYCARE -> PlacementType.PREPARATORY
                    }
                preschoolPeriods.map { it to preschoolPlacementType } +
                    (preschoolDaycarePeriod?.let { period ->
                        val preschoolTerms =
                            tx.getPreschoolTerm(period.start)
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
                                    PlacementType.DAYCARE,
                            )
                        } else {
                            listOf(period to type)
                        }
                    } ?: emptyList())
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
        extent: PlacementPlanExtent,
        now: HelsinkiDateTime,
        userId: EvakaUserId,
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
            placeGuarantee = false,
            now = now,
            userId = userId,
            source = PlacementSource.APPLICATION,
            sourceApplicationId = application.id,
        )

        tx.deleteFutureReservationsAndAbsencesOutsideValidPlacements(childId, clock.today())

        val timeline = DateSet.of(placementTypePeriods.map { it.first })
        asyncJobRunner.plan(
            tx,
            timeline
                .ranges()
                .map { AsyncJob.GenerateFinanceDecisions.forChild(childId, it.asDateRange()) }
                .asIterable(),
            runAt = clock.now(),
        )
    }

    fun calculateSpeculatedPlacements(
        tx: Database.Read,
        unit: Daycare,
        application: ApplicationDetails,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange?,
    ): List<SpeculatedPlacement> {
        val placementType = application.derivePlacementType()

        val familyUnitPlacement =
            unit.type.contains(CareType.FAMILY) || unit.type.contains(CareType.GROUP_FAMILY)

        val placementTypePeriods =
            getPlacementTypePeriods(
                tx,
                application.childId,
                unit.id,
                placementType,
                when (placementType) {
                    PlacementType.PRESCHOOL_DAYCARE,
                    PlacementType.PRESCHOOL_CLUB,
                    PlacementType.PREPARATORY_DAYCARE -> {
                        PlacementPlanExtent.FullDouble(period, preschoolDaycarePeriod!!)
                    }

                    else -> {
                        PlacementPlanExtent.FullSingle(period)
                    }
                },
            )

        return placementTypePeriods.map { (period, placementType) ->
            SpeculatedPlacement(
                type = placementType,
                childId = application.childId,
                unitId = unit.id,
                familyUnitPlacement = familyUnitPlacement,
                period = period,
            )
        }
    }

    private fun resolveServiceNeedFromApplication(
        tx: Database.Read,
        application: ApplicationDetails,
    ): ApplicationServiceNeed? {
        val serviceNeedOptionId =
            application.form.preferences.serviceNeed?.serviceNeedOption?.id ?: return null
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
            serviceNeedOption.validPlacementType,
        )
    }

    fun isSvebiUnit(tx: Database.Read, unitId: DaycareId): Boolean {
        return tx.createQuery {
                sql(
                    """
                    SELECT ca.short_name = 'svenska-bildningstjanster' AS is_svebi
                    FROM daycare d LEFT JOIN care_area ca ON d.care_area_id = ca.id
                    WHERE d.id = ${bind(unitId)}
                    """
                )
            }
            .exactlyOne<Boolean>()
    }
}
