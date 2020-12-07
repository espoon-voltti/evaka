package fi.espoo.evaka.attachments

import fi.espoo.evaka.attachment.checkFileContentType
import fi.espoo.evaka.shared.domain.BadRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AttachmentsIntegrationTest {
    private val pngFile = this::class.java.getResource("/attachments-fixtures/espoo-logo.png")
    private val jpgFile = this::class.java.getResource("/attachments-fixtures/espoo-logo.jpg")

    @Test
    fun `file content type check works for png file`() {
        val file = pngFile.openStream()
        checkFileContentType(file, "image/png")
    }

    @Test
    fun `file content type check works for jpg file`() {
        val file = jpgFile.openStream()
        checkFileContentType(file, "image/jpeg")
    }

    @Test
    fun `file content type check throws with magic numbers and content type mismatch`() {
        val file = pngFile.openStream()
        assertThrows<BadRequest> { checkFileContentType(file, "application/pdf") }
    }
}
