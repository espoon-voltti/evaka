// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision.reasoning

import evaka.core.FullApplicationTest
import evaka.core.shared.DecisionGenericReasoningId
import evaka.core.shared.DecisionIndividualReasoningId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DecisionReasoningIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: DecisionReasoningController

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 15), LocalTime.of(12, 0))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(serviceWorker)
        }
    }

    private fun createGenericReasoning(
        request: DecisionGenericReasoningRequest,
        user: AuthenticatedUser.Employee = admin.user,
        time: HelsinkiDateTime = now,
    ): DecisionGenericReasoningId =
        controller.createGenericReasoning(dbInstance(), user, MockEvakaClock(time), request)

    private fun getGenericReasonings(
        collectionType: DecisionReasoningCollectionType,
        user: AuthenticatedUser.Employee = admin.user,
        time: HelsinkiDateTime = now,
    ): List<DecisionGenericReasoning> =
        controller.getGenericReasonings(dbInstance(), user, MockEvakaClock(time), collectionType)

    private fun updateGenericReasoning(
        id: DecisionGenericReasoningId,
        request: DecisionGenericReasoningRequest,
        user: AuthenticatedUser.Employee = admin.user,
        time: HelsinkiDateTime = now,
    ) = controller.updateGenericReasoning(dbInstance(), user, MockEvakaClock(time), id, request)

    private fun deleteGenericReasoning(
        id: DecisionGenericReasoningId,
        user: AuthenticatedUser.Employee = admin.user,
    ) = controller.deleteGenericReasoning(dbInstance(), user, MockEvakaClock(now), id)

    private fun createIndividualReasoning(
        request: DecisionIndividualReasoningRequest,
        user: AuthenticatedUser.Employee = admin.user,
        time: HelsinkiDateTime = now,
    ): DecisionIndividualReasoningId =
        controller.createIndividualReasoning(dbInstance(), user, MockEvakaClock(time), request)

    private fun getIndividualReasonings(
        collectionType: DecisionReasoningCollectionType,
        user: AuthenticatedUser.Employee = admin.user,
    ): List<DecisionIndividualReasoning> =
        controller.getIndividualReasonings(dbInstance(), user, MockEvakaClock(now), collectionType)

    private fun removeIndividualReasoning(
        id: DecisionIndividualReasoningId,
        user: AuthenticatedUser.Employee = admin.user,
        time: HelsinkiDateTime = now,
    ) = controller.removeIndividualReasoning(dbInstance(), user, MockEvakaClock(time), id)

    @Test
    fun `admin can create and read generic reasonings`() {
        val request =
            DecisionGenericReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(request)

        val result = getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(1, result.size)
        with(result.first()) {
            assertEquals(id, this.id)
            assertEquals(request.collectionType, this.collectionType)
            assertEquals(request.validFrom, this.validFrom)
            assertEquals(request.textFi, this.textFi)
            assertEquals(request.textSv, this.textSv)
            assertEquals(request.ready, this.ready)
            assertEquals(now, this.createdAt)
            assertEquals(now, this.modifiedAt)
            assertNull(this.endDate)
            assertFalse(this.outdated)
        }
    }

    @Test
    fun `admin can update a not-ready generic reasoning`() {
        val request =
            DecisionGenericReasoningRequest(
                collectionType = DecisionReasoningCollectionType.PRESCHOOL,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(request)

        val updatedRequest = request.copy(textFi = "Päivitetty teksti")
        updateGenericReasoning(id, updatedRequest, time = now.plusHours(1L))

        val result = getGenericReasonings(DecisionReasoningCollectionType.PRESCHOOL)
        assertEquals(1, result.size)
        with(result.first()) {
            assertEquals(updatedRequest.textFi, this.textFi)
            assertEquals(updatedRequest.textSv, this.textSv)
            assertEquals(updatedRequest.validFrom, this.validFrom)
            assertEquals(updatedRequest.ready, this.ready)
            assertEquals(now, this.createdAt)
            assertEquals(now.plusHours(1L), this.modifiedAt)
        }
    }

    @Test
    fun `admin can mark generic reasoning as ready`() {
        val request =
            DecisionGenericReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(request)
        updateGenericReasoning(id, request.copy(ready = true))

        val result = getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(true, result.first().ready)
    }

    @Test
    fun `cannot update a ready generic reasoning`() {
        val request =
            DecisionGenericReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(request)
        updateGenericReasoning(id, request.copy(ready = true))

        assertThrows<NotFound> { updateGenericReasoning(id, request.copy(textFi = "Should fail")) }
    }

    @Test
    fun `cannot delete a ready generic reasoning`() {
        val request =
            DecisionGenericReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(request)
        updateGenericReasoning(id, request.copy(ready = true))

        assertThrows<NotFound> { deleteGenericReasoning(id) }
    }

    @Test
    fun `admin can delete a not-ready generic reasoning`() {
        val request =
            DecisionGenericReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(request)
        deleteGenericReasoning(id)

        val result = getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(0, result.size)
    }

    @Test
    fun `endDate is the day before the next validFrom and null for the latest`() {
        val earlierStart = LocalDate.of(2026, 8, 1)
        val laterStart = LocalDate.of(2027, 1, 1)
        val earlierId =
            createGenericReasoning(genericRequest(earlierStart), time = now.minusDays(2L))
        val laterId = createGenericReasoning(genericRequest(laterStart), time = now.minusDays(1L))

        val result =
            getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
                .associateBy { it.id }

        assertEquals(laterStart.minusDays(1), result.getValue(earlierId).endDate)
        assertNull(result.getValue(laterId).endDate)
    }

    @Test
    fun `older reasoning sharing the same validFrom is marked outdated as superseded`() {
        val validFrom = LocalDate.of(2026, 8, 1)
        val olderId = createGenericReasoning(genericRequest(validFrom), time = now.minusDays(1L))
        val newerId = createGenericReasoning(genericRequest(validFrom), time = now)

        val result =
            getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
                .associateBy { it.id }

        assertTrue(result.getValue(olderId).outdated)
        assertFalse(result.getValue(newerId).outdated)
    }

    @Test
    fun `reasoning whose endDate is before today is marked outdated`() {
        val pastStart = LocalDate.of(2025, 8, 1)
        val currentStart = LocalDate.of(2026, 1, 1)
        val pastId = createGenericReasoning(genericRequest(pastStart), time = now.minusDays(2L))
        createGenericReasoning(genericRequest(currentStart), time = now.minusDays(1L))

        val result =
            getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
                .associateBy { it.id }

        assertTrue(result.getValue(pastId).outdated)
        assertEquals(currentStart.minusDays(1), result.getValue(pastId).endDate)
    }

    @Test
    fun `three series points produce one outdated, one bounded active and one open-ended active`() {
        val first = LocalDate.of(2025, 8, 1)
        val second = LocalDate.of(2026, 1, 1)
        val third = LocalDate.of(2027, 1, 1)
        val firstId = createGenericReasoning(genericRequest(first), time = now.minusDays(3L))
        val secondId = createGenericReasoning(genericRequest(second), time = now.minusDays(2L))
        val thirdId = createGenericReasoning(genericRequest(third), time = now.minusDays(1L))

        val result =
            getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
                .associateBy { it.id }

        with(result.getValue(firstId)) {
            assertEquals(second.minusDays(1), endDate)
            assertTrue(outdated)
        }
        with(result.getValue(secondId)) {
            assertEquals(third.minusDays(1), endDate)
            assertFalse(outdated)
        }
        with(result.getValue(thirdId)) {
            assertNull(endDate)
            assertFalse(outdated)
        }
    }

    private fun genericRequest(
        validFrom: LocalDate,
        collectionType: DecisionReasoningCollectionType = DecisionReasoningCollectionType.DAYCARE,
    ) =
        DecisionGenericReasoningRequest(
            collectionType = collectionType,
            validFrom = validFrom,
            textFi = "Teksti FI",
            textSv = "Text SV",
            ready = false,
        )

    @Test
    fun `admin can create and read individual reasonings`() {
        val request =
            DecisionIndividualReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Yksilöllinen otsikko FI",
                titleSv = "Individuell titel SV",
                textFi = "Yksilöllinen teksti FI",
                textSv = "Individuell text SV",
            )
        val id = createIndividualReasoning(request)

        val result = getIndividualReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(1, result.size)
        with(result.first()) {
            assertEquals(id, this.id)
            assertEquals(request.collectionType, this.collectionType)
            assertEquals(request.titleFi, this.titleFi)
            assertEquals(request.titleSv, this.titleSv)
            assertEquals(request.textFi, this.textFi)
            assertEquals(request.textSv, this.textSv)
            assertNull(this.removedAt)
            assertEquals(now, this.createdAt)
            assertEquals(now, this.modifiedAt)
        }
    }

    @Test
    fun `admin can remove an individual reasoning`() {
        val request =
            DecisionIndividualReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Otsikko FI",
                titleSv = "Titel SV",
                textFi = "Teksti FI",
                textSv = "Text SV",
            )
        val id = createIndividualReasoning(request)
        removeIndividualReasoning(id)

        val result = getIndividualReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(1, result.size)
        assertNotNull(result.first().removedAt)
    }

    @Test
    fun `cannot remove an already removed individual reasoning`() {
        val request =
            DecisionIndividualReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Otsikko FI",
                titleSv = "Titel SV",
                textFi = "Teksti FI",
                textSv = "Text SV",
            )
        val id = createIndividualReasoning(request)
        removeIndividualReasoning(id)

        assertThrows<NotFound> { removeIndividualReasoning(id) }
    }

    @Test
    fun `service worker can read but not write generic reasonings`() {
        val result =
            getGenericReasonings(DecisionReasoningCollectionType.DAYCARE, user = serviceWorker.user)
        assertEquals(0, result.size)

        val request =
            DecisionGenericReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        assertThrows<Forbidden> { createGenericReasoning(request, user = serviceWorker.user) }
    }

    @Test
    fun `service worker can read but not write individual reasonings`() {
        val result =
            getIndividualReasonings(
                DecisionReasoningCollectionType.DAYCARE,
                user = serviceWorker.user,
            )
        assertEquals(0, result.size)

        val request =
            DecisionIndividualReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Otsikko FI",
                titleSv = "Titel SV",
                textFi = "Teksti FI",
                textSv = "Text SV",
            )
        assertThrows<Forbidden> { createIndividualReasoning(request, user = serviceWorker.user) }
    }

    @Test
    fun `generic reasonings are filtered by collection type`() {
        val daycareRequest =
            DecisionGenericReasoningRequest(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val preschoolRequest =
            DecisionGenericReasoningRequest(
                collectionType = DecisionReasoningCollectionType.PRESCHOOL,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        createGenericReasoning(daycareRequest)
        createGenericReasoning(preschoolRequest)

        val daycareResult = getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(1, daycareResult.size)
        assertEquals(
            DecisionReasoningCollectionType.DAYCARE,
            daycareResult.first().collectionType,
        )

        val preschoolResult = getGenericReasonings(DecisionReasoningCollectionType.PRESCHOOL)
        assertEquals(1, preschoolResult.size)
        assertEquals(
            DecisionReasoningCollectionType.PRESCHOOL,
            preschoolResult.first().collectionType,
        )
    }
}
