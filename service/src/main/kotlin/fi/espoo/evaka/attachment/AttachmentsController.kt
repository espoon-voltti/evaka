// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import com.amazonaws.services.s3.model.AmazonS3Exception
import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.AttachmentType
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.shared.auth.AccessControlList
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
    private val acl: AccessControlList,
    private val documentClient: DocumentService,
    env: Environment
) {
    private val filesBucket = env.getProperty("fi.espoo.voltti.document.bucket.attachments")!!
    private val maxAttachmentsPerUser = env.getRequiredProperty("fi.espoo.evaka.maxAttachmentsPerUser").toInt()

    @PostMapping("/applications/{applicationId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadApplicationAttachmentEmployee(
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

    @PostMapping("/citizen/applications/{applicationId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadApplicationAttachmentCitizen(
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

    private fun handleFileUpload(
        db: Database,
        user: AuthenticatedUser,
        applicationId: UUID,
        file: MultipartFile,
        type: AttachmentType
    ): UUID {
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
            documentClient.upload(
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
        db.authorizeAttachmentDownload(user, attachmentId)

        val attachment =
            db.read { it.getAttachment(attachmentId) ?: throw NotFound("Attachment $attachmentId not found") }

        return try {
            documentClient.get(filesBucket, "$attachmentId").let { document ->
                ResponseEntity.ok()
                    .header("Content-Disposition", "attachment;filename=${attachment.name}")
                    .contentType(MediaType.valueOf(attachment.contentType))
                    .body(document.getBytes())
            }
        } catch (e: AmazonS3Exception) {
            if (e.statusCode == 403) throw NotFound("Attachment $attachmentId not available, scanning in progress or failed")
            throw e
        }
    }

    @DeleteMapping("/citizen/{id}")
    fun deleteAttachmentCitizen(db: Database, user: AuthenticatedUser, @PathVariable id: UUID): ResponseEntity<Unit> {
        Audit.AttachmentsDelete.log(targetId = id)
        user.requireOneOfRoles(UserRole.END_USER)
        if (!db.read { it.isOwnAttachment(id, user) }) throw Forbidden("Permission denied")

        db.transaction { deleteAttachment(it, id) }

        return ResponseEntity.noContent().build()
    }

    fun deleteAttachment(db: Database.Transaction, id: UUID) {
        db.deleteAttachment(id)
        documentClient.delete(filesBucket, "$id")
    }
}

private fun Database.authorizeAttachmentDownload(user: AuthenticatedUser, attachmentId: UUID) {
    if (user.hasOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN)) return
    if (user.hasOneOfRoles(UserRole.END_USER) && read { it.isOwnAttachment(attachmentId, user) }) return
    if (read { it.userHasSupervisorRights(user, attachmentId) }) return

    throw NotFound("Attachment $attachmentId not found")
}

fun Database.Read.isOwnApplication(applicationId: UUID, user: AuthenticatedUser): Boolean {
    return this.createQuery("SELECT 1 FROM application WHERE id = :id AND guardian_id = :userId")
        .bind("id", applicationId)
        .bind("userId", user.id)
        .mapTo<Int>()
        .any()
}

fun Database.Read.userAttachmentCount(userId: UUID): Int {
    return this.createQuery("SELECT COUNT(*) FROM attachment WHERE uploaded_by_person = :userId")
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}

fun Database.Read.userHasSupervisorRights(user: AuthenticatedUser, attachmentId: UUID): Boolean {
    return this.createQuery(
        """
SELECT 1
FROM attachment
JOIN placement_plan ON attachment.application_id = placement_plan.application_id
JOIN daycare ON placement_plan.unit_id = daycare.id AND daycare.round_the_clock
JOIN daycare_acl_view ON daycare.id = daycare_acl_view.daycare_id
WHERE employee_id = :userId
    AND role = :unitSupervisor
    AND attachment.id = :attachmentId
    AND attachment.type = :extendedCare
"""
    )
        .bind("userId", user.id)
        .bind("unitSupervisor", UserRole.UNIT_SUPERVISOR)
        .bind("attachmentId", attachmentId)
        .bind("extendedCare", AttachmentType.EXTENDED_CARE)
        .mapTo<Int>()
        .any()
}

val contentTypesWithMagicNumbers = mapOf(
    "image/jpeg" to listOf("FF D8 FF DB", "FF D8 FF EE", "FF D8 FF E1", "FF D8 FF E0 00 10 4A 46 49 46 00 01"),
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
        val fileBytes = stream.readNBytes(magicNumbers.map { it.size }.maxOrNull() ?: 0)
        magicNumbers.any { numbers ->
            fileBytes.slice(numbers.indices).toByteArray().contentEquals(numbers)
        }
    }

    if (contentMatchesMagicNumbers) return
    throw BadRequest("Invalid content type")
}
