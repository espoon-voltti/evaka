package fi.espoo.evaka.childimages

import fi.espoo.evaka.Audit
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.s3.checkFileContentType
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import org.springframework.core.env.Environment
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.UUID

@RestController
class ChildImageController(
    private val acl: AccessControlList,
    private val documentClient: DocumentService,
    env: Environment
) {
    private val bucket = env.getRequiredProperty("fi.espoo.voltti.document.bucket.childimages")

    @PutMapping("/children/{childId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun putImage(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<Unit> {
        // Audit.ImageUpload.log(targetId = childId)
        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.STAFF, UserRole.MOBILE)

        uploadImage(db, childId, file)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/child-images/{imageId}")
    fun getImage(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable imageId: UUID
    ): ResponseEntity<InputStreamResource> {
        // Audit.ImageDownload.log(targetId = imageId)

        val image = db.read { it.getChildImage(imageId) }
        acl.getRolesForChild(user, image.childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.STAFF, UserRole.MOBILE)

        val stream = documentClient.stream(bucket, imageId.toString())

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(InputStreamResource(stream))
    }

    private fun uploadImage(
        db: Database,
        childId: UUID,
        file: MultipartFile
    ) {
        val contentType = file.contentType ?: throw BadRequest("Missing content type")
        println(contentType)
        if(contentType !in listOf("image/jpeg", "image/png")){
            throw BadRequest("Unsupported content type")
        }
        checkFileContentType(file.inputStream, contentType)

        db.transaction { tx ->
            val imageId = tx.replaceChildImage(childId)

            documentClient.upload(
                bucket,
                DocumentWrapper(
                    name = imageId.toString(),
                    path = "/",
                    bytes = file.bytes
                ),
                contentType
            )
        }
    }
}
