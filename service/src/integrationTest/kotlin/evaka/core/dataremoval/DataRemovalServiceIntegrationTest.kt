// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.DataRemovalEnv
import evaka.core.FullApplicationTest
import evaka.core.absence.application.AbsenceApplication
import evaka.core.absence.application.AbsenceApplicationStatus
import evaka.core.application.ApplicationAttachmentType
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child as ApplicationFormChild
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.assistance.OtherAssistanceMeasureType
import evaka.core.attachment.AttachmentParent
import evaka.core.attachment.insertAttachment
import evaka.core.calendarevent.CalendarEventType
import evaka.core.caseprocess.CaseProcessState
import evaka.core.caseprocess.insertCaseProcess
import evaka.core.childimages.insertChildImage
import evaka.core.dailyservicetimes.DailyServiceTimesType
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentDeletionBasis
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.finance.notes.createFinanceNote
import evaka.core.holidayperiod.QuestionnaireType
import evaka.core.incomestatement.IncomeStatementBody
import evaka.core.incomestatement.IncomeStatementStatus
import evaka.core.insertServiceNeedOptions
import evaka.core.invoicing.service.IncomeNotificationType
import evaka.core.invoicing.service.createIncomeNotification
import evaka.core.nekku.NekkuProductMealType
import evaka.core.note.child.sticky.ChildStickyNoteBody
import evaka.core.note.child.sticky.createChildStickyNote
import evaka.core.placement.PlacementSource
import evaka.core.placement.PlacementType
import evaka.core.s3.DocumentService
import evaka.core.sficlient.SentSfiMessage
import evaka.core.sficlient.rest.EventType
import evaka.core.sficlient.storeSentSfiMessage
import evaka.core.shared.AbsenceApplicationId
import evaka.core.shared.ApplicationId
import evaka.core.shared.AssistanceActionOptionId
import evaka.core.shared.AttachmentId
import evaka.core.shared.BackupPickupId
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.ChildId
import evaka.core.shared.DecisionId
import evaka.core.shared.IncomeNotificationId
import evaka.core.shared.IncomeStatementId
import evaka.core.shared.PedagogicalDocumentId
import evaka.core.shared.PersonId
import evaka.core.shared.PlacementId
import evaka.core.shared.ServiceApplicationId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevAssistanceAction
import evaka.core.shared.dev.DevAssistanceActionOption
import evaka.core.shared.dev.DevAssistanceFactor
import evaka.core.shared.dev.DevBackupCare
import evaka.core.shared.dev.DevBackupPickup
import evaka.core.shared.dev.DevCalendarEvent
import evaka.core.shared.dev.DevCalendarEventAttendee
import evaka.core.shared.dev.DevCalendarEventTime
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChild
import evaka.core.shared.dev.DevChildAttendance
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDailyServiceTimes
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareAssistance
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFamilyContact
import evaka.core.shared.dev.DevFosterParent
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevFridgePartnership
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevHolidayQuestionnaire
import evaka.core.shared.dev.DevHolidayQuestionnaireAnswer
import evaka.core.shared.dev.DevIncomeStatement
import evaka.core.shared.dev.DevOtherAssistanceMeasure
import evaka.core.shared.dev.DevPedagogicalDocument
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevPlacementDraft
import evaka.core.shared.dev.DevPlacementPlan
import evaka.core.shared.dev.DevPreschoolAssistance
import evaka.core.shared.dev.DevReservation
import evaka.core.shared.dev.DevServiceApplication
import evaka.core.shared.dev.DevSfiMessageEvent
import evaka.core.shared.dev.TestDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.dev.insertTestDecision
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.snDefaultDaycare
import evaka.core.specialdiet.MealTexture
import evaka.core.specialdiet.SpecialDiet
import evaka.core.specialdiet.setMealTextures
import evaka.core.specialdiet.setSpecialDiets
import evaka.core.user.updateLastStrongLogin
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils

class DataRemovalServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var dataRemovalService: DataRemovalService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var documentClient: DocumentService

    private val today = LocalDate.of(2026, 5, 7)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(2, 0))
    private val clock = MockEvakaClock(now)

    private val leafExpireDate = today.minusYears(1)
    private val imageExpireDate = today.minusMonths(1)
    private val financeExpireDate = today.minusYears(5)
    private val tenYearExpireDate = today.minusYears(10)
    private val applicationExpireDate = today.minusYears(10)
    private val incomeStatementExpireDate = today.minusYears(1)
    private val incomeNotificationExpireDate = today.minusYears(1)
    private val removableIncomeStatementStatuses =
        setOf(IncomeStatementStatus.DRAFT, IncomeStatementStatus.HANDLED)

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val careArea = DevCareArea()
    private val daycare = DevDaycare(areaId = careArea.id)
    private val daycareGroup = DevDaycareGroup(daycareId = daycare.id)
    private val child = DevPerson()

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(careArea)
            tx.insert(daycare)
            tx.insert(daycareGroup)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `handler enqueues one DeleteChildDocumentPdf job per published key across all deleted documents`() {
        val template = insertTemplate()
        insertExpiredDocument(template, keys = listOf("s3-key-a-v1", "s3-key-a-v2"))
        insertExpiredDocument(template, keys = listOf("s3-key-b-v1"))
        // A pending PDF (null key) must not leak as a null payload.
        insertExpiredDocument(template, keys = listOf("s3-key-c-v1", null))

        dataRemovalService.deleteExpiredChildDocuments(db, now, limit = 100)

        assertEquals(
            setOf("s3-key-a-v1", "s3-key-a-v2", "s3-key-b-v1", "s3-key-c-v1"),
            scheduledPdfDeletionKeys(),
        )
    }

    @Test
    fun `handler enqueues nothing when no documents are eligible for deletion`() {
        val template = insertTemplate()
        // Recently modified document is not yet expired.
        insertDocument(template, statusModifiedAt = now, keys = listOf("fresh-key"))

        dataRemovalService.deleteExpiredChildDocuments(db, now, limit = 1000)

        assertTrue(scheduledPdfDeletionKeys().isEmpty())
    }

    @Test
    fun `handler enqueues nothing when expired documents have no published PDFs`() {
        val template = insertTemplate()
        insertExpiredDocument(template, keys = emptyList())

        dataRemovalService.deleteExpiredChildDocuments(db, now, limit = 1000)

        assertTrue(scheduledPdfDeletionKeys().isEmpty())
    }

    private fun scheduledPdfDeletionKeys(): Set<String> =
        db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT payload::json->>'key' FROM async_job WHERE type = 'DeleteChildDocumentPdf'"
                        )
                    }
                    .toList<String>()
            }
            .toSet()

    private fun insertTemplate(retentionDays: Int = 10): DevDocumentTemplate {
        val template =
            DevDocumentTemplate(
                type = ChildDocumentType.PEDAGOGICAL_REPORT,
                placementTypes = setOf(PlacementType.DAYCARE),
                validity = DateRange(today.minusYears(5), null),
                content = DocumentTemplateContent(emptyList()),
                deletionRetentionDays = retentionDays,
                deletionRetentionBasis = DocumentDeletionBasis.STATUS_TRANSITION,
            )
        db.transaction { tx -> tx.insert(template) }
        return template
    }

    private fun insertExpiredDocument(
        template: DevDocumentTemplate,
        keys: List<String?>,
    ): ChildDocumentId = insertDocument(template, statusModifiedAt = now.minusYears(1), keys = keys)

    private fun insertDocument(
        template: DevDocumentTemplate,
        statusModifiedAt: HelsinkiDateTime,
        keys: List<String?>,
    ): ChildDocumentId {
        val publishedVersions = keys.mapIndexed { index, key ->
            DevChildDocumentPublishedVersion(
                versionNumber = index + 1,
                createdAt = statusModifiedAt.plusMinutes(index.toLong()),
                createdBy = admin.evakaUserId,
                publishedContent = DocumentContent(emptyList()),
                documentKey = key,
            )
        }
        val doc =
            DevChildDocument(
                status = DocumentStatus.COMPLETED,
                childId = child.id,
                templateId = template.id,
                content = DocumentContent(emptyList()),
                modifiedAt = statusModifiedAt,
                modifiedBy = admin.evakaUserId,
                statusModifiedAt = statusModifiedAt,
                contentLockedAt = statusModifiedAt,
                contentLockedBy = admin.id,
                publishedVersions = publishedVersions,
            )
        return db.transaction { tx -> tx.insert(doc) }
    }

    @Test
    fun `deleteExpiredChildLeafRows deletes rows in every listed table for an expired child`() {
        insertExpiredPlacement(child.id)
        insertCalendarEventAttendee(child.id)
        insertCalendarEventTime(child.id)
        insertChildStickyNote(child.id)
        insertFamilyContact(child.id)
        insertNekkuSpecialDietChoice(child.id)

        deleteExpiredChildLeafRows(
            db,
            expireDate = leafExpireDate,
            limit = 100,
            leafTables = allLeafTables,
        )

        allLeafTables.forEach { assertEquals(0, rowCount(it), "table $it should be empty") }
    }

    @Test
    fun `deleteExpiredChildLeafRows preserves rows for active children`() {
        insertActivePlacement(child.id)
        insertChildStickyNote(child.id)
        insertCalendarEventAttendee(child.id)

        deleteExpiredChildLeafRows(
            db,
            expireDate = leafExpireDate,
            limit = 100,
            leafTables = allLeafTables,
        )

        assertEquals(1, rowCount("child_sticky_note"))
        assertEquals(1, rowCount("calendar_event_attendee"))
    }

    @Test
    fun `deleteExpiredChildLeafRows respects limit`() {
        insertExpiredPlacement(child.id)
        repeat(5) { insertChildStickyNote(child.id) }

        deleteExpiredChildLeafRows(
            db,
            expireDate = leafExpireDate,
            limit = 2,
            leafTables = listOf("child_sticky_note"),
        )

        assertEquals(3, rowCount("child_sticky_note"))
    }

    @Test
    fun `deleteExpiredChildLeafRows with empty leaf table list is a no-op`() {
        insertExpiredPlacement(child.id)
        insertChildStickyNote(child.id)

        deleteExpiredChildLeafRows(
            db,
            expireDate = leafExpireDate,
            limit = 100,
            leafTables = emptyList(),
        )

        assertEquals(1, rowCount("child_sticky_note"))
    }

    @Test
    fun `unsetExpiredChildReferences nulls diet_id, meal_texture_id, and nekku_diet for expired children`() {
        insertExpiredPlacement(child.id)
        setUpChildDietReferences(child.id)

        unsetExpiredChildReferences(
            db,
            expireDate = leafExpireDate,
            limit = 100,
            columns = listOf("diet_id", "meal_texture_id", "nekku_diet"),
        )

        assertEquals(
            ChildDietColumns(dietId = null, mealTextureId = null, nekkuDiet = null),
            readChildDietColumns(child.id),
        )
    }

    @Test
    fun `unsetExpiredChildReferences preserves values for active children`() {
        insertActivePlacement(child.id)
        setUpChildDietReferences(child.id)

        unsetExpiredChildReferences(
            db,
            expireDate = leafExpireDate,
            limit = 100,
            columns = listOf("diet_id", "meal_texture_id", "nekku_diet"),
        )

        assertEquals(
            ChildDietColumns(
                dietId = 1,
                mealTextureId = 2,
                nekkuDiet = NekkuProductMealType.VEGAN.name,
            ),
            readChildDietColumns(child.id),
        )
    }

    @Test
    fun `unsetExpiredChildReferences respects limit across columns`() {
        val children = (1..5).map { DevPerson() }
        db.transaction { tx ->
            children.forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.setSpecialDiets(listOf(SpecialDiet(1, "diet")))
            tx.setMealTextures(listOf(MealTexture(2, "texture")))
        }
        children.forEach {
            insertExpiredPlacement(it.id)
            db.transaction { tx ->
                tx.insert(
                    DevChild(
                        id = it.id,
                        dietId = 1,
                        mealTextureId = 2,
                        nekkuDiet = NekkuProductMealType.VEGAN,
                    )
                )
            }
        }

        unsetExpiredChildReferences(
            db,
            expireDate = leafExpireDate,
            limit = 2,
            columns = listOf("diet_id"),
        )

        assertEquals(3, countChildrenWithNull("diet_id"))
    }

    @Test
    fun `deleteExpiredChildImages deletes child_images row and enqueues DeleteChildImage job for expired child`() {
        insertPlacementEnding(child.id, today.minusMonths(2))
        val imageId = db.transaction { it.insertChildImage(child.id) }

        dataRemovalService.deleteExpiredChildImages(
            db,
            now,
            expireDate = imageExpireDate,
            limit = 100,
        )

        assertEquals(0, rowCount("child_images"))
        assertEquals(setOf(imageId.toString()), scheduledChildImageDeletionIds())
    }

    @Test
    fun `deleteExpiredChildImages preserves images for children at the 1-month boundary`() {
        insertPlacementEnding(child.id, today.minusDays(15))
        db.transaction { it.insertChildImage(child.id) }

        dataRemovalService.deleteExpiredChildImages(
            db,
            now,
            expireDate = imageExpireDate,
            limit = 100,
        )

        assertEquals(1, rowCount("child_images"))
        assertTrue(scheduledChildImageDeletionIds().isEmpty())
    }

    @Test
    fun `deleteExpiredChildImages respects limit`() {
        val children = (1..5).map { DevPerson() }
        db.transaction { tx -> children.forEach { tx.insert(it, DevPersonType.CHILD) } }
        children.forEach {
            insertPlacementEnding(it.id, today.minusMonths(2))
            db.transaction { tx -> tx.insertChildImage(it.id) }
        }

        dataRemovalService.deleteExpiredChildImages(
            db,
            now,
            expireDate = imageExpireDate,
            limit = 2,
        )

        assertEquals(3, rowCount("child_images"))
        assertEquals(2, scheduledChildImageDeletionIds().size)
    }

    @Test
    fun `deleteExpiredData with limit zero performs no deletions`() {
        insertExpiredPlacement(child.id)
        setUpChildDietReferences(child.id)
        insertChildStickyNote(child.id)
        insertFamilyContact(child.id)
        db.transaction { it.insertChildImage(child.id) }

        withLimit(0) { dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData) }

        assertEquals(1, rowCount("child_sticky_note"))
        assertEquals(1, rowCount("family_contact"))
        assertEquals(1, rowCount("child_images"))
        assertEquals(
            ChildDietColumns(
                dietId = 1,
                mealTextureId = 2,
                nekkuDiet = NekkuProductMealType.VEGAN.name,
            ),
            readChildDietColumns(child.id),
        )
        assertTrue(scheduledChildImageDeletionIds().isEmpty())
    }

    @Test
    fun `deleteExpiredData cleans up leaf rows, child references, and images in one pass`() {
        insertExpiredPlacement(child.id)
        setUpChildDietReferences(child.id)
        insertCalendarEventAttendee(child.id)
        insertCalendarEventTime(child.id)
        insertChildStickyNote(child.id)
        insertFamilyContact(child.id)
        insertNekkuSpecialDietChoice(child.id)
        val imageId = db.transaction { it.insertChildImage(child.id) }

        withLimit(100) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        allLeafTables.forEach { assertEquals(0, rowCount(it), "table $it should be empty") }
        assertEquals(0, rowCount("child_images"))
        assertEquals(
            ChildDietColumns(dietId = null, mealTextureId = null, nekkuDiet = null),
            readChildDietColumns(child.id),
        )
        assertEquals(setOf(imageId.toString()), scheduledChildImageDeletionIds())
    }

    @Test
    fun `deleteExpiredData removes ten-year leaf rows for a child whose last placement ended over ten years ago`() {
        insertPlacementEnding(child.id, tenYearExpireDate.minusDays(1))
        insertTenYearLeafData(child.id)

        withLimit(100) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        allTenYearLeafTables.forEach { assertEquals(0, rowCount(it), "table $it should be empty") }
        assertEquals(
            0,
            rowCount("assistance_action_option_ref"),
            "assistance_action_option_ref should be emptied via cascade",
        )
    }

    @Test
    fun `deleteExpiredData keeps ten-year leaf rows for a child whose last placement ended within ten years`() {
        // Expired by the 1-year leaf window but not by the 10-year window
        insertExpiredPlacement(child.id)
        insertTenYearLeafData(child.id)

        withLimit(100) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        allTenYearLeafTables.forEach { assertEquals(1, rowCount(it), "table $it should be kept") }
        assertEquals(1, rowCount("assistance_action_option_ref"))
    }

    @Test
    fun `deleteExpiredData keeps ten-year leaf rows for a child whose last placement ends exactly on the ten-year boundary`() {
        insertPlacementEnding(child.id, tenYearExpireDate)
        insertTenYearLeafData(child.id)

        withLimit(100) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        allTenYearLeafTables.forEach { assertEquals(1, rowCount(it), "table $it should be kept") }
        assertEquals(1, rowCount("assistance_action_option_ref"))
    }

    @Test
    fun `child with no placements is not considered expired`() {
        // child has no placements at all
        setUpChildDietReferences(child.id)

        unsetExpiredChildReferences(
            db,
            expireDate = leafExpireDate,
            limit = 100,
            columns = listOf("diet_id"),
        )

        assertEquals(0, countChildrenWithNull("diet_id"))
    }

    @Test
    fun `child whose only placement ends after expireDate is not considered expired`() {
        insertPlacementEnding(child.id, leafExpireDate.plusDays(1))
        setUpChildDietReferences(child.id)

        unsetExpiredChildReferences(
            db,
            expireDate = leafExpireDate,
            limit = 100,
            columns = listOf("diet_id"),
        )

        assertEquals(0, countChildrenWithNull("diet_id"))
    }

    @Test
    fun `child whose only placement ends before expireDate is considered expired`() {
        insertPlacementEnding(child.id, leafExpireDate.minusDays(1))
        setUpChildDietReferences(child.id)

        unsetExpiredChildReferences(
            db,
            expireDate = leafExpireDate,
            limit = 100,
            columns = listOf("diet_id"),
        )

        assertEquals(1, countChildrenWithNull("diet_id"))
    }

    @Test
    fun `child with an old placement but a recent active one is not considered expired`() {
        // Old placement that ended years ago
        insertPlacement(child.id, startDate = today.minusYears(5), endDate = today.minusYears(4))
        // Later placement still ongoing
        insertPlacement(child.id, startDate = today.minusMonths(2), endDate = today.plusYears(1))
        setUpChildDietReferences(child.id)

        unsetExpiredChildReferences(
            db,
            expireDate = leafExpireDate,
            limit = 100,
            columns = listOf("diet_id"),
        )

        assertEquals(0, countChildrenWithNull("diet_id"))
    }

    @Test
    fun `deleteExpiredCitizenUsers deletes citizen user whose child's last placement ended before expireDate`() {
        val guardian = insertAdultWithCitizenUser()
        insertGuardianship(guardian, child.id)
        insertPlacementEnding(child.id, leafExpireDate.minusDays(1))

        deleteExpiredCitizenUsers(db, expireDate = leafExpireDate, limit = 100)

        assertFalse(citizenUserExists(guardian))
    }

    @Test
    fun `deleteExpiredCitizenUsers keeps citizen user whose child has a recent placement`() {
        val guardian = insertAdultWithCitizenUser()
        insertGuardianship(guardian, child.id)
        insertPlacementEnding(child.id, leafExpireDate.plusDays(1))

        deleteExpiredCitizenUsers(db, expireDate = leafExpireDate, limit = 100)

        assertTrue(citizenUserExists(guardian))
    }

    @Test
    fun `deleteExpiredCitizenUsers keeps citizen user whose child placement ends exactly on expireDate`() {
        val guardian = insertAdultWithCitizenUser()
        insertGuardianship(guardian, child.id)
        insertPlacementEnding(child.id, leafExpireDate)

        deleteExpiredCitizenUsers(db, expireDate = leafExpireDate, limit = 100)

        assertTrue(citizenUserExists(guardian))
    }

    @Test
    fun `deleteExpiredCitizenUsers keeps citizen user with one expired and one recent child placement`() {
        val guardian = insertAdultWithCitizenUser()
        val recentChild = DevPerson()
        db.transaction { it.insert(recentChild, DevPersonType.CHILD) }
        insertGuardianship(guardian, child.id)
        insertGuardianship(guardian, recentChild.id)
        insertPlacementEnding(child.id, leafExpireDate.minusDays(1))
        insertPlacementEnding(recentChild.id, leafExpireDate.plusDays(1))

        deleteExpiredCitizenUsers(db, expireDate = leafExpireDate, limit = 100)

        assertTrue(citizenUserExists(guardian))
    }

    @Test
    fun `deleteExpiredCitizenUsers keeps citizen user whose child has no placements`() {
        val guardian = insertAdultWithCitizenUser()
        insertGuardianship(guardian, child.id)

        deleteExpiredCitizenUsers(db, expireDate = leafExpireDate, limit = 100)

        assertTrue(citizenUserExists(guardian))
    }

    @Test
    fun `deleteExpiredCitizenUsers keeps citizen user with no guardian or foster relationship`() {
        val person = insertAdultWithCitizenUser()

        deleteExpiredCitizenUsers(db, expireDate = leafExpireDate, limit = 100)

        assertTrue(citizenUserExists(person))
    }

    @Test
    fun `deleteExpiredCitizenUsers deletes citizen user linked to an expired child via foster parent`() {
        val fosterParent = insertAdultWithCitizenUser()
        insertFosterParenthood(fosterParent, child.id)
        insertPlacementEnding(child.id, leafExpireDate.minusDays(1))

        deleteExpiredCitizenUsers(db, expireDate = leafExpireDate, limit = 100)

        assertFalse(citizenUserExists(fosterParent))
    }

    @Test
    fun `deleteExpiredCitizenUsers keeps foster parent whose foster parenthood ended long ago while the child still has a recent placement`() {
        val fosterParent = insertAdultWithCitizenUser()
        insertFosterParenthood(
            fosterParent,
            child.id,
            validDuring = DateRange(today.minusYears(3), today.minusYears(2)),
        )
        insertPlacementEnding(child.id, today.plusYears(1))

        deleteExpiredCitizenUsers(db, expireDate = leafExpireDate, limit = 100)

        assertTrue(citizenUserExists(fosterParent))
    }

    @Test
    fun `deleteExpiredFinanceNotes deletes note of adult whose child's last placement ended before expireDate`() {
        val guardian = insertAdult()
        insertGuardianship(guardian, child.id)
        insertPlacementEnding(child.id, financeExpireDate.minusDays(1))
        insertFinanceNote(guardian)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(0, financeNoteCount(guardian))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps note of adult whose child has a recent placement`() {
        val guardian = insertAdult()
        insertGuardianship(guardian, child.id)
        insertPlacementEnding(child.id, financeExpireDate.plusDays(1))
        insertFinanceNote(guardian)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(guardian))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps note of adult whose child placement ends exactly on expireDate`() {
        val guardian = insertAdult()
        insertGuardianship(guardian, child.id)
        insertPlacementEnding(child.id, financeExpireDate)
        insertFinanceNote(guardian)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(guardian))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps note of adult with one expired and one recent child placement`() {
        val guardian = insertAdult()
        val recentChild = DevPerson()
        db.transaction { it.insert(recentChild, DevPersonType.CHILD) }
        insertGuardianship(guardian, child.id)
        insertGuardianship(guardian, recentChild.id)
        insertPlacementEnding(child.id, financeExpireDate.minusDays(1))
        insertPlacementEnding(recentChild.id, financeExpireDate.plusDays(1))
        insertFinanceNote(guardian)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(guardian))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps note of adult whose child has no placements`() {
        val guardian = insertAdult()
        insertGuardianship(guardian, child.id)
        insertFinanceNote(guardian)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(guardian))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps note of adult with no family or finance connection to any placed child`() {
        val person = insertAdult()
        insertFinanceNote(person)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(person))
    }

    @Test
    fun `deleteExpiredFinanceNotes deletes note of head of family who is not a guardian when the child's last placement ended before expireDate`() {
        val head = insertAdult()
        insertHeadOfChild(head, child.id)
        insertPlacementEnding(child.id, financeExpireDate.minusDays(1))
        insertFinanceNote(head)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(0, financeNoteCount(head))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps note of head of family whose head-of-child relationship ended long ago while the child still has a recent placement`() {
        val head = insertAdult()
        insertHeadOfChild(head, child.id, endDate = financeExpireDate.minusDays(1))
        insertPlacementEnding(child.id, today.plusYears(1))
        insertFinanceNote(head)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(head))
    }

    @Test
    fun `deleteExpiredFinanceNotes deletes note of fee-paying partner who is not guardian or head once the household child's placement ended before expireDate`() {
        val head = insertAdult()
        val partner = insertAdult()
        insertHeadOfChild(head, child.id)
        insertPartnership(head, partner)
        insertPlacementEnding(child.id, financeExpireDate.minusDays(1))
        insertFinanceNote(partner)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(0, financeNoteCount(partner))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps note of fee-paying partner whose partnership ended long ago while the household child has a recent placement`() {
        val head = insertAdult()
        val partner = insertAdult()
        insertHeadOfChild(head, child.id)
        insertPartnership(head, partner, endDate = financeExpireDate.minusDays(1))
        insertPlacementEnding(child.id, today.plusYears(1))
        insertFinanceNote(partner)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(partner))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps head note when the only head-of-child link is a conflict row even though the child placement expired`() {
        val head = insertAdult()
        insertHeadOfChild(head, child.id, conflict = true)
        insertPlacementEnding(child.id, financeExpireDate.minusDays(1))
        insertFinanceNote(head)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(head))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps partner note when the only partnership link is a conflict row even though the child placement expired`() {
        val head = insertAdult()
        val partner = insertAdult()
        insertHeadOfChild(head, child.id)
        insertPartnership(head, partner, conflict = true)
        insertPlacementEnding(child.id, financeExpireDate.minusDays(1))
        insertFinanceNote(partner)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(partner))
    }

    @Test
    fun `deleteExpiredFinanceNotes deletes note of adult linked to an expired child via foster parent`() {
        val fosterParent = insertAdult()
        insertFosterParenthood(fosterParent, child.id)
        insertPlacementEnding(child.id, financeExpireDate.minusDays(1))
        insertFinanceNote(fosterParent)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(0, financeNoteCount(fosterParent))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps foster parent note when foster parenthood ended long ago while the child still has a recent placement`() {
        val fosterParent = insertAdult()
        insertFosterParenthood(
            fosterParent,
            child.id,
            validDuring = DateRange(today.minusYears(8), financeExpireDate.minusDays(1)),
        )
        insertPlacementEnding(child.id, today.plusYears(1))
        insertFinanceNote(fosterParent)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(fosterParent))
    }

    @Test
    fun `deleteExpiredFinanceNotes keeps foster parent note when the foster child has no placements`() {
        val fosterParent = insertAdult()
        insertFosterParenthood(
            fosterParent,
            child.id,
            validDuring = DateRange(today.minusYears(8), today.minusYears(7)),
        )
        insertFinanceNote(fosterParent)

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 100)

        assertEquals(1, financeNoteCount(fosterParent))
    }

    @Test
    fun `deleteExpiredFinanceNotes respects limit`() {
        val guardian = insertAdult()
        insertGuardianship(guardian, child.id)
        insertPlacementEnding(child.id, financeExpireDate.minusDays(1))
        repeat(5) { insertFinanceNote(guardian) }

        deleteExpiredFinanceNotes(db, expireDate = financeExpireDate, limit = 2)

        assertEquals(3, financeNoteCount(guardian))
    }

    @Test
    fun `deleteExpiredServiceApplications deletes application whose child's last placement ended over ten years ago`() {
        val guardian = insertAdult()
        setupServiceNeedOptions()
        insertPlacementEnding(child.id, tenYearExpireDate.minusDays(1))
        insertServiceApplication(child.id, guardian)

        deleteExpiredServiceApplications(db, expireDate = tenYearExpireDate, limit = 100)

        assertEquals(0, serviceApplicationCount())
    }

    @Test
    fun `deleteExpiredServiceApplications keeps applications whose child's placement ends on or after the ten-year boundary`() {
        val guardian = insertAdult()
        setupServiceNeedOptions()

        insertPlacementEnding(child.id, tenYearExpireDate)
        insertServiceApplication(child.id, guardian)

        val recentChild = DevPerson()
        db.transaction { it.insert(recentChild, DevPersonType.CHILD) }
        insertPlacementEnding(recentChild.id, tenYearExpireDate.plusDays(1))
        insertServiceApplication(recentChild.id, guardian)

        deleteExpiredServiceApplications(db, expireDate = tenYearExpireDate, limit = 100)

        assertEquals(2, serviceApplicationCount(), "both applications are retained")
    }

    @Test
    fun `deleteExpiredServiceApplications clears the provenance of an expired referenced placement but spares one whose child is still within retention`() {
        val guardian = insertAdult()
        setupServiceNeedOptions()

        val expiredServiceApplicationId = insertServiceApplication(child.id, guardian)
        val expiredPlacementId =
            insertPlacementFromServiceApplication(
                child.id,
                expiredServiceApplicationId,
                endDate = tenYearExpireDate.minusDays(1),
            )

        // this child's referenced placement is just as old, but a newer placement keeps the child
        // within retention, so the application and the old placement's provenance must survive
        val retainedChild = DevPerson()
        db.transaction { it.insert(retainedChild, DevPersonType.CHILD) }
        val retainedServiceApplicationId = insertServiceApplication(retainedChild.id, guardian)
        val oldReferencedPlacementId =
            insertPlacementFromServiceApplication(
                retainedChild.id,
                retainedServiceApplicationId,
                endDate = tenYearExpireDate.minusDays(1),
            )
        insertPlacementEnding(retainedChild.id, tenYearExpireDate.plusYears(1))

        deleteExpiredServiceApplications(db, expireDate = tenYearExpireDate, limit = 100)

        assertEquals(
            1,
            serviceApplicationCount(),
            "only the still-within-retention application remains",
        )
        assertEquals(3, rowCount("placement"), "all placements are retained")
        assertEquals(
            null to null,
            readPlacementProvenance(expiredPlacementId),
            "the expired child's placement provenance is cleared",
        )
        assertEquals(
            PlacementSource.SERVICE_APPLICATION to retainedServiceApplicationId,
            readPlacementProvenance(oldReferencedPlacementId),
            "the within-retention child's old placement provenance is untouched",
        )
    }

    @Test
    fun `deleteExpiredServiceApplications doesn't remove more than the limit`() {
        val guardian = insertAdult()
        setupServiceNeedOptions()
        val children = (1..5).map { DevPerson() }
        db.transaction { tx -> children.forEach { tx.insert(it, DevPersonType.CHILD) } }
        children.forEach {
            insertPlacementEnding(it.id, tenYearExpireDate.minusDays(1))
            insertServiceApplication(it.id, guardian)
        }

        deleteExpiredServiceApplications(db, expireDate = tenYearExpireDate, limit = 2)

        assertEquals(3, serviceApplicationCount())
    }

    @Test
    fun `deleteExpiredData removes expired service applications in the full pass`() {
        val guardian = insertAdult()
        setupServiceNeedOptions()
        insertPlacementEnding(child.id, tenYearExpireDate.minusDays(1))
        insertServiceApplication(child.id, guardian)

        withLimit(100) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(0, serviceApplicationCount())
    }

    @Test
    fun `deleteExpiredData deletes finance notes only for adults whose family placements ended over five years ago`() {
        val expiredGuardian = insertAdult()
        insertGuardianship(expiredGuardian, child.id)
        insertPlacementEnding(child.id, today.minusYears(5).minusDays(1))
        insertFinanceNote(expiredGuardian)

        val activeGuardian = insertAdult()
        val activeChild = DevPerson()
        db.transaction { it.insert(activeChild, DevPersonType.CHILD) }
        insertGuardianship(activeGuardian, activeChild.id)
        insertPlacementEnding(activeChild.id, today.minusYears(5).plusDays(1))
        insertFinanceNote(activeGuardian)

        withLimit(100) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(0, financeNoteCount(expiredGuardian))
        assertEquals(1, financeNoteCount(activeGuardian))
    }

    private fun setupServiceNeedOptions() {
        db.transaction { it.insertServiceNeedOptions() }
    }

    private fun insertServiceApplication(
        childId: ChildId,
        personId: PersonId,
    ): ServiceApplicationId = db.transaction { tx ->
        tx.insert(
            DevServiceApplication(
                sentAt = now,
                personId = personId,
                childId = childId,
                startDate = today,
                serviceNeedOptionId = snDefaultDaycare.id,
            )
        )
    }

    private fun insertPlacementFromServiceApplication(
        childId: ChildId,
        serviceApplicationId: ServiceApplicationId,
        endDate: LocalDate,
    ): PlacementId = db.transaction { tx ->
        tx.insert(
            DevPlacement(
                childId = childId,
                unitId = daycare.id,
                startDate = endDate.minusYears(1),
                endDate = endDate,
                source = PlacementSource.SERVICE_APPLICATION,
                sourceServiceApplicationId = serviceApplicationId,
            )
        )
    }

    private fun serviceApplicationCount(): Int = rowCount("service_application")

    private fun readPlacementProvenance(
        id: PlacementId
    ): Pair<PlacementSource?, ServiceApplicationId?> = db.read { tx ->
        tx.createQuery {
                sql(
                    "SELECT source, source_service_application_id FROM placement WHERE id = ${bind(id)}"
                )
            }
            .exactlyOne {
                column<PlacementSource?>("source") to
                    column<ServiceApplicationId?>("source_service_application_id")
            }
    }

    private fun insertAdult(): PersonId {
        val adult = DevPerson()
        db.transaction { it.insert(adult, DevPersonType.ADULT) }
        return adult.id
    }

    private fun insertFinanceNote(personId: PersonId) {
        db.transaction { it.createFinanceNote(personId, "note", admin.user, now) }
    }

    private fun insertHeadOfChild(
        head: PersonId,
        childId: ChildId,
        startDate: LocalDate = today.minusYears(10),
        endDate: LocalDate = today.plusYears(10),
        conflict: Boolean = false,
    ) {
        db.transaction {
            it.insert(
                DevFridgeChild(
                    childId = childId,
                    headOfChild = head,
                    startDate = startDate,
                    endDate = endDate,
                    conflict = conflict,
                )
            )
        }
    }

    private fun insertPartnership(
        first: PersonId,
        second: PersonId,
        startDate: LocalDate = today.minusYears(10),
        endDate: LocalDate = today.plusYears(10),
        conflict: Boolean = false,
    ) {
        db.transaction {
            it.insert(
                DevFridgePartnership(
                    first = first,
                    second = second,
                    startDate = startDate,
                    endDate = endDate,
                    createdAt = now,
                    conflict = conflict,
                )
            )
        }
    }

    private fun financeNoteCount(personId: PersonId): Int = db.read { tx ->
        tx.createQuery {
                sql("SELECT count(*) FROM finance_note WHERE person_id = ${bind(personId)}")
            }
            .exactlyOne<Int>()
    }

    private fun insertAdultWithCitizenUser(): PersonId = db.transaction { tx ->
        val id = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
        tx.updateLastStrongLogin(now, id)
        id
    }

    private fun insertGuardianship(guardian: PersonId, childId: ChildId) {
        db.transaction { it.insert(DevGuardian(guardianId = guardian, childId = childId)) }
    }

    private fun insertFosterParenthood(
        parent: PersonId,
        childId: ChildId,
        validDuring: DateRange = DateRange(today.minusYears(2), today.minusYears(1)),
    ) {
        db.transaction {
            it.insert(
                DevFosterParent(
                    childId = childId,
                    parentId = parent,
                    validDuring = validDuring,
                    modifiedAt = now,
                    modifiedBy = admin.evakaUserId,
                )
            )
        }
    }

    private fun citizenUserExists(id: PersonId): Boolean = db.read { tx ->
        tx.createQuery { sql("SELECT EXISTS(SELECT FROM citizen_user WHERE id = ${bind(id)})") }
            .exactlyOne<Boolean>()
    }

    private val allLeafTables =
        listOf(
            "calendar_event_attendee",
            "calendar_event_time",
            "child_sticky_note",
            "family_contact",
            "nekku_special_diet_choices",
        )

    private val allAssistanceTables =
        listOf(
            "assistance_factor",
            "assistance_action",
            "daycare_assistance",
            "preschool_assistance",
            "other_assistance_measure",
        )

    private fun insertAssistanceData(childId: ChildId) {
        val actionOption =
            DevAssistanceActionOption(id = AssistanceActionOptionId(UUID.randomUUID()))
        db.transaction { tx ->
            tx.insert(DevAssistanceFactor(childId = childId))
            tx.insert(actionOption)
            tx.insert(DevAssistanceAction(childId = childId, actions = setOf(actionOption.value)))
            tx.insert(DevDaycareAssistance(childId = childId))
            tx.insert(DevPreschoolAssistance(childId = childId))
            tx.insert(
                DevOtherAssistanceMeasure(
                    childId = childId,
                    type = OtherAssistanceMeasureType.TRANSPORT_BENEFIT,
                )
            )
        }
    }

    private val placementLeafTables =
        listOf(
            "daily_service_time",
            "attendance_reservation",
            "backup_care",
            "child_attendance",
            "absence_application",
            "backup_pickup",
            "holiday_questionnaire_answer",
        )

    private fun insertPlacementLeafData(childId: ChildId) {
        val date = today.minusYears(12)
        db.transaction { tx ->
            tx.insert(
                DevDailyServiceTimes(
                    childId = childId,
                    type = DailyServiceTimesType.REGULAR,
                    validityPeriod = DateRange(date, null),
                )
            )
            tx.insert(
                DevReservation(
                    childId = childId,
                    date = date,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = admin.evakaUserId,
                )
            )
            tx.insert(
                DevBackupCare(
                    childId = childId,
                    unitId = daycare.id,
                    period = FiniteDateRange(date, date),
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = childId,
                    unitId = daycare.id,
                    date = date,
                    arrived = LocalTime.of(8, 0),
                    departed = LocalTime.of(16, 0),
                )
            )
            tx.insert(
                AbsenceApplication(
                    id = AbsenceApplicationId(UUID.randomUUID()),
                    createdAt = now,
                    createdBy = admin.evakaUserId,
                    modifiedAt = now,
                    modifiedBy = admin.evakaUserId,
                    childId = childId,
                    startDate = date,
                    endDate = date,
                    description = "test",
                    status = AbsenceApplicationStatus.WAITING_DECISION,
                    decidedAt = null,
                    decidedBy = null,
                    rejectedReason = null,
                )
            )
            tx.insert(
                DevBackupPickup(
                    id = BackupPickupId(UUID.randomUUID()),
                    childId = childId,
                    name = "Pickup Person",
                    phone = "0401234567",
                )
            )
            val questionnaire =
                DevHolidayQuestionnaire(
                    type = QuestionnaireType.FIXED_PERIOD,
                    periodOptions = null,
                    periodOptionLabel = null,
                    period = null,
                    absenceTypeThreshold = null,
                )
            tx.insert(questionnaire)
            tx.insert(
                DevHolidayQuestionnaireAnswer(
                    modifiedBy = admin.evakaUserId,
                    questionnaireId = questionnaire.id,
                    childId = childId,
                    fixedPeriod = FiniteDateRange(date, date),
                )
            )
        }
    }

    private val allTenYearLeafTables = allAssistanceTables + placementLeafTables

    private fun insertTenYearLeafData(childId: ChildId) {
        insertAssistanceData(childId)
        insertPlacementLeafData(childId)
    }

    private fun withLimit(limit: Int, block: () -> Unit) {
        ReflectionTestUtils.setField(
            dataRemovalService,
            "dataRemovalEnv",
            DataRemovalEnv(limit = limit),
        )
        block()
    }

    private fun insertExpiredPlacement(childId: ChildId) =
        insertPlacementEnding(childId, leafExpireDate.minusDays(1))

    private fun insertActivePlacement(childId: ChildId) =
        insertPlacementEnding(childId, today.plusYears(1))

    private fun insertPlacementEnding(childId: ChildId, endDate: LocalDate) =
        insertPlacement(childId, startDate = endDate.minusYears(1), endDate = endDate)

    private fun insertPlacement(childId: ChildId, startDate: LocalDate, endDate: LocalDate) {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
    }

    private fun insertPedagogicalDocument(childId: ChildId): PedagogicalDocumentId =
        db.transaction { tx ->
            tx.insert(
                DevPedagogicalDocument(
                    id = PedagogicalDocumentId(UUID.randomUUID()),
                    childId = childId,
                    description = "desc",
                    createdAt = now,
                    modifiedAt = now,
                )
            )
        }

    private fun insertPedagogicalDocumentAttachment(
        documentId: PedagogicalDocumentId
    ): AttachmentId = db.transaction { tx ->
        tx.insertAttachment(
            admin.user,
            now,
            "ped-doc.pdf",
            "application/pdf",
            AttachmentParent.PedagogicalDocument(documentId),
            type = null,
        )
    }

    private fun insertPedagogicalDocumentRead(
        documentId: PedagogicalDocumentId,
        personId: PersonId,
    ) {
        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        """
INSERT INTO pedagogical_document_read (pedagogical_document_id, person_id, read_at)
VALUES (${bind(documentId)}, ${bind(personId)}, ${bind(now)})
"""
                    )
                }
                .execute()
        }
    }

    private fun insertIncomeStatement(
        personId: PersonId,
        createdAt: HelsinkiDateTime,
        status: IncomeStatementStatus,
        body: IncomeStatementBody =
            IncomeStatementBody.HighestFee(startDate = createdAt.toLocalDate(), endDate = null),
    ): IncomeStatementId = db.transaction { tx ->
        tx.insert(
            DevIncomeStatement(
                personId = personId,
                createdAt = createdAt,
                data = body,
                status = status,
                sentAt = if (status == IncomeStatementStatus.DRAFT) null else createdAt,
                handlerId = if (status == IncomeStatementStatus.HANDLED) admin.id else null,
                handledAt = if (status == IncomeStatementStatus.HANDLED) createdAt else null,
            )
        )
    }

    private fun insertIncomeNotification(
        receiverId: PersonId,
        createdAt: HelsinkiDateTime,
    ): IncomeNotificationId = db.transaction { tx ->
        val id = tx.createIncomeNotification(receiverId, IncomeNotificationType.INITIAL_EMAIL)
        tx.execute {
            sql(
                "UPDATE income_notification SET created = ${bind(createdAt)} WHERE id = ${bind(id)}"
            )
        }
        id
    }

    private fun insertIncomeStatementAttachment(statementId: IncomeStatementId): AttachmentId =
        db.transaction { tx ->
            tx.insertAttachment(
                admin.user,
                now,
                "income.pdf",
                "application/pdf",
                AttachmentParent.IncomeStatement(statementId),
                type = null,
            )
        }

    private fun insertCalendarEvent(): DevCalendarEvent {
        val event =
            DevCalendarEvent(
                title = "title",
                description = "desc",
                period = FiniteDateRange(today, today),
                modifiedAt = now,
                modifiedBy = admin.evakaUserId,
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        db.transaction { tx -> tx.insert(event) }
        return event
    }

    private fun insertCalendarEventAttendee(childId: ChildId) {
        val event = insertCalendarEvent()
        db.transaction { tx ->
            tx.insert(
                DevCalendarEventAttendee(
                    calendarEventId = event.id,
                    unitId = daycare.id,
                    groupId = daycareGroup.id,
                    childId = childId,
                )
            )
        }
    }

    private fun insertCalendarEventTime(childId: ChildId) {
        val event = insertCalendarEvent()
        db.transaction { tx ->
            tx.insert(
                DevCalendarEventTime(
                    calendarEventId = event.id,
                    date = today,
                    start = LocalTime.of(9, 0),
                    end = LocalTime.of(10, 0),
                    childId = childId,
                    modifiedAt = now,
                    modifiedBy = admin.evakaUserId,
                )
            )
        }
    }

    private fun insertChildStickyNote(childId: ChildId) {
        db.transaction { tx ->
            tx.createChildStickyNote(
                childId = childId,
                note = ChildStickyNoteBody(note = "note", expires = today.plusDays(1)),
            )
        }
    }

    private fun insertFamilyContact(childId: ChildId) {
        val contactPerson = DevPerson()
        db.transaction { tx ->
            tx.insert(contactPerson, DevPersonType.ADULT)
            tx.insert(
                DevFamilyContact(
                    id = UUID.randomUUID(),
                    childId = childId,
                    contactPersonId = contactPerson.id,
                    priority = 1,
                )
            )
        }
    }

    private fun insertNekkuSpecialDietChoice(childId: ChildId) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                    INSERT INTO nekku_special_diet (id, name) VALUES ('test-diet', 'Test Diet')
                    ON CONFLICT DO NOTHING
                    """
                )
            }
            tx.execute {
                sql(
                    """
                    INSERT INTO nekku_special_diet_field (id, name, type, diet_id)
                    VALUES ('test-field', 'Test Field', 'TEXT'::nekku_special_diet_type, 'test-diet')
                    ON CONFLICT DO NOTHING
                    """
                )
            }
            tx.execute {
                sql(
                    """
                    INSERT INTO nekku_special_diet_choices (child_id, diet_id, field_id, value, created_at)
                    VALUES (${bind(childId)}, 'test-diet', 'test-field', 'val', ${bind(now)})
                    """
                )
            }
        }
    }

    private fun setUpChildDietReferences(childId: ChildId) {
        db.transaction { tx ->
            tx.setSpecialDiets(listOf(SpecialDiet(1, "diet")))
            tx.setMealTextures(listOf(MealTexture(2, "texture")))
            tx.insert(
                DevChild(
                    id = childId,
                    dietId = 1,
                    mealTextureId = 2,
                    nekkuDiet = NekkuProductMealType.VEGAN,
                )
            )
        }
    }

    private fun rowCount(table: String): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM $table") }.exactlyOne<Int>()
    }

    private fun countChildrenWithNull(column: String): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM child WHERE $column IS NULL") }.exactlyOne<Int>()
    }

    private data class ChildDietColumns(
        val dietId: Int?,
        val mealTextureId: Int?,
        val nekkuDiet: String?,
    )

    private fun readChildDietColumns(childId: ChildId): ChildDietColumns = db.read { tx ->
        tx.createQuery {
                sql(
                    "SELECT diet_id, meal_texture_id, nekku_diet::text AS nekku_diet FROM child WHERE id = ${bind(childId)}"
                )
            }
            .exactlyOne<ChildDietColumns>()
    }

    private fun scheduledChildImageDeletionIds(): Set<String> =
        db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT payload::json->>'imageId' FROM async_job WHERE type = 'DeleteChildImage'"
                        )
                    }
                    .toList<String>()
            }
            .toSet()

    @Test
    fun `deleteExpiredApplications deletes an application and all related rows when the child's last placement ended over ten years ago`() {
        insertApplicationTree(placementEnd = applicationExpireDate.minusDays(1))

        dataRemovalService.deleteExpiredApplications(db, now, applicationExpireDate, limit = 100)

        listOf(
                "application",
                "application_note",
                "application_other_guardian",
                "decision",
                "placement_plan",
                "placement_draft",
                "sfi_message",
                "sfi_message_event",
                "case_process",
                "case_process_history",
            )
            .forEach { assertEquals(0, rowCount(it), "table $it should be empty") }
    }

    @Test
    fun `deleteExpiredApplications deletes only the expired application tree and leaves a fresh one intact`() {
        val expired =
            insertApplicationTree(
                placementEnd = applicationExpireDate.minusDays(1),
                decisionKey = "expired-decision",
                otherGuardianKey = "expired-other-guardian",
            )
        val fresh =
            insertApplicationTree(
                placementEnd = applicationExpireDate.plusDays(1),
                decisionKey = "fresh-decision",
                otherGuardianKey = "fresh-other-guardian",
            )

        dataRemovalService.deleteExpiredApplications(db, now, applicationExpireDate, limit = 100)

        listOf(
                "application",
                "application_note",
                "application_other_guardian",
                "decision",
                "placement_plan",
                "placement_draft",
                "sfi_message",
                "sfi_message_event",
                "case_process",
                "case_process_history",
            )
            .forEach { assertEquals(1, rowCount(it), "table $it should retain only the fresh row") }

        assertEquals(listOf(fresh.applicationId), survivingApplicationIds())
        assertEquals(listOf(fresh.decisionId), survivingDecisionIds())
        assertEquals(
            setOf("expired-decision", "expired-other-guardian"),
            scheduledDecisionPdfDeletionKeys(),
        )
        assertEquals(setOf(expired.attachmentId.toString()), scheduledAttachmentDeletionIds())
    }

    @Test
    fun `deleteExpiredApplications schedules DeleteDecisionPdf jobs for both decision document keys`() {
        insertApplicationTree(
            placementEnd = applicationExpireDate.minusDays(1),
            decisionKey = "decision-key-a",
            otherGuardianKey = "decision-key-b",
        )

        dataRemovalService.deleteExpiredApplications(db, now, applicationExpireDate, limit = 100)

        assertEquals(setOf("decision-key-a", "decision-key-b"), scheduledDecisionPdfDeletionKeys())
    }

    @Test
    fun `deleteExpiredApplications schedules DeleteAttachment jobs and leaves the attachment row for the job to remove`() {
        val tree = insertApplicationTree(placementEnd = applicationExpireDate.minusDays(1))

        dataRemovalService.deleteExpiredApplications(db, now, applicationExpireDate, limit = 100)

        assertEquals(setOf(tree.attachmentId.toString()), scheduledAttachmentDeletionIds())
        assertEquals(1, rowCount("attachment"))
    }

    @Test
    fun `deleteExpiredApplications retains an application whose child's last placement ends exactly on the ten-year boundary`() {
        insertApplicationTree(placementEnd = applicationExpireDate)

        dataRemovalService.deleteExpiredApplications(db, now, applicationExpireDate, limit = 100)

        assertEquals(1, rowCount("application"))
        assertTrue(scheduledDecisionPdfDeletionKeys().isEmpty())
    }

    @Test
    fun `deleteExpiredApplications nulls all provenance references while keeping the referencing rows`() {
        val tree = insertApplicationTree(placementEnd = applicationExpireDate.minusDays(1))
        val survivingChild = DevPerson()
        val referencingPlacementId = PlacementId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(survivingChild, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    id = referencingPlacementId,
                    childId = survivingChild.id,
                    unitId = daycare.id,
                    startDate = today.minusMonths(1),
                    endDate = today.plusMonths(1),
                    source = PlacementSource.APPLICATION,
                    sourceApplicationId = tree.applicationId,
                )
            )
        }
        attachApplicationProvenance(tree.applicationId)

        dataRemovalService.deleteExpiredApplications(db, now, applicationExpireDate, limit = 100)

        assertEquals(0, rowCount("application"))
        assertEquals(
            null to null,
            readPlacementSource(referencingPlacementId),
            "placement source and source_application_id should be nulled",
        )
        listOf(
                Triple("placement", "source_application_id", 2),
                Triple("income", "application_id", 1),
                Triple("fridge_child", "created_by_application", 1),
                Triple("fridge_child", "create_source", 1),
                Triple("fridge_partner", "created_from_application", 2),
                Triple("fridge_partner", "create_source", 2),
                Triple("message_thread", "application_id", 1),
            )
            .forEach { (table, column, survivingRows) ->
                assertEquals(survivingRows, rowCount(table), "rows in $table should survive")
                assertEquals(0, countNonNull(table, column), "$table.$column should be nulled")
            }
    }

    private fun readPlacementSource(id: PlacementId): Pair<PlacementSource?, ApplicationId?> =
        db.read { tx ->
            tx.createQuery {
                    sql(
                        "SELECT source, source_application_id FROM placement WHERE id = ${bind(id)}"
                    )
                }
                .exactlyOne {
                    column<PlacementSource?>("source") to
                        column<ApplicationId?>("source_application_id")
                }
        }

    private fun attachApplicationProvenance(applicationId: ApplicationId) {
        db.transaction { tx ->
            val head = DevPerson()
            val partner = DevPerson()
            val provenanceChild = DevPerson()
            tx.insert(head, DevPersonType.ADULT)
            tx.insert(partner, DevPersonType.ADULT)
            tx.insert(provenanceChild, DevPersonType.CHILD)

            tx.execute {
                sql(
                    """
INSERT INTO income (person_id, data, valid_from, application_id, modified_by, modified_at, created_at, created_by)
VALUES (${bind(head.id)}, '{}'::jsonb, ${bind(today)}, ${bind(applicationId)}, ${bind(admin.evakaUserId)}, ${bind(now)}, ${bind(now)}, ${bind(admin.evakaUserId)})
"""
                )
            }

            val fridgeChild =
                DevFridgeChild(
                    childId = provenanceChild.id,
                    headOfChild = head.id,
                    startDate = today.minusYears(1),
                    endDate = today,
                )
            tx.insert(fridgeChild)
            tx.execute {
                sql(
                    "UPDATE fridge_child SET create_source = 'APPLICATION', created_by_application = ${bind(applicationId)} WHERE id = ${bind(fridgeChild.id)}"
                )
            }

            val partnership =
                DevFridgePartnership(
                    first = head.id,
                    second = partner.id,
                    startDate = today.minusYears(1),
                    createdAt = now,
                )
            tx.insert(partnership)
            tx.execute {
                sql(
                    "UPDATE fridge_partner SET create_source = 'APPLICATION', created_from_application = ${bind(applicationId)} WHERE partnership_id = ${bind(partnership.id)}"
                )
            }

            tx.execute {
                sql(
                    """
INSERT INTO message_thread (message_type, title, is_copy, sensitive, application_id)
VALUES ('MESSAGE'::message_type, 'title', false, false, ${bind(applicationId)})
"""
                )
            }
        }
    }

    private fun countNonNull(table: String, column: String): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM $table WHERE $column IS NOT NULL") }
            .exactlyOne<Int>()
    }

    @Test
    fun `deleteExpiredApplications respects the batch limit`() {
        repeat(3) { insertApplicationTree(placementEnd = applicationExpireDate.minusDays(1)) }

        dataRemovalService.deleteExpiredApplications(db, now, applicationExpireDate, limit = 2)

        assertEquals(1, rowCount("application"))
    }

    @Test
    fun `deleteExpiredData removes expired applications in one pass`() {
        val tree = insertApplicationTree(placementEnd = applicationExpireDate.minusDays(1))

        withLimit(100) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(0, rowCount("application"))
        assertTrue(scheduledAttachmentDeletionIds().contains(tree.attachmentId.toString()))
    }

    private data class ApplicationTree(
        val applicationId: ApplicationId,
        val decisionId: DecisionId,
        val attachmentId: AttachmentId,
    )

    private fun insertApplicationTree(
        placementEnd: LocalDate,
        decisionKey: String? = "decision-key",
        otherGuardianKey: String? = "other-guardian-key",
    ): ApplicationTree = db.transaction { tx ->
        val guardian = DevPerson()
        val otherGuardian = DevPerson()
        val applicationChild = DevPerson()
        tx.insert(guardian, DevPersonType.ADULT)
        tx.insert(otherGuardian, DevPersonType.ADULT)
        tx.insert(applicationChild, DevPersonType.CHILD)
        tx.insert(DevGuardian(guardianId = guardian.id, childId = applicationChild.id))
        tx.insert(
            DevPlacement(
                childId = applicationChild.id,
                unitId = daycare.id,
                startDate = placementEnd.minusYears(1),
                endDate = placementEnd,
            )
        )
        val process =
            tx.insertCaseProcess(
                processDefinitionNumber = "123.456.789",
                year = placementEnd.year,
                organization = "Espoon kaupunki",
                archiveDurationMonths = 120,
            )
        val applicationId =
            tx.insertTestApplication(
                type = ApplicationType.DAYCARE,
                guardianId = guardian.id,
                childId = applicationChild.id,
                otherGuardians = setOf(otherGuardian.id),
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = ApplicationFormChild(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                    ),
                processId = process.id,
            )
        tx.createUpdate {
                sql(
                    """
INSERT INTO application_note (application_id, content, created_by, modified_by, modified_at)
VALUES (${bind(applicationId)}, 'note', ${bind(admin.evakaUserId)}, ${bind(admin.evakaUserId)}, ${bind(now)})
"""
                )
            }
            .execute()
        tx.createUpdate {
                sql(
                    """
INSERT INTO case_process_history (process_id, row_index, state, entered_at, entered_by)
VALUES (${bind(process.id)}, 1, ${bind(CaseProcessState.INITIAL)}, ${bind(now)}, ${bind(admin.evakaUserId)})
"""
                )
            }
            .execute()
        val decisionId =
            tx.insertTestDecision(
                TestDecision(
                    createdBy = admin.evakaUserId,
                    sentDate = placementEnd,
                    unitId = daycare.id,
                    applicationId = applicationId,
                    type = DecisionType.DAYCARE,
                    startDate = placementEnd.minusYears(1),
                    endDate = placementEnd,
                    status = DecisionStatus.ACCEPTED,
                    documentKey = decisionKey,
                )
            )
        if (otherGuardianKey != null) {
            tx.createUpdate {
                    sql(
                        "UPDATE decision SET other_guardian_document_key = ${bind(otherGuardianKey)} WHERE id = ${bind(decisionId)}"
                    )
                }
                .execute()
        }
        val sfiMessageId =
            tx.storeSentSfiMessage(
                SentSfiMessage(guardianId = guardian.id, decisionId = decisionId)
            )
        tx.insert(
            DevSfiMessageEvent(
                messageId = sfiMessageId,
                eventType = EventType.ELECTRONIC_MESSAGE_CREATED,
            )
        )
        tx.insert(DevPlacementPlan(applicationId = applicationId, unitId = daycare.id))
        tx.insert(
            DevPlacementDraft(
                applicationId = applicationId,
                unitId = daycare.id,
                startDate = placementEnd.minusYears(1),
                createdBy = admin.evakaUserId,
                modifiedBy = admin.evakaUserId,
            )
        )
        val attachmentId =
            tx.insertAttachment(
                admin.user,
                now,
                "application.pdf",
                "application/pdf",
                AttachmentParent.Application(applicationId),
                type = ApplicationAttachmentType.URGENCY,
            )
        ApplicationTree(applicationId, decisionId, attachmentId)
    }

    private fun scheduledDecisionPdfDeletionKeys(): Set<String> =
        db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT payload::json->>'key' FROM async_job WHERE type = 'DeleteDecisionPdf'"
                        )
                    }
                    .toList<String>()
            }
            .toSet()

    private fun scheduledAttachmentDeletionIds(): Set<String> =
        db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT payload::json->>'attachmentId' FROM async_job WHERE type = 'DeleteAttachment'"
                        )
                    }
                    .toList<String>()
            }
            .toSet()

    private fun survivingApplicationIds(): List<ApplicationId> = db.read { tx ->
        tx.createQuery { sql("SELECT id FROM application") }.toList<ApplicationId>()
    }

    private fun survivingIncomeStatementIds(): Set<IncomeStatementId> =
        db.read { tx ->
                tx.createQuery { sql("SELECT id FROM income_statement") }
                    .toList<IncomeStatementId>()
            }
            .toSet()

    private fun survivingIncomeNotificationIds(): Set<IncomeNotificationId> =
        db.read { tx ->
                tx.createQuery { sql("SELECT id FROM income_notification") }
                    .toList<IncomeNotificationId>()
            }
            .toSet()

    private fun survivingDecisionIds(): List<DecisionId> = db.read { tx ->
        tx.createQuery { sql("SELECT id FROM decision") }.toList<DecisionId>()
    }

    @Test
    fun `deleteExpiredPedagogicalDocuments deletes document and read markers and enqueues DeleteAttachment per attachment for a child whose last placement ended over ten years ago`() {
        insertPlacementEnding(child.id, tenYearExpireDate.minusDays(1))
        val documentId = insertPedagogicalDocument(child.id)
        val attachmentA = insertPedagogicalDocumentAttachment(documentId)
        val attachmentB = insertPedagogicalDocumentAttachment(documentId)
        insertPedagogicalDocumentRead(documentId, child.id)

        dataRemovalService.deleteExpiredPedagogicalDocuments(
            db,
            now,
            expireDate = tenYearExpireDate,
            limit = 100,
        )

        assertEquals(0, rowCount("pedagogical_document"))
        assertEquals(0, rowCount("pedagogical_document_read"))
        assertEquals(
            setOf(attachmentA.toString(), attachmentB.toString()),
            scheduledAttachmentDeletionIds(),
        )
    }

    @Test
    fun `deleteExpiredPedagogicalDocuments preserves documents and attachments for a child whose last placement ended within ten years`() {
        insertPlacementEnding(child.id, today.minusYears(9))
        val documentId = insertPedagogicalDocument(child.id)
        insertPedagogicalDocumentAttachment(documentId)
        insertPedagogicalDocumentRead(documentId, child.id)

        dataRemovalService.deleteExpiredPedagogicalDocuments(
            db,
            now,
            expireDate = tenYearExpireDate,
            limit = 100,
        )

        assertEquals(1, rowCount("pedagogical_document"))
        assertEquals(1, rowCount("pedagogical_document_read"))
        assertEquals(1, rowCount("attachment"))
        assertTrue(scheduledAttachmentDeletionIds().isEmpty())
    }

    @Test
    fun `deleteExpiredPedagogicalDocuments doesn't remove more than the limit`() {
        insertPlacementEnding(child.id, tenYearExpireDate.minusDays(1))
        repeat(3) {
            val documentId = insertPedagogicalDocument(child.id)
            insertPedagogicalDocumentAttachment(documentId)
            insertPedagogicalDocumentRead(documentId, child.id)
        }

        dataRemovalService.deleteExpiredPedagogicalDocuments(
            db,
            now,
            expireDate = tenYearExpireDate,
            limit = 2,
        )

        assertEquals(1, rowCount("pedagogical_document"))
        assertEquals(1, rowCount("pedagogical_document_read"))
        assertEquals(2, scheduledAttachmentDeletionIds().size)
    }

    @Test
    fun `deleteExpiredData removes pedagogical documents for a child whose last placement ended over ten years ago`() {
        insertPlacementEnding(child.id, tenYearExpireDate.minusDays(1))
        val documentId = insertPedagogicalDocument(child.id)
        val attachmentId = insertPedagogicalDocumentAttachment(documentId)
        insertPedagogicalDocumentRead(documentId, child.id)

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(0, rowCount("pedagogical_document"))
        assertEquals(0, rowCount("pedagogical_document_read"))
        assertEquals(setOf(attachmentId.toString()), scheduledAttachmentDeletionIds())
    }

    @Test
    fun `deleteExpiredData keeps pedagogical documents for a child whose last placement ended within ten years`() {
        insertPlacementEnding(child.id, today.minusYears(9))
        val documentId = insertPedagogicalDocument(child.id)
        insertPedagogicalDocumentAttachment(documentId)

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(1, rowCount("pedagogical_document"))
        assertEquals(1, rowCount("attachment"))
        assertTrue(scheduledAttachmentDeletionIds().isEmpty())
    }

    @Test
    fun `deleteExpiredIncomeStatements deletes handled and draft statements created over a year ago and enqueues DeleteAttachment per attachment`() {
        val adultId = insertAdult()
        val expiredCreatedAt =
            HelsinkiDateTime.of(incomeStatementExpireDate.minusDays(1), LocalTime.of(2, 0))
        val handledId =
            insertIncomeStatement(adultId, expiredCreatedAt, IncomeStatementStatus.HANDLED)
        val attachmentA = insertIncomeStatementAttachment(handledId)
        val attachmentB = insertIncomeStatementAttachment(handledId)
        insertIncomeStatement(
            adultId,
            expiredCreatedAt,
            IncomeStatementStatus.DRAFT,
            body =
                IncomeStatementBody.HighestFee(
                    startDate = expiredCreatedAt.toLocalDate().plusDays(1),
                    endDate = null,
                ),
        )
        dataRemovalService.deleteExpiredIncomeStatements(
            db,
            now,
            expireDate = incomeStatementExpireDate,
            statuses = removableIncomeStatementStatuses,
            limit = 100,
        )

        assertEquals(0, rowCount("income_statement"))
        assertEquals(2, rowCount("attachment"))
        assertEquals(
            setOf(attachmentA.toString(), attachmentB.toString()),
            scheduledAttachmentDeletionIds(),
        )
    }

    @Test
    fun `deleteExpiredIncomeStatements preserves sent and handling statements regardless of age`() {
        val adultId = insertAdult()
        val oldCreatedAt = HelsinkiDateTime.of(today.minusYears(5), LocalTime.of(2, 0))
        val sentId = insertIncomeStatement(adultId, oldCreatedAt, IncomeStatementStatus.SENT)
        insertIncomeStatementAttachment(sentId)
        insertIncomeStatement(
            adultId,
            oldCreatedAt,
            IncomeStatementStatus.HANDLING,
            body =
                IncomeStatementBody.HighestFee(
                    startDate = oldCreatedAt.toLocalDate().plusDays(1),
                    endDate = null,
                ),
        )

        dataRemovalService.deleteExpiredIncomeStatements(
            db,
            now,
            expireDate = incomeStatementExpireDate,
            statuses = removableIncomeStatementStatuses,
            limit = 100,
        )

        assertEquals(2, rowCount("income_statement"))
        assertEquals(1, rowCount("attachment"))
        assertTrue(scheduledAttachmentDeletionIds().isEmpty())
    }

    @Test
    fun `deleteExpiredIncomeStatements preserves statements created at or after the expire date midnight`() {
        val adultId = insertAdult()
        insertIncomeStatement(
            adultId,
            HelsinkiDateTime.of(incomeStatementExpireDate, LocalTime.MIDNIGHT),
            IncomeStatementStatus.HANDLED,
        )

        dataRemovalService.deleteExpiredIncomeStatements(
            db,
            now,
            expireDate = incomeStatementExpireDate,
            statuses = removableIncomeStatementStatuses,
            limit = 100,
        )

        assertEquals(1, rowCount("income_statement"))
    }

    @Test
    fun `deleteExpiredIncomeStatements removes the oldest statements first up to the limit`() {
        val adultId = insertAdult()
        val newest =
            insertIncomeStatement(
                adultId,
                HelsinkiDateTime.of(incomeStatementExpireDate.minusDays(1), LocalTime.of(2, 0)),
                IncomeStatementStatus.HANDLED,
            )
        insertIncomeStatement(
            adultId,
            HelsinkiDateTime.of(incomeStatementExpireDate.minusDays(2), LocalTime.of(2, 0)),
            IncomeStatementStatus.HANDLED,
        )
        insertIncomeStatement(
            adultId,
            HelsinkiDateTime.of(incomeStatementExpireDate.minusDays(3), LocalTime.of(2, 0)),
            IncomeStatementStatus.HANDLED,
        )

        dataRemovalService.deleteExpiredIncomeStatements(
            db,
            now,
            expireDate = incomeStatementExpireDate,
            statuses = removableIncomeStatementStatuses,
            limit = 2,
        )

        assertEquals(setOf(newest), survivingIncomeStatementIds())
    }

    @Test
    fun `deleteExpiredIncomeNotifications deletes notifications created over a year ago and preserves newer ones`() {
        val adultId = insertAdult()
        insertIncomeNotification(
            adultId,
            HelsinkiDateTime.of(incomeNotificationExpireDate.minusDays(1), LocalTime.of(2, 0)),
        )
        val boundaryId =
            insertIncomeNotification(
                adultId,
                HelsinkiDateTime.of(incomeNotificationExpireDate, LocalTime.MIDNIGHT),
            )
        val recentId = insertIncomeNotification(adultId, now)

        deleteExpiredIncomeNotifications(db, expireDate = incomeNotificationExpireDate, limit = 100)

        assertEquals(setOf(boundaryId, recentId), survivingIncomeNotificationIds())
    }

    @Test
    fun `deleteExpiredIncomeNotifications doesn't remove more than the limit`() {
        val adultId = insertAdult()
        repeat(3) {
            insertIncomeNotification(
                adultId,
                HelsinkiDateTime.of(incomeNotificationExpireDate.minusDays(1), LocalTime.of(2, 0)),
            )
        }

        deleteExpiredIncomeNotifications(db, expireDate = incomeNotificationExpireDate, limit = 2)

        assertEquals(1, rowCount("income_notification"))
    }

    @Test
    fun `deleteExpiredData removes income statements and notifications created over a year ago`() {
        val adultId = insertAdult()
        val expiredCreatedAt =
            HelsinkiDateTime.of(incomeStatementExpireDate.minusDays(1), LocalTime.of(2, 0))
        val expiredNotificationCreatedAt =
            HelsinkiDateTime.of(incomeNotificationExpireDate.minusDays(1), LocalTime.of(2, 0))
        insertIncomeStatement(adultId, expiredCreatedAt, IncomeStatementStatus.HANDLED)
        insertIncomeStatement(
            adultId,
            expiredCreatedAt,
            IncomeStatementStatus.DRAFT,
            body =
                IncomeStatementBody.HighestFee(
                    startDate = expiredCreatedAt.toLocalDate().plusDays(1),
                    endDate = null,
                ),
        )
        val expiredSentId =
            insertIncomeStatement(
                adultId,
                expiredCreatedAt,
                IncomeStatementStatus.SENT,
                body =
                    IncomeStatementBody.HighestFee(
                        startDate = expiredCreatedAt.toLocalDate().plusDays(2),
                        endDate = null,
                    ),
            )
        val retainedHandledId =
            insertIncomeStatement(
                adultId,
                HelsinkiDateTime.of(incomeStatementExpireDate.plusDays(1), LocalTime.of(2, 0)),
                IncomeStatementStatus.HANDLED,
            )
        insertIncomeNotification(adultId, expiredNotificationCreatedAt)
        val recentNotificationId = insertIncomeNotification(adultId, now)

        withLimit(1000) {
            dataRemovalService.deleteExpiredData(db, clock, AsyncJob.DeleteExpiredData)
        }

        assertEquals(setOf(expiredSentId, retainedHandledId), survivingIncomeStatementIds())
        assertEquals(setOf(recentNotificationId), survivingIncomeNotificationIds())
    }
}
