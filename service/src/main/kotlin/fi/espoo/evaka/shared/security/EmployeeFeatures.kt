// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

data class EmployeeFeatures(
    val applications: Boolean,
    val employees: Boolean,
    val financeBasics: Boolean,
    val finance: Boolean,
    val messages: Boolean,
    val personSearch: Boolean,
    val reports: Boolean,
    val settings: Boolean,
    val systemNotifications: Boolean,
    val holidayAndTermPeriods: Boolean,
    val unitFeatures: Boolean,
    val units: Boolean,
    val createUnits: Boolean,
    val documentTemplates: Boolean,
    val personalMobileDevice: Boolean,
    val pinCode: Boolean,
    val assistanceNeedDecisionsReport: Boolean,
    val createDraftInvoices: Boolean,
    val createPlacements: Boolean,
    val submitPatuReport: Boolean,
    val placementTool: Boolean,
    val replacementInvoices: Boolean,
)
