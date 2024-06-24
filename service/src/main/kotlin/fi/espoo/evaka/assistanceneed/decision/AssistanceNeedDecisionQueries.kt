// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate

fun Database.Transaction.insertAssistanceNeedDecision(
    childId: ChildId,
    data: AssistanceNeedDecisionForm,
    processId: ArchivedProcessId?,
    user: AuthenticatedUser.Employee
): AssistanceNeedDecision {
    val id =
        createQuery {
            sql(
                """
INSERT INTO assistance_need_decision (
  child_id, process_id, created_by, validity_period, status, language, decision_made, sent_for_decision,
  selected_unit, pedagogical_motivation, structural_motivation_opt_smaller_group,
  structural_motivation_opt_special_group, structural_motivation_opt_small_group,
  structural_motivation_opt_group_assistant, structural_motivation_opt_child_assistant,
  structural_motivation_opt_additional_staff, structural_motivation_description, care_motivation,
  service_opt_consultation_special_ed, service_opt_part_time_special_ed, service_opt_full_time_special_ed,
  service_opt_interpretation_and_assistance_services, service_opt_special_aides, services_motivation,
  expert_responsibilities, guardians_heard_on, view_of_guardians, other_representative_heard, other_representative_details, 
  assistance_levels, motivation_for_decision, decision_maker_employee_id,
  decision_maker_title, preparer_1_employee_id, preparer_1_title, preparer_2_employee_id, preparer_2_title,
  preparer_1_phone_number, preparer_2_phone_number
)
VALUES (
    ${bind(childId)}, 
    ${bind(processId)},
    ${bind(user.id)},
    ${bind(data.validityPeriod)},
    ${bind(data.status)},
    ${bind(data.language)},
    ${bind(data.decisionMade)},
    ${bind(data.sentForDecision)},
    ${bind(data.selectedUnit?.id)}, 
    ${bind(data.pedagogicalMotivation)},
    ${bind(data.structuralMotivationOptions.smallerGroup)},
    ${bind(data.structuralMotivationOptions.specialGroup)},
    ${bind(data.structuralMotivationOptions.smallGroup)},
    ${bind(data.structuralMotivationOptions.groupAssistant)}, 
    ${bind(data.structuralMotivationOptions.childAssistant)},
    ${bind(data.structuralMotivationOptions.additionalStaff)},
    ${bind(data.structuralMotivationDescription)},
    ${bind(data.careMotivation)},
    ${bind(data.serviceOptions.consultationSpecialEd)},
    ${bind(data.serviceOptions.partTimeSpecialEd)},
    ${bind(data.serviceOptions.fullTimeSpecialEd)},
    ${bind(data.serviceOptions.interpretationAndAssistanceServices)},
    ${bind(data.serviceOptions.specialAides)},
    ${bind(data.servicesMotivation)},
    ${bind(data.expertResponsibilities)},
    ${bind(data.guardiansHeardOn)},
    ${bind(data.viewOfGuardians)},
    ${bind(data.otherRepresentativeHeard)},
    ${bind(data.otherRepresentativeDetails)}, 
    ${bind(data.assistanceLevels)},
    ${bind(data.motivationForDecision)},
    ${bind(data.decisionMaker?.employeeId)},
    ${bind(data.decisionMaker?.title)},
    ${bind(data.preparedBy1?.employeeId)},
    ${bind(data.preparedBy1?.title)},
    ${bind(data.preparedBy2?.employeeId)},
    ${bind(data.preparedBy2?.title)},
    ${bind(data.preparedBy1?.phoneNumber)},
    ${bind(data.preparedBy2?.phoneNumber)}
)
RETURNING id
"""
            )
        }.exactlyOne<AssistanceNeedDecisionId>()

    executeBatch(data.guardianInfo) {
        sql(
            """
INSERT INTO assistance_need_decision_guardian (
    assistance_need_decision_id,
    person_id,
    is_heard,
    details
) VALUES (
    ${bind(id)},
    ${bind { it.personId }},
    ${bind { it.isHeard }},
    ${bind { it.details }}
)
"""
        )
    }

    return getAssistanceNeedDecisionById(id)
}

fun Database.Read.getAssistanceNeedDecisionById(id: AssistanceNeedDecisionId): AssistanceNeedDecision =
    createQuery {
        sql(
            """
SELECT ad.id, decision_number, child_id, concat(child.first_name, ' ', child.last_name) child_name, validity_period, status,
  ad.language, decision_made, sent_for_decision, pedagogical_motivation, structural_motivation_opt_smaller_group,
  structural_motivation_opt_special_group, structural_motivation_opt_small_group,
  structural_motivation_opt_group_assistant, structural_motivation_opt_child_assistant,
  structural_motivation_opt_additional_staff, structural_motivation_description, care_motivation,
  service_opt_consultation_special_ed, service_opt_part_time_special_ed, service_opt_full_time_special_ed,
  service_opt_interpretation_and_assistance_services, service_opt_special_aides, services_motivation,
  expert_responsibilities, guardians_heard_on, view_of_guardians, other_representative_heard, other_representative_details, 
  assistance_levels, motivation_for_decision, annulment_reason,
  decision_maker_employee_id, decision_maker_title, concat(coalesce(dm.preferred_first_name, dm.first_name), ' ', dm.last_name) decision_maker_name,
  preparer_1_employee_id, preparer_1_title, concat(coalesce(p1.preferred_first_name, p1.first_name), ' ', p1.last_name) preparer_1_name, preparer_1_phone_number,
  preparer_2_employee_id, preparer_2_title, concat(coalesce(p2.preferred_first_name, p2.first_name), ' ', p2.last_name) preparer_2_name, preparer_2_phone_number,
  coalesce(jsonb_agg(jsonb_build_object(
    'id', dg.id,
    'personId', dg.person_id,
    'name', concat(p.last_name, ' ', p.first_name),
    'isHeard', dg.is_heard,
    'details', dg.details
  )) FILTER (WHERE dg.id IS NOT NULL), '[]') as guardian_info,
  selected_unit selected_unit_id, unit.name selected_unit_name, unit.street_address selected_unit_street_address,
  unit.postal_code selected_unit_postal_code, unit.post_office selected_unit_post_office,
  (document_key IS NOT NULL) has_document, child.date_of_birth child_date_of_birth
FROM assistance_need_decision ad
LEFT JOIN assistance_need_decision_guardian dg ON dg.assistance_need_decision_id = ad.id
LEFT JOIN person p ON p.id = dg.person_id
LEFT JOIN daycare unit ON unit.id = ad.selected_unit
LEFT JOIN employee p1 ON p1.id = ad.preparer_1_employee_id
LEFT JOIN employee p2 ON p2.id = ad.preparer_2_employee_id
LEFT JOIN employee dm ON dm.id = ad.decision_maker_employee_id
LEFT JOIN person child ON child.id = ad.child_id
WHERE ad.id = ${bind(id)}
GROUP BY ad.id, child_id, validity_period, unit.id, p1.id, p2.id, dm.id, child.id;
"""
        )
    }.exactlyOneOrNull<AssistanceNeedDecision>()
        ?: throw NotFound("Assistance need decision $id not found")

fun Database.Transaction.updateAssistanceNeedDecision(
    id: AssistanceNeedDecisionId,
    data: AssistanceNeedDecisionForm,
    decisionMakerHasOpened: Boolean? = null
) {
    createUpdate {
        sql(
            """
UPDATE assistance_need_decision
SET 
    validity_period = ${bind(data.validityPeriod)}, 
    status = ${bind(data.status)},
    language = ${bind(data.language)},
    decision_made = ${bind(data.decisionMade)},
    sent_for_decision = ${bind(data.sentForDecision)},
    selected_unit = ${bind(data.selectedUnit?.id)},
    pedagogical_motivation = ${bind(data.pedagogicalMotivation)},
    structural_motivation_opt_smaller_group = ${bind(data.structuralMotivationOptions.smallerGroup)},
    structural_motivation_opt_special_group = ${bind(data.structuralMotivationOptions.specialGroup)},
    structural_motivation_opt_small_group = ${bind(data.structuralMotivationOptions.smallGroup)},
    structural_motivation_opt_group_assistant = ${bind(data.structuralMotivationOptions.groupAssistant)},
    structural_motivation_opt_child_assistant = ${bind(data.structuralMotivationOptions.childAssistant)},
    structural_motivation_opt_additional_staff = ${bind(data.structuralMotivationOptions.additionalStaff)},
    structural_motivation_description = ${bind(data.structuralMotivationDescription)},
    care_motivation = ${bind(data.careMotivation)},
    service_opt_consultation_special_ed=${bind(data.serviceOptions.consultationSpecialEd)},
    service_opt_part_time_special_ed = ${bind(data.serviceOptions.partTimeSpecialEd)},
    service_opt_full_time_special_ed = ${bind(data.serviceOptions.fullTimeSpecialEd)},
    service_opt_interpretation_and_assistance_services = ${bind(data.serviceOptions.interpretationAndAssistanceServices)},
    service_opt_special_aides = ${bind(data.serviceOptions.specialAides)},
    services_motivation = ${bind(data.servicesMotivation)},
    expert_responsibilities = ${bind(data.expertResponsibilities)},
    guardians_heard_on = ${bind(data.guardiansHeardOn)},
    view_of_guardians = ${bind(data.viewOfGuardians)},
    other_representative_heard = ${bind(data.otherRepresentativeHeard)},
    other_representative_details = ${bind(data.otherRepresentativeDetails)},
    assistance_levels = ${bind(data.assistanceLevels)},
    motivation_for_decision = ${bind(data.motivationForDecision)},
    decision_maker_employee_id = ${bind(data.decisionMaker?.employeeId)},
    decision_maker_title = ${bind(data.decisionMaker?.title)},
    preparer_1_employee_id = ${bind(data.preparedBy1?.employeeId)},
    preparer_1_title = ${bind(data.preparedBy1?.title)},
    preparer_1_phone_number = ${bind(data.preparedBy1?.phoneNumber)},
    preparer_2_employee_id = ${bind(data.preparedBy2?.employeeId)},
    preparer_2_title = ${bind(data.preparedBy2?.title)},
    preparer_2_phone_number = ${bind(data.preparedBy2?.phoneNumber)},
    decision_maker_has_opened = COALESCE(${bind(decisionMakerHasOpened)}, decision_maker_has_opened)
WHERE id = ${bind(id)} AND status IN ('DRAFT', 'NEEDS_WORK')
"""
        )
    }.updateExactlyOne()

    executeBatch(data.guardianInfo) {
        sql(
            """
UPDATE assistance_need_decision_guardian SET 
    is_heard = ${bind { it.isHeard }},
    details = ${bind { it.details }}
WHERE id = ${bind { it.id }}
"""
        )
    }
}

fun Database.Read.getAssistanceNeedDecisionsByChildId(
    childId: ChildId,
    filter: AccessControlFilter<AssistanceNeedDecisionId>
): List<AssistanceNeedDecisionBasics> =
    createQuery {
        sql(
            """
SELECT ad.id, validity_period, status, decision_made, sent_for_decision, ad.created,
    selected_unit selected_unit_id, unit.name selected_unit_name
FROM assistance_need_decision ad
LEFT JOIN daycare unit ON unit.id = selected_unit
WHERE child_id = ${bind(childId)} AND ${predicate(filter.forTable("ad"))}
"""
        )
    }.toList()

fun Database.Transaction.deleteAssistanceNeedDecision(id: AssistanceNeedDecisionId): Boolean =
    createQuery {
        sql(
            """
                DELETE FROM assistance_need_decision
                WHERE id = ${bind(id)} AND status IN ('DRAFT', 'NEEDS_WORK')
                RETURNING id
                """
        )
    }.exactlyOneOrNull<AssistanceNeedDecisionId>() != null

fun Database.Transaction.markAssistanceNeedDecisionAsOpened(id: AssistanceNeedDecisionId) {
    createUpdate {
        sql(
            """
                UPDATE assistance_need_decision
                SET decision_maker_has_opened = TRUE
                WHERE id = ${bind(id)}
                """
        )
    }.updateExactlyOne()
}

fun Database.Read.getAssistanceNeedDecisionsForCitizen(
    today: LocalDate,
    userId: PersonId
): List<AssistanceNeedDecisionCitizenListItem> =
    createQuery {
        sql(
            """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = ${bind(userId)}
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(today)}
)
SELECT ad.id, ad.child_id, validity_period, status, decision_made, assistance_levels,
    selected_unit AS selected_unit_id, unit.name AS selected_unit_name, annulment_reason,
    coalesce(${bind(userId)} = ANY(unread_guardian_ids), false) AS is_unread
FROM children c
JOIN assistance_need_decision ad ON ad.child_id = c.child_id
LEFT JOIN daycare unit ON unit.id = selected_unit
WHERE status IN ('REJECTED', 'ACCEPTED', 'ANNULLED') AND decision_made IS NOT NULL
"""
        )
    }.toList()

fun Database.Read.getAssistanceNeedDecisionDocumentKey(id: AssistanceNeedDecisionId): String? =
    createQuery {
        sql(
            """
                SELECT document_key
                FROM assistance_need_decision ad
                WHERE ad.id = ${bind(id)}
                """
        )
    }.exactlyOneOrNull()

fun Database.Transaction.updateAssistanceNeedDocumentKey(
    id: AssistanceNeedDecisionId,
    key: String
) {
    createUpdate {
        sql(
            """
                UPDATE assistance_need_decision
                SET document_key = ${bind(key)}
                WHERE id = ${bind(id)}
                """
        )
    }.updateExactlyOne()
}

fun Database.Transaction.markAssistanceNeedDecisionAsReadByGuardian(
    assistanceNeedDecisionId: AssistanceNeedDecisionId,
    guardianId: PersonId
) {
    createUpdate {
        sql(
            """
                UPDATE assistance_need_decision
                SET unread_guardian_ids = array_remove(unread_guardian_ids, ${bind(guardianId)})
                WHERE id = ${bind(assistanceNeedDecisionId)}
                """
        )
    }.updateExactlyOne()
}

fun Database.Read.getAssistanceNeedDecisionsUnreadCountsForCitizen(
    today: LocalDate,
    userId: PersonId
): List<UnreadAssistanceNeedDecisionItem> =
    createQuery {
        sql(
            """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = ${bind(userId)}
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(today)}
)
SELECT ad.child_id, COUNT(ad.child_id) as count
FROM assistance_need_decision ad
JOIN children c ON c.child_id = ad.child_id
WHERE (${bind(userId)} = ANY(ad.unread_guardian_ids)) AND status IN ('REJECTED', 'ACCEPTED')
GROUP BY ad.child_id
"""
        )
    }.toList()

fun Database.Transaction.decideAssistanceNeedDecision(
    id: AssistanceNeedDecisionId,
    status: AssistanceNeedDecisionStatus,
    decisionMade: LocalDate?,
    unreadGuardianIds: List<PersonId>?,
    validTo: LocalDate?
) {
    createUpdate {
        sql(
            """
UPDATE assistance_need_decision
SET 
    status = ${bind(status)},
    decision_made = ${bind(decisionMade)},
    unread_guardian_ids = ${bind(unreadGuardianIds)},
    validity_period = daterange(lower(validity_period), ${bind(validTo)}, '[]')
WHERE id = ${bind(id)} AND status IN ('DRAFT', 'NEEDS_WORK')
"""
        )
    }.updateExactlyOne()
}

fun Database.Transaction.endActiveAssistanceNeedDecisions(
    excludingId: AssistanceNeedDecisionId,
    endDate: LocalDate,
    childId: ChildId
) {
    execute {
        sql(
            """
UPDATE assistance_need_decision
SET validity_period = daterange(lower(validity_period), ${bind(endDate)}, '[]')
WHERE
    id <> ${bind(excludingId)} AND
    validity_period @> ${bind(endDate)} AND
    child_id = ${bind(childId)} AND
    status = 'ACCEPTED'
"""
        )
    }
}

fun Database.Transaction.endActiveDaycareAssistanceDecisions(date: LocalDate) =
    execute {
        sql(
            """
WITH daycare_assistance_decision_with_new_end_date AS (
    SELECT daycare_assistance_decision.id, max(placement.end_date) AS new_end_date
    FROM assistance_need_decision daycare_assistance_decision
    JOIN placement ON daycare_assistance_decision.child_id = placement.child_id
     AND daycare_assistance_decision.selected_unit = placement.unit_id
     AND daycare_assistance_decision.validity_period @> placement.end_date
    WHERE daycare_assistance_decision.status = 'ACCEPTED'
      AND upper_inf(daycare_assistance_decision.validity_period)
      AND placement.type IN (
        'DAYCARE',
        'DAYCARE_PART_TIME',
        'DAYCARE_FIVE_YEAR_OLDS',
        'DAYCARE_PART_TIME_FIVE_YEAR_OLDS',
        'PRESCHOOL_DAYCARE',
        'PRESCHOOL_DAYCARE_ONLY',
        'PREPARATORY_DAYCARE',
        'PREPARATORY_DAYCARE_ONLY')
    GROUP BY daycare_assistance_decision.id
    HAVING max(placement.end_date) < ${bind(date)}
)
UPDATE assistance_need_decision
SET validity_period = daterange(lower(validity_period), new_end_date, '[]')
FROM daycare_assistance_decision_with_new_end_date
WHERE daycare_assistance_decision_with_new_end_date.id = assistance_need_decision.id
"""
        )
    }

fun Database.Read.getNextAssistanceNeedDecisionValidFrom(
    childId: ChildId,
    startDate: LocalDate
) = createQuery {
    sql(
        """
SELECT min(lower(validity_period))
FROM assistance_need_decision
WHERE child_id = ${bind(childId)}
  AND lower(validity_period) >= ${bind(startDate)}
  AND status = 'ACCEPTED'
"""
    )
}.mapTo<LocalDate>()
    .exactlyOneOrNull()

fun Database.Transaction.annulAssistanceNeedDecision(
    id: AssistanceNeedDecisionId,
    reason: String
) = createUpdate {
    sql(
        """
UPDATE assistance_need_decision
SET status = 'ANNULLED', annulment_reason = ${bind(reason)}
WHERE id = ${bind(id)}
"""
    )
}.updateExactlyOne()
