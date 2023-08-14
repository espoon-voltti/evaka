// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.decision.DecisionSendAddress
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
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.pis.getEmployeesByRoles
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.sficlient.SfiMessagesClient
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.UserRole
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

const val assistanceNeedDecisionsBucketPrefix = "assistance-need-decisions/"

@Component
class AssistanceNeedDecisionService(
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val pdfGenerator: PdfGenerator,
    private val documentClient: DocumentService,
    private val templateProvider: ITemplateProvider,
    private val messageProvider: IMessageProvider,
    private val sfiClient: SfiMessagesClient,
    bucketEnv: BucketEnv,
    private val emailEnv: EmailEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    private val bucket = bucketEnv.data

    init {
        asyncJobRunner.registerHandler(::runSendAssistanceNeedDecisionEmail)
        asyncJobRunner.registerHandler(::runCreateAssistanceNeedDecisionPdf)
        asyncJobRunner.registerHandler(::runSendSfiDecision)
    }

    fun runSendAssistanceNeedDecisionEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendAssistanceNeedDecisionEmail
    ) {
        this.sendDecisionEmail(db, msg.decisionId, clock.today())
        logger.info { "Successfully sent assistance need decision email (id: ${msg.decisionId})." }
    }

    fun sendDecisionEmail(
        dbc: Database.Connection,
        decisionId: AssistanceNeedDecisionId,
        today: LocalDate
    ) {
        val decision = dbc.read { tx -> tx.getAssistanceNeedDecisionById(decisionId) }

        if (decision.child?.id == null) {
            throw IllegalStateException(
                "Assistance need decision must have a child associated with it"
            )
        }

        logger.info { "Sending assistance need decision email (decisionId: $decision)" }

        val guardians =
            dbc.read { tx -> tx.getChildGuardiansAndFosterParents(decision.child.id, today) }

        val language =
            when (decision.language) {
                AssistanceNeedDecisionLanguage.SV -> Language.sv
                else -> Language.fi
            }
        val fromAddress = emailEnv.applicationReceivedSender(language)
        val content = emailMessageProvider.assistanceNeedDecisionNotification(language)

        guardians.forEach { guardianId ->
            Email.create(
                    dbc,
                    guardianId,
                    EmailMessageType.DECISION_NOTIFICATION,
                    fromAddress,
                    content,
                    "$decisionId - $guardianId",
                )
                ?.also { emailClient.send(it) }
        }
    }

    fun runCreateAssistanceNeedDecisionPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.CreateAssistanceNeedDecisionPdf
    ) {
        db.transaction { tx ->
            this.createDecisionPdf(tx, clock, msg.decisionId)
            logger.info {
                "Successfully created assistance need decision pdf (id: ${msg.decisionId})."
            }
        }
    }

    fun runSendSfiDecision(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendAssistanceNeedDecisionSfiMessage
    ) {
        db.transaction { tx ->
            this.createAndSendSfiDecisionPdf(tx, clock, msg.decisionId)
            logger.info {
                "Successfully created assistance need decision pdf for Suomi.fi (id: ${msg.decisionId})."
            }
        }
    }

    fun createDecisionPdf(
        tx: Database.Transaction,
        clock: EvakaClock,
        decisionId: AssistanceNeedDecisionId
    ) {
        val decision = tx.getAssistanceNeedDecisionById(decisionId)

        check(!decision.hasDocument) {
            "Assistance need decision $decisionId has a document key already"
        }

        val pdf = generatePdf(clock.today(), decision)
        val key =
            documentClient
                .upload(
                    bucket,
                    Document(
                        "${assistanceNeedDecisionsBucketPrefix}assistance_need_decision_$decisionId.pdf",
                        pdf,
                        "application/pdf"
                    )
                )
                .key
        tx.updateAssistanceNeedDocumentKey(decision.id, key)
    }

    fun createAndSendSfiDecisionPdf(
        tx: Database.Transaction,
        clock: EvakaClock,
        decisionId: AssistanceNeedDecisionId
    ) {
        val decision = tx.getAssistanceNeedDecisionById(decisionId)

        if (decision.child?.id == null) {
            throw IllegalStateException(
                "Assistance need decision has to be associated with a child"
            )
        }

        val lang =
            if (decision.language == AssistanceNeedDecisionLanguage.SV) DocumentLang.SV
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

                val pdf = generatePdf(clock.today(), decision, sendAddress, guardian)

                val messageHeader =
                    messageProvider.getAssistanceNeedDecisionHeader(lang.messageLang)
                val messageContent =
                    messageProvider.getAssistanceNeedDecisionContent(lang.messageLang)
                val messageId = "${decision.id}_${guardian.id}"

                sfiClient.send(
                    SfiMessage(
                        messageId = messageId,
                        documentId = messageId,
                        documentDisplayName = suomiFiDocumentFileName(decision.language),
                        documentKey = "",
                        documentBucket = "",
                        language = lang.langCode,
                        firstName = guardian.firstName,
                        lastName = guardian.lastName,
                        streetAddress = sendAddress.street,
                        postalCode = sendAddress.postalCode,
                        postOffice = sendAddress.postOffice,
                        ssn = guardian.identity.ssn,
                        messageHeader = messageHeader,
                        messageContent = messageContent
                    ),
                    pdf
                )
            }
    }

    fun getDecisionPdfResponse(
        dbc: Database.Connection,
        decisionId: AssistanceNeedDecisionId
    ): ResponseEntity<Any> {
        val documentKey =
            dbc.read { it.getAssistanceNeedDecisionDocumentKey(decisionId) }
                ?: throw NotFound("No assistance need decision found with ID ($decisionId)")
        return documentClient.responseAttachment(bucket, documentKey, null)
    }

    fun generatePdf(
        sentDate: LocalDate,
        decision: AssistanceNeedDecision,
        sendAddress: DecisionSendAddress? = null,
        guardian: PersonDTO? = null
    ): ByteArray {
        return pdfGenerator.render(
            Page(
                Template(templateProvider.getAssistanceNeedDecisionPath()),
                Context().apply {
                    locale =
                        Locale.Builder().setLanguage(decision.language.name.lowercase()).build()
                    setVariable("decision", decision)
                    setVariable("sentDate", sentDate)
                    setVariable("sendAddress", sendAddress)
                    setVariable("guardian", guardian)
                    setVariable(
                        "hasAssistanceServices",
                        decision.assistanceLevels.contains(
                            AssistanceLevel.ASSISTANCE_SERVICES_FOR_TIME
                        )
                    )
                }
            )
        )
    }

    fun getDecisionMakerOptions(
        tx: Database.Read,
        id: AssistanceNeedDecisionId,
        roles: Set<UserRole>?
    ): List<Employee> {
        val assistanceDecision = tx.getAssistanceNeedDecisionById(id)
        return if (roles == null) {
            tx.getEmployees().sortedBy { it.email }
        } else {
            tx.getEmployeesByRoles(roles, assistanceDecision.selectedUnit?.id)
        }
    }
}

private fun suomiFiDocumentFileName(lang: AssistanceNeedDecisionLanguage) =
    if (lang == AssistanceNeedDecisionLanguage.SV) {
        "Beslut_om_stöd.pdf"
    } else {
        "Päätös_tuesta.pdf"
    }
