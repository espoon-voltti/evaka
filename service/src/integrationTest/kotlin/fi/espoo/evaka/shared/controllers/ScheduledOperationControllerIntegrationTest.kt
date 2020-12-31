// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.attachment.getApplicationAttachments
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.testAdult_5
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class ScheduledOperationControllerIntegrationTest : FullApplicationTest() {
    @Autowired
    private lateinit var scheduledOperationController: ScheduledOperationController

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
        }
    }

    @Test
    fun `Draft application and attachments older than 30 days is cleaned up`() {
        val id_to_be_deleted = UUID.randomUUID()
        val id_not_to_be_deleted = UUID.randomUUID()
        val user = AuthenticatedUser(testAdult_5.id, setOf(UserRole.END_USER))

        db.transaction { tx ->
            insertApplication(tx.handle, guardian = testAdult_5, applicationId = id_to_be_deleted)
            setApplicationCreatedDate(tx, id_to_be_deleted, LocalDate.now().minusDays(32))

            insertApplication(tx.handle, guardian = testAdult_5, applicationId = id_not_to_be_deleted)
            setApplicationCreatedDate(tx, id_not_to_be_deleted, LocalDate.now().minusDays(31))
        }

        db.transaction { _ ->
            uploadAttachment(id_to_be_deleted, user)
            uploadAttachment(id_not_to_be_deleted, user)
        }

        db.read {
            Assertions.assertEquals(1, it.getApplicationAttachments(id_to_be_deleted).size)
            Assertions.assertEquals(1, it.getApplicationAttachments(id_not_to_be_deleted).size)
        }

        scheduledOperationController.removeOldDraftApplications(db)

        db.read {
            Assertions.assertNull(fetchApplicationDetails(it.handle, id_to_be_deleted))
            Assertions.assertEquals(0, it.getApplicationAttachments(id_to_be_deleted).size)

            Assertions.assertNotNull(fetchApplicationDetails(it.handle, id_not_to_be_deleted)!!)
            Assertions.assertEquals(1, it.getApplicationAttachments(id_not_to_be_deleted).size)
        }
    }

    private fun setApplicationCreatedDate(db: Database.Transaction, applicationId: UUID, created: LocalDate) {
        db.handle.createUpdate("""UPDATE application SET created = :created WHERE id = :id""")
            .bind("created", created)
            .bind("id", applicationId)
            .execute()
    }
}
