// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import com.amazonaws.services.s3.model.AmazonS3Exception
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
import java.io.InputStream
import java.util.UUID
import javax.xml.bind.DatatypeConverter

const val attachmentsPath = "/"

@RestController
@RequestMapping("/attachments")
class AttachmentsController(
    private val s3Client: DocumentService,
    env: Environment
) {
    private val filesBucket = env.getProperty("fi.espoo.voltti.document.bucket.attachments")
    private val maxAttachmentsPerUser = env.getRequiredProperty("fi.espoo.evaka.maxAttachmentsPerUser").toInt()

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
        if (db.read { it.userAttachmentCount(user.id) } >= maxAttachmentsPerUser) throw Forbidden("Too many uploaded files for ${user.id}: $maxAttachmentsPerUser")

        val id = handleFileUpload(db, user, applicationId, file, type)
        return ResponseEntity.ok(id)
    }

    private fun handleFileUpload(db: Database, user: AuthenticatedUser, applicationId: UUID, file: MultipartFile, type: AttachmentType): UUID {
        val name = file.originalFilename
            ?.takeIf { it.isNotBlank() }
            ?: throw BadRequest("Filename missing")

        val contentType = file.contentType ?: throw BadRequest("Missing content type")
        checkFileContentType(file.inputStream, contentType)

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

    data class PreDownloadResponse(
        val fileAvailable: Boolean
    )

    @GetMapping("/{attachmentId}/pre-download")
    fun checkAttachmentAvailability(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable attachmentId: UUID
    ): ResponseEntity<PreDownloadResponse> {
        Audit.AttachmentsRead.log(targetId = attachmentId)
        if (!user.hasOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN)) {
            if (!db.read { it.isOwnAttachment(attachmentId, user) }) throw Forbidden("Permission denied")
        }

        return try {
            s3Client.headObject(filesBucket, "$attachmentId").let { _ ->
                ResponseEntity.ok().body(PreDownloadResponse(true))
            }
        } catch (e: AmazonS3Exception) {
            ResponseEntity.ok().body(PreDownloadResponse(false))
        }
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

        db.transaction {
            it.deleteAttachment(id)
            s3Client.delete(filesBucket, "$id")
        }
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

fun Database.Read.userAttachmentCount(userId: UUID): Int {
    return this.createQuery("SELECT COUNT(*) FROM attachment WHERE uploaded_by_person = :userId")
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}

val contentTypesWithMagicNumbers = mapOf(
    "image/jpeg" to listOf("FF D8 FF DB", "FF D8 FF EE", "FF D8 FF E0 00 10 4A 46 49 46 00 01"),
    "image/png" to listOf("89 50 4E 47 0D 0A 1A 0A"),
    "application/pdf" to listOf("25 50 44 46 2D"),
    "application/msword" to listOf("50 4B 03 04 14 00 06 00", "D0 CF 11 E0 A1 B1 1A E1"),
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to listOf("50 4B 03 04"),
    "application/vnd.oasis.opendocument.text" to listOf("50 4B 03 04")
).mapValues { (_, magicNumbers) ->
    magicNumbers.map { hex -> DatatypeConverter.parseHexBinary(hex.replace(" ", "")) }
}

fun checkFileContentType(file: InputStream, contentType: String) {
    val magicNumbers = contentTypesWithMagicNumbers[contentType]
        ?: throw BadRequest("Invalid content type")

    val contentMatchesMagicNumbers = file.use { stream ->
        val fileBytes = stream.readNBytes(magicNumbers.map { it.size }.max() ?: 0)
        magicNumbers.any { numbers ->
            fileBytes?.contentEquals(numbers) ?: false
        }
    }

    if (contentMatchesMagicNumbers) return
    throw BadRequest("Invalid content type")
}
