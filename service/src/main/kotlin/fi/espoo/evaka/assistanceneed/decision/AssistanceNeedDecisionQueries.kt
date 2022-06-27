// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Transaction.insertAssistanceNeedDecision(
    childId: ChildId,
    data: AssistanceNeedDecision
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
          assistance_level, assistance_service_start, assistance_service_end, motivation_for_decision, decision_maker_employee_id,
          decision_maker_title, preparer_1_employee_id, preparer_1_title, preparer_2_employee_id, preparer_2_title 
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
            :assistanceServiceStart,
            :assistanceServiceEnd,
            :motivationForDecision,
            :decisionMakerEmployeeId,
            :decisionMakerTitle,
            :preparer1EmployeeId,
            :preparer1Title,
            :preparer2EmployeeId,
            :preparer2Title
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
        .bind("assistanceServiceStart", data.assistanceServicesTime?.start)
        .bind("assistanceServiceEnd", data.assistanceServicesTime?.end)
        .bind("decisionMakerEmployeeId", data.decisionMaker?.employeeId)
        .bind("decisionMakerTitle", data.decisionMaker?.title)
        .bind("preparer1EmployeeId", data.preparedBy1?.employeeId)
        .bind("preparer1Title", data.preparedBy1?.title)
        .bind("preparer2EmployeeId", data.preparedBy2?.employeeId)
        .bind("preparer2Title", data.preparedBy2?.title)
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
        SELECT ad.id, decision_number, child_id, start_date, end_date, status, ad.language, decision_made, sent_for_decision,
          selected_unit, pedagogical_motivation, structural_motivation_opt_smaller_group,
          structural_motivation_opt_special_group, structural_motivation_opt_small_group,
          structural_motivation_opt_group_assistant, structural_motivation_opt_child_assistant,
          structural_motivation_opt_additional_staff, structural_motivation_description, care_motivation,
          service_opt_consultation_special_ed, service_opt_part_time_special_ed, service_opt_full_time_special_ed,
          service_opt_interpretation_and_assistance_services, service_opt_special_aides, services_motivation,
          expert_responsibilities, guardians_heard_on, view_of_guardians, other_representative_heard, other_representative_details, 
          assistance_level, assistance_service_start, assistance_service_end, motivation_for_decision, decision_maker_employee_id,
          decision_maker_title, preparer_1_employee_id, preparer_1_title, preparer_2_employee_id, preparer_2_title,
          coalesce(jsonb_agg(jsonb_build_object(
            'id', dg.id,
            'personId', dg.person_id,
            'name', concat(p.last_name, ' ', p.first_name),
            'isHeard', dg.is_heard,
            'details', dg.details
          )) FILTER (WHERE dg.id IS NOT NULL), '[]') as guardian_info
        FROM assistance_need_decision ad
        LEFT JOIN assistance_need_decision_guardian dg ON dg.assistance_need_decision_id = ad.id
        LEFT JOIN person p ON p.id = dg.person_id
        WHERE ad.id = :id
        GROUP BY ad.id, child_id, start_date, end_date;
        """.trimIndent()
    return createQuery(sql)
        .bind("id", id)
        .mapTo<AssistanceNeedDecision>()
        .one()
}

fun Database.Transaction.updateAssistanceNeedDecision(
    id: AssistanceNeedDecisionId,
    data: AssistanceNeedDecision
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
            service_opt_interpretation_and_assistance_services = :serviceOptInterpretationAndAssistanceServices ,
            service_opt_special_aides = :serviceOptSpecialAides ,
            services_motivation = :servicesMotivation ,
            expert_responsibilities = :expertResponsibilities ,
            guardians_heard_on = :guardiansHeardOn ,
            view_of_guardians = :viewOfGuardians ,
            other_representative_heard = :otherRepresentativeHeard ,
            other_representative_details = :otherRepresentativeDetails ,
            assistance_level = :assistanceLevel ,
            assistance_service_start = :assistanceServiceStart ,
            assistance_service_end = :assistanceServiceEnd ,
            motivation_for_decision = :motivationForDecision ,
            decision_maker_employee_id = :decisionMakerEmployeeId ,
            decision_maker_title = :decisionMakerTitle ,
            preparer_1_employee_id = :preparer1EmployeeId ,
            preparer_1_title = :preparer1Title ,
            preparer_2_employee_id = :preparer2EmployeeId ,
            preparer_2_title = :preparer2Title 
        WHERE id = :id
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
        .bind("assistanceServiceStart", data.assistanceServicesTime?.start)
        .bind("assistanceServiceEnd", data.assistanceServicesTime?.end)
        .bind("decisionMakerEmployeeId", data.decisionMaker?.employeeId)
        .bind("decisionMakerTitle", data.decisionMaker?.title)
        .bind("preparer1EmployeeId", data.preparedBy1?.employeeId)
        .bind("preparer1Title", data.preparedBy1?.title)
        .bind("preparer2EmployeeId", data.preparedBy2?.employeeId)
        .bind("preparer2Title", data.preparedBy2?.title)
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
