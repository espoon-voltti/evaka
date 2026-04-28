// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.childimages

import evaka.core.FullApplicationTest
import evaka.core.s3.responseEntityToS3URL
import evaka.core.shared.ChildId
import evaka.core.shared.ChildImageId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevMobileDevice
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.utils.decodeHex
import evaka.core.shared.utils.trustAllCerts
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
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

    private val clock = MockEvakaClock(2024, 1, 1, 12, 0)

    private val child = DevPerson()
    private val area = DevCareArea()
    private val unit = DevDaycare(areaId = area.id)
    private val mobileDevice = DevMobileDevice(unitId = unit.id)
    private val mobileUser = AuthenticatedUser.MobileDevice(mobileDevice.id)

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insert(child, DevPersonType.CHILD)
            it.insert(area)
            it.insert(unit)
            it.insert(
                DevPlacement(
                    unitId = unit.id,
                    childId = child.id,
                    startDate = clock.today(),
                    endDate = clock.today().plusMonths(1),
                )
            )
            it.insert(mobileDevice)
        }
    }

    @Test
    fun `image round trip`() {
        uploadImage(child.id, image1)

        val images = db.read {
            it.createQuery { sql("SELECT * FROM child_images") }.toList<ChildImage>()
        }
        assertEquals(1, images.size)

        val receivedData = downloadImage(images.first().id)
        assertContentEquals(image1.bytes, receivedData)
    }

    @Test
    fun `replacing image`() {
        uploadImage(child.id, image1)
        val oldImage = db.read {
            it.createQuery { sql("SELECT * FROM child_images") }.exactlyOne<ChildImage>()
        }

        uploadImage(child.id, image2)

        val newImage = db.read {
            it.createQuery { sql("SELECT * FROM child_images") }.exactlyOne<ChildImage>()
        }
        assertNotEquals(oldImage.id, newImage.id)

        val receivedData = downloadImage(newImage.id)
        assertContentEquals(image2.bytes, receivedData)
    }

    @Test
    fun `deleting image`() {
        uploadImage(child.id, image1)
        deleteImage(child.id)

        val newImages = db.read {
            it.createQuery { sql("SELECT * FROM child_images") }.toList<ChildImage>()
        }

        assertEquals(0, newImages.size)
    }

    @Test
    fun `image larger than 512x512 is not accepted`() {
        val image = BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB)
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", output)

        assertThrows<BadRequest> {
            uploadImage(
                child.id,
                MockMultipartFile("file", "test1.jpg", "image/jpeg", output.toByteArray()),
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
                .decodeHex(),
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
                .decodeHex(),
        )

    private fun uploadImage(childId: ChildId, file: MultipartFile) {
        childImageController.putImage(dbInstance(), mobileUser, clock, childId, file)
    }

    private fun deleteImage(childId: ChildId) {
        childImageController.deleteImage(dbInstance(), mobileUser, clock, childId)
    }

    private fun downloadImage(imageId: ChildImageId): ByteArray {
        val response = childImageController.getImage(dbInstance(), mobileUser, clock, imageId)
        val http = okhttp3.OkHttpClient.Builder().apply { trustAllCerts(this) }.build()
        return http
            .newCall(okhttp3.Request.Builder().url(responseEntityToS3URL(response)).build())
            .execute()
            .body
            .bytes()
    }
}
