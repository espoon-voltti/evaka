// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import evaka.codegen.api.TsProject.E2ETest
import evaka.codegen.api.TsProject.LibCommon
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.messaging.MessageReceiver
import fi.espoo.evaka.pairing.MobileDeviceDetails
import fi.espoo.evaka.pis.SystemController
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeInterval
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.vasu.CurriculumTemplateError
import fi.espoo.evaka.vasu.VasuQuestion
import java.math.BigDecimal
import java.net.URI
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID
import kotlin.reflect.KType
import kotlin.reflect.typeOf

const val basePackage = "fi.espoo.evaka"

val forceIncludes: Set<KType> =
    setOf(
        typeOf<SystemController.CitizenUserResponse>(),
        typeOf<SystemController.EmployeeUserResponse>(),
        typeOf<MobileDeviceDetails>(),
        typeOf<CurriculumTemplateError>()
    )

// these endpoint paths are deprecated, and API clients should not call them
private val deprecatedEndpoints =
    setOf(
        "/attachments/citizen/{attachmentId}",
        "/decisions/confirm",
        "/decisions/head-of-family/{id}",
        "/decisions/head-of-family/{id}/create-retroactive",
        "/decisions/{id}",
        "/decisions/ignore",
        "/decisions/mark-sent",
        "/decisions/pdf/{decisionId}",
        "/decisions/search",
        "/decisions/set-type/{id}",
        "/decisions/unignore",
        "/person/identity/{personId}",
        "/public/club-terms",
        "/public/pairings/challenge",
        "/public/pairings/{id}/status",
        "/public/preschool-terms",
        "/public/service-needs/options",
        "/public/units",
        "/public/units/{applicationType}",
        "/units"
    )

// these endpoint paths are planned, and API clients should not call them *yet*
private val plannedEndpoints =
    setOf(
        "/employee/absences/by-child/{childId}",
        "/employee/absences/by-child/{childId}/delete",
        "/employee/absences/{groupId}",
        "/employee/absences/{groupId}/delete-holiday-reservations",
        "/employee/absences/{groupId}/present",
        "/employee/applications",
        "/employee/applications/{applicationId}",
        "/employee/applications/{applicationId}/actions/accept-decision",
        "/employee/applications/{applicationId}/actions/{action}",
        "/employee/applications/{applicationId}/actions/create-placement-plan",
        "/employee/applications/{applicationId}/actions/reject-decision",
        "/employee/applications/{applicationId}/actions/respond-to-placement-proposal",
        "/employee/applications/{applicationId}/actions/send-application",
        "/employee/applications/{applicationId}/decision-drafts",
        "/employee/applications/{applicationId}/placement-draft",
        "/employee/applications/batch/actions/{action}",
        "/employee/applications/by-child/{childId}",
        "/employee/applications/by-guardian/{guardianId}",
        "/employee/applications/placement-proposals/{unitId}/accept",
        "/employee/applications/search",
        "/employee/applications/units/{unitId}",
        "/employee/areas",
        "/employee/assistance-action-options",
        "/employee/assistance-actions/{id}",
        "/employee/assistance-factors/{id}",
        "/employee/assistance-need-decision/{id}",
        "/employee/assistance-need-decision/{id}/annul",
        "/employee/assistance-need-decision/{id}/decide",
        "/employee/assistance-need-decision/{id}/decision-maker-option",
        "/employee/assistance-need-decision/{id}/mark-as-opened",
        "/employee/assistance-need-decision/{id}/revert-to-unsent",
        "/employee/assistance-need-decision/{id}/send",
        "/employee/assistance-need-decision/{id}/update-decision-maker",
        "/employee/assistance-need-preschool-decisions/{id}",
        "/employee/assistance-need-preschool-decisions/{id}/annul",
        "/employee/assistance-need-preschool-decisions/{id}/decide",
        "/employee/assistance-need-preschool-decisions/{id}/decision-maker",
        "/employee/assistance-need-preschool-decisions/{id}/decision-maker-options",
        "/employee/assistance-need-preschool-decisions/{id}/mark-as-opened",
        "/employee/assistance-need-preschool-decisions/{id}/send",
        "/employee/assistance-need-preschool-decisions/{id}/unsend",
        "/employee/assistance-need-voucher-coefficients/{id}",
        "/employee/attachments/{attachmentId}",
        "/employee/attendance-reservations",
        "/employee/attendance-reservations/child-date",
        "/employee/attendance-reservations/child-date/expected-absences",
        "/employee/backup-cares/{id}",
        "/employee/backup-pickups/{id}",
        "/employee/calendar-event",
        "/employee/calendar-event/{id}",
        "/employee/calendar-event/{id}/time",
        "/employee/calendar-event/reservation",
        "/employee/calendar-event-time/{id}",
        "/employee/child-documents",
        "/employee/child-documents/{documentId}",
        "/employee/child-documents/{documentId}/content",
        "/employee/child-documents/{documentId}/lock",
        "/employee/child-documents/{documentId}/next-status",
        "/employee/child-documents/{documentId}/prev-status",
        "/employee/child-documents/{documentId}/publish",
        "/employee/children/{child}/assistance",
        "/employee/children/{child}/assistance-factors",
        "/employee/children/{child}/daycare-assistances",
        "/employee/children/{childId}/assistance-actions",
        "/employee/children/{childId}/backup-cares",
        "/employee/children/{childId}/backup-pickups",
        "/employee/children/{childId}/daily-service-times",
        "/employee/children/{childId}/vasu",
        "/employee/children/{childId}/vasu-summaries",
        "/employee/children/{child}/other-assistance-measures",
        "/employee/children/{child}/preschool-assistances",
        "/employee/club-terms",
        "/employee/club-terms/{id}",
        "/employee/daily-service-times/{id}",
        "/employee/daily-service-times/{id}/end",
        "/employee/daycare-assistances/{id}",
        "/employee/daycares/{daycareId}/backup-cares",
        "/employee/decisions/by-application",
        "/employee/decisions/by-child",
        "/employee/decisions/by-guardian",
        "/employee/decisions/{id}/download",
        "/employee/decisions/units",
        "/employee/document-templates",
        "/employee/document-templates/active",
        "/employee/document-templates/import",
        "/employee/document-templates/{templateId}",
        "/employee/document-templates/{templateId}/content",
        "/employee/document-templates/{templateId}/duplicate",
        "/employee/document-templates/{templateId}/export",
        "/employee/document-templates/{templateId}/publish",
        "/employee/document-templates/{templateId}/validity",
        "/employee/employees",
        "/employee/employees/finance-decision-handler",
        "/employee/employees/{id}",
        "/employee/employees/{id}/activate",
        "/employee/employees/{id}/daycare-roles",
        "/employee/employees/{id}/deactivate",
        "/employee/employees/{id}/details",
        "/employee/employees/{id}/global-roles",
        "/employee/employees/pin-code",
        "/employee/employees/pin-code/is-pin-locked",
        "/employee/employees/preferred-first-name",
        "/employee/employees/search",
        "/employee/family/by-adult/{id}",
        "/employee/family/contacts",
        "/employee/family/contacts/priority",
        "/employee/fee-alterations",
        "/employee/fee-alterations/{feeAlterationId}",
        "/employee/fee-decisions/confirm",
        "/employee/fee-decisions/head-of-family/{id}",
        "/employee/fee-decisions/head-of-family/{id}/create-retroactive",
        "/employee/fee-decisions/{id}",
        "/employee/fee-decisions/ignore",
        "/employee/fee-decisions/mark-sent",
        "/employee/fee-decisions/pdf/{decisionId}",
        "/employee/fee-decisions/search",
        "/employee/fee-decisions/set-type/{id}",
        "/employee/fee-decisions/unignore",
        "/employee/filters/units",
        "/employee/finance-basics/fee-thresholds",
        "/employee/finance-basics/fee-thresholds/{id}",
        "/employee/finance-basics/voucher-values",
        "/employee/finance-basics/voucher-values/{id}",
        "/employee/foster-parent",
        "/employee/foster-parent/by-child/{childId}",
        "/employee/foster-parent/by-parent/{parentId}",
        "/employee/foster-parent/{id}",
        "/employee/group-placements/{groupPlacementId}",
        "/employee/group-placements/{groupPlacementId}/transfer",
        "/employee/holiday-period",
        "/employee/holiday-period/{id}",
        "/employee/holiday-period/questionnaire",
        "/employee/holiday-period/questionnaire/{id}",
        "/employee/incomes",
        "/employee/incomes/{incomeId}",
        "/employee/incomes/multipliers",
        "/employee/incomes/notifications",
        "/employee/income-statements/awaiting-handler",
        "/employee/income-statements/child/{childId}",
        "/employee/income-statements/guardian/{guardianId}/children",
        "/employee/income-statements/{incomeStatementId}/handled",
        "/employee/income-statements/person/{personId}",
        "/employee/income-statements/person/{personId}/{incomeStatementId}",
        "/employee/incomes/types",
        "/employee/invoice-corrections",
        "/employee/invoice-corrections/{id}",
        "/employee/invoice-corrections/{id}/note",
        "/employee/invoice-corrections/{personId}",
        "/employee/invoices/codes",
        "/employee/invoices/create-drafts",
        "/employee/invoices/delete-drafts",
        "/employee/invoices/head-of-family/{id}",
        "/employee/invoices/{id}",
        "/employee/invoices/mark-sent",
        "/employee/invoices/search",
        "/employee/invoices/send",
        "/employee/invoices/send/by-date",
        "/employee/note/application/{applicationId}",
        "/employee/note/{noteId}",
        "/employee/note/service-worker/application/{applicationId}",
        "/employee/occupancy/by-unit/{unitId}/speculated/{applicationId}",
        "/employee/occupancy/units/{unitId}",
        "/employee/other-assistance-measures/{id}",
        "/employee/pairings",
        "/employee/pairings/{id}/response",
        "/employee/parentships",
        "/employee/parentships/{id}",
        "/employee/parentships/{id}/retry",
        "/employee/partnerships",
        "/employee/partnerships/{partnershipId}",
        "/employee/partnerships/{partnershipId}/retry",
        "/employee/patu-report",
        "/employee/payments/create-drafts",
        "/employee/payments/delete-drafts",
        "/employee/payments/search",
        "/employee/payments/send",
        "/employee/pedagogical-document",
        "/employee/pedagogical-document/child/{childId}",
        "/employee/pedagogical-document/{documentId}",
        "/employee/placements",
        "/employee/placements/child-placement-periods/{adultId}",
        "/employee/placements/{placementId}",
        "/employee/placements/{placementId}/group-placements",
        "/employee/placements/plans",
        "/employee/preschool-assistances/{id}",
        "/employee/preschool-terms",
        "/employee/preschool-terms/{id}",
        "/employee/reports",
        "/employee/reports/applications",
        "/employee/reports/assistance-need-decisions",
        "/employee/reports/assistance-need-decisions/unread-count",
        "/employee/reports/assistance-needs-and-actions",
        "/employee/reports/assistance-needs-and-actions/by-child",
        "/employee/reports/attendance-reservation/{unitId}",
        "/employee/reports/attendance-reservation/{unitId}/by-child",
        "/employee/reports/child-age-language",
        "/employee/reports/children-in-different-address",
        "/employee/reports/decisions",
        "/employee/reports/duplicate-people",
        "/employee/reports/ended-placements",
        "/employee/reports/exceeded-service-need/rows",
        "/employee/reports/exceeded-service-need/units",
        "/employee/reports/family-conflicts",
        "/employee/reports/family-contacts",
        "/employee/reports/family-daycare-meal-count",
        "/employee/reports/future-preschoolers",
        "/employee/reports/future-preschoolers/groups",
        "/employee/reports/invoices",
        "/employee/reports/manual-duplication",
        "/employee/reports/meal/{unitId}",
        "/employee/reports/missing-head-of-family",
        "/employee/reports/missing-service-need",
        "/employee/reports/non-ssn-children",
        "/employee/reports/occupancy-by-group",
        "/employee/reports/occupancy-by-unit",
        "/employee/reports/partners-in-different-address",
        "/employee/reports/placement-count",
        "/employee/reports/placement-guarantee",
        "/employee/reports/placement-sketching",
        "/employee/reports/presences",
        "/employee/reports/raw",
        "/employee/reports/service-need",
        "/employee/reports/service-voucher-value/units",
        "/employee/reports/service-voucher-value/units/{unitId}",
        "/employee/reports/sextet",
        "/employee/reports/starting-placements",
        "/employee/reports/units",
        "/employee/reports/varda-child-errors",
        "/employee/reports/varda-unit-errors",
        "/employee/service-needs",
        "/employee/service-needs/{id}",
        "/employee/service-needs/options",
        "/employee/settings",
        "/employee/timeline",
        "/employee/units/{unitId}/calendar-events",
        "/employee/units/{unitId}/groups/{groupId}/discussion-reservation-days",
        "/employee/units/{unitId}/groups/{groupId}/discussion-surveys",
        "/employee/value-decisions/head-of-family/{headOfFamilyId}",
        "/employee/value-decisions/head-of-family/{id}/create-retroactive",
        "/employee/value-decisions/{id}",
        "/employee/value-decisions/ignore",
        "/employee/value-decisions/mark-sent",
        "/employee/value-decisions/pdf/{decisionId}",
        "/employee/value-decisions/search",
        "/employee/value-decisions/send",
        "/employee/value-decisions/set-type/{id}",
        "/employee/value-decisions/unignore",
        "/employee/varda/child/reset/{childId}",
        "/employee/varda/start-reset",
        "/employee/varda/start-update",
        "/employee/vasu/{id}",
        "/employee/vasu/{id}/update-state",
        "/employee/vasu/templates",
        "/employee/vasu/templates/{id}",
        "/employee/vasu/templates/{id}/content",
        "/employee/vasu/templates/{id}/copy",
        "/employee/vasu/templates/{id}/migrate",
        "/employee-mobile/absences/by-child/{childId}/future",
        "/employee-mobile/attendance-reservations/by-child/{childId}/confirmed-range",
        "/employee-mobile/attendance-reservations/confirmed-days/daily",
        "/employee-mobile/attendance-reservations/confirmed-days/stats",
        "/employee-mobile/children/{childId}/sensitive-info",
        "/employee-mobile/occupancy/by-unit/{unitId}",
        "/employee-mobile/occupancy/by-unit/{unitId}/groups",
        "/employee-mobile/push-settings",
        "/employee-mobile/push-subscription",
        "/employee-mobile/realtime-staff-attendances",
        "/employee-mobile/realtime-staff-attendances/arrival",
        "/employee-mobile/realtime-staff-attendances/arrival-external",
        "/employee-mobile/realtime-staff-attendances/departure",
        "/employee-mobile/realtime-staff-attendances/departure-external",
        "/employee-mobile/units/stats",
        "/employee-mobile/units/{unitId}",
    )

val endpointExcludes = plannedEndpoints + deprecatedEndpoints

object Imports {
    val localDate = TsImport.Default(LibCommon / "local-date.ts", "LocalDate")
    val localTime = TsImport.Default(LibCommon / "local-time.ts", "LocalTime")
    val helsinkiDateTime =
        TsImport.Default(LibCommon / "helsinki-date-time.ts", name = "HelsinkiDateTime")
    val finiteDateRange = TsImport.Default(LibCommon / "finite-date-range.ts", "FiniteDateRange")
    val dateRange = TsImport.Default(LibCommon / "date-range.ts", "DateRange")
    val timeInterval = TsImport.Default(LibCommon / "time-interval.ts", "TimeInterval")
    val timeRange = TsImport.Default(LibCommon / "time-range.ts", "TimeRange")
    val vasuQuestion = TsImport.Named(LibCommon / "api-types/vasu.ts", "VasuQuestion")
    val mapVasuQuestion = TsImport.Named(LibCommon / "api-types/vasu.ts", "mapVasuQuestion")
    val messageReceiver = TsImport.Named(LibCommon / "api-types/messaging.ts", "MessageReceiver")
    val uuid = TsImport.Named(LibCommon / "types.d.ts", "UUID")
    val action = TsImport.Named(LibCommon / "generated/action.ts", "Action")
    val jsonOf = TsImport.Named(LibCommon / "json.d.ts", "JsonOf")
    val jsonCompatible = TsImport.Named(LibCommon / "json.d.ts", "JsonCompatible")
    val uri = TsImport.Named(LibCommon / "uri.ts", "uri")
    val createUrlSearchParams = TsImport.Named(LibCommon / "api.ts", "createUrlSearchParams")
    val devApiError = TsImport.Named(E2ETest / "dev-api", "DevApiError")
}

val defaultMetadata =
    TypeMetadata(
        Byte::class to TsPlain(type = "number"),
        Int::class to TsPlain(type = "number"),
        Double::class to TsPlain(type = "number"),
        Long::class to TsPlain(type = "number"),
        BigDecimal::class to TsPlain(type = "number"),
        Boolean::class to TsPlain(type = "boolean"),
        String::class to TsPlain(type = "string"),
        ProductKey::class to TsPlain(type = "string"),
        URI::class to TsPlain(type = "string"),
        Any::class to TsPlain(type = "unknown"),
        LocalDate::class to
            TsExternalTypeRef(
                "LocalDate",
                keyRepresentation = TsCode("string"),
                deserializeJson = { json ->
                    TsCode { "${ref(Imports.localDate)}.parseIso(${inline(json)})" }
                },
                serializePathVariable = { value -> value + ".formatIso()" },
                serializeRequestParam = { value, nullable ->
                    value + if (nullable) "?.formatIso()" else ".formatIso()"
                },
                Imports.localDate
            ),
        LocalTime::class to
            TsExternalTypeRef(
                "LocalTime",
                keyRepresentation = TsCode("string"),
                deserializeJson = { json ->
                    TsCode { "${ref(Imports.localTime)}.parseIso(${inline(json)})" }
                },
                serializePathVariable = { value -> value + ".formatIso()" },
                serializeRequestParam = { value, nullable ->
                    value + if (nullable) "?.formatIso()" else ".formatIso()"
                },
                Imports.localTime
            ),
        HelsinkiDateTime::class to
            TsExternalTypeRef(
                "HelsinkiDateTime",
                keyRepresentation = null,
                deserializeJson = { json ->
                    TsCode { "${ref(Imports.helsinkiDateTime)}.parseIso(${inline(json)})" }
                },
                serializePathVariable = { value -> value + ".formatIso()" },
                serializeRequestParam = { value, nullable ->
                    value + if (nullable) "?.formatIso()" else ".formatIso()"
                },
                Imports.helsinkiDateTime
            ),
        FiniteDateRange::class to
            TsExternalTypeRef(
                "FiniteDateRange",
                keyRepresentation = null,
                deserializeJson = { json ->
                    TsCode { "${ref(Imports.finiteDateRange)}.parseJson(${inline(json)})" }
                },
                serializePathVariable = null,
                serializeRequestParam = null,
                Imports.finiteDateRange
            ),
        DateRange::class to
            TsExternalTypeRef(
                "DateRange",
                keyRepresentation = null,
                deserializeJson = { json ->
                    TsCode { "${ref(Imports.dateRange)}.parseJson(${inline(json)})" }
                },
                serializePathVariable = null,
                serializeRequestParam = null,
                Imports.dateRange
            ),
        DateSet::class to
            TsExternalTypeRef(
                "FiniteDateRange[]",
                keyRepresentation = null,
                deserializeJson = { json ->
                    TsCode {
                        "${inline(json)}.map((x) => ${ref(Imports.finiteDateRange)}.parseJson(x))"
                    }
                },
                serializePathVariable = null,
                serializeRequestParam = null,
                Imports.finiteDateRange
            ),
        TimeInterval::class to
            TsExternalTypeRef(
                "TimeInterval",
                keyRepresentation = null,
                deserializeJson = { json ->
                    TsCode { "${ref(Imports.timeInterval)}.parseJson(${inline(json)})" }
                },
                serializePathVariable = null,
                serializeRequestParam = null,
                Imports.timeInterval
            ),
        TimeRange::class to
            TsExternalTypeRef(
                "TimeRange",
                keyRepresentation = null,
                deserializeJson = { json ->
                    TsCode { "${ref(Imports.timeRange)}.parseJson(${inline(json)})" }
                },
                serializePathVariable = null,
                serializeRequestParam = null,
                Imports.timeRange
            ),
        VasuQuestion::class to
            TsExternalTypeRef(
                "VasuQuestion",
                keyRepresentation = null,
                deserializeJson = { json ->
                    TsCode { "${ref(Imports.mapVasuQuestion)}(${inline(json)})" }
                },
                serializePathVariable = null,
                serializeRequestParam = null,
                Imports.vasuQuestion
            ),
        MessageReceiver::class to
            TsExternalTypeRef(
                "MessageReceiver",
                keyRepresentation = null,
                deserializeJson = null,
                serializePathVariable = null,
                serializeRequestParam = null,
                Imports.messageReceiver
            ),
        UUID::class to
            TsExternalTypeRef(
                "UUID",
                keyRepresentation = TsCode(Imports.uuid),
                deserializeJson = null,
                serializePathVariable = { value -> value },
                serializeRequestParam = { value, _ -> value },
                Imports.uuid
            ),
        Id::class to
            TsExternalTypeRef(
                "UUID",
                keyRepresentation = TsCode(Imports.uuid),
                deserializeJson = null,
                serializePathVariable = { value -> value },
                serializeRequestParam = { value, _ -> value },
                Imports.uuid
            ),
        List::class to TsArray,
        Set::class to TsArray,
        Map::class to TsRecord,
        Pair::class to TsTuple(size = 2),
        Triple::class to TsTuple(size = 3),
        Void::class to Excluded,
        ExternalId::class to TsPlain("string"),
        YearMonth::class to
            TsExternalTypeRef(
                "LocalDate",
                keyRepresentation = null,
                deserializeJson = { error("YearMonth in JSON is not supported") },
                serializePathVariable = { value -> value + ".formatExotic('yyyy-MM')" },
                serializeRequestParam = { value, nullable ->
                    value +
                        if (nullable) "?.formatExotic('yyyy-MM')" else ".formatExotic('yyyy-MM')"
                },
                Imports.localDate
            )
    ) +
        TypeMetadata(
            Action::class.nestedClasses.associateWith { action ->
                TsExternalTypeRef(
                    "Action.${action.simpleName}",
                    keyRepresentation = TsCode("string"),
                    deserializeJson = null,
                    serializePathVariable = { value -> value },
                    serializeRequestParam = { value, _ -> value },
                    Imports.action
                )
            } +
                Action.Citizen::class.nestedClasses.associateWith { action ->
                    TsExternalTypeRef(
                        "Action.Citizen.${action.simpleName}",
                        keyRepresentation = TsCode("string"),
                        deserializeJson = null,
                        serializePathVariable = { value -> value },
                        serializeRequestParam = { value, _ -> value },
                        Imports.action
                    )
                }
        )
