// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.preschooldecision

import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.assistanceneed.decision.UnreadAssistanceNeedDecisionItem
import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate

fun Database.Transaction.insertEmptyAssistanceNeedPreschoolDecisionDraft(
    childId: ChildId,
    processId: ArchivedProcessId?,
    user: AuthenticatedUser.Employee,
    language: OfficialLanguage = OfficialLanguage.FI
): AssistanceNeedPreschoolDecision =
    createQuery {
        sql(
            """
                INSERT INTO assistance_need_preschool_decision (child_id, language, process_id, created_by)
                VALUES (${bind(childId)}, ${bind(language)}, ${bind(processId)}, ${bind(user.id)})
                RETURNING id
                """
        )
    }.exactlyOne<AssistanceNeedPreschoolDecisionId>()
        .also { decisionId ->
            execute {
                sql(
                    """
                    INSERT INTO assistance_need_preschool_decision_guardian (
                        assistance_need_decision_id, person_id
                    )
                    SELECT ${bind(decisionId)}, guardian_id
                    FROM guardian
                    WHERE child_id = ${bind(childId)}
                    """
                )
            }
        }.let(::getAssistanceNeedPreschoolDecisionById)

fun Database.Read.getAssistanceNeedPreschoolDecisionById(id: AssistanceNeedPreschoolDecisionId): AssistanceNeedPreschoolDecision =
    createQuery {
        sql(
            """
SELECT 
    ad.id, 
    ad.decision_number,
    ad.child_id, 
    concat(child.first_name, ' ', child.last_name) child_name,
    child.date_of_birth child_date_of_birth,
    ad.status,
    ad.language,
    
    ad.type,
    ad.valid_from,
    ad.valid_to,
    
    ad.extended_compulsory_education,
    ad.extended_compulsory_education_info,
    
    ad.granted_assistance_service,
    ad.granted_interpretation_service,
    ad.granted_assistive_devices,
    ad.granted_services_basis,
    
    ad.selected_unit,
    ad.primary_group,
    ad.decision_basis,
    
    ad.basis_document_pedagogical_report,
    ad.basis_document_psychologist_statement,
    ad.basis_document_social_report,
    ad.basis_document_doctor_statement,
    ad.basis_document_pedagogical_report_date,
    ad.basis_document_psychologist_statement_date,
    ad.basis_document_social_report_date,
    ad.basis_document_doctor_statement_date,
    ad.basis_document_other_or_missing,
    ad.basis_document_other_or_missing_info,
    ad.basis_documents_info,
    
    ad.guardians_heard_on,
    coalesce(jsonb_agg(jsonb_build_object(
      'id', dg.id,
      'personId', dg.person_id,
      'name', concat(p.last_name, ' ', p.first_name),
      'isHeard', dg.is_heard,
      'details', dg.details
    )) FILTER (WHERE dg.id IS NOT NULL), '[]') as guardian_info,            
    ad.other_representative_heard,
    ad.other_representative_details,
    ad.view_of_guardians,
    
    ad.preparer_1_employee_id,
    ad.preparer_1_title,
    ad.preparer_1_phone_number,
    ad.preparer_2_employee_id,
    ad.preparer_2_title,
    ad.preparer_2_phone_number,
    ad.decision_maker_employee_id,
    ad.decision_maker_title,
    
    ad.sent_for_decision,
    ad.decision_made,
    ad.decision_maker_has_opened,
    ad.annulment_reason,
    (ad.document_key IS NOT NULL) has_document,
    
    d.name as unit_name,
    d.street_address as unit_street_address,
    d.postal_code as unit_postal_code,
    d.post_office as unit_post_office,
    CASE WHEN preparer1.id IS NOT NULL THEN coalesce(preparer1.preferred_first_name, preparer1.first_name) || ' ' || preparer1.last_name END as preparer_1_name,
    CASE WHEN preparer2.id IS NOT NULL THEN coalesce(preparer2.preferred_first_name, preparer2.first_name) || ' ' || preparer2.last_name END as preparer_2_name,
    CASE WHEN decision_maker.id IS NOT NULL THEN coalesce(decision_maker.preferred_first_name, decision_maker.first_name) || ' ' || decision_maker.last_name END as decision_maker_name
FROM assistance_need_preschool_decision ad
JOIN person child ON child.id = ad.child_id
LEFT JOIN assistance_need_preschool_decision_guardian dg ON dg.assistance_need_decision_id = ad.id
LEFT JOIN person p ON p.id = dg.person_id
LEFT JOIN daycare d ON ad.selected_unit = d.id
LEFT JOIN employee preparer1 ON ad.preparer_1_employee_id = preparer1.id
LEFT JOIN employee preparer2 ON ad.preparer_2_employee_id = preparer2.id
LEFT JOIN employee decision_maker ON ad.decision_maker_employee_id = decision_maker.id
WHERE ad.id = ${bind(id)}
GROUP BY ad.id, child.id, d.id, preparer1.id, preparer2.id, decision_maker.id;
"""
        )
    }.exactlyOneOrNull() ?: throw NotFound("Assistance need preschool decision $id not found")

fun Database.Transaction.updateAssistanceNeedPreschoolDecision(
    id: AssistanceNeedPreschoolDecisionId,
    data: AssistanceNeedPreschoolDecisionForm,
    decisionMakerHasOpened: Boolean? = null
) {
    createUpdate {
        sql(
            """
UPDATE assistance_need_preschool_decision
SET 
    language = ${bind(data.language)},
    type = ${bind(data.type)},
    valid_from = ${bind(data.validFrom)},
    extended_compulsory_education = ${bind(data.extendedCompulsoryEducation)},
    extended_compulsory_education_info = ${bind(data.extendedCompulsoryEducationInfo)},
    granted_assistance_service = ${bind(data.grantedAssistanceService)},
    granted_interpretation_service = ${bind(data.grantedInterpretationService)},
    granted_assistive_devices = ${bind(data.grantedAssistiveDevices)},
    granted_services_basis = ${bind(data.grantedServicesBasis)},
    selected_unit = ${bind(data.selectedUnit)},
    primary_group = ${bind(data.primaryGroup)},
    decision_basis = ${bind(data.decisionBasis)},
    basis_document_pedagogical_report = ${bind(data.basisDocumentPedagogicalReport)},
    basis_document_psychologist_statement = ${bind(data.basisDocumentPsychologistStatement)},
    basis_document_social_report = ${bind(data.basisDocumentSocialReport)},
    basis_document_doctor_statement = ${bind(data.basisDocumentDoctorStatement)},
    basis_document_pedagogical_report_date = ${bind(data.basisDocumentPedagogicalReportDate)},
    basis_document_psychologist_statement_date = ${bind(data.basisDocumentPsychologistStatementDate)},
    basis_document_social_report_date = ${bind(data.basisDocumentSocialReportDate)},
    basis_document_doctor_statement_date = ${bind(data.basisDocumentDoctorStatementDate)},
    basis_document_other_or_missing = ${bind(data.basisDocumentOtherOrMissing)},
    basis_document_other_or_missing_info = ${bind(data.basisDocumentOtherOrMissingInfo)},
    basis_documents_info = ${bind(data.basisDocumentsInfo)},
    guardians_heard_on = ${bind(data.guardiansHeardOn)},
    other_representative_heard = ${bind(data.otherRepresentativeHeard)},
    other_representative_details = ${bind(data.otherRepresentativeDetails)},
    view_of_guardians = ${bind(data.viewOfGuardians)},
    preparer_1_employee_id = ${bind(data.preparer1EmployeeId)},
    preparer_1_title = ${bind(data.preparer1Title)},
    preparer_1_phone_number = ${bind(data.preparer1PhoneNumber)},
    preparer_2_employee_id = ${bind(data.preparer2EmployeeId)},
    preparer_2_title = ${bind(data.preparer2Title)},
    preparer_2_phone_number = ${bind(data.preparer2PhoneNumber)},
    decision_maker_employee_id = ${bind(data.decisionMakerEmployeeId)},
    decision_maker_title = ${bind(data.decisionMakerTitle)},
    decision_maker_has_opened = COALESCE(${bind(decisionMakerHasOpened)}, decision_maker_has_opened)
WHERE id = ${bind(id)} AND (status = 'NEEDS_WORK' OR (status = 'DRAFT' AND sent_for_decision IS NULL))
"""
        )
    }.updateExactlyOne()

    executeBatch(data.guardianInfo) {
        sql(
            """
            UPDATE assistance_need_preschool_decision_guardian SET 
                is_heard = ${bind { it.isHeard }},
                details = ${bind { it.details }}
            WHERE id = ${bind { it.id }}
            """
        )
    }
}

fun Database.Transaction.updateAssistanceNeedPreschoolDecisionToSent(
    id: AssistanceNeedPreschoolDecisionId,
    today: LocalDate
) = createUpdate {
    sql(
        """
                UPDATE assistance_need_preschool_decision
                SET sent_for_decision = ${bind(today)}, status = 'DRAFT'
                WHERE id = ${bind(id)} AND status in ('DRAFT', 'NEEDS_WORK')
                """
    )
}.updateExactlyOne()

fun Database.Transaction.updateAssistanceNeedPreschoolDecisionToNotSent(id: AssistanceNeedPreschoolDecisionId) =
    createUpdate {
        sql(
            """
                UPDATE assistance_need_preschool_decision
                SET sent_for_decision = NULL, status = 'DRAFT'
                WHERE id = ${bind(id)} AND status in ('DRAFT', 'NEEDS_WORK')
                """
        )
    }.updateExactlyOne()

fun Database.Read.getAssistanceNeedPreschoolDecisionsByChildId(childId: ChildId): List<AssistanceNeedPreschoolDecisionBasics> =
    createQuery {
        sql(
            """
SELECT ad.id, ad.child_id, ad.created, ad.status, ad.type, ad.valid_from, ad.valid_to,
    ad.selected_unit selected_unit_id, unit.name selected_unit_name,
    ad.sent_for_decision, ad.decision_made, ad.annulment_reason, ad.unread_guardian_ids
FROM assistance_need_preschool_decision ad
LEFT JOIN daycare unit ON unit.id = selected_unit
WHERE child_id = ${bind(childId)}
ORDER BY ad.valid_from DESC NULLS FIRST, ad.created DESC
"""
        )
    }.toList()

fun Database.Read.getAssistanceNeedPreschoolDecisionsByChildIdUsingFilter(
    childId: ChildId,
    filter: AccessControlFilter<AssistanceNeedPreschoolDecisionId>
): List<AssistanceNeedPreschoolDecisionBasics> =
    createQuery {
        sql(
            """
SELECT ad.id, ad.child_id, ad.created, ad.status, ad.type, ad.valid_from, ad.valid_to,
    ad.selected_unit selected_unit_id, unit.name selected_unit_name,
    ad.sent_for_decision, ad.decision_made, ad.annulment_reason, ad.unread_guardian_ids
FROM assistance_need_preschool_decision ad
LEFT JOIN daycare unit ON unit.id = selected_unit
WHERE child_id = ${bind(childId)} AND ${predicate(filter.forTable("ad"))}
ORDER BY ad.valid_from DESC NULLS FIRST, ad.created DESC;
"""
        )
    }.toList()

fun Database.Transaction.deleteAssistanceNeedPreschoolDecision(id: AssistanceNeedPreschoolDecisionId): Boolean =
    createQuery {
        sql(
            """
DELETE FROM assistance_need_preschool_decision
WHERE id = ${bind(id)} AND status IN ('DRAFT', 'NEEDS_WORK')
RETURNING id
"""
        )
    }.exactlyOneOrNull<AssistanceNeedDecisionId>() != null

fun Database.Transaction.markAssistanceNeedPreschoolDecisionAsOpened(id: AssistanceNeedPreschoolDecisionId) {
    createUpdate {
        sql(
            """
UPDATE assistance_need_preschool_decision
SET decision_maker_has_opened = TRUE
WHERE id = ${bind(id)}
"""
        )
    }.updateExactlyOne()
}

fun Database.Transaction.decideAssistanceNeedPreschoolDecision(
    id: AssistanceNeedPreschoolDecisionId,
    status: AssistanceNeedDecisionStatus,
    decisionMade: LocalDate?,
    unreadGuardianIds: List<PersonId>?,
    validTo: LocalDate?
) {
    createUpdate {
        sql(
            """
UPDATE assistance_need_preschool_decision
SET 
    status = ${bind(status)},
    decision_made = ${bind(decisionMade)},
    unread_guardian_ids = ${bind(unreadGuardianIds)},
    valid_to = ${bind(validTo)}
WHERE id = ${bind(id)} AND status IN ('DRAFT', 'NEEDS_WORK')
"""
        )
    }.updateExactlyOne()
}

fun Database.Transaction.endActiveAssistanceNeedPreschoolDecisions(
    excludingId: AssistanceNeedPreschoolDecisionId,
    endDate: LocalDate,
    childId: ChildId
) = createUpdate {
    sql(
        """
UPDATE assistance_need_preschool_decision
SET valid_to = ${bind(endDate)}
WHERE id <> ${bind(excludingId)}
  AND valid_from <= ${bind(endDate)}
  AND (valid_to IS NULL OR valid_to > ${bind(endDate)})
  AND child_id = ${bind(childId)}
  AND status = 'ACCEPTED'
"""
    )
}.execute()

fun Database.Transaction.endActivePreschoolAssistanceDecisions(date: LocalDate) =
    createUpdate {
        sql(
            """
WITH preschool_assistance_decision_with_new_end_date AS (
    SELECT preschool_assistance_decision.id, max(placement.end_date) AS new_end_date
    FROM assistance_need_preschool_decision preschool_assistance_decision
    JOIN placement ON preschool_assistance_decision.child_id = placement.child_id
     AND preschool_assistance_decision.selected_unit = placement.unit_id
     AND daterange(preschool_assistance_decision.valid_from, preschool_assistance_decision.valid_to, '[]') @> placement.end_date
    WHERE preschool_assistance_decision.status = 'ACCEPTED'
      AND preschool_assistance_decision.valid_to IS NULL
      AND placement.type IN (
        'PRESCHOOL',
        'PRESCHOOL_DAYCARE',
        'PRESCHOOL_CLUB',
        'PREPARATORY',
        'PREPARATORY_DAYCARE')
    GROUP BY preschool_assistance_decision.id
    HAVING max(placement.end_date) < ${bind(date)}
)
UPDATE assistance_need_preschool_decision
SET valid_to = new_end_date
FROM preschool_assistance_decision_with_new_end_date
WHERE preschool_assistance_decision_with_new_end_date.id = assistance_need_preschool_decision.id
"""
        )
    }.execute()

fun Database.Read.getNextAssistanceNeedPreschoolDecisionValidFrom(
    childId: ChildId,
    startDate: LocalDate
) = createQuery {
    sql(
        """
SELECT min(valid_from)
FROM assistance_need_preschool_decision
WHERE child_id = ${bind(childId)}
  AND valid_from >= ${bind(startDate)}
  AND status = 'ACCEPTED'
"""
    )
}.mapTo<LocalDate>()
    .exactlyOneOrNull()

fun Database.Transaction.updateAssistanceNeedPreschoolDocumentKey(
    id: AssistanceNeedPreschoolDecisionId,
    key: String
) {
    createUpdate {
        sql(
            """
UPDATE assistance_need_preschool_decision
SET document_key = ${bind(key)}
WHERE id = ${bind(id)}
"""
        )
    }.updateExactlyOne()
}

fun Database.Transaction.annulAssistanceNeedPreschoolDecision(
    id: AssistanceNeedPreschoolDecisionId,
    reason: String
) {
    createUpdate {
        sql(
            """
UPDATE assistance_need_preschool_decision
SET status = 'ANNULLED', annulment_reason = ${bind(reason)}
WHERE id = ${bind(id)}
"""
        )
    }.updateExactlyOne()
}

fun Database.Read.getAssistanceNeedPreschoolDecisionsForCitizen(
    today: LocalDate,
    userId: PersonId
): List<AssistanceNeedPreschoolDecisionCitizenListItem> {
    val childIds =
        createQuery {
            sql(
                """
SELECT child_id FROM guardian WHERE guardian_id = ${bind(userId)}
UNION ALL 
SELECT child_id FROM foster_parent WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(today)}
"""
            )
        }.toSet<ChildId>()

    return childIds
        .flatMap { childId -> getAssistanceNeedPreschoolDecisionsByChildId(childId) }
        .filter { it.status.isDecided() }
        .mapNotNull {
            if (
                it.validFrom == null ||
                it.type == null ||
                it.decisionMade == null ||
                it.selectedUnit?.name == null
            ) {
                null
            } else {
                AssistanceNeedPreschoolDecisionCitizenListItem(
                    id = it.id,
                    childId = it.childId,
                    validityPeriod = DateRange(it.validFrom, it.validTo),
                    status = it.status,
                    type = it.type,
                    decisionMade = it.decisionMade,
                    unitName = it.selectedUnit.name,
                    annulmentReason = it.annulmentReason,
                    isUnread = it.unreadGuardianIds?.contains(userId) ?: true
                )
            }
        }
}

fun Database.Transaction.markAssistanceNeedPreschoolDecisionAsReadByGuardian(
    decisionId: AssistanceNeedPreschoolDecisionId,
    guardianId: PersonId
) {
    createUpdate {
        sql(
            """
        UPDATE assistance_need_preschool_decision
        SET unread_guardian_ids = array_remove(unread_guardian_ids, ${bind(guardianId)})
        WHERE id = ${bind(decisionId)}
        """
        )
    }.updateExactlyOne()
}

fun Database.Read.getAssistanceNeedPreschoolDecisionsUnreadCountsForCitizen(
    today: LocalDate,
    userId: PersonId
): List<UnreadAssistanceNeedDecisionItem> =
    createQuery {
        sql(
            """
SELECT ad.child_id, COUNT(ad.id) as count
FROM assistance_need_preschool_decision ad
WHERE (${bind(userId)} = ANY(ad.unread_guardian_ids)) 
    AND status IN ('REJECTED', 'ACCEPTED')
    AND ad.child_id IN (
        SELECT child_id FROM guardian WHERE guardian_id = ${bind(userId)}
        UNION ALL 
        SELECT child_id FROM foster_parent WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(today)}
    )
GROUP BY ad.child_id
"""
        )
    }.toList()

fun Database.Read.getAssistanceNeedPreschoolDecisionDocumentKey(id: AssistanceNeedPreschoolDecisionId): String? =
    createQuery {
        sql(
            """
SELECT document_key
FROM assistance_need_preschool_decision ad
WHERE ad.id = ${bind(id)}
"""
        )
    }.exactlyOneOrNull()
