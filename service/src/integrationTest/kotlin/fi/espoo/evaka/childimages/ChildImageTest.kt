// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import com.github.kittinunf.fuel.core.BlobDataPart
import com.github.kittinunf.fuel.core.Method
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.s3.fuelResponseToS3URL
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insert
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

class ChildImageTest : FullApplicationTest(resetDbBeforeEach = true) {
    private final val adminId = EmployeeId(UUID.randomUUID())
    private val admin = AuthenticatedUser.Employee(adminId, setOf(UserRole.ADMIN))

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insert(DevEmployee(adminId, roles = setOf(UserRole.ADMIN)))
        }
    }

    @Test
    fun `image round trip`() {
        uploadImage(testChild_1.id, imageName1, imageData1)

        val images = db.read { it.createQuery("SELECT * FROM child_images").toList<ChildImage>() }
        assertEquals(1, images.size)

        val receivedData = downloadImage(images.first().id)
        assertContentEquals(imageData1, receivedData)
    }

    @Test
    fun `replacing image`() {
        uploadImage(testChild_1.id, imageName1, imageData1)
        val oldImage =
            db.read { it.createQuery("SELECT * FROM child_images").exactlyOne<ChildImage>() }

        uploadImage(testChild_1.id, imageName2, imageData2)

        val newImage =
            db.read { it.createQuery("SELECT * FROM child_images").exactlyOne<ChildImage>() }
        assertNotEquals(oldImage.id, newImage.id)

        val receivedData = downloadImage(newImage.id)
        assertContentEquals(imageData2, receivedData)
    }

    @Test
    fun `deleting image`() {
        uploadImage(testChild_1.id, imageName1, imageData1)
        deleteImage(testChild_1.id)

        val newImages =
            db.read { it.createQuery("SELECT * FROM child_images").toList<ChildImage>() }

        assertEquals(0, newImages.size)
    }

    @Test
    fun `image larger than 512x512 is not accepted`() {
        val image = BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB)
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", output)
        uploadImage(testChild_1.id, imageName1, output.toByteArray(), 400)
    }

    private val imageName1 = "test1.jpg"
    private val imageData1 =
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

    private val imageName2 = "test2.jpg"
    private val imageData2 =
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

    private fun uploadImage(
        childId: ChildId,
        fileName: String,
        fileData: ByteArray,
        statusCode: Int = 200
    ) {
        val (_, response, _) =
            http
                .upload("/children/$childId/image", Method.PUT)
                .add(BlobDataPart(fileData.inputStream(), "file", fileName))
                .asUser(admin)
                .response()
        assertEquals(statusCode, response.statusCode)
    }

    private fun deleteImage(childId: ChildId) {
        val (_, response, _) = http.delete("/children/$childId/image").asUser(admin).response()
        assertEquals(200, response.statusCode)
    }

    private fun downloadImage(imageId: ChildImageId): ByteArray {
        val (_, response, _) = http.get("/child-images/$imageId").asUser(admin).response()
        assertEquals(200, response.statusCode)

        val (_, _, data) = http.get(fuelResponseToS3URL(response)).response()
        return data.get()
    }
}
