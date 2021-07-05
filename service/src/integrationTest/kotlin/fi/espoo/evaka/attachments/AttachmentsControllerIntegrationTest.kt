// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachments

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.deleteApplication
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.testAdult_5
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class AttachmentsControllerIntegrationTest : FullApplicationTest() {

    private lateinit var user: AuthenticatedUser
    private val applicationId = ApplicationId(UUID.randomUUID())

    @BeforeEach
    protected fun beforeEach() {
        db.transaction {
            it.resetDatabase()
            it.insertGeneralTestFixtures()
        }
        user = AuthenticatedUser.Citizen(testAdult_5.id)
    }

    @AfterEach
    protected fun tearDown() {
        db.transaction {
            it.deleteApplication(applicationId)
        }
    }

    @Test
    fun `Enduser can upload attachments up to the limit`() {
        val maxAttachments = env.getProperty("fi.espoo.evaka.maxAttachmentsPerUser")!!.toInt()
        db.transaction {
            it.insertApplication(applicationId = applicationId, guardian = testAdult_5, status = ApplicationStatus.CREATED)
        }
        for (i in 1..maxAttachments) {
            Assertions.assertTrue(uploadAttachment(applicationId, user))
        }
        Assertions.assertFalse(uploadAttachment(applicationId, user))
    }
}
