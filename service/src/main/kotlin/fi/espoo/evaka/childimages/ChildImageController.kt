package fi.espoo.evaka.childimages

import fi.espoo.evaka.Audit
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.core.env.Environment
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
class ChildImageController(
    private val acl: AccessControlList,
    private val documentClient: DocumentService,
    env: Environment
) {
    private val bucket = env.getRequiredProperty("fi.espoo.voltti.document.bucket.data")

    @PutMapping("/children/{childId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun putImage(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<Unit> {
        Audit.ChildImageUpload.log(targetId = childId)
        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.STAFF, UserRole.MOBILE)

        replaceImage(db, documentClient, bucket, childId, file)

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/children/{childId}/image")
    fun deleteImage(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<Unit> {
        Audit.ChildImageDelete.log(targetId = childId)
        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.STAFF, UserRole.MOBILE)

        removeImage(db, documentClient, bucket, childId)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/child-images/{imageId}")
    fun getImage(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable imageId: UUID
    ): ResponseEntity<InputStreamResource> {
        Audit.ChildImageDownload.log(targetId = imageId)

        val image = db.read { it.getChildImage(imageId) }
        acl.getRolesForChild(user, image.childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.STAFF, UserRole.MOBILE)

        val stream = documentClient.stream(bucket, "$childImagesBucketPrefix$imageId")

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(InputStreamResource(stream))
    }
}
