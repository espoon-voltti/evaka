// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.EmailEnv
import evaka.core.EvakaEnv
import evaka.core.caseprocess.CaseProcessMetadataService
import evaka.core.caseprocess.CaseProcessState
import evaka.core.caseprocess.getCaseProcessByFeeDecisionId
import evaka.core.caseprocess.insertCaseProcess
import evaka.core.caseprocess.insertCaseProcessHistoryRow
import evaka.core.daycare.domain.Language
import evaka.core.decision.DecisionSendAddress
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.invoicing.data.approveFeeDecisionDraftsForSending
import evaka.core.invoicing.data.deleteFeeDecisions
import evaka.core.invoicing.data.findFeeDecisionsForHeadOfFamily
import evaka.core.invoicing.data.getDetailedFeeDecisionsByIds
import evaka.core.invoicing.data.getFeeDecision
import evaka.core.invoicing.data.getFeeDecisionsByIds
import evaka.core.invoicing.data.lockFeeDecisions
import evaka.core.invoicing.data.lockFeeDecisionsForHeadOfFamily
import evaka.core.invoicing.data.removeFeeDecisionIgnore
import evaka.core.invoicing.data.setFeeDecisionProcessId
import evaka.core.invoicing.data.setFeeDecisionSent
import evaka.core.invoicing.data.setFeeDecisionToIgnored
import evaka.core.invoicing.data.setFeeDecisionType
import evaka.core.invoicing.data.setFeeDecisionWaitingForManualSending
import evaka.core.invoicing.data.updateFeeDecisionDocumentKey
import evaka.core.invoicing.data.updateFeeDecisionStatusAndDates
import evaka.core.invoicing.domain.FeeDecisionDetailed
import evaka.core.invoicing.domain.FeeDecisionStatus.DRAFT
import evaka.core.invoicing.domain.FeeDecisionStatus.IGNORED
import evaka.core.invoicing.domain.FeeDecisionStatus.SENT
import evaka.core.invoicing.domain.FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING
import evaka.core.invoicing.domain.FeeDecisionStatus.WAITING_FOR_SENDING
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.FinanceDecisionType
import evaka.core.invoicing.domain.isRetroactive
import evaka.core.invoicing.domain.updateEndDatesOrAnnulConflictingDecisions
import evaka.core.invoicing.validateFinanceDecisionHandler
import evaka.core.pdfgen.PdfGenerator
import evaka.core.pis.EmailMessageType
import evaka.core.s3.DocumentKey
import evaka.core.s3.DocumentService
import evaka.core.setting.getSettings
import evaka.core.sficlient.SentSfiMessage
import evaka.core.sficlient.SfiMessage
import evaka.core.sficlient.storeSentSfiMessage
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.EmployeeId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.message.IMessageProvider
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionService(
    private val pdfGenerator: PdfGenerator,
    private val documentClient: DocumentService,
    private val messageProvider: IMessageProvider,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val env: EvakaEnv,
    private val emailEnv: EmailEnv,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailClient: EmailClient,
    featureConfig: FeatureConfig,
) {
    private val metadata = CaseProcessMetadataService(featureConfig)

    init {
        asyncJobRunner.registerHandler(::runSendNewFeeDecisionEmail)
    }

    fun confirmDrafts(
        tx: Database.Transaction,
        user: AuthenticatedUser.Employee,
        ids: List<FeeDecisionId>,
        confirmDateTime: HelsinkiDateTime,
        decisionHandlerId: EmployeeId?,
        alwaysUseDaycareFinanceDecisionHandler: Boolean,
    ): List<FeeDecisionId> {
        tx.lockFeeDecisions(ids)
        val decisions = tx.getFeeDecisionsByIds(ids)
        if (decisions.isEmpty()) return listOf()
        val notDrafts = decisions.filterNot { it.status == DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some fee decisions were not drafts")
        }
        val today = confirmDateTime.toLocalDate()
        val approvedAt = confirmDateTime
        val lastPossibleDecisionValidFromDate =
            today.plusDays(env.nrOfDaysFeeDecisionCanBeSentInAdvance)
        val decisionsNotValidForConfirmation =
            decisions.filter { it.validFrom > lastPossibleDecisionValidFromDate }
        if (decisionsNotValidForConfirmation.isNotEmpty()) {
            throw BadRequest(
                "Some of the fee decisions are not valid yet",
                "feeDecisions.confirmation.tooFarInFuture",
            )
        }
        decisionHandlerId?.let { validateFinanceDecisionHandler(tx, it) }

        val (conflicts, waitingForSending) =
            decisions
                .flatMap {
                    tx.lockFeeDecisionsForHeadOfFamily(it.headOfFamilyId)
                    val ownDecisions =
                        tx.findFeeDecisionsForHeadOfFamily(
                            it.headOfFamilyId,
                            it.validDuring,
                            listOf(WAITING_FOR_SENDING, WAITING_FOR_MANUAL_SENDING, SENT),
                        )
                    val partnerDecisions =
                        it.partnerId?.let { partnerId ->
                            tx.findFeeDecisionsForHeadOfFamily(
                                partnerId,
                                it.validDuring,
                                listOf(WAITING_FOR_SENDING, WAITING_FOR_MANUAL_SENDING, SENT),
                            )
                        } ?: listOf()
                    ownDecisions + partnerDecisions
                }
                .distinctBy { it.id }
                .filter { !ids.contains(it.id) }
                .partition {
                    it.status != WAITING_FOR_SENDING && it.status != WAITING_FOR_MANUAL_SENDING
                }

        if (waitingForSending.isNotEmpty()) {
            val logMeta = mapOf("feeDecisionIds" to waitingForSending.map { it.id })
            logger.info(logMeta) {
                "Warning: when creating fee decisions, skipped ${waitingForSending.size} fee decisions because head of family had overlapping fee decisions waiting for sending"
            }
        }

        val remainingDecisions =
            decisions.filter { fd ->
                waitingForSending.none { wfd -> wfd.headOfFamilyId == fd.headOfFamilyId }
            }

        val remainingConflicts =
            conflicts.filter { conflict ->
                waitingForSending.none { wfd -> wfd.headOfFamilyId == conflict.headOfFamilyId }
            }

        val updatedConflicts =
            updateEndDatesOrAnnulConflictingDecisions(remainingDecisions, remainingConflicts)
        tx.updateFeeDecisionStatusAndDates(updatedConflicts)

        val (emptyDecisions, validDecisions) =
            remainingDecisions.partition { it.children.isEmpty() }

        tx.deleteFeeDecisions(emptyDecisions.map { it.id })

        val (retroactiveDecisions, otherValidDecisions) =
            validDecisions.partition { isRetroactive(it.validFrom, today) }

        tx.approveFeeDecisionDraftsForSending(
            retroactiveDecisions.map { it.id },
            approvedBy = user.id,
            isRetroactive = true,
            approvedAt = approvedAt,
            decisionHandlerId = decisionHandlerId,
            alwaysUseDaycareFinanceDecisionHandler = alwaysUseDaycareFinanceDecisionHandler,
        )
        tx.approveFeeDecisionDraftsForSending(
            otherValidDecisions.map { it.id },
            approvedBy = user.id,
            approvedAt = approvedAt,
            decisionHandlerId = decisionHandlerId,
            alwaysUseDaycareFinanceDecisionHandler = alwaysUseDaycareFinanceDecisionHandler,
        )

        val process = metadata.getProcessParams(ArchiveProcessType.FEE_DECISION, today.year)
        if (process != null) {
            // TODO: Could be heavy. Does this need to be moved into async jobs?
            validDecisions.forEach { decision ->
                val processId = tx.insertCaseProcess(process).id
                tx.insertCaseProcessHistoryRow(
                    processId = processId,
                    state = CaseProcessState.INITIAL,
                    now = decision.created, // retroactive initial state
                    userId = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )
                tx.insertCaseProcessHistoryRow(
                    processId = processId,
                    state = CaseProcessState.DECIDING,
                    now = confirmDateTime,
                    userId = user.evakaUserId,
                )
                tx.setFeeDecisionProcessId(decision.id, processId)
            }
        }

        return validDecisions.map { it.id }
    }

    fun ignoreDrafts(tx: Database.Transaction, ids: List<FeeDecisionId>, today: LocalDate) {
        tx.getFeeDecisionsByIds(ids)
            .map { decision ->
                if (decision.status != DRAFT) {
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
            .forEach { tx.setFeeDecisionToIgnored(it.id) }
    }

    fun unignoreDrafts(tx: Database.Transaction, ids: List<FeeDecisionId>): Set<PersonId> {
        return tx.getFeeDecisionsByIds(ids)
            .map { decision ->
                if (decision.status != IGNORED) {
                    throw BadRequest("Error with decision ${decision.id}: not ignored")
                }
                decision
            }
            .map {
                tx.removeFeeDecisionIgnore(it.id)
                it.headOfFamilyId
            }
            .toSet()
    }

    private fun getDecisionLanguage(decision: FeeDecisionDetailed): OfficialLanguage {
        val defaultLanguage =
            if (decision.headOfFamily.language == "sv") OfficialLanguage.SV else OfficialLanguage.FI

        val youngestChildUnitLanguage =
            decision.children.maxByOrNull { it.child.dateOfBirth }?.placementUnit?.language

        return if (youngestChildUnitLanguage == "sv") OfficialLanguage.SV else defaultLanguage
    }

    fun createFeeDecisionPdf(tx: Database.Transaction, id: FeeDecisionId) {
        val decision =
            tx.getFeeDecision(id) ?: throw NotFound("No fee decision found with ID ($id)")

        if (!decision.documentKey.isNullOrBlank()) {
            throw Conflict("Fee decision $id has document key already!")
        }

        val settings = tx.getSettings()
        val lang = getDecisionLanguage(decision)

        val pdfByteArray =
            pdfGenerator.generateFeeDecisionPdf(FeeDecisionPdfData(decision, settings, lang))
        val documentKey =
            documentClient
                .upload(DocumentKey.FeeDecision(decision.id, lang), pdfByteArray, "application/pdf")
                .key
        tx.updateFeeDecisionDocumentKey(decision.id, documentKey)
    }

    fun sendDecision(tx: Database.Transaction, clock: EvakaClock, id: FeeDecisionId): Boolean {
        val decision =
            tx.getFeeDecision(id) ?: throw NotFound("No fee decision found with given ID ($id)")

        if (decision.status != WAITING_FOR_SENDING) {
            error("Cannot send fee decision ${decision.id} - has status ${decision.status}")
        }

        if (decision.documentKey == null) {
            error("Cannot send fee decision ${decision.id} - missing document key")
        }

        if (decision.requiresManualSending) {
            tx.setFeeDecisionWaitingForManualSending(decision.id)
            return false
        }

        val recipient = decision.headOfFamily
        val lang = getDecisionLanguage(decision)

        // If address is missing (restricted info enabled), use the financial handling address
        // instead
        val sendAddress =
            DecisionSendAddress.fromPerson(recipient)
                ?: messageProvider.getDefaultFinancialDecisionAddress(lang)

        val feeDecisionDisplayName =
            calculateDecisionFileName(decision, lang, FileNameType.DISPLAY_NAME)

        val documentLocation = documentClient.locate(DocumentKey.FeeDecision(decision.documentKey))

        val messageId =
            tx.storeSentSfiMessage(
                SentSfiMessage(guardianId = decision.headOfFamily.id, feeDecisionId = decision.id)
            )

        val message =
            SfiMessage(
                messageId = messageId,
                documentId = decision.id.toString(),
                documentDisplayName = feeDecisionDisplayName,
                documentBucket = documentLocation.bucket,
                documentKey = documentLocation.key,
                firstName = recipient.firstName,
                lastName = recipient.lastName,
                streetAddress = sendAddress.street,
                postalCode = sendAddress.postalCode,
                postOffice = sendAddress.postOffice,
                ssn = recipient.ssn!!,
                messageHeader = messageProvider.getFeeDecisionHeader(lang),
                messageContent = messageProvider.getFeeDecisionContent(lang),
            )

        logger.info { "Sending fee decision as suomi.fi message ${message.documentId}" }

        asyncJobRunner.plan(tx, listOf(AsyncJob.SendMessage(message)), runAt = clock.now())
        asyncJobRunner.plan(
            tx,
            listOf(AsyncJob.SendNewFeeDecisionEmail(decisionId = decision.id)),
            runAt = clock.now(),
        )

        setSentAndUpdateProcess(
            tx,
            clock,
            AuthenticatedUser.SystemInternalUser,
            listOf(decision.id),
        )

        return true
    }

    fun setManuallySent(
        tx: Database.Transaction,
        clock: EvakaClock,
        user: AuthenticatedUser,
        ids: List<FeeDecisionId>,
    ) {
        val decisions = tx.getDetailedFeeDecisionsByIds(ids)
        if (decisions.any { it.status != WAITING_FOR_MANUAL_SENDING }) {
            throw BadRequest("Some decisions were not supposed to be sent manually")
        }
        setSentAndUpdateProcess(tx, clock, user, ids)
    }

    fun setSentAndUpdateProcess(
        tx: Database.Transaction,
        clock: EvakaClock,
        user: AuthenticatedUser,
        ids: List<FeeDecisionId>,
    ) {
        val now = clock.now()
        // here the number of ids is always small so no need to optimize currently
        ids.forEach { id ->
            tx.getCaseProcessByFeeDecisionId(id)?.let { process ->
                tx.insertCaseProcessHistoryRow(
                    processId = process.id,
                    state = CaseProcessState.COMPLETED,
                    now = now,
                    userId = user.evakaUserId,
                )
            }
        }
        tx.setFeeDecisionSent(clock, ids)
    }

    fun getFeeDecisionPdfResponse(
        dbc: Database.Connection,
        decisionId: FeeDecisionId,
    ): ResponseEntity<Any> {
        val (documentKey, fileName) =
            dbc.read { tx ->
                val decision = tx.getFeeDecision(decisionId) ?: throw NotFound("Decision not found")
                if (decision.documentKey == null)
                    throw NotFound("Document key not found for decision $decisionId")
                val lang = getDecisionLanguage(decision)
                DocumentKey.FeeDecision(decision.documentKey) to
                    calculateDecisionFileName(decision, lang, FileNameType.FILE_NAME)
            }
        val documentLocation = documentClient.locate(documentKey)
        return documentClient.responseAttachment(documentLocation, fileName)
    }

    fun setType(tx: Database.Transaction, decisionId: FeeDecisionId, type: FeeDecisionType) {
        val decision =
            tx.getFeeDecision(decisionId)
                ?: throw BadRequest("Decision not found with id $decisionId")
        if (decision.status != DRAFT) {
            throw BadRequest("Can't change type for decision $decisionId")
        }

        tx.setFeeDecisionType(decisionId, type)
    }

    fun runSendNewFeeDecisionEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendNewFeeDecisionEmail,
    ) {
        val feeDecisionId = msg.decisionId
        val decision =
            db.read { tx -> tx.getFeeDecision(feeDecisionId) }
                ?: throw NotFound("Decision not found")

        logger.info { "Sending fee decision emails for (decisionId: $feeDecisionId)" }

        // simplified to get rid of superfluous language requirement
        val fromAddress = emailEnv.sender(Language.fi)
        val content =
            emailMessageProvider.financeDecisionNotification(FinanceDecisionType.FEE_DECISION)
        Email.create(
                db,
                decision.headOfFamily.id,
                EmailMessageType.DECISION_NOTIFICATION,
                fromAddress,
                content,
                "$feeDecisionId - ${decision.headOfFamily.id}",
            )
            ?.also { emailClient.send(it) }

        logger.info { "Successfully sent fee decision email (id: $feeDecisionId)." }
    }

    private enum class FileNameType {
        DISPLAY_NAME,
        FILE_NAME,
    }

    private fun calculateDecisionFileName(
        decision: FeeDecisionDetailed,
        lang: OfficialLanguage,
        type: FileNameType,
    ): String {
        val prefix =
            if (lang == OfficialLanguage.SV) "Beslut_om_avgift_för_småbarnspedagogik"
            else "Varhaiskasvatuksen_maksupäätös"
        return when (type) {
            FileNameType.DISPLAY_NAME -> "$prefix.pdf"
            FileNameType.FILE_NAME ->
                "${prefix}_${decision.decisionNumber ?: ""}_${decision.validDuring.start}.pdf"
        }
    }
}
