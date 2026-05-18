// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.caseprocess

import evaka.core.application.ApplicationType
import evaka.core.application.fetchApplicationDetails
import evaka.core.shared.*
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import org.springframework.stereotype.Service

@Service
class ProcessMetadataService(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
) {
    fun getApplicationProcessMetadata(
        tx: Database.Read,
        user: AuthenticatedUser,
        clock: EvakaClock,
        applicationId: ApplicationId,
        process: CaseProcess,
    ): ProcessMetadata {
        val isCitizen = user is AuthenticatedUser.Citizen
        val application = tx.fetchApplicationDetails(applicationId) ?: throw NotFound()
        val applicationDocument = tx.getApplicationDocumentMetadata(applicationId)
        val decisionDocuments =
            tx.getSentDecisionIdsByApplication(applicationId).map {
                it to tx.getApplicationDecisionDocumentMetadata(it, isCitizen)
            }

        val (processName, processType) = applicationProcessNameAndType(application.type)
        return ProcessMetadata(
            process = process,
            processName = processName,
            processType = processType,
            businessId = featureConfig.metadataBusinessId,
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
}

private fun applicationProcessNameAndType(
    applicationType: ApplicationType
): Pair<String, ProcessType> =
    when (applicationType) {
        ApplicationType.CLUB -> "Kerhohakemus" to ProcessType.APPLICATION_CLUB
        ApplicationType.DAYCARE ->
            "Varhaiskasvatushakemus / palvelusetelihakemus varhaiskasvatukseen" to
                ProcessType.APPLICATION_DAYCARE
        ApplicationType.PRESCHOOL ->
            "Esiopetushakemus / hakemus esiopetuksessa järjestettävään perusopetukseen valmistavaan opetukseen" to
                ProcessType.APPLICATION_PRESCHOOL
    }
