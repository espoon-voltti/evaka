// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachments

import fi.espoo.evaka.s3.ContentType
import fi.espoo.evaka.s3.checkFileExtension
import fi.espoo.evaka.s3.getAndCheckFileContentType
import fi.espoo.evaka.shared.domain.BadRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AttachmentsIntegrationTest {
    private val pngFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.png")
    private val jpgFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.jpg")
    private val licenseFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.jpg.license")

    @Test
    fun `file content type check works for png file`() {
        pngFile.openStream().use { file ->
            assertEquals("image/png", getAndCheckFileContentType(file, setOf(ContentType.PNG)))
        }
    }

    @Test
    fun `file content type check works for jpg file`() {
        jpgFile.openStream().use { file ->
            assertEquals("image/jpeg", getAndCheckFileContentType(file, setOf(ContentType.JPEG)))
        }
    }

    @Test
    fun `file content type check throws on disallowed content type`() {
        pngFile.openStream().use { file ->
            assertThrows<BadRequest> { getAndCheckFileContentType(file, setOf(ContentType.PDF)) }
        }
    }

    @Test
    fun `file content type check throws on invalid contentType`() {
        licenseFile.openStream().use { file ->
            val exception = assertThrows<BadRequest> {
                getAndCheckFileContentType(file, ContentType.values().toSet())
            }
            assertEquals("Invalid content type text/plain", exception.message)
        }
    }

    @Test
    fun `checkFileExtension does not throw on valid cases`() {
        val cases = listOf(
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "doc" to "application/msword"
        )
        cases.forEach { (extension, contentType) ->
            assertDoesNotThrow { checkFileExtension(extension, contentType) }
        }
    }

    @Test
    fun `checkFileExtension throws on invalid cases`() {
        val cases = listOf(
            "foo" to "bar",
            "png" to "image/jpeg",
            "pdf" to "application/msword"
        )
        cases.forEach { (extension, contentType) ->
            assertThrows<BadRequest> { checkFileExtension(extension, contentType) }
        }
    }
}
