// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.AttachmentType
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.core.env.Environment
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
import java.util.UUID

val validTypes = listOf(
    "image/jpeg",
    "image/png",
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.oasis.opendocument.text"
)

const val attachmentsPath = "/"

@RestController
@RequestMapping("/attachments")
class AttachmentsController(
    private val s3Client: DocumentService,
    env: Environment
) {
    private val filesBucket = env.getProperty("fi.espoo.voltti.document.bucket.attachments")

    @PostMapping("/applications/{applicationId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadApplicationAttachment(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestParam type: AttachmentType,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<UUID> {
        Audit.AttachmentsUpload.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN)

        val id = handleFileUpload(db, user, applicationId, file, type)
        return ResponseEntity.ok(id)
    }

    @PostMapping("/enduser/applications/{applicationId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadEnduserApplicationAttachment(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestParam type: AttachmentType,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<UUID> {
        Audit.AttachmentsUpload.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)
        if (!db.read { it.isOwnApplication(applicationId, user) }) throw Forbidden("Permission denied")

        val id = handleFileUpload(db, user, applicationId, file, type)
        return ResponseEntity.ok(id)
    }

    private fun handleFileUpload(db: Database, user: AuthenticatedUser, applicationId: UUID, file: MultipartFile, type: AttachmentType): UUID {
        if (filesBucket == null) error("Files bucket is missing")

        val name = file.originalFilename
            ?.takeIf { it.isNotBlank() }
            ?: throw BadRequest("Filename missing")

        val contentType = file.contentType
            ?.takeIf { it in validTypes }
            ?: throw BadRequest("Invalid content type")

        val id = UUID.randomUUID()
        db.transaction { tx ->
            tx.insertAttachment(
                id,
                name,
                contentType,
                applicationId,
                uploadedByEnduser = user.id.takeIf { user.isEndUser() },
                uploadedByEmployee = user.id.takeUnless { user.isEndUser() },
                type = type
            )
            s3Client.upload(
                filesBucket,
                DocumentWrapper(
                    name = id.toString(),
                    path = attachmentsPath,
                    bytes = file.bytes
                ),
                contentType
            )
        }
        return id
    }

    @GetMapping("/{attachmentId}/download")
    fun getAttachment(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable attachmentId: UUID
    ): ResponseEntity<ByteArray> {
        Audit.AttachmentsRead.log(targetId = attachmentId)
        if (!user.hasOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN)) {
            if (!db.read { it.isOwnAttachment(attachmentId, user) }) throw Forbidden("Permission denied")
        }

        val attachment =
            db.read { it.getAttachment(attachmentId) ?: throw NotFound("Attachment $attachmentId not found") }

        return s3Client.get(filesBucket, "$attachmentId").let { document ->
            ResponseEntity.ok()
                .header("Content-Disposition", "attachment;filename=${document.getName()}")
                .contentType(MediaType.valueOf(attachment.contentType))
                .body(document.getBytes())
        }
    }

    @DeleteMapping("/enduser/{id}")
    fun deleteEnduserAttachment(db: Database, user: AuthenticatedUser, @PathVariable id: UUID): ResponseEntity<Unit> {
        Audit.AttachmentsDelete.log(targetId = id)
        user.requireOneOfRoles(UserRole.END_USER)
        if (!db.read { it.isOwnAttachment(id, user) }) throw Forbidden("Permission denied")

        s3Client.delete(filesBucket, "$id")

        db.transaction { it.deleteAttachment(id) }
        return ResponseEntity.noContent().build()
    }
}

fun Database.Read.isOwnApplication(applicationId: UUID, user: AuthenticatedUser): Boolean {
    return this.createQuery("SELECT 1 FROM application WHERE id = :id AND guardian_id = :userId")
        .bind("id", applicationId)
        .bind("userId", user.id)
        .mapTo<Int>()
        .toList()
        .isNotEmpty()
}
