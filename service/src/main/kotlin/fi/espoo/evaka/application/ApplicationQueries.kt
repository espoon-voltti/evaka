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
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.persistence.objectMapper
import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.attachment.AttachmentType
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.utils.dateNow
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.StatementContext
import org.postgresql.util.PGobject
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

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
    DUPLICATE_APPLICATION,
    URGENT,
    HAS_ATTACHMENTS
}

enum class ApplicationSortDirection {
    ASC,
    DESC
}

fun Database.Transaction.insertApplication(
    guardianId: UUID,
    childId: UUID,
    origin: ApplicationOrigin,
    hideFromGuardian: Boolean = false,
    sentDate: LocalDate? = null
): ApplicationId {
    // language=sql
    val sql =
        """
        INSERT INTO application (status, guardian_id, child_id, origin, hidefromguardian, sentdate)
        VALUES ('CREATED'::application_status_type, :guardianId, :childId, :origin::application_origin_type, :hideFromGuardian, :sentDate)
        RETURNING id
        """.trimIndent()

    return createUpdate(sql)
        .bind("guardianId", guardianId)
        .bind("childId", childId)
        .bind("origin", origin)
        .bind("hideFromGuardian", hideFromGuardian)
        .bind("sentDate", sentDate)
        .executeAndReturnGeneratedKeys()
        .mapTo<ApplicationId>()
        .first()
}

fun Database.Read.duplicateApplicationExists(
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
                status = ANY ('{CREATED,SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
        """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .bind("guardianId", guardianId)
        .bind("type", type)
        .mapTo<Int>()
        .list()
        .isNotEmpty()
}

fun Database.Read.activePlacementExists(
    childId: UUID,
    type: ApplicationType
): Boolean {
    val placementTypes = when (type) {
        ApplicationType.DAYCARE -> listOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME)
        ApplicationType.PRESCHOOL -> listOf(PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE, PlacementType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE)
        else -> listOf()
    }
    if (placementTypes.isEmpty()) return false

    val sql =
        """
            SELECT 1
            FROM placement
            WHERE 
                child_id = :childId AND
                type = ANY(:types::placement_type[]) AND 
                current_date <= end_date
        """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .bind("types", placementTypes.toTypedArray())
        .mapTo<Int>()
        .list()
        .isNotEmpty()
}

fun Database.Read.fetchApplicationSummaries(
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
    voucherApplications: VoucherApplicationFilter?,
    authorizedUnitsForApplicationsWithoutAssistanceNeed: AclAuthorization,
    authorizedUnitsForApplicationsWithAssistanceNeed: AclAuthorization,
    canReadServiceWorkerNotes: Boolean
): Paged<ApplicationSummary> {
    val params = mapOf(
        "page" to page,
        "pageSize" to pageSize,
        "area" to areas.toTypedArray(),
        "units" to units.toTypedArray(),
        "authorizedUnitsForApplicationsWithoutAssistanceNeed" to authorizedUnitsForApplicationsWithoutAssistanceNeed.ids?.toTypedArray(),
        "authorizedUnitsForApplicationsWithAssistanceNeed" to authorizedUnitsForApplicationsWithAssistanceNeed.ids?.toTypedArray(),
        "documentType" to type.toString(),
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
        if (basis.isNotEmpty()) basis.joinToString("\nAND ") { applicationBasis ->
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
                ApplicationBasis.DUPLICATE_APPLICATION -> "has_duplicates"
                ApplicationBasis.URGENT -> "(f.document ->> 'urgent')::boolean = true"
                ApplicationBasis.HAS_ATTACHMENTS -> "((f.document ->> 'urgent')::boolean = true OR (f.document ->> 'extendedCare')::boolean = true) AND array_length(attachments.attachment_ids, 1) > 0"
            }
        } else null,
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
        if (distinctions.contains(ApplicationDistinctions.SECONDARY)) "f.preferredUnits && :units" else if (units.isNotEmpty()) "d.id = ANY(:units)" else null,
        if (authorizedUnitsForApplicationsWithoutAssistanceNeed != AclAuthorization.All) "((f.document->'careDetails'->>'assistanceNeeded')::boolean = true OR f.preferredUnits && :authorizedUnitsForApplicationsWithoutAssistanceNeed)" else null,
        if (authorizedUnitsForApplicationsWithAssistanceNeed != AclAuthorization.All) "((f.document->'careDetails'->>'assistanceNeeded')::boolean = false OR f.preferredUnits && :authorizedUnitsForApplicationsWithAssistanceNeed)" else null,
        if ((periodStart != null || periodEnd != null) && dateType.contains(ApplicationDateType.DUE)) "between_start_and_end(daterange(:periodStart, :periodEnd, '[]'), a.dueDate)" else null,
        if ((periodStart != null || periodEnd != null) && dateType.contains(ApplicationDateType.START)) "between_start_and_end(daterange(:periodStart, :periodEnd, '[]'), (f.document ->> 'preferredStartDate')::date)" else null,
        if ((periodStart != null || periodEnd != null) && dateType.contains(ApplicationDateType.ARRIVAL)) "between_start_and_end(daterange(:periodStart, :periodEnd, '[]'), a.sentdate)" else null,
        if (searchTerms.isNotBlank()) freeTextQuery else null,
        when (transferApplications) {
            TransferApplicationFilter.TRANSFER_ONLY -> "a.transferApplication"
            TransferApplicationFilter.NO_TRANSFER -> "NOT a.transferApplication"
            else -> null
        },
        when (voucherApplications) {
            VoucherApplicationFilter.VOUCHER_FIRST_CHOICE -> "d.provider_type = 'PRIVATE_SERVICE_VOUCHER'"
            VoucherApplicationFilter.VOUCHER_ONLY -> "f.preferredUnits && (SELECT array_agg(id) FROM daycare WHERE provider_type = 'PRIVATE_SERVICE_VOUCHER')"
            VoucherApplicationFilter.NO_VOUCHER -> "NOT f.preferredUnits && (SELECT array_agg(id) FROM daycare WHERE provider_type = 'PRIVATE_SERVICE_VOUCHER')"
            null -> null
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
            f.document -> 'serviceNeedOption' ->> 'id' as serviceNeedId,
            f.document -> 'serviceNeedOption' ->> 'nameFi' as serviceNeedNameFi,
            f.document -> 'serviceNeedOption' ->> 'nameSv' as serviceNeedNameSv,
            f.document -> 'serviceNeedOption' ->> 'nameEn' as serviceNeedNameEn,
            a.duedate,
            f.document ->> 'preferredStartDate' as preferredStartDate,
            f.document -> 'apply' -> 'preferredUnits' as preferredUnits,
            a.status AS application_status,
            a.origin,
            a.transferapplication,
            a.additionaldaycareapplication,
            a.checkedbyadmin,
            a.service_worker_note,
            (
                COALESCE((f.document -> 'additionalDetails' ->> 'dietType'), '') != '' OR
                COALESCE((f.document -> 'additionalDetails' ->> 'otherInfo'), '') != '' OR
                COALESCE((f.document -> 'additionalDetails' ->> 'allergyType'), '') != ''
            ) as additionalInfo,
            (f.document -> 'apply' ->> 'siblingBasis')::boolean as siblingBasis,
            COALESCE(f.document -> 'careDetails' ->> 'assistanceNeeded', f.document -> 'clubCare' ->> 'assistanceNeeded')::boolean as assistanceNeed,
            (f.document -> 'clubCare' ->> 'assistanceNeeded')::boolean as wasOnClubCare,
            (f.document ->> 'wasOnDaycare')::boolean as wasOnDaycare,
            COALESCE((f.document ->> 'urgent')::boolean, false) as urgent,
            (SELECT COALESCE(array_length(attachments.attachment_ids, 1), 0)) AS attachmentCount,
            COALESCE((f.document ->> 'extendedCare')::boolean, false) as extendedCare,
            has_duplicates,
            pp.unit_confirmation_status,
            pp.unit_reject_reason,
            pp.unit_reject_other_reason,
            ppd.unit_name,
            cpu.id AS current_placement_unit_id,
            cpu.name AS current_placement_unit_name,
            count(*) OVER () AS total
        FROM application a
        JOIN (
            SELECT
                id,
                application_id,
                created,
                revision,
                document,
                updated,
                latest,
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
        JOIN (
            SELECT l.id, EXISTS(
              SELECT 1
              FROM application r
              WHERE
                l.child_id = r.child_id
                AND l.id <> r.id
                AND l.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
                AND r.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
            ) as has_duplicates
            FROM application l
        ) duplicates ON duplicates.id = a.id
        LEFT JOIN (
            SELECT
                appl.id, array_agg(att.id) AS attachment_ids
            FROM
                application appl, attachment att
            WHERE
                appl.id = att.application_id
            GROUP by
                appl.id
        ) attachments ON a.id = attachments.id
        LEFT JOIN LATERAL (
            SELECT daycare.id, daycare.name
            FROM daycare
            JOIN placement ON daycare.id = placement.unit_id
            WHERE placement.child_id = a.child_id AND daterange(start_date, end_date, '[]') && daterange(current_date, null, '[]')
            ORDER BY start_date
            LIMIT 1
        ) cpu ON true
        WHERE a.status != 'CREATED'::application_status_type $andWhere
        """.trimIndent()

    val orderedSql = when (sortBy) {
        ApplicationSortColumn.APPLICATION_TYPE -> "$sql ORDER BY type $sortDir, last_name, first_name"
        ApplicationSortColumn.CHILD_NAME -> "$sql ORDER BY last_name, first_name $sortDir"
        ApplicationSortColumn.DUE_DATE -> "$sql ORDER BY duedate $sortDir, last_name, first_name"
        ApplicationSortColumn.START_DATE -> "$sql ORDER BY preferredStartDate $sortDir, last_name, first_name"
        ApplicationSortColumn.STATUS -> "$sql ORDER BY application_status $sortDir, last_name, first_name"
    }.exhaust()

    val paginatedSql = "$orderedSql LIMIT $pageSize OFFSET ${(page - 1) * pageSize}"

    val applicationSummaries = createQuery(paginatedSql)
        .bindMap(params + freeTextParams)
        .mapToPaged(pageSize, "total") { row ->
            ApplicationSummary(
                id = row.mapColumn("id"),
                firstName = row.mapColumn("first_name"),
                lastName = row.mapColumn("last_name"),
                socialSecurityNumber = row.mapColumn("social_security_number"),
                dateOfBirth = row.mapColumn<String?>("date_of_birth")?.let { LocalDate.parse(it) },
                type = row.mapColumn("type"),
                placementType = mapRequestedPlacementType(row, "document"),
                serviceNeed = row.mapColumn<String?>("serviceNeedId")?.let {
                    ServiceNeedOption(
                        UUID.fromString(it),
                        row.mapColumn("serviceNeedNameFi"),
                        row.mapColumn("serviceNeedNameSv"),
                        row.mapColumn("serviceNeedNameEn")
                    )
                },
                dueDate = row.mapColumn<String?>("duedate")?.let { LocalDate.parse(it) },
                startDate = row.mapColumn<String?>("preferredStartDate")?.let { LocalDate.parse(it) },
                preferredUnits = row.mapJsonColumn<List<String>>("preferredUnits").map {
                    PreferredUnit(
                        id = DaycareId(UUID.fromString(it)),
                        name = "" // filled afterwards
                    )
                },
                origin = row.mapColumn("origin"),
                checkedByAdmin = row.mapColumn("checkedbyadmin"),
                status = row.mapColumn("application_status"),
                additionalInfo = row.mapColumn("additionalInfo"),
                serviceWorkerNote = if (canReadServiceWorkerNotes) row.mapColumn("service_worker_note") else "",
                siblingBasis = row.mapColumn("siblingBasis"),
                assistanceNeed = row.mapColumn("assistanceNeed"),
                wasOnClubCare = row.mapColumn("wasOnClubCare"),
                wasOnDaycare = row.mapColumn("wasOnDaycare"),
                extendedCare = row.mapColumn("extendedCare"),
                duplicateApplication = row.mapColumn("has_duplicates"),
                transferApplication = row.mapColumn("transferapplication"),
                urgent = row.mapColumn("urgent"),
                attachmentCount = row.mapColumn("attachmentCount"),
                additionalDaycareApplication = row.mapColumn("additionaldaycareapplication"),
                placementProposalStatus = row.mapColumn<PlacementPlanConfirmationStatus?>("unit_confirmation_status")
                    ?.let {
                        PlacementProposalStatus(
                            unitConfirmationStatus = it,
                            unitRejectReason = row.mapColumn("unit_reject_reason"),
                            unitRejectOtherReason = row.mapColumn("unit_reject_other_reason")
                        )
                    },
                placementProposalUnitName = row.mapColumn("unit_name"),
                currentPlacementUnit = row.mapColumn<DaycareId?>("current_placement_unit_id")?.let {
                    PreferredUnit(it, row.mapColumn("current_placement_unit_name"))
                }
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
    val unitMap = createQuery(unitSql)
        .bind("unitIds", unitIds.toTypedArray())
        .map { row -> row.mapColumn<DaycareId>("id") to row.mapColumn<String>("name") }
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

fun Database.Read.fetchApplicationSummariesForGuardian(guardianId: UUID): List<PersonApplicationSummary> {
    // language=SQL
    val sql =
        """
        SELECT
            a.id, a.preferredUnit, a.preferredStartDate, a.sentDate, a.document->>'type' AS type,
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

    return createQuery(sql)
        .bind("guardianId", guardianId)
        .map(toPersonApplicationSummary)
        .toList()
}

fun Database.Read.fetchApplicationSummariesForChild(childId: UUID): List<PersonApplicationSummary> {
    // language=SQL
    val sql =
        """
        SELECT
            a.id, a.preferredUnit, a.preferredStartDate, a.sentDate, a.document->>'type' AS type,
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

    return createQuery(sql)
        .bind("childId", childId)
        .map(toPersonApplicationSummary)
        .toList()
}

fun Database.Read.fetchApplicationSummariesForCitizen(citizenId: UUID): List<CitizenApplicationSummary> {
    // language=SQL
    val sql =
        """
        SELECT
            a.id AS application_id, 
            a.document->>'type' AS type,
            a.childId, a.childName, 
            d.name AS preferred_unit_name,
            COALESCE((SELECT array_agg(name) as other_preferred_units
             FROM daycare JOIN (SELECT unnest(preferredUnits) 
                                FROM application WHERE application.id = a.id) pu ON daycare.id = pu.unnest), '{}'::text[]) AS all_preferred_unit_names,
            a.startDate, a.sentDate, 
            a.status AS application_status,
            a.created AS created_date,
            a.formmodified AS modified_date,
            a.transferapplication
        FROM application_view a
        LEFT JOIN daycare d ON a.preferredUnit = d.id
        WHERE guardianId = :guardianId AND NOT a.hidefromguardian AND a.status != 'CANCELLED'
        ORDER BY sentDate DESC
        """.trimIndent()

    return createQuery(sql)
        .bind("guardianId", citizenId)
        .mapTo<CitizenApplicationSummary>()
        .toList()
}

data class CitizenChildren(val childId: UUID, val childName: String)

fun Database.Read.getCitizenChildren(citizenId: UUID): List<CitizenChildren> {
    //language=sql
    val sql =
        """
        SELECT child_id, first_name || ' ' || last_name AS childName
        FROM guardian LEFT JOIN person child ON guardian.child_id = child.id
        WHERE guardian_id = :guardianId
        """.trimIndent()

    return createQuery(sql)
        .bind("guardianId", citizenId)
        .mapTo<CitizenChildren>()
        .list()
}

private val toPersonApplicationSummary: (ResultSet, StatementContext) -> PersonApplicationSummary = { rs, _ ->
    PersonApplicationSummary(
        applicationId = ApplicationId(rs.getUUID("id")),
        childId = rs.getUUID("childId"),
        guardianId = rs.getUUID("guardianId"),
        preferredUnitId = DaycareId(rs.getUUID("preferredUnit")),
        preferredUnitName = rs.getString("daycareName"),
        childName = rs.getString("childName"),
        childSsn = rs.getString("childSsn"),
        guardianName = rs.getString("guardianName"),
        preferredStartDate = rs.getDate("preferredStartDate")?.toLocalDate(),
        sentDate = rs.getDate("sentDate")?.toLocalDate(),
        type = rs.getString("type"),
        status = rs.getEnum("application_status"),
        connectedDaycare = rs.getBoolean("connecteddaycare"),
        preparatoryEducation = rs.getBoolean("preparatoryeducation")
    )
}

fun Database.Read.fetchApplicationDetails(applicationId: ApplicationId, includeCitizenAttachmentsOnly: Boolean = false): ApplicationDetails? {
    val attachmentWhereClause = if (includeCitizenAttachmentsOnly) "WHERE eu.type = 'CITIZEN'" else ""
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
            g1.date_of_death AS guardian_date_of_death,
            a.transferapplication,
            a.additionaldaycareapplication,
            a.hidefromguardian,
            a.created,
            f.updated,
            a.sentdate,
            a.duedate,
            a.duedate_set_manually_at,
            a.checkedbyadmin,
            coalesce(att.json, '[]'::jsonb) attachments
        FROM application a
        JOIN application_form f ON f.application_id = a.id
        LEFT JOIN person c ON c.id = a.child_id
        LEFT JOIN person g1 ON g1.id = a.guardian_id
        LEFT JOIN (
            SELECT application_id, jsonb_agg(jsonb_build_object(
                'id', attachment.id,
                'name', attachment.name,
                'contentType', content_type,
                'updated', updated,
                'receivedAt', received_at,
                'type', attachment.type,
                'uploadedByEmployee', (CASE eu.type WHEN 'EMPLOYEE' THEN eu.id END),
                'uploadedByPerson', (CASE eu.type WHEN 'CITIZEN' THEN eu.id END)
            )) json
            FROM attachment
            JOIN evaka_user eu ON attachment.uploaded_by = eu.id
            $attachmentWhereClause
            GROUP BY application_id
        ) att ON a.id = att.application_id
        WHERE a.id = :id
        AND f.latest IS TRUE
        """.trimIndent()

    val application = createQuery(sql)
        .bind("id", applicationId)
        .map { row ->
            val childRestricted = row.mapColumn("child_restricted") ?: false
            val guardianRestricted = row.mapColumn("guardian_restricted") ?: false
            val deserializedForm = if (row.mapJsonColumn<FormWithType>("document").type == "CLUB") {
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
                otherGuardianLivesInSameAddress = null,
                childRestricted = childRestricted,
                guardianRestricted = guardianRestricted,
                guardianDateOfDeath = row.mapColumn("guardian_date_of_death"),
                transferApplication = row.mapColumn("transferapplication"),
                additionalDaycareApplication = row.mapColumn("additionaldaycareapplication"),
                createdDate = row.mapColumn("created"),
                modifiedDate = row.mapColumn("updated"),
                sentDate = row.mapColumn("sentdate"),
                dueDate = row.mapColumn("duedate"),
                dueDateSetManuallyAt = row.mapColumn("duedate_set_manually_at"),
                checkedByAdmin = row.mapColumn("checkedbyadmin"),
                hideFromGuardian = row.mapColumn("hidefromguardian"),
                attachments = row.mapJsonColumn<Array<ApplicationAttachment>>("attachments").toList()
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
        val unitMap = createQuery(unitSql)
            .bind("unitIds", unitIds.toTypedArray())
            .map { row -> row.mapColumn<DaycareId>("id") to row.mapColumn<String>("name") }
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

fun Database.Read.getApplicationUnitSummaries(unitId: DaycareId): List<ApplicationUnitSummary> {
    //language=sql
    val sql =
        """
        SELECT
            a.id,
            f.document ->> 'type' AS type,
            f.document -> 'serviceNeedOption' ->> 'id' AS serviceNeedId,
            f.document -> 'serviceNeedOption' ->> 'nameFi' AS serviceNeedNameFi,
            f.document -> 'serviceNeedOption' ->> 'nameSv' AS serviceNeedNameSv,
            f.document -> 'serviceNeedOption' ->> 'nameEn' AS serviceNeedNameEn,
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

    return createQuery(sql)
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
                serviceNeed = row.mapColumn<String?>("serviceNeedId")?.let {
                    ServiceNeedOption(
                        UUID.fromString(it),
                        row.mapColumn("serviceNeedNameFi"),
                        row.mapColumn("serviceNeedNameSv"),
                        row.mapColumn("serviceNeedNameEn")
                    )
                },
                preferredStartDate = row.mapColumn("preferred_start_date"),
                preferenceOrder = row.mapColumn("preference_order"),
                status = row.mapColumn("status")
            )
        }
        .list()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FormWithType(val type: String)

fun mapRequestedPlacementType(row: RowView, colName: String): PlacementType =
    when (row.mapJsonColumn<FormWithType>(colName).type) {
        "CLUB" -> PlacementType.CLUB
        "DAYCARE" -> {
            if (row.mapJsonColumn<DaycareFormV0>(colName).partTime) {
                PlacementType.DAYCARE_PART_TIME
            } else {
                PlacementType.DAYCARE
            }
        }
        "PRESCHOOL" -> {
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

fun Database.Transaction.updateForm(
    id: ApplicationId,
    form: ApplicationForm,
    formType: ApplicationType,
    childRestricted: Boolean,
    guardianRestricted: Boolean
) {
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

    createUpdate(sql)
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

fun Database.Transaction.setCheckedByAdminToDefault(id: ApplicationId, form: ApplicationForm) {
    // language=SQL
    val sql = "UPDATE application SET checkedbyadmin = :checked WHERE id = :applicationId"

    val default = !form.child.assistanceNeeded &&
        form.child.allergies.isBlank() &&
        form.child.diet.isBlank() &&
        form.otherInfo.isBlank()

    createUpdate(sql)
        .bind("applicationId", id)
        .bind("checked", default)
        .execute()
}

fun Database.Transaction.updateApplicationStatus(id: ApplicationId, status: ApplicationStatus) {
    // language=SQL
    val sql = "UPDATE application SET status = :status WHERE id = :id"

    createUpdate(sql)
        .bind("id", id)
        .bind("status", status)
        .execute()
}

fun Database.Transaction.updateApplicationDates(id: ApplicationId, sentDate: LocalDate, dueDate: LocalDate?) {
    // language=SQL
    val sql = "UPDATE application SET sentdate = :sentDate, duedate = :dueDate WHERE id = :id"

    createUpdate(sql)
        .bind("id", id)
        .bind("sentDate", sentDate)
        .bind("dueDate", dueDate)
        .execute()
}

fun Database.Transaction.updateApplicationFlags(id: ApplicationId, applicationFlags: ApplicationFlags) {
    // language=SQL
    val sql =
        "UPDATE application SET transferapplication = :transferApplication, additionaldaycareapplication = :additionalDaycareApplication WHERE id = :id"

    createUpdate(sql)
        .bind("id", id)
        .bind("transferApplication", applicationFlags.isTransferApplication)
        .bind("additionalDaycareApplication", applicationFlags.isAdditionalDaycareApplication)
        .execute()
}

fun Database.Transaction.updateApplicationOtherGuardian(applicationId: ApplicationId, otherGuardianId: UUID?) {
    // language=SQL
    val sql = "UPDATE application SET other_guardian_id = :otherGuardianId WHERE id = :applicationId"

    createUpdate(sql)
        .bind("applicationId", applicationId)
        .bind("otherGuardianId", otherGuardianId)
        .execute()
}

fun Database.Transaction.setApplicationVerified(id: ApplicationId, verified: Boolean) {
    // language=SQL
    val sql = "UPDATE application SET checkedByAdmin = :verified WHERE id = :id"

    createUpdate(sql)
        .bind("verified", verified)
        .bind("id", id)
        .execute()
}

fun Database.Transaction.deleteApplication(id: ApplicationId) {
    // language=SQL
    val sql =
        """
        DELETE FROM attachment WHERE application_id = :id;
        DELETE FROM application_form WHERE application_id = :id;
        DELETE FROM application WHERE id = :id;
        """.trimIndent()

    createUpdate(sql).bind("id", id).execute()
}

fun Database.Transaction.removeOldDrafts(deleteAttachment: (db: Database.Transaction, id: AttachmentId) -> Unit) {
    val thresholdDays = 31

    // language=SQL
    val applicationIds =
        createQuery("""SELECT id FROM application WHERE status = 'CREATED' AND created < current_date - :thresholdDays""")
            .bind("thresholdDays", thresholdDays)
            .mapTo<UUID>()
            .toList()

    if (applicationIds.isNotEmpty()) {
        logger.info("Cleaning up ${applicationIds.size} draft applications older than $thresholdDays days")

        // language=SQL
        createUpdate("""DELETE FROM application_form WHERE application_id = ANY(:applicationIds::uuid[])""")
            .bind("applicationIds", applicationIds.toTypedArray())
            .execute()

        // language=SQL
        createUpdate("""DELETE FROM application_note WHERE application_id = ANY(:applicationIds::uuid[])""")
            .bind("applicationIds", applicationIds.toTypedArray())
            .execute()

        applicationIds.forEach { applicationId ->
            val attachmentIds =
                createUpdate("""DELETE FROM attachment WHERE application_id = :id RETURNING id""")
                    .bind("id", applicationId)
                    .executeAndReturnGeneratedKeys()
                    .mapTo<AttachmentId>()
                    .toList()

            attachmentIds.forEach { attachmentId ->
                deleteAttachment(this, attachmentId)
            }
        }

        // language=SQL
        createUpdate("""DELETE FROM application WHERE id = ANY(:applicationIds::uuid[])""")
            .bind("applicationIds", applicationIds.toTypedArray())
            .execute()
    }
}

fun Database.Transaction.cancelOutdatedTransferApplications(): List<ApplicationId> = createUpdate(
    // only include applications that don't have decisions
    // placement type checks are doing in inverse so that the addition and accidental omission of new placement types
    // does not cause the cancellation of applications that shouldn't be cancelled
    """
UPDATE application SET status = :cancelled
WHERE transferapplication
AND status = ANY('{CREATED, SENT, WAITING_PLACEMENT, WAITING_UNIT_CONFIRMATION, WAITING_DECISION}')
AND NOT EXISTS (
    SELECT 1
    FROM placement p
    JOIN application_form f
        ON application.child_id = p.child_id
        AND f.application_id = application.id
        AND f.latest
        AND (CASE
            WHEN f.document->>'type' = 'DAYCARE' THEN NOT p.type = ANY(:notDaycarePlacements::placement_type[])
            WHEN f.document->>'type' = 'PRESCHOOL' THEN NOT p.type = ANY(:notPreschoolPlacements::placement_type[])
            WHEN f.document->>'type' = 'CLUB' THEN NOT p.type = ANY(:notClubPlacements::placement_type[])
        END)
        AND daterange((f.document->>'preferredStartDate')::date, null, '[]') && daterange(p.start_date, p.end_date, '[]')
        AND p.end_date >= :yesterday
)
RETURNING id
"""
)
    .bind("cancelled", ApplicationStatus.CANCELLED)
    .bind("yesterday", dateNow().minusDays(1))
    .bind(
        "notDaycarePlacements",
        arrayOf(
            PlacementType.CLUB,
            PlacementType.PRESCHOOL,
            PlacementType.PRESCHOOL_DAYCARE,
            PlacementType.PREPARATORY,
            PlacementType.PREPARATORY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            PlacementType.SCHOOL_SHIFT_CARE
        )
    )
    .bind(
        "notPreschoolPlacements",
        arrayOf(
            PlacementType.CLUB,
            PlacementType.DAYCARE,
            PlacementType.DAYCARE_PART_TIME,
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            PlacementType.SCHOOL_SHIFT_CARE
        )
    )
    .bind(
        "notClubPlacements",
        arrayOf(
            PlacementType.DAYCARE,
            PlacementType.DAYCARE_PART_TIME,
            PlacementType.DAYCARE_FIVE_YEAR_OLDS,
            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
            PlacementType.PRESCHOOL,
            PlacementType.PRESCHOOL_DAYCARE,
            PlacementType.PREPARATORY,
            PlacementType.PREPARATORY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE,
            PlacementType.TEMPORARY_DAYCARE_PART_DAY,
            PlacementType.SCHOOL_SHIFT_CARE
        )
    )
    .executeAndReturnGeneratedKeys()
    .mapTo<ApplicationId>()
    .toList()

fun Database.Read.getApplicationAttachments(applicationId: ApplicationId): List<ApplicationAttachment> =
    createQuery(
        """
SELECT
    attachment.id, attachment.name, content_type, updated, received_at, attachment.type,
    (CASE evaka_user.type WHEN 'EMPLOYEE' THEN evaka_user.id END) AS uploaded_by_employee,
    (CASE evaka_user.type WHEN 'CITIZEN' THEN evaka_user.id END) AS uploaded_by_person
FROM attachment
JOIN evaka_user ON attachment.uploaded_by = evaka_user.id
WHERE application_id = :applicationId
"""
    )
        .bind("applicationId", applicationId)
        .mapTo<ApplicationAttachment>()
        .toList()

fun Database.Read.getApplicationAttachmentsForUnitSupervisor(applicationId: ApplicationId): List<ApplicationAttachment> =
    createQuery(
        """
SELECT
    attachment.id, attachment.name, attachment.content_type, attachment.updated, attachment.received_at, attachment.type,
    (CASE evaka_user.type WHEN 'EMPLOYEE' THEN evaka_user.id END) AS uploaded_by_employee,
    (CASE evaka_user.type WHEN 'CITIZEN' THEN evaka_user.id END) AS uploaded_by_person
FROM attachment
JOIN evaka_user ON attachment.uploaded_by = evaka_user.id
JOIN application ON application.id = attachment.application_id
JOIN placement_plan ON placement_plan.application_id = application.id
JOIN daycare ON daycare.id = placement_plan.unit_id
WHERE application.id = :applicationId AND daycare.round_the_clock AND attachment.type = ANY(:attachmentTypes)
"""
    )
        .bind("applicationId", applicationId)
        .bind("attachmentTypes", arrayOf(AttachmentType.EXTENDED_CARE))
        .mapTo<ApplicationAttachment>()
        .toList()

fun Database.Read.isOwnApplication(user: AuthenticatedUser.Citizen, applicationId: ApplicationId): Boolean {
    return this.createQuery("SELECT 1 FROM application WHERE id = :id AND guardian_id = :userId")
        .bind("id", applicationId)
        .bind("userId", user.id)
        .mapTo<Int>()
        .any()
}

fun Database.Transaction.cancelAllActiveTransferApplicationsAfterDate(childId: ChildId, preferredStartDateMinDate: LocalDate): List<ApplicationId> = createUpdate(
    //language=sql
    """
UPDATE application
SET status = 'CANCELLED'
WHERE transferapplication
AND child_id = :childId
AND status = ANY(:activeApplicationStatus::application_status_type[])
AND EXISTS (
    SELECT 1
    FROM application_form f
    WHERE 
        f.application_id = application.id
        AND f.latest
        AND daterange(:preferredStartDateMinDate::date, null, '[]') @> (f.document->>'preferredStartDate')::date
)
RETURNING id
    """.trimIndent()
)
    .bind(
        "activeApplicationStatus",
        arrayOf(
            ApplicationStatus.CREATED,
            ApplicationStatus.SENT,
            ApplicationStatus.WAITING_PLACEMENT,
            ApplicationStatus.WAITING_CONFIRMATION,
            ApplicationStatus.WAITING_UNIT_CONFIRMATION,
            ApplicationStatus.WAITING_DECISION,
            ApplicationStatus.WAITING_MAILING
        )
    )
    .bind("preferredStartDateMinDate", preferredStartDateMinDate)
    .bind("childId", childId)
    .executeAndReturnGeneratedKeys()
    .mapTo<ApplicationId>()
    .list()
