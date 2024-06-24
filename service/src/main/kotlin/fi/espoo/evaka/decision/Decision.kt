// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

data class Decision(
    val id: DecisionId,
    val createdBy: String,
    val type: DecisionType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val unit: DecisionUnit,
    val applicationId: ApplicationId,
    val childId: ChildId,
    val childName: String,
    val documentKey: String?,
    val decisionNumber: Long,
    val sentDate: LocalDate?,
    val status: DecisionStatus,
    val requestedStartDate: LocalDate?,
    val resolved: LocalDate?,
    val resolvedByName: String?,
    // True if the document is a legacy document that may contain guardian name and address.
    val documentContainsContactInfo: Boolean
) {
    fun validRequestedStartDatePeriod(featureConfig: FeatureConfig) =
        FiniteDateRange(
            startDate,
            minOf(
                endDate,
                startDate.plusDays(
                    when (this.type) {
                        DecisionType.CLUB -> featureConfig.requestedStartUpperLimit
                        DecisionType.DAYCARE,
                        DecisionType.DAYCARE_PART_TIME -> featureConfig.requestedStartUpperLimit
                        DecisionType.PRESCHOOL -> 0
                        DecisionType.PRESCHOOL_DAYCARE,
                        DecisionType.PRESCHOOL_CLUB -> featureConfig.requestedStartUpperLimit
                        DecisionType.PREPARATORY_EDUCATION -> 0
                    }.toLong()
                )
            )
        )
}

data class DecisionUnit(
    val id: DaycareId,
    val name: String,
    val daycareDecisionName: String,
    val preschoolDecisionName: String,
    val manager: String?,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val phone: String?,
    val decisionHandler: String,
    val decisionHandlerAddress: String,
    val providerType: ProviderType
)
