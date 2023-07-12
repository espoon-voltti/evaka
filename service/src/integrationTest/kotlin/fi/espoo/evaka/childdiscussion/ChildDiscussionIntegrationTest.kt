package fi.espoo.evaka.childdiscussion

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChildDiscussionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired
    lateinit var controller: ChildDiscussionController

    lateinit var employeeUser: AuthenticatedUser

    val now = MockEvakaClock(2022, 1, 1, 15, 0)

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            employeeUser =
                    tx.insertTestEmployee(DevEmployee()).let {
                        AuthenticatedUser.Employee(it, setOf(UserRole.ADMIN))
                    }
            tx.insertTestCareArea(testArea)
            tx.insertTestDaycare(testDaycare.copy(language = Language.sv))
            tx.insertTestPerson(testChild_1)
            tx.insertTestChild(DevChild(testChild_1.id))
            tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = now.today(),
                    endDate = now.today().plusDays(5)
            )
        }
    }

    @Test
    fun `creating new discussion data and fetching it`() {
        val offeredDate = LocalDate.of(2023, 7, 1)
        val heldDate = LocalDate.of(2023, 7, 5)
        val counselingDate = LocalDate.of(2023, 7, 10)

        val discussionId =
                controller.createDiscussion(
                        dbInstance(),
                        employeeUser,
                        now,
                        testChild_1.id,
                        ChildDiscussionBody(offeredDate, heldDate, counselingDate)
                )

        val discussionData = controller.getDiscussionData(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(
                ChildDiscussion(
                        discussionId,
                        testChild_1.id,
                        offeredDate,
                        heldDate,
                        counselingDate
                ),
                discussionData
        )
    }

    @Test
    fun `should throw conflict when creating discussion data for child with existing data`() {
        val discussionId =
                controller.createDiscussion(
                        dbInstance(),
                        employeeUser,
                        now,
                        testChild_1.id,
                        ChildDiscussionBody(
                                LocalDate.of(2023, 7, 1),
                                LocalDate.of(2023, 7, 5),
                                LocalDate.of(2023, 7, 10)
                        )
                )

        assertNotNull(discussionId)

        assertThrows<Conflict> {
            controller.createDiscussion(
                    dbInstance(),
                    employeeUser,
                    now,
                    testChild_1.id,
                    ChildDiscussionBody(
                            LocalDate.of(2023, 7, 1),
                            LocalDate.of(2023, 7, 5),
                            LocalDate.of(2023, 7, 10)
                    )
            )
        }
    }


    @Test
    fun `updating discussion data for child`() {
        val offeredDate = LocalDate.of(2023, 7, 1)
        val heldDate = LocalDate.of(2023, 7, 5)
        val counselingDate = LocalDate.of(2023, 7, 10)

        val discussionId =
                controller.createDiscussion(
                        dbInstance(),
                        employeeUser,
                        now,
                        testChild_1.id,
                        ChildDiscussionBody(offeredDate, heldDate, counselingDate)
                )

        val discussionData = controller.getDiscussionData(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(
                ChildDiscussion(
                        discussionId,
                        testChild_1.id,
                        offeredDate,
                        heldDate,
                        counselingDate
                ),
                discussionData
        )

        val newOfferedDate = LocalDate.of(2023, 8, 2)
        val newHeldDate = LocalDate.of(2023, 8, 6)
        val newCounselingDate = LocalDate.of(2023, 8, 11)

        controller.updateDiscussion(
                dbInstance(),
                employeeUser,
                now,
                testChild_1.id,
                ChildDiscussionBody(newOfferedDate, newHeldDate, newCounselingDate)
        )

        val updatedDiscussionData = controller.getDiscussionData(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(
                ChildDiscussion(
                        discussionId,
                        testChild_1.id,
                        newOfferedDate,
                        newHeldDate,
                        newCounselingDate
                ),
                updatedDiscussionData
        )
    }

    @Test
    fun `should throw not found when updating discussion data for child with no existing data`() {
        assertThrows<NotFound> {
            controller.updateDiscussion(
                    dbInstance(),
                    employeeUser,
                    now,
                    testChild_1.id,
                    ChildDiscussionBody(
                            LocalDate.of(2023, 8, 2),
                            LocalDate.of(2023, 8, 6),
                            LocalDate.of(2023, 8, 11)
                    )
            )
        }
    }
}