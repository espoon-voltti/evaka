// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Transaction.insertAssistanceNeedDecision(
    childId: ChildId,
    data: AssistanceNeedDecisionForm
): AssistanceNeedDecision {
    //language=sql
    val sql =
        """
        INSERT INTO assistance_need_decision (
          child_id, start_date, end_date, status, language, decision_made, sent_for_decision,
          selected_unit, pedagogical_motivation, structural_motivation_opt_smaller_group,
          structural_motivation_opt_special_group, structural_motivation_opt_small_group,
          structural_motivation_opt_group_assistant, structural_motivation_opt_child_assistant,
          structural_motivation_opt_additional_staff, structural_motivation_description, care_motivation,
          service_opt_consultation_special_ed, service_opt_part_time_special_ed, service_opt_full_time_special_ed,
          service_opt_interpretation_and_assistance_services, service_opt_special_aides, services_motivation,
          expert_responsibilities, guardians_heard_on, view_of_guardians, other_representative_heard, other_representative_details, 
          assistance_level, assistance_services_time, motivation_for_decision, decision_maker_employee_id,
          decision_maker_title, preparer_1_employee_id, preparer_1_title, preparer_2_employee_id, preparer_2_title,
          preparer_1_phone_number, preparer_2_phone_number
        )
        VALUES (
            :childId, 
            :startDate, 
            :endDate, 
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
            :assistanceLevel,
            :assistanceServicesTime,
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
        """.trimIndent()

    val id = createQuery(sql)
        .bindKotlin(data)
        .bind("childId", childId)
        .bind("structuralMotivationOptSmallerGroup", data.structuralMotivationOptions.smallerGroup)
        .bind("structuralMotivationOptSpecialGroup", data.structuralMotivationOptions.specialGroup)
        .bind("structuralMotivationOptSmallGroup", data.structuralMotivationOptions.smallGroup)
        .bind("structuralMotivationOptGroupAssistant", data.structuralMotivationOptions.groupAssistant)
        .bind("structuralMotivationOptChildAssistant", data.structuralMotivationOptions.childAssistant)
        .bind("structuralMotivationOptAdditionalStaff", data.structuralMotivationOptions.additionalStaff)
        .bind("serviceOptConsultationSpecialEd", data.serviceOptions.consultationSpecialEd)
        .bind("serviceOptPartTimeSpecialEd", data.serviceOptions.partTimeSpecialEd)
        .bind("serviceOptFullTimeSpecialEd", data.serviceOptions.fullTimeSpecialEd)
        .bind("serviceOptInterpretationAndAssistanceServices", data.serviceOptions.interpretationAndAssistanceServices)
        .bind("serviceOptSpecialAides", data.serviceOptions.specialAides)
        .bind("assistanceServicesTime", data.assistanceServicesTime)
        .bind("decisionMakerEmployeeId", data.decisionMaker?.employeeId)
        .bind("decisionMakerTitle", data.decisionMaker?.title)
        .bind("preparer1EmployeeId", data.preparedBy1?.employeeId)
        .bind("preparer1Title", data.preparedBy1?.title)
        .bind("preparer1PhoneNumber", data.preparedBy1?.phoneNumber)
        .bind("preparer2EmployeeId", data.preparedBy2?.employeeId)
        .bind("preparer2Title", data.preparedBy2?.title)
        .bind("preparer2PhoneNumber", data.preparedBy2?.phoneNumber)
        .bind("selectedUnit", data.selectedUnit?.id)
        .mapTo<AssistanceNeedDecisionId>()
        .first()

    //language=sql
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
            
        """.trimIndent()

    val batch = prepareBatch(guardianSql)
    data.guardianInfo.forEach { guardian ->
        batch
            .bindKotlin(guardian)
            .bind("assistanceNeedDecisionId", id)
            .add()
    }
    batch.execute()

    return getAssistanceNeedDecisionById(id)
}

fun Database.Read.getAssistanceNeedDecisionById(id: AssistanceNeedDecisionId): AssistanceNeedDecision {
    //language=sql
    val sql =
        """
        SELECT ad.id, decision_number, child_id, concat(child.first_name, ' ', child.last_name) child_name, start_date, end_date, status,
          ad.language, decision_made, sent_for_decision, pedagogical_motivation, structural_motivation_opt_smaller_group,
          structural_motivation_opt_special_group, structural_motivation_opt_small_group,
          structural_motivation_opt_group_assistant, structural_motivation_opt_child_assistant,
          structural_motivation_opt_additional_staff, structural_motivation_description, care_motivation,
          service_opt_consultation_special_ed, service_opt_part_time_special_ed, service_opt_full_time_special_ed,
          service_opt_interpretation_and_assistance_services, service_opt_special_aides, services_motivation,
          expert_responsibilities, guardians_heard_on, view_of_guardians, other_representative_heard, other_representative_details, 
          assistance_level, assistance_services_time, motivation_for_decision,
          decision_maker_employee_id, decision_maker_title, concat(dm.first_name, ' ', dm.last_name) decision_maker_name,
          preparer_1_employee_id, preparer_1_title, concat(p1.first_name, ' ', p1.last_name) preparer_1_name, p1.email preparer_1_email, preparer_1_phone_number,
          preparer_2_employee_id, preparer_2_title, concat(p2.first_name, ' ', p2.last_name) preparer_2_name, p2.email preparer_2_email, preparer_2_phone_number,
          coalesce(jsonb_agg(jsonb_build_object(
            'id', dg.id,
            'personId', dg.person_id,
            'name', concat(p.last_name, ' ', p.first_name),
            'isHeard', dg.is_heard,
            'details', dg.details
          )) FILTER (WHERE dg.id IS NOT NULL), '[]') as guardian_info,
          selected_unit selected_unit_id, unit.name selected_unit_name, unit.street_address selected_unit_street_address,
          unit.postal_code selected_unit_postal_code, unit.post_office selected_unit_post_office
        FROM assistance_need_decision ad
        LEFT JOIN assistance_need_decision_guardian dg ON dg.assistance_need_decision_id = ad.id
        LEFT JOIN person p ON p.id = dg.person_id
        LEFT JOIN daycare unit ON unit.id = ad.selected_unit
        LEFT JOIN employee p1 ON p1.id = ad.preparer_1_employee_id
        LEFT JOIN employee p2 ON p2.id = ad.preparer_2_employee_id
        LEFT JOIN employee dm ON dm.id = ad.decision_maker_employee_id
        LEFT JOIN person child ON child.id = ad.child_id
        WHERE ad.id = :id
        GROUP BY ad.id, child_id, start_date, end_date, unit.id, p1.id, p2.id, dm.id, child.id;
        """.trimIndent()
    return createQuery(sql)
        .bind("id", id)
        .mapTo<AssistanceNeedDecision>()
        .firstOrNull() ?: throw NotFound("Assistance need decision $id not found")
}

fun Database.Transaction.updateAssistanceNeedDecision(
    id: AssistanceNeedDecisionId,
    data: AssistanceNeedDecisionForm,
    decisionMakerHasOpened: Boolean? = null
) {
    //language=sql
    val sql =
        """
        UPDATE assistance_need_decision
        SET 
            start_date = :startDate,
            end_date = :endDate, 
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
            assistance_level = :assistanceLevel,
            assistance_services_time = :assistanceServicesTime,
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
        """.trimIndent()
    createUpdate(sql)
        .bindKotlin(data)
        .bind("id", id)
        .bind("structuralMotivationOptSmallerGroup", data.structuralMotivationOptions.smallerGroup)
        .bind("structuralMotivationOptSpecialGroup", data.structuralMotivationOptions.specialGroup)
        .bind("structuralMotivationOptSmallGroup", data.structuralMotivationOptions.smallGroup)
        .bind("structuralMotivationOptGroupAssistant", data.structuralMotivationOptions.groupAssistant)
        .bind("structuralMotivationOptChildAssistant", data.structuralMotivationOptions.childAssistant)
        .bind("structuralMotivationOptAdditionalStaff", data.structuralMotivationOptions.additionalStaff)
        .bind("serviceOptConsultationSpecialEd", data.serviceOptions.consultationSpecialEd)
        .bind("serviceOptPartTimeSpecialEd", data.serviceOptions.partTimeSpecialEd)
        .bind("serviceOptFullTimeSpecialEd", data.serviceOptions.fullTimeSpecialEd)
        .bind("serviceOptInterpretationAndAssistanceServices", data.serviceOptions.interpretationAndAssistanceServices)
        .bind("serviceOptSpecialAides", data.serviceOptions.specialAides)
        .bind("assistanceServicesTime", data.assistanceServicesTime)
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

    //language=sql
    val guardianSql =
        """
        UPDATE assistance_need_decision_guardian SET 
            is_heard = :isHeard,
            details = :details
        WHERE id = :id
        """.trimIndent()
    val batch = prepareBatch(guardianSql)
    data.guardianInfo.forEach { guardian ->
        batch
            .bindKotlin(guardian)
            .add()
    }
    batch.execute()
}

fun Database.Read.getAssistanceNeedDecisionsByChildId(childId: ChildId): List<AssistanceNeedDecisionBasics> {
    //language=sql
    val sql =
        """
        SELECT ad.id, start_date, end_date, status, decision_made, sent_for_decision, ad.created,
            selected_unit selected_unit_id, unit.name selected_unit_name
        FROM assistance_need_decision ad
        LEFT JOIN daycare unit ON unit.id = selected_unit
        WHERE child_id = :childId;
        """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<AssistanceNeedDecisionBasics>()
        .list()
}

fun Database.Transaction.deleteAssistanceNeedDecision(id: AssistanceNeedDecisionId): Boolean {
    //language=sql
    val sql =
        """
        DELETE FROM assistance_need_decision
        WHERE id = :id AND status IN ('DRAFT', 'NEEDS_WORK')
        RETURNING id;
        """.trimIndent()
    return createQuery(sql)
        .bind("id", id)
        .mapTo<AssistanceNeedDecisionId>()
        .firstOrNull() != null
}

fun Database.Transaction.markAssistanceNeedDecisionAsOpened(
    id: AssistanceNeedDecisionId
) {
    //language=sql
    val sql =
        """
        UPDATE assistance_need_decision
        SET decision_maker_has_opened = TRUE
        WHERE id = :id
        """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .updateExactlyOne()
}
