// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachments

import fi.espoo.evaka.s3.ContentTypePattern
import fi.espoo.evaka.s3.checkFileContentType
import fi.espoo.evaka.s3.checkFileContentTypeAndExtension
import fi.espoo.evaka.shared.domain.BadRequest
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AttachmentsIntegrationTest {
    private val pngFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.png")!!
    private val jpgFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.jpg")!!
    private val licenseFile =
        this::class.java.getResource("/attachments-fixtures/evaka-logo.jpg.license")!!

    @Test
    fun `file content type check works for png file`() {
        pngFile.openStream().use { file ->
            assertEquals("image/png", checkFileContentType(file, setOf(ContentTypePattern.PNG)))
        }
    }

    @Test
    fun `file content type check works for jpg file`() {
        jpgFile.openStream().use { file ->
            assertEquals("image/jpeg", checkFileContentType(file, setOf(ContentTypePattern.JPEG)))
        }
    }

    @Test
    fun `file content type check throws on disallowed content type`() {
        pngFile.openStream().use { file ->
            assertThrows<BadRequest> { checkFileContentType(file, setOf(ContentTypePattern.PDF)) }
        }
    }

    @Test
    fun `file content type check throws on invalid contentType`() {
        licenseFile.openStream().use { file ->
            val exception =
                assertThrows<BadRequest> {
                    checkFileContentType(file, ContentTypePattern.values().toSet())
                }
            assertEquals("Invalid content type text/plain", exception.message)
        }
    }

    @Test
    fun `checkFileExtension does not throw on valid cases`() {
        val cases = listOf("jpeg" to jpgFile, "png" to pngFile)
        cases.forEach { (extension, file) ->
            file.openStream().use { stream ->
                assertDoesNotThrow {
                    checkFileContentTypeAndExtension(
                        stream,
                        extension,
                        listOf(ContentTypePattern.JPEG, ContentTypePattern.PNG),
                    )
                }
            }
        }
    }

    @Test
    fun `checkFileExtension throws on invalid cases`() {
        val cases = listOf("foo" to jpgFile, "pdf" to pngFile)
        cases.forEach { (extension, file) ->
            file.openStream().use { stream ->
                assertThrows<BadRequest> {
                    checkFileContentTypeAndExtension(
                        stream,
                        extension,
                        listOf(ContentTypePattern.JPEG, ContentTypePattern.PNG),
                    )
                }
            }
        }
    }
}
