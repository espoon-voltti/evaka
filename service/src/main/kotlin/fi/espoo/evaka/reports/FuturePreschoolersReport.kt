package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareGroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class FuturePreschoolersReport(private val accessControl: AccessControl) {
    @GetMapping("/reports/future-preschoolers")
    fun getFuturePreschoolers(
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
                    it.getFuturePreschoolerRows()
                }
            }
            .also { Audit.FuturePreschoolers.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/reports/future-preschoolers/units")
    fun getPreschoolUnits(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam municipal: Boolean
    ): List<PreschoolUnitReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_FUTURE_PRESCHOOLERS
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getPreschoolUnitRows(municipal)
                }
            }
            .also { Audit.FuturePreschoolers.log(meta = mapOf("count" to it.size)) }
    }
}

const val preschoolSelectionAge = 5

private fun Database.Read.getFuturePreschoolerRows(): List<FuturePreschoolersReportRow> =
    createQuery<DatabaseTable> {
            sql(
                """
SELECT p.id, 
    p.social_security_number AS childSsn,
    p.last_name AS childLastName,
    p.first_name AS childFirstName,
    p.street_address AS childAddress,
    p.postal_code AS childPostalCode,
    upper(p.post_office) AS childPostOffice,
    d.name AS unitName,
    d.street_address AS unitAddress,
    d.postal_code AS unitPostalCode,
    upper(d.post_office) AS unitPostOffice,
    NULL as unitArea, -- cannot be obtained from current evaka dataset, care area is not precise enough
    g1.last_name AS guardian1LastName,
    g1.first_name AS guardian1FirstName,
    g1.street_address AS guardian1Address,
    g1.postal_code AS guardian1PostalCode,
    upper(g1.post_office) AS guardian1PostOffice, 
    g1.phone AS guardian1Phone,
    g1.email AS guardian1Email,
    g2.last_name AS guardian2LastName, 
    g2.first_name AS guardian2FirstName, 
    g2.street_address AS guardian2Address,
    g2.postal_code AS guardian2PostalCode,
    upper(g2.post_office) AS guardian2PostOffice, 
    g2.phone AS guardian2Phone,
    g2.email AS guardian2Email,
    CASE WHEN sn.shift_care = 'FULL'::shift_care_type OR sn.shift_care = 'INTERMITTENT'::shift_care_type THEN true ELSE false END AS shiftCare,
    NULL AS languageEmphasisGroup, -- cannot be obtained from current evaka dataset
    CASE WHEN sno.name_fi like 'Kaksivuotinen%' THEN true ELSE false END AS twoYearPreschool
FROM person p
JOIN placement pl ON pl.child_id = p.id
JOIN daycare d ON d.id = pl.unit_id
LEFT JOIN (
    SELECT id, last_name, first_name, street_address, postal_code, post_office, phone, email
    FROM person
) g1 ON g1.id = (SELECT head_of_child FROM fridge_child WHERE child_id = p.id)
LEFT JOIN (
    SELECT id, last_name, first_name, street_address, postal_code, post_office, phone, email
    FROM person
) g2 ON g2.id = (SELECT person_id FROM fridge_partner WHERE partnership_id =
    (SELECT partnership_id FROM fridge_partner WHERE person_id = g1.id) AND person_id != g1.id)
LEFT JOIN service_need sn ON sn.placement_id = p.id and sn.start_date < current_date AND sn.end_date >= current_date
LEFT JOIN service_need_option sno on sn.option_id = sno.id
WHERE CASE WHEN sno.name_fi like 'Kaksivuotinen%' THEN
    extract(year from current_date) -  extract(year from p.date_of_birth) = $preschoolSelectionAge - 1
ELSE
    extract(year from current_date) -  extract(year from p.date_of_birth) = $preschoolSelectionAge
END
            """
                    .trimIndent()
            )
        }
        .mapTo<FuturePreschoolersReportRow>()
        .toList()

private fun Database.Read.getPreschoolUnitRows(municipal: Boolean): List<PreschoolUnitReportRow> =
    createQuery<DatabaseTable> {
            sql(
                """
SELECT dg.id, 
    d.name AS unitName, 
    dg.name AS groupName, 
    d.street_address AS address, 
    d.postal_code AS postalCode, 
    d.post_office as postOffice, 
    (SELECT count(employee_id) FROM daycare_group_acl WHERE daycare_group_id = dg.id) * 7 AS groupSize,
    NULL AS amongSchool, -- cannot be obtained from current dataset 
    d.round_the_clock AS shiftCare,
    NULL AS languageEmphaseis -- cannot be obtained from current dataset
FROM daycare d
JOIN daycare_group dg on d.id = dg.daycare_id
WHERE d.type && '{PRESCHOOL}'::care_types[] AND 
CASE WHEN :municipal THEN d.provider_type = 'MUNICIPAL' ELSE d.provider_type != 'MUNICIPAL' END 
            """
                    .trimIndent()
            )
        }
        .bind("municipal", municipal)
        .mapTo<PreschoolUnitReportRow>()
        .toList()

data class FuturePreschoolersReportRow(
    val id: ChildId,
    val childSsn: String,
    val childLastName: String,
    val childFirstName: String,
    val childAddress: String,
    val childPostalCode: String,
    val childPostOffice: String,
    val unitName: String,
    val unitAddress: String,
    val unitPostalCode: String,
    val unitPostOffice: String,
    val unitArea: String,
    val guardian1LastName: String,
    val guardian1FirstName: String,
    val guardian1Address: String,
    val guardian1PostalCode: String,
    val guardian1PostOffice: String,
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

data class PreschoolUnitReportRow(
    val id: DaycareGroupId,
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
