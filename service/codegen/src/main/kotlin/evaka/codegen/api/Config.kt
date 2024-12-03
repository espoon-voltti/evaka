// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import evaka.codegen.api.TsProject.E2ETest
import evaka.codegen.api.TsProject.LibCommon
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.messaging.MessageReceiver
import fi.espoo.evaka.pairing.MobileDeviceDetails
import fi.espoo.evaka.pis.SystemController
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeInterval
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.Action
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
    )

// these endpoint paths are deprecated, and API clients should not call them
private val deprecatedEndpoints: Set<String> = setOf()

// these endpoint paths are planned, and API clients should not call them *yet*
private val plannedEndpoints: Set<String> = setOf()

val endpointExcludes = plannedEndpoints + deprecatedEndpoints

object Imports {
    val id = TsImport.Named(LibCommon / "id-type.ts", "Id")
    val localDate = TsImport.Default(LibCommon / "local-date.ts", "LocalDate")
    val localTime = TsImport.Default(LibCommon / "local-time.ts", "LocalTime")
    val helsinkiDateTime =
        TsImport.Default(LibCommon / "helsinki-date-time.ts", name = "HelsinkiDateTime")
    val finiteDateRange = TsImport.Default(LibCommon / "finite-date-range.ts", "FiniteDateRange")
    val dateRange = TsImport.Default(LibCommon / "date-range.ts", "DateRange")
    val timeInterval = TsImport.Default(LibCommon / "time-interval.ts", "TimeInterval")
    val timeRange = TsImport.Default(LibCommon / "time-range.ts", "TimeRange")
    val yearMonth = TsImport.Default(LibCommon / "year-month.ts", "YearMonth")
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

// For lenient id types, a string type alias is generated: `type FooId = string`.
// For others, a strict type is generated: `type FooId = Id<'Foo'>`.
val lenientIdTypes =
    setOf(
        DatabaseTable.ApplicationNote::class,
        DatabaseTable.ArchivedProcess::class,
        DatabaseTable.Area::class,
        DatabaseTable.AssistanceAction::class,
        DatabaseTable.AssistanceActionOption::class,
        DatabaseTable.AssistanceFactor::class,
        DatabaseTable.AssistanceNeedDecision::class,
        DatabaseTable.AssistanceNeedDecisionGuardian::class,
        DatabaseTable.AssistanceNeedPreschoolDecision::class,
        DatabaseTable.AssistanceNeedPreschoolDecisionGuardian::class,
        DatabaseTable.AssistanceNeedVoucherCoefficient::class,
        DatabaseTable.Attachment::class,
        DatabaseTable.AttendanceReservation::class,
        DatabaseTable.BackupCare::class,
        DatabaseTable.BackupPickup::class,
        DatabaseTable.CalendarEvent::class,
        DatabaseTable.CalendarEventAttendee::class,
        DatabaseTable.CalendarEventTime::class,
        DatabaseTable.ChildAttendance::class,
        DatabaseTable.ChildDailyNote::class,
        DatabaseTable.ChildDocument::class,
        DatabaseTable.ChildImage::class,
        DatabaseTable.ChildStickyNote::class,
        DatabaseTable.ClubTerm::class,
        DatabaseTable.GroupNote::class,
        DatabaseTable.DailyServicesTime::class,
        DatabaseTable.DailyServicesTimeNotification::class,
        DatabaseTable.Daycare::class,
        DatabaseTable.DaycareAssistance::class,
        DatabaseTable.DaycareCaretaker::class,
        DatabaseTable.Decision::class,
        DatabaseTable.DocumentTemplate::class,
        DatabaseTable.Employee::class,
        DatabaseTable.EmployeePin::class,
        DatabaseTable.EvakaUser::class,
        DatabaseTable.FamilyContact::class,
        DatabaseTable.FeeAlteration::class,
        DatabaseTable.FeeDecision::class,
        DatabaseTable.FeeThresholds::class,
        DatabaseTable.FosterParent::class,
        DatabaseTable.Group::class,
        DatabaseTable.GroupPlacement::class,
        DatabaseTable.HolidayPeriod::class,
        DatabaseTable.HolidayQuestionnaire::class,
        DatabaseTable.Income::class,
        DatabaseTable.IncomeNotification::class,
        DatabaseTable.IncomeStatement::class,
        DatabaseTable.Invoice::class,
        DatabaseTable.InvoiceCorrection::class,
        DatabaseTable.InvoiceRow::class,
        DatabaseTable.KoskiStudyRight::class,
        DatabaseTable.Message::class,
        DatabaseTable.MessageAccount::class,
        DatabaseTable.MessageContent::class,
        DatabaseTable.MessageDraft::class,
        DatabaseTable.MessageRecipients::class,
        DatabaseTable.MessageThread::class,
        DatabaseTable.MessageThreadFolder::class,
        DatabaseTable.MobileDevice::class,
        DatabaseTable.OtherAssistanceMeasure::class,
        DatabaseTable.Pairing::class,
        DatabaseTable.Parentship::class,
        DatabaseTable.Partnership::class,
        DatabaseTable.Payment::class,
        DatabaseTable.PedagogicalDocument::class,
        DatabaseTable.Person::class,
        DatabaseTable.Placement::class,
        DatabaseTable.PlacementPlan::class,
        DatabaseTable.PreschoolAssistance::class,
        DatabaseTable.PreschoolTerm::class,
        DatabaseTable.ServiceApplication::class,
        DatabaseTable.ServiceNeed::class,
        DatabaseTable.ServiceNeedOptionVoucherValue::class,
        DatabaseTable.StaffAttendance::class,
        DatabaseTable.StaffAttendanceRealtime::class,
        DatabaseTable.StaffAttendanceExternal::class,
        DatabaseTable.StaffAttendancePlan::class,
        DatabaseTable.StaffOccupancyCoefficient::class,
        DatabaseTable.VoucherValueDecision::class,
    )

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
        Id::class to GenericWrapper(default = String::class),
        List::class to TsArray,
        Set::class to TsArray,
        Map::class to TsRecord,
        Pair::class to TsTuple(size = 2),
        Triple::class to TsTuple(size = 3),
        Sensitive::class to GenericWrapper(),
        Void::class to Excluded,
        ExternalId::class to TsPlain("string"),
        YearMonth::class to
            TsExternalTypeRef(
                "YearMonth",
                keyRepresentation = null,
                deserializeJson = { json ->
                    TsCode { "${ref(Imports.yearMonth)}.parseIso(${inline(json)})" }
                },
                serializePathVariable = { value -> value + ".formatIso()" },
                serializeRequestParam = { value, nullable ->
                    value + if (nullable) "?.formatIso(')" else ".formatIso()"
                },
                Imports.yearMonth,
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
