// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.aromi

import evaka.core.AromiEnv
import evaka.core.FullApplicationTest
import evaka.core.placement.PlacementType
import evaka.core.shared.PersonId
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.TimeRange
import evaka.core.shared.sftp.SftpClient
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
                val placement =
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = clock.today(),
                        endDate = clock.today().plusDays(4),
                        type = PlacementType.DAYCARE,
                    )
                tx.insert(placement)

                tx.insert(
                    DevDaycareGroupPlacement(
                        startDate = placement.startDate,
                        endDate = placement.endDate,
                        daycareGroupId = groupId,
                        daycarePlacementId = placement.id,
                    )
                )
            }
        }
    }

    @Test
    fun `sendOrders uploads correct prediction csv to sftp server with default offsets`() {
        aromiService.sendOrders(db, clock)

        val sftpClient = SftpClient(aromiEnv.sftp)
        val data = sftpClient.getAsString("upload/EVAKA03032025.csv", StandardCharsets.ISO_8859_1)
        assertEquals(
            ClassPathResource("aromi/simple_case_partial.csv")
                .getContentAsString(StandardCharsets.ISO_8859_1),
            data,
        )
    }

    @Test
    fun `sendOrders uploads correct prediction csv to sftp server with alternate offsets`() {
        val alternateAromiService =
            AromiService(aromiEnv.copy(windowStartOffset = 1, windowEndOffset = 21))
        alternateAromiService.sendOrders(db, clock)

        val sftpClient = SftpClient(aromiEnv.sftp)
        val data = sftpClient.getAsString("upload/EVAKA03032025.csv", StandardCharsets.ISO_8859_1)
        assertEquals(
            ClassPathResource("aromi/simple_case_full.csv")
                .getContentAsString(StandardCharsets.ISO_8859_1),
            data,
        )
    }

    @Test
    fun `empty attendance result list leads to error`() {
        val clockBeforePlacements = MockEvakaClock(2025, 1, 1, 10, 53, 22)
        assertThrows<IllegalStateException> { aromiService.sendOrders(db, clockBeforePlacements) }
    }
}
