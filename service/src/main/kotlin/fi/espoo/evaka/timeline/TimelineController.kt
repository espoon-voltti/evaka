// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.timeline

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.invoicing.service.WithRange
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/timeline")
class TimelineController(private val accessControl: AccessControl) {
    @GetMapping
    fun getTimeline(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam personId: PersonId
    ): Timeline {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_TIMELINE,
                        personId
                    )

                    Timeline(
                        personId = personId,
                        feeDecisions = tx.getFeeDecisions(personId),
                        incomes = tx.getIncomes(personId),
                        partners =
                            tx.getPartners(personId).map { partner ->
                                TimelinePartnerDetailed(
                                    id = partner.id,
                                    range = partner.range,
                                    partnerId = partner.partnerId,
                                    firstName = partner.firstName,
                                    lastName = partner.lastName,
                                    feeDecisions =
                                        tx.getFeeDecisions(partner.partnerId).filter {
                                            it.range.overlaps(partner.range)
                                        },
                                    incomes = tx.getIncomes(partner.partnerId),
                                    children =
                                        tx.getChildren(partner.partnerId)
                                            .filter { it.range.overlaps(partner.range) }
                                            .map { child ->
                                                TimelineChildDetailed(
                                                    id = child.id,
                                                    range = child.range,
                                                    childId = child.childId,
                                                    firstName = child.firstName,
                                                    lastName = child.lastName,
                                                    dateOfBirth = child.dateOfBirth,
                                                    incomes =
                                                        tx.getIncomes(child.childId).filter {
                                                            it.range.overlaps(child.range)
                                                        },
                                                    placements =
                                                        tx.getPlacements(child.childId).filter {
                                                            it.range.overlaps(child.range)
                                                        },
                                                    serviceNeeds =
                                                        tx.getServiceNeeds(child.childId).filter {
                                                            it.range.overlaps(child.range)
                                                        }
                                                )
                                            }
                                )
                            },
                        children =
                            tx.getChildren(personId).map { child ->
                                TimelineChildDetailed(
                                    id = child.id,
                                    range = child.range,
                                    childId = child.childId,
                                    firstName = child.firstName,
                                    lastName = child.lastName,
                                    dateOfBirth = child.dateOfBirth,
                                    incomes =
                                        tx.getIncomes(child.childId).filter {
                                            it.range.overlaps(child.range)
                                        },
                                    placements =
                                        tx.getPlacements(child.childId).filter {
                                            it.range.overlaps(child.range)
                                        },
                                    serviceNeeds =
                                        tx.getServiceNeeds(child.childId).filter {
                                            it.range.overlaps(child.range)
                                        }
                                )
                            }
                    )
                }
            }
            .also { Audit.TimelineRead.log(targetId = personId) }
    }
}

data class Timeline(
    val personId: PersonId,
    val feeDecisions: List<TimelineFeeDecision>,
    val incomes: List<TimelineIncome>,
    val partners: List<TimelinePartnerDetailed>,
    val children: List<TimelineChildDetailed>
)

data class TimelineFeeDecision(
    val id: FeeDecisionId,
    override val range: DateRange,
    val status: FeeDecisionStatus,
    val totalFee: Int
) : WithRange

private fun Database.Read.getFeeDecisions(personId: PersonId) =
    createQuery<Any> {
            sql(
                """
SELECT id, valid_during as range, status, total_fee
FROM fee_decision
WHERE head_of_family_id = ${bind(personId)}
ORDER BY lower(valid_during)
"""
            )
        }
        .mapTo<TimelineFeeDecision>()
        .list()

data class TimelineIncome(
    val id: IncomeId,
    override val range: DateRange,
    val effect: IncomeEffect
) : WithRange

private fun Database.Read.getIncomes(personId: PersonId) =
    createQuery<Any> {
            sql(
                """
SELECT id, daterange(valid_from, valid_to, '[]') as range, effect
FROM income
WHERE person_id = ${bind(personId)}
ORDER BY valid_from
"""
            )
        }
        .mapTo<TimelineIncome>()
        .list()

data class TimelinePartner(
    val id: PartnershipId,
    override val range: DateRange,
    val partnerId: PersonId,
    val firstName: String,
    val lastName: String
) : WithRange

data class TimelinePartnerDetailed(
    val id: PartnershipId,
    override val range: DateRange,
    val partnerId: PersonId,
    val firstName: String,
    val lastName: String,
    val feeDecisions: List<TimelineFeeDecision>,
    val incomes: List<TimelineIncome>,
    val children: List<TimelineChildDetailed>
) : WithRange

private fun Database.Read.getPartners(personId: PersonId) =
    createQuery<Any> {
            sql(
                """
SELECT 
    fp2.partnership_id AS id, 
    daterange(fp2.start_date, fp2.end_date, '[]') as range, 
    fp2.person_id AS partnerId, 
    first_name, 
    last_name
FROM fridge_partner fp1
JOIN fridge_partner fp2 ON fp2.partnership_id = fp1.partnership_id AND fp2.indx <> fp1.indx
JOIN person p on fp2.person_id = p.id
WHERE fp1.person_id = ${bind(personId)} AND fp1.conflict = FALSE AND fp2.conflict = FALSE
ORDER BY fp2.start_date
"""
            )
        }
        .mapTo<TimelinePartner>()
        .list()

data class TimelineChild(
    val id: ParentshipId,
    override val range: DateRange,
    val childId: PersonId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate
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
    val serviceNeeds: List<TimelineServiceNeed>
) : WithRange

private fun Database.Read.getChildren(personId: PersonId) =
    createQuery<Any> {
            sql(
                """
SELECT
    fc.id,
    daterange(fc.start_date, fc.end_date, '[]') as range, 
    child_id, 
    first_name, 
    last_name,
    date_of_birth
FROM fridge_child fc
JOIN person p on fc.child_id = p.id
WHERE fc.head_of_child = ${bind(personId)} AND fc.conflict = FALSE
ORDER BY fc.start_date
"""
            )
        }
        .mapTo<TimelineChild>()
        .list()

data class TimelinePlacement(
    val id: PlacementId,
    override val range: DateRange,
    val type: PlacementType,
    @Nested("unit") val unit: TimelinePlacementUnit
) : WithRange

data class TimelinePlacementUnit(val id: DaycareId, val name: String)

private fun Database.Read.getPlacements(personId: PersonId) =
    createQuery<Any> {
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
WHERE pl.child_id = ${bind(personId)}
ORDER BY pl.start_date
"""
            )
        }
        .mapTo<TimelinePlacement>()
        .list()

data class TimelineServiceNeed(
    val id: ServiceNeedId,
    override val range: DateRange,
    val name: String
) : WithRange

private fun Database.Read.getServiceNeeds(personId: PersonId) =
    createQuery<Any> {
            sql(
                """
SELECT
    sn.id,
    daterange(sn.start_date, sn.end_date, '[]') as range, 
    sno.name_fi AS name
FROM service_need sn
JOIN placement pl on sn.placement_id = pl.id
JOIN service_need_option sno on sn.option_id = sno.id
WHERE pl.child_id = ${bind(personId)}
ORDER BY pl.start_date
"""
            )
        }
        .mapTo<TimelineServiceNeed>()
        .list()
