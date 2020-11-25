// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.DAYCARE_ONLY
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.PREPARATORY_DAYCARE
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.PREPARATORY_ONLY
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.PRESCHOOL_DAYCARE
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.PRESCHOOL_ONLY
import fi.espoo.evaka.application.enduser.objectMapper
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.StatementContext
import org.postgresql.util.PGobject
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max

enum class ApplicationSortColumn {
    APPLICATION_TYPE,
    CHILD_NAME,
    DUE_DATE,
    START_DATE,
    STATUS
}

enum class ApplicationBasis {
    ADDITIONAL_INFO,
    SIBLING_BASIS,
    ASSISTANCE_NEED,
    CLUB_CARE,
    DAYCARE,
    EXTENDED_CARE,
    DUPLICATE_APPLICATION
}

enum class ApplicationSortDirection {
    ASC,
    DESC
}

fun insertApplication(
    h: Handle,
    guardianId: UUID,
    childId: UUID,
    origin: ApplicationOrigin,
    hideFromGuardian: Boolean = false,
    sentDate: LocalDate? = null
): UUID {
    // language=sql
    val sql =
        """
        INSERT INTO application (status, guardian_id, child_id, origin, hidefromguardian, sentdate)
        VALUES ('CREATED'::application_status_type, :guardianId, :childId, :origin::application_origin_type, :hideFromGuardian, :sentDate)
        RETURNING id
        """.trimIndent()

    return h.createUpdate(sql)
        .bind("guardianId", guardianId)
        .bind("childId", childId)
        .bind("origin", origin)
        .bind("hideFromGuardian", hideFromGuardian)
        .bind("sentDate", sentDate)
        .executeAndReturnGeneratedKeys()
        .mapTo<UUID>()
        .first()
}

fun duplicateApplicationExists(
    h: Handle,
    childId: UUID,
    guardianId: UUID,
    type: ApplicationType
): Boolean {
    // language=sql
    val sql =
        """
            SELECT 1
            FROM application_view
            WHERE 
                childid = :childId AND 
                guardianid = :guardianId AND
                type = :type AND 
                hidefromguardian = false AND
                status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
        """.trimIndent()
    return h.createQuery(sql)
        .bind("childId", childId)
        .bind("guardianId", guardianId)
        .bind("type", type)
        .mapTo<Int>()
        .list()
        .isNotEmpty()
}

fun fetchApplicationSummaries(
    h: Handle,
    user: AuthenticatedUser,
    page: Int,
    pageSize: Int,
    sortBy: ApplicationSortColumn,
    sortDir: ApplicationSortDirection,
    areas: List<String>,
    units: List<UUID>,
    basis: List<ApplicationBasis>,
    type: ApplicationTypeToggle,
    preschoolType: List<ApplicationPreschoolTypeToggle>,
    statuses: List<ApplicationStatusOption>,
    dateType: List<ApplicationDateType>,
    distinctions: List<ApplicationDistinctions>,
    periodStart: LocalDate?,
    periodEnd: LocalDate?,
    searchTerms: String = "",
    transferApplications: TransferApplicationFilter,
    authorizedUnitFilterList: AclAuthorization?,
    onlyAuthorizedToViewApplicationsWithAssistanceNeed: Boolean
): ApplicationSummaries {

    val params = mapOf(
        "page" to page,
        "pageSize" to pageSize,
        "area" to areas.toTypedArray(),
        "units" to units.toTypedArray(),
        "authorizedUnitFilterList" to authorizedUnitFilterList?.ids?.toTypedArray(),
        "documentType" to type.toString().toLowerCase(),
        "preschoolType" to preschoolType.toTypedArray(),
        "status" to statuses.map { it.toStatus() }.toTypedArray(),
        "distinctions" to distinctions.map { it.toString() }.toTypedArray(),
        "dateType" to dateType.map { it.toString() }.toTypedArray(),
        "periodStart" to periodStart,
        "periodEnd" to periodEnd,
        "userId" to user.id
    )

    val (freeTextQuery, freeTextParams) = freeTextSearchQuery(listOf("child"), searchTerms)

    val conditions = listOfNotNull(
        "a.status = ANY(:status::application_status_type[])",
        if (areas.isNotEmpty()) "ca.short_name = ANY(:area)" else null,
        if (basis.isNotEmpty()) basis.map { applicationBasis ->
            when (applicationBasis) {
                ApplicationBasis.ADDITIONAL_INFO -> """(
                            (f.document -> 'additionalDetails' ->> 'dietType') != '' OR 
                            (f.document -> 'additionalDetails' ->> 'otherInfo') != '' OR 
                            (f.document -> 'additionalDetails' ->> 'allergyType') != '')
                """.trimIndent()
                ApplicationBasis.SIBLING_BASIS -> "(f.document -> 'apply' ->> 'siblingBasis')::boolean = true"
                ApplicationBasis.ASSISTANCE_NEED -> "(f.document -> 'careDetails' ->> 'assistanceNeeded')::boolean = true"
                ApplicationBasis.CLUB_CARE -> "(f.document -> 'clubCare' ->> 'assistanceNeeded')::boolean = true"
                ApplicationBasis.DAYCARE -> "(f.document ->> 'wasOnDaycare')::boolean = true"
                ApplicationBasis.EXTENDED_CARE -> "(f.document ->> 'extendedCare')::boolean = true"
                ApplicationBasis.DUPLICATE_APPLICATION -> "array_length(duplicates.duplicate_application_ids, 1) > 0"
            }
        }.joinToString("\nAND ") else null,
        if (type != ApplicationTypeToggle.ALL) "f.document ->> 'type' = :documentType" else null,
        if (type == ApplicationTypeToggle.PRESCHOOL)
            """
            ('$PRESCHOOL_ONLY' = ANY(:preschoolType) OR ((f.document->'careDetails'->>'preparatory')::boolean OR (f.document->>'connectedDaycare')::boolean))
            AND ('$PRESCHOOL_DAYCARE' = ANY(:preschoolType) OR ((f.document->'careDetails'->>'preparatory')::boolean OR NOT (f.document->>'connectedDaycare')::boolean OR a.additionalDaycareApplication))
            AND ('$PREPARATORY_ONLY' = ANY(:preschoolType) OR (NOT (f.document->'careDetails'->>'preparatory')::boolean OR (f.document->>'connectedDaycare')::boolean))
            AND ('$PREPARATORY_DAYCARE' = ANY(:preschoolType) OR (NOT (f.document->'careDetails'->>'preparatory')::boolean OR NOT (f.document->>'connectedDaycare')::boolean OR a.additionalDaycareApplication))
            AND ('$DAYCARE_ONLY' = ANY(:preschoolType) OR NOT a.additionalDaycareApplication)
            """.trimIndent()
        else null,
        if (distinctions.contains(ApplicationDistinctions.SECONDARY)) "f.preferredunits && :units" else if (units.isNotEmpty()) "d.id = ANY(:units)" else null,
        if (authorizedUnitFilterList != null) "d.id = ANY(:authorizedUnitFilterList)" else null,
        if (onlyAuthorizedToViewApplicationsWithAssistanceNeed) "(f.document->'careDetails'->>'assistanceNeeded')::boolean = true" else null,
        if ((periodStart != null || periodEnd != null) && dateType.contains(ApplicationDateType.DUE)) "daterange(:periodStart, :periodEnd, '[]') @> a.dueDate" else null,
        if ((periodStart != null || periodEnd != null) && dateType.contains(ApplicationDateType.START)) "daterange(:periodStart, :periodEnd, '[]') @> (f.document ->> 'preferredStartDate')::date" else null,
        if ((periodStart != null || periodEnd != null) && dateType.contains(ApplicationDateType.ARRIVAL)) "daterange(:periodStart, :periodEnd, '[]') @> a.sentdate" else null,
        if (searchTerms.isNotBlank()) freeTextQuery else null,
        when (transferApplications) {
            TransferApplicationFilter.TRANSFER_ONLY -> "a.transferApplication"
            TransferApplicationFilter.NO_TRANSFER -> "NOT a.transferApplication"
            else -> null
        }
    )

    val andWhere = conditions.takeIf { it.isNotEmpty() }?.joinToString(" AND ")?.let { " AND $it" } ?: ""
    //language=sql
    val sql =
        """
        SELECT
            a.id,
            child.first_name,
            child.last_name,
            child.date_of_birth,
            child.social_security_number,
            f.document,
            f.document ->> 'type' as type,
            a.duedate,
            f.document ->> 'preferredStartDate' as preferredStartDate,
            f.document -> 'apply' -> 'preferredUnits' as preferredUnits,
            a.status AS application_status,
            a.origin,
            a.transferapplication,
            a.additionaldaycareapplication,
            a.checkedbyadmin,
            (
                COALESCE((f.document -> 'additionalDetails' ->> 'dietType'), '') != '' OR
                COALESCE((f.document -> 'additionalDetails' ->> 'otherInfo'), '') != '' OR
                COALESCE((f.document -> 'additionalDetails' ->> 'allergyType'), '') != ''
            ) as additionalInfo,
            (f.document -> 'apply' ->> 'siblingBasis')::boolean as siblingBasis,
            COALESCE(f.document -> 'careDetails' ->> 'assistanceNeeded', f.document -> 'clubCare' ->> 'assistanceNeeded')::boolean as assistanceNeed,
            (f.document -> 'clubCare' ->> 'assistanceNeeded')::boolean as wasOnClubCare,
            (f.document ->> 'wasOnDaycare')::boolean as wasOnDaycare,
            COALESCE((f.document ->> 'extendedCare')::boolean, false) as extendedCare,
            (SELECT COALESCE(array_length(duplicates.duplicate_application_ids, 1), 0) > 0) AS has_duplicates,
            pp.unit_confirmation_status,
            pp.unit_reject_reason,
            pp.unit_reject_other_reason,
            ppd.unit_name,
            count(*) OVER () AS total
        FROM application a
        JOIN (
            SELECT *,
            (
                SELECT COALESCE(array_agg(e::UUID) FILTER (WHERE e IS NOT NULL), '{}'::UUID[])
                FROM jsonb_array_elements_text(af.document -> 'apply' -> 'preferredUnits') e
            ) AS preferredUnits
            FROM application_form af
        ) f ON f.application_id = a.id AND f.latest IS TRUE
        JOIN person child ON child.id = a.child_id
        LEFT JOIN placement_plan pp ON pp.application_id = a.id AND a.status = 'WAITING_UNIT_CONFIRMATION'::application_status_type
        LEFT JOIN  (
            SELECT placement_plan.application_id, placement_plan.unit_id, daycare.name unit_name FROM daycare JOIN placement_plan ON daycare.id = placement_plan.unit_id
        ) ppd ON ppd.application_id = a.id
        JOIN daycare d ON COALESCE(ppd.unit_id, (f.document -> 'apply' -> 'preferredUnits' ->> 0)::uuid) = d.id
        JOIN care_area ca ON d.care_area_id = ca.id
        LEFT JOIN (
            SELECT
                l.id, array_agg(r.id) AS duplicate_application_ids
            FROM
                application l, application r
            WHERE
                l.child_id = r.child_id
                AND l.id <> r.id
                AND l.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
                AND r.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
            GROUP by
                l.id
        ) duplicates ON a.id = duplicates.id
        WHERE a.status != 'CREATED'::application_status_type $andWhere
        """.trimIndent()

    val orderedSql = when (sortBy) {
        ApplicationSortColumn.APPLICATION_TYPE -> "$sql ORDER BY type $sortDir"
        ApplicationSortColumn.CHILD_NAME -> "$sql ORDER BY last_name, first_name $sortDir"
        ApplicationSortColumn.DUE_DATE -> "$sql ORDER BY duedate $sortDir"
        ApplicationSortColumn.START_DATE -> "$sql ORDER BY preferredStartDate $sortDir"
        ApplicationSortColumn.STATUS -> "$sql ORDER BY application_status $sortDir"
    }.exhaust()

    val paginatedSql = "$orderedSql LIMIT $pageSize OFFSET ${(page - 1) * pageSize}"

    val applicationSummaries = h.createQuery(paginatedSql)
        .bindMap(params + freeTextParams)
        .map { row ->
            row.mapColumn<Int>("total") to ApplicationSummary(
                id = row.mapColumn("id"),
                firstName = row.mapColumn("first_name"),
                lastName = row.mapColumn("last_name"),
                socialSecurityNumber = row.mapColumn("social_security_number"),
                dateOfBirth = row.mapColumn("date_of_birth"),
                type = row.mapColumn("type"),
                placementType = mapRequestedPlacementType(row, "document"),
                dueDate = row.mapColumn("duedate"),
                startDate = row.mapColumn("preferredStartDate"),
                preferredUnits = row.mapJsonColumn<List<String>>("preferredUnits").map {
                    PreferredUnit(
                        id = UUID.fromString(it),
                        name = "" // filled afterwards
                    )
                },
                origin = row.mapColumn("origin"),
                checkedByAdmin = row.mapColumn("checkedbyadmin"),
                status = row.mapColumn("application_status"),
                additionalInfo = row.mapColumn("additionalInfo"),
                siblingBasis = row.mapColumn("siblingBasis"),
                assistanceNeed = row.mapColumn("assistanceNeed"),
                wasOnClubCare = row.mapColumn("wasOnClubCare"),
                wasOnDaycare = row.mapColumn("wasOnDaycare"),
                extendedCare = row.mapColumn("extendedCare"),
                duplicateApplication = row.mapColumn("has_duplicates"),
                transferApplication = row.mapColumn("transferapplication"),
                additionalDaycareApplication = row.mapColumn("additionaldaycareapplication"),
                placementProposalStatus = row.mapColumn<PlacementPlanConfirmationStatus?>("unit_confirmation_status")
                    ?.let {
                        PlacementProposalStatus(
                            unitConfirmationStatus = it,
                            unitRejectReason = row.mapColumn("unit_reject_reason"),
                            unitRejectOtherReason = row.mapColumn("unit_reject_other_reason")
                        )
                    },
                placementProposalUnitName = row.mapColumn("unit_name")
            )
        }
        .toList()
        .let { pairs ->
            ApplicationSummaries(
                data = pairs.map { it.second },
                totalCount = pairs.firstOrNull()?.first ?: 0,
                pages = pairs.firstOrNull()?.first?.let { count -> max(1, ceil(1.0 * count / pageSize).toInt()) }
                    ?: 1
            )
        }

    //language=sql
    val unitSql =
        """
        SELECT id, name
        FROM daycare
        WHERE id = ANY(:unitIds)
        """.trimIndent()
    val unitIds = applicationSummaries.data.flatMap { summary -> summary.preferredUnits.map { unit -> unit.id } }
    val unitMap = h.createQuery(unitSql)
        .bind("unitIds", unitIds.toTypedArray())
        .map { row -> row.mapColumn<UUID>("id") to row.mapColumn<String>("name") }
        .toMap()

    return applicationSummaries.copy(
        data = applicationSummaries.data.map { summary ->
            summary.copy(
                preferredUnits = summary.preferredUnits.map {
                    PreferredUnit(
                        id = it.id,
                        name = unitMap.getOrDefault(it.id, "")
                    )
                }
            )
        }
    )
}

fun fetchOwnApplicationIds(h: Handle, guardianId: UUID): List<UUID> {
    // language=SQL
    val sql =
        """
        SELECT a.id
        FROM application a
        WHERE a.guardian_id = :guardianId AND a.hidefromguardian = false
        """.trimIndent()

    return h.createQuery(sql)
        .bind("guardianId", guardianId)
        .mapTo<UUID>()
        .toList()
}

fun fetchApplicationSummariesForGuardian(h: Handle, guardianId: UUID): List<PersonApplicationSummary> {
    // language=SQL
    val sql =
        """
        SELECT
            a.id, a.preferredUnit, a.startDate, a.sentDate, a.document->>'type' AS type,
            a.childId, a.childName, a.childSsn,
            a.guardianId, concat(p.first_name, ' ', p.last_name) as guardianName,
            a.connecteddaycare,
            a.preparatoryeducation,
            d.name AS daycareName,
            a.status AS application_status
        FROM application_view a
        LEFT JOIN daycare d ON a.preferredUnit = d.id
        LEFT JOIN person p ON a.guardianId = p.id
        WHERE guardianId = :guardianId
        AND status != 'CREATED'::application_status_type
        ORDER BY sentDate DESC
        """.trimIndent()

    return h.createQuery(sql)
        .bind("guardianId", guardianId)
        .map(toPersonApplicationSummary)
        .toList()
}

fun fetchApplicationSummariesForChild(h: Handle, childId: UUID): List<PersonApplicationSummary> {
    // language=SQL
    val sql =
        """
        SELECT
            a.id, a.preferredUnit, a.startDate, a.sentDate, a.document->>'type' AS type,
            a.childId, a.childName, a.childSsn,
            a.guardianId, concat(p.first_name, ' ', p.last_name) as guardianName,
            a.connecteddaycare,
            a.preparatoryeducation,
            d.name AS daycareName,
            a.status AS application_status
        FROM application_view a
        LEFT JOIN daycare d ON a.preferredUnit = d.id
        LEFT JOIN person p ON a.guardianId = p.id
        WHERE childId = :childId
        AND status != 'CREATED'::application_status_type
        ORDER BY sentDate DESC
        """.trimIndent()

    return h.createQuery(sql)
        .bind("childId", childId)
        .map(toPersonApplicationSummary)
        .toList()
}

private val toPersonApplicationSummary: (ResultSet, StatementContext) -> PersonApplicationSummary = { rs, _ ->
    PersonApplicationSummary(
        applicationId = rs.getUUID("id"),
        childId = rs.getUUID("childId"),
        guardianId = rs.getUUID("guardianId"),
        preferredUnitId = rs.getUUID("preferredUnit"),
        preferredUnitName = rs.getString("daycareName"),
        childName = rs.getString("childName"),
        childSsn = rs.getString("childSsn"),
        guardianName = rs.getString("guardianName"),
        startDate = rs.getDate("startDate")?.toLocalDate(),
        sentDate = rs.getDate("sentDate")?.toLocalDate(),
        type = rs.getString("type"),
        status = rs.getEnum("application_status"),
        connectedDaycare = rs.getBoolean("connecteddaycare"),
        preparatoryEducation = rs.getBoolean("preparatoryeducation")
    )
}

fun fetchApplicationDetails(h: Handle, applicationId: UUID): ApplicationDetails? {
    //language=sql
    val sql =
        """
        SELECT 
            a.id,
            f.document ->> 'type' AS type,
            f.document,
            a.status,
            a.origin,
            a.child_id,
            a.guardian_id,
            a.other_guardian_id,
            c.restricted_details_enabled AS child_restricted,
            g1.restricted_details_enabled AS guardian_restricted,
            g1.residence_code AS guardian_residence_code,
            a.transferapplication,
            a.additionaldaycareapplication,
            a.hidefromguardian,
            a.created,
            f.updated,
            a.sentdate,
            a.duedate,
            a.checkedbyadmin,
            coalesce(att.json, '[]'::jsonb) attachments
        FROM application a
        JOIN application_form f ON f.application_id = a.id
        LEFT JOIN person c ON c.id = a.child_id
        LEFT JOIN person g1 ON g1.id = a.guardian_id
        LEFT JOIN (
            SELECT application_id, jsonb_agg(jsonb_build_object('id', id, 'name', name, 'contentType', content_type, 'updated', updated, 'type', type)) json
            FROM attachment GROUP BY application_id
        ) att ON a.id = att.application_id
        WHERE a.id = :id
        AND f.latest IS TRUE
        """.trimIndent()

    val application = h.createQuery(sql)
        .bind("id", applicationId)
        .map { row ->
            val childRestricted = row.mapColumn("child_restricted") ?: false
            val guardianRestricted = row.mapColumn("guardian_restricted") ?: false
            val deserializedForm = if (row.mapJsonColumn<FormWithType>("document").type == "club") {
                row.mapJsonColumn<ClubFormV0>("document")
            } else {
                row.mapJsonColumn<DaycareFormV0>("document")
            }

            ApplicationDetails(
                id = row.mapColumn("id"),
                type = row.mapColumn("type"),
                form = deserializedForm.let { ApplicationForm.fromV0(it, childRestricted, guardianRestricted) },
                status = row.mapColumn("status"),
                origin = row.mapColumn("origin"),
                childId = row.mapColumn("child_id"),
                guardianId = row.mapColumn("guardian_id"),
                otherGuardianId = row.mapColumn("other_guardian_id"),
                childRestricted = childRestricted,
                guardianRestricted = guardianRestricted,
                transferApplication = row.mapColumn("transferapplication"),
                additionalDaycareApplication = row.mapColumn("additionaldaycareapplication"),
                createdDate = row.mapColumn("created"),
                modifiedDate = row.mapColumn("updated"),
                sentDate = row.mapColumn("sentdate"),
                dueDate = row.mapColumn("duedate"),
                checkedByAdmin = row.mapColumn("checkedbyadmin"),
                hideFromGuardian = row.mapColumn("hidefromguardian"),
                attachments = row.mapJsonColumn<Array<Attachment>>("attachments").toList()
            )
        }
        .firstOrNull()

    if (application != null) {
        //language=sql
        val unitSql =
            """
            SELECT id, name
            FROM daycare
            WHERE id = ANY(:unitIds)
            """.trimIndent()
        val unitIds = application.form.preferences.preferredUnits.map { it.id }
        val unitMap = h.createQuery(unitSql)
            .bind("unitIds", unitIds.toTypedArray())
            .map { row -> row.mapColumn<UUID>("id") to row.mapColumn<String>("name") }
            .toMap()

        return application.copy(
            form = application.form.copy(
                preferences = application.form.preferences.copy(
                    preferredUnits = application.form.preferences.preferredUnits.map {
                        PreferredUnit(
                            id = it.id,
                            name = unitMap.getOrDefault(it.id, "")
                        )
                    }
                )
            )
        )
    } else return null
}

fun Handle.getApplicationUnitSummaries(unitId: UUID): List<ApplicationUnitSummary> {
    //language=sql
    val sql =
        """
        SELECT
            a.id,
            f.document ->> 'type' AS type,
            f.document,
            (f.document ->> 'preferredStartDate')::date as preferred_start_date,
            (array_position((
                SELECT array_agg(e)
                FROM jsonb_array_elements_text(f.document -> 'apply' -> 'preferredUnits') e
            ), :unitId::text)) as preference_order,
            a.status,
            c.first_name,
            c.last_name,
            c.date_of_birth,
            g.first_name AS guardian_first_name,
            g.last_name AS guardian_last_name,
            g.phone AS guardian_phone,
            g.email AS guardian_email
        FROM application a
        JOIN application_form f ON f.application_id = a.id AND f.latest IS TRUE
        JOIN person c ON c.id = a.child_id
        JOIN person g ON g.id = a.guardian_id
        WHERE EXISTS (
            SELECT 1
            FROM jsonb_array_elements_text(f.document -> 'apply' -> 'preferredUnits') e
            WHERE e = :unitId::text
        ) AND a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_DECISION}'::application_status_type[])
        ORDER BY c.last_name, c.first_name
        """.trimIndent()

    return this.createQuery(sql)
        .bind("unitId", unitId)
        .map { row ->
            ApplicationUnitSummary(
                applicationId = row.mapColumn("id"),
                firstName = row.mapColumn("first_name"),
                lastName = row.mapColumn("last_name"),
                dateOfBirth = row.mapColumn("date_of_birth"),
                guardianFirstName = row.mapColumn("guardian_first_name"),
                guardianLastName = row.mapColumn("guardian_last_name"),
                guardianPhone = row.mapColumn("guardian_phone"),
                guardianEmail = row.mapColumn("guardian_email"),
                requestedPlacementType = mapRequestedPlacementType(row, "document"),
                preferredStartDate = row.mapColumn("preferred_start_date"),
                preferenceOrder = row.mapColumn("preference_order"),
                status = row.mapColumn("status")
            )
        }
        .list()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FormWithType(val type: String)

fun mapRequestedPlacementType(row: RowView, colName: String): PlacementType = when (row.mapJsonColumn<FormWithType>(colName).type) {
    "club" -> PlacementType.CLUB
    "daycare" -> {
        if (row.mapJsonColumn<DaycareFormV0>(colName).partTime) {
            PlacementType.DAYCARE_PART_TIME
        } else {
            PlacementType.DAYCARE
        }
    }
    "preschool" -> {
        row.mapJsonColumn<DaycareFormV0>(colName).let {
            if (it.careDetails.preparatory == true) {
                if (it.connectedDaycare == true) {
                    PlacementType.PREPARATORY_DAYCARE
                } else {
                    PlacementType.PREPARATORY
                }
            } else {
                if (it.connectedDaycare == true) {
                    PlacementType.PRESCHOOL_DAYCARE
                } else {
                    PlacementType.PRESCHOOL
                }
            }
        }
    }
    else -> throw Error("unknown form type")
}

fun updateForm(h: Handle, id: UUID, form: ApplicationForm, formType: ApplicationType, childRestricted: Boolean, guardianRestricted: Boolean) {
    val transformedForm =
        if (formType == ApplicationType.CLUB) ClubFormV0.fromForm2(form, childRestricted, guardianRestricted)
        else DaycareFormV0.fromForm2(form, formType, childRestricted, guardianRestricted)

    // language=SQL
    val sql =
        """
WITH old_revisions AS (
    UPDATE application_form SET latest = FALSE
    WHERE application_id = :applicationId
    RETURNING revision
)
INSERT INTO application_form (application_id, document, revision, latest)
VALUES (:applicationId, :document, (SELECT coalesce(max(revision) + 1, 1) FROM old_revisions), TRUE)
        """.trimIndent()

    h.createUpdate(sql)
        .bind("applicationId", id)
        .bind(
            "document",
            PGobject().apply {
                type = "jsonb"
                value = objectMapper().writeValueAsString(transformedForm)
            }
        )
        .execute()
}

fun setCheckedByAdminToDefault(h: Handle, id: UUID, form: ApplicationForm) {
    // language=SQL
    val sql = "UPDATE application SET checkedbyadmin = :checked WHERE id = :applicationId"

    val default = !form.child.assistanceNeeded &&
        form.child.allergies.isBlank() &&
        form.child.diet.isBlank() &&
        form.otherInfo.isBlank()

    h.createUpdate(sql)
        .bind("applicationId", id)
        .bind("checked", default)
        .execute()
}

fun updateApplicationStatus(h: Handle, id: UUID, status: ApplicationStatus) {
    // language=SQL
    val sql = "UPDATE application SET status = :status WHERE id = :id"

    h.createUpdate(sql)
        .bind("id", id)
        .bind("status", status)
        .execute()
}

fun updateApplicationDates(h: Handle, id: UUID, sentDate: LocalDate, dueDate: LocalDate?) {
    // language=SQL
    val sql = "UPDATE application SET sentdate = :sentDate, duedate = :dueDate WHERE id = :id"

    h.createUpdate(sql)
        .bind("id", id)
        .bind("sentDate", sentDate)
        .bind("dueDate", dueDate)
        .execute()
}

fun updateApplicationFlags(h: Handle, id: UUID, applicationFlags: ApplicationFlags) {
    // language=SQL
    val sql = "UPDATE application SET transferapplication = :transferApplication, additionaldaycareapplication = :additionalDaycareApplication WHERE id = :id"

    h.createUpdate(sql)
        .bind("id", id)
        .bind("transferApplication", applicationFlags.isTransferApplication)
        .bind("additionalDaycareApplication", applicationFlags.isAdditionalDaycareApplication)
        .execute()
}

fun updateApplicationOtherGuardian(h: Handle, applicationId: UUID, otherGuardianId: UUID?) {
    // language=SQL
    val sql = "UPDATE application SET other_guardian_id = :otherGuardianId WHERE id = :applicationId"

    h.createUpdate(sql)
        .bind("applicationId", applicationId)
        .bind("otherGuardianId", otherGuardianId)
        .execute()
}

fun setApplicationVerified(h: Handle, id: UUID, verified: Boolean) {
    // language=SQL
    val sql = "UPDATE application SET checkedByAdmin = :verified WHERE id = :id"

    h.createUpdate(sql)
        .bind("verified", verified)
        .bind("id", id)
        .execute()
}

fun deleteApplication(h: Handle, id: UUID) {
    // language=SQL
    val sql =
        """
        UPDATE attachment SET application_id = NULL WHERE application_id = :id;
        DELETE FROM application_form WHERE application_id = :id;
        DELETE FROM application WHERE id = :id;
        """.trimIndent()

    h.createUpdate(sql).bind("id", id).execute()
}

fun removeOldDrafts(h: Handle) {
    val thresholdDays = 31

    h.transaction { handle ->
        // language=SQL
        handle.createUpdate("""DELETE FROM application_form WHERE application_id IN (SELECT id FROM application WHERE status = 'CREATED' AND created < current_date - :thresholdDays)""")
            .bind("thresholdDays", thresholdDays)
            .execute()

        // language=SQL
        handle.createUpdate("""DELETE FROM application_note WHERE application_id IN (SELECT id FROM application WHERE status = 'CREATED' AND created < current_date - :thresholdDays)""")
            .bind("thresholdDays", thresholdDays)
            .execute()

        // language=SQL
        handle.createUpdate("""DELETE FROM application WHERE status = 'CREATED' AND created < current_date - :thresholdDays""")
            .bind("thresholdDays", thresholdDays)
            .execute()
    }
}
