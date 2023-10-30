// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childdiscussion

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.shared.ChildDiscussionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildDiscussionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired lateinit var controller: ChildDiscussionController

    lateinit var employeeUser: AuthenticatedUser

    val now = MockEvakaClock(2022, 1, 1, 15, 0)

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            employeeUser =
                tx.insert(DevEmployee()).let {
                    AuthenticatedUser.Employee(it, setOf(UserRole.ADMIN))
                }
            tx.insert(testArea)
            tx.insert(testDaycare.copy(language = Language.sv))
            tx.insert(testChild_1, DevPersonType.CHILD)
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
        val offeredDate1 = LocalDate.of(2023, 7, 1)
        val heldDate1 = LocalDate.of(2023, 7, 5)
        val counselingDate1 = LocalDate.of(2023, 7, 10)

        val discussionId1 =
            controller.createDiscussion(
                dbInstance(),
                employeeUser,
                now,
                testChild_1.id,
                ChildDiscussionBody(offeredDate1, heldDate1, counselingDate1)
            )

        val offeredDate2 = LocalDate.of(2023, 8, 15)
        val heldDate2 = LocalDate.of(2023, 8, 17)
        val counselingDate2 = LocalDate.of(2023, 8, 20)

        val discussionId2 =
            controller.createDiscussion(
                dbInstance(),
                employeeUser,
                now,
                testChild_1.id,
                ChildDiscussionBody(offeredDate2, heldDate2, counselingDate2)
            )

        val discussionData =
            controller.getDiscussions(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(
            listOf(
                ChildDiscussionWithPermittedActions(
                    data =
                        ChildDiscussionData(
                            discussionId2,
                            testChild_1.id,
                            offeredDate2,
                            heldDate2,
                            counselingDate2
                        ),
                    permittedActions =
                        setOf(Action.ChildDiscussion.UPDATE, Action.ChildDiscussion.DELETE)
                ),
                ChildDiscussionWithPermittedActions(
                    data =
                        ChildDiscussionData(
                            discussionId1,
                            testChild_1.id,
                            offeredDate1,
                            heldDate1,
                            counselingDate1
                        ),
                    permittedActions =
                        setOf(Action.ChildDiscussion.UPDATE, Action.ChildDiscussion.DELETE)
                )
            ),
            discussionData
        )
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

        val discussionData =
            controller.getDiscussions(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(
            listOf(
                ChildDiscussionWithPermittedActions(
                    data =
                        ChildDiscussionData(
                            discussionId,
                            testChild_1.id,
                            offeredDate,
                            heldDate,
                            counselingDate
                        ),
                    permittedActions =
                        setOf(Action.ChildDiscussion.UPDATE, Action.ChildDiscussion.DELETE)
                )
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
            discussionId,
            ChildDiscussionBody(newOfferedDate, newHeldDate, newCounselingDate)
        )

        val updatedDiscussionData =
            controller.getDiscussions(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(
            listOf(
                ChildDiscussionWithPermittedActions(
                    data =
                        ChildDiscussionData(
                            discussionId,
                            testChild_1.id,
                            newOfferedDate,
                            newHeldDate,
                            newCounselingDate
                        ),
                    permittedActions =
                        setOf(Action.ChildDiscussion.UPDATE, Action.ChildDiscussion.DELETE)
                )
            ),
            updatedDiscussionData
        )
    }

    @Test
    fun `should throw not found when updating discussion data that does not exist`() {
        assertThrows<NotFound> {
            controller.updateDiscussion(
                dbInstance(),
                employeeUser,
                now,
                ChildDiscussionId(UUID.randomUUID()),
                ChildDiscussionBody(
                    LocalDate.of(2023, 8, 2),
                    LocalDate.of(2023, 8, 6),
                    LocalDate.of(2023, 8, 11)
                )
            )
        }
    }

    @Test
    fun `deleting discussion data by id`() {
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

        val discussionData =
            controller.getDiscussions(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(
            listOf(
                ChildDiscussionWithPermittedActions(
                    data =
                        ChildDiscussionData(
                            discussionId,
                            testChild_1.id,
                            offeredDate,
                            heldDate,
                            counselingDate
                        ),
                    permittedActions =
                        setOf(Action.ChildDiscussion.UPDATE, Action.ChildDiscussion.DELETE)
                )
            ),
            discussionData
        )

        controller.deleteDiscussion(dbInstance(), employeeUser, now, discussionId)

        val deletedDiscussionData =
            controller.getDiscussions(dbInstance(), employeeUser, now, testChild_1.id)
        assertEquals(emptyList(), deletedDiscussionData)
    }

    @Test
    fun `should throw not found when deleting discussion data that does not exist`() {
        assertThrows<NotFound> {
            controller.deleteDiscussion(
                dbInstance(),
                employeeUser,
                now,
                ChildDiscussionId(UUID.randomUUID())
            )
        }
    }
}
