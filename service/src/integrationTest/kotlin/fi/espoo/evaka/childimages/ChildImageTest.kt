// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.xml.bind.DatatypeConverter
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChildImageTest : FullApplicationTest() {

    @Autowired
    private lateinit var controller: ChildImageController

    @MockBean
    private lateinit var documentService: DocumentService

    @BeforeEach
    protected fun beforeEach() {
        db.transaction {
            it.resetDatabase()
            it.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `inserting image`() {
        val file = FileMock()
        controller.putImage(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN)),
            testChild_1.id,
            file
        )

        val images = db.read {
            it.createQuery("SELECT * FROM child_images").mapTo<ChildImage>().list()
        }

        assertEquals(1, images.size)
        verify(documentService).upload(
            bucketName = "evaka-data-it",
            document = DocumentWrapper(
                name = "child-images/${images.first().id}",
                bytes = file.bytes
            ),
            contentType = "image/jpeg"
        )
    }

    @Test
    fun `replacing image`() {
        val oldImageId = db.transaction {
            it
                .createQuery("INSERT INTO child_images (child_id) VALUES (:childId) RETURNING id")
                .bind("childId", testChild_1.id)
                .mapTo<UUID>()
                .one()
        }

        val file = FileMock()
        controller.putImage(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN)),
            testChild_1.id,
            file
        )

        val images = db.read {
            it.createQuery("SELECT * FROM child_images").mapTo<ChildImage>().list()
        }

        assertEquals(1, images.size)
        assertFalse(oldImageId == images.first().id)

        verify(documentService).delete(
            bucketName = "evaka-data-it",
            key = "child-images/$oldImageId"
        )
        verify(documentService).upload(
            bucketName = "evaka-data-it",
            document = DocumentWrapper(
                name = "child-images/${images.first().id}",
                bytes = file.bytes
            ),
            contentType = "image/jpeg"
        )
    }

    @Test
    fun `deleting image`() {
        val oldImageId = db.transaction {
            it
                .createQuery("INSERT INTO child_images (child_id) VALUES (:childId) RETURNING id")
                .bind("childId", testChild_1.id)
                .mapTo<UUID>()
                .one()
        }

        controller.deleteImage(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN)),
            testChild_1.id
        )

        verify(documentService).delete(
            bucketName = "evaka-data-it",
            key = "child-images/$oldImageId"
        )
    }

    @Test
    fun `downloading image`() {
        val file = FileMock()
        val oldImageId = db.transaction {
            it
                .createQuery("INSERT INTO child_images (child_id) VALUES (:childId) RETURNING id")
                .bind("childId", testChild_1.id)
                .mapTo<UUID>()
                .one()
        }
        whenever(documentService.stream("evaka-data-it", "$childImagesBucketPrefix$oldImageId"))
            .thenReturn(file.inputStream)

        val response = controller.getImage(
            dbInstance(),
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN)),
            oldImageId
        )

        assertEquals(file.inputStream.readAllBytes().asList(), response.body.inputStream.readAllBytes().asList())
        assertEquals(MediaType.IMAGE_JPEG, response.headers.contentType)

        verify(documentService).stream(
            bucketName = "evaka-data-it",
            key = "child-images/$oldImageId"
        )
    }
}

class FileMock : MultipartFile {
    val data: ByteArray = DatatypeConverter.parseHexBinary("FFD8FFDB0000000000000000").asList().toByteArray()

    override fun getInputStream(): InputStream {
        return data.inputStream()
    }

    override fun getName(): String {
        return "test.jpg"
    }

    override fun getOriginalFilename(): String? {
        return "test.jpg"
    }

    override fun getContentType(): String? {
        return "image/jpeg"
    }

    override fun isEmpty(): Boolean {
        return false
    }

    override fun getSize(): Long {
        return 1000
    }

    override fun getBytes(): ByteArray {
        return data
    }

    override fun transferTo(dest: File) {
    }
}
