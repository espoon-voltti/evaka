// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.data.getVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.isElementaryFamily
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.setVoucherValueDecisionType
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.setting.SettingType
import fi.espoo.evaka.setting.getSettings
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SuomiFiAsyncJob
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.message.langWithDefault
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class VoucherValueDecisionService(
    private val pdfService: PDFService,
    private val documentClient: DocumentService,
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
        val key = documentClient.upload(bucket, Document("value_decision_$decisionId.pdf", pdf, "application/pdf")).key
        tx.updateVoucherValueDecisionDocumentKey(decision.id, key)
    }

    fun getDecisionPdfResponse(dbc: Database.Connection, decisionId: VoucherValueDecisionId): ResponseEntity<Any> {
        val documentKey = dbc.read { it.getVoucherValueDecisionDocumentKey(decisionId) }
            ?: throw NotFound("No voucher value decision found with ID ($decisionId)")
        return documentClient.responseAttachment(bucket, documentKey, null)
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

    private fun getDecision(tx: Database.Read, decisionId: VoucherValueDecisionId): VoucherValueDecisionDetailed =
        tx.getVoucherValueDecision(decisionId)?.let {
            it.copy(isElementaryFamily = tx.isElementaryFamily(it.headOfFamily.id, it.partner?.id, listOf(it.child.id)))
        } ?: error("No voucher value decision found with ID ($decisionId)")

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
