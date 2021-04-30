// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DuplicateApplicationIntegrationTest : FullApplicationTest() {
    val childId = testChild_1.id
    val childId2 = testChild_2.id
    val guardianId = testAdult_1.id
    val unitId = testDaycare.id

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
        }
    }

    @Test
    fun `no applications means no duplicates`() {
        db.transaction { tx -> assertFalse(duplicateApplicationExists(tx.handle, childId, guardianId, ApplicationType.DAYCARE)) }
    }

    @Test
    fun `duplicate is found`() {
        addDaycareApplication()
        db.transaction { tx -> assertTrue(duplicateApplicationExists(tx.handle, childId, guardianId, ApplicationType.DAYCARE)) }
    }

    @Test
    fun `application for a different child is not a duplicate`() {
        addDaycareApplication()
        db.transaction { tx -> assertFalse(duplicateApplicationExists(tx.handle, childId2, guardianId, ApplicationType.DAYCARE)) }
    }

    @Test
    fun `application for a different type is not a duplicate`() {
        addDaycareApplication()
        db.transaction { tx -> assertFalse(duplicateApplicationExists(tx.handle, childId, guardianId, ApplicationType.PRESCHOOL)) }
    }

    @Test
    fun `finished applications are not duplicates`() {
        addDaycareApplication(ApplicationStatus.ACTIVE)
        addDaycareApplication(ApplicationStatus.REJECTED)
        addDaycareApplication(ApplicationStatus.CANCELLED)
        db.transaction { tx -> assertFalse(duplicateApplicationExists(tx.handle, childId, guardianId, ApplicationType.DAYCARE)) }
    }

    private fun addDaycareApplication(status: ApplicationStatus = ApplicationStatus.SENT) {
        db.transaction { tx ->
            val appId = insertTestApplication(
                h = tx.handle,
                status = status,
                childId = childId,
                guardianId = guardianId
            )
            insertTestApplicationForm(
                h = tx.handle,
                applicationId = appId,
                document = DaycareFormV0.fromApplication2(validDaycareApplication)
            )
        }
    }
}
