// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ServiceNeedOption
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.utils.enumSetOf
import java.time.LocalDate
import org.jdbi.v3.json.Json
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val defaultApplicationStatuses =
    enumSetOf(
        ApplicationStatus.SENT,
        ApplicationStatus.WAITING_PLACEMENT,
        ApplicationStatus.WAITING_CONFIRMATION,
        ApplicationStatus.WAITING_DECISION,
        ApplicationStatus.WAITING_MAILING,
        ApplicationStatus.WAITING_UNIT_CONFIRMATION,
        ApplicationStatus.ACTIVE
    )

@RestController
class PlacementSketchingReportController(private val accessControl: AccessControl) {
    @GetMapping(
        "/reports/placement-sketching", // deprecated
        "/employee/reports/placement-sketching"
    )
    fun getPlacementSketchingReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) placementStartDate: LocalDate,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        earliestPreferredStartDate: LocalDate?,
        @RequestParam applicationStatus: Set<ApplicationStatus>? = null,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        earliestApplicationSentDate: LocalDate? = null,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        latestApplicationSentDate: LocalDate? = null
    ): List<PlacementSketchingReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_PLACEMENT_SKETCHING_REPORT
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getPlacementSketchingReportRows(
                        placementStartDate,
                        earliestPreferredStartDate,
                        applicationStatus ?: emptySet(),
                        earliestApplicationSentDate,
                        latestApplicationSentDate
                    )
                }
            }
            .also {
                Audit.PlacementSketchingReportRead.log(
                    meta =
                        mapOf(
                            "placementStartDate" to placementStartDate,
                            "earliestPreferredStartDate" to earliestPreferredStartDate,
                            "count" to it.size
                        )
                )
            }
    }
}

private fun Database.Read.getPlacementSketchingReportRows(
    placementStartDate: LocalDate,
    earliestPreferredStartDate: LocalDate?,
    applicationStatuses: Set<ApplicationStatus>,
    earliestApplicationSentDate: LocalDate?,
    latestApplicationSentDate: LocalDate?
): List<PlacementSketchingReportRow> {
    return createQuery {
            sql(
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
    start_date <= ${bind(placementStartDate)}
    AND end_date >= ${bind(placementStartDate)}
    AND placement.unit_id = daycare.id
)
SELECT
    care_area.name AS area_name,
    daycare.id AS requested_unit_id,
    daycare.name AS requested_unit_name,
    application.id AS application_id,
    application.status AS application_status,
    application.childId,
    application.childfirstname,
    application.childlastname,
    child.date_of_birth AS child_dob,
    application.childstreetaddr,
    application.childpostalcode,
    active_placements.daycare_name AS current_unit_name,
    active_placements.daycare_id AS current_unit_id,
    application.daycareassistanceneeded AS assistance_needed,
    application.preparatoryeducation,
    application.siblingbasis,
    application.connecteddaycare,
    application.serviceneedoption,
    application.startDate AS preferred_start_date,
    application.sentdate,
    application.guardianphonenumber,
    application.document->'guardian'->>'email' AS guardian_email,
    (SELECT array_agg(name) as other_preferred_units
     FROM daycare 
     JOIN (SELECT unnest(preferredUnits) 
           FROM application_view view 
           WHERE view.id = application.id) pu ON daycare.id = pu.unnest) AS other_preferred_units,
    (application.document -> 'additionalDetails' ->> 'otherInfo') as additionalInfo,
    unrestricted_corrected_child_address_details.childMovingDate,
    COALESCE(unrestricted_corrected_child_address_details.childCorrectedStreetAddress,'') as childCorrectedStreetAddress,
    COALESCE(unrestricted_corrected_child_address_details.childCorrectedPostalCode,'') as childCorrectedPostalCode,
    COALESCE(unrestricted_corrected_child_address_details.childCorrectedCity,'') as childCorrectedCity
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
LEFT JOIN LATERAL (select (application.document -> 'child' ->> 'childMovingDate')::date            as childMovingDate,
                           application.document -> 'child' -> 'correctingAddress' ->> 'street'     as childCorrectedStreetAddress,
                           application.document -> 'child' -> 'correctingAddress' ->> 'postalCode' as childCorrectedPostalCode,
                           application.document -> 'child' -> 'correctingAddress' ->> 'city'       as childCorrectedCity
) as unrestricted_corrected_child_address_details on child.restricted_details_enabled IS FALSE
WHERE
    (application.startDate >= ${bind(earliestPreferredStartDate)} OR application.startDate IS NULL)
    AND application.status = ANY(${bind(applicationStatuses.ifEmpty { defaultApplicationStatuses })}::application_status_type[])
    AND application.type = 'PRESCHOOL'
    AND daterange(${bind(earliestApplicationSentDate)}, ${bind(latestApplicationSentDate)}, '[]') @> application.sentdate
ORDER BY
    area_name, requested_unit_name, application.childlastname, application.childfirstname
        """
            )
        }
        .toList<PlacementSketchingReportRow>()
}

data class PlacementSketchingReportRow(
    val areaName: String,
    val requestedUnitId: DaycareId,
    val requestedUnitName: String,
    val childId: ChildId,
    val childFirstName: String,
    val childLastName: String,
    val childDob: LocalDate,
    val childStreetAddr: String?,
    val childPostalCode: String?,
    val applicationId: ApplicationId,
    val applicationStatus: ApplicationStatus,
    val currentUnitName: String?,
    val currentUnitId: DaycareId?,
    val assistanceNeeded: Boolean?,
    val preparatoryEducation: Boolean?,
    val siblingBasis: Boolean?,
    val connectedDaycare: Boolean?,
    @Json val serviceNeedOption: ServiceNeedOption?,
    val preferredStartDate: LocalDate,
    val sentDate: LocalDate,
    val guardianPhoneNumber: String?,
    val guardianEmail: String?,
    val otherPreferredUnits: List<String>,
    val additionalInfo: String,
    val childMovingDate: LocalDate?,
    val childCorrectedStreetAddress: String,
    val childCorrectedPostalCode: String,
    val childCorrectedCity: String
)
