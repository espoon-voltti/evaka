// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.ServiceNeedOption
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PlacementSketchingReportControllerTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var placementSketchingReportController: PlacementSketchingReportController

    private val serviceWorker =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.SERVICE_WORKER))
    private val financeAdmin =
        AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.FINANCE_ADMIN))

    @Test
    fun `service worker is allowed to fetch report`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 8), LocalTime.of(12, 15)))

        val report =
            placementSketchingReportController.getPlacementSketchingReport(
                dbInstance(),
                serviceWorker,
                clock,
                LocalDate.of(2022, 1, 1),
                LocalDate.of(2022, 8, 1),
            )

        assertThat(report).isEmpty()
    }

    @Test
    fun `finance admin is not allowed to fetch report`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 8), LocalTime.of(12, 15)))

        val thrown = catchThrowable {
            placementSketchingReportController.getPlacementSketchingReport(
                dbInstance(),
                financeAdmin,
                clock,
                LocalDate.of(2022, 1, 1),
                LocalDate.of(2022, 8, 1),
            )
        }

        assertThat(thrown).isInstanceOf(Forbidden::class.java)
    }

    @Test
    fun `report works without application service need option`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 8), LocalTime.of(12, 15)))
        val serviceNeedOption = null
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val childId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            tx.insertTestApplication(
                type = ApplicationType.PRESCHOOL,
                sentDate = LocalDate.of(2022, 8, 1),
                dueDate = LocalDate.of(2022, 12, 1),
                guardianId = guardianId,
                childId = childId,
                document =
                    DaycareFormV0(
                        type = ApplicationType.PRESCHOOL,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        preferredStartDate = LocalDate.of(2022, 12, 1),
                        apply = Apply(preferredUnits = listOf(unitId)),
                        serviceNeedOption = serviceNeedOption,
                    ),
            )
        }

        val report =
            placementSketchingReportController.getPlacementSketchingReport(
                dbInstance(),
                serviceWorker,
                clock,
                LocalDate.of(2022, 1, 1),
                LocalDate.of(2022, 8, 1),
            )

        assertThat(report)
            .extracting<ServiceNeedOption> { it.serviceNeedOption }
            .containsExactly(null)
    }

    @Test
    fun `report works with application service need option`() {
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 8), LocalTime.of(12, 15)))
        val serviceNeedOption =
            ServiceNeedOption(ServiceNeedOptionId(UUID.randomUUID()), "", "", "", null)
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val childId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            tx.insertTestApplication(
                type = ApplicationType.PRESCHOOL,
                sentDate = LocalDate.of(2022, 8, 1),
                dueDate = LocalDate.of(2022, 12, 1),
                guardianId = guardianId,
                childId = childId,
                document =
                    DaycareFormV0(
                        type = ApplicationType.PRESCHOOL,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        preferredStartDate = LocalDate.of(2022, 12, 1),
                        apply = Apply(preferredUnits = listOf(unitId)),
                        serviceNeedOption = serviceNeedOption,
                    ),
            )
        }

        val report =
            placementSketchingReportController.getPlacementSketchingReport(
                dbInstance(),
                serviceWorker,
                clock,
                LocalDate.of(2022, 1, 1),
                LocalDate.of(2022, 8, 1),
            )

        assertThat(report)
            .extracting<ServiceNeedOption> { it.serviceNeedOption }
            .containsExactly(serviceNeedOption)
    }
}
