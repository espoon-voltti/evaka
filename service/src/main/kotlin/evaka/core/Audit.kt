// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

import evaka.core.shared.Id
import fi.espoo.voltti.logging.loggers.audit
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.reflect.KProperty1

sealed interface AuditId {
    val value: Any

    @JvmInline value class One(override val value: Any) : AuditId

    @JvmInline value class Many(override val value: List<Any>) : AuditId

    companion object {
        operator fun invoke(value: Id<*>): AuditId = One(value)

        operator fun invoke(value: UUID): AuditId = One(value)

        operator fun invoke(value: String): AuditId = One(value)

        operator fun invoke(value: Collection<Id<*>>): AuditId = Many(value.toList())
    }

    operator fun plus(other: AuditId): AuditId {
        return when (this) {
            is One -> {
                when (other) {
                    is One -> Many(listOf(value, other.value))
                    is Many -> Many(listOf(value) + other.value)
                }
            }

            is Many -> {
                when (other) {
                    is One -> Many(value + other.value)
                    is Many -> Many(value + other.value)
                }
            }
        }
    }
}

enum class Audit(
    private val securityEvent: Boolean = false,
    private val securityLevel: String = "low",
) {
    // AssistanceBasisOptionsRead,
    AssistanceNeedsReportRead,
    AssistanceNeedsReportByChildRead,
    AttachmentsDelete,
    AttachmentsRead,
    AttachmentsUploadForApplication,
    AttachmentsUploadForFeeAlteration,
    AttachmentsUploadForIncome,
    AttachmentsUploadForIncomeStatement,
    AttachmentsUploadForInvoice,
    AttachmentsUploadForMessage,
    AttachmentsUploadForMessageDraft,
    AttachmentsUploadForPedagogicalDocument,
    AttendanceReservationCitizenRead,
    AttendanceReservationDelete,
    AttendanceReservationReportRead,
    BackupCareDelete,
    BackupCareUpdate,
    CalendarEventCreate,
    CalendarEventDelete,
    CalendarEventRead,
    CalendarEventUpdate,
    CalendarEventTimeCreate,
    CalendarEventTimeDelete,
    // CalendarEventTimeRead,
    CalendarEventChildTimesCancellation,
    CalendarEventTimeReservationCreate,
    CalendarEventTimeReservationDelete,
    CalendarEventTimeReservationUpdate,
    ChildAbsenceReport,
    ChildAdditionalInformationRead,
    ChildAdditionalInformationUpdate,
    ChildAgeLanguageReportRead,
    ChildAssistanceNeedVoucherCoefficientCreate,
    ChildAssistanceNeedVoucherCoefficientRead,
    ChildAssistanceNeedVoucherCoefficientUpdate,
    ChildAssistanceNeedVoucherCoefficientDelete,
    ChildAttendanceChildrenRead,
    ChildAttendanceOngoingRead,
    ChildAttendanceReportRead,
    ChildAttendanceStatusesRead,
    // ChildAttendancesUpsert,
    ChildAttendancesArrivalCreate,
    ChildAttendancesDepartureRead,
    ChildAttendancesDepartureCreate,
    ChildAttendancesFullDayAbsenceCreate,
    ChildAttendancesFullDayAbsenceDelete,
    ChildAttendancesAbsenceRangeCreate,
    ChildAttendancesReturnToComing,
    ChildAttendancesReturnToPresent,
    ChildBackupCareCreate,
    ChildBackupCareRead,
    ChildBackupPickupCreate,
    ChildBackupPickupDelete,
    ChildBackupPickupRead,
    ChildBackupPickupUpdate,
    ChildDailyNoteCreate,
    ChildDailyNoteUpdate,
    ChildDailyNoteDelete,
    ChildDailyServiceTimesDelete,
    ChildDailyServiceTimesEdit,
    ChildDailyServiceTimesRead,
    ChildDailyServiceTimeNotificationsRead,
    ChildDailyServiceTimeNotificationsDismiss,
    ChildDatePresenceExpectedAbsencesCheck,
    ChildDocumentProposeDecision,
    ChildDocumentAcceptDecision,
    ChildDocumentRejectDecision,
    ChildDocumentAnnulDecision,
    ChildDocumentUpdateDecisionValidity,
    ChildDocumentCreate,
    ChildDocumentDelete,
    ChildDocumentArchive,
    ChildDocumentReadAcceptedDecisions,
    ChildDocumentDownload,
    ChildDocumentMarkRead,
    ChildDocumentNextStatus,
    ChildDocumentPrevStatus,
    ChildDocumentPublish,
    ChildDocumentRead,
    ChildDocumentReadDecisionMakers,
    ChildDocumentReadMetadata,
    ChildDocumentTryTakeLockOnContent,
    ChildDocumentUnreadCount,
    ChildDocumentUpdate,
    ChildDocumentUpdateContent,
    ChildDocumentsCreate,
    ChildFeeAlterationsCreate,
    ChildFeeAlterationsDelete,
    ChildFeeAlterationsRead,
    ChildFeeAlterationsUpdate,
    ChildrenInDifferentAddressReportRead,
    ChildImageDelete,
    ChildImageDownload,
    ChildImageUpload,
    ChildConfirmedRangeReservationsRead,
    ChildConfirmedRangeReservationsUpdate,
    ChildReservationStatusRead,
    ChildBasicInfoRead,
    ChildDocumentDecisionsReportRead,
    ChildDocumentDecisionsReportNotificationsRead,
    ChildDocumentsReportRead,
    ChildDocumentsReportTemplatesRead,
    ChildSensitiveInfoRead,
    ChildServiceNeedsRead,
    ChildStickyNoteCreate,
    ChildStickyNoteUpdate,
    ChildStickyNoteDelete,
    CitizenChildrenRead,
    CitizenChildServiceApplicationsCreate,
    CitizenChildServiceApplicationsRead,
    CitizenChildServiceApplicationsDelete,
    CitizenChildServiceNeedOptionsRead,
    CitizenChildServiceNeedRead,
    CitizenChildAttendanceSummaryRead,
    CitizenChildDailyServiceTimeRead,
    CitizenDocumentResponseReportRead,
    CitizenDocumentResponseReportGroupOptionsRead,
    CitizenEmailVerificationStatusRead,
    CitizenFamilyRead,
    CitizenFeeDecisionDownloadPdf,
    CitizenNotificationSettingsRead,
    // CitizenNotificationSettingsUpdate,
    CitizenLogin(securityEvent = true, securityLevel = "low"),
    CitizenCredentialsDelete(securityEvent = true, securityLevel = "medium"),
    CitizenCredentialsDeleteAttempt(securityEvent = true, securityLevel = "medium"),
    CitizenCredentialsUpdate(securityEvent = true, securityLevel = "medium"),
    CitizenCredentialsUpdateAttempt(securityEvent = true, securityLevel = "medium"),
    CitizenPasskeyDelete(securityEvent = true, securityLevel = "medium"),
    CitizenPasskeyLogin(securityEvent = true, securityLevel = "low"),
    CitizenPasskeyLoginAttempt(securityEvent = true, securityLevel = "low"),
    CitizenPasskeyRegister(securityEvent = true, securityLevel = "medium"),
    CitizenPasskeyRegisterAttempt(securityEvent = true, securityLevel = "medium"),
    CitizenPasskeysRead,
    CitizenPasskeyUpdate,
    CitizenUserDetailsRead,
    CitizenWeakLogin(securityEvent = true, securityLevel = "low"),
    CitizenWeakLoginAttempt(securityEvent = true, securityLevel = "low"),
    CitizenSendVerificationCode(securityEvent = true, securityLevel = "medium"),
    CitizenVerifyEmail(securityEvent = true, securityLevel = "medium"),
    CitizenVerifyEmailAttempt(securityEvent = true, securityLevel = "medium"),
    CitizenVoucherValueDecisionDownloadPdf,
    ClubTermCreate,
    ClubTermUpdate,
    ClubTermDelete,
    // ClubTermRead,
    CustomerFeesReportRead,
    DataRemovalExpiredDelete,
    DataRemovalExpiredUnset,
    DaycareGroupPlacementCreate,
    DaycareGroupPlacementDelete,
    DaycareGroupPlacementTransfer,
    // DaycareBackupCareRead,
    // DecisionReadByApplication,
    DecisionsReportRead,
    DuplicatePeopleReportRead,
    DocumentTemplateCopy,
    DocumentTemplateCreate,
    DocumentTemplateDelete,
    DocumentTemplateForceUnpublish,
    DocumentTemplatePublish,
    DocumentTemplateRead,
    DocumentTemplateUpdateBasics,
    DocumentTemplateUpdateContent,
    DocumentTemplateUpdateValidity,
    EmployeeActivate(securityEvent = true, securityLevel = "high"),
    EmployeeCreate(securityEvent = true, securityLevel = "high"),
    EmployeeDeactivate(securityEvent = true, securityLevel = "high"),
    EmployeeDelete(securityEvent = true, securityLevel = "high"),
    EmployeeDeleteDaycareRoles(securityEvent = true, securityLevel = "medium"),
    EmployeeDeleteScheduledDaycareRole(securityEvent = true, securityLevel = "medium"),
    EmployeeEmailUpdate,
    EmployeeLogin(securityEvent = true, securityLevel = "low"),
    EmployeeSfiLoginAttempt(securityEvent = true, securityLevel = "low"),
    EmployeeSfiLogin(securityEvent = true, securityLevel = "low"),
    EmployeeRead,
    EmployeeUpdateDaycareRoles(securityEvent = true, securityLevel = "medium"),
    EmployeeUpdateGlobalRoles(securityEvent = true, securityLevel = "high"),
    EmployeePreferredFirstNameRead,
    EmployeePreferredFirstNameUpdate,
    EmployeeUserDetailsRead,
    EmployeesRead,
    EndedPlacementsReportRead,
    FamilyConflictReportRead,
    FamilyContactReportRead,
    FamilyContactsRead,
    FamilyContactsUpdate,
    FamilyDaycareMealReport,
    FeeDecisionConfirm,
    FeeDecisionHeadOfFamilyRead,
    FeeDecisionHeadOfFamilyCreateRetroactive,
    FeeDecisionIgnore,
    // FeeDecisionLiableCitizenRead,
    FeeDecisionMarkSent,
    FeeDecisionPdfRead,
    FeeDecisionRead,
    FeeDecisionReadMetadata,
    FeeDecisionSearch,
    FeeDecisionSetType,
    FeeDecisionUnignore,
    FinanceBasicsFeeThresholdsRead,
    FinanceBasicsFeeThresholdsCreate,
    FinanceBasicsFeeThresholdsUpdate,
    FinanceBasicsVoucherValueCreate,
    FinanceBasicsVoucherValueUpdate,
    FinanceBasicsVoucherValueDelete,
    FinanceBasicsVoucherValuesRead,
    FinanceDecisionHandlersRead,
    FinanceDecisionCitizenRead,
    FinanceNoteCreate,
    FinanceNoteUpdate,
    FinanceNoteDelete,
    FinanceNoteRead,
    FosterParentCreateRelationship,
    FosterParentDeleteRelationship,
    FosterParentReadChildren,
    FosterParentReadParents,
    FosterParentUpdateRelationship,
    FuturePreschoolers,
    GuardianChildrenRead,
    GroupCalendarEventsRead,
    GroupDiscussionReservationCalendarDaysRead,
    GroupNoteCreate,
    GroupNoteUpdate,
    GroupNoteDelete,
    GroupNoteRead,
    HolidayPeriodCreate,
    HolidayPeriodRead,
    HolidayPeriodDelete,
    HolidayPeriodsList,
    HolidayPeriodUpdate,
    HolidayQuestionnairesList,
    HolidayQuestionnaireRead,
    HolidayQuestionnaireCreate,
    HolidayQuestionnaireUpdate,
    HolidayQuestionnaireDelete,
    HolidayPeriodAttendanceReport,
    HolidayQuestionnaireReport,
    HolidayAbsenceCreate,
    IncomeExpirationDatesRead,
    IncomeStatementCreate,
    IncomeStatementCreateForChild,
    IncomeStatementDelete,
    IncomeStatementRead,
    IncomeStatementUpdate,
    IncomeStatementUpdateHandled,
    IncomeStatementsAwaitingHandler,
    IncomeStatementsOfPerson,
    IncomeStatementStatusOfPartner,
    IncomeStatementsOfChild,
    IncomeStatementStartDates,
    IncomeStatementStartDatesOfChild,
    IncompleteIncomeReportRead,
    InvoiceCorrectionsCreate,
    InvoiceCorrectionsDelete,
    InvoiceCorrectionsNoteUpdate,
    InvoiceCorrectionsRead,
    InvoicesCreate,
    InvoicesCreateReplacementDrafts,
    InvoicesDeleteDrafts,
    InvoicesMarkSent,
    InvoicesMarkReplacementDraftSent,
    InvoicesRead,
    InvoicesReportRead,
    InvoicesResend,
    InvoicesResendByDate,
    InvoicesSearch,
    InvoicesSend,
    InvoicesSendByDate,
    // InvoicesUpdate,
    KoskiReportRead,
    MealReportRead,
    MessagingMyAccountsRead,
    MessagingUnreadMessagesRead,
    MessagingMarkMessagesReadWrite,
    MessagingMarkMessagesUnreadWrite,
    MessagingArchiveMessageWrite,
    MessagingChangeFolder,
    MessagingMessageReceiversRead,
    MessagingReceivedMessagesRead,
    MessagingReceivedMessageCopiesRead,
    MessagingMessagesInFolderRead,
    MessagingMessageFoldersRead,
    MessagingSentMessagesRead,
    MessagingNewMessagePreflightCheck,
    MessagingNewMessageWrite,
    MessagingDraftsRead,
    MessagingCreateDraft,
    MessagingUpdateDraft,
    MessagingDeleteDraft,
    MessagingReplyToMessageWrite,
    MessagingReplyToThreadWrite,
    MessagingCitizenFetchReceiversForAccount,
    MessagingCitizenSendMessage,
    MessagingMessageThreadRead,
    MessagingDeleteContent,
    MessagingViewDeletedContent,
    MessagingDeletionEmailSent,
    MissingHeadOfFamilyReportRead,
    MissingServiceNeedReportRead,
    MobileDevicesList,
    MobileDevicesRead,
    MobileDevicesRename,
    MobileDevicesDelete(securityEvent = true, securityLevel = "medium"),
    NekkuUnitsRead,
    NekkuMealTypesRead,
    NekkuSpecialDietsRead,
    NekkuSpecialDietFieldsRead,
    NekkuSpecialDietFieldOptionsRead,
    NekkuOrdersReportRead,
    NekkuManualOrder,
    NonSsnChildrenReport,
    NotesByGroupRead,
    OccupancyGroupReportRead,
    OccupancyRead,
    OccupancyReportRead,
    OccupancySpeculatedRead,
    OutOfOfficeRead,
    OutOfOfficeUpdate,
    OutOfOfficeDelete,
    PairingInit(securityEvent = true, securityLevel = "medium"),
    PairingChallenge(securityEvent = true, securityLevel = "medium"),
    PairingResponse(securityEvent = true, securityLevel = "medium"),
    PairingValidation(securityEvent = true, securityLevel = "medium"),
    PairingStatusRead,
    ParentShipsCreate,
    ParentShipsDelete,
    ParentShipsRead,
    ParentShipsRetry,
    ParentShipsUpdate,
    PartnerShipsCreate,
    PartnerShipsDelete,
    PartnerShipsRead,
    PartnerShipsRetry,
    PartnerShipsUpdate,
    PartnersInDifferentAddressReportRead,
    PatuReportSend,
    PaymentsSearch,
    PaymentsConfirmDrafts,
    PaymentsCreate,
    PaymentsDeleteDrafts,
    PaymentsRevertToDrafts,
    PaymentsSend,
    PedagogicalDocumentCreate,
    PedagogicalDocumentCountUnread,
    PedagogicalDocumentReadByGuardian,
    PedagogicalDocumentRead,
    PedagogicalDocumentUpdate,
    PersonalDataUpdate,
    PersonCreate,
    PersonDelete,
    PersonDependantRead,
    PersonGuardianRead,
    // PersonBlockedGuardiansRead,
    PersonDetailsRead,
    PersonDetailsSearch,
    PersonDuplicate,
    PersonIncomeCreate,
    PersonIncomeDelete,
    PersonIncomeRead,
    PersonIncomeUpdate,
    PersonIncomeNotificationRead,
    PersonRead,
    PersonSensitiveDetailsRead,
    PersonMerge,
    PersonUpdate,
    PersonUpdateEvakaRights(securityEvent = true, securityLevel = "medium"),
    PersonVtjFamilyUpdate,
    PinCodeLockedRead,
    PinCodeUpdate(securityEvent = true, securityLevel = "medium"),
    PinLogin(securityEvent = true, securityLevel = "low"),
    PisFamilyRead,
    PlacementCancel,
    PlacementCountReportRead,
    PlacementCreate,
    PlacementSketchingReportRead,
    // PlacementPlanSearch,
    PlacementSearch,
    PlacementUpdate,
    PlacementServiceNeedCreate,
    PlacementServiceNeedDelete,
    PlacementServiceNeedUpdate,
    PlacementTerminate,
    PlacementChildPlacementPeriodsRead,
    PreschoolAbsenceReport,
    PreschoolTermCreate,
    PreschoolTermUpdate,
    PreschoolTermDelete,
    // PresenceReportRead,
    PushSettingsRead,
    PushSettingsSet,
    PushSubscriptionUpsert,
    RawReportRead,
    SendJamixOrders,
    ServiceNeedOptionsRead,
    ServiceNeedReportRead,
    SettingsRead,
    SettingsUpdate,
    SpecialDietsRead,
    // SpecialDietsUpdate,
    MealTexturesRead,
    SextetReportRead,
    UnitStaffAttendanceRead,
    StaffAttendanceArrivalCreate,
    StaffAttendanceArrivalExternalCreate,
    StaffAttendanceDepartureCreate,
    StaffAttendanceDepartureExternalCreate,
    StaffAttendanceRead,
    StaffAttendanceUpdate,
    // StaffAttendanceDelete,
    // StaffAttendanceExternalDelete,
    StaffAttendanceExternalUpdate,
    StaffOccupancyCoefficientRead,
    StaffOccupancyCoefficientUpsert,
    StaffOpenAttendanceRead,
    StartingPlacementsReportRead,
    SystemNotificationsSet,
    SystemNotificationsDelete,
    SystemNotificationsReadAll,
    SystemNotificationsReadCitizen,
    SystemNotificationsReadEmployee,
    SystemNotificationsReadEmployeeMobile,
    TampereRegionalSurveyMonthly,
    TampereRegionalSurveyYearly,
    TampereRegionalSurveyAgeStatistics,
    TampereRegionalSurveyMunicipalVoucherDistribution,
    TemporaryEmployeesRead,
    TemporaryEmployeeCreate,
    TemporaryEmployeeRead,
    TemporaryEmployeeUpdate,
    TemporaryEmployeeDeleteAcl,
    TemporaryEmployeeDelete,
    TimelineRead,
    TitaniaReportDelete,
    TitaniaReportRead,
    UnitAclCreate(securityEvent = true, securityLevel = "medium"),
    UnitAclDelete(securityEvent = true, securityLevel = "medium"),
    UnitAclDeleteScheduled(securityEvent = true, securityLevel = "medium"),
    UnitAclRead,
    UnitScheduledAclRead,
    UnitAttendanceReservationsRead,
    UnitCalendarEventsRead,
    UnitFeaturesRead,
    UnitFeaturesUpdate,
    UnitServiceWorkerNoteRead,
    UnitServiceWorkerNoteSet,
    UnitGroupAclUpdate(securityEvent = true, securityLevel = "medium"),
    UnitGroupsCreate,
    UnitGroupsUpdate,
    UnitGroupsDelete,
    UnitGroupsSearch,
    UnitGroupsCaretakersCreate,
    UnitGroupsCaretakersDelete,
    UnitGroupsCaretakersRead,
    UnitGroupsCaretakersUpdate,
    UnitCreate,
    UnitCounters,
    UnitRead,
    UnitDailyReservationStatistics,
    UnitSearch,
    UnitOperationPeriodsRead,
    UnitUpdate,
    UnitView,
    UnitsReportRead,
    VardaReportRead,
    VardaReportOperations,
    VardaUnitReportRead,
    VoucherValueDecisionHeadOfFamilyCreateRetroactive,
    VoucherValueDecisionHeadOfFamilyRead,
    VoucherValueDecisionIgnore,
    VoucherValueDecisionMarkSent,
    VoucherValueDecisionPdfRead,
    VoucherValueDecisionRead,
    VoucherValueDecisionReadMetadata,
    VoucherValueDecisionSearch,
    VoucherValueDecisionSend,
    VoucherValueDecisionSetType,
    VoucherValueDecisionUnignore,

    // Everything above still uses the legacy `Audit.<Event>.log(targetId = ...)` signature.
    // Events below have been migrated to `audit.log(Audit.<Event>, clock)` via AuditContext.
    // Move an event here (keeping this section alphabetical) once its endpoint is migrated; when
    // the section above is empty, delete this separator and merge back into one alphabetical list.
    AbsenceApplicationAccept,
    AbsenceApplicationCreate,
    AbsenceApplicationDelete,
    AbsenceApplicationPossibleRead,
    AbsenceApplicationRead,
    AbsenceApplicationReject,
    AbsenceCitizenCreate,
    AbsenceDelete,
    AbsenceDeleteRange,
    AbsenceRead,
    AbsenceUpsert,
    AddressPageDownloadPdf,
    ApplicationAdminDetailsUpdate,
    ApplicationCancel,
    ApplicationConfirmDecisionsMailed,
    ApplicationCreate,
    ApplicationDelete,
    ApplicationPlacementDraftDelete,
    ApplicationPlacementDraftUpdate,
    ApplicationRead,
    ApplicationReadActivePlacementsByType,
    ApplicationReadDuplicates,
    ApplicationReadMetadata,
    ApplicationReadNotifications,
    ApplicationReturnToSent,
    ApplicationReturnToWaitingDecision,
    ApplicationReturnToWaitingPlacement,
    ApplicationSearch,
    ApplicationSend,
    ApplicationSendDecisionsWithoutProposal,
    ApplicationUpdate,
    ApplicationVerify,
    ApplicationsReportRead,
    AssistanceActionOptionsRead,
    AssistanceFactorCreate,
    AssistanceFactorDelete,
    AssistanceFactorUpdate,
    AttendanceReservationCitizenCreate,
    AttendanceReservationEmployeeCreate,
    ChildAssistanceActionCreate,
    ChildAssistanceActionDelete,
    ChildAssistanceActionUpdate,
    ChildDatePresenceUpsert,
    ChildServiceApplicationAccept,
    ChildServiceApplicationReject,
    ChildServiceApplicationsRead,
    DaycareAssistanceCreate,
    DaycareAssistanceDelete,
    DaycareAssistanceUpdate,
    DecisionAccept,
    DecisionArchive,
    DecisionDownloadPdf,
    DecisionDraftRead,
    DecisionDraftUpdate,
    DecisionRead,
    DecisionReasoningGenericCreate,
    DecisionReasoningGenericDelete,
    DecisionReasoningGenericRead,
    DecisionReasoningGenericRemove,
    DecisionReasoningGenericUpdate,
    DecisionReasoningIndividualCreate,
    DecisionReasoningIndividualRead,
    DecisionReasoningIndividualRemove,
    DecisionReject,
    DecisionUnitsRead,
    FeeDecisionArchive,
    NoteCreate,
    NoteDelete,
    NoteRead,
    NoteUpdate,
    OtherAssistanceMeasureCreate,
    OtherAssistanceMeasureDelete,
    OtherAssistanceMeasureUpdate,
    PlacementDesktopDaycaresRead,
    PlacementPlanCreate,
    PlacementPlanDraftRead,
    PlacementPlanRespond,
    PlacementProposalAccept,
    PlacementProposalCreate,
    PlacementTool,
    PlacementToolApplicationCreate,
    PlacementToolValidate,
    PreschoolAssistanceCreate,
    PreschoolAssistanceDelete,
    PreschoolAssistanceUpdate,
    PreschoolTermRead,
    ServiceWorkerNoteUpdate,
    UnitApplicationsRead,
    UnitServiceApplicationsRead,
    VoucherValueDecisionArchive;

    private val eventCode = name

    class UseNamedArguments private constructor()

    /**
     * Logs an audit event with optional context information.
     *
     * Examples:
     * ```
     * // Simple read operation - only targetId
     * Audit.UnitRead.log(targetId = AuditId(daycareId))
     *
     * // Creation operation - targetId is the context, objectId is the created resource
     * Audit.PlacementCreate.log(
     *     targetId = AuditId(listOf(childId, unitId)),
     *     objectId = AuditId(placementIds)
     * )
     *
     * // Update operation - targetId is what was updated, objectId provides related context
     * Audit.ApplicationUpdate.log(
     *     targetId = AuditId(applicationId),
     *     objectId = AuditId(childId)
     * )
     *
     * // Batch operations - meta provides count information
     * Audit.UnitSearch.log(meta = mapOf("count" to units.size))
     *
     * // Complex operations - combining all parameters
     * Audit.PlacementUpdate.log(
     *     targetId = AuditId(placementId),
     *     objectId = AuditId(listOf(childId, unitId)),
     *     meta = mapOf("startDate" to startDate, "endDate" to endDate)
     * )
     *
     * // Report generation - meta provides filter criteria
     * Audit.PreschoolAbsenceReport.log(
     *     meta = mapOf(
     *         "unitId" to unitId,
     *         "termStart" to termStart,
     *         "termEnd" to termEnd
     *     )
     * )
     *
     * // Multiple IDs in targetId
     * Audit.MessagingMarkMessagesReadWrite.log(
     *     targetId = AuditId(listOf(accountId, threadId))
     * )
     * ```
     */
    fun log(
        // This is a hack to force passing all real parameters by name
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        /** The primary resource or entity being acted upon by this audit event. */
        targetId: AuditId? = null,
        /** Related or secondary entities affected by the action, or the result of the action. */
        objectId: AuditId? = null,
        /**
         * Additional contextual information such as counts, date ranges, or other metadata relevant
         * to the audit event.
         */
        meta: Map<String, Any?> = emptyMap(),
    ) {
        logger.audit(
            mapOf(
                "eventCode" to eventCode,
                "targetId" to targetId?.value,
                "objectId" to objectId?.value,
                "securityLevel" to securityLevel,
                "securityEvent" to securityEvent,
            ) + if (meta.isNotEmpty()) mapOf("meta" to meta) else emptyMap()
        ) {
            eventCode
        }
    }

    fun log(
        // This is a hack to force passing all real parameters by name
        @Suppress("UNUSED_PARAMETER") vararg forceNamed: UseNamedArguments,
        today: LocalDate,
        minDate: LocalDate?,
        context: Map<String, Any?>,
        meta: Map<String, Any?> = emptyMap(),
    ) =
        logger.audit(
            mapOf(
                "eventCode" to eventCode,
                "context" to context,
                "minDate" to minDate?.toString(),
                "daysIntoHistory" to daysIntoHistory(minDate, today),
                "securityLevel" to securityLevel,
                "securityEvent" to securityEvent,
            ) + if (meta.isNotEmpty()) mapOf("meta" to meta) else emptyMap()
        ) {
            eventCode
        }
}

/**
 * How many days [minDate] reaches into the past relative to [today]. Returns `null` when [minDate]
 * is null, and `0` when [minDate] is today or in the future, so the value never goes negative.
 */
fun daysIntoHistory(minDate: LocalDate?, today: LocalDate): Long? {
    if (minDate == null) return null
    return maxOf(0L, ChronoUnit.DAYS.between(minDate, today))
}

private val logger = KotlinLogging.logger {}

data class AuditChange(val old: Any?, val new: Any?)

/** Returns changes between given objects for audit logging */
fun <T> changes(
    old: T,
    new: T,
    fields: Pair<KProperty1<T, Any?>, Map<String, KProperty1<T, Any?>>>,
): Map<String, AuditChange> {
    val changes =
        fields.second
            .mapNotNull { (name, property) ->
                val oldValue = property.get(old)
                val newValue = property.get(new)
                if (oldValue != newValue) name to AuditChange(old = oldValue, new = newValue)
                else null
            }
            .toMap()
    return mapOf("id" to AuditChange(old = fields.first.get(old), new = fields.first.get(new))) +
        changes
}
