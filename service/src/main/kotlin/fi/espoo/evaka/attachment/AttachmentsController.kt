package fi.espoo.evaka.attachment

import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
    fun handleFileUpload(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<Unit> {
        user.requireOneOfRoles(UserRole.ADMIN)

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

        return ResponseEntity.noContent().build()
    }
}
