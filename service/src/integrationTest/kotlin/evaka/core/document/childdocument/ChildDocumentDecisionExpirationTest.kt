// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document.childdocument

import evaka.core.PureJdbiTest
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentTemplateContent
import evaka.core.placement.PlacementType
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentDecision
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChildDocumentDecisionExpirationTest : PureJdbiTest(resetDbBeforeEach = true) {
    val today = LocalDate.of(2025, 4, 1)
    val yesterday = today.minusDays(1)
    val now = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
    val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    val careArea = DevCareArea()
    val daycare1 = DevDaycare(areaId = careArea.id, name = "Daycare 1")
    val daycare2 = DevDaycare(areaId = careArea.id, name = "Daycare 2")
    val child = DevPerson()

    @BeforeEach
    fun basicSetup() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(careArea)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    private fun setup(
        placementType: PlacementType = PlacementType.DAYCARE,
        placementStart: LocalDate = today.minusMonths(6),
        placementEnd: LocalDate = today.plusMonths(6),
        placementUnit: DaycareId = daycare1.id,
        decisionStatus: ChildDocumentDecisionStatus = ChildDocumentDecisionStatus.ACCEPTED,
        decisionValidityStart: LocalDate = today.minusMonths(3),
        decisionValidityEnd: LocalDate? = today.plusMonths(3),
        decisionUnit: DaycareId = daycare1.id,
        endDecisionWhenUnitChanges: Boolean = true,
    ) {
        val decisionTemplate =
            DevDocumentTemplate(
                name = "Varhaiskasvatuksen tuen päätös",
                type = ChildDocumentType.OTHER_DECISION,
                placementTypes = setOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME),
                validity = DateRange(today.minusMonths(6), null),
                content = DocumentTemplateContent(emptyList()),
                endDecisionWhenUnitChanges = endDecisionWhenUnitChanges,
            )

        db.transaction { tx ->
            tx.insert(decisionTemplate)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    type = placementType,
                    startDate = placementStart,
                    endDate = placementEnd,
                    unitId = placementUnit,
                )
            )
            tx.insert(
                DevChildDocument(
                    status = DocumentStatus.COMPLETED,
                    childId = child.id,
                    templateId = decisionTemplate.id,
                    content = DocumentContent(emptyList()),
                    modifiedAt = now.minusDays(5),
                    modifiedBy = admin.evakaUserId,
                    contentLockedAt = now.minusDays(5),
                    contentLockedBy = admin.id,
                    decisionMaker = admin.id,
                    decision =
                        DevChildDocumentDecision(
                            createdBy = admin.id,
                            modifiedBy = admin.id,
                            status = decisionStatus,
                            validity =
                                DateRange(decisionValidityStart, decisionValidityEnd).takeIf {
                                    decisionStatus != ChildDocumentDecisionStatus.REJECTED
                                },
                            daycareId =
                                decisionUnit.takeIf {
                                    decisionStatus != ChildDocumentDecisionStatus.REJECTED
                                },
                        ),
                    publishedVersions =
                        listOf(
                            DevChildDocumentPublishedVersion(
                                versionNumber = 1,
                                createdAt = now.minusDays(5),
                                createdBy = admin.evakaUserId,
                                publishedContent = DocumentContent(emptyList()),
                            )
                        ),
                )
            )
        }
    }

    @Test
    fun `decision has not expired`() {
        setup()
        runJobAndExpect(false)
    }

    @Test
    fun `decision expires if there is no placement anymore`() {
        setup(placementEnd = yesterday)
        runJobAndExpect(true)
    }

    @Test
    fun `decision does not expire if placement type changes to another supported one`() {
        setup(placementEnd = yesterday)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare1.id,
                    type = PlacementType.DAYCARE_PART_TIME,
                    startDate = today,
                    endDate = today.plusMonths(6),
                )
            )
        }
        runJobAndExpect(false)
    }

    @Test
    fun `decision expires if placement type changes to an unsupported one`() {
        setup(placementEnd = yesterday)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare1.id,
                    type = PlacementType.PRESCHOOL,
                    startDate = today,
                    endDate = today.plusMonths(6),
                )
            )
        }
        runJobAndExpect(true)
    }

    @Test
    fun `decision expires if placement unit has changed today`() {
        setup(placementEnd = yesterday)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2.id,
                    type = PlacementType.DAYCARE,
                    startDate = today,
                    endDate = today.plusMonths(6),
                )
            )
        }
        runJobAndExpect(true)
    }

    @Test
    fun `decision does not re-expire if placement unit has changed already earlier and expiration has been manually reverted`() {
        setup(placementEnd = yesterday.minusDays(1))
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2.id,
                    type = PlacementType.DAYCARE,
                    startDate = yesterday,
                    endDate = today.plusMonths(6),
                )
            )
        }
        runJobAndExpect(false)
    }

    @Test
    fun `decision does not expire if placement unit changes but related flag is false`() {
        setup(placementEnd = yesterday, endDecisionWhenUnitChanges = false)
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare2.id,
                    type = PlacementType.DAYCARE,
                    startDate = today,
                    endDate = today.plusMonths(6),
                )
            )
        }
        runJobAndExpect(false)
    }

    @Test
    fun `decision does not expire if placement unit changes to one with the same name`() {
        setup(placementEnd = yesterday)
        db.transaction { tx ->
            val daycare1b = DevDaycare(areaId = careArea.id, name = daycare1.name)
            tx.insert(daycare1b)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare1b.id,
                    type = PlacementType.DAYCARE,
                    startDate = today,
                    endDate = today.plusMonths(6),
                )
            )
        }
        runJobAndExpect(false)
    }

    @Test
    fun `decision does not re-expire if validity has already ended`() {
        setup(placementEnd = today.minusDays(5), decisionValidityEnd = today.minusDays(5))
        runJobAndExpect(false)
    }

    @Test
    fun `decision does not expire if validity has not started`() {
        setup(placementEnd = today.minusDays(5), decisionValidityStart = today.plusDays(1))
        runJobAndExpect(false)
    }

    @Test
    fun `expiring to zero days length does not cause error`() {
        setup(placementEnd = today.minusDays(5), decisionValidityStart = today)
        runJobAndExpect(false)
    }

    private fun runJobAndExpect(expectExpired: Boolean) {
        db.transaction { tx ->
            val expiredCount = tx.endExpiredChildDocumentDecisions(today).size
            assertEquals(if (expectExpired) 1 else 0, expiredCount)
            val validityEnd = tx.getChildDocuments(child.id).first().decision?.validity?.end
            assertEquals(expectExpired, validityEnd == yesterday)
        }
    }
}
