// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.PureJdbiTest
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.application.syncApplicationOtherGuardians
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.Question
import evaka.core.document.Section
import evaka.core.document.childdocument.AnsweredQuestion
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.identity.Ssn
import evaka.core.identity.getDobFromSsn
import evaka.core.incomestatement.IncomeStatementBody
import evaka.core.messaging.MessageType
import evaka.core.messaging.getCitizenMessageAccount
import evaka.core.messaging.insertMessage
import evaka.core.messaging.insertMessageContent
import evaka.core.messaging.insertRecipients
import evaka.core.messaging.insertThread
import evaka.core.messaging.upsertEmployeeMessageAccount
import evaka.core.pis.service.MockVtjSoapTransport
import evaka.core.pis.service.VtjSyncService
import evaka.core.pis.service.blockGuardian
import evaka.core.pis.service.deleteGuardianRelationship
import evaka.core.pis.service.insertGuardian
import evaka.core.shared.PedagogicalDocumentId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevIncomeStatement
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPedagogicalDocument
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.dev.insertTestPartnership
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.user.updateLastStrongLogin
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InactivePeopleCleanupIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val testDate = LocalDate.of(2020, 3, 1)
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val adult1 = DevPerson()
    private val adult2 = DevPerson()
    private val child1 = DevPerson()
    private val child2 = DevPerson()

    private val vtjClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2026, 3, 2), LocalTime.of(12, 0)))
    private val vtjA = vtjPerson("150385-9013")
    private val vtjB = vtjPerson("200687-903A")
    private val vtjC = vtjPerson("101090-905X")
    private val vtjX = vtjPerson("050120A910J")
    private val vtjZ = vtjPerson("080622A9123")

    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.insert(area)
            it.insert(daycare)
        }
    }

    @Test
    fun `adult with no family is cleaned up`() {
        db.transaction { tx -> tx.insert(adult1, DevPersonType.RAW_ROW) }

        assertCleanedUpPeople(testDate, setOf(adult1.id))
    }

    @Test
    fun `guardian and their child are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.insertGuardian(adult1.id, child1.id)
        }

        assertCleanedUpPeople(testDate, setOf(adult1.id, child1.id))
    }

    @Test
    fun `blocked guardian is not cleaned up without any archived data`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.blockGuardian(child1.id, adult1.id)
        }

        assertCleanedUpPeople(testDate, setOf(child1.id))
    }

    @Test
    fun `head of family and their child are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
        }

        assertCleanedUpPeople(testDate, setOf(adult1.id, child1.id))
    }

    @Test
    fun `head of family and their partner are cleaned up when neither have archive data`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(adult2, DevPersonType.RAW_ROW)
            tx.insertTestPartnership(adult1 = adult1.id, adult2 = adult2.id)
        }

        assertCleanedUpPeople(testDate, setOf(adult1.id, adult2.id))
    }

    @Test
    fun `adult with no family that has logged in recently is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.execute {
                sql(
                    "UPDATE person SET last_login = ${bind(testDate.minusMonths(2))} WHERE id = ${bind(adult1.id)}"
                )
            }
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `guardian and their child are not cleaned up when guardian has logged in recently`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.insertGuardian(adult1.id, child1.id)
            tx.execute {
                sql(
                    "UPDATE person SET last_login = ${bind(testDate.minusDays(14))} WHERE id = ${bind(adult1.id)}"
                )
            }
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `persons linked by guardian rows to a kept person are kept through any number of links`() {
        // adult2 has logged in recently. child1 is a child of adult2 and adult1, and child2 is
        // another child of adult1.
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(adult2, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.insert(child2, DevPersonType.RAW_ROW)
            tx.insertGuardian(adult2.id, child1.id)
            tx.insertGuardian(adult1.id, child1.id)
            tx.insertGuardian(adult1.id, child2.id)
            tx.execute {
                sql(
                    "UPDATE person SET last_login = ${bind(testDate.minusDays(14))} WHERE id = ${bind(adult2.id)}"
                )
            }
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult who is saved in the application other guardian table is not cleaned up`() {
        lateinit var otherGuardian: PersonId
        db.transaction { tx ->
            val applicationOwner = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            otherGuardian = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val child = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(guardianId = applicationOwner, childId = child))
            tx.insert(DevGuardian(guardianId = otherGuardian, childId = child))
            val application =
                tx.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = applicationOwner,
                    childId = child,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            child = Child(dateOfBirth = null),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(daycare.id)),
                        ),
                )
            tx.syncApplicationOtherGuardians(application, testDate)
            tx.deleteGuardianRelationship(childId = child, guardianId = otherGuardian)
        }
        assertCleanedUpPeople(testDate, setOf())

        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        "DELETE FROM application_other_guardian WHERE guardian_id = ${bind(otherGuardian)}"
                    )
                }
                .execute()
        }
        assertCleanedUpPeople(testDate, setOf(otherGuardian))
    }

    @Test
    fun `head of family and their child are not cleaned up when child has a placement`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(DevPlacement(childId = child1.id, unitId = daycare.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(DevPlacement(childId = child1.id, unitId = daycare.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children and two adults is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(adult2, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insertTestPartnership(adult1 = adult1.id, adult2 = adult2.id)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(DevPlacement(childId = child1.id, unitId = daycare.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `family with two children with separate heads of family is not cleaned up when one of the children has a placement`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(adult2, DevPersonType.RAW_ROW)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insertTestPartnership(adult1 = adult1.id, adult2 = adult2.id)
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(DevPlacement(childId = child1.id, unitId = daycare.id))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with income statement is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(
                DevIncomeStatement(
                    personId = adult1.id,
                    data = IncomeStatementBody.HighestFee(LocalDate.now(), null),
                )
            )
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult and child with pedagogical document is not cleaned up`() {
        db.transaction { tx ->
            tx.insert(child1, DevPersonType.RAW_ROW)
            val docId = PedagogicalDocumentId(UUID.randomUUID())
            tx.insert(DevPedagogicalDocument(docId, child1.id, "document"))
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.createUpdate {
                    sql(
                        "INSERT INTO pedagogical_document_read (person_id, pedagogical_document_id, read_at) VALUES (${bind(adult1.id)}, ${bind(docId)}, ${bind(testDate)})"
                    )
                }
                .execute()
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult and child with child document is not cleaned up`() {
        val templateContent =
            DocumentTemplateContent(
                sections =
                    listOf(
                        Section(
                            id = "s1",
                            label = "Eka",
                            questions =
                                listOf(Question.TextQuestion(id = "q1", label = "kysymys 1")),
                        )
                    )
            )
        val documentContent =
            DocumentContent(
                answers = listOf(AnsweredQuestion.TextAnswer(questionId = "q1", answer = "foobar"))
            )
        val employee = DevEmployee()
        val employeeUser = AuthenticatedUser.Employee(employee.id, setOf(UserRole.ADMIN))
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(child1, DevPersonType.RAW_ROW)
            val template =
                tx.insert(
                    DevDocumentTemplate(
                        validity = DateRange(testDate, null),
                        content = templateContent,
                    )
                )
            val document =
                tx.insert(
                    DevChildDocument(
                        status = DocumentStatus.COMPLETED,
                        childId = child1.id,
                        templateId = template,
                        content = documentContent,
                        modifiedAt = HelsinkiDateTime.now(),
                        modifiedBy = employeeUser.evakaUserId,
                        contentLockedAt = HelsinkiDateTime.now(),
                        contentLockedBy = employeeUser.id,
                        publishedVersions =
                            listOf(
                                DevChildDocumentPublishedVersion(
                                    versionNumber = 1,
                                    createdAt = HelsinkiDateTime.now(),
                                    createdBy = employeeUser.evakaUserId,
                                    publishedContent = documentContent,
                                )
                            ),
                    )
                )
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.createUpdate {
                    sql(
                        "INSERT INTO child_document_read (person_id, document_id, read_at) VALUES (${bind(adult1.id)}, ${bind(document)}, ${bind(testDate)})"
                    )
                }
                .execute()
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `adult with received messages is cleaned up`() {
        val now = HelsinkiDateTime.now()
        db.transaction { tx ->
            val supervisor = DevEmployee()
            tx.insert(supervisor)
            val employeeAccount = tx.upsertEmployeeMessageAccount(supervisor.id)

            tx.insert(adult1, DevPersonType.ADULT)
            val personAccount = tx.getCitizenMessageAccount(adult1.id)

            val contentId = tx.insertMessageContent("content", employeeAccount)
            val threadId =
                tx.insertThread(
                    MessageType.MESSAGE,
                    "title",
                    urgent = false,
                    sensitive = false,
                    isCopy = false,
                )
            val messageId =
                tx.insertMessage(
                    now = now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = employeeAccount,
                    sentAt = now,
                    recipientNames = listOf("recipient name"),
                    municipalAccountName = "Espoo",
                    serviceWorkerAccountName = "Espoon palveluohjaus",
                    financeAccountName = "Espoon asiakasmaksut",
                )
            tx.insertRecipients(listOf(messageId to setOf(personAccount)))
        }

        assertCleanedUpPeople(testDate, setOf(adult1.id))
    }

    @Test
    fun `adult with sent messages is not cleaned up`() {
        val now = HelsinkiDateTime.now()
        db.transaction { tx ->
            val supervisor = DevEmployee()
            tx.insert(supervisor)
            val employeeAccount = tx.upsertEmployeeMessageAccount(supervisor.id)

            tx.insert(adult1, DevPersonType.ADULT)
            val personAccount = tx.getCitizenMessageAccount(adult1.id)

            val contentId = tx.insertMessageContent("content", personAccount)
            val threadId =
                tx.insertThread(
                    MessageType.MESSAGE,
                    "title",
                    urgent = false,
                    sensitive = false,
                    isCopy = false,
                )
            val messageId =
                tx.insertMessage(
                    now = now,
                    contentId = contentId,
                    threadId = threadId,
                    sender = personAccount,
                    sentAt = now,
                    recipientNames = listOf("employee name"),
                    municipalAccountName = "Espoo",
                    serviceWorkerAccountName = "Espoon palveluohjaus",
                    financeAccountName = "Espoon asiakasmaksut",
                )
            tx.insertRecipients(listOf(messageId to setOf(employeeAccount)))
        }

        assertCleanedUpPeople(testDate, setOf())
    }

    @Test
    fun `a family synced from VTJ is cleaned up only where no sync would create the persons again`() {
        // Y was a child of A, but VTJ no longer lists Y as a dependant of A
        val vtjY = vtjPerson("011025A9028")
        db.transaction { tx ->
            tx.insert(vtjA, DevPersonType.RAW_ROW)
            tx.insert(vtjY, DevPersonType.CHILD)
            tx.insertGuardian(vtjA.id, vtjY.id)
        }
        val laterClock = MockEvakaClock(vtjClock.now().plusYears(1))
        withVtjSync { vtjSync, _ ->
            strongLogin(vtjSync, vtjA, vtjClock)
            // B logs in a year later than A, so the cleanup keeps B because of the login. It keeps
            // A only through the shared child X.
            strongLogin(vtjSync, vtjB, laterClock)

            assertCleanedUpPeople(laterClock.today().plusDays(1), setOf(vtjY.id))

            val user = AuthenticatedUser.SystemInternalUser
            db.read { tx -> tx.createQuery { sql("SELECT id FROM person") }.toList<PersonId>() }
                .forEach { vtjSync.refreshPerson(db, user.evakaUserId, laterClock, it) }

            assertTrue(setOf(vtjA.ssn, vtjB.ssn, vtjX.ssn).all { it in personSsns() })
            assertFalse(vtjY.ssn in personSsns())
        }
    }

    private fun vtjPerson(ssn: String) = DevPerson(ssn = ssn, dateOfBirth = getDobFromSsn(ssn))

    /**
     * Fixture family: A is a citizen. X is a child of A and B. B is also a guardian of Z, whose
     * other guardian is C.
     */
    private fun withVtjSync(f: (VtjSyncService, AsyncJobRunner<AsyncJob>) -> Unit) {
        MockVtjSoapTransport.resetQueryCounts()
        addToVtj(vtjA, dependants = listOf(vtjX))
        addToVtj(vtjB, dependants = listOf(vtjX, vtjZ))
        addToVtj(vtjC, dependants = listOf(vtjZ))
        addToVtj(vtjX, guardians = listOf(vtjA, vtjB))
        addToVtj(vtjZ, guardians = listOf(vtjB, vtjC))
        AsyncJobRunner(AsyncJob::class, listOf(AsyncJob.main), jdbi, noopTracer).use { runner ->
            f(
                VtjSyncService(SoapVtjClient(MockVtjSoapTransport(), VtjHenkiloMapper()), runner),
                runner,
            )
        }
    }

    private fun addToVtj(
        person: DevPerson,
        dependants: List<DevPerson> = emptyList(),
        guardians: List<DevPerson> = emptyList(),
    ) {
        MockVtjSoapTransport.addPERUSSANOMA3RequestExpectation(person)
        MockVtjSoapTransport.addHUOLTAJAHUOLLETTAVARequestExpectation(person, dependants)
        MockVtjSoapTransport.addHUOLLETTAVAHUOLTAJATRequestExpectation(person, guardians)
    }

    /** Does what SystemController.citizenLogin does, without the controller */
    private fun strongLogin(
        vtjSync: VtjSyncService,
        person: DevPerson,
        clock: MockEvakaClock = vtjClock,
    ): PersonId {
        val user = AuthenticatedUser.SystemInternalUser
        val id =
            vtjSync
                .getOrCreatePerson(db, user.evakaUserId, clock, Ssn.parse(person.ssn!!)!!)
                .person
                .id
        db.transaction { tx ->
            tx.updateLastStrongLogin(clock.now(), id)
            tx.updateCitizenOnLogin(clock.now(), id)
        }
        vtjSync.ensureDependantsSynced(db, user.evakaUserId, clock, id)
        return id
    }

    private fun personSsns(): Set<String?> = db.read { tx ->
        tx.createQuery { sql("SELECT social_security_number FROM person") }.toSet<String?>()
    }

    private fun assertCleanedUpPeople(queryDate: LocalDate, cleanedUpPeople: Set<PersonId>) {
        val result = db.transaction { cleanUpInactivePeople(it, queryDate) }

        assertEquals(cleanedUpPeople, result)
    }
}
