-- Name: ext; Type: SCHEMA; Schema: -

CREATE SCHEMA ext;

-- Name: btree_gist; Type: EXTENSION; Schema: -

CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA ext;

-- Name: unaccent; Type: EXTENSION; Schema: -

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;

-- Name: uuid-ossp; Type: EXTENSION; Schema: -

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA ext;

-- Name: absence_application_status; Type: TYPE; Schema: public

CREATE TYPE public.absence_application_status AS ENUM (
    'WAITING_DECISION',
    'ACCEPTED',
    'REJECTED'
);

-- Name: absence_category; Type: TYPE; Schema: public

CREATE TYPE public.absence_category AS ENUM (
    'BILLABLE',
    'NONBILLABLE'
);

-- Name: absence_type; Type: TYPE; Schema: public

CREATE TYPE public.absence_type AS ENUM (
    'OTHER_ABSENCE',
    'SICKLEAVE',
    'UNKNOWN_ABSENCE',
    'PLANNED_ABSENCE',
    'PARENTLEAVE',
    'FORCE_MAJEURE',
    'FREE_ABSENCE',
    'UNAUTHORIZED_ABSENCE'
);

-- Name: application_origin_type; Type: TYPE; Schema: public

CREATE TYPE public.application_origin_type AS ENUM (
    'ELECTRONIC',
    'PAPER'
);

-- Name: application_status_type; Type: TYPE; Schema: public

CREATE TYPE public.application_status_type AS ENUM (
    'CREATED',
    'SENT',
    'WAITING_PLACEMENT',
    'WAITING_DECISION',
    'WAITING_MAILING',
    'WAITING_CONFIRMATION',
    'REJECTED',
    'ACTIVE',
    'CANCELLED',
    'TERMINATED',
    'DEPRECATED',
    'WAITING_UNIT_CONFIRMATION'
);

-- Name: application_type; Type: TYPE; Schema: public

CREATE TYPE public.application_type AS ENUM (
    'CLUB',
    'DAYCARE',
    'PRESCHOOL'
);

-- Name: archived_process_state; Type: TYPE; Schema: public

CREATE TYPE public.archived_process_state AS ENUM (
    'INITIAL',
    'PREPARATION',
    'DECIDING',
    'COMPLETED'
);

-- Name: assistance_action_option_category; Type: TYPE; Schema: public

CREATE TYPE public.assistance_action_option_category AS ENUM (
    'DAYCARE',
    'PRESCHOOL'
);

-- Name: assistance_need_decision_status; Type: TYPE; Schema: public

CREATE TYPE public.assistance_need_decision_status AS ENUM (
    'DRAFT',
    'NEEDS_WORK',
    'ACCEPTED',
    'REJECTED',
    'ANNULLED'
);

-- Name: assistance_need_preschool_decision_type; Type: TYPE; Schema: public

CREATE TYPE public.assistance_need_preschool_decision_type AS ENUM (
    'NEW',
    'CONTINUING',
    'TERMINATED'
);

-- Name: calendar_event_type; Type: TYPE; Schema: public

CREATE TYPE public.calendar_event_type AS ENUM (
    'DAYCARE_EVENT',
    'DISCUSSION_SURVEY'
);

-- Name: care_types; Type: TYPE; Schema: public

CREATE TYPE public.care_types AS ENUM (
    'CENTRE',
    'FAMILY',
    'GROUP_FAMILY',
    'CLUB',
    'PRESCHOOL',
    'PREPARATORY_EDUCATION'
);

-- Name: child_daily_note_level; Type: TYPE; Schema: public

CREATE TYPE public.child_daily_note_level AS ENUM (
    'GOOD',
    'MEDIUM',
    'NONE'
);

-- Name: child_daily_note_reminder; Type: TYPE; Schema: public

CREATE TYPE public.child_daily_note_reminder AS ENUM (
    'DIAPERS',
    'CLOTHES',
    'LAUNDRY'
);

-- Name: child_document_decision_status; Type: TYPE; Schema: public

CREATE TYPE public.child_document_decision_status AS ENUM (
    'ACCEPTED',
    'REJECTED',
    'ANNULLED'
);

-- Name: child_document_status; Type: TYPE; Schema: public

CREATE TYPE public.child_document_status AS ENUM (
    'DRAFT',
    'PREPARED',
    'CITIZEN_DRAFT',
    'DECISION_PROPOSAL',
    'COMPLETED'
);

-- Name: confirmation_status; Type: TYPE; Schema: public

CREATE TYPE public.confirmation_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'REJECTED_NOT_CONFIRMED'
);

-- Name: create_source; Type: TYPE; Schema: public

CREATE TYPE public.create_source AS ENUM (
    'USER',
    'APPLICATION',
    'DVV'
);

-- Name: curriculum_document_event_type; Type: TYPE; Schema: public

CREATE TYPE public.curriculum_document_event_type AS ENUM (
    'PUBLISHED',
    'MOVED_TO_READY',
    'RETURNED_TO_READY',
    'MOVED_TO_REVIEWED',
    'RETURNED_TO_REVIEWED',
    'MOVED_TO_CLOSED'
);

-- Name: curriculum_type; Type: TYPE; Schema: public

CREATE TYPE public.curriculum_type AS ENUM (
    'DAYCARE',
    'PRESCHOOL'
);

-- Name: daily_service_time_type; Type: TYPE; Schema: public

CREATE TYPE public.daily_service_time_type AS ENUM (
    'REGULAR',
    'IRREGULAR',
    'VARIABLE_TIME'
);

-- Name: daycare_assistance_level; Type: TYPE; Schema: public

CREATE TYPE public.daycare_assistance_level AS ENUM (
    'GENERAL_SUPPORT',
    'GENERAL_SUPPORT_WITH_DECISION',
    'INTENSIFIED_SUPPORT',
    'SPECIAL_SUPPORT'
);

-- Name: decision_status; Type: TYPE; Schema: public

CREATE TYPE public.decision_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED'
);

-- Name: decision_type; Type: TYPE; Schema: public

CREATE TYPE public.decision_type AS ENUM (
    'DAYCARE',
    'DAYCARE_PART_TIME',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'PRESCHOOL_CLUB',
    'PREPARATORY_EDUCATION',
    'CLUB'
);

-- Name: document_template_type; Type: TYPE; Schema: public

CREATE TYPE public.document_template_type AS ENUM (
    'PEDAGOGICAL_REPORT',
    'PEDAGOGICAL_ASSESSMENT',
    'HOJKS',
    'MIGRATED_VASU',
    'MIGRATED_LEOPS',
    'VASU',
    'LEOPS',
    'CITIZEN_BASIC',
    'OTHER_DECISION',
    'OTHER',
    'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION',
    'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'
);

-- Name: email_message_type; Type: TYPE; Schema: public

CREATE TYPE public.email_message_type AS ENUM (
    'TRANSACTIONAL',
    'MESSAGE_NOTIFICATION',
    'BULLETIN_NOTIFICATION',
    'INCOME_NOTIFICATION',
    'CALENDAR_EVENT_NOTIFICATION',
    'DECISION_NOTIFICATION',
    'DOCUMENT_NOTIFICATION',
    'INFORMAL_DOCUMENT_NOTIFICATION',
    'ATTENDANCE_RESERVATION_NOTIFICATION',
    'DISCUSSION_TIME_NOTIFICATION'
);

-- Name: evaka_user_type; Type: TYPE; Schema: public

CREATE TYPE public.evaka_user_type AS ENUM (
    'SYSTEM',
    'CITIZEN',
    'EMPLOYEE',
    'MOBILE_DEVICE',
    'UNKNOWN'
);

-- Name: fee_alteration_type; Type: TYPE; Schema: public

CREATE TYPE public.fee_alteration_type AS ENUM (
    'DISCOUNT',
    'INCREASE',
    'RELIEF'
);

-- Name: fee_decision_difference; Type: TYPE; Schema: public

CREATE TYPE public.fee_decision_difference AS ENUM (
    'GUARDIANS',
    'CHILDREN',
    'INCOME',
    'PLACEMENT',
    'SERVICE_NEED',
    'SIBLING_DISCOUNT',
    'FEE_ALTERATIONS',
    'FAMILY_SIZE',
    'FEE_THRESHOLDS'
);

-- Name: fee_decision_status; Type: TYPE; Schema: public

CREATE TYPE public.fee_decision_status AS ENUM (
    'DRAFT',
    'WAITING_FOR_SENDING',
    'WAITING_FOR_MANUAL_SENDING',
    'SENT',
    'ANNULLED',
    'IGNORED'
);

-- Name: fee_decision_type; Type: TYPE; Schema: public

CREATE TYPE public.fee_decision_type AS ENUM (
    'NORMAL',
    'RELIEF_REJECTED',
    'RELIEF_PARTLY_ACCEPTED',
    'RELIEF_ACCEPTED'
);

-- Name: income_effect; Type: TYPE; Schema: public

CREATE TYPE public.income_effect AS ENUM (
    'INCOME',
    'MAX_FEE_ACCEPTED',
    'INCOMPLETE',
    'NOT_AVAILABLE'
);

-- Name: income_notification_type; Type: TYPE; Schema: public

CREATE TYPE public.income_notification_type AS ENUM (
    'INITIAL_EMAIL',
    'REMINDER_EMAIL',
    'EXPIRED_EMAIL',
    'NEW_CUSTOMER'
);

-- Name: income_source; Type: TYPE; Schema: public

CREATE TYPE public.income_source AS ENUM (
    'INCOMES_REGISTER',
    'ATTACHMENTS'
);

-- Name: income_statement_status; Type: TYPE; Schema: public

CREATE TYPE public.income_statement_status AS ENUM (
    'DRAFT',
    'SENT',
    'HANDLING',
    'HANDLED'
);

-- Name: income_statement_type; Type: TYPE; Schema: public

CREATE TYPE public.income_statement_type AS ENUM (
    'HIGHEST_FEE',
    'INCOME',
    'CHILD_INCOME'
);

-- Name: invoice_replacement_reason; Type: TYPE; Schema: public

CREATE TYPE public.invoice_replacement_reason AS ENUM (
    'SERVICE_NEED',
    'ABSENCE',
    'INCOME',
    'FAMILY_SIZE',
    'RELIEF_RETROACTIVE',
    'OTHER'
);

-- Name: invoice_status; Type: TYPE; Schema: public

CREATE TYPE public.invoice_status AS ENUM (
    'DRAFT',
    'WAITING_FOR_SENDING',
    'SENT',
    'REPLACEMENT_DRAFT',
    'REPLACED'
);

-- Name: koski_preparatory_input_data; Type: TYPE; Schema: public

CREATE TYPE public.koski_preparatory_input_data AS (
	oph_unit_oid text,
	oph_organizer_oid text,
	placements datemultirange,
	all_placements_in_past boolean,
	last_of_child boolean,
	last_of_type boolean,
	absences jsonb
);

-- Name: koski_preschool_input_data; Type: TYPE; Schema: public

CREATE TYPE public.koski_preschool_input_data AS (
	oph_unit_oid text,
	oph_organizer_oid text,
	placements datemultirange,
	all_placements_in_past boolean,
	last_of_child boolean,
	special_support datemultirange,
	special_support_with_decision_level_1 datemultirange,
	special_support_with_decision_level_2 datemultirange,
	transport_benefit datemultirange,
	child_support datemultirange,
	child_support_and_extended_compulsory_education datemultirange,
	child_support_and_old_extended_compulsory_education datemultirange,
	child_support_2_and_old_extended_compulsory_education datemultirange
);

-- Name: koski_study_right_type; Type: TYPE; Schema: public

CREATE TYPE public.koski_study_right_type AS ENUM (
    'PRESCHOOL',
    'PREPARATORY'
);

-- Name: message_account_type; Type: TYPE; Schema: public

CREATE TYPE public.message_account_type AS ENUM (
    'PERSONAL',
    'GROUP',
    'CITIZEN',
    'MUNICIPAL',
    'SERVICE_WORKER',
    'FINANCE'
);

-- Name: message_type; Type: TYPE; Schema: public

CREATE TYPE public.message_type AS ENUM (
    'MESSAGE',
    'BULLETIN'
);

-- Name: modify_source; Type: TYPE; Schema: public

CREATE TYPE public.modify_source AS ENUM (
    'USER',
    'DVV'
);

-- Name: nekku_customer_weekday; Type: TYPE; Schema: public

CREATE TYPE public.nekku_customer_weekday AS ENUM (
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY',
    'SUNDAY',
    'WEEKDAYHOLIDAY'
);

-- Name: nekku_product_meal_time; Type: TYPE; Schema: public

CREATE TYPE public.nekku_product_meal_time AS ENUM (
    'BREAKFAST',
    'LUNCH',
    'SNACK',
    'DINNER',
    'SUPPER'
);

-- Name: nekku_product_meal_type; Type: TYPE; Schema: public

CREATE TYPE public.nekku_product_meal_type AS ENUM (
    'VEGAN',
    'VEGETABLE'
);

-- Name: nekku_special_diet_type; Type: TYPE; Schema: public

CREATE TYPE public.nekku_special_diet_type AS ENUM (
    'TEXT',
    'CHECKBOXLIST',
    'CHECKBOX',
    'RADIO',
    'TEXTAREA',
    'EMAIL'
);

-- Name: official_language; Type: TYPE; Schema: public

CREATE TYPE public.official_language AS ENUM (
    'FI',
    'SV'
);

-- Name: other_assistance_measure_type; Type: TYPE; Schema: public

CREATE TYPE public.other_assistance_measure_type AS ENUM (
    'TRANSPORT_BENEFIT',
    'ACCULTURATION_SUPPORT',
    'ANOMALOUS_EDUCATION_START',
    'CHILD_DISCUSSION_OFFERED',
    'CHILD_DISCUSSION_HELD',
    'CHILD_DISCUSSION_COUNSELING'
);

-- Name: other_income_type; Type: TYPE; Schema: public

CREATE TYPE public.other_income_type AS ENUM (
    'PENSION',
    'ADULT_EDUCATION_ALLOWANCE',
    'SICKNESS_ALLOWANCE',
    'PARENTAL_ALLOWANCE',
    'HOME_CARE_ALLOWANCE',
    'FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE',
    'ALIMONY',
    'INTEREST_AND_INVESTMENT_INCOME',
    'RENTAL_INCOME',
    'UNEMPLOYMENT_ALLOWANCE',
    'LABOUR_MARKET_SUBSIDY',
    'ADJUSTED_DAILY_ALLOWANCE',
    'JOB_ALTERNATION_COMPENSATION',
    'REWARD_OR_BONUS',
    'RELATIVE_CARE_SUPPORT',
    'BASIC_INCOME',
    'FOREST_INCOME',
    'FAMILY_CARE_COMPENSATION',
    'REHABILITATION',
    'EDUCATION_ALLOWANCE',
    'GRANT',
    'APPRENTICESHIP_SALARY',
    'ACCIDENT_INSURANCE_COMPENSATION',
    'OTHER_INCOME'
);

-- Name: pairing_status; Type: TYPE; Schema: public

CREATE TYPE public.pairing_status AS ENUM (
    'WAITING_CHALLENGE',
    'WAITING_RESPONSE',
    'READY',
    'PAIRED'
);

-- Name: payment_status; Type: TYPE; Schema: public

CREATE TYPE public.payment_status AS ENUM (
    'DRAFT',
    'SENT',
    'CONFIRMED'
);

-- Name: pilot_feature; Type: TYPE; Schema: public

CREATE TYPE public.pilot_feature AS ENUM (
    'MESSAGING',
    'MOBILE',
    'RESERVATIONS',
    'VASU_AND_PEDADOC',
    'MOBILE_MESSAGING',
    'PLACEMENT_TERMINATION',
    'REALTIME_STAFF_ATTENDANCE',
    'PUSH_NOTIFICATIONS',
    'SERVICE_APPLICATIONS',
    'STAFF_ATTENDANCE_INTEGRATION',
    'OTHER_DECISION',
    'CITIZEN_BASIC_DOCUMENT'
);

-- Name: placement_reject_reason; Type: TYPE; Schema: public

CREATE TYPE public.placement_reject_reason AS ENUM (
    'OTHER',
    'REASON_1',
    'REASON_2',
    'REASON_3'
);

-- Name: placement_source; Type: TYPE; Schema: public

CREATE TYPE public.placement_source AS ENUM (
    'APPLICATION',
    'SERVICE_APPLICATION',
    'PLACEMENT_TERMINATION',
    'MANUAL'
);

-- Name: placement_type; Type: TYPE; Schema: public

CREATE TYPE public.placement_type AS ENUM (
    'DAYCARE',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'PRESCHOOL_DAYCARE_ONLY',
    'PRESCHOOL_CLUB',
    'DAYCARE_PART_TIME',
    'PREPARATORY',
    'PREPARATORY_DAYCARE',
    'PREPARATORY_DAYCARE_ONLY',
    'CLUB',
    'TEMPORARY_DAYCARE',
    'TEMPORARY_DAYCARE_PART_DAY',
    'DAYCARE_FIVE_YEAR_OLDS',
    'DAYCARE_PART_TIME_FIVE_YEAR_OLDS',
    'SCHOOL_SHIFT_CARE'
);

-- Name: preschool_assistance_level; Type: TYPE; Schema: public

CREATE TYPE public.preschool_assistance_level AS ENUM (
    'INTENSIFIED_SUPPORT',
    'SPECIAL_SUPPORT',
    'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1',
    'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2',
    'CHILD_SUPPORT',
    'CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION',
    'GROUP_SUPPORT',
    'CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION',
    'CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION'
);

-- Name: push_notification_category; Type: TYPE; Schema: public

CREATE TYPE public.push_notification_category AS ENUM (
    'RECEIVED_MESSAGE',
    'NEW_ABSENCE',
    'CALENDAR_EVENT_RESERVATION'
);

-- Name: questionnaire_type; Type: TYPE; Schema: public

CREATE TYPE public.questionnaire_type AS ENUM (
    'FIXED_PERIOD',
    'OPEN_RANGES'
);

-- Name: service_application_decision_status; Type: TYPE; Schema: public

CREATE TYPE public.service_application_decision_status AS ENUM (
    'ACCEPTED',
    'REJECTED'
);

-- Name: setting_type; Type: TYPE; Schema: public

CREATE TYPE public.setting_type AS ENUM (
    'DECISION_MAKER_NAME',
    'DECISION_MAKER_TITLE'
);

-- Name: sfi_message_event_type; Type: TYPE; Schema: public

CREATE TYPE public.sfi_message_event_type AS ENUM (
    'ELECTRONIC_MESSAGE_CREATED',
    'ELECTRONIC_MESSAGE_FROM_END_USER',
    'ELECTRONIC_MESSAGE_READ',
    'PAPER_MAIL_CREATED',
    'POSTI_RECEIPT_CONFIRMED',
    'POSTI_RETURNED_TO_SENDER',
    'POSTI_UNRESOLVED',
    'RECEIPT_CONFIRMED',
    'SENT_FOR_PRINTING_AND_ENVELOPING'
);

-- Name: shift_care_type; Type: TYPE; Schema: public

CREATE TYPE public.shift_care_type AS ENUM (
    'FULL',
    'INTERMITTENT',
    'NONE'
);

-- Name: staff_attendance_type; Type: TYPE; Schema: public

CREATE TYPE public.staff_attendance_type AS ENUM (
    'PRESENT',
    'OTHER_WORK',
    'TRAINING',
    'OVERTIME',
    'JUSTIFIED_CHANGE',
    'SICKNESS',
    'CHILD_SICKNESS'
);

-- Name: system_notification_target_group; Type: TYPE; Schema: public

CREATE TYPE public.system_notification_target_group AS ENUM (
    'CITIZENS',
    'EMPLOYEES'
);

-- Name: timerange; Type: TYPE; Schema: public

CREATE TYPE public.timerange AS (
	start time without time zone,
	"end" time without time zone
);

-- Name: timerange_non_nullable_range; Type: DOMAIN; Schema: public

CREATE DOMAIN public.timerange_non_nullable_range AS public.timerange
	CONSTRAINT timerange_non_nullable_range_check CHECK (((VALUE IS NOT DISTINCT FROM NULL) OR (((VALUE).start IS NOT NULL) AND ((VALUE)."end" IS NOT NULL))));

-- Name: ui_language; Type: TYPE; Schema: public

CREATE TYPE public.ui_language AS ENUM (
    'FI',
    'SV',
    'EN'
);

-- Name: unit_language; Type: TYPE; Schema: public

CREATE TYPE public.unit_language AS ENUM (
    'fi',
    'sv'
);

-- Name: unit_provider_type; Type: TYPE; Schema: public

CREATE TYPE public.unit_provider_type AS ENUM (
    'MUNICIPAL',
    'PURCHASED',
    'PRIVATE',
    'MUNICIPAL_SCHOOL',
    'PRIVATE_SERVICE_VOUCHER',
    'EXTERNAL_PURCHASED'
);

-- Name: user_role; Type: TYPE; Schema: public

CREATE TYPE public.user_role AS ENUM (
    'ADMIN',
    'DIRECTOR',
    'REPORT_VIEWER',
    'FINANCE_ADMIN',
    'SERVICE_WORKER',
    'UNIT_SUPERVISOR',
    'STAFF',
    'SPECIAL_EDUCATION_TEACHER',
    'EARLY_CHILDHOOD_EDUCATION_SECRETARY',
    'MOBILE',
    'GROUP_STAFF',
    'MESSAGING',
    'FINANCE_STAFF'
);

-- Name: voucher_report_row_type; Type: TYPE; Schema: public

CREATE TYPE public.voucher_report_row_type AS ENUM (
    'ORIGINAL',
    'REFUND',
    'CORRECTION'
);

-- Name: voucher_value_decision_difference; Type: TYPE; Schema: public

CREATE TYPE public.voucher_value_decision_difference AS ENUM (
    'GUARDIANS',
    'INCOME',
    'FAMILY_SIZE',
    'PLACEMENT',
    'SERVICE_NEED',
    'SIBLING_DISCOUNT',
    'CO_PAYMENT',
    'FEE_ALTERATIONS',
    'FINAL_CO_PAYMENT',
    'BASE_VALUE',
    'VOUCHER_VALUE',
    'FEE_THRESHOLDS'
);

-- Name: voucher_value_decision_status; Type: TYPE; Schema: public

CREATE TYPE public.voucher_value_decision_status AS ENUM (
    'DRAFT',
    'WAITING_FOR_SENDING',
    'WAITING_FOR_MANUAL_SENDING',
    'SENT',
    'ANNULLED',
    'IGNORED'
);

-- Name: voucher_value_decision_type; Type: TYPE; Schema: public

CREATE TYPE public.voucher_value_decision_type AS ENUM (
    'NORMAL',
    'RELIEF_REJECTED',
    'RELIEF_PARTLY_ACCEPTED',
    'RELIEF_ACCEPTED'
);

-- Name: as_koski_study_right_type(public.placement_type); Type: FUNCTION; Schema: public

CREATE FUNCTION public.as_koski_study_right_type(public.placement_type) RETURNS public.koski_study_right_type
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
    RETURN (CASE $1 WHEN 'PRESCHOOL'::public.placement_type THEN 'PRESCHOOL'::text WHEN 'PRESCHOOL_DAYCARE'::public.placement_type THEN 'PRESCHOOL'::text WHEN 'PRESCHOOL_CLUB'::public.placement_type THEN 'PRESCHOOL'::text WHEN 'PREPARATORY'::public.placement_type THEN 'PREPARATORY'::text WHEN 'PREPARATORY_DAYCARE'::public.placement_type THEN 'PREPARATORY'::text ELSE NULL::text END)::public.koski_study_right_type;

-- Name: CAST (public.placement_type AS public.koski_study_right_type); Type: CAST; Schema: -

CREATE CAST (public.placement_type AS public.koski_study_right_type) WITH FUNCTION public.as_koski_study_right_type(public.placement_type);

-- Name: absence_categories(public.placement_type); Type: FUNCTION; Schema: public

CREATE FUNCTION public.absence_categories(type public.placement_type) RETURNS public.absence_category[]
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
    AS $$
SELECT CASE type
    WHEN 'CLUB' THEN '{NONBILLABLE}'
    WHEN 'SCHOOL_SHIFT_CARE' THEN '{NONBILLABLE}'
    WHEN 'PRESCHOOL' THEN '{NONBILLABLE}'
    WHEN 'PREPARATORY' THEN '{NONBILLABLE}'
    WHEN 'PRESCHOOL_DAYCARE' THEN '{NONBILLABLE, BILLABLE}'
    WHEN 'PRESCHOOL_DAYCARE_ONLY' THEN '{BILLABLE}'
    WHEN 'PRESCHOOL_CLUB' THEN '{NONBILLABLE, BILLABLE}'
    WHEN 'PREPARATORY_DAYCARE' THEN '{NONBILLABLE, BILLABLE}'
    WHEN 'PREPARATORY_DAYCARE_ONLY' THEN '{BILLABLE}'
    WHEN 'DAYCARE' THEN '{BILLABLE}'
    WHEN 'DAYCARE_PART_TIME' THEN '{BILLABLE}'
    WHEN 'DAYCARE_FIVE_YEAR_OLDS' THEN '{BILLABLE, NONBILLABLE}'
    WHEN 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS' THEN '{BILLABLE, NONBILLABLE}'
    WHEN 'TEMPORARY_DAYCARE' THEN '{}'
    WHEN 'TEMPORARY_DAYCARE_PART_DAY' THEN '{}'
    END::absence_category[]
$$;

-- Name: all_absences_in_range(daterange); Type: FUNCTION; Schema: public

CREATE FUNCTION public.all_absences_in_range(period daterange) RETURNS TABLE(child_id uuid, date date, absence_type public.absence_type, modified_at timestamp with time zone, modified_by uuid, category public.absence_category, questionnaire_id uuid)
    LANGUAGE sql STABLE
    AS $$
SELECT a.child_id,
       a.date,
       a.absence_type,
       a.modified_at,
       a.modified_by,
       a.category,
       a.questionnaire_id
FROM absence a
WHERE between_start_and_end(period, a.date)
UNION ALL
SELECT dst.child_id                       as child_id,
       d::date                            as date,
       'OTHER_ABSENCE'::absence_type      as absence_type,
       dst.updated                        as modified_at,
       null::uuid                         as modified_by,
       unnest(absence_categories(p.type)) as category,
       null::uuid                         as questionnaire_id
FROM daily_service_time as dst
         JOIN generate_series(lower(period), upper(period) - 1, '1 day') as d ON dst.validity_period @> d::date
         JOIN placement as p ON p.child_id = dst.child_id AND d BETWEEN p.start_date AND p.end_date
WHERE irregular_absence_filter(dst, d::date)
  -- Do not move the following into the filter function, as that will cause the function to not be inlined
  AND NOT EXISTS(SELECT ca.date FROM child_attendance ca WHERE ca.child_id = dst.child_id AND ca.date = d::date)
  AND NOT EXISTS(SELECT ar.date FROM attendance_reservation ar WHERE ar.child_id = dst.child_id AND ar.date = d::date)
$$;

-- Name: between_start_and_end(daterange, date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.between_start_and_end(daterange, date) RETURNS boolean
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
    AS $_$
    SELECT $2 BETWEEN coalesce(lower($1), '-infinity') AND coalesce(upper($1), 'infinity') - 1
$_$;

-- Name: between_start_and_end(tstzrange, timestamp with time zone); Type: FUNCTION; Schema: public

CREATE FUNCTION public.between_start_and_end(tstzrange, timestamp with time zone) RETURNS boolean
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
    AS $_$
SELECT ($2 > coalesce(lower($1), '-infinity') AND $2 < coalesce(upper($1), 'infinity'))
   OR (lower_inc($1) AND $2 = coalesce(lower($1), '-infinity'))
   OR (upper_inc($1) AND $2 = coalesce(upper($1), 'infinity'))
$_$;

-- Name: child_absences_in_range(uuid, daterange); Type: FUNCTION; Schema: public

CREATE FUNCTION public.child_absences_in_range(childid uuid, period daterange) RETURNS TABLE(child_id uuid, date date, absence_type public.absence_type, modified_at timestamp with time zone, modified_by uuid, category public.absence_category, questionnaire_id uuid)
    LANGUAGE sql STABLE
    AS $$
SELECT a.child_id,
       a.date,
       a.absence_type,
       a.modified_at,
       a.modified_by,
       a.category,
       a.questionnaire_id
FROM absence a
WHERE between_start_and_end(period, a.date)
  AND a.child_id = childId
UNION ALL
SELECT childId                            as child_id,
       d::date                            as date,
       'OTHER_ABSENCE'::absence_type      as absence_type,
       dst.updated                        as modified_at,
       null::uuid                         as modified_by,
       unnest(absence_categories(p.type)) as category,
       null::uuid                         as questionnaire_id
FROM daily_service_time as dst
         JOIN generate_series(lower(period), upper(period) - 1, '1 day') as d
              ON dst.validity_period @> d::date
         JOIN placement as p ON p.child_id = dst.child_id AND d BETWEEN p.start_date AND p.end_date
WHERE dst.child_id = childId
  AND irregular_absence_filter(dst, d::date)
  -- Do not move the following into the filter function, as that will cause the function to not be inlined
  AND NOT EXISTS(SELECT ca.date FROM child_attendance ca WHERE ca.child_id = dst.child_id AND ca.date = d::date)
  AND NOT EXISTS(SELECT ar.date FROM attendance_reservation ar WHERE ar.child_id = dst.child_id AND ar.date = d::date)
$$;

-- Name: child_absences_on_date(uuid, date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.child_absences_on_date(childid uuid, thedate date) RETURNS TABLE(child_id uuid, date date, absence_type public.absence_type, modified_at timestamp with time zone, modified_by uuid, category public.absence_category, questionnaire_id uuid)
    LANGUAGE sql STABLE
    AS $$
SELECT a.child_id,
       a.date,
       a.absence_type,
       a.modified_at,
       a.modified_by,
       a.category,
       a.questionnaire_id
FROM absence a
WHERE a.date = theDate
  AND a.child_id = childId
UNION ALL
SELECT dst.child_id                       as child_id,
       theDate                            as date,
       'OTHER_ABSENCE'::absence_type      as absence_type,
       dst.updated                        as modified_at,
       null::uuid                         as modified_by,
       unnest(absence_categories(p.type)) as category,
       null::uuid                         as questionnaire_id
FROM daily_service_time as dst
         JOIN placement as p ON theDate BETWEEN p.start_date AND p.end_date AND p.child_id = childId
WHERE dst.child_id = childId
  AND irregular_absence_filter(dst, theDate)
  -- Do not move the following into the filter function, as that will cause the function to not be inlined
  AND NOT EXISTS(SELECT ca.date FROM child_attendance ca WHERE ca.child_id = dst.child_id AND ca.date = theDate)
  AND NOT EXISTS(SELECT ar.date FROM attendance_reservation ar WHERE ar.child_id = dst.child_id AND ar.date = theDate)
$$;

-- Name: child_daycare_acl(date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.child_daycare_acl(today date) RETURNS TABLE(child_id uuid, daycare_id uuid, is_backup_care boolean, from_application boolean, is_assistance_needed boolean)
    LANGUAGE sql STABLE
    AS $$
    SELECT pl.child_id, pl.unit_id AS daycare_id, FALSE AS is_backup_care, FALSE as from_application, FALSE AS is_assistance_needed
    FROM placement pl
    WHERE today < pl.end_date + interval '1 month'

    UNION ALL

    SELECT a.child_id, pp.unit_id AS daycare_id, FALSE AS is_backup_care, TRUE as from_application, COALESCE((a.document -> 'careDetails' ->> 'assistanceNeeded') :: BOOLEAN, FALSE) AS is_assistance_needed
    FROM placement_plan pp
    JOIN application a ON pp.application_id = a.id
    WHERE a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])

    UNION ALL

    SELECT child_id, bc.unit_id AS daycare_id, TRUE AS is_backup_care, FALSE as from_application, FALSE AS is_assistance_needed
    FROM backup_care bc
    WHERE today < bc.end_date + INTERVAL '1 month'
$$;

-- Name: daily_service_time_for_date(date, uuid); Type: FUNCTION; Schema: public

CREATE FUNCTION public.daily_service_time_for_date(the_date date, the_child uuid) RETURNS public.timerange
    LANGUAGE sql STABLE
    AS $$
SELECT (
    CASE dst.type
        WHEN 'REGULAR'
        THEN CASE WHEN extract(dow FROM the_date) IN (1, 2, 3, 4, 5) THEN dst.regular_times END
        WHEN 'IRREGULAR'
        THEN CASE extract(dow FROM the_date)
            WHEN 0 THEN dst.sunday_times
            WHEN 1 THEN dst.monday_times
            WHEN 2 THEN dst.tuesday_times
            WHEN 3 THEN dst.wednesday_times
            WHEN 4 THEN dst.thursday_times
            WHEN 5 THEN dst.friday_times
            WHEN 6 THEN dst.saturday_times
        END
    END
)
FROM person p
LEFT JOIN daily_service_time dst ON dst.child_id = p.id AND dst.validity_period @> the_date
WHERE p.id = the_child
$$;

-- Name: days_in_range(daterange); Type: FUNCTION; Schema: public

CREATE FUNCTION public.days_in_range(daterange) RETURNS integer
    LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE
    RETURN CASE $1 WHEN 'empty'::daterange THEN 0 ELSE (CASE WHEN (upper($1) IS NULL) THEN 'infinity'::date WHEN upper_inc($1) THEN (upper($1) - 1) ELSE upper($1) END - CASE WHEN (lower($1) IS NULL) THEN '-infinity'::date WHEN lower_inc($1) THEN lower($1) ELSE (lower($1) + 1) END) END;

-- Name: employee_child_daycare_acl(date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.employee_child_daycare_acl(today date) RETURNS TABLE(employee_id uuid, child_id uuid, daycare_id uuid, role public.user_role)
    LANGUAGE sql STABLE
    AS $$
    SELECT employee_id, child_id, pl.unit_id AS daycare_id, role
    FROM placement pl
    JOIN daycare_acl ON pl.unit_id = daycare_acl.daycare_id
    WHERE today < pl.end_date + interval '1 month'

    UNION ALL

    SELECT employee_id, child_id, pp.unit_id AS daycare_id, role
    FROM placement_plan pp
    JOIN application a ON pp.application_id = a.id
    JOIN daycare_acl ON pp.unit_id = daycare_acl.daycare_id
    WHERE a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
    AND NOT (role = 'SPECIAL_EDUCATION_TEACHER' AND coalesce((a.document -> 'careDetails' ->> 'assistanceNeeded')::boolean, FALSE) IS FALSE)

    UNION ALL

    SELECT employee_id, child_id, bc.unit_id AS daycare_id,
       (CASE
            WHEN role != 'UNIT_SUPERVISOR' THEN 'STAFF'
            ELSE role
        END) AS role
    FROM backup_care bc
    JOIN daycare_acl ON bc.unit_id = daycare_acl.daycare_id
    WHERE today < bc.end_date + interval '1 month'
$$;

-- Name: employee_child_group_acl(date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.employee_child_group_acl(today date) RETURNS TABLE(employee_id uuid, child_id uuid, daycare_group_id uuid, daycare_id uuid, role public.user_role)
    LANGUAGE sql STABLE
    AS $$
    SELECT employee_id, child_id, dgp.daycare_group_id, daycare_id, daycare_acl.role
    FROM daycare_group_placement dgp
    JOIN placement dp ON dp.id = dgp.daycare_placement_id
    JOIN daycare_group_acl AS group_acl USING (daycare_group_id)
    JOIN daycare_group ON daycare_group_id = daycare_group.id
    JOIN daycare_acl USING (employee_id, daycare_id)
    WHERE daterange(dgp.start_date, dgp.end_date, '[]') @> today

    UNION ALL

    SELECT employee_id, child_id, group_id AS daycare_group_id, daycare_id, daycare_acl.role
    FROM backup_care bc
    JOIN daycare_group_acl AS group_acl ON bc.group_id = daycare_group_id
    JOIN daycare_group ON daycare_group_id = daycare_group.id
    JOIN daycare_acl USING (employee_id, daycare_id)
    WHERE bc.end_date > today - interval '1 month'
$$;

-- Name: ensure_decision_number_curr_year(); Type: FUNCTION; Schema: public

CREATE FUNCTION public.ensure_decision_number_curr_year() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    min_seq integer;
BEGIN
    -- Ensure current sequence value is used for number
    IF (NEW.number <> currval('decision_number_seq')) THEN
        RAISE EXCEPTION 'Column number is not set using sequence'
            USING HINT = 'Please set number either with DEFAULT value or calling nextval(''decision_number_seq'')';
    END IF;

    -- Minimum sequence number for current year
    min_seq := EXTRACT(YEAR FROM now())::integer * 100000;
    -- Update sequence if the year has changed
    IF (currval('decision_number_seq') <= min_seq) THEN
        PERFORM setval('decision_number_seq', min_seq);
        NEW.number := nextval('decision_number_seq');
    END IF;
    RETURN NEW;
END;
$$;

-- Name: daily_service_time; Type: TABLE; Schema: public

CREATE TABLE public.daily_service_time (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL,
    regular_start text,
    regular_end text,
    type public.daily_service_time_type NOT NULL,
    validity_period daterange NOT NULL,
    regular_times public.timerange_non_nullable_range,
    monday_times public.timerange_non_nullable_range,
    tuesday_times public.timerange_non_nullable_range,
    wednesday_times public.timerange_non_nullable_range,
    thursday_times public.timerange_non_nullable_range,
    friday_times public.timerange_non_nullable_range,
    saturday_times public.timerange_non_nullable_range,
    sunday_times public.timerange_non_nullable_range,
    CONSTRAINT "check$regular_daily_service_times" CHECK (((type <> 'REGULAR'::public.daily_service_time_type) OR ((regular_times IS NOT NULL) AND (monday_times IS NULL) AND (tuesday_times IS NULL) AND (wednesday_times IS NULL) AND (thursday_times IS NULL) AND (friday_times IS NULL) AND (saturday_times IS NULL) AND (sunday_times IS NULL)))),
    CONSTRAINT "check$variable_daily_service_times" CHECK (((type <> 'VARIABLE_TIME'::public.daily_service_time_type) OR ((regular_times IS NULL) AND (monday_times IS NULL) AND (tuesday_times IS NULL) AND (wednesday_times IS NULL) AND (thursday_times IS NULL) AND (friday_times IS NULL) AND (saturday_times IS NULL) AND (sunday_times IS NULL))))
);

-- Name: irregular_absence_filter(public.daily_service_time, date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.irregular_absence_filter(dst public.daily_service_time, thedate date) RETURNS boolean
    LANGUAGE sql STABLE
    AS $$
SELECT dst.type = 'IRREGULAR'
           AND (dst.sunday_times IS NOT NULL OR dst.monday_times IS NOT NULL OR dst.tuesday_times IS NOT NULL OR
                dst.wednesday_times IS NOT NULL OR dst.thursday_times IS NOT NULL OR dst.friday_times IS NOT NULL OR
                dst.saturday_times IS NOT NULL)
           AND CASE extract(dow FROM theDate)
                   WHEN 0 THEN dst.sunday_times IS NULL
                   WHEN 1 THEN dst.monday_times IS NULL
                   WHEN 2 THEN dst.tuesday_times IS NULL
                   WHEN 3 THEN dst.wednesday_times IS NULL
                   WHEN 4 THEN dst.thursday_times IS NULL
                   WHEN 5 THEN dst.friday_times IS NULL
                   WHEN 6 THEN dst.saturday_times IS NULL
           END
$$;

-- Name: unaccent; Type: TEXT SEARCH CONFIGURATION; Schema: public

CREATE TEXT SEARCH CONFIGURATION public.unaccent (
    PARSER = pg_catalog."default" );

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR asciiword WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR word WITH public.unaccent, simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR numword WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR email WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR url WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR host WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR sfloat WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR version WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR hword_numpart WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR hword_part WITH public.unaccent, simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR hword_asciipart WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR numhword WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR asciihword WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR hword WITH public.unaccent, simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR url_path WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR file WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR "float" WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR "int" WITH simple;

ALTER TEXT SEARCH CONFIGURATION public.unaccent
    ADD MAPPING FOR uint WITH simple;

-- Name: person; Type: TABLE; Schema: public

CREATE TABLE public.person (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    social_security_number text,
    first_name text DEFAULT ''::text NOT NULL,
    last_name text DEFAULT ''::text NOT NULL,
    email text,
    aad_object_id uuid,
    language text,
    date_of_birth date NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    street_address text DEFAULT ''::character varying NOT NULL,
    postal_code text DEFAULT ''::character varying NOT NULL,
    post_office text DEFAULT ''::character varying NOT NULL,
    nationalities character varying(3)[] DEFAULT '{}'::character varying[] NOT NULL,
    restricted_details_enabled boolean DEFAULT false,
    restricted_details_end_date date,
    phone text DEFAULT ''::text NOT NULL,
    updated_from_vtj timestamp with time zone,
    invoicing_street_address text DEFAULT ''::text NOT NULL,
    invoicing_postal_code text DEFAULT ''::text NOT NULL,
    invoicing_post_office text DEFAULT ''::text NOT NULL,
    invoice_recipient_name text DEFAULT ''::text NOT NULL,
    date_of_death date,
    residence_code text DEFAULT ''::text NOT NULL,
    force_manual_fee_decisions boolean DEFAULT false NOT NULL,
    backup_phone text DEFAULT ''::text NOT NULL,
    last_login timestamp with time zone,
    freetext_vec tsvector GENERATED ALWAYS AS ((((to_tsvector('public.unaccent'::regconfig, COALESCE(first_name, ''::text)) || to_tsvector('public.unaccent'::regconfig, COALESCE(last_name, ''::text))) || to_tsvector('public.unaccent'::regconfig, street_address)) || to_tsvector('public.unaccent'::regconfig, postal_code))) STORED,
    oph_person_oid text,
    vtj_guardians_queried timestamp with time zone,
    vtj_dependants_queried timestamp with time zone,
    ssn_adding_disabled boolean DEFAULT false NOT NULL,
    preferred_name text DEFAULT ''::text NOT NULL,
    duplicate_of uuid,
    keycloak_email text,
    disabled_email_types public.email_message_type[] DEFAULT '{}'::public.email_message_type[] NOT NULL,
    municipality_of_residence text DEFAULT ''::text NOT NULL,
    verified_email text,
    CONSTRAINT person_disabled_ssn_no_ssn CHECK (((NOT ssn_adding_disabled) OR (social_security_number IS NULL))),
    CONSTRAINT ssn_require_vtj_update CHECK (((social_security_number IS NULL) OR (updated_from_vtj IS NOT NULL)))
);

-- Name: placement; Type: TABLE; Schema: public

CREATE TABLE public.placement (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone CONSTRAINT placement_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT placement_updated_not_null NOT NULL,
    type public.placement_type NOT NULL,
    child_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    termination_requested_date date,
    terminated_by uuid,
    place_guarantee boolean NOT NULL,
    modified_at timestamp with time zone,
    modified_by uuid,
    created_by uuid,
    source public.placement_source,
    source_application_id uuid,
    source_service_application_id uuid,
    CONSTRAINT "check$source_application_ref" CHECK (((source <> 'APPLICATION'::public.placement_source) OR (source_application_id IS NOT NULL))),
    CONSTRAINT "check$source_service_application_ref" CHECK (((source <> 'SERVICE_APPLICATION'::public.placement_source) OR (source_service_application_id IS NOT NULL))),
    CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);

-- Name: koski_placement(date, date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.koski_placement(today date, sync_range_start date) RETURNS TABLE(child_id uuid, unit_id uuid, type public.koski_study_right_type, placements datemultirange, all_placements_in_past boolean, last_of_child boolean, last_of_type boolean)
    LANGUAGE sql STABLE PARALLEL SAFE
    BEGIN ATOMIC
 SELECT p.child_id,
     p.unit_id,
     p.type,
     range_agg((daterange(p.start_date, p.end_date, '[]'::text) * daterange(koski_placement.sync_range_start, NULL::date, '[]'::text))) AS placements,
     (max(p.end_date) < koski_placement.today) AS all_placements_in_past,
     bool_or(p.last_of_child) AS last_of_child,
     bool_or(p.last_of_type) AS last_of_type
    FROM ( SELECT placement.child_id,
             placement.unit_id,
             placement.start_date,
             placement.end_date,
             (row_number() OVER child = count(*) OVER child) AS last_of_child,
             (row_number() OVER child_type = count(*) OVER child_type) AS last_of_type,
             (placement.type)::public.koski_study_right_type AS type
            FROM public.placement
           WHERE ((placement.start_date <= koski_placement.today) AND (placement.type = ANY (ARRAY['PRESCHOOL'::public.placement_type, 'PRESCHOOL_DAYCARE'::public.placement_type, 'PRESCHOOL_CLUB'::public.placement_type, 'PREPARATORY'::public.placement_type, 'PREPARATORY_DAYCARE'::public.placement_type])) AND ((koski_placement.sync_range_start IS NULL) OR (koski_placement.sync_range_start <= placement.end_date)))
           WINDOW child AS (PARTITION BY placement.child_id ORDER BY placement.start_date ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING), child_type AS (PARTITION BY placement.child_id, ((placement.type)::public.koski_study_right_type) ORDER BY placement.start_date ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)) p
   WHERE (NOT (EXISTS ( SELECT
            FROM public.person duplicate
           WHERE (duplicate.duplicate_of = p.child_id))))
   GROUP BY p.child_id, p.unit_id, p.type;
END;

-- Name: absence; Type: TABLE; Schema: public

CREATE TABLE public.absence (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    child_id uuid NOT NULL,
    date date NOT NULL,
    absence_type public.absence_type NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    category public.absence_category NOT NULL,
    questionnaire_id uuid
);

-- Name: daycare; Type: TABLE; Schema: public

CREATE TABLE public.daycare (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    name text NOT NULL,
    type public.care_types[] DEFAULT '{CENTRE}'::public.care_types[] NOT NULL,
    care_area_id uuid NOT NULL,
    phone text,
    url text,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    backup_location text,
    opening_date date,
    closing_date date,
    email text,
    schedule text,
    additional_info text,
    cost_center text,
    upload_to_varda boolean DEFAULT false NOT NULL,
    capacity integer DEFAULT 0 NOT NULL,
    decision_daycare_name text DEFAULT ''::text NOT NULL,
    decision_preschool_name text DEFAULT ''::text NOT NULL,
    decision_handler text DEFAULT ''::text NOT NULL,
    decision_handler_address text DEFAULT ''::text NOT NULL,
    street_address text DEFAULT ''::text NOT NULL,
    postal_code text DEFAULT ''::text NOT NULL,
    post_office text DEFAULT ''::text NOT NULL,
    mailing_po_box text,
    location point,
    mailing_street_address text,
    mailing_postal_code text,
    mailing_post_office text,
    invoiced_by_municipality boolean DEFAULT true NOT NULL,
    provider_type public.unit_provider_type DEFAULT 'MUNICIPAL'::public.unit_provider_type NOT NULL,
    language public.unit_language DEFAULT 'fi'::public.unit_language NOT NULL,
    upload_to_koski boolean DEFAULT false NOT NULL,
    oph_unit_oid text,
    oph_organizer_oid text,
    ghost_unit boolean,
    daycare_apply_period daterange,
    preschool_apply_period daterange,
    club_apply_period daterange,
    finance_decision_handler uuid,
    enabled_pilot_features public.pilot_feature[] DEFAULT '{}'::public.pilot_feature[] NOT NULL,
    upload_children_to_varda boolean DEFAULT false NOT NULL,
    business_id text DEFAULT ''::text NOT NULL,
    iban text DEFAULT ''::text NOT NULL,
    provider_id text DEFAULT ''::text NOT NULL,
    operation_times public.timerange_non_nullable_range[] DEFAULT '{NULL,NULL,NULL,NULL,NULL,NULL,NULL}'::public.timerange_non_nullable_range[] NOT NULL,
    operation_days integer[] GENERATED ALWAYS AS (array_remove(ARRAY[
CASE
    WHEN (operation_times[1] IS NOT NULL) THEN 1
    ELSE NULL::integer
END,
CASE
    WHEN (operation_times[2] IS NOT NULL) THEN 2
    ELSE NULL::integer
END,
CASE
    WHEN (operation_times[3] IS NOT NULL) THEN 3
    ELSE NULL::integer
END,
CASE
    WHEN (operation_times[4] IS NOT NULL) THEN 4
    ELSE NULL::integer
END,
CASE
    WHEN (operation_times[5] IS NOT NULL) THEN 5
    ELSE NULL::integer
END,
CASE
    WHEN (operation_times[6] IS NOT NULL) THEN 6
    ELSE NULL::integer
END,
CASE
    WHEN (operation_times[7] IS NOT NULL) THEN 7
    ELSE NULL::integer
END], NULL::integer)) STORED,
    unit_manager_name text DEFAULT ''::text NOT NULL,
    unit_manager_phone text DEFAULT ''::text NOT NULL,
    unit_manager_email text DEFAULT ''::text NOT NULL,
    dw_cost_center text,
    daily_preschool_time public.timerange_non_nullable_range,
    daily_preparatory_time public.timerange_non_nullable_range,
    mealtime_breakfast public.timerange_non_nullable_range,
    mealtime_lunch public.timerange_non_nullable_range,
    mealtime_snack public.timerange_non_nullable_range,
    mealtime_supper public.timerange_non_nullable_range,
    mealtime_evening_snack public.timerange_non_nullable_range,
    shift_care_operation_times public.timerange_non_nullable_range[] DEFAULT '{NULL,NULL,NULL,NULL,NULL,NULL,NULL}'::public.timerange_non_nullable_range[],
    shift_care_operation_days integer[] GENERATED ALWAYS AS (
CASE
    WHEN (shift_care_operation_times IS NOT NULL) THEN array_remove(ARRAY[
    CASE
        WHEN (shift_care_operation_times[1] IS NOT NULL) THEN 1
        ELSE NULL::integer
    END,
    CASE
        WHEN (shift_care_operation_times[2] IS NOT NULL) THEN 2
        ELSE NULL::integer
    END,
    CASE
        WHEN (shift_care_operation_times[3] IS NOT NULL) THEN 3
        ELSE NULL::integer
    END,
    CASE
        WHEN (shift_care_operation_times[4] IS NOT NULL) THEN 4
        ELSE NULL::integer
    END,
    CASE
        WHEN (shift_care_operation_times[5] IS NOT NULL) THEN 5
        ELSE NULL::integer
    END,
    CASE
        WHEN (shift_care_operation_times[6] IS NOT NULL) THEN 6
        ELSE NULL::integer
    END,
    CASE
        WHEN (shift_care_operation_times[7] IS NOT NULL) THEN 7
        ELSE NULL::integer
    END], NULL::integer)
    ELSE NULL::integer[]
END) STORED,
    provides_shift_care boolean GENERATED ALWAYS AS ((shift_care_operation_times IS NOT NULL)) STORED,
    shift_care_open_on_holidays boolean DEFAULT false NOT NULL,
    with_school boolean DEFAULT false NOT NULL,
    preschool_manager_name text DEFAULT ''::text NOT NULL,
    preschool_manager_phone text DEFAULT ''::text NOT NULL,
    preschool_manager_email text DEFAULT ''::text NOT NULL,
    service_worker_note text DEFAULT ''::text NOT NULL,
    partner_code text DEFAULT ''::text NOT NULL,
    nekku_order_reduction_percentage integer DEFAULT 10 NOT NULL,
    nekku_no_weekend_meal_orders boolean DEFAULT false NOT NULL,
    CONSTRAINT "check$full_week_operation_times" CHECK ((cardinality(operation_times) = 7)),
    CONSTRAINT "check$full_week_shift_care_operation_times" CHECK (((shift_care_operation_times IS NULL) OR (cardinality(shift_care_operation_times) = 7))),
    CONSTRAINT "check$shift_care_open_on_holidays_only_for_shift_care" CHECK (((provides_shift_care = true) OR (shift_care_open_on_holidays = false))),
    CONSTRAINT preparatory_time_existence CHECK (((daily_preparatory_time IS NOT NULL) OR (NOT ('PREPARATORY_EDUCATION'::public.care_types = ANY (type))))),
    CONSTRAINT preschool_time_existence CHECK (((daily_preschool_time IS NOT NULL) OR (NOT ('PRESCHOOL'::public.care_types = ANY (type)))))
);

-- Name: koski_child; Type: VIEW; Schema: public

CREATE VIEW public.koski_child AS
 SELECT id,
    NULLIF(social_security_number, ''::text) AS ssn,
    NULLIF(oph_person_oid, ''::text) AS oph_person_oid,
    first_name,
    last_name
   FROM public.person
  WHERE ((NULLIF(social_security_number, ''::text) IS NOT NULL) OR (NULLIF(oph_person_oid, ''::text) IS NOT NULL));

-- Name: koski_unit; Type: VIEW; Schema: public

CREATE VIEW public.koski_unit AS
 SELECT id,
    language AS unit_language,
    provider_type,
    unit_manager_name AS approver_name,
    NULLIF(oph_unit_oid, ''::text) AS oph_unit_oid,
    NULLIF(oph_organizer_oid, ''::text) AS oph_organizer_oid
   FROM public.daycare
  WHERE ((upload_to_koski IS TRUE) AND (NULLIF(oph_unit_oid, ''::text) IS NOT NULL) AND (NULLIF(oph_organizer_oid, ''::text) IS NOT NULL));

-- Name: koski_active_preparatory_study_right(date, date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.koski_active_preparatory_study_right(today date, sync_range_start date) RETURNS TABLE(child_id uuid, unit_id uuid, type public.koski_study_right_type, input_data public.koski_preparatory_input_data)
    LANGUAGE sql STABLE PARALLEL SAFE
    BEGIN ATOMIC
 SELECT p.child_id,
     p.unit_id,
     p.type,
     ROW(d.oph_unit_oid, d.oph_organizer_oid, p.placements, p.all_placements_in_past, p.last_of_child, p.last_of_type, COALESCE(pa.absences, '{}'::jsonb)) AS input_data
    FROM ((public.koski_placement(koski_active_preparatory_study_right.today, koski_active_preparatory_study_right.sync_range_start) p(child_id, unit_id, type, placements, all_placements_in_past, last_of_child, last_of_type)
      JOIN public.koski_unit d ON ((p.unit_id = d.id)))
      JOIN LATERAL ( SELECT jsonb_object_agg(grouped.absence_type, grouped.dates) AS absences
            FROM ( SELECT a.absence_type,
                     array_agg(a.date ORDER BY a.date) AS dates
                    FROM public.absence a
                   WHERE ((a.child_id = p.child_id) AND (a.category = 'NONBILLABLE'::public.absence_category) AND public.between_start_and_end(range_merge(p.placements), a.date) AND (a.date > '2020-08-01'::date))
                   GROUP BY a.absence_type) grouped) pa ON (true))
   WHERE ((p.type = 'PREPARATORY'::public.koski_study_right_type) AND (EXISTS ( SELECT
            FROM public.koski_child
           WHERE (koski_child.id = p.child_id))));
END;

-- Name: other_assistance_measure; Type: TABLE; Schema: public

CREATE TABLE public.other_assistance_measure (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL,
    modified timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    valid_during daterange NOT NULL,
    type public.other_assistance_measure_type NOT NULL,
    CONSTRAINT "check$range_valid" CHECK ((NOT (lower_inf(valid_during) OR upper_inf(valid_during))))
);

-- Name: preschool_assistance; Type: TABLE; Schema: public

CREATE TABLE public.preschool_assistance (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL,
    modified timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    valid_during daterange NOT NULL,
    level public.preschool_assistance_level NOT NULL,
    CONSTRAINT "check$range_valid" CHECK ((NOT (lower_inf(valid_during) OR upper_inf(valid_during))))
);

-- Name: koski_active_preschool_study_right(date, date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.koski_active_preschool_study_right(today date, sync_range_start date) RETURNS TABLE(child_id uuid, unit_id uuid, type public.koski_study_right_type, input_data public.koski_preschool_input_data)
    LANGUAGE sql STABLE PARALLEL SAFE
    BEGIN ATOMIC
 SELECT p.child_id,
     p.unit_id,
     p.type,
     ROW(d.oph_unit_oid, d.oph_organizer_oid, p.placements, p.all_placements_in_past, p.last_of_child, COALESCE(pras.special_support, '{}'::datemultirange), COALESCE(pras.special_support_with_decision_level_1, '{}'::datemultirange), COALESCE(pras.special_support_with_decision_level_2, '{}'::datemultirange), COALESCE(oam.transport_benefit, '{}'::datemultirange), COALESCE(pras.child_support, '{}'::datemultirange), COALESCE(pras.child_support_and_extended_compulsory_education, '{}'::datemultirange), COALESCE(pras.child_support_and_old_extended_compulsory_education, '{}'::datemultirange), COALESCE(pras.child_support_2_and_old_extended_compulsory_education, '{}'::datemultirange)) AS input_data
    FROM (((public.koski_placement(koski_active_preschool_study_right.today, koski_active_preschool_study_right.sync_range_start) p(child_id, unit_id, type, placements, all_placements_in_past, last_of_child, last_of_type)
      JOIN public.koski_unit d ON ((p.unit_id = d.id)))
      JOIN LATERAL ( SELECT range_agg(oam_1.valid_during) AS transport_benefit
            FROM public.other_assistance_measure oam_1
           WHERE ((oam_1.child_id = p.child_id) AND (oam_1.valid_during && range_merge(p.placements)) AND (oam_1.type = 'TRANSPORT_BENEFIT'::public.other_assistance_measure_type))) oam ON (true))
      JOIN LATERAL ( SELECT range_agg(pa.valid_during) FILTER (WHERE (pa.level = 'SPECIAL_SUPPORT'::public.preschool_assistance_level)) AS special_support,
             range_agg(pa.valid_during) FILTER (WHERE (pa.level = 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1'::public.preschool_assistance_level)) AS special_support_with_decision_level_1,
             range_agg(pa.valid_during) FILTER (WHERE (pa.level = 'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2'::public.preschool_assistance_level)) AS special_support_with_decision_level_2,
             range_agg(pa.valid_during) FILTER (WHERE (pa.level = 'CHILD_SUPPORT'::public.preschool_assistance_level)) AS child_support,
             range_agg(pa.valid_during) FILTER (WHERE (pa.level = 'CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION'::public.preschool_assistance_level)) AS child_support_and_extended_compulsory_education,
             range_agg(pa.valid_during) FILTER (WHERE (pa.level = 'CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION'::public.preschool_assistance_level)) AS child_support_and_old_extended_compulsory_education,
             range_agg(pa.valid_during) FILTER (WHERE (pa.level = 'CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION'::public.preschool_assistance_level)) AS child_support_2_and_old_extended_compulsory_education
            FROM public.preschool_assistance pa
           WHERE ((pa.child_id = p.child_id) AND (pa.valid_during && range_merge(p.placements)))) pras ON (true))
   WHERE ((p.type = 'PRESCHOOL'::public.koski_study_right_type) AND (EXISTS ( SELECT
            FROM public.koski_child
           WHERE (koski_child.id = p.child_id))));
END;

-- Name: koski_study_right; Type: TABLE; Schema: public

CREATE TABLE public.koski_study_right (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    type public.koski_study_right_type NOT NULL,
    payload jsonb NOT NULL,
    version integer NOT NULL,
    study_right_oid text,
    person_oid text,
    void_date date,
    data_version integer CONSTRAINT koski_study_right_input_data_version_not_null NOT NULL,
    preschool_input_data public.koski_preschool_input_data,
    preparatory_input_data public.koski_preparatory_input_data,
    CONSTRAINT "check$input_data_type" CHECK (
CASE type
    WHEN 'PRESCHOOL'::public.koski_study_right_type THEN (preparatory_input_data IS NULL)
    WHEN 'PREPARATORY'::public.koski_study_right_type THEN (preschool_input_data IS NULL)
    ELSE NULL::boolean
END)
);

-- Name: koski_voided_study_right(date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.koski_voided_study_right(today date) RETURNS TABLE(child_id uuid, unit_id uuid, type public.koski_study_right_type, oph_unit_oid text, oph_organizer_oid text, void_date date)
    LANGUAGE sql STABLE PARALLEL SAFE
    BEGIN ATOMIC
 SELECT ksr.child_id,
     ksr.unit_id,
     ksr.type,
     d.oph_unit_oid,
     d.oph_organizer_oid,
     ksr.void_date
    FROM ((public.koski_study_right ksr
      JOIN public.koski_unit d ON ((ksr.unit_id = d.id)))
      JOIN public.person pr ON ((ksr.child_id = pr.id)))
   WHERE ((EXISTS ( SELECT
            FROM public.koski_child
           WHERE (koski_child.id = ksr.child_id))) AND (NOT (EXISTS ( SELECT 1
            FROM public.koski_placement(koski_voided_study_right.today, NULL::date) kp(child_id, unit_id, type, placements, all_placements_in_past, last_of_child, last_of_type)
           WHERE ((kp.child_id = ksr.child_id) AND (kp.unit_id = ksr.unit_id) AND (kp.type = ksr.type))))) AND (ksr.study_right_oid IS NOT NULL));
END;

-- Name: realized_placement_all(date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.realized_placement_all(today date) RETURNS TABLE(child_id uuid, unit_id uuid, is_backup boolean, placement_id uuid, placement_type public.placement_type, group_id uuid, backup_care_id uuid)
    LANGUAGE sql STABLE
    AS $$
    SELECT
        child_id, unit_id, FALSE AS is_backup,
        p.id AS placement_id, p.type AS placement_type,
        dgp.daycare_group_id AS group_id, NULL AS backup_care_id
    FROM placement p
    LEFT JOIN daycare_group_placement dgp
    ON p.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> today
    WHERE daterange(p.start_date, p.end_date, '[]') @> today

    UNION ALL

    SELECT
        child_id, bc.unit_id, TRUE AS is_backup,
        p.id AS placement_id, p.type AS placement_type,
        bc.group_id, bc.id AS backup_care_id
    FROM backup_care bc
    JOIN placement p USING (child_id)
    WHERE daterange(bc.start_date, bc.end_date, '[]') @> today
    AND daterange(p.start_date, p.end_date, '[]') @> today
$$;

-- Name: realized_placement_one(date); Type: FUNCTION; Schema: public

CREATE FUNCTION public.realized_placement_one(today date) RETURNS TABLE(child_id uuid, unit_id uuid, is_backup boolean, placement_id uuid, placement_type public.placement_type, group_id uuid, backup_care_id uuid, placement_unit_id uuid, placement_group_id uuid)
    LANGUAGE sql STABLE
    AS $$
    SELECT
        p.child_id, (CASE WHEN bc.id IS NULL THEN p.unit_id ELSE bc.unit_id END) AS unit_id, (bc.id IS NOT NULL) AS is_backup,
        p.id AS placement_id, p.type AS placement_type,
        (CASE WHEN bc.id IS NULL THEN dgp.daycare_group_id ELSE bc.group_id END) AS group_id, bc.id AS backup_care_id,
        p.unit_id AS placement_unit_id, dgp.daycare_group_id AS placement_group_id
    FROM placement p
    LEFT JOIN backup_care bc
    ON p.child_id = bc.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> today
    LEFT JOIN daycare_group_placement dgp
    ON p.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> today
    WHERE daterange(p.start_date, p.end_date, '[]') @> today
$$;

-- Name: trigger_guardian_blocklist_check_guardian(); Type: FUNCTION; Schema: public

CREATE FUNCTION public.trigger_guardian_blocklist_check_guardian() RETURNS trigger
    LANGUAGE plpgsql STABLE PARALLEL SAFE
    AS $$
BEGIN
    IF EXISTS (SELECT FROM guardian WHERE guardian_id = NEW.guardian_id AND child_id = NEW.child_id) THEN
        RAISE EXCEPTION 'Blocklist row conflicts with guardian (%, %)', NEW.guardian_id, NEW.child_id;
    END IF;
    RETURN NEW;
END
$$;

-- Name: trigger_guardian_check_guardian_blocklist(); Type: FUNCTION; Schema: public

CREATE FUNCTION public.trigger_guardian_check_guardian_blocklist() RETURNS trigger
    LANGUAGE plpgsql STABLE PARALLEL SAFE
    AS $$
BEGIN
    IF EXISTS (SELECT FROM guardian_blocklist WHERE guardian_id = NEW.guardian_id AND child_id = NEW.child_id) THEN
        RAISE EXCEPTION 'Guardian conflicts with blocklist row (%, %)', NEW.guardian_id, NEW.child_id;
    END IF;
    RETURN NEW;
END
$$;

-- Name: trigger_prevent_unarchived_document_deletion(); Type: FUNCTION; Schema: public

CREATE FUNCTION public.trigger_prevent_unarchived_document_deletion() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Check if the document template requires external archiving,
    -- the document hasn't been archived yet, and the status is not DRAFT
    IF EXISTS (
        SELECT 1 
        FROM document_template dt 
        WHERE dt.id = OLD.template_id 
        AND dt.archive_externally = true 
        AND OLD.archived_at IS NULL
        AND OLD.status != 'DRAFT'
    ) THEN
        RAISE EXCEPTION 'Cannot delete child document (id: %) - document must be archived before deletion (template requires external archiving and document status is %)', OLD.id, OLD.status;
    END IF;
    RETURN OLD;
END
$$;

-- Name: trigger_refresh_updated(); Type: FUNCTION; Schema: public

CREATE FUNCTION public.trigger_refresh_updated() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated = NOW();
    RETURN NEW;
END;
$$;

-- Name: trigger_refresh_updated_at(); Type: FUNCTION; Schema: public

CREATE FUNCTION public.trigger_refresh_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

-- Name: range_merge(anyrange); Type: AGGREGATE; Schema: public

CREATE AGGREGATE public.range_merge(anyrange) (
    SFUNC = pg_catalog.range_merge,
    STYPE = anyrange
);

-- Name: absence_application; Type: TABLE; Schema: public

CREATE TABLE public.absence_application (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    child_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    description text NOT NULL,
    status public.absence_application_status NOT NULL,
    decided_at timestamp with time zone,
    decided_by uuid,
    rejected_reason text,
    CONSTRAINT "check$decided_valid" CHECK ((((status = 'WAITING_DECISION'::public.absence_application_status) AND (decided_at IS NULL) AND (decided_by IS NULL)) OR ((status <> 'WAITING_DECISION'::public.absence_application_status) AND (decided_at IS NOT NULL) AND (decided_by IS NOT NULL)))),
    CONSTRAINT "check$rejected_reason_valid" CHECK (((status = 'REJECTED'::public.absence_application_status) = (rejected_reason IS NOT NULL))),
    CONSTRAINT "check$start_date_before_end_date" CHECK ((start_date <= end_date))
);

-- Name: application; Type: TABLE; Schema: public

CREATE TABLE public.application (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT application_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT application_updated_not_null NOT NULL,
    sentdate date,
    duedate date,
    guardian_id uuid NOT NULL,
    child_id uuid NOT NULL,
    checkedbyadmin boolean DEFAULT false NOT NULL,
    hidefromguardian boolean DEFAULT false NOT NULL,
    transferapplication boolean DEFAULT false NOT NULL,
    additionaldaycareapplication boolean DEFAULT false NOT NULL,
    status public.application_status_type NOT NULL,
    origin public.application_origin_type NOT NULL,
    duedate_set_manually_at timestamp with time zone,
    service_worker_note text DEFAULT ''::text NOT NULL,
    type public.application_type NOT NULL,
    allow_other_guardian_access boolean NOT NULL,
    document jsonb NOT NULL,
    modified_at timestamp with time zone CONSTRAINT application_form_modified_not_null NOT NULL,
    process_id uuid,
    created_by uuid NOT NULL,
    status_modified_at timestamp with time zone,
    status_modified_by uuid,
    confidential boolean,
    modified_by uuid NOT NULL,
    primary_preferred_unit uuid GENERATED ALWAYS AS (((((document -> 'apply'::text) -> 'preferredUnits'::text) ->> 0))::uuid) STORED,
    senttime time without time zone,
    CONSTRAINT check_confidentiality CHECK (
CASE
    WHEN (status = ANY (ARRAY['CREATED'::public.application_status_type, 'SENT'::public.application_status_type])) THEN true
    WHEN (status = 'WAITING_PLACEMENT'::public.application_status_type) THEN ((checkedbyadmin = false) OR (confidential IS NOT NULL))
    ELSE (confidential IS NOT NULL)
END)
);

-- Name: application_note; Type: TABLE; Schema: public

CREATE TABLE public.application_note (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    application_id uuid NOT NULL,
    content text NOT NULL,
    created_by uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT application_note_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT application_note_updated_not_null NOT NULL,
    modified_by uuid CONSTRAINT application_note_updated_by_not_null NOT NULL,
    message_content_id uuid,
    modified_at timestamp with time zone NOT NULL
);

-- Name: application_other_guardian; Type: TABLE; Schema: public

CREATE TABLE public.application_other_guardian (
    application_id uuid NOT NULL,
    guardian_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT application_other_guardian_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT application_other_guardian_updated_not_null NOT NULL
);

-- Name: decision; Type: TABLE; Schema: public

CREATE TABLE public.decision (
    id uuid DEFAULT ext.uuid_generate_v1mc() CONSTRAINT decision2_id_not_null NOT NULL,
    number integer CONSTRAINT decision2_number_not_null NOT NULL,
    created timestamp with time zone DEFAULT now(),
    updated timestamp with time zone DEFAULT now(),
    created_by uuid CONSTRAINT decision2_created_by_not_null NOT NULL,
    sent_date date,
    unit_id uuid CONSTRAINT decision2_unit_id_not_null NOT NULL,
    application_id uuid CONSTRAINT decision2_application_id_not_null NOT NULL,
    type public.decision_type CONSTRAINT decision2_type_not_null NOT NULL,
    start_date date CONSTRAINT decision2_start_date_not_null NOT NULL,
    end_date date CONSTRAINT decision2_end_date_not_null NOT NULL,
    status public.decision_status DEFAULT 'PENDING'::public.decision_status CONSTRAINT decision2_status_not_null NOT NULL,
    requested_start_date date,
    resolved timestamp with time zone,
    resolved_by uuid,
    planned boolean DEFAULT true CONSTRAINT decision2_planned_not_null NOT NULL,
    pending_decision_emails_sent_count integer DEFAULT 0,
    pending_decision_email_sent timestamp with time zone,
    document_key text,
    other_guardian_document_key text,
    document_contains_contact_info boolean DEFAULT false NOT NULL,
    archived_at timestamp with time zone,
    sent_time time without time zone
);

-- Name: placement_plan; Type: TABLE; Schema: public

CREATE TABLE public.placement_plan (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now(),
    updated timestamp with time zone DEFAULT now(),
    type public.placement_type NOT NULL,
    unit_id uuid NOT NULL,
    application_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    preschool_daycare_start_date date,
    preschool_daycare_end_date date,
    deleted boolean DEFAULT false NOT NULL,
    unit_confirmation_status public.confirmation_status DEFAULT 'PENDING'::public.confirmation_status NOT NULL,
    unit_reject_reason public.placement_reject_reason,
    unit_reject_other_reason text,
    CONSTRAINT preschool_daycare_not_null CHECK (((type <> 'PRESCHOOL_DAYCARE'::public.placement_type) OR ((preschool_daycare_start_date IS NOT NULL) AND (preschool_daycare_end_date IS NOT NULL)))),
    CONSTRAINT preschool_daycare_start_before_end CHECK ((preschool_daycare_start_date <= preschool_daycare_end_date)),
    CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);

-- Name: application_view; Type: VIEW; Schema: public

CREATE VIEW public.application_view AS
 SELECT id,
    document,
    docversion,
    created_at,
    formmodified,
    sentdate,
    duedate,
    status,
    type,
    urgent,
    preferredstartdate,
    startdate,
    ((childlastname || ' '::text) || childfirstname) AS childname,
    childfirstname,
    childlastname,
    childssn,
    childstreetaddr,
    childpostalcode,
    preferredunit,
    preferredunits,
    allergytype,
    diettype,
    otherinfo,
    daycareassistanceneeded,
    siblingbasis,
    childid,
    guardianid,
    (term)::uuid AS term,
    wasondaycare,
    wasonclubcare,
    clubcareassistanceneeded,
    origin,
    extendedcare,
    placementdaycareunit,
    connecteddaycare,
    serviceneedoption,
    preparatoryeducation,
    checkedbyadmin,
    guardianphonenumber,
    hidefromguardian,
    transferapplication,
    additionaldaycareapplication,
    duplicateapplicationids,
    otherguardianagreementstatus
   FROM ( WITH dup_appl AS (
                 SELECT l.id,
                    array_agg(r.id) AS duplicate_application_ids
                   FROM public.application l,
                    public.application r
                  WHERE ((l.child_id = r.child_id) AND (l.id <> r.id) AND (l.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::public.application_status_type[])) AND (r.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::public.application_status_type[])))
                  GROUP BY l.id
                )
         SELECT appl.id,
            appl.document,
            (appl.document -> 'docVersion'::text) AS docversion,
            appl.modified_at AS formmodified,
            appl.created_at,
            appl.sentdate,
            appl.duedate,
            appl.status,
            appl.origin,
            appl.child_id AS childid,
            appl.guardian_id AS guardianid,
            appl.type,
            ((appl.document ->> 'urgent'::text))::boolean AS urgent,
            ((appl.document ->> 'preferredStartDate'::text))::date AS preferredstartdate,
            COALESCE(( SELECT min(COALESCE(d.requested_start_date, d.start_date)) AS min
                   FROM public.decision d
                  WHERE ((d.application_id = appl.id) AND (d.status <> 'REJECTED'::public.decision_status))), placement_plan.start_date, ((appl.document ->> 'preferredStartDate'::text))::date) AS startdate,
            appl.primary_preferred_unit AS preferredunit,
            ( SELECT array_agg((e.value)::uuid) AS array_agg
                   FROM jsonb_array_elements_text(((appl.document -> 'apply'::text) -> 'preferredUnits'::text)) e(value)) AS preferredunits,
            ((appl.document -> 'child'::text) ->> 'firstName'::text) AS childfirstname,
            ((appl.document -> 'child'::text) ->> 'lastName'::text) AS childlastname,
            ((appl.document -> 'child'::text) ->> 'socialSecurityNumber'::text) AS childssn,
            (((appl.document -> 'child'::text) -> 'address'::text) ->> 'street'::text) AS childstreetaddr,
            (((appl.document -> 'child'::text) -> 'address'::text) ->> 'postalCode'::text) AS childpostalcode,
            ((appl.document -> 'guardian'::text) ->> 'phoneNumber'::text) AS guardianphonenumber,
            ((appl.document -> 'additionalDetails'::text) ->> 'allergyType'::text) AS allergytype,
            ((appl.document -> 'additionalDetails'::text) ->> 'dietType'::text) AS diettype,
            ((appl.document -> 'additionalDetails'::text) ->> 'otherInfo'::text) AS otherinfo,
            (((appl.document -> 'apply'::text) ->> 'siblingBasis'::text))::boolean AS siblingbasis,
            (appl.document ->> 'term'::text) AS term,
            ((appl.document ->> 'wasOnDaycare'::text))::boolean AS wasondaycare,
            ((appl.document ->> 'wasOnClubCare'::text))::boolean AS wasonclubcare,
            (((appl.document -> 'clubCare'::text) ->> 'assistanceNeeded'::text))::boolean AS clubcareassistanceneeded,
            (((appl.document -> 'careDetails'::text) ->> 'assistanceNeeded'::text))::boolean AS daycareassistanceneeded,
            ((appl.document ->> 'extendedCare'::text))::boolean AS extendedcare,
            placement_plan.unit_id AS placementdaycareunit,
            ((appl.document ->> 'connectedDaycare'::text))::boolean AS connecteddaycare,
            (appl.document ->> 'serviceNeedOption'::text) AS serviceneedoption,
            (((appl.document -> 'careDetails'::text) ->> 'preparatory'::text))::boolean AS preparatoryeducation,
            appl.checkedbyadmin,
            appl.hidefromguardian,
            appl.transferapplication,
            appl.additionaldaycareapplication,
            dup_appl.duplicate_application_ids AS duplicateapplicationids,
            (appl.document ->> 'otherGuardianAgreementStatus'::text) AS otherguardianagreementstatus
           FROM ((public.application appl
             LEFT JOIN public.placement_plan ON ((placement_plan.application_id = appl.id)))
             LEFT JOIN dup_appl ON ((dup_appl.id = appl.id)))
          WHERE (appl.document @> '{"docVersion": 0}'::jsonb)) jsonv0;

-- Name: case_process; Type: TABLE; Schema: public

CREATE TABLE public.case_process (
    id uuid DEFAULT ext.uuid_generate_v1mc() CONSTRAINT archived_process_id_not_null NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT archived_process_created_at_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT archived_process_updated_at_not_null NOT NULL,
    process_definition_number text CONSTRAINT archived_process_process_definition_number_not_null NOT NULL,
    year smallint CONSTRAINT archived_process_year_not_null NOT NULL,
    number integer CONSTRAINT archived_process_number_not_null NOT NULL,
    organization text CONSTRAINT archived_process_organization_not_null NOT NULL,
    archive_duration_months smallint CONSTRAINT archived_process_archive_duration_months_not_null NOT NULL,
    migrated boolean CONSTRAINT archived_process_migrated_not_null NOT NULL,
    case_identifier text GENERATED ALWAYS AS (((((number || '/'::text) || process_definition_number) || '/'::text) || year)) STORED
);

-- Name: archived_process; Type: VIEW; Schema: public

CREATE VIEW public.archived_process AS
 SELECT id,
    created_at,
    updated_at,
    process_definition_number,
    year,
    number,
    organization,
    archive_duration_months,
    migrated
   FROM public.case_process;

-- Name: case_process_history; Type: TABLE; Schema: public

CREATE TABLE public.case_process_history (
    process_id uuid CONSTRAINT archived_process_history_process_id_not_null NOT NULL,
    row_index smallint CONSTRAINT archived_process_history_row_index_not_null NOT NULL,
    state public.archived_process_state CONSTRAINT archived_process_history_state_not_null NOT NULL,
    entered_at timestamp with time zone CONSTRAINT archived_process_history_entered_at_not_null NOT NULL,
    entered_by uuid CONSTRAINT archived_process_history_entered_by_not_null NOT NULL
);

-- Name: archived_process_history; Type: VIEW; Schema: public

CREATE VIEW public.archived_process_history AS
 SELECT process_id,
    row_index,
    state,
    entered_at,
    entered_by
   FROM public.case_process_history;

-- Name: assistance_action; Type: TABLE; Schema: public

CREATE TABLE public.assistance_action (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_action_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_action_updated_not_null NOT NULL,
    modified_by uuid CONSTRAINT assistance_action_updated_by_not_null NOT NULL,
    child_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    other_action text DEFAULT ''::text NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    CONSTRAINT assistance_action_start_before_end CHECK ((start_date <= end_date))
);

-- Name: assistance_action_option; Type: TABLE; Schema: public

CREATE TABLE public.assistance_action_option (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_action_option_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_action_option_updated_not_null NOT NULL,
    value text NOT NULL,
    name_fi text NOT NULL,
    display_order integer,
    description_fi text,
    valid_from date,
    valid_to date,
    category public.assistance_action_option_category NOT NULL,
    CONSTRAINT check_validity CHECK (((valid_from IS NULL) OR (valid_to IS NULL) OR (valid_from <= valid_to)))
);

-- Name: assistance_action_option_ref; Type: TABLE; Schema: public

CREATE TABLE public.assistance_action_option_ref (
    action_id uuid NOT NULL,
    option_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_action_option_ref_created_not_null NOT NULL
);

-- Name: assistance_factor; Type: TABLE; Schema: public

CREATE TABLE public.assistance_factor (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_factor_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_factor_updated_not_null NOT NULL,
    child_id uuid NOT NULL,
    modified_at timestamp with time zone CONSTRAINT assistance_factor_modified_not_null NOT NULL,
    modified_by uuid NOT NULL,
    valid_during daterange NOT NULL,
    capacity_factor numeric NOT NULL,
    CONSTRAINT "check$range_valid" CHECK ((NOT (lower_inf(valid_during) OR upper_inf(valid_during))))
);

-- Name: assistance_need_decision_number_seq; Type: SEQUENCE; Schema: public

CREATE SEQUENCE public.assistance_need_decision_number_seq
    START WITH 1000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Name: assistance_need_decision; Type: TABLE; Schema: public

CREATE TABLE public.assistance_need_decision (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_need_decision_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_need_decision_updated_not_null NOT NULL,
    decision_number integer DEFAULT nextval('public.assistance_need_decision_number_seq'::regclass) NOT NULL,
    child_id uuid NOT NULL,
    language public.official_language NOT NULL,
    decision_made date,
    sent_for_decision date,
    selected_unit uuid,
    decision_maker_employee_id uuid,
    decision_maker_title text,
    preparer_1_employee_id uuid,
    preparer_1_title text,
    preparer_2_employee_id uuid,
    preparer_2_title text,
    pedagogical_motivation text,
    structural_motivation_opt_smaller_group boolean DEFAULT false CONSTRAINT assistance_need_decision_structural_motivation_opt_sma_not_null NOT NULL,
    structural_motivation_opt_special_group boolean DEFAULT false CONSTRAINT assistance_need_decision_structural_motivation_opt_spe_not_null NOT NULL,
    structural_motivation_opt_small_group boolean DEFAULT false CONSTRAINT assistance_need_decision_structural_motivation_opt_sm_not_null1 NOT NULL,
    structural_motivation_opt_group_assistant boolean DEFAULT false CONSTRAINT assistance_need_decision_structural_motivation_opt_gro_not_null NOT NULL,
    structural_motivation_opt_child_assistant boolean DEFAULT false CONSTRAINT assistance_need_decision_structural_motivation_opt_chi_not_null NOT NULL,
    structural_motivation_opt_additional_staff boolean DEFAULT false CONSTRAINT assistance_need_decision_structural_motivation_opt_add_not_null NOT NULL,
    structural_motivation_description text,
    care_motivation text,
    service_opt_consultation_special_ed boolean DEFAULT false CONSTRAINT assistance_need_decision_service_opt_consultation_spec_not_null NOT NULL,
    service_opt_part_time_special_ed boolean DEFAULT false CONSTRAINT assistance_need_decision_service_opt_part_time_special_not_null NOT NULL,
    service_opt_full_time_special_ed boolean DEFAULT false CONSTRAINT assistance_need_decision_service_opt_full_time_special_not_null NOT NULL,
    service_opt_interpretation_and_assistance_services boolean DEFAULT false CONSTRAINT assistance_need_decision_service_opt_interpretation_an_not_null NOT NULL,
    service_opt_special_aides boolean DEFAULT false NOT NULL,
    services_motivation text,
    expert_responsibilities text,
    guardians_heard_on date,
    view_of_guardians text,
    other_representative_heard boolean DEFAULT false NOT NULL,
    other_representative_details text,
    motivation_for_decision text,
    preparer_1_phone_number text,
    preparer_2_phone_number text,
    decision_maker_has_opened boolean DEFAULT false NOT NULL,
    document_key text,
    unread_guardian_ids uuid[],
    assistance_levels text[] DEFAULT ARRAY[]::text[],
    validity_period daterange,
    status public.assistance_need_decision_status NOT NULL,
    annulment_reason text DEFAULT ''::text NOT NULL,
    document_contains_contact_info boolean DEFAULT false CONSTRAINT assistance_need_decision_document_contains_contact_inf_not_null NOT NULL,
    process_id uuid,
    created_by uuid,
    end_date_not_known boolean NOT NULL,
    CONSTRAINT "check$annulment_reason" CHECK (
CASE status
    WHEN 'ANNULLED'::public.assistance_need_decision_status THEN (annulment_reason <> ''::text)
    ELSE (annulment_reason = ''::text)
END)
);

-- Name: assistance_need_decision_guardian; Type: TABLE; Schema: public

CREATE TABLE public.assistance_need_decision_guardian (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_need_decision_guardian_created_not_null NOT NULL,
    assistance_need_decision_id uuid CONSTRAINT assistance_need_decision_gu_assistance_need_decision_i_not_null NOT NULL,
    person_id uuid NOT NULL,
    is_heard boolean DEFAULT false NOT NULL,
    details text
);

-- Name: assistance_need_preschool_decision_number_seq; Type: SEQUENCE; Schema: public

CREATE SEQUENCE public.assistance_need_preschool_decision_number_seq
    START WITH 1000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Name: assistance_need_preschool_decision; Type: TABLE; Schema: public

CREATE TABLE public.assistance_need_preschool_decision (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_need_preschool_decision_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_need_preschool_decision_updated_not_null NOT NULL,
    decision_number integer DEFAULT nextval('public.assistance_need_preschool_decision_number_seq'::regclass) NOT NULL,
    child_id uuid NOT NULL,
    status public.assistance_need_decision_status DEFAULT 'DRAFT'::public.assistance_need_decision_status NOT NULL,
    language public.official_language DEFAULT 'FI'::public.official_language NOT NULL,
    type public.assistance_need_preschool_decision_type,
    valid_from date,
    extended_compulsory_education boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_extended_compulsory_educat_not_null NOT NULL,
    extended_compulsory_education_info text DEFAULT ''::text CONSTRAINT assistance_need_preschool__extended_compulsory_educat_not_null1 NOT NULL,
    granted_assistance_service boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_granted_assistance_service_not_null NOT NULL,
    granted_interpretation_service boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_granted_interpretation_ser_not_null NOT NULL,
    granted_assistive_devices boolean DEFAULT false CONSTRAINT assistance_need_preschool_de_granted_assistive_devices_not_null NOT NULL,
    granted_services_basis text DEFAULT ''::text CONSTRAINT assistance_need_preschool_decis_granted_services_basis_not_null NOT NULL,
    selected_unit uuid,
    primary_group text DEFAULT ''::text NOT NULL,
    decision_basis text DEFAULT ''::text NOT NULL,
    basis_document_pedagogical_report boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_basis_document_pedagogical_not_null NOT NULL,
    basis_document_psychologist_statement boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_basis_document_psychologis_not_null NOT NULL,
    basis_document_social_report boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_basis_document_social_repo_not_null NOT NULL,
    basis_document_doctor_statement boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_basis_document_doctor_stat_not_null NOT NULL,
    basis_document_other_or_missing boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_basis_document_other_or_mi_not_null NOT NULL,
    basis_document_other_or_missing_info text DEFAULT ''::text CONSTRAINT assistance_need_preschool__basis_document_other_or_mi_not_null1 NOT NULL,
    basis_documents_info text DEFAULT ''::text CONSTRAINT assistance_need_preschool_decisio_basis_documents_info_not_null NOT NULL,
    guardians_heard_on date,
    other_representative_heard boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_other_representative_heard_not_null NOT NULL,
    other_representative_details text DEFAULT ''::text CONSTRAINT assistance_need_preschool_d_other_representative_detai_not_null NOT NULL,
    view_of_guardians text DEFAULT ''::text NOT NULL,
    preparer_1_employee_id uuid,
    preparer_1_title text DEFAULT ''::text NOT NULL,
    preparer_1_phone_number text DEFAULT ''::text CONSTRAINT assistance_need_preschool_deci_preparer_1_phone_number_not_null NOT NULL,
    preparer_2_employee_id uuid,
    preparer_2_title text DEFAULT ''::text NOT NULL,
    preparer_2_phone_number text DEFAULT ''::text CONSTRAINT assistance_need_preschool_deci_preparer_2_phone_number_not_null NOT NULL,
    decision_maker_employee_id uuid,
    decision_maker_title text DEFAULT ''::text CONSTRAINT assistance_need_preschool_decisio_decision_maker_title_not_null NOT NULL,
    sent_for_decision date,
    decision_made date,
    decision_maker_has_opened boolean DEFAULT false CONSTRAINT assistance_need_preschool_de_decision_maker_has_opened_not_null NOT NULL,
    unread_guardian_ids uuid[],
    annulment_reason text DEFAULT ''::text NOT NULL,
    document_key text,
    basis_document_pedagogical_report_date date,
    basis_document_psychologist_statement_date date,
    basis_document_social_report_date date,
    basis_document_doctor_statement_date date,
    valid_to date,
    document_contains_contact_info boolean DEFAULT false CONSTRAINT assistance_need_preschool_d_document_contains_contact__not_null NOT NULL,
    process_id uuid,
    created_by uuid,
    CONSTRAINT "check$annulled" CHECK (
CASE
    WHEN (status = 'ANNULLED'::public.assistance_need_decision_status) THEN (annulment_reason <> ''::text)
    ELSE (annulment_reason = ''::text)
END),
    CONSTRAINT "check$decision_made" CHECK (((status <> ALL (ARRAY['ACCEPTED'::public.assistance_need_decision_status, 'REJECTED'::public.assistance_need_decision_status, 'ANNULLED'::public.assistance_need_decision_status])) OR ((decision_made IS NOT NULL) AND (unread_guardian_ids IS NOT NULL)))),
    CONSTRAINT "check$validated" CHECK (((status = 'NEEDS_WORK'::public.assistance_need_decision_status) OR ((status = 'DRAFT'::public.assistance_need_decision_status) AND (sent_for_decision IS NULL)) OR ((type IS NOT NULL) AND (valid_from IS NOT NULL) AND (selected_unit IS NOT NULL) AND (preparer_1_employee_id IS NOT NULL) AND (decision_maker_employee_id IS NOT NULL)))),
    CONSTRAINT valid_from_before_valid_to CHECK ((valid_from <= valid_to))
);

-- Name: assistance_need_preschool_decision_guardian; Type: TABLE; Schema: public

CREATE TABLE public.assistance_need_preschool_decision_guardian (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_need_preschool_decision_guardian_created_not_null NOT NULL,
    assistance_need_decision_id uuid CONSTRAINT assistance_need_preschool_d_assistance_need_decision_i_not_null NOT NULL,
    person_id uuid NOT NULL,
    is_heard boolean DEFAULT false NOT NULL,
    details text DEFAULT ''::text NOT NULL
);

-- Name: assistance_need_voucher_coefficient; Type: TABLE; Schema: public

CREATE TABLE public.assistance_need_voucher_coefficient (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_need_voucher_coefficient_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT assistance_need_voucher_coefficient_updated_not_null NOT NULL,
    child_id uuid NOT NULL,
    validity_period daterange NOT NULL,
    coefficient numeric(4,2) NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    CONSTRAINT check_validity_period CHECK ((NOT (lower_inf(validity_period) OR upper_inf(validity_period))))
);

-- Name: async_job; Type: TABLE; Schema: public

CREATE TABLE public.async_job (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    type text NOT NULL,
    submitted_at timestamp with time zone DEFAULT now() NOT NULL,
    run_at timestamp with time zone DEFAULT now() NOT NULL,
    claimed_at timestamp with time zone,
    claimed_by bigint,
    retry_count integer NOT NULL,
    retry_interval interval NOT NULL,
    started_at timestamp with time zone,
    completed_at timestamp with time zone,
    payload jsonb NOT NULL
);

-- Name: async_job_work_permit; Type: TABLE; Schema: public

CREATE TABLE public.async_job_work_permit (
    pool_id text NOT NULL,
    available_at timestamp with time zone NOT NULL
);

-- Name: attachment; Type: TABLE; Schema: public

CREATE TABLE public.attachment (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT attachment_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT attachment_updated_not_null NOT NULL,
    name text NOT NULL,
    content_type text NOT NULL,
    application_id uuid,
    type text,
    received_at timestamp with time zone DEFAULT now() NOT NULL,
    income_statement_id uuid,
    message_content_id uuid,
    message_draft_id uuid,
    pedagogical_document_id uuid,
    uploaded_by uuid NOT NULL,
    income_id uuid,
    fee_alteration_id uuid,
    invoice_id uuid,
    CONSTRAINT created_for_fk CHECK ((num_nonnulls(application_id, fee_alteration_id, income_id, income_statement_id, invoice_id, message_content_id, message_draft_id, pedagogical_document_id) <= 1))
);

-- Name: attendance_reservation; Type: TABLE; Schema: public

CREATE TABLE public.attendance_reservation (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT attendance_reservation_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT attendance_reservation_updated_not_null NOT NULL,
    child_id uuid NOT NULL,
    created_by uuid NOT NULL,
    date date NOT NULL,
    start_time time without time zone,
    end_time time without time zone,
    CONSTRAINT attendance_reservation_start_before_end CHECK ((start_time < end_time)),
    CONSTRAINT attendance_reservation_times_consistency CHECK ((((start_time IS NOT NULL) AND (end_time IS NOT NULL)) OR ((start_time IS NULL) AND (end_time IS NULL))))
);

-- Name: backup_care; Type: TABLE; Schema: public

CREATE TABLE public.backup_care (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    group_id uuid,
    start_date date NOT NULL,
    end_date date NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    created_by uuid NOT NULL,
    modified_by uuid NOT NULL,
    CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);

-- Name: backup_pickup; Type: TABLE; Schema: public

CREATE TABLE public.backup_pickup (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    child_id uuid NOT NULL,
    name text NOT NULL,
    phone text NOT NULL
);

-- Name: calendar_event; Type: TABLE; Schema: public

CREATE TABLE public.calendar_event (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    title text NOT NULL,
    description text NOT NULL,
    period daterange NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT calendar_event_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT calendar_event_updated_not_null NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    content_modified_at timestamp with time zone NOT NULL,
    event_type public.calendar_event_type NOT NULL,
    created_by uuid NOT NULL,
    modified_by uuid NOT NULL,
    content_modified_by uuid NOT NULL,
    nekku_unordered_meals public.nekku_product_meal_time[] DEFAULT '{}'::public.nekku_product_meal_time[] NOT NULL
);

-- Name: calendar_event_attendee; Type: TABLE; Schema: public

CREATE TABLE public.calendar_event_attendee (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    calendar_event_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    group_id uuid,
    child_id uuid,
    CONSTRAINT "check$child_has_group" CHECK (((child_id IS NULL) OR (group_id IS NOT NULL)))
);

-- Name: daycare_group_placement; Type: TABLE; Schema: public

CREATE TABLE public.daycare_group_placement (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now(),
    updated timestamp with time zone DEFAULT now(),
    daycare_placement_id uuid NOT NULL,
    daycare_group_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);

-- Name: calendar_event_attendee_child_view; Type: VIEW; Schema: public

CREATE VIEW public.calendar_event_attendee_child_view AS
 SELECT calendar_event_attendee.calendar_event_id,
    calendar_event_attendee.child_id
   FROM public.calendar_event_attendee
  WHERE (calendar_event_attendee.child_id IS NOT NULL)
UNION
 SELECT DISTINCT ce.id AS calendar_event_id,
    p.child_id
   FROM (((public.calendar_event_attendee cea
     JOIN public.calendar_event ce ON ((cea.calendar_event_id = ce.id)))
     JOIN public.daycare_group_placement dgp ON (((cea.group_id = dgp.daycare_group_id) AND (daterange(dgp.start_date, dgp.end_date, '[]'::text) && ce.period))))
     JOIN public.placement p ON (((dgp.daycare_placement_id = p.id) AND (daterange(p.start_date, p.end_date, '[]'::text) && ce.period))))
  WHERE (cea.child_id IS NULL)
UNION
 SELECT DISTINCT ce.id AS calendar_event_id,
    p.child_id
   FROM ((public.calendar_event_attendee cea
     JOIN public.calendar_event ce ON ((cea.calendar_event_id = ce.id)))
     JOIN public.placement p ON (((cea.unit_id = p.unit_id) AND (daterange(p.start_date, p.end_date, '[]'::text) && ce.period))))
  WHERE ((cea.group_id IS NULL) AND (cea.child_id IS NULL));

-- Name: calendar_event_time; Type: TABLE; Schema: public

CREATE TABLE public.calendar_event_time (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by uuid NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    calendar_event_id uuid NOT NULL,
    date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    child_id uuid
);

-- Name: care_area; Type: TABLE; Schema: public

CREATE TABLE public.care_area (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    name text NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    area_code integer,
    sub_cost_center text,
    short_name text DEFAULT ''::text NOT NULL
);

-- Name: child; Type: TABLE; Schema: public

CREATE TABLE public.child (
    id uuid NOT NULL,
    allergies text DEFAULT ''::text NOT NULL,
    diet text DEFAULT ''::text NOT NULL,
    additionalinfo text DEFAULT ''::text NOT NULL,
    medication text DEFAULT ''::text NOT NULL,
    language_at_home text DEFAULT ''::text NOT NULL,
    language_at_home_details text DEFAULT ''::text NOT NULL,
    diet_id integer,
    meal_texture_id integer,
    nekku_diet public.nekku_product_meal_type,
    participates_in_breakfast boolean DEFAULT true CONSTRAINT child_nekku_eats_breakfast_not_null NOT NULL
);

-- Name: child_attendance; Type: TABLE; Schema: public

CREATE TABLE public.child_attendance (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    child_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT child_attendance_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT child_attendance_updated_not_null NOT NULL,
    unit_id uuid NOT NULL,
    date date NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone,
    arrived timestamp with time zone GENERATED ALWAYS AS (((date + start_time) AT TIME ZONE 'Europe/Helsinki'::text)) STORED NOT NULL,
    departed timestamp with time zone GENERATED ALWAYS AS (((date + end_time) AT TIME ZONE 'Europe/Helsinki'::text)) STORED,
    modified_at timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    CONSTRAINT child_attendance_start_before_end CHECK ((start_time < end_time)),
    CONSTRAINT child_attendance_time_resolution CHECK (((EXTRACT(second FROM start_time) = (0)::numeric) AND (EXTRACT(second FROM end_time) = (0)::numeric)))
);

-- Name: child_daily_note; Type: TABLE; Schema: public

CREATE TABLE public.child_daily_note (
    id uuid DEFAULT ext.uuid_generate_v1mc() CONSTRAINT daycare_daily_note_id_not_null NOT NULL,
    child_id uuid NOT NULL,
    note text NOT NULL,
    feeding_note public.child_daily_note_level,
    sleeping_note public.child_daily_note_level,
    reminders public.child_daily_note_reminder[],
    reminder_note text NOT NULL,
    modified_at timestamp with time zone DEFAULT now() CONSTRAINT daycare_daily_note_modified_at_not_null NOT NULL,
    sleeping_minutes integer,
    created timestamp with time zone DEFAULT now() CONSTRAINT daycare_daily_note_created_not_null NOT NULL,
    updated timestamp with time zone DEFAULT now() CONSTRAINT daycare_daily_note_updated_not_null NOT NULL
);

-- Name: child_document; Type: TABLE; Schema: public

CREATE TABLE public.child_document (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL,
    template_id uuid NOT NULL,
    content jsonb NOT NULL,
    deprecated_published_at timestamp with time zone,
    status public.child_document_status NOT NULL,
    deprecated_published_content jsonb,
    modified_at timestamp with time zone NOT NULL,
    deprecated_document_key text,
    content_locked_at timestamp with time zone CONSTRAINT child_document_content_modified_at_not_null NOT NULL,
    content_locked_by uuid,
    process_id uuid,
    created_by uuid,
    archived_at timestamp with time zone,
    answered_at timestamp with time zone,
    answered_by uuid,
    type public.document_template_type NOT NULL,
    decision_maker uuid,
    decision_id uuid,
    deprecated_published_by uuid,
    modified_by uuid DEFAULT '00000000-0000-0000-0000-000000000000'::uuid NOT NULL,
    CONSTRAINT answerable_document CHECK (((type = 'CITIZEN_BASIC'::public.document_template_type) OR ((answered_at IS NULL) AND (answered_by IS NULL)))),
    CONSTRAINT answered_consistency CHECK (((answered_at IS NULL) = (answered_by IS NULL))),
    CONSTRAINT archived_documents_must_be_completed CHECK (((archived_at IS NULL) OR (status = 'COMPLETED'::public.child_document_status))),
    CONSTRAINT decision_consistency CHECK (((decision_id IS NOT NULL) = ((status = 'COMPLETED'::public.child_document_status) AND (type = ANY (ARRAY['OTHER_DECISION'::public.document_template_type, 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION'::public.document_template_type, 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'::public.document_template_type]))))),
    CONSTRAINT decision_maker_consistency CHECK (
CASE
    WHEN (type <> ALL (ARRAY['OTHER_DECISION'::public.document_template_type, 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION'::public.document_template_type, 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'::public.document_template_type])) THEN (decision_maker IS NULL)
    WHEN ((type = ANY (ARRAY['OTHER_DECISION'::public.document_template_type, 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION'::public.document_template_type, 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'::public.document_template_type])) AND (status = ANY (ARRAY['DECISION_PROPOSAL'::public.child_document_status, 'COMPLETED'::public.child_document_status]))) THEN (decision_maker IS NOT NULL)
    ELSE true
END),
    CONSTRAINT valid_status CHECK (
CASE
    WHEN (type = 'PEDAGOGICAL_REPORT'::public.document_template_type) THEN (status = ANY (ARRAY['DRAFT'::public.child_document_status, 'COMPLETED'::public.child_document_status]))
    WHEN (type = 'PEDAGOGICAL_ASSESSMENT'::public.document_template_type) THEN (status = ANY (ARRAY['DRAFT'::public.child_document_status, 'COMPLETED'::public.child_document_status]))
    WHEN (type = 'HOJKS'::public.document_template_type) THEN (status = ANY (ARRAY['DRAFT'::public.child_document_status, 'PREPARED'::public.child_document_status, 'COMPLETED'::public.child_document_status]))
    WHEN (type = 'MIGRATED_VASU'::public.document_template_type) THEN (status = 'COMPLETED'::public.child_document_status)
    WHEN (type = 'MIGRATED_LEOPS'::public.document_template_type) THEN (status = 'COMPLETED'::public.child_document_status)
    WHEN (type = 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION'::public.document_template_type) THEN (status = 'COMPLETED'::public.child_document_status)
    WHEN (type = 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'::public.document_template_type) THEN (status = 'COMPLETED'::public.child_document_status)
    WHEN (type = 'VASU'::public.document_template_type) THEN (status = ANY (ARRAY['DRAFT'::public.child_document_status, 'PREPARED'::public.child_document_status, 'COMPLETED'::public.child_document_status]))
    WHEN (type = 'LEOPS'::public.document_template_type) THEN (status = ANY (ARRAY['DRAFT'::public.child_document_status, 'PREPARED'::public.child_document_status, 'COMPLETED'::public.child_document_status]))
    WHEN (type = 'CITIZEN_BASIC'::public.document_template_type) THEN (status = ANY (ARRAY['DRAFT'::public.child_document_status, 'CITIZEN_DRAFT'::public.child_document_status, 'COMPLETED'::public.child_document_status]))
    WHEN (type = 'OTHER_DECISION'::public.document_template_type) THEN (status = ANY (ARRAY['DRAFT'::public.child_document_status, 'DECISION_PROPOSAL'::public.child_document_status, 'COMPLETED'::public.child_document_status]))
    WHEN (type = 'OTHER'::public.document_template_type) THEN (status = ANY (ARRAY['DRAFT'::public.child_document_status, 'COMPLETED'::public.child_document_status]))
    ELSE NULL::boolean
END)
);

-- Name: child_document_decision_number_seq; Type: SEQUENCE; Schema: public

CREATE SEQUENCE public.child_document_decision_number_seq
    START WITH 10000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Name: child_document_decision; Type: TABLE; Schema: public

CREATE TABLE public.child_document_decision (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    modified_at timestamp with time zone DEFAULT now() NOT NULL,
    modified_by uuid NOT NULL,
    status public.child_document_decision_status NOT NULL,
    valid_from date,
    valid_to date,
    decision_number integer DEFAULT nextval('public.child_document_decision_number_seq'::regclass) NOT NULL,
    daycare_id uuid,
    annulment_reason text DEFAULT ''::text NOT NULL,
    CONSTRAINT "check$annulment_reason" CHECK (
CASE status
    WHEN 'ANNULLED'::public.child_document_decision_status THEN (annulment_reason <> ''::text)
    ELSE (annulment_reason = ''::text)
END),
    CONSTRAINT child_document_decision_check CHECK (((status = ANY (ARRAY['ACCEPTED'::public.child_document_decision_status, 'ANNULLED'::public.child_document_decision_status])) = (valid_from IS NOT NULL))),
    CONSTRAINT child_document_decision_check1 CHECK (((valid_to IS NULL) OR ((valid_from IS NOT NULL) AND (valid_to >= valid_from))))
);

-- Name: child_document_published_version; Type: TABLE; Schema: public

CREATE TABLE public.child_document_published_version (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    child_document_id uuid NOT NULL,
    document_key text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    version_number integer NOT NULL,
    created_by uuid NOT NULL,
    published_content jsonb NOT NULL
);

-- Name: evaka_user; Type: TABLE; Schema: public

CREATE TABLE public.evaka_user (
    id uuid NOT NULL,
    type public.evaka_user_type NOT NULL,
    citizen_id uuid,
    employee_id uuid,
    mobile_device_id uuid,
    name text NOT NULL,
    CONSTRAINT fk_count CHECK ((num_nonnulls(citizen_id, employee_id, mobile_device_id) <= 1)),
    CONSTRAINT type_id_match CHECK (
CASE type
    WHEN 'SYSTEM'::public.evaka_user_type THEN ((id = '00000000-0000-0000-0000-000000000000'::uuid) AND (num_nonnulls(citizen_id, employee_id, mobile_device_id) = 0))
    WHEN 'CITIZEN'::public.evaka_user_type THEN (((id = citizen_id) OR (citizen_id IS NULL)) AND (employee_id IS NULL) AND (mobile_device_id IS NULL))
    WHEN 'EMPLOYEE'::public.evaka_user_type THEN (((id = employee_id) OR (employee_id IS NULL)) AND (citizen_id IS NULL) AND (mobile_device_id IS NULL))
    WHEN 'MOBILE_DEVICE'::public.evaka_user_type THEN (((id = mobile_device_id) OR (mobile_device_id IS NULL)) AND (citizen_id IS NULL) AND (employee_id IS NULL))
    WHEN 'UNKNOWN'::public.evaka_user_type THEN (num_nonnulls(citizen_id, employee_id, mobile_device_id) = 0)
    ELSE NULL::boolean
END)
);

-- Name: child_document_latest_published_version; Type: VIEW; Schema: public

CREATE VIEW public.child_document_latest_published_version AS
 SELECT DISTINCT ON (v.child_document_id) v.child_document_id,
    v.id AS version_id,
    v.created_at AS published_at,
    v.version_number,
    v.created_by AS published_by_id,
    u.name AS published_by_name,
    v.published_content,
    v.document_key
   FROM (public.child_document_published_version v
     JOIN public.evaka_user u ON ((v.created_by = u.id)))
  ORDER BY v.child_document_id, v.version_number DESC;

-- Name: child_document_read; Type: TABLE; Schema: public

CREATE TABLE public.child_document_read (
    document_id uuid NOT NULL,
    person_id uuid NOT NULL,
    read_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Name: child_images; Type: TABLE; Schema: public

CREATE TABLE public.child_images (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid
);

-- Name: child_sticky_note; Type: TABLE; Schema: public

CREATE TABLE public.child_sticky_note (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL,
    note text NOT NULL,
    modified_at timestamp with time zone DEFAULT now() NOT NULL,
    expires date DEFAULT (now() + '7 days'::interval) NOT NULL
);

-- Name: citizen_user; Type: TABLE; Schema: public

CREATE TABLE public.citizen_user (
    id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    last_strong_login timestamp with time zone,
    last_weak_login timestamp with time zone,
    username text,
    username_updated_at timestamp with time zone,
    password jsonb,
    password_updated_at timestamp with time zone,
    CONSTRAINT "check$username_format" CHECK ((lower(TRIM(BOTH FROM username)) = username)),
    CONSTRAINT "check$weak_credentials" CHECK (((username IS NULL) = (password IS NULL))),
    CONSTRAINT "check$weak_credentials_updated" CHECK ((((username IS NULL) OR (username_updated_at IS NOT NULL)) AND ((username_updated_at IS NULL) = (password_updated_at IS NULL))))
);

-- Name: club_term; Type: TABLE; Schema: public

CREATE TABLE public.club_term (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    term daterange NOT NULL,
    application_period daterange NOT NULL,
    term_breaks datemultirange NOT NULL,
    CONSTRAINT "check$application_period_valid" CHECK ((NOT (lower_inf(application_period) OR upper_inf(application_period)))),
    CONSTRAINT "check$term_breaks_valid" CHECK ((NOT (lower_inf(term_breaks) OR upper_inf(term_breaks)))),
    CONSTRAINT "check$term_contains_term_breaks" CHECK ((term @> term_breaks)),
    CONSTRAINT "check$term_valid" CHECK ((NOT (lower_inf(term) OR upper_inf(term))))
);

-- Name: daily_service_time_notification; Type: TABLE; Schema: public

CREATE TABLE public.daily_service_time_notification (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    guardian_id uuid NOT NULL
);

-- Name: daycare_acl; Type: TABLE; Schema: public

CREATE TABLE public.daycare_acl (
    daycare_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    role public.user_role NOT NULL,
    end_date date,
    CONSTRAINT "chk$valid_role" CHECK ((role = ANY (ARRAY['UNIT_SUPERVISOR'::public.user_role, 'STAFF'::public.user_role, 'SPECIAL_EDUCATION_TEACHER'::public.user_role, 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'::public.user_role])))
);

-- Name: daycare_acl_schedule; Type: TABLE; Schema: public

CREATE TABLE public.daycare_acl_schedule (
    daycare_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    role public.user_role NOT NULL,
    start_date date NOT NULL,
    end_date date,
    CONSTRAINT "chk$valid_role" CHECK ((role = ANY (ARRAY['UNIT_SUPERVISOR'::public.user_role, 'STAFF'::public.user_role, 'SPECIAL_EDUCATION_TEACHER'::public.user_role, 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'::public.user_role])))
);

-- Name: mobile_device; Type: TABLE; Schema: public

CREATE TABLE public.mobile_device (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    unit_id uuid,
    name text NOT NULL,
    long_term_token uuid,
    employee_id uuid,
    last_seen timestamp with time zone,
    user_agent text,
    push_notification_categories public.push_notification_category[] DEFAULT '{}'::public.push_notification_category[] NOT NULL,
    CONSTRAINT mobile_device_non_null_reference CHECK ((num_nonnulls(unit_id, employee_id) = 1))
);

-- Name: mobile_device_daycare_acl_view; Type: VIEW; Schema: public

CREATE VIEW public.mobile_device_daycare_acl_view AS
 SELECT mobile_device.id AS mobile_device_id,
    mobile_device.unit_id AS daycare_id
   FROM public.mobile_device
  WHERE (mobile_device.unit_id IS NOT NULL)
UNION ALL
 SELECT mobile_device.id AS mobile_device_id,
    acl.daycare_id
   FROM (public.mobile_device
     JOIN public.daycare_acl acl ON ((mobile_device.employee_id = acl.employee_id)))
  WHERE (mobile_device.unit_id IS NULL);

-- Name: daycare_acl_view; Type: VIEW; Schema: public

CREATE VIEW public.daycare_acl_view AS
 SELECT daycare_acl.employee_id,
    daycare_acl.daycare_id,
    daycare_acl.role
   FROM public.daycare_acl
UNION ALL
 SELECT mobile_device_daycare_acl_view.mobile_device_id AS employee_id,
    mobile_device_daycare_acl_view.daycare_id,
    'MOBILE'::public.user_role AS role
   FROM public.mobile_device_daycare_acl_view;

-- Name: daycare_assistance; Type: TABLE; Schema: public

CREATE TABLE public.daycare_assistance (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL,
    modified timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    valid_during daterange NOT NULL,
    level public.daycare_assistance_level NOT NULL,
    CONSTRAINT "check$range_valid" CHECK ((NOT (lower_inf(valid_during) OR upper_inf(valid_during))))
);

-- Name: daycare_caretaker; Type: TABLE; Schema: public

CREATE TABLE public.daycare_caretaker (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    group_id uuid NOT NULL,
    amount numeric DEFAULT 0 NOT NULL,
    start_date date NOT NULL,
    end_date date,
    CONSTRAINT daycare_caretaker_no_negative_people CHECK ((amount >= (0)::numeric)),
    CONSTRAINT daycare_caretaker_start_before_end CHECK ((start_date <= end_date))
);

-- Name: daycare_group; Type: TABLE; Schema: public

CREATE TABLE public.daycare_group (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    daycare_id uuid NOT NULL,
    name text NOT NULL,
    start_date date NOT NULL,
    end_date date,
    jamix_customer_number integer,
    aromi_customer_id text,
    nekku_customer_number text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);

-- Name: daycare_group_acl; Type: TABLE; Schema: public

CREATE TABLE public.daycare_group_acl (
    daycare_group_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL
);

-- Name: decision_number_seq; Type: SEQUENCE; Schema: public

CREATE SEQUENCE public.decision_number_seq
    START WITH 1000000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Name: decision_number_seq; Type: SEQUENCE OWNED BY; Schema: public

ALTER SEQUENCE public.decision_number_seq OWNED BY public.decision.number;

-- Name: document_template; Type: TABLE; Schema: public

CREATE TABLE public.document_template (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    name text NOT NULL,
    type public.document_template_type NOT NULL,
    language public.ui_language NOT NULL,
    confidential boolean NOT NULL,
    legal_basis text NOT NULL,
    validity daterange NOT NULL,
    published boolean DEFAULT false NOT NULL,
    content jsonb NOT NULL,
    process_definition_number text,
    archive_duration_months smallint,
    placement_types public.placement_type[] NOT NULL,
    confidentiality_duration_years smallint,
    confidentiality_basis text,
    archive_externally boolean NOT NULL,
    end_decision_when_unit_changes boolean,
    CONSTRAINT "check$archive_duration_months_positive" CHECK (((archive_duration_months IS NULL) OR (archive_duration_months > 0))),
    CONSTRAINT "check$archive_externally_requires_metadata" CHECK (((NOT archive_externally) OR ((process_definition_number IS NOT NULL) AND (archive_duration_months IS NOT NULL)))),
    CONSTRAINT "check$archive_fields_nullability" CHECK (((process_definition_number IS NOT NULL) = (archive_duration_months IS NOT NULL))),
    CONSTRAINT "check$process_definition_number_not_blank" CHECK ((process_definition_number <> ''::text)),
    CONSTRAINT "check$validity" CHECK ((NOT lower_inf(validity))),
    CONSTRAINT decision_config CHECK (((type = ANY (ARRAY['OTHER_DECISION'::public.document_template_type, 'MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION'::public.document_template_type, 'MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION'::public.document_template_type])) = (end_decision_when_unit_changes IS NOT NULL))),
    CONSTRAINT document_template_confidentiality_check CHECK (((confidential = false) OR ((confidentiality_duration_years IS NOT NULL) AND (confidentiality_basis IS NOT NULL))))
);

-- Name: dvv_modification_token; Type: TABLE; Schema: public

CREATE TABLE public.dvv_modification_token (
    token text NOT NULL,
    next_token text,
    ssns_sent numeric DEFAULT 0 NOT NULL,
    modifications_received numeric DEFAULT 0 NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL
);

-- Name: employee; Type: TABLE; Schema: public

CREATE TABLE public.employee (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    first_name text NOT NULL,
    last_name text NOT NULL,
    email text,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    roles public.user_role[] DEFAULT '{}'::public.user_role[] NOT NULL,
    external_id text,
    last_login timestamp with time zone,
    employee_number text,
    preferred_first_name text,
    temporary_in_unit_id uuid,
    active boolean NOT NULL,
    social_security_number text,
    CONSTRAINT "chk$valid_role" CHECK ((ARRAY['ADMIN'::public.user_role, 'DIRECTOR'::public.user_role, 'REPORT_VIEWER'::public.user_role, 'FINANCE_ADMIN'::public.user_role, 'FINANCE_STAFF'::public.user_role, 'SERVICE_WORKER'::public.user_role, 'MESSAGING'::public.user_role] @> roles))
);

-- Name: employee_pin; Type: TABLE; Schema: public

CREATE TABLE public.employee_pin (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    user_id uuid NOT NULL,
    pin text NOT NULL,
    locked boolean DEFAULT false NOT NULL,
    failure_count smallint DEFAULT 0,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL
);

-- Name: family_contact; Type: TABLE; Schema: public

CREATE TABLE public.family_contact (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    child_id uuid NOT NULL,
    contact_person_id uuid NOT NULL,
    priority integer NOT NULL
);

-- Name: fee_alteration; Type: TABLE; Schema: public

CREATE TABLE public.fee_alteration (
    id uuid NOT NULL,
    person_id uuid NOT NULL,
    type public.fee_alteration_type NOT NULL,
    amount integer NOT NULL,
    is_absolute boolean NOT NULL,
    valid_from date NOT NULL,
    valid_to date,
    notes text NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    modified_by uuid CONSTRAINT fee_alteration_updated_by_not_null NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    CONSTRAINT fee_alteration_check CHECK ((valid_from <= COALESCE(valid_to, '2099-12-31'::date)))
);

-- Name: fee_decision; Type: TABLE; Schema: public

CREATE TABLE public.fee_decision (
    id uuid CONSTRAINT new_fee_decision_id_not_null NOT NULL,
    created timestamp with time zone DEFAULT now() CONSTRAINT new_fee_decision_created_not_null NOT NULL,
    updated timestamp with time zone DEFAULT now() CONSTRAINT new_fee_decision_updated_not_null NOT NULL,
    status public.fee_decision_status CONSTRAINT new_fee_decision_status_not_null NOT NULL,
    valid_during daterange CONSTRAINT new_fee_decision_valid_during_not_null NOT NULL,
    decision_type public.fee_decision_type DEFAULT 'NORMAL'::public.fee_decision_type CONSTRAINT new_fee_decision_decision_type_not_null NOT NULL,
    head_of_family_id uuid CONSTRAINT new_fee_decision_head_of_family_id_not_null NOT NULL,
    head_of_family_income jsonb,
    partner_id uuid,
    partner_income jsonb,
    family_size integer CONSTRAINT new_fee_decision_family_size_not_null NOT NULL,
    fee_thresholds jsonb CONSTRAINT new_fee_decision_pricing_not_null NOT NULL,
    decision_number bigint,
    document_key text,
    approved_at timestamp with time zone,
    approved_by_id uuid,
    decision_handler_id uuid,
    sent_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    total_fee integer NOT NULL,
    difference public.fee_decision_difference[] NOT NULL,
    document_contains_contact_info boolean DEFAULT false NOT NULL,
    process_id uuid,
    archived_at timestamp with time zone,
    CONSTRAINT "check$head_of_family_is_not_partner" CHECK (((partner_id IS NULL) OR (head_of_family_id <> partner_id))),
    CONSTRAINT "check$valid_range" CHECK ((NOT (lower_inf(valid_during) OR upper_inf(valid_during))))
);

-- Name: fee_decision_child; Type: TABLE; Schema: public

CREATE TABLE public.fee_decision_child (
    id uuid CONSTRAINT new_fee_decision_child_id_not_null NOT NULL,
    created timestamp with time zone DEFAULT now() CONSTRAINT new_fee_decision_child_created_not_null NOT NULL,
    updated timestamp with time zone DEFAULT now() CONSTRAINT new_fee_decision_child_updated_not_null NOT NULL,
    fee_decision_id uuid CONSTRAINT new_fee_decision_child_fee_decision_id_not_null NOT NULL,
    child_id uuid CONSTRAINT new_fee_decision_child_child_id_not_null NOT NULL,
    child_date_of_birth date CONSTRAINT new_fee_decision_child_child_date_of_birth_not_null NOT NULL,
    sibling_discount integer CONSTRAINT new_fee_decision_child_sibling_discount_not_null NOT NULL,
    placement_unit_id uuid CONSTRAINT new_fee_decision_child_placement_unit_id_not_null NOT NULL,
    placement_type public.placement_type CONSTRAINT new_fee_decision_child_placement_type_not_null NOT NULL,
    service_need_fee_coefficient numeric(4,2) CONSTRAINT new_fee_decision_child_service_need_fee_coefficient_not_null NOT NULL,
    service_need_description_fi text CONSTRAINT new_fee_decision_child_service_need_description_fi_not_null NOT NULL,
    service_need_description_sv text CONSTRAINT new_fee_decision_child_service_need_description_sv_not_null NOT NULL,
    base_fee integer CONSTRAINT new_fee_decision_child_base_fee_not_null NOT NULL,
    fee integer CONSTRAINT new_fee_decision_child_fee_not_null NOT NULL,
    fee_alterations jsonb CONSTRAINT new_fee_decision_child_fee_alterations_not_null NOT NULL,
    final_fee integer CONSTRAINT new_fee_decision_child_final_fee_not_null NOT NULL,
    service_need_missing boolean DEFAULT false CONSTRAINT new_fee_decision_child_service_need_missing_not_null NOT NULL,
    service_need_contract_days_per_month integer,
    child_income jsonb,
    service_need_option_id uuid
);

-- Name: fee_decision_number_sequence; Type: SEQUENCE; Schema: public

CREATE SEQUENCE public.fee_decision_number_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Name: fee_thresholds; Type: TABLE; Schema: public

CREATE TABLE public.fee_thresholds (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    valid_during daterange NOT NULL,
    min_income_threshold_2 integer NOT NULL,
    min_income_threshold_3 integer NOT NULL,
    min_income_threshold_4 integer NOT NULL,
    min_income_threshold_5 integer NOT NULL,
    min_income_threshold_6 integer NOT NULL,
    income_multiplier_2 numeric(5,4) NOT NULL,
    income_multiplier_3 numeric(5,4) NOT NULL,
    income_multiplier_4 numeric(5,4) NOT NULL,
    income_multiplier_5 numeric(5,4) NOT NULL,
    income_multiplier_6 numeric(5,4) NOT NULL,
    max_income_threshold_2 integer NOT NULL,
    max_income_threshold_3 integer NOT NULL,
    max_income_threshold_4 integer NOT NULL,
    max_income_threshold_5 integer NOT NULL,
    max_income_threshold_6 integer NOT NULL,
    income_threshold_increase_6_plus integer NOT NULL,
    sibling_discount_2 numeric(5,4) NOT NULL,
    sibling_discount_2_plus numeric(5,4) NOT NULL,
    max_fee integer NOT NULL,
    min_fee integer NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    temporary_fee integer NOT NULL,
    temporary_fee_part_day integer NOT NULL,
    temporary_fee_sibling integer NOT NULL,
    temporary_fee_sibling_part_day integer NOT NULL
);

-- Name: finance_note; Type: TABLE; Schema: public

CREATE TABLE public.finance_note (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    person_id uuid NOT NULL,
    content text,
    created_at timestamp with time zone NOT NULL,
    created_by uuid NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    updated_at timestamp with time zone NOT NULL
);

-- Name: flyway_schema_history; Type: TABLE; Schema: public

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);

-- Name: foster_parent; Type: TABLE; Schema: public

CREATE TABLE public.foster_parent (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT foster_parent_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT foster_parent_updated_not_null NOT NULL,
    child_id uuid NOT NULL,
    parent_id uuid NOT NULL,
    valid_during daterange NOT NULL,
    created_by uuid NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL
);

-- Name: fridge_child; Type: TABLE; Schema: public

CREATE TABLE public.fridge_child (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    child_id uuid NOT NULL,
    head_of_child uuid NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now(),
    updated timestamp with time zone DEFAULT now(),
    conflict boolean DEFAULT false NOT NULL,
    create_source public.create_source,
    created_by_user uuid,
    created_by_application uuid,
    modify_source public.modify_source,
    modified_by_user uuid,
    modified_at timestamp with time zone,
    CONSTRAINT check_created_by_application CHECK (((create_source = 'APPLICATION'::public.create_source) = (created_by_application IS NOT NULL))),
    CONSTRAINT check_created_by_user CHECK (((create_source = 'USER'::public.create_source) = (created_by_user IS NOT NULL))),
    CONSTRAINT check_modified_by_user CHECK (((modify_source = 'USER'::public.modify_source) = (modified_by_user IS NOT NULL))),
    CONSTRAINT fridge_child_check CHECK ((start_date <= end_date)),
    CONSTRAINT no_head_of_self CHECK ((child_id <> head_of_child))
);

-- Name: fridge_partner; Type: TABLE; Schema: public

CREATE TABLE public.fridge_partner (
    partnership_id uuid NOT NULL,
    indx smallint NOT NULL,
    person_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date,
    created_at timestamp with time zone NOT NULL,
    updated timestamp with time zone DEFAULT now(),
    conflict boolean DEFAULT false NOT NULL,
    other_indx smallint NOT NULL,
    create_source public.create_source,
    created_by uuid,
    modify_source public.modify_source,
    modified_at timestamp with time zone,
    modified_by uuid,
    created_from_application uuid,
    CONSTRAINT "chk$other_indx" CHECK ((indx <> other_indx)),
    CONSTRAINT fridge_partner_check CHECK ((start_date <= end_date)),
    CONSTRAINT fridge_partner_indx_check CHECK (((indx >= 1) AND (indx <= 2)))
);

-- Name: fridge_partner_view; Type: VIEW; Schema: public

CREATE VIEW public.fridge_partner_view AS
 SELECT fp1.partnership_id,
    fp1.start_date,
    fp1.end_date,
    fp1.conflict,
    p1.id AS person_id,
    p1.first_name,
    p1.last_name,
    p2.id AS partner_person_id,
    p2.first_name AS partner_first_name,
    p2.last_name AS partner_last_name
   FROM (((public.fridge_partner fp1
     JOIN public.fridge_partner fp2 ON (((fp1.partnership_id = fp2.partnership_id) AND (fp1.indx <> fp2.indx))))
     JOIN public.person p1 ON ((fp1.person_id = p1.id)))
     JOIN public.person p2 ON ((fp2.person_id = p2.id)));

-- Name: group_note; Type: TABLE; Schema: public

CREATE TABLE public.group_note (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    group_id uuid NOT NULL,
    note text NOT NULL,
    modified_at timestamp with time zone DEFAULT now() NOT NULL,
    expires date DEFAULT (now() + '7 days'::interval) NOT NULL
);

-- Name: guardian; Type: TABLE; Schema: public

CREATE TABLE public.guardian (
    guardian_id uuid NOT NULL,
    child_id uuid NOT NULL,
    created timestamp with time zone DEFAULT now()
);

-- Name: guardian_blocklist; Type: TABLE; Schema: public

CREATE TABLE public.guardian_blocklist (
    guardian_id uuid NOT NULL,
    child_id uuid NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL
);

-- Name: holiday_period; Type: TABLE; Schema: public

CREATE TABLE public.holiday_period (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    period daterange NOT NULL,
    reservation_deadline date NOT NULL,
    reservations_open_on date NOT NULL
);

-- Name: holiday_period_questionnaire; Type: TABLE; Schema: public

CREATE TABLE public.holiday_period_questionnaire (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    type public.questionnaire_type NOT NULL,
    absence_type public.absence_type NOT NULL,
    requires_strong_auth boolean NOT NULL,
    active daterange NOT NULL,
    title jsonb NOT NULL,
    description jsonb NOT NULL,
    description_link jsonb NOT NULL,
    condition_continuous_placement daterange,
    period_options daterange[],
    period_option_label jsonb,
    period daterange,
    absence_type_threshold integer,
    CONSTRAINT period_null_or_valid CHECK (((period IS NULL) OR (NOT (lower_inf(period) OR upper_inf(period)))))
);

-- Name: holiday_questionnaire_answer; Type: TABLE; Schema: public

CREATE TABLE public.holiday_questionnaire_answer (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    modified_by uuid NOT NULL,
    questionnaire_id uuid NOT NULL,
    child_id uuid NOT NULL,
    fixed_period daterange,
    open_ranges daterange[] NOT NULL
);

-- Name: income; Type: TABLE; Schema: public

CREATE TABLE public.income (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    person_id uuid NOT NULL,
    data jsonb NOT NULL,
    valid_from date NOT NULL,
    valid_to date,
    notes text DEFAULT ''::text NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    effect public.income_effect DEFAULT 'INCOME'::public.income_effect NOT NULL,
    is_entrepreneur boolean DEFAULT false NOT NULL,
    works_at_echa boolean DEFAULT false NOT NULL,
    application_id uuid,
    modified_by uuid CONSTRAINT income_updated_by_not_null NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by uuid NOT NULL
);

-- Name: income_notification; Type: TABLE; Schema: public

CREATE TABLE public.income_notification (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    receiver_id uuid NOT NULL,
    notification_type public.income_notification_type NOT NULL
);

-- Name: income_statement; Type: TABLE; Schema: public

CREATE TABLE public.income_statement (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    person_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date,
    type public.income_statement_type NOT NULL,
    gross_income_source public.income_source,
    gross_estimated_monthly_income integer,
    gross_other_income public.other_income_type[],
    entrepreneur_full_time boolean,
    start_of_entrepreneurship date,
    spouse_works_in_company boolean,
    startup_grant boolean,
    self_employed_attachments boolean,
    self_employed_estimated_monthly_income integer,
    self_employed_income_start_date date,
    self_employed_income_end_date date,
    limited_company_income_source public.income_source,
    partnership boolean,
    light_entrepreneur boolean,
    accountant_name text,
    accountant_address text,
    accountant_phone text,
    accountant_email text,
    student boolean,
    alimony_payer boolean,
    other_info text,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT income_statement_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT income_statement_updated_not_null NOT NULL,
    handler_id uuid,
    checkup_consent boolean,
    gross_other_income_info text DEFAULT ''::text NOT NULL,
    handler_note text DEFAULT ''::text NOT NULL,
    status public.income_statement_status NOT NULL,
    created_by uuid NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    sent_at timestamp with time zone,
    handled_at timestamp with time zone,
    company_name text NOT NULL,
    business_id text NOT NULL,
    gross_no_income_description text,
    CONSTRAINT income_statement_status_check CHECK ((((status = 'HANDLED'::public.income_statement_status) = (handler_id IS NOT NULL)) AND ((status = 'HANDLED'::public.income_statement_status) = (handled_at IS NOT NULL)) AND ((status = 'DRAFT'::public.income_statement_status) = (sent_at IS NULL))))
);

-- Name: invoice; Type: TABLE; Schema: public

CREATE TABLE public.invoice (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    number bigint,
    invoice_date date NOT NULL,
    due_date date NOT NULL,
    print_date date,
    period_start date NOT NULL,
    period_end date NOT NULL,
    head_of_family uuid NOT NULL,
    sent_at timestamp with time zone,
    sent_by uuid,
    created_at timestamp with time zone DEFAULT now(),
    codebtor uuid,
    area_id uuid NOT NULL,
    status public.invoice_status,
    revision_number integer NOT NULL,
    replaced_invoice_id uuid,
    replacement_reason public.invoice_replacement_reason,
    replacement_notes text,
    CONSTRAINT "check$revision_number_non_negative" CHECK ((revision_number >= 0))
);

-- Name: invoice_correction; Type: TABLE; Schema: public

CREATE TABLE public.invoice_correction (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT invoice_correction_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT invoice_correction_updated_not_null NOT NULL,
    head_of_family_id uuid NOT NULL,
    child_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    product text NOT NULL,
    period daterange NOT NULL,
    amount integer NOT NULL,
    unit_price integer NOT NULL,
    description text NOT NULL,
    note text NOT NULL,
    target_month date,
    created_by uuid NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    modified_by uuid NOT NULL,
    CONSTRAINT "check$invoice_correction_target_month" CHECK (((target_month IS NULL) OR (EXTRACT(day FROM target_month) = (1)::numeric)))
);

-- Name: invoice_row; Type: TABLE; Schema: public

CREATE TABLE public.invoice_row (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    invoice_id uuid NOT NULL,
    child uuid NOT NULL,
    amount integer NOT NULL,
    unit_price integer,
    price integer,
    period_start date NOT NULL,
    period_end date NOT NULL,
    product text NOT NULL,
    saved_cost_center text,
    saved_sub_cost_center text,
    description text NOT NULL,
    unit_id uuid NOT NULL,
    correction_id uuid,
    idx smallint NOT NULL
);

-- Name: invoiced_fee_decision; Type: TABLE; Schema: public

CREATE TABLE public.invoiced_fee_decision (
    invoice_id uuid NOT NULL,
    fee_decision_id uuid NOT NULL
);

-- Name: location_view; Type: VIEW; Schema: public

CREATE VIEW public.location_view AS
 SELECT id,
    name,
    type,
    care_area_id,
    phone,
    street_address,
    postal_code,
    post_office,
    mailing_street_address,
    mailing_po_box,
    mailing_postal_code,
    mailing_post_office,
    location,
    url,
    provider_type,
    language,
    daycare_apply_period,
    preschool_apply_period,
    club_apply_period
   FROM public.daycare;

-- Name: meal_texture; Type: TABLE; Schema: public

CREATE TABLE public.meal_texture (
    id integer NOT NULL,
    name text NOT NULL
);

-- Name: message; Type: TABLE; Schema: public

CREATE TABLE public.message (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    sender_name text NOT NULL,
    content_id uuid CONSTRAINT message_content_id_not_null1 NOT NULL,
    thread_id uuid CONSTRAINT message_thread_id_not_null1 NOT NULL,
    sender_id uuid NOT NULL,
    sent_at timestamp with time zone,
    recipient_names text[] DEFAULT '{}'::text[] NOT NULL
);

-- Name: message_account; Type: TABLE; Schema: public

CREATE TABLE public.message_account (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    daycare_group_id uuid,
    employee_id uuid,
    person_id uuid,
    active boolean DEFAULT true,
    type public.message_account_type,
    CONSTRAINT "check$message_account_foreign_keys" CHECK ((((type = 'PERSONAL'::public.message_account_type) AND (employee_id IS NOT NULL) AND (daycare_group_id IS NULL) AND (person_id IS NULL)) OR ((type = 'GROUP'::public.message_account_type) AND (employee_id IS NULL) AND (daycare_group_id IS NOT NULL) AND (person_id IS NULL)) OR ((type = 'CITIZEN'::public.message_account_type) AND (employee_id IS NULL) AND (daycare_group_id IS NULL) AND (person_id IS NOT NULL)) OR ((type <> 'PERSONAL'::public.message_account_type) AND (type <> 'GROUP'::public.message_account_type) AND (type <> 'CITIZEN'::public.message_account_type) AND (employee_id IS NULL) AND (daycare_group_id IS NULL) AND (person_id IS NULL))))
);

-- Name: message_account_view; Type: VIEW; Schema: public

CREATE VIEW public.message_account_view AS
 SELECT id,
    type,
        CASE type
            WHEN 'GROUP'::public.message_account_type THEN ( SELECT ((d.name || ' - '::text) || dg.name)
               FROM (public.daycare_group dg
                 JOIN public.daycare d ON ((dg.daycare_id = d.id)))
              WHERE (dg.id = acc.daycare_group_id))
            WHEN 'PERSONAL'::public.message_account_type THEN ( SELECT ((e.last_name || ' '::text) || COALESCE(e.preferred_first_name, e.first_name))
               FROM public.employee e
              WHERE (e.id = acc.employee_id))
            WHEN 'CITIZEN'::public.message_account_type THEN ( SELECT ((p.last_name || ' '::text) || p.first_name)
               FROM public.person p
              WHERE (p.id = acc.person_id))
            WHEN 'MUNICIPAL'::public.message_account_type THEN NULL::text
            WHEN 'SERVICE_WORKER'::public.message_account_type THEN NULL::text
            WHEN 'FINANCE'::public.message_account_type THEN NULL::text
            ELSE NULL::text
        END AS name,
    person_id
   FROM public.message_account acc;

-- Name: message_content; Type: TABLE; Schema: public

CREATE TABLE public.message_content (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    author_id uuid NOT NULL,
    content text NOT NULL
);

-- Name: message_draft; Type: TABLE; Schema: public

CREATE TABLE public.message_draft (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT message_draft_created_not_null NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT message_draft_updated_not_null NOT NULL,
    account_id uuid NOT NULL,
    title text DEFAULT ''::text NOT NULL,
    content text DEFAULT ''::text NOT NULL,
    recipient_names text[] DEFAULT '{}'::text[] NOT NULL,
    type public.message_type DEFAULT 'MESSAGE'::public.message_type NOT NULL,
    urgent boolean DEFAULT false NOT NULL,
    sensitive boolean DEFAULT false NOT NULL,
    modified_at timestamp with time zone NOT NULL,
    recipients jsonb DEFAULT '[]'::jsonb NOT NULL
);

-- Name: message_recipients; Type: TABLE; Schema: public

CREATE TABLE public.message_recipients (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    message_id uuid NOT NULL,
    recipient_id uuid NOT NULL,
    read_at timestamp with time zone,
    email_notification_sent_at timestamp with time zone
);

-- Name: message_thread; Type: TABLE; Schema: public

CREATE TABLE public.message_thread (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    message_type public.message_type NOT NULL,
    title text NOT NULL,
    urgent boolean DEFAULT false NOT NULL,
    is_copy boolean NOT NULL,
    application_id uuid,
    sensitive boolean NOT NULL
);

-- Name: message_thread_children; Type: TABLE; Schema: public

CREATE TABLE public.message_thread_children (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    thread_id uuid NOT NULL,
    child_id uuid NOT NULL
);

-- Name: message_thread_folder; Type: TABLE; Schema: public

CREATE TABLE public.message_thread_folder (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    name text DEFAULT ''::text NOT NULL,
    owner_id uuid NOT NULL
);

-- Name: message_thread_participant; Type: TABLE; Schema: public

CREATE TABLE public.message_thread_participant (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    thread_id uuid NOT NULL,
    participant_id uuid NOT NULL,
    last_message_timestamp timestamp with time zone NOT NULL,
    last_received_timestamp timestamp with time zone,
    last_sent_timestamp timestamp with time zone,
    folder_id uuid
);

-- Name: mobile_device_push_group; Type: TABLE; Schema: public

CREATE TABLE public.mobile_device_push_group (
    daycare_group uuid NOT NULL,
    device uuid NOT NULL,
    created_at timestamp with time zone
);

-- Name: mobile_device_push_subscription; Type: TABLE; Schema: public

CREATE TABLE public.mobile_device_push_subscription (
    device uuid NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    endpoint text NOT NULL,
    expires timestamp with time zone,
    auth_secret bytea NOT NULL,
    ecdh_key bytea NOT NULL
);

-- Name: nekku_customer; Type: TABLE; Schema: public

CREATE TABLE public.nekku_customer (
    number text NOT NULL,
    name text NOT NULL,
    customer_group text NOT NULL
);

-- Name: nekku_customer_type; Type: TABLE; Schema: public

CREATE TABLE public.nekku_customer_type (
    weekdays public.nekku_customer_weekday[],
    type text,
    customer_number text
);

-- Name: nekku_orders_report; Type: TABLE; Schema: public

CREATE TABLE public.nekku_orders_report (
    delivery_date date,
    daycare_id uuid,
    group_id uuid,
    meal_sku text,
    total_quantity integer,
    meal_time public.nekku_product_meal_time[],
    meal_type public.nekku_product_meal_type,
    meals_by_special_diet text[],
    nekku_order_info text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Name: nekku_product; Type: TABLE; Schema: public

CREATE TABLE public.nekku_product (
    sku text NOT NULL,
    name text NOT NULL,
    options_id text,
    meal_time public.nekku_product_meal_time[],
    meal_type public.nekku_product_meal_type,
    customer_types text[]
);

-- Name: nekku_special_diet; Type: TABLE; Schema: public

CREATE TABLE public.nekku_special_diet (
    id text NOT NULL,
    name text NOT NULL
);

-- Name: nekku_special_diet_choices; Type: TABLE; Schema: public

CREATE TABLE public.nekku_special_diet_choices (
    child_id uuid NOT NULL,
    diet_id text NOT NULL,
    field_id text NOT NULL,
    value text NOT NULL
);

-- Name: nekku_special_diet_field; Type: TABLE; Schema: public

CREATE TABLE public.nekku_special_diet_field (
    id text NOT NULL,
    name text NOT NULL,
    type public.nekku_special_diet_type NOT NULL,
    diet_id text
);

-- Name: nekku_special_diet_option; Type: TABLE; Schema: public

CREATE TABLE public.nekku_special_diet_option (
    weight integer NOT NULL,
    key text NOT NULL,
    value text NOT NULL,
    field_id text
);

-- Name: out_of_office; Type: TABLE; Schema: public

CREATE TABLE public.out_of_office (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    employee_id uuid NOT NULL,
    period daterange NOT NULL,
    CONSTRAINT period_not_null CHECK ((NOT (lower_inf(period) OR upper_inf(period))))
);

-- Name: pairing; Type: TABLE; Schema: public

CREATE TABLE public.pairing (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    unit_id uuid,
    expires timestamp with time zone NOT NULL,
    status public.pairing_status DEFAULT 'WAITING_CHALLENGE'::public.pairing_status NOT NULL,
    challenge_key text NOT NULL,
    response_key text,
    attempts integer DEFAULT 0 NOT NULL,
    mobile_device_id uuid,
    employee_id uuid,
    CONSTRAINT pairing_non_null_reference CHECK ((num_nonnulls(unit_id, employee_id) = 1))
);

-- Name: password_blacklist; Type: TABLE; Schema: public

CREATE TABLE public.password_blacklist (
    password text NOT NULL,
    source integer NOT NULL
);

-- Name: password_blacklist_source; Type: TABLE; Schema: public

CREATE TABLE public.password_blacklist_source (
    id integer NOT NULL,
    name text NOT NULL,
    imported_at timestamp with time zone NOT NULL
);

-- Name: password_blacklist_source_id_seq; Type: SEQUENCE; Schema: public

ALTER TABLE public.password_blacklist_source ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.password_blacklist_source_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

-- Name: payment; Type: TABLE; Schema: public

CREATE TABLE public.payment (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    unit_id uuid NOT NULL,
    unit_name text NOT NULL,
    unit_business_id text,
    unit_iban text,
    unit_provider_id text,
    period daterange NOT NULL,
    number bigint,
    amount integer NOT NULL,
    status public.payment_status NOT NULL,
    payment_date date,
    due_date date,
    sent_at timestamp with time zone,
    sent_by uuid,
    unit_partner_code text,
    CONSTRAINT "check$payment_state" CHECK (((status = ANY ('{DRAFT,CONFIRMED}'::public.payment_status[])) OR ((unit_business_id IS NOT NULL) AND (unit_business_id <> ''::text) AND (unit_iban IS NOT NULL) AND (unit_iban <> ''::text) AND (unit_provider_id IS NOT NULL) AND (unit_provider_id <> ''::text) AND (number IS NOT NULL) AND (payment_date IS NOT NULL) AND (due_date IS NOT NULL) AND (sent_at IS NOT NULL) AND (sent_by IS NOT NULL))))
);

-- Name: pedagogical_document; Type: TABLE; Schema: public

CREATE TABLE public.pedagogical_document (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    child_id uuid NOT NULL,
    description text NOT NULL,
    created_at timestamp with time zone DEFAULT now() CONSTRAINT pedagogical_document_created_not_null NOT NULL,
    created_by uuid NOT NULL,
    updated_at timestamp with time zone DEFAULT now() CONSTRAINT pedagogical_document_updated_not_null NOT NULL,
    modified_by uuid NOT NULL,
    email_sent boolean DEFAULT false,
    email_job_created_at timestamp without time zone,
    modified_at timestamp with time zone NOT NULL
);

-- Name: pedagogical_document_read; Type: TABLE; Schema: public

CREATE TABLE public.pedagogical_document_read (
    pedagogical_document_id uuid,
    person_id uuid,
    read_at timestamp without time zone
);

-- Name: person_acl_view; Type: VIEW; Schema: public

CREATE VIEW public.person_acl_view AS
 SELECT employee_child_daycare_acl.employee_id,
    employee_child_daycare_acl.child_id AS person_id,
    employee_child_daycare_acl.daycare_id,
    employee_child_daycare_acl.role
   FROM public.employee_child_daycare_acl(CURRENT_DATE) employee_child_daycare_acl(employee_id, child_id, daycare_id, role)
UNION ALL
 SELECT acl.employee_id,
    guardian.guardian_id AS person_id,
    acl.daycare_id,
    acl.role
   FROM (public.employee_child_daycare_acl(CURRENT_DATE) acl(employee_id, child_id, daycare_id, role)
     JOIN public.guardian USING (child_id))
UNION ALL
 SELECT acl.employee_id,
    fridge_child.head_of_child AS person_id,
    acl.daycare_id,
    acl.role
   FROM (public.employee_child_daycare_acl(CURRENT_DATE) acl(employee_id, child_id, daycare_id, role)
     JOIN public.fridge_child USING (child_id));

-- Name: person_email_verification; Type: TABLE; Schema: public

CREATE TABLE public.person_email_verification (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    person_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    email text NOT NULL,
    verification_code text NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    sent_at timestamp with time zone
);

-- Name: placement_draft; Type: TABLE; Schema: public

CREATE TABLE public.placement_draft (
    application_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by uuid NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    modified_at timestamp with time zone DEFAULT now() NOT NULL,
    modified_by uuid NOT NULL,
    start_date date NOT NULL
);

-- Name: preschool_term; Type: TABLE; Schema: public

CREATE TABLE public.preschool_term (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    finnish_preschool daterange NOT NULL,
    swedish_preschool daterange NOT NULL,
    extended_term daterange NOT NULL,
    application_period daterange NOT NULL,
    term_breaks datemultirange NOT NULL,
    CONSTRAINT "check$application_period_valid" CHECK ((NOT (lower_inf(application_period) OR upper_inf(application_period)))),
    CONSTRAINT "check$extended_term_valid" CHECK ((NOT (lower_inf(extended_term) OR upper_inf(extended_term)))),
    CONSTRAINT "check$finnish_preschool_contains_term_breaks" CHECK ((finnish_preschool @> term_breaks)),
    CONSTRAINT "check$finnish_preschool_valid" CHECK ((NOT (lower_inf(finnish_preschool) OR upper_inf(finnish_preschool)))),
    CONSTRAINT "check$swedish_preschool_valid" CHECK ((NOT (lower_inf(swedish_preschool) OR upper_inf(swedish_preschool)))),
    CONSTRAINT "check$term_breaks_valid" CHECK ((NOT (lower_inf(term_breaks) OR upper_inf(term_breaks)))),
    CONSTRAINT "preschool_term$extended_care_contain_finnish_preschool" CHECK ((extended_term @> finnish_preschool)),
    CONSTRAINT "preschool_term$extended_care_contain_swedish_preschool" CHECK ((extended_term @> swedish_preschool))
);

-- Name: primary_units_view; Type: VIEW; Schema: public

CREATE VIEW public.primary_units_view AS
 SELECT head_of_child,
    child_id,
    unit_id
   FROM ( SELECT fc.head_of_child,
            fc.child_id,
            ch.date_of_birth,
            pl.unit_id,
            row_number() OVER (PARTITION BY fc.head_of_child ORDER BY fc.conflict, ch.date_of_birth DESC, ch.id) AS rownum
           FROM ((public.fridge_child fc
             JOIN LATERAL ( SELECT pl_1.unit_id,
                    pl_1.start_date
                   FROM public.placement pl_1
                  WHERE ((pl_1.child_id = fc.child_id) AND (CURRENT_DATE <= pl_1.end_date))
                  ORDER BY pl_1.start_date
                 LIMIT 1) pl ON (true))
             JOIN public.person ch ON ((ch.id = fc.child_id)))
          WHERE (daterange(fc.start_date, fc.end_date, '[]'::text) @> GREATEST(CURRENT_DATE, pl.start_date))) data
  WHERE (rownum = 1);

-- Name: scheduled_tasks; Type: TABLE; Schema: public

CREATE TABLE public.scheduled_tasks (
    task_name text NOT NULL,
    task_instance text NOT NULL,
    task_data bytea,
    execution_time timestamp with time zone NOT NULL,
    picked boolean NOT NULL,
    picked_by text,
    last_success timestamp with time zone,
    last_failure timestamp with time zone,
    consecutive_failures integer,
    last_heartbeat timestamp with time zone,
    version bigint NOT NULL,
    priority smallint
);

-- Name: service_application; Type: TABLE; Schema: public

CREATE TABLE public.service_application (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    sent_at timestamp with time zone NOT NULL,
    person_id uuid NOT NULL,
    child_id uuid NOT NULL,
    start_date date NOT NULL,
    service_need_option_id uuid NOT NULL,
    additional_info text NOT NULL,
    decision_status public.service_application_decision_status,
    decided_by uuid,
    decided_at timestamp with time zone,
    rejected_reason text,
    CONSTRAINT service_application_check CHECK (((decided_by IS NULL) = (decision_status IS NULL))),
    CONSTRAINT service_application_check1 CHECK (((decided_at IS NULL) = (decision_status IS NULL))),
    CONSTRAINT service_application_check2 CHECK (((rejected_reason IS NOT NULL) = (decision_status = 'REJECTED'::public.service_application_decision_status)))
);

-- Name: service_need; Type: TABLE; Schema: public

CREATE TABLE public.service_need (
    id uuid DEFAULT ext.uuid_generate_v1mc() CONSTRAINT new_service_need_id_not_null NOT NULL,
    created timestamp with time zone DEFAULT now() CONSTRAINT new_service_need_created_not_null NOT NULL,
    updated timestamp with time zone DEFAULT now() CONSTRAINT new_service_need_updated_not_null NOT NULL,
    option_id uuid CONSTRAINT new_service_need_option_id_not_null NOT NULL,
    placement_id uuid CONSTRAINT new_service_need_placement_id_not_null NOT NULL,
    start_date date CONSTRAINT new_service_need_start_date_not_null NOT NULL,
    end_date date CONSTRAINT new_service_need_end_date_not_null NOT NULL,
    confirmed_by uuid,
    confirmed_at timestamp with time zone,
    shift_care public.shift_care_type DEFAULT 'NONE'::public.shift_care_type CONSTRAINT service_need_shift_care_temp_not_null NOT NULL,
    part_week boolean NOT NULL,
    CONSTRAINT start_before_end CHECK ((start_date <= end_date))
);

-- Name: service_need_option; Type: TABLE; Schema: public

CREATE TABLE public.service_need_option (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    name_fi text CONSTRAINT service_need_option_name_not_null NOT NULL,
    valid_placement_type public.placement_type NOT NULL,
    fee_coefficient numeric(6,2) NOT NULL,
    occupancy_coefficient numeric(4,2) NOT NULL,
    part_day boolean NOT NULL,
    part_week boolean,
    daycare_hours_per_week integer NOT NULL,
    default_option boolean DEFAULT false NOT NULL,
    fee_description_fi text NOT NULL,
    fee_description_sv text NOT NULL,
    voucher_value_description_fi text NOT NULL,
    voucher_value_description_sv text NOT NULL,
    display_order integer,
    name_sv text NOT NULL,
    name_en text NOT NULL,
    contract_days_per_month integer,
    occupancy_coefficient_under_3y numeric(4,2) NOT NULL,
    show_for_citizen boolean DEFAULT true NOT NULL,
    realized_occupancy_coefficient numeric(4,2) NOT NULL,
    realized_occupancy_coefficient_under_3y numeric(4,2) CONSTRAINT service_need_option_realized_occupancy_coefficient_und_not_null NOT NULL,
    daycare_hours_per_month integer,
    valid_from date NOT NULL,
    valid_to date,
    CONSTRAINT "check$no_contract_days_and_hours" CHECK (((contract_days_per_month IS NULL) OR (daycare_hours_per_month IS NULL))),
    CONSTRAINT start_before_end CHECK (((valid_to IS NULL) OR (valid_to >= valid_from)))
);

-- Name: service_need_option_fee; Type: TABLE; Schema: public

CREATE TABLE public.service_need_option_fee (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    service_need_option_id uuid NOT NULL,
    validity daterange NOT NULL,
    base_fee integer NOT NULL,
    sibling_discount_2 numeric(5,4) NOT NULL,
    sibling_fee_2 integer NOT NULL,
    sibling_discount_2_plus numeric(5,4) NOT NULL,
    sibling_fee_2_plus integer NOT NULL
);

-- Name: service_need_option_voucher_value; Type: TABLE; Schema: public

CREATE TABLE public.service_need_option_voucher_value (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    service_need_option_id uuid CONSTRAINT service_need_option_voucher_val_service_need_option_id_not_null NOT NULL,
    validity daterange NOT NULL,
    base_value integer NOT NULL,
    coefficient numeric(3,2) CONSTRAINT service_need_option_voucher_value_coefficient_not_null1 NOT NULL,
    value integer NOT NULL,
    base_value_under_3y integer NOT NULL,
    coefficient_under_3y numeric(3,2) NOT NULL,
    value_under_3y integer NOT NULL
);

-- Name: setting; Type: TABLE; Schema: public

CREATE TABLE public.setting (
    key public.setting_type NOT NULL,
    value text NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL
);

-- Name: sfi_get_events_continuation_token; Type: TABLE; Schema: public

CREATE TABLE public.sfi_get_events_continuation_token (
    continuation_token text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- Name: sfi_message; Type: TABLE; Schema: public

CREATE TABLE public.sfi_message (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    sfi_id integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    guardian_id uuid NOT NULL,
    decision_id uuid,
    document_id uuid,
    fee_decision_id uuid,
    voucher_value_decision_id uuid,
    CONSTRAINT single_type CHECK ((num_nonnulls(decision_id, document_id, fee_decision_id, voucher_value_decision_id) = 1))
);

-- Name: sfi_message_event; Type: TABLE; Schema: public

CREATE TABLE public.sfi_message_event (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    message_id uuid NOT NULL,
    event_type public.sfi_message_event_type NOT NULL
);

-- Name: special_diet; Type: TABLE; Schema: public

CREATE TABLE public.special_diet (
    id integer NOT NULL,
    abbreviation text NOT NULL
);

-- Name: staff_attendance; Type: TABLE; Schema: public

CREATE TABLE public.staff_attendance (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    group_id uuid NOT NULL,
    date date NOT NULL,
    count numeric NOT NULL,
    created timestamp with time zone DEFAULT now(),
    updated timestamp with time zone DEFAULT now() NOT NULL
);

-- Name: staff_attendance_external; Type: TABLE; Schema: public

CREATE TABLE public.staff_attendance_external (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    name text NOT NULL,
    group_id uuid NOT NULL,
    arrived timestamp with time zone NOT NULL,
    departed timestamp with time zone,
    occupancy_coefficient numeric(4,2) NOT NULL,
    departed_automatically boolean DEFAULT false NOT NULL,
    CONSTRAINT staff_attendance_external_start_before_end CHECK ((arrived < departed))
);

-- Name: staff_attendance_plan; Type: TABLE; Schema: public

CREATE TABLE public.staff_attendance_plan (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    employee_id uuid NOT NULL,
    type public.staff_attendance_type DEFAULT 'PRESENT'::public.staff_attendance_type NOT NULL,
    start_time timestamp with time zone NOT NULL,
    end_time timestamp with time zone NOT NULL,
    description text,
    CONSTRAINT staff_attendance_plan_start_before_end CHECK ((start_time < end_time))
);

-- Name: staff_attendance_realtime; Type: TABLE; Schema: public

CREATE TABLE public.staff_attendance_realtime (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    employee_id uuid NOT NULL,
    group_id uuid,
    arrived timestamp with time zone NOT NULL,
    departed timestamp with time zone,
    occupancy_coefficient numeric(4,2) NOT NULL,
    type public.staff_attendance_type DEFAULT 'PRESENT'::public.staff_attendance_type NOT NULL,
    departed_automatically boolean DEFAULT false NOT NULL,
    arrived_added_at timestamp with time zone,
    arrived_added_by uuid,
    arrived_modified_at timestamp with time zone,
    arrived_modified_by uuid,
    departed_added_at timestamp with time zone,
    departed_added_by uuid,
    departed_modified_at timestamp with time zone,
    departed_modified_by uuid,
    CONSTRAINT check_group_id_if_working_in_group CHECK (((group_id IS NOT NULL) OR (type = ANY ('{TRAINING,OTHER_WORK,SICKNESS,CHILD_SICKNESS}'::public.staff_attendance_type[])))),
    CONSTRAINT staff_attendance_start_before_end CHECK ((arrived < departed))
);

-- Name: staff_occupancy_coefficient; Type: TABLE; Schema: public

CREATE TABLE public.staff_occupancy_coefficient (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now(),
    employee_id uuid NOT NULL,
    daycare_id uuid NOT NULL,
    coefficient numeric(4,2) NOT NULL
);

-- Name: system_notification; Type: TABLE; Schema: public

CREATE TABLE public.system_notification (
    target_group public.system_notification_target_group NOT NULL,
    text text NOT NULL,
    valid_to timestamp with time zone NOT NULL,
    text_sv text,
    text_en text,
    CONSTRAINT notification_i18n CHECK (
CASE
    WHEN (target_group = 'CITIZENS'::public.system_notification_target_group) THEN ((text_sv IS NOT NULL) AND (text_en IS NOT NULL))
    WHEN (target_group = 'EMPLOYEES'::public.system_notification_target_group) THEN ((text_sv IS NULL) AND (text_en IS NULL))
    ELSE NULL::boolean
END),
    CONSTRAINT system_notification_text_check CHECK ((text <> ''::text)),
    CONSTRAINT system_notification_text_en_check CHECK ((text_en <> ''::text)),
    CONSTRAINT system_notification_text_sv_check CHECK ((text_sv <> ''::text))
);

-- Name: titania_errors; Type: TABLE; Schema: public

CREATE TABLE public.titania_errors (
    request_time timestamp with time zone NOT NULL,
    employee_id uuid NOT NULL,
    shift_date date NOT NULL,
    shift_begins time without time zone NOT NULL,
    shift_ends time without time zone NOT NULL,
    overlapping_shift_begins time without time zone NOT NULL,
    overlapping_shift_ends time without time zone NOT NULL,
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL
);

-- Name: vapid_jwt; Type: TABLE; Schema: public

CREATE TABLE public.vapid_jwt (
    origin text NOT NULL,
    public_key bytea NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    jwt text NOT NULL,
    expires_at timestamp with time zone NOT NULL
);

-- Name: varda_state; Type: TABLE; Schema: public

CREATE TABLE public.varda_state (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    child_id uuid NOT NULL,
    state jsonb,
    last_success_at timestamp with time zone,
    errored_at timestamp with time zone,
    error text,
    CONSTRAINT "check$error_nulls" CHECK (((errored_at IS NULL) = (error IS NULL)))
);

-- Name: varda_unit; Type: TABLE; Schema: public

CREATE TABLE public.varda_unit (
    evaka_daycare_id uuid NOT NULL,
    last_success_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    errored_at timestamp with time zone,
    error text,
    state jsonb,
    CONSTRAINT "check$error_nulls" CHECK (((errored_at IS NULL) = (error IS NULL)))
);

-- Name: voucher_value_decision; Type: TABLE; Schema: public

CREATE TABLE public.voucher_value_decision (
    id uuid NOT NULL,
    status public.voucher_value_decision_status NOT NULL,
    valid_from date NOT NULL,
    valid_to date NOT NULL,
    decision_number bigint,
    head_of_family_id uuid CONSTRAINT voucher_value_decision_head_of_family_not_null NOT NULL,
    partner_id uuid,
    head_of_family_income jsonb,
    partner_income jsonb,
    family_size integer NOT NULL,
    fee_thresholds jsonb CONSTRAINT voucher_value_decision_pricing_not_null NOT NULL,
    document_key text,
    created timestamp with time zone DEFAULT now(),
    approved_by uuid,
    approved_at timestamp with time zone,
    sent_at timestamp with time zone,
    cancelled_at timestamp with time zone,
    decision_handler uuid,
    child_id uuid CONSTRAINT voucher_value_decision_child_not_null NOT NULL,
    child_date_of_birth date CONSTRAINT voucher_value_decision_date_of_birth_not_null NOT NULL,
    base_co_payment integer NOT NULL,
    sibling_discount integer NOT NULL,
    placement_unit_id uuid,
    placement_type public.placement_type,
    co_payment integer NOT NULL,
    fee_alterations jsonb NOT NULL,
    base_value integer NOT NULL,
    voucher_value integer NOT NULL,
    final_co_payment integer NOT NULL,
    service_need_fee_coefficient numeric(4,2),
    service_need_voucher_value_coefficient numeric(4,2),
    service_need_fee_description_fi text,
    service_need_fee_description_sv text,
    service_need_voucher_value_description_fi text,
    service_need_voucher_value_description_sv text,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    assistance_need_coefficient numeric(4,2) CONSTRAINT voucher_value_decision_capacity_factor_not_null NOT NULL,
    decision_type public.voucher_value_decision_type DEFAULT 'NORMAL'::public.voucher_value_decision_type NOT NULL,
    annulled_at timestamp with time zone,
    validity_updated_at timestamp with time zone,
    child_income jsonb,
    difference public.voucher_value_decision_difference[] NOT NULL,
    service_need_missing boolean NOT NULL,
    document_contains_contact_info boolean DEFAULT false NOT NULL,
    process_id uuid,
    archived_at timestamp with time zone,
    CONSTRAINT "check$head_of_family_is_not_partner" CHECK (((partner_id IS NULL) OR (head_of_family_id <> partner_id))),
    CONSTRAINT "check$voucher_value_gte_co_payment" CHECK ((voucher_value >= co_payment)),
    CONSTRAINT non_empty_voucher_value_decisions CHECK (((0 = num_nonnulls(placement_unit_id, placement_type, service_need_fee_coefficient, service_need_voucher_value_coefficient, service_need_fee_description_fi, service_need_fee_description_sv, service_need_voucher_value_description_fi, service_need_voucher_value_description_sv)) OR (8 = num_nonnulls(placement_unit_id, placement_type, service_need_fee_coefficient, service_need_voucher_value_coefficient, service_need_fee_description_fi, service_need_fee_description_sv, service_need_voucher_value_description_fi, service_need_voucher_value_description_sv))))
);

-- Name: voucher_value_decision_number_sequence; Type: SEQUENCE; Schema: public

CREATE SEQUENCE public.voucher_value_decision_number_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Name: voucher_value_report_decision; Type: TABLE; Schema: public

CREATE TABLE public.voucher_value_report_decision (
    id uuid DEFAULT ext.uuid_generate_v1mc() CONSTRAINT voucher_value_report_decision_part_id_not_null NOT NULL,
    voucher_value_report_snapshot_id uuid CONSTRAINT voucher_value_report_decisi_voucher_value_report_snaps_not_null NOT NULL,
    realized_amount integer CONSTRAINT voucher_value_report_decision_part_realized_amount_not_null NOT NULL,
    realized_period daterange CONSTRAINT voucher_value_report_decision_part_realized_period_not_null NOT NULL,
    type public.voucher_report_row_type,
    decision_id uuid NOT NULL,
    CONSTRAINT negative_refund_else_positive CHECK ((((type = 'REFUND'::public.voucher_report_row_type) AND (realized_amount <= 0)) OR (realized_amount >= 0))),
    CONSTRAINT single_month_periods CHECK (((EXTRACT(year FROM lower(realized_period)) = EXTRACT(year FROM (upper(realized_period) - '1 day'::interval))) AND (EXTRACT(month FROM lower(realized_period)) = EXTRACT(month FROM (upper(realized_period) - '1 day'::interval)))))
);

-- Name: voucher_value_report_snapshot; Type: TABLE; Schema: public

CREATE TABLE public.voucher_value_report_snapshot (
    id uuid DEFAULT ext.uuid_generate_v1mc() NOT NULL,
    created timestamp with time zone DEFAULT now(),
    month integer NOT NULL,
    year integer NOT NULL,
    taken_at timestamp with time zone NOT NULL
);

-- Name: decision number; Type: DEFAULT; Schema: public

ALTER TABLE ONLY public.decision ALTER COLUMN number SET DEFAULT nextval('public.decision_number_seq'::regclass);

-- Name: absence_application absence_application_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence_application
    ADD CONSTRAINT absence_application_pkey PRIMARY KEY (id);

-- Name: application_other_guardian application_other_guardian_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application_other_guardian
    ADD CONSTRAINT application_other_guardian_pkey PRIMARY KEY (application_id, guardian_id);

-- Name: application application_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_pkey PRIMARY KEY (id);

-- Name: case_process archived_process_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.case_process
    ADD CONSTRAINT archived_process_pkey PRIMARY KEY (id);

-- Name: assistance_action assistance_action_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_action
    ADD CONSTRAINT assistance_action_no_overlap EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);

-- Name: assistance_action_option assistance_action_option_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_action_option
    ADD CONSTRAINT assistance_action_option_pkey PRIMARY KEY (id);

-- Name: assistance_action_option_ref assistance_action_option_ref_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_action_option_ref
    ADD CONSTRAINT assistance_action_option_ref_pkey PRIMARY KEY (action_id, option_id);

-- Name: assistance_action assistance_action_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_action
    ADD CONSTRAINT assistance_action_pkey PRIMARY KEY (id);

-- Name: assistance_factor assistance_factor_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_factor
    ADD CONSTRAINT assistance_factor_pkey PRIMARY KEY (id);

-- Name: assistance_need_decision_guardian assistance_need_decision_guardian_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision_guardian
    ADD CONSTRAINT assistance_need_decision_guardian_pkey PRIMARY KEY (id);

-- Name: assistance_need_decision assistance_need_decision_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision
    ADD CONSTRAINT assistance_need_decision_pkey PRIMARY KEY (id);

-- Name: assistance_need_preschool_decision_guardian assistance_need_preschool_decision_guardian_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision_guardian
    ADD CONSTRAINT assistance_need_preschool_decision_guardian_pkey PRIMARY KEY (id);

-- Name: assistance_need_preschool_decision assistance_need_preschool_decision_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision
    ADD CONSTRAINT assistance_need_preschool_decision_pkey PRIMARY KEY (id);

-- Name: assistance_need_voucher_coefficient assistance_need_voucher_coefficient_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_voucher_coefficient
    ADD CONSTRAINT assistance_need_voucher_coefficient_pkey PRIMARY KEY (id);

-- Name: async_job_work_permit async_job_work_permit_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.async_job_work_permit
    ADD CONSTRAINT async_job_work_permit_pkey PRIMARY KEY (pool_id);

-- Name: attachment attachment_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT attachment_pkey PRIMARY KEY (id);

-- Name: attendance_reservation attendance_reservation_no_overlap_with_times; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attendance_reservation
    ADD CONSTRAINT attendance_reservation_no_overlap_with_times EXCLUDE USING gist (child_id WITH =, tsrange((date + start_time), (date + end_time)) WITH &&) WHERE (((start_time IS NOT NULL) AND (end_time IS NOT NULL)));

-- Name: attendance_reservation attendance_reservation_no_overlap_without_times; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attendance_reservation
    ADD CONSTRAINT attendance_reservation_no_overlap_without_times EXCLUDE USING btree (child_id WITH =, date WITH =) WHERE (((start_time IS NULL) AND (end_time IS NULL)));

-- Name: attendance_reservation attendance_reservation_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attendance_reservation
    ADD CONSTRAINT attendance_reservation_pkey PRIMARY KEY (id);

-- Name: calendar_event_attendee calendar_event_attendee_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_attendee
    ADD CONSTRAINT calendar_event_attendee_pkey PRIMARY KEY (id);

-- Name: calendar_event calendar_event_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event
    ADD CONSTRAINT calendar_event_pkey PRIMARY KEY (id);

-- Name: calendar_event_time calendar_event_time_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_time
    ADD CONSTRAINT calendar_event_time_pkey PRIMARY KEY (id);

-- Name: care_area care_area_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.care_area
    ADD CONSTRAINT care_area_pkey PRIMARY KEY (id);

-- Name: care_area care_area_short_name_unique; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.care_area
    ADD CONSTRAINT care_area_short_name_unique UNIQUE (short_name);

-- Name: assistance_need_decision check$assistance_need_decision_no_validity_period_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision
    ADD CONSTRAINT "check$assistance_need_decision_no_validity_period_overlap" EXCLUDE USING gist (child_id WITH =, validity_period WITH &&) WHERE ((status = 'ACCEPTED'::public.assistance_need_decision_status));

-- Name: assistance_need_preschool_decision check$assistance_need_preschool_decision_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision
    ADD CONSTRAINT "check$assistance_need_preschool_decision_no_overlap" EXCLUDE USING gist (child_id WITH =, daterange(valid_from, valid_to, '[]'::text) WITH &&) WHERE ((status = 'ACCEPTED'::public.assistance_need_decision_status));

-- Name: assistance_need_voucher_coefficient check$no_validity_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_voucher_coefficient
    ADD CONSTRAINT "check$no_validity_overlap" EXCLUDE USING gist (child_id WITH =, validity_period WITH &&);

-- Name: daily_service_time check$no_validity_period_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daily_service_time
    ADD CONSTRAINT "check$no_validity_period_overlap" EXCLUDE USING gist (child_id WITH =, validity_period WITH &&);

-- Name: child_attendance child_attendance_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_attendance
    ADD CONSTRAINT child_attendance_no_overlap EXCLUDE USING gist (child_id WITH =, tsrange((date + start_time), (date + end_time)) WITH &&);

-- Name: child_attendance child_attendance_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_attendance
    ADD CONSTRAINT child_attendance_pkey PRIMARY KEY (id);

-- Name: child_document_decision child_document_decision_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_decision
    ADD CONSTRAINT child_document_decision_pkey PRIMARY KEY (id);

-- Name: child_document child_document_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_pkey PRIMARY KEY (id);

-- Name: child_document_published_version child_document_published_version_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_published_version
    ADD CONSTRAINT child_document_published_version_pkey PRIMARY KEY (id);

-- Name: child_document_read child_document_read_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_read
    ADD CONSTRAINT child_document_read_pkey PRIMARY KEY (document_id, person_id);

-- Name: child_images child_images_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_images
    ADD CONSTRAINT child_images_pkey PRIMARY KEY (id);

-- Name: child child_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child
    ADD CONSTRAINT child_pkey PRIMARY KEY (id);

-- Name: child_sticky_note child_sticky_note_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_sticky_note
    ADD CONSTRAINT child_sticky_note_pkey PRIMARY KEY (id);

-- Name: citizen_user citizen_user_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.citizen_user
    ADD CONSTRAINT citizen_user_pkey PRIMARY KEY (id);

-- Name: club_term club_term$no_overlaps; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.club_term
    ADD CONSTRAINT "club_term$no_overlaps" EXCLUDE USING gist (term WITH &&);

-- Name: club_term club_term_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.club_term
    ADD CONSTRAINT club_term_pkey PRIMARY KEY (id);

-- Name: daily_service_time_notification daily_service_time_notification_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daily_service_time_notification
    ADD CONSTRAINT daily_service_time_notification_pkey PRIMARY KEY (id);

-- Name: daily_service_time daily_service_time_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daily_service_time
    ADD CONSTRAINT daily_service_time_pkey PRIMARY KEY (id);

-- Name: daycare_assistance daycare_assistance_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_assistance
    ADD CONSTRAINT daycare_assistance_pkey PRIMARY KEY (id);

-- Name: daycare_caretaker daycare_caretaker_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_caretaker
    ADD CONSTRAINT daycare_caretaker_no_overlap EXCLUDE USING gist (group_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);

-- Name: daycare_caretaker daycare_caretaker_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_caretaker
    ADD CONSTRAINT daycare_caretaker_pkey PRIMARY KEY (id);

-- Name: daycare_group daycare_group_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_group
    ADD CONSTRAINT daycare_group_pkey PRIMARY KEY (id);

-- Name: daycare_group_placement daycare_group_placement_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_group_placement
    ADD CONSTRAINT daycare_group_placement_pkey PRIMARY KEY (id);

-- Name: daycare daycare_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare
    ADD CONSTRAINT daycare_pkey PRIMARY KEY (id);

-- Name: document_template document_template_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.document_template
    ADD CONSTRAINT document_template_pkey PRIMARY KEY (id);

-- Name: dvv_modification_token dvv_modification_token_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.dvv_modification_token
    ADD CONSTRAINT dvv_modification_token_pkey PRIMARY KEY (token);

-- Name: employee employee_employee_number_key; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_employee_number_key UNIQUE (employee_number);

-- Name: employee_pin employee_pin_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.employee_pin
    ADD CONSTRAINT employee_pin_pkey PRIMARY KEY (id);

-- Name: employee employee_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_pkey PRIMARY KEY (id);

-- Name: evaka_user evaka_user_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.evaka_user
    ADD CONSTRAINT evaka_user_pkey PRIMARY KEY (id);

-- Name: assistance_factor exclude$assistance_factor_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_factor
    ADD CONSTRAINT "exclude$assistance_factor_no_overlap" EXCLUDE USING gist (child_id WITH =, valid_during WITH &&);

-- Name: backup_care exclude$backup_care_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "exclude$backup_care_no_overlap" EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);

-- Name: daycare_assistance exclude$daycare_assistance_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_assistance
    ADD CONSTRAINT "exclude$daycare_assistance_no_overlap" EXCLUDE USING gist (child_id WITH =, valid_during WITH &&);

-- Name: daycare_group_placement exclude$daycare_group_placement_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_group_placement
    ADD CONSTRAINT "exclude$daycare_group_placement_no_overlap" EXCLUDE USING gist (daycare_placement_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);

-- Name: fee_decision exclude$fee_decision_no_overlapping_draft; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT "exclude$fee_decision_no_overlapping_draft" EXCLUDE USING gist (head_of_family_id WITH =, valid_during WITH &&) WHERE ((status = 'DRAFT'::public.fee_decision_status));

-- Name: fee_decision exclude$fee_decision_no_overlapping_sent; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT "exclude$fee_decision_no_overlapping_sent" EXCLUDE USING gist (head_of_family_id WITH =, valid_during WITH &&) WHERE ((status = ANY ('{SENT,WAITING_FOR_SENDING,WAITING_FOR_MANUAL_SENDING}'::public.fee_decision_status[])));

-- Name: fridge_child exclude$fridge_child_no_overlapping_head_of_child; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT "exclude$fridge_child_no_overlapping_head_of_child" EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&) WHERE ((conflict = false));

-- Name: fridge_partner exclude$fridge_partner_enforce_monogamy; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT "exclude$fridge_partner_enforce_monogamy" EXCLUDE USING gist (person_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&) WHERE ((conflict = false));

-- Name: income exclude$income_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income
    ADD CONSTRAINT "exclude$income_no_overlap" EXCLUDE USING gist (person_id WITH =, daterange(valid_from, COALESCE(valid_to, '2099-12-31'::date), '[]'::text) WITH &&);

-- Name: foster_parent exclude$no_overlaps; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.foster_parent
    ADD CONSTRAINT "exclude$no_overlaps" EXCLUDE USING gist (child_id WITH =, parent_id WITH =, valid_during WITH &&);

-- Name: other_assistance_measure exclude$other_assistance_measure_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.other_assistance_measure
    ADD CONSTRAINT "exclude$other_assistance_measure_no_overlap" EXCLUDE USING gist (child_id WITH =, type WITH =, valid_during WITH &&);

-- Name: placement exclude$placement_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT "exclude$placement_no_overlap" EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);

-- Name: preschool_assistance exclude$preschool_assistance_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.preschool_assistance
    ADD CONSTRAINT "exclude$preschool_assistance_no_overlap" EXCLUDE USING gist (child_id WITH =, valid_during WITH &&);

-- Name: service_need exclude$service_need_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need
    ADD CONSTRAINT "exclude$service_need_no_overlap" EXCLUDE USING gist (placement_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&) DEFERRABLE INITIALLY DEFERRED;

-- Name: voucher_value_decision exclude$voucher_value_decision_no_overlapping_draft; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT "exclude$voucher_value_decision_no_overlapping_draft" EXCLUDE USING gist (child_id WITH =, daterange(valid_from, valid_to, '[]'::text) WITH &&) WHERE ((status = 'DRAFT'::public.voucher_value_decision_status));

-- Name: voucher_value_decision exclude$voucher_value_decision_no_overlapping_sent; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT "exclude$voucher_value_decision_no_overlapping_sent" EXCLUDE USING gist (child_id WITH =, daterange(valid_from, valid_to, '[]'::text) WITH &&) WHERE ((status = ANY ('{SENT,WAITING_FOR_SENDING,WAITING_FOR_MANUAL_SENDING}'::public.voucher_value_decision_status[])));

-- Name: family_contact family_contact_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.family_contact
    ADD CONSTRAINT family_contact_pkey PRIMARY KEY (id);

-- Name: fee_thresholds fee_thresholds_no_overlap; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_thresholds
    ADD CONSTRAINT fee_thresholds_no_overlap EXCLUDE USING gist (valid_during WITH &&);

-- Name: fee_thresholds fee_thresholds_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_thresholds
    ADD CONSTRAINT fee_thresholds_pkey PRIMARY KEY (id);

-- Name: finance_note finance_note_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.finance_note
    ADD CONSTRAINT finance_note_pkey PRIMARY KEY (id);

-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);

-- Name: foster_parent foster_parent_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.foster_parent
    ADD CONSTRAINT foster_parent_pkey PRIMARY KEY (id);

-- Name: fridge_child fridge_child_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT fridge_child_pkey PRIMARY KEY (id);

-- Name: fridge_partner fridge_partner_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT fridge_partner_pkey PRIMARY KEY (partnership_id, indx);

-- Name: group_note group_note_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.group_note
    ADD CONSTRAINT group_note_pkey PRIMARY KEY (id);

-- Name: holiday_period holiday_period_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.holiday_period
    ADD CONSTRAINT holiday_period_pkey PRIMARY KEY (id);

-- Name: holiday_period_questionnaire holiday_period_questionnaire_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.holiday_period_questionnaire
    ADD CONSTRAINT holiday_period_questionnaire_pkey PRIMARY KEY (id);

-- Name: holiday_questionnaire_answer holiday_questionnaire_answer_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.holiday_questionnaire_answer
    ADD CONSTRAINT holiday_questionnaire_answer_pkey PRIMARY KEY (id);

-- Name: income_notification income_notification_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income_notification
    ADD CONSTRAINT income_notification_pkey PRIMARY KEY (id);

-- Name: income_statement income_statement_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income_statement
    ADD CONSTRAINT income_statement_pkey PRIMARY KEY (id);

-- Name: invoice_correction invoice_correction_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_correction
    ADD CONSTRAINT invoice_correction_pkey PRIMARY KEY (id);

-- Name: invoiced_fee_decision invoiced_fee_decision_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoiced_fee_decision
    ADD CONSTRAINT invoiced_fee_decision_pkey PRIMARY KEY (invoice_id, fee_decision_id);

-- Name: meal_texture meal_texture_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.meal_texture
    ADD CONSTRAINT meal_texture_pkey PRIMARY KEY (id);

-- Name: message_account message_account_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_account
    ADD CONSTRAINT message_account_pkey PRIMARY KEY (id);

-- Name: message_content message_content_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_content
    ADD CONSTRAINT message_content_pkey PRIMARY KEY (id);

-- Name: message_draft message_draft_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_draft
    ADD CONSTRAINT message_draft_pkey PRIMARY KEY (id);

-- Name: message message_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pkey PRIMARY KEY (id);

-- Name: message_recipients message_recipients_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_recipients
    ADD CONSTRAINT message_recipients_pkey PRIMARY KEY (id);

-- Name: message_thread_children message_thread_children_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_children
    ADD CONSTRAINT message_thread_children_pkey PRIMARY KEY (id);

-- Name: message_thread_folder message_thread_folder_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_folder
    ADD CONSTRAINT message_thread_folder_pkey PRIMARY KEY (id);

-- Name: message_thread_participant message_thread_participant_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_participant
    ADD CONSTRAINT message_thread_participant_pkey PRIMARY KEY (id);

-- Name: message_thread message_thread_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread
    ADD CONSTRAINT message_thread_pkey PRIMARY KEY (id);

-- Name: mobile_device mobile_device_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.mobile_device
    ADD CONSTRAINT mobile_device_pkey PRIMARY KEY (id);

-- Name: mobile_device_push_group mobile_device_push_group_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.mobile_device_push_group
    ADD CONSTRAINT mobile_device_push_group_pkey PRIMARY KEY (daycare_group, device);

-- Name: mobile_device_push_subscription mobile_device_push_subscription_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.mobile_device_push_subscription
    ADD CONSTRAINT mobile_device_push_subscription_pkey PRIMARY KEY (device);

-- Name: nekku_customer nekku_customer_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_customer
    ADD CONSTRAINT nekku_customer_pkey PRIMARY KEY (number);

-- Name: nekku_product nekku_product_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_product
    ADD CONSTRAINT nekku_product_pkey PRIMARY KEY (sku);

-- Name: nekku_special_diet_field nekku_special_diet_field_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_special_diet_field
    ADD CONSTRAINT nekku_special_diet_field_pkey PRIMARY KEY (id);

-- Name: nekku_special_diet nekku_special_diet_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_special_diet
    ADD CONSTRAINT nekku_special_diet_pkey PRIMARY KEY (id);

-- Name: fee_decision_child new_fee_decision_child_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision_child
    ADD CONSTRAINT new_fee_decision_child_pkey PRIMARY KEY (id);

-- Name: fee_decision new_fee_decision_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT new_fee_decision_pkey PRIMARY KEY (id);

-- Name: service_need new_service_need_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need
    ADD CONSTRAINT new_service_need_pkey PRIMARY KEY (id);

-- Name: other_assistance_measure other_assistance_measure_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.other_assistance_measure
    ADD CONSTRAINT other_assistance_measure_pkey PRIMARY KEY (id);

-- Name: out_of_office out_of_office_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.out_of_office
    ADD CONSTRAINT out_of_office_pkey PRIMARY KEY (id);

-- Name: pairing pairing_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pairing
    ADD CONSTRAINT pairing_pkey PRIMARY KEY (id);

-- Name: fridge_partner partnership_end_date_matches; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT partnership_end_date_matches EXCLUDE USING gist (partnership_id WITH =, end_date WITH <>) DEFERRABLE INITIALLY DEFERRED;

-- Name: fridge_partner partnership_start_date_matches; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT partnership_start_date_matches EXCLUDE USING gist (partnership_id WITH =, start_date WITH <>) DEFERRABLE INITIALLY DEFERRED;

-- Name: password_blacklist password_blacklist_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.password_blacklist
    ADD CONSTRAINT password_blacklist_pkey PRIMARY KEY (password);

-- Name: password_blacklist_source password_blacklist_source_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.password_blacklist_source
    ADD CONSTRAINT password_blacklist_source_pkey PRIMARY KEY (id);

-- Name: payment payment_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT payment_pkey PRIMARY KEY (id);

-- Name: pedagogical_document pedagogical_document_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pedagogical_document
    ADD CONSTRAINT pedagogical_document_pkey PRIMARY KEY (id);

-- Name: holiday_period period$no_overlaps; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.holiday_period
    ADD CONSTRAINT "period$no_overlaps" EXCLUDE USING gist (period WITH &&);

-- Name: person_email_verification person_email_verification_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.person_email_verification
    ADD CONSTRAINT person_email_verification_pkey PRIMARY KEY (id);

-- Name: person person_identitynumber_key; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_identitynumber_key UNIQUE (social_security_number);

-- Name: person person_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_pkey PRIMARY KEY (id);

-- Name: absence pk$absence_id; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT "pk$absence_id" PRIMARY KEY (id);

-- Name: application_note pk$application_note; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application_note
    ADD CONSTRAINT "pk$application_note" PRIMARY KEY (id);

-- Name: async_job pk$async_job; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.async_job
    ADD CONSTRAINT "pk$async_job" PRIMARY KEY (id);

-- Name: backup_care pk$backup_care; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "pk$backup_care" PRIMARY KEY (id);

-- Name: backup_pickup pk$backup_pickup; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.backup_pickup
    ADD CONSTRAINT "pk$backup_pickup" PRIMARY KEY (id);

-- Name: child_daily_note pk$daycare_daily_note; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_daily_note
    ADD CONSTRAINT "pk$daycare_daily_note" PRIMARY KEY (id);

-- Name: decision pk$decision; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.decision
    ADD CONSTRAINT "pk$decision" PRIMARY KEY (id);

-- Name: fee_alteration pk$fee_alteration; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_alteration
    ADD CONSTRAINT "pk$fee_alteration" PRIMARY KEY (id);

-- Name: income pk$income; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income
    ADD CONSTRAINT "pk$income" PRIMARY KEY (id);

-- Name: invoice_row pk$invoice_row; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_row
    ADD CONSTRAINT "pk$invoice_row" PRIMARY KEY (id);

-- Name: invoice pk$invoices; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT "pk$invoices" PRIMARY KEY (id);

-- Name: koski_study_right pk$koski_study_right; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.koski_study_right
    ADD CONSTRAINT "pk$koski_study_right" PRIMARY KEY (id);

-- Name: placement pk$placement; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT "pk$placement" PRIMARY KEY (id);

-- Name: placement_plan pk$placement_plan; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement_plan
    ADD CONSTRAINT "pk$placement_plan" PRIMARY KEY (id);

-- Name: staff_attendance pk$staff_attendance_id; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT "pk$staff_attendance_id" PRIMARY KEY (id);

-- Name: voucher_value_decision pk$voucher_value_decision; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT "pk$voucher_value_decision" PRIMARY KEY (id);

-- Name: placement_draft placement_draft_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement_draft
    ADD CONSTRAINT placement_draft_pkey PRIMARY KEY (application_id);

-- Name: preschool_assistance preschool_assistance_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.preschool_assistance
    ADD CONSTRAINT preschool_assistance_pkey PRIMARY KEY (id);

-- Name: preschool_term preschool_term$no_overlaps; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.preschool_term
    ADD CONSTRAINT "preschool_term$no_overlaps" EXCLUDE USING gist (extended_term WITH &&);

-- Name: preschool_term preschool_term_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.preschool_term
    ADD CONSTRAINT preschool_term_pkey PRIMARY KEY (id);

-- Name: scheduled_tasks scheduled_tasks_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.scheduled_tasks
    ADD CONSTRAINT scheduled_tasks_pkey PRIMARY KEY (task_name, task_instance);

-- Name: service_application service_application_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_application
    ADD CONSTRAINT service_application_pkey PRIMARY KEY (id);

-- Name: service_need_option_fee service_need_option_fee$no_overlaps; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need_option_fee
    ADD CONSTRAINT "service_need_option_fee$no_overlaps" EXCLUDE USING gist (service_need_option_id WITH =, validity WITH &&);

-- Name: service_need_option_fee service_need_option_fee_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need_option_fee
    ADD CONSTRAINT service_need_option_fee_pkey PRIMARY KEY (id);

-- Name: service_need_option service_need_option_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need_option
    ADD CONSTRAINT service_need_option_pkey PRIMARY KEY (id);

-- Name: service_need_option_voucher_value service_need_option_voucher_value$no_overlaps; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need_option_voucher_value
    ADD CONSTRAINT "service_need_option_voucher_value$no_overlaps" EXCLUDE USING gist (service_need_option_id WITH =, validity WITH &&);

-- Name: service_need_option_voucher_value service_need_option_voucher_value_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need_option_voucher_value
    ADD CONSTRAINT service_need_option_voucher_value_pkey PRIMARY KEY (id);

-- Name: setting setting_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.setting
    ADD CONSTRAINT setting_pkey PRIMARY KEY (key);

-- Name: sfi_get_events_continuation_token sfi_get_events_continuation_token_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_get_events_continuation_token
    ADD CONSTRAINT sfi_get_events_continuation_token_pkey PRIMARY KEY (continuation_token);

-- Name: sfi_message_event sfi_message_event_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_message_event
    ADD CONSTRAINT sfi_message_event_pkey PRIMARY KEY (id);

-- Name: sfi_message_event sfi_message_event_unique_event_per_message; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_message_event
    ADD CONSTRAINT sfi_message_event_unique_event_per_message UNIQUE (message_id, event_type);

-- Name: sfi_message sfi_message_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_message
    ADD CONSTRAINT sfi_message_pkey PRIMARY KEY (id);

-- Name: special_diet special_diet_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.special_diet
    ADD CONSTRAINT special_diet_pkey PRIMARY KEY (id);

-- Name: staff_attendance_external staff_attendance_external_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_external
    ADD CONSTRAINT staff_attendance_external_pkey PRIMARY KEY (id);

-- Name: staff_attendance staff_attendance_group_id_date_key; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT staff_attendance_group_id_date_key UNIQUE (group_id, date);

-- Name: staff_attendance_realtime staff_attendance_no_overlaps; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_realtime
    ADD CONSTRAINT staff_attendance_no_overlaps EXCLUDE USING gist (employee_id WITH =, tstzrange(arrived, departed) WITH &&);

-- Name: staff_attendance_plan staff_attendance_plan_no_overlaps; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_plan
    ADD CONSTRAINT staff_attendance_plan_no_overlaps EXCLUDE USING gist (employee_id WITH =, tstzrange(start_time, end_time) WITH &&);

-- Name: staff_attendance_plan staff_attendance_plan_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_plan
    ADD CONSTRAINT staff_attendance_plan_pkey PRIMARY KEY (id);

-- Name: staff_attendance_realtime staff_attendance_realtime_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_realtime
    ADD CONSTRAINT staff_attendance_realtime_pkey PRIMARY KEY (id);

-- Name: staff_occupancy_coefficient staff_occupancy_coefficient_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_occupancy_coefficient
    ADD CONSTRAINT staff_occupancy_coefficient_pkey PRIMARY KEY (id);

-- Name: system_notification system_notification_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.system_notification
    ADD CONSTRAINT system_notification_pkey PRIMARY KEY (target_group);

-- Name: titania_errors titania_errors_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.titania_errors
    ADD CONSTRAINT titania_errors_pkey PRIMARY KEY (id);

-- Name: absence uniq$absence_child_date_category; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT "uniq$absence_child_date_category" UNIQUE (child_id, date, category);

-- Name: care_area uniq$care_area_name; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.care_area
    ADD CONSTRAINT "uniq$care_area_name" UNIQUE (name);

-- Name: citizen_user uniq$citizen_user_username; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.citizen_user
    ADD CONSTRAINT "uniq$citizen_user_username" UNIQUE (username);

-- Name: employee uniq$employee_external_id; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT "uniq$employee_external_id" UNIQUE (external_id);

-- Name: employee uniq$employee_ssn; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT "uniq$employee_ssn" UNIQUE (social_security_number);

-- Name: invoice_row uniq$invoice_row_invoice_idx; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_row
    ADD CONSTRAINT "uniq$invoice_row_invoice_idx" UNIQUE (invoice_id, idx);

-- Name: koski_study_right uniq$koski_study_right_child_unit_type; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.koski_study_right
    ADD CONSTRAINT "uniq$koski_study_right_child_unit_type" UNIQUE (child_id, unit_id, type);

-- Name: password_blacklist_source uniq$password_blacklist_source_name; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.password_blacklist_source
    ADD CONSTRAINT "uniq$password_blacklist_source_name" UNIQUE (name);

-- Name: person_email_verification uniq$person_email_verification_person; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.person_email_verification
    ADD CONSTRAINT "uniq$person_email_verification_person" UNIQUE (person_id);

-- Name: placement_plan uniq$placement_plan_application_id; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement_plan
    ADD CONSTRAINT "uniq$placement_plan_application_id" UNIQUE (application_id);

-- Name: message_thread_folder uniq$thread_folder; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_folder
    ADD CONSTRAINT "uniq$thread_folder" UNIQUE (owner_id, name);

-- Name: message_thread_participant uniq$thread_participant; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_participant
    ADD CONSTRAINT "uniq$thread_participant" UNIQUE (thread_id, participant_id);

-- Name: nekku_special_diet_option uniq$unique_field_value; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_special_diet_option
    ADD CONSTRAINT "uniq$unique_field_value" UNIQUE (field_id, value);

-- Name: payment unique$payment_number; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT "unique$payment_number" UNIQUE (number);

-- Name: family_contact unique_child_contact_person_pair; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.family_contact
    ADD CONSTRAINT unique_child_contact_person_pair UNIQUE (child_id, contact_person_id) DEFERRABLE;

-- Name: fee_decision_child unique_child_decision_pair; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision_child
    ADD CONSTRAINT unique_child_decision_pair UNIQUE (fee_decision_id, child_id);

-- Name: family_contact unique_child_priority_pair; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.family_contact
    ADD CONSTRAINT unique_child_priority_pair UNIQUE (child_id, priority) DEFERRABLE;

-- Name: guardian unique_guardian_child; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.guardian
    ADD CONSTRAINT unique_guardian_child UNIQUE (guardian_id, child_id);

-- Name: invoice unique_invoice_num; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT unique_invoice_num UNIQUE (number);

-- Name: fridge_partner unique_partnership_person; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT unique_partnership_person UNIQUE (partnership_id, person_id);

-- Name: child_document_published_version unique_version_per_document; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_published_version
    ADD CONSTRAINT unique_version_per_document UNIQUE (child_document_id, version_number);

-- Name: vapid_jwt vapid_jwt_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.vapid_jwt
    ADD CONSTRAINT vapid_jwt_pkey PRIMARY KEY (origin, public_key);

-- Name: varda_state varda_state_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.varda_state
    ADD CONSTRAINT varda_state_pkey PRIMARY KEY (id);

-- Name: varda_unit varda_upload_unique_evaka_id; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.varda_unit
    ADD CONSTRAINT varda_upload_unique_evaka_id PRIMARY KEY (evaka_daycare_id);

-- Name: voucher_value_report_decision voucher_value_report_decision_part_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_report_decision
    ADD CONSTRAINT voucher_value_report_decision_part_pkey PRIMARY KEY (id);

-- Name: voucher_value_report_snapshot voucher_value_report_snapshot_pkey; Type: CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_report_snapshot
    ADD CONSTRAINT voucher_value_report_snapshot_pkey PRIMARY KEY (id);

-- Name: application_child_id_idx; Type: INDEX; Schema: public

CREATE INDEX application_child_id_idx ON public.application USING btree (child_id);

-- Name: application_guardian_id_idx; Type: INDEX; Schema: public

CREATE INDEX application_guardian_id_idx ON public.application USING btree (guardian_id);

-- Name: assistance_action_child_id_start_date_end_date_idx; Type: INDEX; Schema: public

CREATE INDEX assistance_action_child_id_start_date_end_date_idx ON public.assistance_action USING btree (child_id, start_date, end_date);

-- Name: daycare_acl_daycare_id_employee_id_idx; Type: INDEX; Schema: public

CREATE UNIQUE INDEX daycare_acl_daycare_id_employee_id_idx ON public.daycare_acl USING btree (daycare_id, employee_id);

-- Name: daycare_acl_employee_id_daycare_id_idx; Type: INDEX; Schema: public

CREATE UNIQUE INDEX daycare_acl_employee_id_daycare_id_idx ON public.daycare_acl USING btree (employee_id, daycare_id);

-- Name: daycare_care_area_id_idx; Type: INDEX; Schema: public

CREATE INDEX daycare_care_area_id_idx ON public.daycare USING btree (care_area_id);

-- Name: daycare_caretaker_group_id_start_date_end_date_idx; Type: INDEX; Schema: public

CREATE INDEX daycare_caretaker_group_id_start_date_end_date_idx ON public.daycare_caretaker USING btree (group_id, start_date, end_date);

-- Name: daycare_group_daycare_id_idx; Type: INDEX; Schema: public

CREATE INDEX daycare_group_daycare_id_idx ON public.daycare_group USING btree (daycare_id);

-- Name: daycare_group_placement_daycare_group_id_start_date_end_dat_idx; Type: INDEX; Schema: public

CREATE INDEX daycare_group_placement_daycare_group_id_start_date_end_dat_idx ON public.daycare_group_placement USING btree (daycare_group_id, start_date, end_date);

-- Name: fee_alteration_person_id_valid_from_valid_to_idx; Type: INDEX; Schema: public

CREATE INDEX fee_alteration_person_id_valid_from_valid_to_idx ON public.fee_alteration USING btree (person_id, valid_from, valid_to);

-- Name: fk$application_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$application_created_by" ON public.application USING btree (created_by);

-- Name: fk$application_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$application_modified_by" ON public.application USING btree (modified_by);

-- Name: fk$application_process_id; Type: INDEX; Schema: public

CREATE INDEX "fk$application_process_id" ON public.application USING btree (process_id);

-- Name: fk$application_status_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$application_status_modified_by" ON public.application USING btree (status_modified_by);

-- Name: fk$assistance_action_option_ref_option_id; Type: INDEX; Schema: public

CREATE INDEX "fk$assistance_action_option_ref_option_id" ON public.assistance_action_option_ref USING btree (option_id);

-- Name: fk$assistance_factor_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$assistance_factor_modified_by" ON public.assistance_factor USING btree (modified_by);

-- Name: fk$assistance_need_decision_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$assistance_need_decision_created_by" ON public.assistance_need_decision USING btree (created_by);

-- Name: fk$assistance_need_decision_process_id; Type: INDEX; Schema: public

CREATE INDEX "fk$assistance_need_decision_process_id" ON public.assistance_need_decision USING btree (process_id);

-- Name: fk$assistance_need_preschool_decision_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$assistance_need_preschool_decision_created_by" ON public.assistance_need_preschool_decision USING btree (created_by);

-- Name: fk$assistance_need_preschool_decision_process_id; Type: INDEX; Schema: public

CREATE INDEX "fk$assistance_need_preschool_decision_process_id" ON public.assistance_need_preschool_decision USING btree (process_id);

-- Name: fk$assistance_need_voucher_coefficient_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$assistance_need_voucher_coefficient_modified_by" ON public.assistance_need_voucher_coefficient USING btree (modified_by);

-- Name: fk$backup_care_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$backup_care_created_by" ON public.backup_care USING btree (created_by);

-- Name: fk$backup_care_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$backup_care_modified_by" ON public.backup_care USING btree (modified_by);

-- Name: fk$calendar_event_content_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$calendar_event_content_modified_by" ON public.calendar_event USING btree (content_modified_by);

-- Name: fk$calendar_event_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$calendar_event_created_by" ON public.calendar_event USING btree (created_by);

-- Name: fk$calendar_event_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$calendar_event_modified_by" ON public.calendar_event USING btree (modified_by);

-- Name: fk$child_attendance_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$child_attendance_modified_by" ON public.child_attendance USING btree (modified_by);

-- Name: fk$child_document_answered_by; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_answered_by" ON public.child_document USING btree (answered_by);

-- Name: fk$child_document_content_locked_by; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_content_locked_by" ON public.child_document USING btree (content_locked_by);

-- Name: fk$child_document_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_created_by" ON public.child_document USING btree (created_by);

-- Name: fk$child_document_decision_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_decision_created_by" ON public.child_document_decision USING btree (created_by);

-- Name: fk$child_document_decision_maker; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_decision_maker" ON public.child_document USING btree (decision_maker);

-- Name: fk$child_document_decision_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_decision_modified_by" ON public.child_document_decision USING btree (modified_by);

-- Name: fk$child_document_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_modified_by" ON public.child_document USING btree (modified_by);

-- Name: fk$child_document_process_id; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_process_id" ON public.child_document USING btree (process_id);

-- Name: fk$child_document_published_by; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_published_by" ON public.child_document USING btree (deprecated_published_by);

-- Name: fk$child_document_published_version_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_published_version_created_by" ON public.child_document_published_version USING btree (created_by);

-- Name: fk$child_document_published_version_document_id_version_number; Type: INDEX; Schema: public

CREATE INDEX "fk$child_document_published_version_document_id_version_number" ON public.child_document_published_version USING btree (child_document_id, version_number);

-- Name: fk$created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$created_by" ON public.placement USING btree (created_by);

-- Name: fk$fee_decision_process_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "fk$fee_decision_process_id" ON public.fee_decision USING btree (process_id);

-- Name: fk$foster_parent_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$foster_parent_created_by" ON public.foster_parent USING btree (created_by);

-- Name: fk$foster_parent_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$foster_parent_modified_by" ON public.foster_parent USING btree (modified_by);

-- Name: fk$invoice_correction_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$invoice_correction_created_by" ON public.invoice_correction USING btree (created_by);

-- Name: fk$invoice_correction_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$invoice_correction_modified_by" ON public.invoice_correction USING btree (modified_by);

-- Name: fk$invoice_correction_unit_id; Type: INDEX; Schema: public

CREATE INDEX "fk$invoice_correction_unit_id" ON public.invoice_correction USING btree (unit_id);

-- Name: fk$invoiced_fee_decision_fee_decision_id; Type: INDEX; Schema: public

CREATE INDEX "fk$invoiced_fee_decision_fee_decision_id" ON public.invoiced_fee_decision USING btree (fee_decision_id);

-- Name: fk$modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$modified_by" ON public.placement USING btree (modified_by);

-- Name: fk$nekku_product_options_id; Type: INDEX; Schema: public

CREATE INDEX "fk$nekku_product_options_id" ON public.nekku_product USING btree (options_id);

-- Name: fk$nekku_special_diet_choices_child_id; Type: INDEX; Schema: public

CREATE INDEX "fk$nekku_special_diet_choices_child_id" ON public.nekku_special_diet_choices USING btree (child_id);

-- Name: fk$nekku_special_diet_choices_diet_id; Type: INDEX; Schema: public

CREATE INDEX "fk$nekku_special_diet_choices_diet_id" ON public.nekku_special_diet_choices USING btree (diet_id);

-- Name: fk$nekku_special_diet_choices_field_id; Type: INDEX; Schema: public

CREATE INDEX "fk$nekku_special_diet_choices_field_id" ON public.nekku_special_diet_choices USING btree (field_id);

-- Name: fk$placement_draft_created_by; Type: INDEX; Schema: public

CREATE INDEX "fk$placement_draft_created_by" ON public.placement_draft USING btree (created_by);

-- Name: fk$placement_draft_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$placement_draft_modified_by" ON public.placement_draft USING btree (modified_by);

-- Name: fk$placement_draft_unit_id; Type: INDEX; Schema: public

CREATE INDEX "fk$placement_draft_unit_id" ON public.placement_draft USING btree (unit_id);

-- Name: fk$placement_source_application_id; Type: INDEX; Schema: public

CREATE INDEX "fk$placement_source_application_id" ON public.placement USING btree (source_application_id) WHERE (source_application_id IS NOT NULL);

-- Name: fk$placement_source_service_application_id; Type: INDEX; Schema: public

CREATE INDEX "fk$placement_source_service_application_id" ON public.placement USING btree (source_service_application_id) WHERE (source_service_application_id IS NOT NULL);

-- Name: fk$sfi_message_decision_id_guardian_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "fk$sfi_message_decision_id_guardian_id" ON public.sfi_message USING btree (decision_id, guardian_id) WHERE (decision_id IS NOT NULL);

-- Name: fk$sfi_message_document_id_guardian_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "fk$sfi_message_document_id_guardian_id" ON public.sfi_message USING btree (document_id, guardian_id) WHERE (document_id IS NOT NULL);

-- Name: fk$sfi_message_event_message_id; Type: INDEX; Schema: public

CREATE INDEX "fk$sfi_message_event_message_id" ON public.sfi_message_event USING btree (message_id);

-- Name: fk$sfi_message_fee_decision_id_guardian_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "fk$sfi_message_fee_decision_id_guardian_id" ON public.sfi_message USING btree (fee_decision_id, guardian_id) WHERE (fee_decision_id IS NOT NULL);

-- Name: fk$sfi_message_guardian_id; Type: INDEX; Schema: public

CREATE INDEX "fk$sfi_message_guardian_id" ON public.sfi_message USING btree (guardian_id);

-- Name: fk$sfi_message_voucher_value_decision_id_guardian_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "fk$sfi_message_voucher_value_decision_id_guardian_id" ON public.sfi_message USING btree (voucher_value_decision_id, guardian_id) WHERE (voucher_value_decision_id IS NOT NULL);

-- Name: fk$staff_attendance_realtime_arrived_added_by; Type: INDEX; Schema: public

CREATE INDEX "fk$staff_attendance_realtime_arrived_added_by" ON public.staff_attendance_realtime USING btree (arrived_added_by);

-- Name: fk$staff_attendance_realtime_arrived_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$staff_attendance_realtime_arrived_modified_by" ON public.staff_attendance_realtime USING btree (arrived_modified_by);

-- Name: fk$staff_attendance_realtime_departed_added_by; Type: INDEX; Schema: public

CREATE INDEX "fk$staff_attendance_realtime_departed_added_by" ON public.staff_attendance_realtime USING btree (departed_added_by);

-- Name: fk$staff_attendance_realtime_departed_modified_by; Type: INDEX; Schema: public

CREATE INDEX "fk$staff_attendance_realtime_departed_modified_by" ON public.staff_attendance_realtime USING btree (departed_modified_by);

-- Name: fk$terminated_by; Type: INDEX; Schema: public

CREATE INDEX "fk$terminated_by" ON public.placement USING btree (terminated_by);

-- Name: fk$titania_errors_employee_id; Type: INDEX; Schema: public

CREATE INDEX "fk$titania_errors_employee_id" ON public.titania_errors USING btree (employee_id);

-- Name: fk$voucher_value_decision_process_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "fk$voucher_value_decision_process_id" ON public.voucher_value_decision USING btree (process_id);

-- Name: fk_child_document_decision_daycare_id; Type: INDEX; Schema: public

CREATE INDEX fk_child_document_decision_daycare_id ON public.child_document_decision USING btree (daycare_id);

-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);

-- Name: fridge_child_child_id_start_date_end_date_idx; Type: INDEX; Schema: public

CREATE INDEX fridge_child_child_id_start_date_end_date_idx ON public.fridge_child USING btree (child_id, start_date, end_date);

-- Name: fridge_child_head_of_child_start_date_end_date_idx; Type: INDEX; Schema: public

CREATE INDEX fridge_child_head_of_child_start_date_end_date_idx ON public.fridge_child USING btree (head_of_child, start_date, end_date);

-- Name: fridge_partner_person_id_start_date_end_date_idx; Type: INDEX; Schema: public

CREATE INDEX fridge_partner_person_id_start_date_end_date_idx ON public.fridge_partner USING btree (person_id, start_date, end_date);

-- Name: idx$absence_application_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$absence_application_child_id" ON public.absence_application USING btree (child_id);

-- Name: idx$absence_application_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$absence_application_created_by" ON public.absence_application USING btree (created_by);

-- Name: idx$absence_application_decided_by; Type: INDEX; Schema: public

CREATE INDEX "idx$absence_application_decided_by" ON public.absence_application USING btree (decided_by);

-- Name: idx$absence_application_modified_by; Type: INDEX; Schema: public

CREATE INDEX "idx$absence_application_modified_by" ON public.absence_application USING btree (modified_by);

-- Name: idx$absence_modified_by; Type: INDEX; Schema: public

CREATE INDEX "idx$absence_modified_by" ON public.absence USING btree (modified_by);

-- Name: idx$absence_questionnaire; Type: INDEX; Schema: public

CREATE INDEX "idx$absence_questionnaire" ON public.absence USING btree (questionnaire_id);

-- Name: idx$application_doc; Type: INDEX; Schema: public

CREATE INDEX "idx$application_doc" ON public.application USING gin (document jsonb_path_ops);

-- Name: idx$application_due_date; Type: INDEX; Schema: public

CREATE INDEX "idx$application_due_date" ON public.application USING btree (duedate, status) WHERE (duedate IS NOT NULL);

-- Name: idx$application_note_application; Type: INDEX; Schema: public

CREATE INDEX "idx$application_note_application" ON public.application_note USING btree (application_id);

-- Name: idx$application_note_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$application_note_created_by" ON public.application_note USING btree (created_by);

-- Name: idx$application_note_message_content; Type: INDEX; Schema: public

CREATE INDEX "idx$application_note_message_content" ON public.application_note USING btree (message_content_id) WHERE (message_content_id IS NOT NULL);

-- Name: idx$application_note_updated_by; Type: INDEX; Schema: public

CREATE INDEX "idx$application_note_updated_by" ON public.application_note USING btree (modified_by);

-- Name: idx$application_other_guardian_guardian; Type: INDEX; Schema: public

CREATE INDEX "idx$application_other_guardian_guardian" ON public.application_other_guardian USING btree (guardian_id) INCLUDE (application_id);

-- Name: idx$application_primary_preferred_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$application_primary_preferred_unit" ON public.application USING btree (primary_preferred_unit);

-- Name: idx$application_sent_date; Type: INDEX; Schema: public

CREATE INDEX "idx$application_sent_date" ON public.application USING btree (sentdate, status) WHERE (sentdate IS NOT NULL);

-- Name: idx$assistance_action_updated_by; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_action_updated_by" ON public.assistance_action USING btree (modified_by);

-- Name: idx$assistance_factor_child; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_factor_child" ON public.assistance_factor USING btree (child_id);

-- Name: idx$assistance_need_decision_child; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_decision_child" ON public.assistance_need_decision USING btree (child_id);

-- Name: idx$assistance_need_decision_decision_maker; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_decision_decision_maker" ON public.assistance_need_decision USING btree (decision_maker_employee_id) WHERE (decision_maker_employee_id IS NOT NULL);

-- Name: idx$assistance_need_decision_id; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_decision_id" ON public.assistance_need_decision_guardian USING btree (assistance_need_decision_id);

-- Name: idx$assistance_need_decision_preparer_1; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_decision_preparer_1" ON public.assistance_need_decision USING btree (preparer_1_employee_id) WHERE (preparer_1_employee_id IS NOT NULL);

-- Name: idx$assistance_need_decision_preparer_2; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_decision_preparer_2" ON public.assistance_need_decision USING btree (preparer_2_employee_id) WHERE (preparer_2_employee_id IS NOT NULL);

-- Name: idx$assistance_need_decision_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_decision_unit" ON public.assistance_need_decision USING btree (selected_unit) WHERE (selected_unit IS NOT NULL);

-- Name: idx$assistance_need_decision_validity_period; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_decision_validity_period" ON public.assistance_need_decision USING gist (validity_period);

-- Name: idx$assistance_need_guardian_person; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_guardian_person" ON public.assistance_need_decision_guardian USING btree (person_id);

-- Name: idx$assistance_need_preschool_decision_child; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_preschool_decision_child" ON public.assistance_need_preschool_decision USING btree (child_id);

-- Name: idx$assistance_need_preschool_decision_decision_maker; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_preschool_decision_decision_maker" ON public.assistance_need_preschool_decision USING btree (decision_maker_employee_id) WHERE (decision_maker_employee_id IS NOT NULL);

-- Name: idx$assistance_need_preschool_decision_guardian_decision_id; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_preschool_decision_guardian_decision_id" ON public.assistance_need_preschool_decision_guardian USING btree (assistance_need_decision_id);

-- Name: idx$assistance_need_preschool_decision_guardian_person; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_preschool_decision_guardian_person" ON public.assistance_need_preschool_decision_guardian USING btree (person_id);

-- Name: idx$assistance_need_preschool_decision_preparer_1; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_preschool_decision_preparer_1" ON public.assistance_need_preschool_decision USING btree (preparer_1_employee_id) WHERE (preparer_1_employee_id IS NOT NULL);

-- Name: idx$assistance_need_preschool_decision_preparer_2; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_preschool_decision_preparer_2" ON public.assistance_need_preschool_decision USING btree (preparer_2_employee_id) WHERE (preparer_2_employee_id IS NOT NULL);

-- Name: idx$assistance_need_preschool_decision_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_preschool_decision_unit" ON public.assistance_need_preschool_decision USING btree (selected_unit) WHERE (selected_unit IS NOT NULL);

-- Name: idx$assistance_need_voucher_coefficient_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$assistance_need_voucher_coefficient_child_id" ON public.assistance_need_voucher_coefficient USING btree (child_id);

-- Name: idx$async_job_completed; Type: INDEX; Schema: public

CREATE INDEX "idx$async_job_completed" ON public.async_job USING btree (completed_at) WHERE (completed_at IS NOT NULL);

-- Name: idx$async_job_run_at; Type: INDEX; Schema: public

CREATE INDEX "idx$async_job_run_at" ON public.async_job USING btree (run_at) WHERE (completed_at IS NULL);

-- Name: idx$attachment_application; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_application" ON public.attachment USING btree (application_id) WHERE (application_id IS NOT NULL);

-- Name: idx$attachment_fee_alteration; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_fee_alteration" ON public.attachment USING btree (fee_alteration_id) WHERE (fee_alteration_id IS NOT NULL);

-- Name: idx$attachment_income; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_income" ON public.attachment USING btree (income_id) WHERE (income_id IS NOT NULL);

-- Name: idx$attachment_income_statement; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_income_statement" ON public.attachment USING btree (income_statement_id) WHERE (income_statement_id IS NOT NULL);

-- Name: idx$attachment_invoice; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_invoice" ON public.attachment USING btree (invoice_id) WHERE (invoice_id IS NOT NULL);

-- Name: idx$attachment_message_content; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_message_content" ON public.attachment USING btree (message_content_id) WHERE (message_content_id IS NOT NULL);

-- Name: idx$attachment_message_draft; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_message_draft" ON public.attachment USING btree (message_draft_id) WHERE (message_draft_id IS NOT NULL);

-- Name: idx$attachment_orphan; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_orphan" ON public.attachment USING btree (id) WHERE ((application_id IS NULL) AND (fee_alteration_id IS NULL) AND (income_id IS NULL) AND (income_statement_id IS NULL) AND (invoice_id IS NULL) AND (message_content_id IS NULL) AND (message_draft_id IS NULL) AND (pedagogical_document_id IS NULL));

-- Name: idx$attachment_pedagogic_document; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_pedagogic_document" ON public.attachment USING btree (pedagogical_document_id) WHERE (pedagogical_document_id IS NOT NULL);

-- Name: idx$attachment_uploaded_by; Type: INDEX; Schema: public

CREATE INDEX "idx$attachment_uploaded_by" ON public.attachment USING btree (uploaded_by);

-- Name: idx$attendance_child; Type: INDEX; Schema: public

CREATE INDEX "idx$attendance_child" ON public.child_attendance USING btree (child_id);

-- Name: idx$attendance_date_and_times; Type: INDEX; Schema: public

CREATE INDEX "idx$attendance_date_and_times" ON public.child_attendance USING btree (date, start_time, end_time);

-- Name: idx$attendance_reservation_child_date; Type: INDEX; Schema: public

CREATE INDEX "idx$attendance_reservation_child_date" ON public.attendance_reservation USING btree (child_id, date);

-- Name: idx$attendance_reservation_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$attendance_reservation_created_by" ON public.attendance_reservation USING btree (created_by);

-- Name: idx$backup_care_child; Type: INDEX; Schema: public

CREATE INDEX "idx$backup_care_child" ON public.backup_care USING btree (child_id, start_date, end_date);

-- Name: idx$backup_care_group; Type: INDEX; Schema: public

CREATE INDEX "idx$backup_care_group" ON public.backup_care USING btree (group_id, start_date, end_date);

-- Name: idx$backup_care_group_gist; Type: INDEX; Schema: public

CREATE INDEX "idx$backup_care_group_gist" ON public.backup_care USING gist (group_id, daterange(start_date, end_date, '[]'::text)) WHERE (group_id IS NOT NULL);

-- Name: idx$backup_care_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$backup_care_unit" ON public.backup_care USING btree (unit_id, start_date, end_date);

-- Name: idx$backup_care_unit_gist; Type: INDEX; Schema: public

CREATE INDEX "idx$backup_care_unit_gist" ON public.backup_care USING gist (unit_id, daterange(start_date, end_date, '[]'::text));

-- Name: idx$backup_pickup_child; Type: INDEX; Schema: public

CREATE INDEX "idx$backup_pickup_child" ON public.backup_pickup USING btree (child_id);

-- Name: idx$calendar_event_attendee_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_attendee_child_id" ON public.calendar_event_attendee USING btree (child_id);

-- Name: idx$calendar_event_attendee_event_id; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_attendee_event_id" ON public.calendar_event_attendee USING btree (calendar_event_id);

-- Name: idx$calendar_event_attendee_group_id; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_attendee_group_id" ON public.calendar_event_attendee USING btree (group_id);

-- Name: idx$calendar_event_attendee_unit_id; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_attendee_unit_id" ON public.calendar_event_attendee USING btree (unit_id);

-- Name: idx$calendar_event_created; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_created" ON public.calendar_event USING btree (created_at);

-- Name: idx$calendar_event_period; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_period" ON public.calendar_event USING gist (period);

-- Name: idx$calendar_event_time_child; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_time_child" ON public.calendar_event_time USING btree (child_id) WHERE (child_id IS NOT NULL);

-- Name: idx$calendar_event_time_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_time_created_by" ON public.calendar_event_time USING btree (created_by);

-- Name: idx$calendar_event_time_event_id; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_time_event_id" ON public.calendar_event_time USING btree (calendar_event_id);

-- Name: idx$calendar_event_time_modified_by; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_time_modified_by" ON public.calendar_event_time USING btree (modified_by);

-- Name: idx$calendar_event_type_period; Type: INDEX; Schema: public

CREATE INDEX "idx$calendar_event_type_period" ON public.calendar_event USING gist (event_type, period);

-- Name: idx$child_attendance_child; Type: INDEX; Schema: public

CREATE INDEX "idx$child_attendance_child" ON public.child_attendance USING btree (child_id, date);

-- Name: idx$child_attendance_child_range; Type: INDEX; Schema: public

CREATE INDEX "idx$child_attendance_child_range" ON public.child_attendance USING gist (child_id, tstzrange(arrived, departed));

-- Name: idx$child_attendance_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$child_attendance_unit" ON public.child_attendance USING btree (unit_id, date);

-- Name: idx$child_attendance_unit_range; Type: INDEX; Schema: public

CREATE INDEX "idx$child_attendance_unit_range" ON public.child_attendance USING gist (unit_id, tstzrange(arrived, departed));

-- Name: idx$child_diet_id; Type: INDEX; Schema: public

CREATE INDEX "idx$child_diet_id" ON public.child USING btree (diet_id);

-- Name: idx$child_document_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$child_document_child_id" ON public.child_document USING btree (child_id);

-- Name: idx$child_document_template; Type: INDEX; Schema: public

CREATE INDEX "idx$child_document_template" ON public.child_document USING btree (template_id);

-- Name: idx$child_meal_texture_id; Type: INDEX; Schema: public

CREATE INDEX "idx$child_meal_texture_id" ON public.child USING btree (meal_texture_id);

-- Name: idx$child_sticky_note_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$child_sticky_note_child_id" ON public.child_sticky_note USING btree (child_id);

-- Name: idx$child_sticky_note_expires; Type: INDEX; Schema: public

CREATE INDEX "idx$child_sticky_note_expires" ON public.child_sticky_note USING btree (expires);

-- Name: idx$daily_service_time_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$daily_service_time_child_id" ON public.daily_service_time USING btree (child_id);

-- Name: idx$daily_service_time_notification_guardian_id; Type: INDEX; Schema: public

CREATE INDEX "idx$daily_service_time_notification_guardian_id" ON public.daily_service_time_notification USING btree (guardian_id);

-- Name: idx$daily_service_time_validity_period; Type: INDEX; Schema: public

CREATE INDEX "idx$daily_service_time_validity_period" ON public.daily_service_time USING gist (validity_period);

-- Name: idx$daycare_assistance_child; Type: INDEX; Schema: public

CREATE INDEX "idx$daycare_assistance_child" ON public.daycare_assistance USING btree (child_id);

-- Name: idx$daycare_group_acl_group_employee; Type: INDEX; Schema: public

CREATE INDEX "idx$daycare_group_acl_group_employee" ON public.daycare_group_acl USING btree (daycare_group_id, employee_id);

-- Name: idx$daycare_group_nekku_customer_number; Type: INDEX; Schema: public

CREATE INDEX "idx$daycare_group_nekku_customer_number" ON public.daycare_group USING btree (nekku_customer_number);

-- Name: idx$daycare_group_placement_group; Type: INDEX; Schema: public

CREATE INDEX "idx$daycare_group_placement_group" ON public.daycare_group_placement USING gist (daycare_group_id, daterange(start_date, end_date, '[]'::text));

-- Name: idx$decision_application; Type: INDEX; Schema: public

CREATE INDEX "idx$decision_application" ON public.decision USING btree (application_id);

-- Name: idx$decision_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$decision_created_by" ON public.decision USING btree (created_by);

-- Name: idx$decision_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$decision_unit" ON public.decision USING btree (unit_id);

-- Name: idx$employee_temporary_in_unit_id; Type: INDEX; Schema: public

CREATE INDEX "idx$employee_temporary_in_unit_id" ON public.employee USING btree (temporary_in_unit_id);

-- Name: idx$evaka_user_unknown; Type: INDEX; Schema: public

CREATE INDEX "idx$evaka_user_unknown" ON public.evaka_user USING btree (name) WHERE (type = 'UNKNOWN'::public.evaka_user_type);

-- Name: idx$family_contact_contact_person; Type: INDEX; Schema: public

CREATE INDEX "idx$family_contact_contact_person" ON public.family_contact USING btree (contact_person_id);

-- Name: idx$fee_alteration_person; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_alteration_person" ON public.fee_alteration USING gist (person_id, daterange(valid_from, valid_to, '[]'::text));

-- Name: idx$fee_alteration_updated_by; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_alteration_updated_by" ON public.fee_alteration USING btree (modified_by);

-- Name: idx$fee_decision_approved_by; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_approved_by" ON public.fee_decision USING btree (approved_by_id) WHERE (approved_by_id IS NOT NULL);

-- Name: idx$fee_decision_child_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_child_child_id" ON public.fee_decision_child USING btree (child_id);

-- Name: idx$fee_decision_child_fee_decision_id; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_child_fee_decision_id" ON public.fee_decision_child USING btree (fee_decision_id);

-- Name: idx$fee_decision_child_placement_unit_id; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_child_placement_unit_id" ON public.fee_decision_child USING btree (placement_unit_id);

-- Name: idx$fee_decision_child_service_need_option; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_child_service_need_option" ON public.fee_decision_child USING btree (service_need_option_id);

-- Name: idx$fee_decision_decision_handler_id; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_decision_handler_id" ON public.fee_decision USING btree (decision_handler_id);

-- Name: idx$fee_decision_head_of_family_id; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_head_of_family_id" ON public.fee_decision USING btree (head_of_family_id);

-- Name: idx$fee_decision_partner_id; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_partner_id" ON public.fee_decision USING btree (partner_id);

-- Name: idx$fee_decision_status; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_status" ON public.fee_decision USING btree (status);

-- Name: idx$fee_decision_valid_during; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_valid_during" ON public.fee_decision USING gist (valid_during);

-- Name: idx$fee_decision_waiting_for_manual_sending; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_waiting_for_manual_sending" ON public.fee_decision USING gist (valid_during) WHERE (status = 'WAITING_FOR_MANUAL_SENDING'::public.fee_decision_status);

-- Name: idx$fee_decision_waiting_for_sending; Type: INDEX; Schema: public

CREATE INDEX "idx$fee_decision_waiting_for_sending" ON public.fee_decision USING gist (valid_during) WHERE (status = 'WAITING_FOR_SENDING'::public.fee_decision_status);

-- Name: idx$finance_note_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$finance_note_created_by" ON public.finance_note USING btree (created_by);

-- Name: idx$finance_note_modified_by; Type: INDEX; Schema: public

CREATE INDEX "idx$finance_note_modified_by" ON public.finance_note USING btree (modified_by);

-- Name: idx$finance_note_person; Type: INDEX; Schema: public

CREATE INDEX "idx$finance_note_person" ON public.finance_note USING btree (person_id);

-- Name: idx$foster_parent_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$foster_parent_child_id" ON public.foster_parent USING btree (child_id);

-- Name: idx$foster_parent_parent_id; Type: INDEX; Schema: public

CREATE INDEX "idx$foster_parent_parent_id" ON public.foster_parent USING btree (parent_id);

-- Name: idx$fridge_child_conflicts; Type: INDEX; Schema: public

CREATE INDEX "idx$fridge_child_conflicts" ON public.fridge_child USING btree (id) WHERE (conflict IS TRUE);

-- Name: idx$fridge_partner_conflicts; Type: INDEX; Schema: public

CREATE INDEX "idx$fridge_partner_conflicts" ON public.fridge_partner USING btree (person_id) WHERE (conflict IS TRUE);

-- Name: idx$fridge_partner_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$fridge_partner_created_by" ON public.fridge_partner USING btree (created_by);

-- Name: idx$fridge_partner_created_from_application; Type: INDEX; Schema: public

CREATE INDEX "idx$fridge_partner_created_from_application" ON public.fridge_partner USING btree (created_from_application);

-- Name: idx$fridge_partner_modified_by; Type: INDEX; Schema: public

CREATE INDEX "idx$fridge_partner_modified_by" ON public.fridge_partner USING btree (modified_by);

-- Name: idx$group_note_expires; Type: INDEX; Schema: public

CREATE INDEX "idx$group_note_expires" ON public.group_note USING btree (expires);

-- Name: idx$group_note_group_id; Type: INDEX; Schema: public

CREATE INDEX "idx$group_note_group_id" ON public.group_note USING btree (group_id);

-- Name: idx$guardian_blocklist_child; Type: INDEX; Schema: public

CREATE INDEX "idx$guardian_blocklist_child" ON public.guardian_blocklist USING btree (child_id);

-- Name: idx$guardian_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$guardian_child_id" ON public.guardian USING btree (child_id);

-- Name: idx$guardian_guardian_id; Type: INDEX; Schema: public

CREATE INDEX "idx$guardian_guardian_id" ON public.guardian USING btree (guardian_id);

-- Name: idx$income_application; Type: INDEX; Schema: public

CREATE INDEX "idx$income_application" ON public.income USING btree (application_id) WHERE (application_id IS NOT NULL);

-- Name: idx$income_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$income_created_by" ON public.income USING btree (created_by);

-- Name: idx$income_notification_receiver_id; Type: INDEX; Schema: public

CREATE INDEX "idx$income_notification_receiver_id" ON public.income_notification USING btree (receiver_id);

-- Name: idx$income_statement_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$income_statement_created_by" ON public.income_statement USING btree (created_by);

-- Name: idx$income_statement_handler; Type: INDEX; Schema: public

CREATE INDEX "idx$income_statement_handler" ON public.income_statement USING btree (handler_id) WHERE (handler_id IS NOT NULL);

-- Name: idx$income_statement_modified_by; Type: INDEX; Schema: public

CREATE INDEX "idx$income_statement_modified_by" ON public.income_statement USING btree (modified_by);

-- Name: idx$income_statement_person; Type: INDEX; Schema: public

CREATE INDEX "idx$income_statement_person" ON public.income_statement USING btree (person_id, start_date, end_date);

-- Name: idx$income_statement_person_gist; Type: INDEX; Schema: public

CREATE INDEX "idx$income_statement_person_gist" ON public.income_statement USING gist (person_id, daterange(start_date, end_date, '[]'::text));

-- Name: idx$income_statement_sent_at_awaiting_handler; Type: INDEX; Schema: public

CREATE INDEX "idx$income_statement_sent_at_awaiting_handler" ON public.income_statement USING btree (sent_at) WHERE (status = 'SENT'::public.income_statement_status);

-- Name: idx$income_updated_by; Type: INDEX; Schema: public

CREATE INDEX "idx$income_updated_by" ON public.income USING btree (modified_by);

-- Name: idx$income_valid_to_effect_not_incomplete; Type: INDEX; Schema: public

CREATE INDEX "idx$income_valid_to_effect_not_incomplete" ON public.income USING btree (person_id, valid_to DESC) WHERE (effect <> 'INCOMPLETE'::public.income_effect);

-- Name: idx$invoice_area_id; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_area_id" ON public.invoice USING btree (area_id);

-- Name: idx$invoice_codebtor; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_codebtor" ON public.invoice USING btree (codebtor) WHERE (codebtor IS NOT NULL);

-- Name: idx$invoice_correction_child; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_correction_child" ON public.invoice_correction USING btree (child_id);

-- Name: idx$invoice_correction_head_of_family; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_correction_head_of_family" ON public.invoice_correction USING btree (head_of_family_id);

-- Name: idx$invoice_correction_target_month_head_of_family; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_correction_target_month_head_of_family" ON public.invoice_correction USING btree (target_month, head_of_family_id);

-- Name: idx$invoice_date; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_date" ON public.invoice USING btree (invoice_date);

-- Name: idx$invoice_period; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_period" ON public.invoice USING btree (period_start, period_end);

-- Name: idx$invoice_period_gist; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_period_gist" ON public.invoice USING gist (daterange(period_start, period_end, '[]'::text));

-- Name: idx$invoice_row_child; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_row_child" ON public.invoice_row USING btree (child);

-- Name: idx$invoice_row_correction_id; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_row_correction_id" ON public.invoice_row USING btree (correction_id);

-- Name: idx$invoice_row_unit_id; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_row_unit_id" ON public.invoice_row USING btree (unit_id);

-- Name: idx$invoice_sent_by; Type: INDEX; Schema: public

CREATE INDEX "idx$invoice_sent_by" ON public.invoice USING btree (sent_by);

-- Name: idx$koski_study_right_person_oid; Type: INDEX; Schema: public

CREATE INDEX "idx$koski_study_right_person_oid" ON public.koski_study_right USING btree (person_oid);

-- Name: idx$koski_study_right_study_right_oid; Type: INDEX; Schema: public

CREATE INDEX "idx$koski_study_right_study_right_oid" ON public.koski_study_right USING btree (study_right_oid);

-- Name: idx$koski_study_right_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$koski_study_right_unit" ON public.koski_study_right USING btree (unit_id);

-- Name: idx$message_content_author; Type: INDEX; Schema: public

CREATE INDEX "idx$message_content_author" ON public.message_content USING btree (author_id);

-- Name: idx$message_content_id; Type: INDEX; Schema: public

CREATE INDEX "idx$message_content_id" ON public.message USING btree (content_id);

-- Name: idx$message_draft_account_id; Type: INDEX; Schema: public

CREATE INDEX "idx$message_draft_account_id" ON public.message_draft USING btree (account_id);

-- Name: idx$message_recipients_account_id; Type: INDEX; Schema: public

CREATE INDEX "idx$message_recipients_account_id" ON public.message_recipients USING btree (recipient_id);

-- Name: idx$message_recipients_message_id; Type: INDEX; Schema: public

CREATE INDEX "idx$message_recipients_message_id" ON public.message_recipients USING btree (message_id);

-- Name: idx$message_recipients_unread; Type: INDEX; Schema: public

CREATE INDEX "idx$message_recipients_unread" ON public.message_recipients USING btree (recipient_id, message_id) WHERE (read_at IS NULL);

-- Name: idx$message_sender_id; Type: INDEX; Schema: public

CREATE INDEX "idx$message_sender_id" ON public.message USING btree (sender_id);

-- Name: idx$message_thread_application; Type: INDEX; Schema: public

CREATE INDEX "idx$message_thread_application" ON public.message_thread USING btree (application_id) WHERE (application_id IS NOT NULL);

-- Name: idx$message_thread_children_account_id; Type: INDEX; Schema: public

CREATE INDEX "idx$message_thread_children_account_id" ON public.message_thread_children USING btree (child_id);

-- Name: idx$message_thread_children_thread_id; Type: INDEX; Schema: public

CREATE INDEX "idx$message_thread_children_thread_id" ON public.message_thread_children USING btree (thread_id);

-- Name: idx$message_thread_id; Type: INDEX; Schema: public

CREATE INDEX "idx$message_thread_id" ON public.message USING btree (thread_id);

-- Name: idx$mobile_device_employee; Type: INDEX; Schema: public

CREATE INDEX "idx$mobile_device_employee" ON public.mobile_device USING btree (employee_id) WHERE (employee_id IS NOT NULL);

-- Name: idx$mobile_device_push_group_reverse_pk; Type: INDEX; Schema: public

CREATE INDEX "idx$mobile_device_push_group_reverse_pk" ON public.mobile_device_push_group USING btree (device, daycare_group);

-- Name: idx$mobile_device_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$mobile_device_unit" ON public.mobile_device USING btree (unit_id);

-- Name: idx$nekku_customer_type_number; Type: INDEX; Schema: public

CREATE INDEX "idx$nekku_customer_type_number" ON public.nekku_customer_type USING btree (customer_number);

-- Name: idx$nekku_order_report_filters; Type: INDEX; Schema: public

CREATE INDEX "idx$nekku_order_report_filters" ON public.nekku_orders_report USING btree (daycare_id, group_id, delivery_date);

-- Name: idx$nekku_special_diet_field_diet; Type: INDEX; Schema: public

CREATE INDEX "idx$nekku_special_diet_field_diet" ON public.nekku_special_diet_field USING btree (diet_id);

-- Name: idx$nekku_special_diet_option_field; Type: INDEX; Schema: public

CREATE INDEX "idx$nekku_special_diet_option_field" ON public.nekku_special_diet_option USING btree (field_id);

-- Name: idx$new_service_need_option; Type: INDEX; Schema: public

CREATE INDEX "idx$new_service_need_option" ON public.service_need USING btree (option_id);

-- Name: idx$new_service_need_placement; Type: INDEX; Schema: public

CREATE INDEX "idx$new_service_need_placement" ON public.service_need USING btree (placement_id);

-- Name: idx$other_assistance_measure_child; Type: INDEX; Schema: public

CREATE INDEX "idx$other_assistance_measure_child" ON public.other_assistance_measure USING btree (child_id);

-- Name: idx$payment_amount; Type: INDEX; Schema: public

CREATE INDEX "idx$payment_amount" ON public.payment USING btree (amount);

-- Name: idx$payment_created; Type: INDEX; Schema: public

CREATE INDEX "idx$payment_created" ON public.payment USING btree (created);

-- Name: idx$payment_date; Type: INDEX; Schema: public

CREATE INDEX "idx$payment_date" ON public.payment USING btree (payment_date);

-- Name: idx$payment_period; Type: INDEX; Schema: public

CREATE INDEX "idx$payment_period" ON public.payment USING gist (period);

-- Name: idx$payment_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$payment_unit" ON public.payment USING btree (unit_id) WHERE (unit_id IS NOT NULL);

-- Name: idx$pedagogical_document_child; Type: INDEX; Schema: public

CREATE INDEX "idx$pedagogical_document_child" ON public.pedagogical_document USING btree (child_id);

-- Name: idx$pedagogical_document_created_by; Type: INDEX; Schema: public

CREATE INDEX "idx$pedagogical_document_created_by" ON public.pedagogical_document USING btree (created_by);

-- Name: idx$pedagogical_document_read_person; Type: INDEX; Schema: public

CREATE INDEX "idx$pedagogical_document_read_person" ON public.pedagogical_document_read USING btree (person_id);

-- Name: idx$pedagogical_document_updated_by; Type: INDEX; Schema: public

CREATE INDEX "idx$pedagogical_document_updated_by" ON public.pedagogical_document USING btree (modified_by);

-- Name: idx$person_duplicate_of; Type: INDEX; Schema: public

CREATE INDEX "idx$person_duplicate_of" ON public.person USING btree (duplicate_of);

-- Name: idx$pin_user_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "idx$pin_user_id" ON public.employee_pin USING btree (user_id);

-- Name: idx$placement_draft_start_date; Type: INDEX; Schema: public

CREATE INDEX "idx$placement_draft_start_date" ON public.placement_draft USING btree (start_date);

-- Name: idx$placement_plan_unit_gist; Type: INDEX; Schema: public

CREATE INDEX "idx$placement_plan_unit_gist" ON public.placement_plan USING gist (unit_id, daterange(start_date, end_date, '[]'::text));

-- Name: idx$placement_unit_gist; Type: INDEX; Schema: public

CREATE INDEX "idx$placement_unit_gist" ON public.placement USING gist (unit_id, daterange(start_date, end_date, '[]'::text));

-- Name: idx$preschool_assistance_child; Type: INDEX; Schema: public

CREATE INDEX "idx$preschool_assistance_child" ON public.preschool_assistance USING btree (child_id);

-- Name: idx$questionnaire_answer_questionnaire; Type: INDEX; Schema: public

CREATE INDEX "idx$questionnaire_answer_questionnaire" ON public.holiday_questionnaire_answer USING btree (questionnaire_id);

-- Name: idx$reservation_child; Type: INDEX; Schema: public

CREATE INDEX "idx$reservation_child" ON public.attendance_reservation USING btree (child_id);

-- Name: idx$reservation_date_and_times; Type: INDEX; Schema: public

CREATE INDEX "idx$reservation_date_and_times" ON public.attendance_reservation USING btree (date, start_time, end_time);

-- Name: idx$service_application_child_id; Type: INDEX; Schema: public

CREATE INDEX "idx$service_application_child_id" ON public.service_application USING btree (child_id);

-- Name: idx$service_application_decided_by; Type: INDEX; Schema: public

CREATE INDEX "idx$service_application_decided_by" ON public.service_application USING btree (decided_by);

-- Name: idx$service_application_person_id; Type: INDEX; Schema: public

CREATE INDEX "idx$service_application_person_id" ON public.service_application USING btree (person_id);

-- Name: idx$service_application_service_need_option_id; Type: INDEX; Schema: public

CREATE INDEX "idx$service_application_service_need_option_id" ON public.service_application USING btree (service_need_option_id);

-- Name: idx$service_need_confirmed_by; Type: INDEX; Schema: public

CREATE INDEX "idx$service_need_confirmed_by" ON public.service_need USING btree (confirmed_by);

-- Name: idx$staff_attendance_external_group_id_present; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_external_group_id_present" ON public.staff_attendance_external USING btree (group_id) WHERE (departed IS NULL);

-- Name: idx$staff_attendance_external_group_id_times; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_external_group_id_times" ON public.staff_attendance_external USING btree (group_id, arrived, departed);

-- Name: idx$staff_attendance_external_group_id_times_gist; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_external_group_id_times_gist" ON public.staff_attendance_external USING gist (group_id, tstzrange(arrived, departed));

-- Name: idx$staff_attendance_external_name_times; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_external_name_times" ON public.staff_attendance_external USING btree (name, arrived, departed);

-- Name: idx$staff_attendance_plan_employee_id; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_plan_employee_id" ON public.staff_attendance_plan USING btree (employee_id);

-- Name: idx$staff_attendance_realtime_employee_departed; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_realtime_employee_departed" ON public.staff_attendance_realtime USING btree (employee_id, departed);

-- Name: idx$staff_attendance_realtime_employee_id; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_realtime_employee_id" ON public.staff_attendance_realtime USING btree (employee_id);

-- Name: idx$staff_attendance_realtime_employee_id_times; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_realtime_employee_id_times" ON public.staff_attendance_realtime USING btree (employee_id, arrived, departed);

-- Name: idx$staff_attendance_realtime_group_id_present; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_realtime_group_id_present" ON public.staff_attendance_realtime USING btree (group_id) WHERE (departed IS NULL);

-- Name: idx$staff_attendance_realtime_group_id_times; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_realtime_group_id_times" ON public.staff_attendance_realtime USING btree (group_id, arrived, departed);

-- Name: idx$staff_attendance_realtime_group_id_times_gist; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_attendance_realtime_group_id_times_gist" ON public.staff_attendance_realtime USING gist (group_id, tstzrange(arrived, departed));

-- Name: idx$staff_occupancy_coefficient_employee; Type: INDEX; Schema: public

CREATE INDEX "idx$staff_occupancy_coefficient_employee" ON public.staff_occupancy_coefficient USING btree (employee_id);

-- Name: idx$thread_participant_message; Type: INDEX; Schema: public

CREATE INDEX "idx$thread_participant_message" ON public.message_thread_participant USING btree (participant_id, folder_id, last_message_timestamp);

-- Name: idx$thread_participant_received; Type: INDEX; Schema: public

CREATE INDEX "idx$thread_participant_received" ON public.message_thread_participant USING btree (participant_id, folder_id, last_received_timestamp) WHERE (last_received_timestamp IS NOT NULL);

-- Name: idx$varda_state_errored_at; Type: INDEX; Schema: public

CREATE INDEX "idx$varda_state_errored_at" ON public.varda_state USING btree (errored_at) WHERE (errored_at IS NOT NULL);

-- Name: idx$varda_unit_errored_at; Type: INDEX; Schema: public

CREATE INDEX "idx$varda_unit_errored_at" ON public.varda_unit USING btree (errored_at) WHERE (errored_at IS NOT NULL);

-- Name: idx$voucher_value_decision_approved_by; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_decision_approved_by" ON public.voucher_value_decision USING btree (approved_by) WHERE (approved_by IS NOT NULL);

-- Name: idx$voucher_value_decision_child; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_decision_child" ON public.voucher_value_decision USING btree (child_id);

-- Name: idx$voucher_value_decision_decision_handler; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_decision_decision_handler" ON public.voucher_value_decision USING btree (decision_handler) WHERE (decision_handler IS NOT NULL);

-- Name: idx$voucher_value_decision_head_of_family; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_decision_head_of_family" ON public.voucher_value_decision USING btree (head_of_family_id);

-- Name: idx$voucher_value_decision_partner; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_decision_partner" ON public.voucher_value_decision USING btree (partner_id) WHERE (partner_id IS NOT NULL);

-- Name: idx$voucher_value_decision_placement_unit; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_decision_placement_unit" ON public.voucher_value_decision USING btree (placement_unit_id);

-- Name: idx$voucher_value_decision_valid; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_decision_valid" ON public.voucher_value_decision USING gist (daterange(valid_from, valid_to, '[]'::text));

-- Name: idx$voucher_value_decision_waiting_for_manual_sending; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_decision_waiting_for_manual_sending" ON public.voucher_value_decision USING gist (daterange(valid_from, valid_to, '[]'::text)) WHERE (status = 'WAITING_FOR_MANUAL_SENDING'::public.voucher_value_decision_status);

-- Name: idx$voucher_value_decision_waiting_for_sending; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_decision_waiting_for_sending" ON public.voucher_value_decision USING gist (daterange(valid_from, valid_to, '[]'::text)) WHERE (status = 'WAITING_FOR_SENDING'::public.voucher_value_decision_status);

-- Name: idx$voucher_value_report_decision; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_report_decision" ON public.voucher_value_report_decision USING btree (decision_id);

-- Name: idx$voucher_value_report_decision_part_snapshot_id; Type: INDEX; Schema: public

CREATE INDEX "idx$voucher_value_report_decision_part_snapshot_id" ON public.voucher_value_report_decision USING btree (voucher_value_report_snapshot_id);

-- Name: idx$voucher_value_report_snapshot_time; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "idx$voucher_value_report_snapshot_time" ON public.voucher_value_report_snapshot USING btree (year, month);

-- Name: idx_out_of_office_employee_id_period; Type: INDEX; Schema: public

CREATE INDEX idx_out_of_office_employee_id_period ON public.out_of_office USING gist (employee_id, period);

-- Name: income_person_id_idx; Type: INDEX; Schema: public

CREATE INDEX income_person_id_idx ON public.income USING btree (person_id);

-- Name: invoice_head_of_family_idx; Type: INDEX; Schema: public

CREATE INDEX invoice_head_of_family_idx ON public.invoice USING btree (head_of_family);

-- Name: only_one_finance_account; Type: INDEX; Schema: public

CREATE UNIQUE INDEX only_one_finance_account ON public.message_account USING btree (type) WHERE (type = 'FINANCE'::public.message_account_type);

-- Name: only_one_municipal_account; Type: INDEX; Schema: public

CREATE UNIQUE INDEX only_one_municipal_account ON public.message_account USING btree (type) WHERE (type = 'MUNICIPAL'::public.message_account_type);

-- Name: only_one_service_worker_account; Type: INDEX; Schema: public

CREATE UNIQUE INDEX only_one_service_worker_account ON public.message_account USING btree (type) WHERE (type = 'SERVICE_WORKER'::public.message_account_type);

-- Name: person_freetext; Type: INDEX; Schema: public

CREATE INDEX person_freetext ON public.person USING gin (freetext_vec);

-- Name: placement_child_id_start_date_end_date_idx; Type: INDEX; Schema: public

CREATE INDEX placement_child_id_start_date_end_date_idx ON public.placement USING btree (child_id, start_date, end_date);

-- Name: placement_plan_unit_id_start_date_end_date_idx; Type: INDEX; Schema: public

CREATE INDEX placement_plan_unit_id_start_date_end_date_idx ON public.placement_plan USING btree (unit_id, start_date, end_date);

-- Name: placement_unit_id_start_date_end_date_idx; Type: INDEX; Schema: public

CREATE INDEX placement_unit_id_start_date_end_date_idx ON public.placement USING btree (unit_id, start_date, end_date);

-- Name: service_need_option_one_default_per_placement_type; Type: INDEX; Schema: public

CREATE UNIQUE INDEX service_need_option_one_default_per_placement_type ON public.service_need_option USING btree (valid_placement_type) WHERE default_option;

-- Name: staff_attendance_date_idx; Type: INDEX; Schema: public

CREATE INDEX staff_attendance_date_idx ON public.staff_attendance USING btree (date);

-- Name: staff_attendance_group_id_idx; Type: INDEX; Schema: public

CREATE INDEX staff_attendance_group_id_idx ON public.staff_attendance USING btree (group_id);

-- Name: uniq$archived_process_history_row_index; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$archived_process_history_row_index" ON public.case_process_history USING btree (process_id, row_index);

-- Name: uniq$assistance_action_option_value; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$assistance_action_option_value" ON public.assistance_action_option USING btree (value);

-- Name: uniq$child_attendance_active; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$child_attendance_active" ON public.child_attendance USING btree (child_id) WHERE (end_time IS NULL);

-- Name: uniq$child_daily_note_child_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$child_daily_note_child_id" ON public.child_daily_note USING btree (child_id);

-- Name: uniq$child_document_decision_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$child_document_decision_id" ON public.child_document USING btree (decision_id) WHERE (decision_id IS NOT NULL);

-- Name: uniq$child_images_child_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$child_images_child_id" ON public.child_images USING btree (child_id);

-- Name: uniq$daycare_acl_schedule_daycare_employee; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$daycare_acl_schedule_daycare_employee" ON public.daycare_acl_schedule USING btree (daycare_id, employee_id);

-- Name: uniq$daycare_acl_schedule_employee_daycare; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$daycare_acl_schedule_employee_daycare" ON public.daycare_acl_schedule USING btree (employee_id, daycare_id);

-- Name: uniq$daycare_group_acl_employee_group; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$daycare_group_acl_employee_group" ON public.daycare_group_acl USING btree (employee_id, daycare_group_id);

-- Name: uniq$decision_number; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$decision_number" ON public.decision USING btree (number);

-- Name: uniq$evaka_user_citizen; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$evaka_user_citizen" ON public.evaka_user USING btree (citizen_id) WHERE (citizen_id IS NOT NULL);

-- Name: uniq$evaka_user_employee; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$evaka_user_employee" ON public.evaka_user USING btree (employee_id) WHERE (employee_id IS NOT NULL);

-- Name: uniq$evaka_user_mobile_device; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$evaka_user_mobile_device" ON public.evaka_user USING btree (mobile_device_id) WHERE (mobile_device_id IS NOT NULL);

-- Name: uniq$fridge_child_no_full_duplicates; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$fridge_child_no_full_duplicates" ON public.fridge_child USING btree (head_of_child, child_id, start_date, end_date, conflict);

-- Name: uniq$guardian_blocklist_guardian_child; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$guardian_blocklist_guardian_child" ON public.guardian_blocklist USING btree (guardian_id, child_id);

-- Name: uniq$invoice_replaced_invoice_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$invoice_replaced_invoice_id" ON public.invoice USING btree (replaced_invoice_id) WHERE (replaced_invoice_id IS NOT NULL);

-- Name: uniq$message_account_daycare_group; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$message_account_daycare_group" ON public.message_account USING btree (daycare_group_id) INCLUDE (active) WHERE (daycare_group_id IS NOT NULL);

-- Name: uniq$message_account_employee; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$message_account_employee" ON public.message_account USING btree (employee_id) INCLUDE (active) WHERE (employee_id IS NOT NULL);

-- Name: uniq$message_account_person; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$message_account_person" ON public.message_account USING btree (person_id) INCLUDE (active) WHERE (person_id IS NOT NULL);

-- Name: uniq$pairing_challenge_key; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$pairing_challenge_key" ON public.pairing USING btree (challenge_key);

-- Name: uniq$pairing_response_key; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$pairing_response_key" ON public.pairing USING btree (response_key);

-- Name: uniq$pedagogical_document_read_by_person; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$pedagogical_document_read_by_person" ON public.pedagogical_document_read USING btree (pedagogical_document_id, person_id);

-- Name: uniq$person_aad_object_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$person_aad_object_id" ON public.person USING btree (aad_object_id);

-- Name: uniq$process_number; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$process_number" ON public.case_process USING btree (process_definition_number, year, number);

-- Name: uniq$questionnaire_answer_child_questionnaire; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$questionnaire_answer_child_questionnaire" ON public.holiday_questionnaire_answer USING btree (child_id, questionnaire_id);

-- Name: uniq$service_application_child_id_undecided; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$service_application_child_id_undecided" ON public.service_application USING btree (child_id) WHERE (decision_status IS NULL);

-- Name: uniq$staff_occupancy_coefficient_daycare_employee; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$staff_occupancy_coefficient_daycare_employee" ON public.staff_occupancy_coefficient USING btree (daycare_id, employee_id);

-- Name: uniq$varda_state_child_id; Type: INDEX; Schema: public

CREATE UNIQUE INDEX "uniq$varda_state_child_id" ON public.varda_state USING btree (child_id);

-- Name: guardian check_blocklist; Type: TRIGGER; Schema: public

CREATE CONSTRAINT TRIGGER check_blocklist AFTER INSERT OR UPDATE ON public.guardian DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.trigger_guardian_check_guardian_blocklist();

-- Name: guardian_blocklist check_guardian; Type: TRIGGER; Schema: public

CREATE CONSTRAINT TRIGGER check_guardian AFTER INSERT OR UPDATE ON public.guardian_blocklist DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.trigger_guardian_blocklist_check_guardian();

-- Name: child_document prevent_unarchived_document_deletion; Type: TRIGGER; Schema: public

CREATE TRIGGER prevent_unarchived_document_deletion BEFORE DELETE ON public.child_document FOR EACH ROW EXECUTE FUNCTION public.trigger_prevent_unarchived_document_deletion();

-- Name: absence_application set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.absence_application FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: application set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.application FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: application_note set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.application_note FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: application_other_guardian set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.application_other_guardian FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: assistance_action set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.assistance_action FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: assistance_action_option set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.assistance_action_option FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: assistance_factor set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.assistance_factor FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: assistance_need_decision set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.assistance_need_decision FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: assistance_need_preschool_decision set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.assistance_need_preschool_decision FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: assistance_need_voucher_coefficient set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.assistance_need_voucher_coefficient FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: attachment set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.attachment FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: attendance_reservation set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.attendance_reservation FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: backup_care set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.backup_care FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: calendar_event set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.calendar_event FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: calendar_event_time set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.calendar_event_time FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: care_area set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.care_area FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: case_process set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.case_process FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: child_attendance set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.child_attendance FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: child_daily_note set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.child_daily_note FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: child_document set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.child_document FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: child_document_decision set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.child_document_decision FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: child_document_published_version set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.child_document_published_version FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: child_images set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.child_images FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: child_sticky_note set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.child_sticky_note FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: citizen_user set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.citizen_user FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: club_term set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.club_term FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: daily_service_time set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daily_service_time FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: daycare set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: daycare_acl set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_acl FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: daycare_acl_schedule set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_acl_schedule FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: daycare_assistance set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_assistance FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: daycare_caretaker set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_caretaker FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: daycare_group set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_group FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: daycare_group_acl set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_group_acl FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: daycare_group_placement set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.daycare_group_placement FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: decision set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.decision FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: document_template set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.document_template FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: employee set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.employee FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: employee_pin set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.employee_pin FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: fee_alteration set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.fee_alteration FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: fee_decision set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.fee_decision FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: fee_decision_child set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.fee_decision_child FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: fee_thresholds set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.fee_thresholds FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: finance_note set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.finance_note FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: foster_parent set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.foster_parent FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: fridge_child set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.fridge_child FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: fridge_partner set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.fridge_partner FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: group_note set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.group_note FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: guardian_blocklist set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.guardian_blocklist FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: holiday_period set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.holiday_period FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: holiday_period_questionnaire set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.holiday_period_questionnaire FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: holiday_questionnaire_answer set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.holiday_questionnaire_answer FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: income set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.income FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: income_notification set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.income_notification FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: income_statement set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.income_statement FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: invoice_correction set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.invoice_correction FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: koski_study_right set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.koski_study_right FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: message set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.message FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: message_account set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.message_account FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: message_content set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.message_content FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: message_draft set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.message_draft FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: message_recipients set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.message_recipients FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: message_thread set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.message_thread FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: message_thread_children set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.message_thread_children FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: message_thread_folder set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.message_thread_folder FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: message_thread_participant set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.message_thread_participant FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: mobile_device set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.mobile_device FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: mobile_device_push_subscription set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.mobile_device_push_subscription FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: other_assistance_measure set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.other_assistance_measure FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: pairing set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.pairing FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: payment set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.payment FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: pedagogical_document set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.pedagogical_document FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: person set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.person FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: person_email_verification set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.person_email_verification FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: placement set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.placement FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: placement_draft set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.placement_draft FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: placement_plan set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.placement_plan FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: preschool_assistance set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.preschool_assistance FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: preschool_term set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.preschool_term FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: service_application set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.service_application FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: service_need set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.service_need FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: service_need_option set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.service_need_option FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: service_need_option_fee set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.service_need_option_fee FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: service_need_option_voucher_value set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.service_need_option_voucher_value FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: setting set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.setting FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: sfi_message set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.sfi_message FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: sfi_message_event set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.sfi_message_event FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: staff_attendance set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.staff_attendance FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: staff_attendance_external set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.staff_attendance_external FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: staff_attendance_plan set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.staff_attendance_plan FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: staff_attendance_realtime set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.staff_attendance_realtime FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: staff_occupancy_coefficient set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.staff_occupancy_coefficient FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: vapid_jwt set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.vapid_jwt FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: varda_state set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.varda_state FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: varda_unit set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.varda_unit FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated_at();

-- Name: voucher_value_decision set_timestamp; Type: TRIGGER; Schema: public

CREATE TRIGGER set_timestamp BEFORE UPDATE ON public.voucher_value_decision FOR EACH ROW EXECUTE FUNCTION public.trigger_refresh_updated();

-- Name: decision update_decision_number_sequence; Type: TRIGGER; Schema: public

CREATE TRIGGER update_decision_number_sequence BEFORE INSERT ON public.decision FOR EACH ROW EXECUTE FUNCTION public.ensure_decision_number_curr_year();

-- Name: absence_application absence_application_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence_application
    ADD CONSTRAINT absence_application_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id);

-- Name: absence_application absence_application_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence_application
    ADD CONSTRAINT absence_application_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: absence_application absence_application_decided_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence_application
    ADD CONSTRAINT absence_application_decided_by_fkey FOREIGN KEY (decided_by) REFERENCES public.evaka_user(id);

-- Name: absence_application absence_application_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence_application
    ADD CONSTRAINT absence_application_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: application application_child_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_child_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: application application_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: application application_guardian_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_guardian_fkey FOREIGN KEY (guardian_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: application application_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: application application_process_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_process_id_fkey FOREIGN KEY (process_id) REFERENCES public.case_process(id);

-- Name: application application_status_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application
    ADD CONSTRAINT application_status_modified_by_fkey FOREIGN KEY (status_modified_by) REFERENCES public.evaka_user(id);

-- Name: case_process_history archived_process_history_entered_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.case_process_history
    ADD CONSTRAINT archived_process_history_entered_by_fkey FOREIGN KEY (entered_by) REFERENCES public.evaka_user(id);

-- Name: case_process_history archived_process_history_process_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.case_process_history
    ADD CONSTRAINT archived_process_history_process_id_fkey FOREIGN KEY (process_id) REFERENCES public.case_process(id) ON DELETE CASCADE;

-- Name: assistance_action_option_ref assistance_action_option_ref_action_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_action_option_ref
    ADD CONSTRAINT assistance_action_option_ref_action_id_fkey FOREIGN KEY (action_id) REFERENCES public.assistance_action(id) ON DELETE CASCADE;

-- Name: assistance_action_option_ref assistance_action_option_ref_option_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_action_option_ref
    ADD CONSTRAINT assistance_action_option_ref_option_id_fkey FOREIGN KEY (option_id) REFERENCES public.assistance_action_option(id);

-- Name: assistance_need_decision assistance_need_decision_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision
    ADD CONSTRAINT assistance_need_decision_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE RESTRICT;

-- Name: assistance_need_decision assistance_need_decision_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision
    ADD CONSTRAINT assistance_need_decision_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.employee(id);

-- Name: assistance_need_decision assistance_need_decision_decision_maker_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision
    ADD CONSTRAINT assistance_need_decision_decision_maker_employee_id_fkey FOREIGN KEY (decision_maker_employee_id) REFERENCES public.employee(id) ON DELETE RESTRICT;

-- Name: assistance_need_decision_guardian assistance_need_decision_guard_assistance_need_decision_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision_guardian
    ADD CONSTRAINT assistance_need_decision_guard_assistance_need_decision_id_fkey FOREIGN KEY (assistance_need_decision_id) REFERENCES public.assistance_need_decision(id) ON DELETE CASCADE;

-- Name: assistance_need_decision_guardian assistance_need_decision_guardian_person_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision_guardian
    ADD CONSTRAINT assistance_need_decision_guardian_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: assistance_need_decision assistance_need_decision_preparer_1_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision
    ADD CONSTRAINT assistance_need_decision_preparer_1_employee_id_fkey FOREIGN KEY (preparer_1_employee_id) REFERENCES public.employee(id) ON DELETE RESTRICT;

-- Name: assistance_need_decision assistance_need_decision_preparer_2_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision
    ADD CONSTRAINT assistance_need_decision_preparer_2_employee_id_fkey FOREIGN KEY (preparer_2_employee_id) REFERENCES public.employee(id) ON DELETE RESTRICT;

-- Name: assistance_need_decision assistance_need_decision_process_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision
    ADD CONSTRAINT assistance_need_decision_process_id_fkey FOREIGN KEY (process_id) REFERENCES public.case_process(id) ON DELETE SET NULL;

-- Name: assistance_need_decision assistance_need_decision_selected_unit_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_decision
    ADD CONSTRAINT assistance_need_decision_selected_unit_fkey FOREIGN KEY (selected_unit) REFERENCES public.daycare(id) ON DELETE RESTRICT;

-- Name: assistance_need_preschool_decision_guardian assistance_need_preschool_deci_assistance_need_decision_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision_guardian
    ADD CONSTRAINT assistance_need_preschool_deci_assistance_need_decision_id_fkey FOREIGN KEY (assistance_need_decision_id) REFERENCES public.assistance_need_preschool_decision(id) ON DELETE CASCADE;

-- Name: assistance_need_preschool_decision assistance_need_preschool_decis_decision_maker_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision
    ADD CONSTRAINT assistance_need_preschool_decis_decision_maker_employee_id_fkey FOREIGN KEY (decision_maker_employee_id) REFERENCES public.employee(id) ON DELETE RESTRICT;

-- Name: assistance_need_preschool_decision assistance_need_preschool_decision_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision
    ADD CONSTRAINT assistance_need_preschool_decision_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE RESTRICT;

-- Name: assistance_need_preschool_decision assistance_need_preschool_decision_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision
    ADD CONSTRAINT assistance_need_preschool_decision_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.employee(id);

-- Name: assistance_need_preschool_decision_guardian assistance_need_preschool_decision_guardian_person_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision_guardian
    ADD CONSTRAINT assistance_need_preschool_decision_guardian_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: assistance_need_preschool_decision assistance_need_preschool_decision_preparer_1_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision
    ADD CONSTRAINT assistance_need_preschool_decision_preparer_1_employee_id_fkey FOREIGN KEY (preparer_1_employee_id) REFERENCES public.employee(id) ON DELETE RESTRICT;

-- Name: assistance_need_preschool_decision assistance_need_preschool_decision_preparer_2_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision
    ADD CONSTRAINT assistance_need_preschool_decision_preparer_2_employee_id_fkey FOREIGN KEY (preparer_2_employee_id) REFERENCES public.employee(id) ON DELETE RESTRICT;

-- Name: assistance_need_preschool_decision assistance_need_preschool_decision_process_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision
    ADD CONSTRAINT assistance_need_preschool_decision_process_id_fkey FOREIGN KEY (process_id) REFERENCES public.case_process(id) ON DELETE SET NULL;

-- Name: assistance_need_preschool_decision assistance_need_preschool_decision_selected_unit_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_preschool_decision
    ADD CONSTRAINT assistance_need_preschool_decision_selected_unit_fkey FOREIGN KEY (selected_unit) REFERENCES public.daycare(id) ON DELETE RESTRICT;

-- Name: assistance_need_voucher_coefficient assistance_need_voucher_coefficient_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_voucher_coefficient
    ADD CONSTRAINT assistance_need_voucher_coefficient_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE RESTRICT;

-- Name: assistance_need_voucher_coefficient assistance_need_voucher_coefficient_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_need_voucher_coefficient
    ADD CONSTRAINT assistance_need_voucher_coefficient_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: attendance_reservation attendance_reservation_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attendance_reservation
    ADD CONSTRAINT attendance_reservation_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id);

-- Name: backup_care backup_care_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT backup_care_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: backup_care backup_care_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT backup_care_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: backup_pickup backup_pickup_child_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.backup_pickup
    ADD CONSTRAINT backup_pickup_child_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: calendar_event_attendee calendar_event_attendee_calendar_event_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_attendee
    ADD CONSTRAINT calendar_event_attendee_calendar_event_id_fkey FOREIGN KEY (calendar_event_id) REFERENCES public.calendar_event(id) ON DELETE CASCADE;

-- Name: calendar_event_attendee calendar_event_attendee_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_attendee
    ADD CONSTRAINT calendar_event_attendee_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;

-- Name: calendar_event_attendee calendar_event_attendee_group_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_attendee
    ADD CONSTRAINT calendar_event_attendee_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.daycare_group(id) ON DELETE CASCADE;

-- Name: calendar_event_attendee calendar_event_attendee_unit_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_attendee
    ADD CONSTRAINT calendar_event_attendee_unit_id_fkey FOREIGN KEY (unit_id) REFERENCES public.daycare(id) ON DELETE CASCADE;

-- Name: calendar_event calendar_event_content_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event
    ADD CONSTRAINT calendar_event_content_modified_by_fkey FOREIGN KEY (content_modified_by) REFERENCES public.evaka_user(id);

-- Name: calendar_event calendar_event_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event
    ADD CONSTRAINT calendar_event_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: calendar_event calendar_event_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event
    ADD CONSTRAINT calendar_event_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: calendar_event_time calendar_event_time_calendar_event_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_time
    ADD CONSTRAINT calendar_event_time_calendar_event_id_fkey FOREIGN KEY (calendar_event_id) REFERENCES public.calendar_event(id) ON DELETE CASCADE;

-- Name: calendar_event_time calendar_event_time_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_time
    ADD CONSTRAINT calendar_event_time_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id);

-- Name: calendar_event_time calendar_event_time_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_time
    ADD CONSTRAINT calendar_event_time_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: calendar_event_time calendar_event_time_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.calendar_event_time
    ADD CONSTRAINT calendar_event_time_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: child_attendance child_attendance_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_attendance
    ADD CONSTRAINT child_attendance_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE RESTRICT;

-- Name: child_attendance child_attendance_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_attendance
    ADD CONSTRAINT child_attendance_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: child_attendance child_attendance_unit_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_attendance
    ADD CONSTRAINT child_attendance_unit_id_fkey FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: child_document child_document_answered_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_answered_by_fkey FOREIGN KEY (answered_by) REFERENCES public.evaka_user(id);

-- Name: child_document child_document_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id);

-- Name: child_document child_document_content_locked_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_content_locked_by_fkey FOREIGN KEY (content_locked_by) REFERENCES public.evaka_user(id);

-- Name: child_document child_document_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: child_document_decision child_document_decision_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_decision
    ADD CONSTRAINT child_document_decision_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: child_document_decision child_document_decision_daycare_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_decision
    ADD CONSTRAINT child_document_decision_daycare_id_fkey FOREIGN KEY (daycare_id) REFERENCES public.daycare(id);

-- Name: child_document child_document_decision_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_decision_id_fkey FOREIGN KEY (decision_id) REFERENCES public.child_document_decision(id);

-- Name: child_document child_document_decision_maker_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_decision_maker_fkey FOREIGN KEY (decision_maker) REFERENCES public.employee(id);

-- Name: child_document_decision child_document_decision_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_decision
    ADD CONSTRAINT child_document_decision_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: child_document child_document_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: child_document child_document_process_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_process_id_fkey FOREIGN KEY (process_id) REFERENCES public.case_process(id) ON DELETE SET NULL;

-- Name: child_document child_document_published_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_published_by_fkey FOREIGN KEY (deprecated_published_by) REFERENCES public.evaka_user(id);

-- Name: child_document_published_version child_document_published_version_child_document_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_published_version
    ADD CONSTRAINT child_document_published_version_child_document_id_fkey FOREIGN KEY (child_document_id) REFERENCES public.child_document(id);

-- Name: child_document_published_version child_document_published_version_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_published_version
    ADD CONSTRAINT child_document_published_version_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: child_document_read child_document_read_document_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_read
    ADD CONSTRAINT child_document_read_document_id_fkey FOREIGN KEY (document_id) REFERENCES public.child_document(id) ON DELETE CASCADE;

-- Name: child_document_read child_document_read_person_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document_read
    ADD CONSTRAINT child_document_read_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id);

-- Name: child_document child_document_template_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_document
    ADD CONSTRAINT child_document_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.document_template(id);

-- Name: child_images child_images_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_images
    ADD CONSTRAINT child_images_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: child_sticky_note child_sticky_note_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_sticky_note
    ADD CONSTRAINT child_sticky_note_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id);

-- Name: daily_service_time daily_service_time_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daily_service_time
    ADD CONSTRAINT daily_service_time_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: daily_service_time_notification daily_service_time_notification_guardian_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daily_service_time_notification
    ADD CONSTRAINT daily_service_time_notification_guardian_id_fkey FOREIGN KEY (guardian_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: daycare_acl daycare_acl_employee_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_acl
    ADD CONSTRAINT daycare_acl_employee_id FOREIGN KEY (employee_id) REFERENCES public.employee(id) ON UPDATE CASCADE ON DELETE CASCADE;

-- Name: daycare_acl_schedule daycare_acl_schedule_daycare_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_acl_schedule
    ADD CONSTRAINT daycare_acl_schedule_daycare_id_fkey FOREIGN KEY (daycare_id) REFERENCES public.daycare(id) ON DELETE CASCADE;

-- Name: daycare_acl_schedule daycare_acl_schedule_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_acl_schedule
    ADD CONSTRAINT daycare_acl_schedule_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(id) ON DELETE CASCADE;

-- Name: child_daily_note daycare_daily_note_child_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child_daily_note
    ADD CONSTRAINT daycare_daily_note_child_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: daycare daycare_finance_decision_handler_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare
    ADD CONSTRAINT daycare_finance_decision_handler_fkey FOREIGN KEY (finance_decision_handler) REFERENCES public.employee(id) ON DELETE SET NULL;

-- Name: daycare_group_acl daycare_group_acl_daycare_group_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_group_acl
    ADD CONSTRAINT daycare_group_acl_daycare_group_id_fkey FOREIGN KEY (daycare_group_id) REFERENCES public.daycare_group(id) ON DELETE CASCADE;

-- Name: daycare_group_acl daycare_group_acl_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_group_acl
    ADD CONSTRAINT daycare_group_acl_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(id) ON DELETE CASCADE;

-- Name: daycare_group daycare_group_daycare_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_group
    ADD CONSTRAINT daycare_group_daycare_id_fkey FOREIGN KEY (daycare_id) REFERENCES public.daycare(id) ON DELETE CASCADE;

-- Name: employee_pin employee_pin_user_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.employee_pin
    ADD CONSTRAINT employee_pin_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.employee(id);

-- Name: employee employee_temporary_in_unit_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_temporary_in_unit_id_fkey FOREIGN KEY (temporary_in_unit_id) REFERENCES public.daycare(id);

-- Name: varda_unit evaka_daycare_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.varda_unit
    ADD CONSTRAINT evaka_daycare_id_fkey FOREIGN KEY (evaka_daycare_id) REFERENCES public.daycare(id) NOT VALID;

-- Name: family_contact family_contact_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.family_contact
    ADD CONSTRAINT family_contact_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: family_contact family_contact_contact_person_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.family_contact
    ADD CONSTRAINT family_contact_contact_person_id_fkey FOREIGN KEY (contact_person_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: fee_decision fee_decision_process_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT fee_decision_process_id_fkey FOREIGN KEY (process_id) REFERENCES public.case_process(id);

-- Name: finance_note finance_note_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.finance_note
    ADD CONSTRAINT finance_note_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: finance_note finance_note_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.finance_note
    ADD CONSTRAINT finance_note_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: finance_note finance_note_person_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.finance_note
    ADD CONSTRAINT finance_note_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: application_other_guardian fk$application; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application_other_guardian
    ADD CONSTRAINT "fk$application" FOREIGN KEY (application_id) REFERENCES public.application(id) ON DELETE CASCADE;

-- Name: attachment fk$application; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT "fk$application" FOREIGN KEY (application_id) REFERENCES public.application(id) ON DELETE SET NULL;

-- Name: application_note fk$application_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application_note
    ADD CONSTRAINT "fk$application_id" FOREIGN KEY (application_id) REFERENCES public.application(id) ON DELETE CASCADE;

-- Name: decision fk$application_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.decision
    ADD CONSTRAINT "fk$application_id" FOREIGN KEY (application_id) REFERENCES public.application(id);

-- Name: message_thread fk$application_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread
    ADD CONSTRAINT "fk$application_id" FOREIGN KEY (application_id) REFERENCES public.application(id);

-- Name: placement_plan fk$application_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement_plan
    ADD CONSTRAINT "fk$application_id" FOREIGN KEY (application_id) REFERENCES public.application(id);

-- Name: voucher_value_decision fk$approved_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT "fk$approved_by" FOREIGN KEY (approved_by) REFERENCES public.employee(id) ON UPDATE CASCADE ON DELETE CASCADE;

-- Name: invoice fk$area; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT "fk$area" FOREIGN KEY (area_id) REFERENCES public.care_area(id);

-- Name: daycare fk$care_area; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare
    ADD CONSTRAINT "fk$care_area" FOREIGN KEY (care_area_id) REFERENCES public.care_area(id);

-- Name: assistance_factor fk$child; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_factor
    ADD CONSTRAINT "fk$child" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;

-- Name: daycare_assistance fk$child; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_assistance
    ADD CONSTRAINT "fk$child" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;

-- Name: guardian_blocklist fk$child; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.guardian_blocklist
    ADD CONSTRAINT "fk$child" FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: holiday_questionnaire_answer fk$child; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.holiday_questionnaire_answer
    ADD CONSTRAINT "fk$child" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE RESTRICT;

-- Name: other_assistance_measure fk$child; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.other_assistance_measure
    ADD CONSTRAINT "fk$child" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;

-- Name: preschool_assistance fk$child; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.preschool_assistance
    ADD CONSTRAINT "fk$child" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;

-- Name: absence fk$child_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE RESTRICT;

-- Name: assistance_action fk$child_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_action
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;

-- Name: backup_care fk$child_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;

-- Name: guardian fk$child_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.guardian
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: invoice_row fk$child_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_row
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: koski_study_right fk$child_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.koski_study_right
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE CASCADE;

-- Name: placement fk$child_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT "fk$child_id" FOREIGN KEY (child_id) REFERENCES public.child(id) ON DELETE RESTRICT;

-- Name: child fk$child_id_person_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child
    ADD CONSTRAINT "fk$child_id_person_id" FOREIGN KEY (id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: evaka_user fk$citizen; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.evaka_user
    ADD CONSTRAINT "fk$citizen" FOREIGN KEY (citizen_id) REFERENCES public.person(id) ON DELETE SET NULL;

-- Name: service_need fk$confirmed_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need
    ADD CONSTRAINT "fk$confirmed_by" FOREIGN KEY (confirmed_by) REFERENCES public.evaka_user(id);

-- Name: application_note fk$created_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application_note
    ADD CONSTRAINT "fk$created_by" FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: attendance_reservation fk$created_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attendance_reservation
    ADD CONSTRAINT "fk$created_by" FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: decision fk$created_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.decision
    ADD CONSTRAINT "fk$created_by" FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: income fk$created_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income
    ADD CONSTRAINT "fk$created_by" FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: pedagogical_document fk$created_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pedagogical_document
    ADD CONSTRAINT "fk$created_by" FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: staff_occupancy_coefficient fk$daycare; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_occupancy_coefficient
    ADD CONSTRAINT "fk$daycare" FOREIGN KEY (daycare_id) REFERENCES public.daycare(id) ON DELETE CASCADE;

-- Name: daycare_acl fk$daycare_acl_daycare_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_acl
    ADD CONSTRAINT "fk$daycare_acl_daycare_id" FOREIGN KEY (daycare_id) REFERENCES public.daycare(id) ON DELETE CASCADE;

-- Name: mobile_device_push_group fk$daycare_group; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.mobile_device_push_group
    ADD CONSTRAINT "fk$daycare_group" FOREIGN KEY (daycare_group) REFERENCES public.daycare_group(id) ON DELETE CASCADE;

-- Name: daycare_group_placement fk$daycare_group_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_group_placement
    ADD CONSTRAINT "fk$daycare_group_id" FOREIGN KEY (daycare_group_id) REFERENCES public.daycare_group(id) ON DELETE CASCADE;

-- Name: mobile_device_push_group fk$device; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.mobile_device_push_group
    ADD CONSTRAINT "fk$device" FOREIGN KEY (device) REFERENCES public.mobile_device(id) ON DELETE CASCADE;

-- Name: nekku_special_diet_choices fk$diet_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_special_diet_choices
    ADD CONSTRAINT "fk$diet_id" FOREIGN KEY (diet_id) REFERENCES public.nekku_special_diet(id) ON DELETE CASCADE;

-- Name: child fk$diet_id_special_diet_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child
    ADD CONSTRAINT "fk$diet_id_special_diet_id" FOREIGN KEY (diet_id) REFERENCES public.special_diet(id);

-- Name: evaka_user fk$employee; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.evaka_user
    ADD CONSTRAINT "fk$employee" FOREIGN KEY (employee_id) REFERENCES public.employee(id) ON DELETE SET NULL;

-- Name: income_statement fk$employee; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income_statement
    ADD CONSTRAINT "fk$employee" FOREIGN KEY (handler_id) REFERENCES public.employee(id);

-- Name: staff_occupancy_coefficient fk$employee; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_occupancy_coefficient
    ADD CONSTRAINT "fk$employee" FOREIGN KEY (employee_id) REFERENCES public.employee(id) ON DELETE CASCADE;

-- Name: holiday_questionnaire_answer fk$evaka_user; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.holiday_questionnaire_answer
    ADD CONSTRAINT "fk$evaka_user" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id) ON DELETE RESTRICT;

-- Name: attachment fk$fee_alteration; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT "fk$fee_alteration" FOREIGN KEY (fee_alteration_id) REFERENCES public.fee_alteration(id) ON DELETE SET NULL;

-- Name: nekku_special_diet_choices fk$field_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_special_diet_choices
    ADD CONSTRAINT "fk$field_id" FOREIGN KEY (field_id) REFERENCES public.nekku_special_diet_field(id) ON DELETE CASCADE;

-- Name: backup_care fk$group_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "fk$group_id" FOREIGN KEY (group_id) REFERENCES public.daycare_group(id);

-- Name: daycare_caretaker fk$group_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_caretaker
    ADD CONSTRAINT "fk$group_id" FOREIGN KEY (group_id) REFERENCES public.daycare_group(id) ON DELETE CASCADE;

-- Name: staff_attendance fk$group_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance
    ADD CONSTRAINT "fk$group_id" FOREIGN KEY (group_id) REFERENCES public.daycare_group(id) ON DELETE CASCADE;

-- Name: application_other_guardian fk$guardian; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application_other_guardian
    ADD CONSTRAINT "fk$guardian" FOREIGN KEY (guardian_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: guardian_blocklist fk$guardian; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.guardian_blocklist
    ADD CONSTRAINT "fk$guardian" FOREIGN KEY (guardian_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: guardian fk$guardian_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.guardian
    ADD CONSTRAINT "fk$guardian_id" FOREIGN KEY (guardian_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: invoice fk$head_of_family; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT "fk$head_of_family" FOREIGN KEY (head_of_family) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: voucher_value_decision fk$head_of_family; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT "fk$head_of_family" FOREIGN KEY (head_of_family_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: absence fk$holiday_questionnaire; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT "fk$holiday_questionnaire" FOREIGN KEY (questionnaire_id) REFERENCES public.holiday_period_questionnaire(id) ON DELETE SET NULL;

-- Name: attachment fk$income; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT "fk$income" FOREIGN KEY (income_id) REFERENCES public.income(id) ON DELETE SET NULL;

-- Name: attachment fk$income_statement; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT "fk$income_statement" FOREIGN KEY (income_statement_id) REFERENCES public.income_statement(id) ON DELETE SET NULL;

-- Name: attachment fk$invoice; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT "fk$invoice" FOREIGN KEY (invoice_id) REFERENCES public.invoice(id) ON DELETE SET NULL;

-- Name: invoice_row fk$invoice_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_row
    ADD CONSTRAINT "fk$invoice_id" FOREIGN KEY (invoice_id) REFERENCES public.invoice(id) ON DELETE CASCADE;

-- Name: child fk$meal_texture_id_meal_texture_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.child
    ADD CONSTRAINT "fk$meal_texture_id_meal_texture_id" FOREIGN KEY (meal_texture_id) REFERENCES public.meal_texture(id);

-- Name: attachment fk$message_content; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT "fk$message_content" FOREIGN KEY (message_content_id) REFERENCES public.message_content(id) ON DELETE SET NULL;

-- Name: application_note fk$message_content_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application_note
    ADD CONSTRAINT "fk$message_content_id" FOREIGN KEY (message_content_id) REFERENCES public.message_content(id);

-- Name: attachment fk$message_draft; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT "fk$message_draft" FOREIGN KEY (message_draft_id) REFERENCES public.message_draft(id) ON DELETE SET NULL;

-- Name: evaka_user fk$mobile_device; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.evaka_user
    ADD CONSTRAINT "fk$mobile_device" FOREIGN KEY (mobile_device_id) REFERENCES public.mobile_device(id) ON DELETE SET NULL;

-- Name: mobile_device_push_subscription fk$mobile_device; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.mobile_device_push_subscription
    ADD CONSTRAINT "fk$mobile_device" FOREIGN KEY (device) REFERENCES public.mobile_device(id) ON DELETE CASCADE;

-- Name: pairing fk$mobile_device; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pairing
    ADD CONSTRAINT "fk$mobile_device" FOREIGN KEY (mobile_device_id) REFERENCES public.mobile_device(id) ON DELETE CASCADE;

-- Name: absence fk$modified_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.absence
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: assistance_factor fk$modified_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_factor
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id) ON DELETE CASCADE;

-- Name: daycare_assistance fk$modified_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_assistance
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id) ON DELETE CASCADE;

-- Name: fee_alteration fk$modified_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_alteration
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: income fk$modified_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: other_assistance_measure fk$modified_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.other_assistance_measure
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id) ON DELETE CASCADE;

-- Name: pedagogical_document fk$modified_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pedagogical_document
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: preschool_assistance fk$modified_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.preschool_assistance
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id) ON DELETE CASCADE;

-- Name: daycare_group fk$nekku_customer_number; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_group
    ADD CONSTRAINT "fk$nekku_customer_number" FOREIGN KEY (nekku_customer_number) REFERENCES public.nekku_customer(number);

-- Name: fridge_partner fk$other_indx; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT "fk$other_indx" FOREIGN KEY (partnership_id, other_indx) REFERENCES public.fridge_partner(partnership_id, indx) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

-- Name: voucher_value_decision fk$partner; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT "fk$partner" FOREIGN KEY (partner_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: payment fk$payment_sent_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT "fk$payment_sent_by" FOREIGN KEY (sent_by) REFERENCES public.evaka_user(id);

-- Name: payment fk$payment_unit; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.payment
    ADD CONSTRAINT "fk$payment_unit" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: attachment fk$pedagogical_document; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT "fk$pedagogical_document" FOREIGN KEY (pedagogical_document_id) REFERENCES public.pedagogical_document(id) ON DELETE SET NULL;

-- Name: citizen_user fk$person; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.citizen_user
    ADD CONSTRAINT "fk$person" FOREIGN KEY (id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: income_statement fk$person; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income_statement
    ADD CONSTRAINT "fk$person" FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: person_email_verification fk$person; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.person_email_verification
    ADD CONSTRAINT "fk$person" FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: fee_alteration fk$person_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_alteration
    ADD CONSTRAINT "fk$person_id" FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: income fk$person_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income
    ADD CONSTRAINT "fk$person_id" FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: daycare_group_placement fk$placement_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.daycare_group_placement
    ADD CONSTRAINT "fk$placement_id" FOREIGN KEY (daycare_placement_id) REFERENCES public.placement(id);

-- Name: application fk$primary_preferred_unit; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application
    ADD CONSTRAINT "fk$primary_preferred_unit" FOREIGN KEY (primary_preferred_unit) REFERENCES public.daycare(id);

-- Name: holiday_questionnaire_answer fk$questionnaire; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.holiday_questionnaire_answer
    ADD CONSTRAINT "fk$questionnaire" FOREIGN KEY (questionnaire_id) REFERENCES public.holiday_period_questionnaire(id) ON DELETE RESTRICT;

-- Name: invoice fk$replaced_invoice_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT "fk$replaced_invoice_id" FOREIGN KEY (replaced_invoice_id) REFERENCES public.invoice(id);

-- Name: decision fk$resolved_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.decision
    ADD CONSTRAINT "fk$resolved_by" FOREIGN KEY (resolved_by) REFERENCES public.evaka_user(id);

-- Name: invoice fk$sent_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT "fk$sent_by" FOREIGN KEY (sent_by) REFERENCES public.evaka_user(id);

-- Name: fee_decision_child fk$service_need_option_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision_child
    ADD CONSTRAINT "fk$service_need_option_id" FOREIGN KEY (service_need_option_id) REFERENCES public.service_need_option(id);

-- Name: password_blacklist fk$source; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.password_blacklist
    ADD CONSTRAINT "fk$source" FOREIGN KEY (source) REFERENCES public.password_blacklist_source(id) ON DELETE CASCADE;

-- Name: placement fk$terminated_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT "fk$terminated_by" FOREIGN KEY (terminated_by) REFERENCES public.evaka_user(id);

-- Name: invoice_row fk$unit; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_row
    ADD CONSTRAINT "fk$unit" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: backup_care fk$unit_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.backup_care
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: decision fk$unit_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.decision
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: koski_study_right fk$unit_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.koski_study_right
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: placement fk$unit_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: placement_plan fk$unit_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement_plan
    ADD CONSTRAINT "fk$unit_id" FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: application_note fk$updated_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.application_note
    ADD CONSTRAINT "fk$updated_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: assistance_action fk$updated_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.assistance_action
    ADD CONSTRAINT "fk$updated_by" FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: attachment fk$uploaded_by; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT "fk$uploaded_by" FOREIGN KEY (uploaded_by) REFERENCES public.evaka_user(id);

-- Name: out_of_office fk_out_of_office_employee_id; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.out_of_office
    ADD CONSTRAINT fk_out_of_office_employee_id FOREIGN KEY (employee_id) REFERENCES public.employee(id);

-- Name: foster_parent foster_parent_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.foster_parent
    ADD CONSTRAINT foster_parent_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id);

-- Name: foster_parent foster_parent_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.foster_parent
    ADD CONSTRAINT foster_parent_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: foster_parent foster_parent_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.foster_parent
    ADD CONSTRAINT foster_parent_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: foster_parent foster_parent_parent_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.foster_parent
    ADD CONSTRAINT foster_parent_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.person(id);

-- Name: fridge_child fridge_child_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT fridge_child_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: fridge_child fridge_child_created_by_application_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT fridge_child_created_by_application_fkey FOREIGN KEY (created_by_application) REFERENCES public.application(id);

-- Name: fridge_child fridge_child_created_by_user_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT fridge_child_created_by_user_fkey FOREIGN KEY (created_by_user) REFERENCES public.evaka_user(id);

-- Name: fridge_child fridge_child_head_of_child_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT fridge_child_head_of_child_fkey FOREIGN KEY (head_of_child) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: fridge_child fridge_child_modified_by_user_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_child
    ADD CONSTRAINT fridge_child_modified_by_user_fkey FOREIGN KEY (modified_by_user) REFERENCES public.evaka_user(id);

-- Name: fridge_partner fridge_partner_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT fridge_partner_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: fridge_partner fridge_partner_created_from_application_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT fridge_partner_created_from_application_fkey FOREIGN KEY (created_from_application) REFERENCES public.application(id);

-- Name: fridge_partner fridge_partner_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT fridge_partner_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: fridge_partner fridge_partner_person_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fridge_partner
    ADD CONSTRAINT fridge_partner_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: group_note group_note_group_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.group_note
    ADD CONSTRAINT group_note_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.daycare_group(id);

-- Name: income income_application_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income
    ADD CONSTRAINT income_application_id_fkey FOREIGN KEY (application_id) REFERENCES public.application(id);

-- Name: income_statement income_statement_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income_statement
    ADD CONSTRAINT income_statement_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: income_statement income_statement_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.income_statement
    ADD CONSTRAINT income_statement_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: invoice invoice_codebtor_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice
    ADD CONSTRAINT invoice_codebtor_fkey FOREIGN KEY (codebtor) REFERENCES public.person(id);

-- Name: invoice_correction invoice_correction_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_correction
    ADD CONSTRAINT invoice_correction_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id);

-- Name: invoice_correction invoice_correction_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_correction
    ADD CONSTRAINT invoice_correction_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: invoice_correction invoice_correction_head_of_family_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_correction
    ADD CONSTRAINT invoice_correction_head_of_family_id_fkey FOREIGN KEY (head_of_family_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: invoice_correction invoice_correction_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_correction
    ADD CONSTRAINT invoice_correction_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: invoice_correction invoice_correction_unit_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_correction
    ADD CONSTRAINT invoice_correction_unit_id_fkey FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: invoice_row invoice_row_correction_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoice_row
    ADD CONSTRAINT invoice_row_correction_id_fkey FOREIGN KEY (correction_id) REFERENCES public.invoice_correction(id);

-- Name: invoiced_fee_decision invoiced_fee_decision_fee_decision_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoiced_fee_decision
    ADD CONSTRAINT invoiced_fee_decision_fee_decision_id_fkey FOREIGN KEY (fee_decision_id) REFERENCES public.fee_decision(id) ON DELETE CASCADE;

-- Name: invoiced_fee_decision invoiced_fee_decision_invoice_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.invoiced_fee_decision
    ADD CONSTRAINT invoiced_fee_decision_invoice_id_fkey FOREIGN KEY (invoice_id) REFERENCES public.invoice(id) ON DELETE CASCADE;

-- Name: message_account message_account_daycare_group_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_account
    ADD CONSTRAINT message_account_daycare_group_id_fkey FOREIGN KEY (daycare_group_id) REFERENCES public.daycare_group(id);

-- Name: message_account message_account_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_account
    ADD CONSTRAINT message_account_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(id);

-- Name: message_account message_account_person_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_account
    ADD CONSTRAINT message_account_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: message_content message_content_author_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_content
    ADD CONSTRAINT message_content_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.message_account(id);

-- Name: message message_content_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_content_id_fkey FOREIGN KEY (content_id) REFERENCES public.message_content(id) ON DELETE CASCADE;

-- Name: message_draft message_draft_account_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_draft
    ADD CONSTRAINT message_draft_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.message_account(id) ON DELETE CASCADE;

-- Name: message_recipients message_recipients_message_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_recipients
    ADD CONSTRAINT message_recipients_message_id_fkey FOREIGN KEY (message_id) REFERENCES public.message(id) ON DELETE CASCADE;

-- Name: message_recipients message_recipients_recipient_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_recipients
    ADD CONSTRAINT message_recipients_recipient_id_fkey FOREIGN KEY (recipient_id) REFERENCES public.message_account(id) ON DELETE CASCADE;

-- Name: message message_sender_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES public.message_account(id);

-- Name: message_thread_children message_thread_children_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_children
    ADD CONSTRAINT message_thread_children_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id);

-- Name: message_thread_children message_thread_children_thread_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_children
    ADD CONSTRAINT message_thread_children_thread_id_fkey FOREIGN KEY (thread_id) REFERENCES public.message_thread(id) ON DELETE CASCADE;

-- Name: message_thread_folder message_thread_folder_owner_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_folder
    ADD CONSTRAINT message_thread_folder_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.message_account(id) ON DELETE CASCADE;

-- Name: message message_thread_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_thread_id_fkey FOREIGN KEY (thread_id) REFERENCES public.message_thread(id) ON DELETE CASCADE;

-- Name: message_thread_participant message_thread_participant_folder_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_participant
    ADD CONSTRAINT message_thread_participant_folder_id_fkey FOREIGN KEY (folder_id) REFERENCES public.message_thread_folder(id) ON DELETE CASCADE;

-- Name: message_thread_participant message_thread_participant_participant_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_participant
    ADD CONSTRAINT message_thread_participant_participant_id_fkey FOREIGN KEY (participant_id) REFERENCES public.message_account(id) ON DELETE CASCADE;

-- Name: message_thread_participant message_thread_participant_thread_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.message_thread_participant
    ADD CONSTRAINT message_thread_participant_thread_id_fkey FOREIGN KEY (thread_id) REFERENCES public.message_thread(id) ON DELETE CASCADE;

-- Name: mobile_device mobile_device_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.mobile_device
    ADD CONSTRAINT mobile_device_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(id);

-- Name: mobile_device mobile_device_unit_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.mobile_device
    ADD CONSTRAINT mobile_device_unit_id_fkey FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: nekku_customer_type nekku_customer_type_customer_number_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_customer_type
    ADD CONSTRAINT nekku_customer_type_customer_number_fkey FOREIGN KEY (customer_number) REFERENCES public.nekku_customer(number) ON DELETE CASCADE;

-- Name: nekku_special_diet_choices nekku_special_diet_choices_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_special_diet_choices
    ADD CONSTRAINT nekku_special_diet_choices_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id);

-- Name: nekku_special_diet_field nekku_special_diet_field_diet_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_special_diet_field
    ADD CONSTRAINT nekku_special_diet_field_diet_id_fkey FOREIGN KEY (diet_id) REFERENCES public.nekku_special_diet(id) ON DELETE CASCADE;

-- Name: nekku_special_diet_option nekku_special_diet_option_field_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.nekku_special_diet_option
    ADD CONSTRAINT nekku_special_diet_option_field_id_fkey FOREIGN KEY (field_id) REFERENCES public.nekku_special_diet_field(id) ON DELETE CASCADE;

-- Name: fee_decision new_fee_decision_approved_by_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT new_fee_decision_approved_by_id_fkey FOREIGN KEY (approved_by_id) REFERENCES public.employee(id);

-- Name: fee_decision_child new_fee_decision_child_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision_child
    ADD CONSTRAINT new_fee_decision_child_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: fee_decision_child new_fee_decision_child_fee_decision_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision_child
    ADD CONSTRAINT new_fee_decision_child_fee_decision_id_fkey FOREIGN KEY (fee_decision_id) REFERENCES public.fee_decision(id) ON DELETE CASCADE;

-- Name: fee_decision_child new_fee_decision_child_placement_unit_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision_child
    ADD CONSTRAINT new_fee_decision_child_placement_unit_id_fkey FOREIGN KEY (placement_unit_id) REFERENCES public.daycare(id);

-- Name: fee_decision new_fee_decision_decision_handler_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT new_fee_decision_decision_handler_id_fkey FOREIGN KEY (decision_handler_id) REFERENCES public.employee(id);

-- Name: fee_decision new_fee_decision_head_of_family_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT new_fee_decision_head_of_family_id_fkey FOREIGN KEY (head_of_family_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: fee_decision new_fee_decision_partner_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.fee_decision
    ADD CONSTRAINT new_fee_decision_partner_id_fkey FOREIGN KEY (partner_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: service_need new_service_need_option_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need
    ADD CONSTRAINT new_service_need_option_id_fkey FOREIGN KEY (option_id) REFERENCES public.service_need_option(id);

-- Name: service_need new_service_need_placement_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need
    ADD CONSTRAINT new_service_need_placement_id_fkey FOREIGN KEY (placement_id) REFERENCES public.placement(id) ON DELETE CASCADE;

-- Name: pairing pairing_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pairing
    ADD CONSTRAINT pairing_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(id);

-- Name: pairing pairing_unit_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pairing
    ADD CONSTRAINT pairing_unit_id_fkey FOREIGN KEY (unit_id) REFERENCES public.daycare(id);

-- Name: pedagogical_document pedagogical_document_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pedagogical_document
    ADD CONSTRAINT pedagogical_document_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id);

-- Name: pedagogical_document_read pedagogical_document_read_pedagogical_document_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pedagogical_document_read
    ADD CONSTRAINT pedagogical_document_read_pedagogical_document_id_fkey FOREIGN KEY (pedagogical_document_id) REFERENCES public.pedagogical_document(id);

-- Name: pedagogical_document_read pedagogical_document_read_person_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.pedagogical_document_read
    ADD CONSTRAINT pedagogical_document_read_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id);

-- Name: person person_duplicate_of_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.person
    ADD CONSTRAINT person_duplicate_of_fkey FOREIGN KEY (duplicate_of) REFERENCES public.person(id);

-- Name: placement placement_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT placement_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: placement_draft placement_draft_application_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement_draft
    ADD CONSTRAINT placement_draft_application_id_fkey FOREIGN KEY (application_id) REFERENCES public.application(id) ON DELETE CASCADE;

-- Name: placement_draft placement_draft_created_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement_draft
    ADD CONSTRAINT placement_draft_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.evaka_user(id);

-- Name: placement_draft placement_draft_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement_draft
    ADD CONSTRAINT placement_draft_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: placement_draft placement_draft_unit_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement_draft
    ADD CONSTRAINT placement_draft_unit_id_fkey FOREIGN KEY (unit_id) REFERENCES public.daycare(id) ON DELETE CASCADE;

-- Name: placement placement_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT placement_modified_by_fkey FOREIGN KEY (modified_by) REFERENCES public.evaka_user(id);

-- Name: placement placement_source_application_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT placement_source_application_id_fkey FOREIGN KEY (source_application_id) REFERENCES public.application(id);

-- Name: placement placement_source_service_application_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.placement
    ADD CONSTRAINT placement_source_service_application_id_fkey FOREIGN KEY (source_service_application_id) REFERENCES public.service_application(id);

-- Name: service_application service_application_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_application
    ADD CONSTRAINT service_application_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.child(id);

-- Name: service_application service_application_decided_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_application
    ADD CONSTRAINT service_application_decided_by_fkey FOREIGN KEY (decided_by) REFERENCES public.employee(id);

-- Name: service_application service_application_person_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_application
    ADD CONSTRAINT service_application_person_id_fkey FOREIGN KEY (person_id) REFERENCES public.person(id);

-- Name: service_application service_application_service_need_option_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_application
    ADD CONSTRAINT service_application_service_need_option_id_fkey FOREIGN KEY (service_need_option_id) REFERENCES public.service_need_option(id);

-- Name: service_need_option_fee service_need_option_fee_service_need_option_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need_option_fee
    ADD CONSTRAINT service_need_option_fee_service_need_option_id_fkey FOREIGN KEY (service_need_option_id) REFERENCES public.service_need_option(id);

-- Name: service_need_option_voucher_value service_need_option_voucher_value_service_need_option_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.service_need_option_voucher_value
    ADD CONSTRAINT service_need_option_voucher_value_service_need_option_id_fkey FOREIGN KEY (service_need_option_id) REFERENCES public.service_need_option(id);

-- Name: sfi_message sfi_message_decision_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_message
    ADD CONSTRAINT sfi_message_decision_id_fkey FOREIGN KEY (decision_id) REFERENCES public.decision(id);

-- Name: sfi_message sfi_message_document_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_message
    ADD CONSTRAINT sfi_message_document_id_fkey FOREIGN KEY (document_id) REFERENCES public.child_document(id);

-- Name: sfi_message_event sfi_message_event_message_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_message_event
    ADD CONSTRAINT sfi_message_event_message_id_fkey FOREIGN KEY (message_id) REFERENCES public.sfi_message(id);

-- Name: sfi_message sfi_message_fee_decision_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_message
    ADD CONSTRAINT sfi_message_fee_decision_id_fkey FOREIGN KEY (fee_decision_id) REFERENCES public.fee_decision(id);

-- Name: sfi_message sfi_message_guardian_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_message
    ADD CONSTRAINT sfi_message_guardian_id_fkey FOREIGN KEY (guardian_id) REFERENCES public.person(id);

-- Name: sfi_message sfi_message_voucher_value_decision_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.sfi_message
    ADD CONSTRAINT sfi_message_voucher_value_decision_id_fkey FOREIGN KEY (voucher_value_decision_id) REFERENCES public.voucher_value_decision(id);

-- Name: staff_attendance_external staff_attendance_external_group_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_external
    ADD CONSTRAINT staff_attendance_external_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.daycare_group(id);

-- Name: staff_attendance_plan staff_attendance_plan_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_plan
    ADD CONSTRAINT staff_attendance_plan_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(id);

-- Name: staff_attendance_realtime staff_attendance_realtime_arrived_added_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_realtime
    ADD CONSTRAINT staff_attendance_realtime_arrived_added_by_fkey FOREIGN KEY (arrived_added_by) REFERENCES public.evaka_user(id);

-- Name: staff_attendance_realtime staff_attendance_realtime_arrived_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_realtime
    ADD CONSTRAINT staff_attendance_realtime_arrived_modified_by_fkey FOREIGN KEY (arrived_modified_by) REFERENCES public.evaka_user(id);

-- Name: staff_attendance_realtime staff_attendance_realtime_departed_added_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_realtime
    ADD CONSTRAINT staff_attendance_realtime_departed_added_by_fkey FOREIGN KEY (departed_added_by) REFERENCES public.evaka_user(id);

-- Name: staff_attendance_realtime staff_attendance_realtime_departed_modified_by_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_realtime
    ADD CONSTRAINT staff_attendance_realtime_departed_modified_by_fkey FOREIGN KEY (departed_modified_by) REFERENCES public.evaka_user(id);

-- Name: staff_attendance_realtime staff_attendance_realtime_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_realtime
    ADD CONSTRAINT staff_attendance_realtime_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(id);

-- Name: staff_attendance_realtime staff_attendance_realtime_group_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.staff_attendance_realtime
    ADD CONSTRAINT staff_attendance_realtime_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.daycare_group(id);

-- Name: titania_errors titania_errors_employee_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.titania_errors
    ADD CONSTRAINT titania_errors_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employee(id);

-- Name: varda_state varda_state_child_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.varda_state
    ADD CONSTRAINT varda_state_child_id_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE CASCADE;

-- Name: voucher_value_decision voucher_value_decision_child_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT voucher_value_decision_child_fkey FOREIGN KEY (child_id) REFERENCES public.person(id) ON DELETE RESTRICT;

-- Name: voucher_value_decision voucher_value_decision_decision_handler_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT voucher_value_decision_decision_handler_fkey FOREIGN KEY (decision_handler) REFERENCES public.employee(id);

-- Name: voucher_value_decision voucher_value_decision_placement_unit_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT voucher_value_decision_placement_unit_fkey FOREIGN KEY (placement_unit_id) REFERENCES public.daycare(id);

-- Name: voucher_value_decision voucher_value_decision_process_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_decision
    ADD CONSTRAINT voucher_value_decision_process_id_fkey FOREIGN KEY (process_id) REFERENCES public.case_process(id);

-- Name: voucher_value_report_decision voucher_value_report_decision_decision_id_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_report_decision
    ADD CONSTRAINT voucher_value_report_decision_decision_id_fkey FOREIGN KEY (decision_id) REFERENCES public.voucher_value_decision(id);

-- Name: voucher_value_report_decision voucher_value_report_decision_voucher_value_report_snapsho_fkey; Type: FK CONSTRAINT; Schema: public

ALTER TABLE ONLY public.voucher_value_report_decision
    ADD CONSTRAINT voucher_value_report_decision_voucher_value_report_snapsho_fkey FOREIGN KEY (voucher_value_report_snapshot_id) REFERENCES public.voucher_value_report_snapshot(id);

