// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class TermsControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var termsController: TermsController

    private val today = LocalDate.of(2021, 1, 12)
    private val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 0)))
    private val adminUser =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.ADMIN)
        )
    private val serviceWorker =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.SERVICE_WORKER)
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `get preschool terms`() {
        val terms = termsController.getPreschoolTerms(dbInstance())

        assertEquals(5, terms.size)
    }

    @Test
    fun `create preschool term`() {
        assertEquals(5, termsController.getPreschoolTerms(dbInstance()).size)

        val preschoolTerm2025 =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2025, 8, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 8, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 1, 9), LocalDate.of(2025, 1, 19)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 10, 14), LocalDate.of(2025, 10, 18)),
                    FiniteDateRange(LocalDate.of(2025, 12, 21), LocalDate.of(2026, 1, 6)),
                    FiniteDateRange(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 21)),
                )
            )
        termsController.createPreschoolTerm(dbInstance(), adminUser, clock, preschoolTerm2025)

        val terms = termsController.getPreschoolTerms(dbInstance())
        assertEquals(6, terms.size)

        val createdTerm =
            terms.find { preschoolTerm ->
                preschoolTerm.finnishPreschool == preschoolTerm2025.finnishPreschool
            }
        assertNotNull(createdTerm)
        assertEquals(preschoolTerm2025.swedishPreschool, createdTerm.swedishPreschool)
        assertEquals(preschoolTerm2025.extendedTerm, createdTerm.extendedTerm)
        assertEquals(preschoolTerm2025.applicationPeriod, createdTerm.applicationPeriod)
        assertEquals(preschoolTerm2025.termBreaks, createdTerm.termBreaks)
    }

    @Test
    fun `should not create preschool term if user is not admin`() {
        assertEquals(5, termsController.getPreschoolTerms(dbInstance()).size)

        val preschoolTerm2025 =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2025, 8, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 8, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 1, 9), LocalDate.of(2025, 1, 19)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 10, 14), LocalDate.of(2025, 10, 18)),
                    FiniteDateRange(LocalDate.of(2025, 12, 21), LocalDate.of(2026, 1, 6)),
                    FiniteDateRange(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 21)),
                )
            )
        assertThrows<Forbidden> {
            termsController.createPreschoolTerm(
                dbInstance(),
                serviceWorker,
                clock,
                preschoolTerm2025
            )
        }

        val terms = termsController.getPreschoolTerms(dbInstance())
        assertEquals(5, terms.size)
        assertNull(
            terms.find { preschoolTerm ->
                preschoolTerm.finnishPreschool == preschoolTerm2025.finnishPreschool
            }
        )
    }

    @Test
    fun `should not create preschool term with overlapping finnish term period`() {
        assertEquals(5, termsController.getPreschoolTerms(dbInstance()).size)

        // Finnish and swedish term period overlaps with preschoolTerm2024 from general test
        // fixtures
        val preschoolTerm2025 =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2025, 3, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 3, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2024, 7, 9), LocalDate.of(2025, 1, 19)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 10, 14), LocalDate.of(2025, 10, 18)),
                    FiniteDateRange(LocalDate.of(2025, 12, 21), LocalDate.of(2026, 1, 6)),
                    FiniteDateRange(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 21)),
                )
            )
        assertThrows<BadRequest> {
            termsController.createPreschoolTerm(dbInstance(), adminUser, clock, preschoolTerm2025)
        }

        val terms = termsController.getPreschoolTerms(dbInstance())
        assertEquals(5, terms.size)
        assertNull(
            terms.find { preschoolTerm ->
                preschoolTerm.finnishPreschool == preschoolTerm2025.finnishPreschool
            }
        )
    }

    @Test
    fun `should not create preschool term with extended term that does not include the finnish or swedish term period`() {
        assertEquals(5, termsController.getPreschoolTerms(dbInstance()).size)

        // Extended term period does not include the whole finnish term period
        val firstPreschoolTerm2025 =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2025, 8, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 7, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 10, 1), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 1, 9), LocalDate.of(2025, 1, 19)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 10, 14), LocalDate.of(2025, 10, 18)),
                    FiniteDateRange(LocalDate.of(2025, 12, 21), LocalDate.of(2026, 1, 6)),
                    FiniteDateRange(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 21)),
                )
            )
        assertThrows<BadRequest> {
            termsController.createPreschoolTerm(
                dbInstance(),
                adminUser,
                clock,
                firstPreschoolTerm2025
            )
        }

        // Extended term period does not include the whole swedish term period
        val secondPreschoolTerm2025 =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2025, 8, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 7, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 1, 9), LocalDate.of(2025, 1, 19)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 10, 14), LocalDate.of(2025, 10, 18)),
                    FiniteDateRange(LocalDate.of(2025, 12, 21), LocalDate.of(2026, 1, 6)),
                    FiniteDateRange(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 21)),
                )
            )
        assertThrows<BadRequest> {
            termsController.createPreschoolTerm(
                dbInstance(),
                adminUser,
                clock,
                secondPreschoolTerm2025
            )
        }

        val terms = termsController.getPreschoolTerms(dbInstance())
        assertEquals(5, terms.size)
        assertNull(
            terms.find { preschoolTerm ->
                preschoolTerm.finnishPreschool == firstPreschoolTerm2025.finnishPreschool ||
                    preschoolTerm.finnishPreschool == secondPreschoolTerm2025.finnishPreschool
            }
        )
    }
}
