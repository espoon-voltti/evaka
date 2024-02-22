// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.DAYCARE_ONLY
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.PREPARATORY_DAYCARE
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.PREPARATORY_ONLY
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.PRESCHOOL_CLUB
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.PRESCHOOL_DAYCARE
import fi.espoo.evaka.application.ApplicationPreschoolTypeToggle.PRESCHOOL_ONLY
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.db.freeTextSearchPredicate
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate
import java.util.UUID
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

enum class ApplicationSortColumn {
    APPLICATION_TYPE,
    CHILD_NAME,
    DUE_DATE,
    START_DATE,
    STATUS,
    UNIT_NAME
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
    type: ApplicationType,
    guardianId: PersonId,
    childId: ChildId,
    origin: ApplicationOrigin,
    hideFromGuardian: Boolean = false,
    sentDate: LocalDate? = null,
): ApplicationId {
    // language=sql
    val sql =
        """
        INSERT INTO application (type, status, guardian_id, child_id, origin, hidefromguardian, sentdate, allow_other_guardian_access)
        VALUES (:type, 'CREATED'::application_status_type, :guardianId, :childId, :origin::application_origin_type, :hideFromGuardian, :sentDate, false)
        RETURNING id
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createUpdate(sql)
        .bind("type", type)
        .bind("guardianId", guardianId)
        .bind("childId", childId)
        .bind("origin", origin)
        .bind("hideFromGuardian", hideFromGuardian)
        .bind("sentDate", sentDate)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<ApplicationId>()
}

fun Database.Read.duplicateApplicationExists(
    childId: ChildId,
    guardianId: PersonId,
    type: ApplicationType
): Boolean =
    createQuery<Any> {
            sql(
                """
SELECT EXISTS(
    SELECT
    FROM application
    WHERE
        child_id = ${bind(childId)} AND
        guardian_id = ${bind(guardianId)} AND
        type = ${bind(type)} AND
        hidefromguardian = false AND
        status = ANY ('{CREATED,SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
)
                """
            )
        }
        .exactlyOne()

fun Database.Read.activePlacementExists(
    childId: ChildId,
    type: ApplicationType,
    today: LocalDate
): Boolean {
    val placementTypes =
        when (type) {
            ApplicationType.DAYCARE ->
                listOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME)
            ApplicationType.PRESCHOOL ->
                listOf(
                    PlacementType.PREPARATORY,
                    PlacementType.PREPARATORY_DAYCARE,
                    PlacementType.PRESCHOOL,
                    PlacementType.PRESCHOOL_DAYCARE,
                    PlacementType.PRESCHOOL_CLUB
                )
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
                :today <= end_date
        """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("childId", childId)
        .bind("types", placementTypes)
        .bind("today", today)
        .toList<Int>()
        .isNotEmpty()
}

data class PagedApplicationSummaries(
    val data: List<ApplicationSummary>,
    val total: Int,
    val pages: Int,
)

fun Database.Read.fetchApplicationSummaries(
    today: LocalDate,
    params: SearchApplicationRequest,
    readWithAssistanceNeed: AccessControlFilter<ApplicationId>?,
    readWithoutAssistanceNeed: AccessControlFilter<ApplicationId>?,
    canReadServiceWorkerNotes: Boolean
): PagedApplicationSummaries {
    val page = params.page ?: 1
    val pageSize = params.pageSize ?: 100
    val sortBy = params.sortBy ?: ApplicationSortColumn.CHILD_NAME
    val sortDir = params.sortDir ?: ApplicationSortDirection.ASC
    val areas = params.areas ?: emptyList()
    val units = params.units ?: emptyList()
    val basis = params.basis ?: emptyList()
    val type = params.type
    val preschoolType = params.preschoolType ?: emptyList()
    val statuses = params.statuses ?: emptyList()
    val dateType = params.dateType ?: emptyList()
    val distinctions = params.distinctions ?: emptyList()
    val periodStart = params.periodStart
    val periodEnd = params.periodEnd
    val searchTerms = params.searchTerms ?: ""
    val transferApplications = params.transferApplications ?: TransferApplicationFilter.ALL
    val voucherApplications = params.voucherApplications

    val assistanceNeeded = { value: Boolean ->
        PredicateSql {
            where(
                // value is intentionally inlined
                "coalesce(a.document->'careDetails'->>'assistanceNeeded', a.document->'clubCare'->>'assistanceNeeded')::boolean = $value"
            )
        }
    }

    val predicates =
        PredicateSql.allNotNull(
            PredicateSql { where("a.status = ANY(${bind(statuses)})") },
            if (areas.isNotEmpty()) PredicateSql { where("ca.id = ANY(${bind(areas)})") } else null,
            PredicateSql.all(
                basis.map { applicationBasis ->
                    when (applicationBasis) {
                        ApplicationBasis.ADDITIONAL_INFO ->
                            PredicateSql {
                                where(
                                    """
                            (a.document -> 'additionalDetails' ->> 'dietType') != '' OR 
                            (a.document -> 'additionalDetails' ->> 'otherInfo') != '' OR 
                            (a.document -> 'additionalDetails' ->> 'allergyType') != ''
                    """
                                )
                            }
                        ApplicationBasis.SIBLING_BASIS ->
                            PredicateSql {
                                where("(a.document -> 'apply' ->> 'siblingBasis')::boolean = true")
                            }
                        ApplicationBasis.ASSISTANCE_NEED -> assistanceNeeded(true)
                        ApplicationBasis.CLUB_CARE -> PredicateSql { where("was_on_club_care") }
                        ApplicationBasis.DAYCARE ->
                            PredicateSql {
                                where("(a.document ->> 'wasOnDaycare')::boolean = true")
                            }
                        ApplicationBasis.EXTENDED_CARE ->
                            PredicateSql {
                                where("(a.document ->> 'extendedCare')::boolean = true")
                            }
                        ApplicationBasis.DUPLICATE_APPLICATION ->
                            PredicateSql { where("has_duplicates") }
                        ApplicationBasis.URGENT ->
                            PredicateSql { where("(a.document ->> 'urgent')::boolean = true") }
                        ApplicationBasis.HAS_ATTACHMENTS ->
                            PredicateSql {
                                where(
                                    "((a.document ->> 'urgent')::boolean = true OR (a.document ->> 'extendedCare')::boolean = true) AND array_length(attachments.attachment_ids, 1) > 0"
                                )
                            }
                    }
                }
            ),
            if (type != ApplicationTypeToggle.ALL) PredicateSql { where("a.type = ${bind(type)}") }
            else null,
            if (type == ApplicationTypeToggle.PRESCHOOL) {
                data class PreschoolFlags(
                    val preparatory: Boolean?,
                    val connectedDaycare: Boolean,
                    val additionalDaycareApplication: Boolean,
                    val serviceNeedOptionType: PlacementType? = null
                )
                PredicateSql.any(
                    preschoolType.map {
                        when (it) {
                            PRESCHOOL_ONLY ->
                                PreschoolFlags(
                                    preparatory = false,
                                    connectedDaycare = false,
                                    additionalDaycareApplication = false
                                )
                            PRESCHOOL_DAYCARE ->
                                PreschoolFlags(
                                    preparatory = false,
                                    connectedDaycare = true,
                                    additionalDaycareApplication = false,
                                    serviceNeedOptionType = PlacementType.PRESCHOOL_DAYCARE
                                )
                            PRESCHOOL_CLUB ->
                                PreschoolFlags(
                                    preparatory = false,
                                    connectedDaycare = true,
                                    additionalDaycareApplication = false,
                                    serviceNeedOptionType = PlacementType.PRESCHOOL_CLUB
                                )
                            PREPARATORY_ONLY ->
                                PreschoolFlags(
                                    preparatory = true,
                                    connectedDaycare = false,
                                    additionalDaycareApplication = false
                                )
                            PREPARATORY_DAYCARE ->
                                PreschoolFlags(
                                    preparatory = true,
                                    connectedDaycare = true,
                                    additionalDaycareApplication = false
                                )
                            DAYCARE_ONLY ->
                                PreschoolFlags(
                                    preparatory = null,
                                    connectedDaycare = true,
                                    additionalDaycareApplication = true
                                )
                        }.run {
                            PredicateSql.all(
                                preparatory?.let {
                                    PredicateSql {
                                        where(
                                            "(a.document->'careDetails'->>'preparatory')::boolean = $preparatory"
                                        )
                                    }
                                } ?: PredicateSql.alwaysTrue(),
                                PredicateSql {
                                    where(
                                        "(a.document->>'connectedDaycare')::boolean = $connectedDaycare"
                                    )
                                },
                                PredicateSql {
                                    where(
                                        "a.additionalDaycareApplication = $additionalDaycareApplication"
                                    )
                                },
                                serviceNeedOptionType?.let { pt ->
                                    when (pt) {
                                        PlacementType.PRESCHOOL_DAYCARE ->
                                            PredicateSql {
                                                where(
                                                    "(a.document->'serviceNeedOption'->>'validPlacementType' = 'PRESCHOOL_DAYCARE' OR a.document->'serviceNeedOption'->>'validPlacementType' IS NULL)"
                                                )
                                            }
                                        PlacementType.PRESCHOOL_CLUB ->
                                            PredicateSql {
                                                where(
                                                    "a.document->'serviceNeedOption'->>'validPlacementType' = 'PRESCHOOL_CLUB'"
                                                )
                                            }
                                        else ->
                                            throw Error(
                                                "Unsupported preschool type: $serviceNeedOptionType"
                                            )
                                    }
                                } ?: PredicateSql.alwaysTrue()
                            )
                        }
                    }
                )
            } else null,
            if (distinctions.contains(ApplicationDistinctions.SECONDARY))
                PredicateSql { where("pu.preferredUnits && ${bind(units)}") }
            else if (units.isNotEmpty()) PredicateSql { where("d.id = ANY(${bind(units)})") }
            else null,
            PredicateSql.any(
                assistanceNeeded(true)
                    .and(readWithAssistanceNeed?.forTable("a") ?: PredicateSql.alwaysFalse()),
                assistanceNeeded(false)
                    .and(readWithoutAssistanceNeed?.forTable("a") ?: PredicateSql.alwaysFalse()),
            ),
            if (
                (periodStart != null || periodEnd != null) &&
                    dateType.contains(ApplicationDateType.DUE)
            )
                PredicateSql {
                    where(
                        "between_start_and_end(daterange(${bind(periodStart)}, ${bind(periodEnd)}, '[]'), a.dueDate)"
                    )
                }
            else null,
            if (
                (periodStart != null || periodEnd != null) &&
                    dateType.contains(ApplicationDateType.START)
            )
                PredicateSql {
                    where(
                        "between_start_and_end(daterange(${bind(periodStart)}, ${bind(periodEnd)}, '[]'), (a.document ->> 'preferredStartDate')::date)"
                    )
                }
            else null,
            if (
                (periodStart != null || periodEnd != null) &&
                    dateType.contains(ApplicationDateType.ARRIVAL)
            )
                PredicateSql {
                    where(
                        "between_start_and_end(daterange(${bind(periodStart)}, ${bind(periodEnd)}, '[]'), a.sentdate)"
                    )
                }
            else null,
            if (searchTerms.isNotBlank()) freeTextSearchPredicate(listOf("child"), searchTerms)
            else null,
            when (transferApplications) {
                TransferApplicationFilter.TRANSFER_ONLY ->
                    PredicateSql { where("a.transferApplication") }
                TransferApplicationFilter.NO_TRANSFER ->
                    PredicateSql { where("NOT a.transferApplication") }
                else -> null
            },
            when (voucherApplications) {
                VoucherApplicationFilter.VOUCHER_FIRST_CHOICE ->
                    PredicateSql { where("d.provider_type = 'PRIVATE_SERVICE_VOUCHER'") }
                VoucherApplicationFilter.VOUCHER_ONLY ->
                    PredicateSql {
                        where(
                            "pu.preferredUnits && (SELECT array_agg(id) FROM daycare WHERE provider_type = 'PRIVATE_SERVICE_VOUCHER')"
                        )
                    }
                VoucherApplicationFilter.NO_VOUCHER ->
                    PredicateSql {
                        where(
                            "NOT pu.preferredUnits && (SELECT array_agg(id) FROM daycare WHERE provider_type = 'PRIVATE_SERVICE_VOUCHER')"
                        )
                    }
                null -> null
            }
        )

    val orderBy =
        when (sortBy) {
            ApplicationSortColumn.APPLICATION_TYPE ->
                "ORDER BY type $sortDir, last_name, first_name"
            ApplicationSortColumn.CHILD_NAME -> "ORDER BY last_name $sortDir, first_name"
            ApplicationSortColumn.DUE_DATE -> "ORDER BY duedate $sortDir, last_name, first_name"
            ApplicationSortColumn.START_DATE ->
                "ORDER BY preferredStartDate $sortDir, last_name, first_name"
            ApplicationSortColumn.STATUS ->
                "ORDER BY application_status $sortDir, last_name, first_name"
            ApplicationSortColumn.UNIT_NAME -> "ORDER BY d.name $sortDir, last_name, first_name"
        }.exhaust()

    val applicationSummaries =
        createQuery<Any> {
                sql(
                    """
        SELECT
            a.id,
            child.first_name,
            child.last_name,
            child.date_of_birth,
            child.social_security_number,
            a.document,
            (a.document -> 'serviceNeedOption' ->> 'id')::uuid as serviceNeedId,
            a.document -> 'serviceNeedOption' ->> 'nameFi' as serviceNeedNameFi,
            a.document -> 'serviceNeedOption' ->> 'nameSv' as serviceNeedNameSv,
            a.document -> 'serviceNeedOption' ->> 'nameEn' as serviceNeedNameEn,
            a.document -> 'serviceNeedOption' ->> 'validPlacementType' as serviceNeedValidPlacementType,
            a.duedate,
            (a.document ->> 'preferredStartDate')::date as preferredStartDate,
            a.document -> 'apply' -> 'preferredUnits' as preferredUnits,
            a.type,
            a.status AS application_status,
            a.origin,
            a.transferapplication,
            a.additionaldaycareapplication,
            a.checkedbyadmin,
            a.service_worker_note,
            (
                COALESCE((a.document -> 'additionalDetails' ->> 'dietType'), '') != '' OR
                COALESCE((a.document -> 'additionalDetails' ->> 'otherInfo'), '') != '' OR
                COALESCE((a.document -> 'additionalDetails' ->> 'allergyType'), '') != ''
            ) as additionalInfo,
            (a.document -> 'apply' ->> 'siblingBasis')::boolean as siblingBasis,
            COALESCE(a.document -> 'careDetails' ->> 'assistanceNeeded', a.document -> 'clubCare' ->> 'assistanceNeeded')::boolean as assistanceNeed,
            club_care.was_on_club_care AS was_on_club_care,
            (a.document ->> 'wasOnDaycare')::boolean as wasOnDaycare,
            COALESCE((a.document ->> 'urgent')::boolean, false) as urgent,
            (SELECT COALESCE(array_length(attachments.attachment_ids, 1), 0)) AS attachmentCount,
            COALESCE((a.document ->> 'extendedCare')::boolean, false) as extendedCare,
            has_duplicates,
            pp.unit_confirmation_status,
            pp.unit_reject_reason,
            pp.unit_reject_other_reason,
            pp.start_date AS placement_plan_start_date,
            (CASE WHEN pp.unit_id IS NOT NULL THEN d.name END) AS placement_plan_unit_name,
            cpu.id AS current_placement_unit_id,
            cpu.name AS current_placement_unit_name,
            count(*) OVER () AS total,
            pu.preferredUnits
        FROM application a
        JOIN person child ON child.id = a.child_id
        LEFT JOIN placement_plan pp ON pp.application_id = a.id
        JOIN daycare d ON COALESCE(pp.unit_id, (a.document -> 'apply' -> 'preferredUnits' ->> 0)::uuid) = d.id
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
            SELECT application_id, array_agg(id) AS attachment_ids
            FROM attachment
            WHERE application_id IS NOT NULL
            GROUP by application_id
        ) attachments ON a.id = attachments.application_id
        LEFT JOIN LATERAL (
            SELECT EXISTS( SELECT 1 
            FROM placement p 
            WHERE p.child_id = a.child_id AND p.type = 'CLUB'::placement_type AND p.start_date <= ${bind(today)}) AS was_on_club_care
        ) club_care ON true
        LEFT JOIN LATERAL (
            SELECT daycare.id, daycare.name
            FROM daycare
            JOIN placement ON daycare.id = placement.unit_id
            WHERE placement.child_id = a.child_id AND daterange(start_date, end_date, '[]') && daterange(${bind(today)}, null, '[]')
            ORDER BY start_date
            LIMIT 1
        ) cpu ON true
        LEFT JOIN LATERAL ( 
            SELECT COALESCE(array_agg(e::UUID) FILTER (WHERE e IS NOT NULL), '{}'::UUID[]) AS preferredUnits
            FROM jsonb_array_elements_text(a.document -> 'apply' -> 'preferredUnits') e
        ) pu ON true
        WHERE a.status != 'CREATED'::application_status_type AND ${predicate(predicates)}
        $orderBy LIMIT $pageSize OFFSET ${bind((page - 1) * pageSize)}
        """
                )
            }
            .mapToPaged(::PagedApplicationSummaries, pageSize, "total") {
                val status = column<ApplicationStatus>("application_status")
                ApplicationSummary(
                    id = column("id"),
                    firstName = column("first_name"),
                    lastName = column("last_name"),
                    socialSecurityNumber = column("social_security_number"),
                    dateOfBirth = column("date_of_birth"),
                    type = column("type"),
                    placementType = mapRequestedPlacementType("document"),
                    serviceNeed =
                        column<ServiceNeedOptionId?>("serviceNeedId")?.let {
                            ServiceNeedOption(
                                it,
                                column("serviceNeedNameFi"),
                                column("serviceNeedNameSv"),
                                column("serviceNeedNameEn"),
                                column("serviceNeedValidPlacementType")
                            )
                        },
                    dueDate = column("duedate"),
                    startDate = column("preferredStartDate"),
                    preferredUnits =
                        jsonColumn<List<String>>("preferredUnits").map {
                            PreferredUnit(
                                id = DaycareId(UUID.fromString(it)),
                                name = "" // filled afterwards
                            )
                        },
                    origin = column("origin"),
                    checkedByAdmin = column("checkedbyadmin"),
                    status = status,
                    additionalInfo = column("additionalInfo"),
                    serviceWorkerNote =
                        if (canReadServiceWorkerNotes) column("service_worker_note") else "",
                    siblingBasis = column("siblingBasis"),
                    assistanceNeed = column("assistanceNeed"),
                    wasOnClubCare = column("was_on_club_care"),
                    wasOnDaycare = column("wasOnDaycare"),
                    extendedCare = column("extendedCare"),
                    duplicateApplication = column("has_duplicates"),
                    transferApplication = column("transferapplication"),
                    urgent = column("urgent"),
                    attachmentCount = column("attachmentCount"),
                    additionalDaycareApplication = column("additionaldaycareapplication"),
                    placementProposalStatus =
                        column<PlacementPlanConfirmationStatus?>("unit_confirmation_status")
                            ?.takeIf { status == ApplicationStatus.WAITING_UNIT_CONFIRMATION }
                            ?.let {
                                PlacementProposalStatus(
                                    unitConfirmationStatus = it,
                                    unitRejectReason = column("unit_reject_reason"),
                                    unitRejectOtherReason = column("unit_reject_other_reason")
                                )
                            },
                    placementPlanStartDate = column("placement_plan_start_date"),
                    placementPlanUnitName = column("placement_plan_unit_name"),
                    currentPlacementUnit =
                        column<DaycareId?>("current_placement_unit_id")?.let {
                            PreferredUnit(it, column("current_placement_unit_name"))
                        }
                )
            }

    // language=sql
    val unitSql =
        """
        SELECT id, name
        FROM daycare
        WHERE id = ANY(:unitIds)
        """
            .trimIndent()
    val unitIds =
        applicationSummaries.data.flatMap { summary ->
            summary.preferredUnits.map { unit -> unit.id }
        }
    val unitMap =
        @Suppress("DEPRECATION")
        createQuery(unitSql).bind("unitIds", unitIds).toMap {
            columnPair<DaycareId, String>("id", "name")
        }

    return applicationSummaries.copy(
        data =
            applicationSummaries.data.map { summary ->
                summary.copy(
                    preferredUnits =
                        summary.preferredUnits.map {
                            PreferredUnit(id = it.id, name = unitMap.getOrDefault(it.id, ""))
                        }
                )
            }
    )
}

fun Database.Read.fetchApplicationSummariesForGuardian(
    guardianId: PersonId
): List<PersonApplicationSummary> {
    // language=SQL
    val sql =
        """
        SELECT
            a.id AS applicationId,
            a.preferredUnit AS preferredUnitId,
            a.preferredStartDate, a.sentDate, a.type,
            a.childId, a.childName, a.childSsn,
            a.guardianId, concat(p.last_name, ' ', p.first_name) as guardianName,
            coalesce(a.connecteddaycare, false) AS connectedDaycare,
            coalesce(a.preparatoryeducation, false) AS preparatoryEducation,
            d.name AS preferredUnitName,
            a.status
        FROM application_view a
        LEFT JOIN daycare d ON a.preferredUnit = d.id
        LEFT JOIN person p ON a.guardianId = p.id
        WHERE guardianId = :guardianId
        AND status != 'CREATED'::application_status_type
        ORDER BY sentDate DESC
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("guardianId", guardianId).toList<PersonApplicationSummary>()
}

fun Database.Read.fetchApplicationSummariesForChild(
    childId: ChildId,
    filter: AccessControlFilter<ApplicationId>
): List<PersonApplicationSummary> =
    createQuery<Any> {
            sql(
                """
        SELECT
            a.id AS applicationId,
            a.preferredUnit AS preferredUnitId,
            a.preferredStartDate, a.sentDate, a.type,
            a.childId, a.childName, a.childSsn,
            a.guardianId, concat(p.last_name, ' ', p.first_name) as guardianName,
            coalesce(a.connecteddaycare, false) as connecteddaycare,
            coalesce(a.preparatoryeducation, false) as preparatoryeducation,
            d.name AS preferredUnitName,
            a.status
        FROM application_view a
        LEFT JOIN daycare d ON a.preferredUnit = d.id
        LEFT JOIN person p ON a.guardianId = p.id
        WHERE childId = ${bind(childId)}
        AND ${predicate(filter.forTable("a"))}
        AND status != 'CREATED'::application_status_type
        ORDER BY sentDate DESC
        """
                    .trimIndent()
            )
        }
        .toList<PersonApplicationSummary>()

fun Database.Read.fetchApplicationSummariesForCitizen(
    citizenId: PersonId
): List<CitizenApplicationSummary> =
    createQuery<Any> {
            val useDecisionDateAsStartDate =
                listOf(
                    ApplicationStatus.ACTIVE,
                    ApplicationStatus.WAITING_CONFIRMATION,
                )
            sql(
                """
SELECT
    a.id AS application_id,
    a.type,
    a.child_id,
    (a.document -> 'child' ->> 'lastName') || ' ' || (a.document -> 'child' ->> 'firstName') AS child_name,
    (
        SELECT name
        FROM daycare d
        WHERE d.id = (a.document -> 'apply' -> 'preferredUnits' ->> 0)::uuid
    ) AS preferred_unit_name,
    COALESCE((
        SELECT array_agg(d.name)
        FROM jsonb_array_elements_text(a.document -> 'apply' -> 'preferredUnits') pu
        JOIN daycare d ON d.id = pu::uuid
    ), '{}'::text[]) AS all_preferred_unit_names,
    COALESCE(
        CASE WHEN a.status = ANY(${bind(useDecisionDateAsStartDate)}::application_status_type[]) THEN (
            SELECT min(coalesce(d.requested_start_date, d.start_date))
            FROM decision d
            WHERE d.application_id = a.id AND d.status != 'REJECTED'
        ) END,
        (a.document ->> 'preferredStartDate')::date
    ) AS start_date,
    a.sentDate,
    a.status AS application_status,
    a.created AS created_date,
    a.form_modified AS modified_date,
    a.transferapplication
FROM application a
WHERE (a.guardian_id = ${bind(citizenId)} OR EXISTS (
    SELECT 1 FROM application_other_guardian WHERE application_id = a.id AND guardian_id = ${bind(citizenId)}
))
AND NOT a.hidefromguardian AND a.status != 'CANCELLED'
ORDER BY sentDate DESC
                """
            )
        }
        .toList()

data class CitizenChildren(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val socialSecurityNumber: String?,
    val duplicateOf: PersonId?,
)

fun Database.Read.getCitizenChildren(today: LocalDate, citizenId: PersonId): List<CitizenChildren> {
    // language=sql
    val sql =
        """
        SELECT child.id, first_name, last_name, date_of_birth, social_security_number, duplicate_of
        FROM guardian LEFT JOIN person child ON guardian.child_id = child.id
        WHERE guardian_id = :citizenId

        UNION

        SELECT child.id, first_name, last_name, date_of_birth, social_security_number, duplicate_of
        FROM foster_parent JOIN person child ON foster_parent.child_id = child.id
        WHERE parent_id = :citizenId AND valid_during @> :today
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("citizenId", citizenId)
        .bind("today", today)
        .toList<CitizenChildren>()
}

fun Database.Read.fetchApplicationDetails(
    applicationId: ApplicationId,
    includeCitizenAttachmentsOnly: Boolean = false
): ApplicationDetails? {
    val attachmentWhereClause =
        if (includeCitizenAttachmentsOnly) "WHERE eu.type = 'CITIZEN'" else ""
    // language=sql
    val sql =
        """
        SELECT 
            a.id,
            a.type,
            a.document,
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
            a.form_modified,
            a.sentdate,
            a.duedate,
            a.duedate_set_manually_at,
            a.checkedbyadmin,
            a.allow_other_guardian_access,
            coalesce(att.json, '[]'::jsonb) attachments
        FROM application a
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
        """
            .trimIndent()

    val application =
        @Suppress("DEPRECATION")
        createQuery(sql).bind("id", applicationId).exactlyOneOrNull {
            val childRestricted = column("child_restricted") ?: false
            val guardianRestricted = column("guardian_restricted") ?: false
            val deserializedForm =
                if (jsonColumn<FormWithType>("document").type == "CLUB") {
                    jsonColumn<ClubFormV0>("document")
                } else {
                    jsonColumn<DaycareFormV0>("document")
                }

            ApplicationDetails(
                id = column("id"),
                type = column("type"),
                form =
                    deserializedForm.let {
                        ApplicationForm.fromV0(it, childRestricted, guardianRestricted)
                    },
                status = column("status"),
                origin = column("origin"),
                childId = column("child_id"),
                guardianId = column("guardian_id"),
                otherGuardianId = column("other_guardian_id"),
                otherGuardianLivesInSameAddress = null,
                childRestricted = childRestricted,
                guardianRestricted = guardianRestricted,
                guardianDateOfDeath = column("guardian_date_of_death"),
                transferApplication = column("transferapplication"),
                additionalDaycareApplication = column("additionaldaycareapplication"),
                createdDate = column("created"),
                modifiedDate = column("form_modified"),
                sentDate = column("sentdate"),
                dueDate = column("duedate"),
                dueDateSetManuallyAt = column("duedate_set_manually_at"),
                checkedByAdmin = column("checkedbyadmin"),
                hideFromGuardian = column("hidefromguardian"),
                allowOtherGuardianAccess = column("allow_other_guardian_access"),
                attachments = jsonColumn("attachments")
            )
        }

    if (application != null) {
        // language=sql
        val unitSql =
            """
            SELECT id, name
            FROM daycare
            WHERE id = ANY(:unitIds)
            """
                .trimIndent()
        val unitIds = application.form.preferences.preferredUnits.map { it.id }
        val unitMap =
            @Suppress("DEPRECATION")
            createQuery(unitSql).bind("unitIds", unitIds).toMap {
                columnPair<DaycareId, String>("id", "name")
            }

        return application.copy(
            form =
                application.form.copy(
                    preferences =
                        application.form.preferences.copy(
                            preferredUnits =
                                application.form.preferences.preferredUnits.map {
                                    PreferredUnit(
                                        id = it.id,
                                        name = unitMap.getOrDefault(it.id, "")
                                    )
                                }
                        )
                )
        )
    } else {
        return null
    }
}

fun Database.Read.getApplicationUnitSummaries(unitId: DaycareId): List<ApplicationUnitSummary> {
    // language=sql
    val sql =
        """
        SELECT
            a.id,
            a.type,
            (a.document -> 'serviceNeedOption' ->> 'id')::uuid as serviceNeedId,
            a.document -> 'serviceNeedOption' ->> 'nameFi' AS serviceNeedNameFi,
            a.document -> 'serviceNeedOption' ->> 'nameSv' AS serviceNeedNameSv,
            a.document -> 'serviceNeedOption' ->> 'nameEn' AS serviceNeedNameEn,
            a.document -> 'serviceNeedOption' ->> 'validPlacementType' AS serviceNeedValidPlacementType,
            a.document,
            (a.document ->> 'preferredStartDate')::date as preferred_start_date,
            (array_position((
                SELECT array_agg(e)
                FROM jsonb_array_elements_text(a.document -> 'apply' -> 'preferredUnits') e
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
        JOIN person c ON c.id = a.child_id
        JOIN person g ON g.id = a.guardian_id
        WHERE EXISTS (
            SELECT 1
            FROM jsonb_array_elements_text(a.document -> 'apply' -> 'preferredUnits') e
            WHERE e = :unitId::text
        ) AND a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_DECISION}'::application_status_type[])
        ORDER BY c.last_name, c.first_name
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("unitId", unitId).toList {
        ApplicationUnitSummary(
            applicationId = column("id"),
            firstName = column("first_name"),
            lastName = column("last_name"),
            dateOfBirth = column("date_of_birth"),
            guardianFirstName = column("guardian_first_name"),
            guardianLastName = column("guardian_last_name"),
            guardianPhone = column("guardian_phone"),
            guardianEmail = column("guardian_email"),
            requestedPlacementType = mapRequestedPlacementType("document"),
            serviceNeed =
                column<ServiceNeedOptionId?>("serviceNeedId")?.let {
                    ServiceNeedOption(
                        it,
                        column("serviceNeedNameFi"),
                        column("serviceNeedNameSv"),
                        column("serviceNeedNameEn"),
                        column("serviceNeedValidPlacementType")
                    )
                },
            preferredStartDate = column("preferred_start_date"),
            preferenceOrder = column("preference_order"),
            status = column("status")
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true) data class FormWithType(val type: String)

fun Row.mapRequestedPlacementType(colName: String): PlacementType =
    when (jsonColumn<FormWithType>(colName).type) {
        "CLUB" -> PlacementType.CLUB
        "DAYCARE" -> {
            if (jsonColumn<DaycareFormV0>(colName).partTime) {
                PlacementType.DAYCARE_PART_TIME
            } else {
                PlacementType.DAYCARE
            }
        }
        "PRESCHOOL" -> {
            jsonColumn<DaycareFormV0>(colName).let {
                if (it.careDetails.preparatory == true) {
                    if (it.connectedDaycare == true) {
                        PlacementType.PREPARATORY_DAYCARE
                    } else {
                        PlacementType.PREPARATORY
                    }
                } else {
                    if (it.connectedDaycare == true) {
                        it.serviceNeedOption?.validPlacementType ?: PlacementType.PRESCHOOL_DAYCARE
                    } else {
                        PlacementType.PRESCHOOL
                    }
                }
            }
        }
        else -> throw Error("unknown form type")
    }

fun Database.Read.getApplicationType(id: ApplicationId): ApplicationType =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT type FROM application WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOne<ApplicationType>()

fun Database.Transaction.updateForm(
    id: ApplicationId,
    form: ApplicationForm,
    formType: ApplicationType,
    childRestricted: Boolean,
    guardianRestricted: Boolean,
    now: HelsinkiDateTime
) {
    check(getApplicationType(id) == formType) { "Invalid form type for the application" }
    val transformedForm =
        if (formType == ApplicationType.CLUB) {
            ClubFormV0.fromForm2(form, childRestricted, guardianRestricted)
        } else {
            DaycareFormV0.fromForm2(form, formType, childRestricted, guardianRestricted)
        }

    // language=SQL
    val sql =
        "UPDATE application SET document = :document, form_modified = :now WHERE id = :applicationId;"

    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("applicationId", id)
        .bindJson("document", transformedForm)
        .bind("now", now)
        .execute()
}

fun Database.Transaction.setCheckedByAdminToDefault(id: ApplicationId, form: ApplicationForm) {
    // language=SQL
    val sql = "UPDATE application SET checkedbyadmin = :checked WHERE id = :applicationId"

    val default =
        !form.child.assistanceNeeded &&
            form.child.allergies.isBlank() &&
            form.child.diet.isBlank() &&
            form.otherInfo.isBlank()

    @Suppress("DEPRECATION")
    createUpdate(sql).bind("applicationId", id).bind("checked", default).execute()
}

fun Database.Transaction.updateApplicationStatus(id: ApplicationId, status: ApplicationStatus) {
    // language=SQL
    val sql = "UPDATE application SET status = :status WHERE id = :id"

    @Suppress("DEPRECATION") createUpdate(sql).bind("id", id).bind("status", status).execute()
}

fun Database.Transaction.updateApplicationDates(
    id: ApplicationId,
    sentDate: LocalDate,
    dueDate: LocalDate?
) {
    // language=SQL
    val sql = "UPDATE application SET sentdate = :sentDate, duedate = :dueDate WHERE id = :id"

    @Suppress("DEPRECATION")
    createUpdate(sql).bind("id", id).bind("sentDate", sentDate).bind("dueDate", dueDate).execute()
}

fun Database.Transaction.updateApplicationFlags(
    id: ApplicationId,
    applicationFlags: ApplicationFlags
) =
    @Suppress("DEPRECATION")
    createUpdate(
            """
                UPDATE application
                SET
                    transferapplication = :isTransferApplication,
                    additionaldaycareapplication = :isAdditionalDaycareApplication
                WHERE id = :id
            """
                .trimIndent()
        )
        .bind("id", id)
        .bindKotlin(applicationFlags)
        .execute()

fun Database.Transaction.updateApplicationAllowOtherGuardianAccess(
    id: ApplicationId,
    allowOtherGuardianAccess: Boolean
) =
    createUpdate<DatabaseTable> {
            sql(
                """
                    UPDATE application
                    SET allow_other_guardian_access = ${bind(allowOtherGuardianAccess)}
                    WHERE id = ${bind(id)}
                """
                    .trimIndent()
            )
        }
        .execute()

fun Database.Transaction.updateApplicationOtherGuardian(
    applicationId: ApplicationId,
    otherGuardianId: PersonId?
) {
    // language=SQL
    val sql =
        "UPDATE application SET other_guardian_id = :otherGuardianId WHERE id = :applicationId"

    @Suppress("DEPRECATION")
    createUpdate(sql)
        .bind("applicationId", applicationId)
        .bind("otherGuardianId", otherGuardianId)
        .execute()
}

fun Database.Transaction.syncApplicationOtherGuardians(id: ApplicationId) {
    createUpdate<DatabaseTable> {
            sql("DELETE FROM application_other_guardian WHERE application_id = ${bind(id)}")
        }
        .execute()

    createUpdate<DatabaseTable> {
            sql(
                """
            INSERT INTO application_other_guardian (application_id, guardian_id)
            SELECT application.id, other_citizen.id
            FROM application
            JOIN LATERAL (
                SELECT guardian_id AS id
                FROM guardian
                WHERE application.child_id = guardian.child_id AND application.guardian_id != guardian.guardian_id
                AND EXISTS (
                    SELECT 1
                    FROM guardian WHERE application.child_id = guardian.child_id AND guardian.guardian_id = application.guardian_id
                )

                UNION

                SELECT parent_id AS id
                FROM foster_parent
                WHERE application.child_id = foster_parent.child_id AND application.guardian_id != foster_parent.parent_id
                AND EXISTS (
                    SELECT 1
                    FROM foster_parent WHERE application.child_id = foster_parent.child_id AND foster_parent.parent_id = application.guardian_id
                )
            ) other_citizen ON true
            WHERE application.id = ${bind(id)}
        """
                    .trimIndent()
            )
        }
        .execute()
}

fun Database.Transaction.setApplicationVerified(id: ApplicationId, verified: Boolean) {
    // language=SQL
    val sql = "UPDATE application SET checkedByAdmin = :verified WHERE id = :id"

    @Suppress("DEPRECATION") createUpdate(sql).bind("verified", verified).bind("id", id).execute()
}

fun Database.Transaction.deleteApplication(id: ApplicationId) =
    @Suppress("DEPRECATION")
    createUpdate("DELETE FROM application WHERE id = :id").bind("id", id).execute()

fun Database.Transaction.removeOldDrafts(clock: EvakaClock) {
    // ~2 months
    val thresholdDays = 60

    // language=SQL
    val applicationIds =
        createQuery<DatabaseTable> {
                sql(
                    "SELECT id FROM application WHERE status = 'CREATED' AND created < ${bind(clock.today())} - ${bind(thresholdDays)}"
                )
            }
            .toList<ApplicationId>()

    if (applicationIds.isNotEmpty()) {
        logger.info(
            "Cleaning up ${applicationIds.size} draft applications older than $thresholdDays days"
        )

        // language=SQL
        @Suppress("DEPRECATION")
        createUpdate(
                """DELETE FROM application_note WHERE application_id = ANY(:applicationIds::uuid[])"""
            )
            .bind("applicationIds", applicationIds)
            .execute()

        @Suppress("DEPRECATION")
        createUpdate("""DELETE FROM application WHERE id = ANY(:applicationIds::uuid[])""")
            .bind("applicationIds", applicationIds)
            .execute()
    }
}

fun Database.Transaction.cancelOutdatedSentTransferApplications(
    clock: EvakaClock
): List<ApplicationId> =
    @Suppress("DEPRECATION")
    createUpdate(
            // only include applications that don't have decisions
            // placement type checks are doing in inverse so that the addition and accidental
            // omission of new placement types
            // does not cause the cancellation of applications that shouldn't be cancelled
            """
UPDATE application SET status = :cancelled
WHERE transferapplication
AND status = ANY('{SENT}')
AND NOT EXISTS (
    SELECT 1
    FROM placement p
    WHERE (CASE
        WHEN application.type = 'DAYCARE' THEN NOT p.type = ANY(:notDaycarePlacements::placement_type[])
        WHEN application.type = 'PRESCHOOL' THEN NOT p.type = ANY(:notPreschoolPlacements::placement_type[])
        WHEN application.type = 'CLUB' THEN NOT p.type = ANY(:notClubPlacements::placement_type[])
    END)
    AND daterange((application.document->>'preferredStartDate')::date, null, '[]') && daterange(p.start_date, p.end_date, '[]')
    AND p.end_date >= :yesterday
)
RETURNING id
"""
        )
        .bind("cancelled", ApplicationStatus.CANCELLED)
        .bind("yesterday", clock.today().minusDays(1))
        .bind(
            "notDaycarePlacements",
            arrayOf(
                PlacementType.CLUB,
                PlacementType.PRESCHOOL,
                PlacementType.PRESCHOOL_DAYCARE,
                PlacementType.PRESCHOOL_CLUB,
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
                PlacementType.PRESCHOOL_CLUB,
                PlacementType.PRESCHOOL_DAYCARE,
                PlacementType.PREPARATORY,
                PlacementType.PREPARATORY_DAYCARE,
                PlacementType.TEMPORARY_DAYCARE,
                PlacementType.TEMPORARY_DAYCARE_PART_DAY,
                PlacementType.SCHOOL_SHIFT_CARE
            )
        )
        .executeAndReturnGeneratedKeys()
        .toList<ApplicationId>()

fun Database.Read.getApplicationAttachments(
    applicationId: ApplicationId
): List<ApplicationAttachment> =
    @Suppress("DEPRECATION")
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
        .toList<ApplicationAttachment>()

fun Database.Transaction.cancelAllActiveTransferApplications(
    childId: ChildId
): List<ApplicationId> =
    @Suppress("DEPRECATION")
    createUpdate(
            // language=sql
            """
UPDATE application
SET status = 'CANCELLED'
WHERE transferapplication
AND child_id = :childId
AND status = ANY(:activeApplicationStatus::application_status_type[])
RETURNING id
    """
                .trimIndent()
        )
        .bind("activeApplicationStatus", arrayOf(ApplicationStatus.SENT))
        .bind("childId", childId)
        .executeAndReturnGeneratedKeys()
        .toList<ApplicationId>()

fun Database.Read.fetchApplicationNotificationCountForCitizen(citizenId: PersonId): Int =
    createQuery<Any> {
            sql(
                """
SELECT COUNT(*)
FROM application a
WHERE guardian_id = ${bind(citizenId)}
AND NOT a.hidefromguardian
AND NOT EXISTS (
    SELECT 1 FROM guardian_blocklist bl
    WHERE bl.child_id = a.child_id
    AND bl.guardian_id = ${bind(citizenId)}
)
AND a.status = 'WAITING_CONFIRMATION'
                """
            )
        }
        .exactlyOne()

fun Database.Read.personHasSentApplicationWithId(
    citizenId: PersonId,
    applicationId: ApplicationId
): Boolean {
    val sql =
        """
    SELECT EXISTS (
        SELECT 1
        FROM application app
        WHERE app.guardian_id = :citizenId 
              AND app.id = :applicationId
    )
    """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("citizenId", citizenId)
        .bind("applicationId", applicationId)
        .exactlyOne<Boolean>()
}
