package fi.espoo.evaka.process

import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.shared.*
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.stereotype.Service

@Service
class ProcessMetadataService(private val accessControl: AccessControl) {

    fun getApplicationProcessMetadata(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        applicationId: ApplicationId,
        process: ArchivedProcess,
        isCitizen: Boolean,
    ): ProcessMetadata {
        val application = tx.fetchApplicationDetails(applicationId) ?: throw NotFound()
        val applicationDocument = tx.getApplicationDocumentMetadata(applicationId)
        val decisionDocuments =
            tx.getSentDecisionIdsByApplication(applicationId).map {
                it to tx.getApplicationDecisionDocumentMetadata(it, isCitizen)
            }

        return ProcessMetadata(
            process = process,
            processName = getProcessName(application.type),
            primaryDocument = applicationDocument,
            secondaryDocuments =
                decisionDocuments.map { (decisionId, doc) ->
                    doc.copy(
                        downloadPath =
                            doc.downloadPath?.takeIf {
                                accessControl.hasPermissionFor(
                                    tx,
                                    user,
                                    clock,
                                    if (isCitizen) Action.Citizen.Decision.DOWNLOAD_PDF
                                    else Action.Decision.DOWNLOAD_PDF,
                                    decisionId,
                                )
                            }
                    )
                },
        )
    }

    private fun getProcessName(applicationType: ApplicationType): String =
        when (applicationType) {
            ApplicationType.CLUB -> "Kerhohakemus"
            ApplicationType.DAYCARE ->
                "Varhaiskasvatushakemus / palvelusetelihakemus varhaiskasvatukseen"
            ApplicationType.PRESCHOOL ->
                "Esiopetushakemus / hakemus esiopetuksessa järjestettävään perusopetukseen valmistavaan opetukseen"
        }
}
