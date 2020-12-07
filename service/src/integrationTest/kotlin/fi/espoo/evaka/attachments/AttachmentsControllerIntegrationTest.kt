package fi.espoo.evaka.attachments

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.deleteApplication
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.testAdult_5
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

class AttachmentsControllerIntegrationTest : FullApplicationTest() {

    private lateinit var user: AuthenticatedUser
    private val applicationId = UUID.randomUUID()
    private val pngFile = this::class.java.getResource("/attachments-fixtures/espoo-logo.png")

    @BeforeEach
    protected fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
            user = AuthenticatedUser(testAdult_5.id, setOf(UserRole.END_USER))
        }
    }

    @AfterEach
    protected fun tearDown() {
        jdbi.handle { h ->
            deleteApplication(h, applicationId)
        }
    }

    @Test
    fun `Enduser can upload attachments up to the limit`() {
        jdbi.handle { h ->
            val maxAttachments = env.getProperty("fi.espoo.evaka.maxAttachmentsPerUser").toInt()
            insertApplication(h, applicationId = applicationId, guardian = testAdult_5, status = ApplicationStatus.CREATED)

            for (i in 1..maxAttachments) {
                Assertions.assertTrue(uploadAttachment(applicationId))
            }
            Assertions.assertFalse(uploadAttachment(applicationId))
        }
    }

    private fun uploadAttachment(applicationId: UUID): Boolean {
        val (_, res, _) = http.upload("/attachments/enduser/applications/$applicationId", parameters = listOf("type" to "URGENCY"))
            .add(FileDataPart(File(pngFile.toURI()), name = "file"))
            .asUser(user)
            .response()

        return res.isSuccessful
    }
}
