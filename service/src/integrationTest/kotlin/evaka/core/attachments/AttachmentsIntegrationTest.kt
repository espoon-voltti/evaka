// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.attachments

import evaka.core.s3.ContentTypePattern
import evaka.core.s3.checkFileContentType
import evaka.core.s3.checkFileContentTypeAndExtension
import evaka.core.shared.domain.BadRequest
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AttachmentsIntegrationTest {
    private val pngFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.png")!!
    private val jpgFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.jpg")!!
    private val licenseFile =
        this::class.java.getResource("/attachments-fixtures/evaka-logo.jpg.license")!!
    private val mp4File = this::class.java.getResource("/attachments-fixtures/test-video.mp4")!!

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
                    checkFileContentType(file, ContentTypePattern.entries.toSet())
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

    @Test
    fun `video file is rejected when only default content types are allowed`() {
        mp4File.openStream().use { file ->
            assertThrows<BadRequest> {
                checkFileContentTypeAndExtension(
                    file,
                    "mp4",
                    listOf(
                        ContentTypePattern.JPEG,
                        ContentTypePattern.PNG,
                        ContentTypePattern.PDF,
                        ContentTypePattern.MSWORD,
                        ContentTypePattern.MSWORD_DOCX,
                        ContentTypePattern.OPEN_DOCUMENT_TEXT,
                        ContentTypePattern.TIKA_MSOFFICE,
                        ContentTypePattern.TIKA_OOXML,
                    ),
                )
            }
        }
    }

    @Test
    fun `video file is accepted when video content type is allowed`() {
        mp4File.openStream().use { file ->
            assertDoesNotThrow {
                checkFileContentTypeAndExtension(
                    file,
                    "mp4",
                    listOf(
                        ContentTypePattern.JPEG,
                        ContentTypePattern.PNG,
                        ContentTypePattern.PDF,
                        ContentTypePattern.MSWORD,
                        ContentTypePattern.MSWORD_DOCX,
                        ContentTypePattern.OPEN_DOCUMENT_TEXT,
                        ContentTypePattern.TIKA_MSOFFICE,
                        ContentTypePattern.TIKA_OOXML,
                        ContentTypePattern.VIDEO_ANY,
                        ContentTypePattern.AUDIO_ANY,
                    ),
                )
            }
        }
    }
}
