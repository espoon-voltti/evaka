// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getUnitManager
import fi.espoo.evaka.daycare.service.DaycareManager
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentLocation
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyDecisionCreated
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IEvakaMessageClient
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.message.SuomiFiMessage
import fi.espoo.evaka.shared.message.langWithDefault
import fi.espoo.evaka.shared.template.ITemplateProvider
import fi.espoo.voltti.pdfgen.PDFService
import fi.espoo.voltti.pdfgen.Page
import fi.espoo.voltti.pdfgen.Template
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import java.net.URI
import java.util.Locale
import java.util.UUID

val logger = KotlinLogging.logger { }

@Service
class DecisionService(
    @Value("\${fi.espoo.voltti.document.bucket.daycaredecision}")
    private val decisionBucket: String,
    private val personService: PersonService,
    private val s3Client: DocumentService,
    private val templateProvider: ITemplateProvider,
    private val pdfService: PDFService,
    private val messageProvider: IMessageProvider,
    private val evakaMessageClient: IEvakaMessageClient,
    private val asyncJobRunner: AsyncJobRunner
) {
    fun finalizeDecisions(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        applicationId: UUID,
        sendAsMessage: Boolean
    ): List<UUID> {
        val decisionIds = tx.finalizeDecisions(applicationId)
        asyncJobRunner.plan(tx, decisionIds.map { NotifyDecisionCreated(it, user, sendAsMessage) })
        return decisionIds
    }

    fun createDecisionPdfs(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        decisionId: UUID
    ) {
        val decision = tx.getDecision(decisionId) ?: throw NotFound("No decision with id: $decisionId")
        val decisionLanguage = determineDecisionLanguage(decision, tx)
        val application = tx.fetchApplicationDetails(decision.applicationId)
            ?: throw NotFound("Application ${decision.applicationId} was not found")
        val guardian = personService.getUpToDatePerson(tx, user, application.guardianId)
            ?: error("Guardian not found with id: ${application.guardianId}")
        val child = personService.getUpToDatePerson(tx, user, application.childId)
            ?: error("Child not found with id: ${application.childId}")
        val unitManager = tx.getUnitManager(decision.unit.id)
            ?: throw NotFound("Daycare manager not found with daycare id: ${decision.unit.id}.")
        val guardianDecisionURI = createAndUploadDecision(
            decision, application, guardian, child, decisionLanguage, unitManager
        )

        tx.updateDecisionGuardianDocumentUri(decisionId, guardianDecisionURI)

        if (application.otherGuardianId != null &&
            isDecisionForSecondGuardianRequired(decision, application, tx)
        ) {
            val otherGuardian = personService.getUpToDatePerson(tx, user, application.otherGuardianId)
                ?: throw NotFound("Other guardian not found with id: ${application.otherGuardianId}")

            val otherGuardianDecisionURI = createAndUploadDecision(
                decision, application, otherGuardian, child, decisionLanguage, unitManager
            )
            tx.updateDecisionOtherGuardianDocumentUri(
                decisionId,
                otherGuardianDecisionURI
            )
        }
    }

    private fun createAndUploadDecision(
        decision: Decision,
        application: ApplicationDetails,
        guardian: PersonDTO,
        child: PersonDTO,
        decisionLanguage: String,
        unitManager: DaycareManager
    ): URI {
        val decisionBytes = createDecisionPdf(
            messageProvider,
            templateProvider,
            pdfService,
            decision,
            guardian,
            child,
            application.transferApplication,
            decisionLanguage,
            unitManager
        )

        return uploadPdfToS3(
            decisionBucket,
            constructObjectKey(decision, guardian, decisionLanguage),
            decisionBytes
        ).uri
    }

    private fun isDecisionForSecondGuardianRequired(
        decision: Decision,
        application: ApplicationDetails,
        tx: Database.Transaction
    ) = decision.type != DecisionType.CLUB &&
        application.otherGuardianId != null &&
        !personService.personsLiveInTheSameAddress(
            tx,
            application.guardianId,
            application.otherGuardianId
        )

    private fun determineDecisionLanguage(
        decision: Decision,
        tx: Database.Transaction
    ): String {
        return if (decision.type == DecisionType.CLUB) "fi"
        else tx.getDecisionLanguage(decision.id)
    }

    private fun constructObjectKey(
        decision: Decision,
        guardian: PersonDTO,
        lang: String
    ): String {
        return when (decision.type) {
            DecisionType.CLUB -> "clubdecision"
            DecisionType.DAYCARE, DecisionType.DAYCARE_PART_TIME -> "daycaredecision"
            DecisionType.PRESCHOOL -> "preschooldecision"
            DecisionType.PRESCHOOL_DAYCARE -> "connectingdaycaredecision"
            DecisionType.PREPARATORY_EDUCATION -> "preparatorydecision"
        }.let { "${it}_${decision.id}_${guardian.id}_$lang" }
    }

    private fun uploadPdfToS3(bucket: String, key: String, document: ByteArray): DocumentLocation =
        s3Client.upload(
            bucket,
            DocumentWrapper(name = key, bytes = document),
            "application/pdf"
        ).also {
            logger.debug { "PDF (object name: $key) uploaded to S3 with URL ${it.uri}." }
        }

    fun deliverDecisionToGuardians(tx: Database.Transaction, decisionId: UUID) {
        val decision = tx.getDecision(decisionId) ?: throw NotFound("No decision with id: $decisionId")

        val applicationId = decision.applicationId
        val application = tx.fetchApplicationDetails(applicationId)
            ?: throw NotFound("Application $applicationId was not found")

        val currentVtjGuardianIds = personService.getGuardians(tx, AuthenticatedUser.SystemInternalUser, decision.childId).map { person -> person.id }

        val applicationGuardian = tx.getPersonById(application.guardianId)
            ?: error("Guardian not found with id: ${application.guardianId}")

        if (currentVtjGuardianIds.contains(applicationGuardian.id)) {
            deliverDecisionToGuardian(tx, decision, applicationGuardian, decision.documentUri.toString())
        } else {
            logger.warn("Skipping sending decision $decisionId to application guardian ${applicationGuardian.id} - not a current VTJ guardian")
        }

        if (application.otherGuardianId != null &&
            !decision.otherGuardianDocumentUri.isNullOrBlank() &&
            !applicationGuardian.restrictedDetailsEnabled
        ) {
            val otherGuardian = tx.getPersonById(application.otherGuardianId)
                ?: error("Other guardian not found with id: ${application.otherGuardianId}")

            if (currentVtjGuardianIds.contains(application.otherGuardianId)) {
                deliverDecisionToGuardian(
                    tx,
                    decision,
                    otherGuardian,
                    decision.otherGuardianDocumentUri.toString()
                )
            } else {
                logger.warn("Skipping sending decision $decisionId to application other guardian ${application.otherGuardianId} - not a current VTJ guardian")
            }
        }
    }

    fun deliverDecisionToGuardian(
        tx: Database.Transaction,
        decision: Decision,
        guardian: PersonDTO,
        documentUri: String
    ) {
        if (guardian.identity !is ExternalIdentifier.SSN) {
            logger.info { "Cannot deliver daycare decision ${decision.id} to guardian. SSN is missing." }
            return
        }

        val lang = tx.getDecisionLanguage(decision.id)
        val sendAddress = getSendAddress(messageProvider, guardian, lang)
        // SFI expects unique string for each message so document.id is not suitable as it is NOT string and NOT unique
        val uniqueId = "${decision.id}|${guardian.id}"
        val message = SuomiFiMessage(
            messageId = uniqueId,
            documentId = uniqueId,
            documentDisplayName = calculateDecisionFileName(tx, decision, lang),
            documentUri = documentUri,
            firstName = guardian.firstName!!,
            lastName = guardian.lastName!!,
            streetAddress = sendAddress.street,
            postalCode = sendAddress.postalCode,
            postOffice = sendAddress.postOffice,
            ssn = guardian.identity.ssn,
            language = lang,
            messageHeader = messageProvider.getDecisionHeader(langWithDefault(lang)),
            messageContent = messageProvider.getDecisionContent(langWithDefault(lang))
        )

        evakaMessageClient.send(message)
    }

    fun getDecisionPdf(tx: Database.Read, decisionId: UUID): Document {
        val decision = tx.getDecision(decisionId)
            ?: throw NotFound("No decision $decisionId found")
        val lang = tx.getDecisionLanguage(decisionId)
        return decision.documentUri
            ?.let(URI::create)
            ?.let(s3Client::get)
            ?.let {
                DocumentWrapper(
                    name = calculateDecisionFileName(tx, decision, lang),
                    bytes = it.getBytes()
                )
            } ?: throw NotFound("Decision S3 URL is not set for $decisionId. Document generation is still in progress.")
    }

    private fun calculateDecisionFileName(tx: Database.Read, decision: Decision, lang: String): String {
        val child = tx.getPersonById(decision.childId)
        val childName = "${child?.firstName}_${child?.lastName}"
        val prefix = getLocalizedFilename(decision.type, lang)
        return "${prefix}_$childName.pdf".replace(" ", "_")
    }

    private fun getLocalizedFilename(type: DecisionType, lang: String): String {
        return when (lang) {
            "sv" -> when (type) {
                DecisionType.CLUB -> "Kerhopäätös" // All clubs are in Finnish
                DecisionType.DAYCARE, DecisionType.DAYCARE_PART_TIME -> "Beslut_om_småbarnspedagogisk_verksamhet"
                DecisionType.PRESCHOOL -> "Beslut_om_förskoleplats"
                DecisionType.PRESCHOOL_DAYCARE -> "Anslutande_småbarnspedagogik"
                DecisionType.PREPARATORY_EDUCATION -> "Valmistava_päätös" // Svebi does not offer preparatory education
            }
            else -> when (type) {
                DecisionType.CLUB -> "Kerhopäätös"
                DecisionType.DAYCARE, DecisionType.DAYCARE_PART_TIME -> "Varhaiskasvatuspäätös"
                DecisionType.PRESCHOOL -> "Esiopetuspäätös"
                DecisionType.PRESCHOOL_DAYCARE -> "Liittyvä_varhaiskasvatuspäätös"
                DecisionType.PREPARATORY_EDUCATION -> "Valmistava_päätös"
            }
        }
    }
}

fun createDecisionPdf(
    messageProvider: IMessageProvider,
    templateProvider: ITemplateProvider,
    pdfService: PDFService,
    decision: Decision,
    guardian: PersonDTO,
    child: PersonDTO,
    isTransferApplication: Boolean,
    lang: String,
    unitManager: DaycareManager
): ByteArray {
    val sendAddress = getSendAddress(messageProvider, guardian, lang)
    val template = createTemplate(templateProvider, decision, isTransferApplication)
    val isPartTimeDecision: Boolean = decision.type === DecisionType.DAYCARE_PART_TIME

    val pages = generateDecisionPages(
        template,
        lang,
        decision,
        child,
        guardian,
        unitManager,
        sendAddress,
        isPartTimeDecision
    )

    return pdfService.render(pages)
}

private fun generateDecisionPages(
    template: String,
    lang: String,
    decision: Decision,
    child: PersonDTO,
    guardian: PersonDTO,
    manager: DaycareManager,
    sendAddress: DecisionSendAddress,
    isPartTimeDecision: Boolean
): Page {
    return Page(
        Template(template),
        Context().apply {
            locale = Locale.Builder().setLanguage(lang).build()
            setVariable("decision", decision)
            setVariable("child", child)
            setVariable("guardian", guardian)
            setVariable("manager", manager)
            setVariable("sendAddress", sendAddress)
            setVariable("isPartTimeDecision", isPartTimeDecision)
            setVariable(
                "hideDaycareTime",
                decision.type == DecisionType.PRESCHOOL_DAYCARE || decision.type == DecisionType.CLUB || decision.unit.providerType == ProviderType.PRIVATE_SERVICE_VOUCHER
            )
        }
    )
}

private fun createTemplate(
    templateProvider: ITemplateProvider,
    decision: Decision,
    isTransferApplication: Boolean
): String {
    return when (decision.type) {
        DecisionType.CLUB -> templateProvider.getClubDecisionPath()

        DecisionType.DAYCARE, DecisionType.PRESCHOOL_DAYCARE, DecisionType.DAYCARE_PART_TIME -> {
            if (decision.unit.providerType == ProviderType.PRIVATE_SERVICE_VOUCHER) {
                templateProvider.getDaycareVoucherDecisionPath()
            } else {
                if (isTransferApplication) {
                    templateProvider.getDaycareTransferDecisionPath()
                } else {

                    templateProvider.getDaycareDecisionPath()
                }
            }
        }
        DecisionType.PRESCHOOL ->
            templateProvider.getPreschoolDecisionPath()

        DecisionType.PREPARATORY_EDUCATION ->
            templateProvider.getPreparatoryDecisionPath()
    }
}
