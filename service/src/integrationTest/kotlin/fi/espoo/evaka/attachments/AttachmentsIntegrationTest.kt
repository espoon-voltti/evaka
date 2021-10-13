// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachments

import fi.espoo.evaka.s3.getAndCheckFileContentType
import fi.espoo.evaka.shared.domain.BadRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AttachmentsIntegrationTest {
    private val pngFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.png")
    private val jpgFile = this::class.java.getResource("/attachments-fixtures/evaka-logo.jpg")

    @Test
    fun `file content type check works for png file`() {
        val file = pngFile.openStream()
        getAndCheckFileContentType(file, listOf("image/png"))
    }

    @Test
    fun `file content type check works for jpg file`() {
        val file = jpgFile.openStream()
        getAndCheckFileContentType(file, listOf("image/jpeg"))
    }

    @Test
    fun `file content type check throws with magic numbers and content type mismatch`() {
        val file = pngFile.openStream()
        assertThrows<BadRequest> { getAndCheckFileContentType(file, listOf("application/pdf")) }
    }
}
