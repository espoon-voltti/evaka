// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.invoicing.client.S3DocumentClient
import fi.espoo.evaka.invoicing.data.annulVoucherValueDecisions
import fi.espoo.evaka.invoicing.data.getValueDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.data.getVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.isElementaryFamily
import fi.espoo.evaka.invoicing.data.lockValueDecisions
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.setVoucherValueDecisionType
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionEndDatesIfNeeded
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.setting.SettingType
import fi.espoo.evaka.setting.getSettings
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SuomiFiAsyncJob
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.message.langWithDefault
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Component

@Component
class VoucherValueDecisionService(
    private val pdfService: PDFService,
    private val s3Client: S3DocumentClient,
    private val messageProvider: IMessageProvider,
    private val sfiAsyncJobRunner: AsyncJobRunner<SuomiFiAsyncJob>,
    env: BucketEnv
) {
    private val bucket = env.voucherValueDecisions

    fun createDecisionPdf(tx: Database.Transaction, decisionId: VoucherValueDecisionId) {
        val decision = getDecision(tx, decisionId)
        check(decision.documentKey.isNullOrBlank()) { "Voucher value decision $decisionId has document key already!" }

        val settings = tx.getSettings()

        val pdf = generatePdf(decision, settings)
        val key = uploadPdf(decision.id, pdf)
        tx.updateVoucherValueDecisionDocumentKey(decision.id, key)
    }

    fun getDecisionPdf(tx: Database.Read, decisionId: VoucherValueDecisionId): Pair<String, ByteArray> {
        val key = tx.getVoucherValueDecisionDocumentKey(decisionId)
            ?: throw NotFound("No voucher value decision found with ID ($decisionId)")
        return key to s3Client.getPdf(bucket, key)
    }

    fun sendDecision(tx: Database.Transaction, decisionId: VoucherValueDecisionId): Boolean {
        val decision = getDecision(tx, decisionId)
        check(decision.status == VoucherValueDecisionStatus.WAITING_FOR_SENDING) {
            "Cannot send voucher value decision ${decision.id} - has status ${decision.status}"
        }
        checkNotNull(decision.documentKey) {
            "Cannot send voucher value decision ${decision.id} - missing document key"
        }

        if (decision.requiresManualSending) {
            tx.updateVoucherValueDecisionStatus(
                listOf(decision.id),
                VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING
            )
            return false
        }

        val lang = if (decision.headOfFamily.language == "sv") "sv" else "fi"
        val documentDisplayName = suomiFiDocumentFileName(lang)
        val messageHeader = messageProvider.getVoucherValueDecisionHeader(langWithDefault(lang))
        val messageContent = messageProvider.getVoucherValueDecisionContent(langWithDefault(lang))
        sfiAsyncJobRunner.plan(
            tx,
            listOf(
                SuomiFiAsyncJob.SendMessage(
                    SfiMessage(
                        messageId = decision.id.toString(),
                        documentId = decision.id.toString(),
                        documentDisplayName = documentDisplayName,
                        documentBucket = bucket,
                        documentKey = decision.documentKey,
                        language = lang,
                        firstName = decision.headOfFamily.firstName,
                        lastName = decision.headOfFamily.lastName,
                        streetAddress = decision.headOfFamily.streetAddress,
                        postalCode = decision.headOfFamily.postalCode,
                        postOffice = decision.headOfFamily.postOffice,
                        ssn = decision.headOfFamily.ssn!!,
                        messageHeader = messageHeader,
                        messageContent = messageContent
                    )
                )
            )
        )

        tx.markVoucherValueDecisionsSent(listOf(decision.id), HelsinkiDateTime.now())

        return true
    }

    fun endOutdatedDecisions(tx: Database.Transaction, now: HelsinkiDateTime) {
        val decisionIds = tx.createQuery(
            """
SELECT DISTINCT d.id
FROM voucher_value_decision d

-- The current placement that this decision covers
LEFT JOIN placement p ON (
    :now BETWEEN p.start_date AND p.end_date AND
    d.child_id = p.child_id AND 
    daterange(d.valid_from, d.valid_to, '[]') && daterange(p.start_date, p.end_date, '[]')
)
LEFT JOIN daycare unit ON unit.id = p.unit_id
LEFT JOIN service_need sn ON sn.placement_id = p.id
LEFT JOIN service_need_option sno ON sno.id = sn.option_id
LEFT JOIN service_need_option default_sno ON default_sno.default_option AND default_sno.valid_placement_type = p.type

WHERE
    d.status = 'SENT' AND

    -- This decision covers the current date
    :now BETWEEN d.valid_from AND d.valid_to AND

    -- This is the latest voucher value decision for this child
    NOT EXISTS (
        SELECT 1
        FROM voucher_value_decision d_later
        WHERE
            d_later.id <> d.id AND
            d_later.status IN ('DRAFT', 'WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING', 'SENT') AND
            d_later.child_id = d.child_id AND
            d_later.valid_from > d.valid_from
    ) AND
    
    (
        -- No valid placement exists for this decision currently
        p.id IS NULL OR
        -- The current placement is not to a voucher unit
        unit.provider_type <> 'PRIVATE_SERVICE_VOUCHER'::unit_provider_type OR
        -- Service need is not eligible for voucher
        coalesce(sno.voucher_value_coefficient, default_sno.voucher_value_coefficient) = 0
    )
"""
        ).bind("now", now.toLocalDate()).mapTo<VoucherValueDecisionId>().toList()

        tx.lockValueDecisions(decisionIds)

        tx
            .getValueDecisionsByIds(decisionIds)
            .forEach { decision ->
                val mergedPlacementPeriods = tx
                    .createQuery(
                        """
SELECT daterange(p.start_date, p.end_date, '[]') * daterange(coalesce(sn.start_date, p.start_date), coalesce(sn.end_date, p.end_date), '[]')
FROM placement p
LEFT JOIN service_need sn ON sn.placement_id = p.id AND daterange(sn.start_date, sn.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
LEFT JOIN service_need_option sno ON sno.id = sn.option_id
LEFT JOIN service_need_option default_sno ON default_sno.default_option AND default_sno.valid_placement_type = p.type
WHERE child_id = :childId AND unit_id = :unitId
AND :dateRange && daterange(p.start_date, p.end_date, '[]')
AND :dateRange && daterange(coalesce(sn.start_date, p.start_date), coalesce(sn.end_date, p.end_date), '[]')
AND coalesce(sno.voucher_value_coefficient, default_sno.voucher_value_coefficient) > 0
"""
                    )
                    .bind("childId", decision.child.id)
                    .bind("unitId", decision.placement.unitId)
                    .bind("dateRange", DateRange(decision.validFrom, decision.validTo))
                    .mapTo<FiniteDateRange>()
                    .sortedBy { it.start }
                    .fold(listOf<FiniteDateRange>()) { periods, period ->
                        when {
                            periods.isEmpty() -> listOf(period)
                            periods.last().end.plusDays(1) == period.start ->
                                periods.dropLast(1) + periods.last().copy(end = period.end)
                            else -> periods + period
                        }
                    }

                when {
                    mergedPlacementPeriods.isEmpty() -> tx.annulVoucherValueDecisions(listOf(decision.id), now)
                    mergedPlacementPeriods.first().end < decision.validTo -> {
                        val withUpdatedEndDate = decision.copy(validTo = mergedPlacementPeriods.first().end)
                        tx.updateVoucherValueDecisionEndDatesIfNeeded(listOf(withUpdatedEndDate), now)
                    }
                }
            }
    }

    private fun getDecision(tx: Database.Read, decisionId: VoucherValueDecisionId): VoucherValueDecisionDetailed =
        tx.getVoucherValueDecision(decisionId)?.let {
            it.copy(isElementaryFamily = tx.isElementaryFamily(it.headOfFamily.id, it.partner?.id, listOf(it.child.id)))
        } ?: error("No voucher value decision found with ID ($decisionId)")

    private val key = { id: VoucherValueDecisionId -> "value_decision_$id.pdf" }
    private fun uploadPdf(decisionId: VoucherValueDecisionId, file: ByteArray): String {
        val key = key(decisionId)
        s3Client.uploadPdfToS3(bucket, key, file)
        return key
    }

    private fun generatePdf(decision: VoucherValueDecisionDetailed, settings: Map<SettingType, String>): ByteArray {
        val lang = if (decision.headOfFamily.language == "sv") DocumentLang.sv else DocumentLang.fi
        return pdfService.generateVoucherValueDecisionPdf(VoucherValueDecisionPdfData(decision, settings, lang))
    }

    fun setType(tx: Database.Transaction, decisionId: VoucherValueDecisionId, type: VoucherValueDecisionType) {
        val decision = tx.getVoucherValueDecision(decisionId)
            ?: throw BadRequest("Decision not found with id $decisionId")
        if (decision.status != VoucherValueDecisionStatus.DRAFT) {
            throw BadRequest("Can't change type for decision $decisionId")
        }

        tx.setVoucherValueDecisionType(decisionId, type)
    }
}

private fun suomiFiDocumentFileName(lang: String) =
    if (lang == "sv") "Beslut_om_avgift_för_småbarnspedagogik.pdf"
    else "Varhaiskasvatuksen_maksupäätös.pdf"
