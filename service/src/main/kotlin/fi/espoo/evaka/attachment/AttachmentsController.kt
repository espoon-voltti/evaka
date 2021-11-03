// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.Audit
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.s3.getAndCheckFileContentType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.services.s3.model.S3Exception
import java.util.UUID

@RestController
@RequestMapping("/attachments")
class AttachmentsController(
    private val documentClient: DocumentService,
    private val stateService: ApplicationStateService,
    private val accessControl: AccessControl,
    evakaEnv: EvakaEnv,
    bucketEnv: BucketEnv
) {
    private val filesBucket = bucketEnv.attachments
    private val maxAttachmentsPerUser = evakaEnv.maxAttachmentsPerUser

    @PostMapping("/applications/{applicationId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadApplicationAttachmentEmployee(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestParam type: AttachmentType,
        @RequestPart("file") file: MultipartFile
    ): AttachmentId {
        Audit.AttachmentsUploadForApplication.log(applicationId)
        accessControl.requirePermissionFor(user, Action.Application.UPLOAD_ATTACHMENT, applicationId)
        return handleFileUpload(db, user, AttachmentParent.Application(applicationId), file, defaultAllowedAttachmentContentTypes, type).also {
            db.transaction { tx -> stateService.reCalculateDueDate(tx, applicationId) }
        }
    }

    @PostMapping("/income-statements/{incomeStatementId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadIncomeStatementAttachmentEmployee(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestPart("file") file: MultipartFile
    ): AttachmentId {
        Audit.AttachmentsUploadForIncomeStatement.log(incomeStatementId)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.UPLOAD_ATTACHMENT, incomeStatementId)
        return handleFileUpload(db, user, AttachmentParent.IncomeStatement(incomeStatementId), file, defaultAllowedAttachmentContentTypes)
    }

    @PostMapping("/messages/{draftId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadMessageAttachment(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable draftId: MessageDraftId,
        @RequestPart("file") file: MultipartFile
    ): AttachmentId {
        Audit.AttachmentsUploadForMessageDraft.log(draftId)
        accessControl.requirePermissionFor(user, Action.MessageDraft.UPLOAD_ATTACHMENT, draftId)
        return handleFileUpload(db, user, AttachmentParent.MessageDraft(draftId), file, defaultAllowedAttachmentContentTypes)
    }

    @PostMapping("/pedagogical-documents/{documentId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadPedagogicalDocumentAttachment(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable documentId: PedagogicalDocumentId,
        @RequestPart("file") file: MultipartFile
    ): AttachmentId {
        Audit.AttachmentsUploadForPedagogicalDocument.log(documentId)
        accessControl.requirePermissionFor(user, Action.PedagogicalDocument.CREATE_ATTACHMENT, documentId)

        return handleFileUpload(db, user, AttachmentParent.PedagogicalDocument(documentId), file, pedagogicalDocumentAllowedAttacmentContentTypes)
    }

    @PostMapping("/citizen/applications/{applicationId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadApplicationAttachmentCitizen(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestParam type: AttachmentType,
        @RequestPart("file") file: MultipartFile
    ): AttachmentId {
        Audit.AttachmentsUploadForApplication.log(applicationId)
        accessControl.requirePermissionFor(user, Action.Application.UPLOAD_ATTACHMENT, applicationId)

        val attachTo = AttachmentParent.Application(applicationId)
        checkAttachmentCount(db, attachTo, user)

        return handleFileUpload(db, user, attachTo, file, defaultAllowedAttachmentContentTypes, type)
            .also { db.transaction { stateService.reCalculateDueDate(it, applicationId) } }
    }

    @PostMapping(
        value = ["/citizen/income-statements/{incomeStatementId}", "/citizen/income-statements"],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadAttachmentCitizen(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        @PathVariable(required = false) incomeStatementId: IncomeStatementId?,
        @RequestPart("file") file: MultipartFile
    ): AttachmentId {
        Audit.AttachmentsUploadForIncomeStatement.log(incomeStatementId)
        if (incomeStatementId != null) {
            accessControl.requirePermissionFor(user, Action.IncomeStatement.UPLOAD_ATTACHMENT, incomeStatementId)
        }

        val attachTo =
            if (incomeStatementId != null) AttachmentParent.IncomeStatement(incomeStatementId) else AttachmentParent.None
        checkAttachmentCount(db, attachTo, user)

        return handleFileUpload(db, user, attachTo, file, defaultAllowedAttachmentContentTypes)
    }

    private fun checkAttachmentCount(db: Database.Connection, attachTo: AttachmentParent, user: AuthenticatedUser) {
        val count = db.read {
            when (attachTo) {
                AttachmentParent.None -> it.userUnparentedAttachmentCount(user.id)
                is AttachmentParent.Application -> it.userApplicationAttachmentCount(attachTo.applicationId, user.id)
                is AttachmentParent.IncomeStatement -> it.userIncomeStatementAttachmentCount(
                    attachTo.incomeStatementId,
                    user.id
                )
                is AttachmentParent.PedagogicalDocument -> it.userPedagogicalDocumentCount(
                    attachTo.pedagogicalDocumentId,
                    user.id
                )
                is AttachmentParent.MessageDraft,
                is AttachmentParent.MessageContent -> 0
            }
        }
        if (count >= maxAttachmentsPerUser) {
            throw Forbidden("Too many uploaded files for ${user.id}: $maxAttachmentsPerUser")
        }
    }

    private fun handleFileUpload(
        db: Database.Connection,
        user: AuthenticatedUser,
        attachTo: AttachmentParent,
        file: MultipartFile,
        allowedContentTypes: List<String>,
        type: AttachmentType? = null
    ): AttachmentId {
        val name = file.originalFilename
            ?.takeIf { it.isNotBlank() }
            ?: throw BadRequest("Filename missing")

        val contentType = getAndCheckFileContentType(file.inputStream, allowedContentTypes)

        val id = AttachmentId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insertAttachment(
                user,
                id,
                name,
                contentType,
                attachTo,
                type = type
            )
            documentClient.upload(
                filesBucket,
                DocumentWrapper(
                    name = id.toString(),
                    bytes = file.bytes
                ),
                contentType
            )
        }

        return id
    }

    @GetMapping("/{attachmentId}/download")
    fun getAttachment(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable attachmentId: AttachmentId
    ): ResponseEntity<ByteArray> {
        Audit.AttachmentsRead.log(targetId = attachmentId)

        val attachment =
            db.read { it.getAttachment(attachmentId) } ?: throw NotFound("Attachment $attachmentId not found")

        val action = when (attachment.attachedTo) {
            is AttachmentParent.Application -> Action.Attachment.READ_APPLICATION_ATTACHMENT
            is AttachmentParent.IncomeStatement,
            is AttachmentParent.None -> Action.Attachment.READ_INCOME_STATEMENT_ATTACHMENT
            is AttachmentParent.MessageContent -> Action.Attachment.READ_MESSAGE_CONTENT_ATTACHMENT
            is AttachmentParent.MessageDraft -> Action.Attachment.READ_MESSAGE_DRAFT_ATTACHMENT
            is AttachmentParent.PedagogicalDocument -> Action.Attachment.READ_PEDAGOGICAL_DOCUMENT_ATTACHMENT
        }.exhaust()
        accessControl.requirePermissionFor(user, action, attachmentId)

        return try {
            documentClient.get(filesBucket, "$attachmentId").let { document ->
                ResponseEntity.ok()
                    .header("Content-Disposition", "attachment;filename=${attachment.name}")
                    .contentType(MediaType.valueOf(attachment.contentType))
                    .body(document.getBytes())
            }
        } catch (e: S3Exception) {
            if (e.statusCode() == 403) throw NotFound("Attachment $attachmentId not available, scanning in progress or failed")
            throw e
        }
    }

    @DeleteMapping(value = ["/{attachmentId}", "/citizen/{attachmentId}"])
    fun deleteAttachmentHandler(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable attachmentId: AttachmentId
    ): ResponseEntity<Unit> {
        Audit.AttachmentsDelete.log(targetId = attachmentId)

        val attachment =
            db.read { it.getAttachment(attachmentId) } ?: throw NotFound("Attachment $attachmentId not found")

        val action = when (attachment.attachedTo) {
            is AttachmentParent.Application -> Action.Attachment.DELETE_APPLICATION_ATTACHMENT
            is AttachmentParent.IncomeStatement,
            is AttachmentParent.None -> Action.Attachment.DELETE_INCOME_STATEMENT_ATTACHMENT
            is AttachmentParent.MessageDraft -> Action.Attachment.DELETE_MESSAGE_DRAFT_ATTACHMENT
            is AttachmentParent.MessageContent -> Action.Attachment.DELETE_MESSAGE_CONTENT_ATTACHMENT
            is AttachmentParent.PedagogicalDocument -> Action.Attachment.DELETE_PEDAGOGICAL_DOCUMENT_ATTACHMENT
        }.exhaust()
        accessControl.requirePermissionFor(user, action, attachmentId)

        db.transaction { deleteAttachment(it, attachmentId) }
        return ResponseEntity.noContent().build()
    }

    fun deleteAttachment(tx: Database.Transaction, attachmentId: AttachmentId) {
        tx.deleteAttachment(attachmentId)
        documentClient.delete(filesBucket, "$attachmentId")
    }

    val defaultAllowedAttachmentContentTypes: List<String> = listOf(
        "image/jpeg",
        "image/png",
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.oasis.opendocument.text"
    )

    val pedagogicalDocumentAllowedAttacmentContentTypes = defaultAllowedAttachmentContentTypes + listOf(
        "video/*",
        "audio/*"
    )
}
