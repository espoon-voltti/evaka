package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.bulletin.Bulletin
import fi.espoo.evaka.messaging.bulletin.BulletinControllerEmployee
import fi.espoo.evaka.messaging.bulletin.ReceivedBulletin
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BulletinIntegrationTest : FullApplicationTest() {
    private val supervisorId = UUID.randomUUID()
    private val supervisor = AuthenticatedUser(supervisorId, emptySet())
    private val staffId = UUID.randomUUID()
    private val staffMember = AuthenticatedUser(staffId, emptySet())
    private val guardian = AuthenticatedUser(testAdult_1.id, setOf(UserRole.END_USER))
    private val groupId = UUID.randomUUID()
    private val groupName = "Testaajat"
    private val daycarePlacementId = UUID.randomUUID()
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)
    private val msgTitle = "Koronatiedote"
    private val msgContent = """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore 
        magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo
        consequat.

        Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. 
        Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    """.trimIndent()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
            tx.handle.insertTestPlacement(
                DevPlacement(
                    id = daycarePlacementId,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
            tx.handle.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            insertTestDaycareGroupPlacement(
                h = tx.handle,
                daycarePlacementId = daycarePlacementId,
                groupId = groupId,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.handle.insertTestEmployee(
                DevEmployee(
                    id = supervisorId,
                    firstName = "Elina",
                    lastName = "Esimies"
                )
            )
            tx.handle.insertTestEmployee(
                DevEmployee(
                    id = staffId
                )
            )
            tx.handle.insertDaycareAclRow(testDaycare.id, supervisorId, UserRole.UNIT_SUPERVISOR)
            tx.handle.insertDaycareAclRow(testDaycare.id, staffId, UserRole.STAFF)
            insertGuardian(tx.handle, testAdult_1.id, testChild_1.id)
        }
    }

    @Test
    fun `supervisor sends a bulletin, citizen reads it`() {
        val unitId = testDaycare.id
        val bulletinId = initBulletin(supervisor, unitId)
        updateBulletin(
            supervisor, bulletinId,
            BulletinControllerEmployee.BulletinUpdate(
                groupId = groupId,
                title = msgTitle,
                content = msgContent
            )
        )
        getDraftBulletins(supervisor, unitId).also { assertEquals(1, it.size) }
        sendBulletin(supervisor, bulletinId)

        getBulletinsAsGuardian(guardian)
            .let {
                assertEquals(1, it.size)
                it.first()
            }
            .also {
                assertEquals(msgTitle, it.title)
                assertEquals(msgContent, it.content)
                assertFalse(it.isRead)
                assertEquals(groupName, it.sender)
            }
        markBulletinRead(guardian, bulletinId)
        assertTrue(getBulletinsAsGuardian(guardian).first().isRead)

        getSentBulletinsByUnit(supervisor, testDaycare.id).also {
            assertEquals(msgContent, it.first().content)
        }
        getSentBulletinsByUnit(staffMember, testDaycare.id).also {
            assertEquals(msgContent, it.first().content)
        }
        getDraftBulletins(supervisor, unitId).also { assertTrue(it.isEmpty()) }
    }

    @Test
    fun `supervisor deletes a draft`() {
        val unitId = testDaycare.id
        val bulletinId = initBulletin(supervisor, unitId)
        deleteDraftBulletin(supervisor, bulletinId)
        getDraftBulletins(supervisor, unitId).also { assertTrue(it.isEmpty()) }
    }

    private fun initBulletin(user: AuthenticatedUser, unitId: UUID = testDaycare.id): UUID {
        val (_, res, result) = http.post("/bulletins?unitId=$unitId")
            .asUser(user)
            .responseObject<Bulletin>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get().id
    }

    private fun updateBulletin(user: AuthenticatedUser, id: UUID, update: BulletinControllerEmployee.BulletinUpdate) {
        val (_, res, _) = http.put("/bulletins/$id")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(update))
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun deleteDraftBulletin(user: AuthenticatedUser, id: UUID) {
        val (_, res, _) = http.delete("/bulletins/$id")
            .asUser(user)
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun sendBulletin(user: AuthenticatedUser, id: UUID) {
        val (_, res, _) = http.post("/bulletins/$id/send")
            .asUser(user)
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun getDraftBulletins(user: AuthenticatedUser, unitId: UUID): List<Bulletin> {
        val (_, res, result) = http.get("/bulletins/draft?unitId=$unitId")
            .asUser(user)
            .responseObject<List<Bulletin>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getSentBulletinsByUnit(user: AuthenticatedUser, unitId: UUID): List<Bulletin> {
        val (_, res, result) = http.get("/bulletins/sent?unitId=$unitId")
            .asUser(user)
            .responseObject<List<Bulletin>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getBulletinsAsGuardian(user: AuthenticatedUser): List<ReceivedBulletin> {
        val (_, res, result) = http.get("/citizen/bulletins")
            .asUser(user)
            .responseObject<List<ReceivedBulletin>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun markBulletinRead(user: AuthenticatedUser, id: UUID) {
        val (_, res, _) = http.put("/citizen/bulletins/$id/read")
            .asUser(user)
            .response()

        assertEquals(204, res.statusCode)
    }
}
