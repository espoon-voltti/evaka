// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import evaka.core.absence.AbsenceCategory
import evaka.core.absence.AbsenceType
import evaka.core.application.ApplicationOrigin
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.assistance.DaycareAssistanceLevel
import evaka.core.assistance.OtherAssistanceMeasureType
import evaka.core.assistance.PreschoolAssistanceLevel
import evaka.core.attendance.StaffAttendanceType
import evaka.core.daycare.CareType
import evaka.core.daycare.domain.Language
import evaka.core.daycare.domain.ProviderType
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.invoicing.domain.FeeAlterationType
import evaka.core.invoicing.domain.FeeDecisionDifference
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.invoicing.domain.VoucherValueDecisionDifference
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.pis.CreateSource
import evaka.core.pis.ModifySource
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ShiftCareType
import evaka.core.shared.auth.UserRole
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.TimeRange
import evaka.core.shared.security.PilotFeature
import evaka.core.user.EvakaUserType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class BiApplication(
    val id: UUID,
    val created: String,
    val updated: String,
    val sentdate: LocalDate?,
    val duedate: LocalDate?,
    val guardian_id: UUID,
    val child_id: UUID,
    val checkedbyadmin: Boolean,
    val hidefromguardian: Boolean,
    val transferapplication: Boolean,
    val additionaldaycareapplication: Boolean,
    val status: ApplicationStatus,
    val origin: ApplicationOrigin,
    val duedate_set_manually_at: String?,
    val service_worker_note: String,
    val type: ApplicationType,
    val allow_other_guardian_access: Boolean,
    val document: String?,
    val form_modified: String?,
)

data class BiArea(
    val id: UUID,
    val created: String,
    val updated: String,
    val name: String,
    val area_code: Int?,
    val sub_cost_center: String?,
    val short_name: String,
)

data class BiPerson(
    val id: UUID,
    val social_security_number: String?,
    val first_name: String,
    val last_name: String,
    val email: String?,
    val aad_object_id: UUID?,
    val language: String?,
    val date_of_birth: LocalDate,
    val created: String,
    val updated: String,
    val street_address: String,
    val postal_code: String,
    val post_office: String,
    val nationalities: List<String>,
    val restricted_details_enabled: Boolean,
    val restricted_details_end_date: LocalDate?,
    val phone: String,
    val updated_from_vtj: String?,
    val invoicing_street_address: String,
    val invoicing_postal_code: String,
    val invoicing_post_office: String,
    val invoice_recipient_name: String,
    val date_of_death: LocalDate?,
    val residence_code: String,
    val force_manual_fee_decisions: Boolean,
    val backup_phone: String,
    val last_login: String?,
    val freetext_vec: String? = "", // BI should have no need for this
    val oph_person_oid: String?,
    val vtj_guardians_queried: String?,
    val vtj_dependants_queried: String?,
    val ssn_adding_disabled: Boolean,
    val preferred_name: String,
    val duplicate_of: UUID?,
    val enabled_email_types: List<String>?,
    val keycloak_email: String?,
)

data class BiAbsence(
    val id: UUID,
    val child_id: UUID,
    val date: LocalDate,
    val absence_type: AbsenceType,
    val modified_at: String,
    val modified_by: UUID,
    val category: AbsenceCategory,
    val questionnaire_id: UUID?,
)

data class BiApplicationForm(
    val id: UUID,
    val application_id: UUID,
    val created: String,
    val revision: Long,
    val document: String,
    val updated: String,
    val latest: Boolean,
)

data class BiAssistanceAction(
    val id: UUID,
    val created: String,
    val updated: String,
    val updated_by: UUID,
    val child_id: UUID,
    val start_date: LocalDate,
    val end_date: LocalDate,
    val other_action: String,
    val measures: String, // no db enum type in kotlin, string representation of db enum array
)

data class BiAssistanceActionOption(
    val id: UUID,
    val created: String,
    val updated: String,
    val value: String,
    val name_fi: String,
    val display_order: Int?,
    val description_fi: String?,
)

data class BiAssistanceActionOptionRef(
    val action_id: UUID,
    val option_id: UUID,
    val created: String,
)

data class BiAssistanceFactor(
    val id: UUID,
    val created: String,
    val updated: String,
    val child_id: UUID,
    val modified: String,
    val modified_by: UUID,
    val valid_during: String, // DateRange
    val capacity_factor: BigDecimal,
)

data class BiAssistanceNeedVoucherCoefficient(
    val id: UUID,
    val created: String,
    val updated: String,
    val child_id: UUID,
    val validity_period: String, // DateRange
    val coefficient: BigDecimal,
)

data class BiAttendanceReservation(val child_id: String, val date: LocalDate)

data class BiBackupCare(
    val id: UUID,
    val created: String,
    val updated: String,
    val child_id: UUID,
    val unit_id: UUID,
    val group_id: UUID?,
    val start_date: LocalDate,
    val end_date: LocalDate,
)

data class BiChild(
    val id: UUID,
    val allergies: String,
    val diet: String,
    val additionalinfo: String,
    val medication: String,
    val language_at_home: String,
    val language_at_home_details: String,
)

data class BiChildAttendance(
    val id: UUID,
    val child_id: UUID,
    val created: String,
    val updated: String,
    val unit_id: UUID,
    val date: LocalDate,
    val start_time: LocalTime,
    val end_time: LocalTime?,
)

data class BiDaycare(
    val id: UUID,
    val name: String,
    val type: List<CareType>,
    val care_area_id: UUID,
    val phone: String?,
    val url: String?,
    val created: String,
    val updated: String,
    val backup_location: String?,
    val language_emphasis_id: UUID?,
    val opening_date: LocalDate?,
    val closing_date: LocalDate?,
    val email: String?,
    val schedule: String?,
    val additional_info: String?,
    val cost_center: String?,
    val upload_to_varda: Boolean,
    val capacity: Int,
    val decision_daycare_name: String,
    val decision_preschool_name: String,
    val decision_handler: String,
    val decision_handler_address: String,
    val street_address: String,
    val postal_code: String,
    val post_office: String,
    val mailing_po_box: String?,
    val location: String?, // postgres Point
    val mailing_street_address: String?,
    val mailing_postal_code: String?,
    val mailing_post_office: String?,
    val invoiced_by_municipality: Boolean,
    val provider_type: ProviderType,
    val language: Language,
    val upload_to_koski: Boolean,
    val oph_unit_oid: String?,
    val oph_organizer_oid: String?,
    val ghost_unit: Boolean?,
    val daycare_apply_period: DateRange?,
    val preschool_apply_period: DateRange?,
    val club_apply_period: DateRange?,
    val finance_decision_handler: UUID?,
    val round_the_clock: Boolean,
    val enabled_pilot_features: List<PilotFeature>,
    val upload_children_to_varda: Boolean,
    val business_id: String,
    val iban: String,
    val provider_id: String,
    val operation_times: List<TimeRange>,
    val unit_manager_name: String,
    val unit_manager_phone: String,
    val unit_manager_email: String,
    val dw_cost_center: String?,
    val daily_preschool_time: TimeRange?,
    val daily_preparatory_time: TimeRange?,
    val mealtime_breakfast: TimeRange?,
    val mealtime_lunch: TimeRange?,
    val mealtime_snack: TimeRange?,
    val mealtime_supper: TimeRange?,
    val mealtime_evening_snack: TimeRange?,
)

data class BiDaycareAssistance(
    val id: UUID,
    val created: String,
    val updated: String,
    val child_id: UUID,
    val modified: String,
    val modified_by: UUID,
    val valid_during: String, // DateRange
    val level: DaycareAssistanceLevel,
)

data class BiDaycareCaretaker(
    val id: UUID,
    val created: String,
    val updated: String,
    val group_id: UUID,
    val amount: BigDecimal,
    val start_date: LocalDate,
    val end_date: LocalDate?,
)

data class BiDaycareGroup(
    val id: UUID,
    val daycare_id: UUID,
    val name: String,
    val start_date: LocalDate,
    val end_date: LocalDate?,
    val jamix_customer_number: Int?,
)

data class BiDaycareGroupPlacement(
    val id: UUID,
    val created: String?,
    val updated: String?,
    val daycare_placement_id: UUID,
    val daycare_group_id: UUID,
    val start_date: LocalDate,
    val end_date: LocalDate,
)

data class BiDecision(
    val id: UUID,
    val number: Int,
    val created: String?,
    val updated: String?,
    val created_by: UUID,
    val sent_date: LocalDate?,
    val unit_id: UUID,
    val application_id: UUID,
    val type: DecisionType,
    val start_date: LocalDate,
    val end_date: LocalDate,
    val status: DecisionStatus,
    val requested_start_date: LocalDate?,
    val resolved: String?,
    val resolved_by: UUID?,
    val planned: Boolean,
    val pending_decision_emails_sent_count: Int?,
    val pending_decision_email_sent: String?,
    val document_key: String?,
    val other_guardian_document_key: String?,
    val document_contains_contact_info: Boolean,
)

data class BiEmployee(
    val id: UUID,
    val first_name: String,
    val last_name: String,
    val email: String?,
    val created: String,
    val updated: String,
    val roles: List<UserRole>,
    val external_id: String?,
    val last_login: String,
    val employee_number: String?,
    val preferred_first_name: String?,
    val temporary_in_unit_id: UUID?,
    val active: Boolean,
)

data class BiEvakaUser(
    val id: UUID,
    val type: EvakaUserType,
    val citizen_id: UUID?,
    val employee_id: UUID?,
    val mobile_device_id: UUID?,
    val name: String,
)

data class BiFeeAlteration(
    val id: UUID,
    val person_id: UUID,
    val type: FeeAlterationType,
    val amount: Int,
    val is_absolute: Boolean,
    val valid_from: LocalDate,
    val valid_to: LocalDate?,
    val notes: String,
    val updated_at: String,
    val updated_by: UUID,
)

data class BiFeeDecision(
    val id: UUID,
    val created: String,
    val updated: String,
    val status: FeeDecisionStatus,
    val valid_during: String, // DateRange
    val decision_type: FeeDecisionType,
    val head_of_family_id: UUID,
    val head_of_family_income: String?, // JSON
    val partner_id: UUID?,
    val partner_income: String?, // JSON
    val family_size: Int,
    val fee_thresholds: String, // JSON
    val decision_number: Long?,
    val document_key: String?,
    val approved_at: String?,
    val approved_by_id: UUID?,
    val decision_handler_id: UUID?,
    val sent_at: String?,
    val cancelled_at: String?,
    val total_fee: Int,
    val difference: List<FeeDecisionDifference>,
    val document_contains_contact_info: Boolean,
)

data class BiFeeDecisionChild(
    val id: UUID,
    val created: String,
    val updated: String,
    val fee_decision_id: UUID,
    val child_id: UUID,
    val child_date_of_birth: LocalDate,
    val sibling_discount: Int,
    val placement_unit_id: UUID,
    val placement_type: PlacementType,
    val service_need_fee_coefficient: BigDecimal,
    val service_need_description_fi: String,
    val service_need_description_sv: String,
    val base_fee: Int,
    val fee: Int,
    val fee_alterations: String, // JSON
    val final_fee: Int,
    val service_need_missing: Boolean,
    val service_need_contract_days_per_month: Int?,
    val child_income: String?, // JSON
    val service_need_option_id: UUID?,
)

data class BiFeeThresholds(
    val id: UUID,
    val valid_during: String, // DateRange
    val min_income_threshold_2: Int,
    val min_income_threshold_3: Int,
    val min_income_threshold_4: Int,
    val min_income_threshold_5: Int,
    val min_income_threshold_6: Int,
    val income_multiplier_2: BigDecimal,
    val income_multiplier_3: BigDecimal,
    val income_multiplier_4: BigDecimal,
    val income_multiplier_5: BigDecimal,
    val income_multiplier_6: BigDecimal,
    val max_income_threshold_2: Int,
    val max_income_threshold_3: Int,
    val max_income_threshold_4: Int,
    val max_income_threshold_5: Int,
    val max_income_threshold_6: Int,
    val income_threshold_increase_6_plus: Int,
    val sibling_discount_2: BigDecimal,
    val sibling_discount_2_plus: BigDecimal,
    val max_fee: Int,
    val min_fee: Int,
    val created: String,
    val updated: String,
    val temporary_fee: Int,
    val temporary_fee_part_day: Int,
    val temporary_fee_sibling: Int,
    val temporary_fee_sibling_part_day: Int,
)

data class BiFridgeChild(
    val id: UUID,
    val child_id: UUID,
    val head_of_child: UUID,
    val start_date: LocalDate,
    val end_date: LocalDate,
    val created_at: String?,
    val updated: String?,
    val conflict: Boolean,
)

data class BiFridgePartner(
    val partnership_id: UUID,
    val indx: Int,
    val person_id: UUID,
    val start_date: LocalDate,
    val end_date: LocalDate?,
    val created_at: String,
    val updated: String?,
    val conflict: Boolean,
    val other_indx: Int,
    val create_source: CreateSource?,
    val created_by: UUID?,
    val modify_source: ModifySource?,
    val modified_at: String?,
    val modified_by: UUID?,
    val created_from_application: UUID?,
)

data class BiGuardian(val guardian_id: UUID, val child_id: UUID, val created: String?)

data class BiGuardianBlocklist(
    val guardian_id: UUID,
    val child_id: UUID,
    val created: String,
    val updated: String,
)

data class BiHolidayPeriod(
    val id: UUID,
    val created: String,
    val updated: String,
    val period: String, // DateRange
    val reservation_deadline: LocalDate,
)

data class BiHolidayQuestionnaireAnswer(
    val id: UUID,
    val created: String,
    val updated: String,
    val modified_by: UUID?,
    val questionnaire_id: UUID?,
    val child_id: UUID?,
    val fixed_period: String?, // DateRange
)

data class BiIncome(
    val id: UUID,
    val person_id: UUID,
    val data: String, // JSON
    val valid_from: LocalDate,
    val valid_to: LocalDate?,
    val notes: String,
    val updated_at: String,
    val effect: IncomeEffect,
    val is_entrepreneur: Boolean,
    val works_at_echa: Boolean,
    val application_id: UUID?,
    val updated_by: UUID,
)

data class BiOtherAssistanceMeasure(
    val id: UUID,
    val created: String,
    val updated: String,
    val child_id: UUID,
    val modified: String,
    val modified_by: UUID,
    val valid_during: String, // DateRange
    val type: OtherAssistanceMeasureType,
)

data class BiPlacement(
    val id: UUID,
    val created: String?,
    val updated: String?,
    val type: PlacementType,
    val child_id: UUID,
    val unit_id: UUID,
    val start_date: LocalDate,
    val end_date: LocalDate,
    val termination_requested_date: LocalDate?,
    val terminated_by: UUID?,
    val place_guarantee: Boolean,
)

data class BiPreschoolAssistance(
    val id: UUID,
    val created: String,
    val updated: String,
    val child_id: UUID,
    val modified: String,
    val modified_by: UUID,
    val valid_during: String, // DateRange
    val level: PreschoolAssistanceLevel,
)

data class BiServiceNeed(
    val id: UUID,
    val created: String,
    val updated: String,
    val option_id: UUID,
    val placement_id: UUID,
    val start_date: LocalDate,
    val end_date: LocalDate,
    val confirmed_by: UUID?,
    val confirmed_at: String?,
    val shift_care: ShiftCareType,
)

data class BiServiceNeedOption(
    val id: UUID,
    val created: String,
    val updated: String,
    val name_fi: String,
    val valid_placement_type: PlacementType,
    val fee_coefficient: BigDecimal,
    val occupancy_coefficient: BigDecimal,
    val part_day: Boolean,
    val part_week: Boolean,
    val daycare_hours_per_week: Int,
    val default_option: Boolean,
    val fee_description_fi: String,
    val fee_description_sv: String,
    val voucher_value_description_fi: String,
    val voucher_value_description_sv: String,
    val display_order: Int?,
    val name_sv: String,
    val name_en: String,
    val contract_days_per_month: Int?,
    val occupancy_coefficient_under_3y: BigDecimal,
    val show_for_citizen: Boolean,
    val realized_occupancy_coefficient: BigDecimal,
    val realized_occupancy_coefficient_under_3y: BigDecimal,
    val daycare_hours_per_month: Int?,
    val valid_from: LocalDate,
    val valid_to: LocalDate?,
)

data class BiServiceNeedOptionVoucherValue(
    val id: UUID,
    val created: String,
    val updated: String,
    val service_need_option_id: UUID,
    val validity: String, // DateRange
    val base_value: Int,
    val coefficient: BigDecimal,
    val value: Int,
    val base_value_under_3y: Int,
    val coefficient_under_3y: BigDecimal,
    val value_under_3y: Int,
)

data class BiStaffAttendance(
    val id: UUID,
    val group_id: UUID,
    val date: LocalDate,
    val count: BigDecimal,
    val created: String?,
    val count_other: BigDecimal,
    val updated: String,
)

data class BiStaffAttendanceExternal(
    val id: UUID,
    val created: String,
    val updated: String,
    val name: String,
    val group_id: UUID,
    val arrived: String,
    val departed: String?,
    val occupancy_coefficient: BigDecimal,
    val departed_automatically: Boolean,
)

data class BiStaffAttendancePlan(
    val id: UUID,
    val created: String,
    val updated: String,
    val employee_id: UUID,
    val type: StaffAttendanceType,
    val start_time: String,
    val end_time: String,
    val description: String?,
)

data class BiStaffOccupancyCoefficient(
    val id: UUID,
    val created: String,
    val updated: String?,
    val employee_id: UUID,
    val daycare_id: UUID,
    val coefficient: BigDecimal,
)

data class BiVoucherValueDecision(
    val id: UUID,
    val status: VoucherValueDecisionStatus,
    val valid_from: LocalDate?,
    val valid_to: LocalDate?,
    val decision_number: Long?,
    val head_of_family_id: UUID,
    val partner_id: UUID?,
    val head_of_family_income: String?, // JSON
    val partner_income: String?, // JSON
    val family_size: Int,
    val fee_thresholds: String, // JSON
    val document_key: String?,
    val created: String?,
    val approved_by: UUID?,
    val approved_at: String?,
    val sent_at: String?,
    val cancelled_at: String?,
    val decision_handler: UUID?,
    val child_id: UUID,
    val child_date_of_birth: LocalDate,
    val base_co_payment: Int,
    val sibling_discount: Int,
    val placement_unit_id: UUID?,
    val placement_type: PlacementType?,
    val co_payment: Int,
    val fee_alterations: String, // JSON
    val base_value: Int,
    val voucher_value: Int,
    val final_co_payment: Int,
    val service_need_fee_coefficient: BigDecimal?,
    val service_need_voucher_value_coefficient: BigDecimal?,
    val service_need_fee_description_fi: String?,
    val service_need_fee_description_sv: String?,
    val service_need_voucher_value_description_fi: String?,
    val service_need_voucher_value_description_sv: String?,
    val updated: String,
    val assistance_need_coefficient: BigDecimal,
    val decision_type: VoucherValueDecisionType,
    val annulled_at: String?,
    val validity_updated_at: String?,
    val child_income: String?, // JSON
    val difference: List<VoucherValueDecisionDifference>,
    val service_need_missing: Boolean,
    val document_contains_contact_info: Boolean,
)
