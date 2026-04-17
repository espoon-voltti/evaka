// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.pis.getParentships
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFosterParent
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FamilyInitializerServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var familyInitializerService: FamilyInitializerService

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(12, 0)))

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val employee = DevEmployee()

    private val adult =
        DevPerson(
            ssn = "010180-1232",
            firstName = "Aikuinen",
            lastName = "Meikäläinen",
            streetAddress = "Katu 1",
            postalCode = "02100",
            postOffice = "Espoo",
        )
    private val child =
        DevPerson(
            ssn = "010617A123U",
            firstName = "Lapsi",
            lastName = "Meikäläinen",
            dateOfBirth = LocalDate.of(2017, 6, 1),
            streetAddress = "Katu 1",
            postalCode = "02100",
            postOffice = "Espoo",
        )
    private val child2 =
        DevPerson(
            ssn = "101221A999S",
            firstName = "Lapsi2",
            lastName = "Meikäläinen",
            dateOfBirth = LocalDate.of(2021, 12, 10),
            streetAddress = "Katu 1",
            postalCode = "02100",
            postOffice = "Espoo",
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(employee)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = adult.id, childId = child.id))
            tx.insert(DevGuardian(guardianId = adult.id, childId = child2.id))
        }
        MockPersonDetailsService.addPersons(adult, child, child2)
        MockPersonDetailsService.addDependants(adult, child, child2)
    }

    @Test
    fun `do not create parentship for child with active foster parent when initializing family from application`() {
        val fosterParent =
            DevPerson(ssn = "150786-1766", firstName = "Sijainen", lastName = "Huoltaja")
        db.transaction { tx ->
            tx.insert(fosterParent, DevPersonType.ADULT)
            tx.insert(
                DevFosterParent(
                    childId = child.id,
                    parentId = fosterParent.id,
                    validDuring =
                        DateRange(clock.today().minusMonths(1), clock.today().plusMonths(1)),
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    modifiedAt = clock.now(),
                )
            )
        }

        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = adult.id,
                    childId = child.id,
                    status = ApplicationStatus.WAITING_PLACEMENT,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child =
                                Child(
                                    dateOfBirth = child.dateOfBirth,
                                    socialSecurityNumber = child.ssn ?: "",
                                ),
                            guardian = Adult(socialSecurityNumber = adult.ssn ?: ""),
                        ),
                )
            }

        familyInitializerService.handleInitializeFamilyFromApplication(
            db,
            clock,
            AsyncJob.InitializeFamilyFromApplication(
                applicationId,
                AuthenticatedUser.SystemInternalUser,
            ),
        )

        // Foster child should not have a parentship
        db.read { tx ->
            assertEquals(
                0,
                tx.getParentships(adult.id, child.id, false, DateRange(clock.today(), null)).size,
            )
        }
    }

    @Test
    fun `do not create parentship for sibling with active foster parent when initializing family from application`() {
        // child2 is listed as a sibling on the application and has a foster parent
        val fosterParent =
            DevPerson(ssn = "150786-1766", firstName = "Sijainen", lastName = "Huoltaja")
        db.transaction { tx ->
            tx.insert(fosterParent, DevPersonType.ADULT)
            tx.insert(
                DevFosterParent(
                    childId = child2.id,
                    parentId = fosterParent.id,
                    validDuring =
                        DateRange(clock.today().minusMonths(1), clock.today().plusMonths(1)),
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    modifiedAt = clock.now(),
                )
            )
        }

        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = adult.id,
                    childId = child.id,
                    status = ApplicationStatus.WAITING_PLACEMENT,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child =
                                Child(
                                    dateOfBirth = child.dateOfBirth,
                                    socialSecurityNumber = child.ssn ?: "",
                                ),
                            guardian = Adult(socialSecurityNumber = adult.ssn ?: ""),
                            hasOtherChildren = true,
                            otherChildren =
                                listOf(
                                    evaka.core.application.persistence.daycare.OtherPerson(
                                        firstName = child2.firstName,
                                        lastName = child2.lastName,
                                        socialSecurityNumber = child2.ssn ?: "",
                                    )
                                ),
                        ),
                )
            }

        familyInitializerService.handleInitializeFamilyFromApplication(
            db,
            clock,
            AsyncJob.InitializeFamilyFromApplication(
                applicationId,
                AuthenticatedUser.SystemInternalUser,
            ),
        )

        // The application child (without foster parent) should have a parentship
        db.read { tx ->
            assertEquals(
                1,
                tx.getParentships(adult.id, child.id, false, DateRange(clock.today(), null)).size,
            )
        }

        // The sibling with foster parent should NOT have a parentship
        db.read { tx ->
            assertEquals(
                0,
                tx.getParentships(adult.id, child2.id, false, DateRange(clock.today(), null)).size,
            )
        }
    }
}
