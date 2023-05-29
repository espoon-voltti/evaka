// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.function.ServerResponse

object EspooBiPoc {
    val getAreas =
        streamingCsvRoute<BiArea> { sql("""
SELECT id, created, updated, name
FROM care_area
""") }

    val getUnits =
        streamingCsvRoute<BiUnit> {
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
        streamingCsvRoute<BiGroup> {
            sql("""
SELECT id, name, start_date, end_date
FROM daycare_group
""")
        }

    val getChildren =
        streamingCsvRoute<BiChild> {
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
        streamingCsvRoute<BiPlacement> {
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
        streamingCsvRoute<BiGroupPlacement> {
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
        streamingCsvRoute<BiAbsence> {
            sql(
                """
SELECT id, modified_at AS updated, child_id AS child, date, category
FROM absence
"""
            )
        }

    val getGroupCaretakerAllocations =
        streamingCsvRoute<BiGroupCaretakerAllocation> {
            sql(
                """
SELECT id, created, updated, group_id AS "group", amount, start_date, end_date
FROM daycare_caretaker
"""
            )
        }

    val getApplications =
        streamingCsvRoute<BiApplication> {
            sql(
                """
SELECT
    a.id, a.created, a.updated, a.type, a.transferapplication, a.origin, a.status, a.additionaldaycareapplication, a.sentdate,
    (
      SELECT array_agg(e::UUID)
      FROM jsonb_array_elements_text(document -> 'apply' -> 'preferredUnits') e
    ) AS preferredUnits,
    (document ->> 'preferredStartDate') :: date AS preferred_start_date,
    (document ->> 'urgent') :: boolean AS urgent,
    (document -> 'careDetails' ->> 'assistanceNeeded') :: boolean AS assistanceNeeded,
    (document ->> 'extendedCare') :: boolean AS shift_care
FROM application a
JOIN application_form af ON a.id = af.application_id AND latest IS TRUE
WHERE status != 'CREATED'
"""
            )
        }

    val getDecisions =
        streamingCsvRoute<BiDecision> {
            sql(
                """
SELECT id, created, updated, application_id AS application, sent_date, status, type, start_date, end_date
FROM decision
"""
            )
        }

    val getServiceNeedOptions =
        streamingCsvRoute<BiServiceNeedOption> {
            sql(
                """
SELECT id, created, updated, name_fi AS name, valid_placement_type
FROM service_need_option
"""
            )
        }

    val getServiceNeeds =
        streamingCsvRoute<BiServiceNeed> {
            sql(
                """
SELECT id, created, updated, option_id AS option, placement_id AS placement, start_date, end_date, shift_care
FROM service_need
"""
            )
        }

    val getFeeDecisions =
        streamingCsvRoute<BiFeeDecision> {
            sql(
                """
SELECT
  id, created, updated, decision_number, status, decision_type AS type, family_size,
  lower(valid_during) AS valid_from, upper(valid_during) - 1 AS valid_to
FROM fee_decision
WHERE status != 'DRAFT'
"""
            )
        }

    val getFeeDecisionChildren =
        streamingCsvRoute<BiFeeDecisionChild> {
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
        streamingCsvRoute<BiVoucherValueDecision> {
            sql(
                """
SELECT
    id, created, updated, decision_number, status, decision_type AS type, family_size, valid_from, valid_to,
    placement_unit_id AS placement_unit, service_need_fee_description_fi AS service_need_fee_description,
    service_need_voucher_value_description_fi AS service_need_voucher_value_description,
    final_co_payment
FROM voucher_value_decision
WHERE status != 'DRAFT'
"""
            )
        }

    val getCurriculumTemplates =
        streamingCsvRoute<BiCurriculumTemplate> {
            sql(
                """
SELECT
    id, created, updated, lower(valid) AS valid_from, upper(valid) - 1 AS valid_to, type, language, name
FROM curriculum_template
"""
            )
        }

    val getCurriculumDocuments =
        streamingCsvRoute<BiCurriculumDocument> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child, template_id AS template
FROM curriculum_document
"""
            )
        }

    val getPedagogicalDocuments =
        streamingCsvRoute<BiPedagogicalDocument> {
            sql(
                """
SELECT
    id, created, updated, child_id AS child
FROM pedagogical_document
"""
            )
        }
}

private fun printEspooBiCsvField(value: Any?): String =
    // Espoo BI tooling doesn't know how to handle RFC4180-style CSV double quote escapes, so our
    // only option is to remove quotes from the original data completely
    printCsvField(value).replace("\"", "")

typealias StreamingCsvRoute = (db: Database, user: AuthenticatedUser.Integration) -> ServerResponse

private inline fun <reified T : Any> streamingCsvRoute(
    crossinline f: QuerySql.Builder<T>.() -> QuerySql<T>
): StreamingCsvRoute = { db, _ ->
    ServerResponse.ok().build { _, response ->
        db.connect { dbc ->
            dbc.read { tx ->
                val records =
                    toCsvRecords(
                        ::printEspooBiCsvField,
                        T::class,
                        tx.createQuery { f() }.mapTo<T>().asSequence()
                    )
                val charset = CSV_CHARSET
                response.setHeader("Content-Type", "text/csv;charset=${charset.name()}")
                val writer = response.outputStream.bufferedWriter(charset)
                records.forEach {
                    writer.append(it)
                    writer.append(CSV_RECORD_SEPARATOR)
                }
                writer.flush()
            }
        }
        ModelAndView()
    }
}
