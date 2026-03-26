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
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        body: DecisionGenericReasoningBody,
        user: AuthenticatedUser.Employee = admin.user,
        time: HelsinkiDateTime = now,
    ): DecisionGenericReasoningId =
        controller.createGenericReasoning(dbInstance(), user, MockEvakaClock(time), body)

    private fun getGenericReasonings(
        collectionType: DecisionReasoningCollectionType,
        user: AuthenticatedUser.Employee = admin.user,
    ): List<DecisionGenericReasoning> =
        controller.getGenericReasonings(dbInstance(), user, MockEvakaClock(now), collectionType)

    private fun updateGenericReasoning(
        id: DecisionGenericReasoningId,
        body: DecisionGenericReasoningBody,
        user: AuthenticatedUser.Employee = admin.user,
        time: HelsinkiDateTime = now,
    ) = controller.updateGenericReasoning(dbInstance(), user, MockEvakaClock(time), id, body)

    private fun deleteGenericReasoning(
        id: DecisionGenericReasoningId,
        user: AuthenticatedUser.Employee = admin.user,
    ) = controller.deleteGenericReasoning(dbInstance(), user, MockEvakaClock(now), id)

    private fun createIndividualReasoning(
        body: DecisionIndividualReasoningBody,
        user: AuthenticatedUser.Employee = admin.user,
        time: HelsinkiDateTime = now,
    ): DecisionIndividualReasoningId =
        controller.createIndividualReasoning(dbInstance(), user, MockEvakaClock(time), body)

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
        val body =
            DecisionGenericReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(body)

        val result = getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(1, result.size)
        with(result.first()) {
            assertEquals(id, this.id)
            assertEquals(body, this.body)
            assertEquals(now, this.createdAt)
            assertEquals(now, this.modifiedAt)
        }
    }

    @Test
    fun `admin can update a not-ready generic reasoning`() {
        val body =
            DecisionGenericReasoningBody(
                collectionType = DecisionReasoningCollectionType.PRESCHOOL,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(body)

        val updatedBody = body.copy(textFi = "Päivitetty teksti")
        updateGenericReasoning(id, updatedBody, time = now.plusHours(1L))

        val result = getGenericReasonings(DecisionReasoningCollectionType.PRESCHOOL)
        assertEquals(1, result.size)
        with(result.first()) {
            assertEquals(updatedBody, this.body)
            assertEquals(now, this.createdAt)
            assertEquals(now.plusHours(1L), this.modifiedAt)
        }
    }

    @Test
    fun `admin can mark generic reasoning as ready`() {
        val body =
            DecisionGenericReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(body)
        updateGenericReasoning(id, body.copy(ready = true))

        val result = getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(true, result.first().body.ready)
    }

    @Test
    fun `cannot update a ready generic reasoning`() {
        val body =
            DecisionGenericReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(body)
        updateGenericReasoning(id, body.copy(ready = true))

        assertThrows<Conflict> { updateGenericReasoning(id, body.copy(textFi = "Should fail")) }
    }

    @Test
    fun `cannot delete a ready generic reasoning`() {
        val body =
            DecisionGenericReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(body)
        updateGenericReasoning(id, body.copy(ready = true))

        assertThrows<Conflict> { deleteGenericReasoning(id) }
    }

    @Test
    fun `admin can delete a not-ready generic reasoning`() {
        val body =
            DecisionGenericReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val id = createGenericReasoning(body)
        deleteGenericReasoning(id)

        val result = getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(0, result.size)
    }

    @Test
    fun `admin can create and read individual reasonings`() {
        val body =
            DecisionIndividualReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Yksilöllinen otsikko FI",
                titleSv = "Individuell titel SV",
                textFi = "Yksilöllinen teksti FI",
                textSv = "Individuell text SV",
            )
        val id = createIndividualReasoning(body)

        val result = getIndividualReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(1, result.size)
        with(result.first()) {
            assertEquals(id, this.id)
            assertEquals(body, this.body)
            assertNull(this.removedAt)
            assertEquals(now, this.createdAt)
            assertEquals(now, this.modifiedAt)
        }
    }

    @Test
    fun `admin can remove an individual reasoning`() {
        val body =
            DecisionIndividualReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Otsikko FI",
                titleSv = "Titel SV",
                textFi = "Teksti FI",
                textSv = "Text SV",
            )
        val id = createIndividualReasoning(body)
        removeIndividualReasoning(id)

        val result = getIndividualReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(1, result.size)
        assertNotNull(result.first().removedAt)
    }

    @Test
    fun `cannot remove an already removed individual reasoning`() {
        val body =
            DecisionIndividualReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Otsikko FI",
                titleSv = "Titel SV",
                textFi = "Teksti FI",
                textSv = "Text SV",
            )
        val id = createIndividualReasoning(body)
        removeIndividualReasoning(id)

        assertThrows<Conflict> { removeIndividualReasoning(id) }
    }

    @Test
    fun `service worker can read but not write generic reasonings`() {
        val result =
            getGenericReasonings(DecisionReasoningCollectionType.DAYCARE, user = serviceWorker.user)
        assertEquals(0, result.size)

        val body =
            DecisionGenericReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        assertThrows<Forbidden> { createGenericReasoning(body, user = serviceWorker.user) }
    }

    @Test
    fun `service worker can read but not write individual reasonings`() {
        val result =
            getIndividualReasonings(
                DecisionReasoningCollectionType.DAYCARE,
                user = serviceWorker.user,
            )
        assertEquals(0, result.size)

        val body =
            DecisionIndividualReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Otsikko FI",
                titleSv = "Titel SV",
                textFi = "Teksti FI",
                textSv = "Text SV",
            )
        assertThrows<Forbidden> { createIndividualReasoning(body, user = serviceWorker.user) }
    }

    @Test
    fun `generic reasonings are filtered by collection type`() {
        val daycareBody =
            DecisionGenericReasoningBody(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        val preschoolBody =
            DecisionGenericReasoningBody(
                collectionType = DecisionReasoningCollectionType.PRESCHOOL,
                validFrom = LocalDate.of(2026, 8, 1),
                textFi = "Teksti FI",
                textSv = "Text SV",
                ready = false,
            )
        createGenericReasoning(daycareBody)
        createGenericReasoning(preschoolBody)

        val daycareResult = getGenericReasonings(DecisionReasoningCollectionType.DAYCARE)
        assertEquals(1, daycareResult.size)
        assertEquals(
            DecisionReasoningCollectionType.DAYCARE,
            daycareResult.first().body.collectionType,
        )

        val preschoolResult = getGenericReasonings(DecisionReasoningCollectionType.PRESCHOOL)
        assertEquals(1, preschoolResult.size)
        assertEquals(
            DecisionReasoningCollectionType.PRESCHOOL,
            preschoolResult.first().body.collectionType,
        )
    }
}
