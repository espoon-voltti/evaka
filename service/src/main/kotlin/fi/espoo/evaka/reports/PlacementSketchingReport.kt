// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class PlacementSketchingReportController(private val acl: AccessControlList) {
    @GetMapping("/reports/placement-sketching")
    fun getPlacementSketchingReport(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam("placementStartDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) placementStartDate: LocalDate,
        @RequestParam("earliestPreferredStartDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) earliestPreferredStartDate: LocalDate?
    ): ResponseEntity<List<PlacementSketchingReportRow>> {
        Audit.PlacementSketchingReportRead.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)
        return db.read { ResponseEntity.ok(it.getPlacementSketchingReportRows(placementStartDate, earliestPreferredStartDate)) }
    }
}

private fun Database.Read.getPlacementSketchingReportRows(
    placementStartDate: LocalDate,
    earliestPreferredStartDate: LocalDate?
): List<PlacementSketchingReportRow> {
    // language=sql
    val sql =
        """
WITH active_placements AS (
SELECT
    placement.child_id AS child_id,
    daycare.name AS daycare_name,
    daycare.id AS daycare_id
FROM
    placement,
    daycare
WHERE
    start_date <= :placementStartDate
    AND end_date >= :placementStartDate
    AND placement.unit_id = daycare.id
)
SELECT
    care_area.name AS area_name,
    daycare.id AS requested_unit_id,
    daycare.name AS requested_unit_name,
    application.id AS application_id,
    application.childId,
    application.childfirstname,
    application.childlastname,
    child.date_of_birth AS child_dob,
    application.childstreetaddr,
    active_placements.daycare_name AS current_unit_name,
    active_placements.daycare_id AS current_unit_id,
    application.daycareassistanceneeded AS assistance_needed,
    application.preparatoryeducation,
    application.siblingbasis,
    application.connecteddaycare,
    application.startDate AS preferred_start_date,
    application.sentdate,
    application.guardianphonenumber,
    form.document->'guardian'->>'email' AS guardian_email,
    (SELECT array_agg(name) as other_preferred_units
     FROM daycare JOIN (SELECT unnest(preferredUnits) FROM application WHERE application.id = application_id) pu ON daycare.id = pu.unnest) AS other_preferred_units
FROM
    daycare
LEFT JOIN
    care_area ON care_area.id = daycare.care_area_id
LEFT JOIN
    application_view application ON application.preferredunit = daycare.id
LEFT JOIN
    active_placements ON application.childid = active_placements.child_id
LEFT JOIN
    person AS child ON application.childid = child.id
LEFT JOIN
    application_form AS form ON application.id = form.application_id AND form.latest is TRUE 
WHERE
    (application.startDate >= :earliestPreferredStartDate OR application.startDate IS NULL)
    AND application.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION, ACTIVE}'::application_status_type[])
    AND application.type = 'PRESCHOOL'
ORDER BY
    area_name, requested_unit_name
        """.trimIndent()
    return createQuery(sql)
        .bind("placementStartDate", placementStartDate)
        .bind("earliestPreferredStartDate", earliestPreferredStartDate)
        .mapTo<PlacementSketchingReportRow>()
        .toList()
}

data class PlacementSketchingReportRow(
    val areaName: String,
    val requestedUnitId: UUID,
    val requestedUnitName: String,
    val childId: UUID,
    val childFirstName: String?,
    val childLastName: String?,
    val childDob: String?,
    val childStreetAddr: String?,
    val applicationId: UUID?,
    val currentUnitName: String?,
    val currentUnitId: UUID?,
    val assistanceNeeded: Boolean?,
    val preparatoryEducation: Boolean?,
    val siblingBasis: Boolean?,
    val connectedDaycare: Boolean?,
    val preferredStartDate: LocalDate,
    val sentDate: LocalDate,
    val guardianPhoneNumber: String?,
    val guardianEmail: String?,
    val otherPreferredUnits: List<String>
)
