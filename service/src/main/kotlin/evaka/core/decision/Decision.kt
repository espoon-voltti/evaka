// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.daycare.domain.ProviderType
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
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
    val documentContainsContactInfo: Boolean,
    val archivedAt: HelsinkiDateTime?,
) {
    fun validRequestedStartDatePeriod(featureConfig: FeatureConfig, isCitizen: Boolean) =
        FiniteDateRange(
            startDate,
            minOf(
                endDate,
                startDate.plusDays(
                    when (this.type) {
                        DecisionType.CLUB,
                        DecisionType.DAYCARE,
                        DecisionType.DAYCARE_PART_TIME,
                        DecisionType.PRESCHOOL_DAYCARE,
                        DecisionType.PRESCHOOL_CLUB -> {
                            featureConfig.requestedStartUpperLimit
                        }

                        DecisionType.PRESCHOOL,
                        DecisionType.PREPARATORY_EDUCATION -> {
                            if (isCitizen) 0 else featureConfig.requestedStartUpperLimit
                        }
                    }.toLong()
                ),
            ),
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
    val providerType: ProviderType,
)
