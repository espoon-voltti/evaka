// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class FuturePreschoolersReport(private val accessControl: AccessControl) {
    @GetMapping("/reports/future-preschoolers")
    fun getFuturePreschoolersReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock
    ): List<FuturePreschoolersReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FUTURE_PRESCHOOLERS
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getFuturePreschoolerRows(clock.today())
                }
            }
            .also { Audit.FuturePreschoolers.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/reports/future-preschoolers/groups")
    fun getFuturePreschoolersGroupsReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam municipal: Boolean
    ): List<PreschoolGroupsReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FUTURE_PRESCHOOLERS
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getPreschoolGroupsRows(clock.today(), municipal)
                }
            }
            .also { Audit.FuturePreschoolers.log(meta = mapOf("count" to it.size)) }
    }
}

const val preschoolSelectionAge = 5

fun Database.Read.getFuturePreschoolerRows(today: LocalDate): List<FuturePreschoolersReportRow> =
    createQuery {
            sql(
                """
SELECT p.id, 
    p.social_security_number AS child_ssn,
    p.last_name AS child_last_name,
    p.first_name AS child_first_name,
    p.street_address AS child_address,
    p.postal_code AS child_postal_code,
    upper(p.post_office) AS child_post_office,
    d.name AS unit_name,
    d.street_address AS unit_address,
    d.postal_code AS unit_postal_code,
    upper(d.post_office) AS unit_post_office,
    NULL as unit_area, -- cannot be obtained from current evaka dataset, care area is not precise enough
    g1.last_name AS guardian_1_last_name,
    g1.first_name AS guardian_1_first_name,
    g1.street_address AS guardian_1_address,
    g1.postal_code AS guardian_1_postal_code,
    upper(g1.post_office) AS guardian_1_post_office, 
    g1.phone AS guardian_1_phone,
    g1.email AS guardian_1_email,
    g2.last_name AS guardian_2_last_name, 
    g2.first_name AS guardian_2_first_name, 
    g2.street_address AS guardian_2_address,
    g2.postal_code AS guardian_2_postal_code,
    upper(g2.post_office) AS guardian_2_post_office, 
    g2.phone AS guardian_2_phone,
    g2.email AS guardian_2_email,
    CASE WHEN sn.shift_care = 'FULL'::shift_care_type OR sn.shift_care = 'INTERMITTENT'::shift_care_type THEN true ELSE false END AS shift_care,
    NULL AS language_emphasis_group, -- cannot be obtained from current evaka dataset
    CASE WHEN sno.name_fi like 'Kaksivuotinen%' THEN true ELSE false END AS two_year_preschool
FROM person p
JOIN placement pl ON pl.child_id = p.id AND pl.start_date < :today AND pl.end_date >= :today
JOIN daycare d ON d.id = pl.unit_id
LEFT JOIN (
    SELECT id, last_name, first_name, street_address, postal_code, post_office, phone, email
    FROM person
) g1 ON g1.id = (
    SELECT guardian_id AS id FROM guardian WHERE child_id = p.id
    UNION
    SELECT parent_id AS id FROM foster_parent WHERE child_id = p.id AND valid_during @> :today
    LIMIT 1
    )
LEFT JOIN (
    SELECT id, last_name, first_name, street_address, postal_code, post_office, phone, email
    FROM person
) g2 ON g2.id = (
    SELECT guardian_id AS id FROM guardian WHERE child_id = p.id
    UNION
    SELECT parent_id AS id FROM foster_parent WHERE child_id = p.id AND valid_during @> :today
    LIMIT 1
    OFFSET 1
    )
LEFT JOIN service_need sn ON sn.placement_id = p.id and sn.start_date < :today AND sn.end_date >= :today
LEFT JOIN service_need_option sno on sn.option_id = sno.id
WHERE CASE WHEN sno.name_fi like 'Kaksivuotinen%' THEN
    extract(year from :today) -  extract(year from p.date_of_birth) = $preschoolSelectionAge - 1
ELSE
    extract(year from :today) -  extract(year from p.date_of_birth) = $preschoolSelectionAge
END
            """
                    .trimIndent()
            )
        }
        .bind("today", today)
        .toList<FuturePreschoolersReportRow>()

fun Database.Read.getPreschoolGroupsRows(
    today: LocalDate,
    municipal: Boolean
): List<PreschoolGroupsReportRow> =
    createQuery {
            sql(
                """
SELECT dg.id, 
    d.name AS unit_name, 
    dg.name AS group_name, 
    d.street_address AS address, 
    d.postal_code AS postal_code, 
    d.post_office as post_office, 
    (SELECT count(id) FROM daycare_caretaker WHERE group_id = dg.id AND start_date < :today AND end_date >= :today) * 7 AS group_size,
    NULL AS among_school, -- cannot be obtained from current dataset 
    d.round_the_clock AS shift_care,
    NULL AS language_emphaseis -- cannot be obtained from current dataset
FROM daycare d
JOIN daycare_group dg on d.id = dg.daycare_id
WHERE d.type && '{PRESCHOOL}'::care_types[] AND
dg.start_date <= :today AND (dg.end_date IS NULL OR dg.end_date >= :today) AND
CASE WHEN :municipal THEN d.provider_type = 'MUNICIPAL' ELSE d.provider_type != 'MUNICIPAL' END 
            """
                    .trimIndent()
            )
        }
        .bind("today", today)
        .bind("municipal", municipal)
        .toList<PreschoolGroupsReportRow>()

data class FuturePreschoolersReportRow(
    val id: ChildId,
    val childSsn: String?,
    val childLastName: String,
    val childFirstName: String,
    val childAddress: String,
    val childPostalCode: String,
    val childPostOffice: String,
    val unitName: String,
    val unitAddress: String,
    val unitPostalCode: String,
    val unitPostOffice: String,
    val unitArea: String?,
    val guardian1LastName: String?,
    val guardian1FirstName: String?,
    val guardian1Address: String?,
    val guardian1PostalCode: String?,
    val guardian1PostOffice: String?,
    val guardian1Phone: String?,
    val guardian1Email: String?,
    val guardian2LastName: String?,
    val guardian2FirstName: String?,
    val guardian2Address: String?,
    val guardian2PostalCode: String?,
    val guardian2PostOffice: String?,
    val guardian2Phone: String?,
    val guardian2Email: String?,
    val shiftCare: Boolean,
    val languageEmphasisGroup: String?,
    val twoYearPreschool: Boolean
)

data class PreschoolGroupsReportRow(
    val id: GroupId,
    val unitName: String,
    val groupName: String,
    val address: String,
    val postalCode: String,
    val postOffice: String,
    val groupSize: Int,
    val amongSchool: Boolean?,
    val shiftCare: Boolean,
    val languageEmphasis: Boolean?
)
