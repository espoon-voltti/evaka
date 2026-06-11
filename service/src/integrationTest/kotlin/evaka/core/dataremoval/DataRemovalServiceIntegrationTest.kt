// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.DataRemovalEnv
import evaka.core.FullApplicationTest
import evaka.core.calendarevent.CalendarEventType
import evaka.core.childimages.insertChildImage
import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentDeletionBasis
import evaka.core.document.DocumentTemplateContent
import evaka.core.document.childdocument.DocumentContent
import evaka.core.document.childdocument.DocumentStatus
import evaka.core.nekku.NekkuProductMealType
import evaka.core.note.child.sticky.ChildStickyNoteBody
import evaka.core.note.child.sticky.createChildStickyNote
import evaka.core.placement.PlacementType
import evaka.core.s3.DocumentService
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCalendarEvent
import evaka.core.shared.dev.DevCalendarEventAttendee
import evaka.core.shared.dev.DevCalendarEventTime
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChild
import evaka.core.shared.dev.DevChildDocument
import evaka.core.shared.dev.DevChildDocumentPublishedVersion
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDocumentTemplate
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFamilyContact
import evaka.core.shared.dev.DevFosterParent
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
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

    private fun insertAdultWithCitizenUser(): PersonId = db.transaction { tx ->
        val id = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
        tx.updateLastStrongLogin(now, id)
        id
    }

    private fun insertGuardianship(guardian: PersonId, childId: ChildId) {
        db.transaction { it.insert(DevGuardian(guardianId = guardian, childId = childId)) }
    }

    private fun insertFosterParenthood(parent: PersonId, childId: ChildId) {
        db.transaction {
            it.insert(
                DevFosterParent(
                    childId = childId,
                    parentId = parent,
                    validDuring = DateRange(today.minusYears(2), today.minusYears(1)),
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
}
