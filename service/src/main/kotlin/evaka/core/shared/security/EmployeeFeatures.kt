// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security

data class EmployeeFeatures(
    @Deprecated("Derive from permittedGlobalActions (APPLICATIONS_PAGE)") val applications: Boolean,
    @Deprecated("Derive from permittedGlobalActions (EMPLOYEES_PAGE)") val employees: Boolean,
    @Deprecated("Derive from permittedGlobalActions (FINANCE_BASICS_PAGE)")
    val financeBasics: Boolean,
    @Deprecated("Derive from permittedGlobalActions (FINANCE_PAGE)") val finance: Boolean,
    @Deprecated("Derive from permittedGlobalActions (MESSAGES_PAGE)") val messages: Boolean,
    @Deprecated("Derive from permittedGlobalActions (PERSON_SEARCH_PAGE)")
    val personSearch: Boolean,
    @Deprecated("Derive from permittedGlobalActions (REPORTS_PAGE)") val reports: Boolean,
    @Deprecated("Derive from permittedGlobalActions (SETTINGS_PAGE)") val settings: Boolean,
    @Deprecated("Derive from permittedGlobalActions (READ_SYSTEM_NOTIFICATIONS)")
    val systemNotifications: Boolean,
    @Deprecated("Derive from permittedGlobalActions (HOLIDAY_AND_TERM_PERIODS_PAGE)")
    val holidayAndTermPeriods: Boolean,
    @Deprecated("Derive from permittedGlobalActions (UNIT_FEATURES_PAGE)")
    val unitFeatures: Boolean,
    @Deprecated("Derive from permittedGlobalActions (UNITS_PAGE)") val units: Boolean,
    @Deprecated("Derive from permittedGlobalActions (CREATE_UNIT)") val createUnits: Boolean,
    @Deprecated("Derive from permittedGlobalActions (DOCUMENT_TEMPLATES_PAGE)")
    val documentTemplates: Boolean,
    @Deprecated("Derive from permittedGlobalActions (PERSONAL_MOBILE_DEVICE_PAGE)")
    val personalMobileDevice: Boolean,
    @Deprecated("Derive from permittedGlobalActions (PIN_CODE_PAGE)") val pinCode: Boolean,
    @Deprecated("Derive from permittedGlobalActions (CREATE_DRAFT_INVOICES)")
    val createDraftInvoices: Boolean,
    // Scoped permission summary the frontend cannot derive; kept permanently.
    val createPlacements: Boolean,
    @Deprecated("Derive from permittedGlobalActions (SUBMIT_PATU_REPORT)")
    val submitPatuReport: Boolean,
    @Deprecated("Derive from permittedGlobalActions (PLACEMENT_TOOL)") val placementTool: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.replacementInvoices")
    val replacementInvoices: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.openRangesHolidayQuestionnaire")
    val openRangesHolidayQuestionnaire: Boolean,
    @Deprecated("Derive from permittedGlobalActions (OUT_OF_OFFICE_PAGE)") val outOfOffice: Boolean,
    @Deprecated("Derive from permittedGlobalActions (WRITE_DECISION_REASONINGS)")
    val decisionReasoningManagement: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.decisionReasoningGenericRemoval")
    val decisionReasoningGenericRemoval: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.decisionReasoningsEnabled")
    val decisionReasoningsEnabled: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.allowEnglishChildDocumentsForAllTypes")
    val allowEnglishChildDocumentsForAllTypes: Boolean,
    @Deprecated("Moved to EmployeeFeatureConfig.messageSupportEmail")
    val messageSupportEmail: String?,
)
