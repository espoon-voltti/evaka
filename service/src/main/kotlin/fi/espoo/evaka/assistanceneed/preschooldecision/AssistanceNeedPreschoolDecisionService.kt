// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.preschooldecision

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionLanguage
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.decision.getSendAddress
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.service.DocumentLang
import fi.espoo.evaka.pdfgen.Page
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pdfgen.Template
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IMessageProvider
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
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val pdfGenerator: PdfGenerator,
    private val documentClient: DocumentService,
    private val templateProvider: ITemplateProvider,
    private val messageProvider: IMessageProvider,
    bucketEnv: BucketEnv,
    private val emailEnv: EmailEnv,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    private val bucket = bucketEnv.data

    init {
        asyncJobRunner.registerHandler(::runCreateAssistanceNeedPreschoolDecisionPdf)
        asyncJobRunner.registerHandler(::runSendAssistanceNeedPreschoolDecisionEmail)
        asyncJobRunner.registerHandler(::runSendSfiDecision)
    }

    fun runCreateAssistanceNeedPreschoolDecisionPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.CreateAssistanceNeedPreschoolDecisionPdf
    ) {
        val decisionId = msg.decisionId
        db.transaction { tx ->
            val decision = tx.getAssistanceNeedPreschoolDecisionById(decisionId)
            val endDate =
                tx.getAssistanceNeedPreschoolDecisionsByChildId(decision.child.id)
                    .find { it.id == decisionId }
                    ?.validTo

            check(!decision.hasDocument) {
                "A pdf already exists for the assistance need preschool decision $decisionId"
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

            asyncJobRunner.plan(
                tx,
                listOf(
                    AsyncJob.SendAssistanceNeedPreschoolDecisionEmail(decisionId),
                    AsyncJob.SendAssistanceNeedPreschoolDecisionSfiMessage(decisionId)
                ),
                runAt = clock.now()
            )

            logger.info {
                "Successfully created assistance need preschool decision pdf (id: $decisionId)."
            }
        }
    }

    fun runSendAssistanceNeedPreschoolDecisionEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendAssistanceNeedPreschoolDecisionEmail
    ) {
        val decisionId = msg.decisionId
        val today = clock.today()

        val decision = db.read { tx -> tx.getAssistanceNeedPreschoolDecisionById(decisionId) }

        logger.info { "Sending assistance need preschool decision email (decisionId: $decision)" }

        val language =
            when (decision.form.language) {
                AssistanceNeedDecisionLanguage.SV -> Language.sv
                else -> Language.fi
            }
        val fromAddress = emailEnv.applicationReceivedSender(language)
        val content = emailMessageProvider.assistanceNeedPreschoolDecisionNotification(language)

        val guardians =
            db.read { tx -> tx.getChildGuardiansAndFosterParents(decision.child.id, today) }
        guardians.forEach { guardianId ->
            Email.create(
                    db,
                    guardianId,
                    EmailMessageType.DECISION_NOTIFICATION,
                    fromAddress,
                    content,
                    "$decisionId - $guardianId",
                )
                ?.also { emailClient.send(it) }
        }

        logger.info {
            "Successfully sent assistance need preschool decision email (id: $decisionId)."
        }
    }

    fun runSendSfiDecision(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendAssistanceNeedPreschoolDecisionSfiMessage
    ) {
        val decisionId = msg.decisionId
        db.transaction { tx ->
            val decision = tx.getAssistanceNeedPreschoolDecisionById(decisionId)

            val documentKey =
                tx.getAssistanceNeedPreschoolDecisionDocumentKey(decisionId)
                    ?: throw IllegalStateException(
                        "Assistance need preschool decision pdf has not yet been generated"
                    )

            val lang =
                if (decision.form.language == AssistanceNeedDecisionLanguage.SV) DocumentLang.SV
                else DocumentLang.FI

            tx.getChildGuardiansAndFosterParents(decision.child.id, clock.today())
                .mapNotNull { tx.getPersonById(it) }
                .forEach { guardian ->
                    if (guardian.identity !is ExternalIdentifier.SSN) {
                        logger.info {
                            "Cannot deliver assistance need decision ${decision.id} to guardian via Sfi. SSN is missing."
                        }
                        return@forEach
                    }

                    val sendAddress = getSendAddress(messageProvider, guardian, lang)

                    val messageHeader =
                        messageProvider.getAssistanceNeedPreschoolDecisionHeader(lang.messageLang)
                    val messageContent =
                        messageProvider.getAssistanceNeedPreschoolDecisionContent(lang.messageLang)
                    val messageId = "${decision.id}_${guardian.id}"

                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.SendMessage(
                                SfiMessage(
                                    messageId = messageId,
                                    documentId = messageId,
                                    documentDisplayName =
                                        suomiFiDocumentFileName(decision.form.language),
                                    documentKey = documentKey,
                                    documentBucket = bucket,
                                    firstName = guardian.firstName,
                                    lastName = guardian.lastName,
                                    streetAddress = sendAddress.street,
                                    postalCode = sendAddress.postalCode,
                                    postOffice = sendAddress.postOffice,
                                    ssn = guardian.identity.ssn,
                                    messageHeader = messageHeader,
                                    messageContent = messageContent
                                )
                            )
                        ),
                        runAt = clock.now()
                    )
                }

            logger.info {
                "Successfully scheduled assistance need preschool decision pdf for Suomi.fi sending (id: $decisionId)."
            }
        }
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

private fun suomiFiDocumentFileName(lang: AssistanceNeedDecisionLanguage) =
    if (lang == AssistanceNeedDecisionLanguage.SV) {
        "Beslut_om_stöd.pdf"
    } else {
        "Päätös_tuesta.pdf"
    }
