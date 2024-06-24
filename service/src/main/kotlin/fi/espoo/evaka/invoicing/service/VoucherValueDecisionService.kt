// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.decision.DecisionSendAddress
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.invoicing.data.getValueDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.data.getVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.partnerIsCodebtor
import fi.espoo.evaka.invoicing.data.removeVoucherValueDecisionIgnore
import fi.espoo.evaka.invoicing.data.setVoucherValueDecisionToIgnored
import fi.espoo.evaka.invoicing.data.setVoucherValueDecisionType
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.updateVoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.FinanceDecisionType
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.setting.SettingType
import fi.espoo.evaka.setting.getSettings
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.shared.message.IMessageProvider
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class VoucherValueDecisionService(
    private val pdfGenerator: PdfGenerator,
    private val documentClient: DocumentService,
    private val messageProvider: IMessageProvider,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    env: BucketEnv,
    private val emailEnv: EmailEnv,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailClient: EmailClient
) {
    private val bucket = env.voucherValueDecisions

    init {
        asyncJobRunner.registerHandler(::runSendNewVoucherValueDecisionEmail)
    }

    fun createDecisionPdf(
        tx: Database.Transaction,
        decisionId: VoucherValueDecisionId
    ) {
        val decision = getDecision(tx, decisionId)
        check(decision.documentKey.isNullOrBlank()) {
            "Voucher value decision $decisionId has document key already!"
        }

        val settings = tx.getSettings()

        val pdf = generatePdf(decision, settings)
        val key =
            documentClient
                .upload(bucket, Document("value_decision_$decisionId.pdf", pdf, "application/pdf"))
                .key
        tx.updateVoucherValueDecisionDocumentKey(decision.id, key)
    }

    fun getDecisionPdfResponse(
        dbc: Database.Connection,
        decisionId: VoucherValueDecisionId
    ): ResponseEntity<Any> {
        val documentKey =
            dbc.read { it.getVoucherValueDecisionDocumentKey(decisionId) }
                ?: throw NotFound("No voucher value decision found with ID ($decisionId)")
        return documentClient.responseAttachment(bucket, documentKey, null)
    }

    fun sendDecision(
        tx: Database.Transaction,
        clock: EvakaClock,
        decisionId: VoucherValueDecisionId
    ): Boolean {
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

        val lang =
            if (decision.headOfFamily.language == "sv") OfficialLanguage.SV else OfficialLanguage.FI

        // If address is missing (restricted info enabled), use the financial handling address
        // instead
        val sendAddress =
            DecisionSendAddress.fromPerson(decision.headOfFamily)
                ?: messageProvider.getDefaultFinancialDecisionAddress(lang)

        val documentDisplayName = suomiFiDocumentFileName(lang)
        val messageHeader = messageProvider.getVoucherValueDecisionHeader(lang)
        val messageContent = messageProvider.getVoucherValueDecisionContent(lang)
        asyncJobRunner.plan(
            tx,
            listOf(
                AsyncJob.SendMessage(
                    SfiMessage(
                        messageId = decision.id.toString(),
                        documentId = decision.id.toString(),
                        documentDisplayName = documentDisplayName,
                        documentBucket = bucket,
                        documentKey = decision.documentKey,
                        firstName = decision.headOfFamily.firstName,
                        lastName = decision.headOfFamily.lastName,
                        streetAddress = sendAddress.street,
                        postalCode = sendAddress.postalCode,
                        postOffice = sendAddress.postOffice,
                        ssn = decision.headOfFamily.ssn!!,
                        messageHeader = messageHeader,
                        messageContent = messageContent
                    )
                )
            ),
            runAt = clock.now()
        )

        tx.markVoucherValueDecisionsSent(listOf(decision.id), clock.now())
        asyncJobRunner.plan(
            tx,
            listOf(AsyncJob.SendNewVoucherValueDecisionEmail(decisionId = decision.id)),
            runAt = clock.now()
        )
        return true
    }

    fun ignoreDrafts(
        tx: Database.Transaction,
        ids: List<VoucherValueDecisionId>,
        today: LocalDate
    ) {
        tx
            .getValueDecisionsByIds(ids)
            .map { decision ->
                if (decision.status != VoucherValueDecisionStatus.DRAFT) {
                    throw BadRequest(
                        "Error with decision ${decision.id}: Only drafts can be ignored"
                    )
                }
                if (decision.validFrom > today) {
                    throw BadRequest(
                        "Error with decision ${decision.id}: Must not ignore future drafts, data should be fixed instead"
                    )
                }
                decision
            }.forEach { tx.setVoucherValueDecisionToIgnored(it.id) }
    }

    fun unignoreDrafts(
        tx: Database.Transaction,
        ids: List<VoucherValueDecisionId>
    ): Set<PersonId> =
        tx
            .getValueDecisionsByIds(ids)
            .map { decision ->
                if (decision.status != VoucherValueDecisionStatus.IGNORED) {
                    throw BadRequest("Error with decision ${decision.id}: not ignored")
                }
                decision
            }.map {
                tx.removeVoucherValueDecisionIgnore(it.id)
                it.headOfFamilyId
            }.toSet()

    private fun getDecision(
        tx: Database.Read,
        decisionId: VoucherValueDecisionId
    ): VoucherValueDecisionDetailed =
        tx.getVoucherValueDecision(decisionId)?.let {
            it.copy(
                partnerIsCodebtor =
                    tx.partnerIsCodebtor(
                        it.headOfFamily.id,
                        it.partner?.id,
                        listOf(it.child.id),
                        DateRange(it.validFrom, it.validTo)
                    )
            )
        } ?: error("No voucher value decision found with ID ($decisionId)")

    private fun generatePdf(
        decision: VoucherValueDecisionDetailed,
        settings: Map<SettingType, String>
    ): ByteArray {
        val lang =
            if (decision.placement.unit.language == "sv") {
                OfficialLanguage.SV
            } else {
                OfficialLanguage.FI
            }

        return pdfGenerator.generateVoucherValueDecisionPdf(
            VoucherValueDecisionPdfData(decision, settings, lang)
        )
    }

    fun setType(
        tx: Database.Transaction,
        decisionId: VoucherValueDecisionId,
        type: VoucherValueDecisionType
    ) {
        val decision =
            tx.getVoucherValueDecision(decisionId)
                ?: throw BadRequest("Decision not found with id $decisionId")
        if (decision.status != VoucherValueDecisionStatus.DRAFT) {
            throw BadRequest("Can't change type for decision $decisionId")
        }

        tx.setVoucherValueDecisionType(decisionId, type)
    }

    fun runSendNewVoucherValueDecisionEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendNewVoucherValueDecisionEmail
    ) {
        val voucherValueDecisionId = msg.decisionId
        val decision =
            db.read { tx -> tx.getVoucherValueDecision(voucherValueDecisionId) }
                ?: throw NotFound("Decision not found")

        logger.info {
            "Sending voucher value decision emails for (decisionId: $voucherValueDecisionId)"
        }

        val recipients = listOfNotNull(decision.headOfFamily)

        recipients.forEach { recipient ->
            // simplified to get rid of superfluous language requirement
            val fromAddress = emailEnv.sender(Language.fi)
            val content =
                emailMessageProvider.financeDecisionNotification(
                    FinanceDecisionType.VOUCHER_VALUE_DECISION
                )
            Email
                .create(
                    db,
                    recipient.id,
                    EmailMessageType.DECISION_NOTIFICATION,
                    fromAddress,
                    content,
                    "$voucherValueDecisionId - ${recipient.id}"
                )?.also { emailClient.send(it) }
        }

        logger.info {
            "Successfully sent voucher value decision emails (${recipients.size}) (id: $voucherValueDecisionId)."
        }
    }
}

private fun suomiFiDocumentFileName(lang: OfficialLanguage) =
    if (lang == OfficialLanguage.SV) {
        "Beslut_om_avgift_för_småbarnspedagogik.pdf"
    } else {
        "Varhaiskasvatuksen_maksupäätös.pdf"
    }
