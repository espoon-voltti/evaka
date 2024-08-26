// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.getPreschoolTerm
import fi.espoo.evaka.preschoolTerm2023
import fi.espoo.evaka.preschoolTerm2024
import fi.espoo.evaka.preschoolTerms
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevPreschoolTerm
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class TermsControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var termsController: TermsController

    private val today = LocalDate.of(2024, 1, 12)
    private val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 0)))
    private val adminUser =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.ADMIN),
        )
    private val serviceWorker =
        AuthenticatedUser.Employee(
            id = EmployeeId(UUID.randomUUID()),
            roles = setOf(UserRole.SERVICE_WORKER),
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> preschoolTerms.forEach { tx.insert(it) } }
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
                ),
            )
        termsController.createPreschoolTerm(dbInstance(), adminUser, clock, preschoolTerm2025)
        val terms = termsController.getPreschoolTerms(dbInstance())

        assertEquals(6, terms.size)

        val createdTerm =
            terms.find { term -> term.finnishPreschool == preschoolTerm2025.finnishPreschool }!!

        assertPreschoolTermFromRequest(preschoolTerm2025, createdTerm.id)
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
                ),
            )
        assertThrows<Forbidden> {
            termsController.createPreschoolTerm(
                dbInstance(),
                serviceWorker,
                clock,
                preschoolTerm2025,
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
                ),
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
    fun `should not create preschool term with overlapping extended term period`() {
        assertEquals(5, termsController.getPreschoolTerms(dbInstance()).size)

        // Extended term period overlaps with preschoolTerm2024 from general test fixtures
        val preschoolTerm2025 =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2025, 8, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 8, 8), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 5, 20), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 1, 9), LocalDate.of(2025, 1, 19)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 10, 14), LocalDate.of(2025, 10, 18)),
                    FiniteDateRange(LocalDate.of(2025, 12, 21), LocalDate.of(2026, 1, 6)),
                    FiniteDateRange(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 21)),
                ),
            )
        assertThrows<BadRequest> {
            termsController.createPreschoolTerm(dbInstance(), adminUser, clock, preschoolTerm2025)
        }

        val terms = termsController.getPreschoolTerms(dbInstance())
        assertEquals(5, terms.size)
        assertNull(
            terms.find { preschoolTerm ->
                preschoolTerm.extendedTerm == preschoolTerm2025.extendedTerm
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
                ),
            )
        assertThrows<BadRequest> {
            termsController.createPreschoolTerm(
                dbInstance(),
                adminUser,
                clock,
                firstPreschoolTerm2025,
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
                ),
            )
        assertThrows<BadRequest> {
            termsController.createPreschoolTerm(
                dbInstance(),
                adminUser,
                clock,
                secondPreschoolTerm2025,
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

    @Test
    fun `update preschool term`() {
        val allTerms = termsController.getPreschoolTerms(dbInstance())
        assertEquals(5, allTerms.size)

        val existingTerm = allTerms.find { term -> term.id == preschoolTerm2024.id }!!
        assertEquals(preschoolTerm2024.swedishPreschool, existingTerm.swedishPreschool)
        assertEquals(preschoolTerm2024.extendedTerm, existingTerm.extendedTerm)
        assertEquals(preschoolTerm2024.applicationPeriod, existingTerm.applicationPeriod)
        assertEquals(preschoolTerm2024.termBreaks, existingTerm.termBreaks)

        val preschoolTermUpdate =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2024, 8, 8), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 8, 8), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 3, 9), LocalDate.of(2024, 3, 25)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 1, 14), LocalDate.of(2025, 1, 18)),
                    FiniteDateRange(LocalDate.of(2025, 2, 21), LocalDate.of(2025, 2, 28)),
                ),
            )
        termsController.updatePreschoolTerm(
            dbInstance(),
            adminUser,
            clock,
            preschoolTerm2024.id,
            preschoolTermUpdate,
        )

        val updatedTerm = db.transaction { tx -> tx.getPreschoolTerm(preschoolTerm2024.id)!! }
        assertEquals(preschoolTermUpdate.swedishPreschool, updatedTerm.swedishPreschool)
        assertEquals(preschoolTermUpdate.extendedTerm, updatedTerm.extendedTerm)
        assertEquals(preschoolTermUpdate.applicationPeriod, updatedTerm.applicationPeriod)
        assertEquals(preschoolTermUpdate.termBreaks, updatedTerm.termBreaks)

        assertPreschoolTermFromRequest(preschoolTermUpdate, preschoolTerm2024.id)

        assertEquals(5, termsController.getPreschoolTerms(dbInstance()).size)
    }

    @Test
    fun `should not update preschool term if user is not admin`() {
        val preschoolTermUpdate =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2024, 8, 8), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 8, 8), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 3, 9), LocalDate.of(2024, 3, 25)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 1, 14), LocalDate.of(2025, 1, 18)),
                    FiniteDateRange(LocalDate.of(2025, 2, 21), LocalDate.of(2025, 2, 28)),
                ),
            )
        assertThrows<Forbidden> {
            termsController.updatePreschoolTerm(
                dbInstance(),
                serviceWorker,
                clock,
                preschoolTerm2024.id,
                preschoolTermUpdate,
            )
        }

        assertPreschoolTerm(preschoolTerm2024, preschoolTerm2024.id)
    }

    @Test
    fun `should not update preschool term with overlapping finnish term period`() {
        // Finnish and swedish term period overlaps with preschoolTerm2023 from general test
        // fixtures
        val preschoolTermUpdate2024 =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2024, 5, 25), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2024, 5, 25), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2026, 5, 30)),
                FiniteDateRange(LocalDate.of(2024, 7, 9), LocalDate.of(2025, 1, 19)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 10, 14), LocalDate.of(2025, 10, 18)),
                    FiniteDateRange(LocalDate.of(2025, 12, 21), LocalDate.of(2026, 1, 6)),
                    FiniteDateRange(LocalDate.of(2026, 2, 17), LocalDate.of(2026, 2, 21)),
                ),
            )
        assertThrows<BadRequest> {
            termsController.updatePreschoolTerm(
                dbInstance(),
                adminUser,
                clock,
                preschoolTerm2024.id,
                preschoolTermUpdate2024,
            )
        }

        // Verify no fields have been updated for preschool term 2024
        assertPreschoolTerm(preschoolTerm2024, preschoolTerm2024.id)
    }

    @Test
    fun `should not update preschool term with overlapping extended term period`() {
        // Extended term period overlaps with preschoolTerm2023 from general test fixtures
        val preschoolTermUpdate2024 =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2024, 8, 8), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 8, 8), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 6, 1), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 3, 9), LocalDate.of(2024, 3, 25)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 1, 14), LocalDate.of(2025, 1, 18)),
                    FiniteDateRange(LocalDate.of(2025, 2, 21), LocalDate.of(2025, 2, 28)),
                ),
            )
        assertThrows<BadRequest> {
            termsController.updatePreschoolTerm(
                dbInstance(),
                adminUser,
                clock,
                preschoolTerm2024.id,
                preschoolTermUpdate2024,
            )
        }

        // Verify no fields have been updated for preschool term 2024
        assertPreschoolTerm(preschoolTerm2024, preschoolTerm2024.id)
    }

    @Test
    fun `should not update preschool term with extended term that does not include the finnish or swedish term period`() {
        // Extended term period does not include the whole finnish term period
        val firstPreschoolTermUpdate =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2024, 7, 1), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 7, 15), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 3, 9), LocalDate.of(2024, 3, 25)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 1, 14), LocalDate.of(2025, 1, 18)),
                    FiniteDateRange(LocalDate.of(2025, 2, 21), LocalDate.of(2025, 2, 28)),
                ),
            )
        assertThrows<BadRequest> {
            termsController.updatePreschoolTerm(
                dbInstance(),
                adminUser,
                clock,
                preschoolTerm2024.id,
                firstPreschoolTermUpdate,
            )
        }

        // Extended term period does not include the whole swedish term period
        val secondPreschoolTermUpdate =
            TermsController.PreschoolTermRequest(
                FiniteDateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 7, 1), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 7, 15), LocalDate.of(2025, 8, 30)),
                FiniteDateRange(LocalDate.of(2024, 3, 9), LocalDate.of(2024, 3, 25)),
                DateSet.of(
                    FiniteDateRange(LocalDate.of(2025, 1, 14), LocalDate.of(2025, 1, 18)),
                    FiniteDateRange(LocalDate.of(2025, 2, 21), LocalDate.of(2025, 2, 28)),
                ),
            )
        assertThrows<BadRequest> {
            termsController.updatePreschoolTerm(
                dbInstance(),
                adminUser,
                clock,
                preschoolTerm2024.id,
                secondPreschoolTermUpdate,
            )
        }

        // Verify no fields have been updated for preschool term 2024
        assertPreschoolTerm(preschoolTerm2024, preschoolTerm2024.id)
    }

    @Test
    fun `delete preschool term`() {
        assertEquals(5, termsController.getPreschoolTerms(dbInstance()).size)

        termsController.deletePreschoolTerm(dbInstance(), adminUser, clock, preschoolTerm2024.id)
        val termsFinal = termsController.getPreschoolTerms(dbInstance())

        assertEquals(4, termsFinal.size)
        assertNull(termsFinal.find { preschoolTerm -> preschoolTerm.id == preschoolTerm2024.id })
    }

    @Test
    fun `should not delete preschool term that has started`() {
        assertEquals(5, termsController.getPreschoolTerms(dbInstance()).size)

        assertThrows<BadRequest> {
            termsController.deletePreschoolTerm(
                dbInstance(),
                adminUser,
                clock,
                preschoolTerm2023.id,
            )
        }

        assertEquals(5, termsController.getPreschoolTerms(dbInstance()).size)
    }

    private fun assertPreschoolTerm(expected: DevPreschoolTerm, targetId: PreschoolTermId) {
        val existingTerm = db.transaction { tx -> tx.getPreschoolTerm(targetId)!! }
        assertEquals(expected.finnishPreschool, existingTerm.finnishPreschool)
        assertEquals(expected.swedishPreschool, existingTerm.swedishPreschool)
        assertEquals(expected.extendedTerm, existingTerm.extendedTerm)
        assertEquals(expected.applicationPeriod, existingTerm.applicationPeriod)
        assertEquals(expected.termBreaks, existingTerm.termBreaks)
    }

    private fun assertPreschoolTermFromRequest(
        expected: TermsController.PreschoolTermRequest,
        targetId: PreschoolTermId,
    ) {
        val existingTerm = db.transaction { tx -> tx.getPreschoolTerm(targetId)!! }
        assertEquals(expected.finnishPreschool, existingTerm.finnishPreschool)
        assertEquals(expected.swedishPreschool, existingTerm.swedishPreschool)
        assertEquals(expected.extendedTerm, existingTerm.extendedTerm)
        assertEquals(expected.applicationPeriod, existingTerm.applicationPeriod)
        assertEquals(expected.termBreaks, existingTerm.termBreaks)
    }
}
