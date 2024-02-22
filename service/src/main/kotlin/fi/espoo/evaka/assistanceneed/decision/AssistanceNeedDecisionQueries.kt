// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate

fun Database.Transaction.insertAssistanceNeedDecision(
    childId: ChildId,
    data: AssistanceNeedDecisionForm
): AssistanceNeedDecision {
    // language=sql
    val sql =
        """
        INSERT INTO assistance_need_decision (
          child_id, validity_period, status, language, decision_made, sent_for_decision,
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
            :childId, 
            :validityPeriod,
            :status,
            :language,
            :decisionMade,
            :sentForDecision,
            :selectedUnit, 
            :pedagogicalMotivation,
            :structuralMotivationOptSmallerGroup,
            :structuralMotivationOptSpecialGroup,
            :structuralMotivationOptSmallGroup,
            :structuralMotivationOptGroupAssistant, 
            :structuralMotivationOptChildAssistant,
            :structuralMotivationOptAdditionalStaff,
            :structuralMotivationDescription,
            :careMotivation,
            :serviceOptConsultationSpecialEd,
            :serviceOptPartTimeSpecialEd,
            :serviceOptFullTimeSpecialEd,
            :serviceOptInterpretationAndAssistanceServices,
            :serviceOptSpecialAides,
            :servicesMotivation,
            :expertResponsibilities,
            :guardiansHeardOn,
            :viewOfGuardians,
            :otherRepresentativeHeard,
            :otherRepresentativeDetails, 
            :assistanceLevels,
            :motivationForDecision,
            :decisionMakerEmployeeId,
            :decisionMakerTitle,
            :preparer1EmployeeId,
            :preparer1Title,
            :preparer2EmployeeId,
            :preparer2Title,
            :preparer1PhoneNumber,
            :preparer2PhoneNumber
        )
        RETURNING id
        """
            .trimIndent()

    val id =
        @Suppress("DEPRECATION")
        createQuery(sql)
            .bindKotlin(data)
            .bind("childId", childId)
            .bind(
                "structuralMotivationOptSmallerGroup",
                data.structuralMotivationOptions.smallerGroup
            )
            .bind(
                "structuralMotivationOptSpecialGroup",
                data.structuralMotivationOptions.specialGroup
            )
            .bind("structuralMotivationOptSmallGroup", data.structuralMotivationOptions.smallGroup)
            .bind(
                "structuralMotivationOptGroupAssistant",
                data.structuralMotivationOptions.groupAssistant
            )
            .bind(
                "structuralMotivationOptChildAssistant",
                data.structuralMotivationOptions.childAssistant
            )
            .bind(
                "structuralMotivationOptAdditionalStaff",
                data.structuralMotivationOptions.additionalStaff
            )
            .bind("serviceOptConsultationSpecialEd", data.serviceOptions.consultationSpecialEd)
            .bind("serviceOptPartTimeSpecialEd", data.serviceOptions.partTimeSpecialEd)
            .bind("serviceOptFullTimeSpecialEd", data.serviceOptions.fullTimeSpecialEd)
            .bind(
                "serviceOptInterpretationAndAssistanceServices",
                data.serviceOptions.interpretationAndAssistanceServices
            )
            .bind("serviceOptSpecialAides", data.serviceOptions.specialAides)
            .bind("decisionMakerEmployeeId", data.decisionMaker?.employeeId)
            .bind("decisionMakerTitle", data.decisionMaker?.title)
            .bind("preparer1EmployeeId", data.preparedBy1?.employeeId)
            .bind("preparer1Title", data.preparedBy1?.title)
            .bind("preparer1PhoneNumber", data.preparedBy1?.phoneNumber)
            .bind("preparer2EmployeeId", data.preparedBy2?.employeeId)
            .bind("preparer2Title", data.preparedBy2?.title)
            .bind("preparer2PhoneNumber", data.preparedBy2?.phoneNumber)
            .bind("selectedUnit", data.selectedUnit?.id)
            .exactlyOne<AssistanceNeedDecisionId>()

    // language=sql
    val guardianSql =
        """
        INSERT INTO assistance_need_decision_guardian (
            assistance_need_decision_id,
            person_id,
            is_heard,
            details
        ) VALUES (
            :assistanceNeedDecisionId,
            :personId,
            :isHeard,
            :details
        )
            
        """
            .trimIndent()

    val batch = prepareBatch(guardianSql)
    data.guardianInfo.forEach { guardian ->
        batch.bindKotlin(guardian).bind("assistanceNeedDecisionId", id).add()
    }
    batch.execute()

    return getAssistanceNeedDecisionById(id)
}

fun Database.Read.getAssistanceNeedDecisionById(
    id: AssistanceNeedDecisionId
): AssistanceNeedDecision {
    // language=sql
    val sql =
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
        WHERE ad.id = :id
        GROUP BY ad.id, child_id, validity_period, unit.id, p1.id, p2.id, dm.id, child.id;
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql).bind("id", id).exactlyOneOrNull<AssistanceNeedDecision>()
        ?: throw NotFound("Assistance need decision $id not found")
}

fun Database.Transaction.updateAssistanceNeedDecision(
    id: AssistanceNeedDecisionId,
    data: AssistanceNeedDecisionForm,
    decisionMakerHasOpened: Boolean? = null
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need_decision
        SET 
            validity_period = :validityPeriod, 
            status = :status,
            language = :language,
            decision_made = :decisionMade,
            sent_for_decision = :sentForDecision,
            selected_unit = :selectedUnit,
            pedagogical_motivation = :pedagogicalMotivation,
            structural_motivation_opt_smaller_group = :structuralMotivationOptSmallerGroup,
            structural_motivation_opt_special_group = :structuralMotivationOptSpecialGroup,
            structural_motivation_opt_small_group = :structuralMotivationOptSmallGroup,
            structural_motivation_opt_group_assistant = :structuralMotivationOptGroupAssistant,
            structural_motivation_opt_child_assistant = :structuralMotivationOptChildAssistant,
            structural_motivation_opt_additional_staff = :structuralMotivationOptAdditionalStaff,
            structural_motivation_description = :structuralMotivationDescription,
            care_motivation = :careMotivation,
            service_opt_consultation_special_ed=:serviceOptConsultationSpecialEd,
            service_opt_part_time_special_ed = :serviceOptPartTimeSpecialEd,
            service_opt_full_time_special_ed = :serviceOptFullTimeSpecialEd,
            service_opt_interpretation_and_assistance_services = :serviceOptInterpretationAndAssistanceServices,
            service_opt_special_aides = :serviceOptSpecialAides,
            services_motivation = :servicesMotivation,
            expert_responsibilities = :expertResponsibilities,
            guardians_heard_on = :guardiansHeardOn,
            view_of_guardians = :viewOfGuardians,
            other_representative_heard = :otherRepresentativeHeard,
            other_representative_details = :otherRepresentativeDetails,
            assistance_levels = :assistanceLevels,
            motivation_for_decision = :motivationForDecision,
            decision_maker_employee_id = :decisionMakerEmployeeId,
            decision_maker_title = :decisionMakerTitle,
            preparer_1_employee_id = :preparer1EmployeeId,
            preparer_1_title = :preparer1Title,
            preparer_1_phone_number = :preparer1PhoneNumber,
            preparer_2_employee_id = :preparer2EmployeeId,
            preparer_2_title = :preparer2Title,
            preparer_2_phone_number = :preparer2PhoneNumber,
            decision_maker_has_opened = COALESCE(:decisionMakerHasOpened, decision_maker_has_opened)
        WHERE id = :id AND status IN ('DRAFT', 'NEEDS_WORK')
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bindKotlin(data)
        .bind("id", id)
        .bind("structuralMotivationOptSmallerGroup", data.structuralMotivationOptions.smallerGroup)
        .bind("structuralMotivationOptSpecialGroup", data.structuralMotivationOptions.specialGroup)
        .bind("structuralMotivationOptSmallGroup", data.structuralMotivationOptions.smallGroup)
        .bind(
            "structuralMotivationOptGroupAssistant",
            data.structuralMotivationOptions.groupAssistant
        )
        .bind(
            "structuralMotivationOptChildAssistant",
            data.structuralMotivationOptions.childAssistant
        )
        .bind(
            "structuralMotivationOptAdditionalStaff",
            data.structuralMotivationOptions.additionalStaff
        )
        .bind("serviceOptConsultationSpecialEd", data.serviceOptions.consultationSpecialEd)
        .bind("serviceOptPartTimeSpecialEd", data.serviceOptions.partTimeSpecialEd)
        .bind("serviceOptFullTimeSpecialEd", data.serviceOptions.fullTimeSpecialEd)
        .bind(
            "serviceOptInterpretationAndAssistanceServices",
            data.serviceOptions.interpretationAndAssistanceServices
        )
        .bind("serviceOptSpecialAides", data.serviceOptions.specialAides)
        .bind("decisionMakerEmployeeId", data.decisionMaker?.employeeId)
        .bind("decisionMakerTitle", data.decisionMaker?.title)
        .bind("preparer1EmployeeId", data.preparedBy1?.employeeId)
        .bind("preparer1Title", data.preparedBy1?.title)
        .bind("preparer1PhoneNumber", data.preparedBy1?.phoneNumber)
        .bind("preparer2EmployeeId", data.preparedBy2?.employeeId)
        .bind("preparer2Title", data.preparedBy2?.title)
        .bind("preparer2PhoneNumber", data.preparedBy2?.phoneNumber)
        .bind("selectedUnit", data.selectedUnit?.id)
        .bind("decisionMakerHasOpened", decisionMakerHasOpened)
        .updateExactlyOne()

    // language=sql
    val guardianSql =
        """
        UPDATE assistance_need_decision_guardian SET 
            is_heard = :isHeard,
            details = :details
        WHERE id = :id
        """
            .trimIndent()
    val batch = prepareBatch(guardianSql)
    data.guardianInfo.forEach { guardian -> batch.bindKotlin(guardian).add() }
    batch.execute()
}

fun Database.Read.getAssistanceNeedDecisionsByChildId(
    childId: ChildId,
    filter: AccessControlFilter<AssistanceNeedDecisionId>
): List<AssistanceNeedDecisionBasics> =
    createQuery<Any> {
            sql(
                """
        SELECT ad.id, validity_period, status, decision_made, sent_for_decision, ad.created,
            selected_unit selected_unit_id, unit.name selected_unit_name
        FROM assistance_need_decision ad
        LEFT JOIN daycare unit ON unit.id = selected_unit
        WHERE child_id = ${bind(childId)} AND ${predicate(filter.forTable("ad"))}
    """
                    .trimIndent()
            )
        }
        .toList<AssistanceNeedDecisionBasics>()

fun Database.Transaction.deleteAssistanceNeedDecision(id: AssistanceNeedDecisionId): Boolean {
    // language=sql
    val sql =
        """
        DELETE FROM assistance_need_decision
        WHERE id = :id AND status IN ('DRAFT', 'NEEDS_WORK')
        RETURNING id;
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql).bind("id", id).exactlyOneOrNull<AssistanceNeedDecisionId>() != null
}

fun Database.Transaction.markAssistanceNeedDecisionAsOpened(id: AssistanceNeedDecisionId) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need_decision
        SET decision_maker_has_opened = TRUE
        WHERE id = :id
        """
            .trimIndent()

    @Suppress("DEPRECATION") createUpdate(sql).bind("id", id).updateExactlyOne()
}

fun Database.Read.getAssistanceNeedDecisionsForCitizen(
    today: LocalDate,
    userId: PersonId
): List<AssistanceNeedDecisionCitizenListItem> {
    // language=sql
    val sql =
        """
        WITH children AS (
            SELECT child_id FROM guardian WHERE guardian_id = :userId
            UNION
            SELECT child_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
        )
        SELECT ad.id, ad.child_id, validity_period, status, decision_made, assistance_levels,
            selected_unit AS selected_unit_id, unit.name AS selected_unit_name, annulment_reason,
            coalesce(:userId = ANY(unread_guardian_ids), false) AS is_unread
        FROM children c
        JOIN assistance_need_decision ad ON ad.child_id = c.child_id
        LEFT JOIN daycare unit ON unit.id = selected_unit
        WHERE status IN ('REJECTED', 'ACCEPTED', 'ANNULLED') AND decision_made IS NOT NULL
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("today", today)
        .bind("userId", userId)
        .toList<AssistanceNeedDecisionCitizenListItem>()
}

fun Database.Read.getAssistanceNeedDecisionDocumentKey(id: AssistanceNeedDecisionId): String? {
    // language=sql
    val sql =
        """
        SELECT document_key
        FROM assistance_need_decision ad
        WHERE ad.id = :id
        """
            .trimIndent()
    @Suppress("DEPRECATION") return createQuery(sql).bind("id", id).exactlyOneOrNull<String>()
}

fun Database.Transaction.updateAssistanceNeedDocumentKey(
    id: AssistanceNeedDecisionId,
    key: String
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need_decision
        SET document_key = :key
        WHERE id = :id
        """
            .trimIndent()
    @Suppress("DEPRECATION") createUpdate(sql).bind("id", id).bind("key", key).updateExactlyOne()
}

fun Database.Transaction.markAssistanceNeedDecisionAsReadByGuardian(
    assistanceNeedDecisionId: AssistanceNeedDecisionId,
    guardianId: PersonId
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need_decision
        SET unread_guardian_ids = array_remove(unread_guardian_ids, :guardianId)
        WHERE id = :id
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("id", assistanceNeedDecisionId)
        .bind("guardianId", guardianId)
        .updateExactlyOne()
}

fun Database.Read.getAssistanceNeedDecisionsUnreadCountsForCitizen(
    today: LocalDate,
    userId: PersonId
): List<UnreadAssistanceNeedDecisionItem> {
    // language=sql
    val sql =
        """
        WITH children AS (
            SELECT child_id FROM guardian WHERE guardian_id = :userId
            UNION
            SELECT child_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
        )
        SELECT ad.child_id, COUNT(ad.child_id) as count
        FROM assistance_need_decision ad
        JOIN children c ON c.child_id = ad.child_id
        WHERE (:userId = ANY(ad.unread_guardian_ids)) AND status IN ('REJECTED', 'ACCEPTED')
        GROUP BY ad.child_id
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("today", today)
        .bind("userId", userId)
        .toList<UnreadAssistanceNeedDecisionItem>()
}

fun Database.Transaction.decideAssistanceNeedDecision(
    id: AssistanceNeedDecisionId,
    status: AssistanceNeedDecisionStatus,
    decisionMade: LocalDate?,
    unreadGuardianIds: List<PersonId>?
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need_decision
        SET 
            status = :status,
            decision_made = :decisionMade,
            unread_guardian_ids = :unreadGuardianIds
        WHERE id = :id AND status IN ('DRAFT', 'NEEDS_WORK')
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("id", id)
        .bind("status", status)
        .bind("decisionMade", decisionMade)
        .bind("unreadGuardianIds", unreadGuardianIds)
        .updateExactlyOne()
}

fun Database.Transaction.endActiveAssistanceNeedDecisions(
    excludingId: AssistanceNeedDecisionId,
    endDate: LocalDate,
    childId: ChildId
) {
    // language=sql
    val sql =
        """
        UPDATE assistance_need_decision
        SET validity_period = daterange(lower(validity_period), :endDate, '[]')
        WHERE id <> :excludingId
          AND (upper(validity_period) IS NULL OR upper(validity_period) > :endDate)
          AND child_id = :childId
          AND status = 'ACCEPTED'
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("excludingId", excludingId)
        .bind("endDate", endDate.minusDays(1))
        .bind("childId", childId)
        .execute()
}

fun Database.Read.hasLaterAssistanceNeedDecisions(
    childId: ChildId,
    startDate: LocalDate,
): Boolean {
    @Suppress("DEPRECATION")
    return createQuery(
            """
        SELECT EXISTS (
            SELECT 1
            FROM assistance_need_decision
            WHERE child_id = :childId
              AND :startDate <= lower(validity_period)
              AND status = 'ACCEPTED'
        )
    """
                .trimIndent()
        )
        .bind("childId", childId)
        .bind("startDate", startDate)
        .exactlyOne<Boolean>()
}

fun Database.Transaction.annulAssistanceNeedDecision(
    id: AssistanceNeedDecisionId,
    reason: String,
) {
    @Suppress("DEPRECATION")
    createUpdate(
            """
UPDATE assistance_need_decision
SET status = 'ANNULLED', annulment_reason = :reason
WHERE id = :id
"""
        )
        .bind("id", id)
        .bind("reason", reason)
        .updateExactlyOne()
}
