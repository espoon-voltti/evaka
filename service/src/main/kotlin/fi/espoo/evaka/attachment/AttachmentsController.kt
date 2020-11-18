package fi.espoo.evaka.attachment

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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
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

const val attachmentsPath = "/attachments"

@RestController
@RequestMapping("/attachments")
class AttachmentsController(
    private val s3Client: DocumentService,
    env: Environment
) {
    private val filesBucket = env.getProperty("fi.espoo.voltti.document.bucket.files")

    @PostMapping("/applications/{applicationId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadApplicationAttachment(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<UUID> {
        user.requireOneOfRoles(UserRole.ADMIN)

        val id = handleFileUpload(db, applicationId, file)
        return ResponseEntity.ok(id)
    }

    @PostMapping("/enduser/applications/{applicationId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadEnduserApplicationAttachment(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<UUID> {
        user.requireOneOfRoles(UserRole.END_USER)
        if (!db.read { isOwnApplication(it, applicationId, user) }) throw Forbidden("Permission denied")

        val id = handleFileUpload(db, applicationId, file)
        return ResponseEntity.ok(id)
    }

    private fun handleFileUpload(db: Database, applicationId: UUID, file: MultipartFile): UUID {
        if (filesBucket == null) error("Files bucket is missing")

        val name = file.originalFilename
            ?.takeIf { it.isNotBlank() }
            ?: throw BadRequest("Filename missing")

        val contentType = file.contentType
            ?.takeIf { it in validTypes }
            ?: throw BadRequest("Invalid content type")

        val id = UUID.randomUUID()
        db.transaction { tx ->
            tx.handle.insertAttachment(id, name, contentType, applicationId)
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
        if (!user.hasOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN)) {
            // TODO: support end users by checking whether attachment belongs to an application from the user
            throw Forbidden("Permission denied")
        }
        val attachment = db.read { it.handle.getAttachment(attachmentId) ?: throw NotFound("Attachment $attachmentId not found") }

        // TODO
        // http://s3.lvh.me:9876/evaka-files-dev/537a6c81-e515-40d1-afd3-6be80f77be25
        val uri = "$attachmentId"
        return s3Client.get(filesBucket, uri).let { document ->
            ResponseEntity.ok()
                .header("Content-Disposition", "attachment;filename=${document.getName()}")
                .contentType(MediaType.valueOf(attachment.contentType))
                .body(document.getBytes())
        }
    }
}

fun isOwnApplication(r: Database.Read, applicationId: UUID, user: AuthenticatedUser): Boolean {
    return r.createQuery("SELECT 1 FROM application WHERE id = :id AND guardian_id = :userId")
        .bind("id", applicationId)
        .bind("userId", user.id)
        .mapTo<Int>()
        .toList()
        .isNotEmpty()
}
