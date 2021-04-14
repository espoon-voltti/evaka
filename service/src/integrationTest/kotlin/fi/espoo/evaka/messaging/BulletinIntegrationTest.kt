// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.bulletin.Bulletin
import fi.espoo.evaka.messaging.bulletin.BulletinControllerEmployee
import fi.espoo.evaka.messaging.bulletin.BulletinReceiverTriplet
import fi.espoo.evaka.messaging.bulletin.ReceivedBulletin
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class BulletinIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner
    private val childId = testChild_1.id
    private val unitId = testDaycare.id
    private val secondUnitId = testDaycare2.id

    private val supervisorId = UUID.randomUUID()
    private val supervisor = AuthenticatedUser.Employee(supervisorId, emptySet())
    private val staffId = UUID.randomUUID()
    private val staffMember = AuthenticatedUser.Employee(staffId, emptySet())
    private val guardianPerson = testAdult_6
    private val guardian = AuthenticatedUser.Citizen(guardianPerson.id)
    private val groupId = UUID.randomUUID()
    private val groupName = "Testaajat"
    private val secondGroupId = UUID.randomUUID()
    private val secondGroupName = "Koekaniinit"
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

    private fun insertChildToGroup(tx: Database.Transaction, childId: UUID, guardianId: UUID, groupId: UUID, unitId: UUID) {
        val daycarePlacementId = tx.handle.insertTestPlacement(
            DevPlacement(
                childId = childId,
                unitId = unitId,
                startDate = placementStart,
                endDate = placementEnd
            )
        )
        insertTestDaycareGroupPlacement(
            h = tx.handle,
            daycarePlacementId = daycarePlacementId,
            groupId = groupId,
            startDate = placementStart,
            endDate = placementEnd
        )
        tx.insertGuardian(guardianId, childId)
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)

            tx.handle.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            tx.handle.insertTestDaycareGroup(DevDaycareGroup(id = secondGroupId, daycareId = secondUnitId, name = secondGroupName))

            insertChildToGroup(tx, childId, guardianPerson.id, groupId, unitId)
            insertChildToGroup(tx, testChild_3.id, testAdult_3.id, secondGroupId, secondUnitId)
            insertChildToGroup(tx, testChild_4.id, testAdult_4.id, secondGroupId, secondUnitId)

            tx.handle.createParentship(testChild_3.id, testAdult_2.id, placementStart, placementEnd)

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
            tx.handle.insertDaycareAclRow(unitId, supervisorId, UserRole.UNIT_SUPERVISOR)
            tx.handle.insertDaycareAclRow(unitId, staffId, UserRole.STAFF)
            tx.handle.insertDaycareAclRow(secondUnitId, supervisorId, UserRole.UNIT_SUPERVISOR)
        }
        MockEmailClient.emails.clear()
    }

    @Test
    fun `supervisor sends a bulletin, citizen reads it`() {
        val receivers = listOf(BulletinReceiverTriplet(unitId = unitId))
        val bulletinId = initBulletin(supervisor, receivers)
        updateBulletin(
            supervisor, bulletinId,
            BulletinControllerEmployee.BulletinUpdate(
                title = msgTitle,
                content = msgContent,
                sender = "Testaajat",
                receivers
            )
        )
        getDraftBulletins(supervisor, unitId).also { assertEquals(1, it.total) }
        sendBulletin(supervisor, bulletinId)

        val guardianBulletin = getBulletinsAsGuardian(guardian)
            .let {
                assertEquals(1, it.data.size)
                assertEquals(1, it.total)
                assertEquals(1, it.pages)
                it.data.first()
            }
            .also {
                assertEquals(msgTitle, it.title)
                assertEquals(msgContent, it.content)
                assertFalse(it.isRead)
                assertEquals(groupName, it.sender)
            }
        markBulletinRead(guardian, guardianBulletin.id)
        assertTrue(getBulletinsAsGuardian(guardian).data.first().isRead)

        getSentBulletinsByUnit(supervisor, testDaycare.id).also {
            assertEquals(msgContent, it.data.first().content)
        }
        getSentBulletinsByUnit(staffMember, testDaycare.id).also {
            assertEquals(msgContent, it.data.first().content)
        }
        getDraftBulletins(supervisor, unitId).also { assertTrue(it.data.isEmpty()) }
    }

    @Test
    fun `supervisor sends a bulletin to two units`() {
        val receivers = listOf(BulletinReceiverTriplet(unitId), BulletinReceiverTriplet(secondUnitId))
        val bulletinId = initBulletin(supervisor, receivers)
        updateBulletin(
            supervisor, bulletinId,
            BulletinControllerEmployee.BulletinUpdate(
                title = msgTitle,
                content = msgContent,
                sender = "TestSender",
                receivers = receivers
            )
        )
        getDraftBulletins(supervisor, unitId).also { assertEquals(1, it.total) }
        sendBulletin(supervisor, bulletinId)

        getSentBulletinsByUnit(supervisor, unitId).also {
            assertEquals(msgContent, it.data.first().content)
        }

        getSentBulletinsByUnit(supervisor, secondUnitId).also {
            assertEquals(msgContent, it.data.first().content)
        }

        getDraftBulletins(supervisor, unitId).also { assertTrue(it.data.isEmpty()) }

        db.transaction { tx ->
            val instanceReceivers = getBulletinInstances(tx, bulletinId)
            assertEquals(4, instanceReceivers.size)
        }
    }

    @Test
    fun `supervisor deletes a draft`() {
        val bulletinId = initBulletin(supervisor, receivers = listOf(BulletinReceiverTriplet(unitId)))
        deleteDraftBulletin(supervisor, bulletinId)
        getDraftBulletins(supervisor, unitId).also { assertTrue(it.data.isEmpty()) }
    }

    @Test
    fun `Sending a bulletin sends a reminder email`() {
        val receivers = listOf(BulletinReceiverTriplet(unitId = unitId))
        val bulletinId = initBulletin(supervisor, receivers)
        updateBulletin(
            supervisor, bulletinId,
            BulletinControllerEmployee.BulletinUpdate(
                title = msgTitle,
                content = msgContent,
                sender = "Testaajat",
                receivers = receivers
            )
        )
        sendBulletin(supervisor, bulletinId)

        asyncJobRunner.runPendingJobsSync()

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)
        assertEmail(
            sentMails.first(),
            guardianPerson.email!!,
            "Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>",
            "Uusi tiedote eVakassa [${System.getenv("VOLTTI_ENV")}]"
        )
    }

    @Test
    fun `Notification is sent only once`() {
        val receivers = listOf(BulletinReceiverTriplet(unitId = unitId))
        val bulletinId = initBulletin(supervisor, receivers)
        updateBulletin(
            supervisor, bulletinId,
            BulletinControllerEmployee.BulletinUpdate(
                title = msgTitle,
                content = msgContent,
                sender = "Testaajat",
                receivers = receivers
            )
        )
        sendBulletin(supervisor, bulletinId)

        asyncJobRunner.runPendingJobsSync()

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)

        asyncJobRunner.runPendingJobsSync()

        assertEquals(1, MockEmailClient.emails.size)
    }

    @Test
    fun `Notification language is parsed right`() {
        val receivers = listOf(BulletinReceiverTriplet(unitId = unitId), BulletinReceiverTriplet(unitId = secondUnitId))
        val bulletinId = initBulletin(supervisor, receivers)
        updateBulletin(
            supervisor, bulletinId,
            BulletinControllerEmployee.BulletinUpdate(
                title = msgTitle,
                content = msgContent,
                sender = "Testaajat",
                receivers = receivers
            )
        )

        db.transaction { tx ->
            updatePersonLanguage(tx, guardianPerson.id, Language.en)
            insertChildToGroup(tx, testChild_2.id, testAdult_2.id, groupId, unitId)
            updatePersonLanguage(tx, testAdult_2.id, Language.fi)
            updatePersonLanguage(tx, testAdult_3.id, Language.sv)
            updatePersonLanguage(tx, testAdult_4.id, null)
        }

        sendBulletin(supervisor, bulletinId)

        asyncJobRunner.runPendingJobsSync()

        val sentMails = MockEmailClient.emails
        assertEquals(4, sentMails.size)

        assertEquals(
            "New bulletin in eVaka [${System.getenv("VOLTTI_ENV")}]",
            sentMails.find { it.toAddress == guardianPerson.email }!!.subject
        )
        assertEquals(
            "Uusi tiedote eVakassa [${System.getenv("VOLTTI_ENV")}]",
            sentMails.find { it.toAddress == testAdult_2.email }!!.subject
        )
        assertEquals(
            "Ny meddelande i eVaka [${System.getenv("VOLTTI_ENV")}]",
            sentMails.find { it.toAddress == testAdult_3.email }!!.subject
        )
        assertEquals(
            "Uusi tiedote eVakassa [${System.getenv("VOLTTI_ENV")}]",
            sentMails.find { it.toAddress == testAdult_4.email }!!.subject
        )
    }

    @Test
    fun `Bulletin receiver endpoint works for unit 1`() {
        val (_, res, result) = http.get("/bulletins/receivers?unitId=$unitId")
            .asUser(supervisor)
            .responseObject<List<BulletinControllerEmployee.BulletinReceiversResponse>>(objectMapper)

        assertEquals(200, res.statusCode)

        val receivers = result.get()

        assertEquals(1, receivers.size)

        val groupTestaajat = receivers.find { it.groupName == groupName }!!
        assertEquals(1, groupTestaajat.receivers.size)
    }

    @Test
    fun `Bulletin receiver endpoint works for unit 2`() {
        val (_, res, result) = http.get("/bulletins/receivers?unitId=$secondUnitId")
            .asUser(supervisor)
            .responseObject<List<BulletinControllerEmployee.BulletinReceiversResponse>>(objectMapper)

        assertEquals(200, res.statusCode)

        val receivers = result.get()

        assertEquals(1, receivers.size)

        val groupKoekaniinit = receivers.find { it.groupName == secondGroupName }!!
        assertEquals(2, groupKoekaniinit.receivers.size)
        val childWithTwoReceiverPersons = groupKoekaniinit.receivers.find { it.childId == testChild_3.id }!!
        assertEquals(2, childWithTwoReceiverPersons.receiverPersons.size)
    }

    @Test
    fun `Sender options endpoint works for unit 1`() {
        val (_, res, result) = http.get("/bulletins/sender-options?unitId=$unitId")
            .asUser(supervisor)
            .responseObject<List<String>>()

        assertEquals(200, res.statusCode)

        val senderOptions = result.get()

        assertEquals(3, senderOptions.size)

        assertEquals(setOf("Elina Esimies", "Testaajat", "Test Daycare"), senderOptions.toSet())
    }

    @Test
    fun `Sender options endpoint works for unit 2`() {
        val (_, res, result) = http.get("/bulletins/sender-options?unitId=$secondUnitId")
            .asUser(supervisor)
            .responseObject<List<String>>()

        assertEquals(200, res.statusCode)

        val senderOptions = result.get()

        assertEquals(3, senderOptions.size)

        assertEquals(setOf("Elina Esimies", "Koekaniinit", "Test Daycare 2"), senderOptions.toSet())
    }

    private fun initBulletin(user: AuthenticatedUser, receivers: List<BulletinReceiverTriplet>): UUID {
        val (_, res, result) = http.post("/bulletins")
            .asUser(user)
            .jsonBody(objectMapper.writeValueAsString(BulletinControllerEmployee.CreateBulletinRequest(receivers = receivers, sender = "Testaajat")))
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

    private fun getDraftBulletins(user: AuthenticatedUser, unitId: UUID): Paged<Bulletin> {
        val (_, res, result) = http.get("/bulletins/draft?unitId=$unitId", listOf("page" to 1, "pageSize" to 50))
            .asUser(user)
            .responseObject<Paged<Bulletin>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getSentBulletinsByUnit(user: AuthenticatedUser, unitId: UUID): Paged<Bulletin> {
        val (_, res, result) = http.get("/bulletins/sent?unitId=$unitId", listOf("page" to 1, "pageSize" to 50))
            .asUser(user)
            .responseObject<Paged<Bulletin>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun getBulletinsAsGuardian(user: AuthenticatedUser): Paged<ReceivedBulletin> {
        val (_, res, result) = http.get("/citizen/bulletins", listOf("page" to 1, "pageSize" to 50))
            .asUser(user)
            .responseObject<Paged<ReceivedBulletin>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun markBulletinRead(user: AuthenticatedUser, id: UUID) {
        val (_, res, _) = http.put("/citizen/bulletins/$id/read")
            .asUser(user)
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun getBulletinInstances(tx: Database.Transaction, bulletinId: UUID): List<UUID> {
        return tx.createQuery(
            """
            SELECT receiver_person_id FROM bulletin_instance WHERE bulletin_id = :bulletinId
            """.trimIndent()
        )
            .bind("bulletinId", bulletinId)
            .mapTo<UUID>()
            .toList()
    }

    private fun updatePersonLanguage(tx: Database.Transaction, id: UUID, lang: Language?) {
        tx.createUpdate(
            """
                UPDATE person SET language = :language WHERE id = :id
            """.trimIndent()
        )
            .bind("id", id)
            .bind("language", lang)
            .execute()
    }

    private fun assertEmail(email: MockEmail?, expectedToAddress: String, expectedFromAddress: String, expectedSubject: String) {
        assertNotNull(email)
        assertEquals(expectedToAddress, email?.toAddress)
        assertEquals(expectedFromAddress, email?.fromAddress)
        assertEquals(expectedSubject, email?.subject)
    }
}
