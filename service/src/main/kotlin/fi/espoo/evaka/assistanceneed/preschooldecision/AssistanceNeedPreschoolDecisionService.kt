// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.preschooldecision

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionLanguage
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pdfgen.Page
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pdfgen.Template
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.getChildGuardians
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.template.ITemplateProvider
import java.time.LocalDate
import java.util.Locale
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context

private val logger = KotlinLogging.logger {}

const val assistanceNeedDecisionsBucketPrefix = "assistance-need-preschool-decisions/"

@Component
class AssistanceNeedPreschoolDecisionService(
    private val emailClient: IEmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val pdfGenerator: PdfGenerator,
    private val documentClient: DocumentService,
    private val templateProvider: ITemplateProvider,
    bucketEnv: BucketEnv,
    private val emailEnv: EmailEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    private val bucket = bucketEnv.data

    init {
        asyncJobRunner.registerHandler(::runSendAssistanceNeedPreschoolDecisionEmail)
        asyncJobRunner.registerHandler(::runCreateAssistanceNeedPreschoolDecisionPdf)
    }

    fun runSendAssistanceNeedPreschoolDecisionEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendAssistanceNeedPreschoolDecisionEmail
    ) {
        db.transaction { tx ->
            this.sendDecisionEmail(tx, msg.decisionId)
            logger.info {
                "Successfully sent assistance need preschool decision email (id: ${msg.decisionId})."
            }
        }
    }

    fun sendDecisionEmail(tx: Database.Transaction, decisionId: AssistanceNeedPreschoolDecisionId) {
        val decision = tx.getAssistanceNeedPreschoolDecisionById(decisionId)

        logger.info { "Sending assistance need decision email (decisionId: $decision)" }

        val language =
            when (decision.form.language) {
                AssistanceNeedDecisionLanguage.SV -> Language.sv
                else -> Language.fi
            }
        val fromAddress = emailEnv.applicationReceivedSender(language)
        val content = emailMessageProvider.assistanceNeedPreschoolDecisionNotification(language)

        tx.getChildGuardians(decision.child.id)
            .associateWith { tx.getPersonById(it)?.email }
            .forEach { (guardianId, toAddress) ->
                if (toAddress != null) {
                    emailClient.sendEmail(
                        "$decisionId - $guardianId",
                        toAddress,
                        fromAddress,
                        content
                    )
                }
            }
    }

    fun runCreateAssistanceNeedPreschoolDecisionPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.CreateAssistanceNeedPreschoolDecisionPdf
    ) {
        db.transaction { tx ->
            this.createDecisionPdf(tx, clock, msg.decisionId)
            logger.info {
                "Successfully created assistance need decision pdf (id: ${msg.decisionId})."
            }
        }
    }

    fun createDecisionPdf(
        tx: Database.Transaction,
        clock: EvakaClock,
        decisionId: AssistanceNeedPreschoolDecisionId
    ) {
        val decision = tx.getAssistanceNeedPreschoolDecisionById(decisionId)
        val endDate =
            tx.getAssistanceNeedPreschoolDecisionsByChildId(decision.child.id)
                .find { it.id == decisionId }
                ?.validTo

        check(!decision.hasDocument) {
            "Assistance need preschool decision $decisionId already has a document key"
        }

        val pdf = generatePdf(clock.today(), decision, endDate)

        val key =
            documentClient
                .upload(
                    bucket,
                    Document(
                        "${assistanceNeedDecisionsBucketPrefix}assistance_need_preschool_decision_$decisionId.pdf",
                        pdf,
                        "application/pdf"
                    )
                )
                .key

        tx.updateAssistanceNeedPreschoolDocumentKey(decision.id, key)
    }

    fun getDecisionPdfResponse(
        dbc: Database.Connection,
        decisionId: AssistanceNeedPreschoolDecisionId
    ): ResponseEntity<Any> {
        val documentKey =
            dbc.read { it.getAssistanceNeedPreschoolDecisionDocumentKey(decisionId) }
                ?: throw NotFound("No assistance need decision found with ID ($decisionId)")
        return documentClient.responseAttachment(bucket, documentKey, null)
    }

    fun generatePdf(
        sentDate: LocalDate,
        decision: AssistanceNeedPreschoolDecision,
        validTo: LocalDate?
    ): ByteArray {
        return pdfGenerator.render(
            Page(
                Template(templateProvider.getAssistanceNeedPreschoolDecisionPath()),
                Context().apply {
                    locale =
                        Locale.Builder()
                            .setLanguage(decision.form.language.name.lowercase())
                            .build()
                    setVariable("decision", decision)
                    setVariable("sentDate", sentDate)
                    setVariable("validTo", validTo)
                }
            )
        )
    }
}
