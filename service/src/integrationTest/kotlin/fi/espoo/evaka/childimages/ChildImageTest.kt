// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.s3.responseEntityToS3URL
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.utils.decodeHex
import fi.espoo.evaka.testChild_1
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile

class ChildImageTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var childImageController: ChildImageController

    private final val adminId = EmployeeId(UUID.randomUUID())
    private val admin = AuthenticatedUser.Employee(adminId, setOf(UserRole.ADMIN))

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insert(testChild_1, DevPersonType.CHILD)
            it.insert(DevEmployee(adminId, roles = setOf(UserRole.ADMIN)))
        }
    }

    @Test
    fun `image round trip`() {
        uploadImage(testChild_1.id, image1)

        val images =
            db.read { it.createQuery { sql("SELECT * FROM child_images") }.toList<ChildImage>() }
        assertEquals(1, images.size)

        val receivedData = downloadImage(images.first().id)
        assertContentEquals(image1.bytes, receivedData)
    }

    @Test
    fun `replacing image`() {
        uploadImage(testChild_1.id, image1)
        val oldImage =
            db.read {
                it.createQuery { sql("SELECT * FROM child_images") }.exactlyOne<ChildImage>()
            }

        uploadImage(testChild_1.id, image2)

        val newImage =
            db.read {
                it.createQuery { sql("SELECT * FROM child_images") }.exactlyOne<ChildImage>()
            }
        assertNotEquals(oldImage.id, newImage.id)

        val receivedData = downloadImage(newImage.id)
        assertContentEquals(image2.bytes, receivedData)
    }

    @Test
    fun `deleting image`() {
        uploadImage(testChild_1.id, image1)
        deleteImage(testChild_1.id)

        val newImages =
            db.read { it.createQuery { sql("SELECT * FROM child_images") }.toList<ChildImage>() }

        assertEquals(0, newImages.size)
    }

    @Test
    fun `image larger than 512x512 is not accepted`() {
        val image = BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB)
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", output)

        assertThrows<BadRequest> {
            uploadImage(
                testChild_1.id,
                MockMultipartFile("file", "test1.jpg", "image/jpeg", output.toByteArray())
            )
        }
    }

    private val image1 =
        MockMultipartFile(
            "file",
            "test1.jpg",
            "image/jpeg",
            """
FF D8 FF E0 00 10 4A 46 49 46 00 01 01 01 00 48 00 48 00 00
FF DB 00 43 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
FF FF FF FF FF FF FF FF FF FF C2 00 0B 08 00 01 00 01 01 01
11 00 FF C4 00 14 10 01 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 FF DA 00 08 01 01 00 01 3F 10
"""
                .decodeHex()
        )

    private val image2 =
        MockMultipartFile(
            "file",
            "test2.jpg",
            "image/jpeg",
            """
FF D8 FF E0 00 10 4A 46 49 46 00 01 01 01 00 48 00 48 00 00
FF DB 00 43 00 FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
FF FF FF FF FF FF FF FF FF FF C2 00 0B 08 00 01 00 01 01 01
11 00 FF C4 00 14 10 01 00 00 00 00 00 00 00 00 00 00 00 00
00 00 00 00 FF DA 00 08 01 01 00 01 3F 01
"""
                .decodeHex()
        )

    private fun uploadImage(
        childId: ChildId,
        file: MultipartFile,
    ) {
        childImageController.putImage(dbInstance(), admin, RealEvakaClock(), childId, file)
    }

    private fun deleteImage(childId: ChildId) {
        childImageController.deleteImage(dbInstance(), admin, RealEvakaClock(), childId)
    }

    private fun downloadImage(imageId: ChildImageId): ByteArray {
        val response = childImageController.getImage(dbInstance(), admin, RealEvakaClock(), imageId)
        val (_, _, data) = http.get(responseEntityToS3URL(response)).response()
        return data.get()
    }
}
