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
        typeOf<SystemController.PinLoginResponse>(),
        typeOf<MobileDeviceDetails>(),
        typeOf<CurriculumTemplateError>(),
    )

// these endpoint paths are deprecated, and API clients should not call them
private val deprecatedEndpoints: Set<String> =
    setOf(
        "/absences/by-child/{childId}",
        "/absences/by-child/{childId}/delete",
        "/absences/by-child/{childId}/future",
        "/absences/{groupId}",
        "/absences/{groupId}/delete-holiday-reservations",
        "/absences/{groupId}/present",
        "/areas",
        "/assistance-action-options",
        "/assistance-actions/{id}",
        "/assistance-factors/{id}",
        "/assistance-need-decision/{id}",
        "/assistance-need-decision/{id}/annul",
        "/assistance-need-decision/{id}/decide",
        "/assistance-need-decision/{id}/decision-maker-option",
        "/assistance-need-decision/{id}/mark-as-opened",
        "/assistance-need-decision/{id}/revert-to-unsent",
        "/assistance-need-decision/{id}/send",
        "/assistance-need-decision/{id}/update-decision-maker",
        "/assistance-need-preschool-decisions/{id}",
        "/assistance-need-preschool-decisions/{id}/annul",
        "/assistance-need-preschool-decisions/{id}/decide",
        "/assistance-need-preschool-decisions/{id}/decision-maker",
        "/assistance-need-preschool-decisions/{id}/decision-maker-options",
        "/assistance-need-preschool-decisions/{id}/mark-as-opened",
        "/assistance-need-preschool-decisions/{id}/send",
        "/assistance-need-preschool-decisions/{id}/unsend",
        "/assistance-need-voucher-coefficients/{id}",
        "/attachments/applications/{applicationId}",
        "/attachments/{attachmentId}",
        "/attachments/citizen/applications/{applicationId}",
        "/attachments/citizen/{attachmentId}",
        "/attachments/income-statements/{incomeStatementId}",
        "/attachments/messages/{draftId}",
        "/attachments/pedagogical-documents/{documentId}",
        "/attendance-reservations",
        "/attendance-reservations/by-child/{childId}/confirmed-range",
        "/attendance-reservations/child-date",
        "/attendance-reservations/child-date/expected-absences",
        "/attendance-reservations/confirmed-days/daily",
        "/attendance-reservations/confirmed-days/stats",
        "/attendances/units/{unitId}/attendances",
        "/attendances/units/{unitId}/children",
        "/attendances/units/{unitId}/children/{childId}/absence-range",
        "/attendances/units/{unitId}/children/{childId}/arrival",
        "/attendances/units/{unitId}/children/{childId}/departure",
        "/attendances/units/{unitId}/children/{childId}/departure/expected-absences",
        "/attendances/units/{unitId}/children/{childId}/full-day-absence",
        "/attendances/units/{unitId}/children/{childId}/return-to-coming",
        "/attendances/units/{unitId}/children/{childId}/return-to-present",
        "/backup-cares/{id}",
        "/backup-pickups/{id}",
        "/calendar-event",
        "/calendar-event/{id}",
        "/calendar-event/{id}/time",
        "/calendar-event/reservation",
        "/calendar-event-time/{id}",
        "/child-documents",
        "/child-documents/{documentId}",
        "/child-documents/{documentId}/content",
        "/child-documents/{documentId}/lock",
        "/child-documents/{documentId}/next-status",
        "/child-documents/{documentId}/pdf",
        "/child-documents/{documentId}/prev-status",
        "/child-documents/{documentId}/publish",
        "/child-images/{imageId}",
        "/children/{child}/assistance",
        "/children/{child}/assistance-factors",
        "/children/{child}/daycare-assistances",
        "/children/{childId}",
        "/children/{childId}/additional-information",
        "/children/{childId}/assistance-actions",
        "/children/{childId}/assistance-need-preschool-decisions",
        "/children/{childId}/assistance-needs/decision",
        "/children/{childId}/assistance-needs/decisions",
        "/children/{childId}/assistance-need-voucher-coefficients",
        "/children/{childId}/backup-cares",
        "/children/{childId}/backup-pickups",
        "/children/{childId}/daily-service-times",
        "/children/{childId}/image",
        "/children/{childId}/sensitive-info",
        "/children/{childId}/vasu",
        "/children/{childId}/vasu-summaries",
        "/children/{child}/other-assistance-measures",
        "/children/{child}/preschool-assistances",
        "/club-terms",
        "/club-terms/{id}",
        "/daily-service-times/{id}",
        "/daily-service-times/{id}/end",
        "/daycare-assistances/{id}",
        "/daycare-groups/{groupId}/group-notes",
        "/daycare-groups/{groupId}/notes",
        "/daycares",
        "/daycares/{daycareId}",
        "/daycares/{daycareId}/acl",
        "/daycares/{daycareId}/backup-cares",
        "/daycares/{daycareId}/earlychildhoodeducationsecretary/{employeeId}",
        "/daycares/{daycareId}/full-acl/{employeeId}",
        "/daycares/{daycareId}/groups",
        "/daycares/{daycareId}/groups/{groupId}",
        "/daycares/{daycareId}/groups/{groupId}/caretakers",
        "/daycares/{daycareId}/groups/{groupId}/caretakers/{id}",
        "/daycares/{daycareId}/notifications",
        "/daycares/{daycareId}/specialeducationteacher/{employeeId}",
        "/daycares/{daycareId}/staff/{employeeId}",
        "/daycares/{daycareId}/staff/{employeeId}/groups",
        "/daycares/{daycareId}/supervisors/{employeeId}",
        "/daycares/features",
        "/daycares/unit-features",
        "/daycares/{unitId}/group-details",
        "/daycares/{unitId}/temporary",
        "/daycares/{unitId}/temporary/{employeeId}",
        "/daycares/{unitId}/temporary/{employeeId}/acl",
        "/decisions2/by-application",
        "/decisions2/by-child",
        "/decisions2/by-guardian",
        "/decisions2/{id}/download",
        "/decisions2/units",
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
        "/diets",
        "/document-templates",
        "/document-templates/active",
        "/document-templates/import",
        "/document-templates/{templateId}",
        "/document-templates/{templateId}/content",
        "/document-templates/{templateId}/duplicate",
        "/document-templates/{templateId}/export",
        "/document-templates/{templateId}/force-unpublish",
        "/document-templates/{templateId}/publish",
        "/document-templates/{templateId}/validity",
        "/employee",
        "/employee/finance-decision-handler",
        "/employee/{id}",
        "/employee/{id}/global-roles",
        "/employee/{id}/daycare-roles",
        "/employee/{id}/activate",
        "/employee/{id}/deactivate",
        "/employee/{id}/details",
        "/employee/pin-code",
        "/employee/search",
        "/employee/preferred-first-name",
        "/employee/pin-code/is-pin-locked",
        "/messages/{accountId}/received",
        "/messages/{accountId}/thread/{threadId}",
        "/messages/{accountId}/preflight-check",
        "/messages/{accountId}",
        "/messages/{accountId}/drafts",
        "/messages/{accountId}/drafts/{draftId}",
        "/messages/{accountId}/{messageId}/reply",
        "/messages/{accountId}/threads/{threadId}/read",
        "/messages/receivers",
        "/children/{childId}/child-daily-notes",
        "/child-daily-notes/{noteId}",
        "/child-sticky-notes/{noteId}",
        "/group-notes/{noteId}",
        "/daycare-groups/{groupId}/group-notes",
        "/children/{childId}/child-sticky-notes",
        "/messages/{accountId}/sent",
        "/family/by-adult/{id}",
        "/family/contacts",
        "/family/contacts/priority",
        "/fee-alterations",
        "/fee-alterations/{feeAlterationId}",
        "/fee-decision-generator/generate",
        "/fee-decisions/confirm",
        "/fee-decisions/head-of-family/{id}",
        "/fee-decisions/head-of-family/{id}/create-retroactive",
        "/fee-decisions/{id}",
        "/fee-decisions/ignore",
        "/fee-decisions/mark-sent",
        "/fee-decisions/pdf/{decisionId}",
        "/fee-decisions/search",
        "/fee-decisions/set-type/{id}",
        "/fee-decisions/unignore",
        "/filters/units",
        "/finance-basics/fee-thresholds",
        "/finance-basics/fee-thresholds/{id}",
        "/finance-basics/voucher-values",
        "/finance-basics/voucher-values/{id}",
        "/finance-decisions/selectable-handlers",
        "/foster-parent",
        "/foster-parent/by-child/{childId}",
        "/foster-parent/by-parent/{parentId}",
        "/foster-parent/{id}",
        "/group-placements/{groupPlacementId}",
        "/group-placements/{groupPlacementId}/transfer",
        "/holiday-period",
        "/holiday-period/{id}",
        "/holiday-period/questionnaire",
        "/holiday-period/questionnaire/{id}",
        "/incomes",
        "/incomes/{incomeId}",
        "/incomes/multipliers",
        "/incomes/notifications",
        "/income-statements/awaiting-handler",
        "/income-statements/guardian/{guardianId}/children",
        "/income-statements/{incomeStatementId}/handled",
        "/income-statements/person/{personId}",
        "/income-statements/person/{personId}/{incomeStatementId}",
        "/incomes/types",
        "/invoice-corrections",
        "/invoice-corrections/{id}",
        "/invoice-corrections/{id}/note",
        "/invoice-corrections/{personId}",
        "/invoices/codes",
        "/invoices/create-drafts",
        "/invoices/delete-drafts",
        "/invoices/head-of-family/{id}",
        "/invoices/{id}",
        "/invoices/mark-sent",
        "/invoices/search",
        "/invoices/send",
        "/invoices/send/by-date",
        "/messages/{accountId}/archived",
        "/messages/{accountId}/copies",
        "/messages/{accountId}/threads/{threadId}/archive",
        "/messages/application/{applicationId}",
        "/messages/mobile/my-accounts/{unitId}",
        "/messages/my-accounts",
        "/messages/unread",
        "/messages/unread/{unitId}",
        "/mobile-devices",
        "/mobile-devices/{id}",
        "/mobile-devices/{id}/name",
        "/mobile-devices/personal",
        "/mobile-devices/pin-login",
        "/mobile-devices/push-settings",
        "/mobile-devices/push-subscription",
        "/mobile/realtime-staff-attendances",
        "/mobile/realtime-staff-attendances/arrival",
        "/mobile/realtime-staff-attendances/arrival-external",
        "/mobile/realtime-staff-attendances/departure",
        "/mobile/realtime-staff-attendances/departure-external",
        "/mobile/units/stats",
        "/mobile/units/{unitId}",
        "/note/application/{applicationId}",
        "/note/{noteId}",
        "/note/service-worker/application/{applicationId}",
        "/occupancy/by-unit/{unitId}",
        "/occupancy/by-unit/{unitId}/groups",
        "/occupancy/by-unit/{unitId}/speculated/{applicationId}",
        "/occupancy/units/{unitId}",
        "/other-assistance-measures/{id}",
        "/pairings",
        "/pairings/{id}/response",
        "/parentships",
        "/parentships/{id}",
        "/parentships/{id}/retry",
        "/partnerships",
        "/partnerships/{partnershipId}",
        "/partnerships/{partnershipId}/retry",
        "/patu-report",
        "/payments/create-drafts",
        "/payments/delete-drafts",
        "/payments/search",
        "/payments/send",
        "/pedagogical-document",
        "/pedagogical-document/child/{childId}",
        "/pedagogical-document/{documentId}",
        "/person",
        "/person/blocked-guardians/{personId}",
        "/person/{childId}/evaka-rights",
        "/person/create",
        "/person/dependants/{personId}",
        "/person/details/{personId}",
        "/person/details/ssn",
        "/person/{guardianId}/address-page/download",
        "/person/guardians/{personId}",
        "/person/identity/{personId}",
        "/person/merge",
        "/person/{personId}",
        "/person/{personId}/duplicate",
        "/person/{personId}/ssn",
        "/person/{personId}/ssn/disable",
        "/person/{personId}/vtj-update",
        "/person/search",
        "/placements",
        "/placements/child-placement-periods/{adultId}",
        "/placements/{placementId}",
        "/placements/{placementId}/group-placements",
        "/placements/plans",
        "/preschool-assistances/{id}",
        "/preschool-terms",
        "/preschool-terms/{id}",
        "/public/club-terms",
        "/public/pairings/challenge",
        "/public/pairings/{id}/status",
        "/public/preschool-terms",
        "/public/service-needs/options",
        "/public/units",
        "/public/units/{applicationType}",
        "/reports",
        "/reports/applications",
        "/reports/assistance-need-decisions",
        "/reports/assistance-need-decisions/unread-count",
        "/reports/assistance-needs-and-actions",
        "/reports/assistance-needs-and-actions/by-child",
        "/reports/attendance-reservation/{unitId}",
        "/reports/attendance-reservation/{unitId}/by-child",
        "/reports/child-age-language",
        "/reports/children-in-different-address",
        "/reports/decisions",
        "/reports/duplicate-people",
        "/reports/ended-placements",
        "/reports/exceeded-service-need/rows",
        "/reports/exceeded-service-need/units",
        "/reports/family-conflicts",
        "/reports/family-contacts",
        "/reports/family-daycare-meal-count",
        "/reports/future-preschoolers",
        "/reports/invoices",
        "/reports/manual-duplication",
        "/reports/meal/{unitId}",
        "/reports/missing-head-of-family",
        "/reports/missing-service-need",
        "/reports/non-ssn-children",
        "/reports/occupancy-by-group",
        "/reports/occupancy-by-unit",
        "/reports/partners-in-different-address",
        "/reports/placement-count",
        "/reports/placement-guarantee",
        "/reports/placement-sketching",
        "/reports/presences",
        "/reports/raw",
        "/reports/service-need",
        "/reports/service-voucher-value/units",
        "/reports/service-voucher-value/units/{unitId}",
        "/reports/sextet",
        "/reports/starting-placements",
        "/reports/units",
        "/reports/varda-child-errors",
        "/reports/varda-unit-errors",
        "/service-needs",
        "/service-needs/{id}",
        "/service-needs/options",
        "/settings",
        "/staff-attendances/group/{groupId}",
        "/staff-attendances/realtime",
        "/staff-attendances/realtime/{unitId}/{attendanceId}",
        "/staff-attendances/realtime/{unitId}/external/{attendanceId}",
        "/staff-attendances/realtime/upsert",
        "/staff-attendances/realtime/upsert-external",
        "/staff-attendances/unit/{unitId}",
        "/timeline",
        "/units",
        "/units/{unitId}/calendar-events",
        "/units/{unitId}/groups/{groupId}/discussion-reservation-days",
        "/units/{unitId}/groups/{groupId}/discussion-surveys",
        "/v2/applications",
        "/v2/applications/{applicationId}",
        "/v2/applications/{applicationId}/actions/accept-decision",
        "/v2/applications/{applicationId}/actions/{action}",
        "/v2/applications/{applicationId}/actions/create-placement-plan",
        "/v2/applications/{applicationId}/actions/reject-decision",
        "/v2/applications/{applicationId}/actions/respond-to-placement-proposal",
        "/v2/applications/{applicationId}/actions/send-application",
        "/v2/applications/{applicationId}/decision-drafts",
        "/v2/applications/{applicationId}/placement-draft",
        "/v2/applications/batch/actions/{action}",
        "/v2/applications/by-child/{childId}",
        "/v2/applications/by-guardian/{guardianId}",
        "/v2/applications/placement-proposals/{unitId}/accept",
        "/v2/applications/search",
        "/v2/applications/units/{unitId}",
        "/value-decisions/head-of-family/{headOfFamilyId}",
        "/value-decisions/head-of-family/{id}/create-retroactive",
        "/value-decisions/{id}",
        "/value-decisions/ignore",
        "/value-decisions/mark-sent",
        "/value-decisions/pdf/{decisionId}",
        "/value-decisions/search",
        "/value-decisions/send",
        "/value-decisions/set-type/{id}",
        "/value-decisions/unignore",
        "/varda/child/reset/{childId}",
        "/varda/start-reset",
        "/varda/start-update",
        "/vasu/{id}",
        "/vasu/{id}/update-state",
        "/vasu/templates",
        "/vasu/templates/{id}",
        "/vasu/templates/{id}/content",
        "/vasu/templates/{id}/copy",
        "/vasu/templates/{id}/migrate",
    )

// these endpoint paths are planned, and API clients should not call them *yet*
private val plannedEndpoints: Set<String> = setOf()

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
                Imports.localDate,
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
                Imports.localTime,
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
                Imports.helsinkiDateTime,
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
                Imports.finiteDateRange,
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
                Imports.dateRange,
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
                Imports.finiteDateRange,
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
                Imports.timeInterval,
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
                Imports.timeRange,
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
                Imports.vasuQuestion,
            ),
        MessageReceiver::class to
            TsExternalTypeRef(
                "MessageReceiver",
                keyRepresentation = null,
                deserializeJson = null,
                serializePathVariable = null,
                serializeRequestParam = null,
                Imports.messageReceiver,
            ),
        UUID::class to
            TsExternalTypeRef(
                "UUID",
                keyRepresentation = TsCode(Imports.uuid),
                deserializeJson = null,
                serializePathVariable = { value -> value },
                serializeRequestParam = { value, _ -> value },
                Imports.uuid,
            ),
        Id::class to
            TsExternalTypeRef(
                "UUID",
                keyRepresentation = TsCode(Imports.uuid),
                deserializeJson = null,
                serializePathVariable = { value -> value },
                serializeRequestParam = { value, _ -> value },
                Imports.uuid,
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
                Imports.localDate,
            ),
    ) +
        TypeMetadata(
            Action::class.nestedClasses.associateWith { action ->
                TsExternalTypeRef(
                    "Action.${action.simpleName}",
                    keyRepresentation = TsCode("string"),
                    deserializeJson = null,
                    serializePathVariable = { value -> value },
                    serializeRequestParam = { value, _ -> value },
                    Imports.action,
                )
            } +
                Action.Citizen::class.nestedClasses.associateWith { action ->
                    TsExternalTypeRef(
                        "Action.Citizen.${action.simpleName}",
                        keyRepresentation = TsCode("string"),
                        deserializeJson = null,
                        serializePathVariable = { value -> value },
                        serializeRequestParam = { value, _ -> value },
                        Imports.action,
                    )
                }
        )
