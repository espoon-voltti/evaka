// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.aromi

import fi.espoo.evaka.AromiEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.sftp.SftpClient
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

class AromiServiceTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var aromiService: AromiService
    @Autowired private lateinit var aromiEnv: AromiEnv

    private val clock = MockEvakaClock(2025, 3, 3, 10, 53, 22)

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        name = "Päiväkoti A",
                        shiftCareOpenOnHolidays = true,
                        shiftCareOperationTimes =
                            List(7) { TimeRange(LocalTime.of(7, 0), LocalTime.of(23, 0)) },
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(7, 0), LocalTime.of(17, 0)) } +
                                listOf(null, null),
                    )
                )
            val groupId =
                tx.insert(
                    DevDaycareGroup(
                        daycareId = unitId,
                        name = "Ryhmä A1",
                        aromiCustomerId = "PvkA_PK",
                    )
                )
            val testPerson =
                DevPerson(
                    firstName = "Teemu",
                    lastName = "Testi",
                    ssn = null,
                    dateOfBirth = LocalDate.of(2020, 5, 1),
                    id = PersonId(UUID.fromString("b707a2ac-c411-4ac8-a38a-840d0767f8bb")),
                )

            tx.insert(testPerson, DevPersonType.CHILD).also { childId ->
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = unitId,
                            startDate = clock.today().plusDays(3),
                            endDate = clock.today().plusDays(4),
                            type = PlacementType.DAYCARE,
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        startDate = clock.today().plusDays(3),
                        endDate = clock.today().plusDays(4),
                        daycareGroupId = groupId,
                        daycarePlacementId = placementId,
                    )
                )
            }
        }
    }

    @Test
    fun `sendOrders uploads csv to sftp server`() {
        val naturalWindowStart = clock.today().plusDays(3)
        aromiService.sendOrders(db, clock, naturalWindowStart.minusYears(1))

        val sftpClient = SftpClient(aromiEnv.sftp)
        val data = sftpClient.getAsString("upload/EVAKA03032025.csv", StandardCharsets.ISO_8859_1)
        assertEquals(
            ClassPathResource("aromi/simple_case_full.csv")
                .getContentAsString(StandardCharsets.ISO_8859_1),
            data,
        )
    }

    @Test
    fun `sendOrders sets data pick up range based on earliest start date`() {
        val naturalWindowStart = clock.today().plusDays(3)
        aromiService.sendOrders(db, clock, naturalWindowStart.plusDays(1))

        val sftpClient = SftpClient(aromiEnv.sftp)
        val data = sftpClient.getAsString("upload/EVAKA03032025.csv", StandardCharsets.ISO_8859_1)
        assertEquals(
            ClassPathResource("aromi/simple_case_partial.csv")
                .getContentAsString(StandardCharsets.ISO_8859_1),
            data,
        )
    }

    @Test
    fun `invalid order range results in error`() {
        val naturalWindowEnd = clock.today().plusDays(21)
        assertThrows<IllegalStateException> {
            aromiService.sendOrders(db, clock, naturalWindowEnd.plusDays(1))
        }
    }
}
