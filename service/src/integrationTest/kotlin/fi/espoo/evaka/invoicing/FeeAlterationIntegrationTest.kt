// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.attachment.getAttachment
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.FeeAlterationController
import fi.espoo.evaka.invoicing.data.upsertFeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlterationAttachment
import fi.espoo.evaka.invoicing.domain.FeeAlterationType
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class FeeAlterationIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var feeAlterationController: FeeAlterationController
    @Autowired private lateinit var attachmentsController: AttachmentsController

    private fun assertEqualEnough(expected: List<FeeAlteration>, actual: List<FeeAlteration>) {
        val nullId = FeeAlterationId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        assertEquals(
            expected.map { it.copy(id = nullId, updatedAt = null) }.toSet(),
            actual.map { it.copy(id = nullId, updatedAt = null) }.toSet()
        )
    }

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testDecisionMaker_1)
            tx.insert(testChild_1, DevPersonType.CHILD)
        }
    }

    private val testFeeAlterationId = FeeAlterationId(UUID.randomUUID())
    private val user =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))
    private val personId = testChild_1.id
    private val clock = MockEvakaClock(2019, 1, 10, 12, 0)

    private val testFeeAlteration =
        FeeAlteration(
            id = testFeeAlterationId,
            personId = personId,
            type = FeeAlterationType.DISCOUNT,
            amount = 50,
            isAbsolute = false,
            validFrom = LocalDate.of(2019, 1, 1),
            validTo = LocalDate.of(2019, 1, 31),
            notes = "",
            updatedBy = EvakaUserId(testDecisionMaker_1.id.raw)
        )

    @Test
    fun `getFeeAlterations works with no data in DB`() {
        val result = getFeeAlterations(personId)
        assertEquals(0, result.size)
    }

    @Test
    fun `getFeeAlterations works with single fee alteration in DB`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }

        val result = getFeeAlterations(personId)
        assertEqualEnough(listOf(testFeeAlteration), result.map { it.data })
    }

    @Test
    fun `getFeeAlterations works with multiple fee alterations in DB`() {
        val feeAlterations =
            listOf(
                testFeeAlteration.copy(
                    id = FeeAlterationId(UUID.randomUUID()),
                    validFrom = testFeeAlteration.validFrom.plusYears(1),
                    validTo = testFeeAlteration.validTo!!.plusYears(1)
                ),
                testFeeAlteration
            )
        db.transaction { tx -> feeAlterations.forEach { tx.upsertFeeAlteration(clock, it) } }

        val result = getFeeAlterations(personId)
        assertEqualEnough(feeAlterations, result.map { it.data })
    }

    @Test
    fun `createFeeAlterations works with valid fee alteration`() {
        createFeeAlteration(testFeeAlteration)

        val result = getFeeAlterations(personId)
        assertEqualEnough(
            listOf(testFeeAlteration.copy(updatedBy = EvakaUserId(testDecisionMaker_1.id.raw))),
            result.map { it.data }
        )
    }

    @Test
    fun `createFeeAlterations throws with invalid date range`() {
        val feeAlteration =
            testFeeAlteration.copy(validTo = testFeeAlteration.validFrom.minusDays(1))
        assertThrows<BadRequest> { createFeeAlteration(feeAlteration) }
    }

    @Test
    fun `updateFeeAlteration works with valid fee alteration`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }

        val updated = testFeeAlteration.copy(amount = 100)
        updateFeeAlteration(testFeeAlteration.id!!, updated)

        val result = getFeeAlterations(personId)
        assertEqualEnough(
            listOf(updated.copy(updatedBy = EvakaUserId(testDecisionMaker_1.id.raw))),
            result.map { it.data }
        )
    }

    @Test
    fun `updateFeeAlteration throws with invalid date rage`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }

        val updated = testFeeAlteration.copy(validTo = testFeeAlteration.validFrom.minusDays(1))
        assertThrows<BadRequest> { updateFeeAlteration(testFeeAlteration.id!!, updated) }
    }

    @Test
    fun `delete works with existing fee alteration`() {
        val deletedId = FeeAlterationId(UUID.randomUUID())
        db.transaction { tx ->
            tx.upsertFeeAlteration(clock, testFeeAlteration)
            tx.upsertFeeAlteration(clock, testFeeAlteration.copy(id = deletedId))
        }

        deleteFeeAlteration(deletedId)

        val result = getFeeAlterations(personId)
        assertEquals(1, result.size)
        assertFalse(result.any { it.data.id == deletedId })
    }

    @Test
    fun `delete does nothing with non-existent id`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }

        deleteFeeAlteration(FeeAlterationId(UUID.randomUUID()))

        val result = getFeeAlterations(personId)
        assertEqualEnough(listOf(testFeeAlteration), result.map { it.data })
    }

    @Test
    fun `add an attachment`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }

        val attachmentId = uploadAttachment(testFeeAlterationId)

        val result = getFeeAlterations(personId)
        assertEqualEnough(
            listOf(
                testFeeAlteration.copy(
                    attachments =
                        listOf(FeeAlterationAttachment(attachmentId, "evaka-logo.png", "image/png"))
                )
            ),
            result.map { it.data }
        )
    }

    @Test
    fun `attachment gets orphaned on fee alteration deletion`() {
        db.transaction { tx -> tx.upsertFeeAlteration(clock, testFeeAlteration) }
        val attachmentId = uploadAttachment(testFeeAlterationId)

        val result = getFeeAlterations(personId)
        assertEqualEnough(
            listOf(
                testFeeAlteration.copy(
                    attachments =
                        listOf(FeeAlterationAttachment(attachmentId, "evaka-logo.png", "image/png"))
                )
            ),
            result.map { it.data }
        )

        deleteFeeAlteration(testFeeAlterationId)
        val attachment = db.read { tx -> tx.getAttachment(attachmentId) }
        assertEquals(AttachmentParent.None, attachment?.attachedTo)
    }

    private fun createFeeAlteration(body: FeeAlteration) {
        feeAlterationController.createFeeAlteration(dbInstance(), user, RealEvakaClock(), body)
    }

    private fun updateFeeAlteration(id: FeeAlterationId, body: FeeAlteration) {
        feeAlterationController.updateFeeAlteration(dbInstance(), user, RealEvakaClock(), id, body)
    }

    private fun getFeeAlterations(
        personId: PersonId
    ): List<FeeAlterationController.FeeAlterationWithPermittedActions> {
        return feeAlterationController.getFeeAlterations(dbInstance(), user, clock, personId)
    }

    private fun deleteFeeAlteration(id: FeeAlterationId) {
        feeAlterationController.deleteFeeAlteration(dbInstance(), user, RealEvakaClock(), id)
    }

    private fun uploadAttachment(id: FeeAlterationId): AttachmentId {
        return attachmentsController.uploadFeeAlterationAttachment(
            dbInstance(),
            user,
            clock,
            id,
            MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes())
        )
    }
}
