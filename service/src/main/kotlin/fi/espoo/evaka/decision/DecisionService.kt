// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getUnitManager
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
import fi.espoo.evaka.shared.message.SuomiFiMessage
import fi.espoo.voltti.pdfgen.PDFService
import fi.espoo.voltti.pdfgen.Page
import fi.espoo.voltti.pdfgen.Template
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import java.net.URI
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

val logger = KotlinLogging.logger { }

@Service
class DecisionService(
    @Value("\${fi.espoo.voltti.document.bucket.daycaredecision}")
    private val decisionBucket: String,
    private val personService: PersonService,
    private val s3Client: DocumentService,
    private val pdfService: PDFService,
    private val evakaMessageClient: IEvakaMessageClient,
    private val asyncJobRunner: AsyncJobRunner
) {
    fun finalizeDecisions(h: Handle, user: AuthenticatedUser, applicationId: UUID, sendAsMessage: Boolean): List<UUID> {
        val decisionIds = finalizeDecisions(h, applicationId)
        asyncJobRunner.plan(h, decisionIds.map { NotifyDecisionCreated(it, user, sendAsMessage) })
        return decisionIds
    }

    fun createDecisionPdfs(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        decisionId: UUID
    ) {
        val decision = getDecision(tx.handle, decisionId) ?: throw NotFound("No decision with id: $decisionId")
        val applicationId = decision.applicationId
        val application = fetchApplicationDetails(tx.handle, applicationId)
            ?: throw NotFound("Application $applicationId was not found")
        val guardian = personService.getUpToDatePerson(tx, user, application.guardianId)
            ?: error("Guardian not found with id: ${application.guardianId}")
        val child = personService.getUpToDatePerson(tx, user, application.childId)
            ?: error("Child not found with id: ${application.childId}")

        createDecisionPdf(tx.handle, decision, application, guardian, child)
            .let { uri -> updateDecisionGuardianDocumentUri(tx.handle, decisionId, uri) }

        if (
            decision.type != DecisionType.CLUB &&
            application.otherGuardianId != null &&
            !personService.personsLiveInTheSameAddress(
                tx,
                user,
                application.guardianId,
                application.otherGuardianId
            )
        ) {
            val otherGuardian = personService.getUpToDatePerson(tx, user, application.otherGuardianId)
                ?: throw NotFound("Other guardian not found with id: ${application.otherGuardianId}")

            createDecisionPdf(tx.handle, decision, application, otherGuardian, child)
                .let { uri -> updateDecisionOtherGuardianDocumentUri(tx.handle, decisionId, uri) }
        }
    }

    private fun createDecisionPdf(
        h: Handle,
        decision: Decision,
        application: ApplicationDetails,
        guardian: PersonDTO,
        child: PersonDTO
    ): URI {
        val manager = h.getUnitManager(decision.unit.id)
            ?: throw NotFound("Daycare manager not found with daycare id: ${decision.unit.id}.")
        val lang =
            if (decision.type == DecisionType.CLUB) "fi"
            else getDecisionLanguage(h, decision.id)
        val sendAddress = getSendAddress(guardian, lang)

        val templates = when (decision.type) {
            DecisionType.CLUB -> listOf(
                "club/club-decision",
                "club/club-correction",
                "club/club-acceptance-form"
            )
            DecisionType.DAYCARE, DecisionType.PRESCHOOL_DAYCARE, DecisionType.DAYCARE_PART_TIME -> {
                if (decision.unit.providerType == ProviderType.PRIVATE_SERVICE_VOUCHER) {
                    listOf(
                        "daycare/voucher/daycare-decision-page1",
                        "daycare/voucher/daycare-decision-page2",
                        "daycare/voucher/daycare-correction",
                        "daycare/voucher/daycare-acceptance-form"
                    )
                } else {
                    if (application.transferApplication) {
                        listOf(
                            "daycare/transfer/daycare-decision-page1",
                            "daycare/transfer/daycare-decision-page2",
                            "daycare/transfer/daycare-correction",
                            "daycare/transfer/daycare-acceptance-form"
                        )
                    } else {
                        listOf(
                            "daycare/daycare-decision-page1",
                            "daycare/daycare-decision-page2",
                            "daycare/daycare-correction",
                            "daycare/daycare-acceptance-form"
                        )
                    }
                }
            }
            DecisionType.PRESCHOOL -> listOf(
                "preschool/preschool-decision-page1",
                "preschool/preschool-decision-page2",
                "preschool/preschool-correction",
                "preschool/preschool-acceptance-form"
            )
            DecisionType.PREPARATORY_EDUCATION -> listOf(
                "preparatory/preparatory-decision-page1",
                "preparatory/preparatory-decision-page2",
                "preparatory/preparatory-correction",
                "preparatory/preparatory-acceptance-form"
            )
        }
        val isPartTimeDecision: Boolean = decision.type === DecisionType.DAYCARE_PART_TIME

        // todo: decision templates still except old format :(
        val legacyApplication = PdfTemplateApplication(
            form = DaycareFormV0.fromForm2(application.form, application.type, false, false),
            childDateOfBirth = child.dateOfBirth
        )

        val pages = templates.mapIndexed { i, template ->
            Page(
                Template(template),
                Context().apply {
                    locale = Locale.Builder().setLanguage(lang).build()
                    setVariable("decision", decision)
                    setVariable("application", legacyApplication)
                    setVariable("guardian", guardian)
                    setVariable("manager", manager)
                    setVariable("sendAddress", sendAddress)
                    setVariable("pageNumber", i + 1)
                    setVariable("isPartTimeDecision", isPartTimeDecision)
                }
            )
        }
        val documentBytes = pdfService.render(pages)
        logger.debug { "Daycare decision PDF generated for decision ${decision.id}. Uploading to S3." }

        val objectKey = when (decision.type) {
            DecisionType.CLUB -> "clubdecision"
            DecisionType.DAYCARE, DecisionType.DAYCARE_PART_TIME -> "daycaredecision"
            DecisionType.PRESCHOOL -> "preschooldecision"
            DecisionType.PRESCHOOL_DAYCARE -> "connectingdaycaredecision"
            DecisionType.PREPARATORY_EDUCATION -> "preparatorydecision"
        }.let { "${it}_${decision.id}_${guardian.id}_$lang" }

        return uploadPdfToS3(decisionBucket, objectKey, documentBytes).uri
    }

    data class PdfTemplateApplication(
        var form: DaycareFormV0,
        val childDateOfBirth: LocalDate
    )

    private fun uploadPdfToS3(bucket: String, key: String, document: ByteArray): DocumentLocation =
        s3Client.upload(
            bucket,
            DocumentWrapper(name = key, path = "/", bytes = document),
            "application/pdf"
        ).also {
            logger.debug { "PDF (object name: $key) uploaded to S3 with URL ${it.uri}." }
        }

    fun deliverDecisionToGuardians(h: Handle, decisionId: UUID) {
        val decision = getDecision(h, decisionId) ?: throw NotFound("No decision with id: $decisionId")

        val applicationId = decision.applicationId
        val application = fetchApplicationDetails(h, applicationId)
            ?: throw NotFound("Application $applicationId was not found")

        val applicationGuardian = h.getPersonById(application.guardianId)
            ?: error("Guardian not found with id: ${application.guardianId}")

        deliverDecisionToGuardian(h, decision, applicationGuardian, decision.documentUri.toString())

        if (application.otherGuardianId != null && !decision.otherGuardianDocumentUri.isNullOrBlank()) {
            val otherGuardian = h.getPersonById(application.otherGuardianId)
                ?: error("Other guardian not found with id: ${application.otherGuardianId}")

            deliverDecisionToGuardian(
                h,
                decision,
                otherGuardian,
                decision.otherGuardianDocumentUri.toString()
            )
        }
    }

    fun deliverDecisionToGuardian(
        h: Handle,
        decision: Decision,
        guardian: PersonDTO,
        documentUri: String
    ) {
        if (guardian.identity !is ExternalIdentifier.SSN) {
            logger.info { "Cannot deliver daycare decision ${decision.id} to guardian. SSN is missing." }
            return
        }

        val lang = getDecisionLanguage(h, decision.id)
        val sendAddress = getSendAddress(guardian, lang)
        // SFI expects unique string for each message so document.id is not suitable as it is NOT string and NOT unique
        val uniqueId = "${decision.id}|${guardian.id}"
        val message = SuomiFiMessage(
            messageId = uniqueId,
            documentId = uniqueId.toString(),
            documentDisplayName = calculateDecisionFileName(h, decision, lang),
            documentUri = documentUri,
            firstName = guardian.firstName!!,
            lastName = guardian.lastName!!,
            streetAddress = sendAddress.street,
            postalCode = sendAddress.postalCode,
            postOffice = sendAddress.postOffice,
            ssn = guardian.identity.ssn,
            language = lang,
            messageHeader = messageHeader.getValue(langWithDefault(lang)),
            messageContent = messageContent.getValue(langWithDefault(lang))
        )

        evakaMessageClient.send(message)
    }

    fun getDecisionPdf(h: Handle, decisionId: UUID): Document {
        val decision = getDecision(h, decisionId)
            ?: throw NotFound("No decision $decisionId found")
        val lang = getDecisionLanguage(h, decisionId)
        return decision.documentUri
            ?.let(URI::create)
            ?.let(s3Client::get)
            ?.let {
                DocumentWrapper(
                    name = calculateDecisionFileName(h, decision, lang),
                    path = "/",
                    bytes = it.getBytes()
                )
            } ?: error("Decision S3 url is not set for $decisionId.")
    }

    private fun calculateDecisionFileName(h: Handle, decision: Decision, lang: String): String {
        val child = h.getPersonById(decision.childId)
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

    private fun langWithDefault(lang: String): String = if (lang.toLowerCase() == "sv") "sv" else "fi"

    val messageHeader = mapOf(
        "fi" to """Espoon varhaiskasvatukseen liittyvät päätökset""",
        "sv" to """Beslut gällande Esbos småbarnspedagogik"""
    )

    val messageContent = mapOf(
        "fi" to """Olette hakenut lapsellenne Espoon kaupungin varhaiskasvatus-, esiopetus- ja/tai kerhopaikkaa. Koska olette ottanut Suomi.fi viestit -palvelun käyttöönne, on päätös luettavissa alla olevista liitteistä.

Päätös on hakemuksen tehneen huoltajan hyväksyttävissä/hylättävissä Espoon kaupungin varhaiskasvatuksen sähköisessä palvelussa osoitteessa espoonvarhaiskasvatus.fi . Suomi.fi -palvelussa ei voi antaa vastausta sähköisesti, mutta päätöksen yhteydestä voi tulostaa paperisen vastauslomakkeen.

Huomioittehan, että vastaus päätökseen tulee antaa kahden viikon kuluessa.



In English:

You have applied for a place in the City of Espoo’s early childhood education, pre-primary education and/or a club for your child. As you are a user of Suomi.fi Messages, you can find the decision in the attachments below.

The guardian who submitted the application can accept or reject the decision through the online service of the City of Espoo Early Childhood Education at espoonvarhaiskasvatus.fi. You cannot respond to the decision online through the Suomi.fi service, but you can print out a response form that is attached to the decision.

Please note that you have to respond to the decision within two weeks.""",
        "sv" to """Du har ansökt om plats i Esbo stads småbarnspedagogiska verksamhet, förskoleundervisning och/eller klubbverksamhet. Eftersom du har tagit i bruk Suomi.fi-meddelandetjänsten kan du läsa beslutet från bilagorna nedan.

Vårdnadshavaren, som har gjort ansökan om plats inom småbarnspedagogik, kan godkänna eller avstå från platsen i Esbo stads elektroniska tjänst på adressen espoonvarhaiskasvatus.fi. I tjänsten Suomi.fi kan du inte svara elektroniskt, men du kan skriva ut en svarsblankett.

Vänligen observera att du ska ge ditt svar till beslutet inom två veckor."""
    )
}
