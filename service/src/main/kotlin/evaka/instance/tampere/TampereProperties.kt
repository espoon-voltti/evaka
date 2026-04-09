// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere

import evaka.trevaka.frends.FrendsArchivalProperties
import java.time.Month
import org.springframework.boot.context.properties.ConfigurationProperties

/** All Tampere-specific configuration properties. */
@ConfigurationProperties(prefix = "tampere")
data class TampereProperties(
    val invoice: InvoiceProperties,
    val payment: PaymentProperties,
    val summertimeAbsence: SummertimeAbsenceProperties = SummertimeAbsenceProperties(),
    val bucket: BucketProperties,
    val biExport: BiExportProperties,
    val financeApiKey: String,
    val frends: FrendsArchivalProperties? = null,
    val archival: ArchivalProperties? = null,
)

data class InvoiceProperties(
    val url: String,
    val paymentTerm: String = "V000",
    val salesOrganisation: String = "1312",
    val distributionChannel: String = "00",
    val division: String = "00",
    val salesOrderType: String = "ZPH",
    val interfaceID: String = "352",
    val plant: String = "1310",
)

data class PaymentProperties(val url: String)

data class SummertimeAbsenceProperties(val freeMonth: Month = Month.JUNE)

data class BucketProperties(val export: String) {
    fun allBuckets() = listOf(export)
}

data class BiExportProperties(val prefix: String)

data class ArchivalProperties(
    val baseUrl: String,
    val schedule: ArchivalSchedule = ArchivalSchedule(),
)

data class ArchivalSchedule(
    val dailyDocumentLimit: Long = 0,
    val decisionDelayDays: Long = 180,
    val feeDecisionDelayDays: Long = 180,
    val voucherDecisionDelayDays: Long = 180,
    val documentDecisionDelayDays: Long = 30,
    val documentPlanDelayDays: Long = 180,
)
