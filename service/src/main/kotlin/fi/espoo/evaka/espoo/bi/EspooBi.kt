// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import kotlin.reflect.KClass

object EspooBi {
    val getAreas =
        csvQuery<BiArea> { sql("""
SELECT id, created, updated, name
FROM care_area
""") }

    val getUnits =
        csvQuery<BiUnit> {
            sql(
                """
SELECT
    daycare.id, created, updated, care_area_id AS area, daycare.name, provider_type, cost_center,
    'CLUB' = ANY(type) AS club, 'PRESCHOOL' = ANY(type) AS preschool, 'PREPARATORY_EDUCATION' = ANY(type) AS preparatory_education,
    (CASE WHEN 'GROUP_FAMILY' = ANY(type) THEN 'GROUP_FAMILY'
          WHEN 'FAMILY' = ANY(type) THEN 'FAMILY'
          WHEN 'CENTRE' = ANY(type) THEN 'DAYCARE'
     END) AS daycare,
     opening_date, closing_date, language, unit_manager_name, round_the_clock
FROM daycare
"""
            )
        }

    val getGroups =
        csvQuery<BiGroup> {
            sql(
                """
SELECT id, daycare_id AS unit, name, start_date, end_date
FROM daycare_group
"""
            )
        }

    val getChildren =
        csvQuery<BiChild> {
            sql(
                """
SELECT
    id, created, updated, date_of_birth AS birth_date, language, language_at_home,
    restricted_details_enabled AS vtj_non_disclosure, postal_code, post_office
FROM child
JOIN person USING (id)
"""
            )
        }

    val getPlacements =
        csvQuery<BiPlacement> {
            sql(
                """
SELECT id, created, updated, child_id AS child, unit_id AS unit, start_date, end_date, FALSE AS is_backup, type
FROM placement

UNION ALL

SELECT id, created, updated, child_id AS child, unit_id AS unit, start_date, end_date, TRUE AS is_backup, NULL AS type
FROM backup_care
"""
            )
        }

    val getGroupPlacements =
        csvQuery<BiGroupPlacement> {
            sql(
                """
SELECT id, created, updated, daycare_placement_id AS placement, daycare_group_id AS "group", start_date, end_date
FROM daycare_group_placement

UNION ALL

SELECT id, created, updated, id AS placement, group_id AS "group", start_date, end_date
FROM backup_care
WHERE group_id IS NOT NULL
"""
            )
        }

    val getAbsences =
        csvQuery<BiAbsence> {
            sql(
                """
SELECT id, modified_at AS updated, child_id AS child, date, category
FROM absence
"""
            )
        }

    val getGroupCaretakerAllocations =
        csvQuery<BiGroupCaretakerAllocation> {
            sql(
                """
SELECT id, created, updated, group_id AS "group", amount, start_date, end_date
FROM daycare_caretaker
"""
            )
        }

    val getApplications =
        csvQuery<BiApplication> {
            sql(
                """
SELECT
    id, created, updated, type, transferapplication, origin, status, additionaldaycareapplication, sentdate,
    (
      SELECT array_agg(e::UUID)
      FROM jsonb_array_elements_text(document -> 'apply' -> 'preferredUnits') e
    ) AS preferredUnits,
    (document ->> 'preferredStartDate') :: date AS preferred_start_date,
    (document ->> 'urgent') :: boolean AS urgent,
    coalesce(document -> 'careDetails' ->> 'assistanceNeeded', document -> 'clubCare' ->> 'assistanceNeeded') :: boolean AS assistanceNeeded,
    (document ->> 'extendedCare') :: boolean AS shift_care
FROM application a
WHERE status != 'CREATED'
"""
            )
        }

    val getDecisions =
        csvQuery<BiDecision> {
            sql(
                """
SELECT id, created, updated, application_id AS application, sent_date, status, type, start_date, end_date
FROM decision
"""
            )
        }

    val getServiceNeedOptions =
        csvQuery<BiServiceNeedOption> {
            sql(
                """
SELECT id, created, updated, name_fi AS name, valid_placement_type
FROM service_need_option
"""
            )
        }

    val getServiceNeeds =
        csvQuery<BiServiceNeed> {
            sql(
                """
SELECT id, created, updated, option_id AS option, placement_id AS placement, start_date, end_date, shift_care = 'FULL' as shift_care
FROM service_need
"""
            )
        }

    val getFeeDecisions =
        csvQuery<BiFeeDecision> {
            sql(
                """
SELECT
  id, created, updated, decision_number, status, decision_type AS type, family_size,
  lower(valid_during) AS valid_from, upper(valid_during) - 1 AS valid_to
FROM fee_decision
WHERE status NOT IN ('DRAFT', 'IGNORED')
"""
            )
        }

    val getFeeDecisionChildren =
        csvQuery<BiFeeDecisionChild> {
            sql(
                """
SELECT
  id, created, updated, fee_decision_id AS fee_decision, placement_unit_id AS placement_unit,
  service_need_option_id AS service_need_option,
  service_need_description_fi AS service_need_description, final_fee
FROM fee_decision_child
"""
            )
        }

    val getVoucherValueDecisions =
        csvQuery<BiVoucherValueDecision> {
            sql(
                """
SELECT
    id, created, updated, decision_number, status, decision_type AS type, family_size, valid_from, valid_to,
    placement_unit_id AS placement_unit, service_need_fee_description_fi AS service_need_fee_description,
    service_need_voucher_value_description_fi AS service_need_voucher_value_description,
    final_co_payment
FROM voucher_value_decision
WHERE status NOT IN ('DRAFT', 'IGNORED')
"""
            )
        }

    val getCurriculumTemplates =
        csvQuery<BiCurriculumTemplate> {
            sql(
                """
SELECT
    id, created, updated, lower(valid) AS valid_from, upper(valid) - 1 AS valid_to, type, language, name
FROM curriculum_template
"""
            )
        }

    val getCurriculumDocuments =
        csvQuery<BiCurriculumDocument> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child, template_id AS template
FROM curriculum_document
"""
            )
        }

    val getPedagogicalDocuments =
        csvQuery<BiPedagogicalDocument> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child
FROM pedagogical_document
"""
            )
        }

    val getAssistanceFactors =
        csvQuery<BiAssistanceFactor> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child, capacity_factor,
    lower(valid_during) AS start_date, upper(valid_during) - 1 AS end_date
FROM assistance_factor
"""
            )
        }

    val getDaycareAssistanceEntries =
        csvQuery<BiDaycareAssistanceEntry> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child, level,
    lower(valid_during) AS start_date, upper(valid_during) - 1 AS end_date
FROM daycare_assistance
"""
            )
        }

    val getPreschoolAssistanceEntries =
        csvQuery<BiPreschoolAssistanceEntry> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child, level,
    lower(valid_during) AS start_date, upper(valid_during) - 1 AS end_date
FROM preschool_assistance
"""
            )
        }

    val getAssistanceNeedVoucherCoefficients =
        csvQuery<BiAssistanceNeedVoucherCoefficient> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child, coefficient,
    lower(validity_period) AS start_date, upper(validity_period) - 1 AS end_date
FROM assistance_need_voucher_coefficient
"""
            )
        }

    val getAssistanceActions =
        csvQuery<BiAssistanceAction> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child, start_date, end_date,
    other_action != '' AS has_other_action
FROM assistance_action
"""
            )
        }

    val getAssistanceActionOptionRefs =
        csvQuery<BiAssistanceActionOptionRef> {
            sql(
                """
SELECT
    action_id AS action, option.value AS option
FROM assistance_action_option_ref
JOIN assistance_action_option option ON option.id = option_id
"""
            )
        }

    val getAssistanceNeedDaycareDecisions =
        csvQuery<BiAssistanceNeedDaycareDecision> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child, selected_unit AS unit,
    lower(validity_period) AS valid_from, upper(validity_period) - 1 AS valid_to,
    status,
    'ASSISTANCE_ENDS' = ANY(assistance_levels) AS assistance_ends,
    'ASSISTANCE_SERVICES_FOR_TIME' = ANY(assistance_levels) AS assistance_services_for_time,
    'ENHANCED_ASSISTANCE' = ANY(assistance_levels) AS enhanced_assistance,
    'SPECIAL_ASSISTANCE' = ANY(assistance_levels) AS special_assistance
FROM assistance_need_decision
WHERE status != 'DRAFT'
"""
            )
        }

    val getAssistanceNeedPreschoolDecisions =
        csvQuery<BiAssistanceNeedPreschoolDecision> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child, selected_unit AS unit,
    type, valid_from, status
FROM assistance_need_preschool_decision
WHERE status != 'DRAFT'
"""
            )
        }
}

private fun printEspooBiCsvField(value: Any?): String =
    // Espoo BI tooling doesn't know how to handle RFC4180-style CSV double quote escapes, so our
    // only option is to remove quotes from the original data completely
    printCsvField(value).replace("\"", "")

interface CsvQuery {
    operator fun <R> invoke(tx: Database.Read, useResults: (records: Sequence<String>) -> R): R
}

class StreamingCsvQuery<T : Any>(
    private val clazz: KClass<T>,
    private val query: (Database.Read) -> Database.Result<T>
) : CsvQuery {
    override operator fun <R> invoke(
        tx: Database.Read,
        useResults: (records: Sequence<String>) -> R
    ): R =
        query(tx).useSequence { rows ->
            useResults(toCsvRecords(::printEspooBiCsvField, clazz, rows))
        }
}

private const val QUERY_STREAM_CHUNK_SIZE = 10_000

private inline fun <reified T : Any> csvQuery(
    crossinline f: QuerySql.Builder<T>.() -> QuerySql<T>
): CsvQuery =
    StreamingCsvQuery(T::class) { tx ->
        tx.createQuery { f() }.setFetchSize(QUERY_STREAM_CHUNK_SIZE).mapTo<T>()
    }
