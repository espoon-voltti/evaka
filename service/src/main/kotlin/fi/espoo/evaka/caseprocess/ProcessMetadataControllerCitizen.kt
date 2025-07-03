// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.caseprocess

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/process-metadata")
class ProcessMetadataControllerCitizen(
    private val accessControl: AccessControl,
    private val processMetadataService: ProcessMetadataService,
) {
    @GetMapping("/applications/{applicationId}")
    fun getApplicationMetadata(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Application.READ,
                        applicationId,
                    )
                    val process =
                        tx.getCaseProcessByApplicationId(applicationId)
                            ?: return@read ProcessMetadataResponse(null)
                    val processMetadata =
                        processMetadataService.getApplicationProcessMetadata(
                            tx,
                            user,
                            clock,
                            applicationId,
                            process,
                        )
                    ProcessMetadataResponse(processMetadata.redactForCitizen())
                }
            }
            .also { response ->
                Audit.ApplicationReadMetadata.log(
                    targetId = AuditId(applicationId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }

    @GetMapping("/fee-decisions/{feeDecisionId}")
    fun getFeeDecisionMetadata(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable feeDecisionId: FeeDecisionId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.FeeDecision.READ,
                        feeDecisionId,
                    )
                    val process =
                        tx.getCaseProcessByFeeDecisionId(feeDecisionId)
                            ?: return@read ProcessMetadataResponse(null)
                    val decisionDocument =
                        tx.getFeeDecisionDocumentMetadata(feeDecisionId, isCitizen = true)

                    ProcessMetadataResponse(
                        ProcessMetadata(
                                process = process,
                                processName = "Varhaiskasvatuksen maksupäätös",
                                primaryDocument = decisionDocument,
                                secondaryDocuments = emptyList(),
                            )
                            .redactForCitizen()
                    )
                }
            }
            .also { response ->
                Audit.FeeDecisionReadMetadata.log(
                    targetId = AuditId(feeDecisionId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }

    @GetMapping("/voucher-value-decisions/{voucherValueDecisionId}")
    fun getVoucherValueDecisionMetadata(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable voucherValueDecisionId: VoucherValueDecisionId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.VoucherValueDecision.READ,
                        voucherValueDecisionId,
                    )
                    val process =
                        tx.getCaseProcessByVoucherValueDecisionId(voucherValueDecisionId)
                            ?: return@read ProcessMetadataResponse(null)
                    val decisionDocument =
                        tx.getVoucherValueDecisionDocumentMetadata(
                            voucherValueDecisionId,
                            isCitizen = true,
                        )

                    ProcessMetadataResponse(
                        ProcessMetadata(
                                process = process,
                                processName = "Varhaiskasvatuksen palvelusetelin arvopäätös",
                                primaryDocument = decisionDocument,
                                secondaryDocuments = emptyList(),
                            )
                            .redactForCitizen()
                    )
                }
            }
            .also { response ->
                Audit.VoucherValueDecisionReadMetadata.log(
                    targetId = AuditId(voucherValueDecisionId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }
}
