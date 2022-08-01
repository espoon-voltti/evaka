// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.template.ITemplateProvider
import fi.espoo.voltti.pdfgen.PDFService
import fi.espoo.voltti.pdfgen.Page
import fi.espoo.voltti.pdfgen.Template
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import java.time.LocalDate
import java.util.Locale

private val logger = KotlinLogging.logger {}

const val assistanceNeedDecisionsBucketPrefix = "assistance-need-decisions/"

@Component
class AssistanceNeedDecisionService(
    private val emailClient: IEmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val pdfService: PDFService,
    private val documentClient: DocumentService,
    private val templateProvider: ITemplateProvider,
    bucketEnv: BucketEnv,
    emailEnv: EmailEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    private val senderAddressFi = emailEnv.applicationReceivedSenderAddressFi
    private val senderNameFi = emailEnv.applicationReceivedSenderNameFi
    private val senderAddressSv = emailEnv.applicationReceivedSenderAddressSv
    private val senderNameSv = emailEnv.applicationReceivedSenderNameSv

    private val bucket = bucketEnv.data

    init {
        asyncJobRunner.registerHandler(::runSendAssistanceNeedDecisionEmail)
        asyncJobRunner.registerHandler(::runCreateAssistanceNeedDecisionPdf)
    }

    fun runSendAssistanceNeedDecisionEmail(db: Database.Connection, clock: EvakaClock, msg: AsyncJob.SendAssistanceNeedDecisionEmail) {
        db.transaction { tx ->
            this.sendDecisionEmail(tx, msg.decisionId)
            logger.info { "Successfully sent assistance need decision email (id: ${msg.decisionId})." }
        }
    }

    fun sendDecisionEmail(tx: Database.Transaction, decisionId: AssistanceNeedDecisionId) {
        val decision = tx.getAssistanceNeedDecisionById(decisionId)

        if (decision.child?.id == null) {
            throw IllegalStateException("Assistance need decision must have a child associated with it")
        }

        logger.info { "Sending assistance need decision email (decisionId: $decision)" }

        val fromAddress = when (decision.language) {
            AssistanceNeedDecisionLanguage.SV -> "$senderNameSv <$senderAddressSv>"
            else -> "$senderNameFi <$senderAddressFi>"
        }

        tx.getChildGuardians(decision.child.id).map {
            Pair(it, tx.getPersonById(it)?.email)
        }.toMap().forEach { (guardianId, email) ->
            if (email != null) {
                emailClient.sendEmail(
                    "$decisionId - $guardianId",
                    email,
                    fromAddress,
                    emailMessageProvider.getAssistanceNeedDecisionEmailSubject(),
                    emailMessageProvider.getAssistanceNeedDecisionEmailHtml(decision.child.id, decision.id),
                    emailMessageProvider.getAssistanceNeedDecisionEmailText(decision.child.id, decision.id)
                )
            }
        }
    }

    fun runCreateAssistanceNeedDecisionPdf(db: Database.Connection, clock: EvakaClock, msg: AsyncJob.CreateAssistanceNeedDecisionPdf) {
        db.transaction { tx ->
            this.createDecisionPdf(tx, msg.decisionId)
            logger.info { "Successfully created assistance need decision pdf (id: ${msg.decisionId})." }
        }
    }

    fun createDecisionPdf(tx: Database.Transaction, decisionId: AssistanceNeedDecisionId) {
        val decision = tx.getAssistanceNeedDecisionById(decisionId)

        check(!decision.hasDocument) { "Assistance need decision $decisionId has a document key already" }

        val pdf = generatePdf(decision)
        val key = documentClient.upload(
            bucket,
            Document("${assistanceNeedDecisionsBucketPrefix}assistance_need_decision_$decisionId.pdf", pdf, "application/pdf")
        ).key
        tx.updateAssistanceNeedDocumentKey(decision.id, key)
    }

    fun getDecisionPdfResponse(dbc: Database.Connection, decisionId: AssistanceNeedDecisionId): ResponseEntity<Any> {
        val documentKey = dbc.read { it.getAssistanceNeedDecisionDocumentKey(decisionId) }
            ?: throw NotFound("No assistance need decision found with ID ($decisionId)")
        return documentClient.responseAttachment(bucket, documentKey, null)
    }

    fun generatePdf(decision: AssistanceNeedDecision): ByteArray {
        return pdfService.render(
            Page(
                Template(templateProvider.getAssistanceNeedDecisionPath()),
                Context().apply {
                    locale = Locale.Builder().setLanguage(decision.language.name.lowercase()).build()
                    setVariable("decision", decision)
                    setVariable("sentDate", LocalDate.now())
                }
            )
        )
    }
}
