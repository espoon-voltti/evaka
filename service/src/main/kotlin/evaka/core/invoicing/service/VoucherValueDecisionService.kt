// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.AuditContext
import evaka.core.EmailEnv
import evaka.core.caseprocess.CaseProcessState
import evaka.core.caseprocess.getCaseProcessByVoucherValueDecisionId
import evaka.core.caseprocess.insertCaseProcessHistoryRow
import evaka.core.daycare.domain.Language
import evaka.core.decision.DecisionSendAddress
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.invoicing.data.getValueDecisionsByIds
import evaka.core.invoicing.data.getVoucherValueDecision
import evaka.core.invoicing.data.markVoucherValueDecisionsSent
import evaka.core.invoicing.data.removeVoucherValueDecisionIgnore
import evaka.core.invoicing.data.setVoucherValueDecisionPartnerIsCodebtor
import evaka.core.invoicing.data.setVoucherValueDecisionToIgnored
import evaka.core.invoicing.data.setVoucherValueDecisionType
import evaka.core.invoicing.data.updateVoucherValueDecisionDocumentKey
import evaka.core.invoicing.data.updateVoucherValueDecisionStatus
import evaka.core.invoicing.domain.FinanceDecisionType
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.invoicing.domain.VoucherValueDecision
import evaka.core.invoicing.domain.VoucherValueDecisionDetailed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.pdfgen.PdfGenerator
import evaka.core.pis.NotificationCategory
import evaka.core.s3.DocumentKey
import evaka.core.s3.DocumentLocation
import evaka.core.s3.DocumentService
import evaka.core.setting.SettingType
import evaka.core.setting.getSettings
import evaka.core.sficlient.SentSfiMessage
import evaka.core.sficlient.SfiMessage
import evaka.core.sficlient.storeSentSfiMessage
import evaka.core.shared.PersonId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.PermanentAsyncJobFailure
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.message.IMessageProvider
import evaka.core.webpush.CitizenPushNotification
import evaka.core.webpush.CitizenPushNotifications
import evaka.core.webpush.getPushChildNames
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class VoucherValueDecisionService(
    private val pdfGenerator: PdfGenerator,
    private val documentClient: DocumentService,
    private val messageProvider: IMessageProvider,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailEnv: EmailEnv,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailClient: EmailClient,
    private val citizenPushNotifications: CitizenPushNotifications,
) {
    init {
        asyncJobRunner.registerHandler(::runSendNewVoucherValueDecisionEmail)
    }

    private fun getDecisionLanguage(decision: VoucherValueDecisionDetailed): OfficialLanguage =
        if (decision.headOfFamily.language == "sv") OfficialLanguage.SV else OfficialLanguage.FI

    fun createDecisionPdf(tx: Database.Transaction, decisionId: VoucherValueDecisionId) {
        val decision = getDecision(tx, decisionId)
        if (!decision.documentKey.isNullOrBlank()) {
            throw PermanentAsyncJobFailure(
                "Voucher value decision $decisionId has document key already!"
            )
        }

        val settings = tx.getSettings()

        val pdf = generatePdf(decision, settings)
        val key =
            documentClient
                .upload(DocumentKey.VoucherValueDecision(decisionId), pdf, "application/pdf")
                .key
        tx.updateVoucherValueDecisionDocumentKey(decision.id, key)
        tx.setVoucherValueDecisionPartnerIsCodebtor(
            decision.id,
            decision.partner != null && decision.partnerIsCodebtor == true,
        )
    }

    fun getDecisionPdfResponse(
        dbc: Database.Connection,
        decisionId: VoucherValueDecisionId,
        audit: AuditContext,
    ): ResponseEntity<Any> {
        val (documentKey, fileName) =
            dbc.read { tx ->
                val decision =
                    tx.getVoucherValueDecision(decisionId) ?: throw NotFound("Decision not found")
                audit
                    .add(decision.headOfFamily.id)
                    .add(listOfNotNull(decision.partner?.id))
                    .add(decision.child.id)
                    .add(decision.placement.unit.id)
                    .observeDate(decision.validFrom)
                if (decision.documentKey == null)
                    throw NotFound("Document key not found for decision $decisionId")
                val lang = getDecisionLanguage(decision)
                DocumentKey.VoucherValueDecision(decision.documentKey) to
                    calculateDecisionFileName(decision, lang, FileNameType.FILE_NAME)
            }
        val documentLocation = documentClient.locate(documentKey)
        return documentClient.responseAttachment(documentLocation, fileName)
    }

    fun sendDecision(
        tx: Database.Transaction,
        clock: EvakaClock,
        decisionId: VoucherValueDecisionId,
    ): Boolean {
        val now = clock.now()
        val decision = getDecision(tx, decisionId)
        if (decision.status != VoucherValueDecisionStatus.WAITING_FOR_SENDING) {
            throw PermanentAsyncJobFailure(
                "Cannot send voucher value decision ${decision.id} - has status ${decision.status}"
            )
        }
        checkNotNull(decision.documentKey) {
            "Cannot send voucher value decision ${decision.id} - missing document key"
        }

        if (decision.requiresManualSending) {
            tx.updateVoucherValueDecisionStatus(
                listOf(decision.id),
                VoucherValueDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            )
            return false
        }

        val lang = getDecisionLanguage(decision)
        val documentLocation =
            documentClient.locate(DocumentKey.VoucherValueDecision(decision.documentKey))

        sendSfiMessage(tx, clock, decision, decision.headOfFamily, lang, documentLocation)

        val codebtor = decision.partner?.takeIf { decision.partnerIsCodebtor == true }
        if (codebtor != null) {
            if (codebtor.ssn.isNullOrBlank()) {
                logger.info {
                    "Cannot deliver voucher value decision ${decision.id} to codebtor. SSN is missing."
                }
            } else {
                sendSfiMessage(tx, clock, decision, codebtor, lang, documentLocation)
            }
        }

        tx.markVoucherValueDecisionsSent(listOf(decision.id), now)

        tx.getCaseProcessByVoucherValueDecisionId(decisionId)?.let { process ->
            tx.insertCaseProcessHistoryRow(
                processId = process.id,
                state = CaseProcessState.COMPLETED,
                now = now,
                userId = AuthenticatedUser.SystemInternalUser.evakaUserId,
            )
        }

        planDecisionNotifications(tx, clock, listOf(decision))
        return true
    }

    private fun sendSfiMessage(
        tx: Database.Transaction,
        clock: EvakaClock,
        decision: VoucherValueDecisionDetailed,
        recipient: PersonDetailed,
        lang: OfficialLanguage,
        documentLocation: DocumentLocation,
    ) {
        val sendAddress =
            DecisionSendAddress.fromPerson(recipient)?.takeUnless {
                recipient.restrictedDetailsEnabled
            } ?: messageProvider.getDefaultFinancialDecisionAddress(lang)

        val messageId =
            tx.storeSentSfiMessage(
                SentSfiMessage(guardianId = recipient.id, voucherValueDecisionId = decision.id)
            )

        asyncJobRunner.plan(
            tx,
            listOf(
                AsyncJob.SendMessage(
                    SfiMessage(
                        messageId = messageId,
                        // Suomi.fi requires a unique document ID for each message
                        documentId = "${decision.id}|${recipient.id}",
                        documentDisplayName =
                            calculateDecisionFileName(decision, lang, FileNameType.DISPLAY_NAME),
                        documentBucket = documentLocation.bucket,
                        documentKey = documentLocation.key,
                        firstName = recipient.firstName,
                        lastName = recipient.lastName,
                        streetAddress = sendAddress.street,
                        postalCode = sendAddress.postalCode,
                        postOffice = sendAddress.postOffice,
                        ssn = recipient.ssn!!,
                        messageHeader = messageProvider.getVoucherValueDecisionHeader(lang),
                        messageContent = messageProvider.getVoucherValueDecisionContent(lang),
                    )
                )
            ),
            runAt = clock.now(),
        )
    }

    fun ignoreDrafts(
        tx: Database.Transaction,
        ids: List<VoucherValueDecisionId>,
        today: LocalDate,
        audit: AuditContext,
    ) {
        tx.getValueDecisionsByIds(ids)
            .onEach { audit.observeDate(it.validFrom) }
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
            }
            .forEach { tx.setVoucherValueDecisionToIgnored(it.id) }
    }

    fun unignoreDrafts(
        tx: Database.Transaction,
        ids: List<VoucherValueDecisionId>,
        audit: AuditContext,
    ): Set<PersonId> {
        return tx.getValueDecisionsByIds(ids)
            .onEach { audit.addDecision(it) }
            .map { decision ->
                if (decision.status != VoucherValueDecisionStatus.IGNORED) {
                    throw BadRequest("Error with decision ${decision.id}: not ignored")
                }
                decision
            }
            .map {
                tx.removeVoucherValueDecisionIgnore(it.id)
                it.headOfFamilyId
            }
            .toSet()
    }

    private fun getDecision(
        tx: Database.Read,
        decisionId: VoucherValueDecisionId,
    ): VoucherValueDecisionDetailed =
        tx.getVoucherValueDecision(decisionId)
            ?: error("No voucher value decision found with ID ($decisionId)")

    private fun generatePdf(
        decision: VoucherValueDecisionDetailed,
        settings: Map<SettingType, String>,
    ): ByteArray {
        val lang =
            if (decision.placement.unit.language == "sv") OfficialLanguage.SV
            else OfficialLanguage.FI

        return pdfGenerator.generateVoucherValueDecisionPdf(
            VoucherValueDecisionPdfData(decision, settings, lang)
        )
    }

    fun setType(
        tx: Database.Transaction,
        decisionId: VoucherValueDecisionId,
        type: VoucherValueDecisionType,
        audit: AuditContext,
    ) {
        val decision =
            tx.getVoucherValueDecision(decisionId)
                ?: throw BadRequest("Decision not found with id $decisionId")
        audit
            .add(decision.headOfFamily.id)
            .add(listOfNotNull(decision.partner?.id))
            .add(decision.child.id)
            .observeDate(decision.validFrom)
        if (decision.status != VoucherValueDecisionStatus.DRAFT) {
            throw BadRequest("Can't change type for decision $decisionId")
        }

        tx.setVoucherValueDecisionType(decisionId, type)
    }

    fun planDecisionNotifications(
        tx: Database.Transaction,
        clock: EvakaClock,
        decisions: List<VoucherValueDecisionDetailed>,
    ) {
        val childNames = tx.getPushChildNames(decisions.map { it.child.id })
        decisions.forEach { decision ->
            val recipientIds =
                listOfNotNull(
                    decision.headOfFamily.id,
                    decision.partner
                        ?.takeIf { decision.partnerIsCodebtor == true && !it.ssn.isNullOrBlank() }
                        ?.id,
                )
            asyncJobRunner.plan(
                tx,
                recipientIds.map { recipientId ->
                    AsyncJob.SendNewVoucherValueDecisionEmail(
                        decisionId = decision.id,
                        recipientId = recipientId,
                    )
                },
                runAt = clock.now(),
            )
            recipientIds.forEach { recipientId ->
                citizenPushNotifications.plan(
                    tx,
                    clock.now(),
                    recipientId,
                    CitizenPushNotification.VoucherValueDecision(
                        decisionId = decision.id,
                        childName = childNames.getValue(decision.child.id),
                        unitName = decision.placement.unit.name,
                    ),
                )
            }
        }
    }

    fun runSendNewVoucherValueDecisionEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendNewVoucherValueDecisionEmail,
    ) {
        val voucherValueDecisionId = msg.decisionId
        val decision =
            db.read { tx -> tx.getVoucherValueDecision(voucherValueDecisionId) }
                ?: throw NotFound("Decision not found")

        // Backwards compatibility: recipientId was added to the job later, so if it's null, we
        // default to the head of family.
        // Can be removed after a while when we are sure all old jobs have been processed.
        val recipientId = msg.recipientId ?: decision.headOfFamily.id

        logger.info {
            "Sending voucher value decision emails for (decisionId: $voucherValueDecisionId)"
        }

        // simplified to get rid of superfluous language requirement
        val fromAddress = emailEnv.sender(Language.fi)
        val content =
            emailMessageProvider.financeDecisionNotification(
                FinanceDecisionType.VOUCHER_VALUE_DECISION
            )
        Email.create(
                db,
                recipientId,
                NotificationCategory.DECISION_NOTIFICATION,
                fromAddress,
                content,
                "$voucherValueDecisionId - $recipientId",
            )
            ?.also { emailClient.send(it) }

        logger.info {
            "Successfully sent voucher value decision email (id: $voucherValueDecisionId)."
        }
    }

    private enum class FileNameType {
        DISPLAY_NAME,
        FILE_NAME,
    }

    private fun calculateDecisionFileName(
        decision: VoucherValueDecisionDetailed,
        lang: OfficialLanguage,
        type: FileNameType,
    ): String {
        val prefix =
            if (lang == OfficialLanguage.SV) "Beslut_om_servicecedels_värde"
            else "Varhaiskasvatuksen_arvopäätös"
        return when (type) {
            FileNameType.DISPLAY_NAME -> "$prefix.pdf"
            FileNameType.FILE_NAME ->
                "${prefix}_${decision.decisionNumber ?: ""}_${decision.validFrom}.pdf"
        }
    }
}

@IgnorableReturnValue
internal fun AuditContext.addDecision(decision: VoucherValueDecision): AuditContext =
    add(decision.headOfFamilyId)
        .add(listOfNotNull(decision.partnerId))
        .add(decision.child.id)
        .observeDate(decision.validFrom)
