// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import evaka.core.shared.db.Database
import evaka.core.shared.db.QuerySql
import kotlin.reflect.KClass

object BiQueries {
    val getAreas =
        csvQuery<BiArea> {
            sql(
                """
            SELECT id, name, created::text, updated::text, area_code, sub_cost_center, short_name
            FROM care_area
            """
            )
        }

    val getPersons =
        csvQuery<BiPerson> {
            sql(
                """
            SELECT id, social_security_number, first_name, last_name, email, aad_object_id, language, date_of_birth, created::text, updated::text, street_address, postal_code, post_office, nationalities, restricted_details_enabled, restricted_details_end_date, phone, updated_from_vtj::text, invoicing_street_address, invoicing_postal_code, invoicing_post_office, invoice_recipient_name, date_of_death, residence_code, force_manual_fee_decisions, backup_phone, last_login::text, oph_person_oid, vtj_guardians_queried::text, vtj_dependants_queried::text, ssn_adding_disabled, preferred_name, duplicate_of, NULL AS enabled_email_types
            FROM person
            """
            )
        }

    val getApplications =
        csvQuery<BiApplication> {
            sql(
                """
            select id, created_at::text AS created, updated_at::text AS updated, sentdate, duedate, guardian_id, child_id, checkedbyadmin, hidefromguardian, transferapplication, additionaldaycareapplication, status, origin, duedate_set_manually_at::text, service_worker_note, type, allow_other_guardian_access, document::text, modified_at::text AS form_modified
            FROM application
        """
            )
        }

    val getApplicationForms =
        csvQuery<BiApplicationForm> {
            sql(
                """
            select id, id AS application_id, created_by::text AS created, 1 AS revision, document::text, updated_at::text AS updated, true AS latest
            FROM application
        """
            )
        }

    val getAssistanceActions =
        csvQuery<BiAssistanceAction> {
            sql(
                """
            select id, created_at::text AS created, updated_at::text AS updated, modified_by AS updated_by, child_id, start_date, end_date, other_action, '' AS measures
            FROM assistance_action
        """
            )
        }

    val getAssistanceActionOptions =
        csvQuery<BiAssistanceActionOption> {
            sql(
                """
            select id, created_at::text AS created, updated_at::text AS updated, value, name_fi, display_order, description_fi
            FROM assistance_action_option
        """
            )
        }

    val getAssistanceActionOptionRefs =
        csvQuery<BiAssistanceActionOptionRef> {
            sql(
                """
            select action_id, option_id, created_at::text AS created
            FROM assistance_action_option_ref
        """
            )
        }

    val getAssistanceFactors =
        csvQuery<BiAssistanceFactor> {
            sql(
                """
            select id, created_at::text AS created, updated_at::text AS updated, child_id, modified_at::text AS modified, modified_by, valid_during::text, capacity_factor
            FROM assistance_factor
        """
            )
        }

    val getAssistanceNeedVoucherCoefficients =
        csvQuery<BiAssistanceNeedVoucherCoefficient> {
            sql(
                """
            select id, created_at::text AS created, updated_at::text AS updated, child_id, validity_period::text, coefficient
            FROM assistance_need_voucher_coefficient
        """
            )
        }

    val getAttendanceReservations =
        csvQuery<BiAttendanceReservation> {
            sql("SELECT DISTINCT child_id, date FROM attendance_reservation")
        }

    val getBackupCares =
        csvQuery<BiBackupCare> {
            sql(
                """
            select id, created_at::text AS created, updated_at::text AS updated, child_id, unit_id, group_id, start_date, end_date
            FROM backup_care
        """
            )
        }

    val getChildren =
        csvQuery<BiChild> {
            sql(
                """
            select id, allergies, diet, additionalinfo, medication, language_at_home, language_at_home_details
            FROM child
        """
            )
        }

    val getDaycares =
        csvQuery<BiDaycare> {
            sql(
                """
            select id, name, type, care_area_id, phone, url, created::text, updated::text, backup_location, NULL AS language_emphasis_id, opening_date, closing_date, email, schedule, additional_info, cost_center, upload_to_varda, capacity, decision_daycare_name, decision_preschool_name, decision_handler, decision_handler_address, street_address, postal_code, post_office, mailing_po_box, location::text, mailing_street_address, mailing_postal_code, mailing_post_office, invoiced_by_municipality, provider_type, language, upload_to_koski, oph_unit_oid, oph_organizer_oid, ghost_unit, daycare_apply_period, preschool_apply_period, club_apply_period, finance_decision_handler, provides_shift_care AS round_the_clock, enabled_pilot_features, upload_children_to_varda, business_id, iban, provider_id, operation_times, unit_manager_name, unit_manager_phone, unit_manager_email, dw_cost_center, mealtime_breakfast, mealtime_evening_snack, mealtime_lunch, mealtime_snack, mealtime_supper
            FROM daycare
        """
            )
        }

    val getDaycareAssistances =
        csvQuery<BiDaycareAssistance> {
            sql(
                """
            select id, created::text, updated::text, child_id, modified::text, modified_by, valid_during::text, level
            FROM daycare_assistance
        """
            )
        }

    val getDaycareCaretakers =
        csvQuery<BiDaycareCaretaker> {
            sql(
                """
            select id, created::text, updated::text, group_id, amount, start_date, end_date
            FROM daycare_caretaker
        """
            )
        }

    val getDaycareGroups =
        csvQuery<BiDaycareGroup> {
            sql(
                """
            select id, daycare_id, name, start_date, end_date, jamix_customer_number
            FROM daycare_group
        """
            )
        }

    val getDaycareGroupPlacements =
        csvQuery<BiDaycareGroupPlacement> {
            sql(
                """
            select id, created::text, updated::text, daycare_placement_id, daycare_group_id, start_date, end_date
            FROM daycare_group_placement
        """
            )
        }

    val getDecisions =
        csvQuery<BiDecision> {
            sql(
                """
            select id, number, created::text, updated::text, created_by, sent_date, unit_id, application_id, type, start_date, end_date, status, requested_start_date, resolved::text, resolved_by, planned, pending_decision_emails_sent_count, pending_decision_email_sent::text, document_key, other_guardian_document_key, document_contains_contact_info
            FROM decision
        """
            )
        }

    val getEmployees =
        csvQuery<BiEmployee> {
            sql(
                """
            select id, active, first_name, last_name, email, created::text, updated::text, roles, external_id, coalesce(last_login::text, '2020-12-31 22:00:00+00') AS last_login, employee_number, preferred_first_name, temporary_in_unit_id
            FROM employee
        """
            )
        }

    val getEvakaUsers =
        csvQuery<BiEvakaUser> {
            sql(
                """
            select id, type, citizen_id, employee_id, mobile_device_id, name
            FROM evaka_user
        """
            )
        }

    val getFeeAlterations =
        csvQuery<BiFeeAlteration> {
            sql(
                """
            select id, person_id, type, amount, is_absolute, valid_from, valid_to, notes, modified_at::text AS updated_at, modified_by AS updated_by
            FROM fee_alteration
        """
            )
        }
    val getFeeDecisions =
        csvQuery<BiFeeDecision> {
            sql(
                """
            select id, created::text, updated::text, status, valid_during::text, decision_type, head_of_family_id, head_of_family_income::text, partner_id, partner_income::text, family_size, fee_thresholds::text, decision_number, document_key, approved_at::text, approved_by_id, decision_handler_id, sent_at::text, cancelled_at::text, total_fee, difference, document_contains_contact_info
            FROM fee_decision
        """
            )
        }

    val getFeeDecisionChildren =
        csvQuery<BiFeeDecisionChild> {
            sql(
                """
            select id, created::text, updated::text, fee_decision_id, child_id, child_date_of_birth, sibling_discount, placement_unit_id, placement_type, service_need_fee_coefficient, service_need_description_fi, service_need_description_sv, base_fee, fee, fee_alterations, final_fee, service_need_missing, service_need_contract_days_per_month, child_income, service_need_option_id
            FROM fee_decision_child
        """
            )
        }

    val getFeeThresholds =
        csvQuery<BiFeeThresholds> {
            sql(
                """
            select id, valid_during::text, min_income_threshold_2, min_income_threshold_3, min_income_threshold_4, min_income_threshold_5, min_income_threshold_6, income_multiplier_2, income_multiplier_3, income_multiplier_4, income_multiplier_5, income_multiplier_6, max_income_threshold_2, max_income_threshold_3, max_income_threshold_4, max_income_threshold_5, max_income_threshold_6, income_threshold_increase_6_plus, sibling_discount_2, sibling_discount_2_plus, max_fee, min_fee, created::text, updated::text, temporary_fee, temporary_fee_part_day, temporary_fee_sibling, temporary_fee_sibling_part_day
            FROM fee_thresholds
        """
            )
        }

    val getFridgeChildren =
        csvQuery<BiFridgeChild> {
            sql(
                """
            select id, child_id, head_of_child, start_date, end_date, created_at::text, updated::text, conflict
            FROM fridge_child
        """
            )
        }

    val getFridgePartners =
        csvQuery<BiFridgePartner> {
            sql(
                """
            select partnership_id, indx, person_id, start_date, end_date, created_at::text, updated::text, conflict, other_indx, create_source, created_by, modify_source, modified_at::text, modified_by, created_from_application
            FROM fridge_partner
        """
            )
        }

    val getGuardians =
        csvQuery<BiGuardian> {
            sql(
                """
            select guardian_id, child_id, created::text
            FROM guardian
        """
            )
        }

    val getGuardianBlockLists =
        csvQuery<BiGuardianBlocklist> {
            sql(
                """
            select guardian_id, child_id, created::text, updated::text
            FROM guardian_blocklist
        """
            )
        }

    val getHolidayPeriods =
        csvQuery<BiHolidayPeriod> {
            sql(
                """
            select id, created::text, updated::text, period::text, reservation_deadline
            FROM holiday_period
        """
            )
        }

    val getHolidayQuestionnaireAnswers =
        csvQuery<BiHolidayQuestionnaireAnswer> {
            sql(
                """
            select id, created::text, updated::text, modified_by, questionnaire_id, child_id, fixed_period::text
            FROM holiday_questionnaire_answer
        """
            )
        }

    val getIncomes =
        csvQuery<BiIncome> {
            sql(
                """
            select id, person_id, data::text, valid_from, valid_to, notes, modified_at::text AS updated_at, effect, is_entrepreneur, works_at_echa, application_id, modified_by AS updated_by
            FROM income
        """
            )
        }

    val getOtherAssistanceMeasures =
        csvQuery<BiOtherAssistanceMeasure> {
            sql(
                """
            select id, created::text, updated::text, child_id, modified::text, modified_by, valid_during::text, type
            FROM other_assistance_measure
        """
            )
        }

    val getPlacements =
        csvQuery<BiPlacement> {
            sql(
                """
            select id, created_at::text AS created, updated_at::text AS updated, type, child_id, unit_id, start_date, end_date, termination_requested_date, terminated_by, place_guarantee
            FROM placement
        """
            )
        }

    val getPreschoolAssistances =
        csvQuery<BiPreschoolAssistance> {
            sql(
                """
            select id, created::text, updated::text, child_id, modified::text, modified_by, valid_during::text, level
            FROM preschool_assistance
        """
            )
        }

    val getServiceNeeds =
        csvQuery<BiServiceNeed> {
            sql(
                """
            select id, created::text, updated::text, option_id, placement_id, start_date, end_date, confirmed_by, confirmed_at::text, shift_care
            FROM service_need
        """
            )
        }

    val getServiceNeedOptions =
        csvQuery<BiServiceNeedOption> {
            sql(
                """
            select id, created::text, updated::text, name_fi, valid_placement_type, fee_coefficient, occupancy_coefficient, part_day, part_week, daycare_hours_per_week, default_option, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, display_order, name_sv, name_en, contract_days_per_month, occupancy_coefficient_under_3y, show_for_citizen, realized_occupancy_coefficient, realized_occupancy_coefficient_under_3y, daycare_hours_per_month, valid_from, valid_to
            FROM service_need_option
        """
            )
        }

    val getServiceNeedOptionVoucherValues =
        csvQuery<BiServiceNeedOptionVoucherValue> {
            sql(
                """
            select id, created::text, updated::text, service_need_option_id, validity::text, base_value, coefficient, value, base_value_under_3y, coefficient_under_3y, value_under_3y
            FROM service_need_option_voucher_value
        """
            )
        }

    val getStaffAttendance =
        csvQuery<BiStaffAttendance> {
            sql(
                """
            select id, group_id, date, count, created::text, 0::numeric AS count_other, updated::text
            FROM staff_attendance
        """
            )
        }

    val getStaffAttendanceExternals =
        csvQuery<BiStaffAttendanceExternal> {
            sql(
                """
            select id, created::text, updated::text, name, group_id, arrived::text, departed::text, occupancy_coefficient, departed_automatically
            FROM staff_attendance_external
        """
            )
        }

    val getStaffAttendancePlans =
        csvQuery<BiStaffAttendancePlan> {
            sql(
                """
            select id, created::text, updated::text, employee_id, type, start_time::text, end_time::text, description
            FROM staff_attendance_plan
        """
            )
        }

    val getStaffOccupancyCoefficients =
        csvQuery<BiStaffOccupancyCoefficient> {
            sql(
                """
            select id, created::text, updated::text, employee_id, daycare_id, coefficient
            FROM staff_occupancy_coefficient
        """
            )
        }

    val getVoucherValueDecisions =
        csvQuery<BiVoucherValueDecision> {
            sql(
                """
            select id, status, valid_from, valid_to, decision_number, head_of_family_id, partner_id, head_of_family_income::text, partner_income::text, family_size, fee_thresholds::text, document_key, created::text, approved_by, approved_at::text, sent_at::text, cancelled_at::text, decision_handler, child_id, child_date_of_birth, base_co_payment, sibling_discount, placement_unit_id, placement_type, co_payment, fee_alterations::text, base_value, voucher_value, final_co_payment, service_need_fee_coefficient, service_need_voucher_value_coefficient, service_need_fee_description_fi, service_need_fee_description_sv, service_need_voucher_value_description_fi, service_need_voucher_value_description_sv, updated::text, assistance_need_coefficient, decision_type, annulled_at::text, validity_updated_at::text, child_income::text, difference, service_need_missing, document_contains_contact_info
            FROM voucher_value_decision
        """
            )
        }

    // delta queries
    val getChildAttendanceDelta =
        csvQuery<BiChildAttendance> {
            sql(
                """
            select id, child_id, created_at::text AS created, modified_at::text AS updated, unit_id, date, start_time, end_time
            FROM child_attendance
            WHERE modified_at >= (current_date AT TIME ZONE 'Europe/Helsinki' - interval '60 days')::date
        """
            )
        }

    val getAbsencesDelta =
        csvQuery<BiAbsence> {
            sql(
                """
            SELECT id, child_id, date, absence_type, modified_at::text, modified_by, category, questionnaire_id
            FROM absence
            WHERE modified_at >= (current_date AT TIME ZONE 'Europe/Helsinki' - interval '60 days')::date
            """
            )
        }

    interface CsvQuery {
        operator fun <R> invoke(
            tx: Database.Read,
            config: BiExportConfig,
            useResults: (records: Sequence<String>) -> R,
        ): R
    }

    class StreamingCsvQuery<T : Any>(
        private val clazz: KClass<T>,
        private val query: (Database.Read) -> Database.Result<T>,
    ) : CsvQuery {
        override operator fun <R> invoke(
            tx: Database.Read,
            config: BiExportConfig,
            useResults: (records: Sequence<String>) -> R,
        ): R =
            query(tx).useSequence { rows ->
                useResults(toCsvRecords(::convertToCsv, clazz, rows, config))
            }
    }

    private const val QUERY_STREAM_CHUNK_SIZE = 10_000

    private inline fun <reified T : Any> csvQuery(
        crossinline f: QuerySql.Builder.() -> QuerySql
    ): CsvQuery =
        StreamingCsvQuery(T::class) { tx ->
            tx.createQuery { f() }.setFetchSize(QUERY_STREAM_CHUNK_SIZE).mapTo<T>()
        }
}
