// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.timeline

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.service.generator.WithRange
import fi.espoo.evaka.pis.CreationModificationMetadata
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPartnersForPerson
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/timeline")
class TimelineController(private val accessControl: AccessControl) {
    @GetMapping
    fun getTimeline(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam personId: PersonId,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): Timeline {
        val range = FiniteDateRange(from, to)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_TIMELINE,
                        personId,
                    )

                    val personBasics = tx.getPersonById(personId) ?: throw NotFound()

                    Timeline(
                        personId = personId,
                        firstName = personBasics.firstName,
                        lastName = personBasics.lastName,
                        feeDecisions = tx.getFeeDecisions(personId, range),
                        valueDecisions = tx.getValueDecisions(personId, range),
                        incomes = tx.getIncomes(personId, range),
                        partners =
                            tx.getPartners(personId, range).map { partner ->
                                val partnerRange = range.intersection(partner.range)!!
                                val originApplicationAccessible =
                                    if (
                                        partner.creationModificationMetadata
                                            .createdFromApplication != null
                                    )
                                        accessControl
                                            .getPermittedActions<ApplicationId, Action.Application>(
                                                tx,
                                                user,
                                                clock,
                                                partner.creationModificationMetadata
                                                    .createdFromApplication,
                                            )
                                            .contains(Action.Application.READ)
                                    else false
                                TimelinePartnerDetailed(
                                    id = partner.id,
                                    range = partner.range,
                                    partnerId = partner.partnerId,
                                    firstName = partner.firstName,
                                    lastName = partner.lastName,
                                    feeDecisions =
                                        tx.getFeeDecisions(partner.partnerId, partnerRange),
                                    valueDecisions =
                                        tx.getValueDecisions(partner.partnerId, partnerRange),
                                    incomes = tx.getIncomes(partner.partnerId, partnerRange),
                                    children =
                                        tx.getChildren(partner.partnerId, partnerRange).map { child
                                            ->
                                            val childRange =
                                                partnerRange.intersection(child.range)!!
                                            addDetailsToChild(tx, user, clock, child, childRange)
                                        },
                                    creationModificationMetadata =
                                        partner.creationModificationMetadata,
                                    originApplicationAccessible = originApplicationAccessible,
                                )
                            },
                        children =
                            tx.getChildren(personId, range).map { child ->
                                val childRange = range.intersection(child.range)!!
                                addDetailsToChild(tx, user, clock, child, childRange)
                            },
                    )
                }
            }
            .also { Audit.TimelineRead.log(targetId = AuditId(personId)) }
    }

    private fun addDetailsToChild(
        tx: Database.Read,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        child: TimelineChild,
        childRange: FiniteDateRange,
    ) =
        TimelineChildDetailed(
            id = child.id,
            range = child.range,
            childId = child.childId,
            firstName = child.firstName,
            lastName = child.lastName,
            dateOfBirth = child.dateOfBirth,
            incomes = tx.getIncomes(child.childId, childRange),
            placements = tx.getPlacements(child.childId, childRange),
            serviceNeeds = tx.getServiceNeeds(child.childId, childRange),
            feeAlterations = tx.getFeeAlterations(child.childId, childRange),
            creationModificationMetadata = child.creationModificationMetadata,
            originApplicationAccessible =
                if (child.creationModificationMetadata.createdFromApplication != null)
                    accessControl
                        .getPermittedActions<ApplicationId, Action.Application>(
                            tx,
                            user,
                            clock,
                            child.creationModificationMetadata.createdFromApplication,
                        )
                        .contains(Action.Application.READ)
                else false,
        )
}

data class Timeline(
    val personId: PersonId,
    val firstName: String,
    val lastName: String,
    val feeDecisions: List<TimelineFeeDecision>,
    val valueDecisions: List<TimelineValueDecision>,
    val incomes: List<TimelineIncome>,
    val partners: List<TimelinePartnerDetailed>,
    val children: List<TimelineChildDetailed>,
)

data class TimelineFeeDecision(
    val id: FeeDecisionId,
    override val range: DateRange,
    val status: FeeDecisionStatus,
    val totalFee: Int,
) : WithRange

private fun Database.Read.getFeeDecisions(personId: PersonId, range: FiniteDateRange) =
    findFeeDecisionsForHeadOfFamily(
            headOfFamilyId = personId,
            period = range.asDateRange(),
            status = null,
        )
        .map {
            TimelineFeeDecision(
                id = it.id,
                range = it.validDuring,
                status = it.status,
                totalFee = it.totalFee,
            )
        }
        .sortedBy { it.range.start }

data class TimelineValueDecision(
    val id: VoucherValueDecisionId,
    override val range: DateRange,
    val status: VoucherValueDecisionStatus,
) : WithRange

private fun Database.Read.getValueDecisions(personId: PersonId, range: FiniteDateRange) =
    createQuery {
            sql(
                """
SELECT id, daterange(valid_from, valid_to, '[]') as range, status
FROM voucher_value_decision
WHERE head_of_family_id = ${bind(personId)} AND daterange(valid_from, valid_to, '[]') && ${bind(range)}
ORDER BY valid_from
"""
            )
        }
        .toList<TimelineValueDecision>()

data class TimelineIncome(
    val id: IncomeId,
    override val range: DateRange,
    val effect: IncomeEffect,
) : WithRange

private fun Database.Read.getIncomes(personId: PersonId, range: FiniteDateRange) =
    createQuery {
            sql(
                """
SELECT id, daterange(valid_from, valid_to, '[]') as range, effect
FROM income
WHERE person_id = ${bind(personId)} AND daterange(valid_from, valid_to, '[]') && ${bind(range)}
ORDER BY valid_from
"""
            )
        }
        .toList<TimelineIncome>()

data class TimelinePartner(
    val id: PartnershipId,
    override val range: DateRange,
    val partnerId: PersonId,
    val firstName: String,
    val lastName: String,
    val creationModificationMetadata: CreationModificationMetadata,
) : WithRange

data class TimelinePartnerDetailed(
    val id: PartnershipId,
    override val range: DateRange,
    val partnerId: PersonId,
    val firstName: String,
    val lastName: String,
    val feeDecisions: List<TimelineFeeDecision>,
    val valueDecisions: List<TimelineValueDecision>,
    val incomes: List<TimelineIncome>,
    val children: List<TimelineChildDetailed>,
    val creationModificationMetadata: CreationModificationMetadata,
    val originApplicationAccessible: Boolean,
) : WithRange

private fun Database.Read.getPartners(personId: PersonId, range: FiniteDateRange) =
    this.getPartnersForPerson(
            personId = personId,
            includeConflicts = false,
            period = range.asDateRange(),
        )
        .map {
            TimelinePartner(
                id = it.partnershipId,
                range = DateRange(it.startDate, it.endDate),
                partnerId = it.person.id,
                firstName = it.person.firstName,
                lastName = it.person.lastName,
                creationModificationMetadata = it.creationModificationMetadata,
            )
        }
        .sortedBy { it.range.start }

data class TimelineChild(
    val id: ParentshipId,
    override val range: DateRange,
    val childId: PersonId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val creationModificationMetadata: CreationModificationMetadata,
) : WithRange

data class TimelineChildDetailed(
    val id: ParentshipId,
    override val range: DateRange,
    val childId: PersonId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val incomes: List<TimelineIncome>,
    val placements: List<TimelinePlacement>,
    val serviceNeeds: List<TimelineServiceNeed>,
    val feeAlterations: List<TimelineFeeAlteration>,
    val creationModificationMetadata: CreationModificationMetadata,
    val originApplicationAccessible: Boolean,
) : WithRange

private fun Database.Read.getChildren(personId: PersonId, range: FiniteDateRange) =
    getParentships(
            headOfChildId = personId,
            childId = null,
            includeConflicts = false,
            period = range.asDateRange(),
        )
        .map {
            TimelineChild(
                id = it.id,
                range = DateRange(it.startDate, it.endDate),
                childId = it.child.id,
                firstName = it.child.firstName,
                lastName = it.child.lastName,
                dateOfBirth = it.child.dateOfBirth,
                creationModificationMetadata = it.creationModificationMetadata,
            )
        }
        .sortedBy { it.range.start }

data class TimelinePlacement(
    val id: PlacementId,
    override val range: DateRange,
    val type: PlacementType,
    @Nested("unit") val unit: TimelinePlacementUnit,
) : WithRange

data class TimelinePlacementUnit(val id: DaycareId, val name: String)

private fun Database.Read.getPlacements(personId: PersonId, range: FiniteDateRange) =
    createQuery {
            sql(
                """
SELECT
    pl.id,
    daterange(pl.start_date, pl.end_date, '[]') as range, 
    pl.type,
    d.id AS unit_id,
    d.name AS unit_name
FROM placement pl
JOIN daycare d on d.id = pl.unit_id
WHERE pl.child_id = ${bind(personId)} AND daterange(start_date, end_date, '[]') && ${bind(range)}
ORDER BY pl.start_date
"""
            )
        }
        .toList<TimelinePlacement>()

data class TimelineServiceNeed(
    val id: ServiceNeedId,
    override val range: DateRange,
    val name: String,
) : WithRange

private fun Database.Read.getServiceNeeds(personId: PersonId, range: FiniteDateRange) =
    createQuery {
            sql(
                """
SELECT
    sn.id,
    daterange(sn.start_date, sn.end_date, '[]') as range, 
    sno.name_fi AS name
FROM service_need sn
JOIN placement pl on sn.placement_id = pl.id
JOIN service_need_option sno on sn.option_id = sno.id
WHERE pl.child_id = ${bind(personId)} AND daterange(sn.start_date, sn.end_date, '[]') && ${bind(range)}
ORDER BY pl.start_date
"""
            )
        }
        .toList<TimelineServiceNeed>()

data class TimelineFeeAlteration(
    val id: FeeAlterationId,
    override val range: DateRange,
    val type: FeeAlterationType,
    val amount: Int,
    val absolute: Boolean,
    val notes: String,
) : WithRange

private fun Database.Read.getFeeAlterations(personId: PersonId, range: FiniteDateRange) =
    createQuery {
            sql(
                """
SELECT id, daterange(valid_from, valid_to, '[]') as range, type, amount, is_absolute as absolute, notes
FROM fee_alteration
WHERE person_id = ${bind(personId)} AND daterange(valid_from, valid_to, '[]') && ${bind(range)}
ORDER BY valid_from
"""
            )
        }
        .toList<TimelineFeeAlteration>()
