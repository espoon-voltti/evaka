// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DuplicateApplicationIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val guardian = DevPerson()
    private val child1 = DevPerson()
    private val child2 = DevPerson()

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
        }
    }

    @Test
    fun `no applications means no duplicates`() {
        db.transaction { tx ->
            assertFalse(
                tx.duplicateApplicationExists(child1.id, guardian.id, ApplicationType.DAYCARE)
            )
        }
    }

    @Test
    fun `duplicate is found`() {
        addDaycareApplication()
        db.transaction { tx ->
            assertTrue(
                tx.duplicateApplicationExists(child1.id, guardian.id, ApplicationType.DAYCARE)
            )
        }
    }

    @Test
    fun `application for a different child is not a duplicate`() {
        addDaycareApplication()
        db.transaction { tx ->
            assertFalse(
                tx.duplicateApplicationExists(child2.id, guardian.id, ApplicationType.DAYCARE)
            )
        }
    }

    @Test
    fun `application for a different type is not a duplicate`() {
        addDaycareApplication()
        db.transaction { tx ->
            assertFalse(
                tx.duplicateApplicationExists(child1.id, guardian.id, ApplicationType.PRESCHOOL)
            )
        }
    }

    @Test
    fun `finished applications are not duplicates`() {
        addDaycareApplication(ApplicationStatus.ACTIVE)
        addDaycareApplication(ApplicationStatus.REJECTED)
        addDaycareApplication(ApplicationStatus.CANCELLED)
        db.transaction { tx ->
            assertFalse(
                tx.duplicateApplicationExists(child1.id, guardian.id, ApplicationType.DAYCARE)
            )
        }
    }

    private fun addDaycareApplication(status: ApplicationStatus = ApplicationStatus.SENT) {
        db.transaction { tx ->
            tx.insertTestApplication(
                status = status,
                confidential =
                    if (
                        status in
                            listOf(
                                ApplicationStatus.CREATED,
                                ApplicationStatus.SENT,
                                ApplicationStatus.WAITING_PLACEMENT,
                            )
                    )
                        null
                    else true,
                childId = child1.id,
                guardianId = guardian.id,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                    ),
            )
        }
    }
}
