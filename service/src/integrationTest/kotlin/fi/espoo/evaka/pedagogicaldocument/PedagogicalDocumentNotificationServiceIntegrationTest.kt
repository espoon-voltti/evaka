// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.addUnitFeatures
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PedagogicalDocumentNotificationServiceIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val testGuardianFi = DevPerson(email = "fi@example.com", language = "fi")
    private val testGuardianSv = DevPerson(email = "sv@example.com", language = "sv")
    private val testGuardianNoEmail = DevPerson(language = "en")
    private val testHeadOfChild = DevPerson(email = "head@example.com", language = "en")

    private val testPersons =
        listOf(testGuardianFi, testGuardianSv, testGuardianNoEmail, testHeadOfChild)
    private val testNotificationRecipients = listOf(testGuardianFi, testGuardianSv)
    private val testAddresses = testNotificationRecipients.mapNotNull { it.email }

    private val employeeId = EmployeeId(UUID.randomUUID())
    private val employee =
        AuthenticatedUser.Employee(id = employeeId, roles = setOf(UserRole.UNIT_SUPERVISOR))

    @BeforeEach
    fun beforeEach() {
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)

        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.addUnitFeatures(listOf(testDaycare.id), listOf(PilotFeature.VASU_AND_PEDADOC))

            val groupId = GroupId(UUID.randomUUID())
            tx.insert(
                DevDaycareGroup(
                    id = groupId,
                    daycareId = testDaycare.id,
                    startDate = placementStart,
                )
            )

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            testPersons.forEach { tx.insert(it, DevPersonType.RAW_ROW) }
            tx.insertGuardian(testGuardianFi.id, testChild_1.id)
            tx.insertGuardian(testGuardianSv.id, testChild_1.id)
            tx.createParentship(
                testChild_1.id,
                testHeadOfChild.id,
                LocalDate.now().minusYears(1),
                LocalDate.now().plusYears(1),
                Creator.DVV,
            )

            tx.insert(DevEmployee(id = employeeId))
            tx.upsertEmployeeMessageAccount(employeeId)
            tx.insertDaycareAclRow(testDaycare.id, employeeId, UserRole.STAFF)
        }
    }

    @Test
    fun `notifications are sent to guardians but not heads`() {
        postNewDocument(user = employee, PedagogicalDocumentPostBody(testChild_1.id, "foobar"))
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEquals(testAddresses.toSet(), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertEquals(
            "Uusi pedagoginen dokumentti eVakassa / Nytt pedagogiskt dokument i eVaka / New pedagogical document in eVaka",
            getEmailFor(testGuardianFi).content.subject,
        )
        assertEquals(
            "Esbo småbarnspedagogik <no-reply.evaka@espoo.fi>",
            getEmailFor(testGuardianSv).fromAddress,
        )
    }

    @Test
    fun `sending of notifications are tracked in the database`() {
        val doc =
            postNewDocument(user = employee, PedagogicalDocumentPostBody(testChild_1.id, "foobar"))

        db.read {
            val emailJobCreatedAt =
                it.createQuery {
                        sql(
                            "SELECT email_job_created_at FROM pedagogical_document WHERE id = ${bind(doc.id)}"
                        )
                    }
                    .exactlyOne<HelsinkiDateTime>()
            assertTrue(
                HelsinkiDateTime.now().durationSince(emailJobCreatedAt) < Duration.ofSeconds(1)
            )
        }
        assertEmailSent(doc.id, false)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEmailSent(doc.id)
    }

    @Test
    fun `notifications are not sent before document has content`() {
        val doc = postNewDocument(user = employee, PedagogicalDocumentPostBody(testChild_1.id, ""))
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEmailSent(doc.id, false)

        updateDocument(
            user = employee,
            doc.id,
            PedagogicalDocumentPostBody(testChild_1.id, "babar"),
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEmailSent(doc.id)
    }

    @Test
    fun `notifications are sent after document has an attachment`() {
        val doc = postNewDocument(user = employee, PedagogicalDocumentPostBody(testChild_1.id, ""))
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEmailSent(doc.id, false)

        uploadDocumentAttachment(employee, doc.id)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEmailSent(doc.id, true)
    }

    private fun assertEmailSent(id: PedagogicalDocumentId, sent: Boolean? = true) {
        assertEquals(
            sent,
            db.read {
                it.createQuery {
                        sql("SELECT email_sent FROM pedagogical_document WHERE id = ${bind(id)}")
                    }
                    .exactlyOne<Boolean>()
            },
        )
    }

    @Test
    fun `notifications are sent to the guardians with email addresses even if one guardian has no email address`() {
        db.transaction { tx -> tx.insertGuardian(testGuardianNoEmail.id, testChild_1.id) }

        postNewDocument(user = employee, PedagogicalDocumentPostBody(testChild_1.id, "foobar"))
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEquals(testAddresses.toSet(), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertEquals(
            "Uusi pedagoginen dokumentti eVakassa / Nytt pedagogiskt dokument i eVaka / New pedagogical document in eVaka",
            getEmailFor(testGuardianFi).content.subject,
        )
        assertEquals(
            "Esbo småbarnspedagogik <no-reply.evaka@espoo.fi>",
            getEmailFor(testGuardianSv).fromAddress,
        )
    }

    private fun postNewDocument(
        user: AuthenticatedUser.Employee,
        body: PedagogicalDocumentPostBody,
    ) =
        jsonMapper.readValue<PedagogicalDocument>(
            http
                .post("/pedagogical-document")
                .jsonBody(jsonMapper.writeValueAsString(body))
                .asUser(user)
                .responseString()
                .third
                .get()
        )

    private fun updateDocument(
        user: AuthenticatedUser.Employee,
        id: PedagogicalDocumentId,
        body: PedagogicalDocumentPostBody,
    ) =
        jsonMapper.readValue<PedagogicalDocument>(
            http
                .put("/pedagogical-document/$id")
                .jsonBody(jsonMapper.writeValueAsString(body))
                .asUser(user)
                .responseString()
                .third
                .get()
        )

    private fun uploadDocumentAttachment(user: AuthenticatedUser, id: PedagogicalDocumentId) {
        http
            .upload("/attachments/pedagogical-documents/$id")
            .add(FileDataPart(File(pngFile.toURI()), name = "file"))
            .asUser(user)
            .response()
            .also { assertEquals(200, it.second.statusCode) }
    }

    private fun getEmailFor(person: DevPerson): Email {
        val address = person.email ?: throw Error("$person has no email")
        return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
    }
}
