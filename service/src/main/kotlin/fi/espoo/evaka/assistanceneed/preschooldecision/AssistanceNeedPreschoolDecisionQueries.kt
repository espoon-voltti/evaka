// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.preschooldecision

import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionLanguage
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate

fun Database.Transaction.insertEmptyAssistanceNeedPreschoolDecisionDraft(
    childId: ChildId,
    language: AssistanceNeedDecisionLanguage = AssistanceNeedDecisionLanguage.FI
): AssistanceNeedPreschoolDecision =
    createQuery(
            """
        INSERT INTO assistance_need_preschool_decision (child_id, language)
        VALUES (:childId, :language)
        RETURNING id
        """
        )
        .bind("childId", childId)
        .bind("language", language)
        .mapTo<AssistanceNeedPreschoolDecisionId>()
        .first()
        .also { decisionId ->
            createUpdate(
                    """
            INSERT INTO assistance_need_preschool_decision_guardian (
                assistance_need_decision_id, person_id
            )
            SELECT :decisionId, guardian_id
            FROM guardian
            WHERE child_id = :childId
            """
                )
                .bind("decisionId", decisionId)
                .bind("childId", childId)
                .execute()
        }
        .let(::getAssistanceNeedPreschoolDecisionById)

fun Database.Read.getAssistanceNeedPreschoolDecisionById(
    id: AssistanceNeedPreschoolDecisionId
): AssistanceNeedPreschoolDecision {
    // language=sql
    val sql =
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
            ad.annulment_reason,
            (ad.document_key IS NOT NULL) has_document
        FROM assistance_need_preschool_decision ad
        JOIN person child ON child.id = ad.child_id
        LEFT JOIN assistance_need_preschool_decision_guardian dg ON dg.assistance_need_decision_id = ad.id
        LEFT JOIN person p ON p.id = dg.person_id
        WHERE ad.id = :id
        GROUP BY ad.id, child_id, valid_from, child.id;
        """

    return createQuery(sql).bind("id", id).mapTo<AssistanceNeedPreschoolDecision>().firstOrNull()
        ?: throw NotFound("Assistance need preschool decision $id not found")
}

fun Database.Transaction.updateAssistanceNeedPreschoolDecision(
    id: AssistanceNeedPreschoolDecisionId,
    data: AssistanceNeedPreschoolDecisionForm,
    decisionMakerHasOpened: Boolean? = null
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need_preschool_decision
        SET 
            language = :language,
            type = :type,
            valid_from = :validFrom,
            extended_compulsory_education = :extendedCompulsoryEducation,
            extended_compulsory_education_info = :extendedCompulsoryEducationInfo,
            granted_assistance_service = :grantedAssistanceService,
            granted_interpretation_service = :grantedInterpretationService,
            granted_assistive_devices = :grantedAssistiveDevices,
            granted_services_basis = :grantedServicesBasis,
            selected_unit = :selectedUnit,
            primary_group = :primaryGroup,
            decision_basis = :decisionBasis,
            basis_document_pedagogical_report = :basisDocumentPedagogicalReport,
            basis_document_psychologist_statement = :basisDocumentPsychologistStatement,
            basis_document_social_report = :basisDocumentSocialReport,
            basis_document_doctor_statement = :basisDocumentDoctorStatement,
            basis_document_other_or_missing = :basisDocumentOtherOrMissing,
            basis_document_other_or_missing_info = :basisDocumentOtherOrMissingInfo,
            basis_documents_info = :basisDocumentsInfo,
            guardians_heard_on = :guardiansHeardOn,
            other_representative_heard = :otherRepresentativeHeard,
            other_representative_details = :otherRepresentativeDetails,
            view_of_guardians = :viewOfGuardians,
            preparer_1_employee_id = :preparer1EmployeeId,
            preparer_1_title = :preparer1Title,
            preparer_1_phone_number = :preparer1PhoneNumber,
            preparer_2_employee_id = :preparer2EmployeeId,
            preparer_2_title = :preparer2Title,
            preparer_2_phone_number = :preparer2PhoneNumber,
            decision_maker_employee_id = :decisionMakerEmployeeId,
            decision_maker_title = :decisionMakerTitle,
            decision_maker_has_opened = COALESCE(:decisionMakerHasOpened, decision_maker_has_opened)
        WHERE id = :id AND status IN ('DRAFT', 'NEEDS_WORK')
        """

    createUpdate(sql)
        .bindKotlin(data)
        .bind("id", id)
        .bind("decisionMakerHasOpened", decisionMakerHasOpened)
        .updateExactlyOne()

    // language=sql
    val guardianSql =
        """
        UPDATE assistance_need_preschool_decision_guardian SET 
            is_heard = :isHeard,
            details = :details
        WHERE id = :id
        """

    val batch = prepareBatch(guardianSql)
    data.guardianInfo.forEach { guardian -> batch.bindKotlin(guardian).add() }
    batch.execute()
}

fun Database.Read.getAssistanceNeedPreschoolDecisionsByChildId(
    childId: ChildId
): List<AssistanceNeedPreschoolDecisionBasics> {
    // language=sql
    val sql =
        """
        SELECT ad.id, ad.created, ad.status, ad.type, ad.valid_from,
            ad.selected_unit selected_unit_id, unit.name selected_unit_name,
            ad.sent_for_decision, ad.decision_made
        FROM assistance_need_preschool_decision ad
        LEFT JOIN daycare unit ON unit.id = selected_unit
        WHERE child_id = :childId
        ORDER BY ad.valid_from DESC NULLS FIRST, ad.created DESC;
        """

    val decisions =
        createQuery(sql)
            .bind("childId", childId)
            .mapTo<AssistanceNeedPreschoolDecisionBasics>()
            .list()

    return decisions.map { decision ->
        if (decision.validFrom == null) return@map decision

        val followingStart =
            decisions
                .filter { it.status != AssistanceNeedDecisionStatus.DRAFT && it.validFrom != null }
                .sortedWith(
                    compareByDescending<AssistanceNeedPreschoolDecisionBasics> { it.validFrom }
                        .thenByDescending { it.created }
                )
                .takeWhile { it.id != decision.id }
                .lastOrNull()
                ?.validFrom
                ?: return@map decision

        decision.copy(validTo = minOf(decision.validFrom, followingStart.minusDays(1)))
    }
}

fun Database.Transaction.deleteAssistanceNeedPreschoolDecision(
    id: AssistanceNeedPreschoolDecisionId
): Boolean {
    // language=sql
    val sql =
        """
        DELETE FROM assistance_need_preschool_decision
        WHERE id = :id AND status IN ('DRAFT', 'NEEDS_WORK')
        RETURNING id;
        """

    return createQuery(sql).bind("id", id).mapTo<AssistanceNeedDecisionId>().firstOrNull() != null
}

fun Database.Transaction.markAssistanceNeedPreschoolDecisionAsOpened(
    id: AssistanceNeedPreschoolDecisionId
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need_preschool_decision
        SET decision_maker_has_opened = TRUE
        WHERE id = :id
        """

    createUpdate(sql).bind("id", id).updateExactlyOne()
}

fun Database.Transaction.decideAssistanceNeedDecision(
    id: AssistanceNeedPreschoolDecisionId,
    status: AssistanceNeedDecisionStatus,
    decisionMade: LocalDate?,
    unreadGuardianIds: List<PersonId>?
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need_preschool_decision
        SET 
            status = :status,
            decision_made = :decisionMade,
            unread_guardian_ids = :unreadGuardianIds
        WHERE id = :id AND status IN ('DRAFT', 'NEEDS_WORK')
        """
            .trimIndent()
    createUpdate(sql)
        .bind("id", id)
        .bind("status", status)
        .bind("decisionMade", decisionMade)
        .bind("unreadGuardianIds", unreadGuardianIds)
        .updateExactlyOne()
}

fun Database.Transaction.annulAssistanceNeedPreschoolDecision(
    id: AssistanceNeedPreschoolDecisionId,
    reason: String,
) {
    createUpdate(
            """
UPDATE assistance_need_preschool_decision
SET status = 'ANNULLED', annulment_reason = :reason
WHERE id = :id
"""
        )
        .bind("id", id)
        .bind("reason", reason)
        .updateExactlyOne()
}
