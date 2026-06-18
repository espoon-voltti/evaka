// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.security

/**
 * Deployment / feature configuration returned to the employee frontend so it has a single source of
 * truth and cannot drift out of sync. Independent of the user.
 */
data class EmployeeFeatureConfig(
    val replacementInvoices: Boolean,
    val decisionReasoningGenericRemoval: Boolean,
    val decisionReasoningsEnabled: Boolean,
    val openRangesHolidayQuestionnaire: Boolean,
    val allowEnglishChildDocumentsForAllTypes: Boolean,
    val messageSupportEmail: String?,
    val deletedMessagePlaceholderBody: String,
)
