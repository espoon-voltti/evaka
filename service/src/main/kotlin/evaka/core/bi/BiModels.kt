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
import evaka.core.family.CreateSource
import evaka.core.family.ModifySource
import evaka.core.invoicing.domain.FeeAlterationType
import evaka.core.invoicing.domain.FeeDecisionDifference
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.invoicing.domain.VoucherValueDecisionDifference
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
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

// The CSV columns come in the declaration order of the properties. A new property may only be added
// to the end of its class. This is an agreement with the recipients of the BI data: the existing
// columns must keep their positions in the exported files.

data class BiApplication(
    val additionaldaycareapplication: Boolean,
    val allow_other_guardian_access: Boolean,
    val checkedbyadmin: Boolean,
    val child_id: UUID,
    val created: String,
    @Pii val document: String?,
    val duedate: LocalDate?,
    val duedate_set_manually_at: String?,
    val form_modified: String?,
    val guardian_id: UUID,
    val hidefromguardian: Boolean,
    val id: UUID,
    val origin: ApplicationOrigin,
    val sentdate: LocalDate?,
    @Pii val service_worker_note: String,
    val status: ApplicationStatus,
    val transferapplication: Boolean,
    val type: ApplicationType,
    val updated: String,
)

data class BiArea(
    val area_code: Int?,
    val created: String,
    val id: UUID,
    val name: String,
    val short_name: String,
    val sub_cost_center: String?,
    val updated: String,
)

data class BiPerson(
    @Pii val aad_object_id: UUID?,
    @Pii val backup_phone: String,
    val created: String,
    val date_of_birth: LocalDate,
    val date_of_death: LocalDate?,
    val duplicate_of: UUID?,
    @Pii val email: String?,
    @LegacyColumn val enabled_email_types: List<String>?,
    @Pii val first_name: String,
    val force_manual_fee_decisions: Boolean,
    @LegacyColumn val freetext_vec: String? = "", // BI should have no need for this
    val id: UUID,
    @Pii val invoice_recipient_name: String,
    @Pii val invoicing_post_office: String,
    @Pii val invoicing_postal_code: String,
    @Pii val invoicing_street_address: String,
    @Pii @LegacyColumn val keycloak_email: String?,
    val language: String?,
    val last_login: String?,
    @Pii val last_name: String,
    val nationalities: List<String>,
    val oph_person_oid: String?,
    @Pii val phone: String,
    val post_office: String,
    val postal_code: String,
    @Pii val preferred_name: String,
    val residence_code: String,
    val restricted_details_enabled: Boolean,
    val restricted_details_end_date: LocalDate?,
    @Pii val social_security_number: String?,
    val ssn_adding_disabled: Boolean,
    val street_address: String,
    val updated: String,
    val updated_from_vtj: String?,
    val vtj_dependants_queried: String?,
    val vtj_guardians_queried: String?,
)

data class BiAbsence(
    val absence_type: AbsenceType,
    val category: AbsenceCategory,
    val child_id: UUID,
    val date: LocalDate,
    val id: UUID,
    val modified_at: String,
    val modified_by: UUID,
    val questionnaire_id: UUID?,
)

data class BiApplicationForm(
    val application_id: UUID,
    val created: String,
    @Pii val document: String,
    val id: UUID,
    @LegacyColumn val latest: Boolean,
    @LegacyColumn val revision: Long,
    val updated: String,
)

data class BiAssistanceAction(
    val child_id: UUID,
    val created: String,
    val end_date: LocalDate,
    val id: UUID,
    @LegacyColumn
    val measures: String, // no db enum type in kotlin, string representation of db enum array
    @Pii val other_action: String,
    val start_date: LocalDate,
    val updated: String,
    val updated_by: UUID,
)

data class BiAssistanceActionOption(
    val created: String,
    val description_fi: String?,
    val display_order: Int?,
    val id: UUID,
    val name_fi: String,
    val updated: String,
    val value: String,
)

data class BiAssistanceActionOptionRef(
    val action_id: UUID,
    val created: String,
    val option_id: UUID,
)

data class BiAssistanceFactor(
    val capacity_factor: BigDecimal,
    val child_id: UUID,
    val created: String,
    val id: UUID,
    val modified: String,
    val modified_by: UUID,
    val updated: String,
    val valid_during: String, // DateRange
)

data class BiAssistanceNeedVoucherCoefficient(
    val child_id: UUID,
    val coefficient: BigDecimal,
    val created: String,
    val id: UUID,
    val updated: String,
    val validity_period: String, // DateRange
)

data class BiAttendanceReservation(val child_id: String, val date: LocalDate)

data class BiAttendanceReservationDelta(
    val child_id: UUID,
    val created_at: String,
    val created_by: UUID,
    val date: LocalDate,
    val end_time: LocalTime?,
    val id: UUID,
    val start_time: LocalTime?,
    val updated_at: String,
)

data class BiBackupCare(
    val child_id: UUID,
    val created: String,
    val end_date: LocalDate,
    val group_id: UUID?,
    val id: UUID,
    val start_date: LocalDate,
    val unit_id: UUID,
    val updated: String,
)

data class BiChild(
    val additionalinfo: String,
    val allergies: String,
    val diet: String,
    val id: UUID,
    val language_at_home: String,
    val language_at_home_details: String,
    val medication: String,
)

data class BiChildAttendance(
    val child_id: UUID,
    val created: String,
    val date: LocalDate,
    val end_time: LocalTime?,
    val id: UUID,
    val start_time: LocalTime,
    val unit_id: UUID,
    val updated: String,
)

data class BiClubTerm(
    val id: UUID,
    val created: String,
    val updated: String,
    val term: String, // DateRange
    val application_period: String, // DateRange
    val term_breaks: String, // DateMultiRange
)

data class BiDaycare(
    val additional_info: String?,
    val backup_location: String?,
    val business_id: String,
    val capacity: Int,
    val care_area_id: UUID,
    val closing_date: LocalDate?,
    val club_apply_period: DateRange?,
    val cost_center: String?,
    val created: String,
    val daily_preparatory_time: TimeRange?,
    val daily_preschool_time: TimeRange?,
    val daycare_apply_period: DateRange?,
    val decision_daycare_name: String,
    val decision_handler: String,
    val decision_handler_address: String,
    val decision_preschool_name: String,
    val dw_cost_center: String?,
    val email: String?,
    val enabled_pilot_features: List<PilotFeature>,
    val finance_decision_handler: UUID?,
    val ghost_unit: Boolean?,
    val iban: String,
    val id: UUID,
    val invoiced_by_municipality: Boolean,
    val language: Language,
    @LegacyColumn val language_emphasis_id: UUID?,
    val location: String?, // postgres Point
    val mailing_po_box: String?,
    val mailing_post_office: String?,
    val mailing_postal_code: String?,
    val mailing_street_address: String?,
    val mealtime_breakfast: TimeRange?,
    val mealtime_evening_snack: TimeRange?,
    val mealtime_lunch: TimeRange?,
    val mealtime_snack: TimeRange?,
    val mealtime_supper: TimeRange?,
    val name: String,
    val opening_date: LocalDate?,
    val operation_times: List<TimeRange>,
    val oph_organizer_oid: String?,
    val oph_unit_oid: String?,
    val phone: String?,
    val post_office: String,
    val postal_code: String,
    val preschool_apply_period: DateRange?,
    val provider_id: String,
    val provider_type: ProviderType,
    val round_the_clock: Boolean,
    val schedule: String?,
    val street_address: String,
    val type: List<CareType>,
    val unit_manager_email: String,
    val unit_manager_name: String,
    val unit_manager_phone: String,
    val updated: String,
    val upload_children_to_varda: Boolean,
    val upload_to_koski: Boolean,
    val upload_to_varda: Boolean,
    val url: String?,
    val shift_care_operation_days: List<Int>?,
    val shift_care_open_on_holidays: Boolean,
)

data class BiDaycareAssistance(
    val child_id: UUID,
    val created: String,
    val id: UUID,
    val level: DaycareAssistanceLevel,
    val modified: String,
    val modified_by: UUID,
    val updated: String,
    val valid_during: String, // DateRange
)

data class BiDaycareCaretaker(
    val amount: BigDecimal,
    val created: String,
    val end_date: LocalDate?,
    val group_id: UUID,
    val id: UUID,
    val start_date: LocalDate,
    val updated: String,
)

data class BiDaycareGroup(
    val daycare_id: UUID,
    val end_date: LocalDate?,
    val id: UUID,
    val jamix_customer_number: Int?,
    val name: String,
    val start_date: LocalDate,
)

data class BiDaycareGroupPlacement(
    val created: String?,
    val daycare_group_id: UUID,
    val daycare_placement_id: UUID,
    val end_date: LocalDate,
    val id: UUID,
    val start_date: LocalDate,
    val updated: String?,
)

data class BiDecision(
    val application_id: UUID,
    val created: String?,
    val created_by: UUID,
    val document_contains_contact_info: Boolean,
    val document_key: String?,
    val end_date: LocalDate,
    val id: UUID,
    val number: Int,
    val other_guardian_document_key: String?,
    val pending_decision_email_sent: String?,
    val pending_decision_emails_sent_count: Int?,
    val planned: Boolean,
    val requested_start_date: LocalDate?,
    val resolved: String?,
    val resolved_by: UUID?,
    val sent_date: LocalDate?,
    val start_date: LocalDate,
    val status: DecisionStatus,
    val type: DecisionType,
    val unit_id: UUID,
    val updated: String?,
)

data class BiEmployee(
    val active: Boolean,
    val created: String,
    @Pii val email: String?,
    @Pii val employee_number: String?,
    @Pii val external_id: String?,
    @Pii val first_name: String,
    val id: UUID,
    val last_login: String,
    @Pii val last_name: String,
    @Pii val preferred_first_name: String?,
    val roles: List<UserRole>,
    val temporary_in_unit_id: UUID?,
    val updated: String,
)

data class BiEvakaUser(
    val citizen_id: UUID?,
    val employee_id: UUID?,
    val id: UUID,
    val mobile_device_id: UUID?,
    val name: String,
    val type: EvakaUserType,
)

data class BiFeeAlteration(
    val amount: Int,
    val id: UUID,
    val is_absolute: Boolean,
    @Pii val notes: String,
    val person_id: UUID,
    val type: FeeAlterationType,
    val updated_at: String,
    val updated_by: UUID,
    val valid_from: LocalDate,
    val valid_to: LocalDate?,
)

data class BiFeeDecision(
    val approved_at: String?,
    val approved_by_id: UUID?,
    val cancelled_at: String?,
    val created: String,
    val decision_handler_id: UUID?,
    val decision_number: Long?,
    val decision_type: FeeDecisionType,
    val difference: List<FeeDecisionDifference>,
    val document_contains_contact_info: Boolean,
    val document_key: String?,
    val family_size: Int,
    val fee_thresholds: String, // JSON
    val head_of_family_id: UUID,
    val head_of_family_income: String?, // JSON
    val id: UUID,
    val partner_id: UUID?,
    val partner_income: String?, // JSON
    val sent_at: String?,
    val status: FeeDecisionStatus,
    val total_fee: Int,
    val updated: String,
    val valid_during: String, // DateRange
)

data class BiFeeDecisionChild(
    val base_fee: Int,
    val child_date_of_birth: LocalDate,
    val child_id: UUID,
    val child_income: String?, // JSON
    val created: String,
    val fee: Int,
    val fee_alterations: String, // JSON
    val fee_decision_id: UUID,
    val final_fee: Int,
    val id: UUID,
    val placement_type: PlacementType,
    val placement_unit_id: UUID,
    val service_need_contract_days_per_month: Int?,
    val service_need_description_fi: String,
    val service_need_description_sv: String,
    val service_need_fee_coefficient: BigDecimal,
    val service_need_missing: Boolean,
    val service_need_option_id: UUID?,
    val sibling_discount: Int,
    val updated: String,
)

data class BiFeeThresholds(
    val created: String,
    val id: UUID,
    val income_multiplier_2: BigDecimal,
    val income_multiplier_3: BigDecimal,
    val income_multiplier_4: BigDecimal,
    val income_multiplier_5: BigDecimal,
    val income_multiplier_6: BigDecimal,
    val income_threshold_increase_6_plus: Int,
    val max_fee: Int,
    val max_income_threshold_2: Int,
    val max_income_threshold_3: Int,
    val max_income_threshold_4: Int,
    val max_income_threshold_5: Int,
    val max_income_threshold_6: Int,
    val min_fee: Int,
    val min_income_threshold_2: Int,
    val min_income_threshold_3: Int,
    val min_income_threshold_4: Int,
    val min_income_threshold_5: Int,
    val min_income_threshold_6: Int,
    val sibling_discount_2: BigDecimal,
    val sibling_discount_2_plus: BigDecimal,
    val temporary_fee: Int,
    val temporary_fee_part_day: Int,
    val temporary_fee_sibling: Int,
    val temporary_fee_sibling_part_day: Int,
    val updated: String,
    val valid_during: String, // DateRange
)

data class BiFridgeChild(
    val child_id: UUID,
    val conflict: Boolean,
    val created_at: String?,
    val end_date: LocalDate,
    val head_of_child: UUID,
    val id: UUID,
    val start_date: LocalDate,
    val updated: String?,
)

data class BiFridgePartner(
    val conflict: Boolean,
    val create_source: CreateSource?,
    val created_at: String,
    val created_by: UUID?,
    val created_from_application: UUID?,
    val end_date: LocalDate?,
    val indx: Int,
    val modified_at: String?,
    val modified_by: UUID?,
    val modify_source: ModifySource?,
    val other_indx: Int,
    val partnership_id: UUID,
    val person_id: UUID,
    val start_date: LocalDate,
    val updated: String?,
)

data class BiGuardian(val child_id: UUID, val created: String?, val guardian_id: UUID)

data class BiGuardianBlocklist(
    val child_id: UUID,
    val created: String,
    val guardian_id: UUID,
    val updated: String,
)

data class BiHoliday(val date: LocalDate)

data class BiHolidayPeriod(
    val created: String,
    val id: UUID,
    val period: String, // DateRange
    val reservation_deadline: LocalDate,
    val updated: String,
)

data class BiHolidayQuestionnaireAnswer(
    val child_id: UUID?,
    val created: String,
    val fixed_period: String?, // DateRange
    val id: UUID,
    val modified_by: UUID?,
    val questionnaire_id: UUID?,
    val updated: String,
)

data class BiIncome(
    val application_id: UUID?,
    val data: String, // JSON
    val effect: IncomeEffect,
    val id: UUID,
    val is_entrepreneur: Boolean,
    val notes: String,
    val person_id: UUID,
    val updated_at: String,
    val updated_by: UUID,
    val valid_from: LocalDate,
    val valid_to: LocalDate?,
    val works_at_echa: Boolean,
)

data class BiOtherAssistanceMeasure(
    val child_id: UUID,
    val created: String,
    val id: UUID,
    val modified: String,
    val modified_by: UUID,
    val type: OtherAssistanceMeasureType,
    val updated: String,
    val valid_during: String, // DateRange
)

data class BiPlacement(
    val child_id: UUID,
    val created: String?,
    val end_date: LocalDate,
    val id: UUID,
    val place_guarantee: Boolean,
    val start_date: LocalDate,
    val terminated_by: UUID?,
    val termination_requested_date: LocalDate?,
    val type: PlacementType,
    val unit_id: UUID,
    val updated: String?,
)

data class BiPreschoolAssistance(
    val child_id: UUID,
    val created: String,
    val id: UUID,
    val level: PreschoolAssistanceLevel,
    val modified: String,
    val modified_by: UUID,
    val updated: String,
    val valid_during: String, // DateRange
)

data class BiPreschoolTerm(
    val id: UUID,
    val created: String,
    val updated: String,
    val finnish_preschool: String, // DateRange
    val swedish_preschool: String, // DateRange
    val extended_term: String, // DateRange
    val application_period: String, // DateRange
    val term_breaks: String, // DateMultiRange
)

data class BiServiceNeed(
    val confirmed_at: String?,
    val confirmed_by: UUID?,
    val created: String,
    val end_date: LocalDate,
    val id: UUID,
    val option_id: UUID,
    val placement_id: UUID,
    val shift_care: ShiftCareType,
    val start_date: LocalDate,
    val updated: String,
)

data class BiServiceNeedOption(
    val contract_days_per_month: Int?,
    val created: String,
    val daycare_hours_per_month: Int?,
    val daycare_hours_per_week: Int,
    val default_option: Boolean,
    val display_order: Int?,
    val fee_coefficient: BigDecimal,
    val fee_description_fi: String,
    val fee_description_sv: String,
    val id: UUID,
    val name_en: String,
    val name_fi: String,
    val name_sv: String,
    val occupancy_coefficient: BigDecimal,
    val occupancy_coefficient_under_3y: BigDecimal,
    val part_day: Boolean,
    val part_week: Boolean?,
    val realized_occupancy_coefficient: BigDecimal,
    val realized_occupancy_coefficient_under_3y: BigDecimal,
    val show_for_citizen: Boolean,
    val updated: String,
    val valid_from: LocalDate,
    val valid_placement_type: PlacementType,
    val valid_to: LocalDate?,
    val voucher_value_description_fi: String,
    val voucher_value_description_sv: String,
)

data class BiServiceNeedOptionVoucherValue(
    val base_value: Int,
    val base_value_under_3y: Int,
    val coefficient: BigDecimal,
    val coefficient_under_3y: BigDecimal,
    val created: String,
    val id: UUID,
    val service_need_option_id: UUID,
    val updated: String,
    val validity: String, // DateRange
    val value: Int,
    val value_under_3y: Int,
)

data class BiStaffAttendance(
    val count: BigDecimal,
    @LegacyColumn val count_other: BigDecimal,
    val created: String?,
    val date: LocalDate,
    val group_id: UUID,
    val id: UUID,
    val updated: String,
)

data class BiStaffAttendanceExternal(
    val arrived: String,
    val created: String,
    val departed: String?,
    val departed_automatically: Boolean,
    val group_id: UUID,
    val id: UUID,
    val name: String,
    val occupancy_coefficient: BigDecimal,
    val updated: String,
)

data class BiStaffAttendancePlan(
    val created: String,
    val description: String?,
    val employee_id: UUID,
    val end_time: String,
    val id: UUID,
    val start_time: String,
    val type: StaffAttendanceType,
    val updated: String,
)

data class BiStaffAttendanceRealtime(
    val arrived: String,
    val arrived_added_at: String?,
    val arrived_added_by: UUID?,
    val arrived_modified_at: String?,
    val arrived_modified_by: UUID?,
    val created: String,
    val departed: String?,
    val departed_added_at: String?,
    val departed_added_by: UUID?,
    val departed_automatically: Boolean,
    val departed_modified_at: String?,
    val departed_modified_by: UUID?,
    val employee_id: UUID,
    val group_id: UUID?,
    val id: UUID,
    val occupancy_coefficient: BigDecimal,
    val type: StaffAttendanceType,
    val updated: String,
)

data class BiStaffOccupancyCoefficient(
    val coefficient: BigDecimal,
    val created: String,
    val daycare_id: UUID,
    val employee_id: UUID,
    val id: UUID,
    val updated: String?,
)

data class BiVoucherValueDecision(
    val annulled_at: String?,
    val approved_at: String?,
    val approved_by: UUID?,
    val assistance_need_coefficient: BigDecimal,
    val base_co_payment: Int,
    val base_value: Int,
    val cancelled_at: String?,
    val child_date_of_birth: LocalDate,
    val child_id: UUID,
    val child_income: String?, // JSON
    val co_payment: Int,
    val created: String?,
    val decision_handler: UUID?,
    val decision_number: Long?,
    val decision_type: VoucherValueDecisionType,
    val difference: List<VoucherValueDecisionDifference>,
    val document_contains_contact_info: Boolean,
    val document_key: String?,
    val family_size: Int,
    val fee_alterations: String, // JSON
    val fee_thresholds: String, // JSON
    val final_co_payment: Int,
    val head_of_family_id: UUID,
    val head_of_family_income: String?, // JSON
    val id: UUID,
    val partner_id: UUID?,
    val partner_income: String?, // JSON
    val placement_type: PlacementType?,
    val placement_unit_id: UUID?,
    val sent_at: String?,
    val service_need_fee_coefficient: BigDecimal?,
    val service_need_fee_description_fi: String?,
    val service_need_fee_description_sv: String?,
    val service_need_missing: Boolean,
    val service_need_voucher_value_coefficient: BigDecimal?,
    val service_need_voucher_value_description_fi: String?,
    val service_need_voucher_value_description_sv: String?,
    val sibling_discount: Int,
    val status: VoucherValueDecisionStatus,
    val updated: String,
    val valid_from: LocalDate?,
    val valid_to: LocalDate?,
    val validity_updated_at: String?,
    val voucher_value: Int,
)
